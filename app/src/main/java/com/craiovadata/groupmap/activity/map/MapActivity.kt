package com.craiovadata.groupmap.activity.map

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.craiovadata.groupmap.utils_.MarkerRenderer
import com.craiovadata.groupmap.repo.oldDB.MyDatabase
import com.craiovadata.groupmap.repo.oldDB.MyDatabaseDao
import com.craiovadata.groupmap.databinding.ActivityMapBinding
import com.craiovadata.groupmap.model.Member
import com.craiovadata.groupmap.activity.others_.CreateGroupActivity
import com.craiovadata.groupmap.activity.others_.GroupInfoActivity
import com.craiovadata.groupmap.activity.others_.SettingsActivity
import com.craiovadata.groupmap.utils_.*
import com.craiovadata.groupmap.utils_.MapUtils.zoomOnMe
import com.craiovadata.groupmap.utils_.Util.buildAlertExitGroup
import com.craiovadata.groupmap.utils_.Util.convertDpToPixel
import com.craiovadata.groupmap.utils_.Util.startLoginActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import androidx.lifecycle.Observer
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.viewmodel.old.MapViewModel
import com.craiovadata.groupmap.viewmodel.old.factory.ViewModelFactory
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.toolbar.*
import org.koin.android.ext.android.inject


class MapActivity : AppCompatActivity(), ClusterManager.OnClusterClickListener<Member>, OnMapReadyCallback {

    companion object {
        private const val TAG = "MapActivity"
        private const val RC_SIGN_IN = Activity.RESULT_FIRST_USER

        private enum class Database { Firestore, RealtimeDatabase }

        private var database = Companion.Database.Firestore
    }

    private var map: GoogleMap? = null
    //    var clusterManager: ClusterManager<Member>? = null
    private var viewModel: MapViewModel? = null
    private lateinit var binding: ActivityMapBinding

