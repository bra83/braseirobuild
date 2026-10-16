# HANDOFF STAR WARS EDGE R54 P91 — 2026-09-24

Baseline: P90 fonte real materializada do Google Drive. Projeto não recriado. GitHub não usado para correção.

## Objetivo do bloco
Aplicar a auditoria funcional rápida: verificar caminho real UI → implementação → estado → persistência → efeito visível → teste, corrigindo defeitos seguros e agrupando o build Android apenas no marco final.

## Correções
- Implementado `GeminiBridge.openGeminiScenePrompt(...)`, que o botão ILUSTRAÇÃO já chamava mas não existia.
- Adicionado share-back Android real para imagens: Manifest `ACTION_SEND image/*`, MainActivity `singleTop` + `onNewIntent`, roteamento para cena ou NPC.
- Criado `ExternalImageShareState` persistente para sobreviver a recriação da Activity/processo e preservar o alvo correto de cena/NPC.
- Retorno de imagem de cena e NPC é processado de forma assíncrona; executor do NPC é encerrado no lifecycle.
- TTS migra configuração antiga `com.braseiro.kokoro.ptbr`/DB Next-gen para Sherpa atual instalado (`com.k2fsa.sherpa.onnx.tts.engine` ou `.debug`).
- Sherpa é apresentado como `Braseiro Kokoro PT-BR · neural`; vozes continuam vindo somente de `TextToSpeech.getVoices()`.
- Preservado o pipeline neural existente: blocos naturais 250–650 (~450), próximo bloco após onDone real, prewarm e parâmetros Kokoro.
- Gradle wrapper deixou de depender de github.com e agora usa `downloads.gradle.org` com SHA fixado.
- Versão alinhada para `1.0.91` / `versionCode 10091` / `r54-p91`; bundle regenerado de 61 módulos.
- Agendamento `Star Wars VTT Horário` foi realmente pausado (`is_enabled=false`).

## Evidência
- Focal Android/share/Kokoro: 6/6 PASS.
- Auditoria high-risk: 256/256 PASS.
- Sob um Sol Negro: 106/106 PASS; grafo 17/17 alcançável, zero links quebrados.
- Além da Orla: 57/57 PASS; grafo 26/26 alcançável, zero links quebrados.
- Regressão completa: 1025/1025 PASS, 0 FAIL em 4 shards (268 + 228 + 249 + 280).
- Manifest XML parse PASS; sanity estrutural dos Java alterados PASS; runtime/bundle `node --check` PASS.

## Build Android
Tentado após agrupar as correções, como marco final. A primeira tentativa comprovou que o wrapper P90 ainda apontava para `github.com` e falhou com `UnknownHostException: github.com`. O wrapper foi corrigido para `https://downloads.gradle.org/distributions/gradle-8.13-bin.zip`. A segunda tentativa falhou antes de compilar com `UnknownHostException: downloads.gradle.org`. Portanto o código Android NÃO está declarado compilado/PASS neste ambiente; o bloqueio restante é conectividade/DNS para obter o Gradle 8.13, e não GitHub.

## Pendências reais
- Executar `assembleDebug` em ambiente com Gradle 8.13 disponível/rede e corrigir qualquer erro de compilação caso apareça.
- Em aparelho Android: validar Gemini → Compartilhar → Star Wars para cena e NPC individual, incluindo persistência após recriar Activity.
- Em aparelho com Sherpa/Kokoro instalado: validar migração de preferência, lista real de vozes e narração longa contínua.
- Gemini remoto real continua dependente de app/rede/credencial e não foi declarado PASS.

Relatório detalhado: `evidence/P91/P91_AUDIT_REPORT.md`.
