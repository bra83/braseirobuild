# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P220

## Origem e restrição

Continuidade REAL sobre P219, ZIP Android Studio SHA256 `f38b01eeccc8f3976a9466584ff6b38cafc0765c3a99f19b22c11e17321d677a`, 98.796.128 bytes, 6.583 arquivos. Não recomeçar; preservar os saves, Kokoro e funcionalidades P1–P219. Sem SDK/Gradle/adb/APK/CI BRELGA/homologação física ou push GitHub nesta sessão.

## Fonte oficial consultada

PDF SWE09 *The Jewel of Yavin*, Google Drive id `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, páginas impressas 90–91 (*Sweet Revenge*, *Shen's Residence*, *Shen's Office*, *Shen's Secondary Businesses*, *Shen's Yacht, The Venture*), consultado diretamente via Google Drive. Fichas comparadas ao compêndio NPC já presente no projeto: Arend Shen `arend_shen_429` (JoY:90), K4 Security Droid `k4_security_droid_267` (JoY:90), Yacht Crew `yacht_crew_122` (JoY:91), além dos perfis existentes `shens_goons` e `grayson_private_security_guard`.

## Implementação REAL P220

- Novo `app/src/main/assets/vtt/js/adventure/jewelArendProperty.mjs`: preparação de encontros físicos em cada imóvel SOMENTE após pesquisa P207 e visita que localizou Arend. Mestre confirma presença, acesso, alcance e indivíduos distintos; sem teleporte ou ressuscitar NPCs. Recibos `JEWEL_AREND_PROPERTY_ENCOUNTER`, replay, anti-duplicação, rollback atômico, persistência e validação de saves.
- **Residência:** entrada com três droides K4 RIVAIS separados; sala interna com seis capangas em outro encontro, nunca teleportados para a entrada; Arend nemesis somente em encontro posterior ou com evasão física confirmada.
- **Escritório Figg Hall:** quatro seguranças particulares de Grayson em um grupo; Arend em encontro separado.
- **Port Town:** dez capangas na passarela em dois grupos de cinco; Arend separado.
- **Iate Venture:** três grupos distintos de três tripulantes; Arend separado. A chegada da patrulha Wing Guard em dez minutos após hostilidades exige relógio real e NÃO foi automatizada.
- Novos perfis `arend_shen`, `k4_security_droid`, `yacht_crew` em `rules/adversaries.mjs`, com atributos, armas, talentos e defesas conferidos com fichas JoY. `createAdversary`, iniciativa e combate efetivo no controller, com `publishedAdventureContext`, histórico de encontros e transações atômicas.
- Adapter SWE09, controller, bridge, UI Android PT-BR e bundle offline **108 módulos** integrados. `validateJewelOperations` aceita o novo recibo, enquanto `validateJewelArendProperty` verifica seus campos, a localização, ordem dos encontros, resolução ou evasão dos guardas e estado derivado.
- Android **1.0.220**, `versionCode=10220`; oito Java/Kokoro preservados byte a byte em relação a P219. O teste legado P219 de versão exata foi tornado compatível com versões posteriores e reexecutado; nenhum teste de regra suprimido.
- **Não** concede XP, créditos, 5.000 créditos de Arend, vitória, morte, prisão, aliança ou alteração de cena automaticamente. As fichas de adversários não implicam que estejam todos no mesmo cômodo.

## Evidências

`evidence/P220/`: `TEST_SUMMARY.json`, `PER_FILE_RESULTS.json` e TAP por arquivo, `PDF_TARGETED_SOURCE_CROSSCHECK.json`, `SYNTAX_AND_JAVA.json`. Execução por arquivo, quatro workers; **não** suíte monolítica. Números exatos de testes devem ser lidos do resumo, não inferidos. O teste P220 cobre presença real, composição de quatro imóveis, separação da sala, Arend nemesis, rollback, reabertura e adulteração de recibos.

## Próximas prioridades P221+

1. Implementar patrulhas Wing Guard na residência a cada 15 minutos e chegada de dois voos de dois cloud cars ao Venture até 10 minutos após hostilidades, **somente com relógio persistente real e fichas/veículos oficiais**, sem inventar movimento ou stats.
2. Desfechos posteriores de Arend e Aris, repercussões persistentes e XP oficiais somente após gatilhos comprovados. Não dar XP pela simples criação de um combate.
3. Vertigem Holo-Park em todas as ações compatíveis; modo narrativo de dois minutos só com relógio verificável.
4. Arrow-7 avançado somente com ficha oficial; SWE02 New Meen/Geonosis/Teemo; cinco aventuras cena a cena; 360 consumidores EotE/AoR/expansões.
5. Preservar Kokoro/saves, testes focais+regressão por arquivo+seis stress, ZIP Android Studio completo+SHA+delta+handoff apenas após implementação real. Sem APK/CI/homologação alegados sem execução verificável.

## Biblioteca

Biblioteca `STAR_WARS_EDGE_OF_THE_GALAXY_WORKING/checkpoints/`. Desde P211, o ZIP completo não cabe por limite de armazenamento. Não apagar checkpoints anteriores sem autorização. Se o upload completo continuar recusado, preservar delta/handoff/SHA quando possível e entregar ZIP completo no sandbox. Não afirmar readback do ZIP completo sem sucesso real.
