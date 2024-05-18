package com.github.kilamea.core

/**
 * Enum representing different MIME types.
 *
 * @since 0.1.0
 * @property value The string value of the MIME type.
 */
enum class MimeType(private val value: String) {
    TEXT_PLAIN("text/plain"),
    TEXT_HTML("text/html");

    /**
     * Returns the string value of the MIME type.
     *
     * @return The MIME type as a string.
     */
    override fun toString(): String {
        return value
    }
}
