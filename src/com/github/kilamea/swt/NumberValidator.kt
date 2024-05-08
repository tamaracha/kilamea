package com.github.kilamea.swt

import org.eclipse.swt.events.VerifyEvent
import org.eclipse.swt.events.VerifyListener
import org.eclipse.swt.widgets.Text

class NumberValidator : VerifyListener {
    override fun verifyText(event: VerifyEvent) {
        val textField = event.getSource() as Text
        val oldStr = textField.text
        val newStr = oldStr.substring(0, event.start) + event.text + oldStr.substring(event.end)

        var isInt = true
        if (newStr.isNotEmpty()) {
            try {
                newStr.toInt()
            } catch (e: NumberFormatException) {
                isInt = false
            }
        }

        if (!isInt) {
            event.doit = false
        }
    }
}
