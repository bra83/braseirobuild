# Relatório: Gates de Áudio e Narração Universal TTS V3

Data: 26 de Outubro de 2026  
Status: **100% IMPLEMENTADO & APROVADO**  
Componentes: `UniversalTtsV3Engine.kt`, `VoiceSettingsScreen.kt`, `NarrationTextChunkerTest.kt`, `UniversalTtsV3EngineTest.kt`

---

## 1. Arquitetura do Universal TTS V3

O subsistema de áudio foi concebido para entregar narração imersiva, natural e sem falhas mecânicas em qualquer dispositivo Android (da API 24 à API 36).

### Pilares de Engenharia:
1. **Triplo Suporte de Motores:**
   - `SYSTEM_DEFAULT`: Utiliza o motor TTS nativo do Android (Google TTS ou Samsung TTS nativo do Galaxy A57), detectando automaticamente a melhor voz em Português do Brasil.
   - `KOKORO_SHERPA`: Integração preparada para o motor neural Sherpa ONNX Kokoro offline de altíssima fidelidade.
   - `MOCK`: Fallback resiliente para testes unitários automatizados sem hardware de áudio conectado.
2. **Sanitização de Texto Narravel:**
   - Remove automaticamente markdown (`**negrito**`, `*itálico*`, cabeçalhos `###`, marcadores `•`, links de mapas, notas entre colchetes como `[RESERVADO AO MESTRE]`).
   - Converte abreviações e símbolos para pronúncia natural em português.
3. **Fatiamento Inteligente (Chunking 350-800 caracteres):**
   - Evita travamentos de buffer e timeout do sintetizador nativo do Android.
   - Quebras estritamente em limites semânticos de pontuação (`.`, `!`, `?`, `;`, `\n`).
4. **Gerenciamento de Foco e Ducking de Áudio:**
   - Solicita `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`.
   - Reduz suavemente a trilha sonora de fundo durante a fala do Mestre e restabelece o volume original após a conclusão.

---

## 2. Painel de Controle e Modulação (`VoiceSettingsScreen.kt`)

- **Seleção de Motor:** Alternância dinâmica entre Nativo e Kokoro.
- **Detecção de Vozes:** Lista todas as vozes PT-BR instaladas no aparelho com indicadores de qualidade.
- **Modulação em Tempo Real:** Sliders para Velocidade (0.7x a 1.5x) e Tom/Pitch (0.7x a 1.3x).
- **Auto-Play de Cenas:** Toggle para narrar automaticamente novas cenas ao entrar.
- **Teste Imediato e Interrupção:** Botões de teste e parada com feedback visual instantâneo.

---

## 3. Verificação de Portões e Testes Automatizados

- `TEST-com.braseiro.talesvtt.audio.UniversalTtsV3EngineTest`:
  - `testSanitizeNarrationText`: Higienização de sintaxe e markdown aprovada.
  - `testChunkNarrationText`: Divisão semântica de frases longas respeitando limites de caracteres.
  - `testSpeechRateAndPitchRange`: Validação das faixas e limites operacionais de pitch e rate.
- `TEST-com.braseiro.talesvtt.components.NarrationTextChunkerTest`:
  - `testChunkingBoundaries`: Preservação de integridade de orações.
