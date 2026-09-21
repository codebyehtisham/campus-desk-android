package com.derived.campusdesk.sharedui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.derived.campusdesk.sharedui.R
import com.derived.campusdesk.sharedui.motion.CampusLiveBackdrop
import com.derived.campusdesk.sharedui.theme.CampusRadius
import com.derived.campusdesk.sharedui.theme.CampusSpacing
import com.derived.campusdesk.sharedui.theme.CampusTypography
import com.derived.campusdesk.sharedui.theme.campusColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

fun campusHeroGradient(colors: com.derived.campusdesk.sharedui.theme.CampusColorTokens) =
    Brush.linearGradient(listOf(colors.primaryDark, colors.brandRed, colors.brandBlue))

fun campusPillShape() = RoundedCornerShape(percent = 50)

enum class BrandButtonKind { Primary, Secondary, Ghost, Destructive }

@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Int = 56) {
    val radius = (size * 0.22f).dp
    Image(
        painter = painterResource(R.drawable.campus_desk_app_icon),
        contentDescription = "Campus Desk",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size.dp)
            .shadow(8.dp, RoundedCornerShape(radius), ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(radius)),
    )
}

@Composable
fun BrandButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    kind: BrandButtonKind = BrandButtonKind.Primary,
    leadingIcon: ImageVector? = null,
    showTrailingArrow: Boolean = kind == BrandButtonKind.Primary && leadingIcon == null,
) {
    val colors = campusColors()
    val bg = when (kind) {
        BrandButtonKind.Primary -> Brush.horizontalGradient(listOf(colors.primaryDark, colors.brandRed, colors.brandBlue))
        BrandButtonKind.Secondary -> Brush.horizontalGradient(listOf(colors.brandBlue.copy(alpha = 0.15f), colors.brandRed.copy(alpha = 0.1f)))
        BrandButtonKind.Ghost -> Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
        BrandButtonKind.Destructive -> Brush.horizontalGradient(listOf(colors.primaryDark, colors.brandRed))
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
                when {
                    leadingIcon != null -> {
                        Icon(leadingIcon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    showTrailingArrow -> {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                }
                Text(text, color = textColor, style = MaterialTheme.typography.labelLarge)
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
    leadingIcon: ImageVector? = null,
    placeholder: String? = null,
) {
    val colors = campusColors()
    val passwordVisible = remember { androidx.compose.runtime.mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label.uppercase(),
            style = CampusTypography.Eyebrow,
            color = colors.ink.copy(alpha = 0.78f),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(placeholder ?: label, color = colors.muted)
            },
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusEvent { focusState ->
                    if (focusState.isFocused) {
                        scope.launch {
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
            leadingIcon = leadingIcon?.let { icon ->
                {
                    Icon(icon, contentDescription = null, tint = colors.brandBlue, modifier = Modifier.size(20.dp))
                }
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                        Icon(
                            if (passwordVisible.value) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = colors.brandBlue,
                        )
                    }
                }
            } else null,
            shape = RoundedCornerShape(CampusRadius.Sm.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedBorderColor = colors.brandBlue,
                unfocusedBorderColor = colors.stroke,
            ),
        )
    }
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
    wrapValue: Boolean = false,
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
                maxLines = if (wrapValue) Int.MAX_VALUE else 1,
                overflow = if (wrapValue) TextOverflow.Clip else TextOverflow.Ellipsis,
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
fun EmptyStateView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = campusColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = CampusSpacing.Xl.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CampusSpacing.Md.dp),
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        if (colors.primaryPale.alpha > 0.08f) colors.primaryPale
                        else colors.brandBlue.copy(alpha = 0.12f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = colors.brandBlue,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Text(title, style = CampusTypography.Title, color = colors.ink, textAlign = TextAlign.Center)
        Text(message, style = CampusTypography.Callout, color = colors.muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = campusColors()
    val pill = campusPillShape()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(pill)
            .background(colors.staffSurface)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (selected) {
                            Modifier
                                .shadow(
                                    3.dp,
                                    pill,
                                    ambientColor = colors.ink.copy(alpha = 0.1f),
                                    spotColor = colors.ink.copy(alpha = 0.1f),
                                    clip = false,
                                )
                                .background(colors.surface, pill)
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    style = CampusTypography.Caption,
                    color = if (selected) colors.ink else colors.muted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun QuickStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = campusColors()
    CampusCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
            }
            Text(
                value,
                style = CampusTypography.Title.copy(fontSize = 24.sp),
                color = colors.ink,
            )
            Text(title, style = CampusTypography.Caption, color = colors.muted)
        }
    }
}

@Composable
fun StatusPill(status: String, modifier: Modifier = Modifier) {
    val colors = campusColors()
    val normalized = status.trim().lowercase()
    val (display, tint) = when (normalized) {
        "approved", "accepted" -> "Approved" to colors.brandBlue
        "graded" -> "Graded" to colors.brandBlue
        "submitted" -> "Submitted" to colors.brandBlue.copy(alpha = 0.85f)
        "present" -> "Present" to colors.brandBlue
        "rejected", "declined" -> "Rejected" to colors.brandRed
        "absent" -> "Absent" to colors.brandRed
        "pending" -> "Pending" to colors.muted
        "missing" -> "Missing" to colors.muted
        else -> (status.ifBlank { "Missing" }.replaceFirstChar { it.uppercase() }) to colors.muted
    }
    Text(
        text = display.uppercase(),
        modifier = modifier
            .clip(campusPillShape())
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = tint,
        style = CampusTypography.Caption.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
        ),
    )
}

@Composable
fun ScreenBackground(modifier: Modifier = Modifier) {
    CampusLiveBackdrop(modifier = modifier)
}

enum class DerivedLoaderStyle { Splash, Inline, Overlay }

@Composable
fun CampusLoadingView(
    message: String = "DERIVING",
    modifier: Modifier = Modifier,
    style: DerivedLoaderStyle = DerivedLoaderStyle.Inline,
) {
    val colors = campusColors()
    val splash = style == DerivedLoaderStyle.Splash || style == DerivedLoaderStyle.Overlay
    val outer = if (splash) 118.dp else 72.dp
    val inner = if (splash) 78.dp else 48.dp
    val dot = if (splash) 10.dp else 7.dp
    val ringWidthPxTarget = if (splash) 1.4f else 1.1f
    val labelSize = if (splash) 22.sp else 16.sp
    val iconSize = if (splash) 20.dp else 14.dp
    val tracking = if (splash) 6.sp else 4.sp

    val transition = rememberInfiniteTransition(label = "deriving")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label = "spin",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "pulse",
    )
    val dotPulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "dot",
    )

    Box(
        modifier = modifier.then(
            when (style) {
                DerivedLoaderStyle.Splash -> Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                colors.primaryDark.copy(alpha = 0.92f),
                                colors.brandRed.copy(alpha = 0.78f),
                                colors.brandBlue.copy(alpha = 0.65f),
                            ),
                        ),
                    )
                DerivedLoaderStyle.Overlay -> Modifier
                    .fillMaxSize()
                    .background(colors.canvas.copy(alpha = 0.88f))
                DerivedLoaderStyle.Inline -> Modifier.fillMaxWidth()
            },
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (style == DerivedLoaderStyle.Splash) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize(),
                onDraw = {
                    val step = 28f * density
                    val line = colors.brandBlue.copy(alpha = 0.1f)
                    val canvasW = size.width
                    val canvasH = size.height
                    var x = 0f
                    while (x <= canvasW) {
                        drawLine(line, Offset(x, 0f), Offset(x, canvasH), strokeWidth = 1f)
                        x += step
                    }
                    var y = 0f
                    while (y <= canvasH) {
                        drawLine(line, Offset(0f, y), Offset(canvasW, y), strokeWidth = 1f)
                        y += step
                    }
                },
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (splash) 28.dp else 18.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(outer)) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .size(outer)
                        .graphicsLayer { scaleX = pulse; scaleY = pulse },
                    onDraw = {
                        drawCircle(
                            color = colors.brandBlue.copy(alpha = 0.28f),
                            style = Stroke(width = ringWidthPxTarget * density),
                        )
                    },
                )
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .size(inner)
                        .graphicsLayer { rotationZ = rotation },
                    onDraw = {
                        val dash = 5f * density
                        val gap = 7f * density
                        drawCircle(
                            color = colors.brandRed,
                            style = Stroke(
                                width = ringWidthPxTarget * density,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap)),
                                cap = StrokeCap.Round,
                            ),
                        )
                    },
                )
                Box(
                    modifier = Modifier
                        .size(dot)
                        .graphicsLayer { scaleX = dotPulse; scaleY = dotPulse }
                        .clip(CircleShape)
                        .background(colors.brandRed),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.size(iconSize),
                    onDraw = {
                        val strokeW = size.minDimension * 0.12f
                        val stroke = Stroke(width = strokeW, cap = StrokeCap.Round)
                        drawCircle(
                            color = colors.brandRed,
                            radius = size.minDimension / 2f - strokeW,
                            style = stroke,
                        )
                        drawLine(
                            color = colors.brandRed,
                            start = Offset(size.width * 0.22f, size.height * 0.78f),
                            end = Offset(size.width * 0.78f, size.height * 0.22f),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round,
                        )
                    },
                )
                Text(
                    message.ifBlank { "DERIVING" }.uppercase(),
                    color = colors.brandRed,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = labelSize,
                        letterSpacing = tracking,
                    ),
                )
            }
        }
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
    emptyIcon: ImageVector? = null,
    content: @Composable () -> Unit,
) {
    when {
        isLoading && isEmpty -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CampusLoadingView(message = "DERIVING", modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
        }
        errorMessage != null && isEmpty -> Column(
            modifier = modifier.fillMaxWidth().padding(CampusSpacing.Lg.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.Md.dp),
        ) {
            ErrorBanner(errorMessage)
            onRetry?.let { BrandButton(text = "Try again", onClick = it, modifier = Modifier.width(180.dp)) }
        }
        isEmpty -> EmptyStateView(emptyTitle, emptyMessage, modifier, icon = emptyIcon)
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
