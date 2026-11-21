# Auditoria e padronização visual

Auditoria estática antes da alteração; não equivale a teste visual em aparelho.

| Tela/componente | Confirmado no código | Diagnóstico refutado ou ressalva |
|---|---|---|
| AlmocoScreen.kt | 1 cabeçalho(s) M3 local(is); padding fora de ScreenPadding; botões M3 locais; cards/superfícies locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| AppNavigation.kt | família Default | Rotas tipadas já selecionam Perfil; Início já está em português; preservar IconPack/NavBarStyle. |
| AssuntosScreen.kt | 1 cabeçalho(s) M3 local(is); 108 cores literais; botões M3 locais; cards/superfícies locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| AulasScreen.kt | 1 cabeçalho(s) M3 local(is); padding fora de ScreenPadding; botões M3 locais; cards/superfícies locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| AvisosScreen.kt | padding fora de ScreenPadding; botões M3 locais; cards/superfícies locais; família Default; já usa AppPageHeader | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| BoletimScreen.kt | 1 cabeçalho(s) M3 local(is); padding fora de ScreenPadding; botões M3 locais; cards/superfícies locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| CreditsScreen.kt | 1 cabeçalho(s) M3 local(is); padding fora de ScreenPadding; botões M3 locais; cards/superfícies locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| CustomScannerActivity.kt | Componente específico do fluxo | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| CustomThemeCreatorDialog.kt | padding fora de ScreenPadding; botões M3 locais; cards/superfícies locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| DashboardScreen.kt | botões M3 locais; cards/superfícies locais; família Default; já usa AppPageHeader | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| DetalhesJustificativaScreen.kt | 1 cabeçalho(s) M3 local(is); padding fora de ScreenPadding; botões M3 locais; cards/superfícies locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| FriendsScreen.kt | 2 cabeçalho(s) M3 local(is); 9 cores literais; padding fora de ScreenPadding; botões M3 locais; cards/superfícies locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| HorariosScreen.kt | 1 cabeçalho(s) M3 local(is); 23 cores literais; padding fora de ScreenPadding; botões M3 locais; cards/superfícies locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| JustificativaFaltaScreen.kt | 1 cabeçalho(s) M3 local(is); botões M3 locais; cards/superfícies locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| LoginScreen.kt | padding fora de ScreenPadding; botões M3 locais; cards/superfícies locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| LogsScreen.kt | 1 cabeçalho(s) M3 local(is); botões M3 locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| PdfViewerScreen.kt | 1 cabeçalho(s) M3 local(is); botões M3 locais; cards/superfícies locais | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| ProfileComponents.kt | Componente específico do fluxo | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| ProfileScreen.kt | botões M3 locais; cards/superfícies locais; família Default; já usa AppPageHeader | Perfil já extraído da navegação; não repetir extração. |
| ReposicaoAtividadeScreen.kt | 1 cabeçalho(s) M3 local(is); padding fora de ScreenPadding; botões M3 locais; cards/superfícies locais | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| SigaaCertificadoNativeScreen.kt | 2 cabeçalho(s) M3 local(is); padding fora de ScreenPadding; botões M3 locais; cards/superfícies locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| SigaaNativeLoginScreen.kt | 1 cabeçalho(s) M3 local(is); padding fora de ScreenPadding; botões M3 locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| SigaaNativeMarcarAlmocoScreen.kt | 1 cabeçalho(s) M3 local(is); padding fora de ScreenPadding; botões M3 locais; cards/superfícies locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| SigaaWebViewScreen.kt | 1 cabeçalho(s) M3 local(is); padding fora de ScreenPadding; botões M3 locais; cards/superfícies locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| SituacaoJustificativaScreen.kt | 1 cabeçalho(s) M3 local(is); padding fora de ScreenPadding; botões M3 locais; cards/superfícies locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| StatisticsScreen.kt | padding fora de ScreenPadding; cards/superfícies locais; família Default | Não existe card Notas Detalhadas (Texto); cinco abas funcionais. |
| SuapTutorialDialog.kt | padding fora de ScreenPadding; botões M3 locais; cards/superfícies locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| SuapWebViewScreen.kt | 1 cabeçalho(s) M3 local(is); botões M3 locais | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| SubjectTopicsScreen.kt | 1 cabeçalho(s) M3 local(is); botões M3 locais; cards/superfícies locais; família Default | Subtítulo é Materiais de Aula, não URL crua. |
| XpCard.kt | botões M3 locais; cards/superfícies locais | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| components/DashboardTodayCard.kt | padding fora de ScreenPadding; botões M3 locais; cards/superfícies locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| dialogs/AppDialogs.kt | botões M3 locais; família Default | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |
| dialogs/BatteryAndTermsDialogs.kt | botões M3 locais | Nenhum botão sem ação ou texto de teste comprovado; preservar callbacks. |

## Diagnóstico geral

