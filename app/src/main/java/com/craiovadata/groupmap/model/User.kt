package com.craiovadata.groupmap.model

import com.craiovadata.groupmap.utils_.LOCATION
import com.craiovadata.groupmap.utils_.NAME
import com.craiovadata.groupmap.utils_.PHOTO_URL
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.clustering.ClusterItem

data class User(val userDoc: DocumentSnapshot) : ClusterItem {

    var photoUrl = userDoc[PHOTO_URL] as? String
    var name = userDoc[NAME] as? String
    val id = userDoc.id

    override fun getPosition(): LatLng? {
        val geoPoint = userDoc[LOCATION] as? GeoPoint ?: return null
        return LatLng(geoPoint.latitude, geoPoint.longitude)
    }

    override fun getSnippet(): String? {
        return null
    }

    override fun getTitle(): String? {
        return name
    }
}