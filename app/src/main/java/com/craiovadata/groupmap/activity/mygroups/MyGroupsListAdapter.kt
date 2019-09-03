package com.craiovadata.groupmap.activity.mygroups

import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.ListAdapter
import android.view.LayoutInflater
import android.view.ViewGroup
import com.craiovadata.groupmap.config.AppExecutors
import com.craiovadata.groupmap.databinding.GroupListItemBinding
import com.craiovadata.groupmap.repo.QueryItem
import com.craiovadata.groupmap.viewmodel.GroupDisplay
import com.craiovadata.groupmap.viewmodel.groupDisplayDiffCallback
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * This ListAdapter maps a QueryItem<GroupDisplay> data object into a
 * MyGroupsViewHolder via data binding in group_list_item.
 */

internal class MyGroupsListAdapter (private val itemClickListener: ItemClickListener<MyGroupsViewHolder>)
    : ListAdapter<QueryItem<GroupDisplay>, MyGroupsViewHolder>(asyncDifferConfig) {

    companion object : KoinComponent {
        private val executors by inject<AppExecutors>()
        private val asyncDifferConfig =
            AsyncDifferConfig.Builder<QueryItem<GroupDisplay>>(groupDisplayDiffCallback)
                .setBackgroundThreadExecutor(executors.cpuExecutorService)
                .build()

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyGroupsViewHolder {
        // Using data binding on the individual views
        val inflater = LayoutInflater.from(parent.context)
        val binding = GroupListItemBinding.inflate(inflater, parent, false)
        val holder = MyGroupsViewHolder(binding)
        holder.itemClickListener = itemClickListener
        return holder
    }

    override fun onBindViewHolder(holder: MyGroupsViewHolder, position: Int) {
        val qItem = getItem(position)
        holder.binding.groupDisplay = qItem.item
        holder.groupId = qItem.id
    }

    internal interface ItemClickListener<MyGroupsViewHolder> {
        fun onItemClick(holder: MyGroupsViewHolder)
    }

}
