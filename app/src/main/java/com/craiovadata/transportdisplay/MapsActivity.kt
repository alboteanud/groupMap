package com.craiovadata.transportdisplay

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
import com.firebase.ui.auth.AuthUI
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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val db = FirebaseFirestore.getInstance()
    private val mMarkers = hashMapOf<String, Marker>()
    lateinit var groupId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onStart() {
        super.onStart()
        groupId = getSharedPreferences("_", Context.MODE_PRIVATE).getString("groupId", "bfpirzoq2Fvl9rMHndzZ")
        checkLocationPermission()
        subscribeToGroupUpdates()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMaxZoomPreference(16f)
//        loginToFirebase()
        subscribeToGroupUpdates()
        requestLocationUpdates()
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
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300))


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
            R.id.action_join_group_x -> {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    startLoginActivity()
                } else {
                    joinGroup(currentUser)
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = FirebaseAuth.getInstance().currentUser
                joinGroup(user)
                // ...
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }

    private fun joinGroup(currentUser: FirebaseUser?) {
        if (currentUser == null) return

        val user = HashMap<String, Any?>()
        user["name"] = currentUser.displayName
        user["uid"] = currentUser.uid
        user["photoUrl"] = currentUser.photoUrl?.toString()
        user["email"] = currentUser.email

        val db = FirebaseFirestore.getInstance()
        val groupRef = db.collection("groups").document(groupId)
        val memberRef = groupRef.collection("members").document(currentUser.uid)

        val batch = db.batch()
        batch.set(memberRef, user)
        batch.commit().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "DocumentSnapshot written with ID: ${groupRef.id}")
                Toast.makeText(this, "You joined the group", Toast.LENGTH_SHORT).show()
                subscribeToGroupUpdates()
                requestLocationUpdates()
            } else {
                Log.w(TAG, "Error adding document", task.exception)
                val msg = getString(R.string.toast_group_creation_error)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun subscribeToGroupUpdates() {
        if (!::mMap.isInitialized) return
        mMap.clear()

//            getPreferences(Context.MODE_PRIVATE)
        setActivityTitle(groupId)
        // add other people on the map
        db.collection("groups").document(groupId).collection("members")
            .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@EventListener
                }
                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> setMarker(dc.document)
                        DocumentChange.Type.MODIFIED -> setMarker(dc.document)
                        DocumentChange.Type.REMOVED -> Log.d(TAG, "Removed doc: ${dc.document.data}")
                    }
                }
            })


    }

    private fun setActivityTitle(groupId: String?) {
        groupId?.let { id ->
            db.collection("groups").document(id).get()
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
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest()
        request.numUpdates = 2
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
//                    val ref = FirebaseDatabase.getInstance().getReference(path)
                    val location = locationResult!!.lastLocation
                    FirebaseAuth.getInstance().currentUser?.let { user ->
                        location.provider = user.photoUrl.toString()
                        if (location != null) {
                            Log.d(TAG, "location update $location")
//                        ref.setValue(location)
                            db.collection("groups")
                                .document(groupId).collection("members").document(user.uid)
                                .update("location", location)
                        }
                    }


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
        private val PERMISSIONS_REQUEST = 1
        private val RC_SIGN_IN = 9001
    }


}
