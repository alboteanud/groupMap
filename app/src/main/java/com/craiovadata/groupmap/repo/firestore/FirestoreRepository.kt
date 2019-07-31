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

import android.text.format.Time
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.craiovadata.groupmap.config.AppExecutors
import com.craiovadata.groupmap.livedata.firestore.FirestoreDocumentLiveData
import com.craiovadata.groupmap.livedata.firestore.FirestoreQueryLiveData
import com.craiovadata.groupmap.model.User
import com.craiovadata.groupmap.repo.*
import com.craiovadata.groupmap.utils_.*
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.lang.System.currentTimeMillis
import java.util.*
import java.util.concurrent.TimeUnit

class FirestoreRepository : BaseRepository(), KoinComponent {

    private val executors by inject<AppExecutors>()
    private val firestore by inject<FirebaseFirestore>()

    private val groupsLiveCollection = firestore.collection(GROUPS)

    private val userDeserializer = UserDocumentSnapshotDeserializer()
    private val groupDeserializer = GroupDocumentSnapshotDeserializer()

    private val listeningExecutor = MoreExecutors.listeningDecorator(executors.networkExecutorService)

    override fun getGroupLiveData(groupId: String): LiveData<GroupOrException> {
        val groupDocRef = groupsLiveCollection.document(groupId)

        // This LiveData is going to emit DocumentSnapshot objects.  We need
        // to transform those into User objects for the consumer.
        val documentLiveData = FirestoreDocumentLiveData(groupDocRef)

        // When a transformation is fast and can be executed on the main
        // thread, we can use Transformations.map().
        return Transformations.map(documentLiveData, DeserializeDocumentSnapshotTransform(groupDeserializer))

        // But if a transformation is slow/blocking and shouldn't be executed
        // on the main thread, we can use Transformations.switchMap() with a
        // function that transforms on another thread and returns a LiveData.
//        return Transformations.switchMap(documentLiveData, AsyncDeserializingDocumentSnapshotTransform(userDeserializer))
    }

    override fun getUsersLiveData(groupId: String): LiveData<UsersQueryResults> {
        val refUsersCollection = groupsLiveCollection.document(groupId).collection(USERS)
        val dateRecent = Date(currentTimeMillis() - TimeUnit.HOURS.toMillis(1))
        // query for recently updated users
        val query = refUsersCollection.whereGreaterThan(LOCATION_TIMESTAMP, dateRecent)
        val queryLiveData = FirestoreQueryLiveData(query)
        return Transformations.map(queryLiveData, DeserializeDocumentSnapshotsTransform(userDeserializer))
    }

    override fun getUserPagedListLiveData(pageSize: Int): LiveData<PagedList<QueryItemOrException<User>>> {
        val query = groupsLiveCollection.orderBy(FieldPath.documentId())
        val dataSourceFactory = FirestoreQueryDataSource.Factory(query, Source.DEFAULT)
        val deserializedDataSourceFactory = dataSourceFactory.map { snapshot ->
            try {
                val item = UserQueryItem(userDeserializer.deserialize(snapshot), snapshot.id)
                QueryItemOrException(item, null)
            } catch (e: Exception) {
                QueryItemOrException<User>(null, e)
            }
        }

        return LivePagedListBuilder(deserializedDataSourceFactory, pageSize)
            .setFetchExecutor(executors.networkExecutorService)
            .build()
    }

    override fun syncGroup(groupId: String, timeout: Long, unit: TimeUnit): ListenableFuture<Repository.SyncResult> {
        val stockDocRef = groupsLiveCollection.document(groupId)
        val callable = DocumentSyncCallable(stockDocRef, timeout, unit)
        return listeningExecutor.submit(callable)
    }

}
