package com.github.kilamea.view

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import org.eclipse.jface.action.Action
import org.eclipse.jface.action.IAction
import org.eclipse.jface.action.MenuManager
import org.eclipse.jface.action.Separator
import org.eclipse.jface.viewers.ArrayContentProvider
import org.eclipse.jface.viewers.ColumnLabelProvider
import org.eclipse.jface.viewers.ISelectionChangedListener
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.jface.viewers.LabelProvider
import org.eclipse.jface.viewers.SelectionChangedEvent
import org.eclipse.jface.viewers.StructuredSelection
import org.eclipse.jface.viewers.TableViewer
import org.eclipse.jface.viewers.TableViewerColumn
import org.eclipse.jface.viewers.TreeViewer
import org.eclipse.jface.window.ApplicationWindow
import org.eclipse.jface.window.Window
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.graphics.Cursor
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Table
import org.eclipse.swt.widgets.Tree

import com.github.kilamea.core.Bag
import com.github.kilamea.core.Constants
import com.github.kilamea.core.Context
import com.github.kilamea.database.DatabaseManager
import com.github.kilamea.database.DBRuntimeException
import com.github.kilamea.entity.Account
import com.github.kilamea.entity.AttachmentList
import com.github.kilamea.entity.Contact
import com.github.kilamea.entity.Folder
import com.github.kilamea.entity.FolderType
import com.github.kilamea.entity.Message
import com.github.kilamea.entity.MessageList
import com.github.kilamea.i18n.I18n
import com.github.kilamea.mail.AttachmentConverter
import com.github.kilamea.mail.ClientBuilder
import com.github.kilamea.mail.DefaultClient
import com.github.kilamea.mail.ReceiveException
import com.github.kilamea.mail.SendException
import com.github.kilamea.sort.ComparatorPool
import com.github.kilamea.sort.FieldComparator
import com.github.kilamea.sort.SortField
import com.github.kilamea.sort.SortOrder
import com.github.kilamea.swt.CocoaUIEnhancer
import com.github.kilamea.swt.FileChooser
import com.github.kilamea.swt.MessageDialog
import com.github.kilamea.util.equalsIgnoreCase
import com.github.kilamea.util.FileUtils
import com.github.kilamea.util.SystemUtils

/**
 * Represents the main application window for Kilamea.
 * 
 * @since 0.1.0
 * @property bag The bag containing various application data.
 * @property comparatorPool The pool managing comparators for sorting.
 * @property context The initial context of the application.
 * @property database The database manager instance.
 * @property dbException The database runtime exception, if any.
 */
class Kilamea : ApplicationWindow {
    private lateinit var mailboxAccLabel: Label
    private lateinit var mailboxViewer: TreeViewer
    private lateinit var messageAccLabel: Label
    private lateinit var messageViewer: TableViewer
    private lateinit var messageColumnStatus: TableViewerColumn
    private lateinit var messageColumnSubject: TableViewerColumn
    private lateinit var messageColumnAddress: TableViewerColumn
    private lateinit var messageColumnSentDate: TableViewerColumn
    private lateinit var fileReceiveMenu: MenuManager

    private lateinit var fileOpenAction: FileOpenAction
    private lateinit var fileSaveAsAction: FileSaveAsAction
    private lateinit var fileNewFolderAction: FileNewFolderAction
    private lateinit var fileRenameFolderAction: FileRenameFolderAction
    private lateinit var fileDeleteFolderAction: FileDeleteFolderAction
    private lateinit var fileSearchFolderAction: FileSearchFolderAction
    private lateinit var fileEmptyTrashAction: FileEmptyTrashAction
    private lateinit var fileReceiveActions: Array<FileReceiveAction?>
    private lateinit var fileExitAction: FileExitAction
    private lateinit var viewSortFieldActions: Array<ViewSortFieldAction>
    private lateinit var viewSortOrderActions: Array<ViewSortOrderAction>
    private lateinit var viewSourceAction: ViewSourceAction
    private lateinit var messageNewAction: MessageNewAction
    private lateinit var messageReplyAction: MessageReplyAction
    private lateinit var messageReplyAllAction: MessageReplyAllAction
    private lateinit var messageForwardAction: MessageForwardAction
    private lateinit var messageCopyAction: MessageCopyAction
    private lateinit var messageMoveAction: MessageMoveAction
    private lateinit var messageArchiveAction: MessageArchiveAction
    private lateinit var messageDeleteAction: MessageDeleteAction
    private lateinit var toolsContactAction: ToolsContactAction
    private lateinit var toolsAccountAction: ToolsAccountAction
    private lateinit var toolsPrefsAction: ToolsPrefsAction
    private lateinit var helpAboutAction: HelpAboutAction
    private lateinit var helpAppDataFolderAction: HelpAppDataFolderAction

    private val bag: Bag
    private val comparatorPool: ComparatorPool
    private val context: Context
    private val database: DatabaseManager
    private var dbException: DBRuntimeException? = null

    private var focusedAccount: Account? = null
    private var focusedFolder: Folder? = null
    private var focusedMessage: Message? = null

    /**
     * Constructs the main application window with the initial context.
     * 
     * @param initialContext The initial context of the application.
     */
    constructor(initialContext: Context) : super(null) {
        bag = Bag()
        context = initialContext
        database = DatabaseManager()
        connectDatabase()
        comparatorPool = ComparatorPool()
        comparatorPool.initializeComparator(Message::class.java, bag.options.mailSortField, bag.options.mailSortOrder)
        comparatorPool.initializeComparator(Contact::class.java, bag.options.contactSortField, bag.options.contactSortOrder)
        createActions()
        addMenuBar()
    }

    /**
     * Configures the shell with the application name.
     * 
     * @param newShell The shell to be configure.
     */
    override fun configureShell(newShell: Shell) {
        super.configureShell(newShell)
        newShell.text = Constants.APP_NAME
    }

