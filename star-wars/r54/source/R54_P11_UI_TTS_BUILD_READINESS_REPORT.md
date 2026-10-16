# R54 P11 — TTS UI CONSISTENCY + BUILD READINESS

Data: 2026-09-18
Base: R54 P10 Stress Green

## Bug reproduzível corrigido

A fonte React da tela Configurações > Narração/voz ainda exibia a mensagem antiga de que TTS estava indisponível, contradizendo o bridge Android/Gemini/Web já integrado no P9. A tela agora informa corretamente:

- voz PT-BR;
- TTS Android/Gemini;
- áudio em múltiplos blocos;
- fallback Web Speech;
- Gemini/Charon quando configurado.

Foi adicionado gate `tests/tts-ui-status-p11.test.mjs` para impedir a regressão dessa mensagem falsa.

## Validação

- Gate focal TTS/UI/lifecycle: 8/8 PASS.
- Regressão regular completa após a correção: **770/770 PASS** em 147 arquivos.
- As 6 suítes stress permanecem verdes no checkpoint P10 e nenhum código de gameplay/stress foi alterado no P11.
- Nenhum NPC fixo, binding fuzzy ou fallback visual semântico foi reintroduzido.

## Build Android

O projeto contém Gradle wrapper e estrutura Android. Foi tentado `:app:assembleDebug --offline`, mas o wrapper ainda precisa obter a distribuição do Gradle e o runtime bloqueia rede para `services.gradle.org` (`UnknownHostException`). Portanto o APK não foi falsamente declarado como validado.

## Dependências de aparelho/serviço externo

- latência acústica real Gemini/Charon;
- audição física da sequência completa de blocos;
- build APK em ambiente com distribuição Gradle/cache disponível.
