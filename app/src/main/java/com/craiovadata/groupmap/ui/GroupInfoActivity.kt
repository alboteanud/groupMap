package com.craiovadata.groupmap.ui

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.adapter.MemberAdapter
import com.craiovadata.groupmap.utils.*
import com.craiovadata.groupmap.utils.GroupUtils.startActionShare
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.android.synthetic.main.activity_group_info.*
import kotlinx.android.synthetic.main.content_group_info.*

class GroupInfoActivity : BaseActivity() {
    private var adapter: MemberAdapter? = null
    private var groupData: Map<String, Any>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.craiovadata.groupmap.R.layout.activity_group_info)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        handleIntent()
        button_share_group.setOnClickListener {
            startActionShare(this, groupData)
        }
        infoButtonEditGroupName.setOnClickListener {
            buildAlertChangeGroupName()
        }

    }

    private fun setUpRecyclerView() {
        val query = db.collection("$GROUPS/$groupId/$USERS")
        adapter = object : MemberAdapter(query, object : OnItemSelectedListener {

            override fun onItemSelected(member: DocumentSnapshot) {
                buildAlertMemberOptions(member)
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

    private fun buildAlertMemberOptions(member: DocumentSnapshot) {
        if (memberRole != ROLE_ADMIN) return
        val roleSelectedUser = (userData?.get(ROLE) as? Long)?.toInt() ?: ROLE_USER

        val listOptions = when (roleSelectedUser) {
            ROLE_ADMIN -> arrayOf("Remove", "Dismiss as admin")
            ROLE_USER -> arrayOf("Remove", "Make group admin")
            else -> return
        }

        val ref = db.document("$GROUPS/$groupId/$USERS/${member.id}")
        val builder = AlertDialog.Builder(this)

        builder.setItems(listOptions) { dialogInterface: DialogInterface, i: Int ->
            when (i) {
                0 -> {
                    ref.delete()
                }
                1 -> {
                    val newRole = when (roleSelectedUser) {
                        ROLE_ADMIN -> ROLE_USER
                        ROLE_USER -> ROLE_ADMIN
                        else -> return@setItems
                    }
                    ref.update(mapOf(ROLE to newRole))
                }
            }
        }
        builder.create().show();
    }

    private fun buildAlertChangeGroupName() {
        val textInputLayout = TextInputLayout(this)
        // if you look at android alert_dialog.xml, you will see the message textview have margin 14dp and padding 5dp. This is the reason why I use 19 here
        val padding = resources.getDimensionPixelOffset(R.dimen.dp_16)
        textInputLayout.setPadding(padding, padding, padding, 0)
        val input = EditText(this)
        textInputLayout.hint = "Group Name"
        textInputLayout.addView(input)

        val builder = AlertDialog.Builder(this)
            .setView(textInputLayout)
            .setPositiveButton("OK") { dialog, which ->
                val text = input.text.toString()
                if (TextUtils.isEmpty(text)) {
                    Toast.makeText(this, "enter a valid name", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                val ref = db.document("$GROUPS/$groupId")
                ref.update(GROUP_NAME, text).addOnSuccessListener {
                    infoGroupName.text = text
                }
            }
            .setNegativeButton(
                "Cancel"
            ) { dialog, which -> dialog.cancel() }
//            .setTitle("edit")
        builder.create().show()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent()
    }

    private fun handleIntent() {
        groupId = intent.getStringExtra(GROUP_ID) ?: return
        setUpRecyclerView()
        db.document("$GROUPS/$groupId")
            .get().addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data: ${snapshot.data}")
                    groupData = snapshot.data
                    updateUI()
                } else {
                    Log.d(TAG, "Current data: null")
                }
            }

        val uid = currentUser?.uid ?: return
        db.document("$GROUPS/$groupId/$USERS/$uid").get()
            .addOnSuccessListener { snap ->
                userData = snap.data
                memberRole = (userData?.get(ROLE) as? Long)?.toInt() ?: ROLE_USER
                updateUI()

            }
    }

    private fun updateUI() {
        val groupName = groupData?.get(GROUP_NAME) as? String
        infoGroupName.text = groupName ?: "Group Name"
//        button_share_group.isVisible = memberRole == ROLE_ADMIN
    }


    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    companion object {
        private const val TAG = "GroupInfoActivity"
    }

}
