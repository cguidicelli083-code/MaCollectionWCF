package com.nawash.macollectionwcf.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.nawash.macollectionwcf.data.AppPrefs

// Palette « gamer » néon sur fond sombre (thème DEFAULT). Ces couleurs restent fixes pour
// certains éléments sémantiques (prix, modifier, supprimer) quel que soit le thème actif — seuls
// l'accent/l'accent secondaire (boutons, sélections, contour des cartes) et le dégradé de fond
// suivent la licence choisie (voir [AppTheme]).
val NeonPurple = Color(0xFF8B5CFF)
val NeonCyan = Color(0xFF22E6FF)
val NeonPink = Color(0xFFFF3D9A)
val DeepBg = Color(0xFF0B0B14)
val SurfaceBg = Color(0xFF15151F)
val CardTop = Color(0xFF20203A)
val CardBottom = Color(0xFF16161F)

val CardGradient = Brush.verticalGradient(listOf(CardTop, CardBottom))

/** Contour lumineux des cartes : suit le thème actif (accent → accent secondaire). */
val NeonBorder: Brush
    @Composable get() {
        val t = AppTheme.byId(AppPrefs.selectedTheme.value)
        return Brush.linearGradient(listOf(t.accent, t.accentAlt))
    }

/** Dégradé de fond du thème actif. */
@Composable
fun themedGradient(): Brush = AppTheme.byId(AppPrefs.selectedTheme.value).gradient

private fun gamerColors(primary: Color, onPrimary: Color, secondary: Color) = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    secondary = secondary,
    onSecondary = Color(0xFF00131A),
    tertiary = NeonPink,
    onTertiary = Color.White,
    background = DeepBg,
    onBackground = Color(0xFFECECF7),
    surface = SurfaceBg,
    onSurface = Color(0xFFECECF7),
    surfaceVariant = CardTop,
    onSurfaceVariant = Color(0xFFB6B6CC),
    outline = Color(0xFF3A3A55)
)

@Composable
fun MaCollectionWcfTheme(content: @Composable () -> Unit) {
    val theme = AppTheme.byId(AppPrefs.selectedTheme.value)
    MaterialTheme(
        colorScheme = gamerColors(theme.accent, theme.onAccent, theme.accentAlt),
        typography = Typography(),
        content = content
    )
}

/**
 * Thèmes visuels par licence de figurine : chaque thème redéfinit le dégradé de fond, l'accent
 * (boutons/sélections), l'accent secondaire (bordures lumineuses) et les emojis des 4 icônes de
 * navigation (Collection/Souhaits/Encyclo/Total). Tous gratuits et sélectionnables dès
 * l'installation (pas de boutique/déblocage dans cette app). Emojis standards uniquement —
 * aucune image/logo de personnage sous licence (voir aussi l'icône de l'app).
 */
enum class AppTheme(
    val id: String,
    val label: String,
    val emoji: String,
    val gradient: Brush,
    val accent: Color,
    val onAccent: Color,
    val accentAlt: Color,
    val navIcons: List<String> = emptyList()
) {
    DEFAULT(
        "default", "Néon (défaut)", "🕹️",
        Brush.verticalGradient(listOf(Color(0xFF15102B), Color(0xFF0B0B14), Color(0xFF120A1F))),
        NeonPurple, Color.White, NeonCyan
    ),

    /** Mers déchaînées, rouge du gilet et or de la paille. */
    ONE_PIECE(
        "one_piece", "Pirates", "🏴‍☠️",
        Brush.verticalGradient(listOf(Color(0xFF041C32), Color(0xFF0B0B14), Color(0xFF1B2A41))),
        Color(0xFFE53935), Color.White, Color(0xFFFFC107),
        listOf("🏴‍☠️", "💰", "📖", "⚓")
    ),

    /** Nuit froide, bleu spirituel des sabres et argent de l'acier. */
    BLEACH(
        "bleach", "Shinigami", "⚔️",
        Brush.verticalGradient(listOf(Color(0xFF0A0A12), Color(0xFF0B0B14), Color(0xFF141428))),
        Color(0xFF29B6F6), Color(0xFF00131A), Color(0xFFB0BEC5),
        listOf("⚔️", "💀", "📖", "🌙")
    ),

    /** Énergie orange du gi et éclat doré du Super Saiyan. */
    DRAGON_BALL(
        "dragon_ball", "Boule de cristal", "🐉",
        Brush.verticalGradient(listOf(Color(0xFF2B1000), Color(0xFF0B0B14), Color(0xFF1A0E00))),
        Color(0xFFFF7A00), Color.Black, Color(0xFFFFD600),
        listOf("🐉", "⭐", "📖", "🔥")
    ),

    /** Village caché dans la forêt, orange du bandeau et vert des feuilles. */
    NARUTO(
        "naruto", "Village caché", "🍥",
        Brush.verticalGradient(listOf(Color(0xFF0D1F12), Color(0xFF0B0B14), Color(0xFF1A2A1A))),
        Color(0xFFFF6F00), Color.Black, Color(0xFF66BB6A),
        listOf("🍥", "🍃", "📖", "🌀")
    );

    companion object {
        fun byId(id: String?): AppTheme = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
