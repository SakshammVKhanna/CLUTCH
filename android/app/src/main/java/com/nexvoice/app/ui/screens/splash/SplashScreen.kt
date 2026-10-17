package com.nexvoice.app.ui.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexvoice.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * SplashScreen.kt (Phase 3 — session verify update)
 *
 * App ka pehla screen. Yahan:
 *  1. Logo ka fade + scale animation chalta hai
 *  2. Saath hi SplashViewModel token ko server pe verify karta hai
 *     (loading spinner dikhta hai — user ko pata chale kuch ho raha hai)
 *  3. Faisla hone pe:
 *       valid token   → Home (auto-login)
 *       invalid token → Login page
 *       server down   → Retry button (logout nahi karte)
 *
 * Navigation jaan-boojh kar do cheezon ka intezaar karta hai:
 *   (a) session ka faisla ho jaye, aur
 *   (b) animation kam se kam MIN_SPLASH_MS dikh chuki ho
 * — warna fast network pe splash ek jhatke me gayab ho jaata hai.
 */
private const val MIN_SPLASH_MS = 1200L

@Composable
fun SplashScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = viewModel()
) {
    var visible by remember { mutableStateOf(false) }
    var minTimeElapsed by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "splash_fade"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = tween(durationMillis = 800),
        label = "splash_scale"
    )

    val destination by viewModel.destination.collectAsState()

    LaunchedEffect(Unit) {
        visible = true
        delay(MIN_SPLASH_MS)
        minTimeElapsed = true
    }

    // Faisla + animation, dono ho jayein tab navigate karo.
    // NetworkError pe kuch nahi — usi screen pe Retry dikhate hain.
    LaunchedEffect(destination, minTimeElapsed) {
        if (!minTimeElapsed) return@LaunchedEffect
        when (destination) {
            SplashDestination.GoToHome -> onNavigateToHome()
            SplashDestination.GoToAuth -> onNavigateToAuth()
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NexBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .alpha(alpha)
                .scale(scale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(NexGold.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Headset,
                    contentDescription = "NexVoice Logo",
                    tint = NexGold,
                    modifier = Modifier.size(56.dp)
                )
            }

            Text(
                text = "CLUTCH",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NexGold,
                letterSpacing = 3.sp
            )

            Text(
                text = "Clutch the win. Own the voice.",
                fontSize = 15.sp,
                color = NexTextSecondary,
                letterSpacing = 1.sp
            )

            // ── Session check ka status ────────────────────────────────────
            if (destination == SplashDestination.NetworkError) {
                SplashNetworkError(onRetry = { viewModel.decide() })
            } else {
                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = NexGold,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Signing you in…",
                    fontSize = 12.sp,
                    color = NexTextHint,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * Server tak pahunch hi nahi paye (backend band / internet nahi).
 * User ka token galat nahi hai — isliye logout nahi karte, sirf retry dete hain.
 */
@Composable
private fun SplashNetworkError(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 320.dp)
            .background(NexSurface, RoundedCornerShape(16.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = NexMuted,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "Can't reach the server",
                color = NexTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Could not verify your session. Check that the backend is running, then try again.",
                color = NexTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = NexGold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = NexBg)
                Spacer(Modifier.width(8.dp))
                Text("Try Again", color = NexBg, fontWeight = FontWeight.Bold)
            }
        }
    }
}
