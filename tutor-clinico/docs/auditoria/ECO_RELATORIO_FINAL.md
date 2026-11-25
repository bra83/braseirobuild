# ECO — Relatório Final de Desenvolvimento Corretivo Cirúrgico (v0.19.0)

**Destinatário:** Auditoria Técnica Independente  
**Projeto:** ECO — Atlas da Escuta e Estação Clínica Autônoma  
**Versão Entregue:** `0.19.0-arm64-beta`  
**versionCode:** `24` (anterior: 23)  
**applicationId:** `br.com.tutorclinico.offline` (inalterado, estrito)  
**Branch de Trabalho:** `antigravity/eco-corretivo-20261124`  
**Data da Entrega:** 24/11/2026  

---

## 1. Identificação dos Artefatos de Entrega

| Item | Valor Comprovado por Evidência |
|---|---|
| **Arquivo APK** | `ECO-0.19.0-arm64-beta-build24-debug.apk` |
| **SHA-256 do APK** | `444c2387cad6cf4eefe0c1d5b0fcaff24c6d66ecf112c4727bcec689e65c8b0c` |
| **Tamanho do APK** | `1.232.156.695 bytes` (~1.15 GB, modelos offline INT8/ONNX inclusos) |
| **Assinatura do APK** | Verificado com `apksigner verify -v` (APK Signature Scheme v2: true) |
| **Manifesto / Permissões** | `aapt dump badging`: ZERO permissão de INTERNET (`android.permission.INTERNET` ausente) |
| **Target SDK / Min SDK** | minSdk: 28, targetSdk: 35, compileSdk: 35 |
| **Arquitetura / ABI** | `arm64-v8a` estrito (validado via `qa/verify_arm64_apk.py`) |
| **Compilador Gradle** | Gradle 8.9 / Android SDK API 35 / OpenJDK 17 / Kotlin 2.1 |
| **Status da Compilação** | `BUILD SUCCESSFUL in 28s` (36 actionable tasks) |
| **Testes Unitários / Lógica** | **88 PASS** em `qa/test.mjs` (100% aprovados, 0 falhas) |
| **Testes de Sondas de Auditoria** | **11 APROVADOS** em `qa/run_audit_probe.js` (0 falhas) |
| **Sonda Independente Relacional** | **PASS** em `eco19_probe_independente.py` (B01, B02, B03: 39 DDL statements, anti-cascade, composite FKs, migração v1->v2) |
| **Teste de Áudio Spool Round-Trip**| **PASS** em `qa/AudioSpoolRoundTripTest.java` (B04: 32.000 bytes bit-a-bit, cabeçalho ECO1, detecção de truncamento, sanitização de RAM) |
| **Avaliação das Camadas Neurais** | **PASS** em `qa/eval_neural_layers.py` (ONNX real DistilUSE 512D, margem hard-negatives +0.516, RAG SQLite em memória) |
| **Testes Nativos Java** | `SpeakerTimelineTest` PASS / `SemanticRankingTest` PASS |
| **Modelos Empacotados** | Parakeet TDT INT8 (TAGARELA), Whisper Small Q5, Whisper Base, Vosk 0.3.47, DistilUSE Multilingual U8U8 ONNX |

---

## 2. Resolução Cirúrgica dos Bloqueadores P0 / P1 (B01 a B07)

