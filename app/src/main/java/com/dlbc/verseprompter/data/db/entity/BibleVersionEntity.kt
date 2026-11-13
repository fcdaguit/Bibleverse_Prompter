package com.dlbc.verseprompter.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dlbc.verseprompter.data.model.BibleVersion
import com.dlbc.verseprompter.data.model.VersionSource

/**
 * A translation installed on the device.
 *
 * [isComplete] is written only after the seeder finishes the whole file. If the
 * app is killed mid-seed the row stays incomplete, and the seeder knows to wipe
 * and retry that version on next launch instead of serving a half-loaded Bible.
 */
@Entity(tableName = "bible_versions")
data class BibleVersionEntity(
    @PrimaryKey val code: String,
    val name: String,
    val abbreviation: String,
    val languageTag: String,
    val languageLabel: String,
    val source: String,
    val assetPath: String?,
    val verseCount: Int,
    val installedAt: Long,
    val isComplete: Boolean,
    val sortOrder: Int
) {
    fun toModel(): BibleVersion = BibleVersion(
        code = code,
        name = name,
        abbreviation = abbreviation,
        languageTag = languageTag,
        languageLabel = languageLabel,
        source = runCatching { VersionSource.valueOf(source) }.getOrDefault(VersionSource.BUNDLED),
        verseCount = verseCount,
        isComplete = isComplete
    )
}
