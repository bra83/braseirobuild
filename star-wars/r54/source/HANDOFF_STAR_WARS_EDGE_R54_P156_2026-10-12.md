# Braseiro Star Wars VTT — R54 P156 | 12/10/2026

## Baseline, checkpoint e versão

- Base recuperada: `BRASEIRO_STAR_WARS_EDGE_R54_P155_SOURCE_CHECKPOINT.zip`, SHA-256 verificado antes da extração.
- Nova versão Android Studio: `app/build.gradle.kts` → `versionCode=10156`, `versionName=1.0.156`; shell offline 1.0.156.
- Bundle gerado das fontes canônicas `.mjs`, 79 módulos.
- Entrega: `BRASEIRO_STAR_WARS_EDGE_R54_P156_SOURCE_CHECKPOINT.zip` — projeto Android Studio completo, não APK.
- P155 permanece intacto e recuperável. Nenhuma das oito fontes Java de TTS foi modificada.

## Implementação integrada P156

### Além da Orla — quarto encontro e dados conferidos no PDF

- Conferido **Beyond the Rim.pdf**, Drive ID `1z-GpNm5QU1RGJSUdrP4vYbfBExTlF531`, p. impressa 29 e episódio III (aprox. pp. 84–87).
- Corrigido `yiyar_salvager`: perícias de grupo oficiais, pistola blaster, vibrofaca, propriedades de arma e bônus de armadura do texto.
- Criados `talo_yiyar` (perícias, Adversary 1 e armas textuais cotejadas) e `trandoshan_yiyar_mercenary` (perfil funcional provisório).
- Adicionado **quarto encontro publicado**, `btr-yiyar-scrapheap-raid`, na cena `dropoff-point`, com grupo Yiyar, Talo e mercenário trandoshano. Combate Android, iniciativa, encerramento, histórico e efeitos persistentes de vitória/retirada/incapacitação; encerramento neutro não cria vitória.
- As características, limiares e proteção total dos NPCs não estão integralmente certificados: campos provisórios identificados nos próprios perfis. O novo encontro continua **provisório** quanto às fichas estatísticas.

### Contagem oficial da operação imperial — Android + UAE

- Implementados os marcos de **5, 10, 15, 25 e 30 minutos** da seção *Imperial Deployment* do episódio III.
- Novo relógio explícito na cena `imperial-interruption`, com botão Android PT-BR **AVANÇAR 5 MINUTOS**. Tempo não avança automaticamente ao renderizar, narrar, rolar ou reabrir.
- Cada avanço é validado e persistido com recibo único, limites de 1–30 minutos, detecção de adulteração e eventos não duplicáveis. Ao atingir 30 minutos, há consequência de cerco e exposição imperial 6/6.
- Integração com **WebView durable outbox** (`published-command-client.js`), `executePublishedAdventureCommand(kind='imperial-clock')`, recibos do controlador, prova causal no journal UAE, reconciliação do snapshot e commit atômico. ACK perdido após gravação reabre e recupera a ação sem repetir o avanço.
- Ação inválida, recibo divergente, combate ativo e falha de persistência são recusados com rollback.

### Integridade das cinco aventuras e combate

- Corrigida a finalização de início de combate publicado para registrar o contexto em evento `COMBAT_ACTION`, em vez de atribuição direta sem evento.
- Bloqueado avanço de cena publicada durante **qualquer** combate ativo, mesmo se o contexto publicado estiver ausente.
- Cinco aventuras revalidadas com início, escolha e três reaberturas cada; nenhuma aventura anterior foi substituída.

## Testes efetivamente executados

- **1981/1981** integração e regressão Node (271 arquivos): `evidence/P156/NODE_REGULAR.txt`.
- **6/6** stress: `evidence/P156/NODE_STRESS.txt`.
- **28/28** testes focais P156, incluindo perda de ACK WebView, replay idempotente, recuperação, integridade causal, fonte do livro, quarto encontro e rollback: `evidence/P156/P156_FOCAL_TESTS.txt`.
- **27/27** Kokoro/TTS (código): `evidence/P156/KOKORO_TTS_REGRESSION.txt`.
- **8/8** fontes Java verificadas por gate de sintaxe; símbolos Android não resolvidos sem SDK: `evidence/P156/JAVA_SYNTAX.txt`.
- `tools/audit-p156-release.mjs`: **allPassed=true**, 5 aventuras, 4 encontros, 79 módulos, 8 fontes Java preservadas e cobertura AoR inalterada.
- Fonte editorial conferida e limites: `evidence/P156/BEYOND_RIM_PDF_CROSSWALK.md`.

## Pendências explícitas

1. **AoR: 254/360 posições mecânicas (106 restantes)**; nenhuma nova posição AoR foi homologada em P156. Expansões EotE/AoR ainda não certificadas integralmente.
2. **Cinco aventuras:** ainda sem comparação editorial completa de cada cena, adversário e resultado com os PDFs. *Além da Orla* tem quatro encontros jogáveis, mas estatísticas parciais/provisórias.
3. **APK:** tentativa local `bash gradlew :app:assembleDebug --offline --no-daemon` falhou antes da compilação por `UnknownHostException: downloads.gradle.org`. Não há APK produzido/homologado nem teste físico de instalação/reabertura no Galaxy S23 Ultra/A57.
4. **Kokoro/TTS:** 27 testes de código passaram; reprodução real de áudio em Android não foi executada.

## Reproduzir

```bash
node tools/build-ui-bridge-bundle.mjs
node tools/audit-p156-release.mjs
node --test tests/uae-p156-imperial-clock-source-raid.test.mjs
python tools/run-node-suite.py --regular
python tools/run-node-suite.py --stress
node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs
node tools/java-syntax-gate.mjs
```

Próxima prioridade: resolver as 106 posições AoR com os livros oficiais, cotejar os blocos estatísticos gráficos de *Beyond the Rim* e demais aventuras, compilar/homologar APK no runner Android com Gradle/SDK disponível. P156 é checkpoint recuperável, não homologação final.
