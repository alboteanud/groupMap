package com.craiovadata.groupmap.viewmodel

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.craiovadata.groupmap.model.User
import com.craiovadata.groupmap.repo.QueryItem
import com.craiovadata.groupmap.repo.QueryResultsOrException
import com.craiovadata.groupmap.repo.Repository
import com.google.firebase.auth.FirebaseAuth
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class GroupInfoViewModel : ViewModel(), KoinComponent {

    private val myRepository by inject<Repository>()
    private val auth by inject<FirebaseAuth>()
    private var membersLiveData: LiveData<MemberDisplayQueryResults>? = null
    private var groupLiveData: LiveData<GroupDisplayOrException>? = null
    val textNrMembers = MutableLiveData<String>()
    val textGroupName = MutableLiveData<String>()
    val isAdmin = MutableLiveData<Boolean>()
    private var groupId: String? = null

    fun getMembers(groupId: String): LiveData<MemberDisplayQueryResults> {
        var liveData = membersLiveData
        if (liveData == null) {
            // Convert UsersQueryResults to UserDisplayQueryResults
            val usersLiveData = myRepository.getUsersLiveData(groupId)
            liveData = Transformations.map(usersLiveData) { results ->
                setupTextNrMembers(results.data?.size)

                val convertedResults = mutableListOf<MemberDisplayQueryItem>()
                results.data?.mapIndexedNotNullTo(convertedResults) { index, queryItem ->
                    val member = MemberDisplayQueryItem(queryItem)
                    if (member.id == auth.uid) {
                        isAdmin.value = member.item.isAdmin()
                        convertedResults.add(0, member)
                        null
                    } else
                        member
                }

                val exception = results.exception
                MemberDisplayQueryResults(convertedResults, exception)
            }
        }

        return liveData!!
    }

    @MainThread
    fun getGroupLiveData(groupId: String): LiveData<GroupDisplayOrException> {
        this.groupId = groupId
        var liveData = groupLiveData
        if (liveData == null) {
            val ld = myRepository.getGroupLiveData(groupId)
            liveData = Transformations.map(ld) {
                val group = GroupDisplayOrException(it.data?.toGroupDisplay(), it.exception)
                textGroupName.value = group.data?.groupName ?: "error name"
                group
            }
            groupLiveData = liveData
        }
        return liveData!!
    }

    private fun setupTextNrMembers(nrMembers: Int?) {
        textNrMembers.value = when (nrMembers) {
            null -> ""
            1 -> "$nrMembers participant"
            else -> "$nrMembers participants"
        }
    }

    fun onRemoveUser(uid: String) {
        myRepository.removeUserFromGroup(uid, groupId!!)
    }

    fun onDissmissAsAdmin(uid: String) {
        myRepository.dissmissAsAdmin(uid, groupId!!)
    }

    fun onMakeGroupAdmin(uid: String) {
        myRepository.makeGroupAdmin(uid, groupId!!)
    }

    fun onNameChange(groupName: String) {
        myRepository.changeGroupName(groupId!!, groupName)
    }

}

typealias MemberDisplayQueryResults = QueryResultsOrException<UserDisplay, Exception>

private data class MemberDisplayQueryItem(private val _item: QueryItem<User>) :
    QueryItem<UserDisplay> {
    private val convertedGroup = _item.item.toUserDisplay()

    override val item: UserDisplay
        get() = convertedGroup
    override val id: String
        get() = _item.id
}
