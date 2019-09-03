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

package com.craiovadata.groupmap.repo

import android.location.Location
import androidx.lifecycle.LiveData
import com.craiovadata.groupmap.viewmodel.GroupSkDisplayQueryItem
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit

interface Repository {

//    val allTickers: SortedSet<String>

    /**
     * Gets a LiveData object from this repo that reflects the current value of
     * a single Stock, given by its uid.
     */
    fun getGroupLiveData(groupId: String): LiveData<GroupOrException>

    fun getMyGroupsLiveData(): LiveData<GroupsQueryResults>
    fun getGroups(groupShareKey: String): LiveData<GroupsQueryResults>

    fun getUsersLiveData(groupId: String): LiveData<UsersQueryResults>
    fun getUsersMapLiveData(groupId: String): LiveData<UsersQueryResults>

    /**
     * Synchronizes one stock record so it's available to this repo while offline
     */
    fun syncGroup(groupId: String, timeout: Long, unit: TimeUnit): ListenableFuture<SyncResult>

    fun requestPositionUpdates(groupId: String?)

    fun setNewGroup(groupName: String, callback: (groupIdOrException: GroupIdOrException) -> Unit)

    fun sendMyPosition(groupId: String, location: Location, callback: (successOrException: SuccessOrException) -> Unit)

    fun deleteToken(sentUid: String)
    fun exitGroup(groupId: String)

    enum class SyncResult {
        SUCCESS, UNKNOWN, FAILURE, TIMEOUT
    }

    fun sendTokenToServer(token: String?)

    fun joinGroup(group: GroupSkDisplayQueryItem?, callback: (se: SuccessOrException) -> Unit)
    fun removeUserFromGroup(uid: String, groupId: String)

    fun dissmissAsAdmin(uid: String, groupId: String)
    fun makeGroupAdmin(uid: String, groupId: String)
    fun changeGroupName(groupId: String, groupName: String)
    fun setPauseLocationUpdates(groupId: String)
    fun allowLocationUpdates(groupId: String)
}
