package com.shahid.cents

import android.content.Context
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AppTheme(val label: String) {
    System("System"),
    Light("Light"),
    Dark("Dark"),
    Monochrome("Monochrome"),
    Forest("Forest"),
    Ocean("Ocean")
}

object ThemeManager {
    private var _currentTheme = MutableStateFlow(AppTheme.System)
    val currentTheme: StateFlow<AppTheme> = _currentTheme

    fun load(context: Context) {
        val ord = context.getSharedPreferences("cents_prefs", Context.MODE_PRIVATE)
            .getInt("theme", 0)
        _currentTheme.value = AppTheme.entries.getOrElse(ord) { AppTheme.System }
    }

    fun set(context: Context, theme: AppTheme) {
        context.getSharedPreferences("cents_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("theme", theme.ordinal)
            .apply()
        _currentTheme.value = theme
    }
}

object ThemePalettes {
    val lightGreen = lightColorScheme(
        background = Color(0xFFFAFAF7),
        surface = Color.White,
        surfaceVariant = Color(0xFFF3F1EA),
        primary = Color(0xFF31D98B),
        onBackground = Color(0xFF101820),
        onSurface = Color(0xFF101820),
        onSurfaceVariant = Color(0xFF687068),
        outline = Color(0xFFE0DDD2),
        error = Color(0xFFC33A20),
        secondary = Color(0xFF0B7A4B)
    )
    val darkGreen = darkColorScheme(
        background = Color(0xFF0D1117),
        surface = Color(0xFF161B22),
        surfaceVariant = Color(0xFF21262D),
        primary = Color(0xFF31D98B),
        onBackground = Color(0xFFE6EDF3),
        onSurface = Color(0xFFE6EDF3),
        onSurfaceVariant = Color(0xFF8B949E),
        outline = Color(0xFF30363D),
        error = Color(0xFFF85149),
        secondary = Color(0xFF3FB950)
    )

    val lightMono = lightColorScheme(
        background = Color(0xFFF5F5F5),
        surface = Color.White,
        surfaceVariant = Color(0xFFE8E8E8),
        primary = Color(0xFF555555),
        onBackground = Color(0xFF1A1A1A),
        onSurface = Color(0xFF1A1A1A),
        onSurfaceVariant = Color(0xFF777777),
        outline = Color(0xFFCCCCCC),
        error = Color(0xFF993333),
        secondary = Color(0xFF666666)
    )
    val darkMono = darkColorScheme(
        background = Color(0xFF111111),
        surface = Color(0xFF1A1A1A),
        surfaceVariant = Color(0xFF252525),
        primary = Color(0xFFBBBBBB),
        onBackground = Color(0xFFDDDDDD),
        onSurface = Color(0xFFDDDDDD),
        onSurfaceVariant = Color(0xFF888888),
        outline = Color(0xFF333333),
        error = Color(0xFFCC6666),
        secondary = Color(0xFF999999)
    )

    val lightForest = lightColorScheme(
        background = Color(0xFFF6FAF4),
        surface = Color.White,
        surfaceVariant = Color(0xFFEAF3E6),
        primary = Color(0xFF2E7D32),
        onBackground = Color(0xFF1B2E1B),
        onSurface = Color(0xFF1B2E1B),
        onSurfaceVariant = Color(0xFF5A7A5A),
        outline = Color(0xFFC8DDC0),
        error = Color(0xFFA04040),
        secondary = Color(0xFF558B2F)
    )
    val darkForest = darkColorScheme(
        background = Color(0xFF0F1A0F),
        surface = Color(0xFF162316),
        surfaceVariant = Color(0xFF1E2E1E),
        primary = Color(0xFF4CAF50),
        onBackground = Color(0xFFD8EAD8),
        onSurface = Color(0xFFD8EAD8),
        onSurfaceVariant = Color(0xFF7A9E7A),
        outline = Color(0xFF2A422A),
        error = Color(0xFFEF9A9A),
        secondary = Color(0xFF81C784)
    )

    val lightOcean = lightColorScheme(
        background = Color(0xFFF4F8FC),
        surface = Color.White,
        surfaceVariant = Color(0xFFE4EDF7),
        primary = Color(0xFF1565C0),
        onBackground = Color(0xFF0D1B2A),
        onSurface = Color(0xFF0D1B2A),
        onSurfaceVariant = Color(0xFF5A7A9A),
        outline = Color(0xFFB8CCE0),
        error = Color(0xFFC62828),
        secondary = Color(0xFF1976D2)
    )
    val darkOcean = darkColorScheme(
        background = Color(0xFF0B1929),
        surface = Color(0xFF112240),
        surfaceVariant = Color(0xFF183152),
        primary = Color(0xFF64B5F6),
        onBackground = Color(0xFFD0E0F0),
        onSurface = Color(0xFFD0E0F0),
        onSurfaceVariant = Color(0xFF7A9ABE),
        outline = Color(0xFF1E3A5F),
        error = Color(0xFFEF9A9A),
        secondary = Color(0xFF42A5F5)
    )

