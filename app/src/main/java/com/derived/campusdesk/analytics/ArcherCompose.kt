package com.derived.campusdesk.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.derived.campusdesk.networking.analytics.ArcherScreenAnalytics

/** Records `screen_viewed` when this screen appears. Use snake_case names. */
fun Modifier.archerScreen(name: String): Modifier = composed {
    DisposableEffect(name) {
        ArcherScreenAnalytics.push(name)
        onDispose { ArcherScreenAnalytics.pop(name) }
    }
    this
}

@Composable
fun ArcherScreenEffect(name: String) {
    DisposableEffect(name) {
        ArcherScreenAnalytics.push(name)
        onDispose { ArcherScreenAnalytics.pop(name) }
    }
}
