package com.aibot.crm.data.db.dao

import androidx.room.*
import com.aibot.crm.data.db.entities.Interaction
import kotlinx.coroutines.flow.Flow

@Dao
interface InteractionDao {

    @Query("SELECT * FROM interactions WHERE personId = :personId ORDER BY timestamp DESC")
    fun observeForPerson(personId: Long): Flow<List<Interaction>>

    @Query("SELECT * FROM interactions WHERE personId = :personId ORDER BY timestamp DESC")
    suspend fun getForPerson(personId: Long): List<Interaction>

    @Query("SELECT * FROM interactions WHERE personId = :personId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestForPerson(personId: Long): Interaction?

    @Query("SELECT * FROM interactions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 20): List<Interaction>

    @Insert
    suspend fun insert(interaction: Interaction): Long

    @Delete
    suspend fun delete(interaction: Interaction)
}
