package com.derived.campusdesk.sharedui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = CampusColors.BrandRed,
    onPrimary = CampusColors.OnBrand,
    secondary = CampusColors.BrandBlue,
    background = CampusColors.Canvas,
    surface = CampusColors.Surface,
    onBackground = CampusColors.Ink,
    onSurface = CampusColors.Ink,
    outline = CampusColors.Stroke,
    error = CampusColors.BrandRed,
)

private val DarkColors = darkColorScheme(
    primary = CampusColors.BrandBlue,
    onPrimary = CampusColors.OnBrand,
    secondary = CampusColors.BrandRed,
    background = CampusColors.DarkCanvas,
    surface = CampusColors.DarkSurface,
    onBackground = CampusColors.DarkInk,
    onSurface = CampusColors.DarkInk,
    outline = CampusColors.DarkStroke,
)

object CampusTypography {
    val Display = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
    )
    val Title = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
    )
    val Headline = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
    )
    val Body = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    )
    val Callout = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
    )
    val Caption = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
    )
    val Eyebrow = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.8.sp,
    )
    val CourseTitle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
    )
}

@Composable
fun CampusDeskTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}

@Composable
fun campusColors() = if (isSystemInDarkTheme()) {
    CampusColorTokens(
        brandRed = CampusColors.BrandRed,
        brandBlue = CampusColors.BrandBlue,
        primaryDark = CampusColors.PrimaryDark,
        canvas = CampusColors.DarkCanvas,
        surface = CampusColors.DarkSurface,
        ink = CampusColors.DarkInk,
        muted = CampusColors.DarkMuted,
        stroke = CampusColors.DarkStroke,
        onBrand = CampusColors.OnBrand,
        primaryPale = CampusColors.PrimaryPale.copy(alpha = 0.15f),
        success = CampusColors.Success,
        staffSurface = CampusColors.DarkSurface,
        body = CampusColors.DarkMuted,
    )
} else {
    CampusColorTokens(
        brandRed = CampusColors.BrandRed,
        brandBlue = CampusColors.BrandBlue,
        primaryDark = CampusColors.PrimaryDark,
        canvas = CampusColors.Canvas,
        surface = CampusColors.Surface,
        ink = CampusColors.Ink,
        muted = CampusColors.Muted,
        stroke = CampusColors.Stroke,
        onBrand = CampusColors.OnBrand,
        primaryPale = CampusColors.PrimaryPale,
        success = CampusColors.Success,
        staffSurface = CampusColors.StaffSurface,
        body = CampusColors.Body,
    )
}

data class CampusColorTokens(
    val brandRed: androidx.compose.ui.graphics.Color,
    val brandBlue: androidx.compose.ui.graphics.Color,
    val primaryDark: androidx.compose.ui.graphics.Color,
    val canvas: androidx.compose.ui.graphics.Color,
    val surface: androidx.compose.ui.graphics.Color,
    val ink: androidx.compose.ui.graphics.Color,
    val muted: androidx.compose.ui.graphics.Color,
    val stroke: androidx.compose.ui.graphics.Color,
    val onBrand: androidx.compose.ui.graphics.Color,
    val primaryPale: androidx.compose.ui.graphics.Color,
    val success: androidx.compose.ui.graphics.Color,
    val staffSurface: androidx.compose.ui.graphics.Color,
    val body: androidx.compose.ui.graphics.Color,
)
