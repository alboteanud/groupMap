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

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.craiovadata.groupmap.model.Group
import com.craiovadata.groupmap.repo.QueryItem
import com.craiovadata.groupmap.repo.QueryResultsOrException
import com.craiovadata.groupmap.repo.Repository
import com.google.firebase.auth.FirebaseAuth
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class MyGroupsViewModel
//@Inject constructor(signInViewModelDelegate: SignInViewModelDelegate)
    : ViewModel(), KoinComponent
//    , SignInViewModelDelegate by signInViewModelDelegate
{

    private val myRepository by inject<Repository>()

    private var groupsLiveData = HashMap<String, LiveData<GroupDisplayQueryResults>>()
    private val _currentUserImageUri = MutableLiveData<Uri?>()
    val currentUserImageUri: LiveData<Uri?> = _currentUserImageUri

    init {
        _currentUserImageUri.value = FirebaseAuth.getInstance().currentUser?.photoUrl
    }

    fun getGroups(uid: String): LiveData<GroupDisplayQueryResults> {
        var liveData = groupsLiveData[uid]
        if (liveData == null) {
            // Convert UsersQueryResults to UserDisplayQueryResults
            val groupsLiveData = myRepository.getMyGroupsLiveData()
            liveData = Transformations.map(groupsLiveData) { results ->
                val convertedResults = results.data?.map { GroupDisplayQueryItem(it) }
                val exception = results.exception
                GroupDisplayQueryResults(convertedResults, exception)
            }
        }
        return liveData!!
    }

    fun onProfileClicked() {
//        if (isSignedIn()) {
//            _navigateToSignOutDialogAction.value = Event(Unit)
//        } else {
//            _navigateToSignInDialogAction.value = Event(Unit)
//        }
    }

}

typealias GroupDisplayQueryResults = QueryResultsOrException<GroupDisplay, Exception>

private data class GroupDisplayQueryItem(private val _item: QueryItem<Group>) :
    QueryItem<GroupDisplay> {
    private val convertedGroup = _item.item.toGroupDisplay()

    override val item: GroupDisplay
        get() = convertedGroup
    override val id: String
        get() = _item.id
}
