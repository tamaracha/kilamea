package com.github.kilamea.core

import java.io.File

import com.github.kilamea.util.FileUtils

/**
 * Holds the context for the application, including command-line arguments and the database file location.
 *
 * @since 0.1.0
 * @property arguments The command-line arguments passed to the application.
 * @property databaseFile The file object representing the database file.
 */
class Context(private val arguments: Array<String>) {
    val databaseFile: File = File(FileUtils.getAppDataFolder(), Constants.DATABASE_FILE_NAME)
}
