# HANDOFF — BRASEIRO PATHFINDER 2E KINGMAKER (R36)
**Atualização verificada:** 11/10/2026. **Escopo exclusivo:** Pathfinder 2e no modo Kingmaker.  
**Objetivo do novo chat:** continuar a finalização real, sem reiniciar o trabalho, inventar porcentagens, chamar presença de arquivos de certificação integral ou modificar outros VTTs.

> **COMANDO IMEDIATO PARA O NOVO CHAT:** Acesse o GitHub conectado, confirme HEAD/Actions **novamente** (podem ter mudado após esta captura), leia este arquivo e os workflows, examine o resultado do emulador macOS e feche os testes reais de turnos de Reino, exércitos, guerras, persistência/backup e os desvios dos PDFs. Execute correções e gates reais em lotes completos, preserve R35, reportando commits, resultados e pendências concretas. Não responda só com plano.

## 1. Fonte de verdade — repositório, branch, baseline
- Repositório: https://github.com/bra83/braseirobuild
- **Branch de trabalho APENAS Kingmaker:** `pf2-kingmaker-r36-official-rule-fixes-20261011`
- **HEAD confirmado antes da criação deste handoff:** `09067b0eb9b0e58f6e01a5f7552b3aeb3b7ddd43`. Compare com o HEAD ao reabrir; o commit do próprio handoff naturalmente mudará o HEAD.
- Comparação com a tag estável R35 nesse instante: **122 commits à frente, 0 atrás**. Isso não significa 122 correções homologadas — inclui scripts, relatórios e CI.
- **R35 publicada e imutável:** https://github.com/bra83/braseirobuild/releases/tag/PATHFINDER2E-9.0.36-R35
- ZIP canônico da R35: `BRASEIRO_PATHFINDER2E_9.0.36_R35_KINGMAKER_ANDROID_STUDIO.zip`, **92.975.058 bytes**, SHA-256 `86d9a6fd26e4139e084c8e4069f49ef53e0cf8654e0f9f5662b92f9183941aa6`; GitHub release asset ID `629875743`.
- APK estável R35 disponível na release. **NÃO substituir, recriar a tag ou sobrescrever os assets R35.**
- A branch R36 contém **scripts de transformação aplicados ao ZIP R35** durante CI; não assuma que o último código-fonte Android completo está versionado diretamente na branch. O snapshot concreto do APK R36 resulta da extração do ZIP + sequência exata de patches.

## 2. PDFs oficiais — links CORRETOS (fonte prioritária)
**Pasta de regras e campanha:** https://drive.google.com/drive/folders/1PSqg91bBBIUcEYmTqvXa8gGvH9gkKpz1

1. **Kingmaker.pdf** (principal; 11 capítulos, regras de Reino, edifícios, exércitos): https://drive.google.com/file/d/1hHjK7DjhdDFvejYk70NNxO0zMp7Rnsn3/view
2. **Kingmaker Companion Guide.pdf** (companheiros, acampamento, clima): https://drive.google.com/file/d/18-yv9f-2Fz6fnnoZDFHKEfKSRu9KUViq/view
3. **Kingmaker Player's Guide:** https://drive.google.com/file/d/1iGkxIC2VCesq3qrptqAATysiQ9nVmaT6/view
4. **PF2e Player Core:** https://drive.google.com/file/d/1jsxwABi-rLWbA77kFFlLFzfJffq8XQ0m/view
5. **PF2e GM Core:** https://drive.google.com/file/d/1rHKYwwhTQSE-GzhsaX6S4-tlLaQiN2rq/view
6. **PF2e Monster Core:** https://drive.google.com/file/d/1yaS_csde64k9a-jCqzGeJfbFyNXwn3YY/view

Referência para páginas e cruzamentos: `ci/pf2-kingmaker-certification/KINGMAKER_CANONICAL_BOOK_CROSSWALK.json`. Não tratar manifestos extraídos automaticamente como revisados integralmente; conferir a redação oficial em caso de ambiguidade, sobretudo perícias alternativas, DCs, lotes e custos de commodities.

