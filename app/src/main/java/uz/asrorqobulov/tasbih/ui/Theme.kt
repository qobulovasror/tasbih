package uz.asrorqobulov.tasbih.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import uz.asrorqobulov.tasbih.data.Accent
import uz.asrorqobulov.tasbih.data.AppSettings
import uz.asrorqobulov.tasbih.data.ThemeMode

// PLAN 8.1 ranglari
private fun accentColor(a: Accent, dark: Boolean) = when (a) {
    Accent.EMERALD, Accent.SYSTEM -> if (dark) Color(0xFF3DBB8E) else Color(0xFF1E7A5C)
    Accent.GOLD -> if (dark) Color(0xFFD4B25A) else Color(0xFF8C6D1F)
    Accent.BLUE -> if (dark) Color(0xFF5B9BE0) else Color(0xFF1F5FA8)
    Accent.PURPLE -> if (dark) Color(0xFF9C84E0) else Color(0xFF5B45A8)
}

val Gold = Color(0xFFD4B25A)
val Tnum = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun TasbihTheme(s: AppSettings, content: @Composable () -> Unit) {
    val dark = when (s.theme) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val acc = accentColor(s.accent, dark)
    val scheme = when {
        s.accent == Accent.SYSTEM && dark -> dynamicDarkColorScheme(LocalContext.current)
        s.accent == Accent.SYSTEM -> dynamicLightColorScheme(LocalContext.current)
        dark -> darkColorScheme(
            primary = acc, onPrimary = Color(0xFF04241A),
            primaryContainer = acc.copy(alpha = 0.35f), onPrimaryContainer = Color(0xFFE8EDEA),
            background = Color(0xFF0E1412), onBackground = Color(0xFFE8EDEA),
            surface = Color(0xFF0E1412), onSurface = Color(0xFFE8EDEA),
            surfaceContainer = Color(0xFF18201D), surfaceContainerHigh = Color(0xFF202A26),
            surfaceContainerLow = Color(0xFF141B18), surfaceContainerHighest = Color(0xFF26312D),
            surfaceVariant = Color(0xFF202A26), onSurfaceVariant = Color(0xFF93A39C),
            outline = Color(0xFF3A4743), outlineVariant = Color(0xFF26312D),
            secondaryContainer = Color(0xFF26312D), onSecondaryContainer = Color(0xFFE8EDEA),
            tertiary = Gold, error = Color(0xFFE5736B), onError = Color(0xFF2A0B08),
        )
        else -> lightColorScheme(
            primary = acc, onPrimary = Color.White,
            primaryContainer = acc.copy(alpha = 0.18f), onPrimaryContainer = Color(0xFF14201B),
            // fon kulrang-yashil, kartalar oq — yorug'likda ajralib tursin
            background = Color(0xFFE8EDEA), onBackground = Color(0xFF14201B),
            surface = Color(0xFFE8EDEA), onSurface = Color(0xFF14201B),
            surfaceContainer = Color.White, surfaceContainerHigh = Color(0xFFE4E9E6),
            surfaceContainerLow = Color(0xFFF4F6F4), surfaceContainerHighest = Color(0xFFD3DCD7),
            surfaceVariant = Color(0xFFE4E9E6), onSurfaceVariant = Color(0xFF4F605A),
            outline = Color(0xFFB4C0BA), outlineVariant = Color(0xFFD3DCD7),
            secondaryContainer = Color(0xFFD3DCD7), onSecondaryContainer = Color(0xFF14201B),
            tertiary = Color(0xFFA8862E), error = Color(0xFFB3261E), onError = Color.White,
        )
    }
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val w = (view.context as Activity).window
        WindowCompat.getInsetsController(w, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    // Surface — matn rangi (onBackground) to'g'ri bo'lishi uchun
    MaterialTheme(colorScheme = scheme) {
        Surface(color = scheme.background, contentColor = scheme.onBackground, content = content)
    }
}
