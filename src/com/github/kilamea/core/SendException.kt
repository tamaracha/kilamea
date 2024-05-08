package com.github.kilamea.core

class SendException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    override fun toString(): String {
        var reason = super.toString()
        cause?.let { reason += " ($it)" }
        return reason
    }
}
