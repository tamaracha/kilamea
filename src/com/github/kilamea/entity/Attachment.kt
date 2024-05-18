package com.github.kilamea.entity

/**
 * Represents an attachment with its details.
 *
 * @since 0.1.0
 * @property fileName The name of the attached file.
 * @property content The content of the attachment.
 * @property message The message to which this attachment belongs.
 */
class Attachment : AbstractEntity() {
    var fileName: String = ""
    var content: String = ""
    var message: Message? = null

    /**
     * Returns the file name of the attachment.
     *
     * @return The file name as a string.
     */
    override fun toString(): String {
        return fileName
    }
}
