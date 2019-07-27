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

package com.craiovadata.groupmap.repo.rtdb

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.craiovadata.groupmap.config.AppExecutors
import com.craiovadata.groupmap.livedata.rtdb.RealtimeDatabaseQueryLiveData
import com.craiovadata.groupmap.model.StockPrice
import com.craiovadata.groupmap.repo.*
import com.craiovadata.groupmap.repo.rtdb.*
import com.craiovadata.groupmap.repo.rtdb.DatabaseReferenceSyncCallable
import com.craiovadata.groupmap.repo.rtdb.DeserializeDataSnapshotTransform
import com.craiovadata.groupmap.repo.rtdb.DeserializeQuerySnapshotTransform
import com.craiovadata.groupmap.repo.rtdb.StockPriceSnapshotDeserializer
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.firebase.database.FirebaseDatabase
//import com.hyperaware.android.firebasejetpack.config.AppExecutors
//import com.hyperaware.android.firebasejetpack.livedata.rtdb.RealtimeDatabaseQueryLiveData
//import com.hyperaware.android.firebasejetpack.model.StockPrice
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.concurrent.TimeUnit

class RealtimeDatabaseStockRepository : BaseStockRepository(), KoinComponent {

    private val executors by inject<AppExecutors>()
    private val database by inject<FirebaseDatabase>()

    private val stocksLiveRef = database.getReference("stocks-live")
    private val stocksHistoryRef = database.getReference("stocks-history")

    private val stockPriceDeserializer = StockPriceSnapshotDeserializer()

    private val listeningExecutor = MoreExecutors.listeningDecorator(executors.networkExecutorService)

    override fun getStockPriceLiveData(ticker: String): LiveData<StockPriceOrException> {
        val stockRef = stocksLiveRef.child(ticker)
        val liveData = RealtimeDatabaseQueryLiveData(stockRef)
        return Transformations.map(liveData, DeserializeDataSnapshotTransform(stockPriceDeserializer))
    }

    override fun getStockPriceHistoryLiveData(ticker: String): LiveData<StockPriceHistoryQueryResults> {
        val stockHistoryRef = stocksHistoryRef.child(ticker)
        val query = stockHistoryRef.orderByChild("time")
        val liveData = RealtimeDatabaseQueryLiveData(query)
        return Transformations.map(liveData, DeserializeQuerySnapshotTransform(stockPriceDeserializer))
    }

    override fun getStockPricePagedListLiveData(pageSize: Int): LiveData<PagedList<QueryItemOrException<StockPrice>>> {
        val query = stocksLiveRef.orderByKey()
        val dataSourceFactory = RealtimeDatabaseQueryDataSource.Factory(query)
        val deserializedDataSourceFactory = dataSourceFactory.map { snapshot ->
            try {
                val item = StockPriceQueryItem(stockPriceDeserializer.deserialize(snapshot), snapshot.key!!)
                QueryItemOrException(item, null)
            }
            catch (e: Exception) {
                QueryItemOrException<StockPrice>(null, e)
            }
        }

        return LivePagedListBuilder(deserializedDataSourceFactory, pageSize)
            .setFetchExecutor(executors.cpuExecutorService)
            .build()
    }

    override fun syncStockPrice(ticker: String, timeout: Long, unit: TimeUnit): ListenableFuture<StockRepository.SyncResult> {
        val stockRef = stocksLiveRef.child(ticker)
        val callable = DatabaseReferenceSyncCallable(stockRef, timeout, unit)
        return listeningExecutor.submit(callable)
    }

}
