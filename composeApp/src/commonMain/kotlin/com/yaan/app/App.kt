package com.yaan.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import com.yaan.app.navigation.YaanDestination
import com.yaan.app.ui.components.YaanBottomNavigation
import com.yaan.app.ui.components.YaanTopBar
import com.yaan.app.ui.screens.AskYaanScreen
import com.yaan.app.ui.state.AskYaanUiState
import com.yaan.app.ui.screens.BrandSplashScreen
import com.yaan.app.ui.screens.HomeScreen
import com.yaan.app.ui.screens.LiveVoiceModal
import com.yaan.app.ui.screens.NotesScreen
import com.yaan.app.ui.screens.OnboardingScreen
import com.yaan.app.ui.screens.PlanScreen
import com.yaan.app.ui.screens.VaultLockScreen
import com.yaan.app.ui.screens.YouScreen
import com.yaan.app.ui.theme.YaanTheme
import com.yaan.core.domain.repository.BriefingRepository
import com.yaan.core.domain.repository.MemoryRepository
import com.yaan.core.domain.repository.NoteRepository
import com.yaan.core.domain.repository.ReminderRepository
import com.yaan.core.domain.repository.TaskRepository
import com.yaan.app.desktop.DesktopKeyboardShortcutHandler
import com.yaan.app.desktop.KeyboardShortcutAction
import androidx.compose.ui.input.key.onPreviewKeyEvent
import com.yaan.core.domain.repository.UserSessionRepository
import com.yaan.core.domain.usecase.CaptureIntentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.currentKoinScope

