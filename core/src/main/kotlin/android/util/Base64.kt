package android.util

import java.util.Base64 as JavaBase64

object Base64 {
    const val DEFAULT = 0
    const val NO_PADDING = 1
    const val NO_WRAP = 2
    const val CRLF = 4
    const val URL_SAFE = 8

    fun decode(str: String?, flags: Int = DEFAULT): ByteArray {
        if (str == null) return ByteArray(0)
        val clean = str.trim().replace("\n", "").replace("\r", "")
        return try {
            if ((flags and URL_SAFE) != 0) {
                JavaBase64.getUrlDecoder().decode(clean)
            } else {
                JavaBase64.getDecoder().decode(clean)
            }
        } catch (_: Exception) {
            try {
                JavaBase64.getMimeDecoder().decode(clean)
            } catch (_: Exception) {
                ByteArray(0)
            }
        }
    }

    fun decode(input: ByteArray, flags: Int = DEFAULT): ByteArray {
        return decode(String(input), flags)
    }

    fun encode(input: ByteArray, flags: Int = DEFAULT): ByteArray {
        val encoder = if ((flags and URL_SAFE) != 0) JavaBase64.getUrlEncoder() else JavaBase64.getEncoder()
        val finalEncoder = if ((flags and NO_PADDING) != 0) encoder.withoutPadding() else encoder
        return finalEncoder.encode(input)
    }

    fun encodeToString(input: ByteArray, flags: Int = DEFAULT): String {
        val encoder = if ((flags and URL_SAFE) != 0) JavaBase64.getUrlEncoder() else JavaBase64.getEncoder()
        val finalEncoder = if ((flags and NO_PADDING) != 0) encoder.withoutPadding() else encoder
        val encoded = finalEncoder.encodeToString(input)
        return if ((flags and NO_WRAP) != 0) encoded.replace("\n", "").replace("\r", "") else encoded
    }
}
