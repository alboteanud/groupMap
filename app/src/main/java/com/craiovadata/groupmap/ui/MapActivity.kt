package com.craiovadata.groupmap.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.model.CurrentMemberStatus
import com.craiovadata.groupmap.model.CurrentMemberStatus.Companion.STATE_ADMIN
import com.craiovadata.groupmap.model.CurrentMemberStatus.Companion.STATE_BANNED
import com.craiovadata.groupmap.model.CurrentMemberStatus.Companion.STATE_JOINED
import com.craiovadata.groupmap.model.CurrentMemberStatus.Companion.STATE_NOT_JOINED
import com.craiovadata.groupmap.model.CurrentMemberStatus.Companion.STATE_SIGNED_OFF
import com.craiovadata.groupmap.utils.*
import com.craiovadata.groupmap.utils.GroupUtils.getShareKeyFromInstallRefferer
import com.craiovadata.groupmap.utils.GroupUtils.joinGroup
import com.craiovadata.groupmap.utils.MapUtils.setMarker
import com.craiovadata.groupmap.utils.MapUtils.zoomOnMe
import com.craiovadata.groupmap.utils.Util.buildAlertExitGroup
import com.craiovadata.groupmap.utils.Util.startLoginActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Marker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.content_map.*

class MapActivity : BaseActivity() {

