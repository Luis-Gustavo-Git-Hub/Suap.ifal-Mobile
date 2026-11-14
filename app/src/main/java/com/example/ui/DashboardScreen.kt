package com.example.ui

import com.example.ui.components.*
import com.example.ui.theme.Spacing
import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    studentName: String,
    campusName: String,
    globalFrequency: String,
    globalAbsences: String,
    totalClasses: String,
    passedClasses: String,
    subjectsCount: Int,
    lastSyncTime: String,
    hasSigaaCredentials: Boolean,
    hasSuapCredentials: Boolean,
    onNavigateToBoletim: () -> Unit,
    onNavigateToAssuntos: () -> Unit,
    onNavigateToAlmoco: () -> Unit,
    onNavigateToProfile: (Boolean) -> Unit = {},
    onNavigateToHorarios: () -> Unit = {},
    onSyncClick: () -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val isSynced = subjectsCount > 0 && android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br") != null

    val state = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.ScreenPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.normal)
        ) {
            ScreenHeader("Olá, $studentName", campusName) {
                FilledTonalIconButton(onClick = onNavigateToHorarios, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.DateRange, contentDescription = "Horário das aulas")
                }
            }
            if (lastSyncTime.isNotEmpty()) {
                Text("Última atualização: $lastSyncTime", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

        // Bento Grid Layout
        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.spacedBy(Spacing.normal)
        ) {
            // Boletim Card (Hero style)
            BoletimCard(
                modifier = Modifier.fillMaxWidth(),
                subjectsCount = subjectsCount,
                hasSuapCredentials = hasSuapCredentials,
                onClick = onNavigateToBoletim,
                onNavigateToProfile = { onNavigateToProfile(false) }
            )
            
            // Frequencia Card (Full width now)
            FrequenciaCard(
                modifier = Modifier.fillMaxWidth(),
                globalFrequency = globalFrequency,
                globalAbsences = globalAbsences,
                totalClasses = totalClasses,
                passedClasses = passedClasses,
                hasSuapCredentials = hasSuapCredentials,
                onNavigateToProfile = { onNavigateToProfile(false) }
            )

            // Documentos Card
            AssuntosCard(modifier = Modifier.fillMaxWidth(), hasSuapCredentials = hasSuapCredentials, onClick = onNavigateToAssuntos, onNavigateToProfile = { onNavigateToProfile(false) })
            
            // Almoco Card
            MarcarAlmocoCard(modifier = Modifier.fillMaxWidth(), hasSigaaCredentials = hasSigaaCredentials, onClick = onNavigateToAlmoco, onNavigateToProfile = onNavigateToProfile)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
    }
}

}
@Composable
fun BoletimCard(modifier: Modifier = Modifier, subjectsCount: Int, hasSuapCredentials: Boolean, onClick: () -> Unit, onNavigateToProfile: () -> Unit) {
    val isSynced = subjectsCount > 0 && android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br") != null
    val containerColor = if (hasSuapCredentials) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    val gradientColors = if (hasSuapCredentials) {
        listOf(containerColor, containerColor)
    } else {
        listOf(containerColor, containerColor)
    }
    
    AppCard(
        modifier = modifier.clickable { if (hasSuapCredentials) onClick() else onNavigateToProfile() },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(Spacing.ScreenPadding).fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(com.example.ui.theme.readableOn(containerColor).copy(alpha = 0.2f), MaterialTheme.shapes.medium)
                            .padding(Spacing.medium)
                    ) {
                        Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = com.example.ui.theme.readableOn(containerColor))
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BlinkingSyncDot(isSynced = isSynced)
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                                .padding(horizontal = Spacing.medium, vertical = Spacing.xs)
                        ) {
                            Text(
                                text = if (hasSuapCredentials) "Ver boletim" else "Conectar SUAP",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                Column {
                    Text(
                        text = "Meu Boletim",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.readableOn(containerColor)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hasSuapCredentials) "$subjectsCount disciplinas cadastradas" else "Conta desconectada. Toque para conectar.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = com.example.ui.theme.readableOn(containerColor)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    

                }
            }
        }
    }
}

