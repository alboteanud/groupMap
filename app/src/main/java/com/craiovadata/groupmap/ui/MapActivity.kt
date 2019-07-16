package com.craiovadata.groupmap.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.utils.*
import com.craiovadata.groupmap.utils.GroupUtils.getShareKeyFromInstallRefferer
import com.craiovadata.groupmap.utils.GroupUtils.joinGroup
import com.craiovadata.groupmap.utils.GroupUtils.startActionShare
import com.craiovadata.groupmap.utils.MapUtils.setMarker
import com.craiovadata.groupmap.utils.MapUtils.zoomOnMe
import com.craiovadata.groupmap.utils.Util.buildAlertExitGroup
import com.craiovadata.groupmap.utils.Util.startLoginActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Marker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.*
import com.google.firebase.firestore.FieldValue.serverTimestamp
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.content_map.*

class MapActivity : BaseActivity() {
    private var map: GoogleMap? = null
    private val markers = hashMapOf<String, Marker?>()
    private var groupData: Map<String, Any>? = null
    private var positionListenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        setSupportActionBar(toolbar)
        init()
        initMap()
    }

    fun init(groupShareKey: String? = null) {
        if (groupShareKey != null) {
            getGroupData(groupShareKey) {
                checkMemberRole()
                requestPositionUpdatesFromOthers()
            }
        } else {
            getGroupShareKey { shareKey ->
                getGroupData(shareKey) {
                    checkMemberRole()
                    requestPositionUpdatesFromOthers()
                }
            }
        }
    }

    private fun getGroupShareKey(callback: (groupShareKey: String?) -> Unit) {
        groupId = getSharedPreferences("_", MODE_PRIVATE).getString(GROUP_ID, null)
        if (groupId == null) {  // first start
            groupId = DEFAULT_GROUP
            saveGroupIdToPref(DEFAULT_GROUP)
            getShareKeyFromInstallRefferer(this) { groupShareKeyFromRefferer ->
                val groupShareKey = groupShareKeyFromRefferer
                    ?: getShareKeyFromAppLinkIntent() // maybe prefs deleted
                callback.invoke(groupShareKey)
            }
        } else {
            val groupShareKey = getShareKeyFromAppLinkIntent()
            callback.invoke(groupShareKey)
        }
    }

    private fun getGroupData(groupShareKey: String?, listener: () -> Unit) {
        if (groupShareKey != null) {
            db.collection(GROUPS).whereEqualTo(GROUP_SHARE_KEY, groupShareKey).get()
                .addOnSuccessListener { querySnap ->
                    if (querySnap.documents.isEmpty()) return@addOnSuccessListener
                    groupData = querySnap.documents[0].data
                    groupId = querySnap.documents[0].id
                    saveGroupIdToPref(groupId)
                    listener.invoke()
                }
        } else {      // use saved groupId
            db.document("$GROUPS/$groupId").get()
                .addOnSuccessListener { snap ->
                    groupData = snap.data
                    listener.invoke()
                }
        }
    }

    private fun checkMemberRole() {
        if (groupId == DEFAULT_GROUP) {
            memberRole = ROLE_USER
            updateUI()
            setPositionsListener()
            return
        }
        val uid = currentUser?.uid ?: return
        db.document("$GROUPS/$groupId/$USERS/$uid").get()
            .addOnSuccessListener { snap ->
                userData = snap.data
                memberRole = (userData?.get(ROLE) as? Long)?.toInt() ?: ROLE_USER
                updateUI()
                inviteToGroupIfNeeded()
                setPositionsListener()
            }
    }

    private fun updateUI() {
        val groupName = groupData?.get(GROUP_NAME) as? String
        title = groupName ?: getString(R.string.app_name)
        invalidateOptionsMenu()
    }

    private fun inviteToGroupIfNeeded() {
        if (memberRole == ROLE_USER || memberRole == ROLE_ADMIN
            || groupId == DEFAULT_GROUP || groupId == null
        )
            return
        val groupName = groupData?.get(GROUP_NAME) as? String ?: ""
        val text = "Join \"$groupName\" group?"
        val snack = Snackbar.make(content_main, text, Snackbar.LENGTH_INDEFINITE)
            .setAction("Yes") {
                handleJoinAsked(true)
            }
        snack.show()
    }

    private fun handleJoinAsked(shouldStartLoginActivity: Boolean = false) {
        if (memberRole == ROLE_USER || memberRole == ROLE_ADMIN) return
        if (currentUser == null) {
            if (shouldStartLoginActivity)
                startLoginActivity(this, RC_SIGN_IN_ASKED_JOIN)
            return
        }

        val groupName = groupData?.get(GROUP_NAME) as? String
        joinGroup(currentUser!!.uid, groupId, groupName) { userRole ->
            if (userRole != null) {
                Snackbar.make(content_main, "You joined", Snackbar.LENGTH_LONG).show()
                memberRole = userRole
//                zoomOnMe(this, map)
                updateUI()
            } else {
                Snackbar.make(content_main, "Failed to join group", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestPositionUpdatesFromOthers() {
        if (memberRole != ROLE_USER && memberRole != ROLE_ADMIN)  // not member, not continue
            return
        val uid = currentUser?.uid ?: return
        db.document("$REQUESTS/$groupId")
            .set(mapOf(UID to uid, TIMESTAMP to serverTimestamp()))
            .addOnSuccessListener {
                Log.d(TAG, "request success")
            }
            .addOnFailureListener {
                Log.e(TAG, "request failed")
            }
    }

    override fun onActivityResultOk(requestCode: Int) {
        super.onActivityResultOk(requestCode)
        when (requestCode) {
            RC_SIGN_IN_ASKED_JOIN -> handleJoinAsked(false)
            RC_SIGN_IN -> {
                if (groupId != DEFAULT_GROUP ) {
                    reset()
                    init()
                }
                updateUI()
            }
            RC_CREATE_GROUP -> { // a group was just created
                // groupId was set in BaseActivity
                reset()
                init()
                Snackbar.make(content_main, "Group created. Invite participants?", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Ok") { startActionShare(this, groupData) }
            }
        }
    }

    fun reset() {
        map?.clear()
        positionListenerRegistration?.remove()
        groupData = null
        memberRole = null
    }

    override fun onStart() {
        super.onStart()
        requestPositionUpdatesFromOthers()
        setPositionsListener()
    }

    override fun onStop() {
        super.onStop()
        positionListenerRegistration?.remove()
    }

    private fun setPositionsListener() {
        if (map == null) return
        if (memberRole != ROLE_USER && memberRole != ROLE_ADMIN) return // return if not member

        positionListenerRegistration?.remove()
//        map?.clear()
        positionListenerRegistration = db.collection("$GROUPS/$groupId/$USERS")
            .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@EventListener
                }
                if (snapshots == null) return@EventListener
                for (dc in snapshots.documentChanges) {
                    val userDataDoc = dc.document
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> setMarker(applicationContext, userDataDoc, markers, map)
                        DocumentChange.Type.MODIFIED -> setMarker(applicationContext, userDataDoc, markers, map)
                        DocumentChange.Type.REMOVED -> {
                            Log.d(TAG, "Removed doc: ${userDataDoc.data}")
                            markers.remove(userDataDoc.id)
                        }
                    }
                }
            })
    }

    private fun exitGroup() {
        val uid = currentUser?.uid ?: return
        db.document("$USERS/$uid/$GROUPS/$groupId").delete()
            .addOnSuccessListener {
                groupId = null
                reset()
                saveGroupIdToPref(null)
                updateUI()
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
        val groupShareKey = getShareKeyFromAppLinkIntent()
        init(groupShareKey)
    }

    private fun getShareKeyFromAppLinkIntent(): String? {
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
        menu?.findItem(R.id.menu_item_group_info)?.isVisible = groupId != DEFAULT_GROUP && (memberRole == ROLE_USER || memberRole == ROLE_ADMIN)
        menu?.findItem(R.id.menu_item_exit)?.isVisible =
            groupId != DEFAULT_GROUP && (memberRole == ROLE_USER || memberRole == ROLE_ADMIN)
        menu?.findItem(R.id.menu_item_join)?.isVisible = memberRole == null
        menu?.findItem(R.id.menu_item_login)?.isVisible = currentUser == null && BuildConfig.DEBUG
        menu?.findItem(R.id.menu_item_logout)?.isVisible = currentUser != null && BuildConfig.DEBUG
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
                currentUser = auth.currentUser
                if (groupId != DEFAULT_GROUP ) {
                    reset()
                    init()
                }
                updateUI()
                return true
            }
            R.id.menu_item_login -> {
                startLoginActivity(this, RC_SIGN_IN)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun onFabClicked(view: View) {
        zoomOnMe(this, map)
//        GroupUtils.populateDefaultGroup()
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { map ->
            map.setMaxZoomPreference(16f)
            this.map = map
            setPositionsListener()
        }
    }

    companion object {
        private val TAG = MapActivity::class.java.simpleName
    }

}