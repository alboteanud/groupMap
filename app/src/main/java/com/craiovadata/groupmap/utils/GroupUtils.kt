package com.craiovadata.groupmap.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Toast
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.craiovadata.groupmap.BuildConfig
import com.craiovadata.groupmap.R
import com.google.android.gms.common.api.Batch
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.android.synthetic.main.content_main.*

object GroupUtils {

    fun joinGroup(groupId: String, groupName: String, listener: () -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val ref = db
            .collection(USERS).document(user.uid)
            .collection(GROUPS).document(groupId)

        ref.set(hashMapOf(JOINED to true, GROUP_NAME to groupName))
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    listener.invoke()
                }
            }
    }

    fun exitGroup(groupId: String, listener: () -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val ref = db
            .collection(USERS).document(user.uid)
            .collection(GROUPS).document(groupId)

        ref.set(hashMapOf(JOINED to false), SetOptions.merge())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    listener.invoke()
                }
            }
    }

    fun startActionShare(context: Context, groupShareKey: String?) {
        if (groupShareKey==null) return
        val baseUrl = context.getString(R.string.base_share_url)
        val myUrl = baseUrl + groupShareKey
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "share link: $myUrl")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "send_to"))

    }

    fun populateDefaultGroup() {
        if (!BuildConfig.DEBUG) return
        Log.d("Util", "start populate default group")
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        batch.set(db.collection(GROUPS).document(DEFAULT_GROUP), hashMapOf(GROUP_NAME to "The New Yorkers"))
        val persons = Util.getDummyUsers()
        persons.forEachIndexed { index, person ->
            val ref = db.collection(GROUPS).document(DEFAULT_GROUP).collection(DEVICES).document(index.toString())
            batch.set(ref, person)
        }
        batch.commit()
    }

    fun getShareKeyFromInstallRefferer(context: Context, callback: (groupKey: String?) -> Unit) {
        // check if url contains group link code
        val referrerClient = InstallReferrerClient.newBuilder(context).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {

            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                    // Connection established
                    val response: ReferrerDetails = referrerClient.installReferrer
                    val endUrl = response.installReferrer
                    val downloadUri = Util.getDownloadUri(context, endUrl)
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

    fun getShareKeyFromAppLinkIntent(activity: Activity): String? {
        val appLinkData = activity.intent.data ?: return null
        val segments = appLinkData.pathSegments
        if (segments.size >= 2) {
            if (segments[0] == "group") {
                return segments[1]
            }
        }
        return null
    }

    fun buildAlertJoinGroup(view: View, callback: (didJoin: Boolean) -> Unit) {
        Snackbar.make(view, "Join group?", Snackbar.LENGTH_INDEFINITE)
            .setAction("Yes") {
                callback.invoke(true)
            }
            .show()
    }
 fun buildAlertLoginToJoinGroup(view: View, callback: (didJoin: Boolean) -> Unit) {
        Snackbar.make(view, "Login to join group?", Snackbar.LENGTH_INDEFINITE)
            .setAction("Yes") {
                callback.invoke(true)
            }
            .show()
    }

}