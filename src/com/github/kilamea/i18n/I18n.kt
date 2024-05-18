package com.github.kilamea.i18n

import java.io.IOException
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

import com.github.kilamea.util.FileUtils
import com.github.kilamea.util.SystemUtils

/**
 * Provides internationalization support for the application.
 *
 * @since 0.1.0
 */
object I18n {
    private var bundle: ResourceBundle? = null

    private val languages = arrayOf(
        Language("lang_german", Locale.GERMAN),
        Language("lang_english", Locale.ENGLISH)
    )

    init {
        try {
            bundle = ResourceBundle.getBundle("Messages", getSystemLocale())
        } catch (e: MissingResourceException) {
            bundle = null
        }
    }

    /**
     * Retrieves the index of the currently selected language.
     *
     * @return The index of the currently selected language.
     */
    fun getCurrentLanguageIndex(): Int {
        if (bundle != null) {
            for (i in languages.indices) {
                if (languages[i].locale == bundle?.locale) {
                    return i
                }
            }
        }

        return -1
    }

    /**
     * Retrieves the array of available languages.
     *
     * @return An array of available languages.
     */
    fun getLanguages(): Array<Language> {
        return languages
    }

    /**
     * Retrieves the localized string for the given key, with a default value if not found.
     *
     * @param key The key for the localized string.
     * @param defaultValue The default value to return if the key is not found.
     * @return The localized string for the given key, or the default value if not found.
     */
    fun getString(key: String, defaultValue: String = key): String {
        return try {
            bundle?.getString(key) ?: defaultValue
        } catch (e: MissingResourceException) {
            defaultValue
        }
    }

    /**
     * Retrieves the system locale.
     *
     * @return The system locale.
     */
    fun getSystemLocale(): Locale {
        var locale = Locale.getDefault()
        if (locale == Locale.GERMANY) {
            locale = Locale.GERMAN
        } else {
            locale = Locale.ENGLISH
        }
        return locale
    }

    /**
     * Loads the resource bundle for the specified language index.
     *
     * @param index The index of the language to load.
     */
    fun loadBundle(index: Int) {
        if (bundle != null && languages[index].locale != bundle?.locale) {
            bundle = ResourceBundle.getBundle("Messages", languages[index].locale)
        }
    }

    /**
     * Loads the content of a resource file with optional placeholder replacement.
     *
     * @param name The name of the resource file.
     * @param placeholders The map of placeholders to replace in the resource content.
     * @return The content of the resource file with placeholders replaced.
     */
    fun loadResource(name: String, placeholders: Map<String, String> = emptyMap()): String {
        var content = ""

        if (bundle != null) {
            val resource = String.format(name, bundle?.locale?.toString())
            I18n::class.java.getResourceAsStream(resource)?.use { inputStream ->
                try {
                    content = FileUtils.readAllLines(inputStream)
                } catch (e: IOException) {
                }
            }

            placeholders.forEach { (key, value) ->
                content = content.replaceFirst(key.toRegex(), value)
            }
        }

        return content
    }
}
