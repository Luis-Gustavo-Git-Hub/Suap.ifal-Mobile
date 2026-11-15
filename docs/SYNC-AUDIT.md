# Sincronização SUAP: auditoria e validação

## Diagnóstico

| Item | Resultado |
|---|---|
| Avisos fora do pós-login | Confirmado; incluído no pipeline compartilhado. |
| Horários/Avisos fazem rede ao entrar | Confirmado; efeitos de entrada removidos. A tela agora apenas observa cache/estado; atualização manual solicita módulo ao coordenador. |
| Ausência de estado global | Confirmado; SyncCoordinator.shared possui StateFlow, dono do login em primeiro plano e espera compartilhada de sessão. |
| Comentário de reautenticação enganoso | Confirmado; executeRequest detecta rejeição, sinaliza o coordenador e repete a requisição original uma vez após a recuperação. Não submete credenciais. |
| Erros genéricos de conexão | Confirmado; erros de transporte, sessão, credenciais, verificação, indisponibilidade e parsing têm tipos. Mensagens de atualização dos módulos não classificam toda falha como falta de internet. |
| Worker repete em qualquer falha | Confirmado; orçamento de uma recuperação HTTP por execução, compartilhado entre módulos, gov.br excluído, retry somente para erros transitórios e backoff exponencial. |
| Worker executa exatamente às 19h | Refutado no código atual: WorkScheduler usa período de 24h com flex de 6h; não há garantia de horário exato. |
| Login HTTP sem uso nas telas | Confirmado; agora reservado à recuperação do worker. O login da interface permanece na WebView escondida. |
| Captcha/credenciais inválidas/gov.br renováveis silenciosamente | Refutado: exigem ação do usuário; o coordenador bloqueia novas tentativas e emite estado global para uma ação no app. |

## Arquitetura

`Idle → Syncing(Autenticando) → Syncing(Lendo boletim) → Syncing(Atualizando dados) → Synced(at, partialFailures)`.

Falhas que impedem continuar geram `Failed(reason, requiresUserAction)`. A navegação observa a solicitação de login e usa o LoginScreen escondido existente. O HTML continua sendo processado por parseHtmlAndSave, com os mesmos parsers. Depois disso, calendário, médias, avisos e URLs de disciplinas são atualizados em sequência; as médias continuam consultando duas etapas por vez. Um limite compartilhado de duas requisições protege o transporte de documentos.

Jobs de módulos são aguardados antes da conclusão. Cancelar uma geração de pipeline cancela o módulo aguardado; respostas de uma sessão antiga não iniciam outro login se uma sessão nova já foi aceita. Horários e boletim recebem timestamp após o HTML; os outros módulos só recebem timestamp após sucesso. Falhas parciais preservam os timestamps anteriores e os dados salvos.

Materiais, aulas, justificativas, reposição e PDFs SUAP usam o repositório/coordenador. Aulas deixou de implementar a busca dentro do composable: DisciplineContentRepository mantém a mesma leitura/extração e persistência. HTML de justificativas é carregado por readDocument; downloads e POSTs que já usam OkHttp passam por executeRaw. APIs externas ao host SUAP continuam bloqueadas nesse transporte.

Imagens em Avisos/Perfil obtêm o cookie pelo ponto de acesso central, sem logar valores. O gerenciamento de credenciais permanece em CredentialStore. O método gov.br impede recuperação HTTP e login silencioso automático com senha.

## Requisições e logins

Os testes usam fixtures/interceptadores locais, sem conta real. Os números anteriores abaixo vêm da inspeção dos efeitos da versão anterior, não de tráfego medido em um aparelho. Requisições de imagens, recursos internos de WebView e os downloads gerenciados pelo Android não entram no contador de documentos do repositório.

