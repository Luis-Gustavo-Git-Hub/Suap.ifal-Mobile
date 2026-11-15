package com.example.ui

import androidx.compose.material.icons.outlined.*
import com.example.ui.theme.AppDimens
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    onBack: () -> Unit,
    onViewLogs: () -> Unit = {},
    onActivateDemoMode: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Scaffold(
        topBar = {
            com.example.ui.components.AppTopBar(
                title = { com.example.ui.components.AppBarTitle("Sobre o app") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppDimens.Spacing24)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
            
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Suap.ifal Mobile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Não oficial",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            com.example.ui.components.AppContentCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(AppDimens.Spacing24),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Desenvolvido com carinho para facilitar o acesso aos dados acadêmicos.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Código & Engenharia", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Luis Gustavo", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = Color.Blue,
                        modifier = Modifier.clickable {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Luis-Gustavo-Git-Hub"))
                            context.startActivity(intent)
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DesignServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "UI & UX Design", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Luis Gustavo & Comunidade", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = Color.Blue,
                        modifier = Modifier.clickable {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Luis-Gustavo-Git-Hub"))
                            context.startActivity(intent)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            com.example.ui.components.AppPrimaryButton(
                onClick = onActivateDemoMode,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.AutoFixHigh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ativar Modo Demonstração")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Versão ${com.example.BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
