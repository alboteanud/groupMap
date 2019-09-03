package com.craiovadata.groupmap.application

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.craiovadata.groupmap.BuildConfig
import timber.log.Timber



class ApplicationController : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
