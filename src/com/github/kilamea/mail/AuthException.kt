package com.github.kilamea.mail

/**
 * Exception thrown when an error occurs while authorizing an email account.
 *
 * @since 0.2.0
 * @property message A message describing the exception.
 * @property cause The cause of this exception.
 */
class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /**
     * Returns a string representation of the exception.
     * 
     * @return A string describing the exception.
     */
    override fun toString(): String {
        var reason = super.toString()
        cause?.let { reason += " ($it)" }
        return reason
    }
}
