package com.craiovadata.groupmap.activity.join

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.app.TaskStackBuilder
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.activity.base.BaseActivity
import com.craiovadata.groupmap.activity.map.MapActivity
import com.craiovadata.groupmap.activity.mygroups.MyGroupsActivity
import com.craiovadata.groupmap.databinding.ActivityJoinGroupBinding
import com.craiovadata.groupmap.utils_.Util.goToPrivacyPolicy
import com.craiovadata.groupmap.viewmodel.JoinGroupViewModel
import kotlinx.android.synthetic.main.activity_create_group.*


class JoinGroupActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val groupShareKey = intent.getStringExtra(EXTRA_GROUP_SHARE_KEY)
            ?: throw  IllegalArgumentException("No group share key provided")

        // Inflate view and obtain an instance of the binding class.
        val binding: ActivityJoinGroupBinding = DataBindingUtil.setContentView(this, R.layout.activity_join_group)
        binding.lifecycleOwner = this
        initViews()
        val viewModel = ViewModelProviders.of(this).get(JoinGroupViewModel::class.java)
        binding.joinGroupViewModel = viewModel

        viewModel.navigateToMap.observe(this, Observer { successOrException ->
            successOrException?.let {
                if (it.data != null) {
                    val groupId = it.data
                    navigateToMapActivity(groupId)
                    viewModel.doneNavigatingToMap()
                } else if (it.exception != null) {
                    snack("error " + it.exception.message)
                }
            }
        })

        val groupLiveData = viewModel.getGroup(groupShareKey)
        groupLiveData.observe(this, Observer {
            // empty but needed for FirestoreQueryLiveData
        })

        viewModel.navigateToControlPanel.observe(this, Observer {
            if (it != null){
                startActivity(Intent(this, MyGroupsActivity::class.java))
                viewModel.doneNavigatingToControlPanel()
            }
        })
    }

    private fun navigateToMapActivity(groupId: String) {
        val intent = MapActivity.newIntent(this, groupId)

        TaskStackBuilder.create(this)
            .addParentStack(MapActivity::class.java)
            .addNextIntent(intent)
            .startActivities()
        finish()
    }

    private fun initViews() {
        setSupportActionBar(toolbar)

        val tv : View = findViewById(R.id.btn_privacy)
        tv.setOnClickListener {
            goToPrivacyPolicy(it.context)
        }
    }


    companion object {
        private const val EXTRA_GROUP_SHARE_KEY = "extra_group_share_key"

        fun newIntent(context: Context, shareKey: String): Intent {
            val intent = Intent(context, JoinGroupActivity::class.java)
            intent.putExtra(EXTRA_GROUP_SHARE_KEY, shareKey)
            return intent
        }
    }

}
