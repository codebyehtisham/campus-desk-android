@file:OptIn(ExperimentalMaterial3Api::class)

package com.derived.campusdesk.courses.ui

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.derived.campusdesk.courses.ClassDetailViewModel
import com.derived.campusdesk.courses.ClassListViewModel
import com.derived.campusdesk.networking.models.ClassContent
import com.derived.campusdesk.networking.models.StudentAssignment
import com.derived.campusdesk.networking.models.StudentClass
import com.derived.campusdesk.networking.models.StudentLeave
import com.derived.campusdesk.networking.models.StudentQuiz
import com.derived.campusdesk.networking.models.TimetableSlot
import com.derived.campusdesk.sharedui.components.AsyncStateView
import com.derived.campusdesk.sharedui.components.BrandButton
import com.derived.campusdesk.sharedui.components.CampusCard
import com.derived.campusdesk.sharedui.components.EmptyStateView
import com.derived.campusdesk.sharedui.components.Eyebrow
import com.derived.campusdesk.sharedui.components.MetaChip
import com.derived.campusdesk.sharedui.components.ScreenBackground
import com.derived.campusdesk.sharedui.components.StatusPill
import com.derived.campusdesk.sharedui.components.campusHeroGradient
import com.derived.campusdesk.sharedui.components.campusPillShape
import com.derived.campusdesk.sharedui.theme.CampusLayout
import com.derived.campusdesk.sharedui.theme.CampusTypography
import com.derived.campusdesk.sharedui.theme.campusColors

private enum class ClassDetailTab(val label: String) {
    Content("Content"),
    Timetable("Timetable"),
    Assignments("Assignments"),
    Quizzes("Quizzes"),
    Leave("Leave"),
}

@Composable
fun ClassListScreen(
    viewModel: ClassListViewModel,
    onClassSelected: (StudentClass) -> Unit,
    modifier: Modifier = Modifier,
) {
    val classes by viewModel.classes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isNotEnrolled by viewModel.isNotEnrolled.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier.fillMaxSize()) {
        ScreenBackground()
        when {
            isNotEnrolled -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateView(
                    title = "No classes assigned",
                    message = "You are not enrolled in any classes yet. Contact your institute administrator.",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                )
            }
            else -> PullToRefreshBox(
                isRefreshing = isLoading && classes.isNotEmpty(),
                onRefresh = { viewModel.load() },
                modifier = Modifier.fillMaxSize(),
            ) {
                AsyncStateView(
                    isLoading = isLoading && classes.isEmpty(),
                    errorMessage = errorMessage,
                    isEmpty = classes.isEmpty(),
                    emptyTitle = "No classes",
                    emptyMessage = "Your enrolled classes will appear here.",
                    emptyIcon = Icons.AutoMirrored.Filled.MenuBook,
                    onRetry = { viewModel.load() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                            .statusBarsPadding()
                            .padding(top = 20.dp)
                            .padding(bottom = CampusLayout.TabScrollContentInset.dp)
                            .widthIn(max = CampusLayout.ContentMaxWidth.dp)
                            .align(Alignment.TopCenter),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Eyebrow("Enrolled")
                            Text("Classes", style = CampusTypography.Display, color = campusColors().ink)
                            Text(
                                "${classes.size} class${if (classes.size == 1) "" else "es"} this term",
                                style = CampusTypography.Callout,
                                color = campusColors().muted,
                            )
                        }
                        classes.forEach { item ->
                            CampusCard(onClick = { onClassSelected(item) }) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            item.displayName,
                                            style = CampusTypography.Title,
                                            color = campusColors().ink,
                                            modifier = Modifier.weight(1f),
                                        )
                                        item.code?.takeIf { it.isNotBlank() }?.let {
                                            MetaChip(it, tint = campusColors().brandBlue)
                                        }
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Faculty", style = CampusTypography.Caption, color = campusColors().muted)
                                        Text(item.teacherName, style = CampusTypography.Callout, color = campusColors().muted)
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

@Composable
fun ClassDetailScreen(
    viewModel: ClassDetailViewModel,
    classId: String,
    seed: StudentClass? = null,
    onBack: () -> Unit,
    onAssignment: (String) -> Unit,
    onApplyLeave: (String) -> Unit,
    onLeaveDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val studentClass by viewModel.studentClass.collectAsState()
    val timetable by viewModel.timetable.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var tab by remember { mutableStateOf(ClassDetailTab.Content) }

    LaunchedEffect(classId) { viewModel.load(classId, seed) }

    val colors = campusColors()
    Box(modifier.fillMaxSize()) {
        ScreenBackground()
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.ink)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        studentClass?.displayName ?: "Class",
                        style = CampusTypography.Title,
                        color = colors.ink,
                    )
                    studentClass?.code?.let {
                        Text(it, style = CampusTypography.Caption, color = colors.muted)
                    }
                }
            }

            when {
                isLoading && studentClass == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.derived.campusdesk.sharedui.components.CampusLoadingView()
                }
                errorMessage != null && studentClass == null -> EmptyStateView("Couldn't open class", errorMessage!!)
                studentClass != null -> {
                    val detail = studentClass!!
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ClassDetailTab.entries.forEach { item ->
                            val selected = tab == item
                            Text(
                                item.label,
                                modifier = Modifier
                                    .clip(campusPillShape())
                                    .then(
                                        if (selected) Modifier.background(campusHeroGradient(colors))
                                        else Modifier
                                            .background(colors.surface.copy(alpha = 0.8f))
                                            .border(1.dp, colors.stroke, campusPillShape()),
                                    )
                                    .clickable { tab = item }
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                color = if (selected) colors.onBrand else colors.ink,
                                style = CampusTypography.Caption,
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        when (tab) {
                            ClassDetailTab.Content -> ContentTab(detail.contents.orEmpty())
                            ClassDetailTab.Timetable -> TimetableTab(timetable)
                            ClassDetailTab.Assignments -> AssignmentsTab(detail.assignments.orEmpty(), onAssignment)
                            ClassDetailTab.Quizzes -> QuizzesTab(detail.quizzes.orEmpty())
                            ClassDetailTab.Leave -> LeaveTab(
                                leaves = detail.leaves.orEmpty(),
                                onApplyLeave = { onApplyLeave(detail.id.value) },
                                onLeaveDetail = onLeaveDetail,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentTab(contents: List<ClassContent>) {
    if (contents.isEmpty()) {
        EmptyStateView("No content", "Weekly content for this class will appear here.")
        return
    }
    contents.sortedBy { it.week ?: Int.MAX_VALUE }.forEach { item ->
        CampusCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.week?.let { Eyebrow("Week $it") }
                Text(item.title ?: "Content", style = CampusTypography.Headline, color = campusColors().ink)
                item.body?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = CampusTypography.Body, color = campusColors().muted)
                }
            }
        }
    }
}

@Composable
private fun TimetableTab(slots: List<TimetableSlot>) {
    if (slots.isEmpty()) {
        EmptyStateView("No timetable", "Schedule slots for this class will appear here.")
        return
    }
    slots.forEach { slot ->
        CampusCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(slot.day ?: "Day", style = CampusTypography.Headline, color = campusColors().ink)
                if (slot.timeRange.isNotBlank()) {
                    Text(slot.timeRange, style = CampusTypography.Callout, color = campusColors().brandBlue)
                }
                slot.room?.takeIf { it.isNotBlank() }?.let {
                    Text("Room $it", style = CampusTypography.Caption, color = campusColors().muted)
                }
            }
        }
    }
}

