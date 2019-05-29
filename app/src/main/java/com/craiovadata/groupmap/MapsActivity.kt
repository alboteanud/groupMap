package com.craiovadata.groupmap

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.craiovadata.groupmap.CreateGroupActivity.Companion.KEY_GROUP_ID
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.iid.FirebaseInstanceId


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val mMarkers = hashMapOf<String, Marker>()
    private lateinit var groupId: String
//    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        auth.addAuthStateListener { auth ->
            Toast.makeText(this, "User: " + auth.currentUser?.email, Toast.LENGTH_SHORT).show()
            if (auth.currentUser != null) {
                saveMessagingDeviceToken()
            } else {
                // todo NOT_AUTH but needs write permission
                deleteMessagingDeviceToken()
            }
        }
    }

    private fun saveMessagingDeviceToken() {
        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { result ->
            val currentUser = auth.currentUser ?: return@addOnSuccessListener
            val token = result.token
            val ref = db.collection(FCM_TOKENS).document(token)
            val userData = HashMap<String, Any?>()
            userData["uid"] = currentUser.uid
            ref.set(userData)
        }
    }

    private fun deleteMessagingDeviceToken() {
        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { result ->
            val token = result.token
            val ref = db.collection(FCM_TOKENS).document(token)
            ref.delete()
        }
    }

    override fun onStart() {
        super.onStart()
        groupId = getSharedPreferences("_", MODE_PRIVATE).getString(KEY_GROUP_ID,
            defaultGroupId
        ) ?: defaultGroupId
        checkLocationPermission()
        subscribeToGroupUpdates(groupId)
    }

    private fun requestPositionUpdates() {
        val currentUser = auth.currentUser

      if (currentUser!=null){
          // Update one field, creating the document if it does not already exist.
          val requestData = HashMap<String, Any?>()
          requestData["uid"] = currentUser.uid
          requestData["name"] = currentUser.displayName ?: currentUser.email
          requestData["time"] = FieldValue.serverTimestamp()
          val request = HashMap<String, Any>()
          request[UPDATE_REQUEST] = requestData

          db.collection(GROUPS).document(groupId).set(request, SetOptions.merge())
      } else {
          Toast.makeText(this, "Login to get positions", Toast.LENGTH_SHORT).show()
      }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMaxZoomPreference(16f)
//        loginToFirebase()
        subscribeToGroupUpdates(groupId)
        requestMyLocationUpdates()
        enableMyLocationOnMap()
    }

    private fun enableMyLocationOnMap() {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permission == PackageManager.PERMISSION_GRANTED && ::mMap.isInitialized) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings?.isMyLocationButtonEnabled = true
        }
    }

    // Check location permission is granted - if it is, start
    // the service, otherwise request the permission
    private fun checkLocationPermission() {
        // Check GPS is enabled
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show()
            buildAlertMessageNoGps()
            return
        }

        // Check location permission is granted - if it is, start
        // the service, otherwise request the permission
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            enableMyLocationOnMap()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST && grantResults.size == 1
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Start the service when the permission is granted
            enableMyLocationOnMap()
        } else {
            // permission not granted
//            finish()
        }
    }

    private fun setMarker(document: QueryDocumentSnapshot) {
        // When a location update is received, put or update
        // its value in mMarkers, which contains all the markers
        // for locations received, so that we can build the
        // boundaries required to show them all on the map at once
        val key = document.id
        var locationData = document.data["location"] ?: return
        locationData = locationData as HashMap<*, *>
        val lat = locationData["latitude"] as Double
        val lng = locationData["longitude"] as Double
        val location = LatLng(lat, lng)
        if (!mMarkers.containsKey(key)) {
            val userName = document.data["name"] as String
            val marker = mMap.addMarker(MarkerOptions().title(userName).position(location))
            val iconUrl = document.data["photoUrl"]?.toString()
            setMarkerIcon(marker, iconUrl)
            mMarkers[key] = marker
        } else {
            mMarkers[key]?.position = location
        }
        val builder = LatLngBounds.Builder()
        for (marker in mMarkers.values) {
            builder.include(marker.position)
        }
        if (mMarkers.isNotEmpty())
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 8))
    }

    private fun setMarkerIcon(marker: Marker, iconUrl: String?) {
        if (iconUrl == null) return
        Glide.with(applicationContext)
            .asBitmap()
            .load(iconUrl)
            .into(object : CustomTarget<Bitmap>() {

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val icon = BitmapDescriptorFactory.fromBitmap(resource)
                    marker.setIcon(icon)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    val icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_person_pin)
                    marker.setIcon(icon)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}

            })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
