package com.craiovadata.groupmap.ui

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.craiovadata.groupmap.BuildConfig
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.adapter.MemberAdapter
import com.craiovadata.groupmap.utils.*
import com.craiovadata.groupmap.utils.GroupUtils.startActionShare
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_group_info.*
import kotlinx.android.synthetic.main.content_group_info.*

class GroupInfoActivity : AppCompatActivity(), MemberAdapter.OnItemSelectedListener {
    private var groupData: HashMap<String, Any>? = null
    private var adapter: MemberAdapter? = null
    lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_info)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        db = FirebaseFirestore.getInstance()
        handleIntent()
        button_share_group.setOnClickListener {
            groupData?.get(GROUP_SHARE_KEY)?.let {
                startActionShare(this, it)
            }
        }


        setUpRecyclerView()
    }


    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    private fun setUpRecyclerView() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val query = db.collection("$GROUPS/$groupId/$USERS")

        adapter = object : MemberAdapter(query, this@GroupInfoActivity) {

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

            override fun onError(e: FirebaseFirestoreException) {
                // Show a snackbar on errors
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Error: check logs for info.", Snackbar.LENGTH_LONG
                ).show()
            }
        }
        recyclerParticipants.adapter = adapter
    }

    override fun onItemSelected(member: DocumentSnapshot) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val admins = (groupData?.get(ADMINISTRATORS) as? ArrayList<*>)?.filterIsInstance<String>()
        val iAmAdmin = admins?.contains(currentUser.uid) ?: false
        if (member.id != currentUser.uid && iAmAdmin)
            buildAlertMemberOptions(member)
    }

    private fun buildAlertMemberOptions(member: DocumentSnapshot) {
        if (groupId == null) return

        val isAdmin = member[IS_ADMIN] as? Boolean ?: false

        var option = "Make group admin"
        if (isAdmin) option = "Dismiss as admin"
        val listOptions = arrayOf("Remove", option)

        val refUsr = db.document("$GROUPS/$groupId/$USERS/${member.id}")
        val refGroup = db.document("$GROUPS/$groupId")

        val builder = AlertDialog.Builder(this)
        builder.setItems(listOptions) { dialogInterface: DialogInterface, i: Int ->
            when (i) {

                0 -> {
                    val batch = db.batch()
                    batch.delete(refUsr)
                    if (isAdmin)
                        batch.update(refGroup, ADMINISTRATORS, FieldValue.arrayRemove(member.id))
                    val refUser = db.document("$USERS/${member.id}/$GROUPS/$groupId")
                    batch.set(refUser, mapOf(JOINED to false))
                    batch.commit()
                }
                1 -> {

                    val batch = db.batch()
                    batch.update(refUsr, mapOf(IS_ADMIN to !isAdmin))
                    var operation = FieldValue.arrayUnion(member.id)        // make admin
                    if (isAdmin)
                        operation = FieldValue.arrayRemove(member.id)               // remove as admin
                    batch.update(refGroup, ADMINISTRATORS, operation)
                    batch.commit()

                }


            }
        }
        val alert = builder.create();
        alert.show();
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent()
    }

    private var groupId: String? = null
    private fun handleIntent() {
        groupId = intent.getStringExtra(GROUP_ID) ?: return

        val db = FirebaseFirestore.getInstance()
        val docRef = db.document("$GROUPS/$groupId")

        docRef.get().addOnSuccessListener { snapshot ->
            if (snapshot != null && snapshot.exists()) {
                Log.d(TAG, "Current data: ${snapshot.data}")
                groupData = snapshot.data as HashMap<String, Any>
                updateUI()
            } else {
                Log.d(TAG, "Current data: null")
            }
        }
    }

    private fun updateUI() {
        groupData?.apply {
            val groupName = this[GROUP_NAME] as? String ?: "My group"
            title = groupName
//            val timestampCreated = this[CREATED_AT] as? Timestamp
//            val dtStr = getDateInstance().format(timestampCreated?.toDate())
//            group_date.text = dtStr
//            @Suppress("UNCHECKED_CAST")
//            val founder = this[GROUP_FOUNDER] as? HashMap<String, Any>
//            group_founder.text = founder?.get(NAME) as String
            if (BuildConfig.DEBUG) {
                val baseShareUrl = getString(R.string.base_share_url)
                val groupLinkStr = baseShareUrl + this[GROUP_SHARE_KEY] as String?
                group_link.text = groupLinkStr

            }
        }
    }

    companion object {
        private const val TAG = "GroupInfoActivity"
    }

}
