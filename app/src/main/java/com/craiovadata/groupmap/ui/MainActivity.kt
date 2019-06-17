package com.craiovadata.groupmap.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.utils.*
import com.craiovadata.groupmap.utils.GroupUtils.joinGroup
import com.craiovadata.groupmap.utils.GroupUtils.leaveGroup
import com.craiovadata.groupmap.utils.GroupUtils.startActionShare
import com.craiovadata.groupmap.utils.MapUtils.checkLocationPermission
import com.craiovadata.groupmap.utils.MapUtils.enableMyLocationOnMap
import com.craiovadata.groupmap.utils.MapUtils.requestMyLocationUpdates
import com.craiovadata.groupmap.utils.Util.getDownloadUri
import com.craiovadata.groupmap.utils.Util.saveMessagingDeviceToken
import com.craiovadata.groupmap.utils.Util.startLoginActivity
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var currentUser: FirebaseUser? = null
    private lateinit var db: FirebaseFirestore
    private val mMarkers = hashMapOf<String, Marker?>()
    private lateinit var groupId: String
        var groupData:Map<String, Any>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        db = FirebaseFirestore.getInstance()
        groupId = getSharedPreferences("_", MODE_PRIVATE)
            .getString(KEY_GROUP_ID, NO_GROUP) ?: NO_GROUP
        setAuthStateListener()
        initMap()
