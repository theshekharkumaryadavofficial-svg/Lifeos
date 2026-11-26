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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AiTutorRepository
import com.example.data.LifeRepository
import com.example.firebase.configureAppCheck
import com.example.firebase.performSignOut
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
import com.example.ui.screens.openSafeHttpsDownloadUrl
import com.example.ui.theme.BrightGold
import com.example.ui.theme.LifeOSTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextOnDark
import com.example.ui.theme.TextOnDarkMuted
import com.example.ui.viewmodel.LifeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent BEFORE UI or ViewModels
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
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var splashDismissed by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null || !splashDismissed) {
        SplashAndAuthScreen(
            isAuthenticated = user != null,
            onSplashComplete = { splashDismissed = true },
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
                splashDismissed = true
            }
        )
    } else {
        key(user.uid) {
            AuthenticatedLifeOSApp(
                currentUserId = user.uid,
                googleDisplayName = user.displayName,
                userEmail = user.email,
                photoUrl = user.photoUrl?.toString()
            )
        }
    }
}

@Composable
private fun AuthenticatedLifeOSApp(
    currentUserId: String,
    googleDisplayName: String?,
    userEmail: String?,
    photoUrl: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: LifeViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                LifeViewModel(
                    repository = LifeRepository(db),
                    aiRepository = AiTutorRepository(),
                    currentUserId = currentUserId,
                    googleDisplayName = googleDisplayName
                )
            }
        }
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val appUpdateConfig by viewModel.appUpdateConfig.collectAsStateWithLifecycle()
    val updatePromptDismissed by viewModel.updatePromptDismissed.collectAsStateWithLifecycle()

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
                    userEmail = userEmail,
                    photoUrl = photoUrl,
                    onLogout = {
                        performSignOut(
                            credentialManager = credentialManager,
                            scope = scope
                        )
                    },
                    contentPadding = innerPadding
                )
            }
        }
    }

    val pendingUpdate = appUpdateConfig
    if (pendingUpdate != null && viewModel.hasNewerUpdate(pendingUpdate) && !updatePromptDismissed) {
        AlertDialog(
            onDismissRequest = {
                if (!pendingUpdate.forceUpdate) {
                    viewModel.dismissUpdatePrompt()
                }
            },
            containerColor = ComposeColor(0xFF1A1829),
            titleContentColor = TextOnDark,
            textContentColor = TextOnDarkMuted,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.SystemUpdate,
                        contentDescription = "Update Available",
                        tint = BrightGold,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (pendingUpdate.forceUpdate) {
                            "Important Update v${pendingUpdate.latestVersionName}"
                        } else {
                            "LifeOS v${pendingUpdate.latestVersionName} Available"
                        },
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = pendingUpdate.releaseNotes.ifBlank {
                            "A newer version of LifeOS is ready with fresh improvements."
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextOnDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            },
            confirmButton = {
                if (pendingUpdate.downloadUrl.isNotBlank()) {
                    TextButton(
                        onClick = {
                            openSafeHttpsDownloadUrl(context, pendingUpdate.downloadUrl)
                            if (!pendingUpdate.forceUpdate) {
                                viewModel.dismissUpdatePrompt()
                            }
                        },
                        modifier = Modifier.testTag("auto_update_download_button")
                    ) {
                        Text("Update Now", color = BrightGold, fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    TextButton(onClick = { viewModel.dismissUpdatePrompt() }) {
                        Text("OK", color = BrightGold, fontWeight = FontWeight.ExtraBold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissUpdatePrompt() },
                    modifier = Modifier.testTag("auto_update_later_button")
                ) {
                    Text(
                        text = if (pendingUpdate.forceUpdate) "Close" else "Later",
                        color = TextOnDarkMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}
