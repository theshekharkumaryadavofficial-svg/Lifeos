package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.onGoogleSignInClicked
import com.example.ui.components.CrimsonGlowButton
import com.example.ui.components.LifeGlassCard
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CrimsonDeep
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextOnDark
import com.example.ui.theme.TextOnDarkMuted
import kotlinx.coroutines.delay

@Composable
fun SplashAndAuthScreen(
    isAuthenticated: Boolean,
    onSplashComplete: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var loaderStarted by remember { mutableStateOf(false) }
    var splashFinished by remember { mutableStateOf(false) }
    var signingIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    val progress by animateFloatAsState(
        targetValue = if (loaderStarted) 1f else 0.08f,
        animationSpec = tween(durationMillis = 1250, easing = FastOutSlowInEasing),
        label = "splash_power_bar"
    )

    LaunchedEffect(Unit) {
        loaderStarted = true
        if (!isAuthenticated) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = onAuthSuccess,
                onUnauthenticated = {},
                scope = scope
            )
        }
        delay(1350L)
        splashFinished = true
        if (isAuthenticated) {
            onSplashComplete()
        }
    }

    LaunchedEffect(isAuthenticated, splashFinished) {
        if (isAuthenticated && splashFinished) {
            onSplashComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_summit_1795675589565),
            contentDescription = "LifeOS mountain summit hero artwork",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ObsidianBg.copy(alpha = 0.25f),
                            ObsidianBg.copy(alpha = 0.35f),
                            ObsidianBg.copy(alpha = 0.88f),
                            ObsidianBg.copy(alpha = 0.98f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Iconic LIFEOS logo block matching Screen 1
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = TextOnDark,
                                fontWeight = FontWeight.ExtraBold,
                                fontStyle = FontStyle.Italic
                            )
                        ) {
                            append("LIFE")
                        }
                        withStyle(
                            SpanStyle(
                                color = CrimsonGlow,
                                fontWeight = FontWeight.ExtraBold,
                                fontStyle = FontStyle.Italic
                            )
                        ) {
                            append("OS")
                        }
                    },
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 54.sp,
                        letterSpacing = (-1.5).sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(ObsidianBg.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Dream  •  Plan  •  Achieve",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = TextOnDark,
                            letterSpacing = 0.6.sp
                        )
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = TextOnDark,
                                fontWeight = FontWeight.ExtraBold,
                                fontStyle = FontStyle.Italic
                            )
                        ) {
                            append("“This Time...\nI Will ")
                        }
                        withStyle(
                            SpanStyle(
                                color = BrightGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontStyle = FontStyle.Italic
                            )
                        ) {
                            append("Win!”")
                        }
                    },
                    style = MaterialTheme.typography.headlineLarge.copy(
                        lineHeight = 36.sp,
                        fontSize = 30.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                if (!splashFinished || isAuthenticated) {
                    // Glowing crimson power bar matching Screen 1
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(CrimsonDeep, CrimsonGlow, BrightGold)
                                    )
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Loading Your Power...",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = TextOnDarkMuted
                        )
                    )
                }

                AnimatedVisibility(
                    visible = splashFinished && !isAuthenticated,
                    enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 }
                ) {
                    LifeGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = BrightGold.copy(alpha = 0.35f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = BrightGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sync Study, Naam Jap & Fitness",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = TextOnDark
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sign in with Google to save your progress, streaks, mala counts, workouts, and AI Tutor chats safely in the cloud.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextOnDarkMuted
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (authError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = authError!!,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = CrimsonGlow,
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        CrimsonGlowButton(
                            text = "Sign in with Google",
                            icon = Icons.Rounded.Login,
                            loading = signingIn,
                            testTagName = "google_sign_in_button",
                            onClick = {
                                signingIn = true
                                authError = null
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        signingIn = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        signingIn = false
                                        authError = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        signingIn = false
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
