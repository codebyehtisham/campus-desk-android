package com.derived.campusdesk.sharedui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.derived.campusdesk.sharedui.theme.CampusRadius
import com.derived.campusdesk.sharedui.theme.CampusSpacing
import com.derived.campusdesk.sharedui.theme.CampusTypography
import com.derived.campusdesk.sharedui.theme.campusColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun campusHeroGradient(colors: com.derived.campusdesk.sharedui.theme.CampusColorTokens) =
    Brush.linearGradient(listOf(colors.primaryDark, colors.brandRed, colors.brandBlue))

fun campusPillShape() = RoundedCornerShape(percent = 50)

enum class BrandButtonKind { Primary, Secondary, Ghost, Destructive }

@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Int = 56) {
    val colors = campusColors()
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(CampusRadius.Sm.dp))
            .background(Brush.linearGradient(listOf(colors.primaryDark, colors.brandRed, colors.brandBlue))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.School, contentDescription = null, tint = colors.onBrand, modifier = Modifier.size((size * 0.5f).dp))
    }
}

@Composable
fun BrandButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    kind: BrandButtonKind = BrandButtonKind.Primary,
) {
    val colors = campusColors()
    val bg = when (kind) {
        BrandButtonKind.Primary -> Brush.horizontalGradient(listOf(colors.primaryDark, colors.brandRed))
        BrandButtonKind.Secondary -> Brush.horizontalGradient(listOf(colors.brandBlue.copy(alpha = 0.15f), colors.brandRed.copy(alpha = 0.1f)))
        BrandButtonKind.Ghost -> Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
        BrandButtonKind.Destructive -> Brush.horizontalGradient(listOf(Color(0xFF8B1E1E), Color(0xFFB83232)))
    }
    val textColor = when (kind) {
        BrandButtonKind.Secondary, BrandButtonKind.Ghost -> colors.brandRed
        else -> colors.onBrand
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(campusPillShape())
            .background(bg)
            .then(if (kind == BrandButtonKind.Secondary) Modifier.border(1.dp, colors.stroke, campusPillShape()) else Modifier)
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = textColor, strokeWidth = 2.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text, color = textColor, style = MaterialTheme.typography.labelLarge)
                if (kind == BrandButtonKind.Primary) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrandTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val colors = campusColors()
    val passwordVisible = remember { androidx.compose.runtime.mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusEvent { focusState ->
                if (focusState.isFocused) {
                    scope.launch {
                        // Wait for IME animation, then scroll field above the keyboard.
                        delay(100)
                        bringIntoViewRequester.bringIntoView()
                        delay(250)
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
        visualTransformation = if (isPassword && !passwordVisible.value) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                    Icon(
                        if (passwordVisible.value) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                    )
                }
            }
        } else null,
        shape = RoundedCornerShape(CampusRadius.Sm.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.primaryPale,
            unfocusedContainerColor = colors.primaryPale.copy(alpha = 0.6f),
            focusedBorderColor = colors.brandBlue,
            unfocusedBorderColor = colors.stroke,
        ),
    )
}

@Composable
fun CampusCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = campusColors()
    val shape = RoundedCornerShape(CampusRadius.Md.dp)
    Box(
        modifier = modifier
            .shadow(10.dp, shape, ambientColor = colors.ink.copy(alpha = 0.07f), spotColor = colors.ink.copy(alpha = 0.07f))
            .clip(shape)
            .background(colors.surface.copy(alpha = 0.96f))
            .border(1.dp, colors.stroke.copy(alpha = 0.7f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(CampusSpacing.Md.dp),
    ) { content() }
}

@Composable
fun AvatarView(name: String, modifier: Modifier = Modifier, size: Int = 44) {
    val colors = campusColors()
    val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")
    Box(
        modifier = modifier
            .size(size.dp)
            .shadow(8.dp, CircleShape, ambientColor = colors.brandBlue.copy(alpha = 0.25f), spotColor = colors.brandBlue.copy(alpha = 0.25f))
            .clip(CircleShape)
            .background(campusHeroGradient(colors))
            .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials.ifBlank { "?" },
            color = colors.onBrand,
            style = CampusTypography.Headline.copy(fontSize = (size * 0.32f).sp),
        )
    }
}

