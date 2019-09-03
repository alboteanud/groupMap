package com.craiovadata.groupmap.activity.creategroup

import android.os.Bundle
import android.text.TextUtils
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.craiovadata.groupmap.activity.base.BaseActivity
import com.craiovadata.groupmap.activity.map.MapActivity
import com.craiovadata.groupmap.utils_.PrefUtils
import com.craiovadata.groupmap.viewmodel.CreateGroupViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_create_group.*
import kotlinx.android.synthetic.main.content_create_group.*
import org.koin.android.ext.android.inject


class CreateGroupActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.craiovadata.groupmap.R.layout.activity_create_group)
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
                    startActivity(MapActivity.newIntent(this, groupId))
                    finish()
                    // Reset state to make sure we only navigate once, even if the device
                    // has a configuration change.
                    viewModel.doneNavigating()
                } else if (it.exception != null) {
                    val msg = getString(com.craiovadata.groupmap.R.string.toast_group_creation_error)
                    snack(msg)
                }
            }
            progress_circular.visibility = GONE
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

