package com.bildirimbutce.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bildirimbutce.parser.Category

/**
 * Tasarim token'lari (v2).
 *
 * DIKKAT - dynamicColor kasitli olarak YOK.
 * Android 12+ cihazlarda dinamik renk acik olsaydi asagidaki paletin hicbiri
 * gorunmezdi; sistem duvar kagidindan renk uretirdi. Marka kimligi ve kategori
 * renkleri bu uygulamanin okunabilirliginin parcasi oldugu icin devre disi.
 */

// --- Marka + acik tema ---
private val BrandLight = Color(0xFF0E7C66)
private val BrandBright = Color(0xFF14B88F)
private val BackgroundLight = Color(0xFFF6F4EF)
private val SurfaceLight = Color(0xFFFFFDF9)
private val SurfaceMutedLight = Color(0xFFEDEAE2)
private val OnBackgroundLight = Color(0xFF11151C)
private val OnBackgroundMutedLight = Color(0xFF5C6470)
private val OutlineLight = Color(0xFFDAD5CA)

// --- Koyu tema ---
private val BackgroundDark = Color(0xFF0A0D12)
private val SurfaceDark = Color(0xFF12161E)
private val SurfaceMutedDark = Color(0xFF1B212B)
private val BrandDark = Color(0xFF2AE3AE)
private val OnBrandDark = Color(0xFF06231B)
private val OnBackgroundDark = Color(0xFFF6F4EF)
private val OnBackgroundMutedDark = Color(0xFF98A0AC)
private val OutlineDark = Color(0x1FF6F4EF)
private val ScrimDark = Color(0x8C04060A)

/**
 * Material 3 semasinda karsiligi olmayan, uygulamaya ozgu roller.
 */
data class AppColors(
    val brandBright: Color,
    val surfaceMuted: Color,
    val onBackgroundMuted: Color,
    val refund: Color,
    val warning: Color,
    val danger: Color,
    val proAccent: Color,
    val onProAccent: Color,
    val isDark: Boolean
) {
    /** Kategori rengi - iki temada da ayni. */
    fun categoryColor(category: Category): Color = when (category) {
        Category.MARKET -> Color(0xFF2AE3AE)
        Category.YEME_ICME -> Color(0xFFFFB020)
        Category.ULASIM -> Color(0xFFC6F24E)
        Category.FATURA -> Color(0xFF4FC3F7)
        Category.ALISVERIS -> Color(0xFFFF6B5A)
        Category.SAGLIK -> Color(0xFFFF8FB1)
        Category.EGLENCE -> Color(0xFF8B7BFF)
        Category.DIGER -> Color(0xFF7A8794)
    }

    /** Islem satiri dolgusu: kategori renginin %12'si. */
    fun categoryTint(category: Category): Color = categoryColor(category).copy(alpha = 0.12f)

    /** Islem satiri kenarligi: kategori renginin %22'si. */
    fun categoryTintBorder(category: Category): Color = categoryColor(category).copy(alpha = 0.22f)
}

/**
 * Acik temada durum renkleri kisiliyor: tasarimdaki #2AE3AE / #FFB020 koyu
 * zemin icin secilmis, kum rengi zeminde metin olarak kontrast birakmiyor.
 */
private val LightAppColors = AppColors(
    brandBright = BrandBright,
    surfaceMuted = SurfaceMutedLight,
    onBackgroundMuted = OnBackgroundMutedLight,
    refund = Color(0xFF0E7C66),
    warning = Color(0xFFB8730A),
    danger = Color(0xFFD1442F),
    proAccent = Color(0xFFD9A84E),
    onProAccent = Color(0xFF2B1D02),
    isDark = false
)

private val DarkAppColors = AppColors(
    brandBright = BrandBright,
    surfaceMuted = SurfaceMutedDark,
    onBackgroundMuted = OnBackgroundMutedDark,
    refund = Color(0xFF2AE3AE),
    warning = Color(0xFFFFB020),
    danger = Color(0xFFFF6B5A),
    proAccent = Color(0xFFD9A84E),
    onProAccent = Color(0xFF2B1D02),
    isDark = true
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/**
 * Container renkleri acikca geciliyor: varsayilan birakilsalardi M3'un mor
 * baseline'i sizardi ve izin karti mor cikardi.
 */
private val LightScheme = lightColorScheme(
    primary = BrandLight,
    onPrimary = Color(0xFFFFFDF9),
    primaryContainer = Color(0xFFD6EFE7),
    onPrimaryContainer = Color(0xFF042219),
    secondary = Color(0xFF4A5A55),
    onSecondary = Color(0xFFFFFDF9),
    secondaryContainer = SurfaceMutedLight,
    onSecondaryContainer = OnBackgroundLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceMutedLight,
    onSurfaceVariant = OnBackgroundMutedLight,
    outline = OutlineLight,
    outlineVariant = OutlineLight,
    inverseSurface = OnBackgroundLight,
    inverseOnSurface = BackgroundLight,
    error = Color(0xFFD1442F),
    onError = Color(0xFFFFFDF9)
)

private val DarkScheme = darkColorScheme(
    primary = BrandDark,
    onPrimary = OnBrandDark,
    primaryContainer = Color(0xFF0C3B2F),
    onPrimaryContainer = Color(0xFFB6F5E2),
    secondary = Color(0xFFB0CCC4),
    onSecondary = Color(0xFF06231B),
    secondaryContainer = SurfaceMutedDark,
    onSecondaryContainer = OnBackgroundDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceMutedDark,
    onSurfaceVariant = OnBackgroundMutedDark,
    outline = OutlineDark,
    outlineVariant = OutlineDark,
    inverseSurface = OnBackgroundDark,
    inverseOnSurface = BackgroundDark,
    scrim = ScrimDark,
    error = Color(0xFFFF6B5A),
    onError = Color(0xFF2B0703)
)

/** Kose yariçaplari - tasarim token'lari birebir. */
object AppRadius {
    val xs = 6.dp
    val sm = 10.dp
    val md = 13.dp
    val lg = 20.dp
    val xl = 26.dp
    val sheet = 28.dp
}

/** Aralik olcegi. s6 (26dp) ekran kenar boslugudur. */
object AppSpace {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 20.dp
    val s6 = 26.dp
    val s8 = 34.dp
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(AppRadius.xs),
    small = RoundedCornerShape(AppRadius.sm),
    medium = RoundedCornerShape(AppRadius.md),
    large = RoundedCornerShape(AppRadius.lg),
    extraLarge = RoundedCornerShape(AppRadius.xl)
)

@Composable
fun BildirimButceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

/** Tema disi renklere erisim: `AppTheme.colors.refund` */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}
