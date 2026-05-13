package com.aibot.crm.data.db.dao

import androidx.room.*
import com.aibot.crm.data.db.entities.Person
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Query("SELECT * FROM persons ORDER BY displayName ASC")
    fun observeAll(): Flow<List<Person>>

    @Query("SELECT * FROM persons WHERE id = :id")
    suspend fun getById(id: Long): Person?

    @Query("SELECT * FROM persons WHERE normalizedName = :name LIMIT 1")
    suspend fun findByNormalizedName(name: String): Person?

    @Query("""
        SELECT * FROM persons
        WHERE normalizedName LIKE '%' || :query || '%'
        ORDER BY displayName ASC
    """)
    suspend fun search(query: String): List<Person>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(person: Person): Long

    @Update
    suspend fun update(person: Person)

    @Upsert
    suspend fun upsert(person: Person): Long

    @Delete
    suspend fun delete(person: Person)
}
