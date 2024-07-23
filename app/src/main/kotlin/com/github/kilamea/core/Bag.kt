package com.github.kilamea.core

import com.github.kilamea.entity.AbstractEntity
import com.github.kilamea.entity.AccountList
import com.github.kilamea.entity.ContactList
import com.github.kilamea.entity.Folder

/**
 * Holds the main data structures for accounts, contacts, and options.
 *
 * @since 0.1.0
 * @property accounts The list of email accounts.
 * @property contacts The list of contacts.
 * @property options The application options.
 */
class Bag {
    var accounts: AccountList = AccountList()
    var contacts: ContactList = ContactList()
    var options: Options = Options()

    /**
     * Finds the last accessed mailbox entry.
     *
     * @return The last accessed mailbox entry if found, otherwise null.
     */
    fun findLastMailboxEntry(): AbstractEntity? {
        var treeItem: AbstractEntity? = null

        if (accounts.isNotEmpty()) {
            treeItem = accounts[0]

            val lastMailboxEntry = options.lastMailboxEntry
            for (account in accounts) {
                var found = false

                if (account.id == lastMailboxEntry) {
                    treeItem = account
                    found = true
                } else {
                    for (folder in account.folders) {
                        if (folder.id == lastMailboxEntry) {
                            treeItem = folder
                            found = true
                            break
                        }
                    }
                }

                if (found) {
                    break
                }
            }
        }

        return treeItem
    }
}
