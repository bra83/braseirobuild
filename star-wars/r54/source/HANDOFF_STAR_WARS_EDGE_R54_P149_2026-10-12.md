# Braseiro Star Wars VTT — R54 P149 | 12/10/2026

## Baseline e retomada

- **Baseline imutável:** `BRASEIRO_STAR_WARS_EDGE_R54_P148_SOURCE_CHECKPOINT.zip`, fornecido na conversa.
- **Checkpoint promovido:** `BRASEIRO_STAR_WARS_EDGE_R54_P149_SOURCE_CHECKPOINT.zip` (projeto Android Studio completo; NÃO é APK).
- `versionCode=10149`, `versionName=1.0.149`, shell offline `data-version=1.0.149`.
- Manter a implementação de P148: cinco aventuras, journal causal, recibos, rollback de cascatas, preflight semântico, 79 módulos offline.
- Não reverter os oito arquivos Java Android; foram comparados por SHA-256 com P148 e permanecem idênticos.

## Implementação real P149

1. **AoR — cinco posições adicionais ativas:** `NI UN PELO DE TONTO` (3 posições) e `DESORIENTAR` (2 posições). Cobertura mecânica passa de **224/360 para 229/360**; 131 posições continuam pendentes. Sem atribuir efeitos às outras posições.
2. **Defesa social:** `nobodyFoolDefense` aplica upgrades ao pool negativo SOMENTE quando o jogador é alvo de Charme, Coerção ou Enganação. Novo `resolveIncomingSocialInfluenceCheck` usa o pool positivo do NPC e a oposição do jogador, valida perícias/atores, persiste a rolagem e reverte completamente se a gravação falhar.
3. **Combate — Desorientar:** `disorientTalentOption` + `activatePendingDisorientTalent` aplicam estado `disoriented` por ranks comprados, mediante acerto e gasto de **2 Vantagens** do MESMO orçamento compartilhado com críticos e qualidades de arma. Uma ativação por ataque; estado, log e orçamento salvos numa única transação, com recuperação após reabrir e rollback em falha de armazenamento.
4. **UI Android offline:** botão **TALENTO DESORIENTAR** no painel de combate, habilitado somente quando há talento, acerto e Vantagens suficientes; painel **INFLUÊNCIA RECEBIDA** com rolagem de NPC contra jogador. Rotas `BraseiroUiBridge` e contrato `gameBridgeClient.ts` atualizados.
5. **UAE — persistência/segurança:** auditoria do journal rejeita `ADVENTURE_ACTION_COMMITTED` com IDs duplicados ou janelas causais sobrepostas. Pré-validação semântica exige que todas as verificações autoradas das cinco aventuras tenham perícia, dificuldade e propósito válidos.
6. **Gate reproduzível:** `node tools/audit-p149-release.mjs` valida os cinco módulos, recibos idempotentes e três reaberturas por aventura; contabiliza AoR e verifica presença das novas rotas no bundle.

## Testes executados

- **1.775/1.775 testes regulares PASS** (263 arquivos, incluindo 29 testes novos P149).
- **6/6 testes de stress PASS**. Total: **1.781/1.781 PASS**.
- **27/27 testes de regressão Kokoro/TTS PASS**, apenas de código, sem áudio real.
- **5/5 aventuras** passaram preflight de grafo, condições, efeitos, verificações, journal, idempotência e três reaberturas no gate P149.
- **8/8 fontes Java Android idênticas** a P148 por SHA-256. Bundle offline recompilado com **79 módulos**.
- `node tools/java-syntax-gate.mjs`: PASS; não substitui compilação com Android SDK.
- Evidências: `evidence/P149/{NODE_REGULAR.txt,NODE_STRESS.txt,P149_FOCAL_TESTS.txt,KOKORO_TTS_REGRESSION.txt,RELEASE_AUDIT.json,VALIDATION.json}`.

## Pendências reais — não declarar 100%

- **AoR: 131/360 posições mecânicas pendentes**. A aquisição das posições já existia, mas nem todas possuem efeitos executáveis.
- Expansões EotE/AoR **não certificadas integralmente contra os PDFs**.
- As cinco aventuras estão integradas e testadas estruturalmente; **não houve cotejo integral de cenas, testes, recompensas e encontros com os PDFs oficiais**.
- *Além da Orla* permanece sem especificação de combate oficial (`encounters:false`). Não fabricar encontro sem fonte.
- **APK não compilado; sem homologação em Galaxy S23 Ultra/A57 e sem teste de áudio Kokoro no aparelho**. Android SDK/runner de build não utilizado neste ambiente.
- `tools/verify-final-source.mjs` não concluiu no limite de execução; o gate Java e as suítes Node foram executados separadamente.

## Como reproduzir

Na raiz do ZIP: `node build-ui-bridge-bundle.mjs`; `node tools/audit-p149-release.mjs`; `python tools/run-node-suite.py --regular`; `python tools/run-node-suite.py --stress`. Para gerar APK, abrir o projeto no Android Studio com SDK instalado ou usar runner Android conectado, então homologar instalação/reaberturas e áudio real. **Não retomar de P148 ou anterior como baseline depois de validar o ZIP P149.**
