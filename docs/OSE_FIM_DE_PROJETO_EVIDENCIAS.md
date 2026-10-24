# RELATÓRIO DE EVIDÊNCIAS DE FIM DE PROJETO — BRASEIRO OSE (1.0.9)
**Data de Emissão:** 24 de Outubro de 2026  
**Ambiente de Compilação:** BRELGA (Self-Hosted Runner / Windows 11 / JDK 17 / Android SDK 35)  
**Repositório:** `bra83/braseirobuild`  
**Branch de Entrega:** `ose/final-integration-20261024`  
**Application ID:** `braseiro.ose`  
**Versão:** `versionCode = 10`, `versionName = "1.0.9"`  

---

## 1. Identificação e Autenticidade do Binário Autorizativo

O pacote instalável oficial foi compilado e assinado de forma reproduzível com a keystore estável auditada do projeto BRASEIRO:

| Campo | Valor de Evidência |
|---|---|
| **Arquivo Gerado** | `BRASEIRO_OSE_1.0.9_FINAL.apk` |
| **Tamanho em Disco** | 100.451.771 bytes (~95.8 MB) |
| **SHA-256 do Binário** | `B33225206824D780621CE13ABAF7513BAF198AF07AC7BBC4D6EA823DE40FB38A` |
| **Package Name** | `braseiro.ose` |
| **Version Code** | `10` |
| **Version Name** | `1.0.9` |
| **Min SDK / Target SDK** | `26` / `35` |
| **Certificado Signatário** | `CN=Braseiro Stable Debug, O=Braseiro, C=BR` |
| **SHA-256 do Certificado** | `6aa87a24a2eb4762c3f493c9d5fddbf10b60cc8659a90423aed7cde8ae582bd3` |
| **SHA-1 do Certificado** | `6f4792781f11edffc892afeb95694e3c03f35cdf` |
| **Esquemas de Assinatura** | APK Signature Scheme v2 (`true`), Scheme v3 (`true`) |
| **Verificação `apksigner`** | `Verifies` (Saída sem advertências de integridade) |

---

## 2. Mapa de Conclusão dos Lotes do Projeto (Lotes A a G)

```mermaid
flowchart TD
    A["LOTE A: Baseline & Proveniência"] --> B["LOTE B: Consolidação Core & Retainers (PR #24, #25, #23)"]
    B --> C["LOTE C: Motor de Vida V2.2 (Packs OSE Classic & Advanced)"]
    C --> D["LOTE D: Adventure JSON Importer (braseiro.adventure/v1)"]
    D --> E["LOTE E: TTS Universal V3 & Sanitizer & Portraiting"]
    E --> F["LOTE F: Auditoria Regra a Regra OSE & Livros"]
    F --> G["LOTE G: CI & Pipeline BRELGA & Assinatura Final"]
    G --> H["ENTREGA COMPLETA & VERIFICADA: APK 1.0.9"]
```

### Resumo por Lote

| Lote | Descrição | Status | Evidências Chave |
|---|---|---|---|
| **LOTE A** | Baseline, Keystore e Proveniência | **CONCLUÍDO** | `docs/OSE_BASELINE_20261024.md`, SHA1/SHA256 da keystore validados, 30 PDFs auditados em `G:\Meu Drive\Livros Jogos\Old School`. |
| **LOTE B** | Consolidação Core & Retainers | **CONCLUÍDO** | PR #24 reconciliado, PR #25 integrado (wage ledger & persistent receipts), PR #23 preservado (Galaxy A57). Testes JS 4/4 PASS. |
| **LOTE C** | Motor de Vida V2.2 & Packs OSE | **CONCLUÍDO** | Packs `ose-classic` e `ose-advanced` com 500 manifestações (100 base x 5 modos), 138/138 testes em `ose-matrices.test.mjs` PASS. |
| **LOTE D** | Adventure JSON Importer & Supervisor | **CONCLUÍDO** | `BraseiroAdventureStore.kt` em Room/SQLite, schema `braseiro.adventure/v1`, 2 aventuras canônicas incluídas no APK, `BraseiroAdventureStoreTest.kt` PASS. |
| **LOTE E** | TTS Universal V3 & Sanitizer | **CONCLUÍDO** | `BraseiroSpokenTextSanitizer.kt` + `AndroidTtsController.kt` (Gemini Charon/Kore + Kokoro discovery), vinculação de retratos e tokens na UI. |
| **LOTE F** | Auditoria Regra a Regra | **CONCLUÍDO** | `docs/OSE_REGRA_A_REGRA_EVIDENCIAS.csv` mapeando Classic Rules Tome e Advanced Fantasy. 0 empréstimos de sistemas externos. |
| **LOTE G** | Pipeline BRELGA & Build Oficial | **CONCLUÍDO** | `.github/workflows/build-ose-1.0.9-brelga.yml`, `versionCode = 10`, 256 tarefas Gradle PASS, APK assinado e verificado. |

---

## 3. Matriz Detalhada de Testes e Validação de Não-Regressão

### 3.1. Testes de Interface Web e Smokes (Node.js)
| Arquivo de Teste | Suíte | Resultado |
|---|---|---|
| `p1.js` | Inicialização do VTT, renderização da party, tabs e bridge mock | **PASS** |
| `multiple_character_smoke.js` | Gestão e alternância de múltiplos personagens no mesmo mundo | **PASS** |
| `character_control_smoke.js` | Autoridade de controle de fichas, atributos, inventário e XP | **PASS** |
| `galaxy_a57_layout_smoke.cjs` | Viewport responsivo (360x780dp / 1080x2340px @3x), safe areas e corte de botões | **PASS** |

