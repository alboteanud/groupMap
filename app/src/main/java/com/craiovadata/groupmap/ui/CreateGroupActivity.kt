package com.craiovadata.groupmap.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.utils.*
import com.craiovadata.groupmap.utils.Util.startLoginActivity
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.android.synthetic.main.activity_create_group.*
import kotlinx.android.synthetic.main.content_create_group.*


class CreateGroupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)
        setSupportActionBar(toolbar)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        buttonCreateGroup.setOnClickListener { view -> onCreateGroupClicked(view) }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        updateUI()
    }

    private fun onCreateGroupClicked(view: View) {
        if (auth.currentUser == null) {
            startLoginActivity(this)
        } else {
            createGroup()
        }
    }

    private fun updateUI() {
//        hideProgressDialog()
        if (auth.currentUser != null) {
            buttonCreateGroup.text = getString(R.string.buttonTextCreateGroup)
        } else {
            buttonCreateGroup.text = getString(R.string.buttonTextLoginToCreateGroup)
        }
    }

    // sign in result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                updateUI()
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                if (response == null) {
                    // User pressed the back button.
                    return
                }
                if (response.error?.errorCode == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                    return
                }

                if (response.error?.errorCode == ErrorCodes.UNKNOWN_ERROR) {
                    Toast.makeText(this, getString(R.string.error_default), Toast.LENGTH_SHORT).show();
                    return
                }
            }
        }
    }

    private fun createGroup() {
        val currentUser = auth.currentUser ?: return
        val groupName = editTextGroupName.text.toString()
        if (TextUtils.isEmpty(groupName)) {
            val errMsg = getString(R.string.error_msg_title_not_blank)
            editTextGroupName.error = errMsg
            return
        }
        val refGroup = db.collection(GROUPS).document()
        val groupId = refGroup.id
        val refUser = db.collection(USERS).document(currentUser.uid)
            .collection(GROUPS).document(groupId)

        val group = HashMap<String, Any?>()
        group[GROUP_NAME] = groupName
//        group[GROUP_FOUNDER] = getUserData()
//        group[CREATED_AT] = FieldValue.serverTimestamp()
        group[GROUP_SHARE_KEY] = db.collection(GROUP_SHARE_KEY).document().id

        val batch = db.batch()
        batch.set(refGroup, group)
        batch.set(refUser, hashMapOf(JOINED to true, GROUP_NAME to groupName))
        batch.set(refGroup.collection(USERS).document(currentUser.uid), hashMapOf(IS_ADMIN to true), SetOptions.merge())

        batch.commit()
            .addOnSuccessListener {
                val msg = getString(R.string.toast_group_created_success)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                val resultIntent = Intent(this, GroupInfoActivity::class.java)
                resultIntent.putExtra(GROUP_ID, groupId)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            .addOnFailureListener {
                val msg = getString(R.string.toast_group_creation_error)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        private val TAG = CreateGroupActivity::class.java.simpleName

    }

}
