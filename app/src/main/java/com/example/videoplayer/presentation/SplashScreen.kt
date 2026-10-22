
package com.example.videoplayer.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.videoplayer.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController) {

    val transition = rememberInfiniteTransition(label = "splash")

    val glowScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1600,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val logoScale = remember { Animatable(0.55f) }
    val contentAlpha = remember { Animatable(0f) }
    val logoRotation = remember { Animatable(-8f) }

    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 850,
                easing = FastOutSlowInEasing
            )
        )
    }

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    LaunchedEffect(Unit) {
        logoRotation.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 850,
                easing = FastOutSlowInEasing
            )
        )

        delay(2200)

        navController.navigate("home") {
            popUpTo("splash") {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF080B18),
                        Color(0xFF11172B),
                        Color(0xFF080B18)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // Soft ambient glow behind the logo
        Box(
            modifier = Modifier
                .size(250.dp)
                .scale(glowScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x553F7CFF),
                            Color(0x223F7CFF),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Thin decorative ring
        Box(
            modifier = Modifier
                .size(190.dp)
                .border(
                    width = 1.dp,
                    color = Color(0x334F8CFF),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(contentAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Image(
                painter = painterResource(
                    id = R.drawable.videoplayer
                ),
                contentDescription = "Video Player logo",
                modifier = Modifier
                    .size(132.dp)
                    .scale(logoScale.value)
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "VIDEO PLAYER",
                fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "YOUR MEDIA. YOUR MOMENT.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color = Color(0xFF9CAFD4),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Minimal animated loading indicator
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 3.dp)
                    .background(
                        color = Color(0x334F8CFF),
                        shape = CircleShape
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF5C8DFF),
                                    Color(0xFFB9CFFF)
                                )
                            ),
                            shape = CircleShape
                        )
                        .scale(
                            scaleX = 0.65f,
                            scaleY = 1f
                        )
                )
            }
        }

        Text(
            text = "CRAFTED FOR YOUR VIEWING EXPERIENCE",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .alpha(contentAlpha.value)
                .then(Modifier),
            fontSize = 9.sp,
            letterSpacing = 1.5.sp,
            color = Color(0xFF64718D),
            textAlign = TextAlign.Center
        )
    }
}