@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = campusColors().brandRed,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = CampusTypography.Eyebrow,
        color = color,
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    val colors = campusColors()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = CampusTypography.Title, color = colors.ink)
        if (subtitle != null) {
            Text(subtitle, style = CampusTypography.Callout, color = colors.muted)
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = campusColors().brandBlue,
    onClick: (() -> Unit)? = null,
) {
    val colors = campusColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.muted, style = CampusTypography.Caption)
            Text(
                value,
                color = colors.ink,
                style = CampusTypography.Headline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onClick != null) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = colors.muted, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = campusColors().brandBlue,
    subtitle: String? = null,
) {
    val colors = campusColors()
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = CampusSpacing.Sm.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.ink, style = CampusTypography.Headline)
            if (subtitle != null) {
                Text(subtitle, color = colors.muted, style = CampusTypography.Caption)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun MetaChip(text: String, modifier: Modifier = Modifier, tint: Color = campusColors().brandRed) {
    Text(
        text = text,
        modifier = modifier
            .clip(campusPillShape())
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = tint,
        style = CampusTypography.Caption,
    )
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(color = campusColors().stroke, thickness = 1.dp)
}

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    val colors = campusColors()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CampusRadius.Sm.dp))
            .background(colors.brandRed.copy(alpha = 0.12f))
            .border(1.dp, colors.brandRed.copy(alpha = 0.3f), RoundedCornerShape(CampusRadius.Sm.dp))
            .padding(CampusSpacing.Md.dp),
    ) {
        Text(message, color = colors.brandRed, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EmptyStateView(title: String, message: String, modifier: Modifier = Modifier) {
    val colors = campusColors()
    Column(
        modifier = modifier.fillMaxWidth().padding(CampusSpacing.Xl.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CampusSpacing.Sm.dp),
    ) {
        Text(title, style = CampusTypography.Title, color = colors.ink, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun CampusLoadingView(message: String = "Loading your session", modifier: Modifier = Modifier) {
    val colors = campusColors()
    val transition = rememberInfiniteTransition(label = "loading")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "rotation",
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CampusSpacing.Lg.dp),
    ) {
        BrandMark(size = 72)
        Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(96.dp).graphicsLayer { rotationZ = rotation },
                color = colors.brandBlue.copy(alpha = 0.35f),
                strokeWidth = 2.dp,
            )
        }
        Text(message, color = colors.muted, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun AsyncStateView(
    isLoading: Boolean,
    errorMessage: String?,
    isEmpty: Boolean,
    emptyTitle: String,
    emptyMessage: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when {
        isLoading && isEmpty -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CampusLoadingView(message = "Loading…", modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
        }
        errorMessage != null && isEmpty -> Column(
            modifier = modifier.fillMaxWidth().padding(CampusSpacing.Lg.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.Md.dp),
        ) {
            ErrorBanner(errorMessage)
            onRetry?.let { BrandButton(text = "Try again", onClick = it, modifier = Modifier.width(180.dp)) }
        }
        isEmpty -> EmptyStateView(emptyTitle, emptyMessage, modifier)
        else -> content()
    }
}

@Composable
fun ViewfinderOverlay(modifier: Modifier = Modifier, color: Color = Color.White) {
    CanvasOverlay(modifier, color)
}

@Composable
private fun CanvasOverlay(modifier: Modifier, color: Color) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val corner = 28.dp.toPx()
        val stroke = 3.5.dp.toPx()
        val inset = 28.dp.toPx()
        val w = size.width
        val h = size.height
        // Top-left
        drawLine(color, androidx.compose.ui.geometry.Offset(inset, inset), androidx.compose.ui.geometry.Offset(inset + corner, inset), stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(inset, inset), androidx.compose.ui.geometry.Offset(inset, inset + corner), stroke)
        // Top-right
        drawLine(color, androidx.compose.ui.geometry.Offset(w - inset, inset), androidx.compose.ui.geometry.Offset(w - inset - corner, inset), stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(w - inset, inset), androidx.compose.ui.geometry.Offset(w - inset, inset + corner), stroke)
        // Bottom-left
        drawLine(color, androidx.compose.ui.geometry.Offset(inset, h - inset), androidx.compose.ui.geometry.Offset(inset + corner, h - inset), stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(inset, h - inset), androidx.compose.ui.geometry.Offset(inset, h - inset - corner), stroke)
        // Bottom-right
        drawLine(color, androidx.compose.ui.geometry.Offset(w - inset, h - inset), androidx.compose.ui.geometry.Offset(w - inset - corner, h - inset), stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(w - inset, h - inset), androidx.compose.ui.geometry.Offset(w - inset, h - inset - corner), stroke)
    }
}
