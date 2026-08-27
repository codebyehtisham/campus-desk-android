package com.derived.campusdesk.di

import android.content.Context
import com.derived.campusdesk.BuildConfig
import com.derived.campusdesk.networking.debug.DevToolsConfig
import com.derived.campusdesk.ui.debug.ShakeDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

object DevToolsBootstrap {
    private var shakeDetector: ShakeDetector? = null

    fun configure(context: Context) {
        if (BuildConfig.DEBUG && BuildConfig.DEV_TOOLS) {
            shakeDetector = ShakeDetector(context.applicationContext).also { it.start() }
        }
    }

    fun stop() {
        shakeDetector?.stop()
        shakeDetector = null
    }
}

@Singleton
class AppDevToolsConfig @Inject constructor() : DevToolsConfig {
    override val isDevToolsEnabled: Boolean = BuildConfig.DEBUG && BuildConfig.DEV_TOOLS
}
