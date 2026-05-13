package com.aibot.crm.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "persons",
    indices = [Index(value = ["normalizedName"], unique = true)]
)
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    /** Lowercased + trimmed for fuzzy lookup by name */
    val normalizedName: String = displayName.trim().lowercase(),
    val phone: String? = null,
    val email: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
