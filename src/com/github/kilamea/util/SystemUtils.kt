package com.github.kilamea.util

import java.io.IOException

object SystemUtils {
    val LINE_BREAK: String = System.getProperty("line.separator")

    fun getOSArch(): String = System.getProperty("os.arch")

    fun getOSName(): String = System.getProperty("os.name")

    fun getOSVersion(): String = System.getProperty("os.version")

    fun getUserHomeDir(): String = System.getProperty("user.home")

    fun isMac(): Boolean {
        val osName = getOSName().lowercase()
        return osName.startsWith("mac os x")
    }

    fun isWindows(): Boolean {
        val osName = getOSName().lowercase()
        return osName.startsWith("windows")
    }

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

    @Throws(IOException::class)
    fun shutdown() {
        val runtime = Runtime.getRuntime()

        if (isWindows()) {
            runtime.exec("shutdown.exe -s -t 0")
        } else {
            runtime.exec("shutdown -h now")
        }
    }
}
