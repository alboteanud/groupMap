package com.craiovadata.groupmap.utils

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.craiovadata.groupmap.R
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.QueryDocumentSnapshot
import java.lang.IllegalStateException

object MapUtils {

    // Check location permission is granted - if it is, start
    // the service, otherwise request the permission
    fun checkLocationPermission(activity: Activity, callback: () -> Unit) {
        // Check GPS is enabled
        val lm = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show()
            Util.buildAlertMessageNoGps(activity)
            return
        }

        // Check location permission is granted - if it is, start
        // the service, otherwise request the permission
        val permission = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            callback.invoke()
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST
            )
        }
    }

    fun requestMyGpsLocation(context: Context, callback: (location: Location) -> Unit) {
        val request = LocationRequest()
//        request.interval = 10000
//        request.fastestInterval = 5000
        request.numUpdates = 1
        request.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val client = LocationServices.getFusedLocationProviderClient(context)
//        val path = getString(R.string.firebase_path) + "/" + getString(R.string.transport_id)
        val permission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permission == PackageManager.PERMISSION_GRANTED) {
            // Request location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    val location = locationResult?.lastLocation
                    if (location != null)
                        callback.invoke(location)
                }
            }, null)
        }
    }

    fun zoomOnMe(activity: Activity, map: GoogleMap?) {
        checkLocationPermission(activity){
            map?.isMyLocationEnabled = true
            map?.uiSettings?.isMyLocationButtonEnabled = true
            requestMyGpsLocation(activity) { location ->
                val builder = LatLngBounds.Builder()
                builder.include(LatLng(location.latitude, location.longitude))
                map?.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 80))
            }
        }
    }

    fun setMarker(
        context: Context,
        document: QueryDocumentSnapshot,
        mask: Boolean,
        mMarkers: HashMap<String, Marker?>,
        mMap: GoogleMap?
    ) {
        // When a location update is received, put or update
        // its value in mMarkers, which contains all the markers
        // for locations received, so that we can build the
        // boundaries required to show them all on the map at once
        val key = document.id

        val locationData = (document.data[LOCATION] as? HashMap<String, Any?>) ?: return
        val lat = locationData[LATITUDE] as? Double
        val lng = locationData[LONGITUDE] as? Double
        if (lat == null || lng == null) return
        val location = LatLng(lat, lng)
        if (!mMarkers.containsKey(key)) {
            var userName = document.data[NAME] as String
            var iconUrl = document.data[PHOTO_URL]?.toString()
            if (mask) {
                userName = "?"
                iconUrl = null
            }
            val marker = mMap?.addMarker(MarkerOptions().title(userName).position(location))

            setMarkerIcon(context, marker, iconUrl)
            mMarkers[key] = marker
        } else {
            mMarkers[key]?.position = location
        }
        val builder = LatLngBounds.Builder()
        for (marker in mMarkers.values) {
            marker?.apply { builder.include(position) }
        }
        if (mMarkers.isNotEmpty()) {
            val padding = 80
            try {
                mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), padding))
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }

    }

    fun setMarkerIcon(context: Context?, marker: Marker?, iconUrl: String?) {
        if (iconUrl == null) return
        if (marker == null) return
        if (context == null) return


        Glide.with(context)
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


}