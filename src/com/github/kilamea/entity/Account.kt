package com.github.kilamea.entity

import com.github.kilamea.i18n.I18n
import com.github.kilamea.mail.MailProtocol

/**
 * Represents an email account with its settings and folders.
 *
 * @since 0.1.0
 * @property email The email address of the account.
 * @property displayName The name associated with the account.
 * @property user The username for authentication.
 * @property password The password for authentication.
 * @property tokens The access and refresh token for authentication.
 * @property protocol The mail protocol used (e.g., IMAP or POP3).
 * @property sslActive Indicates if SSL is active.
 * @property incomingHost The host for incoming mail.
 * @property incomingPort The port for incoming mail.
 * @property outgoingHost The host for outgoing mail.
 * @property outgoingPort The port for outgoing mail.
 * @property folders The list of folders associated with the account.
 * @property displayNameAndEmail The name and email formatted as a string.
 */
class Account : AbstractEntity() {
    var email: String = ""
    var displayName: String = ""
    var user: String = ""
    var password: String = ""
    var tokens: String = ""
    var protocol: MailProtocol = MailProtocol.IMAP
    var sslActive: Boolean = true
    var incomingHost: String = ""
    var incomingPort: Int = MailProtocol.IMAP.port(sslActive)
    var outgoingHost: String = ""
    var outgoingPort: Int = MailProtocol.SMTP.port(sslActive)
    var folders: FolderList = FolderList()

    val displayNameAndEmail: String
        get() = "$displayName <$email>"

    /**
     * Initializes the default folders for the account.
     */
    fun initFolders() {
        val folderList = FolderList()

        folderList.add(createFolder("folder_inbox", FolderType.Inbox))
        folderList.add(createFolder("folder_drafts", FolderType.Drafts))
        folderList.add(createFolder("folder_sent", FolderType.Sent))
        folderList.add(createFolder("folder_archive", FolderType.Archive))
        folderList.add(createFolder("folder_trash", FolderType.Trash))

        folders = folderList
    }

    /**
     * Creates a folder with the specified name key and type.
     *
     * @param nameKey The key for the folder's name.
     * @param type The type of the folder.
     * @return The created folder.
     */
    private fun createFolder(nameKey: String, type: FolderType): Folder {
        val folder = Folder()
        folder.name = I18n.getString(nameKey)
        folder.type = type
        folder.account = this
        return folder
    }

    /**
     * Retrieves a folder by its type.
     *
     * @param type The type of the folder.
     * @return The folder of the specified type, or null if not found.
     */
    fun getFolderByType(type: FolderType): Folder? {
        for (folder in folders) {
            if (folder.type == type) {
                return folder
            }
        }
        return null
    }

    /**
     * Returns the email address of the account.
     *
     * @return The email address as a string.
     */
    override fun toString(): String {
        return email
    }

    companion object {
        /**
         * Checks if the email address belongs to Gmail.
         *
         * @return True if the email address ends with "@gmail.com" or "@googlemail.com", otherwise false.
         */
        fun isGmail(string: String): Boolean {
            return string.lowercase().endsWith("@gmail.com") || string.lowercase().endsWith("@googlemail.com")
        }
    }
}
