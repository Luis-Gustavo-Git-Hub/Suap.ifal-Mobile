# Instruções do Projeto: Assistente Acadêmico IFAL (SUAP/SIGAA)

Estas são as diretrizes permanentes de arquitetura, design visual e comportamento para este aplicativo Android. Siga estas regras rigorosamente em todas as futuras manutenções e adições de código.

## 🎨 UI & UX (Design Visual)
- **Framework:** 100% Jetpack Compose com Material Design 3 (M3).
- **Padrão de Layout (Dashboard):** Estilo "Bento Grid". O Dashboard deve usar cards com bordas arredondadas generosas (ex: 16dp a 24dp), espaçamento consistente (16dp) e ícones expressivos para criar uma interface moderna estilo widget.
- **Componentes:** Utilize sempre os componentes oficiais do M3 (`Scaffold`, `TopAppBar`, `Card`, `ElevatedCard`, `FilledTonalButton`).
- **Acessibilidade & Feedback:** Adicionar animações de transição suaves (ex: `animateContentSize`), ripples nos botões e suporte a navegação por gestos.

## 🖌️ Motor de Temas (Theme Engine)
O aplicativo não usa apenas temas estáticos; ele possui um motor dinâmico gerido no pacote `ui/theme`.
- **Enum `AppTheme`:** Controla as opções de temas globais.
- **Temas Predefinidos:** O app suporta temas nativos (Dynamic, Light, Dark) e temas ultra-personalizados (AMOLED para telas OLED, Dracula, Cyberpunk, Sunset, Nature, Amber, Deep Purple, etc.).
- **Temas Customizados:** O usuário pode montar seu próprio tema via `CustomThemeCreatorDialog.kt`. As paletas geradas interagem diretamente com o `ColorScheme` do Material 3.
- **Ícones do Launcher Dinâmicos:** As cores dos ícones (mipmap) são geradas para combinar perfeitamente com a estética escolhida no app.

## 📂 Estrutura de Telas (Mapeamento do Código Fonte)
Para manter o limite de contexto saudável, não copie o código fonte inteiro aqui. Em vez disso, **sempre consulte** estes arquivos em `app/src/main/java/com/example/ui/` quando precisar de contexto:

1. **`AppNavigation.kt`**: O coração do app. Contém o `NavHost` e todas as rotas (Dashboard, Boletim, Login, SIGAA, Horários).
2. **`DashboardScreen.kt`**: A tela principal em "Bento Grid", mostrando saudações, atalhos, frequência global e status de sincronização (`SyncWorker`).
3. **`BoletimScreen.kt`**: Detalhamento de notas, faltas, e cálculo inteligente para exames finais.
4. **Backend e Scraping (O Motor do SUAP/SIGAA)**:
   - **`SuapParser.kt` & `SuapViewModel.kt`**: Aqui reside toda a lógica de web scraping com Jsoup e requisições HTTP (OkHttp). Sempre que precisar mexer na extração de dados do SUAP, leia o `SuapParser.kt`.
   - **`SyncWorker.kt`**: Lógica de sincronização em background.
5. **`FriendsScreen.kt` & `FriendsManager.kt`**: UI e lógica para a Rede Acadêmica via Firebase Firestore. Adição via QR Code e comparação de grades/notas.
6. **`SigaaNativeMarcarAlmocoScreen.kt` & `AlmocoScreen.kt`**: Integração do refeitório, raspagem com JSoup para agendamento 100% nativo.
7. **`HorariosScreen.kt`**: Grade semanal de aulas sincronizada do SUAP.
8. **`LoginScreen.kt`**: Tela de autenticação baseada no web scraping oficial do SUAP.

## ⚙️ Regras de Ouro do Desenvolvimento
1. **Offline-First:** O app armazena dados acadêmicos usando Room (`AppDatabase`). Sempre leia do banco de dados local primeiro para garantir que o app funcione sem internet.
2. **Atualização em Segundo Plano:** O pacote `com.example.worker` gerencia o WorkManager (`SyncWorker`) para buscar novas notas e emitir notificações locais em background.
3. **Sem Código Obsoleto:** Mantenha o Kotlin atualizado (atualmente K2 / 2.2), use `collectAsStateWithLifecycle` para fluxos de estado e evite componentes legados do Android View (XML).
