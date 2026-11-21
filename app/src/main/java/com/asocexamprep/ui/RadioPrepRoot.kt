package com.asocexamprep.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import com.asocexamprep.ui.game.CelebrationHost
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.asocexamprep.ui.screens.ExamScreen
import com.asocexamprep.ui.screens.HomeScreen
import com.asocexamprep.ui.screens.InfoListScreen
import com.asocexamprep.ui.screens.InfoPageScreen
import com.asocexamprep.ui.screens.ReferenceScreen
import com.asocexamprep.ui.screens.TopicScreen
import com.asocexamprep.ui.morse.AlphabetScreen
import com.asocexamprep.ui.morse.CopyScreen
import com.asocexamprep.ui.morse.LearnScreen
import com.asocexamprep.ui.morse.MorseScreen
import com.asocexamprep.ui.morse.SendScreen
import com.asocexamprep.ui.screens.ProgressScreen
import com.asocexamprep.ui.screens.SettingsScreen
import com.asocexamprep.ui.screens.StudyScreen

@Composable
fun RadioPrepRoot(startRoute: String = Tab.HOME.route) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBar = current == null || Tab.entries.any { it.route == current }

    fun goTab(tab: Tab) = nav.navigate(tab.route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        bottomBar = {
            if (showBar) NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = { goTab(tab) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("tab_${tab.route}"),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.HOME.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.HOME.route) { HomeScreen(onSettings = { nav.navigate(Routes.SETTINGS) }) }
            composable(Tab.STUDY.route) {
                StudyScreen(
                    onTopic = { nav.navigate("topic/$it") },
                    onInfo = { nav.navigate(Routes.INFO) },
                    onReference = { nav.navigate(Routes.REFERENCE) },
                )
            }
            composable("topic/{id}") { TopicScreen(it.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() }) }
            composable(Routes.INFO) { InfoListScreen(onPage = { id -> nav.navigate("info/$id") }, onBack = { nav.popBackStack() }) }
            composable("info/{id}") { InfoPageScreen(it.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() }) }
            composable(Routes.REFERENCE) { ReferenceScreen(onBack = { nav.popBackStack() }) }
            composable(Tab.MORSE.route) { MorseScreen(onOpen = { nav.navigate("morse/$it") }) }
            composable("morse/learn") { LearnScreen(onBack = { nav.popBackStack() }) }
            composable("morse/copy") { CopyScreen(onBack = { nav.popBackStack() }) }
            composable("morse/send") { SendScreen(onBack = { nav.popBackStack() }) }
            composable("morse/alphabet") { AlphabetScreen(onBack = { nav.popBackStack() }) }
            composable(Tab.EXAM.route) { ExamScreen() }
            composable(Tab.PROGRESS.route) { ProgressScreen() }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
        }
    }

    CelebrationHost()
    }

    // Debug shortcut: jump to a route once the graph exists.
    if (startRoute != Tab.HOME.route && (startRoute in Routes.all || startRoute.startsWith("topic/"))) {
        androidx.compose.runtime.LaunchedEffect(startRoute) {
            Tab.entries.firstOrNull { it.route == startRoute }?.let { goTab(it) } ?: nav.navigate(startRoute)
        }
    }
}
