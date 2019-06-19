package com.craiovadata.groupmap.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.craiovadata.groupmap.BuildConfig
import com.craiovadata.groupmap.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object GroupUtils {

    fun joinGroup(context: Context, groupId: String, listener: () -> Unit) {
        FirebaseAuth.getInstance().currentUser?.apply {
            val ref =  FirebaseFirestore.getInstance()
                .collection(USERS).document(uid)
                .collection(GROUPS).document(groupId)

            ref.set(hashMapOf(JOINED to true)).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    listener.invoke()
                } else {
                    val msg = context.getString(R.string.toast_group_creation_error)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun leaveGroup(groupId: String) {
        FirebaseAuth.getInstance().currentUser?.apply {
            val ref = FirebaseFirestore.getInstance()
                .collection(USERS).document(uid)
                .collection(GROUPS).document(groupId)
//        ref.delete()
            ref.set(hashMapOf(JOINED to false))
        }

    }

     fun startActionShare(context: Context, groupShareKey: Any?) {
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
        if(!BuildConfig.DEBUG) return
        Log.d(Util.TAG, "start populate default group")
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

    fun checkInstallReffererForGroupKey(context: Context, callback: (groupKey: String?) -> Unit) {
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


    fun getGroupKeyFromIntent(activity: Activity): String? {
        val appLinkIntent = activity.intent
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

}