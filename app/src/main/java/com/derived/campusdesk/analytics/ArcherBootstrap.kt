package com.derived.campusdesk.analytics

import android.content.Context
import android.util.Log
import co.archer.sdk.Archer
import co.archer.sdk.ArcherConfiguration
import co.archer.sdk.ArcherEnvironment
import co.archer.sdk.LogLevel
import com.derived.campusdesk.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Archer SDK configure + start. Host supplies only apiKey + environment;
 * ingest URL is owned by the SDK. Mirrors Traject `ArcherBootstrap` / iOS CampusAnalytics.
 */
object ArcherBootstrap {
    private const val TAG = "ArcherBootstrap"

    /** Call once from [android.app.Application.onCreate] before any log / analytics / crash APIs. */
    fun configure(context: Context) {
        Archer.configure(
            context.applicationContext,
            ArcherConfiguration(
                apiKey = BuildConfig.ARCHER_API_KEY,
                environment = environment,
            ),
        )
    }

    /** Registers the device, installs crash capture, then emits launch signals. */
    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                Archer.start()
                Archer.log("app_started", LogLevel.INFO)
                Archer.analytics("app_opened")
            } catch (t: Throwable) {
                Log.e(TAG, "Archer start failed", t)
                Archer.log("start_failed: ${t.message}", LogLevel.ERROR)
            }
        }
    }

    private val environment: ArcherEnvironment
        get() = when (BuildConfig.ARCHER_ENVIRONMENT.lowercase()) {
            "production", "prod" -> ArcherEnvironment.PRODUCTION
            "staging" -> ArcherEnvironment.STAGING
            else -> ArcherEnvironment.DEVELOPMENT
        }
}
