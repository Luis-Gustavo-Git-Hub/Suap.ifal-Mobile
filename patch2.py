with open("app/src/main/java/com/example/ui/AppNavigation.kt", "r") as f:
    text = f.read()

target = '''                                        Button(
                                            onClick = {
                                                 if (termsAccepted) {
                                                    sharedPrefs.edit().putString("username", editMatricula).putString("password", editSenha).apply()
                                                    showSavedMsg = true
                                                } else {
                                                    termsDialogTarget = "SUAP"
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(if (showSavedMsg) "Salvo com sucesso!" else "Salvar SUAP")
                                        }'''

replacement = target + '''
                                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                Text("Login Automático", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text("Sincroniza automaticamente ao abrir", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            androidx.compose.material3.Switch(
                                                checked = autoLoginEnabled,
                                                onCheckedChange = {
                                                    autoLoginEnabled = it
                                                    sharedPrefs.edit().putBoolean("auto_login", it).apply()
                                                }
                                            )
                                        }'''

text = text.replace(target, replacement)

with open("app/src/main/java/com/example/ui/AppNavigation.kt", "w") as f:
    f.write(text)
