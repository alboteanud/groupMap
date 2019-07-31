package com.craiovadata.groupmap.activity.map

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.utils_.MapUtils.getCameraBounds
import com.craiovadata.groupmap.utils_.MarkerRenderer2
import com.craiovadata.groupmap.utils_.Util.convertDpToPixel
import com.craiovadata.groupmap.viewmodel.GroupMapViewModel
import com.craiovadata.groupmap.viewmodel.UserDisplay
import com.craiovadata.groupmap.viewmodel.UsersDisplayQueryResults

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.algo.GridBasedAlgorithm
import org.koin.android.ext.android.inject

class MapActivity2 : AppCompatActivity(), OnMapReadyCallback, ClusterManager.OnClusterClickListener<UserDisplay> {

    companion object {
        private const val TAG = "MapActivity2"
        const val EXTRA_GROUP = "MapActivity2.groupId"

        fun newIntent(context: Context, ticker: String): Intent {
            val intent = Intent(context, MapActivity2::class.java)
            intent.putExtra(EXTRA_GROUP, ticker)
            return intent
        }

        const val INITIAL_PADDING = 0
    }

    private val auth by inject<FirebaseAuth>()
    private lateinit var mMap: GoogleMap
    private lateinit var clusterManager: ClusterManager<UserDisplay>
    private var padding = INITIAL_PADDING

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map2)
        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setUpMap()
        setUpObservers()
    }

    private fun setUpObservers() {
        val groupId = intent.getStringExtra(EXTRA_GROUP) ?: return
        val viewModel = ViewModelProviders.of(this).get(GroupMapViewModel::class.java)
        val usersObserver = Observer<UsersDisplayQueryResults> { queryResults ->
            if (queryResults != null) {
                if (queryResults.data != null) {
                    val users = queryResults.data.map { it.item }
                    showUsersOnMap(users)
                    if (padding == INITIAL_PADDING) boundUsers(users)
                } else if (queryResults.exception != null) {
                    Log.e(TAG, "Error getting stock history", queryResults.exception)
                    TODO("Handle the error")
                }
            }
        }
        val usersLiveData = viewModel.getUsers(groupId)
        usersLiveData.observe(this@MapActivity2, usersObserver)
    }

    private fun showUsersOnMap(users: List<UserDisplay>) {
        clusterManager.clearItems()
        clusterManager.addItems(users)
        clusterManager.cluster()
    }

    private fun boundUsers(users: List<UserDisplay>) {
        padding = convertDpToPixel(40 * resources.displayMetrics.density, this)
        val cameraBounds = getCameraBounds(users) ?: return
        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(cameraBounds, padding))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setUpMap() {
        clusterManager = ClusterManager(this, mMap)
        mMap.setMaxZoomPreference(16f)
//        map?.uiSettings?.isMapToolbarEnabled = true
        clusterManager.algorithm = GridBasedAlgorithm<UserDisplay>()
        clusterManager.renderer = MarkerRenderer2(this, mMap, clusterManager)
        mMap.setOnCameraIdleListener(clusterManager)
        mMap.setOnMarkerClickListener(clusterManager)
        mMap.setOnInfoWindowClickListener(clusterManager)
        clusterManager.setOnClusterClickListener(this)
//        clusterManager.setOnClusterInfoWindowClickListener(this)
//        clusterManager?.setOnClusterItemClickListener { item -> true }
//        clusterManager.setOnClusterItemInfoWindowClickListener(this)
    }

    override fun onClusterClick(cluster: Cluster<UserDisplay>?): Boolean {
        if (cluster == null) return false
        // Show a toast with some info when the cluster is clicked.
        val firstName = cluster.items.iterator().next().name
        Toast.makeText(this, "${cluster.size} (including " + firstName + ")", Toast.LENGTH_SHORT).show()

        // Zoom in the cluster. Need to create LatLngBounds and including all the cluster items
        // inside of bounds, then animate to center of the bounds.

        // Create the builder to collect all essential cluster items for the bounds.
        val builder = LatLngBounds.builder()
        for (item in cluster.items) {
            builder.include(item.position)
        }
        // Get the LatLngBounds
        val bounds = builder.build()

        // Animate camera to the bounds
        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return true
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        if (auth.currentUser == null) {
            finish()
        }
    }

}
