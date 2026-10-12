# HANDOFF — PATHFINDER 2E KINGMAKER R36 — CAPÍTULOS OFICIAIS 1–11

**Captura:** 11/10/2026 ~21h30, horário de Brasília (12/10/2026 00h30 UTC). **Projeto EXCLUSIVO:** VTT Android Pathfinder 2e, modo Kingmaker. **Objetivo:** terminar/certificar os 11 capítulos oficiais, não reiniciar o projeto nem tocar nos demais VTTs.

> **PRIMEIRA AÇÃO NO NOVO CHAT:** confira HEAD e Actions da branch abaixo; recupere a R35 verificada, aplique a cadeia *atual* de patches R36; confronte cada uma das 40 partes dos capítulos com o `Kingmaker.pdf` correto; corrija lacunas reais em assets e lógica persistida, rode gates, reporte commits e evidências. NÃO confundir scripts/testes simulados com campanha jogável, Android real ou BRELGA concluído.

## Estado do repositório
- Repositório: https://github.com/bra83/braseirobuild
- Branch: `pf2-kingmaker-r36-official-rule-fixes-20261011`
- **HEAD capturado ANTES de gravar este handoff:** `cf2fa41d1f6ea99a5a3c2d0f2d4e2284c7471dbd` — `ci(pf2): verify latest R36 source on hosted emulators and pinned BRELGA; cancel stale test runs` (GitHub 2026-10-12T00:30:04Z). Revalidar: os commits prosseguem e este handoff muda o HEAD.
- **R35 oficial imutável:** https://github.com/bra83/braseirobuild/releases/tag/PATHFINDER2E-9.0.36-R35
- ZIP `BRASEIRO_PATHFINDER2E_9.0.36_R35_KINGMAKER_ANDROID_STUDIO.zip`; asset 629875743; 92.975.058 bytes; SHA256 `86d9a6fd26e4139e084c8e4069f49ef53e0cf8654e0f9f5662b92f9183941aa6`.
- Os workflows R36 **baixam a R35, validam SHA/tamanho e aplicam patches** (target VERSION_NAME=9.0.37). Não presumir que há árvore Android completa/atual diretamente commitada ou release R36.

## PDFs corretos no Drive
Pasta: https://drive.google.com/drive/folders/1PSqg91bBBIUcEYmTqvXa8gGvH9gkKpz1

- **Kingmaker.pdf (FONTE PRINCIPAL):** https://drive.google.com/file/d/1hHjK7DjhdDFvejYk70NNxO0zMp7Rnsn3/view
- **Companion Guide:** https://drive.google.com/file/d/18-yv9f-2Fz6fnnoZDFHKEfKSRu9KUViq/view
- **Player's Guide:** https://drive.google.com/file/d/1iGkxIC2VCesq3qrptqAATysiQ9nVmaT6/view
- **PF2e Player Core:** https://drive.google.com/file/d/1jsxwABi-rLWbA77kFFlLFzfJffq8XQ0m/view
- **PF2e GM Core:** https://drive.google.com/file/d/1rHKYwwhTQSE-GzhsaX6S4-tlLaQiN2rq/view
- **PF2e Monster Core:** https://drive.google.com/file/d/1yaS_csde64k9a-jCqzGeJfbFyNXwn3YY/view

Não usar os PDFs de nomes semelhantes rejeitados em `KINGMAKER_CANONICAL_BOOK_CROSSWALK.json`; um deles é Grande Bazar, outro é um jogo de tabuleiro não-PF2e.

## 11 capítulos reais — **40 partes oficiais NÃO homologadas integralmente**
O livro tem 11 capítulos; o Android usa assets `chapter_00` a `chapter_10`, com `chapter_11_hex_atlas_pois.json` = atlas e asset 12 = compêndio de companheiros, **não** capítulos 11 e 12 da história.

