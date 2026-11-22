# Tutor Clínico Android — protótipo funcional 0.1.0

Projeto de aplicativo Android offline criado a partir do inventário de funcionalidades auditadas do Tutor Clínico de Psicanálise, em 22/11/2026. Este projeto é **uma implementação independente**, não o código do site original. O material didático foi resumido e reescrito; não inclui cópia integral da biblioteca de terceiros.

## O que já está implementado no projeto

- Navegação com Início, Casos, Agenda, Estudos e Ajustes; interface adaptativa para telefone Android.
- Cadastro de casos por apelido/iniciais em quatro etapas (identificação, queixa/14 temas, história, sinais de atenção), edição e consulta de anamnese.
- Oito áreas de consulta por caso: Síntese, Investigar, Técnicas, Próxima sessão, Plano 30 dias, O que estudar, Sessões e Anamnese.
- Registro de sessões com data, relato, intervenções, próximos passos e histórico de versões ao retificar (sem apagar a versão anterior).
- Separação entre status do caso e alertas de atenção.
- Agenda **real e nova**, com caso vinculado, data, hora, duração, status (Agendado, Realizado, Falta, Cancelado) e bloqueio de sobreposição de horários.
- Busca local por caso, queixa, história e conteúdo das sessões, além de técnicas de estudo.
- 15 técnicas com notas introdutórias originais, 14 temas e 5 perguntas frequentes originais (não chatbot).
- PDF via sistema de impressão do Android, com anamnese e sessões.
- Importação de JSON legado exportado pelo site auditado; os registros importados são normalizados e sessões recebem identificador próprio.
- Backup completo com senha própria, PBKDF2-HMAC-SHA256 e AES-256-GCM; importação não destrutiva por IDs.
- Armazenamento local cifrado AES-256-GCM pelo Android Keystore e acesso ao aplicativo por PIN local (mín. 6 dígitos).
- Bloqueio de captura de tela (FLAG_SECURE) e nenhuma permissão de rede. Backup automático do Android desabilitado.

## IMPORTANTE: o que este projeto NÃO é

A versão 0.1.0 é **um protótipo para testes com dados fictícios**. Não passou por compilação e testes de dispositivo nesta entrega, nem por pentest, inspeção de acessibilidade e proteção de dados, testes de recuperação contra falhas, validação clínica ou auditoria jurídica/LGPD. **Não use dados reais de pacientes.**

Não existem nesta versão: login multiusuário, sincronização entre aparelhos, notificações, lembretes push, pagamentos, assinatura digital, anexos, supervisor remoto, atendimento remoto ou IA generativa. O plano de 30 dias é informativo; as sugestões não realizam avaliação ou diagnóstico clínico.

## Como gerar o APK no GitHub sem instalar SDK no computador

1. Crie um repositório privado no GitHub e coloque o **conteúdo desta pasta** na raiz do repositório (incluindo `.github/workflows`). Não envie arquivos com pacientes reais.
2. Abra o menu **Actions** do repositório.
3. Escolha **Android APK (debug)** e **Run workflow**.
4. Ao terminar, abra o build e faça download do artifact **TutorClinico-debug-apk**.
5. Extraia `app-debug.apk` desse artifact e instale em seu Android como versão de **teste**.

O workflow depende de serviços externos de compilação e Android SDK disponibilizados pelo GitHub. Não foi executado nem certificado nesta entrega. Os arquivos de teste são todos fictícios.

## Como abrir no Android Studio

1. Instale Android Studio e abra a pasta `TutorClinicoAndroid` como projeto Gradle.
2. Use JDK 17, Android SDK Platform 35 e Build Tools 35.0.0.
3. O projeto usa Android Gradle Plugin 8.7.3 e Gradle 8.9. Configure Gradle 8.9 na IDE se solicitado (o ZIP não inclui o JAR do Gradle Wrapper).
4. Aguarde o sincronismo e execute **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. APK de depuração (quando compilado): `app/build/outputs/apk/debug/app-debug.apk`.

Sem Gradle e SDK instalados no ambiente de criação, não foi possível gerar um APK aqui. Os testes de lógica em Node foram executados, mas não equivalem a um teste do aplicativo Android.

## Testes da lógica

Requer Node.js >=20:

```bash
node qa/test.mjs
```

Testa: estado inicial, validação de datas, escape HTML, cadastro de casos, retificação de sessões, conflitos de agenda, agendamento consecutivo, leitura do backup fictício do site original e serialização do estado.

## Arquitetura

- `app/src/main/java/br/com/tutorclinico/MainActivity.java`: camada Android, PIN, WebView isolada, exportação/importação pelo seletor de arquivos, impressão Android.
- `app/src/main/java/br/com/tutorclinico/SecureStore.java`: criptografia do arquivo interno e dos backups externos.
- `app/src/main/assets/index.html`: interface responsiva e lógica dos formulários, com armazenamento passado exclusivamente pela ponte nativa. O app Android **não grava dados clínicos em localStorage**.
- `docs/AUDITORIA_TUTOR_CLINICO.md`: auditoria fornecida pelo usuário e utilizada como referência de comportamento.
- `.github/workflows/android-debug.yml`: pipeline opcional para construir um APK debug.

A integração da ponte JS `NativeVault` com uma página local precisa ser revisada contra as melhores práticas de segurança de WebView antes de um produto clínico real. A chave interna do Keystore é protegida pelo dispositivo, mas não é derivada do PIN; o PIN é uma barreira de interface, não a única medida de segurança. O PDF e dados exibidos ou importados podem sair do armazenamento cifrado e exigem cuidado.

## Regras de migração

- JSON legado é **texto puro** e portanto inseguro para compartilhamento não protegido.
- O importador recebe um array de casos de 19 campos (conforme a auditoria) e converte `sessoes:[{data,texto}]` para o novo formato com ID e lista de retificações.
- Importações são por mesclagem: preserva o existente quando já há ID igual e adiciona somente IDs novos.
- Backups `.psicbackup` têm senha independente do PIN e não oferecem recuperação sem senha.
- Dados não se transferem automaticamente para outro dispositivo e podem se perder ao desinstalar o aplicativo. Guarde o backup criptografado.

## Antes de usar com pacientes reais

Executar todos os itens de `docs/CHECKLIST_PRE_PRODUCAO.md`, obter avaliação jurídica e de privacidade adequada à LGPD, revisar o uso clínico com profissional habilitado, testar restauração real e segurança em dispositivo físico. Implementar atualização de app e recuperação operacional antes de uma utilização profissional.
