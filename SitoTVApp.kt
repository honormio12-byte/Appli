package com.sitotv.iptv

import android.app.Application
import com.sitotv.iptv.database.SitoDatabase
import com.sitotv.iptv.repository.StreamRepository

class SitoTVApp : Application() {

    val database: SitoDatabase by lazy { SitoDatabase.getInstance(this) }
    val streamRepository: StreamRepository by lazy { StreamRepository() }

    companion object {
        lateinit var instance: SitoTVApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
