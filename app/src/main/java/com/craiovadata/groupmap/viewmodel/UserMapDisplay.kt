package com.craiovadata.groupmap.viewmodel

import android.annotation.SuppressLint
import android.widget.ImageView
import com.craiovadata.groupmap.diffcallback.QueryItemDiffCallback
import com.craiovadata.groupmap.model.User

import com.craiovadata.groupmap.viewmodel.Formatters.timeFormatter
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import java.text.SimpleDateFormat
import java.util.*
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.Glide
import androidx.databinding.BindingAdapter
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.util.ROLE_ADMIN
import java.util.concurrent.TimeUnit


/**
 * A container class for displaying properly formatted stock role data.
 */
data class UserMapDisplay(
    val name: String,
    val photoUrl: String?,
    val role: Int?,
    val location: LatLng?,
    val time: Date?,
    val pause: Boolean?
) : ClusterItem {

    override fun getSnippet(): String? {
        if (time == null) return null
        return timeFormatter.format(time)
    }

    override fun getTitle(): String {
        return name
    }

    override fun getPosition(): LatLng? {
        return location
    }

    fun isAdmin(): Boolean {
        return role != null && role >= ROLE_ADMIN
    }

    var uid: String? = null
    fun setId(uid: String) {
        this.uid = uid
    }

    fun isPositionFresh(): Boolean {
        val delay = TimeUnit.MINUTES.toMillis(30)
        val someTimeAgo = Date(System.currentTimeMillis() - delay)

        return time != null && time > someTimeAgo
    }
}

@BindingAdapter("app:profileImageMap")
fun loadImage2(view: ImageView, imageUrl: String?) {
    Glide.with(view.context)
        .load(imageUrl).apply(RequestOptions().circleCrop())
        .placeholder(R.drawable.ic_face)
        .into(view)
}

/**
 * Converts a User object into a UserDisplay object.
 */

fun User.toUserMapDisplay() = UserMapDisplay(
    this.name,
    this.img,
    this.role,
    this.loc,
    this.time,
    this.pause
//    priceFormatter.format(this.role)
)


val userMapDisplayDiffCallback = object : QueryItemDiffCallback<UserMapDisplay>() {}


@SuppressLint("ConstantLocale")
private object Formatters {

    val timeFormatter by lazy {
        val locale = Locale.getDefault()
        SimpleDateFormat("HH:mm  EEE dd", locale)
    }

}
