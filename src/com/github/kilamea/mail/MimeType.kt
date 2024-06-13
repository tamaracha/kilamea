package com.github.kilamea.mail

/**
 * Enum representing different MIME types.
 *
 * @since 0.1.0
 * @property value The string value of the MIME type.
 */
enum class MimeType(private val value: String) {
    APPLICATION_OCTET_STREAM("application/octet-stream"),
    TEXT_HTML("text/html"),
    TEXT_PLAIN("text/plain");

    /**
     * Returns the string value of the MIME type.
     *
     * @return The MIME type as a string.
     */
    override fun toString(): String {
        return value
    }
}
