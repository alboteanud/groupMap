package com.craiovadata.groupmap.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.Drawable
import android.location.Location
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.craiovadata.groupmap.viewmodel.UserMapDisplay
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.QueryDocumentSnapshot
import java.lang.IllegalStateException

object MapUtils {
    const val LOCATION_RECEIVED = 2

    fun requestMyGpsLocation(context: Context, callback: (state: Int, location: Location?) -> Unit) {
        val request = LocationRequest()
//        request.interval = 10000
//        request.fastestInterval = 5000
        request.numUpdates = 1
        request.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val client = LocationServices.getFusedLocationProviderClient(context)
//        val path = getString(R.string.firebase_path) + "/" + getString(R.string.transport_id)
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            callback.invoke(PackageManager.PERMISSION_GRANTED, null)
            // Request location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    val location = locationResult?.lastLocation
//                    if (location != null)
                    callback.invoke(LOCATION_RECEIVED, location)
                }
            }, null)
        } else {
            callback.invoke(PackageManager.PERMISSION_DENIED, null)
        }
    }

    @SuppressLint("MissingPermission")
    fun zoomOnMe(activity: Activity, map: GoogleMap?) {
        requestMyGpsLocation(activity) {permissionGranted, location ->
            if (location == null) return@requestMyGpsLocation
            val builder = LatLngBounds.Builder()
            builder.include(LatLng(location.latitude, location.longitude))
            map?.isMyLocationEnabled = true
            map?.uiSettings?.isMyLocationButtonEnabled = true
            map?.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 80))
        }
    }

    fun setMarker_orig(
        context: Context,
        userDoc: QueryDocumentSnapshot,
        markers: HashMap<String, Marker?>,
        map: GoogleMap?
    ) {
        // When a location update is received, put or update
        // its value in markers, which contains all the markers
        // for locations received, so that we can build the
        // boundaries required to show them all on the map at once
        val key = userDoc.id

        val geoPoint = (userDoc.data[LOCATION] as? GeoPoint) ?: return
        val location = LatLng(geoPoint.latitude, geoPoint.longitude)
        if (!markers.containsKey(key)) {
            val userName = userDoc.data[NAME] as? String ?: "?"
            val iconUrl = userDoc.data[PHOTO_URL] as? String
            val marker = map?.addMarker(
                MarkerOptions()
                    .title(userName)
                    .position(location)
            )
            marker?.tag = key
            setMarkerIcon(context, marker, iconUrl)
            markers[key] = marker
        } else {
            markers[key]?.position = location
        }
        val builder = LatLngBounds.Builder()
        for (marker in markers.values) {
            marker?.apply { builder.include(position) }
        }
        if (markers.isNotEmpty()) {
            try {
                map?.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 60))
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }

    private fun setMarkerIcon(context: Context, marker: Marker?, iconUrl: String?) {
        if (iconUrl == null) return

        Glide.with(context)
            .asBitmap()
            .load(iconUrl)
            .into(object : CustomTarget<Bitmap>(50, 50) {

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {

//                    val icon = BitmapDescriptorFactory.fromBitmap(resource)
                    if (marker?.tag != null) {
                        val bitmap = buildCustomIcon(resource)
                        val icon = BitmapDescriptorFactory.fromBitmap(bitmap)
                        marker.setIcon(icon)
                    }

                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
//                    val icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_person_pin)
//                    if (marker?.tag != null) {
//                        marker.setIcon(icon)
//                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {}

            })


    }

    private fun buildCustomIcon(resource: Bitmap): Bitmap {
        val bmp = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        val canvas1 = Canvas(bmp)
        val color = Paint()
        canvas1.drawBitmap(resource, 0f, 0f, color)
//        canvas1.drawText("Member Name!", 30f, 40f, color)
        return bmp
    }

    fun getCameraBounds(users: List<UserMapDisplay>): LatLngBounds? {
        if (users.isNullOrEmpty()) return null
        val builder = LatLngBounds.Builder()
        users.forEach { user ->
            if (user.position != null)
                builder.include(user.position)
        }
        return try {
            builder.build()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            null
        }
    }


}

