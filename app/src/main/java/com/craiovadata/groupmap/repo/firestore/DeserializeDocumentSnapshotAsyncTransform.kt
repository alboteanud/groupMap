/*
 * Copyright 2019 Google LLC
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

import androidx.arch.core.util.Function
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.craiovadata.groupmap.common.DataOrException
import com.craiovadata.groupmap.config.AppExecutors
import com.craiovadata.groupmap.livedata.firestore.DocumentSnapshotOrException
import com.craiovadata.groupmap.repo.Deserializer
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

internal class DeserializeDocumentSnapshotAsyncTransform<T>(
    private val deserializer: DocumentSnapshotDeserializer<T>
) : Function<DocumentSnapshotOrException, LiveData<DataOrException<T, Exception>>>, KoinComponent {

    private val executors by inject<AppExecutors>()

    override fun apply(input: DocumentSnapshotOrException): LiveData<DataOrException<T, Exception>> {
        val (snapshot, exception) = input
        val liveData = MutableLiveData<DataOrException<T, Exception>>()
        if (snapshot != null) {
            // Do this in a thread when DataSnapshot deserialization
            // could be costly (e.g. using reflection).
            executors.cpuExecutorService.execute {
                try {
                    val value = deserializer.deserialize(snapshot)
                    liveData.postValue(DataOrException(value, null))
                }
                catch (e: Deserializer.DeserializerException) {
                    liveData.postValue(DataOrException(null, e))
                }
            }
        }
        else if (exception != null) {
            liveData.value = DataOrException(null, exception)
        }

        return liveData
    }

}
