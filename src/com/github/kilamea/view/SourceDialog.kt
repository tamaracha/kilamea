package com.github.kilamea.view

import org.eclipse.swt.SWT
import org.eclipse.swt.events.ShellAdapter
import org.eclipse.swt.events.ShellEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text

import com.github.kilamea.core.Constants
import com.github.kilamea.i18n.I18n
import com.github.kilamea.swt.Dimension
import com.github.kilamea.swt.ModalDialog

internal class SourceDialog(parentShell: Shell, private val rawData: String) :
    ModalDialog(parentShell, emptyArray<String>()) {

    private lateinit var sourceText: Text

    override fun configureShell(newShell: Shell) {
        super.configureShell(newShell)
        newShell.text = I18n.getString("source_window_title")
        newShell.addShellListener(object : ShellAdapter() {
            override fun shellActivated(event: ShellEvent) {
                sourceText.setFocus()
            }
        })
    }

    override fun createDialogArea(parent: Composite): Control {
        val container = super.createDialogArea(parent) as Composite
        container.layout = GridLayout(1, false)

        val sourceStyle = SWT.BORDER or SWT.H_SCROLL or SWT.MULTI or SWT.READ_ONLY or SWT.V_SCROLL
        sourceText = Text(container, sourceStyle)
        val textSize = Dimension.getTextSize(sourceText)
        val sourceGridData = GridData(SWT.LEFT, SWT.TOP, true, true)
        sourceGridData.heightHint = Constants.TEXT_AREA_ROWS * textSize.y
        sourceGridData.widthHint = Constants.TEXT_AREA_COLS * textSize.x
        sourceText.layoutData = sourceGridData
        sourceText.text = rawData

        return container
    }
}
