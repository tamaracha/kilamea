package com.github.kilamea.swt

import org.eclipse.swt.SWT
import org.eclipse.swt.events.TraverseEvent
import org.eclipse.swt.events.TraverseListener

class TabTraverse : TraverseListener {
    override fun keyTraversed(event: TraverseEvent) {
        if (event.detail == SWT.TRAVERSE_TAB_NEXT || event.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
            event.doit = true
        }
    }
}
