package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
                Column {
                Text(
                    text = campusName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Olá, $studentName",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (lastSyncTime.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Última vez",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = lastSyncTime,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                FilledTonalIconButton(
                    onClick = onNavigateToHorarios,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Horário das Aulas",
                        modifier = Modifier.size(18.dp)
                    )
                }

            }
        }

        // Bento Grid Layout
        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
            Spacer(modifier = Modifier.height(100.dp))
        }
        
    }
}

}
@Composable
fun BoletimCard(modifier: Modifier = Modifier, subjectsCount: Int, hasSuapCredentials: Boolean, onClick: () -> Unit, onNavigateToProfile: () -> Unit) {
    val isSynced = subjectsCount > 0 && android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br") != null
    val containerColor = if (hasSuapCredentials) MaterialTheme.colorScheme.primary else Color.Gray
    val gradientColors = if (hasSuapCredentials) {
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.inversePrimary)
    } else {
        listOf(Color.Gray, Color.DarkGray)
    }
    
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BlinkingSyncDot(isSynced = isSynced)
                        Box(
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "ACESSAR",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
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
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$subjectsCount disciplinas cadastradas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Progress bar placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color.White.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .fillMaxHeight()
                                .background(Color.White, CircleShape)
                        )
                    }
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
    val containerColor = if (hasSuapCredentials) MaterialTheme.colorScheme.secondaryContainer else Color.LightGray
    val onContainerColor = if (hasSuapCredentials) MaterialTheme.colorScheme.onSecondaryContainer else Color.DarkGray
    
    Card(
        modifier = modifier.clickable { if (!hasSuapCredentials) onNavigateToProfile() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
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
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun AssuntosCard(modifier: Modifier = Modifier, hasSuapCredentials: Boolean, onClick: () -> Unit, onNavigateToProfile: () -> Unit) {
    val containerColor = if (hasSuapCredentials) MaterialTheme.colorScheme.tertiary else Color(0xFF9E9E9E)
    val gradientColors = if (hasSuapCredentials) {
        listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f))
    } else {
        listOf(Color(0xFF9E9E9E), Color(0xFF757575))
    }

    Card(
        modifier = modifier.clickable { if (hasSuapCredentials) onClick() else onNavigateToProfile() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White)
                    }
                    Column {
                        Text(
                            text = "Assuntos das matérias",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (hasSuapCredentials) "Tópicos das disciplinas" else "Conecte sua conta para ver",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (hasSuapCredentials) "ACESSAR" else "CONFIGURAR",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasSuapCredentials) MaterialTheme.colorScheme.tertiary else Color.DarkGray,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
fun MarcarAlmocoCard(modifier: Modifier = Modifier, hasSigaaCredentials: Boolean, onClick: () -> Unit, onNavigateToProfile: (Boolean) -> Unit) {
    val containerColor = if (hasSigaaCredentials) androidx.compose.material3.MaterialTheme.colorScheme.secondary else androidx.compose.ui.graphics.Color.Gray
    val gradientColors = if (hasSigaaCredentials) {
        listOf(
            androidx.compose.material3.MaterialTheme.colorScheme.secondary,
            androidx.compose.material3.MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
        )
    } else {
        listOf(androidx.compose.ui.graphics.Color.Gray, androidx.compose.ui.graphics.Color.DarkGray)
    }

    androidx.compose.material3.Card(
        modifier = modifier.clickable { 
            if (hasSigaaCredentials) onClick() else onNavigateToProfile(true)
        },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = containerColor),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                    Column {
                        androidx.compose.material3.Text(
                            text = "Refeitório (SIGAA)",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                        androidx.compose.material3.Text(
                            text = if (hasSigaaCredentials) "Marque seus dias de almoço" else "Configure as credenciais no perfil",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
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
    
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
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
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = onContainerColor,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
