package com.github.kilamea.entity

import com.github.kilamea.core.MailProtocol
import com.github.kilamea.i18n.I18n

class Account : AbstractEntity() {
    var email: String = ""
    var displayName: String = ""
    var user: String = ""
    var password: String = ""
    var protocol: MailProtocol = MailProtocol.IMAP
    var sslActive: Boolean = true
    var incomingHost: String = ""
    var incomingPort: Int = MailProtocol.IMAP.port(sslActive)
    var outgoingHost: String = ""
    var outgoingPort: Int = MailProtocol.SMTP.port(sslActive)
    var folders: FolderList = FolderList()

    val displayNameAndEmail: String
        get() = "$displayName <$email>"

    fun initFolders() {
        val folderList = FolderList()

        folderList.add(createFolder("folder_inbox", FolderType.Inbox))
        folderList.add(createFolder("folder_drafts", FolderType.Drafts))
        folderList.add(createFolder("folder_sent", FolderType.Sent))
        folderList.add(createFolder("folder_archive", FolderType.Archive))
        folderList.add(createFolder("folder_trash", FolderType.Trash))

        folders = folderList
    }

    private fun createFolder(nameKey: String, type: FolderType): Folder {
        val folder = Folder()
        folder.name = I18n.getString(nameKey)
        folder.type = type
        folder.account = this
        return folder
    }

    fun getFolderByType(type: FolderType): Folder? {
        for (folder in folders) {
            if (folder.type == type) {
                return folder
            }
        }
        return null
    }

    override fun toString(): String {
        return email
    }
}
