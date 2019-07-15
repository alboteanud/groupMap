package com.craiovadata.groupmap.ui

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

private const val TITLE_TAG = "settingsActivityTitle"

class SettingsActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.craiovadata.groupmap.R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(com.craiovadata.groupmap.R.id.settings, HeaderFragment())
                .commit()
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                setTitle(com.craiovadata.groupmap.R.string.title_activity_settings)
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, title)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment
//            , args
        ).apply {
            arguments = args
            setTargetFragment(caller, 0)
        }
        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
            .replace(com.craiovadata.groupmap.R.id.settings, fragment)
            .addToBackStack(null)
            .commit()
        title = pref.title
        return true
    }

    class HeaderFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(com.craiovadata.groupmap.R.xml.header_preferences, rootKey)
        }
    }

    class AccountFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key.equals("some_key")) {
                // Set summary to be the user-description for the selected value
//                val exercisesPref = findPreference<Preference>(key)
//                exercisesPref!!.summary = sharedPreferences.getString(key, "")
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(com.craiovadata.groupmap.R.xml.account_preferences, rootKey)
        }

        override fun onResume() {
            super.onResume()
            // Set up a listener whenever a key changes
            preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            // Set up a listener whenever a key changes
            preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }


    }

    class InviteFragment : PreferenceFragmentCompat(){
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(com.craiovadata.groupmap.R.xml.invite_preferences, rootKey)
        }
    }
}
