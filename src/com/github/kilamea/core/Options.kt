package com.github.kilamea.core

import com.github.kilamea.sort.SortField
import com.github.kilamea.sort.SortOrder

data class Options(
    var lastMailboxEntry: String = "",
    var mailSortField: SortField = SortField.SentDate,
    var mailSortOrder: SortOrder = SortOrder.Ascending,
    var contactSortField: SortField = SortField.Email,
    var contactSortOrder: SortOrder = SortOrder.Ascending,
    var retrieveOnStart: Boolean = true,
    var deleteFromServer: Boolean = false
)
