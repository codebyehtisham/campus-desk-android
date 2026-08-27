package com.derived.campusdesk.profile.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.derived.campusdesk.auth.SessionStore
import com.derived.campusdesk.networking.models.Organization
import com.derived.campusdesk.networking.models.StudentApplication
import com.derived.campusdesk.networking.models.User
import com.derived.campusdesk.profile.ProfileViewModel
import com.derived.campusdesk.sharedui.components.AvatarView
import com.derived.campusdesk.sharedui.components.BrandButton
import com.derived.campusdesk.sharedui.components.BrandButtonKind
import com.derived.campusdesk.sharedui.components.CampusCard
import com.derived.campusdesk.sharedui.components.ErrorBanner
import com.derived.campusdesk.sharedui.components.Eyebrow
import com.derived.campusdesk.sharedui.components.SettingsDivider
import com.derived.campusdesk.sharedui.components.SettingsRow
import com.derived.campusdesk.sharedui.components.SettingsToggleRow
import com.derived.campusdesk.sharedui.components.campusHeroGradient
import com.derived.campusdesk.sharedui.components.campusPillShape
import com.derived.campusdesk.sharedui.motion.CampusHeroDecor
import com.derived.campusdesk.sharedui.motion.CampusLiveBackdrop
import com.derived.campusdesk.sharedui.theme.CampusLayout
import com.derived.campusdesk.sharedui.theme.CampusTypography
import com.derived.campusdesk.sharedui.theme.campusColors
import kotlinx.coroutines.delay

private const val PREFS_PUSH = "campusdesk.prefs"
private const val KEY_PUSH = "campusdesk.push.enabled"

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    sessionStore: SessionStore,
    user: User,
    organization: Organization?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val application by viewModel.application.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val prefs = remember { context.getSharedPreferences(PREFS_PUSH, Context.MODE_PRIVATE) }
    var pushEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_PUSH, false)) }
    var copiedLabel by remember { mutableStateOf<String?>(null) }
    val colors = campusColors()

    val notificationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        pushEnabled = granted
        prefs.edit().putBoolean(KEY_PUSH, granted).apply()
    }

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(copiedLabel) {
        if (copiedLabel != null) {
            delay(1400)
            copiedLabel = null
        }
    }

    Box(modifier.fillMaxSize()) {
        CampusLiveBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .padding(bottom = CampusLayout.TabScrollContentInset.dp)
                .widthIn(max = CampusLayout.ContentMaxWidth.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            IdentityCard(user)
            errorMessage?.let { ErrorBanner(it) }
            AdmissionsCard(application)
            CampusCard {
                Column {
                    Eyebrow("Account", color = colors.brandBlue)
                    Spacer(Modifier.height(8.dp))
                    SettingsRow(
                        icon = Icons.Default.Email,
                        tint = colors.brandBlue,
                        title = "Campus email",
                        value = user.email.orEmpty().ifBlank { "Not on file" },
                        onClick = user.email?.takeIf { it.isNotBlank() }?.let {
                            {
                                copyToClipboard(context, "email", it)
                                copiedLabel = "email"
                            }
                        },
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Tag,
                        tint = colors.brandRed,
                        title = "Student ID",
                        value = user.id.value,
                        onClick = {
                            copyToClipboard(context, "id", user.id.value)
                            copiedLabel = "ID"
                        },
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Person,
                        tint = colors.brandBlue,
                        title = "Role",
                        value = (user.role ?: "student").replaceFirstChar { it.uppercase() },
                    )
                }
            }
            CampusCard {
                Column {
                    Eyebrow("Campus")
                    Spacer(Modifier.height(8.dp))
                    SettingsRow(
                        icon = Icons.Default.AccountBalance,
                        tint = colors.brandRed,
                        title = "Organisation",
                        value = organization?.name ?: organization?.title ?: "Campus Desk",
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.School,
                        tint = colors.brandBlue,
                        title = "Portal",
                        value = "Student LMS",
                    )
                }
            }
            CampusCard {
                Column {
                    Eyebrow("Settings", color = colors.brandBlue)
                    SettingsToggleRow(
                        icon = Icons.Default.Notifications,
                        tint = colors.brandBlue,
                        title = "Push notifications",
                        subtitle = "Class reminders and campus alerts",
                        checked = pushEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= 33) {
                                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                pushEnabled = enabled
                                prefs.edit().putBoolean(KEY_PUSH, enabled).apply()
                            }
                        },
                    )
                }
            }
            CampusCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Eyebrow("Appearance", color = colors.brandBlue)
                    Text("Follows your device", style = CampusTypography.Title, color = colors.ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ThemePreview("Light", light = true, Modifier.weight(1f))
                        ThemePreview("Dark", light = false, Modifier.weight(1f))
                    }
                    Text(
                        "Teal chrome stays consistent in light and dark.",
                        style = CampusTypography.Caption,
                        color = colors.muted,
                    )
                }
            }
            BrandButton(text = "Sign out", onClick = sessionStore::logout, kind = BrandButtonKind.Destructive)
        }

        copiedLabel?.let { label ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .clip(campusPillShape())
                    .background(colors.surface.copy(alpha = 0.92f))
                    .border(1.dp, colors.stroke, campusPillShape())
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text("Copied $label", color = colors.ink, style = CampusTypography.Caption)
            }
        }
    }
}

