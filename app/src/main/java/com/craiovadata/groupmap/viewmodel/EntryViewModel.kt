package com.craiovadata.groupmap.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.craiovadata.groupmap.common.DataOrException
import com.craiovadata.groupmap.model.Group
import com.craiovadata.groupmap.repo.*
import com.craiovadata.groupmap.tracker.TrackerService
import com.google.firebase.auth.FirebaseAuth
import org.koin.android.ext.android.inject
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class EntryGroupViewModel : ViewModel(), KoinComponent {
    private val repository by inject<Repository>()
    private val auth by inject<FirebaseAuth>()
    private var groupLiveData: LiveData<GroupSkDisplayOrException>? = null

    private var _navigateToJoinActivity = MutableLiveData<Boolean>()
    val navigateToJoinActivity: LiveData<Boolean> = _navigateToJoinActivity

    init {
        _navigateToJoinActivity.value= true
    }

    fun getGroup(groupShareKey: String): LiveData<GroupSkDisplayOrException> {
        var liveData = groupLiveData

        if (liveData == null) {
            val groupsLiveData = repository.getGroups(groupShareKey)

            liveData = Transformations.map(groupsLiveData) { results ->
                var exception = results.exception
                val groupDisplay: GroupSkDisplayQueryItem? =
                    if (!results.data.isNullOrEmpty()) {
                        val queryItem = results.data[0]
                        GroupSkDisplayQueryItem(queryItem)
                    } else {
                        if (exception == null) exception =
                            java.lang.Exception("group was null or empty")
                        null
                    }

                GroupSkDisplayOrException(groupDisplay, exception)
            }
            groupLiveData = liveData
        }
        return liveData!!
    }


    fun doneNavigatingToJoinActivity() {
//        groupLiveData = null
        _navigateToJoinActivity.value = null
    }

    fun canNavigaitToJoin(): Boolean {
        return _navigateToJoinActivity.value == true
    }

}

typealias GroupSkDisplayOrException = DataOrException<GroupSkDisplayQueryItem, Exception>

data class GroupSkDisplayQueryItem(private val _item: QueryItem<Group>) : QueryItem<GroupDisplay> {
    private val convertedGroup = _item.item.toGroupDisplay()

    override val item: GroupDisplay
        get() = convertedGroup
    override val id: String
        get() = _item.id
}
