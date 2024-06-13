package com.github.kilamea.mail

import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.security.GeneralSecurityException
import java.util.Properties

import javax.mail.Session

import com.github.kilamea.core.Constants
import com.github.kilamea.core.Options
import com.github.kilamea.database.DBRuntimeException
import com.github.kilamea.database.DatabaseManager
import com.github.kilamea.entity.Account
import com.github.kilamea.entity.Message
import com.github.kilamea.entity.MessageList
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.MemoryDataStoreFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes

/**
 * Gmail client for interacting with the Google API.
 *
 * @since 0.2.0
 * @param account The account from which to receive messages or to send an email.
 * @param database The database manager for handling database operations.
 * @param options Additional options for message exchange.
 */
class GmailClient(
    account: Account,
    database: DatabaseManager,
    options: Options
) : DefaultClient(account, database, options) {
    private var httpTransport: HttpTransport? = null
    private var jsonFactory: JsonFactory? = null

    /**
     * Authorizes the Gmail client.
     *
     * @throws AuthException If authorization fails.
     */
    @Throws(AuthException::class)
    fun authorize() {
        val scopes = listOf(GmailScopes.MAIL_GOOGLE_COM)
        val memoryDataStoreFactory = MemoryDataStoreFactory()

        try {
            initTransport()
            val clientSecrets = loadClientSecrets()
            val flow = GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, clientSecrets, scopes)
                .setDataStoreFactory(memoryDataStoreFactory)
                .setAccessType("offline")
                .build()
            val receiver = LocalServerReceiver.Builder().setPort(8888).build()

            val credential = AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
            credential?.let {
                val tokens = "${it.accessToken} ${it.refreshToken}"
                account.tokens = tokens
                database.updateAccountTokens(account.id, account.tokens)
            }
        } catch (e: Exception) {
            throw AuthException(e.message ?: "", e)
        }
    }

    /**
     * Retrieves a list of unread messages from the specified Gmail account.
     *
     * @return A list of unread messages.
     * @throws ReceiveException If an error occurs during message retrieval.
     */
    @Throws(ReceiveException::class)
    override fun receive(): MessageList {
        val properties = Properties()
        val session = Session.getInstance(properties)

        try {
            val gmail = getGmailService()

            val unreadMessages = gmail.users().messages().list("me")
                .setQ("is:unread").execute().messages
            val messages = MessageList()

            unreadMessages?.let {
                for (email in it) {
                    val rawEmail = gmail.users().messages().get("me", email.id).setFormat("raw").execute()
                    val newMessage = MailMapper.mapGmailMessageToMessage(rawEmail, session)
                    messages.add(newMessage)
                    if (options.deleteFromServer) {
                        gmail.users().messages().delete("me", email.id).execute()
                    }
                }
            }

            updateTokens(gmail)
            return messages
        } catch (e: Exception) {
            throw ReceiveException(e.message ?: "", e)
        }
    }

    /**
     * Sends an email using the specified account and message.
     *
     * @param message The message to be sent.
     * @throws SendException If an error occurs during email sending.
     */
    @Throws(SendException::class)
    override fun send(message: Message) {
        val properties = Properties()
        val session = Session.getInstance(properties)

        try {
            val gmail = getGmailService()
            val newEmail = MailMapper.mapMessageToGmailMessage(message, session)
            gmail.users().messages().send("me", newEmail).execute()
            updateTokens(gmail)
        } catch (e: Exception) {
            throw SendException(e.message ?: "", e)
        }
    }

    /**
     * Retrieves the Gmail service using the stored account tokens.
     *
     * @return The Gmail service.
     * @throws GeneralSecurityException If there is a security error.
     * @throws IOException If there is an I/O error.
     */
    @Throws(GeneralSecurityException::class, IOException::class)
    private fun getGmailService(): Gmail {
        initTransport()
        val clientSecrets = loadClientSecrets()

        val tokenPair = account.tokens.split(" ")
        val accessToken = tokenPair[0]
        val refreshToken = tokenPair[1]
        val credential = GoogleCredential.Builder()
            .setTransport(httpTransport)
            .setJsonFactory(jsonFactory)
            .setClientSecrets(clientSecrets.details.clientId, clientSecrets.details.clientSecret)
            .build()
            .setAccessToken(accessToken)
            .setRefreshToken(refreshToken)

        val gmail = Gmail.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName(Constants.APP_NAME)
            .build()
        return gmail
    }

    /**
     * Initializes the HTTP transport and JSON factory.
     *
     * @throws GeneralSecurityException If there is a security error.
     * @throws IOException If there is an I/O error.
     */
    @Throws(GeneralSecurityException::class, IOException::class)
    private fun initTransport() {
        if (httpTransport == null) {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        }

        if (jsonFactory == null) {
            jsonFactory = JacksonFactory.getDefaultInstance()
        }
    }

    /**
     * Loads the Google client secrets from the specified resource file.
     *
     * @return The loaded Google client secrets.
     * @throws IOException If an error occurs while loading the resource.
     * @throws FileNotFoundException If the resource file is not found.
     */
    @Throws(IOException::class)
    private fun loadClientSecrets(): GoogleClientSecrets {
        var clientSecrets = GoogleClientSecrets()

        val inputStream = javaClass.getResourceAsStream(Constants.GOOGLE_CREDENTIALS_FILE)
        if (inputStream != null) {
            clientSecrets = GoogleClientSecrets.load(jsonFactory, InputStreamReader(inputStream))
        } else {
            throw FileNotFoundException("Resource not found: ${Constants.GOOGLE_CREDENTIALS_FILE}")
        }

        return clientSecrets
    }

    /**
     * Updates the tokens in the account and database if they have changed.
     *
     * @param gmail The Gmail service.
     * @throws DBRuntimeException If an error occurs while updating the account.
     */
    @Throws(DBRuntimeException::class)
    private fun updateTokens(gmail: Gmail) {
        val credential = gmail.requestFactory.initializer as GoogleCredential
        credential?.let {
            val tokens = "${it.accessToken} ${it.refreshToken}"
            if (tokens != account.tokens) {
                account.tokens = tokens
                database.updateAccountTokens(account.id, account.tokens)
            }
        }
    }
}
