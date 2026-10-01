package com.multidrive.app.presentation.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SnapchatSplashScreen(
    onSplashFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    // Pulsing blue flash scale animation like Snapchat
    val scaleAnim = rememberInfiniteTransition(label = "pulseScale")
    val pulseScale by scaleAnim.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScaleValue"
    )

    // Flash background color shift animation
    val flashAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "flashAlpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1600) // Show glowing blue flash for 1.6s on startup
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF3D5AFE), // Glowing Snapchat Electric Blue
                        Color(0xFF1A56C4),
                        Color(0xFF0D1B2A)  // Dark backdrop
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glowing Blue Aura Ring
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(pulseScale)
                .background(
                    color = Color(0xFF4DD0E1).copy(alpha = 0.35f * flashAlpha),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .scale(pulseScale),
                tint = Color.White
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "MultiDrive",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Unified Storage",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
                letterSpacing = 1.sp
            )
        }
    }
}
