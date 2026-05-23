package com.example.echowithin

import android.app.Application
import com.example.echowithin.data.network.SessionManager

class EchoWithinApplication : Application() {
    companion object {
        lateinit var instance: EchoWithinApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        SessionManager.init(this)
    }
}