| Cenário | Antes (inspeção) | Depois (teste/contrato) |
|---|---|---|
| Abertura e gatilhos repetidos enquanto sincroniza | Não havia dono compartilhado | 1 pipeline; gatilhos concorrentes reutilizam o estado |
| Abrir Horários após sincronização | 1 busca de horário + descoberta e busca do calendário (pelo menos 3 GETs) | 0 requisições de dados na entrada |
| Abrir Avisos após sincronização | 1 GET por entrada | 0 requisições de dados na entrada |
| Alternar Horários/Avisos durante sincronização | Podia acrescentar as buscas acima com cookies ainda indisponíveis | 0 requisições, cache presente e mensagem Sincronizando |
| Alternar abas após sincronização | As buscas de entrada se repetiam | 0 requisições |
| Atualizar módulo com sessão pronta | Busca própria da tela | Pipeline do módulo; segunda tentativa manual antes de 60s não inicia trabalho |
| Duas requisições de detalhe rejeitadas | Duas falhas, sem recuperação compartilhada | 1 fluxo de login, 4 requisições: 2 originais + 2 repetições |
| Worker: dois módulos sempre rejeitados | Retry periódico sem recuperação limitada | 1 login HTTP no máximo, 3 requisições no teste (original + repetição + segundo módulo) |
| Worker gov.br | Falha/retry | 0 logins HTTP e sem retry por expiração |

`SYNC_METRICS` registra somente contadores acumulados do processo, sem URLs, credenciais, cookies ou HTML. O contador de login inclui tentativas de fluxo de autenticação, não comprova que o servidor aceitou credenciais. O total de requisições reais do login WebView e de uma abertura completa depende dos redirecionamentos/HTML do portal e não foi medido neste ambiente.

## Cookies e prazo da sessão

CookieManager aceita cookies explicitamente; o cookie jar e o caminho de sucesso do LoginScreen executam flush. Isso solicita persistência no armazenamento do WebView. A presença de um cookie não é usada como prova de autenticação: a resposta protegida é validada.

Não foi verificada a sobrevivência de uma sessão real após morte do processo neste ambiente. Cookies de sessão e cookies persistentes podem ter políticas diferentes, e o servidor também pode revogar sessões. Sem conta real e observação dos atributos de expiração do servidor, não é possível afirmar o tempo real de validade. Nenhum prazo foi inventado; rejeição HTTP/página de login é tratada como fonte de verdade.

## Código duplicado ou sem adoção completa (preservado)

- AuthRepository/AuthViewModel continuam como alternativa histórica ao login WebView; não foram apagados.
- AvisosViewModel, AulasViewModel, BoletimViewModel, AlmocoViewModel e ThemeViewModel permanecem para avaliação da migração; a UI principal usa SuapViewModel e fluxos próprios existentes.
- ScheduleRepository/AvisosRepository e outros repositórios históricos ainda sobrepõem parte do trabalho do SuapViewModel; consolidação integral não foi feita de uma vez.
- Downloads feitos pelo DownloadManager ainda são executados pelo Android, fora do transporte coordenado. SIGAA mantém autenticação própria; uma sessão SUAP não autentica o SIGAA. Esses caminhos exigem validação complementar e não são tratados como se fossem requisições SUAP.

## Limites e suposições

- Nenhum parser ou fórmula acadêmica foi alterado. A apresentação foi preservada, além dos estados e textos de sincronização solicitados.
- Não houve teste com uma conta SUAP real, nem medição histórica real antes/depois no mesmo aparelho.
- O teste de rotação é coberto pelo contrato de estado compartilhado/deduplicação; não comprova quantas requisições internas uma WebView recriada fará após rotação.
- Erros de POSTs não são repetidos quando não indicam rejeição de sessão; não se presume que um erro de transporte autorize repetir um envio.


## Referências aos ViewModels candidatos

- AuthViewModel: nenhuma referência externa à definição nos fontes principais.
- AvisosViewModel: nenhuma referência externa à definição nos fontes principais.
- AulasViewModel: nenhuma referência externa à definição nos fontes principais.
- BoletimViewModel: nenhuma referência externa à definição nos fontes principais.
- AlmocoViewModel: nenhuma referência externa à definição nos fontes principais.
- ThemeViewModel: nenhuma referência externa à definição nos fontes principais.

## Validação final

`assembleDebug` e `testDebugUnitTest`: BUILD SUCCESSFUL, 98 testes, zero falhas/erros. O teste Compose com renderização nativa verificou cache preservado e zero requisições ao alternar Horários/Avisos durante e após sincronização. Os testes com interceptadores verificaram recuperação compartilhada de detalhes e classificação dos erros. Testes puros cobriram cooldown, falhas que exigem ação, timestamps parciais e orçamento de recuperação do worker.
