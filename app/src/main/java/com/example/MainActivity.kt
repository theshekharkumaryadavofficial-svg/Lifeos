package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AiTutorRepository
import com.example.data.LifeDatabase
import com.example.data.LifeRepository
import com.example.firebase.configureAppCheck
import com.example.ui.components.BottomNavDestination
import com.example.ui.components.LifeBottomBar
import com.example.ui.screens.AiTutorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HubSection
import com.example.ui.screens.LifeHubScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.QuizTimeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashAndAuthScreen
import com.example.ui.screens.StudyScreen
import com.example.ui.screens.StudyTimerScreen
import com.example.ui.theme.LifeOSTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.viewmodel.LifeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check for Firebase AI Logic (AI Tutor & Quiz)
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            LifeOSTheme {
                LifeOSRoot()
            }
        }
    }
}

private enum class StudyToolOverlay {
    NONE, AI_TUTOR, QUIZ, NOTES, TIMER
}

@Composable
private fun LifeOSRoot() {
    var splashDismissed by rememberSaveable { mutableStateOf(false) }

    if (!splashDismissed) {
        SplashAndAuthScreen(
            onSplashComplete = { splashDismissed = true }
        )
    } else {
        LifeOSApp()
    }
}

@Composable
private fun LifeOSApp() {
    val viewModel: LifeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val db = LifeDatabase.getInstance(app)
                LifeViewModel(
                    repository = LifeRepository(db.lifeDao()),
                    aiRepository = AiTutorRepository()
                )
            }
        }
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    var currentTab by rememberSaveable { mutableStateOf("home") }
    var hubSection by rememberSaveable { mutableStateOf(HubSection.TASKS) }
    var activeOverlay by rememberSaveable { mutableStateOf(StudyToolOverlay.NONE) }

    val destinations = remember {
        listOf(
            BottomNavDestination("home", "Home", Icons.Outlined.Home, Icons.Rounded.Home),
            BottomNavDestination("study", "Study", Icons.Outlined.MenuBook, Icons.Rounded.MenuBook),
            BottomNavDestination("progress", "Progress", Icons.Outlined.BarChart, Icons.Rounded.BarChart),
            BottomNavDestination("hub", "Hub", Icons.Outlined.WorkOutline, Icons.Rounded.Work),
            BottomNavDestination("settings", "Settings", Icons.Outlined.Settings, Icons.Rounded.Settings)
        )
    }

    if (activeOverlay != StudyToolOverlay.NONE) {
        when (activeOverlay) {
            StudyToolOverlay.AI_TUTOR -> AiTutorScreen(
                viewModel = viewModel,
                onBack = { activeOverlay = StudyToolOverlay.NONE }
            )
            StudyToolOverlay.QUIZ -> QuizTimeScreen(
                viewModel = viewModel,
                onBack = { activeOverlay = StudyToolOverlay.NONE }
            )
            StudyToolOverlay.NOTES -> NotesScreen(
                viewModel = viewModel,
                onBack = { activeOverlay = StudyToolOverlay.NONE }
            )
            StudyToolOverlay.TIMER -> StudyTimerScreen(
                viewModel = viewModel,
                onBack = { activeOverlay = StudyToolOverlay.NONE }
            )
            StudyToolOverlay.NONE -> Unit
        }
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            LifeBottomBar(
                destinations = destinations,
                currentRoute = currentTab,
                onSelect = { currentTab = it }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                fadeIn(tween(240)) togetherWith fadeOut(tween(180))
            },
            label = "main_tab_transition"
        ) { tab ->
            when (tab) {
                "home" -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToStudy = { currentTab = "study" },
                    onNavigateToNaamJap = {
                        hubSection = HubSection.NAAM_JAP
                        currentTab = "hub"
                    },
                    onNavigateToFitness = {
                        hubSection = HubSection.FITNESS
                        currentTab = "hub"
                    },
                    onNavigateToAiTutor = { activeOverlay = StudyToolOverlay.AI_TUTOR },
                    onNavigateToTasks = {
                        hubSection = HubSection.TASKS
                        currentTab = "hub"
                    },
                    onNavigateToTimer = { activeOverlay = StudyToolOverlay.TIMER },
                    contentPadding = innerPadding
                )

                "study" -> StudyScreen(
                    viewModel = viewModel,
                    onOpenAiTutor = { activeOverlay = StudyToolOverlay.AI_TUTOR },
                    onOpenQuiz = { activeOverlay = StudyToolOverlay.QUIZ },
                    onOpenNotes = { activeOverlay = StudyToolOverlay.NOTES },
                    onOpenTimer = { activeOverlay = StudyToolOverlay.TIMER },
                    contentPadding = innerPadding
                )

                "progress" -> ProgressScreen(
                    viewModel = viewModel,
                    onOpenStudy = { currentTab = "study" },
                    contentPadding = innerPadding
                )

                "hub" -> LifeHubScreen(
                    viewModel = viewModel,
                    selectedSection = hubSection,
                    onSelectSection = { hubSection = it },
                    contentPadding = innerPadding
                )

                "settings" -> SettingsScreen(
                    viewModel = viewModel,
                    contentPadding = innerPadding
                )
            }
        }
    }
}
