# HANDOFF CANÔNICO — BRASEIRO STAR WARS VTT R54 P197

**BASE EXCLUSIVA:** checkpoint P196 `BRASEIRO_STAR_WARS_EDGE_R54_P196_SOURCE_CHECKPOINT.zip`, SHA256 `02a62d595d6da473947227908f9e8ede675b1ed875671d1c0d2d94cb781eb7c5`, 98.280.103 bytes, 6.343 arquivos. P197 é a continuação real. **Não retroceder nem substituir por relatório.**

## Código real P197 — SWE09 Wing Guard

1. `app/src/main/assets/vtt/js/rules/wingGuardTactics.mjs`: agendamento pelo Mestre com validação de ator, habilidade, alvo, quantidade, distância explicitamente confirmada e duplicação. Aplicação no turno efetivo de iniciativa do NPC, não na fila; persistência em `combat.wingGuardOrders` e nos adversários. Manobra cancelada sem travar se aliado ficar incapacitado antes de agir.
2. **Covering Fire**, oficial Wing Guard: manobra com até dois aliados confirmados a Curto; cada um recebe +1 Defesa à distância até o fim da rodada seguinte, sem acumular. O bônus entra no `setback` do ataque real do jogador contra o adversário; não afeta corpo a corpo. Um uso por oficial por rodada.
3. **Tactical Direction**, sargento Wing Guard: manobra para grupo minion aliado confirmado a Médio; modo **Boost** concede um Reforço real ao próximo ataque do grupo, consumido uma vez no pool. **Modo de manobra livre não implementado** e explicitamente rejeitado, não confundir com cobertura total da habilidade.
4. `controller.mjs`, `combat.mjs`, `adversaries.mjs`, `uiBridge.mjs`, bundle offline e `r46/final-runtime.js` integrados. Painel Android PT-BR na tela 02 para selecionar ator/alvos e confirmar distância; sem geometria automática entre NPCs. Corrigido erro de sintaxe **preexistente no P196**: apóstrofo não escapado em `NOBODY'S FOOL 3` no HTML de G-100.
5. `app/build.gradle.kts`: `versionCode=10197`, `versionName=1.0.197`; `r46-shell.html` e diagnóstico sincronizados; bundle offline **94 módulos**. Oito arquivos Java, incluindo Kokoro/TTS, byte a byte idênticos a P196.

## Evidências reais

- `tests/uae-p197-wing-guard-tactics.test.mjs`: **13 novos testes** para mecânicas, pools reais de ataque jogador/NPC, rejeições, persistência de três reaberturas, bridge/UI e cancelamento por incapacitação.
- **Regressão FINAL:** 316 arquivos regulares **2.893/2.893** + seis stress **6/6** = **2.899/2.899 em 322 arquivos individuais**, após correções de integração e repetição global. Evidência `evidence/P197/TEST_SUMMARY.json` e `PER_FILE_RESULTS.json`. **NÃO declarar suíte monolítica verde.**
- Sintaxe: **128 MJS, 22 JSON, dois XML**, `final-runtime.js` e `uiBridge.bundle.js` verificados. `evidence/P197/SYNTAX_CHECK.json`.
- Java/Kokoro: `evidence/P197/JAVA_PRESERVATION.json` com oito SHA iguais à base.
- PDF SWE09 *The Jewel of Yavin* Google Drive ID `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`: metadados do arquivo oficial confirmados em P197; regras de Wing Guard pp. impressas 12–13 apoiadas no cotejo direcionado P193, **sem nova certificação integral do PDF**. Ver `evidence/P197/PDF_TARGETED_SOURCE_CROSSCHECK.json`.

## Limites e prioridades P198+

1. Concluir **modo manobra livre** de Tactical Direction com economia de manobras e deslocamento real; verificar geometria NPC-NPC quando existir, sem inventar distância. Rever proteção de ordem pendente quando o ator é incapacitado antes de sua iniciativa.
2. Vincular restraining bolt a item **real** do inventário após cotejo PDF, **não confundir com `restraining_device`**; substituição real de ferramentas para apagar assinatura eletrônica, sem preço inventado.
3. SWE09: liquidação bancária, traição de Naboka, fuga, consequências; SWE02: New Meen, Geonosis e Teemo. Cotejar as **cinco aventuras cena a cena** e os **360 consumidores AoR/EotE/expansões individualmente** antes de certificar.
4. Rodar focal + regressão por arquivo + seis stress, preservar Kokoro. Só produzir próximo ZIP completo Android Studio, SHA256, delta e handoff após implementação substancial; salvar Biblioteca e verificar readback SHA/CRC.
5. CI GitHub BRELGA/Gradle somente com acesso real e evidências; **nenhum APK, CI BRELGA, SDK, adb ou homologação física foi realizado em P197**.

**A tarefa horária deve permanecer ativa até homologação comprovada.**
