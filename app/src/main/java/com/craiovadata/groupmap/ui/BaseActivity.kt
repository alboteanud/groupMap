package com.craiovadata.groupmap.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.utils.*
import com.craiovadata.groupmap.utils.Util.sendTokenToServer
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.content_map.*

abstract class BaseActivity : AppCompatActivity() {
    lateinit var db: FirebaseFirestore
    var groupId: String = NO_GROUP
    var memberRole: Int? = null
    var userData: Map<String, Any>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    fun saveGroupIdToPref(groupId: String) {
        getSharedPreferences("_", MODE_PRIVATE).edit()
            .putString(GROUP_ID, groupId).apply()
    }


}
