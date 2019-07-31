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

package com.craiovadata.groupmap.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.craiovadata.groupmap.model.User
import com.craiovadata.groupmap.repo.QueryItem
import com.craiovadata.groupmap.repo.QueryResultsOrException
import com.craiovadata.groupmap.repo.Repository
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class GroupMapViewModel : ViewModel(), KoinComponent {

    private val groupMapRepository by inject<Repository>()

    private var groupUsersLiveData = HashMap<String, LiveData<UsersDisplayQueryResults>>()

    fun getUsers(groupId: String): LiveData<UsersDisplayQueryResults> {
        var liveData = groupUsersLiveData[groupId]
        if (liveData == null) {
            // Convert UsersQueryResults to UsersDisplayQueryResults
            val usersLiveData = groupMapRepository.getUsersLiveData(groupId)
            liveData = Transformations.map(usersLiveData) { results ->
                val convertedResults = results.data?.map { UserDisplayQueryItem(it) }
                val exception = results.exception
                UsersDisplayQueryResults(convertedResults, exception)
            }
        }
        return liveData!!
    }
}

typealias UsersDisplayQueryResults = QueryResultsOrException<UserDisplay, Exception>

private data class UserDisplayQueryItem(private val _item: QueryItem<User>) : QueryItem<UserDisplay>  {
    private val convertedUser = _item.item.toUserDisplay()

    override val item: UserDisplay
        get() = convertedUser
    override val id: String
        get() = _item.id
}
