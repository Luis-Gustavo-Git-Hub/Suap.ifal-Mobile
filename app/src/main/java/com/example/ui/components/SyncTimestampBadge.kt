package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncTimestampBadge(
    rawTimestamp: String,
    modifier: Modifier = Modifier
) {
    val relativeText = remember(rawTimestamp) {
        formatRelativeSyncTime(rawTimestamp)
    }

    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.extraSmall
            ),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Última atualização",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = relativeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun formatRelativeSyncTime(rawTime: String): String {
    if (rawTime.isBlank()) return "Não sincronizado"
    return try {
        val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
        val date = sdf.parse(rawTime) ?: return "Atualizado em $rawTime"
        val diffMs = System.currentTimeMillis() - date.time
        if (diffMs < 0) return "Atualizado agora"
        val diffMinutes = diffMs / (1000 * 60)
        val diffHours = diffMinutes / 60
        val diffDays = diffHours / 24

        when {
            diffMinutes < 1 -> "Atualizado agora"
            diffMinutes == 1L -> "Atualizado há 1 min"
            diffMinutes < 60 -> "Atualizado há $diffMinutes min"
            diffHours == 1L -> "Atualizado há 1 h"
            diffHours < 24 -> "Atualizado há $diffHours h"
            diffDays == 1L -> "Atualizado ontem"
            else -> "Atualizado há $diffDays dias"
        }
    } catch (_: Exception) {
        "Atualizado em $rawTime"
    }
}
