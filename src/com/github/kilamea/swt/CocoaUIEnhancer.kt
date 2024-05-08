package com.github.kilamea.swt

import org.eclipse.jface.action.IAction
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Event
import org.eclipse.swt.widgets.Listener
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.MenuItem

object CocoaUIEnhancer {
    fun hookApplicationMenu(aboutAction: IAction?, prefsAction: IAction?, exitAction: IAction?) {
        val display = Display.getCurrent()

        val systemMenu = display.systemMenu
        for (systemItem in systemMenu.items) {
            when (systemItem.id) {
                SWT.ID_ABOUT -> {
                    if (aboutAction != null) {
                        systemItem.addSelectionListener(object : SelectionAdapter() {
                            override fun widgetSelected(event: SelectionEvent) {
                                aboutAction.run()
                            }
                        })
                    }
                }
                SWT.ID_PREFERENCES -> {
                    if (prefsAction != null) {
                        systemItem.addSelectionListener(object : SelectionAdapter() {
                            override fun widgetSelected(event: SelectionEvent) {
                                prefsAction.run()
                            }
                        })
                    }
                }
                SWT.ID_QUIT -> {
                    if (exitAction != null) {
                        systemItem.addSelectionListener(object : SelectionAdapter() {
                            override fun widgetSelected(event: SelectionEvent) {
                                exitAction.run()
                            }
                        })
                    }
                }
            }
        }

        display.addFilter(SWT.KeyDown, Listener { event ->
            if (event.stateMask and SWT.COMMAND == SWT.COMMAND && event.keyCode == 'w'.code) {
                event.doit = false
                display.activeShell?.close()
            }
        })
    }
}
