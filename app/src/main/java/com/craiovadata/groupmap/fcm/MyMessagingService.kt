package com.craiovadata.groupmap.fcm

import android.os.Bundle
import com.craiovadata.groupmap.repo.Repository
import com.craiovadata.groupmap.tracker.TrackerService
import com.craiovadata.groupmap.utils_.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.IOException
import java.lang.NumberFormatException
import java.text.SimpleDateFormat
import java.util.*


class MyMessagingService : FirebaseMessagingService() {
    private val auth by inject<FirebaseAuth>()
//    private val analytics by inject<FirebaseAnalytics>()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val repository by inject<Repository>()
        repository.sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

//        PAYLOAD look
//        group: groupId,
//        reqPos: requestTime,
//        to: uidDest

        val payload = remoteMessage.data
        Timber.e("onMessageReceived() fcm payload: $payload")
        val toUid = payload[UID] ?: return      // destinatarul

        logAnalytics(payload)

        val isDestinationCorrect = auth.uid != null && auth.uid == toUid
        if (!isDestinationCorrect) {
            Timber.e("wrong destination")
            resetInstanceId()
            return
        }

        try {
            val requestTime = payload["reqPos"]?.toLong()   // catch the exception

            // functions effects must be idempotent
            // check time to see if already done sending position
            val isRequestNewAndValid = PrefUtils.isValidTimeForLocationRequest(this, requestTime)
            if (isRequestNewAndValid) {
            val groupId = payload["group"] ?: return
            startService(TrackerService.newIntent(this, groupId))
        }
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
    }

    private fun logAnalytics(payload: MutableMap<String, String>) {
        val bundle = Bundle()

        val toUid = payload[UID]
        bundle.putString(FirebaseAnalytics.Param.DESTINATION, toUid)

        val groupId = payload["group"]
        bundle.putString("groupId", groupId)

        val requestTime = payload["reqPos"]?.toLong()
        val time = SimpleDateFormat.getDateTimeInstance().format(Date(requestTime!!))
        bundle.putString("request_time", time)

        val firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        firebaseAnalytics.logEvent("fcm_message_received", bundle)
    }

    fun resetInstanceId() {
        Thread(Runnable {
            try {
                FirebaseInstanceId.getInstance().deleteInstanceId()
                FirebaseInstanceId.getInstance().instanceId
                Timber.d("InstanceId removed and regenerated.")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }).start()
    }

}