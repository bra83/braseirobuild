# R54 P15 — EMBEDDED TTS CONSISTENCY

Data: 2026-09-18
Base: R54 P14 Full Green

## Correção real
A fonte React de Configurações já havia sido corrigida no P11, porém o `r46-shell.bundle.js` efetivamente embarcado ainda continha a mensagem legada de que narração/TTS estavam indisponíveis. Isso podia aparecer antes/fora do enhancement de runtime e contradizia o TTS funcional.

O bundle embarcado agora informa corretamente Android PT-BR, Gemini/Charon, Web Speech e encadeamento multibloco. Foi adicionado gate permanente `tests/tts-bundle-status-p15.test.mjs`.

## Validação
- Gate focal TTS: 5/5 PASS.
- Regressão canônica `tests/*.test.mjs`: 782/782 PASS, 0 fail, 0 skipped.
- TTS multibloco P9 preservado.
- Mundo Vivo P9 preservado.
- Stress P14 permanece como último stress canônico 6/6 PASS; esta correção é apenas de texto/UI embarcada e não toca motor de campanha.

## Auditoria de modelos Gemini em 2026-09-18
Os defaults já presentes no projeto continuam atuais: `gemini-3.8-flash` para Mestre, `gemini-3.1-flash-image` para imagem e `gemini-3.1-flash-tts-preview` para TTS. Nenhuma troca foi necessária.

## Ainda exige ambiente real
- latência acústica e continuidade auditiva Charon/Gemini com chave/rede reais;
- build/instalação e reprodução em aparelho Android;
- substituição adicional de esquemas por mapas locais reais somente quando houver correspondência confiável.
