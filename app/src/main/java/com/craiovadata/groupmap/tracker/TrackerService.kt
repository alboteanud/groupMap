package com.craiovadata.groupmap.tracker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.craiovadata.groupmap.BuildConfig
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.repo.Repository
import com.craiovadata.groupmap.util.DEFAULT_GROUP
import com.craiovadata.groupmap.util.EXTRA_GROUP_ID
import com.craiovadata.groupmap.util.MapUtils.LOCATION_RECEIVED
import com.craiovadata.groupmap.util.MapUtils.requestMyGpsLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.lang.IllegalArgumentException
import java.util.concurrent.TimeUnit

class TrackerService : Service() {
    private val repository by inject<Repository>()
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(this)
    private val db by inject<FirebaseFirestore>()
    private val auth by inject<FirebaseAuth>()
    private val handler = Handler()
    private var isReceiverRegistered = false
    private var timestamp: String? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val groupId = intent?.getStringExtra(EXTRA_GROUP_ID)

        if (groupId == null || groupId == DEFAULT_GROUP) {
            finishService()
            return super.onStartCommand(intent, flags, startId)
        }
        timestamp = intent.getStringExtra(EXTRA_TIMESTAMP)

        if (BuildConfig.DEBUG && timestamp != null) {
            db.collection("testData/${auth.currentUser?.email}/receivedMsg")
                .document(timestamp!!).set(hashMapOf("onStartCommand" to true), SetOptions.merge())
        }

        val permission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            //        getLocation(groupId)
            goForeground()
            getLocation2(groupId)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun getLocation2(groupId: String) {
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            // Got last known location. In some rare situations this can be null.

            if (BuildConfig.DEBUG && timestamp != null) {
                db.collection("testData/${auth.currentUser?.email}/receivedMsg")
                    .document(timestamp!!)
                    .set(hashMapOf("locationReceived" to true), SetOptions.merge())
            }

            sendLocationToDB(location, groupId)
        }
            .addOnCanceledListener {

                if (BuildConfig.DEBUG && timestamp != null) {
                    db.collection("testData/${auth.currentUser?.email}/receivedMsg")
                        .document(timestamp!!)
                        .set(hashMapOf("locationCanceled" to true), SetOptions.merge())
                }

            }

            .addOnFailureListener {

                if (BuildConfig.DEBUG && timestamp != null) {
                    db.collection("testData/${auth.currentUser?.email}/receivedMsg")
                        .document(timestamp!!)
                        .set(hashMapOf("locationFailure" to it.message), SetOptions.merge())
                }

            }
    }

    private fun sendLocationToDB(location: Location?, groupId: String) {
        if (location == null) return
        val handler = Handler()
        val bundle = Bundle()
        bundle.putBoolean("location_received", true)
        repository.sendMyPosition(groupId, location) {
            if (it.data != null) {
                Timber.d("position updated successfully")
                bundle.putBoolean("db_updated", true)

                if (BuildConfig.DEBUG && timestamp != null) {
                    db.collection("testData/${auth.currentUser?.email}/receivedMsg")
                        .document(timestamp!!)
                        .set(hashMapOf("db_updated" to true), SetOptions.merge())
                }

            } else if (it.exception != null) {
                Timber.e(it.exception, "position update error")
                bundle.putString("db_update_error", "${it.exception.message}")

                if (BuildConfig.DEBUG && timestamp != null) {
                    db.collection("testData/${auth.currentUser?.email}/receivedMsg")
                        .document(timestamp!!)
                        .set(hashMapOf("db_update_error" to true), SetOptions.merge())
                }

            }
            firebaseAnalytics.logEvent("TrackerService", bundle)

            val delay = TimeUnit.SECONDS.toMillis(3) // show a bit of notif
            handler.postDelayed(runnableFinish3, delay)
        }
    }

    private val runnableFinish3 = Runnable {
        if (BuildConfig.DEBUG && timestamp != null) {
            db.collection("testData/${auth.currentUser?.email}/receivedMsg").document(timestamp!!)
                .set(hashMapOf("finish_service_in" to "3 sec"), SetOptions.merge())
        }

        finishService()
    }
    private val runnableFinish120 = Runnable {
        if (BuildConfig.DEBUG && timestamp != null) {
            db.collection("testData/${auth.currentUser?.email}/receivedMsg").document(timestamp!!)
                .set(hashMapOf("finish_service_in" to "120 sec"), SetOptions.merge())
        }

        finishService()
    }

    private fun getLocation(groupId: String) {
        val bundle = Bundle()

        requestMyGpsLocation(this) { status, location ->
            when (status) {
                PackageManager.PERMISSION_GRANTED -> {     // signals starting the location process
                    bundle.putBoolean("start_location", true)
                    goForeground()

                    handler.postDelayed({
                        firebaseAnalytics.logEvent("TrackerService", bundle)
                        finishService()
                    }, 15000)   // clear notif anyway in some long time
                }
                PackageManager.PERMISSION_DENIED -> {  // no permission
                    bundle.putBoolean("permission_denied", true)
                    firebaseAnalytics.logEvent("TrackerService", bundle)
                    finishService()
                }
                LOCATION_RECEIVED -> {    // signals receive location
                    if (location != null) {
                        bundle.putBoolean("location_received", true)
                        repository.sendMyPosition(groupId, location) {
                            if (it.data != null) {
                                Timber.d("position updated successfully")
                                bundle.putBoolean("db_updated", true)
                            } else if (it.exception != null) {
                                Timber.e(it.exception, "position update error")
                                bundle.putString("db_update_error", "${it.exception.message}")
                            }
                            handler.removeCallbacksAndMessages(null)
                            handler.postDelayed({
                                firebaseAnalytics.logEvent("TrackerService", bundle)
                                finishService()
                            }, 3000)
                        }
                    }
                }
            }
        }
    }

    private fun finishService() {
        handler.removeCallbacks(runnableFinish3)
        handler.removeCallbacks(runnableFinish120)
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(stopReceiver)
                isReceiverRegistered = false
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }

        stopForeground(true)
        stopSelf()
    }

    private fun goForeground() {

        val CHANNEL_ID = "my_channel_01"
        if (Build.VERSION.SDK_INT >= 26) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val stop = "stop"

        registerReceiver(stopReceiver, IntentFilter(stop))
        isReceiverRegistered = true
        val broadcastIntent = PendingIntent.getBroadcast(
            this, 0, Intent(stop), PendingIntent.FLAG_UPDATE_CURRENT
        )
//        val time = SimpleDateFormat("hh:mm").format(Calendar.getInstance().getTime())
        val notifTxt = "sending location"
        // Create the persistent notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(notifTxt)
//            .setOngoing(true)
            .setContentIntent(broadcastIntent)
            .setSmallIcon(R.drawable.ic_satelite)
        startForeground(1, builder.build())

        val delay = TimeUnit.SECONDS.toMillis(30)
        handler.postDelayed(runnableFinish120, delay)   // clear notif anyway
    }

    private var stopReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("received stop broadcast")
            // Stop the service when the notification is tapped
            finishService()
        }
    }

    companion object {
        const val EXTRA_TIMESTAMP = "extra_timestamp"
        fun newIntent(context: Context, groupId: String, timestampReceived: String?): Intent {
            val intent = Intent(context, TrackerService::class.java)
            intent.putExtra(EXTRA_GROUP_ID, groupId)
            if (timestampReceived != null)
                intent.putExtra(EXTRA_TIMESTAMP, timestampReceived)
            return intent
        }
    }

}
