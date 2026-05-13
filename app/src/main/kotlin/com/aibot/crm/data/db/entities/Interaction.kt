package com.aibot.crm.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "interactions",
    foreignKeys = [
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("personId"), Index("timestamp")]
)
data class Interaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    /** Free-form notes about what was discussed */
    val notes: String? = null,
    /** What this person needs from you or others */
    val needs: String? = null,
    /** What this person wants (aspirations, preferences) */
    val wants: String? = null,
    /** Caller-supplied timestamp; defaults to now */
    val timestamp: Long = System.currentTimeMillis(),
    val loggedAt: Long = System.currentTimeMillis(),
)
