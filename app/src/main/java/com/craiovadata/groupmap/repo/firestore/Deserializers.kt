package com.craiovadata.groupmap.repo.firestore

import com.craiovadata.groupmap.model.Group
import com.craiovadata.groupmap.model.User
import com.google.firebase.firestore.DocumentSnapshot
import com.craiovadata.groupmap.repo.Deserializer
import com.craiovadata.groupmap.utils_.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint

internal interface DocumentSnapshotDeserializer<T> : Deserializer<DocumentSnapshot, T>

internal class UserDocumentSnapshotDeserializer : DocumentSnapshotDeserializer<User> {
    override fun deserialize(input: DocumentSnapshot): User {
//        val id = input.id
        val name = input.getString(NAME)
            ?: throw Deserializer.DeserializerException("User was missing for NAME role document ${input.id}")
        val photoUrl = input.getString(PHOTO_URL)
        val role = input.getDouble(ROLE)
        val location = input.getGeoPoint(LOCATION)
        val locationTimestamp = input.getDate(TIMESTAMP)
        val pauseLocationUpdate = input.getBoolean(PAUSE)

        return User(name, photoUrl, role?.toInt(), getLatLng(location), locationTimestamp, pauseLocationUpdate)
    }

    private fun getLatLng(point: GeoPoint?): LatLng? {
        if (point == null) return null
        return LatLng(point.latitude, point.longitude)
    }
}

internal class GroupDocumentSnapshotDeserializer : DocumentSnapshotDeserializer<Group> {
    override fun deserialize(input: DocumentSnapshot): Group {
//        val id = input.id
        val groupName = input.getString(NAME)
            ?: throw Deserializer.DeserializerException("Group was missing for NAME role document ${input.id}")
        val shareKey = input.getString(GROUP_SHARE_KEY)

        return Group(groupName, shareKey)
    }
}

