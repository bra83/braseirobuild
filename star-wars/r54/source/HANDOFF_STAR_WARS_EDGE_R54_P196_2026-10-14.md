# HANDOFF CANÔNICO — BRASEIRO STAR WARS VTT R54 P196

**Base obrigatória:** P195 ZIP integral `BRASEIRO_STAR_WARS_EDGE_R54_P195_SOURCE_CHECKPOINT.zip`, SHA256 `8b843c8b6ea4cd374fb81f7e4a4891d5084603f28df5999d14d3a1708fb7ccf4`, 98.265.825 bytes, 6.334 arquivos. Não reconstruir de P194 ou anterior.

## Implementação REAL P196 — G-100, SWE09

1. **Nobody's Fool 3 corrigido:** no PDF oficial *The Jewel of Yavin*, a ficha G-100 declara `Nobody's Fool 3`, que aumenta a dificuldade de testes de Charm, Coercion e Deception contra ele. A negociação já implementada em P195 usa Charm. O novo pool aplica **três upgrades de dificuldade** à base Média, resultando em **1 dado de dificuldade e 2 dados de desafio**. O novo recibo `BANK_DROID` inclui `droidCharmVersion=2` e valida faces, símbolos e pool. **Compatibilidade:** recibos P195 sem versão permanecem reproduzíveis no pool antigo; não reescrever saves. A ficha do droid declara `Discipline 2`, mas **não fornece rank de Cool/Frieza**, que não foi inventado.
2. **Assistência física oficial para incapacitar o droid:** seção *Slicing Ahead*, teste **Difícil de Athletics ou Brawl** opcional. Uma rolagem narrativa real de Atletismo/Briga gera recibo aninhado em `BANK_STEP`; se houver sucesso, a tentativa de Computadores recebe **2 dados de Reforço reais**. Falha não fornece bônus. Replay valida faces, símbolos, atributos, custo de dificuldade e bônus efetivo. Assistência só existe na etapa `disable`, sem custo/tempo artificial. Usa a ficha do operador jogador selecionado; participação de outro membro do grupo não está integrada à UI.
3. UI Android PT-BR: seletor de Atletismo/Briga, bônus condicionado; aviso de `Nobody's Fool 3`. `r46-shell.html`, diagnóstico e Gradle sincronizados em **1.0.196**, `versionCode=10196`. Bundle offline regenerado com **93 módulos**. Oito arquivos Java, incluindo Kokoro/TTS, idênticos a P195.

## Fonte e evidências

- PDF oficial SWE09 *The Jewel of Yavin*, Drive ID `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, cotejo **DIRECIONADO** das seções *G-100 Remote Banking Droid* e *Slicing Ahead*; ver `evidence/P196/PDF_TARGETED_SOURCE_CROSSCHECK.json`.
- Testes: `tests/uae-p196-jewel-g100-official-nobodys-fool-muscle-assist.test.mjs` (11 casos), mais regressão P195 atualizada. **315 arquivos regulares, 2.880/2.880 testes; seis stress, 6/6; total 2.886/2.886 em 321 arquivos individuais**, segunda execução global limpa após sincronizar versão do shell. Evidências globais por arquivo em `evidence/P196/TEST_SUMMARY.json` e `PER_FILE_RESULTS.json`; **não confundir com suíte monolítica**.
- Sintaxe: 127 MJS, 22 JSON e dois XML aprovados; `evidence/P196/SYNTAX_CHECK.json`. Preservação Java: `evidence/P196/JAVA_PRESERVATION.json`.

## Pendências obrigatórias P197+

1. Restraining bolt: localizar item correto nos PDFs e integrar posse real no inventário. **Não equiparar automaticamente a `restraining_device`.** Substituição de ferramentas de slicing após assinatura capturada: exigir equipamento real verificado, sem inventar preço.
2. Efeitos de **Covering Fire** (+1 defesa à distância até 2 aliados a curto alcance até fim da próxima rodada, sem acumular) e **Tactical Direction** (sargento com manobra para 1 grupo minion até médio alcance, manobra livre ou reforço no próximo teste), cotejados na introdução SWE09. Integrar ao combate real, não apenas descrição.
3. SWE09: liquidação bancária, fuga, traição Naboka, consequências de assinatura; SWE02: New Meen/Geonosis/Teemo. Cotejar integralmente cinco aventuras e certificar semanticamente cada um dos 360 consumidores EotE/AoR/expansões.
4. Rodar testes focal+regressão global por arquivo+seis stress, preservar Kokoro, gerar ZIP Android Studio completo+SHA+delta+handoff somente com avanço substancial, salvar Biblioteca e conferir readback SHA/CRC. CI GitHub BRELGA/Gradle apenas com acesso real, não declarar APK/CI/homologação sem prova.

**Não há APK, CI BRELGA, homologação Android física ou certificação integral dos cinco livros neste checkpoint. Manter a tarefa horária ativa.**
