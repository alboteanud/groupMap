package com.craiovadata.groupmap.activity.base

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.activity.entry.EntryActivity
import com.craiovadata.groupmap.utils_.PERMISSIONS_REQUEST
import com.craiovadata.groupmap.utils_.Util
import com.google.android.gms.maps.GoogleMap
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

    }

    open fun onLogout() {
        if (this !is EntryActivity) {
            EntryActivity.startEntryActivityNewTask(this)
        }
    }

}
