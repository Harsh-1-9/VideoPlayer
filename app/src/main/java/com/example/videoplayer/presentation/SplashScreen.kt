package com.example.videoplayer.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.videoplayer.R
import com.example.videoplayer.ui.theme.AppBackground
import com.example.videoplayer.ui.theme.AppSurfaceElevated
import com.example.videoplayer.ui.theme.PrimaryColor
import com.example.videoplayer.ui.theme.PrimaryDark
import com.example.videoplayer.ui.theme.TextSecondary
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(navController: NavHostController) {

    val textAlpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            // name and bar fade in, bar fills like a video progress bar
            launch { textAlpha.animateTo(1f, tween(500)) }
            launch { progress.animateTo(1f, tween(1400, easing = LinearEasing)) }
        }
        delay(150)
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
            launchSingleTop = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentAlignment = Alignment.Center
    ) {
        // Same blue circle + icon as the Android 12+ system splash,
        // so there is no visible jump between the two.
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(PrimaryDark),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.requiredSize(240.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
                .alpha(textAlpha.value)
        ) {
            Text(
                text = "Video Player",
                color = TextSecondary,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .width(120.dp)
                    .height(3.dp),
                color = PrimaryColor,
                trackColor = AppSurfaceElevated,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
    }
}