package com.github.kilamea.util

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors

import com.github.kilamea.util.equalsIgnoreCase

/**
 * Utility class for various file-related operations.
 *
 * @since 0.1.0
 */
object FileUtils {
    /**
     * Gets the application data folder.
     *
     * @param startupFile The startup file. Defaults to the startup file of the application.
     * @return The application data folder as a File object.
     */
    fun getAppDataFolder(startupFile: File = getStartupFile()): File {
        return if (SystemUtils.isMac()) {
            startupFile.parentFile
        } else {
            val path = File(System.getenv("APPDATA"), getFilenameWithoutExtension(startupFile))
            if (!path.exists()) {
                path.mkdir()
            }
            path
        }
    }

    /**
     * Gets the extension of a file.
     *
     * @param file The file.
     * @return The file extension.
     */
    fun getExtension(file: File): String {
        return getExtension(file.name)
    }

    /**
     * Gets the extension of a filename.
     *
     * @param name The filename.
     * @return The file extension.
     */
    fun getExtension(name: String): String {
        val i = name.lastIndexOf(".")
        return if (i != -1) {
            name.substring(i)
        } else {
            ""
        }
    }

    /**
     * Gets the filename without its extension.
     *
     * @param file The file.
     * @return The filename without the extension.
     */
    fun getFilenameWithoutExtension(file: File): String {
        var name = file.name
        val i = name.lastIndexOf(".")
        if (i != -1) {
            name = name.substring(0, i)
        }
        return name
    }

    /**
     * Gets the startup file of the application.
     *
     * @return The startup file.
     * @throws UnsupportedEncodingException If the character encoding is not supported.
     */
    @Throws(UnsupportedEncodingException::class)
    fun getStartupFile(): File {
        val url = FileUtils::class.java.protectionDomain.codeSource.location.path
        var file = File(URLDecoder.decode(url, StandardCharsets.UTF_8.name()))

        if (SystemUtils.isMac()) {
            while (!hasExtension(file, ".app")) {
                file = file.parentFile
            }
            file = File(file, "Contents${File.separator}MacOS${File.separator}JavaAppLauncher")
        }

        return file
    }

    /**
     * Gets the user's home folder.
     *
     * @return The user's home folder.
     */
    fun getUserHomeFolder(): File {
        return File(System.getProperty("user.home"))
    }

    /**
     * Checks if a file has a specific extension.
     *
     * @param file The file.
     * @param ext The extension to check for.
     * @return True if the file has the specified extension, false otherwise.
     */
    fun hasExtension(file: File, ext: String): Boolean {
        return hasExtension(file.name, ext)
    }

    /**
     * Checks if a filename has a specific extension.
     *
     * @param name The filename.
     * @param ext The extension to check for.
     * @return True if the filename has the specified extension, false otherwise.
     */
    fun hasExtension(name: String, ext: String): Boolean {
        return getExtension(name).equalsIgnoreCase(ext)
    }

    /**
     * Reads all lines from a file.
     *
     * @param file The file to read.
     * @param charset The charset to use. Defaults to UTF-8.
     * @return The file content as a string.
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    fun readAllLines(file: File, charset: Charset = StandardCharsets.UTF_8): String {
        file.inputStream().use { inputStream ->
            return readAllLines(inputStream, charset)
        }
    }

    /**
     * Reads all lines from an input stream.
     *
     * @param inputStream The input stream to read.
     * @param charset The charset to use. Defaults to UTF-8.
     * @return The input stream content as a string.
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    fun readAllLines(inputStream: InputStream, charset: Charset = StandardCharsets.UTF_8): String {
        inputStream.bufferedReader(charset).use { reader ->
            return reader.lines().collect(Collectors.joining("\n"))
        }
    }

    /**
     * Shows the folder in the system's file explorer.
     *
     * @param path The path to the folder.
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    fun showFolder(path: String) {
        val runtime = Runtime.getRuntime()
        if (SystemUtils.isWindows()) {
            runtime.exec("explorer.exe \"$path\"")
        } else if (SystemUtils.isMac()) {
            runtime.exec("open $path")
        }
    }
}
