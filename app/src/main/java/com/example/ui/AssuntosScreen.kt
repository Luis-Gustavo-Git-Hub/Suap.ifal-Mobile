package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Subject
import androidx.compose.foundation.layout.Row

import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssuntosScreen(
    viewModel: SuapViewModel,
    onBack: () -> Unit,
    onSubjectClick: (String, String) -> Unit
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredSubjects = subjects.filter { 
        it.name.orEmpty().contains(searchQuery, ignoreCase = true) 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Matérias") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                placeholder = { Text("Pesquisar disciplina...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredSubjects) { subject ->
                    val urlToUse = if ((subject.url ?: "").isNotEmpty()) subject.url.orEmpty() else subject.detailUrl.orEmpty()
                    AssuntoSubjectCard(subject = subject, onClick = { onSubjectClick(subject.name.orEmpty(), urlToUse) })
                }
            }
        }
    }
}

@Composable
fun AssuntoSubjectCard(subject: Subject, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Acessar materiais",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Acessar assuntos",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
