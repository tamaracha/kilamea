package com.github.kilamea.core

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.Base64
import java.util.Date
import java.util.stream.Collectors

import javax.activation.DataHandler
import javax.activation.DataSource
import javax.mail.Address
import javax.mail.BodyPart
import javax.mail.Message as Email
import javax.mail.MessagingException
import javax.mail.Part
import javax.mail.Session
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.internet.MimeUtility
import javax.mail.util.ByteArrayDataSource

import com.github.kilamea.entity.Attachment
import com.github.kilamea.entity.AttachmentList
import com.github.kilamea.entity.Message

internal object MailMapper {
    @Throws(IOException::class, MessagingException::class)
    fun map(email: MimeMessage): Message {
        val newMessage = Message()

        val emailReference = email.getMessageID()
        newMessage.emailReference = emailReference

        val fromAddresses = collectAddresses(email.getFrom())
        newMessage.fromAddresses = fromAddresses

        val recipients = collectAddresses(email.getRecipients(Email.RecipientType.TO))
        newMessage.recipients = recipients

        val ccAddresses = collectAddresses(email.getRecipients(Email.RecipientType.CC))
        newMessage.ccAddresses = ccAddresses

        val sentDate = email.getSentDate() ?: Date()
        newMessage.sentDate = sentDate

        val receivedDate = email.getReceivedDate() ?: Date()
        newMessage.receivedDate = receivedDate

        val subject = email.getSubject()
        newMessage.subject = subject

        val content = email.getContent()
        if (content is MimeMultipart) {
            extractMultipart(content, newMessage)
        } else {
            newMessage.content = content as String
        }

        newMessage.rawData = emailToString(email)

        return newMessage
    }

    @Throws(IOException::class, MessagingException::class)
    fun map(session: Session, message: Message): MimeMessage {
        val newEmail = MimeMessage(session)

        try {
            newEmail.setFrom(InternetAddress(message.fromAddresses))

            newEmail.setRecipients(Email.RecipientType.TO, parse(message.recipients))

            if (message.ccAddresses.isNotEmpty()) {
                newEmail.setRecipients(Email.RecipientType.CC, parse(message.ccAddresses))
            }

            if (message.bccAddresses.isNotEmpty()) {
                newEmail.setRecipients(Email.RecipientType.BCC, parse(message.bccAddresses))
            }
        } catch (e: AddressException) {
            throw MessagingException(e.message ?: "")
        } catch (e: UnsupportedEncodingException) {
            throw MessagingException(e.message ?: "")
        }

        newEmail.setSubject(message.subject)
        newEmail.setSentDate(message.sentDate)
        newEmail.setHeader("User-Agent", Constants.APP_NAME)

        if (message.attachments.isEmpty()) {
            newEmail.setText(message.content)
        } else {
            addMultipart(newEmail, message.content, message.attachments)
        }

        return newEmail
    }

    @Throws(IOException::class, MessagingException::class)
    private fun emailToString(email: MimeMessage): String {
        val outputStream = ByteArrayOutputStream()
        email.writeTo(outputStream)
        return outputStream.toString()
    }

    @Throws(IOException::class, MessagingException::class)
    private fun extractMultipart(multipart: MimeMultipart, message: Message) {
        val textContent = StringBuilder()
        val htmlContent = StringBuilder()

        val count = multipart.count
        for (i in 0 until count) {
            val bodyPart = multipart.getBodyPart(i)
            val disposition = bodyPart.disposition
            if ((disposition == null || disposition.equals(Part.ATTACHMENT, ignoreCase = true))
                && bodyPart.fileName != null
            ) {
                val contentBytes = getContentBytes(bodyPart)
                val base64Content = Base64.getEncoder().encodeToString(contentBytes)
                val attachment = Attachment().apply {
                    content = base64Content
                    fileName = MimeUtility.decodeText(bodyPart.fileName)
                    this.message = message
                }
                message.attachments.add(attachment)
            } else {
                val content = bodyPart.content
                if (bodyPart.isMimeType(MimeType.TEXT_PLAIN.toString())) {
                    textContent.append(content as String)
                } else if (bodyPart.isMimeType(MimeType.TEXT_HTML.toString())) {
                    htmlContent.append(content as String)
                } else if (content is MimeMultipart) {
                    extractMultipart(content, message)
                }
            }
        }

        if (textContent.isNotEmpty()) {
            message.content = textContent.toString()
        } else if (htmlContent.isNotEmpty()) {
            message.content = htmlContent.toString()
        }
    }

    @Throws(IOException::class, MessagingException::class)
    private fun addMultipart(email: MimeMessage, text: String, attachments: AttachmentList) {
        val multipart = MimeMultipart()

        val textPart = MimeBodyPart()
        textPart.setText(text)
        multipart.addBodyPart(textPart)

        for (attachment in attachments) {
            val attachmentPart = MimeBodyPart()
            val decodedBytes = Base64.getDecoder().decode(attachment.content)
            val dataSource = ByteArrayDataSource(decodedBytes, "application/octet-stream")
            attachmentPart.dataHandler = DataHandler(dataSource)
            attachmentPart.fileName = attachment.fileName
            multipart.addBodyPart(attachmentPart)
        }

        email.setContent(multipart)
    }

    private fun collectAddresses(addresses: Array<out Address>?): String {
        return addresses?.joinToString(", ") { decodeAddress(it) } ?: ""
    }

    private fun decodeAddress(address: Address): String {
        return try {
            val internetAddress = InternetAddress(address.toString())
            val personal = internetAddress.personal
            val mailAddress = internetAddress.address
            if (!personal.isNullOrBlank()) {
                "$personal <$mailAddress>"
            } else {
                mailAddress
            }
        } catch (e: AddressException) {
            ""
        }
    }

    @Throws(IOException::class, MessagingException::class)
    private fun getContentBytes(bodyPart: BodyPart): ByteArray {
        bodyPart.inputStream.use { inputStream ->
            ByteArrayOutputStream().use { outputStream ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                return outputStream.toByteArray()
            }
        }
    }

    @Throws(AddressException::class, UnsupportedEncodingException::class)
    private fun parse(recipients: String): Array<InternetAddress> {
        val encodedRecipients = StringBuilder()
        var recipientsList = recipients.replace(";", ",")

        for (recipient in recipientsList.split(",")) {
            val address = InternetAddress(recipient.trim())
            address.setPersonal(address.personal, StandardCharsets.UTF_8.name())
            encodedRecipients.append(address.toString()).append(",")
        }

        if (encodedRecipients.length > 0) {
            encodedRecipients.setLength(encodedRecipients.length - 1)
        }

        return InternetAddress.parse(encodedRecipients.toString(), true)
    }
}
