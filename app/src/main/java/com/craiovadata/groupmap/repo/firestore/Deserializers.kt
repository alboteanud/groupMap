/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        val locationTimestamp = input.getDate(LOCATION_TIMESTAMP)

        return User( name, photoUrl, role?.toInt(), getLatLng(location), locationTimestamp)
    }

    private fun getLatLng(point: GeoPoint?): LatLng? {
        if (point == null) return null
        return LatLng(point.latitude, point.longitude)
    }
}

internal class GroupDocumentSnapshotDeserializer : DocumentSnapshotDeserializer<Group> {
    override fun deserialize(input: DocumentSnapshot): Group {
//        val id = input.id
        val groupName = input.getString(GROUP_NAME)
            ?: throw Deserializer.DeserializerException("Group was missing for GROUP_NAME role document ${input.id}")
        val shareKey = input.getString(GROUP_SHARE_KEY)

        return Group( groupName, shareKey)
    }
}
