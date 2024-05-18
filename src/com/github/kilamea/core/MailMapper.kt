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

/**
 * Utility object for mapping email-related data.
 *
 * @since 0.1.0
 */
internal object MailMapper {
    /**
     * Maps a MimeMessage to a Message object.
     *
     * @param email The MimeMessage to map.
     * @return The mapped Message object.
     * @throws IOException If an I/O error occurs.
     * @throws MessagingException If a messaging error occurs.
     */
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

    /**
     * Maps a Message object to a MimeMessage.
     *
     * @param session The email session to use.
     * @param message The Message object to map.
     * @return The mapped MimeMessage.
     * @throws IOException If an I/O error occurs.
     * @throws MessagingException If a messaging error occurs.
     */
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

    /**
     * Converts a MimeMessage to its raw string representation.
     *
     * @param email The MimeMessage to convert.
     * @return The string representation of the MimeMessage.
     * @throws IOException If an I/O error occurs.
     * @throws MessagingException If a messaging error occurs.
     */
    @Throws(IOException::class, MessagingException::class)
    private fun emailToString(email: MimeMessage): String {
        val outputStream = ByteArrayOutputStream()
        email.writeTo(outputStream)
        return outputStream.toString()
    }

    /**
     * Extracts content from a MimeMultipart object and sets it in the given Message object.
     *
     * @param multipart The MimeMultipart to extract content from.
     * @param message The Message object to set the extracted content.
     * @throws IOException If an I/O error occurs.
     * @throws MessagingException If a messaging error occurs.
     */
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

    /**
     * Adds text content and attachments to a MimeMessage.
     *
     * @param email The MimeMessage to add content and attachments.
     * @param text The text content to add.
     * @param attachments The attachments to add.
     * @throws IOException If an I/O error occurs.
     * @throws MessagingException If a messaging error occurs.
     */
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

    /**
     * Collects email addresses from an array of Address objects and returns them as a single string.
     *
     * @param addresses The array of Address objects.
     * @return A string of collected email addresses, separated by commas.
     */
    private fun collectAddresses(addresses: Array<out Address>?): String {
        return addresses?.joinToString(", ") { decodeAddress(it) } ?: ""
    }

    /**
     * Decodes an email address to its string representation.
     *
     * @param address The Address to decode.
     * @return The decoded email address as a string.
     */
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

    /**
     * Reads the content of a BodyPart as a byte array.
     *
     * @param bodyPart The BodyPart to read content from.
     * @return The content of the BodyPart as a byte array.
     * @throws IOException If an I/O error occurs.
     * @throws MessagingException If a messaging error occurs.
     */
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

    /**
     * Parses a string of recipient email addresses into an array of InternetAddress objects.
     * 
     * @param recipients A string containing recipient email addresses, separated by commas or semicolons.
     * @return An array of InternetAddress objects.
     * @throws AddressException If any of the addresses are invalid.
     * @throws UnsupportedEncodingException If the encoding is not supported.
     */
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
