package com.dlbc.verseprompter.data.model

/** Where a translation came from. Bundled versions can never be deleted. */
enum class VersionSource { BUNDLED, DOWNLOADED }

/** A translation installed in the local database. */
data class BibleVersion(
    val code: String,
    val name: String,
    val abbreviation: String,
    val languageTag: String,
    val languageLabel: String,
    val source: VersionSource,
    val verseCount: Int,
    val isComplete: Boolean
) {
    val canDelete: Boolean get() = source == VersionSource.DOWNLOADED

    /** Short badge shown in the corner of the prompter screen. */
    val badge: String get() = abbreviation.uppercase()

    companion object {
        /** Falls back to KJV when the user's saved default has been deleted. */
        const val DEFAULT_CODE = "KJV"
    }
}