- Confirmado: componentes existentes pouco adotados, cabeçalhos locais, cores de prova e mapeamento de disciplinas duplicado, espaçamentos irregulares e rótulo Estatística no singular.
- Refutado: Type.kt já tem escala completa; Theme.kt já define cinco shapes e SemanticColors já tem claro/escuro/Amoled. Perfil já está extraído. A seleção de Perfil usa rotas tipadas.
- Sobreposições preservadas: Assuntos escolhe disciplinas, Aulas mostra conteúdo, SubjectTopics mostra materiais; almoço tem entrada/autenticação/agendamento; WebViews e PDF têm renderizadores próprios; justificativas têm operações distintas.
- Personalização: preservar fontes, ROBIN_KAWA, Custom, IconPack e NavBarStyle. Não substituir seleção de ícones por uma família fixa.

## Fora do escopo de limpeza

BenchmarkDataSeeder.kt é ferramenta de benchmark desativada em builds normais. Nenhum arquivo fora de UI será apagado. Arquivos soltos, APKs, .idea e baseline de lint devem ser avaliados separadamente.

## Design system adotado

AppTopBar (título/subtítulo e slots, altura adaptada à fonte), AppPageHeader, AppCard (accent), AppContentCard (conteúdo com padding próprio), AppButton e variantes primária/tonal/outline/texto/destrutiva, AppTextField, AppFilterChip, AppDialog, AppBottomSheet, AppInfoRow, AppSwitchRow, AppEmptyState, AppErrorState e AppLoadingState. Os últimos dois estados e InfoRow/SwitchRow são pontos de extensão; nem todos os estados locais foram substituídos. SkeletonLoader foi preservado.

Dimens: 4/8/12/16/24/32, ScreenPadding, CardPadding, ItemSpacing, IconSmall/Medium/Large. Shapes existentes preservados; shapes numéricos de tela migrados aos papéis do tema; pill percentual e recortes da personalização preservados. Os ícones comuns usam Outlined e os direcionais AutoMirrored; a barra mantém seus IconPacks.

Cores: mapeamento e gradientes de disciplinas e amostras de cores de amigos centralizados em SubjectPalette. Provas usam warning. Paletas selecionadas preservadas; pares ilegíveis de texto/fundo recebem correção de contraste AA. Esta verificação não cobre toda composição com transparência/gráficos de todas as telas.

## Antes → depois por arquivo

| Tela/componente | Alterações realizadas |
|---|---|
| AlmocoScreen.kt | cabeçalho compartilhado; card compartilhado; espaçamento por tokens; ícones Outlined |
| AppNavigation.kt | espaçamento por tokens; ícones Outlined; Estatísticas no plural; fade de abas e slide/fade de detalhes ≤240 ms |
| AssuntosScreen.kt | cabeçalho compartilhado; campo compartilhado; card compartilhado; espaçamento por tokens; ícones Outlined; paleta duplicada removida; ícones extraídos para SubjectVisuals |
| AulasScreen.kt | cabeçalho compartilhado; botões compartilhados; campo compartilhado; card compartilhado; bottom sheet compartilhado; filtros compartilhados; espaçamento por tokens; ícones Outlined |
| AvisosScreen.kt | cabeçalho de aba; campo compartilhado; card compartilhado; diálogo compartilhado; bottom sheet compartilhado; filtros compartilhados; espaçamento por tokens; ícones Outlined |
| BlinkingSyncDot.kt | espaçamento por tokens; ícones Outlined |
| BoletimScreen.kt | cabeçalho compartilhado; campo compartilhado; card compartilhado; bottom sheet compartilhado; espaçamento por tokens; ícones Outlined |
| CreditsScreen.kt | cabeçalho compartilhado; botões compartilhados; card compartilhado; espaçamento por tokens; ícones Outlined; acesso duplicado a logs removido; acesso preservado no Perfil |
| CustomThemeCreatorDialog.kt | botões compartilhados; campo compartilhado; card compartilhado; espaçamento por tokens; ícones Outlined |
| DashboardScreen.kt | cabeçalho de aba; card compartilhado; diálogo compartilhado; espaçamento por tokens; ícones Outlined |
| DetalhesJustificativaScreen.kt | cabeçalho compartilhado; botões compartilhados; espaçamento por tokens; ícones Outlined |
| FriendsScreen.kt | cabeçalho compartilhado; botões compartilhados; campo compartilhado; diálogo compartilhado; bottom sheet compartilhado; espaçamento por tokens; ícones Outlined |
| HorariosScreen.kt | cabeçalho compartilhado; espaçamento por tokens; ícones Outlined; cor de provas semântica |
| JustificativaFaltaScreen.kt | cabeçalho compartilhado; botões compartilhados; campo compartilhado; espaçamento por tokens; ícones Outlined |
| LoginScreen.kt | cabeçalho compartilhado; botões compartilhados; campo compartilhado; card compartilhado; diálogo compartilhado; espaçamento por tokens; ícones Outlined; voltar manual substituído por AppTopBar na tela nativa |
| LogsScreen.kt | cabeçalho compartilhado; botões compartilhados; filtros compartilhados; espaçamento por tokens; ícones Outlined |
| PdfViewerScreen.kt | cabeçalho compartilhado; card compartilhado; espaçamento por tokens |
| ProfileComponents.kt | espaçamento por tokens |
| ProfileScreen.kt | cabeçalho de aba; botões compartilhados; campo compartilhado; card compartilhado; diálogo compartilhado; espaçamento por tokens; ícones Outlined |
| ReposicaoAtividadeScreen.kt | cabeçalho compartilhado; botões compartilhados; campo compartilhado; card compartilhado; espaçamento por tokens |
| SigaaCertificadoNativeScreen.kt | cabeçalho compartilhado; botões compartilhados; campo compartilhado; card compartilhado; espaçamento por tokens; ícones Outlined |
| SigaaNativeLoginScreen.kt | cabeçalho compartilhado; botões compartilhados; campo compartilhado; espaçamento por tokens; ícones Outlined |
| SigaaNativeMarcarAlmocoScreen.kt | cabeçalho compartilhado; botões compartilhados; card compartilhado; espaçamento por tokens; ícones Outlined |
| SigaaWebViewScreen.kt | cabeçalho compartilhado; espaçamento por tokens; ícones Outlined |
| SituacaoJustificativaScreen.kt | cabeçalho compartilhado; botões compartilhados; espaçamento por tokens; ícones Outlined |
| StatisticsScreen.kt | cabeçalho de aba; filtros compartilhados; espaçamento por tokens; ícones Outlined |
| SuapTutorialDialog.kt | botões compartilhados; card compartilhado; espaçamento por tokens; ícones Outlined |
| SuapWebViewScreen.kt | cabeçalho compartilhado |
| SubjectTopicsScreen.kt | cabeçalho compartilhado; card compartilhado; espaçamento por tokens; ícones Outlined |
| XpCard.kt | card compartilhado; diálogo compartilhado; espaçamento por tokens |
| components/AppComponents.kt | cabeçalho compartilhado; cabeçalho de aba; espaçamento por tokens; ícones Outlined |
| components/DashboardTodayCard.kt | espaçamento por tokens; ícones Outlined |
| components/SkeletonLoader.kt | espaçamento por tokens |
| components/SyncTimestampBadge.kt | espaçamento por tokens; ícones Outlined |
| dialogs/AppDialogs.kt | diálogo compartilhado; espaçamento por tokens; ícones Outlined |
| dialogs/BatteryAndTermsDialogs.kt | botões compartilhados; diálogo compartilhado |