    fun scheme(theme: AppTheme, isDark: Boolean) = when (theme) {
        AppTheme.System -> if (isDark) darkGreen else lightGreen
        AppTheme.Light -> lightGreen
        AppTheme.Dark -> darkGreen
        AppTheme.Monochrome -> if (isDark) darkMono else lightMono
        AppTheme.Forest -> if (isDark) darkForest else lightForest
        AppTheme.Ocean -> if (isDark) darkOcean else lightOcean
    }
}

data class AppColors(
    val spentCardBg: Color,
    val incomeCardBg: Color,
    val positiveAmount: Color,
    val negativeAmount: Color,
    val selectedCategoryBg: Color,
    val progressBarBg: Color,
    val darkCardBg: Color,
    val emptyStateBg: Color,
    val spentCardSelectedBorder: Color,
    val incomeCardSelectedBorder: Color
)

object AppColorPalettes {
    val green = AppColors(
        spentCardBg = Color(0xFFFFE0D6),
        incomeCardBg = Color(0xFFDDF6E7),
        positiveAmount = Color(0xFF087A38),
        negativeAmount = Color(0xFFC33A20),
        selectedCategoryBg = Color(0xFFE6FFF1),
        progressBarBg = Color(0xFFEFEFE8),
        darkCardBg = Color(0xFF101820),
        emptyStateBg = Color(0xFFEFEFE8),
        spentCardSelectedBorder = Color(0xFF31D98B),
        incomeCardSelectedBorder = Color(0xFF31D98B)
    )
    val greenDark = AppColors(
        spentCardBg = Color(0xFF3D1F1A),
        incomeCardBg = Color(0xFF1A3D2A),
        positiveAmount = Color(0xFF3FB950),
        negativeAmount = Color(0xFFF85149),
        selectedCategoryBg = Color(0xFF1A3D2A),
        progressBarBg = Color(0xFF21262D),
        darkCardBg = Color(0xFF1C2128),
        emptyStateBg = Color(0xFF1C2128),
        spentCardSelectedBorder = Color(0xFF31D98B),
        incomeCardSelectedBorder = Color(0xFF31D98B)
    )

    val mono = AppColors(
        spentCardBg = Color(0xFFE8E8E8),
        incomeCardBg = Color(0xFFE8E8E8),
        positiveAmount = Color(0xFF444444),
        negativeAmount = Color(0xFF444444),
        selectedCategoryBg = Color(0xFFDDDDDD),
        progressBarBg = Color(0xFFE0E0E0),
        darkCardBg = Color(0xFF333333),
        emptyStateBg = Color(0xFFE8E8E8),
        spentCardSelectedBorder = Color(0xFF666666),
        incomeCardSelectedBorder = Color(0xFF666666)
    )
    val monoDark = AppColors(
        spentCardBg = Color(0xFF252525),
        incomeCardBg = Color(0xFF252525),
        positiveAmount = Color(0xFFBBBBBB),
        negativeAmount = Color(0xFFBBBBBB),
        selectedCategoryBg = Color(0xFF2A2A2A),
        progressBarBg = Color(0xFF2A2A2A),
        darkCardBg = Color(0xFF222222),
        emptyStateBg = Color(0xFF222222),
        spentCardSelectedBorder = Color(0xFF888888),
        incomeCardSelectedBorder = Color(0xFF888888)
    )

    val forest = AppColors(
        spentCardBg = Color(0xFFEAF3E6),
        incomeCardBg = Color(0xFFEAF3E6),
        positiveAmount = Color(0xFF2E7D32),
        negativeAmount = Color(0xFFA04040),
        selectedCategoryBg = Color(0xFFE0F0DC),
        progressBarBg = Color(0xFFE0E8DC),
        darkCardBg = Color(0xFF1B2E1B),
        emptyStateBg = Color(0xFFEAF3E6),
        spentCardSelectedBorder = Color(0xFF4CAF50),
        incomeCardSelectedBorder = Color(0xFF4CAF50)
    )
    val forestDark = AppColors(
        spentCardBg = Color(0xFF1E2E1E),
        incomeCardBg = Color(0xFF1E2E1E),
        positiveAmount = Color(0xFF81C784),
        negativeAmount = Color(0xFFEF9A9A),
        selectedCategoryBg = Color(0xFF1E2E1E),
        progressBarBg = Color(0xFF1E2E1E),
        darkCardBg = Color(0xFF162316),
        emptyStateBg = Color(0xFF162316),
        spentCardSelectedBorder = Color(0xFF4CAF50),
        incomeCardSelectedBorder = Color(0xFF4CAF50)
    )

    val ocean = AppColors(
        spentCardBg = Color(0xFFE4EDF7),
        incomeCardBg = Color(0xFFE4EDF7),
        positiveAmount = Color(0xFF1565C0),
        negativeAmount = Color(0xFFC62828),
        selectedCategoryBg = Color(0xFFD8E4F2),
        progressBarBg = Color(0xFFDCE4EC),
        darkCardBg = Color(0xFF0D1B2A),
        emptyStateBg = Color(0xFFE4EDF7),
        spentCardSelectedBorder = Color(0xFF42A5F5),
        incomeCardSelectedBorder = Color(0xFF42A5F5)
    )
    val oceanDark = AppColors(
        spentCardBg = Color(0xFF183152),
        incomeCardBg = Color(0xFF183152),
        positiveAmount = Color(0xFF64B5F6),
        negativeAmount = Color(0xFFEF9A9A),
        selectedCategoryBg = Color(0xFF183152),
        progressBarBg = Color(0xFF183152),
        darkCardBg = Color(0xFF112240),
        emptyStateBg = Color(0xFF112240),
        spentCardSelectedBorder = Color(0xFF64B5F6),
        incomeCardSelectedBorder = Color(0xFF64B5F6)
    )

    fun colors(theme: AppTheme, isDark: Boolean) = when (theme) {
        AppTheme.System -> if (isDark) greenDark else green
        AppTheme.Light -> green
        AppTheme.Dark -> greenDark
        AppTheme.Monochrome -> if (isDark) monoDark else mono
        AppTheme.Forest -> if (isDark) forestDark else forest
        AppTheme.Ocean -> if (isDark) oceanDark else ocean
    }
}
