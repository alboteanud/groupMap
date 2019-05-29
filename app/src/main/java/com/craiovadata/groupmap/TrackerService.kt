package com.craiovadata.groupmap

//import com.google.firebase.database.FirebaseDatabase
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
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.iid.FirebaseInstanceId


class TrackerService : Service() {

    private val tag = TrackerService::class.java.simpleName
    var user: FirebaseUser? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        buildNotification()
//        loginToFirebase()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            if (it.hasExtra("groupId")) {
                val groupId = it.getStringExtra("groupId")
                val currentUser = FirebaseAuth.getInstance().currentUser ?: return@let

                // get registration token
                FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { result ->
                    val token = result.token
//                    val ref = db.collection(MapsActivity.FCM_TOKENS).document(token)
//                    val userData = HashMap<String, Any?>()
//                    userData["uid"] = currentUser.uid
//                    ref.set(userData)

                    requestLocationUpdates(groupId, token)
                }


            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun buildNotification() {
        val CHANNEL_ID = "my_channel_01"
        if (Build.VERSION.SDK_INT >= 26) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }

        val stop = "stop"
        registerReceiver(stopReceiver, IntentFilter(stop))
        val broadcastIntent = PendingIntent.getBroadcast(
            this, 0, Intent(stop), PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Create the persistent notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .setContentIntent(broadcastIntent)
            .setSmallIcon(R.drawable.ic_tracker);
        startForeground(1, builder.build())


    }

    private var stopReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, "received stop broadcast")
            // Stop the service when the notification is tapped
            unregisterReceiver(this)
            stopSelf()
        }
    }



    private fun loginToFirebase() {
        // Authenticate with Firebase, and request location updates
//        val email = getString(R.string.firebase_email)
//        val password = getString(R.string.firebase_password)
//        FirebaseAuth.getInstance().signInWithEmailAndPassword(
//            email, password
//        ).addOnCompleteListener { task ->
//            if (task.isSuccessful) {
//                Log.d(tag, "firebase auth success")
//                requestLocationUpdates()
//            } else {
//                Log.d(tag, "firebase auth failed")
//            }
//        }
        user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
//            requestLocationUpdates(groupId, token)
        }

    }

    private fun requestLocationUpdates(groupId: String, token: String) {
        val request = LocationRequest()
        request.interval = 10000
        request.fastestInterval = 5000
        request.numUpdates = 2
        request.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val client = LocationServices.getFusedLocationProviderClient(this)
//        val path = getString(R.string.firebase_path) + "/" + getString(R.string.transport_id)
        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permission == PackageManager.PERMISSION_GRANTED) {
            // Request location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
//                    val ref = FirebaseDatabase.getInstance().getReference(path)
                    val location = locationResult!!.lastLocation
//                    location.provider = user?.photoUrl.toString()
                    if (location != null) {
                        Log.d(tag, "location update $location")
//                        ref.setValue(location)
                        val locationData = HashMap<String, Any?>()
                        locationData["location"] = location
                        val db = FirebaseFirestore.getInstance()
                        db.collection("groups").document(groupId).collection("devices")
                            .document(token)
                            .set(locationData, SetOptions.merge())
//                        stopSelf()
                    }


                }
            }, null)
        }
    }


}
