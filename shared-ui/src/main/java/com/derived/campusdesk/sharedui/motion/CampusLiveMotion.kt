package com.derived.campusdesk.sharedui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.derived.campusdesk.sharedui.theme.campusColors
import kotlin.math.sin
import kotlin.random.Random

object CampusMotion {
    val SpringSpec = spring<Float>(dampingRatio = 0.84f, stiffness = Spring.StiffnessMediumLow)
    val SnappySpec = spring<Float>(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium)
    val SheetSpec = spring<Float>(dampingRatio = 0.86f, stiffness = Spring.StiffnessLow)
}

/**
 * Decorative rings + faint grid for hero cards (Home / Profile), matching iOS
 * `CampusLiveRings` + `CampusGrid`.
 */
@Composable
fun CampusHeroDecor(
    modifier: Modifier = Modifier,
    strokeAlpha: Float = 0.14f,
    gridAlpha: Float = 0.12f,
) {
    val transition = rememberInfiniteTransition(label = "heroDecor")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "heroPulse",
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(48000, easing = LinearEasing)),
        label = "heroSpin",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val anchor = Offset(w * 0.92f, h * 0.82f)
        for (i in 0 until 3) {
            val radius = (w * (0.28f + i * 0.16f)) * pulse
            drawCircle(
                color = Color.White.copy(alpha = strokeAlpha + i * 0.04f),
                radius = radius,
                center = anchor,
                style = Stroke(width = 1.2.dp.toPx()),
            )
        }
        drawCircle(
            color = Color.White.copy(alpha = 0.08f * pulse),
            radius = 60.dp.toPx() * pulse,
            center = Offset(w * 0.88f, h * 0.78f),
        )
        // Soft rotation cue via offset rings
        val spinRad = Math.toRadians(spin.toDouble()).toFloat()
        val orbit = Offset(
            anchor.x + kotlin.math.cos(spinRad) * 8.dp.toPx(),
            anchor.y + kotlin.math.sin(spinRad) * 8.dp.toPx(),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.22f),
            radius = w * 0.22f,
            center = orbit,
            style = Stroke(width = 1.dp.toPx()),
        )
        val step = 18.dp.toPx()
        var x = 0f
        while (x < w) {
            drawLine(Color.White.copy(alpha = gridAlpha), Offset(x, 0f), Offset(x, h), 1.dp.toPx())
            x += step
        }
        var y = 0f
        while (y < h) {
            drawLine(Color.White.copy(alpha = gridAlpha), Offset(0f, y), Offset(w, y), 1.dp.toPx())
            y += step
        }
    }
}

@Composable
fun CampusLiveBackdrop(
    modifier: Modifier = Modifier,
    intense: Boolean = false,
) {
    val colors = campusColors()
    val transition = rememberInfiniteTransition(label = "backdrop")
    val orb1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5400, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb1",
    )
    val orb2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6400, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb2",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(colors.canvas)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colors.brandBlue.copy(alpha = if (intense) 0.28f else 0.14f), Color.Transparent),
                ),
                radius = size.width * 0.85f,
                center = Offset(size.width * (0.1f + orb1 * 0.1f), size.height * 0.15f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colors.brandRed.copy(alpha = if (intense) 0.22f else 0.12f), Color.Transparent),
                ),
                radius = size.width * 0.9f,
                center = Offset(size.width * (0.85f - orb2 * 0.1f), size.height * (0.85f - orb1 * 0.05f)),
            )
            for (i in 0 until 4) {
                val radius = size.width * (0.36f + i * 0.14f) * pulse
                drawCircle(
                    color = colors.stroke.copy(alpha = 0.35f),
                    radius = radius,
                    center = Offset(size.width * 0.72f, size.height * 0.18f),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }
    }
}

@Composable
fun FloatingParticles(modifier: Modifier = Modifier, count: Int = 18) {
    val colors = campusColors()
    val particles = remember {
        List(count) {
            Particle(
                x = Random.nextFloat(),
                startY = Random.nextFloat(),
                speed = 0.08f + Random.nextFloat() * 0.12f,
                size = 2f + Random.nextFloat() * 3f,
                isRed = Random.nextBoolean(),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "particles")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "progress",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val y = ((p.startY + progress * p.speed) % 1f)
            drawCircle(
                color = if (p.isRed) colors.brandRed.copy(alpha = 0.45f) else colors.brandBlue.copy(alpha = 0.45f),
                radius = p.size,
                center = Offset(size.width * p.x, size.height * (1f - y)),
            )
        }
    }
}

private data class Particle(
    val x: Float,
    val startY: Float,
    val speed: Float,
    val size: Float,
    val isRed: Boolean,
)

@Composable
fun ShakeEffect(trigger: Int, content: @Composable () -> Unit) {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            repeat(4) { i ->
                offset.animateTo(if (i % 2 == 0) 8f else -8f, tween(50))
            }
            offset.animateTo(0f, tween(50))
        }
    }
    Box(modifier = Modifier.graphicsLayer { translationX = offset.value }) {
        content()
    }
}

fun shakeOffset(trigger: Int, frame: Int): Float {
    if (trigger <= 0) return 0f
    return sin(frame * 0.8f) * 8f
}
