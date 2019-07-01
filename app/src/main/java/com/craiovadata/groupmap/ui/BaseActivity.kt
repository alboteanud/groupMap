package com.craiovadata.groupmap.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.utils.*
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

abstract class BaseActivity : AppCompatActivity() {
    lateinit var db: FirebaseFirestore
    var groupId: String = NO_GROUP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            if (auth.currentUser != null) {
                Util.sendDeviceTokenToServer()
            } else {
                // current user is null

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleOnActivityResult(requestCode, resultCode, data)
    }

    open fun handleOnActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == RESULT_OK) {
                // Successfully signed in
//                sendDeviceTokenToServer()
                onLoginSuccess()
            } else {
                when {
                    response == null -> {
                        // User pressed the back button.
                    }
                    response.error?.errorCode == ErrorCodes.NO_NETWORK ->
                        Toast.makeText(this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show()
                    response.error?.errorCode == ErrorCodes.UNKNOWN_ERROR ->
                        Toast.makeText(this, getString(R.string.error_default), Toast.LENGTH_SHORT).show()
                }
                return
            }
        } else if (requestCode == CREATE_GROUP_REQUEST) {   // a group was just created
            if (resultCode == RESULT_OK) {
                groupId = data?.getStringExtra(GROUP_ID) ?: groupId
                onGroupCreated()
            }
        }
    }

    open fun onGroupCreated() {

    }

    open fun onLoginSuccess() {

    }

    fun saveGroupIdToPref(groupId: String) {
        getSharedPreferences("_", AppCompatActivity.MODE_PRIVATE).edit()
            .putString(GROUP_ID, groupId).apply()
    }

}
