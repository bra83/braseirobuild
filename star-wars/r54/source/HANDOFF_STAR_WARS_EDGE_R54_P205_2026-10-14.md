# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P205

## Base preservada

P204 SHA-256 `6efdf23474232b9c770aec896e5fb1d00d6489f4ba24da25324dc5d8ef89366a`, 6.421 arquivos. Projeto Android Studio completo preservado. P205 acrescenta o módulo de canhão Arrow-7 e integração com combate, sem alterar o código Java/Kokoro.

## Implementação REAL P205 — Arrow-7 (SWE09, p.90)

O PDF oficial *The Jewel of Yavin* no Drive (`1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`), seção *Time for a New Plan*, confirma **quatro capangas de Arend Shen a alcance Médio**, o **Arrow-7 customizado** e sua arma **traseira de escala PESSOAL**: blaster de repetição leve, dano 11, crítico 3, alcance Extremo, Auto-Fire e Perfuração 1. O veículo tem um piloto, seis passageiros, carga 20, preço 12.000, raridade 2 e dois hard points. O PDF não forneceu no texto extraído os valores completos de velocidade, blindagem e casco: NÃO inventar.

Novo `app/src/main/assets/vtt/js/adventure/jewelArrow7Turret.mjs`: exige encontro oficial `arend-shen-four-goons` ativo, artilheiro real do grupo, confirmação expressa pelo Mestre do arco traseiro e da distância atual. Agenda um único disparo por rodada no turno de iniciativa NPC real, usando **Artilharia** com graduação de grupo de minions sobreviventes. O motor de combate existente aplica a arma de escala pessoal, dados, Absorção, Perfuração 1, dano e possíveis críticos. Auto-Fire está disponível como qualidade mas **não produz acertos extras automáticos**. Nenhum teleporte, velocidade, tempo ou perseguição foi inventado.

Recibos `JEWEL_ARROW7_TURRET` com `contractVersion:1`, replay de arma/alcance/arco/rodada/identidade e idempotência; estado `publishedAdventure.operations.arrow7Turret` e `combat.arrow7TurretOrder` persistidos em `WORLD_UPDATED` + `COMBAT_ACTION` atômicos. O disparo real muda o estado da ordem para `FIRED`; a arma pessoal carabina permanece o padrão quando não há ordem. Saves P204 sem operação Arrow-7 continuam válidos. UI Android PT-BR e bridge adicionados. `versionName 1.0.205`, `versionCode 10205`, bundle offline 100 módulos, oito Java/Kokoro byte-idênticos a P204.

## Evidência e limites

Ver `evidence/P205/` para testes por arquivo, sintaxe/Java, cotejo PDF DIRECIONADO e evidência de implementação. NÃO alegar suíte monolítica, APK, CI BRELGA, SDK/Gradle/adb, homologação física ou certificação integral.

**P206+:** perseguição Arrow-7 real com movimento, distâncias, manobras e efeitos veiculares conforme fontes completas; distinguir escala pessoal e planetária, não inventar ficha faltante. Desfechos Arend/Naboka com gatilhos oficiais; SWE02 New Meen/Geonosis/Teemo; cotejar cinco aventuras cena a cena e certificar individualmente 360 consumidores EotE/AoR/expansões. Preservar Kokoro/saves. Rodar testes focais + regressão por arquivo + seis stress. Gerar ZIP integral, SHA, delta, handoff, salvar na Biblioteca e readback CRC/SHA. GitHub/BRELGA somente com execução verificável. Manter continuidade até homologação comprovada.
