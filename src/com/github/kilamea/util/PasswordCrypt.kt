package com.github.kilamea.util

import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import java.security.AlgorithmParameters
import java.security.GeneralSecurityException
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.util.Base64

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object PasswordCrypt {
    private const val password = "secret"
    private val salt = "12345678".toByteArray()
    private const val iterationCount = 40000
    private const val keyLength = 128

    @Throws(GeneralSecurityException::class, IOException::class)
    fun decrypt(text: String): String {
        if (text.isEmpty()) {
            return ""
        }

        val iv = text.split(":")[0]
        val cryptoText = text.split(":")[1]
        val key = createSecretKey(password.toCharArray(), salt, iterationCount, keyLength)
        val pbeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        pbeCipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(base64Decode(iv)))

        return String(pbeCipher.doFinal(base64Decode(cryptoText)), StandardCharsets.UTF_8)
    }

    @Throws(GeneralSecurityException::class, UnsupportedEncodingException::class)
    fun encrypt(text: String): String {
        if (text.isEmpty()) {
            return ""
        }

        val key = createSecretKey(password.toCharArray(), salt, iterationCount, keyLength)
        val pbeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        pbeCipher.init(Cipher.ENCRYPT_MODE, key)
        val parameters = pbeCipher.parameters
        val ivParameterSpec = parameters.getParameterSpec(IvParameterSpec::class.java)
        val cryptoText = pbeCipher.doFinal(text.toByteArray(StandardCharsets.UTF_8))
        val iv = ivParameterSpec.iv

        return base64Encode(iv) + ":" + base64Encode(cryptoText)
    }

    @Throws(IOException::class)
    private fun base64Decode(string: String): ByteArray {
        return Base64.getDecoder().decode(string)
    }

    private fun base64Encode(bytes: ByteArray): String {
        return Base64.getEncoder().encodeToString(bytes)
    }

    @Throws(InvalidKeySpecException::class, NoSuchAlgorithmException::class)
    private fun createSecretKey(password: CharArray, salt: ByteArray, iterationCount: Int, keyLength: Int): SecretKeySpec {
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        val keySpec = PBEKeySpec(password, salt, iterationCount, keyLength)
        val keyTmp = keyFactory.generateSecret(keySpec)
        return SecretKeySpec(keyTmp.encoded, "AES")
    }
}
