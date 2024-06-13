package com.github.kilamea.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.Date
import java.util.logging.Logger

import com.github.kilamea.core.Bag
import com.github.kilamea.core.Constants
import com.github.kilamea.core.Options
import com.github.kilamea.entity.Account
import com.github.kilamea.entity.Attachment
import com.github.kilamea.entity.Contact
import com.github.kilamea.entity.Folder
import com.github.kilamea.entity.FolderType
import com.github.kilamea.entity.Message
import com.github.kilamea.i18n.I18n
import com.github.kilamea.mail.MailProtocol
import com.github.kilamea.util.PasswordCrypt
import com.google.gson.Gson

import liquibase.command.CommandScope
import liquibase.command.core.UpdateCommandStep
import liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep
import liquibase.database.Database
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection

/**
 * Manages database connections and operations for email accounts, folders, messages, attachments, and contacts.
 *
 * @since 0.1.0
 * @property connection The SQL connection to the database.
 */
class DatabaseManager {
    private val logger = Logger.getLogger(DatabaseManager::class.java.name)
    private var connection: Connection? = null

    private val SQL_SELECT_ACCOUNTS =
        "SELECT " +
            "a.id AS accountId, a.email AS email, a.display_name AS displayName, a.user AS user, a.password AS password, a.tokens AS tokens, " +
            "a.protocol AS mailProtocol, a.ssl_active AS sslActive, a.incoming_host AS incomingHost, a.incoming_port AS incomingPort, " +
            "a.outgoing_host AS outgoingHost, a.outgoing_port AS outgoingPort, " +
            "f.id AS folderId, f.name AS folderName, f.type AS folderType, " +
            "m.id AS messageId, m.email_reference AS emailReference, m.from_addresses AS fromAddresses, m.recipients AS recipients, " +
            "m.cc_addresses AS ccAddresses, m.bcc_addresses AS bccAddresses, m.sent_date AS sentDate, m.received_date AS receivedDate, " +
            "m.subject AS subject, m.content AS content, m.raw_data AS rawData, m.unread AS unread, " +
            "t.id AS attachmentId, t.file_name AS fileName, t.content AS fileContent " +
            "FROM accounts a INNER JOIN folders f ON a.id = f.account " +
            "LEFT JOIN messages m ON f.id = m.folder LEFT JOIN attachments t ON m.id = t.message"
    private val SQL_INSERT_ACCOUNT =
        "INSERT INTO accounts (id, email, display_name, user, password, tokens, protocol, ssl_active, incoming_host, incoming_port, outgoing_host, outgoing_port) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    private val SQL_UPDATE_ACCOUNT =
        "UPDATE accounts SET email = ?, display_name = ?, user = ?, password = ?, tokens = ?, protocol = ?, ssl_active = ?, incoming_host = ?, incoming_port = ?, outgoing_host = ?, outgoing_port = ? WHERE id = ?"
    private val SQL_UPDATE_ACCOUNT_TOKENS =
        "UPDATE accounts SET tokens = ? WHERE id = ?"
    private val SQL_DELETE_ACCOUNT =
        "DELETE FROM accounts WHERE id = ?"

    private val SQL_INSERT_FOLDER =
        "INSERT INTO folders (id, name, type, account) VALUES (?, ?, ?, ?)"
    private val SQL_UPDATE_FOLDER_NAME =
        "UPDATE folders SET name = ? WHERE id = ?"
    private val SQL_DELETE_FOLDER =
        "DELETE FROM folders WHERE id = ?"

