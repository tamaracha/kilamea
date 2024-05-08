package com.github.kilamea.core

import com.github.kilamea.entity.AbstractEntity
import com.github.kilamea.entity.AccountList
import com.github.kilamea.entity.ContactList
import com.github.kilamea.entity.Folder

class Bag {
    var accounts: AccountList = AccountList()
    var contacts: ContactList = ContactList()
    var options: Options = Options()

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
