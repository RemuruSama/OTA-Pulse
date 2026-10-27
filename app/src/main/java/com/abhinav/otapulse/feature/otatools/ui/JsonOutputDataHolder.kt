/*
 * Copyright (C) 2026 OTA Pulse
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.abhinav.otapulse.feature.otatools.ui

import com.abhinav.otapulse.core.model.OtaUpdate

/**
 * In-memory holder for [OtaUpdate] passed to [JsonOutputActivity] to avoid
 * [android.os.TransactionTooLargeException] when passing large update payloads
 * with raw JSON strings through Binder IPC.
 */
object JsonOutputDataHolder {
    @Volatile
    private var data: OtaUpdate? = null

    fun set(update: OtaUpdate?) {
        data = update
    }

    fun get(): OtaUpdate? = data

    fun clear() {
        data = null
    }
}
