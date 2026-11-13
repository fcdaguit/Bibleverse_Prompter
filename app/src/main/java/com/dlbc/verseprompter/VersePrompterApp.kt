package com.dlbc.verseprompter

import android.app.Application
import com.dlbc.verseprompter.data.db.BibleDatabase
import com.dlbc.verseprompter.data.db.seed.BibleSeedManager
import com.dlbc.verseprompter.data.repository.BibleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency wiring - no Hilt. The object graph here is three objects
 * deep, and keeping it explicit means no annotation processor, no extra build
 * time and nothing to debug in CI.
 */
class VersePrompterApp : Application() {

    /** Lives as long as the process; used for the one-time Bible import. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: BibleDatabase by lazy { BibleDatabase.get(this) }

    val bibleRepository: BibleRepository by lazy { BibleRepository(database) }

    val seedManager: BibleSeedManager by lazy { BibleSeedManager(this, database) }

    override fun onCreate() {
        super.onCreate()
        // Kicks off on a background thread and reports progress through
        // seedManager.state. Safe to call on every launch.
        seedManager.ensureSeeded(appScope)
    }
}