//                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            R.id.action_create_group -> {
//                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, CreateGroupActivity::class.java))
                return true
            }
            R.id.action_refresh -> {
                requestPositionUpdates()
                return true
            }
            R.id.action_join_group_x -> {
                joinGroup(groupId)
                return true
            }
            R.id.action_leave_group -> {
                leaveGroupX(groupId)
                return true
            }
            R.id.action_logout -> {
                auth.signOut()
                return true
            }
            R.id.action_login -> {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    startLoginActivity()
                } else {
                    Toast.makeText(this, "You are already logged in", Toast.LENGTH_SHORT).show()
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun leaveGroupX(groupId: String) {
        val currentUser = auth.currentUser ?: return
        val ref = db.collection(USERS).document(currentUser.uid)
            .collection(GROUPS).document(groupId)
//        ref.delete()
        val data = HashMap<String, Any?>()
        data[JOINED]= false
        ref.set(data)
    }

    private fun startLoginActivity() {
        val providers =
            arrayListOf(AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build())
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(true)
                .setLogo(R.drawable.ic_person_pin)
//                        .setAlwaysShowSignInMethodScreen(true)
                .build(), RC_SIGN_IN
        )
    }

    // sign in result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
//                val currentUser = auth.currentUser
//                joinGroup(groupId)
//                FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
//                    val token = it.token
//                    Util.sendRegistrationToServer(currentUser, token)
//                }
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                if (response == null) {
                    // User pressed the back button.
                    return
                }
                if (response.error?.errorCode == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                    return
                }

                if (response.error?.errorCode == ErrorCodes.UNKNOWN_ERROR) {
                    Toast.makeText(this, getString(R.string.error_default), Toast.LENGTH_SHORT).show();
                    return
                }
            }
        }
    }

    private fun joinGroup(groupId: String) {
        val currentUser = auth.currentUser ?: return

        val ref = db.collection(USERS).document(currentUser.uid)
            .collection(GROUPS).document(groupId)
//        val data = Util.getUserData_(currentUser)
        val data = HashMap<String, Any?>()
        data[JOINED]= true

        ref.set(data).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "You joined the group", Toast.LENGTH_SHORT).show()
                subscribeToGroupUpdates(groupId)
//                requestMyLocationUpdates()
            } else {
                val msg = getString(R.string.toast_group_creation_error)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun subscribeToGroupUpdates(groupId: String) {
        if (!::mMap.isInitialized) return
        mMap.clear()
        setActivityTitle(groupId)
        // add other people on the map
        db.collection(GROUPS).document(groupId).collection(DEVICES)
            .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@EventListener
                }
                if (snapshots == null) return@EventListener
                for (dc in snapshots.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> setMarker(dc.document)
                        DocumentChange.Type.MODIFIED -> setMarker(dc.document)
                        DocumentChange.Type.REMOVED -> Log.d(TAG, "Removed doc: ${dc.document.data}")
                    }
                }
            })
    }

    private fun setActivityTitle(groupId: String) {
        db.collection(GROUPS).document(groupId).get()
            .addOnSuccessListener { documentSnapshot ->
                documentSnapshot?.data?.let { groupData ->
                    val groupName = groupData["groupName"] as String?
                    groupName?.let { title = groupName }
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }

    private fun requestMyLocationUpdates() {
        val request = LocationRequest()
        request.numUpdates = 5
        request.interval = 10000
        request.fastestInterval = 5000
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
                }
            }, null)
        }
    }

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this);
        builder.setMessage("Your GPS is disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ -> startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            .setNegativeButton("No") { dialog, _ -> dialog.cancel(); }
        val alert = builder.create();
        alert.show();
    }

    companion object {
        private val TAG = MapsActivity::class.java.simpleName
        private const val PERMISSIONS_REQUEST = 1
        private const val RC_SIGN_IN = 9001
        const val DEVICES = "devices"
        const val GROUPS = "groups"
        const val FCM_TOKENS = "fcmTokens"
        const val JOINED = "joined"
        const val USERS = "users"
        const val defaultGroupId: String = "defaultGroupId"
        const val UPDATE_REQUEST = "updateRequest";
    }


}
