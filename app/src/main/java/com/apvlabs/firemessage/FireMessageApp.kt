package com.apvlabs.firemessage

import android.app.Application
import com.apvlabs.firemessage.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class FireMessageApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@FireMessageApp)
            modules(appModule)
        }
    }
}