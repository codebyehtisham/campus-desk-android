package com.derived.campusdesk.sharedui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.derived.campusdesk.sharedui.theme.CampusLayout
import com.derived.campusdesk.sharedui.theme.CampusTypography
import com.derived.campusdesk.sharedui.theme.campusColors

enum class AppTab { Home, Classes, Assignments, Attendance, Profile }

private val DockPillHeight = CampusLayout.FloatingDockHeight.dp

@Composable
fun FloatingDock(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = campusColors()
    val pill = campusPillShape()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = CampusLayout.FloatingDockMaxWidth.dp)
                .height(DockPillHeight)
                .shadow(
                    14.dp,
                    pill,
                    ambientColor = colors.ink.copy(alpha = 0.12f),
                    spotColor = colors.ink.copy(alpha = 0.12f),
                    clip = false,
                )
                .background(colors.surface.copy(alpha = 0.96f), pill)
                .border(1.dp, colors.stroke.copy(alpha = 0.7f), pill),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DockItem(AppTab.Home, "Home", Icons.Default.Home, selected, onSelect, Modifier.weight(1f))
                DockItem(AppTab.Classes, "Classes", Icons.Default.MenuBook, selected, onSelect, Modifier.weight(1f))
                DockItem(AppTab.Assignments, "Assignments", Icons.Default.Assignment, selected, onSelect, Modifier.weight(1f))
                DockItem(AppTab.Attendance, "Attendance", Icons.Default.QrCodeScanner, selected, onSelect, Modifier.weight(1f))
                DockItem(AppTab.Profile, "You", Icons.Default.Person, selected, onSelect, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DockItem(
    tab: AppTab,
    label: String,
    icon: ImageVector,
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = campusColors()
    val isSelected = selected == tab
    val scale by animateFloatAsState(if (isSelected) 1f else 0.96f, spring(dampingRatio = 0.84f), label = "scale")
    val tint = if (isSelected) colors.brandRed else colors.muted

    Column(
        modifier = modifier
            .height(DockPillHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable { onSelect(tab) }
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
        Text(
            label,
            color = tint,
            textAlign = TextAlign.Center,
            style = CampusTypography.Caption.copy(fontSize = 8.sp),
            maxLines = 1,
        )
    }
}

@Composable
fun TabCrossfade(selected: AppTab, modifier: Modifier = Modifier, content: @Composable (AppTab) -> Unit) {
    AnimatedContent(
        targetState = selected,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(spring(dampingRatio = 0.84f)) + scaleIn(initialScale = 0.985f))
                .togetherWith(fadeOut(spring(dampingRatio = 0.84f)) + scaleOut(targetScale = 0.985f))
        },
        label = "tab",
    ) { tab -> content(tab) }
}
