package com.aibot.crm.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aibot.crm.data.db.dao.InteractionDao
import com.aibot.crm.data.db.dao.PersonDao
import com.aibot.crm.data.db.entities.Interaction
import com.aibot.crm.data.db.entities.Person

@Database(
    entities = [Person::class, Interaction::class],
    version = 1,
    exportSchema = true,
)
abstract class CrmDatabase : RoomDatabase() {

    abstract fun personDao(): PersonDao
    abstract fun interactionDao(): InteractionDao

    companion object {
        @Volatile private var INSTANCE: CrmDatabase? = null

        fun getInstance(context: Context): CrmDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CrmDatabase::class.java,
                    // Stored in app's private internal storage — never leaves device
                    "crm.db",
                )
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = false)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
