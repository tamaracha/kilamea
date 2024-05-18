package com.github.kilamea.entity

/**
 * Represents a folder containing messages, with a specific type and belonging to an account.
 *
 * @since 0.1.0
 * @property name The name of the folder.
 * @property type The type of the folder.
 * @property messages The list of messages in the folder.
 * @property messageFilter The filter applied to the messages in the folder.
 * @property account The account to which the folder belongs.
 */
class Folder : AbstractEntity() {
    var name: String = ""
    var type: FolderType = FolderType.Custom
    var messages: MessageList = MessageList()
    var messageFilter: ListFilter = ListFilter()
    var account: Account? = null

    /**
     * Gets the count of unread messages in the folder.
     *
     * @return The number of unread messages.
     */
    fun getUnreadMessageCount(): Int {
        var unread = 0

        for (message in messages) {
            if (message.unread) {
                unread++
            }
        }

        return unread
    }

    /**
     * Checks if the folder contains a message with the given email reference.
     *
     * @param emailReference The reference of the email to check.
     * @return true if the folder contains the message, otherwise false.
     */
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

    /**
     * Returns the name of the folder.
     *
     * @return The name of the folder as string.
     */
    override fun toString(): String {
        return name
    }
}
