package com.abhinav.otapulse.core.common

import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utility object for handling cryptographic operations required by the OTA server.
 */
object Crypto {
    private val keys = listOf(
        "oppo1997", "baed2017", "java7865", "231uiedn", "09e32ji6",
        "0oiu3jdy", "0pej387l", "2dkliuyt", "20odiuye", "87j3id7w"
    )

    private fun getKey(pseudoKey: String): ByteArray {
        val index = pseudoKey[0].digitToInt()
        val combinedKey = keys[index] + pseudoKey.substring(4, 12)
        return combinedKey.toByteArray(Charsets.UTF_8)
    }

    private fun generateRandomAesKey(): ByteArray {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey().encoded
    }

    private fun generateIv(): ByteArray {
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        return iv
    }

    private fun encryptAesCtr(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        val ivSpec = IvParameterSpec(iv)
        val secretKeySpec = SecretKeySpec(key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)
        return cipher.doFinal(data)
    }

    private fun decryptAesCtr(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        val ivSpec = IvParameterSpec(iv)
        val secretKeySpec = SecretKeySpec(key, "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec)
        return cipher.doFinal(data)
    }

    private fun encryptAesEcb(data: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKeySpec = SecretKeySpec(key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
        return cipher.doFinal(data)
    }

    private fun decryptAesEcb(data: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKeySpec = SecretKeySpec(key, "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
        return cipher.doFinal(data)
    }

    fun encryptCtrV1(data: String): String {
        val pseudoKey = (0..9).random().toString() + (1..14).map { ('0'..'9').random() }.joinToString("")
        val key = getKey(pseudoKey)
        val md = MessageDigest.getInstance("MD5")
        val iv = md.digest(key)
        val encrypted = encryptAesCtr(data.toByteArray(Charsets.UTF_8), key, iv)
        return Base64.getEncoder().encodeToString(encrypted) + pseudoKey
    }

    fun decryptCtrV1(data: String): String {
        val encryptedData = Base64.getDecoder().decode(data.substring(0, data.length - 15))
        val pseudoKey = data.takeLast(15)
        val key = getKey(pseudoKey)
        val md = MessageDigest.getInstance("MD5")
        val iv = md.digest(key)
        val decrypted = decryptAesCtr(encryptedData, key, iv)
        return String(decrypted, Charsets.UTF_8)
    }

    fun encryptCtrV2(data: String): Triple<String, String, String> {
        val key = generateRandomAesKey()
        val iv = generateIv()
        val encrypted = encryptAesCtr(data.toByteArray(Charsets.UTF_8), key, iv)
        return Triple(
            Base64.getEncoder().encodeToString(encrypted),
            Base64.getEncoder().encodeToString(key),
            Base64.getEncoder().encodeToString(iv)
        )
    }

    fun decryptCtrV2(data: String, key: String, iv: String): String {
        val decodedData = Base64.getDecoder().decode(data)
        val decodedKey = Base64.getDecoder().decode(key)
        val decodedIv = Base64.getDecoder().decode(iv)
        val decrypted = decryptAesCtr(decodedData, decodedKey, decodedIv)
        return String(decrypted, Charsets.UTF_8)
    }

    fun encryptEcb(data: String): String {
        val pseudoKey = (0..9).random().toString() + (1..14).map { ('a'..'z').random() }.joinToString("")
        val key = getKey(pseudoKey)
        val encrypted = encryptAesEcb(data.toByteArray(Charsets.UTF_8), key)
        return Base64.getEncoder().encodeToString(encrypted) + pseudoKey
    }

    fun decryptEcb(data: String): String {
        val encryptedData = Base64.getDecoder().decode(data.substring(0, data.length - 15))
        val pseudoKey = data.takeLast(15)
        val key = getKey(pseudoKey)
        val plain = decryptAesEcb(encryptedData, key)
        return String(plain, Charsets.UTF_8)
    }

    fun generateProtectedKey(key: String, pubKey: String): String {
        val decodedPubKey = Base64.getDecoder().decode(pubKey)
        val encrypted = encryptRsa(key.toByteArray(Charsets.UTF_8), decodedPubKey)
        return Base64.getEncoder().encodeToString(encrypted)
    }

    private fun encryptRsa(data: ByteArray, pubKeyBytes: ByteArray): ByteArray {
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKeySpec = X509EncodedKeySpec(pubKeyBytes)
        val publicKey: PublicKey = keyFactory.generatePublic(publicKeySpec)
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(data)
    }

    fun sha256(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
