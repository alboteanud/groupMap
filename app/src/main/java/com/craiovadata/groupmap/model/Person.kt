package com.craiovadata.groupmap.model

import com.craiovadata.groupmap.utils.LOCATION
import com.craiovadata.groupmap.utils.NAME
import com.craiovadata.groupmap.utils.PHOTO_URL
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.maps.android.clustering.ClusterItem

data class Person(val userDoc: QueryDocumentSnapshot) : ClusterItem {

    var photoUrl = userDoc.data[PHOTO_URL] as? String
    var name = userDoc.data[NAME] as? String ?: "?"
    val id = userDoc.id

    override fun getPosition(): LatLng? {
        val geoPoint = (userDoc.data[LOCATION] as? GeoPoint) ?: return null
        return LatLng(geoPoint.latitude, geoPoint.longitude)

    }

    override fun getSnippet(): String? {
        return null
    }

    override fun getTitle(): String? {
        return null
    }


}