## 3. Evidências concretas de CI — estado até este handoff
### Compilação de snapshot verificada: APROVADA
- **GitHub Actions:** https://github.com/bra83/braseirobuild/actions/runs/38177657958
- Workflow: `.github/workflows/pf2-r36-snapshot-20261011.yml`
- Commit testado: `b48be4cd25e7950bd5c9b41ed733b062e9802869` (**não** o HEAD final acima).
- Resultado: `verify-hosted = success`; **30 testes dirigidos passaram**; **393 testes gerais passaram, 10 skipped**; `:app:assembleDebug` = `BUILD SUCCESSFUL`.
- APK de debug gerado no runner: SHA-256 `c5de25e235cf7056e053e9bed74b6a93ed8ebde1875e189a54fd251eabfa87f8`; **não há link de download de APK R36 publicado**.
- `:app:testDebugUnitTest` encerrou com Gradle success, mas com **`NO-SOURCE`**: NÃO contar como testes unitários Android executados.
- Outros workflows mais recentes de validação hospedada também passaram parcialmente, mas o gate self-hosted permanece distinto.

### CI R36 com BRELGA
- Workflow geral: `.github/workflows/pf2-r36-official-rule-fixes.yml`.
- https://github.com/bra83/braseirobuild/actions/runs/38177577506 — `civic-inspection`, `official-content-crosswalk`, `patch-synthetic`, `verify-hosted` concluíram **success**; **job `verify` BRELGA estava QUEUED** na leitura.
- Não atribuir aprovação BRELGA a um job `queued`. Reconsultar Actions; evitar disparar dezenas de pipelines concorrentes. Há snapshots cancelados por controle de concorrência.

### Teste REAL em Android emulado — passou parcialmente, FALHOU no gate total
- https://github.com/bra83/braseirobuild/actions/runs/38178825967 — Linux Android Emulator sobre commit `09067b0eb...`: **workflow failure**. Dessa vez o emulador iniciou, o APK **instalou e abriu**, e o script leu o **AppDb real do aplicativo**, não uma simulação SQLite.
- Log confirma:
  - `ACTIVATION_ANDROID_APPDB=PASS`: estado SQLite `enabled` mudou de 0 para 1 após ativar Kingmaker.
  - `PERSISTENCE_AFTER_PROCESS_RESTART=PASS`: o estado sobreviveu ao force-stop/reabertura.
  - `KINGDOM_TURN_ANDROID_APPDB=NOT_PROVEN`: teste não localizou os botões de iniciar turno, enquanto a UI navegava/mostrava outras telas.
  - `ARMY_ANDROID_APPDB=NOT_PROVEN`: teste não localizou `ABRIR ADMINISTRAÇÃO AVANÇADA`.
  - `FOUR_LIVE_CHECKS` = `{activation:true, kingdom_turn:false, army:false, persistence:true}`.
  - Contadores de turnos/exércitos/batalhas ainda estavam em **0** antes e após a reabertura. **Não reivindicar persistência validada com dados não vazios**.
  - Logs exibiram navegação equivocada em tela de Configurações ao procurar Reino: pode ser problema do seletor/hierarquia/scroll ou de UI real; INVESTIGAR, NÃO supor resolvido.
- Script de aceitação: `ci/pf2-kingmaker-certification/android_ui_endurance.py`. Melhorar seletores por `resource-id`, hierarquia/área ativa e captura `uiautomator` de modo determinístico, em vez de texto ambíguo `Kingmaker`, swipe sem foco ou menus inexistentes.
- Workflow Linux: `.github/workflows/pf2-r36-android-emulator-smoke.yml`.
- Emulador **macOS Intel**: https://github.com/bra83/braseirobuild/actions/runs/38178766993 — estado **in_progress** na leitura; verificar resultado final. Workflow `.github/workflows/pf2-r36-android-emulator-macos.yml`. Mac é alternativa ao Ubuntu sem KVM, não substitui a exigência de testes de UI/dados.

## 4. Implementações candidatas R36 e ordem canônica
Workflow de referência para reconstrução: `.github/workflows/pf2-r36-snapshot-20261011.yml`.

Aplicar **exatamente nesta ordem** ao diretório extraído e verificado da R35, com gates fail-closed:
```bash
python ci/pf2-kingmaker-r36/patch.py "$project"
python ci/pf2-kingmaker-r36/patch_claim_hex_prepaid.py "$project"
python ci/pf2-kingmaker-r36/patch_kingdom_accounting.py "$project"
python ci/pf2-kingmaker-r36/patch_claim_hex_ui.py "$project"
python ci/pf2-kingmaker-r36/patch_build_structure.py "$project"
python ci/pf2-kingmaker-r36/patch_build_structure_repairs.py "$project"
python ci/pf2-kingmaker-r36/patch_structure_catalog_values.py "$project"
python ci/pf2-kingmaker-r36/patch_multi_structure.py "$project"
```
**Não aplicar patch duas vezes** nem ignorar source drift. Os scripts alteram o projeto gerado R36, não a release R35. Reexecutar toda a bateria de testes após qualquer alteração, em um snapshot fixo, e guardar SHA do código e APK.

