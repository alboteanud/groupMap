package com.craiovadata.groupmap.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap

object MapUtils {

    // Check location permission is granted - if it is, start
    // the service, otherwise request the permission
    fun checkLocationPermission(activity: Activity, map: GoogleMap?) {
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
            enableMyLocationOnMap(activity, map)
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST
            )
        }
    }

    fun enableMyLocationOnMap(context: Context, map: GoogleMap?) {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            map?.isMyLocationEnabled = true
            map?.uiSettings?.isMyLocationButtonEnabled = true
        }
    }

    fun requestMyLocationUpdates(context: Context) {
        val request = LocationRequest()
        request.numUpdates = 5
        request.interval = 10000
        request.fastestInterval = 5000
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
                }
            }, null)
        }
    }
}