package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BlinkingSyncDot(isSynced: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "blinkTransition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )
    val color = if (isSynced) Color(0xFF4CAF50) else Color.Red
    
    Box(
        modifier = modifier
            .size(10.dp)
            .background(color.copy(alpha = alpha), CircleShape)
    )
}
