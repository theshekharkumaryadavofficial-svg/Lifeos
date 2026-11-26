package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LifeOSColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = TextOnDark,
    primaryContainer = CrimsonDeep,
    onPrimaryContainer = TextOnDark,
    secondary = AmberGold,
    onSecondary = ObsidianBg,
    secondaryContainer = SoftGoldSurface,
    onSecondaryContainer = BrightGold,
    tertiary = ElectricCyan,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextOnDark,
    surface = DeepTwilight,
    onSurface = TextOnDark,
    surfaceVariant = GlassSurfaceElevated,
    onSurfaceVariant = TextOnDarkMuted,
    outline = GlassBorder
)

val LifeOSShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LifeOSTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LifeOSColorScheme,
        typography = Typography,
        shapes = LifeOSShapes,
        content = content
    )
}
