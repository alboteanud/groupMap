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

package com.craiovadata.groupmap.repo.firestore

import com.craiovadata.groupmap.model.StockPrice
import com.google.firebase.firestore.DocumentSnapshot
import com.craiovadata.groupmap.repo.Deserializer

internal interface DocumentSnapshotDeserializer<T> : Deserializer<DocumentSnapshot, T>

internal class StockPriceDocumentSnapshotDeserializer : DocumentSnapshotDeserializer<StockPrice> {
    override fun deserialize(input: DocumentSnapshot): StockPrice {
        val ticker = input.id
        val price = input.getDouble("price") ?:
            throw Deserializer.DeserializerException("price was missing for stock price document $ticker")
        val time = input.getDate("time") ?:
            throw Deserializer.DeserializerException("time was missing for stock price document $ticker")

        return StockPrice(ticker, price.toFloat(), time)
    }
}
