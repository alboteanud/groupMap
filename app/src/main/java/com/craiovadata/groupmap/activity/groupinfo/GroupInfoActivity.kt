package com.craiovadata.groupmap.activity.groupinfo

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.activity.base.BaseActivity
import com.craiovadata.groupmap.adapter_.MemberAdapter
import com.craiovadata.groupmap.databinding.ActivityGroupInfoBinding
import com.craiovadata.groupmap.utils_.EXTRA_GROUP_ID
import com.craiovadata.groupmap.utils_.GROUP_ID
import com.craiovadata.groupmap.utils_.Util.startActionShare
import com.craiovadata.groupmap.viewmodel.GroupDisplay
import com.craiovadata.groupmap.viewmodel.GroupInfoViewModel
import com.craiovadata.groupmap.viewmodel.MemberDisplayQueryResults
import com.craiovadata.groupmap.viewmodel.UserDisplay
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.activity_group_info.*
import timber.log.Timber
import kotlin.collections.ArrayList

class GroupInfoActivity : BaseActivity() {

    private var groupId: String? = null
    private var adapter: MemberAdapter? = null
    private lateinit var viewModel: GroupInfoViewModel
    private var group: GroupDisplay? = null
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupId = intent.getStringExtra(EXTRA_GROUP_ID)
            ?: throw IllegalArgumentException("groupId not provided")

        initViews()
    }

    private fun initViews() {
        val binding: ActivityGroupInfoBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_group_info)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewModel = ViewModelProviders.of(this).get(GroupInfoViewModel::class.java)
        binding.lifecycleOwner = this
        binding.infoViewModel = viewModel

        setupObservers()
        button_share_group.setOnClickListener {
            startActionShare(this, group)
        }
    }

    private fun setupObservers() {
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerMembers).apply {
            setHasFixedSize(true)
            val listAdapter = MemberListAdapter(ItemClickListener())

            viewModel.getMembers(groupId!!)
                .observe(this@GroupInfoActivity, Observer<MemberDisplayQueryResults> {
                    if (it.data != null) {
                        listAdapter.submitList(it.data)
                    } else if (it.exception != null) {
                        snack("Error getting members")
                        Timber.e(it.exception, "Error getting members %s", it.exception.message)
                    }

                })
            adapter = listAdapter
        }

        viewModel.isAdmin.observe(this, Observer {
            if (it != isAdmin) {
                isAdmin = it
                invalidateOptionsMenu()
            }
        })

        viewModel.getGroupLiveData(groupId!!).observe(this, Observer {
            // for use in buildAlertChangeGroupName()
            group = it.data
        })

        viewModel.textGroupName.observe(this, Observer {
            toolbar.title = it
        })

    }

    private inner class ItemClickListener : MemberListAdapter.ItemClickListener<MemberViewHolder> {
        override fun onItemClick(holder: MemberViewHolder) {
            val uid = holder.uid
            val member = holder.binding.userDisplay
            buildAlertMemberOptions(member, uid)
        }
    }

    private fun buildAlertMemberOptions(member: UserDisplay?, uid: String?) {
        if (member == null || uid == null) return
        if (!isAdmin) return               // only admins can modify
        if (uid == auth.currentUser?.uid) return    // no alert in case of me selected

        val list = ArrayList<String>()
        list.add(REMOVE)
        if (member.isAdmin()) list.add(DISSMISS_AS_ADMIN)
        else list.add(MAKE_ADMIN)

        val builder = AlertDialog.Builder(this)
        builder.setItems(list.toTypedArray()) { dialogInterface: DialogInterface, i: Int ->
            when (list[i]) {
                REMOVE -> viewModel.onRemoveUser(uid)
                DISSMISS_AS_ADMIN -> viewModel.onDissmissAsAdmin(uid)
                MAKE_ADMIN -> viewModel.onMakeGroupAdmin(uid)
            }
        }
        builder.create().show()
    }

    private fun buildAlertChangeGroupName() {
        val textInputLayout = TextInputLayout(this)
        val padding = resources.getDimensionPixelOffset(R.dimen.dp_16)
        textInputLayout.setPadding(padding, padding, padding, 0)
        val input = EditText(this)
        textInputLayout.hint = "Group name"
        input.setText(group!!.groupName, TextView.BufferType.EDITABLE)
        textInputLayout.addView(input)

        val builder = AlertDialog.Builder(this)
            .setView(textInputLayout)
            .setPositiveButton("OK") { dialog, which ->
                val newName = input.text.toString()
                if (TextUtils.isEmpty(newName)) {
                    snack("enter a valid name")
                    return@setPositiveButton
                }
                viewModel.onNameChange(newName)
            }
            .setNegativeButton(
                "Cancel"
            ) { dialog, which -> dialog.cancel() }
//            .setTitle("edit")
        builder.create().show()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newGroupId = intent?.getStringExtra(GROUP_ID)
        if (newGroupId != null) {
            groupId = newGroupId
            setupObservers()
        }
    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_group_info, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menu_item_edit_group_name)?.isVisible = isAdmin
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_edit_group_name -> {
                buildAlertChangeGroupName()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val REMOVE = "Remove"
        const val DISSMISS_AS_ADMIN = "Dismiss as admin"
        const val MAKE_ADMIN = "Make group admin"

        fun newIntent(context: Context, groupId: String): Intent {
            val intent = Intent(context, GroupInfoActivity::class.java)
            intent.putExtra(EXTRA_GROUP_ID, groupId)
            return intent
        }
    }

}
