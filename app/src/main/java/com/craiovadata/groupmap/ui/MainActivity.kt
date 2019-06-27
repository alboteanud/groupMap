package com.craiovadata.groupmap.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.utils.*
import com.craiovadata.groupmap.utils.GroupUtils.buildAlertJoinGroup
import com.craiovadata.groupmap.utils.GroupUtils.buildAlertLoginToJoinGroup
import com.craiovadata.groupmap.utils.GroupUtils.getShareKeyFromInstallRefferer
import com.craiovadata.groupmap.utils.GroupUtils.getShareKeyFromAppLinkIntent
import com.craiovadata.groupmap.utils.GroupUtils.joinGroup
import com.craiovadata.groupmap.utils.MapUtils.setMarker
import com.craiovadata.groupmap.utils.MapUtils.zoomOnMe
import com.craiovadata.groupmap.utils.Util.buildAlertExitGroup
import com.craiovadata.groupmap.utils.Util.sendDeviceTokenToServer
import com.craiovadata.groupmap.utils.Util.startLoginActivity
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Marker
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {
    private var mMap: GoogleMap? = null
    private var currentUser: FirebaseUser? = null
    private lateinit var db: FirebaseFirestore
    private val mMarkers = hashMapOf<String, Marker?>()
    private lateinit var groupId: String
    private var groupData: Map<String, Any>? = null
    private var positionListenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        db = FirebaseFirestore.getInstance()
        groupId = getSharedPreferences("_", MODE_PRIVATE).getString(GROUP_ID, NO_GROUP) ?: NO_GROUP
        initMap {
            initGroupConnection { groupShareKey ->
                getGroupData(groupShareKey)
            }
        }
        fabMyLocation.setOnClickListener {
            zoomOnMe(this, mMap)
//            populateDefaultGroup()
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
//            finish()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val groupShareKey = getShareKeyFromAppLinkIntent(this)
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
            getSharedPreferences("_", MODE_PRIVATE).edit().putString(GROUP_ID, groupId).apply()
            getShareKeyFromInstallRefferer(this) { groupShareKeyInstallRef ->
                if (groupShareKeyInstallRef != null)
                    callback.invoke(groupShareKeyInstallRef)
                else {
                    val groupShareKeyAppLink = getShareKeyFromAppLinkIntent(this)
                    callback.invoke(groupShareKeyAppLink)
                }
            }
        } else {
            val groupShareKey = getShareKeyFromAppLinkIntent(this)
            callback.invoke(groupShareKey)
        }
    }

    private fun getGroupData(groupShareKey: String?) {
        if (groupShareKey == null) {
            db.collection(GROUPS).document(groupId).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        groupData = task.result?.data
                    }
                    handleGroupData()
                }
        } else {
            db.collection(GROUPS).whereEqualTo(GROUP_SHARE_KEY, groupShareKey)
                .get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        task.result?.documents?.let { snap ->
                            if (snap.isNotEmpty()) {
                                groupData = snap[0].data
                                groupId = snap[0].id
                                getSharedPreferences("_", MODE_PRIVATE).edit()
                                    .putString(GROUP_ID, groupId).apply()
                            }
                        }
                    }
                    handleGroupData()
                }
        }
    }

    private fun handleGroupData() {
        val groupName = groupData?.get(GROUP_NAME) as? String ?: getString(R.string.app_name)
        title = groupName
        checkGroupAfiliation(groupName) { shouldMaskPins ->
            setPositionsListener(shouldMaskPins)
        }
    }

    private fun requestPositionUpdatesFromOthers() {
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == RESULT_OK) {
                sendDeviceTokenToServer()
                initGroupConnection { groupShareKey ->
                    getGroupData(groupShareKey)
                }
            } else {
                when {
                    response == null -> {       // User pressed the back button.
                    }
                    response.error?.errorCode == ErrorCodes.NO_NETWORK ->
                        Toast.makeText(this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show()
                    response.error?.errorCode == ErrorCodes.UNKNOWN_ERROR ->
                        Toast.makeText(this, getString(R.string.error_default), Toast.LENGTH_SHORT).show()
                }
                return
            }
        } else if (requestCode == CREATE_GROUP_REQUEST) {   // a group was just created
            if (resultCode == RESULT_OK) {
                data?.getStringExtra(GROUP_ID)?.let { extra ->
                    groupId = extra
                    getSharedPreferences("_", MODE_PRIVATE).edit()
                        .putString(GROUP_ID, groupId).apply()
                }
                getGroupData(null)
                zoomOnMe(this, mMap)
            }
        }
    }

    private fun checkGroupAfiliation(groupName: String, callback: (maskPins: Boolean) -> Unit) {
        if (groupId == DEFAULT_GROUP) {
            callback.invoke(false)
        } else if (currentUser == null) {
            callback.invoke(true)
            buildAlertLoginToJoinGroup(content_main) {
                startLoginActivity(this)
            }
        } else {     //  check if currentUser is a member of the group
            val ref = db.document("$GROUPS/$groupId/$USERS/${currentUser?.uid}")
            ref.get().addOnCompleteListener { task ->
                val result = task.result
                if (task.isSuccessful && result != null && result.exists()) {
                    val banned = result.data?.get(BANNED) as? Boolean ?: false
                    if (banned) {
                        Toast.makeText(this, "Unable to connect to group", Toast.LENGTH_LONG).show()
                    } else {
                        callback.invoke(false)
                        requestPositionUpdatesFromOthers()
                    }
                } else {
                    callback.invoke(true)
                    buildAlertJoinGroup(content_main) {
                        joinGroup(groupId, groupName) {
                            callback.invoke(false)
                            requestPositionUpdatesFromOthers()
                        }
                    }
                }
            }
        }
    }

    private fun setPositionsListener(mask: Boolean) {
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
                        DocumentChange.Type.ADDED -> setMarker(applicationContext, dc.document, mask, mMarkers, mMap)
                        DocumentChange.Type.MODIFIED -> setMarker(applicationContext, dc.document, mask, mMarkers, mMap)
                        DocumentChange.Type.REMOVED -> {
                            Log.d(TAG, "Removed doc: ${dc.document.data}")
                            mMarkers.remove(dc.document.id)
                        }
                    }
                }
            })
    }

    private fun exitGroup() {
        getSharedPreferences("_", Context.MODE_PRIVATE).edit().putString(GROUP_ID, null).apply()
        db.document("$USERS/${currentUser?.uid}/$groupId").delete()
            .addOnSuccessListener {
                Toast.makeText(this, "You left the group", Toast.LENGTH_LONG).show()
                title = getString(R.string.app_name)
            }
        positionListenerRegistration?.remove()
        mMap?.clear()
        groupId = NO_GROUP
        groupData = null
        mMarkers.clear()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

}