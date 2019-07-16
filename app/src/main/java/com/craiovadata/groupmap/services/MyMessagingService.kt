package com.craiovadata.groupmap.services

import android.content.Context
import android.content.Intent
import com.craiovadata.groupmap.utils.*
import com.craiovadata.groupmap.utils.Util.sendTokenToServer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.lang.NumberFormatException

const val KEY_TIMESTAMP_UPDATE = "key_timestamp_update"

class MyMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String?) {
        super.onNewToken(token)
        sendTokenToServer()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        super.onMessageReceived(remoteMessage)
        // Check if message contains a data payload.
        val payload = remoteMessage?.data ?: return
        if (payload["request"] == "updatePosition") {
            startTrackerService(payload)
        }
    }

    private fun isAllowedTpSendPosition(payload: Map<String, Any>): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val sentUid = payload[UID] as? String ?: return false
        if (uid == null || sentUid != uid) {
            // cleanup this token from the group
            val groupId = payload[GROUP_ID] as? String ?: return false
            val ref = FirebaseFirestore.getInstance()
                .document("$GROUPS/$groupId/$TOKENS/$sentUid")
            ref.delete()
            return false
        }

        val requestTimestampimestampMills: Long?
        try {
            val requestTimestamp = payload[TIMESTAMP] as? String
            requestTimestampimestampMills = requestTimestamp?.toLong()
            if (requestTimestampimestampMills == null) return false
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            return false
        }

        val savedTimestamp = getSharedPreferences("_", Context.MODE_PRIVATE)
            .getLong(KEY_TIMESTAMP_UPDATE, 0)

        val shouldUpdate = requestTimestampimestampMills != savedTimestamp
        if (shouldUpdate) {
            getSharedPreferences("_", Context.MODE_PRIVATE).edit()
                .putLong(KEY_TIMESTAMP_UPDATE, requestTimestampimestampMills).apply()
        }
        return shouldUpdate
    }

    private fun startTrackerService(payload: Map<String, Any>) {
        val shouldUpdate = isAllowedTpSendPosition(payload)
        if (!shouldUpdate) return
        val groupId = payload[GROUP_ID] as? String ?: return
        val intent = Intent(this, TrackerService::class.java)
        intent.putExtra(GROUP_ID, groupId)
        startService(intent)
//        finish()
    }


    companion object {
        private val TAG = MyMessagingService::class.java.simpleName
    }


}