package com.github.kilamea.view

import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.ShellAdapter
import org.eclipse.swt.events.ShellEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Combo
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text

import com.github.kilamea.core.Bag
import com.github.kilamea.database.DatabaseManager
import com.github.kilamea.database.DBRuntimeException
import com.github.kilamea.entity.Contact
import com.github.kilamea.i18n.I18n
import com.github.kilamea.swt.MessageDialog
import com.github.kilamea.swt.ModalDialog
import com.github.kilamea.util.equalsIgnoreCase

/**
 * Represents a dialog for managing contacts, including adding, editing, and deleting contacts.
 * 
 * @since 0.1.0
 * @property bag The Bag object containing the contact information.
 * @property database The database manager for handling database operations.
 */
internal class ContactDialog(parentShell: Shell, private val bag: Bag, private val database: DatabaseManager) :
    ModalDialog(parentShell, emptyArray<String>()), IFormValidator {

    private lateinit var doneButton: Button
    private lateinit var newButton: Button
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button
    private lateinit var emailLabel: Label
    private lateinit var emailCombo: Combo
    private lateinit var firstNameLabel: Label
    private lateinit var firstNameText: Text
    private lateinit var lastNameLabel: Label
    private lateinit var lastNameText: Text

    private var contact: Contact? = null
    private var isNew: Boolean = false

    /**
     * Configures the shell (window) settings for the dialog.
     * 
     * @param newShell The shell to configure.
     */
    override fun configureShell(newShell: Shell) {
        super.configureShell(newShell)
        newShell.text = I18n.getString("contact_window_title")
        newShell.addShellListener(object : ShellAdapter() {
            override fun shellActivated(event: ShellEvent) {
                emailCombo.setFocus()
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

        val topComposite = Composite(container, SWT.BORDER)
        topComposite.layout = GridLayout(4, true)
        val topGridData = GridData(SWT.FILL, SWT.TOP, true, false)
        topGridData.horizontalSpan = 2
        topComposite.layoutData = topGridData

        doneButton = Button(topComposite, SWT.PUSH)
        doneButton.layoutData = GridData()
        doneButton.text = I18n.getString("contact_done_button")
        doneButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                close()
            }
        })

        newButton = Button(topComposite, SWT.PUSH)
        newButton.layoutData = GridData()
        newButton.text = I18n.getString("contact_new_button")
        newButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                newContact()
                emailCombo.setFocus()
            }
        })

        saveButton = Button(topComposite, SWT.PUSH)
        saveButton.layoutData = GridData()
        saveButton.text = I18n.getString("contact_save_button")
        saveButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                saveContact()
            }
        })

        deleteButton = Button(topComposite, SWT.PUSH)
        deleteButton.layoutData = GridData()
        deleteButton.text = I18n.getString("contact_delete_button")
        deleteButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                if (MessageDialog.openConfirm(I18n.getString("confirm_delete_contact")) == SWT.YES) {
                    deleteContact()
                    emailCombo.setFocus()
                }
            }
        })

        emailLabel = Label(container, SWT.NONE)
        emailLabel.layoutData = GridData()
        emailLabel.text = I18n.getString("contact_email_label")

        emailCombo = Combo(container, SWT.BORDER or SWT.SINGLE or SWT.V_SCROLL)
        emailCombo.layoutData = GridData(SWT.FILL, SWT.TOP, true, false)
        emailCombo.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                selectContact(emailCombo.selectionIndex)
            }
        })

        firstNameLabel = Label(container, SWT.NONE)
        firstNameLabel.layoutData = GridData()
        firstNameLabel.text = I18n.getString("contact_firstname_label")

        firstNameText = Text(container, SWT.BORDER or SWT.H_SCROLL or SWT.SINGLE)
        firstNameText.layoutData = GridData(SWT.FILL, SWT.TOP, true, false)

        lastNameLabel = Label(container, SWT.NONE)
        lastNameLabel.layoutData = GridData()
        lastNameLabel.text = I18n.getString("contact_lastname_label")

        lastNameText = Text(container, SWT.BORDER or SWT.H_SCROLL or SWT.SINGLE)
        lastNameText.layoutData = GridData(SWT.FILL, SWT.TOP, true, false)

        fillCombo()

        return container
    }

    /**
     * Validates the input fields to ensure they are not empty and that the email address is unique.
     * 
     * @return true if the validation passes, false otherwise.
     */
    override fun validate(): Boolean {
        var value = emailCombo.text.trim()
        emailCombo.text = value
        if (value.isEmpty()) {
            emailCombo.setFocus()
            MessageDialog.openError(I18n.getString("contact_no_email_error"))
            return false
        }

        bag.contacts.forEach {
            if (it != contact && it.email.equalsIgnoreCase(value)) {
                emailCombo.setFocus()
                MessageDialog.openError(I18n.getString("contact_exists_error"))
                return false
            }
        }

        value = firstNameText.text.trim()
        firstNameText.text = value
        if (value.isEmpty()) {
            firstNameText.setFocus()
            MessageDialog.openError(I18n.getString("contact_no_firstname_error"))
            return false
        }

        value = lastNameText.text.trim()
        lastNameText.text = value
        if (value.isEmpty()) {
            lastNameText.setFocus()
            MessageDialog.openError(I18n.getString("contact_no_lastname_error"))
            return false
        }

        return true
    }

    /**
     * Fills the email combo box with contact emails from the bag.
     */
    private fun fillCombo() {
        emailCombo.removeAll()
        var selectionIndex = -1

        for (element in bag.contacts) {
            emailCombo.add(element.email)
            if (selectionIndex == -1) {
                if (contact == null) {
                    selectionIndex = 0
                } else {
                    if (contact == element) {
                        selectionIndex = emailCombo.itemCount - 1
                    }
                }
            }
        }

        if (selectionIndex == -1) {
            newContact()
        } else {
            emailCombo.select(selectionIndex)
            selectContact(selectionIndex)
        }
    }

    /**
     * Selects a contact from the list based on the given index.
     * 
     * @param index The index of the contact to select.
     */
    private fun selectContact(index: Int) {
        isNew = false
        contact = bag.contacts[index]
        showContact()
        deleteButton.isEnabled = true
    }

    /**
     * Displays the selected contact's details in the form.
     */
    private fun showContact() {
        emailCombo.text = contact?.email ?: ""
        firstNameText.text = contact?.firstName ?: ""
        lastNameText.text = contact?.lastName ?: ""
    }

    /**
     * Creates a new contact and displays its details in the form.
     */
    private fun newContact() {
        isNew = true
        contact = Contact()
        contact?.let {
            bag.contacts.add(it)
            showContact()
            deleteButton.isEnabled = false
        }
    }

    /**
     * Saves the current contact details to the database.
     */
    private fun saveContact() {
        if (validate()) {
            contact?.apply {
                email = emailCombo.text
                firstName = firstNameText.text
                lastName = lastNameText.text
            }

            try {
                contact?.let {
                    if (isNew) {
                        database.addContact(it)
                    } else {
                        database.updateContact(it)
                    }
                    fillCombo()
                }
            } catch (e: DBRuntimeException) {
                MessageDialog.openError(e.message ?: "")
            }
        }
    }

    /**
     * Deletes the current contact from the database.
     */
    private fun deleteContact() {
        try {
            contact?.let {
                database.deleteContact(it)
                bag.contacts.remove(it)
                contact = null
                fillCombo()
            }
        } catch (e: DBRuntimeException) {
            MessageDialog.openError(e.message ?: "")
        }
    }
}
