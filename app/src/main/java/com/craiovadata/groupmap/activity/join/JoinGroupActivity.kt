package com.craiovadata.groupmap.activity.join

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.TaskStackBuilder
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.activity.base.BaseActivity
import com.craiovadata.groupmap.activity.map.MapActivity
import com.craiovadata.groupmap.activity.mygroups.MyGroupsActivity
import com.craiovadata.groupmap.databinding.ActivityJoinGroupBinding
import com.craiovadata.groupmap.util.EXTRA_GROUP_ID
import com.craiovadata.groupmap.util.EXTRA_GROUP_NAME
import com.craiovadata.groupmap.viewmodel.JoinGroupViewModel
import kotlinx.android.synthetic.main.activity_create_group.toolbar
import kotlinx.android.synthetic.main.activity_join_group.*


class JoinGroupActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME) ?: ""
        val groupId = intent.getStringExtra(EXTRA_GROUP_ID)
            ?: throw  IllegalArgumentException("No groupId provided")

        // Inflate view and obtain an instance of the binding class.
        val binding: ActivityJoinGroupBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_join_group)
        binding.lifecycleOwner = this
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val viewModel = ViewModelProviders.of(this).get(JoinGroupViewModel::class.java)
        binding.joinGroupViewModel = viewModel

        viewModel.navigateToMap.observe(this, Observer {
            if (it != null && it) {
                viewModel.doneNavigatingToMap()
                navigateToMapActivity(groupId)

            }
        })

        viewModel.navigateToMyGroups.observe(this, Observer {
            if (it != null && it) {
                viewModel.doneNavigatingToMyGroups()
                navigateToMyGroupsActivity()
            }
        })

        val labelText = getString(R.string.label_join_group_name, groupName)
        textViewGroupName.text = labelText
        btn_join_group.setOnClickListener {
            viewModel.onJoin(groupId, groupName)
        }

    }

    private fun navigateToMapActivity(groupId: String) {
        val intent = MapActivity.newIntent(this, groupId)

        TaskStackBuilder.create(this)
            .addParentStack(MapActivity::class.java)
            .addNextIntent(intent)
            .startActivities()
        finish()
    }

    private fun navigateToMyGroupsActivity() {
        val intent = Intent(this, MyGroupsActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {

        fun newIntent(context: Context, groupId: String, groupName: String): Intent {
            val intent = Intent(context, JoinGroupActivity::class.java)
            intent.putExtra(EXTRA_GROUP_ID, groupId)
            intent.putExtra(EXTRA_GROUP_NAME, groupName)
            return intent
        }
    }

}
