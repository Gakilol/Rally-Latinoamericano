package ni.edu.rally.nexosos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Paleta representativa Celeste y Blanco de Nexo SOS.
 */
object NexoCelesteColors {
    val CelestePrimario = Color(0xFF0288D1)       // Celeste institucional intenso
    val CelesteBrillante = Color(0xFF03A9F4)      // Celeste vivo para botones y ruta Waze
    val CelesteClaroFondo = Color(0xFFE1F5FE)     // Fondo celeste suave para tarjetas destacadas
    val CelesteBorde = Color(0xFF81D4FA)          // Borde celeste limpio
    val CelesteOscuroTexto = Color(0xFF01579B)    // Texto de alto contraste sobre celeste/blanco
    val BlancoPuro = Color(0xFFFFFFFF)            // Blanco representativo principal
    val BlancoHieloFondo = Color(0xFFF4FAFF)      // Fondo general blanco-celeste muy claro
}

private val EsquemaCelesteYBlanco = lightColorScheme(
    primary = NexoCelesteColors.CelestePrimario,
    onPrimary = NexoCelesteColors.BlancoPuro,
    primaryContainer = NexoCelesteColors.CelesteClaroFondo,
    onPrimaryContainer = NexoCelesteColors.CelesteOscuroTexto,
    secondary = NexoCelesteColors.CelesteBrillante,
    onSecondary = NexoCelesteColors.BlancoPuro,
    secondaryContainer = Color(0xFFB3E5FC),
    onSecondaryContainer = NexoCelesteColors.CelesteOscuroTexto,
    background = NexoCelesteColors.BlancoHieloFondo,
    onBackground = NexoCelesteColors.CelesteOscuroTexto,
    surface = NexoCelesteColors.BlancoPuro,
    onSurface = Color(0xFF0D2538),
    surfaceVariant = NexoCelesteColors.CelesteClaroFondo,
    onSurfaceVariant = NexoCelesteColors.CelesteOscuroTexto
)

@Composable
fun NexoSosCelesteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaCelesteYBlanco,
        content = content
    )
}
