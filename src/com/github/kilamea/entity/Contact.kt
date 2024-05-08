package com.github.kilamea.entity

class Contact : AbstractEntity() {
    var email: String = ""
    var firstName: String = ""
    var lastName: String = ""

    override fun toString(): String {
        return "$firstName $lastName <$email>"
    }
}
