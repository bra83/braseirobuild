# R54 P10 — STRESS VALIDATION

Data: 2026-09-18
Base: R54 P9 TTS + Mundo Vivo

## Correção no executor de regressão

`tools/run-node-suite.py` usava timeout de 75 s por arquivo. A suíte `stress-33-sessions-r15` pode exceder esse teto sob contenção do host, produzindo falso TIMEOUT mesmo quando passa isoladamente. O limite por arquivo foi elevado para 180 s; a execução continua sequencial para stress, sem remover asserts nem mascarar falhas.

## Stress executado isoladamente

- stress-33-aftermath-r20: PASS — 1/1
- stress-33-faction-plan-r19: PASS — 1/1
- stress-33-sessions-r15: PASS — 1/1
- stress-faction-plans-r19: PASS — 1/1
- stress-factions-r18: PASS — 1/1
- stress-persistent-adversary-r17: PASS — 1/1

**Stress total: 6/6 PASS.**

A tentativa de executar toda a suíte regular + stress em um único processo ultrapassou a janela total disponível do host; isso não foi convertido em PASS. Os seis stress foram então executados individualmente com seus resultados reais.

## Revalidação focal P9

TTS multibloco, lifecycle, bridge nativa e Mundo Vivo foram reexecutados juntos após a alteração do runner: **20/20 PASS**.

## Estado

Nenhuma mecânica de Star Wars, asset, fallback ou NPC fixo foi reintroduzido. Esta revisão altera somente a robustez do executor de testes e registra prova de stress real.

## Ainda depende de ambiente externo

- latência acústica real Charon/Gemini: chave válida + rede + aparelho;
- reprodução acústica de todos os blocos: aparelho Android;
- APK: Android SDK/Gradle funcional.
