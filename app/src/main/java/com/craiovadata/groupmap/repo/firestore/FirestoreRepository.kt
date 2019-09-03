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

import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.craiovadata.groupmap.config.AppExecutors
import com.craiovadata.groupmap.livedata.firestore.FirestoreDocumentLiveData
import com.craiovadata.groupmap.livedata.firestore.FirestoreQueryLiveData
import com.craiovadata.groupmap.model.Group
import com.craiovadata.groupmap.repo.*
import com.craiovadata.groupmap.utils_.*
import com.craiovadata.groupmap.viewmodel.GroupSkDisplayQueryItem
import com.google.android.gms.common.api.Batch
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.iid.FirebaseInstanceId
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import java.lang.Exception
import java.lang.System.currentTimeMillis
import java.util.*
import java.util.concurrent.TimeUnit

class FirestoreRepository : Repository, KoinComponent {

    private val executors by inject<AppExecutors>()
    private val db by inject<FirebaseFirestore>()
    private val auth by inject<FirebaseAuth>()

    private val groupsLiveCollection = db.collection(GROUPS)
    private val usersLiveCollection = db.collection(USERS)

    private val userDeserializer = UserDocumentSnapshotDeserializer()
    private val groupDeserializer = GroupDocumentSnapshotDeserializer()

    private val listeningExecutor =
        MoreExecutors.listeningDecorator(executors.networkExecutorService)


    override fun getGroupLiveData(groupId: String): LiveData<GroupOrException> {
        val groupDocRef = groupsLiveCollection.document(groupId)

        // This LiveData is going to emit DocumentSnapshot objects.  We need
        // to transform those into User objects for the consumer.
        val documentLiveData = FirestoreDocumentLiveData(groupDocRef)

        // When a transformation is fast and can be executed on the main
        // thread, we can use Transformations.map().
        return Transformations.map(
            documentLiveData,
            DeserializeDocumentSnapshotTransform(groupDeserializer)
        )

        // But if a transformation is slow/blocking and shouldn't be executed
        // on the main thread, we can use Transformations.switchMap() with a
        // function that transforms on another thread and returns a LiveData.
//        return Transformations.switchMap(documentLiveData, AsyncDeserializingDocumentSnapshotTransform(userDeserializer))
    }

    override fun getUsersLiveData(groupId: String): LiveData<UsersQueryResults> {
        val refUsersCollection = groupsLiveCollection.document(groupId).collection(USERS)
        val query = refUsersCollection
//            .whereGreaterThan(TIMESTAMP, someTimeAgo)
        val queryLiveData = FirestoreQueryLiveData(query)
        return Transformations.map(
            queryLiveData,
            DeserializeDocumentSnapshotsTransform(userDeserializer)
        )
    }

    override fun getUsersMapLiveData(groupId: String): LiveData<UsersQueryResults> {
        val refUsersCollection = groupsLiveCollection.document(groupId).collection(USERS)
        val delay = TimeUnit.HOURS.toMillis(24)
        val someTimeAgo = Date(currentTimeMillis() - delay)
        // query for recently updated users
        val query = refUsersCollection
            .whereGreaterThan(TIMESTAMP, someTimeAgo)
        query.get().addOnSuccessListener {
            // use the result
        }
        val queryLiveData = FirestoreQueryLiveData(query)
        return Transformations.map(
            queryLiveData,
            DeserializeDocumentSnapshotsTransform(userDeserializer)
        )
    }

    override fun getMyGroupsLiveData(): LiveData<GroupsQueryResults> {
        val uid = auth.uid ?: return MutableLiveData<GroupsQueryResults>()
        val refGroupsCollection = usersLiveCollection.document(uid).collection(GROUPS)
        val query: Query = refGroupsCollection
        val queryLiveData = FirestoreQueryLiveData(query)
        return Transformations.map(
            queryLiveData,
            DeserializeDocumentSnapshotsTransform(groupDeserializer)
        )
    }

    override fun getGroups(groupShareKey: String): LiveData<GroupsQueryResults> {
//        if (groupShareKey == null) return MutableLiveData<GroupsSkQueryResults>()
//        val query = db.collection(DB_GROUP_SHARE_KEYS)
        val query = db.collection(GROUPS)
            .whereEqualTo(GROUP_SHARE_KEY, groupShareKey)
        val queryLiveData = FirestoreQueryLiveData(query)
        return Transformations.map(
            queryLiveData,
            DeserializeDocumentSnapshotsTransform(groupDeserializer)
        )
    }

    override fun syncGroup(
        groupId: String,
        timeout: Long,
        unit: TimeUnit
    ): ListenableFuture<Repository.SyncResult> {
        val stockDocRef = groupsLiveCollection.document(groupId)
        val callable = DocumentSyncCallable(stockDocRef, timeout, unit)
        return listeningExecutor.submit(callable)
    }

    override fun setNewGroup(groupName: String, callback: (groupId: GroupIdOrException) -> Unit) {
        val uid = auth.uid ?: return
        val groupId = groupsLiveCollection.document().id
        val shareKey = groupsLiveCollection.document().id
        val group = Group(groupName, shareKey)

        val batch = db.batch()
        batch.set(db.document("$GROUPS/$groupId"), group)
        batch.set(db.document("$GROUPS/$groupId/$USERS/$uid"), hashMapOf(ROLE to ROLE_SUPER_ADMIN))
        batch.set(db.document("$USERS/$uid/$GROUPS/$groupId"), hashMapOf(NAME to groupName))
        batch.commit().addOnCompleteListener { task ->
            if (task.isSuccessful) {
//                saveGroupIdToPref(uid)
                callback.invoke(
                    GroupIdOrException(groupId, null)
                )
            } else {
                callback.invoke(
                    GroupIdOrException(null, task.exception)
                )
            }
        }
    }

