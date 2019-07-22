package com.craiovadata.groupmap.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.craiovadata.groupmap.PersonRenderer
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.model.Person
import com.craiovadata.groupmap.utils.*
import com.craiovadata.groupmap.utils.GroupUtils.checkInstallRefferer
import com.craiovadata.groupmap.utils.GroupUtils.joinGroup
import com.craiovadata.groupmap.utils.GroupUtils.startActionShare
import com.craiovadata.groupmap.utils.MapUtils.checkOrAskLocationPermission
import com.craiovadata.groupmap.utils.MapUtils.zoomOnMe
import com.craiovadata.groupmap.utils.Util.buildAlertExitGroup
import com.craiovadata.groupmap.utils.Util.convertDpToPixel
import com.craiovadata.groupmap.utils.Util.startLoginActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.*
import com.google.firebase.firestore.FieldValue.serverTimestamp
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.content_map.*
import com.google.maps.android.clustering.algo.Algorithm
import java.lang.IllegalStateException


class MapActivity : BaseActivity(), OnMapReadyCallback, ClusterManager.OnClusterClickListener<Person> {
    private var map: GoogleMap? = null
    private var groupData: Map<String, Any>? = null
    private var positionListenerRegistration: ListenerRegistration? = null
    var clusterManager: ClusterManager<Person>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        setSupportActionBar(toolbar)
        groupId = getSharedPreferences("_", MODE_PRIVATE).getString(GROUP_ID, null)

