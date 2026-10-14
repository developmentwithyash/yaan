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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yaan.app.navigation.YaanDestination
import com.yaan.app.ui.components.YaanBottomNavigation
import com.yaan.app.ui.components.YaanTopBar
import com.yaan.app.ui.screens.AskYaanScreen
import com.yaan.app.ui.screens.BrandSplashScreen
import com.yaan.app.ui.screens.HomeScreen
import com.yaan.app.ui.screens.LiveVoiceModal
import com.yaan.app.ui.screens.NotesScreen
import com.yaan.app.ui.screens.PlanScreen
import com.yaan.app.ui.screens.YouScreen
import com.yaan.app.ui.theme.YaanTheme

@Composable
fun YaanApp() {
    var isDarkTheme by remember { mutableStateOf(false) }
    var currentDestination by remember { mutableStateOf(YaanDestination.Home) }
    var isLiveVoiceOpen by remember { mutableStateOf(false) }
    var isBrandScreenVisible by remember { mutableStateOf(false) }

    YaanTheme(darkTheme = isDarkTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Scaffold(
                topBar = {
                    YaanTopBar(
                        title = currentDestination.title,
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
                                onLaunchVoice = { isLiveVoiceOpen = true }
                            )
                            YaanDestination.Plan -> PlanScreen()
                            YaanDestination.AskYaan -> AskYaanScreen(
                                onLaunchVoice = { isLiveVoiceOpen = true }
                            )
                            YaanDestination.Notes -> NotesScreen(
                                onNavigateToAskYaan = { currentDestination = YaanDestination.AskYaan }
                            )
                            YaanDestination.You -> YouScreen(
                                onToggleDarkTheme = { isDarkTheme = !isDarkTheme },
                                isDarkTheme = isDarkTheme
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
                    onDismiss = { isLiveVoiceOpen = false }
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
        }
    }
}
