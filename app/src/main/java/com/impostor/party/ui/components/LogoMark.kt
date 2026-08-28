package com.impostor.party.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app mark: three empty circles and one filled. Three of these are the same and
 * one is not - which is the whole game in one shape.
 */
@Composable
fun LogoMark(
    ringColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    animateIn: Boolean = false,
    breathe: Boolean = false,
    contentDescription: String? = null,
) {
    val entrance = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(animateIn) {
        if (animateIn) entrance.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }

    val pulse by rememberInfiniteTransition(label = "mark").animateFloat(
        initialValue = 1f,
        targetValue = if (breathe) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "markPulse",
    )

    val label = contentDescription
    Canvas(
        modifier
            .size(size)
            .then(
                if (label != null) {
                    Modifier.semantics { this.contentDescription = label }
                } else {
                    Modifier
                }
            )
    ) {
        val s = this.size.minDimension
        val gap = s * 0.44f
        val radius = s * 0.155f
        val stroke = s * 0.052f
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f

        val centers = listOf(
            Offset(cx - gap / 2f, cy - gap / 2f),
            Offset(cx + gap / 2f, cy - gap / 2f),
            Offset(cx - gap / 2f, cy + gap / 2f),
        )

        centers.forEachIndexed { index, center ->
            val local = stagger(entrance.value, index)
            if (local <= 0f) return@forEachIndexed
            drawCircle(
                color = ringColor,
                radius = radius * local,
                center = center,
                alpha = local,
                style = Stroke(width = stroke),
            )
        }

        val accentProgress = stagger(entrance.value, 3)
        if (accentProgress > 0f) {
            drawCircle(
                color = accentColor,
                radius = radius * 1.06f * accentProgress * pulse,
                center = Offset(cx + gap / 2f, cy + gap / 2f),
                alpha = accentProgress,
            )
        }
    }
}

/** Turns a single 0..1 driver into four overlapping 0..1 ramps. */
private fun stagger(t: Float, index: Int): Float {
    val start = index * 0.13f
    val span = 0.5f
    return ((t - start) / span).coerceIn(0f, 1f)
}
