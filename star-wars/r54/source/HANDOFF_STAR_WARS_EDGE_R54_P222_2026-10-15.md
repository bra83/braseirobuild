# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P222

## Base e continuidade

P222 é uma evolução **direta** do ZIP integral canônico P221: `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P221_SOURCE_CHECKPOINT.zip`, SHA256 `3f220f5914aba305e75c403eea64399bd025b1bb2874e2b6ab7254d1fff695f8`, 99.389.745 bytes, 7.297 arquivos. Não reiniciar, não apagar saves ou alterar Kokoro.

## Implementação real P222

Fonte primária consultada diretamente: SWE09 *The Jewel of Yavin*, PDF Google Drive id `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, página impressa 95, **Episode III XP Rewards**. Novo `app/src/main/assets/vtt/js/adventure/jewelEpisode3Rewards.mjs`:

- **10 PE** para o PJ ativo apenas após recibo real `JEWEL_HOLO_END` com `ESCAPED`.
- **5 PE** para o PJ ativo apenas após `JEWEL_ARIS_ALLIANCE` com ambas as partes confirmando aliança.
- **15 PE** para o PJ ativo apenas após investigação que localizou Arend, destino `DEAD` ou `INCARCERATED` e **encontro individual real de Arend concluído como `adversaries_defeated`**. Não basta uma marcação GM de morte ou prisão.
- Mestre confirma a concessão explicitamente. O motor chama `grantExperience`, registra evento `XP_GRANTED`, atualiza `xp.total` e `xp.available` na ficha real, e grava recibo `JEWEL_EP3_XP` com marco, prova original, PJ, PE antes/depois, anti-replay e rollback atômico. O mesmo marco não concede PE duas vezes ao mesmo slot/ID de PJ. A validação de saves rejeita recibos alterados ou derivados divergentes. Testes verificam três reaberturas.
- UI Android PT-BR na tela da aventura, controller, bridge e bundle offline **110 módulos** integrados. Android **1.0.222**, versionCode **10222**. Shell `data-version` sincronizado. Oito Java/Kokoro byte-idênticos P221.
- Limitação: o motor possui um PJ ativo; personagens sem ID próprio usam `player-slot` estável. Não inventar concessões para outros jogadores ou companheiros.
- **NÃO implementado:** os outros dois marcos oficiais de 5 PE cada (derrotar/evadir cloud cars; escapar de Bespin com a Joia), pois não existe ainda prova de combate veicular/viagem concluída. Não premiar automaticamente.

## Verificação e evidências

`evidence/P222/PDF_TARGETED_SOURCE_CROSSCHECK.json`, `TEST_SUMMARY.json`, `PER_FILE_RESULTS.json`, TAP individual por arquivo, `SYNTAX_AND_JAVA.json`. Os números definitivos são os arquivos de evidência. Testes executados por arquivo, não em suíte monolítica. Dois testes legados de sincronização de versão falharam inicialmente porque `r46-shell.html` ainda dizia 1.0.221; shell corrigido para 1.0.222 e ambos passaram novamente. Teste legado P221 com versão fixa foi corrigido para aceitar versões >=10221. Não apagar evidência da correção.

## Prioridade P223+

1. **Cloud cars reais:** cotejar ficha oficial Storm IV e piloto com SWE09 p.94 e/ou EotE Core; instanciar quatro veículos distintos no runtime veicular com posição, piloto, alcance e confirmação de presença física. Integrar combate e evasão com prova persistente; somente então habilitar os 5 PE oficiais.
2. Consequências de falhas das patrulhas Wing Guard com fichas, quantidade e presença confirmadas, sem teleporte.
3. Fuga real de Bespin com posse física da Joia, 5 PE apenas com recibo de navegação/viagem.
4. Vertigem Holo-Park em todas as ações aplicáveis; relógio narrativo real de dois minutos; Arrow-7 avançado somente com ficha oficial.
5. SWE02 New Meen/Geonosis/Teemo, cinco aventuras cena a cena e 360 consumidores EotE/AoR/expansões.
6. Preservar Kokoro e saves, executar focais + regressão por arquivo + seis stress, gerar ZIP Android Studio completo, SHA256, delta e handoff só após avanço real.

## Limites operacionais

Nenhum SDK/Gradle/adb/APK/CI BRELGA ou homologação física foi executado. Nenhum commit ou upload GitHub realizado. A Biblioteca estava cheia em P221; não apagar checkpoints antigos. Se não houver capacidade, entregar ZIP completo no sandbox e não alegar upload/readback do ZIP. Manter tarefa ativa até homologação real.
