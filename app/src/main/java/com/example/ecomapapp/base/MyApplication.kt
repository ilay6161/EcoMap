package com.example.ecomapapp.base

import android.app.Application
import android.content.Context

class MyApplication : Application() {

    companion object {
        var appContext: Context? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
}
