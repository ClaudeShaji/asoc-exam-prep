package com.radioprep.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.ui.graphics.vector.ImageVector

enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Outlined.Home),
    STUDY("study", "Study", Icons.AutoMirrored.Outlined.MenuBook),
    MORSE("morse", "Morse", Icons.Outlined.GraphicEq),
    EXAM("exam", "Exam", Icons.Outlined.Quiz),
    PROGRESS("progress", "Progress", Icons.Outlined.Insights),
}

object Routes {
    const val SETTINGS = "settings"
    const val INFO = "info"
    const val REFERENCE = "reference"
    val all: Set<String> = Tab.entries.map { it.route }.toSet() + SETTINGS + INFO + REFERENCE
}
