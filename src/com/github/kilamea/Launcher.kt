package com.github.kilamea

import com.github.kilamea.core.Context
import com.github.kilamea.view.Kilamea

object Launcher {
    @JvmStatic
    fun main(args: Array<String>) {
        val app = Kilamea(Context(args))
        app.run()
    }
}