    private var mMap: GoogleMap? = null
    private val mMarkers = hashMapOf<String, Marker?>()
    private var groupData: Map<String, Any>? = null
    private var positionListenerRegistration: ListenerRegistration? = null
    private val currentMember = CurrentMemberStatus()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        setSupportActionBar(toolbar)
        groupId = getSharedPreferences("_", MODE_PRIVATE).getString(GROUP_ID, NO_GROUP) ?: NO_GROUP
        initMap {
            initGroupConnection { groupShareKey ->
                getGroupData(groupShareKey)
            }
        }
        fabMyLocation.setOnClickListener {
            zoomOnMe(this, mMap)
//            populateDefaultGroup()
//            Util.deleteDB("sent text")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_AND_ZOOM_ME && grantResults.size == 1
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {     // coming from MapUtils.checkLocationPermissions() - ActivityCompat.requestPermissions()
            // Start the service when the permission is granted
            zoomOnMe(this, mMap)
        } else {
            // permission not granted
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val groupShareKey = getShareKeyFromAppLinkIntent()
        getGroupData(groupShareKey)
    }

    private fun initMap(callback: () -> Unit) {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { map ->
            mMap = map
            mMap?.setMaxZoomPreference(16f)
            callback.invoke()
        }
    }

    private fun initGroupConnection(callback: (shareKey: String?) -> Unit) {
        if (groupId == NO_GROUP) {  // first start
            groupId = DEFAULT_GROUP
            saveGroupIdToPref(groupId)
            getShareKeyFromInstallRefferer(this) { groupShareKey ->
                if (groupShareKey != null)
                    callback.invoke(groupShareKey)
                else {
                    val groupShareKeyLnk = getShareKeyFromAppLinkIntent()
                    callback.invoke(groupShareKeyLnk)
                }
            }
        } else {
            val groupShareKey = getShareKeyFromAppLinkIntent()
            callback.invoke(groupShareKey)
        }
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

    private fun getGroupData(groupShareKey: String?) {
        if (groupShareKey != null) {
            db.collection(GROUPS).whereEqualTo(GROUP_SHARE_KEY, groupShareKey)
                .get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        task.result?.documents?.let { snap ->
                            if (snap.isNotEmpty()) {
                                groupData = snap[0].data
                                groupId = snap[0].id
                                saveGroupIdToPref(groupId)
                            }
                        }
                    }
                    verifyMemberStatus()
                }
        } else {
            // go on with the saved groupId
            db.collection(GROUPS).document(groupId).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        groupData = task.result?.data
                    }
                    verifyMemberStatus()
                }
        }
    }

    // todo adjust menus and other ui elements according to state
    private fun updateUI() {
        val groupName = groupData?.get(GROUP_NAME) as? String ?: getString(R.string.app_name)
        title = groupName
        var shouldMaskPins = true
        when (currentMember.state) {
            STATE_SIGNED_OFF -> {
                Snackbar.make(content_main, "Login and join \"$groupName\" group ?", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Yes") { startLoginActivity(this) }.show()
            }
            STATE_BANNED -> {
                Toast.makeText(this, "Unable to connect to group", Toast.LENGTH_LONG).show()
            }
            STATE_NOT_JOINED -> {
                // building join alert
                val snack = Snackbar.make(content_main, "Join \"$groupName\" group ?", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Yes") { joinGroup(groupId, groupName) { verifyMemberStatus() } }
                snack.show()
                Handler().postDelayed(Runnable {
                    snack.dismiss()
            // todo        addMenuItemJoin()
                }, 11000)
            }
            STATE_JOINED -> {
                shouldMaskPins = false
                requestPositionUpdatesFromOthers()
            }
            STATE_ADMIN -> {
                shouldMaskPins = false
                showShareGroup = true
                invalidateOptionsMenu()
            }
        }

        setPositionsListener(shouldMaskPins)
    }

    private fun verifyMemberStatus() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            currentMember.state = STATE_SIGNED_OFF
            updateUI()
            return
        }
        val uid = currentUser.uid
        db.document("$GROUPS/$groupId/$USERS/$uid").get()
            .addOnCompleteListener { task ->
                currentMember.state = STATE_NOT_JOINED
                val result = task.result
                if (task.isSuccessful && result != null && result.exists()) {
                    val data = result.data
                    val banned = data?.get(BANNED) as? Boolean ?: false
                    val joined = data?.get(JOINED) as? Boolean ?: true
                    val isAdmin = data?.get(IS_ADMIN) as? Boolean ?: false
                    if (isAdmin) {
                        currentMember.state = STATE_ADMIN
                    } else if (joined) {
                        currentMember.state = STATE_JOINED
                    } else if (banned) {
                        currentMember.state = STATE_BANNED
                    }
                }
                updateUI()
            }
    }


    private fun requestPositionUpdatesFromOthers() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.apply {
            // Update one field, creating the document if it does not already exist.
            val data = HashMap<String, Any?>()
            data[UID] = uid
            if (!displayName.isNullOrBlank()) data[NAME] = displayName
            else data[NAME] = email
            data[TIMESTAMP] = FieldValue.serverTimestamp()
            db.document("$GROUPS/$groupId/$UPDATE_REQUEST/0").set(data)
        }
    }

    private var showShareGroup = false
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_map, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menu_item_add_members)?.isVisible = showShareGroup
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            R.id.menu_item_new_group -> {
                startActivityForResult(Intent(this, CreateGroupActivity::class.java), CREATE_GROUP_REQUEST)
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
                buildAlertExitGroup(this, groupId, groupName) {
                    exitGroup()
                }
                return true
            }
            R.id.menu_item_add_members -> {
                val shareKey = groupData?.get(GROUP_SHARE_KEY) as? String
                GroupUtils.startActionShare(this, shareKey)
                return true
            }
            R.id.menu_item_join -> {
                verifyMemberStatus()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onLoginSuccess() {
        super.onLoginSuccess()
        initGroupConnection { groupShareKey ->
            getGroupData(groupShareKey)
        }
    }

    override fun onGroupCreated() {
        super.onGroupCreated()
        getGroupData(null)
        // will ask map permision
        zoomOnMe(this, mMap)
    }

    private fun setPositionsListener(mask: Boolean) {
        if (currentMember.state != STATE_BANNED) return
        var shouldMask = mask
        if (groupId == DEFAULT_GROUP) shouldMask = false

        positionListenerRegistration?.remove()
        mMap?.clear()
        positionListenerRegistration = db.collection("$GROUPS/$groupId/$DEVICES")
            .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@EventListener
                }
                if (snapshots == null) return@EventListener
                for (dc in snapshots.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> setMarker(
                            applicationContext,
                            dc.document,
                            shouldMask,
                            mMarkers,
                            mMap
                        )
                        DocumentChange.Type.MODIFIED -> setMarker(
                            applicationContext,
                            dc.document,
                            shouldMask,
                            mMarkers,
                            mMap
                        )
                        DocumentChange.Type.REMOVED -> {
                            Log.d(TAG, "Removed doc: ${dc.document.data}")
                            mMarkers.remove(dc.document.id)
                        }
                    }
                }
            })
    }

    private fun exitGroup() {
        // todo !!daca apasa exitGr prima data!
        groupId = DEFAULT_GROUP
        saveGroupIdToPref(groupId)
        val currentUser = FirebaseAuth.getInstance().currentUser
        val refUsr = db.document("$USERS/${currentUser?.uid}/$groupId")
        val refGrUsr = db.document("$GROUPS/$groupId/$USERS/${currentUser?.uid}")

        val batch = db.batch()
        batch.update(refUsr, mapOf(JOINED to false))
        batch.update(refGrUsr, mapOf(JOINED to false))
        batch.commit()

        // todo improve here
        Toast.makeText(this, "You left the group", Toast.LENGTH_LONG).show()
        title = getString(R.string.app_name)
        positionListenerRegistration?.remove()
        mMap?.clear()
        groupId = DEFAULT_GROUP
        groupData = null
        mMarkers.clear()
    }

    companion object {
        private val TAG = MapActivity::class.java.simpleName
    }

}