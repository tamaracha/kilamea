package com.github.kilamea.swt

import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.MessageBox
import org.eclipse.swt.widgets.Shell

import com.github.kilamea.i18n.I18n

object MessageDialog {
    fun openConfirm(message: String): Int {
        return showMessageDialog(I18n.getString("confirm_dialog_title", "Confirmation"), message, SWT.YES or SWT.NO or SWT.ICON_QUESTION)
    }

    fun openConfirmWithCancel(message: String): Int {
        return showMessageDialog(I18n.getString("confirm_dialog_title", "Confirmation"), message, SWT.YES or SWT.NO or SWT.CANCEL or SWT.ICON_QUESTION)
    }

    fun openError(message: String) {
        showMessageDialog(I18n.getString("error_dialog_title", "Error"), message, SWT.OK or SWT.ICON_ERROR)
    }

    fun openInformation(message: String) {
        showMessageDialog(I18n.getString("info_dialog_title", "Information"), message, SWT.OK or SWT.ICON_INFORMATION)
    }

    fun openWarning(message: String) {
        showMessageDialog(I18n.getString("warning_dialog_title", "Warning"), message, SWT.OK or SWT.ICON_WARNING)
    }

    private fun getShell(): Shell {
        val display = Display.getCurrent() ?: Display.getDefault()
        var shell = display.activeShell
        if (shell == null) {
            shell = Shell()
        }
        return shell
    }

    private fun showMessageDialog(title: String, message: String, style: Int): Int {
        val parent = getShell()
        val mb = MessageBox(parent, style)
        mb.message = message
        mb.text = title
        return mb.open()
    }
}
