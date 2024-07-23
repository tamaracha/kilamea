package com.github.kilamea.mail

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Base64

import javax.activation.DataHandler
import javax.activation.DataSource
import javax.mail.BodyPart
import javax.mail.MessagingException
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMultipart
import javax.mail.internet.MimeUtility
import javax.mail.util.ByteArrayDataSource

import com.github.kilamea.entity.Attachment
import com.github.kilamea.entity.Message

/**
 * Utility class for converting files and email body parts to attachments and vice versa.
 *
 * @since 0.2.0
 */
object AttachmentConverter {
    /**
     * Converts a file to an attachment.
     * 
     * @param file The file to be converted.
     * @param message The message to which the attachment belongs.
     * @return The created attachment.
     * @throws IOException If an I/O error occurs during file reading.
     */
    @Throws(IOException::class)
    fun convert(file: File, message: Message): Attachment {
        val contentBytes = getContentBytes(file)
        return createAttachment(contentBytes, file.name, message)
    }

    /**
     * Converts a body part to an attachment.
     * 
     * @param bodyPart The email body part to be converted.
     * @param message The message to which the attachment belongs.
     * @return The created attachment.
     * @throws IOException If an I/O error occurs during reading the body part.
     * @throws MessagingException If an error occurs during the body part processing.
     */
    @Throws(IOException::class, MessagingException::class)
    fun convert(bodyPart: BodyPart, message: Message): Attachment {
        val contentBytes = getContentBytes(bodyPart)
        return createAttachment(contentBytes, MimeUtility.decodeText(bodyPart.fileName), message)
    }

    /**
     * Converts an attachment to a file.
     * 
     * @param file The file to be created from the attachment.
     * @param attachment The attachment to be converted.
     * @throws IOException If an I/O error occurs during file writing.
     */
    @Throws(IOException::class)
    fun convert(file: File, attachment: Attachment) {
        val decodedBytes = Base64.getDecoder().decode(attachment.content)
        FileOutputStream(file).use { outputStream ->
            outputStream.write(decodedBytes, 0, decodedBytes.size)
            outputStream.flush()
        }
    }

    /**
     * Converts an attachment to a body part.
     * 
     * @param multipart The MimeMultipart to which the attachment will be added.
     * @param attachment The attachment to be converted.
     * @throws IOException If an I/O error occurs during conversion.
     * @throws MessagingException If an error occurs during the creation of the body part.
     */
    @Throws(IOException::class, MessagingException::class)
    fun convert(multipart: MimeMultipart, attachment: Attachment) {
        val attachmentPart = MimeBodyPart()
        val decodedBytes = Base64.getDecoder().decode(attachment.content)
        val dataSource = ByteArrayDataSource(decodedBytes, MimeType.APPLICATION_OCTET_STREAM.toString())
        attachmentPart.dataHandler = DataHandler(dataSource)
        attachmentPart.fileName = attachment.fileName
        multipart.addBodyPart(attachmentPart)
    }

    /**
     * Creates an attachment from the provided content bytes.
     * 
     * @param contentBytes The content of the file or body part.
     * @param fileName The name of the file.
     * @param message The message to which the attachment belongs.
     * @return The created attachment.
     */
    private fun createAttachment(contentBytes: ByteArray, fileName: String, message: Message): Attachment {
        val base64Content = Base64.getEncoder().encodeToString(contentBytes)
        val attachment = Attachment().apply {
            content = base64Content
            this.fileName = fileName
            this.message = message
        }
        message.attachments.add(attachment)
        return attachment
    }

    /**
     * Reads the content of a file and returns it as a byte array.
     * 
     * @param file The file to read.
     * @return The content of the file as a byte array.
     * @throws IOException If an I/O error occurs while reading the file.
     */
    @Throws(IOException::class)
    private fun getContentBytes(file: File): ByteArray {
        FileInputStream(file).use { inputStream ->
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
     * Reads the content of a body part as a byte array.
     *
     * @param bodyPart The body part to read content from.
     * @return The content of the body part as a byte array.
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
}
