# Braseiro Star Wars VTT — R54 P144 (11/10/2026)

## Base e preservação

Entrada integral: `BRASEIRO_STAR_WARS_EDGE_R54_P143_SOURCE_CHECKPOINT.zip` (SHA-256: `38d744ce17d1e4a90b0bdae67458e7db9600f85e85cb5bb361adefe82fa7d04d`). Nova versão: `versionCode=10144`, `versionName=1.0.144`, shell offline `data-version=1.0.144`. Bundle offline regenerado dos fontes canônicos, **77 módulos**. Nenhum código Java Android nem módulo TTS foi alterado; conferência byte a byte contra P143.

## Código implementado

1. **Persistência transacional integrada:** `GameController.atomicAction()` agora agrupa mudanças de sessão, mundo, cena, mestre, personagem, combate, resultados e UAE numa única gravação persistente. Em falhas de adaptador, validação ou gravação, o estado do controlador volta ao snapshot anterior; saves intermediários não são expostos. Aplicado ao início das cinco aventuras, escolhas e recompensas das cinco, adjudicação de perícias publicadas, início de encontros publicados e finalização de combate (inclusive genérico).
2. **AoR Core — nove posições mecânicas adicionais:** Dotes de mando (4 posições; remove revés de Liderança/Frieza), Observador (1; remove revés de Percepção/Vigilância e reduz tempo de busca contextual), Decifrador (2; remove revés e reduz dificuldade de decifração explicitamente declarada), Mateiro (2; remove revés de deslocamento ambiental e reduz tempo de viagem contextual). O motor só aplica efeitos condicionais quando o Mestre/bridge declara o contexto; outras perícias não recebem bônus.
3. **Conexão real:** `purchaseTalent` AoR → `talentRankByOriginal` → `talentSkillPoolModifiers` / `talentTaskTimeSpec` / `codebreakerDifficultyReduction` → `GameController.resolveSkillCheck` → persistência → Android `BraseiroUiBridge.resolveSkillCheck` (já encaminha options). As novas opções são `codebreaking`, `outdoors` e `searchingArea`. Não houve mudança visual na UI de rolagem: contextos devem ser fornecidos pelo fluxo do Mestre/bridge.
4. **Política anti-invenção:** *Falar Binário* continua pendente porque exige bônus **aos testes de droides comandados**, não um bônus falso à Liderança do jogador. *Além da Orla* continua sem ficha de combate oficial certificada; não foram fabricados perfis de inimigos ou recompensas.

## Evidência executada

- Integração/regressão regular: **1637/1637 PASS** em 258 arquivos (+20 testes P144 sobre os 1617 do P143).
- Stress: **6/6 PASS**; total **1643/1643**.
- Testes focais P144: **20/20 PASS** (cinco aventuras × início/rollback e escolha/rollback; perícias em duas; combate publicado e rollback; compra real de talentos AoR; contexto e persistência).
- Kokoro/TTS: **27/27 PASS** (código, não áudio real no aparelho).
- JS: **126 arquivos PASS**; JSON: **25 PASS**; XML: **2 PASS**; Java sintático: **8 arquivos PASS**, com 699 símbolos Android não resolvidos sem SDK.
- Bundle: **77 módulos PASS**; verificações em `evidence/P144/`.

## Pendências reais

- AoR: **224/360 posições** com efeito executável; **136 posições pendentes**. A matriz mede posições, não talentos distintos. Falta confrontar integralmente o efeito de cada posição com os PDFs originais.
- EotE/AoR expansões: certificação integral ainda não feita.
- Cinco aventuras: fluxos e persistência testados, mas falta conferência integral de cenas/encontros/recompensas contra PDFs. Combate oficial de *Além da Orla* não implementado/certificado.
- Android: sem Android SDK e sem APK compilado/homologado em S23 Ultra/A57 nesta execução. Os testes Node/Java sintático **não** substituem instalação real.

## Retomada

P144 é a base a preservar. Priorizar fonte oficial para 136 posições AoR, completar combate publicado de *Além da Orla*, homologar conteúdo das cinco aventuras, executar Gradle/runner BRELGA e testar instalação, reaberturas e Kokoro no dispositivo. **Não declarar 100% até esses gates passarem.**
