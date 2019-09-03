package com.craiovadata.groupmap.utils_

import com.craiovadata.groupmap.model.Group
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.functions.FirebaseFunctions
import java.util.*
import java.util.concurrent.TimeUnit

object DummyUsersUtils {

    fun populateDefaultGroup() {
        val db = FirebaseFirestore.getInstance()
        val refGroup = db.document("$GROUPS/$DEFAULT_GROUP")
        val batch = db.batch()
        batch.set(refGroup, Group( "Bike club NewYork", "_"))
        val persons = getDummyUsers_()
        persons.forEachIndexed { index, person ->
            val ref = refGroup.collection(USERS).document(index.toString())
            batch.set(ref, person)
        }
        batch.commit()
    }

    fun getDummyUsers_(): MutableList<Map<String, Any>> {
        val r = Random()
        val time = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365 * 15) // after 15 years
        val myArray = mutableListOf<Map<String, Any>>()

        dummyUsers.forEach {
            val lat = 40.65 + r.nextFloat() / 8
            val lon = -73.96 + r.nextFloat() / 4
            val mapPerson = mapOf(
                NAME to it.key, PHOTO_URL to it.value,
                LOCATION to GeoPoint(lat, lon), TIMESTAMP to Date(time),
                VISIBILITY to "public"
            )
            myArray.add(mapPerson)
        }
        return myArray
    }

    private val dummyUsers = mapOf(
        "Dan" to "https://lh3.googleusercontent.com/-JwqhJ989hXw/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3rdvLnxofP2V96sjhxQ0lXf2HrRgKg.CMID/s64-c-mo/photo.jpg",
        "Anca" to "https://lh3.googleusercontent.com/-dFfcBHiIoTk/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3rd8xLcopVHADHFqH_8wDBHo5nd6IQ/s64-c-mo/photo.jpg",
        "Mihaela" to "https://lh3.googleusercontent.com/-o3phkZogqYY/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3reUul-g6ccRH73tgTbvAVUVAU5igQ.CMID/s64-c-mo/photo.jpg",
        "Victoria" to "https://lh3.googleusercontent.com/-aKPXx00bFf4/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3rfH3oBxY63PQ_FrT48c9yX2wf-U9A.CMID/s64-c-mo/photo.jpg",
        "Android" to "https://lh3.googleusercontent.com/-r_wtGPpwhGo/AAAAAAAAAAI/AAAAAAAAAAA/ACHi3reJ6DA346tlC_Kv3OLbE_qdmt9r6Q.CMID/s64-c-mo/photo.jpg",
        "Ionel" to "https://lh3.googleusercontent.com/-FzX2I30Hhkw/AAAAAAAAAAI/AAAAAAAAFHY/ACHi3rc8vTf6ZzuNErb0cr5Ir9fem8AuvA.CMID/s64-c-mo/photo.jpg",
        "Ivona" to "https://firebasestorage.googleapis.com/v0/b/groupmap-6b346.appspot.com/o/faces%2Fface_1.jpg?alt=media&token=0cf56449-f626-45f4-9932-4aad4a48ab55",
        "Helen" to "https://firebasestorage.googleapis.com/v0/b/groupmap-6b346.appspot.com/o/faces%2Fface_2.jpg?alt=media&token=136beae6-4de7-4a4b-a79e-d3e2e24072b2",
        "Michael" to "https://firebasestorage.googleapis.com/v0/b/groupmap-6b346.appspot.com/o/faces%2Fface_3.jpg?alt=media&token=426a4174-5235-4a9a-a293-c540648219f0",
        "Edwin" to "https://firebasestorage.googleapis.com/v0/b/groupmap-6b346.appspot.com/o/faces%2Fface_4.jpg?alt=media&token=61a77fd9-61b6-4590-978b-81d7a464c17c",
        "Alice" to "https://firebasestorage.googleapis.com/v0/b/groupmap-6b346.appspot.com/o/faces%2Fface_5.jpg?alt=media&token=55854484-39e3-4655-986c-af672a8bc88f",
        "Kevin" to "https://firebasestorage.googleapis.com/v0/b/groupmap-6b346.appspot.com/o/faces%2Fface_6.jpg?alt=media&token=3ec22af7-f665-4ce5-be62-0f0c4f8ecbf2",
        "Julia" to "https://firebasestorage.googleapis.com/v0/b/groupmap-6b346.appspot.com/o/faces%2Fface_7.jpg?alt=media&token=36f171de-bccd-4c69-b557-7c43c86a860a"
    )

    // !!! delete all database
    fun deleteDB(text: String): Task<String>? {
        // Create the arguments to the callable function.
        val data = mapOf(
            "text" to text,
            "push" to true
        )
        val functions = FirebaseFunctions.getInstance()
        return functions
            .getHttpsCallable("deleteDB")
            .call(data)
            .continueWith { task ->
                // This continuation runs on either success or failure, but if the task
                // has failed then result will throw an Exception which will be
                // propagated down.
                val result = task.result?.data as? String
                result
            }
    }

}