    /**
     * Creates the contents of the application window.
     * 
     * @param parent The parent composite.
     * @return The control representing the contents.
     */
    override fun createContents(parent: Composite): Control {
        val container = Composite(parent, SWT.NONE)
        container.layout = GridLayout()

        val sashForm = SashForm(container, SWT.HORIZONTAL)
        sashForm.layoutData = GridData()
        sashForm.sashWidth = 20

        val mailboxComposite = Composite(sashForm, SWT.NONE)
        mailboxComposite.layout = GridLayout()

        mailboxAccLabel = Label(mailboxComposite, SWT.NONE)
        val mailboxLabelGridData = GridData()
        mailboxLabelGridData.exclude = true
        mailboxAccLabel.layoutData = mailboxLabelGridData
        mailboxAccLabel.text = I18n.getString("mailbox_acclabel")
        mailboxAccLabel.isVisible = false

        mailboxViewer = TreeViewer(mailboxComposite, SWT.BORDER or SWT.FULL_SELECTION or SWT.SINGLE or SWT.V_SCROLL)
        mailboxViewer.setContentProvider(MailboxViewerContentProvider())
        mailboxViewer.setLabelProvider(object : LabelProvider() {
            override fun getText(element: Any): String {
                var string = element.toString()
                if (element is Folder) {
                    var count = when (element.type) {
                        FolderType.Inbox, FolderType.Custom -> element.getUnreadMessageCount()
                        FolderType.Drafts -> element.messages.size
                        else -> 0
                    }
                    if (count > 0) {
                        string += " ($count)"
                    }
                }
                return string
            }
        })
        mailboxViewer.addSelectionChangedListener(object : ISelectionChangedListener {
            override fun selectionChanged(event: SelectionChangedEvent) {
                if (!mailboxViewer.tree.isDisposed) {
                    val selection = event.selection as IStructuredSelection
                    if (!selection.isEmpty) {
                        var messages = MessageList()

                        val element = selection.firstElement
                        if (element is Account) {
                            focusedFolder = null
                            focusedAccount = element
                            focusedAccount?.let { account ->
                                bag.options.lastMailboxEntry = account.id
                            }
                        } else {
                            focusedFolder = element as Folder
                            focusedFolder?.let { folder ->
                                focusedAccount = folder.account
                                bag.options.lastMailboxEntry = folder.id
                                messages = folder.messages
                            }
                        }

                        if (focusedFolder != null && (focusedFolder!!.type == FolderType.Drafts
                            || focusedFolder!!.type == FolderType.Sent)) {
                            messageColumnAddress.column.text = I18n.getString("message_column_recipients")
                        } else {
                            messageColumnAddress.column.text = I18n.getString("message_column_from_addresses")
                        }

                        messageViewer.input = messages
                        if (messages.isNotEmpty()) {
                            messageViewer.setSelection(StructuredSelection(messages[messages.size - 1]), true)
                        } else {
                            messageViewer.setSelection(StructuredSelection.EMPTY, false)
                        }
                    } else {
                        focusedMessage = null
                        focusedFolder = null
                        focusedAccount = null
                    }

                    enableDisableActions()
                }
            }
        })

        val tree = mailboxViewer.tree
        tree.headerVisible = false
        tree.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        tree.linesVisible = true
        tree.menu = createMailboxContextMenu(tree)
        tree.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) {
                when {
                    event.keyCode == SWT.DEL.code -> {
                        event.doit = false
                        if (fileDeleteFolderAction.isEnabled) {
                            fileDeleteFolderAction.run()
                        }
                    }
                }
            }
        })

        val messageComposite = Composite(sashForm, SWT.NONE)
        messageComposite.layout = GridLayout()
        sashForm.setWeights(1, 6)

        messageAccLabel = Label(messageComposite, SWT.NONE)
        val messageLabelGridData = GridData()
        messageLabelGridData.exclude = true
        messageAccLabel.layoutData = messageLabelGridData
        messageAccLabel.text = I18n.getString("message_acclabel")
        messageAccLabel.isVisible = false

        messageViewer = TableViewer(messageComposite, SWT.BORDER or SWT.FULL_SELECTION or SWT.SINGLE or SWT.V_SCROLL)
        messageViewer.setContentProvider(ArrayContentProvider.getInstance())
        messageViewer.addSelectionChangedListener(object : ISelectionChangedListener {
            override fun selectionChanged(event: SelectionChangedEvent) {
                if (!messageViewer.table.isDisposed) {
                    val selection = event.selection as IStructuredSelection
                    focusedMessage = if (!selection.isEmpty) {
                        selection.firstElement as Message
                    } else {
                        null
                    }
                }
            }
        })

        messageColumnStatus = TableViewerColumn(messageViewer, SWT.LEFT)
        messageColumnStatus.column.text = I18n.getString("message_column_status")
        messageColumnStatus.column.width = 100
        messageColumnStatus.setLabelProvider(object : ColumnLabelProvider() {
            override fun getText(element: Any): String {
                val message = element as Message
                return if (message.unread) {
                    I18n.getString("status_unread")
                } else {
                    I18n.getString("status_read")
                }
            }
        })

        messageColumnSubject = TableViewerColumn(messageViewer, SWT.LEFT)
        messageColumnSubject.column.text = I18n.getString("message_column_subject")
        messageColumnSubject.column.width = 430
        messageColumnSubject.setLabelProvider(object : ColumnLabelProvider() {
            override fun getText(element: Any): String {
                return (element as Message).subject
            }
        })

        messageColumnAddress = TableViewerColumn(messageViewer, SWT.LEFT)
        messageColumnAddress.column.text = I18n.getString("message_column_from_addresses")
        messageColumnAddress.column.width = 250
        messageColumnAddress.setLabelProvider(object : ColumnLabelProvider() {
            override fun getText(element: Any): String {
                val message = element as Message
                return if (focusedFolder != null) {
                    if (focusedFolder!!.type == FolderType.Drafts || focusedFolder!!.type == FolderType.Sent) {
                        message.recipients
                    } else {
                        message.fromAddresses
                    }
                } else {
                    ""
                }
            }
        })

        messageColumnSentDate = TableViewerColumn(messageViewer, SWT.LEFT)
        messageColumnSentDate.column.text = I18n.getString("message_column_sent_date")
        messageColumnSentDate.column.width = 120
        messageColumnSentDate.setLabelProvider(object : ColumnLabelProvider() {
            override fun getText(element: Any): String {
                val sentDate = (element as Message).sentDate
                return SimpleDateFormat(I18n.getString("message_column_date_format"), Locale.getDefault())
                    .format(sentDate)
            }
        })

        val table = messageViewer.table
        table.headerVisible = true
        val tableGridData = GridData(SWT.LEFT, SWT.TOP, true, false)
        tableGridData.heightHint = Constants.MESSAGE_COUNT * table.itemHeight
        table.layoutData = tableGridData
        table.linesVisible = true
        table.menu = createMessageContextMenu(table)
        table.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) {
                when {
                    event.keyCode == SWT.CR.code || event.keyCode == SWT.SPACE.code -> {
                        event.doit = false
                        if (fileOpenAction.isEnabled) {
                            fileOpenAction.run()
                        }
                    }
                    event.keyCode == SWT.DEL.code -> {
                        event.doit = false
                        if (messageDeleteAction.isEnabled) {
                            messageDeleteAction.run()
                        }
                    }
                }
            }
        })

        if (SystemUtils.isMac()) {
            CocoaUIEnhancer.hookApplicationMenu(helpAboutAction, toolsPrefsAction, fileExitAction)
        }

        mailboxViewer.input = bag
        mailboxViewer.expandAll()

        prepare()

        return container
    }

    /**
     * Creates the menu manager and sets up the menu structure.
     * 
     * @return The created menu manager.
     */
    override fun createMenuManager(): MenuManager {
        val menuMgr = MenuManager()

        val fileMenu = MenuManager(MenuFactory.getText("file_menu"))
        fileMenu.add(fileOpenAction)
        fileMenu.add(fileSaveAsAction)
        fileMenu.add(Separator())
        fileMenu.add(fileNewFolderAction)
        fileMenu.add(fileRenameFolderAction)
        fileMenu.add(fileDeleteFolderAction)
        fileMenu.add(fileSearchFolderAction)
        fileMenu.add(Separator())
        fileMenu.add(fileEmptyTrashAction)
        fileMenu.add(Separator())
        fileReceiveMenu = MenuManager(MenuFactory.getText("file_receive_menu"))
        fileMenu.add(fileReceiveMenu)
        if (!SystemUtils.isMac()) {
            fileMenu.add(Separator())
            fileMenu.add(fileExitAction)
        }
        menuMgr.add(fileMenu)

        val viewMenu = MenuManager(MenuFactory.getText("view_menu"))
        val viewSortMenu = MenuManager(MenuFactory.getText("view_sort_menu"))
        viewSortFieldActions.forEach { viewSortMenu.add(it) }
        viewSortMenu.add(Separator())
        viewSortOrderActions.forEach { viewSortMenu.add(it) }
        viewMenu.add(viewSortMenu)
        viewMenu.add(viewSourceAction)
        menuMgr.add(viewMenu)

        val messageMenu = MenuManager(MenuFactory.getText("message_menu"))
        messageMenu.add(messageNewAction)
        messageMenu.add(Separator())
        messageMenu.add(messageReplyAction)
        messageMenu.add(messageReplyAllAction)
        messageMenu.add(messageForwardAction)
        messageMenu.add(Separator())
        messageMenu.add(messageCopyAction)
        messageMenu.add(messageMoveAction)
        messageMenu.add(messageArchiveAction)
        messageMenu.add(messageDeleteAction)
        menuMgr.add(messageMenu)

        val toolsMenu = MenuManager(MenuFactory.getText("tools_menu"))
        toolsMenu.add(toolsContactAction)
        toolsMenu.add(toolsAccountAction)
        toolsMenu.add(toolsPrefsAction)
        menuMgr.add(toolsMenu)

        if (!SystemUtils.isMac()) {
            val helpMenu = MenuManager(MenuFactory.getText("help_menu"))
            helpMenu.add(helpAboutAction)
            helpMenu.add(helpAppDataFolderAction)
            menuMgr.add(helpMenu)
        }

        updateFileReceiveMenu()

        return menuMgr
    }

    /**
     * Closes the application window and disconnects from the database.
     * 
     * @return true if the window is closed successfully, false otherwise.
     */
    override fun close(): Boolean {
        disconnectDatabase()

        return super.close()
    }

    /**
     * Opens the application window. If there is a database exception, an error message is displayed.
     * 
     * @return The result of the open operation.
     */
    override fun open(): Int {
        dbException?.let { ex ->
            MessageDialog.openError(ex.message ?: "")
            return CANCEL
        }

        return super.open()
    }

    /**
     * Runs the application and disposing the display after closing.
     */
    fun run() {
        setBlockOnOpen(true)
        open()
        Display.getCurrent()?.dispose()
    }

    /**
     * Retrieves the display associated with the shell.
     * 
     * @return The display associated with the shell.
     */
    private fun getDisplay(): Display {
        return shell.display
    }

    /**
     * Prepares the application by setting up the initial state and selections.
     */
    private fun prepare() {
        val treeItem = bag.findLastMailboxEntry()
        if (treeItem != null) {
            mailboxViewer.setSelection(StructuredSelection(treeItem), true)
        } else {
            mailboxViewer.setSelection(StructuredSelection.EMPTY, false)
        }

        viewSortFieldActions.firstOrNull { it.sortField == bag.options.mailSortField }?.run()
        viewSortOrderActions.firstOrNull { it.sortOrder == bag.options.mailSortOrder }?.run()

        if (bag.accounts.isEmpty()) {
            if (MessageDialog.openConfirm(I18n.getString("confirm_open_account_management")) == SWT.YES) {
                toolsAccountAction.run()
            }
        }

        if (context.hasArguments()) {
            messageNewAction.run()
        }

        if (bag.options.retrieveOnStart) {
            if (fileReceiveActions.isNotEmpty()) {
                fileReceiveActions[0]?.run()
            }
        }
    }

    /**
     * Creates and initializes the actions for the application.
     */
    private fun createActions() {
        fileOpenAction = FileOpenAction()
        fileSaveAsAction = FileSaveAsAction()
        fileNewFolderAction = FileNewFolderAction()
        fileRenameFolderAction = FileRenameFolderAction()
        fileDeleteFolderAction = FileDeleteFolderAction()
        fileSearchFolderAction = FileSearchFolderAction()
        fileEmptyTrashAction = FileEmptyTrashAction()
        fileExitAction = FileExitAction()

        viewSourceAction = ViewSourceAction()

        messageNewAction = MessageNewAction()
        messageReplyAction = MessageReplyAction()
        messageReplyAllAction = MessageReplyAllAction()
        messageForwardAction = MessageForwardAction()
        messageCopyAction = MessageCopyAction()
        messageMoveAction = MessageMoveAction()
        messageArchiveAction = MessageArchiveAction()
        messageDeleteAction = MessageDeleteAction()

        toolsContactAction = ToolsContactAction()
        toolsAccountAction = ToolsAccountAction()
        toolsPrefsAction = ToolsPrefsAction()

        helpAboutAction = HelpAboutAction()
        helpAppDataFolderAction = HelpAppDataFolderAction()

        createFileReceiveActions()
        createViewSortActions()
    }

    /**
     * Creates and initializes the file receive actions based on the number of accounts.
     */
    private fun createFileReceiveActions() {
        val actionCount = bag.accounts.size + 2
        fileReceiveActions = arrayOfNulls<FileReceiveAction>(actionCount)
        fileReceiveActions[0] = FileReceiveAction(MenuFactory.getText("file_receive_all_menu"), 0)
        fileReceiveActions[1] = FileReceiveAction(MenuFactory.getText("file_receive_current_menu"), 1)
        for (i in 2 until actionCount) {
            var text = bag.accounts[i - 2].email
            if (text.contains("@")) {
                text += '\t'
            }
            fileReceiveActions[i] = FileReceiveAction(text, i)
        }
    }

    /**
     * Creates and initializes the view sort actions for sorting messages.
     */
    private fun createViewSortActions() {
        viewSortFieldActions = arrayOf(
            ViewSortFieldAction(MenuFactory.getText("view_sort_from_addresses"), SortField.FromAddresses),
            ViewSortFieldAction(MenuFactory.getText("view_sort_recipients"), SortField.Recipients),
            ViewSortFieldAction(MenuFactory.getText("view_sort_sent_date"), SortField.SentDate),
            ViewSortFieldAction(MenuFactory.getText("view_sort_received_date"), SortField.ReceivedDate),
            ViewSortFieldAction(MenuFactory.getText("view_sort_subject"), SortField.Subject)
        )

        viewSortOrderActions = arrayOf(
            ViewSortOrderAction(MenuFactory.getText("view_sort_ascending"), SortOrder.Ascending),
            ViewSortOrderAction(MenuFactory.getText("view_sort_descending"), SortOrder.Descending)
        )
    }

    /**
     * Creates the context menu for the mailbox tree.
     * 
     * @param parent The parent control to attach the context menu to.
     * @return The created context menu.
     */
    private fun createMailboxContextMenu(parent: Control): Menu {
        val menuMgr = MenuManager()
        menuMgr.add(fileNewFolderAction)
        menuMgr.add(fileRenameFolderAction)
        menuMgr.add(fileDeleteFolderAction)
        menuMgr.add(fileSearchFolderAction)
        menuMgr.add(Separator())
        menuMgr.add(fileEmptyTrashAction)
        return menuMgr.createContextMenu(parent)
    }

    /**
     * Creates the context menu for the message table.
     * 
     * @param parent The parent control to attach the context menu to.
     * @return The created context menu.
     */
    private fun createMessageContextMenu(parent: Control): Menu {
        val menuMgr = MenuManager()
        menuMgr.add(messageNewAction)
        menuMgr.add(Separator())
        menuMgr.add(messageReplyAction)
        menuMgr.add(messageReplyAllAction)
        menuMgr.add(messageForwardAction)
        menuMgr.add(Separator())
        menuMgr.add(messageCopyAction)
        menuMgr.add(messageMoveAction)
        menuMgr.add(messageArchiveAction)
        menuMgr.add(messageDeleteAction)
        return menuMgr.createContextMenu(parent)
    }

    /**
     * Enables or disables actions based on the current state of the application.
     */
    private fun enableDisableActions() {
        val hasAccounts = bag.accounts.isNotEmpty()
        var hasMessages = false
        var hasDeletedMessages = false
        var isArchive = false
        var isFolderFocused = false

        focusedAccount?.let { account ->
            val folderTrash = account.getFolderByType(FolderType.Trash)
            if (folderTrash != null) {
                hasDeletedMessages = folderTrash.messages.isNotEmpty()
            }
        }

        focusedFolder?.let { folder ->
            hasMessages = folder.messages.isNotEmpty()
            isArchive = folder.type == FolderType.Archive
            isFolderFocused = true
        }

        fileReceiveActions.forEach { it?.isEnabled = hasAccounts }

        fileOpenAction.isEnabled = hasMessages
        fileSaveAsAction.isEnabled = hasMessages
        fileNewFolderAction.isEnabled = hasAccounts
        fileRenameFolderAction.isEnabled = isFolderFocused
        fileDeleteFolderAction.isEnabled = isFolderFocused
        fileSearchFolderAction.isEnabled = isFolderFocused
        fileEmptyTrashAction.isEnabled = hasAccounts and hasDeletedMessages
        viewSourceAction.isEnabled = hasMessages
        messageNewAction.isEnabled = hasAccounts
        messageReplyAction.isEnabled = hasMessages
        messageReplyAllAction.isEnabled = hasMessages
        messageForwardAction.isEnabled = hasMessages
        messageCopyAction.isEnabled = hasMessages
        messageMoveAction.isEnabled = hasMessages
        messageArchiveAction.isEnabled = hasMessages and !isArchive
        messageDeleteAction.isEnabled = hasMessages
    }

    /**
     * Updates the file receive menu with the current file receive actions.
     */
    private fun updateFileReceiveMenu() {
        fileReceiveMenu.removeAll()
        fileReceiveActions.forEach { fileReceiveMenu.add(it) }
        fileReceiveMenu.updateAll(true)
    }

    /**
     * Connects to the database and loads initial data.
     */
    private fun connectDatabase() {
        val databaseFile = context.databaseFile
        try {
            database.connect(databaseFile.toString())
            database.loadAccounts(bag)
            database.loadContacts(bag)
            database.loadOptions(bag)
        } catch (e: DBRuntimeException) {
            dbException = e
        }
    }

    /**
     * Disconnects from the database and saves current state.
     */
    private fun disconnectDatabase() {
        try {
            database.saveOptions(bag)
            database.disconnect()
        } catch (e: DBRuntimeException) {
            MessageDialog.openError(e.message ?: "")
        }
    }

    /**
     * Creates a new message and opens the compose dialog.
     * 
     * @param fromAddressesParam The sender addresses.
     * @param recipientsParam The recipient addresses.
     * @param subjectParam The subject for the message.
     * @param contentParam The content for the message.
     * @param attachmentsParam The attachments for the message.
     */
    private fun createNewMessage(
        fromAddressesParam: String,
        recipientsParam: String,
        subjectParam: String,
        contentParam: String,
        attachmentsParam: AttachmentList = AttachmentList()
    ) {
        val folderDrafts = focusedAccount?.getFolderByType(FolderType.Drafts)
        val folderSent = focusedAccount?.getFolderByType(FolderType.Sent)

        val message = Message().apply {
            fromAddresses = fromAddressesParam
            recipients = recipientsParam
            subject = subjectParam
            content = contentParam
            attachments = attachmentsParam
            folder = folderDrafts
        }

        if (context.hasArguments()) {
            var file: File? = null
            try {
                for (fileName in context.arguments) {
                    file = File(fileName)
                    if (!file.exists()) {
                        throw FileNotFoundException()
                    }
                    AttachmentConverter.convert(file, message)
                }
            } catch (e: IOException) {
                MessageDialog.openError(
                    String.format(I18n.getString("compose_add_attachment_error"), file?.name)
                )
            }

            context.arguments = arrayOf()
        }

        val composeDialog = ComposeDialog(shell, bag, message)
        if (composeDialog.open() == Window.OK) {
            if (message.drafted) {
                storeMessage(message, folderDrafts)
            } else {
                focusedAccount?.let { account ->
                    val client = ClientBuilder.build(account, database, bag.options)
                    try {
                        client.send(message)

                        if (!storeMessage(message, folderSent)) {
                            storeMessage(message, folderDrafts)
                        }
                    } catch (e: SendException) {
                        MessageDialog.openError(e.message ?: "")
                    }
                }
            }
        }
    }

    /**
     * Sorts all messages in all folders based on the current comparator.
     */
    private fun sortAllMessages() {
        val comparator = comparatorPool.getComparator(Message::class.java)
        if (comparator != null && comparator is FieldComparator) {
            for (account in bag.accounts) {
                for (folder in account.folders) {
                    folder.messages.sortWith(comparator)
                }
            }
            if (focusedFolder != null) {
                mailboxViewer.setSelection(StructuredSelection(focusedFolder), true)
            }
        }
    }

    /**
     * Stores a message in the specified folder.
     * 
     * @param message The message to store.
     * @param targetFolder The target folder to store the message in.
     * @return True if the message was successfully stored, false otherwise.
     */
    private fun storeMessage(message: Message, targetFolder: Folder?): Boolean {
        var result = false

        if (targetFolder != null) {
            message.folder = targetFolder
            try {
                database.addMessage(message)
                targetFolder.messages.add(message)
                result = true

                mailboxViewer.refresh(targetFolder, true)
                if (focusedFolder != null) {
                    if (focusedFolder == targetFolder) {
                        mailboxViewer.setSelection(StructuredSelection(targetFolder), true)
                    }
                }
            } catch (e: DBRuntimeException) {
                MessageDialog.openError(e.message ?: "")
            }
        }

        return result
    }

    private inner class FileOpenAction : Action {
        constructor() : super(MenuFactory.getText("file_open_menu")) {
            accelerator = MenuFactory.getAccelerator("file_open_menu")
        }

        override fun run() {
            focusedMessage?.let { message ->
                if (message.unread) {
                    val unread = false
                    try {
                        database.updateMessageUnread(message.id, unread)
                        message.unread = unread
                        mailboxViewer.refresh(focusedFolder, true)
                        mailboxViewer.setSelection(StructuredSelection(focusedFolder), true)
                    } catch (e: DBRuntimeException) {
                    }
                }

                val composeDialog = ComposeDialog(shell, bag, message)
                composeDialog.setReadOnly(true)
                composeDialog.open()
            } ?: MessageDialog.openError(I18n.getString("no_message_selected"))
        }
    }

    private inner class FileSaveAsAction : Action {
        constructor() : super(MenuFactory.getText("file_save_as_menu")) {
            accelerator = MenuFactory.getAccelerator("file_save_as_menu")
        }

        override fun run() {
            focusedMessage?.let { message ->
                val fileChooser = FileChooser(shell)
                fileChooser.fileName = message.subject
                fileChooser.filterExtensions = arrayOf("*.eml", "*.txt")
                fileChooser.filterNames = arrayOf(
                    I18n.getString("file_chooser_filter_eml"),
                    I18n.getString("file_chooser_filter_txt")
                )
                fileChooser.message = I18n.getString("file_chooser_confirm_overwrite")

                val fileName = fileChooser.saveDialog()
                fileName?.let {
                    val file = File(it)
                    val extension = FileUtils.getExtension(file)

                    try {
                        PrintWriter(file).use { writer ->
                            when {
                                extension.equalsIgnoreCase(".eml") -> writer.println(message.rawData)
                                extension.equalsIgnoreCase(".txt") -> writer.println(message.content)
                            }
                            writer.flush()
                        }
                    } catch (e: IOException) {
                        MessageDialog.openError(
                            String.format(I18n.getString("save_file_error"), file.name)
                        )
                    }
                }
            } ?: MessageDialog.openError(I18n.getString("no_message_selected"))
        }
    }

    private inner class FileNewFolderAction : Action(MenuFactory.getText("file_new_folder_menu")) {
        override fun run() {
            focusedAccount?.let { account ->
                val newFolder = Folder().apply {
                    type = FolderType.Custom
                    this.account = account
                }

                val folderDialog = FolderDialog(shell, account.folders, newFolder)
                if (folderDialog.open() == Window.OK) {
                    try {
                        database.addFolder(newFolder)
                        account.folders.add(newFolder)
                        mailboxViewer.refresh(account)
                        mailboxViewer.setSelection(StructuredSelection(newFolder), true)
                    } catch (e: DBRuntimeException) {
                        MessageDialog.openError(e.message ?: "")
                    }
                }
            } ?: MessageDialog.openError(I18n.getString("no_account_selected"))
        }
    }

    private inner class FileRenameFolderAction : Action(MenuFactory.getText("file_rename_folder_menu")) {
        override fun run() {
            focusedFolder?.let { folder ->
                focusedAccount?.let { account ->
                    val folderDialog = FolderDialog(shell, account.folders, folder)
                    if (folderDialog.open() == Window.OK) {
                        try {
                            database.updateFolderName(folder.id, folder.name)
                            mailboxViewer.refresh(folder, true)
                        } catch (e: DBRuntimeException) {
                            MessageDialog.openError(e.message ?: "")
                        }
                    }
                } ?: MessageDialog.openError(I18n.getString("no_account_selected"))
            } ?: MessageDialog.openError(I18n.getString("no_folder_selected"))
        }
    }

    private inner class FileDeleteFolderAction : Action(MenuFactory.getText("file_delete_folder_menu")) {
        override fun run() {
            focusedFolder?.let { folder ->
                focusedAccount?.let { account ->
                    if (MessageDialog.openConfirm(I18n.getString("confirm_delete_folder")) == SWT.YES) {
                        try {
                            database.deleteFolder(folder)
                            account.folders.remove(folder)
                            mailboxViewer.refresh(account)
                            mailboxViewer.setSelection(StructuredSelection(account), true)
                        } catch (e: DBRuntimeException) {
                            MessageDialog.openError(e.message ?: "")
                        }
                    }
                } ?: MessageDialog.openError(I18n.getString("no_account_selected"))
            } ?: MessageDialog.openError(I18n.getString("no_folder_selected"))
        }
    }

    private inner class FileSearchFolderAction : Action(MenuFactory.getText("file_search_folder_menu")) {
        override fun run() {
            focusedFolder?.let { folder ->
                val searchDialog = SearchDialog(shell, folder.messageFilter)
                if (searchDialog.open() == Window.OK) {
                    try {
                        database.applyMessageFilter(folder)
                        mailboxViewer.refresh(folder, true)
                        mailboxViewer.setSelection(StructuredSelection(folder), true)
                    } catch (e: DBRuntimeException) {
                        MessageDialog.openError(e.message ?: "")
                    }
                }
            } ?: MessageDialog.openError(I18n.getString("no_folder_selected"))
        }
    }

    private inner class FileEmptyTrashAction : Action(MenuFactory.getText("file_empty_trash_menu")) {
        override fun run() {
            val folderTrash = focusedAccount?.getFolderByType(FolderType.Trash)
            if (folderTrash == null) {
                MessageDialog.openError(I18n.getString("no_trash_available"))
                return
            }

            if (MessageDialog.openConfirm(I18n.getString("confirm_empty_trash")) == SWT.YES) {
                val messages = folderTrash.messages

                try {
                    while (messages.isNotEmpty()) {
                        database.deleteMessage(messages[0])
                        messages.removeAt(0)
                    }
                } catch (e: DBRuntimeException) {
                    MessageDialog.openError(e.message ?: "")
                }

                mailboxViewer.refresh(folderTrash, true)
                if (focusedFolder != null) {
                    if (focusedFolder == folderTrash) {
                        mailboxViewer.setSelection(StructuredSelection(folderTrash), true)
                    }
                }
            }
        }
    }

    private inner class FileReceiveAction(text: String, private val index: Int) : Action(text) {
        override fun run() {
            val cursor = shell.cursor
            shell.cursor = getDisplay().getSystemCursor(SWT.CURSOR_WAIT)

            when (index) {
                0 -> bag.accounts.forEach { handleTransfer(it) }
                1 -> handleTransfer(focusedAccount!!)
                else -> handleTransfer(bag.accounts[index - 2])
            }

            shell.cursor = cursor

            if (focusedFolder != null) {
                mailboxViewer.setSelection(StructuredSelection(focusedFolder), true)
            }
        }

        private fun handleTransfer(account: Account) {
            val folderInbox = account.getFolderByType(FolderType.Inbox)
            if (folderInbox == null) {
                MessageDialog.openError(I18n.getString("no_inbox_available"))
                return
            }

            val client = ClientBuilder.build(account, database, bag.options)

            try {
                val messages = client.receive()

                try {
                    for (message in messages) {
                        if (!folderInbox.containsMessage(message.emailReference)) {
                            message.folder = folderInbox
                            database.addMessage(message)
                            folderInbox.messages.add(message)
                        }
                    }
                } catch (e: DBRuntimeException) {
                    MessageDialog.openError(e.message ?: "")
                }

                mailboxViewer.refresh(account)
            } catch (e: ReceiveException) {
                MessageDialog.openError(e.message ?: "")
            }
        }
    }

    private inner class FileExitAction : Action(MenuFactory.getText("file_exit_menu")) {
        override fun run() {
            close()
        }
    }

    private inner class ViewSortFieldAction(text: String, val sortField: SortField) :
        Action(text, IAction.AS_CHECK_BOX) {
        override fun run() {
            comparatorPool.changeComparator(Message::class.java, sortField)
            sortAllMessages()
            bag.options.mailSortField = sortField

            viewSortFieldActions.forEach { it.isChecked = false }
            isChecked = true
        }
    }

    private inner class ViewSortOrderAction(text: String, val sortOrder: SortOrder) :
        Action(text, IAction.AS_CHECK_BOX) {
        override fun run() {
            comparatorPool.changeComparator(Message::class.java, sortOrder)
            sortAllMessages()
            bag.options.mailSortOrder = sortOrder

            viewSortOrderActions.forEach { it.isChecked = false }
            isChecked = true
        }
    }

    private inner class ViewSourceAction : Action(MenuFactory.getText("view_source_menu")) {
        override fun run() {
            focusedMessage?.let { message ->
                val sourceDialog = SourceDialog(shell, message.rawData)
                sourceDialog.open()
            } ?: MessageDialog.openError(I18n.getString("no_message_selected"))
        }
    }

    private inner class MessageNewAction : Action {
        constructor() : super(MenuFactory.getText("message_new_menu")) {
            accelerator = MenuFactory.getAccelerator("message_new_menu")
        }

        override fun run() {
            focusedAccount?.let { account ->
                createNewMessage(account.displayNameAndEmail, "", "", "")
            } ?: MessageDialog.openError(I18n.getString("no_account_selected"))
        }
    }

    private inner class MessageReplyAction : Action {
        constructor() : super(MenuFactory.getText("message_reply_menu")) {
            accelerator = MenuFactory.getAccelerator("message_reply_menu")
        }

        override fun run() {
            focusedMessage?.let { message ->
                focusedAccount?.let { account ->
                    var ccLine = ""
                    val recipients = message.fromAddresses

                    val ccAddresses = message.ccAddresses
                    if (ccAddresses.isNotEmpty()) {
                        ccLine = String.format("Cc: %s%n", ccAddresses)
                    }

                    val subject = String.format(I18n.getString("compose_reply_subject"), message.subject)
                    val content = String.format(
                        I18n.getString("compose_reply_content"),
                        message.fromAddresses,
                        SimpleDateFormat(I18n.getString("compose_date_format"), Locale.getDefault())
                            .format(message.sentDate),
                        message.recipients,
                        ccLine,
                        message.subject,
                        message.content
                    )
                    createNewMessage(account.displayNameAndEmail, recipients, subject, content)
                } ?: MessageDialog.openError(I18n.getString("no_account_selected"))
            } ?: MessageDialog.openError(I18n.getString("no_message_selected"))
        }
    }

    private inner class MessageReplyAllAction : Action {
        constructor() : super(MenuFactory.getText("message_reply_all_menu")) {
            accelerator = MenuFactory.getAccelerator("message_reply_all_menu")
        }

        override fun run() {
            focusedMessage?.let { message ->
                focusedAccount?.let { account ->
                    var ccLine = ""
                    var recipients = message.fromAddresses

                    val ccAddresses = message.ccAddresses
                    if (ccAddresses.isNotEmpty()) {
                        ccLine = String.format("Cc: %s%n", ccAddresses)
                        recipients += ", $ccAddresses"
                    }

                    val subject = String.format(I18n.getString("compose_reply_subject"), message.subject)
                    val content = String.format(
                        I18n.getString("compose_reply_content"),
                        message.fromAddresses,
                        SimpleDateFormat(I18n.getString("compose_date_format"), Locale.getDefault())
                            .format(message.sentDate),
                        message.recipients,
                        ccLine,
                        message.subject,
                        message.content
                    )
                    createNewMessage(account.displayNameAndEmail, recipients, subject, content)
                } ?: MessageDialog.openError(I18n.getString("no_account_selected"))
            } ?: MessageDialog.openError(I18n.getString("no_message_selected"))
        }
    }

    private inner class MessageForwardAction : Action {
        constructor() : super(MenuFactory.getText("message_forward_menu")) {
            accelerator = MenuFactory.getAccelerator("message_forward_menu")
        }

        override fun run() {
            focusedMessage?.let { message ->
                focusedAccount?.let { account ->
                    var ccLine = ""
                    var recipients = ""

                    val ccAddresses = message.ccAddresses
                    if (ccAddresses.isNotEmpty()) {
                        ccLine = String.format("Cc: %s%n", ccAddresses)
                    }

                    val subject = String.format(I18n.getString("compose_forward_subject"), message.subject)
                    val content = String.format(
                        I18n.getString("compose_forward_content"),
                        message.fromAddresses,
                        SimpleDateFormat(I18n.getString("compose_date_format"), Locale.getDefault())
                            .format(message.sentDate),
                        message.recipients,
                        ccLine,
                        message.subject,
                        message.content
                    )
                    createNewMessage(account.displayNameAndEmail, recipients, subject, content, message.attachments)
                } ?: MessageDialog.openError(I18n.getString("no_account_selected"))
            } ?: MessageDialog.openError(I18n.getString("no_message_selected"))
        }
    }

    private inner class MessageCopyAction : Action(MenuFactory.getText("message_copy_menu")) {
        override fun run() {
            focusedMessage?.let { message ->
                focusedFolder?.let { folderSrc ->
                    val treeDialog = TreeDialog(shell, bag)
                    if (treeDialog.open() == Window.OK) {
                        val selectedFolder = treeDialog.getSelectedFolder()
                        if (folderSrc != selectedFolder) {
                            val newMessage = message.copy()
                            newMessage.folder = selectedFolder

                            try {
                                database.addMessage(newMessage)
                                selectedFolder.messages.add(newMessage)
                                mailboxViewer.refresh(focusedAccount)
                                mailboxViewer.setSelection(StructuredSelection(folderSrc), true)
                            } catch (e: DBRuntimeException) {
                                MessageDialog.openError(e.message ?: "")
                            }
                        }
                    }
                } ?: MessageDialog.openError(I18n.getString("no_folder_selected"))
            } ?: MessageDialog.openError(I18n.getString("no_message_selected"))
        }
    }

    private inner class MessageMoveAction : Action(MenuFactory.getText("message_move_menu")) {
        override fun run() {
            focusedMessage?.let { message ->
                focusedFolder?.let { folderSrc ->
                    val treeDialog = TreeDialog(shell, bag)
                    if (treeDialog.open() == Window.OK) {
                        val selectedFolder = treeDialog.getSelectedFolder()
                        if (folderSrc != selectedFolder) {
                            try {
                                database.updateMessageFolder(message.id, selectedFolder.id)
                                message.folder = selectedFolder
                                folderSrc.messages.remove(message)
                                selectedFolder.messages.add(message)
                                mailboxViewer.refresh(focusedAccount)
                                mailboxViewer.setSelection(StructuredSelection(folderSrc), true)
                            } catch (e: DBRuntimeException) {
                                MessageDialog.openError(e.message ?: "")
                            }
                        }
                    }
                } ?: MessageDialog.openError(I18n.getString("no_folder_selected"))
            } ?: MessageDialog.openError(I18n.getString("no_message_selected"))
        }
    }

    private inner class MessageArchiveAction : Action(MenuFactory.getText("message_archive_menu")) {
        override fun run() {
            val folderArchive = focusedAccount?.getFolderByType(FolderType.Archive)
            if (folderArchive == null) {
                MessageDialog.openError(I18n.getString("no_archive_available"))
                return
            }

            focusedMessage?.let { message ->
                focusedFolder?.let { folderSrc ->
                    try {
                        database.updateMessageFolder(message.id, folderArchive.id)
                        message.folder = folderArchive
                        folderSrc.messages.remove(message)
                        folderArchive.messages.add(message)
                        mailboxViewer.refresh(focusedAccount)
                        mailboxViewer.setSelection(StructuredSelection(folderSrc), true)
                    } catch (e: DBRuntimeException) {
                        MessageDialog.openError(e.message ?: "")
                    }
                } ?: MessageDialog.openError(I18n.getString("no_folder_selected"))
            } ?: MessageDialog.openError(I18n.getString("no_message_selected"))
        }
    }

    private inner class MessageDeleteAction : Action(MenuFactory.getText("message_delete_menu")) {
        override fun run() {
            val folderTrash = focusedAccount?.getFolderByType(FolderType.Trash)
            if (folderTrash == null) {
                MessageDialog.openError(I18n.getString("no_trash_available"))
                return
            }

            focusedMessage?.let { message ->
                focusedFolder?.let { folderSrc ->
                    try {
                        if (folderTrash == folderSrc) {
                            if (MessageDialog.openConfirm(I18n.getString("confirm_delete_message")) == SWT.NO) {
                                return
                            }
                            database.deleteMessage(message)
                            folderTrash.messages.remove(message)
                        } else {
                            database.updateMessageFolder(message.id, folderTrash.id)
                            message.folder = folderTrash
                            folderSrc.messages.remove(message)
                            folderTrash.messages.add(message)
                        }
                        mailboxViewer.refresh(focusedAccount)
                        mailboxViewer.setSelection(StructuredSelection(folderSrc), true)
                    } catch (e: DBRuntimeException) {
                        MessageDialog.openError(e.message ?: "")
                    }
                } ?: MessageDialog.openError(I18n.getString("no_folder_selected"))
            } ?: MessageDialog.openError(I18n.getString("no_message_selected"))
        }
    }

    private inner class ToolsContactAction : Action(MenuFactory.getText("tools_contact_menu")) {
        override fun run() {
            val contactDialog = ContactDialog(shell, bag, database)
            contactDialog.open()
        }
    }

    private inner class ToolsAccountAction : Action(MenuFactory.getText("tools_account_menu")) {
        override fun run() {
            val accountDialog = AccountDialog(shell, bag, database)
            accountDialog.open()

            mailboxViewer.input = bag
            mailboxViewer.expandAll()
            if (bag.accounts.isNotEmpty()) {
                mailboxViewer.setSelection(StructuredSelection(bag.accounts[0]), true)
            } else {
                mailboxViewer.setSelection(StructuredSelection.EMPTY, false)
            }

            createFileReceiveActions()
            updateFileReceiveMenu()
        }
    }

    private inner class ToolsPrefsAction : Action(MenuFactory.getText("tools_prefs_menu")) {
        override fun run() {
            val optionDialog = OptionDialog(shell, bag.options)
            optionDialog.open()
        }
    }

    private inner class HelpAboutAction : Action(MenuFactory.getText("help_about_menu")) {
        override fun run() {
            MessageDialog.openInformation(
                "${Constants.APP_NAME}${SystemUtils.LINE_BREAK}Version ${Constants.APP_VERSION}${SystemUtils.LINE_BREAK}${SystemUtils.LINE_BREAK}${I18n.getString("published_on_github")}${SystemUtils.LINE_BREAK}${Constants.GITHUB_REPOSITORY_URL}"
            )
        }
    }

    private inner class HelpAppDataFolderAction : Action(MenuFactory.getText("help_appdata_folder_menu")) {
        override fun run() {
            val folder = context.appDataFolder
            try {
                FileUtils.showFolder(folder.toString())
            } catch (e: IOException) {
                MessageDialog.openError(String.format(I18n.getString("show_folder_error"), folder.fileName))
            }
        }
    }
}
