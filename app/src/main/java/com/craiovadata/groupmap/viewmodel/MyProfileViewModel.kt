package com.craiovadata.groupmap.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.craiovadata.groupmap.common.DataOrException
import com.craiovadata.groupmap.model.Group
import com.craiovadata.groupmap.repo.*
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class MyProfileViewModel : ViewModel(), KoinComponent {

    private val repository by inject<Repository>()

    //    private var _groupLiveData = MutableLiveData<GroupSkDisplayOrException>()
    private var groupLiveData: LiveData<GroupSkDisplayOrException>? = null
    var textBtnJoin = MutableLiveData<String>()

    private var _navigateToMap = MutableLiveData<SuccessOrException>()
    val navigateToMap: LiveData<SuccessOrException> = _navigateToMap

    private var _navigateToControlPanel = MutableLiveData<Boolean?>()
    val navigateToControlPanel: LiveData<Boolean?> get() = _navigateToControlPanel

    private var _showLoading = MutableLiveData<Boolean>()
    val showLoading: LiveData<Boolean?> get() = _showLoading

    fun onJoin() {
        _showLoading.value = true
        val group = groupLiveData?.value?.data
        repository.joinGroup(group) { successOrException ->
            _navigateToMap.value = successOrException
            _showLoading.value = false
        }
    }

    fun onNavToControlPanel() {
        _navigateToControlPanel.value = true
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
                        if (exception == null) exception = java.lang.Exception("group was null or empty")
                        null
                    }
                if (groupDisplay != null) {
                    textBtnJoin.value = "Join \"${groupDisplay.item.groupName}\" group"
                } else {
                    // todo hide btn join
                }
                GroupSkDisplayOrException(groupDisplay, exception)
            }
            groupLiveData = liveData
        }
        return liveData!!
    }

    fun doneNavigatingToMap() {
        _navigateToMap.value = null
    }

    fun doneNavigatingToControlPanel() {
        _navigateToControlPanel.value = null
    }

}

typealias GroupSkDisplayOrException_ = DataOrException<GroupSkDisplayQueryItem_, Exception>

data class GroupSkDisplayQueryItem_(private val _item: QueryItem<Group>) : QueryItem<GroupDisplay> {
    private val convertedGroup = _item.item.toGroupDisplay()

    override val item: GroupDisplay
        get() = convertedGroup
    override val id: String
        get() = _item.id
}
