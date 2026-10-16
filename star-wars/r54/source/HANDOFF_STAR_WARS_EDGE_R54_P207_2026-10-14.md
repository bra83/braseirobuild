# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P207

## Base preservada

Partir exclusivamente do ZIP Android Studio P207, construído sobre o checkpoint P206 SHA-256 `b8307eef4a1e6ee8ada5c73dcf0016e2d43013e016a2182ff957a41317e60907`, 6.443 arquivos. Não recriar o projeto, não regredir o Arrow-7, o banco, os quatro capangas ou o TTS. Oito arquivos Java/Kokoro mantidos byte-idênticos.

## Implementação REAL P207 — SWE09 Sweet Revenge

Consulta **direcionada** ao PDF oficial *The Jewel of Yavin*, Drive `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, pp. 90–91. A fonte NÃO traz a ficha completa do Arrow-7 nem um piloto NPC, então não foram inventadas velocidade máxima, blindagem, casco, manobrabilidade ou Tensão de sistema. Em vez de fingir uma perseguição completa, foi implementado um bloco jogável e verificável de **investigação contra Arend Shen**.

Novo `app/src/main/assets/vtt/js/adventure/jewelArendRevenge.mjs`: depois de revelada a traição, com o encontro dos quatro capangas resolvido e sem combate ativo, o personagem acessa a rede de Cloud City com confirmação do Mestre. Rolagem **real** de Computadores (Intelecto) ou Conhecimento das Ruas (Astúcia), dificuldade **Média (2)**, revela quatro endereços: residência no Distrito da Praça (p90), escritório perto de Figg Hall (p91), imóvel em Port Town (p91), iate no nível da Praça (p91). O terminal **NÃO** informa onde Arend está.

O Mestre confirma a chegada a cada local físico diferente. Motor lança d100 com as chances oficiais progressivas: **25%** no primeiro, **50%** no segundo, **75%** no terceiro e **presença automática** no quarto. Nenhum local pode ser repetido; depois de encontrar Arend não é possível repetir a busca. Rolagem de pesquisa fracassada só pode ser repetida mediante autorização expressa do Mestre. O resultado **não** teleporta personagens, não mata/captura Arend e não conclui a aventura automaticamente.

Recibos `JEWEL_AREND_LEADS` e `JEWEL_AREND_VISIT`, replay de pools, faces, símbolos, ID, sequência de locais, d100, chance e estado derivado `operations.arendRevenge`. Controller atomicAction + WORLD_UPDATED, bridge, UI Android PT-BR e bundle offline **102 módulos** integrados. Saves P206 sem investigação preservados. Android **1.0.207**, versionCode **10207**, shell/diagnóstico sincronizados.

## Testes P207

`evidence/P207/TEST_SUMMARY.json` e `PER_FILE_RESULTS.json`: **326 arquivos regulares, 3.005/3.005 testes; seis stress 6/6; total 3.011/3.011 em 332 arquivos executados individualmente**. Dez testes focais P207. Houve quatro falhas intermediárias de versões antigas em shell/testes de versão, corrigidas; regressão global final reexecutada sem falhas. NÃO foi suíte monolítica. Sintaxe **136 MJS, 16 JS, 22 JSON, 2 XML** PASS. Oito Java/Kokoro byte-idênticos ao P206. Evidência de fonte é cotejo direcionado, NÃO certificação editorial integral.

## Pendências reais P208+

1. **Arrow-7**: velocidade máxima, blindagem, casco, manobrabilidade e piloto não estão publicados no perfil SWE09 p90; buscar ficha oficial compatível ou exigir ficha real confirmada pelo Mestre. Não inventar atributos. Completar manobras, dano, colisões e escala veicular somente com fontes válidas.
2. **Arend**: confronto e consequências após a descoberta do endereço; alternativas do Mestre, Aris e perdão oficial por Lando (+5 Obligation: Favor por personagem) apenas com gatilhos publicados.
3. **Naboka**: falha inicial encerra sua disposição para tentativas adicionais, conforme SWE09 p69; integrar ao slicing real sem apagar histórico.
4. **Flight over Fight**: Holo-Park e perseguição de três facções conforme SWE09 pp92–94; terreno muda a cada duas rodadas, fuga em Longo ou até dez rodadas, consequências oficiais.
5. **SWE02**: New Meen/Geonosis/Teemo, além do cotejo cena a cena das cinco aventuras e 360 consumidores EotE/AoR/expansões.
6. **Android real**: Gradle/APK, CI BRELGA, instalação e três reaberturas no aparelho. Nada disso foi executado em P207.

## Continuidade obrigatória

Não reiniciar; preservar Kokoro e saves. Testar focal + regressão global **por arquivo** + seis stress; produzir ZIP Android Studio completo, SHA, delta, handoff e evidências apenas após implementação real. Upload à Biblioteca e readback SHA/CRC. GitHub `bra83/braseirobuild` e BRELGA somente se realmente acessíveis e acionados; nunca declarar CI/APK/homologação/certificação sem provas.
