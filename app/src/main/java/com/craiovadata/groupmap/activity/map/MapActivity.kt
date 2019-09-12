package com.craiovadata.groupmap.activity.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.activity.base.BaseActivity
import com.craiovadata.groupmap.activity.groupinfo.GroupInfoActivity
import com.craiovadata.groupmap.markerrenderer.MarkerRenderer
import com.craiovadata.groupmap.repo.QueryItem
import com.craiovadata.groupmap.tracker.TrackerService
import com.craiovadata.groupmap.util.*
import com.craiovadata.groupmap.util.MapUtils.getCameraBounds
import com.craiovadata.groupmap.util.Util.convertDpToPixel
import com.craiovadata.groupmap.util.Util.startActionShare
import com.craiovadata.groupmap.viewmodel.GroupDisplay
import com.craiovadata.groupmap.viewmodel.MapViewModel
import com.craiovadata.groupmap.viewmodel.UserMapDisplay
import com.craiovadata.groupmap.viewmodel.UserMapDisplayQueryResults
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import kotlinx.android.synthetic.main.activity_create_group.*
import timber.log.Timber

class MapActivity : BaseActivity(), OnMapReadyCallback,
    ClusterManager.OnClusterClickListener<UserMapDisplay> {

    private var groupId: String? = null
    private lateinit var mMap: GoogleMap
    private lateinit var clusterManager: ClusterManager<UserMapDisplay>
    private var viewModel: MapViewModel? = null
    private var groupData: GroupDisplay? = null
    private var usersWithLocation: List<UserMapDisplay> = listOf()
    private var isPauseLocation = false
    private var padding = -1
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        groupId = intent.getStringExtra(EXTRA_GROUP_ID)
            ?: throw IllegalArgumentException("no groupId provided")
        initViews()
    }

    private fun initViews() {
        setContentView(R.layout.activity_map)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
    }

    override fun onRestart() {
        super.onRestart()
        viewModel?.requestPositionUpdate(groupId)
        sendMyPosition()
    }

    private fun sendMyPosition() {
        if (groupId == null) return
        startService(TrackerService.newIntent(this, groupId!!, null))
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setUpMap()
        setUpObservers()
        viewModel?.requestPositionUpdate(groupId)
        sendMyPosition()
    }

    private fun setUpObservers() {
        viewModel = ViewModelProviders.of(this).get(MapViewModel::class.java)

        viewModel!!.getGroupLiveData(groupId!!).observe(this, Observer {
            if (it != null) {
                if (it.data != null) {
                    val groupName = it.data.groupName
                    toolbar.title = groupName
                    groupData = it.data
                } else if (it.exception != null) {
                    Timber.e(it.exception, "Observed unexpected exception")
                    toolbar.title = getString(R.string.title_activity_map)
                }
            }
        })

        viewModel!!.getUsers(groupId!!, auth.uid)
            .observe(this@MapActivity, Observer<UserMapDisplayQueryResults> { queryResults ->
                if (queryResults != null) {
                    val data = queryResults.data
                    if (!data.isNullOrEmpty()) {
                        showUsersOnMap(data)
                        if (padding == -1) {
                            padding = convertDpToPixel(
                                paddingMap * resources.displayMetrics.density,
                                this
                            )
                            Handler().postDelayed({
                                boundUsersOnMap()
                            }, 3000)
                        }
                    } else if (queryResults.exception != null) {
                        Timber.e(queryResults.exception, "Error getting users")
                        snack("Error getting users")
                    }
                }
            })

        viewModel!!.isAdmin.observe(this, Observer {
            isAdmin = it
            invalidateOptionsMenu()
        })
        viewModel!!.isPauseLocation.observe(this, Observer {
            isPauseLocation = it ?: false
            invalidateOptionsMenu()
        })
    }

    private fun showUsersOnMap(data: List<QueryItem<UserMapDisplay>>) {
        usersWithLocation = data.mapNotNull { userQueryItem ->
            val member = userQueryItem.item
            if (member.location != null
//                && member.isPositionFresh()
            ) {
                member.setId(userQueryItem.id)
                member
            } else null
        }
        clusterManager.clearItems()
        clusterManager.addItems(usersWithLocation)
        clusterManager.cluster()
    }

    private fun boundUsersOnMap() {
        val cameraBounds = getCameraBounds(usersWithLocation) ?: return

        try {
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(cameraBounds, padding)
            mMap.animateCamera(cameraUpdate)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    private fun setUpMap() {
        clusterManager = ClusterManager(this, mMap)
//        mMap.setMaxZoomPreference(16f)
        mMap.uiSettings?.isMapToolbarEnabled = true
//        clusterManager.algorithm = GridBasedAlgorithm<UserDisplay>()
        clusterManager.renderer =
            MarkerRenderer(this, mMap, clusterManager)
        mMap.setOnCameraIdleListener(clusterManager)
        mMap.setOnMarkerClickListener(clusterManager)
        mMap.setOnInfoWindowClickListener(clusterManager)
        if (!isDemo()) {
            checkOrAskLocationPermission {
                enableMyLocation()
            }
            mMap.uiSettings?.isMyLocationButtonEnabled = true
        }
        clusterManager.setOnClusterClickListener(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                enableMyLocation()
        }
    }

    private fun enableMyLocation() {
        try {
            mMap.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }


    // Check location permission is granted - if it is, start
    // the service, otherwise request the permission
    private fun checkOrAskLocationPermission(callback: () -> Unit) {
        // Check GPS is enabled
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            snack("Please enable location services")
            Util.buildAlertMessageNoGps(this)
            return
        }
        val permission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            callback.invoke()
        } else {
            // callback will be inside the activity's onRequestPermissionsResult(
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSIONS_REQUEST
            )
        }
    }

    override fun onClusterClick(cluster: Cluster<UserMapDisplay>?): Boolean {
        if (cluster == null) return false
        val iterator = cluster.items.iterator()
        val firstName = iterator.next().name
        Toast.makeText(this, "${cluster.size} (including " + firstName + ")", Toast.LENGTH_SHORT)
            .show()

        // Zoom in the cluster. Need to create LatLngBounds and including all the cluster items
        val builder = LatLngBounds.builder()
        for (item in cluster.items) {
            builder.include(item.position)
        }
        val bounds = builder.build()
        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    override fun onLogout() {
        if (!isDemo()) {
            // in case of logOut -> will go to EntryActivity
            super.onLogout()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_map, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menu_item_group_info)?.isVisible = !isDemo()
        menu?.findItem(R.id.menu_item_exit)?.isVisible = !isDemo()
        menu?.findItem(R.id.menu_item_enable_location)?.isVisible = isPauseLocation && !isDemo()
        menu?.findItem(R.id.menu_item_pause_location)?.isVisible = !isPauseLocation && !isDemo()
        menu?.findItem(R.id.menu_item_add_participants)?.isVisible = isAdmin
        return super.onPrepareOptionsMenu(menu)
    }

    private fun isDemo(): Boolean {
        return groupId == DEFAULT_GROUP
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_bound_users -> {
                boundUsersOnMap()
                return true
            }
            R.id.menu_item_add_participants -> {
                startActionShare(this, groupData)
                return true
            }
            R.id.menu_item_group_info -> {
                val intent = GroupInfoActivity.newIntent(this, groupId!!)
                startActivity(intent)
                return true
            }
            R.id.menu_item_exit -> {
                Util.buildAlertExitGroup(this, groupData?.groupName) {
                    viewModel?.onQuitGroup(groupId!!)
                    finish()
                }
                return true
            }
            R.id.menu_item_pause_location -> {
                viewModel?.pauseLocationUpdates(groupId!!)
                return true
            }
            R.id.menu_item_enable_location -> {
                viewModel?.allowLocationUpdates(groupId!!)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null && intent.hasExtra(EXTRA_GROUP_ID)) {
            val extraGroupId = intent.getStringExtra(EXTRA_GROUP_ID)
            if (extraGroupId != null && extraGroupId != groupId) {
                groupId = extraGroupId
                viewModel?.refresh(groupId!!, auth.uid)
            }
        }
    }

    companion object {

        const val paddingMap = 30

        fun newIntent(context: Context, groupId: String): Intent {
            val intent = Intent(context, MapActivity::class.java)
            intent.putExtra(EXTRA_GROUP_ID, groupId)
            return intent
        }
    }

}
