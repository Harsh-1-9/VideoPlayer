package com.example.videoplayer.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.videoplayer.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun SplashScreen(navController: NavHostController) {

    /* ── Navigate away after 3 s ── */
    LaunchedEffect(Unit) {
        delay(3_200)
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
            launchSingleTop = true
        }
    }

    /* ── Master alpha (fade-in everything) ── */
    val masterAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        masterAlpha.animateTo(1f, animationSpec = tween(900, easing = EaseOut))
    }

    /* ── Logo scale spring ── */
    val logoScale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        logoScale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMedium
            )
        )
    }

    /* ── Tagline alpha (delayed) ── */
    val tagAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(800)
        tagAlpha.animateTo(1f, animationSpec = tween(700))
    }

    /* ── Divider width expansion ── */
    val dividerScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(600)
        dividerScale.animateTo(1f, animationSpec = tween(800, easing = EaseOutCubic))
    }

    /* ── Pulse ring (infinite) ── */
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.20f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0.1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    /* ── Rotating outer ring ── */
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "ringRotation"
    )

    /* ── Background shimmer ── */
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )

    /* ── Bottom bar progress ── */
    val progressWidth = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(400)
        progressWidth.animateTo(1f, animationSpec = tween(2400, easing = EaseInOut))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0E0B00),
                        Color(0xFF050500),
                        LuxuryBlack
                    ),
                    radius = 1200f
                )
            )
            .alpha(masterAlpha.value),
        contentAlignment = Alignment.Center
    ) {

        /* ── Background subtle shimmer canvas ── */
        Canvas(Modifier.fillMaxSize()) {
            drawShimmerBackground(shimmerOffset)
        }

        /* ── Glow aura behind logo ── */
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(pulseScale)
                .blur(40.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            GoldPrimary.copy(alpha = pulseAlpha * 0.6f),
                            GoldPrimary.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        /* ── Rotating dashed outer ring ── */
        Canvas(Modifier.size(220.dp)) {
            drawRotatingRing(ringRotation)
        }

        /* ── Static inner ring ── */
        Box(
            modifier = Modifier
                .size(170.dp)
                .background(Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            GoldPrimary.copy(alpha = 0.6f),
                            GoldLight.copy(alpha = 0.3f),
                            Color.Transparent,
                            GoldDark.copy(alpha = 0.4f),
                            GoldPrimary.copy(alpha = 0.6f)
                        )
                    ),
                    radius = size.minDimension / 2f - 1.dp.toPx(),
                    style  = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        /* ── Play icon (vector drawn) ── */
        Box(
            modifier = Modifier.size(80.dp).scale(logoScale.value),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Outer gold circle
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(GoldLight, GoldPrimary, GoldDark)
                    ),
                    radius = size.minDimension / 2f,
                    style  = Stroke(width = 3.dp.toPx())
                )
                // Inner semi-transparent fill
                drawCircle(
                    color  = GoldPrimary.copy(alpha = 0.12f),
                    radius = size.minDimension / 2f - 2.dp.toPx()
                )
                // Play triangle
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r  = size.minDimension * 0.28f
                val path = Path().apply {
                    moveTo(cx - r * 0.5f, cy - r)
                    lineTo(cx + r,        cy)
                    lineTo(cx - r * 0.5f, cy + r)
                    close()
                }
                drawPath(
                    path  = path,
                    brush = Brush.linearGradient(
                        listOf(GoldLight, GoldPrimary),
                        start = Offset(cx - r, cy - r),
                        end   = Offset(cx + r, cy + r)
                    )
                )
            }
        }

        /* ── Text content ── */
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 200.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LUXE PLAYER",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            /* ── Gold horizontal divider ── */
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f * dividerScale.value)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, GoldPrimary, Color.Transparent)
                        )
                    )
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "YOUR MEDIA. YOUR MOMENT.",
                style = MaterialTheme.typography.labelMedium,
                color = PlatinumMid,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(tagAlpha.value)
            )
        }

        /* ── Bottom progress bar ── */
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CRAFTED FOR YOUR VIEWING PLEASURE",
                style = MaterialTheme.typography.labelSmall,
                color = PlatinumDark,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            // Progress track
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(2.dp)
                    .background(GoldSubtle, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressWidth.value)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(GoldDark, GoldPrimary, GoldLight)),
                            CircleShape
                        )
                )
            }
        }
    }
}

/* ── Canvas helpers ── */

private fun DrawScope.drawShimmerBackground(offset: Float) {
    val w = size.width
    val h = size.height
    // Subtle diagonal shimmer lines
    for (i in 0..8) {
        val x = w * (i / 8f) + offset * w * 0.3f
        drawLine(
            brush = Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    GoldPrimary.copy(alpha = 0.025f),
                    Color.Transparent
                )
            ),
            start       = Offset(x, 0f),
            end         = Offset(x + h * 0.4f, h),
            strokeWidth = 60f
        )
    }
}

private fun DrawScope.drawRotatingRing(rotation: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r  = size.minDimension / 2f - 4.dp.toPx()
    val count = 24
    for (i in 0 until count) {
        val angle = Math.toRadians((i * 360.0 / count) + rotation)
        val dotRadius = if (i % 6 == 0) 3.dp.toPx() else 1.5.dp.toPx()
        val alpha = if (i % 6 == 0) 0.9f else 0.3f
        drawCircle(
            color  = GoldPrimary.copy(alpha = alpha),
            radius = dotRadius,
            center = Offset(
                cx + r * cos(angle).toFloat(),
                cy + r * sin(angle).toFloat()
            )
        )
    }
}
