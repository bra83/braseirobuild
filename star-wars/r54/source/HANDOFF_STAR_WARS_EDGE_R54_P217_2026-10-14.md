# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P217

## Continuidade e fonte

P217 foi construído sobre **P216**, sem recomeçar nem apagar material anterior. ZIP base: `BRASEIRO_STAR_WARS_EDGE_R54_P216_SOURCE_CHECKPOINT.zip`, SHA256 `fd64d835d57d3ebc2a5516163befe3777c3a8bf8a927d56b0e4363d46cbaa935` (6.540 arquivos).

PDF oficial consultado via Google Drive: **SWE09 The Jewel of Yavin**, ID `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, pp.78,92–94; compêndio NPC do projeto `JoY:78` (Private Security Guard) e `JoY:87` (Shen's Goons). O texto não fixa quantos capangas de Arend e seguranças de Grayson aparecem simultaneamente no Holo-Park: a quantidade depende de confirmação do Mestre. Os dois grupos não obstruem diretamente a Guarda Alada, que atua primeiro no roteiro publicado.

## Implementação REAL P217

- Novo módulo `app/src/main/assets/vtt/js/adventure/jewelHoloFactions.mjs`, recibo `JEWEL_HOLO_FACTION_ARRIVAL` com replay e verificação de identidade, grupo, quantidade, alcance, presença física, indivíduos distintos e Guarda Alada não engajada. Nenhuma facção aparece automaticamente.
- Perfil `grayson_private_security_guard` publicado SWE09 p78 e JoY:78: minion com ficha real (Agilidade 3, Vigor 3, soak 4, ferimentos 5 por membro, defesa corpo a corpo/à distância 1, arma antidistúrbios e vibrofaca). Corrigido `createAdversary` para propagar defesa documentada do perfil ao estado de combate.
- Facção de Arend usa `shens_goons` já existente (SWE09 p90/JoY:87), sem ressuscitar os quatro capangas do Arrow-7: Mestre deve confirmar indivíduos **diferentes**. Máximo conservador de quatro, não alegado como tamanho oficial do encontro.
- Segurança de Grayson usa perfil p78; máximo de seis por grupo conforme organização publicada, sem inserir Burr Naxis/Gantel Dro automaticamente.
- Método atômico `beginJewelHoloParkFaction` no controller e bridge, com iniciativa real, adversários reais, `publishedAdventureContext`, registro de desfecho existente e bloqueio de duplicações. Exige que o combate anterior tenha terminado e que a Guarda Alada não esteja engajada.
- `currentJewelOfYavinEncounter` agora não mantém os capangas Arrow-7 já resolvidos como encontro corrente; reconhece grupos preparados e resolvidos corretamente.
- Relógio `holoParkRoundOffset` mantém sequência de rodadas de paisagem ao mudar de combate, sem inventar rodadas não registradas. UI Android PT-BR inclui confirmações e ação para iniciar cada facção.
- Android `1.0.217`, versionCode `10217`, bundle offline **105 módulos**, 8 arquivos Java/Kokoro idênticos a P216.

## Testes e evidências

**3.084/3.084** testes em **342 arquivos individuais**, 336 arquivos regulares com **3.078/3.078**, seis stress **6/6**; 9 testes focais P217. Quatro shards, sem suíte monolítica. Sintaxe: **507 MJS**, **2 JS runtime**, **22 JSON**, **2 XML**; sem erros. Evidências `evidence/P217/` incluem resultados por arquivo, resumo, comparação Java, cotejo direcionado de PDF e prova de implementação. Builders offline raiz/tools produziram bundles idênticos.

## Limites e prioridades P218+

1. Desfechos Arend/Aris/Lando e gatilhos oficiais para `+5 Obligation Favor` por personagem, sem premiação antecipada.
2. Fuga do Holo-Park: verificar integração semântica de todos os 16 efeitos com ações além de ataque/perícia, além de modo narrativo por minutos confirmados; não confundir 16/16 em código com homologação integral.
3. Arrow-7: atributos e piloto oficiais ausentes; não inventar casco, velocidade, blindagem, manobras e dano avançado.
4. SWE02 New Meen/Geonosis/Teemo; cotejo cena a cena das cinco aventuras e certificação dos 360 consumidores EotE/AoR/expansões.
5. Testes por arquivo e seis stress, ZIP integral + SHA + delta + handoff, readback se a Biblioteca permitir. A Biblioteca havia atingido `library_storage_limit_exceeded`: não apagar checkpoints antigos sem autorização.

**Não houve Gradle, SDK/adb, APK, CI BRELGA, push GitHub nem homologação Android física.** A integridade ZIP/delta e os hashes do checkpoint final são informados no handoff externo após empacotamento; esta cópia dentro do ZIP é pré-hash.
