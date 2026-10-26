# RELATÓRIO DE ACEITAÇÃO E FECHAMENTO — BRASEIRO OSE 1.0.10 FINAL

**Destinatário:** Engenharia e Gestão do Projeto BRASEIRO  
**Data:** 24/10/2026  
**Status da Versão:** 1.0.10 (versionCode 11)  
**Ambiente:** BRELGA (Windows 11, JDK 17 JBR, Android SDK 35)  
**Assinatura:** `braseiro-stable-debug.keystore` (Alias `androiddebugkey`)  
**Package Name:** `braseiro.ose`

---

## 1. COMPROVAÇÃO DO ARTEFATO GERADO

- **APK Gerado:** `BRASEIRO_OSE_1.0.10_FINAL.apk`
- **Tamanho:** `102.135.923 bytes` (97,4 MB)
- **SHA-256:** `ee180a4651c783aa9c00d974caebea402fdc63fe32d5719fd048f7aaf7a5dd22`
- **Validação com `apksigner verify --verbose`:**
  - `Verifies: true`
  - `Verified using v2 scheme: true`
  - `Verified using v3 scheme: true`
  - `Signers: 1`
- **Badging `aapt dump badging`:**
  - `package: name='braseiro.ose' versionCode='11' versionName='1.0.10'`
- **Disponibilização Local e Drive:**
  - `D:\brelga-ose-1.0.9-final\publish\BRASEIRO_OSE_1.0.10_FINAL.apk` (e `BRASEIRO_OSE_1.0.9_FINAL.apk`)
  - `D:\brelga-ose-1.0.9-final\publish\BRASEIRO_OSE_1.0.10_FINAL.apk.sha256`
  - `G:\Meu Drive\BRASEIRO_OSE_1.0.10_FINAL.apk`
  - `C:\Users\bcruz\Downloads\BRASEIRO_OSE_1.0.10_FINAL.apk` (e `BRASEIRO_OSE_1.0.9_FINAL.apk`)

---

## 2. RESULTADOS DOS TESTES E SUÍTES

| Suíte de Testes | Quantidade | Aprovados | Falhas | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Motor de Vida V2.2 (Node test runner)** | 144 | 144 | 0 | **PASS (100%)** |
| **Testes Unitários Gradle Android (`:app-android:testDebugUnitTest`)** | Todos | Todos | 0 | **BUILD SUCCESSFUL** |
| **Persistência Room e Migrações (`:persistence-room:testDebugUnitTest`)** | Todos | Todos | 0 | **BUILD SUCCESSFUL** |
| **Certificação em Galaxy A57 (Dispositivo Físico)** | — | — | — | **NOT_TESTED (Transparência obrigatória)** |

---

## 3. RESOLUÇÃO DOS ITENS P0 E BUGS B01–B12

1. **B01 — Cancelamento na Criação de Personagem:**
   - Adicionada mensagem de bridge `"CharacterCreationCancel"`.
   - Adicionado botão "CANCELAR CRIAÇÃO" na tela de rascunho de atributos e na tela de loadout de equipamento inicial.
   - O rascunho é purgado de `creationDrafts` no banco sem corromper o estado da campanha.

2. **B02 e B03 — Exportação/Importação de Ficha de Personagem:**
   - A exportação em JSON agora inclui o campo `portraitUrl`.
   - A rotina de importação detecta colisões de ID de personagem ou itens e gera identificadores canônicos distintos, preservando equipamentos e retrato sem rejeitar o arquivo nem quebrar a chave primária.

3. **B04 — Menu de Foto / Retrato do Personagem e NPCs:**
   - O clique no botão da câmera agora exibe menu claro de confirmação para escolher imagem da galeria ou manter a existente, permitindo substituição sem ambiguidade.

4. **B05 — Mesa Física e Dados 3D Integrados:**
   - Incorporado o motor 3D de física realista (`dice3d_core.js`, `dice3d_app.js` e texturas `dice/d*.png`).
   - Rola dados físicos no canvas `#stage` dentro de overlay modal quando a rolagem é acionada, usando exclusivamente o RNG determinístico autoritativo do OSE.

5. **B06 — Limpeza de Bordas e Molduras de Itens:**
   - Removido excesso de linhas pesadas nas classes `.item`, `.item-thumb` e `.equipment-art` no `p1.css`, garantindo acabamento visual limpo e moderno.

6. **B07 — Roteamento Universal de TTS V3 sem Falhas Silenciosas:**
   - Adicionada interrupção explícita de áudio em ambos os motores (`tts.stop()` e `geminiTts.stop()`) antes da troca de configuração em `TtsSettingsSet`.
   - Estado de áudio e erros são reportados na interface sem fallbacks silenciosos.

7. **B08 — Proteção contra Crash de Memória em Imagens da Câmera:**
   - Limite do `PresentationMediaStore` elevado para 20 MB com decodificação segura via `runCatching`.
   - No cliente web (`p1.js`), implementado redimensionamento preliminar via `<canvas>` (1024×1024, JPEG 85%), reduzindo fotos pesadas de celulares (10–25 MB) para cerca de 150–250 KB antes do tráfego pelo bridge.

8. **B09 — Importação de Aventuras JSON sem Bloqueio de WorldId:**
   - `MainActivity.kt` passa `expectedWorldId = null` para novas aventuras importadas, permitindo que pacotes canônicos com mundos próprios (`val-do-inverno-canonico`, `costa-do-mago-canonico`) sejam aceitos.
   - Botões de importação vinculados universalmente via `querySelectorAll`.

9. **B10 — Origem Dinâmica e Coerente no Bootstrap do Mundo:**
   - A seleção do hexágono inicial agora busca dinamicamente áreas de planície/vila ou POI em vez de fixar estaticamente `(3, 2)`.

10. **B11 — Diagnóstico da Narração Gemini vs Local:**
    - `GeminiBarbaraSupervisor` grava `NarrationReceipt` com provider (`GEMINI_TEXT` vs `LOCAL`), modelo, status e motivo de fallback.
    - O viewState projeta `narrationReceipt` e a interface exibe badge claro indicando a procedência da narração.

11. **B12 — Compactação de Navegação e Eliminação do Espaço Vazio no Galaxy A57:**
    - Altura da barra inferior compactada para 58 px, padding zerado e remoção do `safe-area-inset-bottom` duplicado que causava espaçamento morto de 40–50 dp no rodapé de dispositivos Android.
    - Cabeçalho compactado para ~52 px.

12. **Motor de Orientação ao Jogador ("Aprender Jogando"):**
    - Módulo `OSEPlayerGuide.kt` integrado com tópicos completos de regras (Atributos, CA, PV, Turnos de Masmorra, Tochas, Viagem nos Ermos, Auxiliares, Rodadas de Combate).
    - Botão `?` no cabeçalho abrindo modal contextual com alternância entre Modo Aprendiz e Modo Experiente.
    - Botão `"💡 O QUE POSSO FAZER AGORA?"` apresentando sugestões oficiais contextuais conforme a fase e localização da companhia, com preenchimento assistido de ações sem consumir RNG nem alterar o mundo.

---

## 4. MOTOR DE VIDA V2.2 — AUTORIDADE CAUSAL

- Eliminada a heurística de busca por palavras como `'ouro'` no texto do jogador.
- Toda repercussão nasce exclusivamente de evento canônico confirmado (`mechanicalMutation === true`), respeitando intenção mecânica autorizada pelo árbitro OSE.
- Ações não confirmadas são estritamente ignoradas pelo motor biográfico.
