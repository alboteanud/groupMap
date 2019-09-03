package com.craiovadata.groupmap.activity.groupinfo

import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.ListAdapter
import android.view.LayoutInflater
import android.view.ViewGroup
import com.craiovadata.groupmap.config.AppExecutors
import com.craiovadata.groupmap.databinding.MemberListItemBinding
import com.craiovadata.groupmap.repo.QueryItem
import com.craiovadata.groupmap.viewmodel.UserDisplay
import com.craiovadata.groupmap.viewmodel.userDisplayDiffCallback
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * This ListAdapter maps a QueryItem<GroupDisplay> data object into a
 * MyGroupsViewHolder via data binding in group_list_item.
 */

internal class MemberListAdapter (private val itemClickListener: ItemClickListener<MemberViewHolder>)
    : ListAdapter<QueryItem<UserDisplay>, MemberViewHolder>(asyncDifferConfig) {

    companion object : KoinComponent {
        private val executors by inject<AppExecutors>()
        private val asyncDifferConfig =
            AsyncDifferConfig.Builder<QueryItem<UserDisplay>>(userDisplayDiffCallback)
                .setBackgroundThreadExecutor(executors.cpuExecutorService)
                .build()

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        // Using data binding on the individual views
        val inflater = LayoutInflater.from(parent.context)
        val binding = MemberListItemBinding.inflate(inflater, parent, false)
        val holder = MemberViewHolder(binding)
        holder.itemClickListener = itemClickListener
        return holder
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val qItem = getItem(position)
        holder.binding.userDisplay = qItem.item
        holder.uid = qItem.id
    }

    internal interface ItemClickListener<UserViewHolder> {
        fun onItemClick(holder: UserViewHolder)
    }

}
