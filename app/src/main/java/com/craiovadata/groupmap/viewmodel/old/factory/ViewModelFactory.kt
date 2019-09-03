package com.craiovadata.groupmap.viewmodel.old.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.craiovadata.groupmap.repo.Repository
import com.craiovadata.groupmap.viewmodel.MapViewModel

class ViewModelFactory(
    private val dataSource: Repository

) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
//            return MapViewModel(dataSource) as T
            return MapViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}