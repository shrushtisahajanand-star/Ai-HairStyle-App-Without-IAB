package com.hairstyle.changer.aitryon.utils

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * CryptoJS-compatible AES encrypt/decrypt (OpenSSL "Salted__" format).
 * Password: [PASSWORD]
 */
object AESHelper {

    const val PASSWORD = "dfrtbvcujmnhytgf"

    private const val KEY_SIZE = 256
    private const val IV_SIZE = 128
    private const val HASH_CIPHER = "AES/CBC/PKCS5Padding"
    private const val AES = "AES"
    private const val KDF_DIGEST = "MD5"
    private const val APPEND = "Salted__"

    fun encrypt(plainText: String, password: String = PASSWORD): String {
        val saltBytes = generateSalt(8)
        val key = ByteArray(KEY_SIZE / 8)
        val iv = ByteArray(IV_SIZE / 8)
        evpKdf(password.toByteArray(StandardCharsets.UTF_8), KEY_SIZE, IV_SIZE, saltBytes, key, iv)

        val keyS: SecretKey = SecretKeySpec(key, AES)
        val cipher = Cipher.getInstance(HASH_CIPHER)
        cipher.init(Cipher.ENCRYPT_MODE, keyS, IvParameterSpec(iv))
        val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

        val sBytes = APPEND.toByteArray(StandardCharsets.UTF_8)
        val combined = ByteArray(sBytes.size + saltBytes.size + cipherText.size)
        System.arraycopy(sBytes, 0, combined, 0, sBytes.size)
        System.arraycopy(saltBytes, 0, combined, sBytes.size, saltBytes.size)
        System.arraycopy(cipherText, 0, combined, sBytes.size + saltBytes.size, cipherText.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String, password: String = PASSWORD): String {
        val ctBytes = Base64.decode(cipherText, Base64.DEFAULT)
        val saltBytes = ctBytes.copyOfRange(8, 16)
        val ciphertextBytes = ctBytes.copyOfRange(16, ctBytes.size)
        val key = ByteArray(KEY_SIZE / 8)
        val iv = ByteArray(IV_SIZE / 8)
        evpKdf(password.toByteArray(StandardCharsets.UTF_8), KEY_SIZE, IV_SIZE, saltBytes, key, iv)

        val cipher = Cipher.getInstance(HASH_CIPHER)
        val keyS: SecretKey = SecretKeySpec(key, AES)
        cipher.init(Cipher.DECRYPT_MODE, keyS, IvParameterSpec(iv))
        val plainText = cipher.doFinal(ciphertextBytes)
        return String(plainText, StandardCharsets.UTF_8)
    }

    private fun evpKdf(
        password: ByteArray,
        keySizeBits: Int,
        ivSizeBits: Int,
        salt: ByteArray,
        resultKey: ByteArray,
        resultIv: ByteArray
    ): ByteArray {
        var keySize = keySizeBits / 32
        var ivSize = ivSizeBits / 32
        val targetKeySize = keySize + ivSize
        val derivedBytes = ByteArray(targetKeySize * 4)
        var numberOfDerivedWords = 0
        var block: ByteArray? = null
        val hasher = MessageDigest.getInstance(KDF_DIGEST)

        while (numberOfDerivedWords < targetKeySize) {
            if (block != null) {
                hasher.update(block)
            }
            hasher.update(password)
            block = hasher.digest(salt)
            hasher.reset()

            System.arraycopy(
                block,
                0,
                derivedBytes,
                numberOfDerivedWords * 4,
                minOf(block.size, (targetKeySize - numberOfDerivedWords) * 4)
            )
            numberOfDerivedWords += block.size / 4
        }

        System.arraycopy(derivedBytes, 0, resultKey, 0, keySize * 4)
        System.arraycopy(derivedBytes, keySize * 4, resultIv, 0, ivSize * 4)
        return derivedBytes
    }

    private fun generateSalt(length: Int): ByteArray {
        return ByteArray(length).also { SecureRandom().nextBytes(it) }
    }
}
