# ECO — recuperação incremental e formulários clínicos — 25/11/2026

Base: 9cdfd55, execução 45610043159 concluída com sucesso (lógica, Chromium, runtime/modelos, Gradle APK debug). Publicação de APK atualizável com chave permanente não executada.

## Alterações desta rodada
- 38bc1ce / f293e9e / f13e6a6 / 50cb244: checkpoint de ASR de gravação própria autenticada em limite PCM de 50 ms, separado do último timestamp de palavra; só avança após confirmação dos IDs de segmentos do prefixo. Recuperação de clusters de falantes cifrada com chave temporária da gravação dentro do cofre; exclusão lógica do checkpoint e chave na mesma transação terminal. Áudio externo ou ausência de checkpoint continuam com replay conservador. A leitura PCM ainda percorre o prefixo para autenticação; evita repetir inferência do prefixo, não promete seek instantâneo.
- 4d004a6 / 200b62b: regressões sintéticas de clusters, reabertura SQLite, chave/modelo divergentes, idempotência e limpeza terminal.
- 47bd0cd / 02905f8 / 1d5a896: rascunhos cifrados de anamnese e enquadre, escopo por paciente e formulário, revisão explícita de divergências, backup/restore sem sobrescrever rascunho local.
- 2e22d89 / 56339c8: testes de persistência e interface Chromium, isolamento entre pacientes e salvamento explícito.

## Limitações
- A fila completa ainda não é independente da Activity; a recuperação por job não equivale a processamento paralelo de múltiplas sessões.
- Checkpoints de ASR usam material acústico temporário; não constituem identificação biométrica. Exclusão lógica não é garantia de apagamento físico do armazenamento flash.
- Outros formulários clínicos (sonhos, genograma, cronologia etc.) ainda precisam de journaling próprio. Backup portátil permanece limitado em tamanho.
- Validação bibliográfica, licenças, auditoria de segurança independente e avaliação de qualidade clínica ainda não fechadas.
- Testes físicos adiados pelo titular. Não certificar 100% por contagem de commits ou por build debug.

Execução de referência dos novos commits: 45625074698 (lógica aprovada, demais etapas ainda não confirmadas no momento do registro).
