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

package com.abhinav.otapulse.core.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton holder for pending OTA update payloads triggered from background notifications
 * or external deep-link intents, ensuring the UI immediately loads and highlights the
 * discovered update card when launched.
 */
object PendingOtaDetailsHolder {
    private val _pendingOtaJson = MutableStateFlow<String?>(null)
    val pendingOtaJson: StateFlow<String?> = _pendingOtaJson.asStateFlow()

    fun setPendingOta(json: String) {
        if (json.isNotBlank()) {
            _pendingOtaJson.value = json
        }
    }

    fun consumePendingOta(): String? {
        val current = _pendingOtaJson.value
        _pendingOtaJson.value = null
        return current
    }
}
