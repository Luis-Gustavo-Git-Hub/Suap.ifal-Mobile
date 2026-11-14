package com.example.ui

import com.example.ui.components.*
import com.example.ui.theme.Spacing
import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext


@Composable
fun ProfileIdentity(studentName: String, profilePhotoUrl: String, initials: String) {
    // Header background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(top = 48.dp, bottom = Spacing.large),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(Spacing.xs),
                contentAlignment = Alignment.Center
            ) {
                if (profilePhotoUrl.isNotEmpty()) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    ) {
                        coil.compose.AsyncImage(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(profilePhotoUrl)
                                .crossfade(true)
                                .addHeader("Cookie", android.webkit.CookieManager.getInstance().getCookie("https://suap.ifal.edu.br") ?: "")
                                .addHeader("Referer", "https://suap.ifal.edu.br/")
                                .build(),
                            contentDescription = "Foto do aluno",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.onPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = studentName.ifEmpty { "Perfil do Aluno" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun ProfileAccount(fullName: String, matricula: String, emailPessoal: String, ingresso: String, curso: String) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(Spacing.ScreenPadding), verticalArrangement = Arrangement.spacedBy(Spacing.normal)) {
            ProfileField(label = "Nome", value = fullName)
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            ProfileField(label = "Matrícula", value = matricula, isCopyable = true)
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            ProfileField(label = "E-mail Pessoal", value = emailPessoal, isCopyable = true)
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            ProfileField(label = "Ingresso", value = ingresso)
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            ProfileField(label = "Curso", value = curso.replaceFirst(Regex("^\\S+\\s*-\\s*"), ""))
        }
    }
}
