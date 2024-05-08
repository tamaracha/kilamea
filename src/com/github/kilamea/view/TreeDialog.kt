package com.github.kilamea.view

import org.eclipse.jface.viewers.ISelectionChangedListener
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.jface.viewers.LabelProvider
import org.eclipse.jface.viewers.SelectionChangedEvent
import org.eclipse.jface.viewers.StructuredSelection
import org.eclipse.jface.viewers.TreeViewer
import org.eclipse.swt.SWT
import org.eclipse.swt.events.ShellAdapter
import org.eclipse.swt.events.ShellEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Tree

import com.github.kilamea.core.Bag
import com.github.kilamea.entity.Folder
import com.github.kilamea.i18n.I18n
import com.github.kilamea.swt.MessageDialog
import com.github.kilamea.swt.ModalDialog

internal class TreeDialog(parentShell: Shell, private val bag: Bag) :
    ModalDialog(parentShell, arrayOf("ok_button", "cancel_button")), IFormValidator {

    private lateinit var mailboxAccLabel: Label
    private lateinit var mailboxViewer: TreeViewer
    private var selectedFolder: Folder? = null

    override fun configureShell(newShell: Shell) {
        super.configureShell(newShell)
        newShell.text = I18n.getString("tree_window_title")
        newShell.addShellListener(object : ShellAdapter() {
            override fun shellActivated(event: ShellEvent) {
                mailboxViewer.tree.setFocus()
            }
        })
    }

    override fun createDialogArea(parent: Composite): Control {
        val container = super.createDialogArea(parent) as Composite

        mailboxAccLabel = Label(container, SWT.NONE)
        val mailboxLabelGridData = GridData()
        mailboxLabelGridData.exclude = true
        mailboxAccLabel.layoutData = mailboxLabelGridData
        mailboxAccLabel.text = I18n.getString("mailbox_acclabel")
        mailboxAccLabel.isVisible = false

        mailboxViewer = TreeViewer(container, SWT.BORDER or SWT.FULL_SELECTION or SWT.SINGLE or SWT.V_SCROLL)
        mailboxViewer.setContentProvider(MailboxViewerContentProvider())
        mailboxViewer.setLabelProvider(object : LabelProvider() {
            override fun getText(element: Any): String {
                return element.toString()
            }
        })
        mailboxViewer.addSelectionChangedListener(object : ISelectionChangedListener {
            override fun selectionChanged(event: SelectionChangedEvent) {
                if (!mailboxViewer.tree.isDisposed) {
                    val selection = event.selection as IStructuredSelection
                    selectedFolder = if (!selection.isEmpty && selection.firstElement is Folder) {
                        selection.firstElement as Folder
                    } else {
                        null
                    }
                }
            }
        })

        val tree = mailboxViewer.tree
        tree.headerVisible = false
        tree.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        tree.linesVisible = true

        mailboxViewer.input = bag
        mailboxViewer.setSelection(StructuredSelection.EMPTY, false)

        return container
    }

    override fun okPressed() {
        if (validate()) {
            super.okPressed()
        }
    }

    override fun validate(): Boolean {
        if (selectedFolder == null) {
            MessageDialog.openError(I18n.getString("tree_no_folder_error"))
            return false
        }

        return true
    }

    fun getSelectedFolder(): Folder {
        return selectedFolder!!
    }
}
