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

internal class SearchDialog(parentShell: Shell, private val filter: ListFilter) :
    ModalDialog(parentShell, arrayOf("ok_button", "cancel_button")) {

    private lateinit var promptLabel: Label
    private lateinit var textField: Text
    private lateinit var matchCaseCheck: Button

    override fun configureShell(newShell: Shell) {
        super.configureShell(newShell)
        newShell.text = I18n.getString("search_window_title")
        newShell.addShellListener(object : ShellAdapter() {
            override fun shellActivated(event: ShellEvent) {
                textField.setFocus()
            }
        })
    }

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

    override fun okPressed() {
        filter.findText = textField.text.trim()
        filter.matchCase = matchCaseCheck.selection
        super.okPressed()
    }
}
