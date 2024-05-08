package com.github.kilamea.swt

import org.eclipse.jface.dialogs.Dialog
import org.eclipse.jface.dialogs.IDialogConstants
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Shell

import com.github.kilamea.i18n.I18n
import com.github.kilamea.util.SystemUtils

abstract class ModalDialog(parentShell: Shell, private val buttonKeys: Array<String>) : Dialog(parentShell) {
    override fun createButtonsForButtonBar(parent: Composite) {
        if (buttonKeys.isNotEmpty()) {
            if (isButtonBarForAssistant()) {
                createButtonBarForAssistant(parent)
            } else {
                if (SystemUtils.isMac()) {
                    for (i in buttonKeys.indices.reversed()) {
                        createButtonForIndex(parent, i)
                    }
                } else {
                    for (i in buttonKeys.indices) {
                        createButtonForIndex(parent, i)
                    }
                }
            }
        } else {
            val gridLayout = parent.layout as GridLayout
            gridLayout.marginHeight = 0
        }
    }

    private fun createButtonBarForAssistant(parent: Composite) {
        var backButtonText = "back_button"
        var nextButtonText = "next_button"
        var cancelButtonText = "cancel_button"
        if (buttonKeys.size == 3) {
            backButtonText = buttonKeys[0]
            nextButtonText = buttonKeys[1]
            cancelButtonText = buttonKeys[2]
        }

        val backButton = createButton(parent, IDialogConstants.BACK_ID, I18n.getString(backButtonText), false)
        backButton.isEnabled = false
        createButton(parent, IDialogConstants.NEXT_ID, I18n.getString(nextButtonText), true)
        createButton(parent, IDialogConstants.CANCEL_ID, I18n.getString(cancelButtonText), false)
    }

    private fun createButtonForIndex(parent: Composite, index: Int) {
        var buttonText = buttonKeys.getOrNull(index) ?: "button${index + 1}"

        when (index) {
            0 -> createButton(parent, IDialogConstants.OK_ID, I18n.getString(buttonText), true)
            1 -> createButton(parent, IDialogConstants.CANCEL_ID, I18n.getString(buttonText), false)
            else -> createButton(parent, IDialogConstants.INTERNAL_ID, I18n.getString(buttonText), false)
        }
    }

    private fun isButtonBarForAssistant(): Boolean {
        return (buttonKeys.size == 3 && buttonKeys[0] == "back_button" && buttonKeys[1] == "next_button" && buttonKeys[2] == "cancel_button")
    }
}
