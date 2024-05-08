package com.github.kilamea.entity

class Folder : AbstractEntity() {
    var name: String = ""
    var type: FolderType = FolderType.Custom
    var messages: MessageList = MessageList()
    var messageFilter: ListFilter = ListFilter()
    var account: Account? = null

    fun getUnreadMessageCount(): Int {
        var unread = 0

        for (message in messages) {
            if (message.unread) {
                unread++
            }
        }

        return unread
    }

    fun containsMessage(emailReference: String): Boolean {
        var found = false

        for (message in messages) {
            if (message.emailReference == emailReference) {
                found = true
                break
            }
        }

        return found
    }

    override fun toString(): String {
        return name
    }
}
