package com.example.smartswine

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.example.smartswine.data.FirestoreManager
import com.example.smartswine.utils.Translator

class SmartSwineApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        FirestoreManager.configure()
        Translator.init(applicationContext)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var appContext: Context? = null
            private set
    }
}
