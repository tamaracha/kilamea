package com.github.kilamea.core

import java.io.File

import com.github.kilamea.util.FileUtils

class Context(private val arguments: Array<String>) {
    val databaseFile: File = File(FileUtils.getAppDataFolder(), Constants.DATABASE_FILE_NAME)
}
