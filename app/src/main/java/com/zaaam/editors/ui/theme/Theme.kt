package com.zaaam.editors.ui.theme

import androidx.compose.material3.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.textStyles
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val lightScheme = lightColorScheme(
    primary = RetroTokens.Olive,
    onPrimary = RetroTokens.Ink,
    primaryContainer = RetroTokens.OliveWash,
    onPrimaryContainer = RetroTokens.Ink,
    secondary = RetroTokens.OliveDim,
    onSecondary = RetroTokens.Ink,
    tertiary = RetroTokens.LedOrange,
    onTertiary = RetroTokens.Ink,
    error = RetroTokens.Brick,
    onError = RetroTokens.Ink,
    errorContainer = RetroTokens.BrickWash,
    onErrorContainer = RetroTokens.Ink,
    background = RetroTokens.Shell,
    onBackground = RetroTokens.Graphite,
    surface = RetroTokens.Card,
    onSurface = RetroTokens.Graphite,
    surfaceVariant = RetroTokens.Border,
    onSurfaceVariant = RetroTokens.Dim,
    outline = RetroTokens.Border,
    outlineVariant = RetroTokens.Border,
    surfaceContainerLowest = RetroTokens.Shell,
    surfaceContainerLow = RetroTokens.Card,
    surfaceContainer = RetroTokens.Card,
    surfaceContainerHigh = RetroTokens.Border,
    surfaceContainerHighest = RetroTokens.Border,
)

@Stable
interface LcdPalette {
    val bg: Color
    val bg2: Color
    val text: Color
    val tag: Color
    val attr: Color
    val string: Color
    val keyword: Color
    val comment: Color
    val number: Color
    val glow: Color
    val glowAlpha: Float
    val washAlpha: Float
    val scanlineAlpha: Float
}

val LocalLcdPalette = staticCompositionLocalOf { DefaultLcdPalette }

val DefaultLcdPalette = object : LcdPalette {
    override val bg = RetroTokens.LcdBg
    override val bg2 = RetroTokens.LcdBg2
    override val text = RetroTokens.LcdTextOnBg
    override val tag = RetroTokens.LcdTag
    override val attr = RetroTokens.LcdAttr
    override val string = RetroTokens.LcdString
    override val keyword = RetroTokens.LcdKeyword
    override val comment = RetroTokens.LcdComment
    override val number = RetroTokens.LcdNumber
    override val glow = RetroTokens.OliveGlow
    override val glowAlpha = 0.38f
    override val washAlpha = 0.18f
    override val scanlineAlpha = 0.06f
}

@Stable
interface LedPalette {
    val orange: Color
    val green: Color
    val red: Color
    val pulseAlpha: Float
}

val LocalLedPalette = staticCompositionLocalOf { DefaultLedPalette }

val DefaultLedPalette = object : LedPalette {
    override val orange = RetroTokens.LedOrange
    override val green = RetroTokens.LedGreen
    override val red = RetroTokens.LedRed
    override val pulseAlpha = 1f
}

@Stable
interface RetroThemeShapes {
    val shell: dp
    val heroCard: dp
    val card: dp
    val searchBtn: dp
    val dialog: dp
    val tabTop: dp
    val chip: dp
    val pill: dp
    val stencil: dp
    val navItem: dp
}

val LocalRetroShapes = staticCompositionLocalOf { DefaultRetroShapes }

val DefaultRetroShapes = object : RetroThemeShapes {
    override val shell = RetroShapes.Shell
    override val heroCard = RetroShapes.HeroCard
    override val card = RetroShapes.Card
    override val searchBtn = RetroShapes.SearchBtn
    override val dialog = RetroShapes.Dialog
    override val tabTop = RetroShapes.TabTop
    override val chip = RetroShapes.Chip
    override val pill = RetroShapes.Pill
    override val stencil = RetroShapes.Stencil
    override val navItem = RetroShapes.NavItem
}

@Stable
interface RetroTypography {
    val displayHero: TextStyle
    val displaySmall: TextStyle
    val titleLarge: TextStyle
    val titleMedium: TextStyle
    val titleSmall: TextStyle
    val bodyLarge: TextStyle
    val bodyMedium: TextStyle
    val bodySmall: TextStyle
    val labelLarge: TextStyle
    val labelSmall: TextStyle
    val codeMono: TextStyle
    val codeGutter: TextStyle
    val pixelReadout: TextStyle
    val urlBar: TextStyle
}

val LocalRetroTypography = staticCompositionLocalOf { DefaultRetroTypography }

val DefaultRetroTypography = object : RetroTypography {
    override val displayHero = RetroTypography.DisplayHero
    override val displaySmall = RetroTypography.DisplaySmall
    override val titleLarge = RetroTypography.TitleLarge
    override val titleMedium = RetroTypography.TitleMedium
    override val titleSmall = RetroTypography.TitleSmall
    override val bodyLarge = RetroTypography.BodyLarge
    override val bodyMedium = RetroTypography.BodyMedium
    override val bodySmall = RetroTypography.BodySmall
    override val labelLarge = RetroTypography.LabelLarge
    override val labelSmall = RetroTypography.LabelSmall
    override val codeMono = RetroTypography.CodeMono
    override val codeGutter = RetroTypography.CodeGutter
    override val pixelReadout = RetroTypography.PixelReadout
    override val urlBar = RetroTypography.UrlBar
}

@Stable
interface BevelSpec {
    val highlightTop: Color
    val highlightBottom: Color
    val shadowTop: Color
    val shadowBottom: Color
}

val LocalBevelSpec = staticCompositionLocalOf { DefaultBevelSpec }

val DefaultBevelSpec = object : BevelSpec {
    override val highlightTop = RetroTokens.White.copy(alpha = 0.55f)
    override val highlightBottom = RetroTokens.White.copy(alpha = 0.15f)
    override val shadowTop = RetroTokens.Graphite.copy(alpha = 0.08f)
    override val shadowBottom = RetroTokens.Graphite.copy(alpha = 0.14f)
}

@Composable
fun RetroTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = lightScheme,
        typography = textStyles {
            displaySmall = DefaultRetroTypography.displaySmall
            titleMedium = DefaultRetroTypography.titleMedium
            labelLarge = DefaultRetroTypography.labelLarge
            labelSmall = DefaultRetroTypography.labelSmall
            bodyMedium = DefaultRetroTypography.bodyMedium
            bodySmall = DefaultRetroTypography.bodySmall
        },
        content = {
            CompositionLocalProvider(
                LocalContentColor provides RetroTokens.Graphite,
                LocalLcdPalette provides DefaultLcdPalette,
                LocalLedPalette provides DefaultLedPalette,
                LocalRetroShapes provides DefaultRetroShapes,
                LocalRetroTypography provides DefaultRetroTypography,
                LocalBevelSpec provides DefaultBevelSpec
            ) {
                content()
            }
        }
    )
}

val lcdPalette: LcdPalette
    @Composable get() = LocalLcdPalette.current

val ledPalette: LedPalette
    @Composable get() = LocalLedPalette.current

val retroShapes: RetroThemeShapes
    @Composable get() = LocalRetroShapes.current

val retroTypography: RetroTypography
    @Composable get() = LocalRetroTypography.current

val bevelSpec: BevelSpec
    @Composable get() = LocalBevelSpec.current