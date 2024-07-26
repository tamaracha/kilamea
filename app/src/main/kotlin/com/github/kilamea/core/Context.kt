package com.github.kilamea.core

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import net.harawata.appdirs.AppDirs
import net.harawata.appdirs.AppDirsFactory

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
    private val appDirs: AppDirs = AppDirsFactory.getInstance()
    val appDataFolder: Path = Paths.get(appDirs.getUserDataDir(Constants.APP_NAME, null, null)).apply { createDirectories() }
    private val logFolder: Path = Paths.get(appDirs.getUserLogDir(Constants.APP_NAME, null, null)).apply { createDirectories() }
    val databaseFile: Path = appDataFolder.resolve(Constants.DATABASE_FILE_NAME)
    val logFile: Path = logFolder.resolve(Constants.LOG_FILE_NAME)

    /**
     * Checks if there are any command line arguments.
     * 
     * @return True if there are arguments, false otherwise.
     */
    fun hasArguments(): Boolean {
        return arguments.isNotEmpty()
    }
}
