@file:OptIn(ExperimentalMaterial3Api::class)

package com.derived.campusdesk.home.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.derived.campusdesk.home.HomeViewModel
import com.derived.campusdesk.networking.models.FacultyMember
import com.derived.campusdesk.networking.models.NewsItem
import com.derived.campusdesk.networking.models.User
import com.derived.campusdesk.sharedui.components.AsyncStateView
import com.derived.campusdesk.sharedui.components.AvatarView
import com.derived.campusdesk.sharedui.components.CampusCard
import com.derived.campusdesk.sharedui.components.CampusLoadingView
import com.derived.campusdesk.sharedui.components.Eyebrow
import com.derived.campusdesk.sharedui.components.SectionHeader
import com.derived.campusdesk.sharedui.components.campusHeroGradient
import com.derived.campusdesk.sharedui.motion.CampusHeroDecor
import com.derived.campusdesk.sharedui.motion.CampusLiveBackdrop
import com.derived.campusdesk.sharedui.theme.CampusLayout
import com.derived.campusdesk.sharedui.theme.CampusRadius
import com.derived.campusdesk.sharedui.theme.CampusSpacing
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
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsState()
    val news by viewModel.news.collectAsState()
    val faculty by viewModel.faculty.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val isEmpty = settings == null && news.isEmpty() && faculty.isEmpty()

    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier.fillMaxSize()) {
        CampusLiveBackdrop()
        when {
            isLoading && isEmpty -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CampusLoadingView(
                        message = "Loading campus…",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                    )
                }
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
                    emptyTitle = "Campus unavailable",
                    emptyMessage = "Pull to refresh when you're back online.",
                    onRetry = { viewModel.load() },
                    modifier = Modifier.fillMaxSize(),
                ) {
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
                        HeroCard(
                            user = user,
                            tagline = settings?.displayTagline ?: "Ready when the bell rings.",
                        )
                        ScanTile(onScan = onScan)
                        QuickStats(newsCount = news.size, facultyCount = faculty.size)
                        NewsSection(news = news)
                        FacultySection(faculty = faculty.take(10))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(user: User, tagline: String) {
    val colors = campusColors()
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val dateLabel = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        .format(Date())
        .uppercase(Locale.getDefault())
    val shape = RoundedCornerShape(CampusRadius.Lg.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, shape, ambientColor = colors.brandBlue.copy(alpha = 0.28f), spotColor = colors.brandBlue.copy(alpha = 0.28f))
            .clip(shape)
            .background(campusHeroGradient(colors))
            .height(214.dp),
    ) {
        CampusHeroDecor(
            modifier = Modifier.fillMaxSize(),
            strokeAlpha = 0.14f,
            gridAlpha = 0.12f,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow(dateLabel, color = colors.onBrand.copy(alpha = 0.82f))
                Spacer(Modifier.weight(1f))
                AvatarView(user.displayName, size = 44)
            }
            Text(greeting, style = CampusTypography.Callout, color = colors.onBrand.copy(alpha = 0.86f))
            Text(
                user.firstName.ifBlank { user.displayName },
                style = CampusTypography.Display,
                color = colors.onBrand,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(tagline, style = CampusTypography.Callout, color = colors.onBrand.copy(alpha = 0.86f))
        }
    }
}

@Composable
private fun ScanTile(onScan: () -> Unit) {
    val colors = campusColors()
    val shape = RoundedCornerShape(CampusRadius.Md.dp)
    val pulse = rememberInfiniteTransition(label = "scanPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "scanAlpha",
    )
    val pulseScale by pulse.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "scanScale",
    )
    val borderAlpha by pulse.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "scanBorder",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.copy(alpha = 0.72f))
            .border(1.dp, colors.brandRed.copy(alpha = borderAlpha), shape)
            .clickable(onClick = onScan)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.brandRed.copy(alpha = pulseAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = colors.brandRed,
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale },
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Check in now", style = CampusTypography.Headline, color = colors.ink)
            Text(
                "Scan the lecture QR. Attendance lands in the campus record.",
                style = CampusTypography.Caption,
                color = colors.muted,
                maxLines = 2,
            )
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = colors.brandBlue, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun QuickStats(newsCount: Int, facultyCount: Int) {
    val colors = campusColors()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("News", newsCount.toString(), colors.brandRed, Modifier.weight(1f))
        StatCard("Faculty", facultyCount.toString(), colors.brandBlue, Modifier.weight(1f))
        StatCard("Portal", "Live", colors.success, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    CampusCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                label.uppercase(),
                style = CampusTypography.Eyebrow.copy(letterSpacing = 1.1.sp),
                color = campusColors().muted,
            )
            Text(value, style = CampusTypography.Title, color = tint)
        }
    }
}

@Composable
private fun NewsSection(news: List<NewsItem>) {
    if (news.isEmpty()) return
    val colors = campusColors()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Campus desk", subtitle = "What's moving this week")
        news.firstOrNull()?.let { featured ->
            CampusCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Eyebrow("Lead story")
                    Text(featured.title ?: "News", style = CampusTypography.Title, color = colors.ink)
                    if (featured.displayBody.isNotBlank()) {
                        Text(featured.displayBody, style = CampusTypography.Body, color = colors.muted, maxLines = 4)
                    }
                    featured.displayDate?.let {
                        Text(it, style = CampusTypography.Caption, color = colors.brandBlue)
                    }
                }
            }
        }
        news.drop(1).take(3).forEach { item ->
            CampusCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(campusHeroGradient(colors)),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.title ?: "News", style = CampusTypography.Headline, color = colors.ink)
                        if (item.displayBody.isNotBlank()) {
                            Text(item.displayBody, style = CampusTypography.Caption, color = colors.muted, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FacultySection(faculty: List<FacultyMember>) {
    if (faculty.isEmpty()) return
    val colors = campusColors()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Faculty", subtitle = "The people behind the lectures")
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            faculty.forEach { member ->
                CampusCard(modifier = Modifier.widthIn(min = 148.dp, max = 168.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    ) {
                        AvatarView(member.displayName, size = 56)
                        Text(member.displayName, style = CampusTypography.Headline, color = colors.ink, maxLines = 1)
                        Text(
                            member.displayTitle,
                            style = CampusTypography.Caption,
                            color = colors.brandBlue,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}