    private val SQL_SELECT_MESSAGES =
        "SELECT " +
            "m.id AS messageId, m.email_reference AS emailReference, m.from_addresses AS fromAddresses, m.recipients AS recipients, " +
            "m.cc_addresses AS ccAddresses, m.bcc_addresses AS bccAddresses, m.sent_date AS sentDate, m.received_date AS receivedDate, " +
            "m.subject AS subject, m.content AS content, m.raw_data AS rawData, m.unread AS unread, " +
            "t.id AS attachmentId, t.file_name AS fileName, t.content AS fileContent " +
            "FROM messages m LEFT JOIN attachments t ON m.id = t.message WHERE m.folder = ? AND (?)"
    private val SQL_INSERT_MESSAGE =
        "INSERT INTO messages (id, email_reference, from_addresses, recipients, cc_addresses, bcc_addresses, sent_date, received_date, subject, content, raw_data, unread, folder) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    private val SQL_UPDATE_MESSAGE_FOLDER =
        "UPDATE messages SET folder = ? WHERE id = ?"
    private val SQL_UPDATE_MESSAGE_UNREAD =
        "UPDATE messages SET unread = ? WHERE id = ?"
    private val SQL_DELETE_MESSAGE =
        "DELETE FROM messages WHERE id = ?"
    private val SQL_INSERT_ATTACHMENT =
        "INSERT INTO attachments (id, file_name, content, message) VALUES (?, ?, ?, ?)"
    private val SQL_DELETE_ATTACHMENT =
        "DELETE FROM attachments WHERE id = ?"

    private val SQL_SELECT_CONTACTS =
        "SELECT id AS contactId, email AS email, first_name AS firstName, last_name AS lastName FROM contacts ORDER BY email"
    private val SQL_INSERT_CONTACT =
        "INSERT INTO contacts (id, email, first_name, last_name) VALUES (?, ?, ?, ?)"
    private val SQL_UPDATE_CONTACT =
        "UPDATE contacts SET email = ?, first_name = ?, last_name = ? WHERE id = ?"
    private val SQL_DELETE_CONTACT =
        "DELETE FROM contacts WHERE id = ?"

    private val SQL_SELECT_OPTIONS = "SELECT value FROM options WHERE id = \"app\""
    private val SQL_UPDATE_OPTIONS = "UPDATE options SET value = ? WHERE id = \"app\""

    /**
     * Connects to the database using the provided file name.
     * 
     * @param fileName The name of the database file.
     * @throws DBRuntimeException If an error occurs while connecting to the database.
     */
    @Throws(DBRuntimeException::class)
    fun connect(fileName: String) {
        val correctedFileName = fileName.replace("\\\\", "/")

        try {
            Class.forName(Constants.DATABASE_DRIVER_CLASS)
            val url = Constants.DATABASE_JDBC_SCHEME + correctedFileName
            connection = DriverManager.getConnection(url)
            runMigrations()
            connection?.autoCommit = true
            setForeignKeysPragma()
        } catch (e: Exception) {
            throw DBRuntimeException(I18n.getString("database_connect_error"), e)
        }
    }

    /**
     * Disconnects from the database.
     * 
     * @throws DBRuntimeException If an error occurs while disconnecting from the database.
     */
    @Throws(DBRuntimeException::class)
    fun disconnect() {
        try {
            connection?.close()
            connection = null
        } catch (e: Exception) {
            throw DBRuntimeException(I18n.getString("database_disconnect_error"), e)
        }
    }

