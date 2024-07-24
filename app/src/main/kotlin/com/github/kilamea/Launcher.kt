package com.github.kilamea

import java.io.IOException
import java.nio.file.Path
import java.util.logging.ConsoleHandler
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

import com.github.kilamea.core.Context
import com.github.kilamea.view.Kilamea

/**
 * An object containing the main functionality for launching the application.
 *
 * @since 0.1.0
 */
object Launcher {
    private val logger = Logger.getLogger(Launcher::class.java.name)

    /**
     * Main entry point of the application.
     *
     * @param args Command line arguments passed to the application.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val context = Context(args)
        try {
            initLogging(context.logFile)
        } catch (e: IOException) {
            System.err.println("Failed to initialize logging: ${e.message}")
            e.printStackTrace(System.err)
            return
        }

        logger.info("Starting application...")
        val app = Kilamea(context)
        app.run()
        logger.info("Application has finished running.")
    }

    /**
     * Initializes logging for the application.
     *
     * @param logFile The file to which log messages should be written.
     * @throws IOException If logging cannot be initialized.
     */
    @Throws(IOException::class)
    private fun initLogging(logFile: Path) {
        val rootLogger = Logger.getLogger("")
        rootLogger.handlers.forEach { rootLogger.removeHandler(it) }

        val fileHandler = FileHandler(logFile.toString(), false).apply {
            formatter = SimpleFormatter()
        }
        rootLogger.addHandler(fileHandler)

        val consoleHandler = ConsoleHandler().apply {
            formatter = SimpleFormatter()
        }
        rootLogger.addHandler(consoleHandler)

        rootLogger.level = Level.INFO
    }
}
