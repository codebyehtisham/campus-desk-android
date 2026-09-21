@file:OptIn(ExperimentalMaterial3Api::class)

package com.derived.campusdesk.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.derived.campusdesk.home.HomeViewModel
import com.derived.campusdesk.networking.models.QuizMarkSummary
import com.derived.campusdesk.networking.models.User
import com.derived.campusdesk.sharedui.components.AsyncStateView
import com.derived.campusdesk.sharedui.components.AvatarView
import com.derived.campusdesk.sharedui.components.CampusCard
import com.derived.campusdesk.sharedui.components.CampusLoadingView
import com.derived.campusdesk.sharedui.components.EmptyStateView
import com.derived.campusdesk.sharedui.components.Eyebrow
import com.derived.campusdesk.sharedui.components.MetaChip
import com.derived.campusdesk.sharedui.components.QuickStatCard
import com.derived.campusdesk.sharedui.components.SectionHeader
import com.derived.campusdesk.sharedui.components.ScreenBackground
import com.derived.campusdesk.sharedui.components.campusHeroGradient
import com.derived.campusdesk.sharedui.components.campusPillShape
import com.derived.campusdesk.sharedui.motion.CampusHeroDecor
import com.derived.campusdesk.sharedui.theme.CampusLayout
import com.derived.campusdesk.sharedui.theme.CampusRadius
import com.derived.campusdesk.sharedui.theme.CampusTypography
import com.derived.campusdesk.sharedui.theme.campusColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    user: User,
    onOpenClasses: () -> Unit,
    onOpenAssignments: () -> Unit,
    onOpenLeaves: () -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dashboard by viewModel.dashboard.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isNotEnrolled by viewModel.isNotEnrolled.collectAsState()
    val unread = notifications.count { it.read != true }
    val isEmpty = dashboard == null

    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier.fillMaxSize()) {
        ScreenBackground()
        when {
            isNotEnrolled -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateView(
                    title = "Not enrolled yet",
                    message = "You are not assigned to any classes. Contact your institute once enrollment is complete.",
                )
            }
            isLoading && isEmpty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CampusLoadingView(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
            }
            else -> PullToRefreshBox(
                isRefreshing = isLoading && !isEmpty,
                onRefresh = { viewModel.load() },
                modifier = Modifier.fillMaxSize(),
            ) {
                AsyncStateView(
                    isLoading = false,
                    errorMessage = errorMessage,
                    isEmpty = isEmpty,
                    emptyTitle = "Dashboard unavailable",
                    emptyMessage = "Pull to refresh when you're back online.",
                    onRetry = { viewModel.load() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val data = dashboard!!
                    Box(Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp)
                                .statusBarsPadding()
                                .padding(top = 16.dp)
                                .padding(bottom = CampusLayout.TabScrollContentInset.dp)
                                .widthIn(max = CampusLayout.ContentMaxWidth.dp)
                                .align(Alignment.TopCenter),
                            verticalArrangement = Arrangement.spacedBy(22.dp),
                        ) {
                            HeroCard(user = user)
                            if (!data.openSessions.isNullOrEmpty()) {
                                OpenSessionBanner(
                                    sessionName = data.openSessions!!.first().displayName,
                                    onAttendance = onOpenAttendance,
                                )
                            }
                            StatsRow(
                                assignments = data.pendingAssignments ?: 0,
                                leaves = data.pendingLeaves ?: 0,
                                classes = data.classCount ?: 0,
                                onAssignments = onOpenAssignments,
                                onLeaves = onOpenLeaves,
                                onClasses = onOpenClasses,
                            )
                            CheckInTile(onOpenAttendance = onOpenAttendance)
                            QuizMarksSection(marks = data.recentQuizMarks.orEmpty())
                        }
                        NotificationBell(
                            unread = unread,
                            onClick = onOpenNotifications,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(top = 8.dp, end = 20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationBell(
    unread: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = campusColors()
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.surface.copy(alpha = 0.92f))
            .border(1.dp, colors.stroke, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Notifications,
            contentDescription = "Notifications",
            tint = colors.brandBlue,
            modifier = Modifier.size(20.dp),
        )
        if (unread > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(colors.brandRed),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    minOf(unread, 9).toString(),
                    color = colors.onBrand,
                    style = CampusTypography.Caption.copy(fontSize = 9.sp),
                )
            }
        }
    }
}

@Composable
private fun HeroCard(user: User) {
    val colors = campusColors()
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val dateLabel = SimpleDateFormat("EEEE, d MMM", Locale.getDefault())
        .format(Date())
        .uppercase(Locale.getDefault())
    val shape = RoundedCornerShape(CampusRadius.Lg.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                24.dp,
                shape,
                ambientColor = colors.brandBlue.copy(alpha = 0.28f),
                spotColor = colors.brandBlue.copy(alpha = 0.28f),
            )
            .clip(shape)
            .background(campusHeroGradient(colors)),
    ) {
        CampusHeroDecor(
            modifier = Modifier.matchParentSize(),
            strokeAlpha = 0.14f,
            gridAlpha = 0.12f,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Eyebrow(dateLabel, color = colors.onBrand.copy(alpha = 0.82f))
                Text(greeting, style = CampusTypography.Callout, color = colors.onBrand.copy(alpha = 0.86f))
                Text(
                    user.displayName,
                    style = CampusTypography.Display,
                    color = colors.onBrand,
                )
                Text(
                    "Your learning dashboard",
                    style = CampusTypography.Callout,
                    color = colors.onBrand.copy(alpha = 0.86f),
                )
                MetaChip("Student LMS", tint = colors.onBrand.copy(alpha = 0.9f))
            }
            AvatarView(user.displayName, size = 56)
        }
    }
}

