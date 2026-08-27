package com.derived.campusdesk.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.derived.campusdesk.networking.debug.ApiResponseLog
import com.derived.campusdesk.networking.debug.NetworkResponseStore
import com.derived.campusdesk.sharedui.components.BrandButton
import com.derived.campusdesk.sharedui.components.BrandButtonKind
import com.derived.campusdesk.sharedui.theme.campusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiLogsSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember { mutableStateOf<ApiResponseLog?>(null) }
    val logs = remember { NetworkResponseStore.all() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("API Logs", style = MaterialTheme.typography.titleLarge, color = campusColors().ink)
                BrandButton(
                    text = "Clear",
                    onClick = { NetworkResponseStore.clear() },
                    kind = BrandButtonKind.Ghost,
                    modifier = Modifier.fillMaxWidth(0.35f),
                )
            }
            if (selected == null) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(logs) { log ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected = log }
                                .background(campusColors().staffSurface)
                                .padding(12.dp),
                        ) {
                            Text("${log.method} ${log.statusCode}", color = campusColors().ink)
                            Text(log.url, color = campusColors().muted, style = MaterialTheme.typography.bodySmall)
                            Text("${log.durationMs} ms", color = campusColors().brandBlue, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else {
                val log = selected!!
                Text("${log.method} ${log.statusCode}", color = campusColors().ink, style = MaterialTheme.typography.titleMedium)
                Text(log.url, color = campusColors().muted)
                Text("Request", color = campusColors().brandRed, style = MaterialTheme.typography.labelLarge)
                Text(log.requestBody.orEmpty(), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                Text("Response", color = campusColors().brandRed, style = MaterialTheme.typography.labelLarge)
                Text(log.responseBody.orEmpty(), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                BrandButton(text = "Back", onClick = { selected = null }, kind = BrandButtonKind.Secondary)
            }
        }
    }
}

@Composable
fun DevEnvironmentBanner(host: String, modifier: Modifier = Modifier) {
    Text(
        text = "DEV · $host",
        modifier = modifier
            .background(campusColors().brandRed.copy(alpha = 0.92f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        color = campusColors().onBrand,
        style = MaterialTheme.typography.labelMedium,
    )
}
