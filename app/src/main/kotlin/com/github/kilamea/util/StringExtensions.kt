package com.github.kilamea.util

/**
 * Compares this string with the specified string, ignoring case considerations.
 *
 * @param other The string to compare this string with.
 * @return An integer value: 0 if the strings are equal ignoring case,
 *         a negative value if this string is less than the other string,
 *         a positive value if this string is greater than the other string.
 */
fun String.compareToIgnoreCase(other: String): Int {
    return this.compareTo(other, ignoreCase = true)
}

/**
 * Compares this string with the specified string, ignoring case considerations.
 *
 * @param other The string to compare this string with.
 * @return True if the specified string is equal to this string, ignoring case considerations, false otherwise.
 */
fun String.equalsIgnoreCase(other: String): Boolean {
    return this.equals(other, ignoreCase = true)
}
