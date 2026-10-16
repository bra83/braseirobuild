# Braseiro Star Wars VTT — R54 P151 | 12/10/2026

## Checkpoint canônico

- **Base real:** `BRASEIRO_STAR_WARS_EDGE_R54_P150_SOURCE_CHECKPOINT.zip`, SHA-256 `c14ce4b7167ed9e60ca70ce35d6b68d2567e10118d44d78d7ca8a722390e5810`.
- **Entrega:** `BRASEIRO_STAR_WARS_EDGE_R54_P151_SOURCE_CHECKPOINT.zip`, projeto integral Android Studio, não APK.
- Android `versionCode=10151`, `versionName=1.0.151`; shell 1.0.151; bundle offline regenerado (79 módulos).
- Java Android/Kokoro: oito fontes preservadas byte a byte, comparadas ao manifesto de P150.

## Bloco integrado P151 — código executável

1. **AoR: +11 posições de talentos com consumidores mecânicos**, de 236 para **247/360** (restam **113**). Novos aliases e efeitos:
   - `ATAJO` (2) / `SHORTCUT`: +1 dado de Bônus por graduação somente em perseguições declaradas (pilotagem ou movimento).
   - `INVENTOR` (2): +1 dado de Bônus por graduação em Mecânica apenas para construir ou modificar equipamentos, não para reparos comuns.
   - `ERUDITO RESPETADO` (2) / `RESSCH`: downgrade de dados vermelhos para roxos ao interagir com instituições de ensino, limitado à interação social explicitamente confirmada; não elimina dados roxos.
   - `HABLAR BINARIO` (2) / `SPEAKBIN`: +1 dado de Bônus por graduação para o teste **do NPC droide dirigido**, com estatísticas declaradas pelo Mestre; nunca aumenta automaticamente a perícia do jogador.
   - `CONDUCTOR TODOTERRENO` (1): ignora contratempos exclusivamente atribuídos ao terreno na pilotagem planetária; preserva demais penalidades.
   - `CONDUCTOR NATO` (1) / `NATDRI`: rerrolagem de Pilotagem planetária ou Artilharia, respeitando o limite já existente de uma vez por sessão.
   - `ASTRONAVEGANTE EXPERTO` (1) / `MASSTAR`: gasta 2 de Tensão para reduzir em 1 a dificuldade de Astronavegação (mínimo 1), inclusive ao calcular curso no veículo.
2. **Skill Engine:** `resolveSkillCheck` inteiro passou a uma transação atômica; alterações de Destino, Tensão, rodada, status e rolagem são confirmadas juntas ou revertidas integralmente. Contextos `chase`, `crafting`, `scholarlyInstitution`, `terrainSetback` implementados e persistidos no check.
3. **NPCs e veículos:** novo `resolveDirectedDroidSkillCheck` com validação rígida dos atributos/perícia/dificuldade e confirmação explícita de NPC droide; `plotHyperspaceCourse` aceita `useExpertTalent`, desconta Tensão e persiste em transação única, inclusive rollback em erro de disco.
4. **Android/WebView:** bridge expõe os novos métodos; controles PT-BR para testes contextuais, ordens a droides e navegação especialista; interface TypeScript atualizada. Bundle offline recompilado com 79 módulos.
5. **Cinco aventuras:** integração P151 exercitada em *Sob o Sol Negro*, *Além da Orla*, *Long Arm of the Hutt*, *Jewel of Yavin* e *Mask of the Pirate Queen*: início idempotente, teste contextual, comando a droide, três reaberturas sem escrita duplicada; preflight estrutural/semântico preservado.
6. **Regressões históricas:** expectativas antigas de talentos pendentes atualizadas somente onde a implementação P151 ativou o consumidor; testes de não implementação continuam cobrindo talentos que realmente permanecem pendentes. Teste P150 de versão alterado para nova versão.

## Validação

- Testes focais P151: **17/17 PASS** (`evidence/P151/P151_FOCAL_TESTS.txt`).
- Regressão dos quatro arquivos históricos afetados: **93/93 PASS** (`evidence/P151/REGRESSION_FIXED.txt`).
- Stress Node: **6/6 PASS** (`evidence/P151/NODE_STRESS.txt`).
- Kokoro/TTS: **27/27 PASS** (`evidence/P151/KOKORO_TTS_REGRESSION.txt`), apenas código.
- Cinco aventuras: **5/5 PASS** no gate `tools/audit-p151-release.mjs`; START, escolha e três reaberturas; sem comparação integral com PDFs (`evidence/P151/RELEASE_AUDIT.json`).
- Sintaxe Java Android: **PASS**, mas não substitui compilação Gradle (`evidence/P151/JAVA_SYNTAX.txt`).
- Regressão Node regular integral: ver `evidence/P151/NODE_REGULAR.txt` para a contagem final.

## Pendências reais — não declarar conclusão

- **AoR: 113 posições** sem consumidor mecânico. Certificação EotE/AoR completa contra os livros ainda não realizada.
- **Cinco aventuras:** validação estrutural e transacional não substitui cotejo de todas as cenas, bifurcações, encontros e recompensas contra os PDFs. Combate oficial de *Além da Orla* continua pendente de conferência.
- **APK não compilado nem testado em dispositivo**; não há validação de instalação, três reaberturas físicas nem reprodução de áudio Kokoro no S23 Ultra/A57.
- Comandos a NPC droides usam estatísticas explicitamente informadas pelo Mestre; fichas e IA de todos os NPCs não foram inventadas.

## Retomada e reprodução

Retomar de P151, não P150. Na raiz do ZIP: `node tools/build-ui-bridge-bundle.mjs`, `node tools/audit-p151-release.mjs`, `python tools/run-node-suite.py --regular`, `python tools/run-node-suite.py --stress`, `node --test tests/uae-p151-integrated-context-talents.test.mjs`, `node tools/java-syntax-gate.mjs`. Para APK, utilizar Android Studio/SDK e runner Android; não inferir compilação a partir do gate de sintaxe Java.
