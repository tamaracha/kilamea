package com.github.kilamea.view

import org.eclipse.swt.SWT
import org.eclipse.swt.events.ShellAdapter
import org.eclipse.swt.events.ShellEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text

import com.github.kilamea.entity.ListFilter
import com.github.kilamea.i18n.I18n
import com.github.kilamea.swt.ModalDialog

/**
 * Represents a dialog for searching items based on provided criteria.
 * 
 * @since 0.1.0
 * @property filter The filter containing search criteria.
 */
internal class SearchDialog(parentShell: Shell, private val filter: ListFilter) :
    ModalDialog(parentShell, arrayOf("ok_button", "cancel_button")) {

    private lateinit var promptLabel: Label
    private lateinit var textField: Text
    private lateinit var matchCaseCheck: Button

    /**
     * Configures the shell (window) settings for the dialog.
     * 
     * @param newShell The shell to configure.
     */
    override fun configureShell(newShell: Shell) {
        super.configureShell(newShell)
        newShell.text = I18n.getString("search_window_title")
        newShell.addShellListener(object : ShellAdapter() {
            override fun shellActivated(event: ShellEvent) {
                textField.setFocus()
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

        promptLabel = Label(container, SWT.NONE)
        promptLabel.layoutData = GridData()
        promptLabel.text = I18n.getString("search_prompt_label")

        textField = Text(container, SWT.BORDER or SWT.H_SCROLL or SWT.SINGLE)
        textField.layoutData = GridData(SWT.FILL, SWT.TOP, true, false)
        textField.text = filter.findText
        textField.selectAll()

        matchCaseCheck = Button(container, SWT.CHECK)
        matchCaseCheck.layoutData = GridData()
        matchCaseCheck.setSelection(filter.matchCase)
        matchCaseCheck.text = I18n.getString("search_match_case_check")

        return container
    }

    /**
     * Handles the behavior when the OK button is pressed.
     * Updates the filter criteria based on user input and closes the dialog.
     */
    override fun okPressed() {
        filter.findText = textField.text.trim()
        filter.matchCase = matchCaseCheck.selection
        super.okPressed()
    }
}
