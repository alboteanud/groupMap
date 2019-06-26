package com.craiovadata.groupmap.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.craiovadata.groupmap.BuildConfig
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.ui.CreateGroupActivity
import com.craiovadata.groupmap.utils.GroupUtils.exitGroup
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import org.jetbrains.annotations.TestOnly
import java.io.Serializable
import java.util.*
import kotlin.collections.HashMap


object Util {

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    fun checkPlayServices(activity: Activity): Boolean {

        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(activity)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(
                    activity, resultCode,
                    PLAY_SERVICES_RESOLUTION_REQUEST
                ).show()
            } else {
                Log.i("Util", "This device is not supported.")
//                activity.finish()
            }
            return false
        }
        return true
    }

    private const val PLAY_SERVICES_RESOLUTION_REQUEST = 9000

    private fun hideKeyboard(activity: Activity) {
        val view = activity.currentFocus
        if (view != null) {
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    fun sendRegistrationToServer(currentUser: FirebaseUser?, token: String?) {
        if (currentUser == null || token == null) return
        val ref = FirebaseFirestore.getInstance().collection(FCM_TOKENS).document(token)
        val userData = getUserData()
        if (userData != null)
            ref.set(userData)
    }

    fun getUserData(): HashMap<String, Any?>? {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return null

        val user = HashMap<String, Any?>()
        user[UID] = currentUser.uid
        var name = currentUser.displayName
        if (name == null || name.isBlank()) name = currentUser.email
        user[NAME] = name
        user[EMAIL] = currentUser.email
        currentUser.photoUrl?.let { uri ->
            user[PHOTO_URL] = uri.toString()
        }
        return user
    }

    fun startLoginActivity(activity: Activity) {
        val providers =
            arrayListOf(AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build())
        activity.startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(true)
                .setLogo(R.drawable.ic_person_pin)
//                        .setAlwaysShowSignInMethodScreen(true)
                .build(), RC_SIGN_IN
        )
    }


    fun buildAlertMessageNoGps(context: Context) {
        val builder = AlertDialog.Builder(context);
        builder.setMessage("Your GPS is disabled. Do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ -> context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            .setNegativeButton("No") { dialog, _ -> dialog.cancel(); }
        val alert = builder.create();
        alert.show();
    }

    fun buildAlertExitGroup(context: Context, groupId: String, groupName: String, callback: () -> Unit) {
        val builder = AlertDialog.Builder(context);
        builder.setMessage("Exit \"$groupName\" group?")
            .setCancelable(false)
            .setPositiveButton("EXIT") { _, _ ->
                exitGroup(groupId) {
                    callback.invoke()
                }
            }
            .setNegativeButton("CANCEL") { dialog, _ -> dialog.cancel(); }
        val alert = builder.create();
        alert.show();
    }


    fun getDownloadUri(context: Context, endUrl: String): Uri {
        val baseUrl = context.getString(R.string.base_download_url) + endUrl
        val appId = BuildConfig.APPLICATION_ID
        return Uri.parse(baseUrl)
            .buildUpon()
            .appendQueryParameter("id", appId)
            .build()
    }

    fun saveMessagingDeviceToken() {
        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { result ->
            FirebaseAuth.getInstance().currentUser?.apply {
                val token = result.token
                val ref = FirebaseFirestore.getInstance()
                    .collection(FCM_TOKENS).document(token)
                val userData = HashMap<String, Any>()
                userData[UID] = uid
                ref.set(userData)
            }
        }
    }

    const val TAG = "Util"


    fun getDummyUsers(): Array<HashMap<String, Serializable>> {
        return arrayOf(
            hashMapOf(
                NAME to "Dan",
                PHOTO_URL to "https://lh3.googleusercontent.com/-JwqhJ989hXw/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3rdvLnxofP2V96sjhxQ0lXf2HrRgKg.CMID/s64-c-mo/photo.jpg",
                LOCATION to hashMapOf(LATITUDE to 40.6608, LONGITUDE to -73.949)
            ),
            hashMapOf(
                NAME to "Anca",
                PHOTO_URL to "https://lh3.googleusercontent.com/-dFfcBHiIoTk/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3rd8xLcopVHADHFqH_8wDBHo5nd6IQ/s64-c-mo/photo.jpg",
                LOCATION to hashMapOf(LATITUDE to 40.596, LONGITUDE to -74.143)
            ),
            hashMapOf(
                NAME to "Mihaela",
                PHOTO_URL to "https://lh3.googleusercontent.com/-o3phkZogqYY/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3reUul-g6ccRH73tgTbvAVUVAU5igQ.CMID/s64-c-mo/photo.jpg",
                LOCATION to hashMapOf(LATITUDE to 40.828, LONGITUDE to -74.067)
            ),
            hashMapOf(
                NAME to "Victoria",
                PHOTO_URL to "https://lh3.googleusercontent.com/-aKPXx00bFf4/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3rfH3oBxY63PQ_FrT48c9yX2wf-U9A.CMID/s64-c-mo/photo.jpg",
                LOCATION to hashMapOf(LATITUDE to 40.455, LONGITUDE to -74.35)
            ),
            hashMapOf(
                NAME to "Droid",
                PHOTO_URL to "https://lh3.googleusercontent.com/-r_wtGPpwhGo/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3reJ6DA346tlC_Kv3OLbE_qdmt9r6Q.CMID/s64-c-mo/photo.jpg",
                LOCATION to hashMapOf(LATITUDE to 40.774, LONGITUDE to -73.461)
            )
        )
    }


    fun deleteMessagingDeviceToken() {
        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { result ->
            val token = result.token
            val db = FirebaseFirestore.getInstance()
            val ref = db.collection(FCM_TOKENS).document(token)
            ref.delete()
        }
    }

}


