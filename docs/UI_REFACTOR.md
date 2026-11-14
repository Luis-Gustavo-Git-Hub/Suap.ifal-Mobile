# Refatoração visual — Suap.ifal Mobile

## Auditoria do código original

| Tela/área | Confirmado | Ressalvas / não confirmado |
|---|---|---|
| Login | Voltar com posição fixa; aceite de termos próprio; nomenclatura de navegador técnica | `onDemoClick` e `getDemoHtml` existiam, mas não havia botão de demonstração visível |
| Dashboard | Cabeçalho próprio, gutters de 24dp, textos de 8/10sp, cinza fixo em contas desconectadas | Barra de progresso do boletim era constante em 75%, sem vínculo com dados |
| Boletim | TopBar com subtítulo, cores de aprovação e favorito fixas, textos pequenos | Cálculos e callbacks de notas devem ser preservados |
| Disciplinas | Título “Matérias”, textos de assuntos/materiais inconsistentes | Rota `assuntos` deve continuar existindo |
| Materiais (SubjectTopics) | URL crua no subtítulo, botão “Atualizar / Tentar Novamente” | URL continua necessária para buscar e baixar materiais |
| Horários | Cabeçalho e subtítulo próprios, tamanhos e formas locais | Dados e seleção de dias preservados |
| Avisos | Cabeçalho próprio, paleta fixa de avatares, textos pequenos, bottom sheet próprio | Rota `notifications` já exibia “Avisos” |
| Estatísticas | Cores fixas e corpo de 10sp; destaques/atenção/alertas sobrepostos | Fallback contém dados reais de notas textuais; foi mantido com nome claro |
| Perfil | Mais de 900 linhas inline na navegação; termos duplicados; botão destrutivo vermelho fixo | O acesso aos logs estava **comentado**, portanto não havia dois acessos ativos |
| Criador de tema | Modal e campos com estilos locais | Cores dos seletores representam as cores escolhidas; devem permanecer como amostras |
| Almoço | Cabeçalhos e cards locais | As três telas existem, mas o Dashboard abre `sigaa_almoco` diretamente; não há uma sequência obrigatória das três |
| Login SIGAA | Textos “Oculto” e “Clássico”; campo de senha próprio | WebView e autenticação preservados |
| Marcar almoço | Cabeçalho e formas locais | Marcação e JavaScript preservados |
| Navegador SIGAA | TopBar local | Navegação da WebView preservada |
| Créditos | Acesso ativo a logs e links azuis fixos | O acesso foi transferido para Sobre no Perfil |
| Logs | TopBar local e fonte monoespaçada com tamanho local | Leitura, exportação e limpeza de logs preservadas |
| Temas | Typography incompleta; esquemas com papéis padrão não derivados da paleta; Custom com texto/container de baixo contraste | `material-icons-extended` confirmado nas dependências |
| Barra inferior | “Home”; seleção de Perfil comparava rota sem argumentos; ícones de famílias misturadas | Rotas e argumentos não foram renomeados |

## Design system

- `theme/Color.kt`: fábrica de `ColorScheme`, família de superfícies M3, foreground legível por luminância, contraste dos acentos e semântica `AppColors` (`success`, `warning`, `danger`, `info`, containers e foregrounds). A distribuição de notas usa essas cores.
- `theme/Theme.kt`: seleciona todos os nove temas e Custom, fornece `LocalAppColors`, tipografia e formas.
- `theme/Type.kt`: quinze estilos M3, mínimo de 11sp.
- `theme/Shape.kt`: 8/12/16/24/28dp; `Spacing` 4/8/12/16/24/32dp e gutter horizontal de 16dp.
- `components/`: AppTopBar, ScreenHeader, AppCard, SectionHeader, InfoRow, StatusChip, StatusDot, PrimaryButton, SecondaryButton, DestructiveButton, AppTextField, SwitchRow, EmptyState, LoadingState, ErrorState, AppDialog, AppBottomSheet, TermsCheckRow e AppTimePickerDialog.
- Ícones Rounded, AutoMirrored.Rounded para direção e Outlined apenas nas abas não selecionadas.
- Botões e abas com área mínima de 48dp. Rótulos da barra inferior podem ocupar várias linhas; não dependem de animação horizontal para aparecer.
- Strings repetidas de navegação e ações em `strings.xml`.

## Alterações por tela

