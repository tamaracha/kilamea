package com.github.kilamea.swt

import java.io.File

import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.FileDialog
import org.eclipse.swt.widgets.Shell

class FileChooser(private val parentShell: Shell) {
    var checkExists: Boolean = true
    var checkOverwrite: Boolean = true
    var fileName: String? = null
    var filterExtensions: Array<String> = arrayOf("*.*")
    var filterNames: Array<String> = arrayOf("All files (*.*)")
    var message: String = ""
    var text: String = ""

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
