package com.craiovadata.groupmap.activity.creategroup

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View.*
import android.view.WindowManager
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.activity.base.BaseActivity
import com.craiovadata.groupmap.activity.map.MapActivity
import com.craiovadata.groupmap.activity.mygroups.MyGroupsActivity
import com.craiovadata.groupmap.tracker.TrackerService
import com.craiovadata.groupmap.viewmodel.CreateGroupViewModel
import kotlinx.android.synthetic.main.activity_create_group.*
import kotlinx.android.synthetic.main.content_create_group.*


class CreateGroupActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // The model
        val viewModel = ViewModelProviders.of(this).get(CreateGroupViewModel::class.java)

        buttonCreateGroup.setOnClickListener {
            val groupName = getInsertedText() ?: return@setOnClickListener
            buttonCreateGroup.isEnabled = false
            progress_circular.visibility = VISIBLE
            viewModel.setNewGroup(groupName)
        }

        viewModel.navigateToMap.observe(this, Observer {
            if (it != null) {
                val groupId = it.data
                if (groupId != null) {
                    // just created new group
//                    PrefUtils.saveGroupId(this, groupId)
                    Toast.makeText(this, "success", Toast.LENGTH_SHORT).show()
                    viewModel.doneNavigating()
                    startActivity(Intent(this, MyGroupsActivity::class.java))
                    finish()
                    // Reset state to make sure we only navigate once, even if the device
                    // has a configuration change.

                } else if (it.exception != null) {
                    val msg = getString(com.craiovadata.groupmap.R.string.toast_group_creation_error)
                    snack(msg)
                }
            }
            progress_circular.visibility = INVISIBLE
            buttonCreateGroup.isEnabled = true
        })

    }

    private fun getInsertedText(): String? {
        val groupName = editTextGroupName.text.toString()
        if (TextUtils.isEmpty(groupName)) {
            val errMsg = getString(com.craiovadata.groupmap.R.string.error_msg_title_not_blank)
            editTextGroupName.error = errMsg
            return null
        }
        return groupName
    }



}

