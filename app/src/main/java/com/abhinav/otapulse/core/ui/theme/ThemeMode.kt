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

package com.abhinav.otapulse.core.ui.theme

/**
 * Defines the available theme modes in OTA Pulse.
 *
 * [MATERIAL_YOU] uses Android 12+ dynamic colors (or static fallback palette based on #8B1A1A seed).
 * [HOLOGRAPHIC] uses an alternate frosted glassmorphism aesthetic with iridescent accents.
 */
enum class ThemeMode {
    MATERIAL_YOU,
    HOLOGRAPHIC
}
