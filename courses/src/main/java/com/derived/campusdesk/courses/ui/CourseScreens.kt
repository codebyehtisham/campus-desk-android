@file:OptIn(ExperimentalMaterial3Api::class)

package com.derived.campusdesk.courses.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.derived.campusdesk.courses.CourseListViewModel
import com.derived.campusdesk.networking.models.Course
import com.derived.campusdesk.sharedui.components.AsyncStateView
import com.derived.campusdesk.sharedui.components.CampusCard
import com.derived.campusdesk.sharedui.components.Eyebrow
import com.derived.campusdesk.sharedui.components.MetaChip
import com.derived.campusdesk.sharedui.components.campusHeroGradient
import com.derived.campusdesk.sharedui.components.campusPillShape
import com.derived.campusdesk.sharedui.motion.CampusLiveBackdrop
import com.derived.campusdesk.sharedui.theme.CampusLayout
import com.derived.campusdesk.sharedui.theme.CampusRadius
import com.derived.campusdesk.sharedui.theme.CampusSpacing
import com.derived.campusdesk.sharedui.theme.CampusTypography
import com.derived.campusdesk.sharedui.theme.campusColors

@Composable
fun CourseListScreen(
    viewModel: CourseListViewModel,
    onCourseSelected: (Course) -> Unit,
    modifier: Modifier = Modifier,
) {
    val courses by viewModel.visibleCourses.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier.fillMaxSize()) {
        CampusLiveBackdrop()
        PullToRefreshBox(
            isRefreshing = isLoading && courses.isNotEmpty(),
            onRefresh = { viewModel.load() },
            modifier = Modifier.fillMaxSize(),
        ) {
            AsyncStateView(
                isLoading = isLoading && courses.isEmpty(),
                errorMessage = errorMessage,
                isEmpty = courses.isEmpty(),
                emptyTitle = "Catalogue is empty",
                emptyMessage = "Programmes appear here once the campus publishes them.",
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
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Eyebrow("Syllabus")
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("Courses", style = CampusTypography.Display, color = campusColors().ink, modifier = Modifier.weight(1f))
                            Text(
                                "${courses.size} this term",
                                style = CampusTypography.Caption,
                                color = campusColors().muted,
                            )
                        }
                        Text(
                            "A numbered syllabus — tap a row to open the programme.",
                            style = CampusTypography.Callout,
                            color = campusColors().muted,
                        )
                    }

                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categories.forEach { category ->
                            val selected = selectedCategory == category
                            val colors = campusColors()
                            Text(
                                text = category,
                                modifier = Modifier
                                    .clip(campusPillShape())
                                    .then(
                                        if (selected) Modifier.background(campusHeroGradient(colors))
                                        else Modifier
                                            .background(colors.surface.copy(alpha = 0.8f))
                                            .border(1.dp, colors.stroke, campusPillShape()),
                                    )
                                    .clickable { viewModel.selectCategory(category) }
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                color = if (selected) colors.onBrand else colors.ink,
                                style = CampusTypography.Caption,
                            )
                        }
                    }

                    courses.forEachIndexed { index, course ->
                        SyllabusPlate(
                            course = course,
                            index = index,
                            isLast = index == courses.lastIndex,
                            onClick = { onCourseSelected(course) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyllabusPlate(course: Course, index: Int, isLast: Boolean, onClick: () -> Unit) {
    val colors = campusColors()
    val even = index % 2 == 0
    val tint = if (even) colors.brandRed else colors.brandBlue
    val shape = RoundedCornerShape(22.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = 148.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            modifier = Modifier.width(34.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tint),
                contentAlignment = Alignment.Center,
            ) {
                Text("%02d".format(index + 1), color = colors.onBrand, style = CampusTypography.Caption)
            }
            if (!isLast) {
                Spacer(
                    Modifier
                        .padding(top = 0.dp)
                        .width(2.dp)
                        .weight(1f)
                        .background(colors.stroke),
                )
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (even) 0.dp else 12.dp, end = if (even) 12.dp else 0.dp)
                .shadow(12.dp, shape, ambientColor = tint.copy(alpha = 0.12f), spotColor = tint.copy(alpha = 0.12f))
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.stroke, shape)
                .clickable(onClick = onClick),
        ) {
            Box(
                modifier = Modifier
                    .width(7.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                    .background(tint),
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        course.displayCategory.uppercase(),
                        style = CampusTypography.Eyebrow.copy(letterSpacing = 1.4.sp),
                        color = tint,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(glyphFor(course.displayCategory), contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                }
                Text(course.displayTitle, style = CampusTypography.CourseTitle, color = colors.ink)
                if (course.displaySummary.isNotBlank()) {
                    Text(
                        course.displaySummary,
                        style = CampusTypography.Callout,
                        color = colors.muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                course.duration?.takeIf { it.isNotBlank() }?.let { MetaChip(it, tint = tint) }
            }
        }
    }
}

@Composable
fun CourseDetailScreen(course: Course, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = campusColors()
    Box(modifier.fillMaxSize()) {
        CampusLiveBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = CampusLayout.TabScrollContentInset.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(campusHeroGradient(colors)),
            ) {
                IconButton(onClick = onBack, modifier = Modifier.padding(CampusSpacing.Md.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.onBrand)
                }
                Column(Modifier.align(Alignment.BottomStart).padding(CampusSpacing.Lg.dp)) {
                    Icon(glyphFor(course.displayCategory), contentDescription = null, tint = colors.onBrand, modifier = Modifier.size(36.dp))
                    Text(course.displayTitle, style = CampusTypography.Title, color = colors.onBrand)
                    Text(course.displayCategory, color = colors.onBrand.copy(alpha = 0.85f), style = CampusTypography.Callout)
                }
            }
            Column(
                modifier = Modifier
                    .offset(y = (-28).dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CampusCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Eyebrow(course.displayCategory)
                        Text(course.displayTitle, style = CampusTypography.Title, color = colors.ink)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            course.duration?.let { MetaChip(it, tint = colors.brandBlue) }
                            MetaChip("ID ${course.id.value.take(8)}", tint = colors.muted)
                        }
                    }
                }
                CampusCard {
                    Column(verticalArrangement = Arrangement.spacedBy(CampusSpacing.Sm.dp)) {
                        Text("About", style = CampusTypography.Headline, color = colors.ink)
                        Text(
                            course.displaySummary.ifBlank { "Programme details coming soon." },
                            color = colors.muted,
                            style = CampusTypography.Body,
                        )
                    }
                }
                CampusCard {
                    Column(verticalArrangement = Arrangement.spacedBy(CampusSpacing.Sm.dp)) {
                        Text("On this programme", style = CampusTypography.Headline, color = colors.ink)
                        course.duration?.let { DetailRow("Duration", it) }
                        course.school?.let { DetailRow("School", it) }
                        course.teachingMethod?.let { DetailRow("Teaching method", it) }
                        course.outcome?.let { DetailRow("Outcome", it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = CampusSpacing.Xs.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = campusColors().muted, style = CampusTypography.Callout)
        Text(value, color = campusColors().ink, style = CampusTypography.Headline)
    }
}

private fun glyphFor(category: String): ImageVector = when {
    category.contains("nurs", ignoreCase = true) -> Icons.Default.Favorite
    else -> Icons.Default.Science
}
