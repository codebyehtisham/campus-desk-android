package com.derived.campusdesk.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.derived.campusdesk.auth.ForgotPasswordViewModel
import com.derived.campusdesk.auth.LoginViewModel
import com.derived.campusdesk.networking.api.ApiEnvironmentPreset
import com.derived.campusdesk.networking.api.ApiEnvironmentStore
import com.derived.campusdesk.networking.api.EnvironmentHealthProbe
import com.derived.campusdesk.networking.debug.DevToolsConfig
import com.derived.campusdesk.sharedui.components.BrandButton
import com.derived.campusdesk.sharedui.components.BrandMark
import com.derived.campusdesk.sharedui.components.BrandTextField
import com.derived.campusdesk.sharedui.components.CampusLoadingView
import com.derived.campusdesk.sharedui.components.DerivedLoaderStyle
import com.derived.campusdesk.sharedui.components.ErrorBanner
import com.derived.campusdesk.sharedui.components.Eyebrow
import com.derived.campusdesk.sharedui.components.campusPillShape
import com.derived.campusdesk.sharedui.motion.CampusLiveBackdrop
import com.derived.campusdesk.sharedui.motion.FloatingParticles
import com.derived.campusdesk.sharedui.motion.ShakeEffect
import com.derived.campusdesk.sharedui.theme.CampusLayout
import com.derived.campusdesk.sharedui.theme.CampusRadius
import com.derived.campusdesk.sharedui.theme.CampusSpacing
import com.derived.campusdesk.sharedui.theme.CampusTypography
import com.derived.campusdesk.sharedui.theme.campusColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel,
    forgotPasswordViewModel: ForgotPasswordViewModel,
    devToolsConfig: DevToolsConfig,
    environmentStore: ApiEnvironmentStore,
    onApiEnvironmentChanged: () -> Unit,
) {
    var showForgot by remember { mutableStateOf(false) }
    var showEnvironmentPicker by remember { mutableStateOf(false) }
    val email by loginViewModel.email.collectAsState()
    val password by loginViewModel.password.collectAsState()
    val isLoading by loginViewModel.isLoading.collectAsState()
    val errorMessage by loginViewModel.errorMessage.collectAsState()
    var errorShake by remember { mutableIntStateOf(0) }
    if (errorMessage != null) errorShake++

    var taglineStep by remember { mutableIntStateOf(1) } // Courses highlighted like screenshot
    LaunchedEffect(Unit) {
        while (true) {
            delay(2200)
            taglineStep = (taglineStep + 1) % 3
        }
    }

    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val colors = campusColors()
    val taglines = listOf("Attendance", "Courses", "News")

    Box(Modifier.fillMaxSize()) {
        CampusLiveBackdrop(intense = true)
        FloatingParticles()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    })
                },
            verticalArrangement = if (imeVisible) Arrangement.Top else Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = if (imeVisible) 12.dp else 18.dp)
                    .then(
                        if (devToolsConfig.isDevToolsEnabled) {
                            Modifier.clickable { showEnvironmentPicker = true }
                        } else {
                            Modifier
                        },
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BrandMark(size = if (imeVisible) 40 else 48)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Campus Desk", style = CampusTypography.Title.copy(fontSize = 22.sp), color = colors.ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        taglines.forEachIndexed { index, item ->
                            Text(
                                item,
                                style = CampusTypography.Caption,
                                color = if (taglineStep == index) colors.brandRed else colors.muted,
                            )
                            if (index < taglines.lastIndex) {
                                Text(" · ", style = CampusTypography.Caption, color = colors.stroke)
                            }
                        }
                    }
                }
            }

            LoginSheet(
                email = email,
                password = password,
                isLoading = isLoading,
                errorMessage = errorMessage,
                errorShake = errorShake,
                onEmailChange = loginViewModel::onEmailChange,
                onPasswordChange = loginViewModel::onPasswordChange,
                onSubmit = loginViewModel::submit,
                onForgot = { showForgot = true },
                canSubmit = loginViewModel.canSubmit,
            )
        }

        if (isLoading) {
            CampusLoadingView(style = DerivedLoaderStyle.Overlay)
        }
    }

    if (showForgot) {
        ForgotPasswordScreen(
            viewModel = forgotPasswordViewModel,
            onBack = { showForgot = false },
        )
    }

    if (showEnvironmentPicker && devToolsConfig.isDevToolsEnabled) {
        EnvironmentPickerSheet(
            environmentStore = environmentStore,
            onDismiss = { showEnvironmentPicker = false },
            onSelected = {
                onApiEnvironmentChanged()
                showEnvironmentPicker = false
            },
        )
    }
}

