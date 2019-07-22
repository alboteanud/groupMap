package com.craiovadata.groupmap.ui

import android.util.Log
import androidx.core.app.ActivityCompat.invalidateOptionsMenu
import androidx.lifecycle.ViewModel
import com.craiovadata.groupmap.PersonRenderer
import com.craiovadata.groupmap.model.Person
import com.craiovadata.groupmap.utils.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FieldValue
import com.google.maps.android.clustering.ClusterManager
import kotlinx.android.synthetic.main.content_map.*

class MapViewModel : ViewModel(){
    var groupData: Map<String, Any>? = null


    init {
        Log.i("MapViewModel", "MapViewModel created")
    }

    override fun onCleared() {
        super.onCleared()
        Log.i("MapViewModel", "MapViewModel destroyed")
    }


}