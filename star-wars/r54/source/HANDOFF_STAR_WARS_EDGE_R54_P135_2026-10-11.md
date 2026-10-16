# BRASEIRO STAR WARS — R54 P135 | checkpoint Android Studio

## Base preservada

Base canônica **P134**, extraída do ZIP completo `BRASEIRO_STAR_WARS_EDGE_R54_P134_SOURCE_CHECKPOINT.zip` sem mesclar snapshots. A versão Android passou a `versionCode=10135`, `versionName=1.0.135`; o `r46-shell.html` anuncia a mesma versão. Bundle WebView offline recompilado a partir de fontes ESM com **73 módulos**.

## Código implementado

1. **AoR — 15 famílias de talentos com mecânica efetiva**: 10 equivalências de combate/perícias/armadura e treinamento físico, mais 5 equivalências de rerrolagem natural e Slicer Mestre. Compras mantêm custos e caminhos da árvore, XP, escolhas obrigatórias, persistência e os consumidores já implementados (iniciativa, pools de perícias, combate e armadura). O talento **Treinamento físico** concede 1 dado de benefício por grau em Atletismo e Resistência via `talentSkillPoolModifiers`, consumido pelo controlador de testes. Nenhum efeito foi anunciado para nomes não mapeados. Cobertura AoR declarada e testada: **177/360 posições** com mecânica, 360/360 compráveis; restam 183 posições sem efeito completo.
2. **Universal Adventure Engine — proteção contra perda silenciosa no JSON**: snapshots e payloads agora rejeitam propriedades simbólicas, getters, `toJSON` oculto, campos não enumeráveis e propriedades extras em arrays. Importações não persistíveis são bloqueadas antes de migrar o estado. Falhas fazem rollback sem criar eventos, flags ou objetivos parciais.
3. **Condições de gatilhos/rotas**: relógios e contadores ausentes não são mais interpretados como zero, evitando desbloqueio indevido por comparações `<=` ou `<`; comparações não numéricas falham fechadas.
4. **Cinco aventuras**: testes de ponta a ponta com eventos, condições, persistência JSON e restauração dos adaptadores existentes, sem alterar conteúdo narrativo ou inventar encontros. **Além da Orla continua sem motor de encontros validado.**
5. **Kokoro/TTS**: nenhum código de voz alterado, 27 testes específicos executados; sem teste físico de áudio.

## Validação real

- `python3 tools/run-node-suite.py --regular`: **1386/1386 PASS**, 248 arquivos.
- `python3 tools/run-node-suite.py --stress`: **6/6 PASS**, 6 arquivos.
- `node --test --test-reporter=tap tests/*tts*.test.mjs tests/*kokoro*.test.mjs`: **27/27 PASS**.
- Sintaxe JS **122 arquivos PASS**; Java **8 arquivos** sem erros sintáticos bloqueantes (699 símbolos Android não resolvidos fora do SDK); JSON **79**, XML **2** — PASS.
- Bundle offline reconstruído e validado com `node --check`, **73 módulos**.
- `tools/verify-final-source.mjs` foi interrompido pelo limite de execução após validar as suítes Node e JS/Java/JSON; **não** reportar o verificador global como concluído.

## Pendências não certificadas

- **183/360** posições AoR ainda sem efeitos mecânicos completos. Expansões EotE/AoR ainda não confrontadas integralmente com os PDFs primários.
- Encontros oficiais de *Além da Orla* ainda sem estatísticas e validação autorizadas.
- Sem APK nesta execução: compilar no GitHub runner self-hosted **BRELGA** ou Android Studio e homologar instalação, retomada, 3 reaberturas, voz Kokoro física e cenas das cinco aventuras. **Não declarar homologação Android.**

## Retomada

Descompactar o ZIP P135 em uma pasta limpa; usar esse projeto como base única; executar as três suítes registradas, comparar PDFs, completar os 183 efeitos ainda ausentes sem inventar regras e validar APK no BRELGA. Nunca substituir checkpoints P134/P133.
