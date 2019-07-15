package com.craiovadata.groupmap.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.craiovadata.groupmap.BuildConfig
import com.craiovadata.groupmap.R
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.iid.FirebaseInstanceId
import java.io.Serializable


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

    fun startLoginActivity(activity: Activity, requestCode: Int) {
        val providers =
            arrayListOf(AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build())
        activity.startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(true)
                .setLogo(R.drawable.ic_person_pin)
//                        .setAlwaysShowSignInMethodScreen(true)
//                .build(), RC_SIGN_IN
                .build(), requestCode
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
                callback.invoke()
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

    fun sendTokenToServer() {
        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { result ->
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener
            FirebaseFirestore.getInstance().document("$USERS/$uid")
                .set(mapOf(TOKEN to result.token), SetOptions.merge()) // PAUSE can be there
        }
    }

    fun getDummyUsers(): Array<HashMap<String, Serializable>> {
        return arrayOf(
            hashMapOf(
                NAME to "Dan",
                PHOTO_URL to "https://lh3.googleusercontent.com/-JwqhJ989hXw/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3rdvLnxofP2V96sjhxQ0lXf2HrRgKg.CMID/s64-c-mo/photo.jpg",
                LOCATION to hashMapOf(LATITUDE to 40.6608, LONGITUDE to -73.949),
                VISIBILITY to "public"
            ),
            hashMapOf(
                NAME to "Anca",
                PHOTO_URL to "https://lh3.googleusercontent.com/-dFfcBHiIoTk/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3rd8xLcopVHADHFqH_8wDBHo5nd6IQ/s64-c-mo/photo.jpg",
                LOCATION to hashMapOf(LATITUDE to 40.596, LONGITUDE to -74.143)
                , VISIBILITY to "public"
            ),
            hashMapOf(
                NAME to "Mihaela",
                PHOTO_URL to "https://lh3.googleusercontent.com/-o3phkZogqYY/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3reUul-g6ccRH73tgTbvAVUVAU5igQ.CMID/s64-c-mo/photo.jpg",
                LOCATION to hashMapOf(LATITUDE to 40.828, LONGITUDE to -74.067)
                , VISIBILITY to "public"
            ),
            hashMapOf(
                NAME to "Victoria",
                PHOTO_URL to "https://lh3.googleusercontent.com/-aKPXx00bFf4/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3rfH3oBxY63PQ_FrT48c9yX2wf-U9A.CMID/s64-c-mo/photo.jpg",
                LOCATION to hashMapOf(LATITUDE to 40.455, LONGITUDE to -74.35)
                , VISIBILITY to "public"
            ),
            hashMapOf(
                NAME to "AnDroid",
                PHOTO_URL to "https://lh3.googleusercontent.com/-r_wtGPpwhGo/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3reJ6DA346tlC_Kv3OLbE_qdmt9r6Q.CMID/s64-c-mo/photo.jpg",
                LOCATION to hashMapOf(LATITUDE to 40.774, LONGITUDE to -73.461)
                , VISIBILITY to "public"
            ),
            hashMapOf(
                NAME to "Ionel",
                PHOTO_URL to "https://lh3.googleusercontent.com/-FzX2I30Hhkw/AAAAAAAAAAI/AAAAAAAAFHY/ACHi3rc8vTf6ZzuNErb0cr5Ir9fem8AuvA.CMID/s64-c-mo/photo.jpg",
                LOCATION to hashMapOf(LATITUDE to 40.614, LONGITUDE to -73.5)
                , VISIBILITY to "public"
            )
        )
    }


    fun deleteDB(text: String): Task<String>? {
        // Create the arguments to the callable function.
        val data = mapOf(
            "text" to text,
            "push" to true
        )
        val functions = FirebaseFunctions.getInstance()
        return functions
            .getHttpsCallable("deleteDB")
            .call(data)
            .continueWith { task ->
                // This continuation runs on either success or failure, but if the task
                // has failed then result will throw an Exception which will be
                // propagated down.
                val result = task.result?.data as? String
                result
            }
    }


    private fun saveUserDataToPrefs(context: Context, data: Map<String, Any>) {
        val userName = data[NAME] as? String ?: return
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val savedName = prefs.getString("pref_key_user_name", null)
        if (userName != savedName)
            prefs.edit().putString("pref_key_user_name", userName).apply()
    }


}


