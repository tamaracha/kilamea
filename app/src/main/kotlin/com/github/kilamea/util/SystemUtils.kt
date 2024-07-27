package com.github.kilamea.util

import java.io.IOException

/**
 * Utility class for system-related functions.
 *
 * @since 0.1.0
 * @property LINE_BREAK The line break string for the current operating system.
 */
object SystemUtils {
    val LINE_BREAK: String = System.lineSeparator()

    /**
     * Gets the OS architecture.
     *
     * @return The OS architecture.
     */
    fun getOSArch(): String = System.getProperty("os.arch")

    /**
     * Gets the OS name.
     *
     * @return The OS name.
     */
    fun getOSName(): String = System.getProperty("os.name")

    /**
     * Gets the OS version.
     *
     * @return The OS version.
     */
    fun getOSVersion(): String = System.getProperty("os.version")

    /**
     * Gets the user's home directory.
     *
     * @return The user's home directory.
     */
    fun getUserHomeDir(): String = System.getProperty("user.home")

    /**
     * Checks if the OS is macOS.
     *
     * @return True if the OS is macOS, false otherwise.
     */
    fun isMac(): Boolean {
        val osName = getOSName().lowercase()
        return osName.startsWith("mac os x")
    }

    /**
     * Checks if the OS is Windows.
     *
     * @return True if the OS is Windows, false otherwise.
     */
    fun isWindows(): Boolean {
        val osName = getOSName().lowercase()
        return osName.startsWith("windows")
    }

    /**
     * Opens the specified URL in the default browser.
     *
     * @param url The URL to open.
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    fun openUrl(url: String) {
        val runtime = Runtime.getRuntime()

        if (isWindows()) {
            runtime.exec("rundll32.exe url.dll,FileProtocolHandler $url")
        } else if (isMac()) {
            runtime.exec("open $url")
        } else {
            val browsers = arrayOf("epiphany", "firefox", "mozilla", "konqueror", "netscape", "opera", "links", "lynx")
            val cmd = StringBuilder()
            for (i in browsers.indices) {
                cmd.append((if (i == 0) "" else " || ") + browsers[i] + " \"$url\" ")
            }
            runtime.exec(arrayOf("sh", "-c", cmd.toString()))
        }
    }
}
