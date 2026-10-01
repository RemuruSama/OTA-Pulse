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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtaUtilsTest {

    @Test
    fun testGetBaseOtaModel() {
        assertEquals("RMX3840", OtaUtils.getBaseOtaModel("RMX3840IN"))
        assertEquals("CPH2487", OtaUtils.getBaseOtaModel("CPH2487EEA"))
        assertEquals("CPH2487", OtaUtils.getBaseOtaModel("CPH2487EU"))
        assertEquals("CPH2487", OtaUtils.getBaseOtaModel("CPH2487NV44"))
        assertEquals("CPH2487", OtaUtils.getBaseOtaModel("CPH2487_11.H.54_3540_202602261724"))
        assertEquals("RMX3840", OtaUtils.getBaseOtaModel("RMX3840.export"))
    }

    @Test
    fun testGetNvSuffix() {
        assertEquals("NV44", OtaUtils.getNvSuffix("NV44"))
        assertEquals("NV01", OtaUtils.getNvSuffix("nv01"))
        assertEquals("", OtaUtils.getNvSuffix("10010111"))
        assertEquals("", OtaUtils.getNvSuffix(""))
    }

    @Test
    fun testConstructOtaString() {
        assertEquals(
            "RMX3840NV44_11.H.01_0001_100001010000",
            OtaUtils.constructOtaString("RMX3840IN", "NV44", "H")
        )
        assertEquals(
            "CPH2487_11.F.01_0001_100001010000",
            OtaUtils.constructOtaString("CPH2487", "", "F")
        )
    }

    @Test
    fun testPrioritizeLetters() {
        val fromH = OtaUtils.prioritizeLetters("H")
        assertEquals("H", fromH[0])
        assertEquals("J", fromH[1])
        assertTrue(fromH.containsAll(listOf("A", "C", "F", "H", "J")))

        val fromA = OtaUtils.prioritizeLetters("A")
        assertEquals(listOf("A", "C", "F", "H", "J"), fromA)
    }

    @Test
    fun testIsServerVersionNewer_MajorUpgrade() {
        // H (ColorOS 15) is newer than F (ColorOS 14)
        assertTrue(
            OtaUtils.isServerVersionNewer(
                serverOtaVersion = "CPH2487_11.H.10_3540_202601011200",
                currentOtaVersion = "CPH2487_11.F.50_3540_202511011200"
            )
        )

        // F is older than H
        assertFalse(
            OtaUtils.isServerVersionNewer(
                serverOtaVersion = "CPH2487_11.F.50_3540_202511011200",
                currentOtaVersion = "CPH2487_11.H.10_3540_202601011200"
            )
        )
    }

    @Test
    fun testIsServerVersionNewer_TimestampComparison() {
        // Same generation H, newer timestamp
        assertTrue(
            OtaUtils.isServerVersionNewer(
                serverOtaVersion = "CPH2487_11.H.54_3540_202602261724",
                currentOtaVersion = "CPH2487_11.H.50_3500_202601151200"
            )
        )

        // Same generation H, older timestamp
        assertFalse(
            OtaUtils.isServerVersionNewer(
                serverOtaVersion = "CPH2487_11.H.50_3500_202601151200",
                currentOtaVersion = "CPH2487_11.H.54_3540_202602261724"
            )
        )
    }

    @Test
    fun testIsServerVersionNewer_BuildNumberComparison() {
        // Same letter, build number comparison when timestamp absent
        assertTrue(
            OtaUtils.isServerVersionNewer(
                serverOtaVersion = "CPH2487_11.H.54",
                currentOtaVersion = "CPH2487_11.H.50"
            )
        )

        assertFalse(
            OtaUtils.isServerVersionNewer(
                serverOtaVersion = "CPH2487_11.H.50",
                currentOtaVersion = "CPH2487_11.H.54"
            )
        )
    }

    @Test
    fun testIsServerVersionNewer_IdenticalOrDummy() {
        // Identical versions
        assertFalse(
            OtaUtils.isServerVersionNewer(
                serverOtaVersion = "CPH2487_11.H.54_3540_202602261724",
                currentOtaVersion = "CPH2487_11.H.54_3540_202602261724"
            )
        )

        // Dummy server version should never be considered newer
        assertFalse(
            OtaUtils.isServerVersionNewer(
                serverOtaVersion = "CPH2487_11.H.01_0001_100001010000",
                currentOtaVersion = "CPH2487_11.H.50_3500_202601151200"
            )
        )
    }

    @Test
    fun testGetServerSearchOrder() {
        val inOrder = OtaUtils.getServerSearchOrder("IN")
        assertEquals(listOf("IN", "GL", "EU", "CN"), inOrder)

        val cnOrder = OtaUtils.getServerSearchOrder("CN")
        assertEquals(listOf("CN", "GL", "EU", "IN"), cnOrder)

        val customNvOrder = OtaUtils.getServerSearchOrder("IN", "10010111")
        assertEquals(listOf("CN", "IN", "GL", "EU"), customNvOrder)
    }
}
