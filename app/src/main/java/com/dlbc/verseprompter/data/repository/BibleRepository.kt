package com.dlbc.verseprompter.data.repository

import com.dlbc.verseprompter.data.db.BibleDatabase
import com.dlbc.verseprompter.data.model.BibleBooks
import com.dlbc.verseprompter.data.model.BibleVersion
import com.dlbc.verseprompter.data.model.Verse
import com.dlbc.verseprompter.data.model.VerseRange
import com.dlbc.verseprompter.data.model.VerseRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * The only thing the view models talk to for scripture.
 *
 * Everything here is offline and local. Nothing in this class touches the
 * network - the optional downloader in Step 2b writes into the same tables and
 * then disappears from the picture.
 */
class BibleRepository(private val database: BibleDatabase) {

    private val verseDao get() = database.verseDao()
    private val versionDao get() = database.bibleVersionDao()

    // ---- Versions -----------------------------------------------------------

    fun observeInstalledVersions(): Flow<List<BibleVersion>> =
        versionDao.observeInstalled().map { rows -> rows.map { it.toModel() } }

    suspend fun getVersion(code: String): BibleVersion? = withContext(Dispatchers.IO) {
        versionDao.getByCode(code)?.toModel()
    }

    /**
     * Resolves the version to actually use: the requested one if it is installed
     * and complete, otherwise KJV, otherwise whatever is installed. Keeps the
     * prompter from going blank when a saved default has been deleted.
     */
    suspend fun resolveUsableVersion(preferred: String?): BibleVersion? =
        withContext(Dispatchers.IO) {
            val installed = versionDao.getAll().filter { it.isComplete }.map { it.toModel() }
            installed.firstOrNull { it.code.equals(preferred, ignoreCase = true) }
                ?: installed.firstOrNull { it.code == BibleVersion.DEFAULT_CODE }
                ?: installed.firstOrNull()
        }

    suspend fun deleteVersion(code: String) = withContext(Dispatchers.IO) {
        // Verses cascade via the foreign key, but deleting explicitly keeps this
        // correct even if someone turns the PRAGMA off later.
        verseDao.deleteVersion(code)
        versionDao.delete(code)
    }

    // ---- Verse lookup (the prompter hot path) -------------------------------

    suspend fun getVerse(versionCode: String, ref: VerseRef): Verse? =
        withContext(Dispatchers.IO) {
            val languageTag = languageTagFor(versionCode)
            verseDao.getVerse(versionCode, ref.bookNumber, ref.chapter, ref.verse)
                ?.toVerse(languageTag)
        }

    /**
     * Loads a spoken range. The caller displays only the first verse and holds
     * the rest as a queue - see the prompter view model in Step 3.
     */
    suspend fun getRange(versionCode: String, range: VerseRange): List<Verse> =
        withContext(Dispatchers.IO) {
            val languageTag = languageTagFor(versionCode)
            verseDao.getRange(
                versionCode = versionCode,
                bookNumber = range.bookNumber,
                chapter = range.chapter,
                startVerse = range.startVerse,
                endVerse = range.endVerse
            ).map { it.toVerse(languageTag) }
        }

    suspend fun getChapter(versionCode: String, bookNumber: Int, chapter: Int): List<Verse> =
        withContext(Dispatchers.IO) {
            val languageTag = languageTagFor(versionCode)
            verseDao.getChapter(versionCode, bookNumber, chapter).map { it.toVerse(languageTag) }
        }

    /**
     * Next verse in reading order, rolling into the next chapter and then the
     * next book. Returns null at Revelation 22:21.
     */
    suspend fun nextVerse(versionCode: String, ref: VerseRef): Verse? =
        withContext(Dispatchers.IO) {
            val languageTag = languageTagFor(versionCode)
            verseDao.getVerse(versionCode, ref.bookNumber, ref.chapter, ref.verse + 1)
                ?.let { return@withContext it.toVerse(languageTag) }

            // End of chapter - try the first verse of the next chapter.
            val lastChapter = verseDao.lastChapterOfBook(versionCode, ref.bookNumber) ?: 0
            if (ref.chapter < lastChapter) {
                verseDao.getVerse(versionCode, ref.bookNumber, ref.chapter + 1, 1)
                    ?.let { return@withContext it.toVerse(languageTag) }
            }

            // End of book - try the first verse of the next book.
            val nextBook = ref.bookNumber + 1
            if (BibleBooks.byNumber(nextBook) != null) {
                verseDao.getVerse(versionCode, nextBook, 1, 1)
                    ?.let { return@withContext it.toVerse(languageTag) }
            }
            null
        }

    /** Previous verse in reading order, for the manual swipe-back gesture. */
    suspend fun previousVerse(versionCode: String, ref: VerseRef): Verse? =
        withContext(Dispatchers.IO) {
            val languageTag = languageTagFor(versionCode)
            if (ref.verse > 1) {
                verseDao.getVerse(versionCode, ref.bookNumber, ref.chapter, ref.verse - 1)
                    ?.let { return@withContext it.toVerse(languageTag) }
            }
            if (ref.chapter > 1) {
                val lastVerse =
                    verseDao.lastVerseOfChapter(versionCode, ref.bookNumber, ref.chapter - 1)
                if (lastVerse != null) {
                    verseDao.getVerse(versionCode, ref.bookNumber, ref.chapter - 1, lastVerse)
                        ?.let { return@withContext it.toVerse(languageTag) }
                }
            }
            val previousBook = ref.bookNumber - 1
            val previousBookLastChapter =
                if (previousBook >= 1) verseDao.lastChapterOfBook(versionCode, previousBook) else null
            if (previousBookLastChapter != null) {
                val lastVerse =
                    verseDao.lastVerseOfChapter(versionCode, previousBook, previousBookLastChapter)
                if (lastVerse != null) {
                    verseDao.getVerse(versionCode, previousBook, previousBookLastChapter, lastVerse)
                        ?.let { return@withContext it.toVerse(languageTag) }
                }
            }
            null
        }

    suspend fun lastVerseOfChapter(versionCode: String, bookNumber: Int, chapter: Int): Int? =
        withContext(Dispatchers.IO) {
            verseDao.lastVerseOfChapter(versionCode, bookNumber, chapter)
        }

    /**
     * Clamps a spoken range to what the chapter actually contains, so
     * "Psalm 23 verses 1 to 20" quietly becomes 1-6 instead of showing blanks.
     */
    suspend fun clampRange(versionCode: String, range: VerseRange): VerseRange? =
        withContext(Dispatchers.IO) {
            val last = verseDao.lastVerseOfChapter(versionCode, range.bookNumber, range.chapter)
                ?: return@withContext null
            val start = range.startVerse.coerceIn(1, last)
            val end = range.endVerse.coerceIn(start, last)
            range.copy(startVerse = start, endVerse = end)
        }

    // ---- Search -------------------------------------------------------------

    suspend fun search(versionCode: String, query: String, limit: Int = 100): List<Verse> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            val languageTag = languageTagFor(versionCode)
            verseDao.search(versionCode, query.trim(), limit).map { it.toVerse(languageTag) }
        }

    // ---- Internals ----------------------------------------------------------

    /**
     * Language tags barely ever change, but they are read on every verse lookup,
     * so they are cached. Concurrent because lookups run on the IO pool.
     */
    private val languageTagCache = ConcurrentHashMap<String, String>()

    private suspend fun languageTagFor(versionCode: String): String =
        languageTagCache[versionCode] ?: run {
            val tag = versionDao.getByCode(versionCode)?.languageTag ?: "en"
            languageTagCache[versionCode] = tag
            tag
        }
}
