@file:OptIn(ExperimentalMaterial3Api::class)

package com.derived.campusdesk.attendance.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.derived.campusdesk.attendance.AttendanceViewModel
import com.derived.campusdesk.attendance.ScanPhase
import com.derived.campusdesk.networking.models.AttendanceSession
import com.derived.campusdesk.networking.models.LocationFix
import com.derived.campusdesk.networking.models.StudentAttendanceHistory
import com.derived.campusdesk.sharedui.components.BrandButton
import com.derived.campusdesk.sharedui.components.BrandButtonKind
import com.derived.campusdesk.sharedui.components.CampusCard
import com.derived.campusdesk.sharedui.components.CampusLoadingView
import com.derived.campusdesk.sharedui.components.EmptyStateView
import com.derived.campusdesk.sharedui.components.ErrorBanner
import com.derived.campusdesk.sharedui.components.Eyebrow
import com.derived.campusdesk.sharedui.components.ScreenBackground
import com.derived.campusdesk.sharedui.components.SectionHeader
import com.derived.campusdesk.sharedui.components.SegmentedControl
import com.derived.campusdesk.sharedui.components.StatusPill
import com.derived.campusdesk.sharedui.components.ViewfinderOverlay
import com.derived.campusdesk.sharedui.theme.CampusLayout
import com.derived.campusdesk.sharedui.theme.CampusTypography
import com.derived.campusdesk.sharedui.theme.campusColors
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel,
    attendanceLocationEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = campusColors()
    val phase by viewModel.phase.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val session by viewModel.session.collectAsState()
    val lastFix by viewModel.lastFix.collectAsState()
    val history by viewModel.history.collectAsState()
    val historyLoading by viewModel.historyLoading.collectAsState()
    val historyError by viewModel.historyError.collectAsState()
    val isSuccess = phase == ScanPhase.Success
    var selectedSegment by remember { mutableIntStateOf(0) }
    val isScanTab = selectedSegment == 0

    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var locationGranted by remember {
        mutableStateOf(hasLocationPermission(context))
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
    }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        locationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            hasLocationPermission(context)
        viewModel.onLocationPermissionResult(locationGranted)
    }

    LaunchedEffect(Unit) {
        if (!cameraGranted) cameraLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(attendanceLocationEnabled) {
        if (attendanceLocationEnabled && !locationGranted) {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    val needsLocationPermission by viewModel.needsLocationPermission.collectAsState()
    LaunchedEffect(needsLocationPermission) {
        if (needsLocationPermission) {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    LaunchedEffect(selectedSegment) {
        if (selectedSegment == 1) viewModel.loadHistory()
    }

    val title = when {
        !isScanTab -> "Your record"
        isSuccess -> "Checked in"
        else -> "Check in"
    }

    Box(modifier.fillMaxSize()) {
        ScreenBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp)
                .padding(bottom = CampusLayout.TabScrollContentInset.dp)
                .widthIn(max = CampusLayout.ContentMaxWidth.dp + 80.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow("Attendance")
                Text(title, style = CampusTypography.Display, color = colors.ink)
                if (isScanTab && !isSuccess) {
                    Text(
                        "Scan the QR code displayed in class to mark your attendance.",
                        style = CampusTypography.Callout,
                        color = colors.muted,
                    )
                }
            }

            SegmentedControl(
                options = listOf("Scan", "History"),
                selectedIndex = selectedSegment,
                onSelect = { selectedSegment = it },
            )

            when (selectedSegment) {
                0 -> ScanSection(
                    isSuccess = isSuccess,
                    cameraGranted = cameraGranted,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    successMessage = successMessage,
                    session = session,
                    lastFix = lastFix,
                    locationEnabled = attendanceLocationEnabled,
                    onCode = viewModel::handleScan,
                    onScanAnother = viewModel::reset,
                )
                else -> HistorySection(
                    history = history,
                    isLoading = historyLoading,
                    errorMessage = historyError,
                    onRetry = viewModel::loadHistory,
                )
            }
        }
    }
}

@Composable
private fun ScanSection(
    isSuccess: Boolean,
    cameraGranted: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    successMessage: String?,
    session: AttendanceSession?,
    lastFix: LocationFix?,
    locationEnabled: Boolean,
    onCode: (String) -> Unit,
    onScanAnother: () -> Unit,
) {
    AnimatedContent(
        targetState = isSuccess,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.96f))
        },
        label = "attendancePhase",
    ) { success ->
        if (success) {
            SuccessCard(
                message = successMessage ?: "You are marked present.",
                session = session,
                lastFix = lastFix,
                onScanAnother = onScanAnother,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                ScannerStage(
                    cameraGranted = cameraGranted,
                    isLoading = isLoading,
                    locationEnabled = locationEnabled,
                    onCode = onCode,
                )
                errorMessage?.let { ErrorBanner(it) }
            }
        }
    }
}

