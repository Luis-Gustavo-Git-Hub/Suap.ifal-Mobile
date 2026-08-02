# Suap.ifal Mobile

Um cliente não oficial moderno e ágil para o SUAP e SIGAA do Instituto Federal de Alagoas (IFAL). Desenvolvido com foco na experiência do aluno, fornecendo acesso rápido a notas, horários e rotinas acadêmicas (incluindo marcação de almoço).

## 📱 Recursos e Funcionalidades

### 🔐 Acesso e Autenticação
*   **Login Integrado:** Acesse usando suas credenciais do SUAP.
*   **Login Automático:** Opção de auto-login para entrar rapidamente.
*   **Modo Offline e Sincronização:** Os dados são salvos localmente e sincronizados em segundo plano.

### 📚 Acadêmico
*   **Dashboard:** Visão geral do seu semestre, aulas do dia e matérias.
*   **Boletim:** Acompanhe suas notas e faltas com indicação visual de progresso.
*   **Horários de Aula:** Grade de horários organizada, com salas e professores.
*   **Avisos e Mural:** Fique por dentro dos avisos institucionais mais recentes.
*   **Conteúdo das Disciplinas:** Consulte o plano de ensino e aulas de cada matéria.
*   **Estatísticas:** Visualize seu desempenho acadêmico em gráficos interativos.

### 🍽️ Refeitório (SIGAA)
*   **Reserva de Almoço Nativa:** Marque seu almoço no refeitório diretamente pelo aplicativo, integrado à sua conta SIGAA, de forma nativa e sem precisar lidar com telas complexas do navegador.
*   **Lembrete de Almoço:** Notificações diárias configuráveis lembrando você de marcar o almoço, com alertas em segundo plano.

### 🔔 Notificações e Alertas
*   **Lembrete de Início de Aula:** Seja notificado com antecedência sobre suas próximas aulas, contendo horários e matérias.
*   **Gerenciamento de Bateria:** Alertas e diálogos informativos que ensinam a desativar a otimização de bateria para garantir o recebimento ininterrupto de notificações em segundo plano.

## 🛠️ Tecnologias Utilizadas
*   **Linguagem:** Kotlin
*   **Interface UI:** Jetpack Compose & Material Design 3
*   **Sincronização:** WorkManager (Sincronização silenciosa e processamento de background)
*   **Alocação de Dados:** Room Database & DataStore Preferences
*   **Integração:** WebViews personalizadas com injeção JavaScript (Scraping local, sem backend intermediário)
*   **Navegação:** Compose Navigation

## ⚠️ Isenção de Responsabilidade
Este é um aplicativo **não oficial**, criado por estudantes para a comunidade, sem vínculos com a reitoria do IFAL, SUAP ou SIGAA. As informações trafegam e são obtidas diretamente entre o dispositivo do usuário e os servidores institucionais, preservando a privacidade das credenciais.
