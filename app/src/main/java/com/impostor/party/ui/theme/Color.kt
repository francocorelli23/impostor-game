package com.impostor.party.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------- dark palette

val InkBlack = Color(0xFF0A0A0D)
val InkSurface = Color(0xFF131318)
val InkSurfaceHigh = Color(0xFF1B1B22)
val InkOutline = Color(0xFF2B2B35)
val InkOnSurface = Color(0xFFF3F1EC)
val InkMuted = Color(0xFF9A98A4)
val InkPrimary = Color(0xFFEEEBE4)
val InkOnPrimary = Color(0xFF0A0A0D)

// --------------------------------------------------------------- light palette

val PaperBackground = Color(0xFFF7F5F1)
val PaperSurface = Color(0xFFFFFFFF)
val PaperSurfaceHigh = Color(0xFFEFEDE7)
val PaperOutline = Color(0xFFDCD8D0)
val PaperOnSurface = Color(0xFF14141A)
val PaperMuted = Color(0xFF6B6975)
val PaperPrimary = Color(0xFF14141A)
val PaperOnPrimary = Color(0xFFFAF9F6)

// ------------------------------------------------------------------- accents

val ImpostorRedDark = Color(0xFFE5484D)
val ImpostorRedLight = Color(0xFFCE2A31)
val CrewGreenDark = Color(0xFF63D9A0)
val CrewGreenLight = Color(0xFF10794F)
val AmberDark = Color(0xFFE8B84B)
val AmberLight = Color(0xFF9A6C05)

/**
 * Colours Material 3 has no slot for. Kept in a CompositionLocal so screens can read
 * them the same way they read [androidx.compose.material3.MaterialTheme].
 */
@Immutable
data class ExtendedColors(
    val impostor: Color,
    val impostorContainer: Color,
    val crew: Color,
    val crewContainer: Color,
    val accent: Color,
    val muted: Color,
    val elevated: Color,
    val hairline: Color,
    val isDark: Boolean,
)

val DarkExtendedColors = ExtendedColors(
    impostor = ImpostorRedDark,
    impostorContainer = Color(0xFF2A1214),
    crew = CrewGreenDark,
    crewContainer = Color(0xFF0E241B),
    accent = AmberDark,
    muted = InkMuted,
    elevated = InkSurfaceHigh,
    hairline = InkOutline,
    isDark = true,
)

val LightExtendedColors = ExtendedColors(
    impostor = ImpostorRedLight,
    impostorContainer = Color(0xFFFCE9E9),
    crew = CrewGreenLight,
    crewContainer = Color(0xFFE3F5EC),
    accent = AmberLight,
    muted = PaperMuted,
    elevated = PaperSurfaceHigh,
    hairline = PaperOutline,
    isDark = false,
)

val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }
