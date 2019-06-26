package com.craiovadata.groupmap.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.utils.*
import com.craiovadata.groupmap.utils.GroupUtils.buildAlertJoinGroup
import com.craiovadata.groupmap.utils.GroupUtils.checkInstallReffererForGroupKey
import com.craiovadata.groupmap.utils.GroupUtils.getGroupKeyFromIntent
import com.craiovadata.groupmap.utils.GroupUtils.joinGroup
import com.craiovadata.groupmap.utils.MapUtils.setMarker
import com.craiovadata.groupmap.utils.MapUtils.zoomOnMe
import com.craiovadata.groupmap.utils.Util.buildAlertExitGroup
import com.craiovadata.groupmap.utils.Util.saveMessagingDeviceToken
import com.craiovadata.groupmap.utils.Util.startLoginActivity
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Marker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    private var mMap: GoogleMap? = null
    private var currentUser: FirebaseUser? = null
    private lateinit var db: FirebaseFirestore
    private val mMarkers = hashMapOf<String, Marker?>()
    private var groupId: String = NO_GROUP
    private var groupData: Map<String, Any>? = null
    private var positionListenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        db = FirebaseFirestore.getInstance()
        setAuthStateListener()
        initMap()
//        populateDefaultGroup()
        fabMyLocation.setOnClickListener {
            zoomOnMe(this, mMap)
        }
    }

    private fun initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync {
            mMap = it
            mMap?.setMaxZoomPreference(16f)
            initGroupData()
//            requestGpsLocationUpdates(this)
//            enableMyLocationOnMap(this, mMap)
//            if (groupId != DEFAULT_GROUP) {
//                checkLocationPermission(this) {
//                    mMap?.isMyLocationEnabled = true
//                    mMap?.uiSettings?.isMyLocationButtonEnabled = true
//
//                }
//            }
        }
    }

    private fun initGroupData() {
        val prefs = getSharedPreferences("_", MODE_PRIVATE)
        groupId = prefs.getString(GROUP_ID, NO_GROUP) ?: NO_GROUP
        if (groupId == NO_GROUP) {  // first start
            groupId = DEFAULT_GROUP
            prefs.edit().putString(GROUP_ID, groupId).apply()
            checkInstallReffererForGroupKey(this) { groupKey ->
                getGroupData(groupKey)
            }
        } else {
            val groupKey = getGroupKeyFromIntent(this)
            getGroupData(groupKey)
        }
    }

    private fun handleGroupData() {
        groupData?.apply {
            title = this[GROUP_NAME] as? String
            subscribeToGroupUpdates()
        }
    }

    private fun getGroupData(groupShareKey: String?) {
        if (groupShareKey != null) {

            db.collection(GROUPS).whereEqualTo(GROUP_SHARE_KEY, groupShareKey)
                .get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        task.result?.documents?.apply {
                            if (this.isNotEmpty()) {
                                groupData = this[0].data
                                groupId = this[0].id
                                getSharedPreferences("_", MODE_PRIVATE).edit()
                                    .putString(GROUP_ID, groupId).apply()
                            }
                        }
                    }
                    handleGroupData()
                }

        } else {
            db.collection(GROUPS).document(groupId).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        task.result?.apply { groupData = data }
                    }
                    handleGroupData()
                }
        }
    }

    private fun setAuthStateListener() {
        FirebaseAuth.getInstance().addAuthStateListener {
            currentUser = it.currentUser
            if (currentUser != null) {
                saveMessagingDeviceToken()
            } else {
                // todo need this?
//                deleteMessagingDeviceToken()        // needs write permission but NOT_AUTH
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val groupShareKey = getGroupKeyFromIntent(this) ?: return
        getGroupData(groupShareKey)
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
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        // Return true to display menu
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
                //authState listener will send token to server
//                FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
//                    val token = it.token
//                    Util.sendRegistrationToServer(currentUser, token)
//                }
                subscribeToGroupUpdates()
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                if (response == null) {
                    // User pressed the back button.
                    return
                }
                if (response.error?.errorCode == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                    return
                }

                if (response.error?.errorCode == ErrorCodes.UNKNOWN_ERROR) {
                    Toast.makeText(this, getString(R.string.error_default), Toast.LENGTH_SHORT).show();
                    return
                }
            }
        } else if (requestCode == CREATE_GROUP_REQUEST) {
            if (resultCode == RESULT_OK) {
                // a group was just created
                data?.getStringExtra(GROUP_ID)?.let { resultId ->
                    groupId = resultId
                    getGroupData(null)
                    zoomOnMe(this, mMap)
                    getSharedPreferences("_", MODE_PRIVATE).edit()
                        .putString(GROUP_ID, groupId).apply()

                }
            }
        }
    }

    private fun subscribeToGroupUpdates() {
        if (groupId == DEFAULT_GROUP) return setPositionsListener()
        else if (currentUser == null) return startLoginActivity(this)

        //  check if login and is member
        db.document("$USERS/${currentUser!!.uid}/$GROUPS/$groupId").get()
            .addOnCompleteListener { task ->
                var joined: Boolean? = null
                var banned: Boolean? = null
                if (task.isSuccessful) {
                    joined = task.result?.data?.get(JOINED) as? Boolean
                    banned = task.result?.data?.get(BANNED) as? Boolean
                }

                var shouldMask = true
                if (banned == null || !banned) {
                    if (joined != null && joined) {
                        requestPositionUpdatesFromOthers()
                        shouldMask = false
                    } else {
                        buildAlertJoinGroup(content_main) {
                            val groupName = groupData?.get(GROUP_NAME) as? String ?: "My Group"
                            joinGroup(groupId, groupName) {
                                requestPositionUpdatesFromOthers()
                                setPositionsListener()
                            }
                        }
                    }
                    setPositionsListener(mask = shouldMask)
                } else {
                    Toast.makeText(this, "Unable to connect to group", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun exitGroup() {
        getSharedPreferences("_", Context.MODE_PRIVATE).edit().putString(GROUP_ID, null).apply()
        title = getString(R.string.app_name)
        db.document("$USERS/${currentUser?.uid}/$groupId").delete()
            .addOnSuccessListener {
                Toast.makeText(this, "You left the group", Toast.LENGTH_LONG).show()
            }
        positionListenerRegistration?.remove()
        mMap?.clear()
        groupId = NO_GROUP
        groupData = null
        mMarkers.clear()
    }

    private fun setPositionsListener(mask: Boolean = false) {
        positionListenerRegistration?.remove()
        mMap?.clear()
        positionListenerRegistration = db.collection(GROUPS).document(groupId).collection(DEVICES)
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

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

}