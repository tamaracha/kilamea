package com.github.kilamea.entity

import java.util.Date

/**
 * Represents an email message with various properties such as sender, recipients, subject, and content.
 *
 * @since 0.1.0
 * @property emailReference The message ID.
 * @property fromAddresses The sender's addresses.
 * @property recipients The recipient addresses.
 * @property ccAddresses The CC (carbon copy) addresses.
 * @property bccAddresses The BCC (blind carbon copy) addresses.
 * @property sentDate The date the email was sent.
 * @property receivedDate The date the email was received.
 * @property subject The subject of the email.
 * @property content The content of the email.
 * @property rawData The raw data of the email.
 * @property unread Indicates if the email is unread.
 * @property attachments The list of attachments in the email.
 * @property folder The folder to which the email belongs.
 * @property drafted Indicates if the email is a draft.
 */
class Message : AbstractEntity() {
    var emailReference: String = ""
    var fromAddresses: String = ""
    var recipients: String = ""
    var ccAddresses: String = ""
    var bccAddresses: String = ""
    var sentDate: Date = Date()
    var receivedDate: Date = Date()
    var subject: String = ""
    var content: String = ""
    var rawData: String = ""
    var unread: Boolean = true
    var attachments: AttachmentList = AttachmentList()
    var folder: Folder? = null

    @Transient
    var drafted: Boolean = false

    /**
     * Returns the subject of the email.
     *
     * @return The subject of the email as a string.
     */
    override fun toString(): String {
        return subject
    }

    /**
     * Creates a copy of this message.
     *
     * @return A new Message object that is a copy of this message.
     */
    fun copy(): Message {
        val newMessage = Message()
        newMessage.fromAddresses = this.fromAddresses
        newMessage.recipients = this.recipients
        newMessage.ccAddresses = this.ccAddresses
        newMessage.bccAddresses = this.bccAddresses
        newMessage.sentDate = Date(this.sentDate.getTime())
        newMessage.receivedDate = Date(this.receivedDate.getTime())
        newMessage.subject = this.subject
        newMessage.content = this.content
        newMessage.rawData = this.rawData
        newMessage.unread = this.unread
        newMessage.folder = this.folder
        newMessage.drafted = this.drafted

        val newAttachments = AttachmentList()
        this.attachments.forEach { attachment ->
            val newAttachment = Attachment()
            newAttachment.content = attachment.content
            newAttachment.fileName = attachment.fileName
            newAttachment.message = newMessage
            newAttachments.add(newAttachment)
        }
        newMessage.attachments = newAttachments

        return newMessage
    }
}
