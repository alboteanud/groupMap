package com.craiovadata.groupmap.services

import android.content.Context
import android.content.Intent
import android.util.Log
import com.craiovadata.groupmap.utils.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.lang.NumberFormatException

const val KEY_TIMESTAMP_UPDATE = "key_timestamp_update"

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String?) {
        super.onNewToken(token)
//        sendRegistrationToServer(token,)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        super.onMessageReceived(remoteMessage)
        // Check if message contains a data payload.
        val payload = remoteMessage?.data
        payload?.apply {
            Log.d(TAG, "Message data payload: $this")
            if (this["request"] == UPDATE_REQUEST) {
                val groupId = this[GROUP_ID]
                val timestamp = this[TIMESTAMP]
                val name = this[NAME]
                val uidSent = this[UID]

                FirebaseAuth.getInstance().currentUser?.apply {
                    if (uidSent == uid) {
                        if (isOldPosition(timestamp)) {
                            startTrackerService(groupId)
                        }

                    }
                }


            }
        }
    }

    private fun isOldPosition(timestamp: String?): Boolean {
        if (timestamp == null) return false
        val timestampMills: Long
        try {
            timestampMills = timestamp.toLong()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            return false
        }

        val updatedAt = getSharedPreferences("_", Context.MODE_PRIVATE)
            .getLong(KEY_TIMESTAMP_UPDATE, 0)

        val margin = 1 * 60 * 1000 // 1 min
        val shouldUpdate = timestampMills > updatedAt + margin
        if (shouldUpdate) {
            getSharedPreferences("_", Context.MODE_PRIVATE).edit()
                .putLong(KEY_TIMESTAMP_UPDATE, timestampMills).apply()
        }
        return shouldUpdate
    }

    private fun startTrackerService(groupId: String?) {
        if (groupId == null) return
        val intent = Intent(this, TrackerService::class.java)
        intent.putExtra(GROUP_ID, groupId)
        startService(intent)
//        finish()
    }


    companion object {
        private val TAG = MyFirebaseMessagingService::class.java.simpleName
    }


}