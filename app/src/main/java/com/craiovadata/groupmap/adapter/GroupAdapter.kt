package com.craiovadata.groupmap.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.utils.GROUP_NAME
import com.craiovadata.groupmap.utils.GlideApp
import com.craiovadata.groupmap.utils.PHOTO_URL
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.item_group.view.*
import java.text.SimpleDateFormat
import java.util.*

open class GroupAdapter(query: Query, private val listener: OnItemSelectedListener) :
        FirestoreAdapter<GroupAdapter.ViewHolder>(query) {

    interface OnItemSelectedListener {
        fun onItemSelected(group: DocumentSnapshot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return ViewHolder(inflater.inflate(R.layout.item_group, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getSnapshot(position), listener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(snapshot: DocumentSnapshot, listener: OnItemSelectedListener?) {

            val group = snapshot.data as? HashMap<String, Any> ?: return
            group[PHOTO_URL]?.let {
                GlideApp.with(itemView.groupPhoto.context)
                    .load(it)
                    .placeholder(R.drawable.ic_people)
                    .into(itemView.groupPhoto)
            }


            group[GROUP_NAME]?.let {
                itemView.groupPhoto.contentDescription =
                    String.format(itemView.context.getString(R.string.description_person_icon), it as String)

                itemView.groupNameTextView.text = it as String
            }

            itemView.setOnClickListener {
                listener?.onItemSelected(snapshot)
            }
        }
    }


    companion object {
        private val FORMAT = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
    }

}