    /**
     * Adds a new account to the database.
     * 
     * @param account The account to add.
     * @throws DBRuntimeException If an error occurs while adding the account.
     */
    @Throws(DBRuntimeException::class)
    fun addAccount(account: Account) {
        checkConnection()

        try {
            connection?.prepareStatement(SQL_INSERT_ACCOUNT)?.use { ps ->
                ps.setString(1, account.id)
                ps.setString(2, account.email)
                ps.setString(3, account.displayName)
                ps.setString(4, account.user)
                ps.setString(5, encryptPassword(account.password))
                ps.setString(6, account.tokens)
                ps.setInt(7, account.protocol.ordinal)
                ps.setBoolean(8, account.sslActive)
                ps.setString(9, account.incomingHost)
                ps.setInt(10, account.incomingPort)
                ps.setString(11, account.outgoingHost)
                ps.setInt(12, account.outgoingPort)

                val affectedRows = ps.executeUpdate()
                if (affectedRows > 0) {
                    account.initFolders()
                    account.folders.forEach { addFolder(it) }
                }
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_add_or_update_account_error"), e)
        }
    }

    /**
     * Adds a new folder to the database.
     * 
     * @param folder The folder to add.
     * @throws DBRuntimeException If an error occurs while adding the folder.
     */
    @Throws(DBRuntimeException::class)
    fun addFolder(folder: Folder) {
        checkConnection()

        try {
            connection?.prepareStatement(SQL_INSERT_FOLDER)?.use { ps ->
                ps.setString(1, folder.id)
                ps.setString(2, folder.name)
                ps.setInt(3, folder.type.ordinal)
                ps.setString(4, folder.account!!.id)

                ps.executeUpdate()
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_add_folder_error"), e)
        }
    }

    /**
     * Adds a new message to the database.
     * 
     * @param message The message to add.
     * @throws DBRuntimeException If an error occurs while adding the message.
     */
    @Throws(DBRuntimeException::class)
    fun addMessage(message: Message) {
        checkConnection()

        try {
            connection?.prepareStatement(SQL_INSERT_MESSAGE)?.use { ps ->
                ps.setString(1, message.id)
                ps.setString(2, message.emailReference)
                ps.setString(3, message.fromAddresses)
                ps.setString(4, message.recipients)
                ps.setString(5, message.ccAddresses)
                ps.setString(6, message.bccAddresses)
                ps.setLong(7, message.sentDate.getTime())
                ps.setLong(8, message.receivedDate.getTime())
                ps.setString(9, message.subject)
                ps.setString(10, message.content)
                ps.setString(11, message.rawData)
                ps.setBoolean(12, message.unread)
                ps.setString(13, message.folder!!.id)

                val affectedRows = ps.executeUpdate()
                if (affectedRows > 0) {
                    message.attachments.forEach { addAttachment(it) }
                }
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_add_message_error"), e)
        }
    }

    /**
     * Adds a new attachment to the database.
     * 
     * @param attachment The attachment to add.
     * @throws DBRuntimeException If an error occurs while adding the attachment.
     */
    @Throws(DBRuntimeException::class)
    fun addAttachment(attachment: Attachment) {
        checkConnection()

        try {
            connection?.prepareStatement(SQL_INSERT_ATTACHMENT)?.use { ps ->
                ps.setString(1, attachment.id)
                ps.setString(2, attachment.fileName)
                ps.setString(3, attachment.content)
                ps.setString(4, attachment.message!!.id)

                ps.executeUpdate()
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_add_attachment_error"), e)
        }
    }

    /**
     * Adds a new contact to the database.
     * 
     * @param contact The contact to add.
     * @throws DBRuntimeException If an error occurs while adding the contact.
     */
    @Throws(DBRuntimeException::class)
    fun addContact(contact: Contact) {
        checkConnection()

        try {
            connection?.prepareStatement(SQL_INSERT_CONTACT)?.use { ps ->
                ps.setString(1, contact.id)
                ps.setString(2, contact.email)
                ps.setString(3, contact.firstName)
                ps.setString(4, contact.lastName)

                ps.executeUpdate()
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_add_or_update_contact_error"), e)
        }
    }

    /**
     * Updates the account in the database.
     * 
     * @param account The account to update.
     * @throws DBRuntimeException If an error occurs while updating the account.
     */
    @Throws(DBRuntimeException::class)
    fun updateAccount(account: Account) {
        checkConnection()

        try {
            connection?.prepareStatement(SQL_UPDATE_ACCOUNT)?.use { ps ->
                ps.setString(1, account.email)
                ps.setString(2, account.displayName)
                ps.setString(3, account.user)
                ps.setString(4, encryptPassword(account.password))
                ps.setString(5, account.tokens)
                ps.setInt(6, account.protocol.ordinal)
                ps.setBoolean(7, account.sslActive)
                ps.setString(8, account.incomingHost)
                ps.setInt(9, account.incomingPort)
                ps.setString(10, account.outgoingHost)
                ps.setInt(11, account.outgoingPort)
                ps.setString(12, account.id)

                ps.executeUpdate()
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_add_or_update_account_error"), e)
        }
    }

    /**
     * Updates the access and refresh token for a given account in the database.
     *
     * @param id The ID of the account.
     * @param tokens The new tokens to be set for the account.
     * @throws DBRuntimeException If an error occurs while updating the account.
     */
    @Throws(DBRuntimeException::class)
    fun updateAccountTokens(id: String, tokens: String) {
        checkConnection()

        try {
            connection?.prepareStatement(SQL_UPDATE_ACCOUNT_TOKENS)?.use { ps ->
                ps.setString(1, tokens)
                ps.setString(2, id)

                ps.executeUpdate()
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_update_account_tokens_error"), e)
        }
    }

    /**
     * Updates the name of the folder in the database.
     * 
     * @param id The folder ID.
     * @param name The new name of the folder.
     * @throws DBRuntimeException If an error occurs while updating the folder name.
     */
    @Throws(DBRuntimeException::class)
    fun updateFolderName(id: String, name: String) {
        checkConnection()

        try {
            connection?.prepareStatement(SQL_UPDATE_FOLDER_NAME)?.use { ps ->
                ps.setString(1, name)
                ps.setString(2, id)

                ps.executeUpdate()
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_update_folder_name_error"), e)
        }
    }

    /**
     * Updates the folder of a message in the database.
     * 
     * @param id The message ID.
     * @param folder The new folder ID.
     * @throws DBRuntimeException If an error occurs while updating the message folder.
     */
    @Throws(DBRuntimeException::class)
    fun updateMessageFolder(id: String, folder: String) {
        checkConnection()

        try {
            connection?.prepareStatement(SQL_UPDATE_MESSAGE_FOLDER)?.use { ps ->
                ps.setString(1, folder)
                ps.setString(2, id)

                ps.executeUpdate()
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_update_message_folder_error"), e)
        }
    }

    /**
     * Updates the unread status of a message in the database.
     * 
     * @param id The message ID.
     * @param unread The new unread status.
     * @throws DBRuntimeException If an error occurs while updating the unread status.
     */
    @Throws(DBRuntimeException::class)
    fun updateMessageUnread(id: String, unread: Boolean) {
        checkConnection()

        try {
            connection?.prepareStatement(SQL_UPDATE_MESSAGE_UNREAD)?.use { ps ->
                ps.setBoolean(1, unread)
                ps.setString(2, id)

                ps.executeUpdate()
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_update_message_unread_error"), e)
        }
    }

    /**
     * Updates the contact in the database.
     * 
     * @param contact The contact to update.
     * @throws DBRuntimeException If an error occurs while updating the contact.
     */
    @Throws(DBRuntimeException::class)
    fun updateContact(contact: Contact) {
        checkConnection()

        try {
            connection?.prepareStatement(SQL_UPDATE_CONTACT)?.use { ps ->
                ps.setString(1, contact.email)
                ps.setString(2, contact.firstName)
                ps.setString(3, contact.lastName)
                ps.setString(4, contact.id)

                ps.executeUpdate()
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_add_or_update_contact_error"), e)
        }
    }

    /**
     * Deletes an account from the database.
     * 
     * @param account The account to delete.
     * @throws DBRuntimeException If an error occurs while deleting the account.
     */
    @Throws(DBRuntimeException::class)
    fun deleteAccount(account: Account) {
        checkConnection()

        account.folders.forEach { deleteFolder(it) }

        try {
            connection?.prepareStatement(SQL_DELETE_ACCOUNT)?.use { ps ->
                ps.setString(1, account.id)
                ps.executeUpdate()
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_delete_account_error"), e)
        }
    }

    /**
     * Deletes a folder from the database.
     * 
     * @param folder The folder to delete.
     * @throws DBRuntimeException If an error occurs while deleting the folder.
     */
    @Throws(DBRuntimeException::class)
    fun deleteFolder(folder: Folder) {
        checkConnection()

        folder.messages.forEach { deleteMessage(it) }

        try {
            connection?.prepareStatement(SQL_DELETE_FOLDER)?.use { ps ->
                ps.setString(1, folder.id)
                ps.executeUpdate()
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_delete_folder_error"), e)
        }
    }

    /**
     * Deletes a message from the database.
     * 
     * @param message The message to delete.
     * @throws DBRuntimeException If an error occurs while deleting the message.
     */
    @Throws(DBRuntimeException::class)
    fun deleteMessage(message: Message) {
        checkConnection()

        message.attachments.forEach { deleteAttachment(it) }

        try {
            connection?.prepareStatement(SQL_DELETE_MESSAGE)?.use { ps ->
                ps.setString(1, message.id)
                ps.executeUpdate()
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_delete_message_error"), e)
        }
    }

    /**
     * Deletes an attachment from the database.
     * 
     * @param attachment The attachment to delete.
     * @throws DBRuntimeException If an error occurs while deleting the attachment.
     */
    @Throws(DBRuntimeException::class)
    fun deleteAttachment(attachment: Attachment) {
        checkConnection()

        try {
            connection?.prepareStatement(SQL_DELETE_ATTACHMENT)?.use { ps ->
                ps.setString(1, attachment.id)
                ps.executeUpdate()
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_delete_attachment_error"), e)
        }
    }

    /**
     * Deletes a contact from the database.
     * 
     * @param contact The contact to delete.
     * @throws DBRuntimeException If an error occurs while deleting the contact.
     */
    @Throws(DBRuntimeException::class)
    fun deleteContact(contact: Contact) {
        checkConnection()

        try {
            connection?.prepareStatement(SQL_DELETE_CONTACT)?.use { ps ->
                ps.setString(1, contact.id)
                ps.executeUpdate()
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_delete_contact_error"), e)
        }
    }

    /**
     * Applies a message filter to retrieve messages from the database.
     * 
     * @param theFolder The folder to filter messages from.
     * @throws DBRuntimeException If an error occurs while applying the message filter.
     */
    @Throws(DBRuntimeException::class)
    fun applyMessageFilter(theFolder: Folder) {
        checkConnection()

        val findText = theFolder.messageFilter.findText.split(" ")
        var where = ""
        for (find in findText) {
            where += if (theFolder.messageFilter.matchCase) {
                "LOWER(m.subject) LIKE \"%${find.lowercase()}%\" OR LOWER(m.content) LIKE \"%${find.lowercase()}%\" OR "
            } else {
                "LOWER(m.subject) LIKE \"%$find%\" OR LOWER(m.content) LIKE \"%$find%\" OR "
            }
        }
        where = if (where.isNotEmpty()) where.substring(0, where.length - 4) else "TRUE"

        var sql = SQL_SELECT_MESSAGES
        sql = sql.replaceFirst("\\?", theFolder.id)
        sql = sql.replaceFirst("\\?", where)

        try {
            connection?.createStatement()?.use { stmt ->
                stmt.executeQuery(sql)?.use { rs ->
                    theFolder.messages.clear()
                    val messageMap = HashMap<String, Message>()

                    while (rs.next()) {
                        val messageId = rs.getString("messageId")
                        if (messageId != null) {
                            val newMessage = messageMap.computeIfAbsent(messageId) {
                                val message = Message().apply {
                                    id = messageId
                                    emailReference = rs.getString("emailReference")
                                    fromAddresses = rs.getString("fromAddresses")
                                    recipients = rs.getString("recipients")
                                    ccAddresses = rs.getString("ccAddresses")
                                    bccAddresses = rs.getString("bccAddresses")
                                    sentDate = Date(rs.getLong("sentDate"))
                                    receivedDate = Date(rs.getLong("receivedDate"))
                                    subject = rs.getString("subject")
                                    content = rs.getString("content")
                                    rawData = rs.getString("rawData")
                                    unread = rs.getBoolean("unread")
                                    folder = theFolder
                                }
                                theFolder.messages.add(message)
                                message
                            }

                            val attachmentId = rs.getString("attachmentId")
                            if (attachmentId != null) {
                                val newAttachment = Attachment().apply {
                                    id = attachmentId
                                    fileName = rs.getString("fileName")
                                    content = rs.getString("fileContent")
                                    message = newMessage
                                }
                                newMessage.attachments.add(newAttachment)
                            }
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_message_filter_error"), e)
        }
    }

    /**
     * Loads accounts from the database.
     *
     * @param bag The bag object to populate with loaded accounts.
     * @throws DBRuntimeException If an error occurs while loading accounts.
     */
    @Throws(DBRuntimeException::class)
    fun loadAccounts(bag: Bag) {
        checkConnection()

        try {
            connection?.createStatement()?.use { stmt ->
                stmt.executeQuery(SQL_SELECT_ACCOUNTS)?.use { rs ->
                    val accountMap = HashMap<String, Account>()
                    val folderMap = HashMap<String, Folder>()
                    val messageMap = HashMap<String, Message>()

                    while (rs.next()) {
                        val accountId = rs.getString("accountId")
                        val newAccount = accountMap.computeIfAbsent(accountId) {
                            val account = Account().apply {
                                id = accountId
                                email = rs.getString("email")
                                displayName = rs.getString("displayName")
                                user = rs.getString("user")
                                password = decryptPassword(rs.getString("password"))
                                tokens = rs.getString("tokens")
                                protocol = MailProtocol.values()[rs.getInt("mailProtocol")]
                                sslActive = rs.getBoolean("sslActive")
                                incomingHost = rs.getString("incomingHost")
                                incomingPort = rs.getInt("incomingPort")
                                outgoingHost = rs.getString("outgoingHost")
                                outgoingPort = rs.getInt("outgoingPort")
                            }
                            bag.accounts.add(account)
                            account
                        }

                        val folderId = rs.getString("folderId")
                        val newFolder = folderMap.computeIfAbsent(folderId) {
                            val folder = Folder().apply {
                                id = folderId
                                name = rs.getString("folderName")
                                type = FolderType.values()[rs.getInt("folderType")]
                                account = newAccount
                            }
                            newAccount.folders.add(folder)
                            folder
                        }

                        val messageId = rs.getString("messageId")
                        if (messageId != null) {
                            val newMessage = messageMap.computeIfAbsent(messageId) {
                                val message = Message().apply {
                                    id = messageId
                                    emailReference = rs.getString("emailReference")
                                    fromAddresses = rs.getString("fromAddresses")
                                    recipients = rs.getString("recipients")
                                    ccAddresses = rs.getString("ccAddresses")
                                    bccAddresses = rs.getString("bccAddresses")
                                    sentDate = Date(rs.getLong("sentDate"))
                                    receivedDate = Date(rs.getLong("receivedDate"))
                                    subject = rs.getString("subject")
                                    content = rs.getString("content")
                                    rawData = rs.getString("rawData")
                                    unread = rs.getBoolean("unread")
                                    folder = newFolder
                                }
                                newFolder.messages.add(message)
                                message
                            }

                            val attachmentId = rs.getString("attachmentId")
                            if (attachmentId != null) {
                                val newAttachment = Attachment().apply {
                                    id = attachmentId
                                    fileName = rs.getString("fileName")
                                    content = rs.getString("fileContent")
                                    message = newMessage
                                }
                                newMessage.attachments.add(newAttachment)
                            }
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_load_accounts_error"), e)
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw DBRuntimeException(I18n.getString("database_load_accounts_error"), e)
        }
    }

    /**
     * Loads contacts from the database.
     *
     * @param bag The bag object to populate with loaded contacts.
     * @throws DBRuntimeException If an error occurs while loading contacts.
     */
    @Throws(DBRuntimeException::class)
    fun loadContacts(bag: Bag) {
        checkConnection()

        try {
            connection?.createStatement()?.use { stmt ->
                stmt.executeQuery(SQL_SELECT_CONTACTS)?.use { rs ->
                    while (rs.next()) {
                        val newContact = Contact().apply {
                            id = rs.getString("contactId")
                            email = rs.getString("email")
                            firstName = rs.getString("firstName")
                            lastName = rs.getString("lastName")
                        }
                        bag.contacts.add(newContact)
                    }
                }
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_load_contacts_error"), e)
        }
    }

    /**
     * Loads options from the database.
     *
     * @param bag The bag object to populate with loaded options.
     * @throws DBRuntimeException If an error occurs while loading options.
     */
    @Throws(DBRuntimeException::class)
    fun loadOptions(bag: Bag) {
        checkConnection()

        try {
            connection?.createStatement()?.use { stmt ->
                stmt.executeQuery(SQL_SELECT_OPTIONS)?.use { rs ->
                    if (rs.next()) {
                        val jsonString = rs.getString("value")
                        val gson = Gson()
                        bag.options = gson.fromJson(jsonString, Options::class.java)
                    }
                }
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_load_options_error"), e)
        }
    }

    /**
     * Saves options to the database.
     *
     * @param bag The bag object containing options to save.
     * @throws DBRuntimeException If an error occurs while saving options.
     */
    @Throws(DBRuntimeException::class)
    fun saveOptions(bag: Bag) {
        checkConnection()

        try {
            connection?.prepareStatement(SQL_UPDATE_OPTIONS)?.use { ps ->
                val gson = Gson()
                val jsonString = gson.toJson(bag.options)
                ps.setString(1, jsonString)
                ps.executeUpdate()
            }
        } catch (e: SQLException) {
            throw DBRuntimeException(I18n.getString("database_save_options_error"), e)
        }
    }

    /**
     * Checks if the database connection is established.
     *
     * @throws DBRuntimeException If the connection is not established.
     */
    @Throws(DBRuntimeException::class)
    private fun checkConnection() {
        if (connection == null) {
            throw DBRuntimeException(I18n.getString("database_not_connected_error"))
        }
    }

    /**
     * Runs database migrations using Liquibase.
     *
     * @throws Exception If an error occurs while running the migrations.
     */
    @Throws(Exception::class)
    private fun runMigrations() {
        val changeLogLocation = "migration/changelog.xml"
        val database = DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(JdbcConnection(connection))
        val updateCommand = CommandScope(UpdateCommandStep.COMMAND_NAME.joinToString(" "))
        updateCommand.addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database)
        updateCommand.addArgumentValue(UpdateCommandStep.CHANGELOG_FILE_ARG, changeLogLocation)
        logger.info("Starting database migrations with changelog: $changeLogLocation")
        updateCommand.execute()
        logger.info("Database migrations completed.")
    }

    /**
     * Sets foreign key pragma for SQLite.
     */
    private fun setForeignKeysPragma() {
        try {
            connection?.createStatement()?.use { stmt ->
                stmt.execute("PRAGMA foreign_keys = ON")
            }
        } catch (e: SQLException) {
        }
    }

    /**
     * Encrypts the given password.
     *
     * @param password The password to encrypt.
     * @return The encrypted password.
     * @throws DBRuntimeException If an error occurs while encrypting the password.
     */
    private fun encryptPassword(password: String): String {
        var cipher = ""

        try {
            cipher = PasswordCrypt.encrypt(password)
        } catch (e: Exception) {
        }

        return cipher
    }

    /**
     * Decrypts the given password.
     *
     * @param password The password to decrypt.
     * @return The decrypted password.
     */
    private fun decryptPassword(password: String): String {
        var plain = ""

        try {
            plain = PasswordCrypt.decrypt(password)
        } catch (e: Exception) {
        }

        return plain
    }
}
