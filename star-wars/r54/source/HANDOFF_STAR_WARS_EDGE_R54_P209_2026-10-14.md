# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P209

## Fonte e integridade

Base imutável P208: `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P208_SOURCE_CHECKPOINT.zip`, SHA-256 `959be9d5d382c8e2e25d23753854eed8583c89083948b7ac13377f92709bd5cc`, 6.464 arquivos CRC PASS. Não reiniciar. Preservar os módulos P197–P208, o banco, os saves e Kokoro. O P209 não é APK nem homologação física.

## Implementação REAL P209

Fonte consultada diretamente: PDF oficial SWE09 *The Jewel of Yavin*, Google Drive id `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, página impressa 94, seção AFTER THE BATTLE (encontro inicial no starport). O texto determina **dois grupos minion de dois oficiais Wing Guard e um sargento**. Não é a patrulha bancária P193 de tamanho variável.

- `app/src/main/assets/vtt/js/adventure/jewelOfYavin.mjs`: `holo-park-wing-guard` é um encontro publicado real após a resolução do confronto anterior com os capangas de Arend. Dois grupos separados de oficiais, `count=2` cada, e um sargento rival; perfis oficiais `bespin_wing_guard_officer` e `bespin_wing_guard_sergeant`, inicialmente Médio, sem inventar atributos. A condição `activeEncounterId` preserva a capacidade de encerrar o encontro já iniciado; P208 com Holo-Park iniciado fora desse fluxo permanece válido.
- `js/core/controller.mjs`: `beginJewelHoloParkWingGuard` exige confirmação explícita de chegada física e presença da Guarda Alada, ausência de combate prévio e fuga anterior resolvida. Em uma única `atomicAction`, usa `beginPublishedAdventureEncounter` e `enterJewelHoloPark`, iniciando iniciativa real e persistindo `combat.publishedAdventureContext`, `JEWEL_HOLO_ENTER`, d100 da paisagem e estado do Holo-Park. Erros causam rollback sem saves parciais. A rota genérica não pode iniciar esse encontro sem confirmação especial.
- `js/core/uiBridge.mjs`, `r46/final-runtime.js`: botão específico Android PT-BR com as duas confirmações do Mestre. O botão genérico é ocultado nesse encontro para não contornar a confirmação.
- `js/adventure/jewelOfYavinOperations.mjs`: corrigida lista de recibos comuns para aceitar os tipos já implementados P205–P208 (`JEWEL_ARROW7_*`, `JEWEL_AREND_*`, `JEWEL_HOLO_*`, `JEWEL_COMPANION_IMPACT`). Cada módulo continua validando sua própria semântica.
- Testes antigos P205–P207 de versão Android ajustados para exigir versão mínima, não a igualdade com P208.

**Limites deliberados:** a fonte p.92 descreve três grupos procurando os PCs, mas o ataque inicial da p.94 é Wing Guard. Não criar automaticamente os capangas de Arend nem os seguranças de Grayson, nem duplicar atores existentes. Os efeitos especiais das quatro paisagens ainda não estão ligados ao motor. Sem XP/Obligation automática, teleporte, distância inventada ou avanço artificial do tempo.

## Testes e plataforma

`tests/uae-p209-holopark-wing-guard.test.mjs`: **7/7** focais, incluindo composição oficial, confirmação GM, bloqueio da rota genérica, iniciativa e estado reais, uma única persistência atômica, três reaberturas, desfecho e UI/bridge. Regressão final `evidence/P209/TEST_SUMMARY.json`: **328 arquivos regulares 3.023/3.023 + seis stress 6/6 = 3.029/3.029 em 334 arquivos individuais**, sem suíte monolítica. Sintaxe `evidence/P209/SYNTAX_AND_JAVA.json`: 497 MJS, dois JS runtime, 22 JSON, dois XML PASS. **8/8 Java/Kokoro byte-idênticos a P208**. Android `versionName=1.0.209`, `versionCode=10209`, bundle offline **103 módulos**.

**Não foi executado:** SDK/Gradle, adb, APK, CI BRELGA, instalação no Galaxy ou homologação física. Código P209 não foi enviado ao GitHub.

## Próxima prioridade P210+

1. Implementar as outras facções do Holo-Park **com perfis oficiais reais**, gatilhos de entrada, distinção entre minions existentes e reforços, combate persistido e desfechos sem teleporte; conferir SWE09 pp.92–94 e compêndio.
2. Implementar efeitos dos símbolos narrativos de Alderaan/Christophsis/Coruscant/Kashyyyk (SWE09 pp.93–94) sobre as fichas e combate reais, sem confundir resultado do dado com símbolo narrativo. Considerar modo narrativo de dois minutos com tempo confirmado.
3. Desfechos Arend, Aris e perdão de Lando (+5 Obligation: Favor por personagem somente com gatilhos oficiais); Arrow-7 avançado apenas se ficha oficial completa for localizada, nunca inventar stats.
4. SWE02 New Meen/Geonosis/Teemo; cinco aventuras cena a cena; 360 consumidores EotE/AoR/expansões individualmente certificados; APK BRELGA e testes Android reais.

## Continuidade obrigatória

Validar SHA/CRC P209, preservar P208 e Java/Kokoro. Fazer mudanças de código e testes focais, depois regressão por arquivo e seis stress. Gerar ZIP Android Studio integral, SHA externo, delta com manifesto de hashes, handoff e evidências **somente com implementação real**. Upload na Biblioteca `/STAR_WARS_EDGE_OF_THE_GALAXY_WORKING/checkpoints/P210` e readback SHA/CRC; não afirmar readback sem realmente recuperar os bytes. GitHub/Gradle/BRELGA apenas com acesso verificável. Manter tarefa ativa até homologação comprovada.
