package upca.pm.northhealth.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val LightColors = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    secondary = Color(0xFF455A64),
    onSecondary = Color.White,
    tertiary = Orange40,
    onTertiary = Color.White,
    background = Color(0xFFFAFAF7),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8ECE6),
    onSurfaceVariant = Ink,
    surfaceContainerHighest = Color(0xFFEDEFEA),
    error = Color(0xFFB3261E)
)

private val HighContrastColors = darkColorScheme(
    primary = GreenHC,
    onPrimary = Color.Black,
    secondary = Color.White,
    onSecondary = Color.Black,
    tertiary = OrangeHC,
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color.White,
    surfaceContainerHighest = Color(0xFF1E1E1E),
    error = Color(0xFFFF8A80)
)

/**
 * Tema com as preferências de acessibilidade do utilizador:
 * - fontScale: multiplica o tamanho de letra (1.0, 1.25 ou 1.5)
 * - highContrast: esquema de cores de alto contraste
 */
@Composable
fun NorthHealthTheme(
    fontScale: Float = 1f,
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * fontScale)
    ) {
        MaterialTheme(
            colorScheme = if (highContrast) HighContrastColors else LightColors,
            typography = Typography,
            content = content
        )
    }
}
