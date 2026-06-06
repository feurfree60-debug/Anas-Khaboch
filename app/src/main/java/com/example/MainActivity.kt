package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.JournalScreen
import com.example.ui.MainViewModel
import com.example.ui.RecordScreen
import com.example.ui.StudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.CosmicDeepSpace
import com.example.ui.theme.CosmicMidnightCard
import com.example.ui.theme.CosmicPrimaryPurple
import com.example.ui.theme.CosmicTextPrimary
import com.example.ui.theme.CosmicTextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppLayout()
            }
        }
    }
}

@Composable
fun MainAppLayout() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val dreams by viewModel.allDreams.collectAsState()

    // Screen tracking to show/hide bottom navigation bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf("journal", "loom")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CosmicDeepSpace,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("app_bottom_navigator"),
                    containerColor = CosmicMidnightCard,
                    contentColor = CosmicTextSecondary
                ) {
                    NavigationBarItem(
                        selected = currentRoute == "journal",
                        onClick = {
                            viewModel.lockJournal() // Auto-lock private screen on focus shift
                            navController.navigate("journal") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Book, contentDescription = "Vault") },
                        label = { Text("Vault") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CosmicPrimaryPurple,
                            selectedTextColor = CosmicTextPrimary,
                            indicatorColor = Color(0x33BC80FF)
                        ),
                        modifier = Modifier.testTag("bottom_tab_journal")
                    )

                    NavigationBarItem(
                        selected = currentRoute == "loom",
                        onClick = {
                            navController.navigate("loom") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Loom") },
                        label = { Text("Weave") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CosmicPrimaryPurple,
                            selectedTextColor = CosmicTextPrimary,
                            indicatorColor = Color(0x33BC80FF)
                        ),
                        modifier = Modifier.testTag("bottom_tab_loom")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "loom",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("journal") {
                JournalScreen(
                    viewModel = viewModel,
                    onPlayDream = { dream ->
                        navController.navigate("studio_saved/${dream.id}")
                    }
                )
            }

            composable("loom") {
                RecordScreen(
                    viewModel = viewModel,
                    onNavigateToStudio = {
                        navController.navigate("studio_workshop")
                    }
                )
            }

            composable("studio_workshop") {
                StudioScreen(
                    viewModel = viewModel,
                    onBackToLoom = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "studio_saved/{dreamId}",
                arguments = listOf(navArgument("dreamId") { type = NavType.IntType })
            ) { backStackEntry ->
                val dreamId = backStackEntry.arguments?.getInt("dreamId")
                val dream = dreams.firstOrNull { it.id == dreamId }

                if (dream != null) {
                    val scenesList = remember(dream) {
                        viewModel.getScenesListFromEntity(dream)
                    }

                    StudioScreen(
                        viewModel = viewModel,
                        onBackToLoom = {
                            navController.popBackStack()
                        },
                        isViewingSavedDream = true,
                        savedTitle = dream.title,
                        savedNarrative = dream.storyNarrative,
                        savedInterpretation = dream.interpretation,
                        savedScenes = scenesList,
                        savedMood = dream.mood
                    )
                } else {
                    // Fallback close
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}
