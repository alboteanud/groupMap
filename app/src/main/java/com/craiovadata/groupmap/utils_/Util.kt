package com.craiovadata.groupmap.utils_

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.craiovadata.groupmap.BuildConfig
import com.craiovadata.groupmap.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import android.util.DisplayMetrics
import android.view.View
import androidx.core.content.ContextCompat.startActivity
import com.craiovadata.groupmap.model.Group
import com.craiovadata.groupmap.viewmodel.GroupDisplay
import com.firebase.ui.auth.AuthUI

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

    fun buildAlertMessageNoGps(context: Context) {
        val builder = AlertDialog.Builder(context);
        builder.setMessage("Your GPS is disabled. Do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ -> context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            .setNegativeButton("No") { dialog, _ -> dialog.cancel(); }
        val alert = builder.create();
        alert.show();
    }

    fun buildAlertExitGroup(context: Context, groupName: String?, callback: () -> Unit) {
        val grName = groupName ?: "this"
        val builder = AlertDialog.Builder(context);
        builder.setMessage("Exit \"${groupName}\" group?")
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


    fun convertDpToPixel(dp: Float, context: Context): Int {
        return (dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()
    }

/*
    // util for later - on demand bound
    private fun boundMap(clusterManager: ClusterManager<Member>) {
        // clusters are not loaded until MapLoadedCallback
        try {
//            if (clusterManager == null) return
            val field = clusterManager.javaClass.getDeclaredField("mAlgorithm")
            field.isAccessible = true
            val mAlgorithm = field.get(clusterManager)
            val clusters = arrayListOf((mAlgorithm as Algorithm<*>).items)
            val builder = LatLngBounds.Builder()
            clusters.forEach { cluster ->
                cluster.forEach { clusterItem ->
                    builder.include(clusterItem.position)
                }
            }
            val padding = convertDpToPixel(20 * resources.displayMetrics.density, this)
            map?.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), padding))
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    */


    fun goToPrivacyPolicy(context: Context) {
        val myLink = Uri.parse(context.getString(R.string.privacy_link))
        val intent = Intent(Intent.ACTION_VIEW, myLink)
        val activities: List<ResolveInfo> = context.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        val isIntentSafe: Boolean = activities.isNotEmpty()
        if (isIntentSafe)
            context.startActivity(intent)
    }


    fun startActionShare(context: Context, group: GroupDisplay?) {
        group?.let {
            val shareKey = it.shareKey ?: return
            val baseUrl = context.getString(R.string.base_share_url)
            val myUrl = baseUrl + shareKey
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "invitation to \"${it.groupName}\" group: $myUrl")
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Invite to group"))
        }
    }

    fun showLoginScreen(activity: Activity) {
        val providers =
            listOf(
                AuthUI.IdpConfig.GoogleBuilder().build(),
                AuthUI.IdpConfig.EmailBuilder().build()
            )
        activity.startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(true)
                .setLogo(R.drawable.people_map)
//                        .setAlwaysShowSignInMethodScreen(true)
//                .setTheme(R.style.AuthenticationTheme)
                .setTosAndPrivacyPolicyUrls(
                    activity.getString(R.string.terms_of_service_link),
                    activity.getString(R.string.privacy_link)
                )
                .build(), RC_SIGN_IN
        )
    }


}