    override fun requestPositionUpdates(groupId: String?) {
        if (groupId == null) return
        val user = auth.currentUser ?: return
        val name =
            if (!user.displayName.isNullOrEmpty()) user.displayName
            else user.email
        val ref = db.document("$REQUESTS/$groupId")

        val data = hashMapOf(
            TIMESTAMP to FieldValue.serverTimestamp(),
            UID to user.uid,
            NAME to name
        )
        ref.set(data).addOnSuccessListener {
            Timber.d("success sending update request")
        }
    }

    override fun sendMyPosition(
        groupId: String,
        location: Location,
        callback: (se: SuccessOrException) -> Unit
    ) {
        val uid = auth.uid ?: return
        val point = GeoPoint(location.latitude, location.longitude)
        val dateNow = Timestamp.now()
        val data = mapOf(
            TIMESTAMP to dateNow,
            LOCATION to point
        )
        val ref = db.document("$GROUPS/$groupId/$USERS/$uid")
        ref.update(data)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Timber.d("success sending my position")
                    callback.invoke(SuccessOrException("success sending my position", null))
                } else {
                    callback.invoke(SuccessOrException(null, it.exception))
                }
            }
    }

    override fun deleteToken(sentUid: String) {
        val ref = usersLiveCollection.document(sentUid)
        val updates = hashMapOf<String, Any>(
            TOKEN to FieldValue.delete()
        )
        ref.update(updates)
    }

    override fun exitGroup(groupId: String) {
        val uid = auth.uid ?: return
//        db.document("$USERS/$uid/$GROUPS/$uid").delete()
        db.document("$GROUPS/$groupId/$USERS/$uid").delete()
        // the cloud function will take care to delete the token and the group from User
    }

    override fun sendTokenToServer(token: String?) {
        val uid = auth.uid ?: return
        val ref = db.document("$USERS/$uid")
        if (token != null) {
            ref.set(hashMapOf(TOKEN to token))
        } else {
            // on login
            FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { result ->
                ref.set(mapOf(TOKEN to result.token))
            }
        }
    }

    override fun joinGroup(
        group: GroupSkDisplayQueryItem?,
        callback: (se: SuccessOrException) -> Unit
    ) {
        val uid = auth.uid ?: return
        val groupId = group?.id ?: return
        val groupName = group.item.groupName
        db.document("$USERS/$uid/$GROUPS/$groupId")
            .set(hashMapOf(NAME to groupName)) // will trigger a cloud function(3) to update the Group with userData
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    callback.invoke(SuccessOrException(groupId, null))
                } else {
                    callback.invoke(SuccessOrException(null, it.exception))
                }
            }
    }

    override fun removeUserFromGroup(uid: String, groupId: String) {
        db.document("$GROUPS/$groupId/$USERS/$uid").delete()
        // this will trigger a function to clear token and group in user
    }

    override fun dissmissAsAdmin(uid: String, groupId: String) {
        val data = hashMapOf<String, Any>(ROLE to FieldValue.delete())
        db.document("$GROUPS/$groupId/$USERS/$uid").update(data)
    }

    override fun makeGroupAdmin(uid: String, groupId: String) {
        val data = hashMapOf<String, Any>(ROLE to ROLE_ADMIN)
        db.document("$GROUPS/$groupId/$USERS/$uid").update(data)
    }

    override fun changeGroupName(groupId: String, groupName: String) {
        val ref = db.document("$GROUPS/$groupId")
        ref.update(NAME, groupName).addOnSuccessListener {
            Timber.d("changed group name success")
        }
    }

    override fun setPauseLocationUpdates(groupId: String) {
        val uid = auth.uid ?: return
        val refTk = db.document("$GROUPS/$groupId/$TOKENS/$uid")
        val refUsrGr = db.document("$GROUPS/$groupId/$USERS/$uid")
        val refUsr = db.document("$USERS/$uid/$GROUPS/$groupId")

        val batch = db.batch()
        batch.delete(refTk)
        batch.set(refUsrGr, hashMapOf(PAUSE to true), SetOptions.merge())
        batch.set(refUsr, hashMapOf(PAUSE to true), SetOptions.merge())
        batch.commit().addOnSuccessListener {
            Timber.d("set pause update location - success")
        }
    }

    override fun allowLocationUpdates(groupId: String) {
        val uid = auth.uid ?: return

        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { result ->
            val refTk = db.document("$GROUPS/$groupId/$TOKENS/$uid")
            val refUsrGr = db.document("$GROUPS/$groupId/$USERS/$uid")
            val refUsr = db.document("$USERS/$uid/$GROUPS/$groupId")

            val batch = db.batch()
            batch.set(refTk, mapOf(TOKEN to result.token))
            batch.update(refUsrGr, PAUSE, FieldValue.delete())
            batch.update(refUsr, PAUSE, FieldValue.delete())
            batch.commit().addOnSuccessListener {
                Timber.d("set allow update location - success")
            }
        }


    }

}
