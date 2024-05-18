package com.github.kilamea.view

/**
 * Represents an interface for form validation.
 * 
 * @since 0.1.0
 */
internal interface IFormValidator {
    /**
     * Validates the form.
     * 
     * @return True if the form is valid, false otherwise.
     */
    fun validate(): Boolean
}
