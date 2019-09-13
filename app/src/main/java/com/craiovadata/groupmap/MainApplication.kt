package com.craiovadata.groupmap

import android.app.Application
import android.os.StrictMode
import com.craiovadata.groupmap.util.CrashlyticsTree
import timber.log.Timber

class MainApplication : Application() {


    override fun onCreate() {
        // ThreeTenBP for times and dates, called before super to be available for objects
//        AndroidThreeTen.init(this)

        // Enable strict mode before Dagger creates graph
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }

        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }  else {
            Timber.plant(CrashlyticsTree())
        }
    }


    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
//                .detectDiskReads()
//                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
    }
}
