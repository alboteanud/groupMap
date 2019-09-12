package com.craiovadata.groupmap.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.craiovadata.groupmap.repo.*
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class JoinGroupViewModel : ViewModel(), KoinComponent {

    private val repository by inject<Repository>()
    private var _navigateToMap = MutableLiveData<Boolean>()
    val navigateToMap: LiveData<Boolean> = _navigateToMap

    private var _navigateToMyGroups = MutableLiveData<Boolean>()
    val navigateToMyGroups: LiveData<Boolean> get() = _navigateToMyGroups

    private var _showLoading = MutableLiveData<Boolean>()
    val showLoading: LiveData<Boolean?> get() = _showLoading

    fun onJoin(groupId: String, groupName: String) {
        _showLoading.value = true
        repository.joinGroup(groupId, groupName) { successOrException ->
            if (successOrException.data != null) {
                _navigateToMap.value = true
            }
            _showLoading.value = false
        }
    }

    fun doneNavigatingToMap() {
        _navigateToMap.value = null
    }

    fun doneNavigatingToMyGroups() {
        _navigateToMyGroups.value = null
    }

    fun onNavToMyGroups(){
        _navigateToMyGroups.value = true
    }

}

