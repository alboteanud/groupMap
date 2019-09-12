package com.craiovadata.groupmap.activity.mygroups

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.activity.base.BaseActivity
import com.craiovadata.groupmap.activity.map.MapActivity
import com.craiovadata.groupmap.activity.creategroup.CreateGroupActivity
import com.craiovadata.groupmap.signin.setupProfileMenuItem
import com.craiovadata.groupmap.viewmodel.GroupDisplayQueryResults
import com.craiovadata.groupmap.viewmodel.MyGroupsViewModel
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.android.synthetic.main.activity_my_groups.*
import kotlinx.android.synthetic.main.content_my_groups.*
import timber.log.Timber


class MyGroupsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_my_groups)
        setSupportActionBar(toolbar)
        setUpGroupList(auth.uid)
        fab.setOnClickListener {
            startActivity(Intent(this, CreateGroupActivity::class.java))
        }

        checkGooglePlayServices()

    }

    private fun checkGooglePlayServices() {
        val statusServices = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        // success == 0
        when (statusServices) {
            ConnectionResult.SERVICE_MISSING -> {
                Toast.makeText(this, "Google Play SERVICE_MISSING", Toast.LENGTH_SHORT).show()
                GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
            }
            ConnectionResult.SERVICE_UPDATING -> {
                Toast.makeText(this, "Google Play SERVICE_UPDATING", Toast.LENGTH_SHORT).show()
            }
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                Toast.makeText(
                    this,
                    "Google Play SERVICE_VERSION_UPDATE_REQUIRED",
                    Toast.LENGTH_SHORT
                ).show()
                GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
            }
            ConnectionResult.SERVICE_DISABLED -> {
                Toast.makeText(this, "Google Play SERVICE_DISABLED", Toast.LENGTH_SHORT).show()
                GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
            }
            ConnectionResult.SERVICE_INVALID -> {
                Toast.makeText(this, "Google Play SERVICE_INVALID", Toast.LENGTH_SHORT).show()
                GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
            }

        }
    }

    private var viewModel: MyGroupsViewModel? = null
    private fun setUpGroupList(uid: String?) {
        if (uid == null) return
        viewModel = ViewModelProviders.of(this)
            .get(MyGroupsViewModel::class.java)
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerGroups).apply {
            setHasFixedSize(true)

            val listAdapter = MyGroupsListAdapter(ItemClickListener())

            // Observe the groups changes, and submit them to the ListAdapter.
            // Order of the list is reversed so that new data appears at the top.
            val groupsObserver = Observer<GroupDisplayQueryResults> {
                if (it != null) {
                    if (it.data != null) {
                        val data = it.data
                        // Show/hide content if the query returns empty.
                        if (data.isNullOrEmpty()) {
                            visibility = View.GONE
                            view_empty_list.visibility = View.VISIBLE
                        } else {
                            visibility = View.VISIBLE
                            view_empty_list.visibility = View.GONE
                        }
                        listAdapter.submitList(data)
                    } else if (it.exception != null) {
                        Timber.e(it.exception, "Error getting my groups %s", it.exception.message)
//                        TODO("Handle the error")
                    }
                }
            }
            val groupsLiveData = viewModel!!.getGroups(uid)
            groupsLiveData.observe(this@MyGroupsActivity, groupsObserver)

            adapter = listAdapter
        }
    }

    private inner class ItemClickListener :
        MyGroupsListAdapter.ItemClickListener<MyGroupsViewHolder> {
        override fun onItemClick(holder: MyGroupsViewHolder) {
            holder.groupId?.let {
                startActivity(MapActivity.newIntent(this@MyGroupsActivity, it))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_my_groups, menu)
        if (viewModel != null) {
            toolbar.setupProfileMenuItem(
                viewModel!!, this@MyGroupsActivity
            )
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_log_out -> {
                AuthUI.getInstance().signOut(this)
                return true
            }
            R.id.menu_privacy_policy -> {
               goToPrivacyPolicy(item.actionView)
//                startService(Intent(this, MyMessagingService::class.java))
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
