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

package com.abhinav.otapulse.feature.updates.ui

import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.feature.devicecatalog.ui.PartitionSelectDialogData

data class HomeUpdateUiState(
    val deviceModel: String = "",
    val deviceName: String = "",
    val marketName: String = "",
    val nvId: String = "",
    val versionLetter: String = "A",
    val reqMode: String = "manual",
    val osVersion: String = "",
    val displayOtaVersion: String = "",
    val fallbackOtaVersion: String = "",
    val isLoading: Boolean = false,
    val multiResults: List<OtaUpdate>? = null,
    val error: String? = null,
    val selectedOta: OtaUpdate? = null,
    val isFetchingPartitions: Boolean = false,
    val partitionSelectDialog: PartitionSelectDialogData? = null,
    val isStartingExtraction: Boolean = false,
    val userMessage: String? = null
)
