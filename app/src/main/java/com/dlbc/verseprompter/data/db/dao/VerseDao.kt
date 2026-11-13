package com.dlbc.verseprompter.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dlbc.verseprompter.data.db.entity.VerseEntity

@Dao
interface VerseDao {

    // ---- Prompter hot path --------------------------------------------------

    /** Single-verse lookup. Resolves straight through the primary-key index. */
    @Query(
        """
        SELECT * FROM verses
        WHERE versionCode = :versionCode
          AND bookNumber = :bookNumber
          AND chapter = :chapter
          AND verse = :verse
        LIMIT 1
        """
    )
    suspend fun getVerse(
        versionCode: String,
        bookNumber: Int,
        chapter: Int,
        verse: Int
    ): VerseEntity?

    /** Inclusive verse range, used to fill the verse-by-verse queue. */
    @Query(
        """
        SELECT * FROM verses
        WHERE versionCode = :versionCode
          AND bookNumber = :bookNumber
          AND chapter = :chapter
          AND verse BETWEEN :startVerse AND :endVerse
        ORDER BY verse ASC
        """
    )
    suspend fun getRange(
        versionCode: String,
        bookNumber: Int,
        chapter: Int,
        startVerse: Int,
        endVerse: Int
    ): List<VerseEntity>

    /** Whole chapter - used by manual swipe navigation past the end of a range. */
    @Query(
        """
        SELECT * FROM verses
        WHERE versionCode = :versionCode
          AND bookNumber = :bookNumber
          AND chapter = :chapter
        ORDER BY verse ASC
        """
    )
    suspend fun getChapter(
        versionCode: String,
        bookNumber: Int,
        chapter: Int
    ): List<VerseEntity>

    /**
     * Highest verse number in a chapter. The prompter uses this to stop cleanly
     * at the end of a chapter instead of showing a blank screen.
     */
    @Query(
        """
        SELECT MAX(verse) FROM verses
        WHERE versionCode = :versionCode
          AND bookNumber = :bookNumber
          AND chapter = :chapter
        """
    )
    suspend fun lastVerseOfChapter(
        versionCode: String,
        bookNumber: Int,
        chapter: Int
    ): Int?

    /** Highest chapter number present for a book in this translation. */
    @Query(
        """
        SELECT MAX(chapter) FROM verses
        WHERE versionCode = :versionCode AND bookNumber = :bookNumber
        """
    )
    suspend fun lastChapterOfBook(versionCode: String, bookNumber: Int): Int?

    // ---- Search (Bible Manager / manual lookup) -----------------------------

    @Query(
        """
        SELECT * FROM verses
        WHERE versionCode = :versionCode AND text LIKE '%' || :query || '%'
        ORDER BY bookNumber, chapter, verse
        LIMIT :limit
        """
    )
    suspend fun search(versionCode: String, query: String, limit: Int): List<VerseEntity>

    // ---- Seeding / maintenance ---------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(verses: List<VerseEntity>)

    @Query("SELECT COUNT(*) FROM verses WHERE versionCode = :versionCode")
    suspend fun countForVersion(versionCode: String): Int

    @Query("DELETE FROM verses WHERE versionCode = :versionCode")
    suspend fun deleteVersion(versionCode: String)
}
