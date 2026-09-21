package com.derived.campusdesk

import android.app.Application
import com.derived.campusdesk.analytics.ArcherBootstrap
import com.derived.campusdesk.di.DevToolsBootstrap
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@HiltAndroidApp
class CampusDeskApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ArcherBootstrap.configure(this)
        ArcherBootstrap.start(applicationScope)
        DevToolsBootstrap.configure(this)
    }
}
