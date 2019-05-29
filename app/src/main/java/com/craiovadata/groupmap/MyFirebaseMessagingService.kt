package com.craiovadata.groupmap

import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String?) {
        super.onNewToken(token)
//        sendRegistrationToServer(token,)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        super.onMessageReceived(remoteMessage)
        // Check if message contains a data payload.
        remoteMessage?.data?.isNotEmpty()?.let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            val groupId = remoteMessage.data["groupId"]
            startTrackerService(groupId)
        }
    }

    private fun startTrackerService(groupId: String?) {
        val intent = Intent(this, TrackerService::class.java)
        intent.putExtra("groupId", groupId)
        startService(intent)
//        finish()
    }


    companion object {
        private val TAG = MyFirebaseMessagingService::class.java.simpleName
    }


}