@Composable
fun FrequenciaCard(
    modifier: Modifier = Modifier,
    globalFrequency: String,
    globalAbsences: String,
    totalClasses: String,
    passedClasses: String,
    hasSuapCredentials: Boolean = true,
    onNavigateToProfile: () -> Unit = {}
) {
    val containerColor = if (hasSuapCredentials) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
    val onContainerColor = if (hasSuapCredentials) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    
    AppCard(
        modifier = modifier.clickable { if (!hasSuapCredentials) onNavigateToProfile() },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.ScreenPadding).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Column {
                Text(
                    text = "Frequência Global",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onContainerColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.normal)) {
                Column {
                        Text(
                            text = if (globalFrequency.endsWith("%") || globalFrequency == "-") globalFrequency else "$globalFrequency%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Presença",
                            style = MaterialTheme.typography.labelMedium,
                            color = onContainerColor.copy(alpha = 0.7f)
                        )
                    }
                Column {
                        Text(
                            text = globalAbsences.ifEmpty { "0" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Faltas",
                            style = MaterialTheme.typography.labelMedium,
                            color = onContainerColor.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.normal)) {
                    Column {
                        Text(
                            text = totalClasses.ifEmpty { "-" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = onContainerColor
                        )
                        Text(
                            text = "Carga Horária",
                            style = MaterialTheme.typography.labelSmall,
                            color = onContainerColor.copy(alpha = 0.7f)
                        )
                    }
                    Column {
                        Text(
                            text = passedClasses.ifEmpty { "-" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = onContainerColor
                        )
                        Text(
                            text = "Aulas Passadas",
                            style = MaterialTheme.typography.labelSmall,
                            color = onContainerColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.medium)
                    .padding(Spacing.normal)
            ) {
                Icon(Icons.AutoMirrored.Rounded.FactCheck, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun AssuntosCard(modifier: Modifier = Modifier, hasSuapCredentials: Boolean, onClick: () -> Unit, onNavigateToProfile: () -> Unit) {
    val containerColor = if (hasSuapCredentials) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceContainerHighest
    val gradientColors = if (hasSuapCredentials) {
        listOf(containerColor, containerColor)
    } else {
        listOf(containerColor, containerColor)
    }

    AppCard(
        modifier = modifier.clickable { if (hasSuapCredentials) onClick() else onNavigateToProfile() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = gradientColors
                    )
                )
        ) {
            Row(
                modifier = Modifier.padding(Spacing.normal).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.normal),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .background(com.example.ui.theme.readableOn(containerColor).copy(alpha = 0.2f), MaterialTheme.shapes.medium)
                            .padding(Spacing.medium)
                    ) {
                        Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = com.example.ui.theme.readableOn(containerColor))
                    }
                    Column {
                        Text(
                            text = "Disciplinas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.readableOn(containerColor)
                        )
                        Text(
                            text = if (hasSuapCredentials) "Tópicos das disciplinas" else "Conecte sua conta para ver",
                            style = MaterialTheme.typography.bodySmall,
                            color = com.example.ui.theme.readableOn(containerColor)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                        .padding(horizontal = Spacing.medium, vertical = Spacing.xs)
                ) {
                    Text(
                        text = if (hasSuapCredentials) "ACESSAR" else "CONFIGURAR",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
fun MarcarAlmocoCard(modifier: Modifier = Modifier, hasSigaaCredentials: Boolean, onClick: () -> Unit, onNavigateToProfile: (Boolean) -> Unit) {
    val containerColor = if (hasSigaaCredentials) androidx.compose.material3.MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHighest
    val gradientColors = if (hasSigaaCredentials) {
        listOf(containerColor, containerColor)
    } else {
        listOf(containerColor, containerColor)
    }

    AppCard(
        modifier = modifier.clickable { 
            if (hasSigaaCredentials) onClick() else onNavigateToProfile(true)
        },
        shape = MaterialTheme.shapes.large,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = containerColor),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = gradientColors
                    )
                )
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(Spacing.normal).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.normal),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(com.example.ui.theme.readableOn(containerColor).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.Restaurant,
                            contentDescription = null,
                            tint = com.example.ui.theme.readableOn(containerColor)
                        )
                    }
                    Column {
                        androidx.compose.material3.Text(
                            text = "Refeitório (SIGAA)",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = com.example.ui.theme.readableOn(containerColor)
                        )
                        androidx.compose.material3.Text(
                            text = if (hasSigaaCredentials) "Marque seus dias de almoço" else "Configure as credenciais no perfil",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = com.example.ui.theme.readableOn(containerColor)
                        )
                    }
                }
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = com.example.ui.theme.readableOn(containerColor),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun HorariosCard(modifier: Modifier = Modifier, hasSuapCredentials: Boolean = true, onClick: () -> Unit = {}, onNavigateToProfile: () -> Unit = {}) {
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
    
    AppCard(
        modifier = modifier.clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.ScreenPadding).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Horários",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onContainerColor
                )
                Text(
                    text = "Veja o horário de todas as suas aulas",
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainerColor.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.Rounded.DateRange,
                contentDescription = null,
                tint = onContainerColor,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
