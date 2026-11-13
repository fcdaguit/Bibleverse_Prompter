package com.dlbc.verseprompter.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dlbc.verseprompter.data.db.entity.BibleVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BibleVersionDao {

    /** Everything that finished seeding, in display order. The Bible Manager and
     *  the Settings version picker both observe this. */
    @Query("SELECT * FROM bible_versions WHERE isComplete = 1 ORDER BY sortOrder ASC, name ASC")
    fun observeInstalled(): Flow<List<BibleVersionEntity>>

    @Query("SELECT * FROM bible_versions ORDER BY sortOrder ASC, name ASC")
    suspend fun getAll(): List<BibleVersionEntity>

    @Query("SELECT * FROM bible_versions WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): BibleVersionEntity?

    @Query("SELECT COUNT(*) FROM bible_versions WHERE isComplete = 1")
    suspend fun completeCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(version: BibleVersionEntity)

    @Query("UPDATE bible_versions SET isComplete = :complete, verseCount = :verseCount WHERE code = :code")
    suspend fun markComplete(code: String, complete: Boolean, verseCount: Int)

    @Query("DELETE FROM bible_versions WHERE code = :code")
    suspend fun delete(code: String)
}
