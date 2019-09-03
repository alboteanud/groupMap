
package com.craiovadata.groupmap.repo

import com.craiovadata.groupmap.common.DataOrException
import com.craiovadata.groupmap.model.Group
import com.craiovadata.groupmap.model.User

/**
 * An item of data type T that resulted from a query. It adds the notion of
 * a unique id to that item.
 */

interface QueryItem<T> {
    val item: T
    val id: String
}

typealias QueryItemOrException<T> = DataOrException<QueryItem<T>, Exception>

data class UserQueryItem(private val _item: User, private val _id: String) : QueryItem<User> {
    override val item: User
        get() = _item
    override val id: String
        get() = _id
}

data class GroupQueryItem(private val _item: Group, private val _id: String) : QueryItem<Group> {
    override val item: Group
        get() = _item
    override val id: String
        get() = _id
}

typealias UserOrException = DataOrException<User, Exception>
typealias GroupOrException = DataOrException<Group, Exception>
typealias GroupIdOrException = DataOrException<String?, Exception?>
typealias SuccessOrException = DataOrException<String?, Exception?>

/**
 * The results of a database query (a List of QueryItems), or an Exception.
 */

typealias QueryResultsOrException<T, E> = DataOrException<List<QueryItem<T>>, E>

typealias UsersQueryResults = QueryResultsOrException<User, Exception>
typealias GroupsQueryResults = QueryResultsOrException<Group, Exception>
