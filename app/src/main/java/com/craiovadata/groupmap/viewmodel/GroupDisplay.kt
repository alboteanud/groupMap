package com.craiovadata.groupmap.viewmodel

import com.craiovadata.groupmap.diffcallback.QueryItemDiffCallback
import com.craiovadata.groupmap.model.Group

/**
 * A container class for displaying properly formatted stock role data.
 */

data class GroupDisplay(
    val groupName: String, val shareKey: String?
)

/**
 * Converts a User object into a UserDisplay object.
 */

fun Group.toGroupDisplay() = GroupDisplay(
    this.name, this.sk
//    priceFormatter.format(this.price)
)


val groupDisplayDiffCallback = object : QueryItemDiffCallback<GroupDisplay>() {}

