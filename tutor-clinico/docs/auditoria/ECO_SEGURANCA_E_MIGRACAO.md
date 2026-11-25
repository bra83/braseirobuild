# ECO — Arquitetura de Segurança, Privacidade e Migração

**Versão do Sistema:** `0.19.0-arm64-beta` (build 24)  
**Data:** 24/11/2026  
**Identificador Android:** `br.com.tutorclinico.offline`  
**Conformidade:** LGPD (Lei 13.709/2018), Código de Ética CFP, OWASP MASVS/MASTG v2.0  

---

## 1. Modelo de Ameaças e Isolamento

O projeto ECO é uma estação clínica autônoma offline para profissionais de saúde mental e psicanálise. O modelo de segurança foi concebido para eliminar riscos de vazamento de prontuários em caso de furto, perda, inspeção não autorizada ou adulteração física do dispositivo.

### 1.1 Vetores Mitigados
1. **Ausência Absoluta de Conexão à Internet**: O manifesto Android (`AndroidManifest.xml`) **não contém** a permissão `android.permission.INTERNET`. Todas as operações (reconhecimento de voz, diarização, indexação vetorial e persistência) operam estritamente no processador ARM64 local.
2. **Proteção de Interface e Memória**: `FLAG_SECURE` (`WindowManager.LayoutParams.FLAG_SECURE`) ativa em toda a aplicação, impedindo capturas de tela, espelhamento indevido e geração de screenshots nos aplicativos recentes do sistema operacional.
3. **Criptografia em Fluxo do Áudio Temporário (Spool)**: O áudio PCM durante a gravação é gravado no spool cifrado com `AES-256-CTR` com chave efêmera de 256 bits gerada no início da sessão e mantida estritamente na memória volátil (RAM).
4. **Cabeçalho Binário Estruturado do Spool (B04)**: O arquivo de áudio temporário é prefixado com cabeçalho autenticado de 32 bytes:
   - Magic: `ECO1` (4 bytes ASCII: `0x45, 0x43, 0x4F, 0x31`)
   - Versão: `1` (uint32 Little-Endian)
   - IV: 16 bytes aleatórios (`SecureRandom`)
   - Comprimento Esperado: uint64 Little-Endian (8 bytes) atualizado ao finalizar a gravação.
5. **Sanitização Contra Nivelamento de Desgaste Flash (Wear-Leveling)**: Sobrescrever arquivos com zeros em memória flash não garante a remoção física de blocos. A sanitização via `SessionRecorder.wipeAndClean(spoolFile, ephemeralKey)` executa:
   - Sobrescrita de todo o arquivo físico com bytes nulos (`0x00`).
   - Sincronização de hardware forçada via `fileDescriptor.sync()`.
   - Limpeza matemática da chave de decodificação na RAM (`Arrays.fill(ephemeralKey, (byte) 0)`).
   - Exclusão do arquivo. Sem a chave efêmera, blocos físicos remanescentes na flash tornam-se matematicamente indecifráveis.
6. **Decodificação em RAM sem WAV Claro em Disco (B04)**: O decodificador `AudioFileTranscriber.decodeSpool()` decifra o spool diretamente em buffers de memória RAM e alimenta o motor de transcrição sem jamais materializar arquivos de áudio desprotegidos no armazenamento.
7. **Protocolo Nativo de ACK de Persistência (B05)**: A exclusão do arquivo temporário é terminantemente proibida até o recebimento formal e durável de persistência emitido pelo serviço nativo (`persistenceAckReceived == true` e confirmação no banco SQLite).

---

## 2. Banco de Dados Transacional e Integridade Relacional (B01, B02, B03)

O ECO v0.19.0 implementa arquitetura relacional SQLite nativa em modo WAL (`PRAGMA journal_mode=WAL`) e integridade referencial ativa (`PRAGMA foreign_keys=ON`) sob o esquema versionado `DB_VERSION = 2`.

