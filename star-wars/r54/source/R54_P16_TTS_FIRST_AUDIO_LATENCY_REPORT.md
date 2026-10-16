# R54 P16 — TTS FIRST-AUDIO LATENCY

Baseline: R54 P15 EMBEDDED TTS GREEN. Projeto não recriado.

## Mudança
- Gemini/Charon TTS: primeiro bloco reduzido de 420 para 300 caracteres.
- Objetivo: reduzir o tempo até o primeiro áudio sem alterar o encadeamento multibloco.
- Blocos seguintes permanecem em 850 caracteres e continuam sendo gerados/reproduzidos em sequência pela fila dedicada.
- Android TTS e Web Speech não foram alterados.

## Validação
- Gate focal TTS/lifecycle/multibloco: 7/7 PASS.
- Regressão regular canônica: 776/776 PASS, 0 fail.
- Stress do motor não foi alterado por esta mudança; a última prova completa permanece 6/6 PASS no P14/P15. Uma nova tentativa de stress neste host excedeu a janela de execução e não foi contada como PASS.

## Verificação externa atual
- Documentação Gemini consultada em 2026-09-18 confirma `gemini-3.1-flash-tts-preview` como TTS atual de baixa latência e confirma suporte a streaming desde 3.1.
- O P16 mantém a API não-streaming por bloco para evitar introduzir uma mudança de transporte de áudio sem teste Android real. O bloco inicial menor reduz a espera dentro da arquitetura já validada.

## Ainda exige aparelho/chave real
- tempo acústico real até o primeiro som com Charon;
- continuidade audível entre blocos sob rede móvel/Wi-Fi;
- instalação/build Android neste host sem distribuição Gradle local.