@Composable
fun YaanApp(
    taskRepository: TaskRepository? = null,
    noteRepository: NoteRepository? = null,
    reminderRepository: ReminderRepository? = null,
    briefingRepository: BriefingRepository? = null,
    memoryRepository: MemoryRepository? = null,
    captureUseCase: CaptureIntentUseCase? = null,
    sessionRepository: UserSessionRepository? = null,
    onRegisterShortcutHandler: (((KeyboardShortcutAction) -> Unit) -> Unit)? = null,
) {
    val koin = currentKoinScope()
    val resolvedTaskRepo = taskRepository ?: koin.getOrNull<TaskRepository>()
    val resolvedNoteRepo = noteRepository ?: koin.getOrNull<NoteRepository>()
    val resolvedReminderRepo = reminderRepository ?: koin.getOrNull<ReminderRepository>()
    val resolvedBriefingRepo = briefingRepository ?: koin.getOrNull<BriefingRepository>()
    val resolvedMemoryRepo = memoryRepository ?: koin.getOrNull<MemoryRepository>()
    val resolvedCaptureUseCase = captureUseCase ?: koin.getOrNull<CaptureIntentUseCase>()
    val resolvedSessionRepo = sessionRepository ?: koin.getOrNull<UserSessionRepository>()

    val coroutineScope = rememberCoroutineScope()
    val userProfile by (resolvedSessionRepo?.observeProfile() ?: MutableStateFlow(null)).collectAsState(null)

    var isDarkTheme by remember { mutableStateOf(false) }
    var currentDestination by remember { mutableStateOf(YaanDestination.Home) }
    var isLiveVoiceOpen by remember { mutableStateOf(false) }
    var isBrandScreenVisible by remember { mutableStateOf(false) }
    var isAppInitializing by remember { mutableStateOf(true) }
    var askYaanUiState by remember { mutableStateOf(AskYaanUiState()) }

    LaunchedEffect(Unit) {
        delay(1600)
        isAppInitializing = false
    }

    val handleShortcutAction: (KeyboardShortcutAction) -> Unit = { action ->
        when (action) {
            KeyboardShortcutAction.OPEN_QUICK_CAPTURE -> isLiveVoiceOpen = true
            KeyboardShortcutAction.NAVIGATE_HOME -> currentDestination = YaanDestination.Home
            KeyboardShortcutAction.NAVIGATE_PLAN -> currentDestination = YaanDestination.Plan
            KeyboardShortcutAction.NAVIGATE_NOTES -> currentDestination = YaanDestination.Notes
            KeyboardShortcutAction.NAVIGATE_YOU -> currentDestination = YaanDestination.You
            KeyboardShortcutAction.TOGGLE_THEME -> isDarkTheme = !isDarkTheme
            KeyboardShortcutAction.DISMISS_MODAL -> {
                if (isLiveVoiceOpen) isLiveVoiceOpen = false
                else if (isBrandScreenVisible) isBrandScreenVisible = false
            }
        }
    }

    LaunchedEffect(Unit) {
        onRegisterShortcutHandler?.invoke(handleShortcutAction)
    }

    YaanTheme(darkTheme = isDarkTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .onPreviewKeyEvent { event ->
                    DesktopKeyboardShortcutHandler.handleKeyEvent(event, handleShortcutAction)
                }
        ) {
            Scaffold(
                topBar = {
                    YaanTopBar(
                        title = currentDestination.title,
                        isLoading = isAppInitializing,
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = !isDarkTheme },
                        onMicClick = { isLiveVoiceOpen = true },
                        onProfileClick = { currentDestination = YaanDestination.You },
                        onLogoClick = { isBrandScreenVisible = true }
                    )
                },
                bottomBar = {
                    YaanBottomNavigation(
                        currentDestination = currentDestination,
                        onNavigate = { destination ->
                            currentDestination = destination
                        }
                    )
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AnimatedContent(
                        targetState = currentDestination,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "DestinationTransition"
                    ) { destination ->
                        when (destination) {
                            YaanDestination.Home -> HomeScreen(
                                onNavigateToAskYaan = { currentDestination = YaanDestination.AskYaan },
                                onLaunchVoice = { isLiveVoiceOpen = true },
                                taskRepository = resolvedTaskRepo,
                                briefingRepository = resolvedBriefingRepo,
                                captureUseCase = resolvedCaptureUseCase
                            )
                            YaanDestination.Plan -> PlanScreen(
                                taskRepository = resolvedTaskRepo,
                                reminderRepository = resolvedReminderRepo
                            )
                            YaanDestination.AskYaan -> AskYaanScreen(
                                onLaunchVoice = { isLiveVoiceOpen = true },
                                noteRepository = resolvedNoteRepo,
                                taskRepository = resolvedTaskRepo,
                                memoryRepository = resolvedMemoryRepo,
                                persistedState = askYaanUiState,
                                onStateChange = { askYaanUiState = it }
                            )
                            YaanDestination.Notes -> NotesScreen(
                                onNavigateToAskYaan = { currentDestination = YaanDestination.AskYaan },
                                noteRepository = resolvedNoteRepo
                            )
                            YaanDestination.You -> YouScreen(
                                onToggleDarkTheme = { isDarkTheme = !isDarkTheme },
                                isDarkTheme = isDarkTheme,
                                noteRepository = resolvedNoteRepo,
                                taskRepository = resolvedTaskRepo,
                                sessionRepository = resolvedSessionRepo
                            )
                        }
                    }
                }
            }

            // Live Voice Mode Overlay Modal
            AnimatedVisibility(
                visible = isLiveVoiceOpen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                LiveVoiceModal(
                    onDismiss = { isLiveVoiceOpen = false },
                    noteRepository = resolvedNoteRepo,
                    taskRepository = resolvedTaskRepo,
                    memoryRepository = resolvedMemoryRepo
                )
            }

            // Brand Presentation & Splash Screen Overlay (Centered Logo in Dark & Light Themes)
            AnimatedVisibility(
                visible = isBrandScreenVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                BrandSplashScreen(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                    onEnterApp = { isBrandScreenVisible = false }
                )
            }

            // Onboarding & First-Launch Gate (Critical User Journey 1, F-006 & F-003)
            val isOnboardingCompleted = userProfile?.isOnboardingCompleted ?: false
            AnimatedVisibility(
                visible = !isOnboardingCompleted,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                OnboardingScreen(
                    onComplete = { name, ageConfirmed ->
                        resolvedSessionRepo?.completeOnboarding(name, ageConfirmed)
                            ?: com.yaan.core.common.result.YaanResult.Success(
                                userProfile ?: com.yaan.core.domain.model.UserProfile(
                                    userId = "usr_guest",
                                    displayName = name,
                                    authToken = "local:offline",
                                    isOnboardingCompleted = true,
                                    ageConfirmed18Plus = ageConfirmed
                                )
                            )
                    },
                    onSignIn = { email, password ->
                        resolvedSessionRepo?.signInWithEmail(email, password)
                            ?: com.yaan.core.common.result.YaanResult.Failure(
                                com.yaan.core.common.result.YaanError.UnknownError("Session repository not ready.")
                            )
                    },
                    onSignUp = { email, password, name, ageConfirmed ->
                        resolvedSessionRepo?.signUpWithEmail(email, password, name, ageConfirmed)
                            ?: com.yaan.core.common.result.YaanResult.Failure(
                                com.yaan.core.common.result.YaanError.UnknownError("Session repository not ready.")
                            )
                    },
                    initialDisplayName = userProfile?.displayName ?: "Elena Rostova"
                )
            }

            // Biometric Vault Lock Gate (Zero-Knowledge Invariant)
            val isVaultLocked = userProfile?.let { it.biometricLockEnabled && it.isVaultLocked } ?: false
            AnimatedVisibility(
                visible = isVaultLocked,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                VaultLockScreen(
                    userDisplayName = userProfile?.displayName ?: "User",
                    onUnlock = {
                        coroutineScope.launch {
                            resolvedSessionRepo?.unlockVault()
                        }
                    }
                )
            }
        }
    }
}
