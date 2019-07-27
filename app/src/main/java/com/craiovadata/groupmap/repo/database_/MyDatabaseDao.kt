package com.craiovadata.groupmap.repo.database_

import com.google.firebase.firestore.DocumentSnapshot

//@Dao
interface MyDatabaseDao {

    fun fetchGroupInfo(callback: (snap: DocumentSnapshot?)-> Unit): DocumentSnapshot?

    fun setNewUpdatePositionsRequest()

    fun getGroupId(): String?

    fun fetchUserFromGroup(callback: (snap: DocumentSnapshot?)-> Unit): DocumentSnapshot?

    fun fetchAllMembers (callback: (members: List<DocumentSnapshot>? )-> Unit): List<DocumentSnapshot>?

    fun deleteGroupInUser (callback: ()-> Unit)

}