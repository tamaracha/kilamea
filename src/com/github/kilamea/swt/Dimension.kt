package com.github.kilamea.swt

import org.eclipse.swt.graphics.GC
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.widgets.Control

object Dimension {
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