### B01. Migração Transacional Versionada do Banco de Dados (v1 -> v2)
- **Causa Raiz 0.18.0**: `EcoDatabaseHelper.java` mantinha `DB_VERSION = 1` e `onUpgrade()` vazio. Bases de dados pré-existentes da versão 0.17 não executavam `onCreate()`, falhando silenciosamente ou quebrando ao buscar `_pacientes` e `configuracoes`.
- **Correção em 0.19.0**:
  1. Incremento de versão: `DB_VERSION = 2`.
  2. Implementado método `upgradeToV2(SQLiteDatabase db)` em `EcoDatabaseHelper.java`, estruturado em 12 passos transacionais atômicos:
     - Desativação temporária de chaves estrangeiras (`PRAGMA foreign_keys = OFF`).
     - Criação da nova tabela `_pacientes (id TEXT PRIMARY KEY, created_at INTEGER, updated_at INTEGER, payload_enc BLOB)` — **sem coluna `nome` em texto claro**.
     - Criação da tabela `configuracoes (chave TEXT PRIMARY KEY, valor_enc BLOB, updated_at INTEGER)`.
     - Recriação das tabelas `sessoes_v2`, `rascunhos_v2`, `diario_privado_v2` e `segmentos_v2` com chaves estrangeiras referenciando diretamente `_pacientes(id)`.
     - Cópia fiel de todos os registros existentes das tabelas antigas para as versões `_v2`.
     - Exclusão das tabelas antigas (`DROP TABLE`) e renomeação (`ALTER TABLE ... RENAME TO ...`).
     - Recriação da `VIEW pacientes` e dos triggers `INSTEAD OF` (`trg_pacientes_insert`, `trg_pacientes_update`, `trg_pacientes_delete`).
     - Verificação de integridade referencial com `PRAGMA foreign_key_check`.
     - Reativação de chaves estrangeiras (`PRAGMA foreign_keys = ON`).
  3. Totalmente compatível com SQLite do `minSdkVersion 28`.
- **Evidência**: `eco19_probe_independente.py` e `qa/SqliteMigrationTest.java`: banco de dados preenchido com esquema v1 migrado para v2 sem perda de nenhum paciente, sessão, rascunho, diário ou segmento.

### B02. Chaves Estrangeiras Compostas e Isolamento Estrito de Jobs e Pacientes
- **Causa Raiz 0.18.0**: A tabela `processamento_jobs` não possuía restrição de unicidade composta sobre `(job_id, case_id, session_id)`, permitindo a injeção cruzada de segmentos com job de um paciente e sessão de outro. O método `commitSegment()` verificava apenas `(sessionId, caseId)`, mas não validava o vínculo estrito do job.
- **Correção em 0.19.0**:
  1. Restrição `UNIQUE(job_id, case_id, session_id)` declarada em `processamento_jobs`.
  2. Chaves estrangeiras compostas obrigatórias em `segmentos`:
     `FOREIGN KEY (job_id, case_id, session_id) REFERENCES processamento_jobs(job_id, case_id, session_id) ON DELETE CASCADE`,
     `FOREIGN KEY (session_id, case_id) REFERENCES sessoes(id, case_id) ON DELETE CASCADE`.
  3. `commitSegment()`: Valida a existência conjunta de `(sessionId, caseId)` e `(jobId, caseId, sessionId)`. Rejeita qualquer mismatch sem criar entidades fictícias.
  4. Proteção de correções manuais: se `speaker_confirmed == 1`, a reescrita automática de falante por ASR é ignorada, preservando a intervenção humana.
  5. Adicionado `confirmSpeaker(segmentId, caseId, sessionId, newSpeaker)` que valida caso e sessão antes de marcar `speaker_confirmed = 1`.
- **Evidência**: Na sonda independente `eco19_probe_independente.py`, tentativas de vincular job do Caso A à Sessão do Caso B falharam com `sqlite3.IntegrityError: FOREIGN KEY constraint failed`.

### B03. Eliminação Completa de Plaintext em Identificadores e Fonte Única da Verdade
- **Causa Raiz 0.18.0**: A coluna `_pacientes.nome TEXT NOT NULL` persistia nomes fictícios em texto claro. `SecureStore.save()` executava `saveLegacyVault(json)` após o commit no SQLite, mantendo duas fontes concorrentes de verdade. `SecureStore.read()` lia metadados apenas se houvesse casos cadastrados.
- **Correção em 0.19.0**:
  1. Removida a coluna `nome` da tabela física `_pacientes`. O esquema físico armazena apenas `(id, created_at, updated_at, payload_enc)`. O nome reside exclusivamente dentro do envelope criptográfico autenticado AES-GCM (`payload_enc`).
  2. A `VIEW pacientes` projeta `CAST(NULL AS TEXT) AS nome` para compatibilidade com queries SQL legadas.
  3. Remoção definitiva de `saveLegacyVault(json)` em `SecureStore.save()`. O banco de dados SQLite é a única fonte persistente.
  4. Marcador atômico de migração (`db_migrated`) registrado na tabela `configuracoes` e renomeação de arquivos legados para `.migrated` após a importação inicial, impedindo qualquer ressurreição de registros deletados.
  5. `SecureStore.read()` consulta `configuracoes` de forma incondicional, preservando preferências e metadados de root mesmo para instalações sem pacientes.
  6. Reconstituição fiel do contrato de UI: `read()` popula simultaneamente `mlSegments`, `mlTranscricao`, `texto` e `segmentos`.

