package com.glow.app

import android.app.Application

class GlowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GlowStrings.init(this)
    }
}