## Remoções e nomes

- Removido mapeamento duplicado de cores de Assuntos; a API visual delega ao SubjectPalette.
- Removido botão de logs de Créditos; o Perfil continua oferecendo diagnóstico.
- Extraídos os seletores de ícone de disciplina para SubjectVisuals.kt, sem mudar assinatura.
- “Estatística” → “Estatísticas”; “Materiais e Aulas” → “Disciplinas”; “Módulo Refeitório” → “Almoço”; “Entrar (Oculto)” → “Entrar”; “Login via Navegador (Clássico)” → “Entrar pelo navegador”.
- “Cancelar”, “Voltar”, “Tentar novamente”, “Salvar” e “Fechar” disponíveis em strings.xml; ocorrências de Text simples adotam os recursos.
- Nenhum arquivo de produção fora da UI foi apagado. A alteração em strings.xml contém somente textos.

## Suposições e limites

- Manter superfícies específicas de badges, gráficos e cards de personalização quando têm função visual própria. Os wrappers aceitam os mesmos estilos e callbacks; estilos específicos não foram todos eliminados.
- Não fundir fluxos parecidos, não remover demonstração existente nem ações sem prova de que estavam mortas.
- Preservar todos os temas, fontes e estilos da barra. Testar componentes compartilhados não substitui inspeção manual de todas as telas.
- Os 36 casos de teste de tema cobrem dez temas nativos mais Custom Âmbar claro/escuro, cada um com Fira Code, Cinzel e Fredoka, fonte de sistema a 200%, largura 320dp, título sem overflow, botão acionável e pares essenciais de cores.
- Não há validação manual com TalkBack, Accessibility Scanner ou dispositivo físico. Landscape, telas reais completas e transparências exigem revisão visual complementar; não considerar esses critérios integralmente aceitos apenas pelo build.

## Verificação realizada

- assembleDebug e testDebugUnitTest: BUILD SUCCESSFUL. 87 testes, zero falhas; 36 casos de temas com GraphicsMode.NATIVE.
- Teste de consistência visual corrigido para localizar os fontes tanto na raiz quanto no diretório do módulo, evitando aprovação sem examinar arquivos.
- Diff: nenhum ViewModel, repository, rede, banco, worker, receiver, FriendsManager ou NavRoute alterado. Blocos triple-quoted de login/SIGAA idênticos aos da versão anterior.
- Limpeza do repositório: somente app/lint-baseline.xml aparece entre APKs/.idea/patches/baselines versionados examinados; preservado.