### 2.1 Esquema e Proteção Anti-Cascade (B01, B03)
- **Tabela Base `_pacientes` e View `pacientes`**:
  - `_pacientes`: `id TEXT PRIMARY KEY NOT NULL`, `created_at INTEGER NOT NULL`, `updated_at INTEGER NOT NULL`, `payload_enc BLOB NOT NULL`.
  - **Zero Plaintext**: A coluna `nome` em texto claro foi completamente eliminada da tabela física. Nomes e dados identificadores residem exclusivamente no envelope autenticado AES-GCM (`payload_enc`).
  - `VIEW pacientes`: Projeta `CAST(NULL AS TEXT) AS nome` para compatibilidade com queries SQL legadas.
  - `Triggers INSTEAD OF`: Triggers de interceptação traduzem operações `INSERT` ou `INSERT OR REPLACE` em operações `UPSERT` seguras (`ON CONFLICT(id) DO UPDATE SET ...`), eliminando a execução de `DELETE` interno do SQLite e protegendo 100% das tabelas filhas contra deleção em cascata acidental durante salvamento.

### 2.2 Integridade Composta e Isolamento de Casos e Jobs (B02)
- **`sessoes`**: Possui restrição `UNIQUE(id, case_id)`.
- **`processamento_jobs`**: Restrição de unicidade composta `UNIQUE(job_id, case_id, session_id)` e chave estrangeira `FOREIGN KEY (session_id, case_id) REFERENCES sessoes(id, case_id) ON DELETE CASCADE`.
- **`segmentos`**: Chaves estrangeiras compostas duplas:
  - `FOREIGN KEY (job_id, case_id, session_id) REFERENCES processamento_jobs(job_id, case_id, session_id) ON DELETE CASCADE`
  - `FOREIGN KEY (session_id, case_id) REFERENCES sessoes(id, case_id) ON DELETE CASCADE`
  Impede categoricamente qualquer contaminação cruzada de segmentos entre casos clínicos distintos.
- **`rascunhos`**: `case_id TEXT PRIMARY KEY REFERENCES _pacientes(id) ON DELETE CASCADE`.
- **`diario_privado`**: `case_id TEXT PRIMARY KEY REFERENCES _pacientes(id) ON DELETE CASCADE`. Segregado de relatórios externos e exportações de prontuário.
- **`configuracoes` (B03)**: `chave TEXT PRIMARY KEY, valor_enc BLOB, updated_at INTEGER`. Armazena preferências globais do aplicativo (`app_state_meta`), assegurando que o banco SQLite seja a fonte única da verdade.

### 2.3 Criptografia Autenticada
- **Provedor**: `AndroidKeyStore`.
- **Chave Mestre**: `eco_master_key` (`AES/GCM/NoPadding`, 256 bits) vinculada ao hardware seguro do dispositivo.
- **IV Único**: Vetor de inicialização de 12 bytes gerado com `SecureRandom` para cada payload.

---

## 3. Migração Sem Perdas e Compatibilidade

1. **Migração Transacional v1 -> v2 (B01)**:
   - Em bases existentes (versão 0.14 a 0.18), `EcoDatabaseHelper.onUpgrade()` invoca `upgradeToV2()`.
   - Executa transação atômica em 12 passos com desativação temporária de chaves estrangeiras, recriação das tabelas com novas FKs, cópia integral dos dados, verificação com `PRAGMA foreign_key_check` e reativação de chaves estrangeiras.
2. **Preservação de Módulos Estruturados**:
   - `normalizeImported` garante a recuperação integral dos 12 módulos psicanalíticos (`anamnese`, `enquadre`, `investigacaoAxes`, `hipotesesList`, `intervencoesList`, `genograma`, `cronologia`, `sonhosList`, `seguranca`).
3. **Backup Criptografado (.psicbackup)**:
   - Derivação via `PBKDF2WithHmacSHA256` (240.000 iterações, salt de 16 bytes).
   - Criptografia simétrica autenticada via `AES-256-GCM`.
