@file:OptIn(ExperimentalMaterial3Api::class)

package com.derived.campusdesk.courses.assignments

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.derived.campusdesk.networking.models.AssignmentSubmissionFile
import com.derived.campusdesk.networking.models.StudentAssignment
import com.derived.campusdesk.sharedui.components.BrandButton
import com.derived.campusdesk.sharedui.components.BrandButtonKind
import com.derived.campusdesk.sharedui.components.CampusCard
import com.derived.campusdesk.sharedui.components.CampusLoadingView
import com.derived.campusdesk.sharedui.components.EmptyStateView
import com.derived.campusdesk.sharedui.components.ErrorBanner
import com.derived.campusdesk.sharedui.components.Eyebrow
import com.derived.campusdesk.sharedui.components.ScreenBackground
import com.derived.campusdesk.sharedui.components.StatusPill
import com.derived.campusdesk.sharedui.components.campusPillShape
import com.derived.campusdesk.sharedui.theme.CampusLayout
import com.derived.campusdesk.sharedui.theme.CampusTypography
import com.derived.campusdesk.sharedui.theme.campusColors

@Composable
fun AssignmentListScreen(
    viewModel: AssignmentListViewModel,
    onAssignmentSelected: (StudentAssignment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val assignments by viewModel.assignments.collectAsState()
    val selectedClassId by viewModel.selectedClassId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isNotEnrolled by viewModel.isNotEnrolled.collectAsState()
    val colors = campusColors()
    val filters = viewModel.classFilters(assignments)
    val visible = viewModel.visibleAssignments(assignments, selectedClassId)

    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier.fillMaxSize()) {
        ScreenBackground()
        when {
            isNotEnrolled -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateView(
                    title = "Not enrolled",
                    message = "Assignments appear once you are enrolled in classes.",
                    icon = Icons.Default.Inbox,
                )
            }
            isLoading && assignments.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CampusLoadingView()
            }
            errorMessage != null && assignments.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(20.dp),
                ) {
                    ErrorBanner(errorMessage!!)
                    BrandButton(text = "Try again", onClick = { viewModel.load() })
                }
            }
            else -> PullToRefreshBox(
                isRefreshing = isLoading && assignments.isNotEmpty(),
                onRefresh = { viewModel.load() },
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
                        Eyebrow("Homework")
                        Text("Assignments", style = CampusTypography.Display, color = colors.ink)
                        Text(
                            "${visible.size} item${if (visible.size == 1) "" else "s"}",
                            style = CampusTypography.Callout,
                            color = colors.muted,
                        )
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        filters.forEach { filter ->
                            val selected = selectedClassId == filter.id
                            Text(
                                filter.name,
                                style = CampusTypography.Caption,
                                color = if (selected) colors.brandBlue else colors.muted,
                                modifier = Modifier
                                    .clip(campusPillShape())
                                    .background(if (selected) colors.primaryPale else colors.surface)
                                    .then(
                                        if (selected) Modifier
                                        else Modifier.border(1.dp, colors.stroke, campusPillShape()),
                                    )
                                    .clickable { viewModel.selectClass(filter.id) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }

                    if (visible.isEmpty()) {
                        EmptyStateView(
                            title = "No assignments",
                            message = "Homework from your teachers will appear here.",
                            icon = Icons.Default.Inbox,
                        )
                    } else {
                        visible.forEach { item ->
                            CampusCard(onClick = { onAssignmentSelected(item) }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(item.displayTitle, style = CampusTypography.Headline, color = colors.ink)
                                        item.className?.let {
                                            Text(it, style = CampusTypography.Caption, color = colors.muted)
                                        }
                                        item.dueDate?.let {
                                            Text(
                                                "Due ${it.take(10)}",
                                                style = CampusTypography.Caption,
                                                color = colors.muted,
                                            )
                                        }
                                    }
                                    StatusPill(item.status ?: "missing")
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
fun AssignmentDetailScreen(
    viewModel: AssignmentDetailViewModel,
    assignmentId: String,
    seed: StudentAssignment? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val assignment by viewModel.assignment.collectAsState()
    val body by viewModel.body.collectAsState()
    val fileName by viewModel.fileName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val submitSuccess by viewModel.submitSuccess.collectAsState()
    val context = LocalContext.current
    val colors = campusColors()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.attachFile(context, uri)
    }

    LaunchedEffect(assignmentId) { viewModel.load(assignmentId, seed) }

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
                Text("Assignment", style = CampusTypography.Title, color = colors.ink)
            }
            when {
                isLoading && assignment == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CampusLoadingView()
                }
                errorMessage != null && assignment == null -> EmptyStateView("Couldn't open", errorMessage!!)
                assignment != null -> {
                    val item = assignment!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CampusCard {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(item.displayTitle, style = CampusTypography.Title, color = colors.ink, modifier = Modifier.weight(1f))
                                    StatusPill(item.status ?: "missing")
                                }
                                item.className?.let {
                                    Text(it, style = CampusTypography.Callout, color = colors.muted)
                                }
                                item.dueDate?.let {
                                    Text("Due ${it.take(10)}", style = CampusTypography.Caption, color = colors.brandBlue)
                                }
                                item.marksObtained?.let {
                                    Text("Marks: $it", style = CampusTypography.Headline, color = colors.brandBlue)
                                }
                            }
                        }
                        CampusCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Brief", style = CampusTypography.Headline, color = colors.ink)
                                Text(
                                    item.body?.takeIf { it.isNotBlank() } ?: "No description provided.",
                                    style = CampusTypography.Body,
                                    color = colors.muted,
                                )
                            }
                        }
                        item.feedback?.takeIf { it.isNotBlank() }?.let { feedback ->
                            CampusCard {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Feedback", style = CampusTypography.Headline, color = colors.ink)
                                    Text(feedback, style = CampusTypography.Body, color = colors.muted)
                                }
                            }
                        }
                        val status = item.status?.lowercase().orEmpty()
                        if (status !in setOf("graded", "submitted")) {
                            CampusCard {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Submit work", style = CampusTypography.Headline, color = colors.ink)
                                    OutlinedTextField(
                                        value = body,
                                        onValueChange = viewModel::onBodyChange,
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Response") },
                                        minLines = 4,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        BrandButton(
                                            text = if (fileName != null) "Change file" else "Attach PDF/DOC",
                                            onClick = {
                                                picker.launch(AssignmentSubmissionFile.allowedMimeTypes.toTypedArray())
                                            },
                                            kind = BrandButtonKind.Secondary,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (fileName != null) {
                                            BrandButton(
                                                text = "Remove",
                                                onClick = viewModel::clearFile,
                                                kind = BrandButtonKind.Ghost,
                                                modifier = Modifier.weight(0.6f),
                                            )
                                        }
                                    }
                                    fileName?.let {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = colors.brandBlue)
                                            Text(it, style = CampusTypography.Caption, color = colors.muted)
                                        }
                                    }
                                    errorMessage?.let { ErrorBanner(it) }
                                    if (submitSuccess) {
                                        Text("Submitted successfully.", style = CampusTypography.Callout, color = colors.success)
                                    }
                                    BrandButton(
                                        text = "Submit assignment",
                                        onClick = { viewModel.submit(assignmentId) },
                                        loading = isSubmitting,
                                        enabled = body.isNotBlank() || fileName != null,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
