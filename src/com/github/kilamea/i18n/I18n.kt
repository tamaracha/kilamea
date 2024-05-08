package com.github.kilamea.i18n

import java.io.IOException
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

import com.github.kilamea.util.FileUtils
import com.github.kilamea.util.SystemUtils

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

    fun getLanguages(): Array<Language> {
        return languages
    }

    fun getString(key: String, defaultValue: String = key): String {
        return try {
            bundle?.getString(key) ?: defaultValue
        } catch (e: MissingResourceException) {
            defaultValue
        }
    }

    fun getSystemLocale(): Locale {
        var locale = Locale.getDefault()
        if (locale == Locale.GERMANY) {
            locale = Locale.GERMAN
        } else {
            locale = Locale.ENGLISH
        }
        return locale
    }

    fun loadBundle(index: Int) {
        if (bundle != null && languages[index].locale != bundle?.locale) {
            bundle = ResourceBundle.getBundle("Messages", languages[index].locale)
        }
    }

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
