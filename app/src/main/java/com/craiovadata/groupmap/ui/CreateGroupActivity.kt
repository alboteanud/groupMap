package com.craiovadata.groupmap.ui

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.utils.*
import com.craiovadata.groupmap.utils.Util.startLoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_create_group.*
import kotlinx.android.synthetic.main.content_create_group.*

class CreateGroupActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)
        setSupportActionBar(toolbar)

        buttonCreateGroup.setOnClickListener { view -> onCreateGroupClicked(view) }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        updateUI()
    }

    private fun onCreateGroupClicked(view: View) {
        createGroup()
    }

    private fun updateUI() {
//        hideProgressDialog()
        val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            buttonCreateGroup.text = getString(R.string.buttonTextLoginToCreateGroup)
        } else {
            buttonCreateGroup.text = getString(R.string.buttonTextCreateGroup)
        }
    }


//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        handleOnActivityResult(requestCode, resultCode, data)
//
//    }

    override fun onLoginSuccess() {
        super.onLoginSuccess()
        updateUI()
    }

    private fun createGroup() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startLoginActivity(this)
            return
        }
        progress_circular.visibility = VISIBLE
        val uid = currentUser.uid
        val groupName = getInsertedText() ?: return

        val refGroup = db.collection(GROUPS).document()
        groupId = refGroup.id
        val shareKey = db.collection(DB_GROUP_SHARE_KEYS).document().id
        val refUser = db.collection(USERS).document(uid)
            .collection(GROUPS).document(groupId)
        val refGrUser = db.document("$GROUPS/$groupId/$USERS/$uid")

        val batch = db.batch()
        batch.set(refGroup, hashMapOf(GROUP_NAME to groupName, GROUP_SHARE_KEY to shareKey))
        batch.set(refGrUser, hashMapOf(IS_ADMIN to true))
        batch.set(refUser, hashMapOf(GROUP_NAME to groupName))
        batch.commit().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val msg = getString(R.string.toast_group_created_success)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                saveGroupIdToPref(groupId)
                goToMapActivity(groupId)
            } else {
                val msg = getString(R.string.toast_group_creation_error)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
            progress_circular.visibility = GONE
        }
    }

    private fun getInsertedText(): String? {
        val groupName = editTextGroupName.text.toString()
        if (TextUtils.isEmpty(groupName)) {
            val errMsg = getString(R.string.error_msg_title_not_blank)
            editTextGroupName.error = errMsg
            return null
        }
        return groupName
    }

    private fun goToMapActivity(groupId: String) {
        val resultIntent = Intent(this, GroupInfoActivity::class.java)
        resultIntent.putExtra(GROUP_ID, groupId)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    companion object {
        private val TAG = CreateGroupActivity::class.java.simpleName
    }

}
