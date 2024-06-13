package com.github.kilamea.view

import org.eclipse.swt.SWT
import org.eclipse.swt.events.FocusAdapter
import org.eclipse.swt.events.FocusEvent
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
import org.eclipse.swt.widgets.List
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text

import com.github.kilamea.core.Bag
import com.github.kilamea.database.DatabaseManager
import com.github.kilamea.database.DBRuntimeException
import com.github.kilamea.entity.Account
import com.github.kilamea.i18n.I18n
import com.github.kilamea.mail.AuthException
import com.github.kilamea.mail.GmailClient
import com.github.kilamea.mail.MailProtocol
import com.github.kilamea.swt.MessageDialog
import com.github.kilamea.swt.ModalDialog
import com.github.kilamea.swt.NumberValidator
import com.github.kilamea.util.equalsIgnoreCase

/**
 * Represents a dialog for managing email accounts, including adding, editing, and deleting email accounts.
 * 
 * @since 0.1.0
 * @property bag The Bag object containing the account information.
 * @property database The database manager for handling database operations.
 */
internal class AccountDialog(parentShell: Shell, private val bag: Bag, private val database: DatabaseManager) :
    ModalDialog(parentShell, emptyArray<String>()), IFormValidator {

    private lateinit var doneButton: Button
    private lateinit var newButton: Button
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button
    private lateinit var emailLabel: Label
    private lateinit var emailCombo: Combo
    private lateinit var nameLabel: Label
    private lateinit var nameText: Text
    private lateinit var userLabel: Label
    private lateinit var userText: Text
    private lateinit var passwordLabel: Label
    private lateinit var passwordText: Text
    private lateinit var protocolLabel: Label
    private lateinit var protocolList: List
    private lateinit var sslActiveCheck: Button
    private lateinit var incomingHostLabel: Label
    private lateinit var incomingHostText: Text
    private lateinit var incomingPortLabel: Label
    private lateinit var incomingPortText: Text
    private lateinit var outgoingHostLabel: Label
    private lateinit var outgoingHostText: Text
    private lateinit var outgoingPortLabel: Label
    private lateinit var outgoingPortText: Text

    private var account: Account? = null
    private var isNew: Boolean = false

    /**
     * Configures the shell (window) settings for the dialog.
     * 
     * @param newShell The shell to configure.
     */
    override fun configureShell(newShell: Shell) {
        super.configureShell(newShell)
        newShell.text = I18n.getString("account_window_title")
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
        gridLayout.numColumns = 3

        val topComposite = Composite(container, SWT.BORDER)
        topComposite.layout = GridLayout(4, true)
        val topGridData = GridData(SWT.FILL, SWT.TOP, true, false)
        topGridData.horizontalSpan = 3
        topComposite.layoutData = topGridData

        doneButton = Button(topComposite, SWT.PUSH)
        doneButton.layoutData = GridData()
        doneButton.text = I18n.getString("account_done_button")
        doneButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                close()
            }
        })

        newButton = Button(topComposite, SWT.PUSH)
        newButton.layoutData = GridData()
        newButton.text = I18n.getString("account_new_button")
        newButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                newAccount()
                emailCombo.setFocus()
            }
        })

        saveButton = Button(topComposite, SWT.PUSH)
        saveButton.layoutData = GridData()
        saveButton.text = I18n.getString("account_save_button")
        saveButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                saveAccount()

                account?.let { account ->
                    if (Account.isGmail(account.email)) {
                        MessageDialog.openInformation(I18n.getString("account_gmail_note"))
                        try {
                            val client = GmailClient(account, database, bag.options)
                            client.authorize()
                        } catch (e: AuthException) {
                            MessageDialog.openError(e.message ?: "")
                        }
                    }
                }
            }
        })

        deleteButton = Button(topComposite, SWT.PUSH)
        deleteButton.layoutData = GridData()
        deleteButton.text = I18n.getString("account_delete_button")
        deleteButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                if (MessageDialog.openConfirm(I18n.getString("confirm_delete_account")) == SWT.YES) {
                    deleteAccount()
                    emailCombo.setFocus()
                }
            }
        })

        emailLabel = Label(container, SWT.NONE)
        emailLabel.layoutData = GridData()
        emailLabel.text = I18n.getString("account_email_label")

        emailCombo = Combo(container, SWT.BORDER or SWT.SINGLE or SWT.V_SCROLL)
        val emailGridData = GridData(SWT.FILL, SWT.TOP, true, false)
        emailGridData.horizontalSpan = 2
        emailCombo.layoutData = emailGridData
        emailCombo.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                selectAccount(emailCombo.selectionIndex)
            }
        })
        emailCombo.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent) {
                val input = emailCombo.text.trim()

                if (userText.text.isEmpty()) {
                    userText.text = emailCombo.text
                }

                if (incomingHostText.text.isEmpty() && outgoingHostText.text.isEmpty()) {
                    val atIndex = input.indexOf('@')
                    if (atIndex != -1) {
                        val textAfterAt = input.substring(atIndex + 1)
                        val incomingHostPrefix = if (protocolList.selectionIndex == 0) "imap" else "pop"
                        val outgoingHostPrefix = "smtp"
                        incomingHostText.text = "$incomingHostPrefix.$textAfterAt"
                        outgoingHostText.text = "$outgoingHostPrefix.$textAfterAt"
                    }
                }
            }
        })

        nameLabel = Label(container, SWT.NONE)
        nameLabel.layoutData = GridData()
        nameLabel.text = I18n.getString("account_name_label")

        nameText = Text(container, SWT.BORDER or SWT.H_SCROLL or SWT.SINGLE)
        val nameGridData = GridData(SWT.FILL, SWT.TOP, true, false)
        nameGridData.horizontalSpan = 2
        nameText.layoutData = nameGridData

        userLabel = Label(container, SWT.NONE)
        userLabel.layoutData = GridData()
        userLabel.text = I18n.getString("account_user_label")

        userText = Text(container, SWT.BORDER or SWT.H_SCROLL or SWT.SINGLE)
        val userGridData = GridData(SWT.FILL, SWT.TOP, true, false)
        userGridData.horizontalSpan = 2
        userText.layoutData = userGridData

        passwordLabel = Label(container, SWT.NONE)
        passwordLabel.layoutData = GridData()
        passwordLabel.text = I18n.getString("account_password_label")

        passwordText = Text(container, SWT.BORDER or SWT.H_SCROLL or SWT.PASSWORD or SWT.SINGLE)
        val passwordGridData = GridData(SWT.FILL, SWT.TOP, true, false)
        passwordGridData.horizontalSpan = 2
        passwordText.layoutData = passwordGridData

        protocolLabel = Label(container, SWT.NONE)
        protocolLabel.layoutData = GridData()
        protocolLabel.text = I18n.getString("account_protocol_label")

        protocolList = List(container, SWT.BORDER or SWT.SINGLE or SWT.V_SCROLL)
        val protocolGridData = GridData(SWT.FILL, SWT.TOP, true, false)
        protocolGridData.heightHint = protocolList.itemHeight
        protocolList.layoutData = protocolGridData
        protocolList.add(MailProtocol.IMAP.toString())
        protocolList.add(MailProtocol.POP3.toString())
        protocolList.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                val sslActive = sslActiveCheck.selection
                val incomingPort = MailProtocol.values()[protocolList.selectionIndex].port(sslActive)
                incomingPortText.text = incomingPort.toString()

                val input = incomingHostText.text.trim()
                val dotIndex = input.indexOf('.')
                if (dotIndex != -1) {
                    val textFromDot = input.substring(dotIndex)
                    val incomingHostPrefix = if (protocolList.selectionIndex == 0) "imap" else "pop"
                    incomingHostText.text = "$incomingHostPrefix$textFromDot"
                }
            }
        })

        sslActiveCheck = Button(container, SWT.CHECK)
        sslActiveCheck.layoutData = GridData()
        sslActiveCheck.text = I18n.getString("account_ssl_active_check")
        sslActiveCheck.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                val sslActive = sslActiveCheck.selection
                val incomingPort = MailProtocol.values()[protocolList.selectionIndex].port(sslActive)
                val outgoingPort = MailProtocol.SMTP.port(sslActive)
                incomingPortText.text = incomingPort.toString()
                outgoingPortText.text = outgoingPort.toString()
            }
        })

        incomingHostLabel = Label(container, SWT.NONE)
        incomingHostLabel.layoutData = GridData()
        incomingHostLabel.text = I18n.getString("account_incoming_host_label")

        incomingHostText = Text(container, SWT.BORDER or SWT.H_SCROLL or SWT.SINGLE)
        val incomingHostGridData = GridData(SWT.FILL, SWT.TOP, true, false)
        incomingHostGridData.horizontalSpan = 2
        incomingHostText.layoutData = incomingHostGridData

        incomingPortLabel = Label(container, SWT.NONE)
        incomingPortLabel.layoutData = GridData()
        incomingPortLabel.text = I18n.getString("account_incoming_port_label")

        incomingPortText = Text(container, SWT.BORDER or SWT.H_SCROLL or SWT.SINGLE)
        val incomingPortGridData = GridData(SWT.FILL, SWT.TOP, true, false)
        incomingPortGridData.horizontalSpan = 2
        incomingPortText.layoutData = incomingPortGridData
        incomingPortText.addVerifyListener(NumberValidator())

        outgoingHostLabel = Label(container, SWT.NONE)
        outgoingHostLabel.layoutData = GridData()
        outgoingHostLabel.text = I18n.getString("account_outgoing_host_label")

        outgoingHostText = Text(container, SWT.BORDER or SWT.H_SCROLL or SWT.SINGLE)
        val outgoingHostGridData = GridData(SWT.FILL, SWT.TOP, true, false)
        outgoingHostGridData.horizontalSpan = 2
        outgoingHostText.layoutData = outgoingHostGridData

        outgoingPortLabel = Label(container, SWT.NONE)
        outgoingPortLabel.layoutData = GridData()
        outgoingPortLabel.text = I18n.getString("account_outgoing_port_label")

        outgoingPortText = Text(container, SWT.BORDER or SWT.H_SCROLL or SWT.SINGLE)
        val outgoingPortGridData = GridData(SWT.FILL, SWT.TOP, true, false)
        outgoingPortGridData.horizontalSpan = 2
        outgoingPortText.layoutData = outgoingPortGridData
        outgoingPortText.addVerifyListener(NumberValidator())

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
            MessageDialog.openError(I18n.getString("account_no_email_error"))
            return false
        }

        bag.accounts.forEach {
            if (it != account && it.email.equalsIgnoreCase(value)) {
                emailCombo.setFocus()
                MessageDialog.openError(I18n.getString("account_exists_error"))
                return false
            }
        }

        value = nameText.text.trim()
        nameText.text = value
        if (value.isEmpty()) {
            nameText.setFocus()
            MessageDialog.openError(I18n.getString("account_no_name_error"))
            return false
        }

        if (Account.isGmail(emailCombo.text)) {
            userText.text = ""
            passwordText.text = ""
            incomingHostText.text = ""
            incomingPortText.text = ""
            outgoingHostText.text = ""
            outgoingPortText.text = ""
        } else {
            value = userText.text.trim()
            userText.text = value
            if (value.isEmpty()) {
                userText.setFocus()
                MessageDialog.openError(I18n.getString("account_no_user_error"))
                return false
            }

            value = passwordText.text.trim()
            passwordText.text = value
            if (value.isEmpty()) {
                passwordText.setFocus()
                MessageDialog.openError(I18n.getString("account_no_password_error"))
                return false
            }

            value = incomingHostText.text.trim()
            incomingHostText.text = value
            if (value.isEmpty()) {
                incomingHostText.setFocus()
                MessageDialog.openError(I18n.getString("account_no_incoming_host_error"))
                return false
            }

            value = incomingPortText.text.trim()
            incomingPortText.text = value
            if (value.isEmpty()) {
                incomingPortText.setFocus()
                MessageDialog.openError(I18n.getString("account_no_incoming_port_error"))
                return false
            }

            value = outgoingHostText.text.trim()
            outgoingHostText.text = value
            if (value.isEmpty()) {
                outgoingHostText.setFocus()
                MessageDialog.openError(I18n.getString("account_no_outgoing_host_error"))
                return false
            }

            value = outgoingPortText.text.trim()
            outgoingPortText.text = value
            if (value.isEmpty()) {
                outgoingPortText.setFocus()
                MessageDialog.openError(I18n.getString("account_no_outgoing_port_error"))
                return false
            }
        }

        return true
    }

    /**
     * Fills the email combo box with account emails from the bag.
     */
    private fun fillCombo() {
        emailCombo.removeAll()
        var selectionIndex = -1

        for (element in bag.accounts) {
            emailCombo.add(element.email)
            if (selectionIndex == -1) {
                if (account == null) {
                    selectionIndex = 0
                } else {
                    if (account == element) {
                        selectionIndex = emailCombo.itemCount - 1
                    }
                }
            }
        }

        if (selectionIndex == -1) {
            newAccount()
        } else {
            emailCombo.select(selectionIndex)
            selectAccount(selectionIndex)
        }
    }

    /**
     * Selects an account from the list based on the given index.
     * 
     * @param index The index of the account to select.
     */
    private fun selectAccount(index: Int) {
        isNew = false
        account = bag.accounts[index]
        showAccount()
        deleteButton.isEnabled = true
    }

    /**
     * Displays the selected account's details in the form.
     */
    private fun showAccount() {
        emailCombo.text = account?.email ?: ""
        nameText.text = account?.displayName ?: ""
        userText.text = account?.user ?: ""
        passwordText.text = account?.password ?: ""
        protocolList.setSelection(account?.protocol?.ordinal ?: 0)
        sslActiveCheck.setSelection(account?.sslActive ?: true)
        incomingHostText.text = account?.incomingHost ?: ""
        incomingPortText.text = account?.incomingPort?.toString() ?: ""
        outgoingHostText.text = account?.outgoingHost ?: ""
        outgoingPortText.text = account?.outgoingPort?.toString() ?: ""
    }

    /**
     * Creates a new account and displays its details in the form.
     */
    private fun newAccount() {
        isNew = true
        account = Account()
        account?.let {
            bag.accounts.add(it)
            showAccount()
            deleteButton.isEnabled = false
        }
    }

    /**
     * Saves the current account details to the database.
     */
    private fun saveAccount() {
        if (validate()) {
            account?.apply {
                email = emailCombo.text
                displayName = nameText.text
                user = userText.text
                password = passwordText.text
                protocol = MailProtocol.values()[protocolList.selectionIndex]
                sslActive = sslActiveCheck.selection
                incomingHost = incomingHostText.text
                if (incomingPortText.text.isNotEmpty()) {
                    incomingPort = incomingPortText.text.toInt()
                }
                outgoingHost = outgoingHostText.text
                if (outgoingPortText.text.isNotEmpty()) {
                    outgoingPort = outgoingPortText.text.toInt()
                }
            }

            try {
                account?.let {
                    if (isNew) {
                        database.addAccount(it)
                    } else {
                        database.updateAccount(it)
                    }
                    fillCombo()
                }
            } catch (e: DBRuntimeException) {
                MessageDialog.openError(e.message ?: "")
            }
        }
    }

    /**
     * Deletes the current account from the database.
     */
    private fun deleteAccount() {
        try {
            account?.let {
                database.deleteAccount(it)
                bag.accounts.remove(it)
                account = null
                fillCombo()
            }
        } catch (e: DBRuntimeException) {
            MessageDialog.openError(e.message ?: "")
        }
    }
}
