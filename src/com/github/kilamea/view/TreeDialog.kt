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

/**
 * Represents a dialog for displaying a tree structure with selectable items.
 * The items are populated from the provided bag.
 * 
 * @since 0.1.0
 * @property bag The bag containing the items to be displayed in the tree.
 */
internal class TreeDialog(parentShell: Shell, private val bag: Bag) :
    ModalDialog(parentShell, arrayOf("ok_button", "cancel_button")), IFormValidator {

    private lateinit var mailboxAccLabel: Label
    private lateinit var mailboxViewer: TreeViewer
    private var selectedFolder: Folder? = null

    /**
     * Configures the shell (window) settings for the dialog.
     * 
     * @param newShell The shell to configure.
     */
    override fun configureShell(newShell: Shell) {
        super.configureShell(newShell)
        newShell.text = I18n.getString("tree_window_title")
        newShell.addShellListener(object : ShellAdapter() {
            override fun shellActivated(event: ShellEvent) {
                mailboxViewer.tree.setFocus()
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

    /**
     * Handles the behavior when the OK button is pressed.
     * Closes the dialog if validation passes.
     */
    override fun okPressed() {
        if (validate()) {
            super.okPressed()
        }
    }

    /**
     * Validates the selected folder.
     * 
     * @return True if the selected folder is valid, false otherwise.
     */
    override fun validate(): Boolean {
        if (selectedFolder == null) {
            MessageDialog.openError(I18n.getString("tree_no_folder_error"))
            return false
        }

        return true
    }

    /**
     * Returns the selected folder.
     * 
     * @return The selected folder.
     */
    fun getSelectedFolder(): Folder {
        return selectedFolder!!
    }
}
