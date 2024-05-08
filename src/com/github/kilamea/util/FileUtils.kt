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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.stream.Collectors

object FileUtils {
    private val INVALID_FILE_NAME_CHARS = charArrayOf('\\', '/', ':', '"', '<', '>', '|', '\b', '\u0000', '\t',
        '\u0010', '\u0011', '\u0012', '\u0014', '\u0015', '\u0016', '\u0017', '\u0018', '\u0019')

    fun deleteDirectoryOrFile(file: File): Boolean {
        if (!file.exists()) {
            return true
        }

        if (file.isDirectory) {
            for (entry in file.listFiles() ?: emptyArray()) {
                if (!deleteDirectoryOrFile(entry)) {
                    return false
                }
            }
        }

        return file.delete()
    }

    fun formatFileSize(value: Long): String {
        return when {
            value >= 1024L && value < 1048576L -> String.format("%.1f KB", value / 1024.0)
            value > 1048576L -> String.format("%.1f MB", value / 1048576.0)
            else -> "$value Bytes"
        }
    }

    fun formatLastModified(file: File): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        return sdf.format(Date(file.lastModified()))
    }

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

    fun getDirectoryOrFileSize(file: File, recursive: Boolean): Long {
        var size = 0L

        if (file.isDirectory) {
            for (entry in file.listFiles() ?: emptyArray()) {
                if (entry.isDirectory) {
                    if (recursive) {
                        size += getDirectoryOrFileSize(entry, recursive)
                    }
                } else {
                    size += entry.length()
                }
            }
        } else {
            size = file.length()
        }

        return size
    }

    fun getExtension(file: File): String {
        return getExtension(file.name)
    }

    fun getExtension(name: String): String {
        val i = name.lastIndexOf(".")
        return if (i != -1) {
            name.substring(i)
        } else {
            ""
        }
    }

    fun getFilenameWithoutExtension(file: File): String {
        var name = file.name
        val i = name.lastIndexOf(".")
        if (i != -1) {
            name = name.substring(0, i)
        }
        return name
    }

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

    fun getUserHomeFolder(): File {
        return File(System.getProperty("user.home"))
    }

    fun hasExtension(file: File, ext: String): Boolean {
        return hasExtension(file.name, ext)
    }

    fun hasExtension(name: String, ext: String): Boolean {
        return getExtension(name).equals(ext, ignoreCase = true)
    }

    fun isEmptyDirectory(file: File): Boolean {
        return file.isDirectory && getDirectoryOrFileSize(file, true) == 0L
    }

    fun isValidFilename(value: String): String {
        var badCharacters = ""

        for (i in 0 until value.length) {
            val c = value[i]
            for (element in INVALID_FILE_NAME_CHARS) {
                if (c == element) {
                    badCharacters += c
                    break
                }
            }
        }

        return badCharacters
    }

    @Throws(IOException::class)
    fun readAllLines(file: File, charset: Charset = StandardCharsets.UTF_8): String {
        file.inputStream().use { inputStream ->
            return readAllLines(inputStream, charset)
        }
    }

    @Throws(IOException::class)
    fun readAllLines(inputStream: InputStream, charset: Charset = StandardCharsets.UTF_8): String {
        inputStream.bufferedReader(charset).use { reader ->
            return reader.lines().collect(Collectors.joining("\n"))
        }
    }

    fun removeInvalidChars(value: String): String {
        var newValue = ""

        for (i in 0 until value.length) {
            val c = value[i]
            var matches = false
            for (element in INVALID_FILE_NAME_CHARS) {
                if (c == element) {
                    matches = true
                    break
                }
            }
            if (!matches) {
                newValue += c
            }
        }

        return newValue
    }

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