| Tela | Alteração |
|---|---|
| Login | AppTopBar, conteúdo rolável, campos e senha compartilhados, TermsCheckRow e AppDialog |
| Dashboard | ScreenHeader, data legível, tokens de forma/tipografia, estados desconectados temáticos com CTA, remoção do progresso fictício |
| Boletim | AppTopBar, cards/campos/sheet compartilhados, cores semânticas de notas e favorito |
| Disciplinas | Título coerente, AppTopBar, campo de busca e cards compartilhados |
| Materiais | Subtítulo descritivo sem URL, LoadingState/EmptyState, busca/baixar preservados |
| Horários | AppTopBar, componentes e tokens, ícones Rounded |
| Avisos | ScreenHeader, loading/vazio compartilhados, avatar por container de tema, busca e sheet compartilhados |
| Estatísticas | ScreenHeader, cores semânticas na distribuição, remoção de Destaques/Atenção redundantes, alertas preservados e fallback renomeado |
| Perfil | ProfileScreen, ProfileAccount/ProfileIdentity e ProfileField separados; seções Conta/Aparência/Notificações/Credenciais/Sobre; switches/termos/senhas/diálogos compartilhados |
| Criador de tema | Campos/cards e tokens compartilhados; amostras continuam mostrando cores reais |
| Almoço | Título Almoço, AppTopBar, cards temáticos e foregrounds correspondentes |
| Login SIGAA | Cabeçalho coerente, Entrar/Entrar pelo navegador, senha compartilhada |
| Marcar almoço | Título Almoço, AppTopBar, cards/botões e tokens |
| Navegador SIGAA | AppTopBar e ícones coerentes |
| Créditos | AppTopBar, cards e cores de links do tema, logs transferidos para Perfil |
| Logs | AppTopBar, ações/diálogos e tipografia do tema; família monoespaçada mantida |
| Termos / economia de energia / tema / horário | Diálogos de tokens compartilhados; horário usa entrada M3 de 24 horas |
| Navegação | Seleção de Perfil por prefixo, rótulo Início, abas com fade-through e detalhes com slide curto/fade, até 240ms |

## Remoções e renomeações

- Removidos `getDemoHtml` sem uso, bloco de UI de logs comentado e função de Destaques/Atenção redundante nas estatísticas.
- Removida barra de progresso constante em 75% do Dashboard por sugerir informação inexistente.
- Único acesso a logs em Perfil → Sobre. O acesso antigo em Créditos foi removido.
- “Notas Detalhadas (Texto)” → “Notas disponíveis”: preserva notas que não podem ser representadas nos gráficos.
- “Home” → “Início”; “Matérias”/“Assuntos das matérias” → “Disciplinas”; nomes técnicos de login → textos de acesso comuns.
- Substituída a paleta antiga de Color.kt por esquemas e semântica compartilhados.
- Fora de `ui/`, foram adicionadas strings, testes/documentação e reparado o wrapper Gradle corrompido com o binário oficial da mesma versão. Nenhum arquivo foi apagado fora de `ui/`.

## Arquivos potencialmente dispensáveis (não removidos)

`patch.py`, `patch2.py`, `patch3.py`; `app/release/Suap-Mobile-2.0.apk` (17.017.683 bytes); metadados de `.idea/`; `app/lint-baseline.xml`. Não remover o baseline automaticamente: ele pode registrar avisos conhecidos do projeto.

## Validação

- `./gradlew assembleDebug`: APK de depuração gerado; assinatura verificada com `apksigner`.
- `ThemeContrastTest`: testa os nove temas e 512 combinações Custom (cor principal × cor do Perfil × claro/escuro), com os dois estados do tema do sistema. Verifica papéis de texto/container e acentos sobre superfícies.
- `ThemeGalleryTest`: renderiza todos os temas, incluindo Custom Âmbar claro/escuro, em viewport normal, tela pequena com fonte 2× e landscape; também renderiza o Dashboard com fonte 2×. São 34 capturas.
- Inspeção das capturas de Dracula, Cyberpunk, Custom Âmbar e do Dashboard; o contraste do CTA desconectado foi corrigido após inspeção.
- Comparação do diff: ViewModels, Room/dados, workers, receivers e utilitários sem alterações; nove chamadas `evaluateJavascript` preservadas; definições de rotas e argumentos preservadas.
- `git diff --check`: sem erros.
- SDK/Gradle, cache, configuração de proxy e chave **local** de debug foram preparados no ambiente. Nenhum segredo ou chave de assinatura foi versionado. A configuração original permite a ausência de `google-services.json` com aviso.

Limites: capturas automatizadas cobrem o design system e o Dashboard; não representam uma navegação manual autenticada por todas as telas, nem uma auditoria AA de cada conteúdo dinâmico. Login SUAP/SIGAA real, marcação de almoço, permissões e notificações não foram acionados nos testes de UI. O bloqueio original a retrato no manifest foi preservado. Última execução: **BUILD SUCCESSFUL**, com **7 testes aprovados**, zero falhas e 34 capturas atualizadas após o último ajuste de código.

## Suposições

- Mantidos todos os identificadores persistidos de tema, rotas e argumentos.
- Notas textuais são conteúdo útil, não debug, e continuam acessíveis.
- Fluxos de login e de almoço continuam separados; somente sua apresentação foi padronizada.
- O manifest original fixa a Activity em retrato. Essa restrição foi preservada para evitar recriar WebViews e mudar os fluxos de autenticação; os testes de layout usam também viewport landscape.
- O wrapper foi reparado para viabilizar a compilação solicitada, sem trocar versões de Gradle, AGP, Kotlin ou dependências.


## Capturas para revisão

[Claro](ui-preview/claro.png) · [Dracula](ui-preview/dracula.png) · [Custom Âmbar](ui-preview/custom-ambar.png) · [Dashboard com fonte 2×](ui-preview/dashboard-fonte-ampliada.png)
