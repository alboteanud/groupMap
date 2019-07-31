package com.craiovadata.groupmap.viewmodel.old

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.craiovadata.groupmap.repo.oldDB.MyDatabaseDao
import com.craiovadata.groupmap.model.Member
import com.craiovadata.groupmap.utils_.*
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.lang.IllegalStateException

class MapViewModel(var database: MyDatabaseDao) : ViewModel() {
    val db = FirebaseFirestore.getInstance()
    //    var memberRole: Int? = null
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    var positionListenerRegistration: ListenerRegistration? = null

    private var groupData = MutableLiveData<DocumentSnapshot?>()
    private var userData = MutableLiveData<DocumentSnapshot?>()
    val groupName: LiveData<String?> = Transformations.map(groupData) {
        it?.get(GROUP_NAME) as? String
    }
    val joinButtonVisible = Transformations.map(userData) {
        val groupId = database.getGroupId()
        null == it && groupId != null && groupId != DEFAULT_GROUP
    }


    init {
        initializeConnection()
    }

    private fun initializeConnection() {
        database.fetchGroupInfo { groupInfo ->
            groupData.value = groupInfo
            database.fetchUserFromGroup { currentUserData ->
                userData.value = currentUserData
                loadUsers()
//                requestPositionUpdatesFromOthers()
//                    invalidateOptionsMenu()
//                    setPositionsListener()

            }
        }
    }


    private fun loadUsers() {
        // Do an asynchronous operation to fetch allGroupUsers.
        database.fetchAllMembers { allMembers ->
            allGroupUsers.value = allMembers?.mapNotNull { member ->
                val person = Member(member)
                if (person.position == null) null
                else person
            }
        }
    }

//    fun exitGroup() {
//        val groupId = database.getGroupId()
//        val uid = auth.currentUser?.uid ?: return
//        db.document("$USERS/$uid/$GROUPS/$groupId").delete()
//            .addOnSuccessListener {
//                positionListenerRegistration?.remove()
//                groupData = null
//                memberRole = null
//                setTitle()
//                map?.clear()
//                groupId = null
//                saveGroupIdToPref(null)
//                MapUtils.zoomOnMe(this, map)
//                Snackbar.make(content_main, "You left the group", Snackbar.LENGTH_SHORT).show()
//            }
//    }

    fun onQuitGroup() {
        database.deleteGroupInUser {

        }
    }

    fun handleJoinAsked(canStartLogin: Boolean = false) {
        val groupId = database.getGroupId()
//        if (memberRole == ROLE_USER || memberRole == ROLE_ADMIN || groupId == null) return
        if (auth.currentUser == null && canStartLogin) {
//            Util.startLoginActivity(this, RC_SIGN_IN_ASKED_JOIN)
            return
        }
        val groupName = " ..."
        GroupUtils.joinGroup(groupId, groupName) { role ->
            if (role != null) {
//                Snackbar.make(content_main, "You joined the group", Snackbar.LENGTH_LONG).show()
//                memberRole = role
//                MapUtils.zoomOnMe(this, map)
//                invalidateOptionsMenu()
            } else {
//                Snackbar.make(content_main, "Failed to join the group", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    fun requestPositionUpdatesFromOthers() {
        val groupId = database.getGroupId() ?: return
//        if (!(memberRole == ROLE_USER || memberRole == ROLE_ADMIN)) return
        val uid = auth.currentUser?.uid ?: return

        val ref = db.document("$REQUESTS/$groupId")
        ref.set(mapOf(UID to uid, TIMESTAMP to FieldValue.serverTimestamp()))
    }

    override fun onCleared() {
        super.onCleared()
        Log.i("MapViewModel", "MapViewModel destroyed")
    }

    fun getCameraBounds(): LatLngBounds? {
        val users = getUsers().value
        if (users.isNullOrEmpty()) {
            return null
        }
        val builder = LatLngBounds.Builder()
        users.forEach { user ->
            builder.include(user.position)
        }
        return try {
            builder.build()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            null
        }


    }

    private val allGroupUsers: MutableLiveData<List<Member>> by lazy {
        MutableLiveData<List<Member>>().also { loadUsers() }
    }

    fun getUsers(): LiveData<List<Member>> {
        return allGroupUsers
    }

}