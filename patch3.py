with open("app/src/main/java/com/example/ui/AppNavigation.kt", "r") as f:
    lines = f.readlines()

new_lines = []
inserted = False
for line in lines:
    new_lines.append(line)
    if 'Text(if (showSavedMsg) "Salvo com sucesso!" else "Salvar SUAP")' in line:
        if not inserted:
            inserted = True
            new_lines.append('                                        }\n')
            new_lines.append('                                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))\n')
            new_lines.append('                                        Row(\n')
            new_lines.append('                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),\n')
            new_lines.append('                                            verticalAlignment = Alignment.CenterVertically,\n')
            new_lines.append('                                            horizontalArrangement = Arrangement.SpaceBetween\n')
            new_lines.append('                                        ) {\n')
            new_lines.append('                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {\n')
            new_lines.append('                                                Text("Login Automático", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)\n')
            new_lines.append('                                                Text("Sincroniza automaticamente ao abrir", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)\n')
            new_lines.append('                                            }\n')
            new_lines.append('                                            androidx.compose.material3.Switch(\n')
            new_lines.append('                                                checked = autoLoginEnabled,\n')
            new_lines.append('                                                onCheckedChange = {\n')
            new_lines.append('                                                    autoLoginEnabled = it\n')
            new_lines.append('                                                    sharedPrefs.edit().putBoolean("auto_login", it).apply()\n')
            new_lines.append('                                                }\n')
            new_lines.append('                                            )\n')
            new_lines.append('                                        \n')

with open("app/src/main/java/com/example/ui/AppNavigation.kt", "w") as f:
    f.writelines(new_lines)
