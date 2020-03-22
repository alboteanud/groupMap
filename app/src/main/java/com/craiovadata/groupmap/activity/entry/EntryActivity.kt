package com.craiovadata.groupmap.activity.entry

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.activity.base.BaseActivity
import com.craiovadata.groupmap.activity.map.MapActivity
import com.craiovadata.groupmap.activity.mygroups.MyGroupsActivity
import com.craiovadata.groupmap.activity.join.JoinGroupActivity
import com.craiovadata.groupmap.repo.Repository
import com.craiovadata.groupmap.util.*
import com.craiovadata.groupmap.util.PrefUtils.isFirstStart
import com.craiovadata.groupmap.util.PrefUtils.revokeFirstStart
import com.craiovadata.groupmap.util.Util.showLoginScreen
import com.craiovadata.groupmap.viewmodel.*
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import kotlinx.android.synthetic.main.activity_entry.*
import org.koin.android.ext.android.inject
import timber.log.Timber

class EntryActivity : BaseActivity() {
    private var groupData: GroupSkDisplayQueryItem? = null
    private lateinit var viewModel: EntryGroupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(EntryGroupViewModel::class.java)
        verifyAndNavigate()

        setContentView(R.layout.activity_entry)
        setSupportActionBar(toolbar)
        btn_demo.setOnClickListener {
            startActivity(MapActivity.newIntent(this, DEFAULT_GROUP))
        }
        btn_get_started.setOnClickListener {
            showLoginScreen(this)
        }
        btn_log_out.setOnClickListener {
            logOut()
        }

    }



    private fun verifyAndNavigate() {
        findShareKey { groupShareKey ->

            val groupLiveData = viewModel.getGroup(groupShareKey)
            groupLiveData.observe(this, Observer {
                it.data?.let { data ->
                    val loggedIn = auth.currentUser != null
                    if (loggedIn) {
                        if (viewModel.canNavigaitToJoin()) {
                            viewModel.doneNavigatingToJoinActivity()
                            goToJoinActivity(data)
                        }

                    } else {
                        groupData = data
                    }
                }
            })

            viewModel.navigateToJoinActivity.observe(this, Observer {

            })
        }
    }

    private fun goToJoinActivity(groupData: GroupSkDisplayQueryItem) {
        val groupId = groupData.id
        val groupName = groupData.item.groupName
        val intent = JoinGroupActivity.newIntent(this, groupId, groupName)
        startActivity(intent)
//        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            // Successfully signed in
            if (resultCode == Activity.RESULT_OK) {
//                snack("Signed in")
                val repository by inject<Repository>()
                repository.sendTokenToServer(null)
                if (groupData != null) {
                    goToJoinActivity(groupData!!)
                } else {
                    startActivity(Intent(this, MyGroupsActivity::class.java))
//                    finish()
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

    override fun onLogin() {
        super.onLogin()
        updateUI(true)
    }

    override fun onLogout() {
        super.onLogout()
        updateUI(false)
    }

    private fun updateUI(loggedIn: Boolean) {
        btn_get_started?.isVisible = !loggedIn
        btn_log_out?.isVisible = loggedIn
    }

    private fun findShareKey(callback: (shareKey: String) -> Unit) {
        val appLinkShareKey: String? = checkAppLinkIntent()
        if (appLinkShareKey != null) {
            callback.invoke(appLinkShareKey)
        } else {
            if (isFirstStart(this)) {
                revokeFirstStart(this)
                checkInstallRefferer {
                    callback(it)
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

    private fun checkAppLinkIntent(): String? {
        val appLinkData = intent.data
        appLinkData?.let {
            val segments = appLinkData.pathSegments
            if (segments.size >= 2 && segments[0] == "group") {
                return segments[1]
            }
        }
        return null
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        verifyAndNavigate()
    }

    companion object {

        fun startEntryActivityNewTask(activity: Activity) {
            val intent = Intent(activity, EntryActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            activity.startActivity(intent)
            activity.finish()
        }
    }

}
