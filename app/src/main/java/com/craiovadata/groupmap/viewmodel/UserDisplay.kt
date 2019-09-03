package com.craiovadata.groupmap.viewmodel

import android.widget.ImageView
import com.craiovadata.groupmap.diffcallback.QueryItemDiffCallback
import com.craiovadata.groupmap.model.User
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.Glide
import androidx.databinding.BindingAdapter
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.utils_.ROLE_ADMIN


/**
 * A container class for displaying properly formatted stock role data.
 */
data class UserDisplay(
    val name: String,
    val photoUrl: String?,
    val role: Int?
)  {
    fun isAdmin(): Boolean {
        return role!=null && role >= ROLE_ADMIN
    }
}

@BindingAdapter("app:profileImageI")
fun loadImage(view: ImageView, imageUrl: String?) {
    Glide.with(view.getContext())
        .load(imageUrl).apply(RequestOptions().circleCrop())
        .placeholder(R.drawable.ic_face)
        .into(view)
}

/**
 * Converts a User object into a UserDisplay object.
 */

fun User.toUserDisplay() = UserDisplay(
    this.name,
    this.img,
    this.role
)


val userDisplayDiffCallback = object : QueryItemDiffCallback<UserDisplay>() {}

