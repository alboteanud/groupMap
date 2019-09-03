package com.craiovadata.groupmap.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import com.craiovadata.groupmap.repo.GroupIdOrException
import com.craiovadata.groupmap.repo.Repository
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class CreateGroupViewModel : ViewModel(), KoinComponent {

    private val repository by inject<Repository>()

    fun setNewGroup(groupName: String) {
        repository.setNewGroup( groupName) {
            _navigateToMap.value = it
        }
    }

    // Call this immediately after navigating to Map
    fun doneNavigating() {
        _navigateToMap.value = null
    }

    /**
     * Variable that tells the fragment whether it should navigate to [SleepTrackerFragment].
     *
     * This is `private` because we don't want to expose the ability to set [MutableLiveData] to
     * the [Fragment]
     */
    private val _navigateToMap = MutableLiveData<GroupIdOrException?>()

    // If this is non-null, immediately navigate to [SleepQualityFragment] and call [doneNavigatingToMap]
    val navigateToMap: LiveData<GroupIdOrException?>
        get() = _navigateToMap


}
