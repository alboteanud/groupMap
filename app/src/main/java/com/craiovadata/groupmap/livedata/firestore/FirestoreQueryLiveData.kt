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

package com.craiovadata.groupmap.livedata.firestore

import com.google.firebase.firestore.*
import com.craiovadata.groupmap.common.DataOrException
import com.craiovadata.groupmap.config.AppExecutors
import com.craiovadata.groupmap.livedata.common.LingeringLiveData
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

typealias DocumentSnapshotsOrException = DataOrException<List<DocumentSnapshot>?, FirebaseFirestoreException?>

class FirestoreQueryLiveData(private val query: Query)
    : LingeringLiveData<DocumentSnapshotsOrException>(), EventListener<QuerySnapshot>, KoinComponent {

    private val executors by inject<AppExecutors>()

    private var listenerRegistration: ListenerRegistration? = null

    override fun beginLingering() {
        listenerRegistration = query.addSnapshotListener(
            executors.cpuExecutorService,
            MetadataChanges.INCLUDE,
            this)
    }

    override fun endLingering() {
        listenerRegistration?.remove()
    }

    override fun onEvent(snapshot: QuerySnapshot?, e: FirebaseFirestoreException?) {
        val documents = snapshot?.documents
        postValue(DocumentSnapshotsOrException(documents, e))
    }

}
