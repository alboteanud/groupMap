package com.craiovadata.groupmap.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.craiovadata.groupmap.BuildConfig
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.utils.*
import com.craiovadata.groupmap.utils.GroupUtils.startActionShare
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_group_info.*
import kotlinx.android.synthetic.main.content_group_info.*
import java.text.DateFormat.getDateInstance

class GroupInfoActivity : AppCompatActivity() {
    private var groupData: HashMap<String, Any>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_info)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        handleIntent()
        button_share_group.setOnClickListener {
            groupData?.get(GROUP_SHARE_KEY)?.let {
                startActionShare(this, it) }
            }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent()
    }

    private fun handleIntent() {
        val groupId = intent.getStringExtra(GROUP_ID) ?: return

        val db = FirebaseFirestore.getInstance()
        val docRef = db.document("$GROUPS/$groupId")

        docRef.get().addOnSuccessListener { snapshot ->
            if (snapshot != null && snapshot.exists()) {
                Log.d(TAG, "Current data: ${snapshot.data}")
                groupData = snapshot.data as HashMap<String, Any>
                updateUI()
            } else {
                Log.d(TAG, "Current data: null")
            }
        }
    }

    private fun updateUI() {
        groupData?.apply {
            group_name.text = this[GROUP_NAME] as String
            val timestampCreated = this[CREATED_AT] as? Timestamp
            val dtStr = getDateInstance().format(timestampCreated?.toDate())
            group_date.text = dtStr
//            @Suppress("UNCHECKED_CAST")
            val founder = this[GROUP_FOUNDER] as? HashMap<String, Any>
            group_founder.text = founder?.get(NAME) as String
            if (BuildConfig.DEBUG) {
                val baseShareUrl = getString(R.string.base_share_url)
                val groupLinkStr = baseShareUrl + this[GROUP_SHARE_KEY] as String?
                group_link.text = groupLinkStr

            }
        }
    }

    companion object {
        private const val TAG = "GroupInfoActivity"
    }

}