    private val auth by inject<FirebaseAuth>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
//        viewModel.positionListenerRegistration?.remove()
        auth.removeAuthStateListener(authStateListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            // Successfully signed in
            if (resultCode == Activity.RESULT_OK) {
                snack("Signed in")
            } else {
                // Sign in failed
                val response = IdpResponse.fromResultIntent(data) ?: return

                if (response.error!!.errorCode == ErrorCodes.NO_NETWORK) {
                    snack("No network")
                    return
                }

                snack("Unknown error with sign in")
                Log.e(TAG, "Sign-in error: ", response.error)
            }
        }

//            RC_SIGN_IN_ASKED_JOIN -> viewModel?.handleJoinAsked(false)
//            RC_SIGN_IN -> {
//                if (groupId != DEFAULT_GROUP) {
//                    viewModel.startApp()
//                }
//            }
//            RC_CREATE_GROUP -> { // a group was just created
        // groupId was set in BaseActivity
//                viewModel.startApp()
//                invalidateOptionsMenu()
//                checkOrAskLocationPermission(this) {
//                    zoomOnMe(this, map)
//                }
//            }
    }

    private fun initViews() {
//        setContentView(R.layout.activity_map)
        binding = DataBindingUtil.setContentView(this, com.craiovadata.groupmap.R.layout.activity_map)
        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
        setSupportActionBar(toolbar)
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        if (map != null) return
        map = googleMap
        val clusterManager = setUpMap()
        verifyShareKeyPresence { groupShareKey ->
            setUpViewModel(groupShareKey)
            setUpUsersObserver(clusterManager)
            invalidateOptionsMenu()
        }
    }

    private fun setUpViewModel(groupShareKey: String?) {
        val application = requireNotNull(this).application
        val dataSource: MyDatabaseDao = MyDatabase(application, groupShareKey)
        val viewModelFactory = ViewModelFactory(dataSource)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MapViewModel::class.java)
        binding.mapViewModel = viewModel
        binding.lifecycleOwner = this

    }

    var doneAnimating = false
    private fun setUpUsersObserver(clusterManager: ClusterManager<Member>) {
        viewModel?.getUsers()?.observe(this, Observer { users ->
            clusterManager.clearItems()
            clusterManager.addItems(users)
            clusterManager.cluster()
            if (!doneAnimating) { // execute first locationTimestamp only
                val cameraBounds = viewModel?.getCameraBounds() ?: return@Observer
                val padding = convertDpToPixel(40 * resources.displayMetrics.density, this)
                map?.animateCamera(CameraUpdateFactory.newLatLngBounds(cameraBounds, padding))
                doneAnimating = true
            }
        })
    }

    private fun setUpMap(): ClusterManager<Member> {
        map?.setMaxZoomPreference(16f)
//        map?.uiSettings?.isMapToolbarEnabled = true
        val clusterManager: ClusterManager<Member> = ClusterManager(this, map)
        clusterManager.renderer = MarkerRenderer(this, map, clusterManager)
        map?.setOnCameraIdleListener(clusterManager)
        map?.setOnMarkerClickListener(clusterManager)
        map?.setOnInfoWindowClickListener(clusterManager)
        clusterManager.setOnClusterClickListener(this)
//        clusterManager.setOnClusterInfoWindowClickListener(this)
//        clusterManager?.setOnClusterItemClickListener { item -> true }
//        clusterManager.setOnClusterItemInfoWindowClickListener(this)
        return clusterManager
    }

    private fun verifyShareKeyPresence(callback: (groupShareKey: String?) -> Unit) {
        val groupId = getSharedPreferences("_", MODE_PRIVATE).getString(GROUP_ID, null)
        if (groupId != null) {  // other start
            val groupShareKey = checkAppLinkIntent()
            callback.invoke(groupShareKey)
        } else { //  null. first start
//            saveGroupIdToPref(DEFAULT_GROUP)
            GroupUtils.checkInstallRefferer(this) { groupShareKeyFromRefferer ->
                val groupShareKey = groupShareKeyFromRefferer
                    ?: checkAppLinkIntent() // maybe prefs deleted
                callback.invoke(groupShareKey)
            }
        }
    }

    private fun checkAppLinkIntent(): String? {
        val appLinkData = intent.data ?: return null
        val segments = appLinkData.pathSegments
        if (segments.size >= 2) {
            if (segments[0] == "group") {
                val groupShareKey = segments[1]
                return groupShareKey
            }
        }
        return null
    }

    override fun onResume() {
        super.onResume()
//        setUpMap()
    }

    override fun onRestart() {
        super.onRestart()
//        viewModel.requestPositionUpdatesFromOthers()
//        viewModel.setPositionsListener()
    }

    override fun onClusterClick(cluster: Cluster<Member>?): Boolean {
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
            map?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_AND_ZOOM_ME && grantResults.size == 1
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {     // coming from MapUtils.checkLocationPermissions() - ActivityCompat.requestPermissions()
            // Start the service when the permission is granted
            zoomOnMe(this, map)
        } else {
            // permission not granted
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
//        groupId = DEFAULT_GROUP // this will force  startApp()  to checkAppLinkIntent()
//        viewModel.startApp()
        // todo reset data source with the new shareKey
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(com.craiovadata.groupmap.R.menu.menu_map, menu)
        return true
    }

    private var completedSetupMenuObservers = false
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
//        menu?.findItem(com.craiovadata.groupmap.R.id.menu_item_group_info)?.isVisible =
//            memberRole == ROLE_USER || memberRole == ROLE_ADMIN
//        menu?.findItem(com.craiovadata.groupmap.R.id.menu_item_exit)?.isVisible =
//            memberRole == ROLE_USER || memberRole == ROLE_ADMIN
        menu?.findItem(com.craiovadata.groupmap.R.id.menu_item_login)?.isVisible = auth.currentUser == null
        menu?.findItem(com.craiovadata.groupmap.R.id.menu_item_logout)?.isVisible = auth.currentUser != null
//        menu?.findItem(com.craiovadata.groupmap.R.id.menu_item_add_members)?.isVisible = memberRole == ROLE_ADMIN

        if (!completedSetupMenuObservers)
            viewModel?.joinButtonVisible?.observe(this, Observer {
                menu?.findItem(com.craiovadata.groupmap.R.id.menu_item_join)?.isVisible = it
            })

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            com.craiovadata.groupmap.R.id.menu_item_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            com.craiovadata.groupmap.R.id.menu_item_new_group -> {
                startActivityForResult(Intent(this, CreateGroupActivity::class.java), RC_CREATE_GROUP)
                return true
            }
            com.craiovadata.groupmap.R.id.menu_item_group_info -> {
                val intent = Intent(this, GroupInfoActivity::class.java)
//                intent.putExtra(GROUP_ID, groupId)
                startActivity(intent)
                return true
            }
            com.craiovadata.groupmap.R.id.menu_item_exit -> {
//                val groupName = viewModel.groupData?.get(GROUP_NAME) as? String ?: "My Group"
                buildAlertExitGroup(this, "groupName") {
                    viewModel?.onQuitGroup()
                }
                return true
            }
            com.craiovadata.groupmap.R.id.menu_item_add_members -> {
//                startActionShare(this, viewModel.groupData)
                return true
            }
            com.craiovadata.groupmap.R.id.menu_item_join -> {
                viewModel?.handleJoinAsked(true)
                return true
            }
            com.craiovadata.groupmap.R.id.menu_item_logout -> {
                auth.signOut()
//                if (groupId == DEFAULT_GROUP) return true
                viewModel?.positionListenerRegistration?.remove()
//                viewModel.groupData = null
//                memberRole = null
//                setTitle()
                map?.clear()
//                viewModel.startApp()
                return true
            }
            com.craiovadata.groupmap.R.id.menu_item_login -> {
                startLoginActivity(this, RC_SIGN_IN)
                return true
            }
            com.craiovadata.groupmap.R.id.menu_item_mylocation -> {
//                checkOrAskLocationPermission(this) {
//                    zoomOnMe(this, map)
//                }
//        GroupUtils.populateDefaultGroup()

                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val loggedIn = auth.currentUser != null
//        btnLogIn.isEnabled = !loggedIn
//        btnLogOut.isEnabled = loggedIn
//        btnTrackTwo.isEnabled = loggedIn
//        btnTrackRecyclerView.isEnabled = loggedIn
//        btnTrackPagedRecyclerView.isEnabled = loggedIn
//        btnStockHistory.isEnabled = loggedIn
//        if (loggedIn) {
//            startActivity(Intent(this, MultiTrackerRecyclerView::class.java))
//        }
    }

    private fun snack(message: String) {
        val vRoot: View = findViewById(R.id.root)
        Snackbar.make(vRoot, message, Snackbar.LENGTH_SHORT).show()
    }

//    private fun setPositionsListener(mask: Boolean = false) {
//        positionListenerRegistration?.remove()
//        mMap?.clear()
//        positionListenerRegistration = db.collection(GROUPS).document(groupId).collection(DEVICES)
//            .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
//                if (e != null) {
//                        if (snapshots == null) return@EventListener
//                        for (dc in snapshots.documentChanges) {
//                            when (dc.type) {
//                                DocumentChange.Type.ADDED -> setMarker(this, dc.document, mask, mMarkers, mMap)
//                                DocumentChange.Type.MODIFIED -> setMarker(this, dc.document, mask, mMarkers, mMap)
//                                DocumentChange.Type.REMOVED -> Log.d(TAG, "Removed doc: ${dc.document.data}")
//                                DocumentChange.Type.ADDED -> setMarker(applicationContext, dc.document, mask, mMarkers, mMap)
//                                DocumentChange.Type.MODIFIED -> setMarker(applicationContext, dc.document, mask, mMarkers, mMap)
//                                DocumentChange.Type.REMOVED -> {
//                                    Log.d(TAG, "Removed doc: ${dc.document.data}")
//                                    mMarkers.remove(dc.document.id)
////                            setMarker(this, dc.document, true, mMarkers, mMap)
//                                }
//                            }
//                        }
//                    })


}