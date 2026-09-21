@file:OptIn(ExperimentalMaterial3Api::class)

package com.derived.campusdesk.courses.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.derived.campusdesk.sharedui.components.AsyncStateView
import com.derived.campusdesk.sharedui.components.CampusCard
import com.derived.campusdesk.sharedui.components.Eyebrow
import com.derived.campusdesk.sharedui.components.ScreenBackground
import com.derived.campusdesk.sharedui.theme.CampusLayout
import com.derived.campusdesk.sharedui.theme.CampusTypography
import com.derived.campusdesk.sharedui.theme.campusColors

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onBack: () -> Unit,
    onLeave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val colors = campusColors()

    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier.fillMaxSize()) {
        ScreenBackground()
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                        .shadow(
                            8.dp,
                            CircleShape,
                            ambientColor = colors.ink.copy(alpha = 0.12f),
                            spotColor = colors.ink.copy(alpha = 0.12f),
                        )
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = colors.ink,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    "Notifications",
                    style = CampusTypography.Title,
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            PullToRefreshBox(
                isRefreshing = isLoading && notifications.isNotEmpty(),
                onRefresh = { viewModel.load() },
                modifier = Modifier.fillMaxSize(),
            ) {
                AsyncStateView(
                    isLoading = isLoading && notifications.isEmpty(),
                    errorMessage = errorMessage,
                    isEmpty = notifications.isEmpty(),
                    emptyTitle = "All caught up",
                    emptyMessage = "Leave decisions and class updates will appear here.",
                    emptyIcon = Icons.Default.Notifications,
                    onRetry = { viewModel.load() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 32.dp)
                            .widthIn(max = CampusLayout.ContentMaxWidth.dp)
                            .align(Alignment.TopCenter),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        notifications.forEach { item ->
                            CampusCard(
                                onClick = {
                                    val leaveId = item.leaveId?.value
                                    if (item.isLeaveDecision && !leaveId.isNullOrBlank()) {
                                        onLeave(leaveId)
                                    }
                                },
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (item.isLeaveDecision) Eyebrow("Leave decision", color = colors.brandBlue)
                                    Text(item.title ?: "Notification", style = CampusTypography.Headline, color = colors.ink)
                                    if (item.displayBody.isNotBlank()) {
                                        Text(item.displayBody, style = CampusTypography.Body, color = colors.muted)
                                    }
                                    item.createdAt?.let {
                                        Text(it.take(16), style = CampusTypography.Caption, color = colors.muted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
