package com.craiovadata.groupmap.activity.entry

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.activity.base.BaseActivity
import com.craiovadata.groupmap.activity.map.MapActivity
import com.craiovadata.groupmap.activity.mygroups.MyGroupsActivity
import com.craiovadata.groupmap.activity.join.JoinGroupActivity
import com.craiovadata.groupmap.repo.Repository
import com.craiovadata.groupmap.utils_.*
import com.craiovadata.groupmap.utils_.PrefUtils.isFirstStart
import com.craiovadata.groupmap.utils_.PrefUtils.revokeFirstStart
import com.craiovadata.groupmap.utils_.Util.showLoginScreen
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import kotlinx.android.synthetic.main.activity_entry.*
import org.koin.android.ext.android.inject
import timber.log.Timber

class EntryActivity : BaseActivity(), View.OnClickListener {
    private var groupShareKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check if case is - second Start loggedIn
        val loggedIn = auth.currentUser != null
        if (loggedIn) {
            checkAppLinkIntent()?.let { shareKey ->
                navigateToJoinActivity(shareKey)
                return
            }
        }     // not the case. Continue

        findShareKey()
        initViews()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            // Successfully signed in
            if (resultCode == Activity.RESULT_OK) {
//                snack("Signed in")
                val repository by inject<Repository>()
                repository.sendTokenToServer(null)
                if (groupShareKey != null) {
                    navigateToJoinActivity(groupShareKey!!)
                } else {
//                    startActivity(Intent(this, ControlPanelActivity::class.java))
                    startActivity(Intent(this, MyGroupsActivity::class.java))
                    finish()
                }
            } else {
                // Sign in failed
                val response = IdpResponse.fromResultIntent(data) ?: return

                if (response.error!!.errorCode == ErrorCodes.NO_NETWORK) {
                    snack("No network")
                    return
                }
                snack("Unknown error with sign in")
                Timber.e(response.error, "Sign-in error: ")
            }
        }
    }

    private fun initViews() {
        setContentView(R.layout.activity_entry)
        setSupportActionBar(toolbar)
        btn_demo.setOnClickListener(this)
        btn_log_in.setOnClickListener(this)
        btn_log_out.setOnClickListener(this)
//        btn_privacy.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_demo -> startActivity(MapActivity.newIntent(this, DEFAULT_GROUP))
            R.id.btn_log_in -> showLoginScreen(this)
            R.id.btn_log_out -> AuthUI.getInstance().signOut(this)
            R.id.btn_my_groups -> startActivity(Intent(this, MyGroupsActivity::class.java))
//            R.id.btn_privacy -> goToPrivacyPolicy(v)
        }
    }

   override fun onLogin(){
        super.onLogin()
       updateUI(true)
    }

    override fun onLogout() {
        super.onLogout()
        updateUI(false)
    }

    private fun updateUI(loggedIn: Boolean){
        btn_log_in?.isVisible = !loggedIn
        btn_log_out?.isVisible = loggedIn
    }

    private fun findShareKey() {
        groupShareKey = checkAppLinkIntent()
        if (groupShareKey == null) {
            if (isFirstStart(this)) {
                revokeFirstStart(this)
                checkInstallRefferer { shareKey ->
                    groupShareKey = shareKey
                }
            }
        }
    }

    private fun checkInstallRefferer(callback: (shareKey: String) -> Unit) {
        // check if url contains group link code
        val referrerClient = InstallReferrerClient.newBuilder(this).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {

            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                    // Connection established
                    val response: ReferrerDetails = referrerClient.installReferrer
                    val endUrl = response.installReferrer
                    val downloadUri = Util.getDownloadUri(applicationContext, endUrl)

                    val newShareKey = downloadUri.getQueryParameter("utm_content")
//                    navigateToJoinActivity()
                    if (newShareKey != null)
                        callback.invoke(newShareKey)
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    private fun navigateToJoinActivity(groupShareKey: String) {
        val intent = JoinGroupActivity.newIntent(this, groupShareKey)
        startActivity(intent)
        finish()
    }

    private fun checkAppLinkIntent(): String? {
        val appLinkData = intent.data
        appLinkData?.let {
            val segments = appLinkData.pathSegments
            if (segments.size >= 2 && segments[0] == "group") {
                val shareKey = segments[1]
                return shareKey
            }
        }
        return null
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkAppLinkIntent()?.let { shareKey ->
            val loggedIn = auth.currentUser != null
            if (loggedIn) {     // check if - second Start
                navigateToJoinActivity(shareKey)
            } else {        // store for later login
                groupShareKey = shareKey
            }
        }
    }



    companion object {

        fun startEntryActivityNewTask(activity:Activity){
            val intent = Intent(activity, EntryActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            activity.startActivity(intent)
            activity.finish()
        }
    }

}
