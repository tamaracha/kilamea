package com.github.kilamea.mail

/**
 * Enum representing different email protocols.
 *
 * @since 0.1.0
 */
enum class MailProtocol {
    IMAP, POP3, SMTP;

    /**
     * Returns the protocol string to be used by the client, potentially with SSL.
     *
     * @param sslActive Indicates if SSL is active.
     * @return The protocol string.
     */
    fun clientSpec(sslActive: Boolean): String {
        var protocol = this.toString().lowercase()
        if (sslActive) {
            protocol += "s"
        }
        return protocol
    }

    /**
     * Returns the default port for the protocol, depending on whether SSL is active.
     *
     * @param sslActive Indicates if SSL is active.
     * @return The port number.
     */
    fun port(sslActive: Boolean): Int {
        return when (this) {
            IMAP -> if (sslActive) 993 else 143
            POP3 -> if (sslActive) 995 else 110
            SMTP -> if (sslActive) 587 else 25
        }
    }
}
