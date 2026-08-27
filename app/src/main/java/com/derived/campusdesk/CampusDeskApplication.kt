package com.derived.campusdesk

import android.app.Application
import com.derived.campusdesk.di.DevToolsBootstrap
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CampusDeskApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DevToolsBootstrap.configure(this)
    }
}
