package com.github.kilamea.swt

import org.eclipse.swt.SWT
import org.eclipse.swt.events.TraverseEvent
import org.eclipse.swt.events.TraverseListener

/**
 * A listener to enable tab traversal.
 * 
 * @since 0.1.0
 */
class TabTraverse : TraverseListener {
    /**
     * Handles tab traversal events.
     * 
     * @param event The traverse event.
     */
    override fun keyTraversed(event: TraverseEvent) {
        if (event.detail == SWT.TRAVERSE_TAB_NEXT || event.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
            event.doit = true
        }
    }
}
