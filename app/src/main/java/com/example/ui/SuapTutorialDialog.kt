package com.example.ui

import com.example.ui.theme.AppSemanticColors

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class TutorialStep(
    val stepNumber: Int,
    val title: String,
    val subtitle: String,
    val description: String,
    val tip: String,
    val icon: ImageVector,
    val accentColor: Color
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SuapTutorialDialog(
    onDismissRequest: () -> Unit,
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }

    val steps = listOf(
        TutorialStep(
            stepNumber = 1,
            title = "Por Que Precisamos Disso?",
            subtitle = "Sincronização & Acesso Offline",
            description = "Sua matrícula e senha são necessárias para conectar diretamente ao SUAP do IFAL, permitindo buscar suas notas, boletins, faltas e horários e exibi-los em um visual moderno.",
            tip = "100% Seguro: Suas credenciais são armazenadas de forma estritamente local no seu próprio celular e NUNCA são enviadas a servidores externos.",
            icon = Icons.Default.Security,
            accentColor = MaterialTheme.colorScheme.primary
        ),
        TutorialStep(
            stepNumber = 2,
            title = "Sua Matrícula do IFAL",
            subtitle = "Identificador Único do Aluno",
            description = "O campo Matrícula deve ser preenchido com o seu número de matrícula oficial do IFAL (por exemplo: 20241012345).",
            tip = "Onde encontrar: No comprovante de matrícula, declaração de vínculo, crachá estudantil ou e-mail de confirmação do campus.",
            icon = Icons.Default.Badge,
            accentColor = MaterialTheme.colorScheme.primary
        ),
        TutorialStep(
            stepNumber = 3,
            title = "Sua Senha do SUAP",
            subtitle = "Mesma Senha do Portal Oficial",
            description = "Utilize a mesma senha que você usa para acessar o sistema no portal oficial suap.ifal.edu.br.",
            tip = "Primeiro acesso? Caso nunca tenha acessado o SUAP, você pode cadastrar ou redefinir sua senha diretamente no site oficial do SUAP.",
            icon = Icons.Default.Key,
            accentColor = MaterialTheme.colorScheme.tertiary
        ),
        TutorialStep(
            stepNumber = 4,
            title = "Onde Preencher no App",
            subtitle = "Aba Perfil ➔ Credenciais SUAP",
            description = "Você insere suas credenciais na aba Perfil, dentro do cartão 'Credenciais SUAP'. Ao clicar no botão abaixo, você será levado diretamente para lá com os campos abertos!",
            tip = "Preencha uma única vez para ter consulta rápida e instantânea de todo o seu histórico acadêmico!",
            icon = Icons.Default.Lock,
            accentColor = AppSemanticColors.success
        )
    )

    val step = steps[currentStep]

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
                .padding(vertical = 16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = step.accentColor.copy(alpha = 0.12f),
                    contentColor = step.accentColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text(
                            text = "PASSO ${step.stepNumber} DE ${steps.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Animated step content
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "tutorial_step_content"
                ) { targetStep ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Big Icon Circle with soft aura
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            targetStep.accentColor.copy(alpha = 0.22f),
                                            targetStep.accentColor.copy(alpha = 0.06f)
                                        )
                                    )
                                )
                                .border(2.dp, targetStep.accentColor.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = targetStep.icon,
                                contentDescription = null,
                                tint = targetStep.accentColor,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = targetStep.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = targetStep.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = targetStep.accentColor,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Description Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = targetStep.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tip Box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = targetStep.accentColor.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, targetStep.accentColor.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "💡",
                                    
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                                Text(
                                    text = targetStep.tip,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Stepper Dots Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, _ ->
                        val isSelected = index == currentStep
                        val width by animateDpAsState(targetValue = if (isSelected) 22.dp else 7.dp, label = "dot_width")
                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .width(width)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(
                                    if (isSelected) step.accentColor
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions Bottom Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        TextButton(
                            onClick = { currentStep-- },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Voltar", )
                        }
                    } else {
                        TextButton(
                            onClick = {
                                onComplete()
                            },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("Pular",  color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (currentStep < steps.size - 1) {
                        Button(
                            onClick = { currentStep++ },
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = step.accentColor)
                        ) {
                            Text("Próximo",  fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                onComplete()
                            },
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = step.accentColor)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ir para o Perfil e Preencher",  fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
