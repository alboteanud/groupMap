package com.craiovadata.groupmap

import android.util.Log
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.craiovadata.groupmap.utils.JOINED
import com.google.firebase.firestore.FirebaseFirestore

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("com.craiovadata.transportdisplay", appContext.packageName)
    }


    @Test
    fun populateDefaultGroup(){
        val db = FirebaseFirestore.getInstance()
        Log.d("tag", "start test")
        db.collection("test").document().set(hashMapOf(JOINED to true)).addOnCompleteListener {
            Log.d("tag", "finish")
        }
    }



}
