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
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.iid.FirebaseInstanceId
import java.util.HashMap
import android.util.DisplayMetrics


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
                .setLogo(R.drawable.people_map)
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

    fun buildAlertExitGroup(context: Context, groupName: String, callback: () -> Unit) {
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

    fun getDummyUsers(): Array<HashMap<String, out Any>> {
        return arrayOf(
            hashMapOf(
                NAME to "Dan",
                PHOTO_URL to "https://lh3.googleusercontent.com/-JwqhJ989hXw/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3rdvLnxofP2V96sjhxQ0lXf2HrRgKg.CMID/s64-c-mo/photo.jpg",
                LOCATION to GeoPoint(40.66, -73.949),
                VISIBILITY to "public"
            ),
            hashMapOf(
                NAME to "Anca",
                PHOTO_URL to "https://lh3.googleusercontent.com/-dFfcBHiIoTk/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3rd8xLcopVHADHFqH_8wDBHo5nd6IQ/s64-c-mo/photo.jpg",
                LOCATION to GeoPoint(40.6, -74.14)
                , VISIBILITY to "public"
            ),
            hashMapOf(
                NAME to "Mihaela",
                PHOTO_URL to "https://lh3.googleusercontent.com/-o3phkZogqYY/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3reUul-g6ccRH73tgTbvAVUVAU5igQ.CMID/s64-c-mo/photo.jpg",
                LOCATION to GeoPoint(40.828, -74.067)
            ),
            hashMapOf(
                NAME to "Victoria",
                PHOTO_URL to "https://lh3.googleusercontent.com/-aKPXx00bFf4/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3rfH3oBxY63PQ_FrT48c9yX2wf-U9A.CMID/s64-c-mo/photo.jpg",
                LOCATION to GeoPoint(40.455, -74.35)
            ),
            hashMapOf(
                NAME to "AnDroid",
                PHOTO_URL to "https://lh3.googleusercontent.com/-r_wtGPpwhGo/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3reJ6DA346tlC_Kv3OLbE_qdmt9r6Q.CMID/s64-c-mo/photo.jpg",
                LOCATION to GeoPoint(41.2, -73.461)
            ),
            hashMapOf(
                NAME to "Ionel",
                PHOTO_URL to "https://lh3.googleusercontent.com/-FzX2I30Hhkw/AAAAAAAAAAI/AAAAAAAAFHY/ACHi3rc8vTf6ZzuNErb0cr5Ir9fem8AuvA.CMID/s64-c-mo/photo.jpg",
                LOCATION to GeoPoint(40.614, -73.5)
            ),
            hashMapOf(
                NAME to "Ivona",
                PHOTO_URL to "https://firebasestorage.googleapis.com/v0/b/groupmap-6b346.appspot.com/o/faces%2Fface_1.jpg?alt=media&token=0cf56449-f626-45f4-9932-4aad4a48ab55",
                LOCATION to GeoPoint(40.814, -73.4)
            ),
            hashMapOf(
                NAME to "Helen",
                PHOTO_URL to "https://firebasestorage.googleapis.com/v0/b/groupmap-6b346.appspot.com/o/faces%2Fface_2.jpg?alt=media&token=136beae6-4de7-4a4b-a79e-d3e2e24072b2",
                LOCATION to GeoPoint(41.15, -73.05)
            ),
            hashMapOf(
                NAME to "Michael",
                PHOTO_URL to "https://firebasestorage.googleapis.com/v0/b/groupmap-6b346.appspot.com/o/faces%2Fface_3.jpg?alt=media&token=426a4174-5235-4a9a-a293-c540648219f0",
                LOCATION to GeoPoint(40.68, -73.7)
            ),
            hashMapOf(
                NAME to "Edwin",
                PHOTO_URL to "https://firebasestorage.googleapis.com/v0/b/groupmap-6b346.appspot.com/o/faces%2Fface_4.jpg?alt=media&token=61a77fd9-61b6-4590-978b-81d7a464c17c",
                LOCATION to GeoPoint(40.9, -73.1)
            ),
            hashMapOf(
                NAME to "Alice",
                PHOTO_URL to "https://firebasestorage.googleapis.com/v0/b/groupmap-6b346.appspot.com/o/faces%2Fface_5.jpg?alt=media&token=55854484-39e3-4655-986c-af672a8bc88f",
                LOCATION to GeoPoint(41.0, -73.6)
            ),
            hashMapOf(
                NAME to "Kevin",
                PHOTO_URL to "https://firebasestorage.googleapis.com/v0/b/groupmap-6b346.appspot.com/o/faces%2Fface_6.jpg?alt=media&token=3ec22af7-f665-4ce5-be62-0f0c4f8ecbf2",
                LOCATION to GeoPoint(40.9, -73.1)
            ),
            hashMapOf(
                NAME to "Julia",
                PHOTO_URL to "https://firebasestorage.googleapis.com/v0/b/groupmap-6b346.appspot.com/o/faces%2Fface_7.jpg?alt=media&token=36f171de-bccd-4c69-b557-7c43c86a860a",
                LOCATION to GeoPoint(40.3, -74.5)
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


    fun convertDpToPixel(dp: Float, context: Context): Int {
        return (dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    fun convertPixelsToDp(px: Float, context: Context): Float {
        return px / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

}


