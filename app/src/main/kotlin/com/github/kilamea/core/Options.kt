package com.github.kilamea.core

import com.github.kilamea.sort.SortField
import com.github.kilamea.sort.SortOrder

/**
 * Represents the options for sorting and retrieving emails and contacts.
 *
 * @since 0.1.0
 * @property lastMailboxEntry The ID of the last accessed mailbox entry.
 * @property mailSortField The field used for sorting emails.
 * @property mailSortOrder The order in which to sort emails.
 * @property contactSortField The field used for sorting contacts.
 * @property contactSortOrder The order in which to sort contacts.
 * @property retrieveOnStart Flag indicating whether to retrieve emails on application start.
 * @property deleteFromServer Flag indicating whether to delete emails from the server after retrieval.
 */
data class Options(
    var lastMailboxEntry: String = "",
    var mailSortField: SortField = SortField.SentDate,
    var mailSortOrder: SortOrder = SortOrder.Ascending,
    var contactSortField: SortField = SortField.Email,
    var contactSortOrder: SortOrder = SortOrder.Ascending,
    var retrieveOnStart: Boolean = false,
    var deleteFromServer: Boolean = false
)
