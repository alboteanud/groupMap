package com.craiovadata.groupmap.adapter_

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.util.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.member_list_item.view.*
import java.text.SimpleDateFormat
import java.util.*

open class MemberAdapter(query: Query, private val listener: OnItemSelectedListener) :
    FirestoreAdapter<MemberAdapter.ViewHolder>(query) {

    interface OnItemSelectedListener {
        fun onItemSelected(member: DocumentSnapshot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return ViewHolder(inflater.inflate(R.layout.member_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getSnapshot(position), listener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(snapshot: DocumentSnapshot, listener: OnItemSelectedListener?) {

            val member = snapshot.data as? HashMap<String, Any> ?: return
            member[PHOTO_URL]?.let {
                GlideApp.with(itemView.memberPhoto.context)
                    .load(it)
                    .placeholder(R.drawable.ic_face)
                    .into(itemView.memberPhoto)
            }


            member[NAME]?.let {
                itemView.memberPhoto.contentDescription =
                    String.format(itemView.context.getString(R.string.description_person_icon), it as String)

                itemView.memberName.text = it as String
            }

            val role = (member.get(ROLE) as? Long)?.toInt() ?: ROLE_USER
            if (role == ROLE_ADMIN) {
                itemView.isAdminText.visibility = VISIBLE
                itemView.isAdminText.text = itemView.context.getString(R.string.label_admin)
            } else {
                itemView.isAdminText.text = null
                itemView.isAdminText.visibility = GONE
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
