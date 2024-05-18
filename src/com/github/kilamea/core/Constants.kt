package com.github.kilamea.core

/**
 * Interface containing application constants.
 *
 * @since 0.1.0
 */
interface Constants {
    companion object {
        const val APP_NAME: String = "Kilamea"
        const val APP_VERSION: String = "0.1.0"
        const val DATABASE_DRIVER_CLASS: String = "org.sqlite.JDBC"
        const val DATABASE_DUMP_FILE: String = "/Dump.sql"
        const val DATABASE_FILE_NAME: String = "Kilamea.db"
        const val DATABASE_JDBC_SCHEME: String = "jdbc:sqlite:/"
        const val MESSAGE_COUNT: Int = 50
        const val TEXT_AREA_COLS: Int = 100
        const val TEXT_AREA_ROWS: Int = 20
        const val TEXT_FIELD_COLS: Int = 20
    }
}
