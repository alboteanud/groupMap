package com.craiovadata.groupmap.ui

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.adapter.MemberAdapter
import com.craiovadata.groupmap.utils.*
import com.craiovadata.groupmap.utils.GroupUtils.startActionShare
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_group_info.*
import kotlinx.android.synthetic.main.content_group_info.*

class GroupInfoActivity : AppCompatActivity() {
    private var adapter: MemberAdapter? = null
    private lateinit var db: FirebaseFirestore
    private var currentUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_info)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        db = FirebaseFirestore.getInstance()
        currentUser = FirebaseAuth.getInstance().currentUser
        handleIntent()
    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    private fun setUpRecyclerView(groupId: String, currentUserData: MutableMap<String, Any>?) {
        val query = db.collection("$GROUPS/$groupId/$USERS")

        adapter = object : MemberAdapter(query, object : OnItemSelectedListener {

            override fun onItemSelected(member: DocumentSnapshot) {
                val iAmAdmin = currentUserData?.get(IS_ADMIN) as? Boolean ?: false
                if (iAmAdmin) {
                    val uid = currentUser?.uid
                    if (uid != null && uid != member.id)
                        buildAlertMemberOptions(member, groupId)
                }
            }
        }) {

            override fun onDataChanged() {
                // Show/hide content if the query returns empty.
                if (itemCount == 0) {
                    recyclerParticipants.visibility = View.GONE
//                    viewEmpty.visibility = View.VISIBLE

                } else {
                    recyclerParticipants.visibility = View.VISIBLE
//                    viewEmpty.visibility = View.GONE
                    var text = "$itemCount participants"
                    if (itemCount == 1) text = "$itemCount participant"
                    participantsText.text = text
                }
            }
        }
        recyclerParticipants.adapter = adapter
    }

    private fun buildAlertMemberOptions(member: DocumentSnapshot, groupId: String) {
        val isAdmin = member[IS_ADMIN] as? Boolean ?: false

        var listOptions = arrayOf("Remove", "Dismiss as admin")
        if (!isAdmin) listOptions = arrayOf("Remove", "Make group admin")

        val refGrUser = db.document("$GROUPS/$groupId/$USERS/${member.id}")
        val batch = db.batch()
        val builder = AlertDialog.Builder(this)
        builder.setItems(listOptions) { dialogInterface: DialogInterface, i: Int ->
            when (i) {
                0 -> {
                    batch.delete(refGrUser)
                    val refUser = db.document("$USERS/${member.id}/$GROUPS/$groupId")
                    batch.update(refUser, mapOf(JOINED to false))
                }
                1 -> {
                    batch.update(refGrUser, mapOf(IS_ADMIN to !isAdmin))
                }
            }
            batch.commit()
        }
        builder.create().show();
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent()
    }

    private fun handleIntent() {
        val groupId = intent.getStringExtra(GROUP_ID) ?: return

        db.document("$GROUPS/$groupId")
            .get().addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data: ${snapshot.data}")
                    val groupData = snapshot.data
                    updateUI(groupData)
                } else {
                    Log.d(TAG, "Current data: null")
                }
            }
        db.document("$GROUPS/$groupId/$USERS/${currentUser?.uid}").get().addOnSuccessListener { snapshot ->
            if (snapshot != null && snapshot.exists()) {
                val currentUserData = snapshot.data
                setUpRecyclerView(groupId, currentUserData)
            }
        }

    }

    private fun updateUI(groupData: MutableMap<String, Any>?) {
        val groupName = groupData?.get(GROUP_NAME) as? String ?: "My group"
        val shareKey = groupData?.get(GROUP_SHARE_KEY) as? String

        title = groupName
        button_share_group.setOnClickListener {
            startActionShare(this, shareKey)
        }
    }

    companion object {
        private const val TAG = "GroupInfoActivity"
    }

}
