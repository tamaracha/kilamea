package com.github.kilamea.mail

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.Base64
import java.util.Date
import java.util.stream.Collectors

import javax.mail.Address
import javax.mail.BodyPart
import javax.mail.MessagingException
import javax.mail.Part
import javax.mail.Session
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

import com.github.kilamea.core.Constants
import com.github.kilamea.entity.Attachment
import com.github.kilamea.entity.AttachmentList
import com.github.kilamea.entity.Message
import com.github.kilamea.util.equalsIgnoreCase
import com.google.api.services.gmail.model.Message as GmailMessage

/**
 * Utility object for mapping email-related data.
 *
 * @since 0.1.0
 */
internal object MailMapper {
    /**
     * Maps a `MimeMessage` to a `Message` object.
     *
     * @param email The `MimeMessage` to map.
     * @return The mapped `Message` object.
     * @throws IOException If an I/O error occurs.
     * @throws MessagingException If a messaging error occurs.
     */
    @Throws(IOException::class, MessagingException::class)
    fun mapMimeMessageToMessage(email: MimeMessage): Message {
        val newMessage = Message()

        val emailReference = email.getMessageID()
        newMessage.emailReference = emailReference

        val fromAddresses = collectAddresses(email.getFrom())
        newMessage.fromAddresses = fromAddresses

        val recipients = collectAddresses(email.getRecipients(MimeMessage.RecipientType.TO))
        newMessage.recipients = recipients

        val ccAddresses = collectAddresses(email.getRecipients(MimeMessage.RecipientType.CC))
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

        newMessage.rawData = getRawData(email)

        return newMessage
    }

    /**
     * Maps a `GmailMessage` to a `Message` object.
     *
     * @param email The `GmailMessage` to map.
     * @param session The email session to use.
     * @return The mapped `Message` object.
     * @throws IOException If an I/O error occurs.
     * @throws MessagingException If a messaging error occurs.
     */
    @Throws(IOException::class, MessagingException::class)
    fun mapGmailMessageToMessage(email: GmailMessage, session: Session): Message {
        val rawMessageBytes = Base64.getUrlDecoder().decode(email.raw)
        val decodedString = String(rawMessageBytes)
        val mimeMessage = MimeMessage(session, ByteArrayInputStream(decodedString.toByteArray()))

        val newMessage = mapMimeMessageToMessage(mimeMessage)
        return newMessage
    }

    /**
     * Maps a `Message` object to a `MimeMessage`.
     *
     * @param message The `Message` object to map.
     * @param session The email session to use.
     * @return The mapped `MimeMessage`.
     * @throws IOException If an I/O error occurs.
     * @throws MessagingException If a messaging error occurs.
     */
    @Throws(IOException::class, MessagingException::class)
    fun mapMessageToMimeMessage(message: Message, session: Session): MimeMessage {
        val newEmail = MimeMessage(session)

        try {
            newEmail.setFrom(parse(message.fromAddresses)[0])

            newEmail.setRecipients(MimeMessage.RecipientType.TO, parse(message.recipients))

            if (message.ccAddresses.isNotEmpty()) {
                newEmail.setRecipients(MimeMessage.RecipientType.CC, parse(message.ccAddresses))
            }

            if (message.bccAddresses.isNotEmpty()) {
                newEmail.setRecipients(MimeMessage.RecipientType.BCC, parse(message.bccAddresses))
            }
        } catch (e: AddressException) {
            throw MessagingException(e.message ?: "")
        } catch (e: UnsupportedEncodingException) {
            throw MessagingException(e.message ?: "")
        }

        newEmail.setSentDate(message.sentDate)
        newEmail.setSubject(message.subject)
        newEmail.setHeader("User-Agent", Constants.APP_NAME)

        if (message.attachments.isEmpty()) {
            newEmail.setText(message.content)
        } else {
            addMultipart(newEmail, message.content, message.attachments)
        }

        return newEmail
    }

    /**
     * Maps a `Message` object to a `GmailMessage`.
     *
     * @param message The `Message` object to map.
     * @param session The email session to use.
     * @return The mapped `GmailMessage`.
     * @throws IOException If an I/O error occurs.
     * @throws MessagingException If a messaging error occurs.
     */
    @Throws(IOException::class, MessagingException::class)
    fun mapMessageToGmailMessage(message: Message, session: Session): GmailMessage {
        val mimeMessage = mapMessageToMimeMessage(message, session)
        val outputStream = ByteArrayOutputStream()
        mimeMessage.writeTo(outputStream)
        val rawMessageBytes = outputStream.toByteArray()
        val encodedString = Base64.getEncoder().encodeToString(rawMessageBytes)

        val newEmail = GmailMessage()
        newEmail.raw = encodedString

        return newEmail
    }

    /**
     * Converts a `MimeMessage` to its raw string representation.
     *
     * @param email The `MimeMessage` to convert.
     * @return The string representation of the `MimeMessage`.
     * @throws IOException If an I/O error occurs.
     * @throws MessagingException If a messaging error occurs.
     */
    @Throws(IOException::class, MessagingException::class)
    private fun getRawData(email: MimeMessage): String {
        val outputStream = ByteArrayOutputStream()
        email.writeTo(outputStream)
        return outputStream.toString()
    }

    /**
     * Extracts content from a `MimeMultipart` object and sets it in the given `Message` object.
     *
     * @param multipart The `MimeMultipart` to extract content from.
     * @param message The `Message` object to set the extracted content.
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
            if ((disposition == null || disposition.equalsIgnoreCase(Part.ATTACHMENT))
                && bodyPart.fileName != null
            ) {
                AttachmentConverter.convert(bodyPart, message)
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
     * Adds text content and attachments to a `MimeMessage`.
     *
     * @param email The `MimeMessage` to add content and attachments.
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
            AttachmentConverter.convert(multipart, attachment)
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
     * Parses a string of email addresses into an array of `InternetAddress` objects.
     * 
     * @param addresses A string containing email addresses, separated by commas or semicolons.
     * @return An array of `InternetAddress` objects.
     * @throws AddressException If any of the addresses are invalid.
     * @throws UnsupportedEncodingException If the encoding is not supported.
     */
    @Throws(AddressException::class, UnsupportedEncodingException::class)
    private fun parse(addresses: String): Array<InternetAddress> {
        val encodedAddresses = StringBuilder()
        var addressesList = addresses.replace(";", ",")

        for (address in addressesList.split(",")) {
            val iaddr = InternetAddress(address.trim())
            iaddr.setPersonal(iaddr.personal, StandardCharsets.UTF_8.name())
            encodedAddresses.append(iaddr.toString()).append(",")
        }

        if (encodedAddresses.length > 0) {
            encodedAddresses.setLength(encodedAddresses.length - 1)
        }

        return InternetAddress.parse(encodedAddresses.toString(), true)
    }
}
