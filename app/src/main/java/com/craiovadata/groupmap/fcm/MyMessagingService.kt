package com.craiovadata.groupmap.fcm

import android.os.Bundle
import com.craiovadata.groupmap.BuildConfig
import com.craiovadata.groupmap.repo.Repository
import com.craiovadata.groupmap.tracker.TrackerService
import com.craiovadata.groupmap.util.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    private val db by inject<FirebaseFirestore>()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val repository by inject<Repository>()
        repository.sendTokenToServer(token)
    }

    init {
        Timber.e("MyMessagingService  init")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

//        PAYLOAD
//        group: groupId,
//        reqPos: requestTime,
//        to: uidDest
        val payload = remoteMessage.data
        Timber.e("onMessageReceived() fcm payload: ${payload}")
        if (BuildConfig.DEBUG){
            val reqTimeDate0 = SimpleDateFormat.getInstance().format(Date())
            db.collection("testData/${auth.currentUser?.email}/receivedMsg")
                .document("onMessageReceived").set(hashMapOf("payload" to payload.toString(),
                    "timestamp" to reqTimeDate0))
        }

        val toUid = payload[UID] ?: return      // destinatarul

        logAnalytics(payload)

        val isDestinationCorrect = auth.uid != null && auth.uid == toUid
        if (!isDestinationCorrect) {
            Timber.e("wrong destination")
            resetInstanceId()
            return
        }

        try {
            val requestTimeString = payload["reqPos"] ?: return  // catch the exception
            val requestTime = requestTimeString.toLong()   // catch the exception
            if (BuildConfig.DEBUG){
                val reqTimeDate = SimpleDateFormat.getInstance().format(Date(requestTime))
                db.collection("testData/${auth.currentUser?.email}/receivedMsg")
                    .document(requestTimeString).set(hashMapOf("onMessageReceived" to reqTimeDate ))
            }
            // functions effects must be idempotent
            // check time to see if already done sending position
            val isRequestNewAndValid = PrefUtils.isValidTimeForLocationRequest(this, requestTime)
            if (isRequestNewAndValid) {
                val groupId = payload["group"] ?: return
                startService(TrackerService.newIntent(this, groupId, requestTimeString))
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