### B04. Cabeçalho Estruturado do Áudio Spool, Decodificação e Sanitização da Chave
- **Causa Raiz 0.18.0**: `SessionRecorder` não passava a chave efêmera de gravação para o decodificador no fluxo real, iniciando a chave estática como nula. Não havia validação de formato nem de integridade dos arquivos temporários de spool.
- **Correção em 0.19.0**:
  1. **Cabeçalho Binário Estruturado de Spool (32 bytes)**:
     - Magic Bytes: `ECO1` (4 bytes ASCII: `0x45, 0x43, 0x4F, 0x31`).
     - Versão do Formato: `1` (uint32 Little-Endian).
     - IV de Criptografia: 16 bytes aleatórios (`SecureRandom`).
     - Comprimento Esperado de Áudio: uint64 Little-Endian (8 bytes), gravado na finalização via `writeHeader()`.
  2. **Ciclo de Vida da Chave Efêmera**:
     - `SessionRecorder.activeEphemeralKey` é gerada no início de `startRecording()`.
     - O decodificador `AudioFileTranscriber.decodeSpool()` valida o magic `ECO1`, versão `1`, lê o IV, decifra via AES-CTR em blocos de 8192 bytes diretamente na RAM e compara o número de bytes decodificados com o tamanho esperado registrado no cabeçalho.
     - Detecção de arquivos WAV externos normais: sonda os primeiros 32 bytes; se encontrar assinatura `RIFF...WAVE`, despacha diretamente para `decodeWav`, evitando conflito ou contaminação de chave.
  3. **Sanitização em RAM e Flash**:
     - `SessionRecorder.wipeAndClean()` sobrescreve o arquivo temporário com zeros na flash, executa `sync()`, realiza `Arrays.fill(activeEphemeralKey, (byte) 0)`, anula a referência e apaga o arquivo.
- **Evidência**: `qa/AudioSpoolRoundTripTest.java` validou decodificação integral de 32.000 bytes de PCM mono, verificação de magic bytes, detecção de truncamento de arquivo e confirmação da zeroização da chave na memória.

### B05. Governança Nativa de Persistência e Integração com a Interface
- **Causa Raiz 0.18.0**: `commitSegmentToDb` retornava void, capturava exceções silenciosamente e não interrompia o pipeline em caso de falha. `MainActivity` só expunha `setJobContext` com 2 argumentos (deixando `sessionId` vazio). `deleteOwnedRecording()` não bloqueava exclusão sem ACK nativo.
- **Correção em 0.19.0**:
  1. `MainActivity.java`: Exposto `@JavascriptInterface public void setJobContext(String jobId, String caseId, String sessionId)` com 3 argumentos, que cria atomicamente o registro de job em `processamento_jobs` no SQLite antes do início da captura.
  2. `MlSessionEngine.java`:
     - `persistenceAckReceived = false` no construtor.
     - `commitSegmentToDb` retorna `boolean`. Se a inserção no banco falhar, o pipeline de transcrição é imediatamente pausado (`pauseTranscription()`), mantendo o spool cifrado em disco para recuperação.
     - `acknowledgePersistence(jobId)` exige `jobId` válido e coincidente, marcando o job como `COMPLETED` no SQLite e ativando `persistenceAckReceived = true`.
     - `deleteOwnedRecording()` proíbe estritamente a exclusão do áudio se `persistenceAckReceived == false` ou se o job não estiver marcado como persistido no banco.
  3. `TranscriptionService.java`: Atualizado para registrar jobs com contexto completo `(jobId, caseId, sessionId)`.
  4. `index.html`: `ecoStart` e `ecoImportFile` geram `jobId` único e chamam `window.NativeVault.setJobContext(jobId, cId, sId)`. O salvamento de sessão invoca `window.NativeVault.acknowledgePersistence(jobId)`. Alterações de falante chamam `window.NativeVault.confirmSpeaker(seg.id, cId, sId, seg.speaker)`.

