# Segurança e privacidade

O Suap.ifal Mobile é um cliente comunitário, não oficial, para Android. O app
acessa SUAP e SIGAA em nome do aluno e mantém dados acadêmicos localmente para
uso offline.

## Dados armazenados

- Credenciais do SUAP e SIGAA são cifradas com AES-GCM e uma chave do Android
  Keystore. A matrícula é mantida localmente para identificar a conta.
- A autenticação gov.br acontece na WebView do provedor. O app não lê nem
  armazena CPF, senha ou códigos de verificação gov.br.
- Boletim, frequência, avisos, horários e preferências ficam no banco Room e
  nas preferências privadas do app. A função existente Amigos pode enviar os
  dados que o usuário escolhe compartilhar para o Firebase; esta fase não muda
  esse fluxo nem as regras do Firebase.
- Registros de diagnóstico ficam no armazenamento privado do app. O relatório
  compartilhado remove e-mails, identificadores numéricos, URLs, conteúdo HTML,
  cookies e campos de autenticação; o arquivo temporário tem limite de 128 KiB,
  fica no cache privado e é substituído no próximo compartilhamento.

## Rede e WebViews

- O tráfego sem criptografia está proibido pelo manifesto e pela configuração
  de segurança de rede.
- WebViews remotas usam somente HTTPS em hosts aprovados, sem acesso a arquivos
  locais, conteúdo local ou conteúdo misto. Safe Browsing fica ligado quando o
  Android oferece esse recurso. A depuração fica restrita a builds debug.
- Não há pinning de certificado: SUAP e SIGAA podem rotacionar certificados,
  então um pin embutido poderia bloquear o acesso legítimo. O transporte usa a
  validação TLS padrão do Android.

## Reportar uma vulnerabilidade

Abra um reporte privado de segurança no GitHub do mantenedor antes de publicar
detalhes que possam afetar usuários. Inclua versão do app, impacto e passos de
reprodução, sem anexar credenciais, cookies, boletins ou dumps de log.

## Verificações locais

O CI executa build, testes, lint e OSV-Scanner. Dependências Gradle ficam
travadas em `app/gradle.lockfile` e `baselineprofile/gradle.lockfile`; as ações
do GitHub são acompanhadas pelo Dependabot. Para atualizar os locks após uma
mudança de dependência, resolva a configuração afetada usando `--write-locks`
e revise o diff dos arquivos de lock antes de enviar.
