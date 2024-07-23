package com.github.kilamea.mail

import com.github.kilamea.core.Options
import com.github.kilamea.database.DatabaseManager
import com.github.kilamea.entity.Account

/**
 * A builder object for creating instances of `DefaultClient`.
 *
 * @since 0.2.0
 */
object ClientBuilder {
    /**
     * Builds a `DefaultClient` instance based on the given account and options.
     *
     * @param account The account to be used by the client.
     * @param database The database manager to be used by the client.
     * @param options The options to be used by the client.
     * @return A `DefaultClient` instance. If the account is a Gmail account,
     *         a `GmailClient` instance is returned; otherwise, a `DefaultClient` instance is returned.
     */
    fun build(account: Account, database: DatabaseManager, options: Options): DefaultClient {
        return if (Account.isGmail(account.email)) {
            GmailClient(account, database, options)
        } else {
            DefaultClient(account, database, options)
        }
    }
}