@Composable
private fun AssignmentsTab(assignments: List<StudentAssignment>, onAssignment: (String) -> Unit) {
    if (assignments.isEmpty()) {
        EmptyStateView("No assignments", "Class assignments will appear here.")
        return
    }
    assignments.forEach { item ->
        CampusCard(onClick = { onAssignment(item.id.value) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.displayTitle, style = CampusTypography.Headline, color = campusColors().ink)
                    item.dueDate?.let {
                        Text("Due ${it.take(10)}", style = CampusTypography.Caption, color = campusColors().muted)
                    }
                }
                StatusPill(item.status ?: "missing")
            }
        }
    }
}

@Composable
private fun QuizzesTab(quizzes: List<StudentQuiz>) {
    if (quizzes.isEmpty()) {
        EmptyStateView("No quizzes", "Quiz marks for this class will appear here.")
        return
    }
    quizzes.forEach { quiz ->
        CampusCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(quiz.displayTitle, style = CampusTypography.Headline, color = campusColors().ink, modifier = Modifier.weight(1f))
                Text(quiz.displayMarks, style = CampusTypography.Headline, color = campusColors().brandBlue)
            }
        }
    }
}

@Composable
private fun LeaveTab(
    leaves: List<StudentLeave>,
    onApplyLeave: () -> Unit,
    onLeaveDetail: (String) -> Unit,
) {
    BrandButton(text = "Apply leave", onClick = onApplyLeave)
    Spacer(Modifier.height(4.dp))
    if (leaves.isEmpty()) {
        EmptyStateView("No leave requests", "Leave requests for this class will appear here.")
        return
    }
    leaves.forEach { leave ->
        CampusCard(onClick = { onLeaveDetail(leave.id.value) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(leave.leaveType?.label ?: leave.type ?: "Leave", style = CampusTypography.Headline, color = campusColors().ink)
                    Text(leave.dateRangeLabel, style = CampusTypography.Caption, color = campusColors().muted)
                }
                StatusPill(leave.status ?: "pending")
            }
        }
    }
}