| Oficial | Título | Início no livro | Asset(s) candidatos no Android |
|---|---|---:|---|
| 1 | A Call for Heroes | p.14 | chapter_00_*; chapter_01_stolen_lands_part1.json |
| 2 | Into the Wild | p.42 | chapter_01_stolen_lands_part1.json; chapter_02_stolen_lands_part2.json; atlas |
| 3 | Stolen Land | p.160 | chapter_01_stolen_lands_part1.json; chapter_02_stolen_lands_part2.json |
| 4 | Rivers Run Red | p.184 | chapter_03_rivers_run_red.json |
| 5 | Cult of the Bloom | p.214 | chapter_04_cult_of_bloom.json |
| 6 | The Varnhold Vanishing | p.248 | chapter_05_varnhold_vanishing.json |
| 7 | Blood for Blood | p.286 | chapter_06_blood_for_blood.json |
| 8 | War of the River Kings | p.330 | chapter_07_war_of_river_kings.json |
| 9 | They Lurk Below | p.396 | chapter_08_they_lurk_below.json |
| 10 | Sound of a Thousand Screams | p.422 | chapter_09_sound_thousand_screams.json |
| 11 | Curse of the Lantern King | p.478 | chapter_10_curse_lantern_king.json |

**Arquivos de verdade:**
- `ci/pf2-kingmaker-certification/KINGMAKER_CANONICAL_BOOK_CROSSWALK.json` (11 capítulos, 40 partes, fontes, candidatos, todos semantic_coverage_certified=false).
- `ci/pf2-kingmaker-certification/KINGMAKER_11_CHAPTERS_40_PARTS_PAGES.json` (páginas das 40 partes).
- `ci/pf2-kingmaker-certification/audit_chapters_1_11.py` (audita JSONs reais, quests/cenas não vazias, nomes/ligações, dependências, 10→11; retorna `official_fidelity_certified=false`, não prova campanha concluída).
- `.github/workflows/pf2-r36-chapters-1-11.yml` (gate `--fail-on-critical`).
- `ci/pf2-kingmaker-certification/audit_campaign.py` (continuidade e fixture SQLite, NÃO AppDb Android).
- `ci/pf2-kingmaker-r36/patch_chapter11_gate.py` e `test_chapter11_production.py`: Nyrissa `q_redencao_espada_briar` antes de `q_revelacao_rei_lanterna`. Conferir se o patch realmente roda na pipeline do candidato.

**Homologação necessária PARA CADA PARTE:** texto/página oficial; locais; mapa/POIs; quest e objetivos; gatilhos; NPCs/criaturas; encontros e perigos; testes, CDs, loot e XP; escolhas e consequências persistidas; transições; testes de UI/AppDb; evidência concreta. Localizar nomes (Jamandi, Vordakai, Irovetti, Nyrissa etc.) não demonstra semântica fiel.

## CI: evidência honesta na captura
- **Snapshot ANTIGO passou**: https://github.com/bra83/braseirobuild/actions/runs/38177657958 (SHA `b48be4cd25e7950bd5c9b41ed733b062e9802869`, **não o HEAD**). Handoff anterior registra 30 testes específicos e 393 gerais passando, 10 skipped, Gradle assembleDebug aprovado. `testDebugUnitTest` = `NO-SOURCE`, não contar como testes JVM executados.
- **Emulador Linux anterior parcial:** https://github.com/bra83/braseirobuild/actions/runs/38178825967 — app instalado, ativação persistiu; turno/exército/batalha **NÃO PROVADOS**; persistência era de estado essencialmente vazio. Distinto de real campanha.
- **Capítulos recente:** https://github.com/bra83/braseirobuild/actions/runs/38188200773 (SHA `012d0574658d`) = **failure**. A consulta devolveu job sem steps; NÃO atribuir causa a defeito de capítulo sem log.
- **Hosted último HEAD:** https://github.com/bra83/braseirobuild/actions/runs/38188243444 = **failure**, sem steps legíveis.
- **Linux Android emulator último HEAD:** https://github.com/bra83/braseirobuild/actions/runs/38188243489 = **failure**, sem steps legíveis.
- **macOS Android emulator último HEAD:** https://github.com/bra83/braseirobuild/actions/runs/38188243371 = **failure**, sem steps legíveis.
- **BRELGA último HEAD:** https://github.com/bra83/braseirobuild/actions/runs/38188243327 = **queued** na captura, NÃO aprovado.
- Tentativas de recuperar logs das falhas hospedadas retornaram **404 BlobNotFound**; investigar a infraestrutura/Actions e a primeira etapa verificável, **sem inventar causa**.
- Não existe comprovação de playthrough **integral** 1–11 (40 partes) com escolhas e consequências persistidas no Android.

