@file:OptIn(ExperimentalMaterial3Api::class)

package com.derived.campusdesk.courses.leaves

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.derived.campusdesk.networking.models.LeaveType
import com.derived.campusdesk.networking.models.StudentLeave
import com.derived.campusdesk.sharedui.components.AsyncStateView
import com.derived.campusdesk.sharedui.components.BrandButton
import com.derived.campusdesk.sharedui.components.CampusCard
import com.derived.campusdesk.sharedui.components.CampusLoadingView
import com.derived.campusdesk.sharedui.components.EmptyStateView
import com.derived.campusdesk.sharedui.components.ErrorBanner
import com.derived.campusdesk.sharedui.components.Eyebrow
import com.derived.campusdesk.sharedui.components.ScreenBackground
import com.derived.campusdesk.sharedui.components.StatusPill
import com.derived.campusdesk.sharedui.components.campusHeroGradient
import com.derived.campusdesk.sharedui.components.campusPillShape
import com.derived.campusdesk.sharedui.theme.CampusLayout
import com.derived.campusdesk.sharedui.theme.CampusTypography
import com.derived.campusdesk.sharedui.theme.campusColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun LeaveListScreen(
    viewModel: LeaveListViewModel,
    onBack: () -> Unit,
    onApply: () -> Unit,
    onLeaveDetail: (StudentLeave) -> Unit,
    modifier: Modifier = Modifier,
) {
    val leaves by viewModel.leaves.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isNotEnrolled by viewModel.isNotEnrolled.collectAsState()
    val colors = campusColors()

    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier.fillMaxSize()) {
        ScreenBackground()
        Column(Modifier.fillMaxSize()) {
            DetailTopBar(title = "Leave requests", onBack = onBack)
            when {
                isNotEnrolled -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateView(
                        title = "Not enrolled yet",
                        message = "You need to be enrolled in a class before you can request leave.",
                    )
                }
                else -> PullToRefreshBox(
                    isRefreshing = isLoading && leaves.isNotEmpty(),
                    onRefresh = { viewModel.load() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AsyncStateView(
                        isLoading = isLoading && leaves.isEmpty(),
                        errorMessage = errorMessage,
                        isEmpty = leaves.isEmpty(),
                        emptyTitle = "No leave requests",
                        emptyMessage = "Apply for leave when you need time away from class.",
                        onRetry = { viewModel.load() },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            BrandButton(text = "Apply leave", onClick = onApply)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SummaryChip("Pending", viewModel.pendingCount.toString(), colors.muted, Modifier.weight(1f))
                                SummaryChip("Approved", viewModel.approvedCount.toString(), colors.brandBlue, Modifier.weight(1f))
                                SummaryChip("Rejected", viewModel.rejectedCount.toString(), colors.brandRed, Modifier.weight(1f))
                            }
                            leaves.forEach { leave ->
                                CampusCard(onClick = { onLeaveDetail(leave) }) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(leave.displayClassName, style = CampusTypography.Headline, color = colors.ink)
                                            Text(
                                                leave.leaveType?.label ?: leave.type ?: "Leave",
                                                style = CampusTypography.Caption,
                                                color = colors.muted,
                                            )
                                            Text(leave.dateRangeLabel, style = CampusTypography.Caption, color = colors.brandBlue)
                                        }
                                        StatusPill(leave.status ?: "pending")
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
fun LeaveDetailScreen(
    leave: StudentLeave,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = campusColors()
    Box(modifier.fillMaxSize()) {
        ScreenBackground()
        Column(Modifier.fillMaxSize()) {
            DetailTopBar(title = "Leave detail", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CampusCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(leave.displayClassName, style = CampusTypography.Title, color = colors.ink, modifier = Modifier.weight(1f))
                            StatusPill(leave.status ?: "pending")
                        }
                        Text(leave.leaveType?.label ?: leave.type ?: "Leave", style = CampusTypography.Headline, color = colors.brandBlue)
                        Text(leave.dateRangeLabel, style = CampusTypography.Callout, color = colors.muted)
                        Text("Teacher: ${leave.displayTeacherName}", style = CampusTypography.Caption, color = colors.muted)
                    }
                }
                leave.reason?.takeIf { it.isNotBlank() }?.let {
                    CampusCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Reason", style = CampusTypography.Headline, color = colors.ink)
                            Text(it, style = CampusTypography.Body, color = colors.muted)
                        }
                    }
                }
                leave.reviewNotes?.takeIf { it.isNotBlank() }?.let {
                    CampusCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Review notes", style = CampusTypography.Headline, color = colors.ink)
                            Text(it, style = CampusTypography.Body, color = colors.muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApplyLeaveScreen(
    viewModel: ApplyLeaveViewModel,
    presetClassId: String?,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val classes by viewModel.classes.collectAsState()
    val selectedClassId by viewModel.selectedClassId.collectAsState()
    val leaveType by viewModel.leaveType.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val reason by viewModel.reason.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val success by viewModel.success.collectAsState()
    val colors = campusColors()
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    LaunchedEffect(presetClassId) { viewModel.bootstrap(presetClassId) }
    LaunchedEffect(success) { if (success) onSuccess() }

    Box(modifier.fillMaxSize()) {
        ScreenBackground()
        Column(Modifier.fillMaxSize()) {
            DetailTopBar(title = "Apply leave", onBack = onBack)
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CampusLoadingView() }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (classes.isEmpty()) {
                        EmptyStateView("No enrolled classes", "No enrolled classes available.")
                    } else {
                        Text("Class", style = CampusTypography.Headline, color = colors.ink)
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            classes.forEach { item ->
                                val selected = selectedClassId == item.id.value
                                Text(
                                    item.displayName,
                                    modifier = Modifier
                                        .clip(campusPillShape())
                                        .then(
                                            if (selected) Modifier.background(campusHeroGradient(colors))
                                            else Modifier
                                                .background(colors.surface.copy(alpha = 0.8f))
                                                .border(1.dp, colors.stroke, campusPillShape()),
                                        )
                                        .clickable { viewModel.selectClass(item.id.value) }
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                    color = if (selected) colors.onBrand else colors.ink,
                                    style = CampusTypography.Caption,
                                )
                            }
                        }
                        Text("Leave type", style = CampusTypography.Headline, color = colors.ink)
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            LeaveType.entries.forEach { type ->
                                val selected = leaveType == type
                                Text(
                                    type.label,
                                    modifier = Modifier
                                        .clip(campusPillShape())
                                        .then(
                                            if (selected) Modifier.background(campusHeroGradient(colors))
                                            else Modifier
                                                .background(colors.surface.copy(alpha = 0.8f))
                                                .border(1.dp, colors.stroke, campusPillShape()),
                                        )
                                        .clickable { viewModel.selectType(type) }
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                    color = if (selected) colors.onBrand else colors.ink,
                                    style = CampusTypography.Caption,
                                )
                            }
                        }
                        BrandButton(text = "Start: ${startDate}", onClick = { pickingStart = true }, kind = com.derived.campusdesk.sharedui.components.BrandButtonKind.Secondary)
                        BrandButton(text = "End: ${endDate}", onClick = { pickingEnd = true }, kind = com.derived.campusdesk.sharedui.components.BrandButtonKind.Secondary)
                        OutlinedTextField(
                            value = reason,
                            onValueChange = viewModel::setReason,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Reason (optional)") },
                            minLines = 3,
                        )
                        errorMessage?.let { ErrorBanner(it) }
                        BrandButton(text = "Submit request", onClick = viewModel::submit, loading = isSubmitting)
                    }
                }
            }
        }
    }

    if (pickingStart) {
        LeaveDatePicker(
            initial = startDate,
            onDismiss = { pickingStart = false },
            onConfirm = {
                viewModel.setStartDate(it)
                pickingStart = false
            },
        )
    }
    if (pickingEnd) {
        LeaveDatePicker(
            initial = endDate,
            onDismiss = { pickingEnd = false },
            onConfirm = {
                viewModel.setEndDate(it)
                pickingEnd = false
            },
        )
    }
}

@Composable
private fun LeaveDatePicker(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis ?: return@TextButton
                onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun SummaryChip(label: String, value: String, tint: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    CampusCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label.uppercase(), style = CampusTypography.Eyebrow, color = campusColors().muted)
            Text(value, style = CampusTypography.Title, color = tint)
        }
    }
}

@Composable
internal fun DetailTopBar(title: String, onBack: () -> Unit) {
    val colors = campusColors()
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
        Text(title, style = CampusTypography.Title, color = colors.ink)
    }
}