//        populateDefaultGroup()
    }

    private fun initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        checkLocationPermission(this, mMap)
    }

    private fun initGroupId() {
        if (groupId == NO_GROUP) {  // first start
            groupId = DEFAULT_GROUP
            checkInstallReffererForGroupKey { groupKey ->
                getGroupData(groupKey)
            }
            getSharedPreferences("_", Context.MODE_PRIVATE).edit()
                .putString(KEY_GROUP_ID, groupId).apply()
        } else {
            val groupKey = getGroupKeyFromIntent()
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
        if (groupShareKey!=null){

            db.collection(GROUPS).whereEqualTo(GROUP_SHARE_KEY, groupShareKey)
                .get().addOnCompleteListener {task ->
                    if (task.isSuccessful){
                        task.result?.documents?.apply {
                            if(this.isNotEmpty()){
                                groupData = this[0].data
                                groupId = this[0].id
                                getSharedPreferences("_", Context.MODE_PRIVATE).edit()
                                    .putString(KEY_GROUP_ID, groupId).apply()
                            }
                        }
                    }
                    handleGroupData()
                }

        } else{

            db.collection(GROUPS).document(groupId).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        task.result?.apply { groupData = data }
                    }
                    handleGroupData()
                }
        }

    }

    private fun checkInstallReffererForGroupKey(callback: (groupKey: String?) -> Unit) {
        // check if url contains group link code
        val referrerClient = InstallReferrerClient.newBuilder(this).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {

            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                    // Connection established
                    val response: ReferrerDetails = referrerClient.installReferrer
                    val endUrl = response.installReferrer
                    val downloadUri = getDownloadUri(this@MainActivity, endUrl)
                    val groupKey = downloadUri.getQueryParameter("utm_content")
                    callback.invoke(groupKey)
                } else {
                    callback.invoke(null)
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                callback.invoke(null)
            }
        })
    }

    private fun getGroupKeyFromIntent(): String? {
        val appLinkIntent = intent
//        val appLinkAction = appLinkIntent.action
        val appLinkData = appLinkIntent.data

        if (appLinkData != null) {
            val segments = appLinkData.pathSegments
            if (segments.size >= 2) {
                if (segments[0] == "group") {
                    return segments[1]
                }
            }
        }
        return null
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val  groupShareKey = groupData?.get(GROUP_SHARE_KEY)
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
        val groupKey = getGroupKeyFromIntent()
        if (groupKey != null)
            getGroupData(groupKey)
    }

    private fun requestPositionUpdates() {
        currentUser?.apply {
            // Update one field, creating the document if it does not already exist.
            val requestData = HashMap<String, Any?>()
            requestData[UID] = uid
            requestData[NAME] = displayName ?: email
            requestData[TIME] = FieldValue.serverTimestamp()
            val request = HashMap<String, Any>()
            request[UPDATE_REQUEST] = requestData

            db.collection(GROUPS).document(groupId).set(request, SetOptions.merge())
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.setMaxZoomPreference(16f)
        initGroupId()
        requestMyLocationUpdates(this)
        enableMyLocationOnMap(this, mMap)
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

    private fun setMarker(document: QueryDocumentSnapshot) {
        // When a location update is received, put or update
        // its value in mMarkers, which contains all the markers
        // for locations received, so that we can build the
        // boundaries required to show them all on the map at once
        val key = document.id
        var locationData = document.data[LOCATION] ?: return
        locationData = locationData as HashMap<*, *>
        val lat = locationData[LATITUDE] as Double
        val lng = locationData[LONGITUDE] as Double
        val location = LatLng(lat, lng)
        if (!mMarkers.containsKey(key)) {
            val userName = document.data[NAME] as String
            val marker = mMap?.addMarker(MarkerOptions().title(userName).position(location))
            val iconUrl = document.data[PHOTO_URL]?.toString()
            setMarkerIcon(marker, iconUrl)
            mMarkers[key] = marker
        } else {
            mMarkers[key]?.position = location
        }
        val builder = LatLngBounds.Builder()
        for (marker in mMarkers.values) {
            marker?.let { builder.include(it.position) }

        }
        if (mMarkers.isNotEmpty()) {
            val padding = 80
            mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), padding))
        }

    }

    private fun setMarkerIcon(marker: Marker?, iconUrl: String?) {
        if (iconUrl == null) return
        if (marker == null) return
        Glide.with(applicationContext)
            .asBitmap()
            .load(iconUrl)
            .into(object : CustomTarget<Bitmap>() {

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val icon = BitmapDescriptorFactory.fromBitmap(resource)
                    marker.setIcon(icon)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    val icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_person_pin)
                    marker.setIcon(icon)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}

            })
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
                requestPositionUpdates()
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
                intent.putExtra(KEY_GROUP_ID, groupId)
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
                // a group was created
                data?.getStringExtra(KEY_GROUP_ID)?.let { resultId ->
                    groupId = resultId
                    getGroupData(null)
                    getSharedPreferences("_", Context.MODE_PRIVATE).edit()
                        .putString(KEY_GROUP_ID, groupId).apply()
                }
            }
        }
    }

    private fun subscribeToGroupUpdates() {
        if (groupId == DEFAULT_GROUP) {
            setPositionsListener()
            return
        }
        //  check if user is login and is member. Ask to join
        currentUser?.apply {
            db.collection(USERS).document(uid).collection(GROUPS).document(groupId).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result?.data?.get(JOINED) == true) {
                        requestPositionUpdates()
                        setPositionsListener()
                    } else {
                        buildAlertJoinGroup {
                            requestPositionUpdates()
                            setPositionsListener()
                        }
                    }
                }
        } ?: startLoginActivity(this)
    }

    private fun buildAlertJoinGroup(callback: () -> Unit) {
        Snackbar.make(content_main, "Join group?", Snackbar.LENGTH_INDEFINITE)
            .setAction("Yes") {
                joinGroup(this@MainActivity, groupId) {
                    callback.invoke()
                    Snackbar.make(content_main, "You joined the group.", Snackbar.LENGTH_LONG)
                        .setAction("Privacy policy") {
                            startActivity(Intent(this, PrivacyActivity::class.java))
                        }.show()
                }
            }.show()
    }

    private fun setPositionsListener() {
        mMap?.clear()
        // add others on map
        db.collection(GROUPS).document(groupId).collection(DEVICES)
            .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@EventListener
                }
                if (snapshots == null) return@EventListener
                for (dc in snapshots.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> setMarker(dc.document)
                        DocumentChange.Type.MODIFIED -> setMarker(dc.document)
                        DocumentChange.Type.REMOVED -> Log.d(TAG, "Removed doc: ${dc.document.data}")
                    }
                }
            })
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

}

