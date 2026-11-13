package com.dlbc.verseprompter.data.db.seed

import android.util.JsonReader
import android.util.JsonToken
import com.dlbc.verseprompter.data.db.entity.VerseEntity
import com.dlbc.verseprompter.data.model.BibleBooks
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

/**
 * Streaming importer for a Bible JSON file.
 *
 * Deliberately uses [android.util.JsonReader] rather than loading the document
 * into memory: a full Bible is roughly 31,000 verses and 4-5 MB of text, and
 * parsing it as a tree on a low-end phone is the difference between a smooth
 * first launch and an OutOfMemoryError.
 *
 * ### Accepted shapes
 * The canonical format is documented in `assets/bibles/README.md`:
 * ```json
 * { "code": "KJV", "verses": [ { "b": 1, "c": 1, "v": 1, "t": "In the ..." } ] }
 * ```
 * For convenience with the various public-domain dumps floating around, the
 * importer also accepts:
 *  - the root being a bare array of verse objects;
 *  - the verse array under `verses`, `data` or `rows`;
 *  - book identified by number (`1`) or by name (`Genesis`, `Juan`, `1Cor`);
 *  - long key names: `book`/`book_name`, `chapter`, `verse`, `text`.
 *
 * Rows that cannot be resolved to a canonical book are skipped and counted in
 * [Result.skipped] rather than aborting the whole import - one bad line in a
 * community-maintained file should not cost the user their Bible.
 */
class BibleJsonImporter {

    data class Result(val inserted: Int, val skipped: Int)

    /**
     * Parses [input] and hands batches of verses to [onBatch].
     *
     * @param batchSize rows per database insert. 2,000 keeps peak memory small
     *                  while still amortising transaction overhead.
     */
    suspend fun import(
        input: InputStream,
        versionCode: String,
        gzipped: Boolean,
        batchSize: Int = DEFAULT_BATCH,
        onBatch: suspend (List<VerseEntity>) -> Unit
    ): Result {
        val stream = if (gzipped) GZIPInputStream(input, GZIP_BUFFER) else input
        var inserted = 0
        var skipped = 0
        val batch = ArrayList<VerseEntity>(batchSize)

        JsonReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
            // Tolerate the unquoted keys and trailing commas that turn up in
            // hand-edited community Bible files.
            reader.isLenient = true
            when (reader.peek()) {
                JsonToken.BEGIN_ARRAY -> {
                    // Root is already the verse array.
                    reader.beginArray()
                    while (reader.hasNext()) {
                        val row = readVerse(reader, versionCode)
                        if (row == null) skipped++ else batch += row
                        if (batch.size >= batchSize) {
                            onBatch(batch.toList()); inserted += batch.size; batch.clear()
                        }
                    }
                    reader.endArray()
                }

                JsonToken.BEGIN_OBJECT -> {
                    reader.beginObject()
                    var sawArray = false
                    while (reader.hasNext()) {
                        val name = reader.nextName()
                        if (name in VERSE_ARRAY_KEYS && reader.peek() == JsonToken.BEGIN_ARRAY) {
                            sawArray = true
                            reader.beginArray()
                            while (reader.hasNext()) {
                                val row = readVerse(reader, versionCode)
                                if (row == null) skipped++ else batch += row
                                if (batch.size >= batchSize) {
                                    onBatch(batch.toList()); inserted += batch.size; batch.clear()
                                }
                            }
                            reader.endArray()
                        } else {
                            // Header fields (code, name, language, ...) are read
                            // from the manifest, so skip whatever else is here.
                            reader.skipValue()
                        }
                    }
                    reader.endObject()
                    if (!sawArray) {
                        throw IllegalStateException(
                            "No verse array found in $versionCode. Expected one of " +
                                VERSE_ARRAY_KEYS.joinToString()
                        )
                    }
                }

                else -> throw IllegalStateException("Unexpected JSON root in $versionCode")
            }
        }

        if (batch.isNotEmpty()) {
            onBatch(batch.toList()); inserted += batch.size; batch.clear()
        }
        return Result(inserted, skipped)
    }

    /** Reads one verse object; returns null when the row is unusable. */
    private fun readVerse(reader: JsonReader, versionCode: String): VerseEntity? {
        var bookRaw: String? = null
        var chapter: Int? = null
        var verse: Int? = null
        var text: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                in BOOK_KEYS -> bookRaw = reader.nextStringLenient()
                in CHAPTER_KEYS -> chapter = reader.nextStringLenient()?.toIntOrNull()
                in VERSE_KEYS -> verse = reader.nextStringLenient()?.toIntOrNull()
                in TEXT_KEYS -> text = reader.nextStringLenient()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        val book = bookRaw?.let { BibleBooks.resolve(it) } ?: return null
        val c = chapter ?: return null
        val v = verse ?: return null
        val t = text?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (c < 1 || v < 1) return null

        return VerseEntity(
            versionCode = versionCode,
            bookNumber = book.number,
            chapter = c,
            verse = v,
            text = t
        )
    }

    /** Reads a value as text whether it is a JSON string, number or null. */
    private fun JsonReader.nextStringLenient(): String? = when (peek()) {
        JsonToken.NULL -> { nextNull(); null }
        JsonToken.BOOLEAN -> nextBoolean().toString()
        else -> nextString()
    }

    companion object {
        const val DEFAULT_BATCH = 2_000
        private const val GZIP_BUFFER = 32 * 1024

        private val VERSE_ARRAY_KEYS = setOf("verses", "data", "rows")
        private val BOOK_KEYS = setOf("b", "book", "book_number", "bookNumber", "book_name", "bookName")
        private val CHAPTER_KEYS = setOf("c", "chapter", "chapter_number", "chapterNumber")
        private val VERSE_KEYS = setOf("v", "verse", "verse_number", "verseNumber")
        private val TEXT_KEYS = setOf("t", "text", "verse_text", "content")
    }
}
