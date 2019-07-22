package com.craiovadata.groupmap.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.utils.*
import com.craiovadata.groupmap.utils.Util.sendTokenToServer
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

abstract class BaseActivity : AppCompatActivity() {
    lateinit var db: FirebaseFirestore
    var groupId: String? = null
    var memberRole: Int? = null
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val response = IdpResponse.fromResultIntent(data)
        if (resultCode == RESULT_OK) {
            // Successfully signed in

            if (requestCode == RC_SIGN_IN || requestCode == RC_SIGN_IN_ASKED_JOIN) {
                sendTokenToServer()
            } else if (requestCode == RC_CREATE_GROUP) {   // a group was just created
                data?.getStringExtra(GROUP_ID)?.let { groupId = it }
//                Toast.makeText(this, "The group was created", LENGTH_LONG).show()
            }
            onActivityResultOk(requestCode) // after groupId = it
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
    }

    open fun onActivityResultOk(requestCode: Int) {

    }

    fun saveGroupIdToPref(groupId: String?) {
        getSharedPreferences("_", MODE_PRIVATE).edit()
            .putString(GROUP_ID, groupId).apply()
    }



}
