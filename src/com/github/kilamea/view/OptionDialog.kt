package com.github.kilamea.view

import org.eclipse.swt.SWT
import org.eclipse.swt.events.ShellAdapter
import org.eclipse.swt.events.ShellEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Shell

import com.github.kilamea.core.Options
import com.github.kilamea.i18n.I18n
import com.github.kilamea.swt.ModalDialog

/**
 * Represents a dialog for configuring options.
 * 
 * @since 0.1.0
 * @property options The options to be configured.
 */
internal class OptionDialog(parentShell: Shell, private val options: Options) :
    ModalDialog(parentShell, arrayOf("ok_button", "cancel_button")) {

    private lateinit var retrieveOnStartCheck: Button
    private lateinit var deleteFromServerCheck: Button

    /**
     * Configures the shell (window) settings for the dialog.
     * 
     * @param newShell The shell to configure.
     */
    override fun configureShell(newShell: Shell) {
        super.configureShell(newShell)
        newShell.text = I18n.getString("option_window_title")
        newShell.addShellListener(object : ShellAdapter() {
            override fun shellActivated(event: ShellEvent) {
                retrieveOnStartCheck.setFocus()
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

        retrieveOnStartCheck = Button(container, SWT.CHECK)
        retrieveOnStartCheck.layoutData = GridData()
        retrieveOnStartCheck.selection = options.retrieveOnStart
        retrieveOnStartCheck.text = I18n.getString("option_retrieve_on_start_check")

        deleteFromServerCheck = Button(container, SWT.CHECK)
        deleteFromServerCheck.layoutData = GridData()
        deleteFromServerCheck.selection = options.deleteFromServer
        deleteFromServerCheck.text = I18n.getString("option_delete_from_server_check")

        return container
    }

    /**
     * Handles the behavior when the OK button is pressed.
     * It updates the options based on the selection of the checkboxes and closes the dialog.
     */
    override fun okPressed() {
        options.retrieveOnStart = retrieveOnStartCheck.selection
        options.deleteFromServer = deleteFromServerCheck.selection
        super.okPressed()
    }
}
