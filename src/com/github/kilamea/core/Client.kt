package com.github.kilamea.core

import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Properties

import javax.mail.Authenticator
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Store
import javax.mail.Transport
import javax.mail.internet.MimeMessage
import javax.mail.search.FlagTerm
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

import com.github.kilamea.entity.Account
import com.github.kilamea.entity.FolderType
import com.github.kilamea.entity.Message
import com.github.kilamea.entity.MessageList
import com.github.kilamea.i18n.I18n

class Client {
    @Throws(ReceiveException::class)
    fun receive(account: Account, options: Options): MessageList {
        disableSSLValidation()

        val protocol = account.protocol.clientSpec(account.sslActive)
        var emailStore: Store? = null
        var emailFolder: Folder? = null

        val properties = Properties()
        properties["mail.store.protocol"] = protocol
        properties["mail.$protocol.host"] = account.incomingHost
        properties["mail.$protocol.port"] = account.incomingPort.toString()
        val emailSession = Session.getInstance(properties)

        try {
            emailStore = emailSession.getStore()
            emailStore.connect(account.user, account.password)

            emailFolder = emailStore.getFolder(FolderType.Inbox.toString().uppercase())
            emailFolder.open(Folder.READ_WRITE)

            val unreadMessages = emailFolder.search(FlagTerm(Flags(Flags.Flag.SEEN), false))
            val messages = MessageList()

            for (email in unreadMessages) {
                val newMessage = MailMapper.map(email as MimeMessage)
                messages.add(newMessage)
                if (account.protocol == MailProtocol.IMAP && options.deleteFromServer) {
                    email.setFlag(Flags.Flag.DELETED, true)
                }
            }
            if (account.protocol == MailProtocol.IMAP && options.deleteFromServer) {
                emailFolder.expunge()
            }

            return messages
        } catch (e: Exception) {
            throw ReceiveException(e.message ?: "", e)
        } finally {
            try {
                if (emailFolder != null && emailFolder.isOpen) {
                    emailFolder.close(false)
                }
                if (emailStore != null && emailStore.isConnected) {
                    emailStore.close()
                }
            } catch (e: Exception) {
                throw ReceiveException(e.message ?: "", e)
            }
        }
    }

    @Throws(SendException::class)
    fun send(account: Account, message: Message) {
        disableSSLValidation()

        val properties = Properties()
        properties["mail.smtp.auth"] = "true"
        properties["mail.smtp.starttls.enable"] = "true"
        properties["mail.smtp.host"] = account.outgoingHost
        properties["mail.smtp.port"] = account.outgoingPort.toString()

        val emailSession = Session.getInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(account.user, account.password)
            }
        })

        try {
            val newEmail = MailMapper.map(emailSession, message)
            Transport.send(newEmail)
        } catch (e: Exception) {
            throw SendException(e.message ?: "", e)
        }
    }

    private fun disableSSLValidation() {
        try {
            val sslContext = SSLContext.getInstance("SSL")

            val trustAllCerts: Array<TrustManager> = arrayOf(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate>? {
                    return null
                }

                override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {
                }

                override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {
                }
            })

            sslContext.init(null, trustAllCerts, SecureRandom())
            SSLContext.setDefault(sslContext)
        } catch (e: Exception) {
        }
    }
}
