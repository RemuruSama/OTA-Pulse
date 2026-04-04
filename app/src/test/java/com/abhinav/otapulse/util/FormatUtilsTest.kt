package com.abhinav.otapulse.util

import org.junit.Assert.*
import org.junit.Test
import com.abhinav.otapulse.core.common.FormatUtils

/**
 * Unit tests for FormatUtils — verifies size and speed formatting.
 */
class FormatUtilsTest {

    // --- formatSize tests ---

    @Test
    fun `formatSize returns 0 B for zero`() {
        assertEquals("0 B", FormatUtils.formatSize(0))
    }

    @Test
    fun `formatSize returns 0 B for negative`() {
        assertEquals("0 B", FormatUtils.formatSize(-1))
    }

    @Test
    fun `formatSize formats bytes correctly`() {
        assertEquals("500 B", FormatUtils.formatSize(500))
    }

    @Test
    fun `formatSize formats kilobytes correctly`() {
        val result = FormatUtils.formatSize(1024)
        assertEquals("1 KB", result)
    }

    @Test
    fun `formatSize formats megabytes correctly`() {
        val result = FormatUtils.formatSize(1024 * 1024)
        assertEquals("1 MB", result)
    }

    @Test
    fun `formatSize formats gigabytes correctly`() {
        val result = FormatUtils.formatSize(1024L * 1024 * 1024)
        assertEquals("1 GB", result)
    }

    @Test
    fun `formatSize formats large file (4_5 GB) correctly`() {
        val size = (4.5 * 1024 * 1024 * 1024).toLong()
        val result = FormatUtils.formatSize(size)
        assertEquals("4.5 GB", result)
    }

    // --- formatDownloadSpeed tests ---

    @Test
    fun `formatDownloadSpeed returns empty for negative`() {
        assertEquals("", FormatUtils.formatDownloadSpeed(-1))
    }

    @Test
    fun `formatDownloadSpeed formats bytes per second`() {
        val result = FormatUtils.formatDownloadSpeed(500)
        assertEquals("500 B/s", result)
    }

    @Test
    fun `formatDownloadSpeed formats kilobytes per second`() {
        val result = FormatUtils.formatDownloadSpeed(1024)
        assertEquals("1.0 KB/s", result)
    }

    @Test
    fun `formatDownloadSpeed formats megabytes per second`() {
        val result = FormatUtils.formatDownloadSpeed(1024 * 1024)
        assertEquals("1.0 MB/s", result)
    }

    @Test
    fun `formatDownloadSpeed formats fractional MB per second`() {
        val result = FormatUtils.formatDownloadSpeed((5.5 * 1024 * 1024).toLong())
        assertEquals("5.5 MB/s", result)
    }
}