Recursos com patches e contratos de teste (implementação não equivale a homologação plena):
- **Collect Taxes:** somente `Trade`; sucesso +1 / crítico +2 de bônus **circunstancial de Economy**, não RP inventado; efeito e duração pelo registro do turno.
- **Quell Unrest:** crítico reduz `1d6` de Unrest; falha crítica escolhe `+1d4` Unrest **ou** `+2 Ruins`; limitar atividades/perícias consecutivas; consequências persistidas.
- **Claim Hex:** cobra 1 RP **antes** da rolagem, registra gasto uma única vez; restrições de perícia/limites por nível, XP de Reino e penalidade temporária de Stability na falha crítica; UI encaminhada ao resolvedor oficial.
- **Bônus:** Expansion Expert +2 circunstancial só em Claim Hex a partir do nível adequado, sem acumular bônus de mesmo tipo.
- **Reino:** Fame/Infamy por turno/sucesso crítico; RP sobrantes convertidos em XP sem teto artificial.
- **Build Structure:** pagamento antecipado RP e commodities; falha → obra incompleta persistida, sem cobrar de novo ao retomar; falha crítica → rubble; crítico → devolução de metade das commodities, não RP; lotes e edição de assentamento.
- **Repair Structure:** metade dos custos, bônus de item +2 não cumulativo, falhas e escombros persistidos; há testes de fonte/transformação.
- **Catálogo de estruturas:** o manifesto `ci/pf2-kingmaker-certification/kingmaker_official_structures_p544_552.json` contém **73 entradas extraídas**, mas **não integralmente certificadas**; `ci/pf2-kingmaker-certification/audit_structure_catalog.py` comparou **18 mapeadas** e o snapshot teve `Keys with differences: 0` para essas 18 após patches. **NÃO declarar que as 73 estão implementadas/revisadas.**
- Arquivos Android centrais: `android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java`, `KingmakerRulesEngine.java`, `MainActivity.java`, `AppDb.java`, mais motor de guerras e assets Kingmaker.

## 5. Campanha oficial, companheiros, guerras — medição com limites
- Auditoria: `ci/pf2-kingmaker-certification/audit_campaign.py`, `crosswalk_runtime_audit.py`, `official_part_semantic_audit.py`.
- 13 assets JSON de campanha do ZIP R35, aproximadamente **65 quests, 31 scenes, 62 POIs, 44 hazards, 24 events e 48 milestones**; 23 dependências de quests sem ciclos ou dangling na auditoria. São contagens de **catálogo**, não prova de progressão real.
- Crosswalk: **11 capítulos oficiais** com pelo menos um asset candidato e **12 perfis** do Companion Guide (7 principais + 5 secundários). A lista secundária inclui o perfil conjunto Kalikke e Kanerah; não confundir entrada com personagem individual.
- Código de guerras: 6 superfícies/funções encontradas; **não** valida cálculo integral de batalha, baixas, desdobramentos narrativos ou save/restore.
- Verificação literal dos títulos das partes: **36 de 40** exigem comparação manual da tradução PT-BR / cenas; ausência do título inglês **não prova** que falta a parte.
- **GATES AINDA ABERTOS:** confronto cena-a-cena/capítulo 1–11 com PDFs e ramificações; influência/quests/diálogos e consequências dos companheiros; acampamento/clima; Reino e exército com campanhas reais; coerência do gameplay com motor de regras determinístico; persistência **não vazia**, backup/export/import e restauração prolongada.
- **Requisito do usuário:** regras principais em código para gameplay, sem depender de RAG para executar mecânicas oficiais, ainda que PDFs sejam suporte de leitura. Não confundir presença de RAG com implementação das regras.

