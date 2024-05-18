package com.github.kilamea.entity

/**
 * Represents a contact with an email address and a name.
 *
 * @since 0.1.0
 * @property email The email address of the contact.
 * @property firstName The first name of the contact.
 * @property lastName The last name of the contact.
 */
class Contact : AbstractEntity() {
    var email: String = ""
    var firstName: String = ""
    var lastName: String = ""

    /**
     * Returns the contact's full name and email address formatted as a string.
     *
     * @return The formatted string containing the full name and email address.
     */
    override fun toString(): String {
        return "$firstName $lastName <$email>"
    }
}
