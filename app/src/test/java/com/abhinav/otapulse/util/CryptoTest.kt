package com.abhinav.otapulse.util

import org.junit.Assert.*
import org.junit.Test
import com.abhinav.otapulse.core.common.Crypto

/**
 * Unit tests for Crypto utility — verifies encrypt/decrypt roundtrips for all 3 protocols.
 */
class CryptoTest {

    @Test
    fun `ECB encrypt-decrypt roundtrip preserves data`() {
        val originalData = """{"model":"RMX3840","otaVersion":"test_v1.0"}"""
        val encrypted = Crypto.encryptEcb(originalData)
        val decrypted = Crypto.decryptEcb(encrypted)
        assertEquals(originalData, decrypted)
    }

    @Test
    fun `CTR v1 encrypt-decrypt roundtrip preserves data`() {
        val originalData = """{"model":"CPH2585","region":0,"imei":"123456789012345"}"""
        val encrypted = Crypto.encryptCtrV1(originalData)
        val decrypted = Crypto.decryptCtrV1(encrypted)
        assertEquals(originalData, decrypted)
    }

    @Test
    fun `CTR v2 encrypt-decrypt roundtrip preserves data`() {
        val originalData = """{"model":"PHB110","version":"3","otaVersion":"RMX3840_14.0.0.800(EX01)"}"""
        val (cipher, key, iv) = Crypto.encryptCtrV2(originalData)
        val decrypted = Crypto.decryptCtrV2(cipher, key, iv)
        assertEquals(originalData, decrypted)
    }

    @Test
    fun `ECB handles empty string`() {
        val encrypted = Crypto.encryptEcb("")
        val decrypted = Crypto.decryptEcb(encrypted)
        assertEquals("", decrypted)
    }

    @Test
    fun `CTR v1 handles unicode characters`() {
        val originalData = """{"language":"zh-CN","desc":"系统更新"}"""
        val encrypted = Crypto.encryptCtrV1(originalData)
        val decrypted = Crypto.decryptCtrV1(encrypted)
        assertEquals(originalData, decrypted)
    }

    @Test
    fun `CTR v2 produces different ciphertexts for same plaintext`() {
        val data = "test data"
        val (cipher1, _, _) = Crypto.encryptCtrV2(data)
        val (cipher2, _, _) = Crypto.encryptCtrV2(data)
        // Different random keys/IVs each time should produce different ciphertext
        assertNotEquals(cipher1, cipher2)
    }

    @Test
    fun `SHA-256 produces correct hash`() {
        val hash = Crypto.sha256("test")
        assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08", hash)
    }

    @Test
    fun `SHA-256 produces different hashes for different inputs`() {
        val hash1 = Crypto.sha256("input1")
        val hash2 = Crypto.sha256("input2")
        assertNotEquals(hash1, hash2)
    }
}
