package com.craiovadata.groupmap.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.utils.*
import com.craiovadata.groupmap.utils.MapUtils.requestMyGpsLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firestore.v1.DocumentTransform
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*


class TrackerService : Service() {

    private val tag = TrackerService::class.java.simpleName

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val groupId = intent?.getStringExtra(GROUP_ID) ?: return super.onStartCommand(intent, flags, startId)
        buildNotification()
        requestMyGpsLocation(this) { location ->
            updateDB(location, groupId)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateDB(location: Location, groupId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val point = GeoPoint(location.latitude, location.longitude)

        FirebaseFirestore.getInstance().document("$GROUPS/$groupId/$USERS/$uid")
            .set(mapOf(LOCATION to point, "locationTimestamp" to com.google.firebase.Timestamp.now()), SetOptions.merge())
            .addOnCompleteListener {
                stopForeground(true)
                stopSelf()
            }
    }

    private fun buildNotification() {
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
        val broadcastIntent = PendingIntent.getBroadcast(
            this, 0, Intent(stop), PendingIntent.FLAG_UPDATE_CURRENT
        )
//        val time = SimpleDateFormat("hh:mm").format(Calendar.getInstance().getTime())
        val notifTxt = "getting location"
        // Create the persistent notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(notifTxt)
//            .setOngoing(true)
            .setContentIntent(broadcastIntent)
            .setSmallIcon(R.drawable.ic_satelite)
        startForeground(1, builder.build())
    }

    private var stopReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, "received stop broadcast")
            // Stop the service when the notification is tapped
            unregisterReceiver(this)
            stopForeground(true)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(stopReceiver)
    }


}