## Avanços candidatos R36 (não confundir com homologação de ponta a ponta)
- Commits próximos ao HEAD: `012d0574658d` (capítulo vazio proibido), `f8d34b5079f6` (custos UI e exército adversário), `46e78b1e2fac` (teste de persistência com dados não vazios), `db2a66ed9ea5`/`df29308d757c` (custos Madeira/Pedra), `1c6bcf84daee` (UI chama motor oficial), `4bb150c92b25` (estrutura inválida não gera construção grátis).
- Regras com patches: Collect Taxes +1/+2 circunstancial Economia, nunca RP fictício; Quell Unrest 1d6 crítico e escolha falha crítica 1d4 Unrest OU +2 Ruins; Claim Hex 1 RP pago antes de rolar e penalty Stability temporário; edificações e reparos, custos alternativos, proficiências, resultados persistidos. Validar em AppDb real.
- Manifesto de 73 estruturas `ci/pf2-kingmaker-certification/kingmaker_official_structures_p544_552.json`; mesmo quando gates numéricos passam, **não** atribuir homologação a todos os efeitos especiais e variantes.
- Workflows: `pf2-r36-official-rule-fixes.yml`, `pf2-r36-snapshot-20261011.yml`, `pf2-r36-android-emulator-smoke.yml`, `pf2-r36-android-emulator-macos.yml`, `pf2-r36-brelga-independent.yml`.
- Consultar a **ordem exata atual dos patches** nesses workflows. Eles transformam R35 verificada em candidato R36 e podem falhar por source drift; não rodar patch duas vezes nem contornar validação.

## Regras inegociáveis para a próxima execução
1. Priorizar **corrigir e provar** os 11 capítulos/40 partes, usando o PDF correto, não apenas auditar títulos. Prosseguir depois nas dependências de companheiros, mundo vivo, reino, exércitos e guerras, sem perder a integridade narrativa.
2. Implementar regras oficiais como código persistente. Não exigir RAG durante rolagem/jogabilidade; PDFs são autoridade de implementação, não substituto de mecânica.
3. Em cada etapa: commits/arquivos realmente gravados, teste realmente executado, URL/código de saída, pendências. Se um bloqueio impedir GitHub/BRELGA, identificar precisamente e continuar outra tarefa verificável.
4. Preservar R35, mapas/hexes, sons, TTS externo Kokoro/Charon, integração Gemini/imagens, saves existentes e PT-BR. Não mexer em outros VTTs.
5. Fixar SHA de verificação, não cancelar continuamente testes longos com commits menores. Separar `fixture sintética`, `contrato de fonte`, `Gradle`, `AppDb no emulador`, `BRELGA`, `Android físico`, `campanha integral` — nenhuma dessas etapas substitui a outra.
6. Não afirmar 100%, não inventar porcentagens, não publicar R36 final com pendências essenciais.

**Estado final da captura:** HEAD `cf2fa41d...` com numerosas correções candidatas, **sem homologação integral**. Gate recente dos capítulos e últimas verificações hospedadas = failure sem logs legíveis; BRELGA = queued. O novo chat deve seguir do HEAD real e executar correções com evidências.
