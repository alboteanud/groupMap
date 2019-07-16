package com.craiovadata.groupmap.ui

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View.*
import android.widget.Toast
import androidx.core.view.isVisible
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.utils.*
import com.craiovadata.groupmap.utils.Util.startLoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_create_group.*
import kotlinx.android.synthetic.main.content_create_group.*

class CreateGroupActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        buttonCreateGroup.setOnClickListener {
            val groupName = getInsertedText() ?: return@setOnClickListener
            createGroup(groupName)
        }
        buttonLogin.setOnClickListener {
            startLoginActivity(this, RC_SIGN_IN)
        }

    }

    override fun onStart() {
        super.onStart()
        updateUI()
    }

    private fun updateUI() {
        if (currentUser == null) {
            buttonCreateGroup.isEnabled = false
            buttonCreateGroup.alpha = 0.8f
            buttonLogin.visibility = VISIBLE
        } else {
            buttonCreateGroup.isEnabled = true
            buttonCreateGroup.alpha = 1.0f
            buttonLogin.visibility = INVISIBLE
        }
    }

    override fun onActivityResultOk(requestCode: Int) {
        super.onActivityResultOk(requestCode)
        if (requestCode == RC_SIGN_IN)
            updateUI()
    }

    private fun createGroup(groupName: String) {
        val uid = currentUser?.uid ?: return
        groupId = db.collection(GROUPS).document().id
        val shareKey = db.collection(DB_GROUP_SHARE_KEYS).document().id

        val batch = db.batch()
        batch.set(db.document("$GROUPS/$groupId"), mapOf(GROUP_NAME to groupName, GROUP_SHARE_KEY to shareKey))
        batch.set(db.document("$GROUPS/$groupId/$USERS/$uid"), mapOf(ROLE to ROLE_ADMIN))
        batch.set(db.document("$USERS/$uid/$GROUPS/$groupId"), mapOf(GROUP_NAME to groupName))
        batch.commit().addOnCompleteListener { task ->
            progress_circular.visibility = VISIBLE
            if (task.isSuccessful) {
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

    private fun goToMapActivity(groupId: String?) {
        val resultIntent = Intent(this, GroupInfoActivity::class.java)
        resultIntent.putExtra(GROUP_ID, groupId)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

}
