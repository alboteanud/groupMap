package com.craiovadata.groupmap.viewmodel

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.craiovadata.groupmap.common.DataOrException
import com.craiovadata.groupmap.model.User
import com.craiovadata.groupmap.repo.QueryItem
import com.craiovadata.groupmap.repo.QueryResultsOrException
import com.craiovadata.groupmap.repo.Repository
import com.craiovadata.groupmap.tracker.TrackerService
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class MapViewModel : ViewModel(), KoinComponent {

    private val repository by inject<Repository>()
    private var groupLiveData: LiveData<GroupDisplayOrException>? = null
    private var usersLiveData: LiveData<UserMapDisplayQueryResults>? = null

    private val _isAdmin = MutableLiveData<Boolean>()
    val isAdmin: LiveData<Boolean> = _isAdmin

    private val _isPauseLocation = MutableLiveData<Boolean>()
    val isPauseLocation: LiveData<Boolean> = _isPauseLocation

    fun pauseLocationUpdates(groupId: String) {
        repository.setPauseLocationUpdates(groupId)
    }

    fun allowLocationUpdates(groupId: String) {
        repository.allowLocationUpdates(groupId)
    }

    @MainThread
    fun getGroupLiveData(groupId: String): LiveData<GroupDisplayOrException> {

        var liveData = groupLiveData
        if (liveData == null) {
            val ld = repository.getGroupLiveData(groupId)
            liveData = Transformations.map(ld) {
                GroupDisplayOrException(it.data?.toGroupDisplay(), it.exception)
            }
            groupLiveData = liveData
        }
        return liveData!!
    }

    fun requestPositionUpdate(groupId: String?) {
        repository.requestPositionUpdates(groupId)
    }

    fun getUsers(groupId: String, uid: String?): LiveData<UserMapDisplayQueryResults> {
        var liveData = usersLiveData
        if (liveData == null) {
            // Convert UsersQueryResults to UserDisplayQueryResults
            val usersLiveData = repository.getUsersMapLiveData(groupId)
            liveData = Transformations.map(usersLiveData) { results ->
                val usersWithLocation = results.data?.map {
                    val member = UserMapDisplayQueryItem(it)

                    // check if current user is admin
                    if (it.id == uid) {
                        _isAdmin.value = member.item.isAdmin()
                        _isPauseLocation.value = member.item.pause
                    }

                    member
                }
                val exception = results.exception
                UserMapDisplayQueryResults(usersWithLocation, exception)

            }
        }

        return liveData!!
    }

    fun refresh(groupId: String, uid: String?) {
        getGroupLiveData(groupId)
        getUsers(groupId, uid)
    }

    fun onQuitGroup(groupId: String) {
        repository.exitGroup(groupId)
    }


}

typealias UserMapDisplayQueryResults = QueryResultsOrException<UserMapDisplay, Exception>
typealias GroupDisplayOrException = DataOrException<GroupDisplay, Exception>

private data class UserMapDisplayQueryItem(private val _item: QueryItem<User>) :
    QueryItem<UserMapDisplay> {
    private val convertedUser = _item.item.toUserMapDisplay()

    override val item: UserMapDisplay
        get() = convertedUser
    override val id: String
        get() = _item.id
}


