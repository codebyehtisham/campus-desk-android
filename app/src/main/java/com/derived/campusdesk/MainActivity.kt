package com.derived.campusdesk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.derived.campusdesk.analytics.ArcherPushBootstrap
import com.derived.campusdesk.networking.api.ApiEnvironmentStore
import com.derived.campusdesk.networking.debug.DevToolsConfig
import com.derived.campusdesk.auth.SessionStore
import com.derived.campusdesk.sharedui.theme.CampusDeskTheme
import com.derived.campusdesk.ui.CampusRoot
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionStore: SessionStore
    @Inject lateinit var devToolsConfig: DevToolsConfig
    @Inject lateinit var environmentStore: ApiEnvironmentStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ArcherPushBootstrap.activate(this)
        setContent {
            CampusDeskTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CampusRoot(
                        sessionStore = sessionStore,
                        devToolsConfig = devToolsConfig,
                        environmentStore = environmentStore,
                    )
                }
            }
        }
    }
}
