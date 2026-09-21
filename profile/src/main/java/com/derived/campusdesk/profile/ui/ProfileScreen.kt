package com.derived.campusdesk.profile.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VerifiedUser
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
import com.derived.campusdesk.sharedui.components.BrandMark
import com.derived.campusdesk.sharedui.components.CampusCard
import com.derived.campusdesk.sharedui.components.Eyebrow
import com.derived.campusdesk.sharedui.components.ScreenBackground
import com.derived.campusdesk.sharedui.components.SettingsDivider
import com.derived.campusdesk.sharedui.components.SettingsRow
import com.derived.campusdesk.sharedui.components.SettingsToggleRow
import com.derived.campusdesk.sharedui.components.campusHeroGradient
import com.derived.campusdesk.sharedui.components.campusPillShape
import com.derived.campusdesk.sharedui.motion.CampusHeroDecor
import com.derived.campusdesk.sharedui.theme.CampusLayout
import com.derived.campusdesk.sharedui.theme.CampusTypography
import com.derived.campusdesk.sharedui.theme.campusColors
import kotlinx.coroutines.delay

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    sessionStore: SessionStore,
    user: User,
    organization: Organization?,
    onOpenLeaves: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = campusColors()
    val application by viewModel.application.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val pushEnabled by viewModel.pushEnabled.collectAsState()
    var copiedLabel by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    val versionLabel = remember {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val version = info.versionName ?: "1.0"
            val code = if (android.os.Build.VERSION.SDK_INT >= 28) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            "Version $version (build $code)"
        } catch (_: PackageManager.NameNotFoundException) {
            "Version 1.0 (build 1)"
        }
    }

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(copiedLabel) {
        if (copiedLabel != null) {
            delay(1400)
            copiedLabel = null
        }
    }

    fun copy(value: String, label: String) {
        if (value.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        copiedLabel = label
    }

    Box(modifier.fillMaxSize()) {
        ScreenBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(top = 8.dp, start = 20.dp, end = 20.dp)
                .padding(bottom = CampusLayout.TabScrollContentInset.dp)
                .widthIn(max = CampusLayout.ContentMaxWidth.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            IdentityCard(user)

            CampusCard {
                Column {
                    Eyebrow("Class leave", color = colors.brandBlue)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenLeaves)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.brandBlue.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.EventNote,
                                contentDescription = null,
                                tint = colors.brandBlue,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("My leave requests", style = CampusTypography.Headline, color = colors.ink)
                            Text(
                                "Apply and track per-class leave",
                                style = CampusTypography.Caption,
                                color = colors.muted,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.muted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            AdmissionsCard(application = application, errorMessage = errorMessage)

            CampusCard {
                Column {
                    Eyebrow("Account", color = colors.brandBlue)
                    Spacer(Modifier.height(8.dp))
                    SettingsRow(
                        title = "Campus email",
                        value = user.email?.takeIf { it.isNotBlank() } ?: "Not on file",
                        icon = Icons.Default.Email,
                        tint = colors.brandBlue,
                        onClick = user.email?.takeIf { it.isNotBlank() }?.let { email ->
                            { copy(email, "email") }
                        },
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Student ID",
                        value = user.id.value.ifBlank { "—" },
                        icon = Icons.Default.Tag,
                        tint = colors.brandRed,
                        wrapValue = true,
                        onClick = { copy(user.id.value, "ID") },
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Role",
                        value = (user.role ?: "student").replaceFirstChar { it.uppercase() },
                        icon = Icons.Default.Person,
                        tint = colors.brandBlue,
                    )
                }
            }

            CampusCard {
                Column {
                    Eyebrow("Campus organisation")
                    Spacer(Modifier.height(8.dp))
                    SettingsRow(
                        title = "Organisation",
                        value = organization?.name ?: organization?.title ?: "Campus Desk",
                        icon = Icons.Default.AccountBalance,
                        tint = colors.brandRed,
                        wrapValue = true,
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Faculty",
                        value = user.title?.takeIf { it.isNotBlank() } ?: "Student LMS",
                        icon = Icons.Default.School,
                        tint = colors.brandBlue,
                        wrapValue = true,
                    )
                }
            }

            CampusCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Eyebrow("Settings", color = colors.brandBlue)
                    SettingsToggleRow(
                        title = "Push notifications",
                        subtitle = "Class reminders and campus alerts",
                        checked = pushEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && viewModel.needsNotificationPermission()) {
                                if (Build.VERSION.SDK_INT >= 33) {
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    )
                                } else {
                                    viewModel.setPushEnabled(true)
                                }
                            } else {
                                viewModel.setPushEnabled(enabled)
                            }
                        },
                        icon = Icons.Default.Notifications,
                        tint = colors.brandBlue,
                    )
                }
            }

            CampusCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Eyebrow("Appearance", color = colors.brandBlue)
                    Text("Follows your device", style = CampusTypography.Title, color = colors.ink)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ThemePreviewTile(title = "Light", light = true, modifier = Modifier.weight(1f))
                        ThemePreviewTile(title = "Dark", light = false, modifier = Modifier.weight(1f))
                    }
                    Text(
                        "Teal chrome stays consistent in light and dark.",
                        style = CampusTypography.Caption,
                        color = colors.muted,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("About Derived", color = colors.muted)
                CampusCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CampusBrandRow(iconSize = 40)
                        Text(
                            "Campus Desk is built by Derived Studio. We design and ship thoughtful, privacy-respecting apps for education — and Campus Desk keeps getting better under the Derived banner.",
                            style = CampusTypography.Callout,
                            color = colors.muted,
                        )
                        Row(
                            modifier = Modifier.clickable { openUrl(context, "https://derivedstudio.com") },
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                tint = colors.brandBlue,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                "Visit derivedstudio.com",
                                style = CampusTypography.Headline,
                                color = colors.brandBlue,
                            )
                        }
                    }
                }
                Text(
                    "Derived Studio is the studio behind Campus Desk.",
                    style = CampusTypography.Caption,
                    color = colors.muted,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("Legal & Support", color = colors.muted)
                CampusCard {
                    Column {
                        LegalRow(Icons.Default.VerifiedUser, "Age Assurance", showsChevron = true) {
                            openUrl(context, "https://derivedstudio.com/age-assurance")
                        }
                        SettingsDivider()
                        LegalRow(Icons.Default.Policy, "Privacy Policy", showsChevron = true) {
                            openUrl(context, "https://derivedstudio.com/privacy")
                        }
                        SettingsDivider()
                        LegalRow(Icons.Default.Description, "Terms", showsChevron = true) {
                            openUrl(context, "https://derivedstudio.com/terms")
                        }
                        SettingsDivider()
                        LegalRow(Icons.Default.Info, "Acknowledgements", showsChevron = true) {
                            openUrl(context, "https://derivedstudio.com/acknowledgements")
                        }
                        SettingsDivider()
                        LegalRow(Icons.Default.Email, "Contact support", showsChevron = false) {
                            openUrl(context, "mailto:support@derivedstudio.com")
                        }
                    }
                }
                Text(
                    versionLabel,
                    style = CampusTypography.Caption,
                    color = colors.muted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CampusBrandRow(iconSize = 52)
                }
            }

            BrandButton(
                text = "Sign out",
                onClick = sessionStore::logout,
                kind = BrandButtonKind.Primary,
                leadingIcon = Icons.AutoMirrored.Filled.Logout,
                showTrailingArrow = false,
            )
        }

        AnimatedVisibility(
            visible = copiedLabel != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp),
        ) {
            Text(
                text = "Copied ${copiedLabel.orEmpty()}",
                style = CampusTypography.Caption,
                color = colors.ink,
                modifier = Modifier
                    .clip(campusPillShape())
                    .background(colors.surface.copy(alpha = 0.92f))
                    .border(1.dp, colors.stroke, campusPillShape())
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
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
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(campusHeroGradient(colors)),
        ) {
            CampusHeroDecor(
                modifier = Modifier.fillMaxSize(),
                strokeAlpha = 0.18f,
                gridAlpha = 0.14f,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Eyebrow("Your record", color = colors.onBrand.copy(alpha = 0.85f))
                Spacer(Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        (user.role ?: "student").uppercase(),
                        style = CampusTypography.Eyebrow.copy(letterSpacing = 1.3.sp),
                        color = colors.onBrand,
                        modifier = Modifier
                            .clip(campusPillShape())
                            .background(Color.White.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = colors.onBrand,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-34).dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                AvatarView(user.displayName, size = 84)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.dp, colors.stroke, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = colors.ink,
                        modifier = Modifier.size(10.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    user.displayName.ifBlank { "Student" },
                    style = CampusTypography.Title,
                    color = colors.ink,
                )
                if (!user.email.isNullOrBlank()) {
                    Text(user.email.orEmpty(), style = CampusTypography.Callout, color = colors.muted)
                }
                if (!user.title.isNullOrBlank()) {
                    Text(user.title.orEmpty(), style = CampusTypography.Caption, color = colors.brandBlue)
                }
            }
        }
    }
}

