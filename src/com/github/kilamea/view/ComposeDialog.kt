package com.github.kilamea.view

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException
import java.util.Base64

import org.eclipse.jface.action.Action
import org.eclipse.jface.action.MenuManager
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.ShellAdapter
import org.eclipse.swt.events.ShellEvent
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.List
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text

import com.github.kilamea.core.Bag
import com.github.kilamea.core.Constants
import com.github.kilamea.entity.Attachment
import com.github.kilamea.entity.Message
import com.github.kilamea.i18n.I18n
import com.github.kilamea.swt.Dimension
import com.github.kilamea.swt.FileChooser
import com.github.kilamea.swt.MessageDialog
import com.github.kilamea.swt.ModalDialog
import com.github.kilamea.swt.TabTraverse

internal class ComposeDialog(parentShell: Shell, private val bag: Bag, private val message: Message) :
    ModalDialog(parentShell, emptyArray<String>()), IFormValidator {

    private lateinit var sendButton: Button
    private lateinit var saveDraftButton: Button
    private lateinit var discardButton: Button
    private lateinit var fromAddressLabel: Label
    private lateinit var fromAddressText: Text
    private lateinit var recipientLabel: Label
    private lateinit var recipientText: Text
    private lateinit var recipientContactButton: Button
    private lateinit var ccAddressLabel: Label
    private lateinit var ccAddressText: Text
    private lateinit var ccAddressContactButton: Button
    private lateinit var bccAddressLabel: Label
    private lateinit var bccAddressText: Text
    private lateinit var bccAddressContactButton: Button
    private lateinit var attachmentLabel: Label
    private lateinit var attachmentList: List
    private lateinit var browseButton: Button
    private lateinit var subjectLabel: Label
    private lateinit var subjectText: Text
    private lateinit var contentLabel: Label
    private lateinit var contentText: Text
    private lateinit var contactMenu: Menu
    private lateinit var contactActions: Array<ContactAction>
    private lateinit var attachmentMenu: Menu
    private lateinit var saveAttachmentAction: SaveAttachmentAction
    private lateinit var deleteAttachmentAction: DeleteAttachmentAction

    private var readOnly: Boolean = false

    init {
        createActions()
    }

    override fun configureShell(newShell: Shell) {
        super.configureShell(newShell)
        newShell.text = I18n.getString("compose_window_title")
        newShell.addShellListener(object : ShellAdapter() {
            override fun shellActivated(event: ShellEvent) {
                recipientText.setFocus()
            }
        })
    }

    override fun createDialogArea(parent: Composite): Control {
        val container = super.createDialogArea(parent) as Composite
        val gridLayout = container.layout as GridLayout
        gridLayout.numColumns = 3

        val topComposite = Composite(container, SWT.BORDER)
        topComposite.layout = GridLayout(3, true)
        val topGridData = GridData(SWT.FILL, SWT.TOP, true, false)
        topGridData.horizontalSpan = 3
        topComposite.layoutData = topGridData

        sendButton = Button(topComposite, SWT.PUSH)
        sendButton.isEnabled = !readOnly
        sendButton.layoutData = GridData()
        sendButton.text = I18n.getString("compose_send_button")
        sendButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                if (validate()) {
                    fillMessage(false)
                    returnCode = OK
                    close()
                }
            }
        })

        saveDraftButton = Button(topComposite, SWT.PUSH)
        saveDraftButton.isEnabled = !readOnly
        saveDraftButton.layoutData = GridData()
        saveDraftButton.text = I18n.getString("compose_save_draft_button")
        saveDraftButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                if (message.folder != null) {
                    fillMessage(true)
                    returnCode = OK
                    close()
                } else {
                    MessageDialog.openError(I18n.getString("no_drafts_available"))
                }
            }
        })

        discardButton = Button(topComposite, SWT.PUSH)
        discardButton.layoutData = GridData()
        discardButton.text = I18n.getString("compose_discard_button")
        discardButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                returnCode = CANCEL
                close()
            }
        })

        fromAddressLabel = Label(container, SWT.NONE)
        fromAddressLabel.layoutData = GridData()
        fromAddressLabel.text = I18n.getString("compose_from_address_label")

        val fromAddressStyle = SWT.BORDER or SWT.H_SCROLL or SWT.READ_ONLY or SWT.SINGLE
        fromAddressText = Text(container, fromAddressStyle)
        val fromAddressGridData = GridData(SWT.FILL, SWT.TOP, true, false)
        fromAddressGridData.horizontalSpan = 2
        fromAddressText.layoutData = fromAddressGridData
        fromAddressText.text = message.fromAddresses
        fromAddressText.selectAll()

        recipientLabel = Label(container, SWT.NONE)
        recipientLabel.layoutData = GridData()
        recipientLabel.text = I18n.getString("compose_recipient_label")

        var recipientStyle = SWT.BORDER or SWT.H_SCROLL or SWT.SINGLE
        if (readOnly) {
            recipientStyle = recipientStyle or SWT.READ_ONLY
        }
        recipientText = Text(container, recipientStyle)
        recipientText.layoutData = GridData(SWT.FILL, SWT.TOP, true, false)
        recipientText.text = message.recipients
        recipientText.selectAll()

        recipientContactButton = Button(container, SWT.PUSH)
        recipientContactButton.isEnabled = !readOnly && bag.contacts.isNotEmpty()
        recipientContactButton.layoutData = GridData()
        recipientContactButton.text = I18n.getString("compose_contact_button")
        recipientContactButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                contactMenu.setData(recipientText)
                contactMenu.setLocation(shell.toDisplay(event.x, event.y))
                contactMenu.setVisible(true)
            }
        })

        ccAddressLabel = Label(container, SWT.NONE)
        ccAddressLabel.layoutData = GridData()
        ccAddressLabel.text = I18n.getString("compose_cc_address_label")

        var ccAddressStyle = SWT.BORDER or SWT.H_SCROLL or SWT.SINGLE
        if (readOnly) {
            ccAddressStyle = ccAddressStyle or SWT.READ_ONLY
        }
        ccAddressText = Text(container, ccAddressStyle)
        ccAddressText.layoutData = GridData(SWT.FILL, SWT.TOP, true, false)
        ccAddressText.text = message.ccAddresses
        ccAddressText.selectAll()

        ccAddressContactButton = Button(container, SWT.PUSH)
        ccAddressContactButton.isEnabled = !readOnly && bag.contacts.isNotEmpty()
        ccAddressContactButton.layoutData = GridData()
        ccAddressContactButton.text = I18n.getString("compose_contact_button")
        ccAddressContactButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                contactMenu.setData(ccAddressText)
                contactMenu.setLocation(shell.toDisplay(event.x, event.y))
                contactMenu.setVisible(true)
            }
        })

        bccAddressLabel = Label(container, SWT.NONE)
        bccAddressLabel.layoutData = GridData()
        bccAddressLabel.text = I18n.getString("compose_bcc_address_label")

        var bccAddressStyle = SWT.BORDER or SWT.H_SCROLL or SWT.SINGLE
        if (readOnly) {
            bccAddressStyle = bccAddressStyle or SWT.READ_ONLY
        }
        bccAddressText = Text(container, bccAddressStyle)
        bccAddressText.layoutData = GridData(SWT.FILL, SWT.TOP, true, false)
        bccAddressText.text = message.bccAddresses
        bccAddressText.selectAll()

        bccAddressContactButton = Button(container, SWT.PUSH)
        bccAddressContactButton.isEnabled = !readOnly && bag.contacts.isNotEmpty()
        bccAddressContactButton.layoutData = GridData()
        bccAddressContactButton.text = I18n.getString("compose_contact_button")
        bccAddressContactButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                contactMenu.setData(bccAddressText)
                contactMenu.setLocation(shell.toDisplay(event.x, event.y))
                contactMenu.setVisible(true)
            }
        })

        attachmentLabel = Label(container, SWT.NONE)
        attachmentLabel.layoutData = GridData()
        attachmentLabel.text = I18n.getString("compose_attachment_label")

        val attachmentStyle = SWT.BORDER or SWT.SINGLE or SWT.V_SCROLL
        attachmentList = List(container, attachmentStyle)
        val attachmentGridData = GridData(SWT.FILL, SWT.TOP, true, false)
        attachmentGridData.heightHint = attachmentList.itemHeight
        attachmentList.layoutData = attachmentGridData
        attachmentList.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) {
                when {
                    event.keyCode == SWT.DEL.code -> {
                        event.doit = false
                        if (deleteAttachmentAction.isEnabled) {
                            deleteAttachmentAction.run()
                        }
                    }
                }
            }
        })
        message.attachments.forEach { attachmentList.add(it.fileName) }
        if (attachmentList.itemCount > 0) {
            attachmentList.setSelection(0)
        }

        browseButton = Button(container, SWT.PUSH)
        browseButton.isEnabled = !readOnly
        browseButton.layoutData = GridData()
        browseButton.text = I18n.getString("compose_browse_button")
        browseButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                val fileChooser = FileChooser(shell)
                fileChooser.filterExtensions = arrayOf("*.*")
                fileChooser.filterNames = arrayOf(I18n.getString("file_chooser_filter_all"))
                fileChooser.message = I18n.getString("file_chooser_not_found")

                val fileName = fileChooser.openDialog()
                fileName?.let {
                    val file = File(it)
                    try {
                        val contentBytes = getContentBytes(file)
                        val base64Content = Base64.getEncoder().encodeToString(contentBytes)
                        val attachment = Attachment()
                        attachment.content = base64Content
                        attachment.fileName = file.name
                        attachment.message = message
                        message.attachments.add(attachment)
                        attachmentList.add(file.name)
                        attachmentList.setSelection(attachmentList.itemCount - 1)
                        enableDisableActions()
                    } catch (e: IOException) {
                        MessageDialog.openError(I18n.getString("compose_add_attachment_error"))
                    }
                }
            }
        })

        subjectLabel = Label(container, SWT.NONE)
        subjectLabel.layoutData = GridData()
        subjectLabel.text = I18n.getString("compose_subject_label")

        var subjectStyle = SWT.BORDER or SWT.H_SCROLL or SWT.SINGLE
        if (readOnly) {
            subjectStyle = subjectStyle or SWT.READ_ONLY
        }
        subjectText = Text(container, subjectStyle)
        val subjectGridData = GridData(SWT.FILL, SWT.TOP, true, false)
        subjectGridData.horizontalSpan = 2
        subjectText.layoutData = subjectGridData
        subjectText.text = message.subject
        subjectText.selectAll()

        contentLabel = Label(container, SWT.NONE)
        contentLabel.layoutData = GridData()
        contentLabel.text = I18n.getString("compose_content_label")

        var contentStyle = SWT.BORDER or SWT.MULTI or SWT.V_SCROLL or SWT.WRAP
        if (readOnly) {
            contentStyle = contentStyle or SWT.READ_ONLY
        }
        contentText = Text(container, contentStyle)
        val textSize = Dimension.getTextSize(contentText)
        val contentGridData = GridData(SWT.LEFT, SWT.TOP, true, true)
        contentGridData.heightHint = Constants.TEXT_AREA_ROWS * textSize.y
        contentGridData.widthHint = Constants.TEXT_AREA_COLS * textSize.x
        contentGridData.horizontalSpan = 2
        contentText.layoutData = contentGridData
        contentText.text = message.content
        contentText.addTraverseListener(TabTraverse())

        contactMenu = createContactContextMenu(shell)

        attachmentMenu = createAttachmentContextMenu(shell)
        attachmentList.menu = attachmentMenu

        return container
    }

    fun setReadOnly(readOnly: Boolean) {
        this.readOnly = readOnly
    }

    override fun validate(): Boolean {
        var value = recipientText.text.trim()
        recipientText.text = value
        if (value.isEmpty()) {
            recipientText.setFocus()
            MessageDialog.openError(I18n.getString("compose_no_recipient_error"))
            return false
        }

        return true
    }

    private fun createActions() {
        val actionCount = bag.contacts.size
        contactActions = Array(actionCount) { i ->
            val email = bag.contacts[i].toString()
            var text = email
            if (text.contains("@")) {
                text += '\t'
            }
            ContactAction(email, text)
        }

        saveAttachmentAction = SaveAttachmentAction()
        deleteAttachmentAction = DeleteAttachmentAction()
        enableDisableActions()
    }

    private fun createContactContextMenu(parent: Control): Menu {
        val menuMgr = MenuManager()
        contactActions.forEach { menuMgr.add(it) }
        return menuMgr.createContextMenu(parent)
    }

    private fun createAttachmentContextMenu(parent: Control): Menu {
        val menuMgr = MenuManager()
        menuMgr.add(saveAttachmentAction)
        menuMgr.add(deleteAttachmentAction)
        return menuMgr.createContextMenu(parent)
    }

    private fun enableDisableActions() {
        val hasAttachments = message.attachments.isNotEmpty()
        saveAttachmentAction.isEnabled = hasAttachments
        deleteAttachmentAction.isEnabled = hasAttachments and !readOnly
    }

    private fun fillMessage(drafted: Boolean) {
        var subject = subjectText.text.trim()
        if (subject.isEmpty()) {
            subject = I18n.getString("compose_no_subject")
        }

        message.recipients = recipientText.text
        message.ccAddresses = ccAddressText.text
        message.bccAddresses = bccAddressText.text
        message.subject = subject
        message.content = contentText.text
        message.unread = false
        message.drafted = drafted
    }

    @Throws(IOException::class)
    private fun getContentBytes(file: File): ByteArray {
        FileInputStream(file).use { inputStream ->
            ByteArrayOutputStream().use { outputStream ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                return outputStream.toByteArray()
            }
        }
    }

    private inner class ContactAction(private val email: String, text: String) : Action(text) {
        override fun run() {
            if (contactMenu.getData() is Text) {
                val addressText = contactMenu.getData() as Text
                var addresses = addressText.text.trim()
                if (!addresses.lowercase().contains(email.lowercase())) {
                    if (addresses.isNotEmpty() && !addresses.endsWith(",")) {
                        addresses += ", "
                    }
                    addresses += email
                    addressText.text = addresses
                    addressText.selectAll()
                }
                addressText.setFocus()
            }
            contactMenu.setData(null)
        }
    }

    private inner class SaveAttachmentAction : Action(I18n.getString("compose_save_attachment_action")) {
        override fun run() {
            val index = attachmentList.selectionIndex
            val attachment = message.attachments[index]

            val fileChooser = FileChooser(shell)
            fileChooser.fileName = attachment.fileName
            fileChooser.filterExtensions = arrayOf("*.*")
            fileChooser.filterNames = arrayOf(I18n.getString("file_chooser_filter_all"))
            fileChooser.message = I18n.getString("file_chooser_confirm_overwrite")

            val fileName = fileChooser.saveDialog()
            fileName?.let {
                val decodedBytes = Base64.getDecoder().decode(attachment.content)
                try {
                    FileOutputStream(it).use { outputStream ->
                        outputStream.write(decodedBytes)
                        outputStream.flush()
                    }
                } catch (e: IOException) {
                    MessageDialog.openError(I18n.getString("compose_save_attachment_error"))
                }
            }
        }
    }

    private inner class DeleteAttachmentAction : Action(I18n.getString("compose_delete_attachment_action")) {
        override fun run() {
            var index = attachmentList.selectionIndex
            message.attachments.removeAt(index)
            attachmentList.remove(index)
            if (attachmentList.itemCount > 0) {
                if (index == attachmentList.itemCount) {
                    index--
                }
                attachmentList.setSelection(index)
            }
            enableDisableActions()
        }
    }
}
