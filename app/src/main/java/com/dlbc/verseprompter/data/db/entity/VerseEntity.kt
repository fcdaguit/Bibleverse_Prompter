package com.dlbc.verseprompter.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.dlbc.verseprompter.data.model.Verse
import com.dlbc.verseprompter.data.model.VerseRef

/**
 * One verse of one translation.
 *
 * The composite primary key (version, book, chapter, verse) is what makes a
 * single-verse lookup effectively instant: SQLite resolves it through the
 * primary-key index with no table scan, which is comfortably inside the 10 ms
 * budget even with 100k+ rows across several translations.
 *
 * Using a natural composite key instead of an auto-generated row id also makes
 * re-seeding idempotent - inserting the same verse twice simply replaces it.
 */
@Entity(
    tableName = "verses",
    primaryKeys = ["versionCode", "bookNumber", "chapter", "verse"],
    foreignKeys = [
        ForeignKey(
            entity = BibleVersionEntity::class,
            parentColumns = ["code"],
            childColumns = ["versionCode"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        // Supports "give me the whole chapter" and the range queue.
        Index(value = ["versionCode", "bookNumber", "chapter"]),
        // Supports the version filter used when deleting a downloaded Bible.
        Index(value = ["versionCode"])
    ]
)
data class VerseEntity(
    val versionCode: String,
    val bookNumber: Int,
    val chapter: Int,
    val verse: Int,
    val text: String
) {
    fun toVerse(languageTag: String): Verse = Verse(
        ref = VerseRef(bookNumber, chapter, verse),
        text = text,
        versionCode = versionCode,
        languageTag = languageTag
    )
}