@Composable
private fun AdmissionsCard(
    application: StudentApplication?,
    errorMessage: String?,
) {
    val colors = campusColors()
    val raw = (application?.decision ?: application?.status ?: "").lowercase()
    val tint = when {
        raw.contains("accept") -> colors.success
        raw.contains("reject") -> colors.brandRed
        application != null -> colors.brandBlue
        else -> colors.muted
    }
    val icon = when {
        raw.contains("accept") -> Icons.Default.CheckCircle
        raw.contains("reject") -> Icons.Default.Cancel
        application != null -> Icons.Default.Description
        else -> Icons.Default.Inbox
    }
    val headline = when {
        application != null && tint == colors.success -> "Application complete"
        application != null -> application.displayStatus
        else -> "No file yet"
    }
    val detail = when {
        application != null && tint == colors.success -> "You are admitted to the university."
        application != null -> "Your admissions record is live with the college."
        !errorMessage.isNullOrBlank() -> errorMessage
        else -> "Nothing has been submitted on this account."
    }

    CampusCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Eyebrow("Admissions")
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(headline, style = CampusTypography.Headline, color = colors.ink)
                    Text(detail, style = CampusTypography.Callout, color = colors.muted)
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ThemePreviewTile(
    title: String,
    light: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = campusColors()
    val fill = if (light) Color(0xFFF2F5FA) else Color(0xFF0A0F1C)
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
                .background(fill)
                .border(1.dp, colors.stroke, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(width = 36.dp, height = 6.dp)
                        .clip(campusPillShape())
                        .background(colors.brandRed),
                )
                Box(
                    Modifier
                        .size(width = 48.dp, height = 6.dp)
                        .clip(campusPillShape())
                        .background(colors.brandBlue.copy(alpha = 0.8f)),
                )
            }
        }
        Text(title, style = CampusTypography.Caption, color = colors.muted)
    }
}

@Composable
private fun CampusBrandRow(iconSize: Int) {
    val colors = campusColors()
    Row(
        horizontalArrangement = Arrangement.spacedBy((iconSize * 0.28f).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark(size = iconSize)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Campus Desk",
                style = CampusTypography.Headline.copy(fontSize = (iconSize * 0.42f).sp),
                color = colors.ink,
            )
            Text(
                "by Derived",
                style = CampusTypography.Caption.copy(fontSize = (iconSize * 0.28f).sp),
                color = colors.ink.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun LegalRow(
    icon: ImageVector,
    title: String,
    showsChevron: Boolean,
    onClick: () -> Unit,
) {
    val colors = campusColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.brandBlue.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.brandBlue, modifier = Modifier.size(16.dp))
        }
        Text(title, style = CampusTypography.Headline, color = colors.ink, modifier = Modifier.weight(1f))
        if (showsChevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
