package com.github.kilamea.core

import java.io.File

import com.github.kilamea.util.FileUtils

/**
 * Context class holding application arguments and providing paths to important files.
 *
 * @since 0.1.0
 * @property arguments The command line arguments passed to the application.
 * @property appDataFolder The directory for application data.
 * @property databaseFile The object representing the database file.
 * @property logFile The file to which log messages should be written.
 */
class Context(var arguments: Array<String>) {
    val appDataFolder: File = FileUtils.getAppDataFolder()
    val databaseFile: File = File(appDataFolder, Constants.DATABASE_FILE_NAME)
    val logFile: File = File(appDataFolder, Constants.LOG_FILE_NAME)

    /**
     * Checks if there are any command line arguments.
     * 
     * @return True if there are arguments, false otherwise.
     */
    fun hasArguments(): Boolean {
        return arguments.isNotEmpty()
    }
}
