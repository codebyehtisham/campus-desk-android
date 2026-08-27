package com.derived.campusdesk.sharedui.theme

import androidx.compose.ui.graphics.Color

object CampusColors {
    val BrandRed = Color(0xFF0F5C5C)
    val BrandBlue = Color(0xFF2A8F8F)
    val PrimaryDark = Color(0xFF0A4545)
    val Canvas = Color(0xFFE8F2F2)
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF0A3D3D)
    val Muted = Color(0xFF5A7272)
    val Stroke = Color(0xFFC5DDDD)
    val OnBrand = Color(0xFFFFFFFF)
    val PrimaryPale = Color(0xFFE6F4F4)
    val Success = Color(0xFF0C8A62)
    val StaffSurface = Color(0xFFF2F8F8)
    val Body = Color(0xFF2F4A4A)
    val DevBanner = Color(0xFFFF8C42)

    // Dark mode
    val DarkCanvas = Color(0xFF0A1F1F)
    val DarkSurface = Color(0xFF122828)
    val DarkInk = Color(0xFFE8F2F2)
    val DarkMuted = Color(0xFF8AA8A8)
    val DarkStroke = Color(0xFF1E4040)
}

object CampusSpacing {
    const val Xs = 6
    const val Sm = 10
    const val Md = 16
    const val Lg = 24
    const val Xl = 32
    const val Xxl = 48
}

object CampusRadius {
    const val Sm = 14
    const val Md = 22
    const val Lg = 32
    const val Pill = 50 // percent for capsule corners (Compose RoundedCornerShape Int overload)
}

object CampusLayout {
    const val FloatingDockHeight = 64
    const val FloatingDockMaxWidth = 520
    const val TabScrollBottom = 8
    const val ContentMaxWidth = 760
    const val FormMaxWidth = 480
    val TabScrollContentInset get() = FloatingDockHeight + TabScrollBottom + 42
}
