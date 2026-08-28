package com.impostor.party.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impostor.party.R
import com.impostor.party.ui.theme.ImpostorTheme
import com.impostor.party.util.LocalFeedback

/** Standard page frame: safe insets, optional back arrow, big display title. */
@Composable
fun ScreenScaffold(
    title: String?,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        if (onBack != null || title != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 20.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    val feedback = LocalFeedback.current
                    IconButton(
                        onClick = {
                            feedback.tap()
                            onBack()
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                } else {
                    Spacer(Modifier.width(20.dp))
                }
            }
        }
        if (title != null) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ImpostorTheme.extended.muted,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        content()
    }
}

/** The one loud button on a screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color? = null,
    contentColor: Color? = null,
) {
    val feedback = LocalFeedback.current
    Button(
        onClick = {
            feedback.tap()
            onClick()
        },
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container ?: MaterialTheme.colorScheme.primary,
            contentColor = contentColor ?: MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = ImpostorTheme.extended.elevated,
            disabledContentColor = ImpostorTheme.extended.muted,
        ),
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Quieter sibling of [PrimaryButton]. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val feedback = LocalFeedback.current
    OutlinedButton(
        onClick = {
            feedback.tap()
            onClick()
        },
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ImpostorTheme.extended.hairline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onBackground,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val feedback = LocalFeedback.current
    TextButton(
        onClick = {
            feedback.tap()
            onClick()
        },
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = color ?: ImpostorTheme.extended.muted,
        )
    }
}

/** A bordered block that groups one setting or one decision. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, ImpostorTheme.extended.hairline, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        if (title != null) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = ImpostorTheme.extended.muted,
            )
            Spacer(Modifier.height(12.dp))
        }
        content()
        if (subtitle != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ImpostorTheme.extended.muted,
            )
        }
    }
}

/** Big, unmistakable number picker. Touch targets are 56dp. */
@Composable
fun Stepper(
    value: Int,
    onChange: (Int) -> Unit,
    min: Int,
    max: Int,
    modifier: Modifier = Modifier,
) {
    val feedback = LocalFeedback.current
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StepperButton(
            glyph = "−",
            enabled = value > min,
            contentDescription = stringResource(R.string.decrease),
            onClick = {
                feedback.tap()
                onChange((value - 1).coerceAtLeast(min))
            },
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 96.dp),
        )
        StepperButton(
            glyph = "+",
            enabled = value < max,
            contentDescription = stringResource(R.string.increase),
            onClick = {
                feedback.tap()
                onChange((value + 1).coerceAtMost(max))
            },
        )
    }
}

@Composable
private fun StepperButton(
    glyph: String,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.32f, label = "stepperAlpha")
    Box(
        Modifier
            .size(56.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(ImpostorTheme.extended.elevated)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Pill row for two to four mutually exclusive choices. */
@Composable
fun SegmentedSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedback = LocalFeedback.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ImpostorTheme.extended.elevated)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val background by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                animationSpec = tween(180),
                label = "segmentBg",
            )
            val textColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    ImpostorTheme.extended.muted
                },
                animationSpec = tween(180),
                label = "segmentFg",
            )
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(background)
                    .clickable(role = Role.RadioButton) {
                        feedback.tap()
                        onSelect(index)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}

/** Title + optional explanation with any trailing control. */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val feedback = LocalFeedback.current
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null) {
                        feedback.tap()
                        onClick()
                    }
                } else {
                    Modifier
                }
            )
            .heightIn(min = 56.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ImpostorTheme.extended.muted,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(16.dp))
            trailing()
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val feedback = LocalFeedback.current
    SettingRow(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        onClick = {
            onCheckedChange(!checked)
        },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = {
                    feedback.tap()
                    onCheckedChange(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedTrackColor = ImpostorTheme.extended.elevated,
                    uncheckedBorderColor = ImpostorTheme.extended.hairline,
                ),
            )
        },
    )
}

/** Thin progress rail used by the timers and the pass-the-phone counter. */
@Composable
fun ProgressRail(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(clamped, animationSpec = tween(400), label = "rail")
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(ImpostorTheme.extended.elevated)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(6.dp)
                .clip(CircleShape)
                .background(color ?: MaterialTheme.colorScheme.onBackground)
        )
    }
}

/** Small uppercase label used above sections and beside counters. */
@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = color ?: ImpostorTheme.extended.muted,
        modifier = modifier,
    )
}

fun formatClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val minutes = safe / 60
    val seconds = safe % 60
    return "%d:%02d".format(minutes, seconds)
}
