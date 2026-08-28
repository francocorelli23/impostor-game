package com.impostor.party.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.impostor.party.data.model.ThemeMode

private val DarkColors = darkColorScheme(
    primary = InkPrimary,
    onPrimary = InkOnPrimary,
    primaryContainer = InkSurfaceHigh,
    onPrimaryContainer = InkOnSurface,
    secondary = CrewGreenDark,
    onSecondary = InkOnPrimary,
    tertiary = AmberDark,
    onTertiary = InkOnPrimary,
    background = InkBlack,
    onBackground = InkOnSurface,
    surface = InkSurface,
    onSurface = InkOnSurface,
    surfaceVariant = InkSurfaceHigh,
    onSurfaceVariant = InkMuted,
    outline = InkOutline,
    outlineVariant = InkOutline,
    error = ImpostorRedDark,
    onError = InkOnPrimary,
    errorContainer = DarkExtendedColors.impostorContainer,
    onErrorContainer = ImpostorRedDark,
    scrim = InkBlack,
)

private val LightColors = lightColorScheme(
    primary = PaperPrimary,
    onPrimary = PaperOnPrimary,
    primaryContainer = PaperSurfaceHigh,
    onPrimaryContainer = PaperOnSurface,
    secondary = CrewGreenLight,
    onSecondary = PaperOnPrimary,
    tertiary = AmberLight,
    onTertiary = PaperOnPrimary,
    background = PaperBackground,
    onBackground = PaperOnSurface,
    surface = PaperSurface,
    onSurface = PaperOnSurface,
    surfaceVariant = PaperSurfaceHigh,
    onSurfaceVariant = PaperMuted,
    outline = PaperOutline,
    outlineVariant = PaperOutline,
    error = ImpostorRedLight,
    onError = PaperOnPrimary,
    errorContainer = LightExtendedColors.impostorContainer,
    onErrorContainer = ImpostorRedLight,
    scrim = PaperOnSurface,
)

val ImpostorShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun ImpostorTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (dark) DarkColors else LightColors
    val extended = if (dark) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var context: Context = view.context
            while (context !is Activity && context is ContextWrapper) {
                context = context.baseContext
            }
            val activity = context as? Activity
            if (activity != null) {
                val window = activity.window
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }

    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ImpostorTypography,
            shapes = ImpostorShapes,
            content = content,
        )
    }
}

/** Shorthand for the palette Material 3 has no slot for. */
object ImpostorTheme {
    val extended: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
