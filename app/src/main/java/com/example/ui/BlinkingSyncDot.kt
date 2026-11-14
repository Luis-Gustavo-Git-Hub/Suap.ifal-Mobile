package com.example.ui

import com.example.ui.theme.AppSemanticColors
import androidx.compose.foundation.isSystemInDarkTheme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BlinkingSyncDot(
    isSynced: Boolean, 
    modifier: Modifier = Modifier,
    isSyncing: Boolean = false
) {
    if (isSyncing) {
        val infiniteTransition = rememberInfiniteTransition(label = "syncSpin")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spin"
        )
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = modifier
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sincronizando",
                    modifier = Modifier.size(10.dp).rotate(rotation)
                )
                Text(
                    text = "Sincronizando...",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else if (isSynced) {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = AppSemanticColors.success.copy(alpha = 0.12f),
            contentColor = AppSemanticColors.success,
            modifier = modifier
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Sincronizado",
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = "Sincronizado",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        // Subtle offline dot
        Box(
            modifier = modifier
                .size(7.dp)
                .background(Color.Gray.copy(alpha = 0.4f), CircleShape)
        )
    }
}