## 6. Próxima execução concreta — não recomeçar
1. Ler HEAD, os últimos commits e workflows da R36. Reconhecer que a última validação completa aprovada é do snapshot `b48be4cd...`, não presumir que commits posteriores passaram no mesmo gate.
2. **Prioridade 1: fechar Android real (emulado) e persistência**: consultar macOS run `38178766993` e logs Linux `38178825967`; consertar navegação para Reino/administração/turnos, e validar em um AppDb **com pelo menos 1 turno, 1 exército, 1 batalha e consequências alteradas**, três reaberturas, export/import/backup, limites e integridade SQLite. Diferenciar BUG DO APP versus BUG DO TESTE.
3. **Prioridade 2: gap de livros e catálogo**: examinar os 73 blocos oficiais, custos alternativos, restrições de perícias e proficiência, CDs, pré-requisitos, níveis, lotes, efeitos especiais, demolição e reparos; ampliar mapeamento para além dos 18. Criar testes por caso mecânico e persistência, não só string-matching.
4. **Prioridade 3: campanha/companheiros/guerras**: confrontar *cada* capítulo 1–11 e cada missão dos companheiros com PDF por referência de páginas, escolhas e consequências, preencher lacunas comprovadas em assets/código, automatizar caminhos e testes de integração, testar guerra de ponta a ponta e salvamento.
5. Fixar um SHA após lote coerente; rodar testes sintéticos, catálogo `--require-zero-mismatches`, testes reais dos assets, `assembleDebug`, testes instrumentados/emulador e, quando BRELGA disponível, compilar e executar **no self-hosted**. Só aprovar se todos os gates explícitos passarem.
6. **Não criar uma nova release ou declarar “100%”** enquanto faltar cobertura integral e instalação/execução. Responder com **comits + workflow URLs + saída real + pendências remanescentes**, não estimativas arbitrárias.

## 7. Proteções absolutas
- **Não alterar outros VTTs** dentro de `bra83/braseirobuild`. Não atuar em D&D, Star Wars, Forbidden Lands etc.
- Preservar build R35 (tag, ZIP e APK), modelo de dados, saves, geração de imagens Gemini, voz Kokoro/Charon, TTS, Atlas/mapas/hexes, música e compatibilidade Android existente.
- Não inserir modelos TTS dentro do APK; motor Kokoro permanece externo. A implementação não deve acessar `Android/data` para copiar modelos.
- Evitar novos pipelines sobrepostos e commits apenas de documentação quando a prioridade é gate funcional.
- Sem RAG para o motor de regras em gameplay. PT-BR na interface e narração.
- Os testes *synthetic transform*, *source contract*, *SQLite fixture*, *Android Emulator*, *BRELGA runner* e *telefone físico* são níveis DISTINTOS. Jamais tratar um como prova de outro.
- Se endpoint do GitHub bloquear update, testar ações alternativas autorizadas e apresentar bloqueio exato, preservando artefatos executáveis; **não mascarar como “código enviado”**.

## 8. Links rápidos
- [Branch R36](https://github.com/bra83/braseirobuild/tree/pf2-kingmaker-r36-official-rule-fixes-20261011)
- [Release R35 estável](https://github.com/bra83/braseirobuild/releases/tag/PATHFINDER2E-9.0.36-R35)
- [Snapshot hospedado aprovado](https://github.com/bra83/braseirobuild/actions/runs/38177657958)
- [Workflow geral — quatro jobs verdes; BRELGA na fila](https://github.com/bra83/braseirobuild/actions/runs/38177577506)
- [Emulador Linux — 2/4 gates efetivamente aprovados, run terminou failure](https://github.com/bra83/braseirobuild/actions/runs/38178825967)
- [Emulador macOS Intel — resultado a verificar](https://github.com/bra83/braseirobuild/actions/runs/38178766993)
- [Crosswalk livros x assets](https://github.com/bra83/braseirobuild/blob/pf2-kingmaker-r36-official-rule-fixes-20261011/ci/pf2-kingmaker-certification/KINGMAKER_CANONICAL_BOOK_CROSSWALK.json)

**STATUS FINAL DO HANDOFF:** R35 publicada e preservada; R36 compilável no snapshot e com testes estáticos/regressão passando; ativação Kingmaker e restauração de estado vazio em emulador comprovadas. **NÃO CERTIFICADOS:** jogar turnos reais e guerras no Android, efeitos persistidos com dados não vazios, totalidade dos capítulos/companheiros e todas as 73 estruturas. NÃO declarar projeto concluído.
