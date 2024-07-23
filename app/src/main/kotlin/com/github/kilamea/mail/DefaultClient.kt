package com.github.kilamea.mail

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

import com.github.kilamea.core.Options
import com.github.kilamea.database.DatabaseManager
import com.github.kilamea.entity.Account
import com.github.kilamea.entity.FolderType
import com.github.kilamea.entity.Message
import com.github.kilamea.entity.MessageList
import com.github.kilamea.i18n.I18n

/**
 * Class responsible for handling email client operations such as receiving and sending emails.
 *
 * @since 0.1.0
 * @property account The account from which to receive messages or to send an email.
 * @property database The database manager for handling database operations.
 * @property options Additional options for message exchange.
 */
open class DefaultClient(
    protected val account: Account,
    protected val database: DatabaseManager,
    protected val options: Options
) {
    /**
     * Retrieves a list of unread messages from the specified email account.
     *
     * @return A list of unread messages.
     * @throws ReceiveException If an error occurs during message retrieval.
     */
    @Throws(ReceiveException::class)
    open fun receive(): MessageList {
        disableSSLValidation()

        val protocol = account.protocol.clientSpec(account.sslActive)
        var store: Store? = null
        var folder: Folder? = null

        val properties = Properties()
        properties["mail.store.protocol"] = protocol
        properties["mail.$protocol.host"] = account.incomingHost
        properties["mail.$protocol.port"] = account.incomingPort.toString()
        val session = Session.getInstance(properties)

        try {
            store = session.getStore()
            store.connect(account.user, account.password)

            folder = store.getFolder(FolderType.Inbox.toString().uppercase())
            folder.open(Folder.READ_WRITE)

            val unreadMessages = folder.search(FlagTerm(Flags(Flags.Flag.SEEN), false))
            val messages = MessageList()

            for (email in unreadMessages) {
                val newMessage = MailMapper.mapMimeMessageToMessage(email as MimeMessage)
                messages.add(newMessage)
                if (account.protocol == MailProtocol.IMAP && options.deleteFromServer) {
                    email.setFlag(Flags.Flag.DELETED, true)
                }
            }
            if (account.protocol == MailProtocol.IMAP && options.deleteFromServer) {
                folder.expunge()
            }

            return messages
        } catch (e: Exception) {
            throw ReceiveException(e.message ?: "", e)
        } finally {
            try {
                if (folder != null && folder.isOpen) {
                    folder.close(false)
                }
                if (store != null && store.isConnected) {
                    store.close()
                }
            } catch (e: Exception) {
                throw ReceiveException(e.message ?: "", e)
            }
        }
    }

    /**
     * Sends an email using the specified account and message.
     *
     * @param message The message to be sent.
     * @throws SendException If an error occurs during email sending.
     */
    @Throws(SendException::class)
    open fun send(message: Message) {
        disableSSLValidation()

        val properties = Properties()
        properties["mail.smtp.auth"] = "true"
        properties["mail.smtp.starttls.enable"] = "true"
        properties["mail.smtp.host"] = account.outgoingHost
        properties["mail.smtp.port"] = account.outgoingPort.toString()

        val session = Session.getInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(account.user, account.password)
            }
        })

        try {
            val newEmail = MailMapper.mapMessageToMimeMessage(message, session)
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
