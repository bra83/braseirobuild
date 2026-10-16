# Braseiro Star Wars VTT — R54 P150 | 12/10/2026

## Base canônica

- **Entrada:** `BRASEIRO_STAR_WARS_EDGE_R54_P149_SOURCE_CHECKPOINT.zip` (SHA-256 `8e836a55fc3fe52d680821cf80f39cafca92c90f2151482b550457400e680386`).
- **Saída:** `BRASEIRO_STAR_WARS_EDGE_R54_P150_SOURCE_CHECKPOINT.zip` — projeto **completo** para Android Studio, NÃO é APK.
- Android `versionCode=10150`, `versionName=1.0.150`; shell `data-version=1.0.150`; bundle offline recompilado com 79 módulos.
- Os oito arquivos Java Android, incluindo Kokoro/TTS, permanecem byte a byte idênticos aos de P149 (manifest SHA em `evidence/P150/JAVA_P149_BASELINE_SHA256.json`).

## Implementação P150 — código integrado

1. **AoR — sete posições mecânicas ativadas** (229 → **236/360**, restam **124**):
   - `PROGRAMACIÓN DEFENSIVA` (3 posições) → `DEFSLI`: +1 Contratempo por graduação quando o personagem defende sistema que possui/programou contra invasão. Não se aplica a testes de Computadores do jogador nem a sistemas não protegidos.
   - `PROGRAMACIÓN DEFENSIVA MEJORADA` (1) → `DEFSLIIMP`: **substitui** os Contratempos por melhorias da Dificuldade, uma por graduação de Programação Defensiva; não acumula os dois bônus.
   - `ESCURRIDIZO` (3) → `TRICK`: a nave pilotada pelo personagem conta com silhueta um ponto menor **somente ao ser atacada**, mínimo 0. Não reduz silhueta real, não se acumula por múltiplas compras.
2. **Computadores, invasão recebida:** novo `resolveIncomingComputerIntrusionCheck` com pool positivo do NPC, Dificuldade do sistema adjudicada, talentos contextuais, dados do adversário quando identificado, transação atômica, histórico persistido e rollback em falha. UI PT-BR e WebView bridge expostos. O sistema protegido exige confirmação explícita; por padrão o talento não é aplicado.
3. **Combate veicular de duas vias:** `resolveIncomingVehicleAttack` permite ataque real do inimigo à nave selecionada, usando atributos/perícia explicitamente informados pelo Mestre; integra Escurridizo, alcance, munição limitada, cadência, defesa, dano, efeitos, log, uma ação inimiga por rodada e salvamento atômico. `activateIncomingVehicleCritical` permite gastar os resultados reais, manter a rolagem de d100 ao solicitar componente e impedir ativação duplicada; `declineIncomingVehicleCritical` permite dispensar o crítico. UI Android com controles para Agilidade/Artilharia do inimigo e crítico pendente. **Não inventa fichas oficiais de NPC**.
4. **UAE + cinco aventuras:** `recordPublishedCommandProof` fornece entrada dedicada à prova causal do controlador. `dispatchEvent` e `commitAction` genéricos não podem falsificar a origem `controller` nem o evento `PUBLISHED_COMMAND_COMMITTED`. Recibos e journal permanecem compatíveis com P149. O gate P150 testa START, escolha, replay sem escrita e três reaberturas em cada aventura.
5. **Regressão histórica:** três testes antigos foram ajustados para manter as asserções de não implementação apenas sobre talentos que **ainda** estão pendentes, sem apagar as verificações das funcionalidades P137–P149. Shell e Gradle sincronizados.

## Gates executados nesta rodada

- **Node regular:** `1803/1803 PASS` (264 arquivos, **28 testes P150**).
- **Stress:** `6/6 PASS` (6 arquivos). **Total Node: 1809/1809 PASS**.
- **Kokoro/TTS:** `27/27 PASS` (subconjunto de regressão, incluído na suíte regular; sem áudio físico).
- **Cinco aventuras:** `5/5` passam preflight estrutural/semântico, START, escolha, prova causal, idempotência e três reaberturas no gate `tools/audit-p150-release.mjs`.
- **Bundle offline:** 79 módulos, rotas novas presentes; `node tools/java-syntax-gate.mjs` PASS (análise de sintaxe, não compilação Android).
- **8/8 Java Android:** hashes iguais aos de P149; fonte de TTS não modificada.
- **Gate geral legado `tools/verify-final-source.mjs`: NÃO certificado** nesta execução — excedeu limite de execução. Suites Node, stress, TTS, Java e preflight foram executadas separadamente e passaram.
- Evidências: `evidence/P150/` (NODE_REGULAR, NODE_STRESS, KOKORO_TTS_REGRESSION, JAVA_SYNTAX, RELEASE_AUDIT, PUBLISHED_PREFLIGHT, P150_FOCAL_TESTS, SHA Java).

## Pendências — não declarar 100%

- **AoR: 124 posições ainda sem consumidores mecânicos**, apesar da aquisição disponível. Expansões EotE/AoR sem certificação integral contra PDFs.
- **Cinco aventuras:** integração estrutural e transacional testada, mas não cotejada integralmente contra PDFs oficiais (cenas, testes, encontros, recompensas, bifurcações). Combate oficial de *Além da Orla* continua pendente de fonte verificada.
- **APK não compilado**, sem instalação/reaberturas físicas Galaxy S23 Ultra/A57, sem reprodução Kokoro no aparelho. Android SDK e runner BRELGA não foram utilizados nesta rodada.
- Ataque veicular inimigo utiliza atributos de NPC fornecidos pelo Mestre; a automatização de ficha/IA de piloto inimigo e outras manobras ofensivas permanece trabalho futuro.

## Reproduzir

Na raiz do ZIP: `node tools/build-ui-bridge-bundle.mjs`, `node tools/audit-p150-release.mjs`, `python tools/run-node-suite.py --regular`, `python tools/run-node-suite.py --stress`, `node --test --test-reporter=tap tests/r54-p76-tts-kokoro-pipeline.test.mjs tests/r54-p91-android-share-kokoro-audit.test.mjs tests/tts-*.test.mjs`, `node tools/java-syntax-gate.mjs`. Para APK, usar Android Studio com SDK/Gradle ou runner Android conectado. **Retomar de P150, não P149.**