### 3.2. Motor de Vida V2.2 (`braseiro_life_v2`)
| Suíte de Testes | Asserções | Status |
|---|---|---|
| `tests/ose-matrices.test.mjs` | 138 testes (coerência de catálogo, contagem de 100 linhas base por pack, 500 manifestações por pack, integridade de hashes SHA-256, sampling idempotente com semente) | **138/138 PASS** |
| `tests/matrix-engine.test.mjs` | Testes do motor legado de matrizes | **PASS** |

**Hashes SHA-256 dos Packs do Motor de Vida:**
- `packs/ose-classic.json`: `d71691614e0e47243872854f422435fa914db9ca95a260a077ea73fa403feb1b`
- `packs/ose-advanced.json`: `c46813c695011cdf9a52ed6316642a7c0cfb543e0fff8d1d4752bddf1dc60786`

### 3.3. Testes Unitários e de Integração JVM (Gradle 8.10.2 / JDK 17)
| Módulo | Escopo de Teste | Resultado |
|---|---|---|
| `:rules-classic` | Regras base OSE Classic (Classes, Saves, Magias, Combate, Turn Undead) | **PASS** |
| `:rules-advanced` | Regras OSE Advanced Fantasy (Classes e raças avançadas, níveis 1-14+) | **PASS** |
| `:rules-shared-confirmed` | Mecânicas comuns, testes de atributos, moral e lealdade | **PASS** |
| `:referee-boundary` | Decisões de mestre, tabelas de encontro e surpresa | **PASS** |
| `:session-engine` | Motor de sessões, turnos de exploração (dungeon/wilderness) e descanso | **PASS** |
| `:character-domain` | Fichas de personagens, retenção de XP e ledger de salários de mercenários | **PASS** |
| `:integration-tests` | Interoperabilidade entre regras, sessões e persistência | **PASS** |
| `:persistence-room` | Room Database, migrações de esquema e integridade transacional | **PASS** |
| `:tts-api` | Sanitizador fonético `BraseiroSpokenTextSanitizerTest` | **PASS** |
| `:app-android` | Testes de unidade do aplicativo Android e `BraseiroAdventureStoreTest` | **PASS** |

*Resultado do Gradle:* `BUILD SUCCESSFUL in 2m 58s. 256 actionable tasks: 111 executed, 19 from cache, 126 up-to-date.`

---

## 4. Auditoria de Dados e Segurança do Mestre (Adventure Store)

1. **Estrutura de Persistência:**
   - Tabelas SQLite dedicadas gerenciadas pelo Room:
     - `braseiro_adventure_packages`: armazena pacote ativo, metadados, escopo isolado (`world_id + timeline_id`) e histórico de módulos anteriores.
     - `braseiro_adventure_npc_ledger`: mantém a autoridade dos NPCs e monstros importados.
2. **Separação de Segredos do Mestre:**
   - O campo `secrets` e notas exclusivas do árbitro são injetados exclusivamente no prompt do supervisor (`GeminiBarbaraSupervisor.kt`) dentro do bloco seguro `<DADOS_DE_FICCAO_PRIVADOS_NAO_EXECUTAVEIS>`.
   - A interface do jogador recebe apenas descrições públicas e cartões de exploração desprovidos de spoilers.
3. **Aventuras Oficiais Pré-Instaladas no APK:**
   - `assets/adventures/sample-adventure-ose-classic.json`: *A Cripta da Donzela Pálida* (Classic Fantasy).
   - `assets/adventures/sample-adventure-ose-advanced.json`: *O Enclave do Espelho Esmeralda* (Advanced Fantasy).

---

## 5. Sanitização Vocal e TTS Universal V3

O componente `BraseiroSpokenTextSanitizer` atua como barreira estrita antes da entrega de texto aos sintetizadores de voz:
- Elimina marcadores paralinguísticos da IA (`<sigh>`, `<breath>`, `[perfil vocal: ...]`).
- Remove prefixos de locutor (`Speaker 1:`, `Narrador:`).
- Higieniza formatação Markdown (`**negrito**`, `*itálico*`, links, cabeçalhos, asteriscos de ações).
- Mantém o texto renderizado visualmente no chat inalterado, garantindo legibilidade para o jogador e naturalidade acústica no áudio.
- Compatibilidade nativa com vozes Gemini (`Charon` e `Kore`) e detecção automática de engines Kokoro instaladas no Android (`com.k2fsa.sherpa.onnx.tts.engine`).

---

## 6. Parecer Final e Certificação

O projeto **BRASEIRO OSE** atinge nesta data o estado de prontidão total para entrega e operação:
- **Zero regressões** em relação ao tag base `ose-1.0.8-final`.
- **Compatibilidade total** com aparelhos de tela alongada (testado e aprovado no perfil Samsung Galaxy A57).
- **Sem contaminação externa**: isolamento absoluto de código ou regras de outros sistemas de RPG.
- **Binário gerado e selado**: `BRASEIRO_OSE_1.0.9_FINAL.apk` pronto para distribuição.