@Composable
private fun OpenSessionBanner(sessionName: String, onAttendance: () -> Unit) {
    val colors = campusColors()
    CampusCard(onClick = onAttendance) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = colors.brandBlue,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Eyebrow("Open attendance", color = colors.brandBlue)
                Text("QR session active — tap to check in", style = CampusTypography.Headline, color = colors.ink)
                Text(sessionName, style = CampusTypography.Caption, color = colors.muted)
            }
            Text(
                "Scan",
                style = CampusTypography.Caption,
                color = colors.brandBlue,
                modifier = Modifier.clickable(onClick = onAttendance),
            )
        }
    }
}

@Composable
private fun StatsRow(
    assignments: Int,
    leaves: Int,
    classes: Int,
    onAssignments: () -> Unit,
    onLeaves: () -> Unit,
    onClasses: () -> Unit,
) {
    val colors = campusColors()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickStatCard(
            title = "Assignments",
            value = assignments.toString(),
            icon = Icons.Default.Description,
            tint = colors.brandRed,
            modifier = Modifier.weight(1f),
            onClick = onAssignments,
        )
        QuickStatCard(
            title = "Leaves",
            value = leaves.toString(),
            icon = Icons.Default.CalendarMonth,
            tint = colors.brandBlue,
            modifier = Modifier.weight(1f),
            onClick = onLeaves,
        )
        QuickStatCard(
            title = "Classes",
            value = classes.toString(),
            icon = Icons.Default.MenuBook,
            tint = colors.success,
            modifier = Modifier.weight(1f),
            onClick = onClasses,
        )
    }
}

@Composable
private fun CheckInTile(onOpenAttendance: () -> Unit) {
    val colors = campusColors()
    val shape = RoundedCornerShape(CampusRadius.Lg.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = colors.ink.copy(alpha = 0.07f), spotColor = colors.ink.copy(alpha = 0.07f))
            .clip(shape)
            .background(colors.surface.copy(alpha = 0.96f))
            .border(1.dp, colors.stroke.copy(alpha = 0.7f), shape)
            .clickable(onClick = onOpenAttendance)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.brandRed.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = colors.brandRed,
                modifier = Modifier.size(26.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Check in now", style = CampusTypography.Headline, color = colors.ink)
            Text("Scan QR at your classroom", style = CampusTypography.Caption, color = colors.muted)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.brandBlue,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun QuizMarksSection(marks: List<QuizMarkSummary>) {
    val colors = campusColors()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Recent quiz marks")
        if (marks.isEmpty()) {
            CampusCard {
                EmptyStateView(
                    title = "No quiz marks yet",
                    message = "Marks from in-class quizzes will show up here.",
                    icon = Icons.Default.Verified,
                )
            }
        } else {
            marks.take(6).forEach { mark ->
                CampusCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(mark.displayTitle, style = CampusTypography.Headline, color = colors.ink)
                            mark.className?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = CampusTypography.Caption, color = colors.muted)
                            }
                        }
                        Text(
                            mark.displayMarks,
                            style = CampusTypography.Title,
                            color = colors.brandBlue,
                            modifier = Modifier
                                .clip(campusPillShape())
                                .background(colors.brandBlue.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
