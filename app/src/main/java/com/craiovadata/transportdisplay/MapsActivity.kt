package com.craiovadata.transportdisplay

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private val tag = MapsActivity::class.java.simpleName
    private val mMarkers = hashMapOf<String, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
//        val sydney = LatLng(-34.0, 151.0)
//        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

        mMap.setMaxZoomPreference(16f)
        loginToFirebase()
    }

    private fun loginToFirebase() {
        val email = getString(R.string.firebase_email)
        val password = getString(R.string.firebase_password)
        // Authenticate with Firebase and subscribe to updates
        FirebaseAuth.getInstance().signInWithEmailAndPassword(
            email, password
        ).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                subscribeToUpdates()
                Log.d(tag, "firebase auth success")
            } else {
                Log.d(tag, "firebase auth failed")
            }
        }
    }

    // Access a Cloud Firestore instance from your Activity
    val db = FirebaseFirestore.getInstance()

    private fun subscribeToUpdates() {

        db.collection("data")
            .get()
            .addOnSuccessListener { result ->

                setMarker(result)
            }
            .addOnFailureListener { exception ->
                Log.w(tag, "Error getting documents.", exception)
            }

    }

    private fun setMarker(querySnapshot: QuerySnapshot) {
        for (document in querySnapshot) {
        Log.d(tag, "${document.id} => ${document.data}")

            // When a location update is received, put or update
            // its value in mMarkers, which contains all the markers
            // for locations received, so that we can build the
            // boundaries required to show them all on the map at once
            val key = document.id
            val value = document.data
            val lat = java.lang.Double.parseDouble(value["latitude"].toString())
            val lng = java.lang.Double.parseDouble(value["longitude"].toString())
            val location = LatLng(lat, lng)
            if (!mMarkers.containsKey(key)) {
                val marker = mMap.addMarker(MarkerOptions().title(key).position(location))
                mMarkers[key] = marker
            } else {
                mMarkers[key]?.position = location
            }

    }

        val builder = LatLngBounds.Builder()
        for (marker in mMarkers.values) {
            builder.include(marker.position)
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300))
    }

}
