package com.craiovadata.groupmap.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.utils.*
import com.craiovadata.groupmap.utils.GroupUtils.checkInstallReffererForGroupKey
import com.craiovadata.groupmap.utils.GroupUtils.getGroupKeyFromIntent
import com.craiovadata.groupmap.utils.GroupUtils.joinGroup
import com.craiovadata.groupmap.utils.GroupUtils.leaveGroup
import com.craiovadata.groupmap.utils.GroupUtils.startActionShare
import com.craiovadata.groupmap.utils.MapUtils.checkLocationPermission
import com.craiovadata.groupmap.utils.MapUtils.enableMyLocationOnMap
import com.craiovadata.groupmap.utils.MapUtils.setMarker
import com.craiovadata.groupmap.utils.MapUtils.zoomOnMe
import com.craiovadata.groupmap.utils.Util.getDownloadUri
import com.craiovadata.groupmap.utils.Util.saveMessagingDeviceToken
import com.craiovadata.groupmap.utils.Util.startLoginActivity
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
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
    private lateinit var groupId: String
    private var groupData: Map<String, Any>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        db = FirebaseFirestore.getInstance()
        setAuthStateListener()
        initMap()
//        populateDefaultGroup()
    }

    private fun initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync {
            mMap = it
            mMap?.setMaxZoomPreference(16f)
            initGroupData()
//            requestGpsLocationUpdates(this)
            enableMyLocationOnMap(this, mMap)

        }
        checkLocationPermission(this, mMap)
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

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val groupShareKey = groupData?.get(GROUP_SHARE_KEY)
        menu?.findItem(R.id.menu_item_share)?.isVisible = groupShareKey != null
        return super.onPrepareOptionsMenu(menu)
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
        val groupKey = getGroupKeyFromIntent(this)
        if (groupKey != null)
            getGroupData(groupKey)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST && grantResults.size == 1
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Start the service when the permission is granted
            enableMyLocationOnMap(this, mMap)
        } else {
            // permission not granted
//            finish()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        // Return true to display menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
//                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            R.id.action_create_group -> {
                startActivityForResult(
                    Intent(this, CreateGroupActivity::class.java),
                    CREATE_GROUP_REQUEST
                )
                return true
            }
            R.id.action_refresh -> {
                requestPositionUpdatesFromOthers()
                return true
            }
//            R.id.action_join_group_x -> {
//                joinGroup(this, groupId) {
//                    subscribeToGroupUpdates()
//                }
//                return true
//            }
            R.id.action_leave_group -> {
                leaveGroup(groupId)
                return true
            }
            R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                return true
            }
            R.id.action_login -> {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    startLoginActivity(this)
                } else {
                    Toast.makeText(this, "You are already logged in", Toast.LENGTH_SHORT).show()
                }
                return true
            }
            R.id.action_group_info -> {
                val intent = Intent(this, GroupInfoActivity::class.java)
                intent.putExtra(GROUP_ID, groupId)
                startActivity(intent)
                return true
            }
            R.id.menu_item_share -> {
                groupData?.get(GROUP_SHARE_KEY)?.let {
                    startActionShare(this, it)
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
        if (groupId == DEFAULT_GROUP) {
            setPositionsListener(mask = false)
            return
        }
        //  check if user is login and is member. Ask to join
        currentUser?.apply {
            db.collection(USERS).document(uid).collection(GROUPS).document(groupId).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result?.data?.get(JOINED) == true) {
                        requestPositionUpdatesFromOthers()
                        setPositionsListener(mask = false)
                    } else {
                        setPositionsListener(mask = true)
                        buildAlertJoinGroup { joined ->
                            if (joined) {
                                joinGroup(this@MainActivity, groupId) {
                                    requestPositionUpdatesFromOthers()
                                    setPositionsListener(mask = false)
                                    Snackbar.make(content_main, "You joined the group.", Snackbar.LENGTH_LONG)
                                        .setAction("Privacy policy") {
                                            val intent = Intent(this@MainActivity, PrivacyActivity::class.java)
                                            startActivity(intent)
                                        }.show()
                                }
                            } else {
                                // todo how to join group after dismissing
                            }

                        }
                    }
                }
        } ?: startLoginActivity(this)
    }

    private fun buildAlertJoinGroup(callback: (didJoin: Boolean) -> Unit) {
        Snackbar.make(content_main, "Join group?", Snackbar.LENGTH_INDEFINITE)
            .setAction("Yes") {
                callback.invoke(true)
            }
            .setAction("No") {
                callback.invoke(false)
            }
            .show()
    }

    var positionListenerRegistration: ListenerRegistration? = null
    private fun setPositionsListener(mask: Boolean) {
        mMap?.clear()

        positionListenerRegistration?.remove()
        positionListenerRegistration = db.collection(GROUPS).document(groupId).collection(DEVICES)
            .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@EventListener
                }
                if (snapshots == null) return@EventListener
                for (dc in snapshots.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> setMarker(this, dc.document, mask, mMarkers, mMap)
                        DocumentChange.Type.MODIFIED -> setMarker(this, dc.document, mask, mMarkers, mMap)
                        DocumentChange.Type.REMOVED -> Log.d(TAG, "Removed doc: ${dc.document.data}")
                    }
                }
            })
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

}

//  keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

//  keytool -list -v -keystore /Users/danalboteanu/AndroidStudioProjects/upload_key_2.jks -alias key0 -storepass 666666 -keypass 666666