package com.bildirimbutce.app

import android.app.Application
import com.bildirimbutce.app.data.PatternProvider

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Desenleri onceden derle: ilk bildirim geldiginde gecikme olmasin.
        PatternProvider.parser(this)
    }
}
