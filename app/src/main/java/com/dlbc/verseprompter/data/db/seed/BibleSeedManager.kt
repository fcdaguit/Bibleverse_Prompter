package com.dlbc.verseprompter.data.db.seed

import android.content.Context
import android.util.Log
import com.dlbc.verseprompter.data.db.BibleDatabase
import com.dlbc.verseprompter.data.db.entity.BibleVersionEntity
import com.dlbc.verseprompter.data.model.VersionSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Imports the bundled translations into Room on first launch.
 *
 * Design notes:
 *  - Runs off the main thread and reports progress through [state], so the UI
 *    can show "Preparing Ang Dating Biblia..." instead of a frozen screen.
 *  - Never trusts a single "first run" flag. It checks, per version, whether a
 *    complete row exists. An app killed halfway through leaves an incomplete
 *    row, which is wiped and re-imported next launch.
 *  - Is safe to call on every launch; when everything is installed it does a
 *    couple of indexed count queries and reports [SeedState.Ready].
 */
class BibleSeedManager(
    private val context: Context,
    private val database: BibleDatabase,
    private val importer: BibleJsonImporter = BibleJsonImporter()
) {

    private val _state = MutableStateFlow<SeedState>(SeedState.Ready)
    val state: StateFlow<SeedState> = _state.asStateFlow()

    private val mutex = Mutex()

    /** Fire-and-forget entry point used from Application.onCreate(). */
    fun ensureSeeded(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) { ensureSeededNow() }
    }

    suspend fun ensureSeededNow() = mutex.withLock {
        withContext(Dispatchers.IO) {
            val manifest = runCatching { BundledVersion.readManifest(context) }
                .getOrElse { error ->
                    Log.e(TAG, "Cannot read bundled Bible manifest", error)
                    _state.value = SeedState.Failed("manifest", error.friendlyMessage())
                    return@withContext
                }

            val versionDao = database.bibleVersionDao()
            val verseDao = database.verseDao()

            val pending = manifest.filterIndexed { _, bundled ->
                val existing = versionDao.getByCode(bundled.code)
                val rows = verseDao.countForVersion(bundled.code)
                val installed = existing?.isComplete == true && rows > 0
                !installed
            }

            if (pending.isEmpty()) {
                BibleDatabase.freshlyCreated.set(false)
                _state.value = SeedState.Ready
                return@withContext
            }

            pending.forEachIndexed { index, bundled ->
                try {
                    seedVersion(bundled, index + 1, pending.size)
                } catch (error: Throwable) {
                    Log.e(TAG, "Seeding ${bundled.code} failed", error)
                    // Leave the version marked incomplete so the next launch retries.
                    runCatching { verseDao.deleteVersion(bundled.code) }
                    _state.value = SeedState.Failed(bundled.code, error.friendlyMessage())
                    return@withContext
                }
            }

            BibleDatabase.freshlyCreated.set(false)
            _state.value = SeedState.Ready
        }
    }

    private suspend fun seedVersion(bundled: BundledVersion, index: Int, total: Int) {
        val versionDao = database.bibleVersionDao()
        val verseDao = database.verseDao()

        // Clear any partial import from a previous interrupted run.
        verseDao.deleteVersion(bundled.code)

        versionDao.upsert(
            BibleVersionEntity(
                code = bundled.code,
                name = bundled.name,
                abbreviation = bundled.abbreviation,
                languageTag = bundled.languageTag,
                languageLabel = bundled.languageLabel,
                source = VersionSource.BUNDLED.name,
                assetPath = bundled.assetPath,
                verseCount = 0,
                installedAt = System.currentTimeMillis(),
                isComplete = false,
                sortOrder = bundled.sortOrder
            )
        )

        _state.value = SeedState.InProgress(bundled.code, bundled.name, 0, index, total)

        var loaded = 0
        val result = context.assets.open(bundled.assetPath).use { stream ->
            importer.import(
                input = stream,
                versionCode = bundled.code,
                gzipped = bundled.assetPath.endsWith(".gz", ignoreCase = true)
            ) { batch ->
                verseDao.insertAll(batch)
                loaded += batch.size
                _state.value =
                    SeedState.InProgress(bundled.code, bundled.name, loaded, index, total)
            }
        }

        check(result.inserted > 0) {
            "${bundled.code} imported 0 verses - is ${bundled.assetPath} still the sample file?"
        }

        versionDao.markComplete(bundled.code, complete = true, verseCount = result.inserted)
        Log.i(
            TAG,
            "Seeded ${bundled.code}: ${result.inserted} verses" +
                if (result.skipped > 0) " (${result.skipped} rows skipped)" else ""
        )
    }

    private fun Throwable.friendlyMessage(): String = when (this) {
        is java.io.FileNotFoundException ->
            "A bundled Bible file is missing from the app package."
        is java.io.IOException ->
            "Could not read the bundled Bible file (${message ?: "I/O error"})."
        else -> message ?: this::class.java.simpleName
    }

    private companion object {
        const val TAG = "BibleSeedManager"
    }
}
