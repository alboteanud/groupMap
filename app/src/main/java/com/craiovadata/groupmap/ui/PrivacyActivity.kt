package com.craiovadata.groupmap.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.craiovadata.groupmap.R

class PrivacyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}
