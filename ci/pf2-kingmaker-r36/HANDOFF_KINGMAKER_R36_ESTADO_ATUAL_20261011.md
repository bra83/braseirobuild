# HANDOFF — BRASEIRO PATHFINDER 2E KINGMAKER (R36)
**Atualização verificada: 12/10/2026. Escopo exclusivo: PF2e Kingmaker.**

## Continuidade verificada em 12/10/2026, 19:27 UTC

Esta seção supersede as datas, a ordem de reconstrução e a próxima ação do texto histórico abaixo. O objetivo completo continua aberto: semântica integral das 73 estruturas, 11 capítulos/40 partes e companheiros. Não declarar 100%, não criar APK até as implementações solicitadas estarem prontas, não atuar em outros VTTs e não alterar a R35.

### Código e evidência mais recentes

- Commit de código validado: `0fe8d3a129f9c47392081f196ea7c302f73a2cec`.
- [BRELGA, run 38297432696](https://github.com/bra83/braseirobuild/actions/runs/38297432696): verify **success**, job `114971802383`.
- Saída real: **15 testes sintéticos**, **257 verificações JVM de semântica**, **45 testes focados**, **418 testes de regressão completos** e `:app:compileDebugJavaWithJavac` aprovado.
- `R36_ANDROID_JAVA_TYPECHECK_PASS=true`, `BUILD SUCCESSFUL in 1m 4s`.
- Emulador **skipped**. Nenhum APK desta continuidade foi gerado ou publicado. O APK instalado anteriormente não contém estas correções. Compilação de tipos e JVM não são homologação de UI, AppDb real, três reinícios ou backup.
- Consultar HEAD e Actions novamente antes de alterar a branch. O commit que grava esta atualização de documentação naturalmente muda HEAD; a evidência acima é do commit de código informado.

### Correções executadas nesta continuidade

**Estruturas e assentamentos**
- Lotes físicos por exemplar, malha, bloco e lista de lotes; tamanho impresso, contiguidade, sobreposição, obras incompletas e escombros. Registro de posições antigas por formulário normal, sem inventar coordenadas para saves legados.
- Foundry/Dump não compartilham bloco com Residential; Tannery admite apenas Tenement; Luxury Store precisa de Mansion/Noble Villa no mesmo bloco.
- Lumberyard/Pier/Waterfront exigem contato físico com borda de água; Mill reduz Consumo somente pela posição registrada junto à água.
- Streetlamps e Paved Streets por malha; Sewer uma vez por assentamento; Bridge e muros por borda, sem sobreposição.
- Bordas de água declaradas por malha a partir de hex elegível. Assentamento insular sem ponte tem influência zero e penalidade real de Trade; construir/demolir ponte atualiza a influência salva.
- Habitação e nível do assentamento usam blocos realmente construídos; escombros, obras e infraestrutura não contam como blocos construídos. Saves com posição desconhecida mantêm fallback explícito até organização.
- Demolição normal com d20 real e Engenharia contra CD de Controle. Sucesso libera um lote; sucesso crítico escolhe estrutura inteira ou recuperação 1d6 de Madeira/Pedra/Minério. Falhas deixam escombros; falha crítica também +1 Unrest. Toda a estrutura perde seus efeitos no início; cada lote restante fica reservado e a mutação é transacional. Interface normal em Editar assentamento.
- Construção pré-paga/resumo, reparos, bônus específicos, Treasury/Bank/Mint, história de efeitos de primeira construção e o catálogo numérico já integrados continuam na cadeia.

**Companheiros**
- Influência compartilhada das gêmeas; Kalikke/ Kanerah ativa controla o investimento automático de Counselor/Emissary. Persona salva como feature, escolha do grupo a partir de Influência 8.
- Preparações passam pelo formulário normal: sono confirmado pelas horas distintas de Descansar registradas e pela duração da rotação de vigias do grupo. Não usar um acampamento encerrado sem sono registrado como prova.
- Persona usa o dia do descanso registrado; preparações avançam 30 minutos no relógio real, dentro da mesma transação. Repetir a mesma preparação é idempotente. Não reaproveitar descanso de outro dia para nova preparação.
- Harrim: atividade normal de acampamento, proteção +2 na CD do teste real de encontros **noturnos**, sem acumular repetições no mesmo camp. Não altera CD básica permanentemente nem concede proteção de dia.
- Perfis com sete enredos publicados: Amiri **Blood Calling / O Chamado do Sangue**, Ekundayo **A Feast of Feasts**, Jubilost **The Inconsequent Debates**, Linzi **Treasury Trouble**, Nok-Nok **Heroes or Gods?**, Tristian **Kingdom of the Cleansed**, Valerie **Sir Fredero’s Challenge**.
- Suplemento JSON passa de doze aventuras inventadas para sete aventuras publicadas e doze perfis. Retira ouro, itens, XP e eventos automáticos sem fonte; não reaproveita IDs falsos como prova de concluir aventuras diferentes.
- Migração arquiva missões antigas, desativa eventos/POIs/cenas antigos e quatro marcos inventados de lealdade/redenção; preserva histórico, sem apagar ou recalcular arbitrariamente consequências já salvas.
- Perfis descritivos não criam combatentes genéricos nem reiniciam o estado cognitivo de NPCs. Novos registros usam chaves canônicas de companheiro.
- Editor de líderes inclui companheiros disponíveis, seleciona por tipo/ID e usa role_key, não rótulo traduzido. Bloqueia o mesmo companheiro em dois cargos via entradas NPC/companion; um herói homônimo não herda automaticamente o investimento de um companheiro.
- Isto corrige **enredos/dados e mecânicas específicas**, não compila integralmente os encontros, mapas, tesouros, rituais, magias, runas ou todos os arcos do guia.

**Campanha**
- Mantida a correção de Candlemere/Foras/Thresholders/Pharasmins e o ritual opcional da Hélice, sem a antiga trama inventada de Nar-Voth/Sekamina.
- Mantidos capítulos oficiais 1–11 e manifesto de 40 partes. Atlas e suplemento de companheiros não são capítulos 12/13 da campanha.
- O suplemento conserva `chapterNumber:12` apenas como identidade legada de arquivo; metadata diz explicitamente que não é capítulo oficial.
- Não confundir a revisão de enredo de Candlemere ou das sete aventuras com cobertura integral dos onze capítulos.

### Reconstrução atual

Use o workflow **`.github/workflows/pf2-r36-brelga-independent.yml`**, extraindo o ZIP canônico R35 e seguindo sua cadeia exata. A lista antiga de oito patches abaixo está incompleta para o código atual. A etapa final integra `patch_semantic_domains.py`, que compõe os módulos de estruturas, tesouraria, campanha, gêmeas, bordas, lotes, demolição, sono e liderança e copia três helpers Java. Ela também corrige os assets e contratos obsoletos de regressão.

Arquivos recentes em `ci/pf2-kingmaker-r36/`:
- `patch_grid_infrastructure.py`, `patch_twins_persona.py`, `patch_border_infrastructure.py`, `patch_lot_placement.py`, `patch_occupied_blocks.py`.
- `patch_companion_campaign.py`, `patch_harrim_camping.py`, `patch_demolition_lots.py`, `patch_completed_camp_sleep.py`, `patch_companion_leaders.py`.
- `semantic/companion_adventures.json`, os três helpers e `semantic/R36BookSemanticsHarness.java`.
- `test_r36_book_fidelity.py` é um conjunto de guardas de integração; não certifica execução no telefone.

O executor local desconectou durante esta continuidade. As alterações foram preservadas via GitHub e testadas no BRELGA. Não perder tempo repetindo exec_command offline: ferramentas conectadas de GitHub e Drive continuam suficientes para ler fontes, enviar código e obter CI. O snapshot `ci/pf2-r36-source-inspection/` é **antigo** e serve para âncoras; não é o código Android completo mais recente.

Os PDFs corretos podem ser lidos integralmente como texto com o fetch do Drive em modo padrão, sem download local: principal ~2,47 milhões de caracteres; Companion Guide ~532 mil. Não gravar URLs de download com credenciais em repositório, workflow ou logs.

### Próximas correções concretas

1. Crescimento Village→Town→City→Metropolis e malhas adicionais precisa dos predicados espaciais do livro p.541 e transição no momento correto de Build Structure. O fluxo administrativo legado ainda usa totais agregados. Village cheio: próximo bloco adjacente, Reino 3; Town: dois lotes em **cada um** dos quatro blocos, Reino 9; City: dois lotes em cada bloco antes de outra malha, Reino 15. Não conceder milestone de promoção só por editar categoria.
2. Upgrades físicos/custos: custo realmente usado no exemplar, despesas alternativas e construções gratuitas; diferença de RP/commodities, lotes adicionais no mesmo bloco, substituição de efeitos somente ao concluir. Não usar custo de catálogo como prova de pagamento anterior. Conferir diferenças negativas publicadas, inclusive Academy→Military Academy e Wooden Wall→Stone Wall.
3. Museum: transferência de item real nível 6+, relevância aprovada pelo Mestre, Unrest −1 por doação/+1 por retirada. Menagerie: captura/transporte reais, criatura 6+, Reino >= criatura+4, Fame/Infamy ou Ruin, Unrest por sapientes a cada início de turno. Exigem inventário/ledger de propriedade real; equipment atual é texto livre.
4. PC checks/shops: ligar bônus de estruturas e níveis de itens ao contexto e fluxo de jogo reais, respeitando tipos de bônus. Paved Streets: movimento e relógio. Town Hall/Castle/Palace: quotas por líder PC; Palace/Ruler +3 em todas Leadership checks; Relocate Capital e transformação em uma única Noble Villa de quatro lotes.
5. Companheiros: New Leadership com grau melhorado nos cargos publicados, Jaethal/Famous +1d3 Unrest, atividades especiais de líderes, magias/runas/itens, demais atividades de camping, separação das gêmeas, janelas de história e aventuras completas. Horas de sono confirmadas não equivalem a aplicação integral de cura, magias, foco ou condições nas fichas.
6. Legados: revisar possíveis duplicatas npc_* vs chaves canônicas e personal_quest_status vindo de arcos inventados; não somar Influência, apagar decisões ou considerar esse status prova de completar outra aventura. Manter histórico separado e identidade verificável.
7. Campanha: confronto real de cada uma das 40 partes, ramificações, encontros, mapas, tesouros e consequências; Cap.1 ainda precisa dos encontros físicos da Mansão Aldori. As sete aventuras do suplemento não têm seus encontros/recompensas automáticos compilados, e não confundir sua presença em JSON com progressão completa no Solo.
8. Hélice: bônus +4 de status contra teletransporte/banimento para IDs presentes na Casa na Margem do Tempo ainda precisa se ligar a saving throws reais.
9. Revisar APIs administrativas antigas de demolição/reparo e quaisquer rotas que ainda possam contornar os resolvedores oficiais. As rotas normais novas e resolveKingdomActivity usam demolição por lote; não chamar a remoção administrativa antiga de atividade oficial.
10. Depois das implementações, compilar um snapshot fixo e fazer Android real, AppDb com dados não vazios, três reinícios, export/import/backup, guerras e consequências. Não gerar APK antecipadamente. Não transformar resultado JVM/source contract em homologação desses gates.

---

## Registro histórico de 11/10/2026 — preservado, não usar como status atual

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