        setUpMap()
    }

    override fun onRestart() {
        super.onRestart()
        requestPositionUpdatesFromOthers()
        setPositionsListener()
    }

    override fun onStop() {
        super.onStop()
        positionListenerRegistration?.remove()
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        if (map != null) {
            return
        }
        map = googleMap
        map?.setMaxZoomPreference(16f)
//        map?.uiSettings?.isMapToolbarEnabled = false
        clusterManager = ClusterManager(this, map)
        clusterManager?.renderer = PersonRenderer(this, map, clusterManager)
        map?.setOnCameraIdleListener(clusterManager)
        map?.setOnMarkerClickListener(clusterManager)
        map?.setOnInfoWindowClickListener(clusterManager)
        clusterManager?.setOnClusterClickListener(this)
//        clusterManager.setOnClusterInfoWindowClickListener(this)
//        clusterManager?.setOnClusterItemClickListener { item -> true }
//        clusterManager.setOnClusterItemInfoWindowClickListener(this)
        startApp()
        map?.setOnMapLoadedCallback {
            boundMap()
        }
    }

    private fun boundMap() {
        // clusters are not loaded until MapLoadedCallback
        try {
//            if(clusterManager==null) return
            val field = clusterManager?.javaClass?.getDeclaredField("mAlgorithm")
            field?.isAccessible = true
            val mAlgorithm = field?.get(clusterManager)
//            val clusters_ = clusterManager?.algorithm?.getClusters()
            val clusters = arrayListOf((mAlgorithm as Algorithm<*>).items)
            val builder = LatLngBounds.Builder()
            clusters.forEach { cluster ->
                cluster.forEach { clusterItem ->
                    builder.include(clusterItem.position)
                }
            }
            val padding = convertDpToPixel(20 * resources.displayMetrics.density, this)
            map?.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), padding))
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onClusterClick(cluster: Cluster<Person>?): Boolean {
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

    private fun startApp() {
        checkForShareKey { shareKey ->
            getGroupData(shareKey) { groupData ->
                this.groupData = groupData
                setTitle()
                getMemberRole { memberRole ->
                    this.memberRole = memberRole
                    requestPositionUpdatesFromOthers()
                    invalidateOptionsMenu()
                    setPositionsListener()
                }
            }
        }
    }

    private fun checkForShareKey(callback: (groupShareKey: String?) -> Unit) {
        if (groupId != null) {  // other start
            val groupShareKey = checkAppLinkIntent()
            callback.invoke(groupShareKey)
        } else { //  first start
            groupId = DEFAULT_GROUP
            saveGroupIdToPref(DEFAULT_GROUP)
            checkInstallRefferer(this) { groupShareKeyFromRefferer ->
                val groupShareKey = groupShareKeyFromRefferer
                    ?: checkAppLinkIntent() // maybe prefs deleted
                callback.invoke(groupShareKey)
            }
        }
    }

    private fun getGroupData(groupShareKey: String?, listener: (groupData: Map<String, Any>?) -> Unit) {
        if (groupShareKey == null) {    // no shareKey. Use the saved groupId
            db.document("$GROUPS/$groupId").get()
                .addOnSuccessListener { snap ->
                    listener.invoke(snap.data)
                }
        } else { // share key present. Find the group
            db.collection(GROUPS).whereEqualTo(GROUP_SHARE_KEY, groupShareKey).get()
                .addOnSuccessListener { querySnap ->
                    if (querySnap == null || querySnap.documents.isEmpty()) return@addOnSuccessListener
                    groupId = querySnap.documents[0].id
                    saveGroupIdToPref(groupId)
                    listener.invoke(querySnap.documents[0].data)
                }
        }
    }

    private fun getMemberRole(callback: (memberRole: Int?) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null || groupId == null) {
            return callback(null)
        }
        db.document("$GROUPS/$groupId/$USERS/$uid").get()
            .addOnCompleteListener { task ->
                var role: Int? = null
                if (task.isSuccessful && task.result != null) {
                    role = (task.result?.get(ROLE) as? Long)?.toInt()
                }
                callback(role)
            }
    }

    private fun handleJoinAsked(canStartLogin: Boolean = false) {
        if (memberRole == ROLE_USER || memberRole == ROLE_ADMIN || groupId == null) return
        if (auth.currentUser == null && canStartLogin) {
            startLoginActivity(this, RC_SIGN_IN_ASKED_JOIN)
            return
        }
        val groupName = groupData?.get(GROUP_NAME) as? String
        joinGroup(groupId, groupName) { role ->
            if (role != null) {
                Snackbar.make(content_main, "You joined the group", Snackbar.LENGTH_LONG).show()
                memberRole = role
                zoomOnMe(this, map)
                invalidateOptionsMenu()
            } else {
                Snackbar.make(content_main, "Failed to join the group", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestPositionUpdatesFromOthers() {
        if (groupId == null) return
        if (!(memberRole == ROLE_USER || memberRole == ROLE_ADMIN)) return
        val uid = auth.currentUser?.uid ?: return

        val ref = db.document("$REQUESTS/$groupId")
        ref.set(mapOf(UID to uid, TIMESTAMP to serverTimestamp()))
    }

    override fun onActivityResultOk(requestCode: Int) {
        super.onActivityResultOk(requestCode)
        when (requestCode) {
            RC_SIGN_IN_ASKED_JOIN -> handleJoinAsked(false)
            RC_SIGN_IN -> {
                if (groupId != DEFAULT_GROUP) {
                    startApp()
                }
            }
            RC_CREATE_GROUP -> { // a group was just created
                // groupId was set in BaseActivity
                startApp()
                invalidateOptionsMenu()
                checkOrAskLocationPermission(this) {
                    zoomOnMe(this, map)
                }
            }
        }
    }

    var persons: HashMap<String, Person> = hashMapOf()
    private fun setPositionsListener() {
        if (map == null || groupId == null || clusterManager == null) return
        if (!(memberRole == ROLE_USER || memberRole == ROLE_ADMIN || groupId == DEFAULT_GROUP)) return

        positionListenerRegistration?.remove()
        map?.clear()
//        markers.clear()

        positionListenerRegistration = db.collection("$GROUPS/$groupId/$USERS")
            .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@EventListener
                }
                if (snapshots == null) return@EventListener
                for (dc in snapshots.documentChanges) {
                    val userDoc = dc.document
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            val person = Person(userDoc)
                            if (person.position != null) persons[userDoc.id] = person
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val person = Person(userDoc)
                            if (person.position != null) persons[userDoc.id] = person
                        }
                        DocumentChange.Type.REMOVED -> persons.remove(userDoc.id)
                    }
                    refreshCluster()
                }
            })
    }

    private fun refreshCluster() {
        clusterManager?.clearItems()
        clusterManager?.addItems(persons.values)
        clusterManager?.cluster()
    }

    private fun exitGroup() {
        val uid = auth.currentUser?.uid ?: return
        db.document("$USERS/$uid/$GROUPS/$groupId").delete()
            .addOnSuccessListener {
                positionListenerRegistration?.remove()
                groupData = null
                memberRole = null
                setTitle()
                map?.clear()
                groupId = null
                saveGroupIdToPref(null)
                zoomOnMe(this, map)
                Snackbar.make(content_main, "You left the group", Snackbar.LENGTH_SHORT).show()
            }
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
        groupId = DEFAULT_GROUP // this will force  startApp()  to checkAppLinkIntent()
        startApp()
    }

    private fun checkAppLinkIntent(): String? {
        val appLinkData = intent.data ?: return null
        val segments = appLinkData.pathSegments
        if (segments.size >= 2) {
            if (segments[0] == "group") {
                return segments[1]
            }
        }
        return null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_map, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val showJoin =
            !(memberRole == ROLE_USER || memberRole == ROLE_ADMIN || groupId == DEFAULT_GROUP || groupId == null)
        menu?.findItem(R.id.menu_item_group_info)?.isVisible = memberRole == ROLE_USER || memberRole == ROLE_ADMIN
        menu?.findItem(R.id.menu_item_exit)?.isVisible = memberRole == ROLE_USER || memberRole == ROLE_ADMIN
        menu?.findItem(R.id.menu_item_join)?.isVisible = showJoin
        menu?.findItem(R.id.menu_item_login)?.isVisible = auth.currentUser == null
        menu?.findItem(R.id.menu_item_logout)?.isVisible = auth.currentUser != null
        menu?.findItem(R.id.menu_item_add_members)?.isVisible = memberRole == ROLE_ADMIN
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            R.id.menu_item_new_group -> {
                startActivityForResult(Intent(this, CreateGroupActivity::class.java), RC_CREATE_GROUP)
                return true
            }
            R.id.menu_item_group_info -> {
                val intent = Intent(this, GroupInfoActivity::class.java)
                intent.putExtra(GROUP_ID, groupId)
                startActivity(intent)
                return true
            }
            R.id.menu_item_exit -> {
                val groupName = groupData?.get(GROUP_NAME) as? String ?: "My Group"
                buildAlertExitGroup(this, groupName) {
                    exitGroup()
                }
                return true
            }
            R.id.menu_item_add_members -> {
                startActionShare(this, groupData)
                return true
            }
            R.id.menu_item_join -> {
                handleJoinAsked(true)
                return true
            }
            R.id.menu_item_logout -> {
                auth.signOut()
                if (groupId == DEFAULT_GROUP) return true
                positionListenerRegistration?.remove()
                groupData = null
                memberRole = null
                setTitle()
                map?.clear()
                startApp()
                return true
            }
            R.id.menu_item_login -> {
                startLoginActivity(this, RC_SIGN_IN)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setUpMap() {
        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
    }

    private fun setTitle() {
        val groupName = groupData?.get(GROUP_NAME) as? String
        title = groupName ?: getString(R.string.app_name)
    }

    companion object {
        private val TAG = MapActivity::class.java.simpleName
    }

    fun onFabClicked(view: View) {
        checkOrAskLocationPermission(this) {
            zoomOnMe(this, map)
        }
//        GroupUtils.populateDefaultGroup()
    }

}