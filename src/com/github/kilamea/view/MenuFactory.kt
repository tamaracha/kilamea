package com.github.kilamea.view

import org.eclipse.swt.SWT

import com.github.kilamea.i18n.I18n
import com.github.kilamea.util.SystemUtils

object MenuFactory {
    private val data = HashMap<String, Pair<Int, String>>()

    init {
        var key = SWT.CTRL
        var label = I18n.getString("shortcut_control")
        if (SystemUtils.isMac()) {
            key = SWT.COMMAND
            label = I18n.getString("shortcut_command")
        }

        putEntry("file_open_menu", key or 'O'.code, "$label+O")
        putEntry("file_save_as_menu", key or 'S'.code, "$label+S")
        putEntry("message_new_menu", key or 'N'.code, "$label+N")
        putEntry("message_reply_menu", key or 'R'.code, "$label+R")
        putEntry("message_reply_all_menu", key or SWT.ALT or 'R'.code, "$label+Alt+R")
        putEntry("message_forward_menu", key or 'F'.code, "$label+F")
    }

    fun getText(id: String): String {
        return data[id]?.second ?: I18n.getString(id)
    }

    fun getAccelerator(id: String): Int {
        return data[id]?.first ?: SWT.NONE
    }

    private fun putEntry(id: String, code: Int, text: String) {
        data[id] = Pair(code, "${I18n.getString(id)}\t$text")
    }
}