@Composable
private fun HistorySection(
    history: StudentAttendanceHistory?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
) {
    when {
        isLoading && history == null -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CampusLoadingView()
        }
        errorMessage != null && history == null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ErrorBanner(errorMessage)
            BrandButton(text = "Try again", onClick = onRetry, kind = BrandButtonKind.Secondary)
        }
        history == null || (history.dailyRecords.isEmpty() && history.sessions.isNullOrEmpty()) -> {
            EmptyStateView(
                title = "No attendance yet",
                message = "Your check-ins and daily register will appear here.",
                icon = Icons.Default.CalendarMonth,
            )
        }
        else -> Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (history.dailyRecords.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("Daily register", subtitle = "Your attendance log")
                    history.dailyRecords.forEach { day ->
                        CampusCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        day.className?.takeIf { it.isNotBlank() } ?: "Campus",
                                        style = CampusTypography.Headline,
                                        color = campusColors().ink,
                                    )
                                    Text(
                                        day.dateLabel ?: day.date ?: "—",
                                        style = CampusTypography.Caption,
                                        color = campusColors().muted,
                                    )
                                }
                                StatusPill(day.status ?: "pending")
                            }
                        }
                    }
                }
            }
            history.sessions.orEmpty().takeIf { it.isNotEmpty() }?.let { sessions ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("Session attendance", subtitle = "QR check-ins")
                    sessions.forEach { item ->
                        CampusCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        item.className ?: "Session",
                                        style = CampusTypography.Headline,
                                        color = campusColors().ink,
                                    )
                                    Text(
                                        item.dateLabel ?: item.date ?: "—",
                                        style = CampusTypography.Caption,
                                        color = campusColors().muted,
                                    )
                                }
                                StatusPill(item.status ?: "pending")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerStage(
    cameraGranted: Boolean,
    isLoading: Boolean,
    locationEnabled: Boolean,
    onCode: (String) -> Unit,
) {
    val colors = campusColors()
    val shape = RoundedCornerShape(36.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .shadow(
                24.dp,
                shape,
                ambientColor = colors.brandRed.copy(alpha = 0.22f),
                spotColor = colors.brandRed.copy(alpha = 0.22f),
            )
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center,
    ) {
        if (cameraGranted) {
            QrScanner(onCode = onCode, enabled = !isLoading, modifier = Modifier.fillMaxSize())
            ViewfinderOverlay(Modifier.fillMaxSize(), color = Color.White)
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(horizontal = 28.dp),
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = colors.onBrand.copy(alpha = 0.9f),
                    modifier = Modifier.size(42.dp),
                )
                Text(
                    "Allow camera access in Settings to scan the classroom QR code.",
                    style = CampusTypography.Callout,
                    color = colors.onBrand.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(color = colors.onBrand, strokeWidth = 2.dp)
                    Text(
                        if (locationEnabled) "Checking campus location…" else "Marking attendance…",
                        style = CampusTypography.Caption,
                        color = colors.onBrand,
                    )
                }
            }
        }
    }
}

@Composable
private fun SuccessCard(
    message: String,
    session: AttendanceSession?,
    lastFix: LocationFix?,
    onScanAnother: () -> Unit,
) {
    val colors = campusColors()
    CampusCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(colors.success.copy(alpha = 0.16f)),
                )
                Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    tint = colors.success,
                    modifier = Modifier.size(42.dp),
                )
            }
            Text("You're in", style = CampusTypography.Display, color = colors.ink, textAlign = TextAlign.Center)
            Text(message, style = CampusTypography.Callout, color = colors.muted, textAlign = TextAlign.Center)
            session?.let {
                Text(it.displayTitle, style = CampusTypography.Headline, color = colors.brandBlue, textAlign = TextAlign.Center)
                if (it.displayMeta.isNotBlank()) {
                    Text(it.displayMeta, style = CampusTypography.Caption, color = colors.muted, textAlign = TextAlign.Center)
                }
            }
            lastFix?.let { fix ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = if (fix.onCampus == false) colors.brandRed else colors.success,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        fix.campusSummary,
                        style = CampusTypography.Caption,
                        color = if (fix.onCampus == false) colors.brandRed else colors.success,
                    )
                }
                Text(
                    fix.coordinateLabel,
                    style = CampusTypography.Caption.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    ),
                    color = colors.muted,
                )
            }
            Spacer(Modifier.height(4.dp))
            BrandButton(
                text = "Scan another",
                onClick = onScanAnother,
                kind = BrandButtonKind.Ghost,
            )
        }
    }
}

@Composable
private fun QrScanner(
    onCode: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val processed = remember { mutableSetOf<String>() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(
            com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(executor) { imageProxy ->
                        if (!enabled) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    barcodes.firstOrNull()?.rawValue?.let { raw ->
                                        if (processed.add(raw)) onCode(raw)
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
    )
}

private fun hasLocationPermission(context: android.content.Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}
