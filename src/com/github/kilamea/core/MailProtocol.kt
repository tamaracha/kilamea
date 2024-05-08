package com.github.kilamea.core

enum class MailProtocol {
    IMAP, POP3, SMTP;

    fun clientSpec(sslActive: Boolean): String {
        var protocol = this.toString().lowercase()
        if (sslActive) {
            protocol += "s"
        }
        return protocol
    }

    fun port(sslActive: Boolean): Int {
        return when (this) {
            IMAP -> if (sslActive) 993 else 143
            POP3 -> if (sslActive) 995 else 110
            SMTP -> if (sslActive) 587 else 25
        }
    }
}
