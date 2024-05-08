package com.github.kilamea.view

import org.eclipse.jface.viewers.ITreeContentProvider

import com.github.kilamea.core.Bag
import com.github.kilamea.entity.Account

internal class MailboxViewerContentProvider : ITreeContentProvider {
    override fun getChildren(parentElement: Any?): Array<Any> {
        return when (parentElement) {
            is Bag -> parentElement.accounts.toTypedArray()
            is Account -> parentElement.folders.toTypedArray()
            else -> arrayOf()
        }
    }

    override fun getElements(inputElement: Any?): Array<Any> {
        return getChildren(inputElement)
    }

    override fun getParent(element: Any): Any? {
        return null
    }

    override fun hasChildren(element: Any?): Boolean {
        return getChildren(element).isNotEmpty()
    }
}
