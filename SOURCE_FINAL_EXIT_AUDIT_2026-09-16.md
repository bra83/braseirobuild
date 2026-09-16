# BRASEIRO WWN — Source Final Exit Audit — 2026-09-16

## Resultado

`SOURCE_FINAL_EXIT_GATE = PASS_WITH_ANDROID_RUNTIME_EXTERNALS_DEFERRED`

O critério deste gate é o projeto-fonte jogável e coerente. Gradle, APK/AAB, emulador/dispositivo e serviços/hardware externos estão explicitamente fora deste ambiente e não foram declarados como validados.

## Bloqueantes reais encontrados após o gate anterior e corrigidos

1. **Dialeto de classe do personagem inicial** — o seed de produção ainda normalizava Expert no identificador legado `FULL_EXPERT`, enquanto progressão/chassis SRD atuais usam `EXPERT`. Isso foi demonstrado por falha real de subida de nível (`INVALID_CLASS_PATH`). O seed e o bootstrap agora usam o dialeto atual, com aliases legados seguros apenas para migração de Expert/Warrior.
2. **Atlas com autoridade de fixture no fallback de navegador** — `scr014.js` ainda possuía um runtime próprio baseado em fixture. O fallback agora nasce do mesmo starter bootstrap de produção; modos QA permanecem explicitamente isolados.
3. **Importação portátil aceitava classe mecanicamente inválida** — o validador agora exige classe atual reconhecida, valida Adventurer/partials, converte aliases legados seguros e recusa `FULL_MAGE` ambíguo em vez de inventar tradição.
4. **Rotas quebradas do Diário** — Regras e Combate apontavam para `jornada.html#diario`, âncora inexistente. Ambos usam `diario.html`, a rota canônica.
5. **Texto de protótipo visível na Ficha** — referência a “fixture persistida” foi removida da UI de produção.

## Validação funcional/regressiva

- **145/145** arquivos JavaScript: sintaxe PASS.
- **31/31** suítes `.mjs` em `qa/`: PASS.
- **66 sessões simuladas acumuladas**: 33 de fechamento + 33 profundas, PASS.
- `production-starter-e2e-regression.mjs`: starter real → perícia → encontro → combate → XP/progressão, PASS.
- `starter-production-seed-regression.mjs`: `EXPERT`, chassis atual e progressão, PASS.
- `manual-creation-production-regression.mjs`: builder manual → finalização SRD atual, PASS.
- `portable-character-class-regression.mjs`: roundtrip atual, migração segura e recusas ambíguas, PASS.
- `navigation-consistency-regression.mjs`: navegação local/Diário, PASS.
- `final-source-release-audit.mjs`: source-only, JSONs, rotas e autoridade de produção, PASS.

## Auditoria estática independente

- **398** imports ESM locais analisados: **0** alvo ausente e **0** import nomeado sem export correspondente.
- **15** JSONs em assets de produção: parse PASS.
- **14** HTMLs: referências literais `href/src` locais existem; nenhuma rota/recurso local ausente.
- Pares HTML/JS foram auditados para IDs literais; o único aparente ausente (`txt_breadcrumb`) é criado dinamicamente pela própria Ficha, portanto não é quebra de DOM.
- Bridge nativa: **38** métodos do `BraseiroNative` usados pelo JS, **45** `@JavascriptInterface` expostos no Kotlin, **0** método usado sem implementação nativa correspondente.
- Telas de produção auditadas contra autoridade Wave/fixture. SCR-015/SCR-016 e `core/wave3-runtime.js` permanecem isolados como legado/QA e não são rota/estado autoritativo de produção.

## Integridade do pacote source-only

O gate final recusa:

- APK/AAB;
- `build/`, `.gradle/`, `.idea/`, `.kotlin/`;
- `local.properties`;
- `.env` / `.env.*`;
- `.jks` / `.keystore`.

Nenhum desses artefatos faz parte da entrega final.

## Classificação das pendências

### Bloqueantes de fonte conhecidos

**Nenhum identificado** após as regressões e a auditoria estática independente descritas acima.

### Fora deste gate

- sync/build Gradle e compilação Android;
- APK/AAB;
- execução WebView em aparelho/emulador;
- seletor de arquivos/SAF e PDFBox reais;
- TTS/áudio e dados físicos 3D no dispositivo;
- Gemini com credencial/rede reais.

Esses itens dependem do runtime Android ou de serviços externos e não foram apresentados como PASS.
