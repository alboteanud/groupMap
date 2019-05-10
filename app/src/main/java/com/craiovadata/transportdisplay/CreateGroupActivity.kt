package com.craiovadata.transportdisplay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_create_group.*
import kotlinx.android.synthetic.main.content_create_group.*

class CreateGroupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)
        setSupportActionBar(toolbar)
        auth = FirebaseAuth.getInstance()
        buttonCreateGroup.setOnClickListener { view -> onButtonCreateGroupClicked(view) }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun onButtonCreateGroupClicked(view: View) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startLoginActivity()
        } else {
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            addGroupToFirebase(currentUser)

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

    private fun addGroupToFirebase(currentUser: FirebaseUser) {
        val groupName = editTextGroupName.text.toString()
        if (TextUtils.isEmpty(groupName)) {
            val errMsg = getString(R.string.error_msg_title_not_blank)
            editTextGroupName.error = errMsg
            return
        }
        val user = HashMap<String, Any?>()
        user["name"] = currentUser.displayName
        user["uid"] = currentUser.uid
        user["photoUrl"] = currentUser.photoUrl?.toString()
        user["email"] = currentUser.email

        val group = HashMap<String, Any?>()
        group["groupName"] = groupName
        group["admins"] = listOf(user)

        val db = FirebaseFirestore.getInstance()
        val groupRef = db.collection("groups").document()
        val memberRef = groupRef.collection("members").document(currentUser.uid)

        val batch = db.batch()
        batch.set(groupRef, group)
        batch.set(memberRef, user)
        batch.commit().addOnCompleteListener {task ->
           if (task.isSuccessful){
               Log.d(TAG, "DocumentSnapshot written with ID: ${groupRef.id}")
               getSharedPreferences("_", Context.MODE_PRIVATE).edit().putString("groupId", groupRef.id).apply()
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

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun updateUI(user: FirebaseUser?) {
//        hideProgressDialog()
        if (user != null) {
            buttonCreateGroup.text = getString(R.string.buttonTextCreateGroup)
        } else {
            buttonCreateGroup.text = getString(R.string.buttonTextLoginToCreateGroup)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = FirebaseAuth.getInstance().currentUser
                updateUI(user)
                // ...
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }

    companion object {
        private const val TAG = "CreateGroupActivity"
        private const val RC_SIGN_IN = 9001
    }

}
