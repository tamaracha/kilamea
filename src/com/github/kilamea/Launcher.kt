package com.github.kilamea

import com.github.kilamea.core.Context
import com.github.kilamea.view.Kilamea

/**
 * Main launcher class for the Kilamea application.
 *
 * @since 0.1.0
 */
object Launcher {
    /**
     * Main entry point of the application.
     *
     * @param args Command-line arguments passed to the application.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val app = Kilamea(Context(args))
        app.run()
    }
}
