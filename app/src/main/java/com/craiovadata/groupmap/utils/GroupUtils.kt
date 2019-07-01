package com.craiovadata.groupmap.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.accessibility.AccessibilityEventCompat.setAction
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
import kotlinx.android.synthetic.main.content_map.*

object GroupUtils {

    fun joinGroup(groupId: String, groupName: String, listener: () -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseFirestore.getInstance()
            .document("$USERS/$uid/$GROUPS/$groupId")
        ref.set(hashMapOf(JOINED to true, GROUP_NAME to groupName))
            .addOnSuccessListener { listener.invoke() }
    }

    fun startActionShare(context: Context, groupShareKey: String?) {
        if (groupShareKey == null) return
        val baseUrl = context.getString(R.string.base_share_url)
        val myUrl = baseUrl + groupShareKey
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "invitation to group: $myUrl")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Invite to group"))
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

 fun deleteAllDB() {
        if (!BuildConfig.DEBUG) return
        Log.d("Util", "start delete all db")
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

}