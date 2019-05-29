package com.craiovadata.groupmap

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.inputmethod.InputMethodManager
import com.craiovadata.groupmap.MapActivity.Companion.FCM_TOKENS
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*


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
                apiAvailability.getErrorDialog(activity, resultCode,
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
        val userData = getUserData_(currentUser)
        ref.set(userData)
    }

    fun getUserData_(currentUser: FirebaseUser): HashMap<String, Any?> {
        val userData = HashMap<String, Any?>()
//        userData["uid"] = currentUser.uid
        userData["email"] = currentUser.email
        userData["name"] = currentUser.displayName ?: currentUser.email
        currentUser.photoUrl?.let {
            userData["photoUrl"] = it.toString()
        }
        return userData
    }

}


