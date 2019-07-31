package com.craiovadata.groupmap.activity.main

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import com.craiovadata.groupmap.R
import com.craiovadata.groupmap.activity.map.MapActivity
import com.craiovadata.groupmap.activity.map.MapActivity2
import com.craiovadata.groupmap.koin.RuntimeConfig
import com.craiovadata.groupmap.repo.firestore.FirestoreRepository
import com.craiovadata.groupmap.utils_.DEFAULT_GROUP
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_create_group.*
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val RC_SIGN_IN = Activity.RESULT_FIRST_USER

        private enum class Database { Firestore, RealtimeDatabase }

        private var database = Companion.Database.Firestore
    }

    private val auth by inject<FirebaseAuth>()
    private val runtimeConfig by inject<RuntimeConfig>()

    private lateinit var vRoot: View
    private lateinit var btnLogIn: Button
    private lateinit var btnLogOut: Button
    private lateinit var btnToggleDb: Button
    private lateinit var btnTrackTwo: Button
    private lateinit var btnTrackRecyclerView: Button
    private lateinit var btnTrackPagedRecyclerView: Button
    private lateinit var btnStockHistory: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            // Successfully signed in
            if (resultCode == Activity.RESULT_OK) {
                snack("Signed in")
            }
            else {
                // Sign in failed
                val response = IdpResponse.fromResultIntent(data) ?: return

                if (response.error!!.errorCode == ErrorCodes.NO_NETWORK) {
                    snack("No network")
                    return
                }

                snack("Unknown error with sign in")
                Log.e(TAG, "Sign-in error: ", response.error)
            }
        }
    }

    private fun initViews() {
        setContentView(R.layout.activity_main)
        vRoot = findViewById(R.id.root)
        setSupportActionBar(toolbar)
        btnLogIn = findViewById(R.id.btn_log_in)
        btnLogIn.setOnClickListener {
            startActivityForResult(
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(
                        listOf(
                            AuthUI.IdpConfig.GoogleBuilder().build(),
                            AuthUI.IdpConfig.EmailBuilder().build()
                        )
                    )
                    .build(),
                RC_SIGN_IN
            )
        }

        btnLogOut = findViewById(R.id.btn_log_out)
        btnLogOut.setOnClickListener {
            AuthUI.getInstance().signOut(this)
        }

        btnToggleDb = findViewById(R.id.btn_toggle_db)
        updateToggleButton()
        btnToggleDb.setOnClickListener {
            toggleDatabase()
        }

        btnTrackTwo = findViewById(R.id.btn_track_two_stocks)
        btnTrackTwo.setOnClickListener {
            //            startActivity(Intent(this, StockPriceTrackerActivity::class.java))
        }

        btnTrackRecyclerView = findViewById(R.id.btn_track_recycler_view)
        btnTrackRecyclerView.setOnClickListener {
            //            startActivity(Intent(this, StockPriceTrackerRecyclerViewActivity::class.java))
        }

        btnTrackPagedRecyclerView = findViewById(R.id.btn_track_paged_recycler_view)
        btnTrackPagedRecyclerView.setOnClickListener {
            //            startActivity(Intent(this, AllStocksPagedRecyclerViewActivity::class.java))
                        startActivity(Intent(this, MapActivity::class.java))
        }

        btnStockHistory = findViewById(R.id.btn_stock_history)
        btnStockHistory.setOnClickListener {
            //            startActivity(StockPriceHistoryActivity.newIntent(this, "HSTK"))
                        startActivity(MapActivity2.newIntent(this, DEFAULT_GROUP))
        }
    }

    private fun updateToggleButton() {
        val label = when (database) {
            Database.Firestore -> "Switch to Realtime Database"
            Database.RealtimeDatabase -> "Switch to Firestore"
        }
        btnToggleDb.text = label
    }

    private fun toggleDatabase() {
        when (database) {
            Database.Firestore -> {
//                runtimeConfig.repository = inject<RealtimeDatabaseGroupMapRepository>().value
                runtimeConfig.repository = inject<FirestoreRepository>().value
                database = Companion.Database.RealtimeDatabase
            }
            Database.RealtimeDatabase -> {
                runtimeConfig.repository = inject<FirestoreRepository>().value
                database = Database.Firestore
            }
        }
        updateToggleButton()
    }

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val loggedIn = auth.currentUser != null
        btnLogIn.isEnabled = !loggedIn
        btnLogOut.isEnabled = loggedIn
        btnTrackTwo.isEnabled = loggedIn
        btnTrackRecyclerView.isEnabled = loggedIn
        btnTrackPagedRecyclerView.isEnabled = loggedIn
        btnStockHistory.isEnabled = loggedIn
//        if (loggedIn) {
//            startActivity(Intent(this, MultiTrackerRecyclerView::class.java))
//        }
    }

    private fun snack(message: String) {
        Snackbar.make(vRoot, message, Snackbar.LENGTH_SHORT).show()
    }

}
