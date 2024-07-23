package com.github.kilamea.view

import org.eclipse.jface.viewers.ITreeContentProvider

import com.github.kilamea.core.Bag
import com.github.kilamea.entity.Account

/**
 * Provides content for the mailbox viewer.
 * 
 * @since 0.1.0
 */
internal class MailboxViewerContentProvider : ITreeContentProvider {
    /**
     * Returns the children of the given parent element.
     * 
     * @param parentElement The parent element.
     * @return An array of child elements.
     */
    override fun getChildren(parentElement: Any?): Array<Any> {
        return when (parentElement) {
            is Bag -> parentElement.accounts.toTypedArray()
            is Account -> parentElement.folders.toTypedArray()
            else -> arrayOf()
        }
    }

    /**
     * Returns the elements to display in the viewer when its input is set to the given element.
     * 
     * @param inputElement The input element.
     * @return An array of elements.
     */
    override fun getElements(inputElement: Any?): Array<Any> {
        return getChildren(inputElement)
    }

    /**
     * Returns the parent element of the given element.
     * 
     * @param element The element.
     * @return The parent element, or null if there is no parent.
     */
    override fun getParent(element: Any): Any? {
        return null
    }

    /**
     * Returns whether the given element has children.
     * 
     * @param element The element.
     * @return True if the element has children, false otherwise.
     */
    override fun hasChildren(element: Any?): Boolean {
        return getChildren(element).isNotEmpty()
    }
}
