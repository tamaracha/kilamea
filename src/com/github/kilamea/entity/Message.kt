package com.github.kilamea.entity

import java.util.Date

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

    override fun toString(): String {
        return subject
    }

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
