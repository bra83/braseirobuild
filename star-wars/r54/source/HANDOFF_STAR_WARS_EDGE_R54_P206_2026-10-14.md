# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P206

## Base imutável

P205 ZIP SHA-256 `3a850cee7344c5ba3105f1371065f8e6364f003f59837312b21541166e19f21b`, 98.460.630 bytes, 6.430 arquivos CRC PASS. P206 construído **sobre P205**, sem reinicialização. Oito arquivos Java/Kokoro permaneceram byte-idênticos.

## Implementação REAL P206 — Arrow-7 / The Chase

Cotejo **direcionado** com PDF oficial SWE09 *The Jewel of Yavin*, Drive `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, p90, e PDF *Edge of the Empire Core Rulebook*, Drive `1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX`, capítulo VII, seção *The Chase*. Não foi feito cotejo integral das cinco aventuras.

Novo `app/src/main/assets/vtt/js/adventure/jewelArrow7Chase.mjs`: testa **Pilotagem (Planetária)** de dois pilotos em dados narrativos reais no início da rodada do encontro `arend-shen-four-goons`. O piloto do jogador usa a ficha carregada; o piloto NPC exige Agilidade e graduação de uma ficha existente explicitamente confirmada pelo Mestre. O PDF SWE09 não fornece essas estatísticas do piloto, portanto NÃO inventar perfil. O Mestre confirma ambos os veículos, a distância inicial em **escala planetária**, o terreno e a dificuldade (0 simples, 1–5 se houver perigos). Resultado competitivo usa sucessos líquidos positivos: empate ou duas falhas não movem veículos. Vencedor aproxima ou afasta uma faixa; **somente quando ambas velocidades atuais forem verificadas** a diferença favorável acrescenta faixas. Não inventar velocidade máxima ou duração de ações. `Bocajarro/Curto/Médio/Longo/Extremo` planetários são independentes do alcance **pessoal** da torre P205. Contato ou fuga além de Extremo exigem `confirmJewelArrow7ChaseOutcome` do Mestre; encontro não é encerrado automaticamente.

Recibos `JEWEL_ARROW7_CHASE` e `JEWEL_ARROW7_CHASE_END` com replay de faces, pools, símbolos, identidade, terreno, distância, ordem, velocidades verificadas, desfecho e duplicatas. Persistência em `publishedAdventure.operations.arrow7Chase` e `combat.arrow7Chase`, eventos atômicos `WORLD_UPDATED` + `COMBAT_ACTION`, rollback e três reaberturas. Bridge/controller/UI Android PT-BR integrados; `versionName 1.0.206`, `versionCode 10206`, bundle offline 101 módulos.

## Testes e integridade

Ver `evidence/P206/TEST_SUMMARY.json`, `PER_FILE_RESULTS.json`, `SYNTAX_AND_JAVA.json`, `JAVA_PRESERVATION.json`, `PDF_TARGETED_SOURCE_CROSSCHECK.json`, `IMPLEMENTATION_EVIDENCE.json`. **325 arquivos regulares 2.995/2.995 + 6 stress 6/6 = 3.001/3.001 em 331 arquivos individuais**. Testes focais P206 12/12. Não houve suíte monolítica. Sintaxe 135 MJS, 16 JS, 22 JSON e 2 XML aprovada; Java 8/8 byte-idêntico P205. A regressão inicial identificou três verificações antigas de versão do shell; shell sincronizado e teste histórico P205 atualizado para conferir a versão atual. Regressão global final repetida sem falhas.

## Pendências reais P207+

1. **Perseguição expandida**: obter fonte para piloto NPC, velocidade máxima, casco, blindagem, manobrabilidade e tensão de sistema do Arrow-7; integrar manobras/colisões/dano e interações de escala pessoal/planetária sem valores inventados. P206 cobre competição de distância e decisão GM, não certifica todas as manobras.
2. **SWE09**: desfechos de Arend Shen e Naboka conforme gatilhos oficiais, Flight over Fight, encontros/facções e consequências.
3. **SWE02**: New Meen/Geonosis/Teemo com estado, regras e testes reais.
4. **Certificação**: cotejo cena a cena das cinco aventuras e certificação semântica individual dos 360 consumidores EotE/AoR/expansões.
5. **Android**: build Gradle/APK real, CI BRELGA, instalação e três reaberturas em aparelho. Nenhum desses itens executado no P206.

## Regras de continuidade

Não reiniciar, não regredir P205, não tocar Kokoro. Testar focais + regressão global por arquivo + seis stress. Gerar ZIP Android Studio completo, SHA, delta e handoff apenas com implementação real; preservar na Biblioteca e confirmar readback SHA/CRC. GitHub `bra83/braseirobuild` somente com fonte correta e execução verificável; P206 NÃO foi enviado ao GitHub e CI NÃO foi disparado. Não declarar APK, certificação ou homologação sem prova.
