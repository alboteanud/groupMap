package com.craiovadata.transportdisplay

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val db = FirebaseFirestore.getInstance()
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

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
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

    private fun subscribeToUpdates() {
        db.collection("data")
            .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
                if (e != null) {
                    Log.w(tag, "listen:error", e)
                    return@EventListener
                }
                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> setMarker(dc.document)
                        DocumentChange.Type.MODIFIED -> setMarker(dc.document)
                        DocumentChange.Type.REMOVED -> Log.d(tag, "Removed doc: ${dc.document.data}")
                    }
                }
            })
    }

    private fun setMarker(document: QueryDocumentSnapshot) {
        // When a location update is received, put or update
        // its value in mMarkers, which contains all the markers
        // for locations received, so that we can build the
        // boundaries required to show them all on the map at once
        val key = document.id
        val lat = document.data["latitude"] as Double
        val lng = document.data["longitude"] as Double
        val location = LatLng(lat, lng)
        if (!mMarkers.containsKey(key)) {
            val marker = mMap.addMarker(MarkerOptions().title(key).position(location))
            val iconUrl = document.data["provider"] as String
            setMarkerIcon(marker, iconUrl)
            mMarkers[key] = marker
        } else {
            mMarkers[key]?.position = location
        }
        val builder = LatLngBounds.Builder()
        for (marker in mMarkers.values) {
            builder.include(marker.position)
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300))

    }

    private fun setMarkerIcon(marker: Marker, iconUrl: String) {
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
                    val icon = BitmapDescriptorFactory.fromResource(R.mipmap.ic_test)
                    marker.setIcon(icon)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}

            })
    }


}
