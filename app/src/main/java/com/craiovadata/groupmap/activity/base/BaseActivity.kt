package com.craiovadata.groupmap.activity.base

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.activity.entry.EntryActivity
import com.firebase.ui.auth.AuthUI
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import org.koin.android.ext.android.inject

open class BaseActivity : AppCompatActivity() {

    val auth by inject<FirebaseAuth>()

    fun snack(message: String) {
        val view = findViewById(R.id.root) as? View
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        if (auth.currentUser == null) {
            onLogout()
        } else {
            onLogin()
        }
    }

    open fun onLogin() {
//        Toast.makeText(this, "Successfully logged in", Toast.LENGTH_SHORT).show()

    }

    open fun logOut(){
//        auth.signOut()
        AuthUI.getInstance().signOut(this)
    }

    open fun onLogout() {
//        Toast.makeText(this, "Successfully logged out", Toast.LENGTH_SHORT).show()
        if (this !is EntryActivity) {
            EntryActivity.startEntryActivityNewTask(this)
        }
    }

    fun goToPrivacyPolicy() {
        val myLink = Uri.parse(getString(R.string.privacy_link))
        val intent = Intent(Intent.ACTION_VIEW, myLink)
        val activities: List<ResolveInfo> = packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        val isIntentSafe: Boolean = activities.isNotEmpty()
        if (isIntentSafe)
            startActivity(intent)
    }

}
