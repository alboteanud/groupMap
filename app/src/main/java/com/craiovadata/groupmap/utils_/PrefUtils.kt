package com.craiovadata.groupmap.utils_

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.craiovadata.groupmap.activity.entry.EntryActivity

object PrefUtils {
    private const val PREF_KEY_FIRST_START = "pref_key_first_start"
    private const val PREF_KEY_REQUEST_POSITION_TIME = "pref_key_position_request_time"

    fun isFirstStart(context: Context): Boolean {
        return context.getSharedPreferences("_", AppCompatActivity.MODE_PRIVATE)
            .getBoolean(PREF_KEY_FIRST_START, true)
    }

    fun revokeFirstStart(context: Context) {
        context.getSharedPreferences("_", AppCompatActivity.MODE_PRIVATE).edit()
            .putBoolean(PREF_KEY_FIRST_START, false).apply()
    }

    fun isValidTimeForLocationRequest(context: Context, requestTime: Long?): Boolean {
        if (requestTime == null) return false

        val savedTime = context.getSharedPreferences("_", AppCompatActivity.MODE_PRIVATE)
            .getLong(PREF_KEY_REQUEST_POSITION_TIME, 0L)
        if (requestTime > savedTime) {
            context.getSharedPreferences("_", AppCompatActivity.MODE_PRIVATE).edit()
                .putLong(PREF_KEY_REQUEST_POSITION_TIME, requestTime).apply()
        }
        return requestTime > savedTime + 1000 * 30
    }
}