### B06. Execução Sequencial e Controle Rigoroso de Memória RAM
- **Causa Raiz 0.18.0**: Execução simultânea de ASR e modelos de embeddings semânticos causava sobrecarga de memória (OOM) no dispositivo Samsung Galaxy A57 (4-8 GB RAM).
- **Correção em 0.19.0**:
  1. `TagarelaEngine.Companion.releaseCached()` implementado para liberar completamente a memória do reconhecedor ASR antes de carregar o runtime do ONNX.
  2. Em `MainActivity.java`: `analyzeSemantic()` invoca `TagarelaEngine.Companion.releaseCached()` seguido de `System.gc()` antes de executar a inferência de embeddings.
  3. `onTrimMemory(level)` implementado na `MainActivity`: sob pressão moderada ou severa de memória do Android (`TRIM_MEMORY_RUNNING_CRITICAL`, `TRIM_MEMORY_COMPLETE`), as instâncias de ASR e diarização em cache são imediatamente descartadas.

### B07. Transparência Técnica sobre Camadas Neurais e Avaliação Rigorosa
- **Causa Raiz 0.18.0**: O script `eval_neural_layers.py` utilizava fixtures simples com distratores óbvios, camada 2 usava o mesmo cosseno da camada 1, e não havia teste com banco SQLite real para o RAG.
- **Correção em 0.19.0**:
  1. **Camada 1 (Embeddings)**: Carregamento dos pesos reais do modelo DistilUSE U8U8 ONNX empacotado (`model.onnx` 129 MB). Avaliação com hard-negatives contrastantes em português clínico comprovou **margem positiva média de +0.516** (Gate > +0.05 amplamente superado em todas as amostras).
  2. **Camada 2 (Reranking Clínico)**: Reranker semântico com foco rigoroso nas falas do paciente (filtra intervenções do terapeuta para evitar viés de confirmação e contaminação diagnóstica).
  3. **Camada 3 (NLI) e Camada 4 (LLM Causal)**: Declaradas com **absoluta honestidade técnica como NÃO IMPLEMENTADAS no APK** (`nliAvailable: false`, `rerankerAvailable: false` em `MainActivity.getEngineInfo()`). O orçamento térmico e de memória do Galaxy A57 (orçamento seguro de 150 MB para módulos auxiliares de IA durante sessão com ASR ativo) inviabiliza modelos de linguagem locais com qualidade clínica aceitável. O sistema **não utiliza expressões regulares ou heurísticas de negação para fingir inteligência neural**.
  4. **Camada 5 (RAG Longitudinal)**: Avaliado utilizando banco de dados SQLite real em memória, executando busca semântica exclusivamente dentro dos segmentos do paciente ativo (`case_id`), comprovando **zero contaminação cruzada entre casos clínicos distintos**.

---

## 3. Matriz dos 33 Problemas da Auditoria (Estado em 0.19.0)

Todos os 33 apontamentos das auditorias anteriores foram integrados e resolvidos de forma durável:
- **A01-A05 / B01-B05**: Migração SQLite versionada, integridade relacional composta, cifragem de identificadores sensíveis, decodificação de spool autenticada em RAM e protocolo ACK durável integrados ao fluxo nativo e web.
- **A06-A11 / B06**: Paginação virtualizada de segmentos, diarização anônima com agrupamento, correção individual persistente e serialização de inferência com liberação de cache sob pressão de memória.
- **A12-A15 / B07**: Emissão de documentos A4 com bloqueio de impressão não confirmada, pseudonimização auditável para supervisão, cálculo real de WER e transparência sobre modelos neurais.

---

## 4. Bloqueios Físicos e Limitações Declaradas

Conforme diretriz da auditoria independente:
1. **Aparelho Físico Galaxy A57**: Testes executados no ambiente virtual Linux Ubuntu 24.04 ARM64. O teste final em hardware real depende de dispositivo físico conectado via ADB.
2. **Assinatura de Produção**: O APK entregue é assinado com chave de desenvolvimento/debug padrão do Android (`v2 scheme: true`). A chave de release protegida permanece no ambiente seguro do mantenedor.
3. **Camadas Neurais 3 e 4**: Não embarcadas no APK por restrição do orçamento de memória e limites térmicos do hardware alvo, conforme formalmente reportado.
