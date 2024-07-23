package com.github.kilamea.view

import org.eclipse.swt.SWT
import org.eclipse.swt.events.ShellAdapter
import org.eclipse.swt.events.ShellEvent
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text

import com.github.kilamea.core.Constants
import com.github.kilamea.entity.Folder
import com.github.kilamea.entity.FolderList
import com.github.kilamea.i18n.I18n
import com.github.kilamea.swt.Dimension
import com.github.kilamea.swt.MessageDialog
import com.github.kilamea.swt.ModalDialog
import com.github.kilamea.util.equalsIgnoreCase

/**
 * Represents a dialog for managing folders.
 * 
 * @since 0.1.0
 * @property folders The list of folders.
 * @property folder The folder being edited.
 */
internal class FolderDialog(parentShell: Shell, private val folders: FolderList, private val folder: Folder) :
    ModalDialog(parentShell, arrayOf("ok_button", "cancel_button")), IFormValidator {

    private lateinit var nameLabel: Label
    private lateinit var nameText: Text

    /**
     * Configures the shell (window) settings for the dialog.
     * 
     * @param newShell The shell to configure.
     */
    override fun configureShell(newShell: Shell) {
        super.configureShell(newShell)
        newShell.text = I18n.getString("folder_window_title")
        newShell.addShellListener(object : ShellAdapter() {
            override fun shellActivated(event: ShellEvent) {
                nameText.setFocus()
            }
        })
    }

    /**
     * Creates the main content area of the dialog.
     * 
     * @param parent The parent composite in which the dialog area is created.
     * @return The control representing the dialog area.
     */
    override fun createDialogArea(parent: Composite): Control {
        val container = super.createDialogArea(parent) as Composite
        val gridLayout = container.layout as GridLayout
        gridLayout.numColumns = 2

        nameLabel = Label(container, SWT.NONE)
        nameLabel.layoutData = GridData()
        nameLabel.text = I18n.getString("folder_name_label")

        nameText = Text(container, SWT.BORDER or SWT.H_SCROLL or SWT.SINGLE)
        val textSize = Dimension.getTextSize(nameText)
        val nameGridData = GridData(SWT.FILL, SWT.TOP, true, false)
        nameGridData.widthHint = Constants.TEXT_FIELD_COLS * textSize.x
        nameText.layoutData = nameGridData
        nameText.text = folder.name
        nameText.selectAll()

        return container
    }

    /**
     * Handles the behavior when the OK button is pressed.
     * Closes the dialog if validation passes.
     */
    override fun okPressed() {
        if (validate()) {
            folder.name = nameText.text
            super.okPressed()
        }
    }

    /**
     * Validates the form inputs.
     * 
     * @return True if the form inputs are valid, false otherwise.
     */
    override fun validate(): Boolean {
        var value = nameText.text.trim()
        nameText.text = value
        if (value.isEmpty()) {
            nameText.setFocus()
            MessageDialog.openError(I18n.getString("folder_no_name_error"))
            return false
        }

        folders.forEach {
            if (it != folder && it.name.equalsIgnoreCase(value)) {
                nameText.setFocus()
                MessageDialog.openError(I18n.getString("folder_exists_error"))
                return false
            }
        }

        return true
    }
}
