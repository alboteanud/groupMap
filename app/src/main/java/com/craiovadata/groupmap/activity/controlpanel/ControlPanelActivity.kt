package com.craiovadata.groupmap.activity.controlpanel

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.activity.base.BaseActivity
import com.craiovadata.groupmap.activity.mygroups.MyGroupsActivity
import com.craiovadata.groupmap.activity.myprofile.MyProfileActivity
import com.firebase.ui.auth.AuthUI
import kotlinx.android.synthetic.main.activity_control_panel.*
import kotlinx.android.synthetic.main.activity_create_group.toolbar
import kotlinx.android.synthetic.main.privacy.*

class ControlPanelActivity : BaseActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
    }


    private fun initViews() {
        setContentView(R.layout.activity_control_panel)
        setSupportActionBar(toolbar)
        btn_my_groups.setOnClickListener(this)
        buttonPrivacyPolicy.setOnClickListener(this)
        btn_my_profile.setOnClickListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_control_panel, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_log_out -> {
                AuthUI.getInstance().signOut(this)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_my_groups -> startActivity(Intent(this, MyGroupsActivity::class.java))
            R.id.buttonPrivacyPolicy -> goToPrivacyPolicy()
            R.id.btn_my_profile -> startActivity(Intent(this, MyProfileActivity::class.java))
//          ->  DummyUsersUtils.populateDefaultGroup()
        }
    }


}
