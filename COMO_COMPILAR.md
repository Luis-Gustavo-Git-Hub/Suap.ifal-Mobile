# Compilação do projeto revisado

## Android Studio

1. Extraia o ZIP e abra a pasta que contém `settings.gradle.kts`.
2. Use uma versão do Android Studio compatível com o Android Gradle Plugin configurado em `gradle/libs.versions.toml` e selecione o JDK do Gradle nas configurações da IDE.
3. Instale o SDK Android API 37, usado pelo projeto, e aguarde a sincronização do Gradle.
4. Gere o APK da variante `debug`.

O projeto inclui o Gradle Wrapper. O caminho do SDK da sua máquina deve ser configurado pelo Android Studio ou em seu próprio `local.properties`.

## Terminal

Com JDK e Android SDK configurados, na pasta do projeto:

Linux/macOS:

```sh
./gradlew :app:assembleDebug
```

Windows (PowerShell):

```powershell
.\gradlew.bat :app:assembleDebug
```

O APK de debug é gerado em `app/build/outputs/apk/debug/app-debug.apk`.

Para executar os testes locais:

```sh
./gradlew :app:testDebugUnitTest
```

No Windows, substitua `./gradlew` por `.\gradlew.bat`.

## Assinatura e serviços

- O debug usa a chave local padrão do Android se não houver `debug.keystore` na raiz do projeto. Se você usa outra assinatura, confira também sua configuração de login Google/Firebase.
- A configuração Firebase existente foi preservada em `app/google-services.json`. O arquivo `.env.example` continua disponível para configuração do projeto.
- Para assinar uma versão de release, configure `KEYSTORE_PATH`, `STORE_PASSWORD` e `KEY_PASSWORD`. O alias configurado é `upload`. Sem a chave de release, essa variante permanece sem assinatura.

## Revisão incluída

- Preservação dos dados acadêmicos na inicialização e de favoritos durante a sincronização.
- Atualizações de favoritos, URLs e médias restritas aos campos correspondentes, evitando sobrescritas e a recriação de disciplinas removidas.
- Validação das respostas HTTP e do resultado do login; renovação de sessão compartilhada entre requisições concorrentes.
- Leitor de boletim compartilhado entre a interface e o serviço em segundo plano; rejeição de páginas incompatíveis.
- Gravação transacional do calendário e remoção de dados fictícios usados como substitutos automáticos.
- Indicadores de atualização, mensagens de erro, novas tentativas e melhorias de teclado e acessibilidade no login.
- Limpeza local que aguarda o encerramento das tarefas acadêmicas, remove os caches locais e desconecta a conta Google sem apagar os dados sociais na nuvem.
- Sincronização de notificações limitada a uma janela diária flexível do WorkManager, com cancelamento do alarme exato legado e verificação segura da permissão Android 13+.
- Cache offline de materiais e avisos encontrados durante a sincronização, limite de 40 disciplinas em cache e limite de 1 MiB para o log interno.
- Retomada das aulas salvas quando a rede falha e ícone compatível com notificações do Android.
- O parser de aulas deixou de reter HTML bruto redundante, que não era usado pelas telas.
- A busca das quatro médias por etapa agora usa concorrência limitada a duas requisições e atualiza somente as colunas de médias que mudaram.
- A busca do dashboard pré-normaliza os campos pesquisáveis, e os cálculos/ordenações dos cards de estatísticas são memorizados entre recomposições.
- A aba Estatística ficou acessível na navegação principal e compara médias do estudante e da turma por etapa e disciplina, informando quando a média da turma não existe.
- Testes de regressão para leitura do boletim, autenticação, atualizações parciais e observação do calendário.

## Validação neste ambiente

A tentativa de executar `:app:testDebugUnitTest` foi interrompida pelo Gradle antes da compilação: `JAVA_HOME` não está definido e o comando `java` não está instalado. Portanto, a compilação, os testes automatizados e a execução em Android ainda precisam ser realizados no seu ambiente. As verificações estáticas e a integridade do ZIP foram conferidas na entrega.
