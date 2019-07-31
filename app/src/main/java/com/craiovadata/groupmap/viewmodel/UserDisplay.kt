package com.craiovadata.groupmap.viewmodel

import android.annotation.SuppressLint
import com.craiovadata.groupmap.diffcallback.QueryItemDiffCallback
import com.craiovadata.groupmap.model.User

import com.craiovadata.groupmap.viewmodel.Formatters.priceFormatter
import com.craiovadata.groupmap.viewmodel.Formatters.timeFormatter
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * A container class for displaying properly formatted stock role data.
 */

data class UserDisplay(
//    val id: String,
    val name: String,
    val photoUrl: String?,
    val role: Int?,
    val location: LatLng?,
    val locationTimestamp: Date?
): ClusterItem {

    override fun getSnippet(): String? {
        return null
    }

    override fun getTitle(): String {
        return name
    }

    override fun getPosition(): LatLng? {
        return location
    }

}

/**
 * Converts a User object into a UserDisplay object.
 */

fun User.toUserDisplay() = UserDisplay(
//    this.id,
    this.name,
    this.photoUrl,
    this.role,
    this.location,
    this.locationTimestamp
//    this.id,
//    priceFormatter.format(this.role),
//    timeFormatter.format(this.locationTimestamp)
)

val stockPriceDisplayDiffCallback = object : QueryItemDiffCallback<UserDisplay>() {}


@SuppressLint("SimpleDateFormat")
private object Formatters {

    val timeFormatter by lazy {
        SimpleDateFormat("HH:mm:ss")
    }

    val priceFormatter by lazy {
        val priceFormatter = NumberFormat.getNumberInstance()
        priceFormatter.minimumFractionDigits = 2
        priceFormatter.maximumFractionDigits = 2
        priceFormatter
    }

}
