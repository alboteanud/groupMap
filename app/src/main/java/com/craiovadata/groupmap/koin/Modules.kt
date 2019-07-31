/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.craiovadata.groupmap.koin

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.craiovadata.groupmap.config.AppExecutors
import com.craiovadata.groupmap.repo.Repository
import com.craiovadata.groupmap.repo.firestore.FirestoreRepository
import org.koin.dsl.module.Module
import org.koin.dsl.module.module

interface RuntimeConfig {
    var repository: Repository
}

class SingletonRuntimeConfig : RuntimeConfig {
    companion object {
        val instance = SingletonRuntimeConfig()
    }

    override var repository: Repository = firestoreStockRepository
}

private val firestoreStockRepository by lazy { FirestoreRepository() }
//private val realtimeDatabaseStockRepository by lazy { RealtimeDatabaseGroupMapRepository() }

val appModule: Module = module {
    single { firestoreStockRepository }
//    single { realtimeDatabaseStockRepository }
    single { SingletonRuntimeConfig.instance as RuntimeConfig }
    factory { get<RuntimeConfig>().repository }
    single { AppExecutors.instance }
}

val firebaseModule: Module = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
//    single {
//        val instance = FirebaseDatabase.getInstance()
//        instance.setPersistenceEnabled(false)
//        instance
//    }
}

val allModules = listOf(appModule, firebaseModule)
