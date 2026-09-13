# BRASEIRO — FICHAS MULTISSISTEMA — FINAL

Hub **standalone e mobile-first** de fichas de personagem em PT-BR, com **16 sistemas**, **639 campos explicados** e camada separada de validação mecânica.

## Escopo entregue

- Seletor e busca de sistemas.
- Ficha específica por sistema, sem transformar os RPGs em uma ficha genérica única.
- Dependências dinâmicas conhecidas (ex.: carreira → especialização, carreira → assignment, perfil/método/classe de OSE).
- Campos derivados conhecidos (ex.: D&D 2024 background grants, números-alvo de O Um Anel, antecedente de Mausritter, Luck de Tales from the Loop).
- Auditoria de regras em tempo real com fontes e estados `VALID`, `INVALID`, `MISSING_EVIDENCE` e `NEEDS_REFEREE` quando aplicável.
- Política **fail-closed**: a central não transforma lacuna documental em regra inventada.
- Importar, exportar e copiar JSON; importação é revalidada e a troca silenciosa entre sistemas é rejeitada.
- Rascunhos com `MISSING_EVIDENCE` podem ser reimportados sem serem falsamente finalizados; apenas `INVALID` é rejeitado na entrada.
- Persistência local por `localStorage`.
- Sem dependências web externas: o pacote é autocontido.

## Sistemas

D&D 5.5 / 2024; Mystara; O Um Anel 2e; Cyberpunk RED; Mausritter; Pathfinder 2e; Traveller; Forbidden Lands; Tales from the Loop; Things from the Flood; Star Wars EotE; Star Wars AoR; Worlds Without Number; Old-School Essentials; Pendragon; Wanderhome.

## Uso

Abra `index.html` em um navegador moderno. Os arquivos `systems-data.js`, `rules-engine.js`, `app.js` e `styles.css` devem permanecer na mesma pasta.

## Integridade e QA

Consulte `QA_REPORT.md`, `QA_STRESS_33.log`, `RULES_COVERAGE.md`, `SOURCE_MANIFEST.json` e `CHECKSUMS_SHA256.txt`.

O escopo deste artefato é a **central de fichas/validação**. Ele não afirma ser VTT completo, campanha completa, Android APK/AAB nem substituto dos livros de regras.
