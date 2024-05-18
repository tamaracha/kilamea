package com.github.kilamea.swt

import org.eclipse.swt.graphics.GC
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.widgets.Control

/**
 * Utility object for dimension calculations.
 * 
 * @since 0.1.0
 */
object Dimension {
    /**
     * Calculates the average text size of the control's font.
     * 
     * @param control The control for which the text size is calculated.
     * @return The average character width and height of the control's font.
     */
    fun getTextSize(control: Control): Point {
        val gc = GC(control)
        return try {
            gc.font = control.font
            val fm = gc.fontMetrics
            Point(fm.averageCharacterWidth.toInt(), fm.height)
        } finally {
            gc.dispose()
        }
    }
}
