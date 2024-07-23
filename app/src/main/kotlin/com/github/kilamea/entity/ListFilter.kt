package com.github.kilamea.entity

/**
 * Represents a filter for searching through lists with optional case sensitivity.
 *
 * @since 0.1.0
 * @property findText The text to find.
 * @property matchCase Indicates if the search should be case-sensitive.
 */
data class ListFilter(var findText: String = "", var matchCase: Boolean = false)