@Composable
private fun LoginSheet(
    email: String,
    password: String,
    isLoading: Boolean,
    errorMessage: String?,
    errorShake: Int,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onForgot: () -> Unit,
    canSubmit: Boolean,
) {
    val colors = campusColors()
    val sheetShape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(30.dp, sheetShape, ambientColor = colors.ink.copy(alpha = 0.12f), spotColor = colors.ink.copy(alpha = 0.12f))
            .clip(sheetShape)
            .background(colors.surface)
            .border(1.dp, colors.stroke.copy(alpha = 0.85f), sheetShape)
            .padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 12.dp, bottom = 8.dp)
                .size(width = 42.dp, height = 5.dp)
                .clip(campusPillShape())
                .background(colors.stroke.copy(alpha = 0.85f)),
        )
        Column(
            modifier = Modifier
                .widthIn(max = CampusLayout.FormMaxWidth.dp)
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow("Student portal")
                Text("Welcome back", style = CampusTypography.Title, color = colors.ink)
                Text(
                    "Sign in with your campus email to mark attendance and open your courses.",
                    color = colors.muted,
                    style = CampusTypography.Callout,
                )
            }
            BrandTextField(
                value = email,
                onValueChange = onEmailChange,
                label = "Campus email",
                placeholder = "Campus email",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                leadingIcon = Icons.Default.Email,
            )
            BrandTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Password",
                placeholder = "Password",
                isPassword = true,
                leadingIcon = Icons.Default.Lock,
            )
            Text(
                "Forgot password?",
                color = colors.brandBlue,
                style = CampusTypography.Callout,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(onClick = onForgot),
            )
            ShakeEffect(errorShake) {
                if (errorMessage != null) ErrorBanner(errorMessage)
            }
            BrandButton(
                text = "Continue",
                onClick = onSubmit,
                enabled = canSubmit && !isLoading,
                loading = false,
            )
            Text(
                "Accounts are issued by your college. Password resets go to your campus inbox.",
                color = colors.muted,
                style = CampusTypography.Caption,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun ForgotPasswordScreen(viewModel: ForgotPasswordViewModel, onBack: () -> Unit) {
    val email by viewModel.email.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val didSend by viewModel.didSend.collectAsState()
    val colors = campusColors()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(Modifier.fillMaxSize()) {
        CampusLiveBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(CampusSpacing.Lg.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    })
                },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (didSend) {
                Text("Check your inbox", style = CampusTypography.Title, color = colors.ink)
                Text(
                    "If an account exists for that email, we've sent reset instructions.",
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text("Reset password", style = CampusTypography.Title, color = colors.ink)
                Spacer(Modifier.height(12.dp))
                BrandTextField(
                    email,
                    viewModel::onEmailChange,
                    "Campus email",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                    leadingIcon = Icons.Default.Email,
                )
                Spacer(Modifier.height(12.dp))
                BrandButton(text = "Send reset link", onClick = viewModel::submit, loading = isLoading)
            }
            Spacer(Modifier.height(CampusSpacing.Md.dp))
            BrandButton(text = "Back to sign in", onClick = onBack, kind = com.derived.campusdesk.sharedui.components.BrandButtonKind.Ghost)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentPickerSheet(
    environmentStore: ApiEnvironmentStore,
    onDismiss: () -> Unit,
    onSelected: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var selected by remember { mutableStateOf(environmentStore.selected) }
    var healthMap by remember { mutableStateOf<Map<ApiEnvironmentPreset, String>>(emptyMap()) }
    val scope = rememberCoroutineScopeLocal()

    LaunchedEffect(Unit) {
        ApiEnvironmentPreset.entries.forEach { preset ->
            healthMap = healthMap + (preset to "Checking…")
            val online = EnvironmentHealthProbe.isOnline(preset)
            healthMap = healthMap + (preset to if (online) "Online" else "Unreachable")
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(CampusSpacing.Lg.dp), verticalArrangement = Arrangement.spacedBy(CampusSpacing.Md.dp)) {
            Text("API Environment", style = CampusTypography.Title)
            Text(
                "Login uses the selected host. Prefer Development if Production cannot resolve on this device.",
                color = campusColors().muted,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            )
            ApiEnvironmentPreset.entries.forEach { preset ->
                val status = healthMap[preset] ?: "Checking…"
                val statusColor = when (status) {
                    "Online" -> campusColors().success
                    "Unreachable" -> campusColors().brandRed
                    else -> campusColors().brandBlue
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CampusRadius.Sm.dp))
                        .background(if (selected == preset) campusColors().primaryPale else campusColors().surface)
                        .border(1.dp, campusColors().stroke, RoundedCornerShape(CampusRadius.Sm.dp))
                        .clickable {
                            selected = preset
                            environmentStore.selected = preset
                            scope.launch {
                                healthMap = healthMap + (preset to "Checking…")
                                val online = EnvironmentHealthProbe.isOnline(preset)
                                healthMap = healthMap + (preset to if (online) "Online" else "Unreachable")
                            }
                        }
                        .padding(CampusSpacing.Md.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(preset.label, color = campusColors().ink, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                        Text(preset.hostLabel, color = campusColors().muted, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                        Text(status, color = statusColor, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Text("Shake device to inspect logs.", color = campusColors().muted, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            BrandButton(
                text = "Done",
                onClick = {
                    onSelected()
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun rememberCoroutineScopeLocal(): CoroutineScope = androidx.compose.runtime.rememberCoroutineScope()
