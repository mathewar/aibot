package com.aibot.crm

import android.app.Application
import com.aibot.crm.data.db.CrmDatabase

class CrmApplication : Application() {
    // Eagerly open the database on startup so the first App Function call is fast
    val database by lazy { CrmDatabase.getInstance(this) }
}
