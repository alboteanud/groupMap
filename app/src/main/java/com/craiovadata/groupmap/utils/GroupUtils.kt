package com.craiovadata.groupmap.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
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


}