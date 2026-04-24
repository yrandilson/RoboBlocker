package com.roboblocker

import android.app.Application
import com.roboblocker.data.db.AppDatabase
import com.roboblocker.data.prefs.AppPreferences
import com.roboblocker.data.repository.BlockerRepository
import com.roboblocker.utils.NotificationHelper

class App : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val preferences by lazy { AppPreferences(this) }
    val repository by lazy { BlockerRepository(database, preferences) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.createChannels(this)
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
