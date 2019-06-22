package com.craiovadata.groupmap.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.adapter.GroupAdapter
import com.craiovadata.groupmap.utils.GROUPS
import com.craiovadata.groupmap.utils.GROUP_ID
import com.craiovadata.groupmap.utils.JOINED
import com.craiovadata.groupmap.utils.USERS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot

import kotlinx.android.synthetic.main.activity_my_groups.*
import kotlinx.android.synthetic.main.content_my_groups.*

class MyGroupsActivity : AppCompatActivity(), GroupAdapter.OnItemSelectedListener {
    private var adapter: GroupAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_groups)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setUpRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    private fun setUpRecyclerView() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = currentUser.uid
        val db = FirebaseFirestore.getInstance()
        val query = db.collection("USERS/$uid/GROUPS")
            .whereEqualTo(JOINED, true).limit(10)


        adapter = object: GroupAdapter(query, this@MyGroupsActivity){

            override fun onDataChanged() {
                // Show/hide content if the query returns empty.
                if (itemCount == 0) {
                    recyclerGroups.visibility = View.GONE
//                    viewEmpty.visibility = View.VISIBLE
                } else {
                    recyclerGroups.visibility = View.VISIBLE
//                    viewEmpty.visibility = View.GONE
                }
            }

            override fun onError(e: FirebaseFirestoreException) {
                // Show a snackbar on errors
                Snackbar.make(findViewById(android.R.id.content),
                    "Error: check logs for info.", Snackbar.LENGTH_LONG).show()
            }

        }
        recyclerGroups.adapter = adapter
//        adapter?.startListening()  // for search to work
    }



    override fun onItemSelected(group: DocumentSnapshot) {
        // Go to the details page for the selected restaurant
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(GROUP_ID, group.id)

        startActivity(intent)
//        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
    }


}
