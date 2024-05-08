package com.github.kilamea.core

enum class MimeType(private val value: String) {
    TEXT_PLAIN("text/plain"),
    TEXT_HTML("text/html");

    override fun toString(): String {
        return value
    }
}
