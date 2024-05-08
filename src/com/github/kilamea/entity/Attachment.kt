package com.github.kilamea.entity

class Attachment : AbstractEntity() {
    var fileName: String = ""
    var content: String = ""
    var message: Message? = null

    override fun toString(): String {
        return fileName
    }
}
