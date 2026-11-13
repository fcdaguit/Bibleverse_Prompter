package com.dlbc.verseprompter.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dlbc.verseprompter.data.db.dao.BibleVersionDao
import com.dlbc.verseprompter.data.db.dao.VerseDao
import com.dlbc.verseprompter.data.db.entity.BibleVersionEntity
import com.dlbc.verseprompter.data.db.entity.VerseEntity
import java.util.concurrent.atomic.AtomicBoolean

@Database(
    entities = [VerseEntity::class, BibleVersionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class BibleDatabase : RoomDatabase() {

    abstract fun verseDao(): VerseDao
    abstract fun bibleVersionDao(): BibleVersionDao

    companion object {
        private const val DB_NAME = "bible_prompter.db"

        /**
         * Set by the Room creation callback the first time the database file is
         * created. [com.dlbc.verseprompter.data.db.seed.BibleSeedManager] reads
         * it as a fast path, but never trusts it alone - it also verifies row
         * counts, so an interrupted first launch still recovers.
         */
        val freshlyCreated = AtomicBoolean(false)

        @Volatile
        private var instance: BibleDatabase? = null

        fun get(context: Context): BibleDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): BibleDatabase =
            Room.databaseBuilder(context, BibleDatabase::class.java, DB_NAME)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Do NOT seed here. onCreate runs on the database thread
                        // while the first query is blocked behind it; parsing
                        // three Bibles inline would stall app start for seconds.
                        // We only raise the flag and let the seed manager run it
                        // in the background with progress reported to the UI.
                        freshlyCreated.set(true)
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Foreign keys are off by default in SQLite; the
                        // verses -> version cascade delete depends on them.
                        db.execSQL("PRAGMA foreign_keys = ON")
                    }
                })
                // Only bundled, regenerable content lives here, so a schema change
                // can safely rebuild rather than ship a migration. Revisit this if
                // user-created data (bookmarks, history) is ever added.
                .fallbackToDestructiveMigration()
                .build()
    }
}
