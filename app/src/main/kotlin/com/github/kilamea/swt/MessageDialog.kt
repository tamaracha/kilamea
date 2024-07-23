package com.github.kilamea.swt

import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.MessageBox
import org.eclipse.swt.widgets.Shell

import com.github.kilamea.i18n.I18n

/**
 * Utility object for displaying various message dialogs.
 * 
 * @since 0.1.0
 */
object MessageDialog {
    /**
     * Opens a confirmation dialog with Yes and No options.
     * 
     * @param message The message to display in the dialog.
     * @return The user's response, either SWT.YES or SWT.NO.
     */
    fun openConfirm(message: String): Int {
        return showMessageDialog(I18n.getString("confirm_dialog_title", "Confirmation"), message, SWT.YES or SWT.NO or SWT.ICON_QUESTION)
    }

    /**
     * Opens a confirmation dialog with Yes, No, and Cancel options.
     * 
     * @param message The message to display in the dialog.
     * @return The user's response, either SWT.YES, SWT.NO, or SWT.CANCEL.
     */
    fun openConfirmWithCancel(message: String): Int {
        return showMessageDialog(I18n.getString("confirm_dialog_title", "Confirmation"), message, SWT.YES or SWT.NO or SWT.CANCEL or SWT.ICON_QUESTION)
    }

    /**
     * Opens an error dialog with an OK option.
     * 
     * @param message The message to display in the dialog.
     */
    fun openError(message: String) {
        showMessageDialog(I18n.getString("error_dialog_title", "Error"), message, SWT.OK or SWT.ICON_ERROR)
    }

    /**
     * Opens an information dialog with an OK option.
     * 
     * @param message The message to display in the dialog.
     */
    fun openInformation(message: String) {
        showMessageDialog(I18n.getString("info_dialog_title", "Information"), message, SWT.OK or SWT.ICON_INFORMATION)
    }

    /**
     * Opens a warning dialog with an OK option.
     * 
     * @param message The message to display in the dialog.
     */
    fun openWarning(message: String) {
        showMessageDialog(I18n.getString("warning_dialog_title", "Warning"), message, SWT.OK or SWT.ICON_WARNING)
    }

    /**
     * Retrieves the current shell or creates a new one if none is active.
     * 
     * @return The current or a new shell.
     */
    private fun getShell(): Shell {
        val display = Display.getCurrent() ?: Display.getDefault()
        var shell = display.activeShell
        if (shell == null) {
            shell = Shell()
        }
        return shell
    }

    /**
     * Shows a message dialog with the given title, message, and style.
     * 
     * @param title The title of the dialog.
     * @param message The message to display in the dialog.
     * @param style The style of the dialog (e.g., SWT.ICON_ERROR).
     * @return The user's response.
     */
    private fun showMessageDialog(title: String, message: String, style: Int): Int {
        val parent = getShell()
        val mb = MessageBox(parent, style)
        mb.message = message
        mb.text = title
        return mb.open()
    }
}
