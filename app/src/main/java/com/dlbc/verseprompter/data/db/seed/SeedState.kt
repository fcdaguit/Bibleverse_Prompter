package com.dlbc.verseprompter.data.db.seed

/**
 * Progress of the one-time bundled-Bible import.
 *
 * The prompter screen is usable as soon as the first version reports [Ready];
 * the remaining translations keep loading in the background.
 */
sealed interface SeedState {

    /** Nothing to do - every bundled version is already installed. */
    data object Ready : SeedState

    data class InProgress(
        val versionCode: String,
        val versionName: String,
        val versesLoaded: Int,
        val versionIndex: Int,
        val versionCount: Int
    ) : SeedState {
        val label: String
            get() = "Preparing $versionName ($versionIndex of $versionCount)"
    }

    /**
     * Seeding failed. The app still runs on whatever versions did install; the
     * Bible Manager screen surfaces [message] so the problem is visible instead
     * of silent.
     */
    data class Failed(val versionCode: String, val message: String) : SeedState
}
