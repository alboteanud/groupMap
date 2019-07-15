package com.craiovadata.groupmap.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.craiovadata.groupmap.R
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

object GroupUtils {

    fun joinGroup(uid: String, groupId: String, groupName: String?, listener: (userRole: Int?) -> Unit) {
        FirebaseFirestore.getInstance().document("$USERS/$uid/$GROUPS/$groupId")
            .set(mapOf(GROUP_NAME to groupName)) // will trigger a cloud function(3) to update the Group with userData
            .addOnSuccessListener {
                Log.d("tag", "Transaction success!")
                listener.invoke(ROLE_USER)
            }
            .addOnFailureListener { e ->
                Log.w("tag", "Transaction failure.", e)
                listener.invoke(null)
            }
    }

    fun startActionShare(context: Context, groupData: Map<String, Any?>?) {
        val shareKey = groupData?.get(GROUP_SHARE_KEY) as? String
        if (shareKey == null) {
            Toast.makeText(context, "error building invitation", LENGTH_SHORT).show()
            return
        }
        val baseUrl = context.getString(R.string.base_share_url)
        val myUrl = baseUrl + shareKey
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "invitation to group: $myUrl")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Invite to group"))
    }

    fun populateDefaultGroup() {
        val db = FirebaseFirestore.getInstance()
        val refGroup = db.document("$GROUPS/$DEFAULT_GROUP")
        val batch = db.batch()
        batch.set(refGroup, mapOf(GROUP_NAME to "New Yorkers"))
        val persons = Util.getDummyUsers()
        persons.forEachIndexed { index, person ->
            val ref = refGroup.collection(USERS).document(index.toString())
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