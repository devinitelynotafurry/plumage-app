package dev.plumage.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightScheme = lightColorScheme(
    primary = Clay,
    onPrimary = Cream,
    primaryContainer = Blush,
    onPrimaryContainer = ClayDeep,
    secondary = Salmon,
    onSecondary = ClayDeep,
    secondaryContainer = BlushSoft,
    onSecondaryContainer = ClayDeep,
    tertiary = Amber,
    onTertiary = ClayDeep,
    tertiaryContainer = Amber,
    onTertiaryContainer = ClayDeep,
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = Blush,
    onSurfaceVariant = InkMuted,
    surfaceContainer = BlushSoft,
    surfaceContainerHigh = Blush,
    outline = Salmon,
    outlineVariant = BlushSoft,
    error = Rust,
    onError = Cream
)

private val DarkScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightOnPrimary,
    primaryContainer = NightRaised,
    onPrimaryContainer = NightPrimary,
    secondary = Salmon,
    onSecondary = NightOnPrimary,
    secondaryContainer = NightRaised,
    onSecondaryContainer = NightText,
    tertiary = Amber,
    onTertiary = NightOnPrimary,
    background = NightBase,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightRaised,
    onSurfaceVariant = NightTextMuted,
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightRaised,
    outline = NightTextMuted,
    outlineVariant = NightRaised,
    error = NightError,
    onError = NightOnPrimary
)

/**
 * Dynamic color needs API 31. Below that the toggle in Settings is shown disabled
 * with "Unavailable on this device, using the built-in palette", which is why
 * [dynamicColorAvailable] is public.
 */
val dynamicColorAvailable: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun PlumageTheme(
    useDynamicColor: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scheme = when {
        useDynamicColor && dynamicColorAvailable ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = PlumageTypography,
        content = content
    )
}
