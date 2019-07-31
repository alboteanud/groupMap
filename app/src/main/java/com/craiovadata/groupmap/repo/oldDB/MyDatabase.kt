package com.craiovadata.groupmap.repo.oldDB

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.craiovadata.groupmap.utils_.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

class MyDatabase(private val application: Application, private val groupShareKey: String?) : MyDatabaseDao {
    private val db = FirebaseFirestore.getInstance()
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    var groupInfo: DocumentSnapshot? = null
    var currentUserDataInGroup: DocumentSnapshot? = null
    var positionListenerRegistration: ListenerRegistration? = null
    var allGroupMembers: List<DocumentSnapshot>? = null

    override fun fetchUserFromGroup(callback: (snap: DocumentSnapshot?) -> Unit): DocumentSnapshot? {
        val uid = auth.currentUser?.uid
        val groupId = getGroupId()
        if (uid == null || groupId == null) return null

        db.document("$GROUPS/$groupId/$USERS/$uid").get()
            .addOnSuccessListener { snap ->
                //                var role: Int? = null
//                if (task.isSuccessful && task.result != null) {
//                    role = (task.result?.get(ROLE) as? Long)?.toInt()
//                }
                currentUserDataInGroup = snap
                callback(currentUserDataInGroup)
            }
        return currentUserDataInGroup
    }

    override fun setNewUpdatePositionsRequest() {

    }

    override fun fetchGroupInfo(callback: (groupInfo: DocumentSnapshot?) -> Unit): DocumentSnapshot? {
        if (groupShareKey == null) {    // no shareKey. Use the saved groupId
            val groupId = application.getSharedPreferences("_", AppCompatActivity.MODE_PRIVATE)
                .getString(GROUP_ID, DEFAULT_GROUP)
            db.document("$GROUPS/$groupId").get()
                .addOnSuccessListener { snap ->
                    if (snap == null) return@addOnSuccessListener
                    groupInfo = snap
                    callback.invoke(groupInfo)
                }
        } else { // share key present. Find the group
            db.collection(GROUPS).whereEqualTo(GROUP_SHARE_KEY, groupShareKey).get()
                .addOnSuccessListener { querySnap ->
                    if (querySnap == null || querySnap.documents.isEmpty()) return@addOnSuccessListener
                    groupInfo = querySnap.documents[0]
                    callback.invoke(groupInfo)
//                    saveGroupIdToPref(groupId)
                }
        }
        return groupInfo
    }

    private fun isAdminOrUser(): Boolean {
        val memberRole = (currentUserDataInGroup?.get(ROLE)  as? Long)?.toInt()
        return memberRole == ROLE_USER || memberRole == ROLE_ADMIN
    }

    override fun fetchAllMembers(callback: (allMembers: List<DocumentSnapshot>?) -> Unit): List<DocumentSnapshot>? {
        val groupId = getGroupId()
        when {
            groupId == DEFAULT_GROUP -> {   // continue
            }
            isAdminOrUser() -> {            // continue
            }
            else -> return null
        }

        positionListenerRegistration?.remove()
        positionListenerRegistration = db.collection("$GROUPS/$groupId/$USERS")
            .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
                if (e != null) {
                    Log.w("tag", "listen:error", e)
                    return@EventListener
                }

                if (snapshots == null) return@EventListener
                allGroupMembers = snapshots.documents
                callback.invoke(allGroupMembers)
            })
        return allGroupMembers
    }

    override fun getGroupId(): String? {
        return groupInfo?.id
    }

    private fun saveGroupIdToPref(groupId: String?) {
        application.getSharedPreferences("_", AppCompatActivity.MODE_PRIVATE).edit()
            .putString(GROUP_ID, groupId).apply()
    }

    override fun deleteGroupInUser(callback: ()-> Unit) {
        val groupId = getGroupId() ?: return
        val uid = auth.currentUser?.uid ?: return
        db.document("$USERS/$uid/$GROUPS/$groupId").delete()
            .addOnSuccessListener {
                positionListenerRegistration?.remove()
                positionListenerRegistration = null
                groupInfo = null
                currentUserDataInGroup = null
                allGroupMembers = null
                callback.invoke()
            }
    }


}