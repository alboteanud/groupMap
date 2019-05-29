package com.craiovadata.groupmap

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.craiovadata.groupmap.MapActivity.Companion.GROUPS
import com.craiovadata.groupmap.MapActivity.Companion.USERS
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
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
    }

    private fun onCreateGroupClicked(view: View) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startLoginActivity()
        } else {
            createGroup(currentUser)
        }
    }

    private fun startLoginActivity() {
        val providers =
            arrayListOf(AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build())
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(true)
                .setLogo(R.drawable.ic_person_pin)
//                        .setAlwaysShowSignInMethodScreen(true)
                .build(), RC_SIGN_IN
        )
    }

    public override fun onStart() {
        super.onStart()
        updateUI(auth.currentUser)
    }

    private fun updateUI(user: FirebaseUser?) {
//        hideProgressDialog()
        if (user != null) {
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
                updateUI(auth.currentUser)
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

    private fun createGroup(currentUser: FirebaseUser) {
        val groupName = editTextGroupName.text.toString()
        if (TextUtils.isEmpty(groupName)) {
            val errMsg = getString(R.string.error_msg_title_not_blank)
            editTextGroupName.error = errMsg
            return
        }
        val userData = HashMap<String, Any?>()
        userData["uid"] = currentUser.uid
        val groupData = HashMap<String, Any?>()
        groupData["groupName"] = groupName
        groupData["founder"] = userData

        val refGroup = db.collection(GROUPS).document(MapActivity.defaultGroupId)
        val groupId = refGroup.id

        val refUserGroup = db.collection(USERS).document(currentUser.uid)
            .collection(GROUPS).document(groupId)
        val dataJoined = HashMap<String, Any?>()
        dataJoined[MapActivity.JOINED]= true

        val batch = db.batch()
        batch.set(refGroup, groupData)
        batch.set(refUserGroup, dataJoined)

        batch.commit().addOnCompleteListener { task ->
            if (task.isSuccessful) {

                getSharedPreferences("_", Context.MODE_PRIVATE).edit()
                    .putString(KEY_GROUP_ID, groupId).apply()
                val msg = getString(R.string.toast_group_created_success)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Log.w(TAG, "Error adding document", task.exception)
                val msg = getString(R.string.toast_group_creation_error)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val TAG = "CreateGroupActivity"
        private const val RC_SIGN_IN = 9001
        const val KEY_GROUP_ID = "groupId"
    }

}
