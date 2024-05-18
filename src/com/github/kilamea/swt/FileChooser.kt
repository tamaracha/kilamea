package com.github.kilamea.swt

import java.io.File

import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.FileDialog
import org.eclipse.swt.widgets.Shell

/**
 * A class for handling file selection dialogs in an SWT application.
 * 
 * @since 0.1.0
 * @property parentShell The parent shell for the dialog.
 * @property checkExists Whether to check if the selected file exists.
 * @property checkOverwrite Whether to check for overwriting existing files.
 * @property fileName The name of the file selected.
 * @property filterExtensions The file extensions to filter by.
 * @property filterNames The names for the file filters.
 * @property message The message to display in dialogs.
 * @property text The text to display in the dialog title.
 */
class FileChooser(private val parentShell: Shell) {
    var checkExists: Boolean = true
    var checkOverwrite: Boolean = true
    var fileName: String? = null
    var filterExtensions: Array<String> = arrayOf("*.*")
    var filterNames: Array<String> = arrayOf("All files (*.*)")
    var message: String = ""
    var text: String = ""

    /**
     * Opens a file dialog for selecting a file to open.
     * 
     * @return The selected file name, or null if no file was selected.
     */
    fun openDialog(): String? {
        val openFileDialog = FileDialog(parentShell, SWT.OPEN)
        if (fileName != null) {
            openFileDialog.fileName = fileName
        }
        openFileDialog.filterExtensions = filterExtensions
        openFileDialog.filterNames = filterNames
        openFileDialog.text = text

        var completed: Boolean
        do {
            completed = true
            fileName = openFileDialog.open()
            if (fileName != null) {
                if (checkExists) {
                    val file = File(fileName)
                    if (!file.isFile || !file.exists()) {
                        MessageDialog.openError(String.format(message, fileName))
                        completed = false
                    }
                }
            }
        } while (!completed)

        return fileName
    }

    /**
     * Opens a file dialog for selecting a file to save.
     * 
     * @return The selected file name, or null if no file was selected.
     */
    fun saveDialog(): String? {
        val saveFileDialog = FileDialog(parentShell, SWT.SAVE)
        if (fileName != null) {
            saveFileDialog.fileName = fileName
        }
        saveFileDialog.filterExtensions = filterExtensions
        saveFileDialog.filterNames = filterNames
        saveFileDialog.text = text

        var completed: Boolean
        do {
            completed = true
            fileName = saveFileDialog.open()
            if (fileName != null) {
                if (checkOverwrite) {
                    val file = File(fileName)
                    if (file.isFile && file.exists()) {
                        if (MessageDialog.openConfirm(String.format(message, fileName)) == SWT.NO) {
                            completed = false
                        }
                    }
                }
            }
        } while (!completed)

        return fileName
    }
}