@Composable
private fun IdentityCard(user: User) {
    val colors = campusColors()
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.stroke, shape)
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(campusHeroGradient(colors)),
        ) {
            CampusHeroDecor(
                modifier = Modifier.fillMaxSize(),
                strokeAlpha = 0.18f,
                gridAlpha = 0.14f,
            )
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 40.dp, y = 40.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Eyebrow("Your record", color = colors.onBrand.copy(alpha = 0.85f))
                Spacer(Modifier.weight(1f))
                Text(
                    (user.role ?: "student").uppercase(),
                    style = CampusTypography.Eyebrow.copy(letterSpacing = 1.3.sp),
                    color = colors.onBrand,
                    modifier = Modifier
                        .clip(campusPillShape())
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .offset(y = (-46).dp)
                .padding(bottom = 20.dp),
        ) {
            Box {
                Box(
                    Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(colors.canvas)
                        .align(Alignment.Center),
                )
                AvatarView(user.displayName, size = 92, modifier = Modifier.align(Alignment.Center))
            }
            Text(user.displayName, style = CampusTypography.Display, color = colors.ink, maxLines = 1)
            if (!user.email.isNullOrBlank()) {
                Text(user.email.orEmpty(), style = CampusTypography.Callout, color = colors.muted)
            }
        }
    }
}

@Composable
private fun AdmissionsCard(application: StudentApplication?) {
    val colors = campusColors()
    val raw = (application?.decision ?: application?.status ?: "").lowercase()
    val tint = when {
        raw.contains("accept") -> colors.success
        raw.contains("reject") -> colors.brandRed
        application != null -> colors.brandBlue
        else -> colors.muted
    }
    val icon: ImageVector = when {
        raw.contains("accept") -> Icons.Default.CheckCircle
        raw.contains("reject") -> Icons.Default.Inbox
        application != null -> Icons.Default.Description
        else -> Icons.Default.Inbox
    }
    val title = application?.displayStatus ?: "No file yet"
    val detail = when {
        application != null -> "Your admissions record is live with the college."
        else -> "Nothing has been submitted on this account."
    }

    CampusCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Eyebrow("Admissions")
            Text("Application", style = CampusTypography.Title, color = colors.ink)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tint.copy(alpha = 0.08f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = CampusTypography.Headline, color = colors.ink)
                    Text(detail, style = CampusTypography.Callout, color = colors.muted)
                }
            }
        }
    }
}

@Composable
private fun ThemePreview(title: String, light: Boolean, modifier: Modifier = Modifier) {
    val colors = campusColors()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (light) Color(0xFFF2F5FA) else Color(0xFF0A0F1C))
                .border(1.dp, colors.stroke, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(36.dp, 6.dp).clip(campusPillShape()).background(colors.brandRed))
                Box(Modifier.size(48.dp, 6.dp).clip(campusPillShape()).background(colors.brandBlue.copy(alpha = 0.8f)))
            }
        }
        Text(title, style = CampusTypography.Caption, color = colors.muted)
    }
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
}
