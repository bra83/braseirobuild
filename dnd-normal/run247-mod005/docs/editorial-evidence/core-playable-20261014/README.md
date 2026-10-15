# Núcleo jogável 2024 — pacote de criação e cenário comportamental

## Resultado e limite

O núcleo **não está certificado como sessão completa**. Foi corrigido um defeito
concreto de seleção de edição na criação atômica e preparado um cenário Robolectric
com gateways Android reais. A execução Android e a compilação foram bloqueadas
pela infraestrutura antes de executar testes ou compilar o aplicativo.

Nenhum APK, commit, push ou merge foi produzido. Não foram alterados Mystara,
DB61, Backup21, identidade, assinatura, workflows ou sopros de dragões.

## Menor fluxo real selecionado

`AppDb.saveCampaign` → `saveCharacterSheetAtomic` →
`WorldStateEngine.commitGameplayInput` → `addCompiledStatblockCombatant` →
`moveCombatant` / `applySingleWeaponAttackForPartyAction` /
`applyCompiledStatblockAttack` → fechamento e reabertura de SQLite →
`exportAll` / `importAll` repetido → `WorldStateEngine.preview/commit` para
descansos → `applyCharacterShortRestHitDice` → XP explícito do Mestre →
novo backup e reabertura.

Usa campanha 2024, clériga de nível 1, inventário equipado e Goblin Warrior real
do XMM (10 PV, CA 15, Scimitar). Não instala módulos de aventuras. Exploração
é continuidade sandbox confirmada pelo Mestre, sem geração de encontro por IA.

O teste verifica edição, espaços de magia, movimento, dano nos dois sentidos,
sincronização de PV da ficha, rejeição de ataque repetido e de segunda ação,
condição manual, turno persistido, recursos, localização, importação sem duplicar
heróis/combate/diário, cura por Dado de Vida e recuperação por descanso longo.
Uma resolução complexa pendente não muda o sandbox; resolução manual confirmada
permite continuidade sem inferir mecânica. XP não é concedido automaticamente a
uma criatura apenas ferida; há uma concessão explícita de 50 XP por objetivo.

## Correção

`saveCharacterSheetAtomic` gravava `rules_source=""` quando a ficha omitia a fonte.
`optString` não substitui um valor SQL vazio pelo fallback; `canonicalProfile("")`
seleciona 2014. A ficha e seus recursos podiam discordar da campanha 2024.

Agora uma criação sem fonte usa a edição da campanha antes de estruturar recursos.
Uma edição sem fonte conserva a fonte do personagem salvo, depois a edição da
ficha salva e, para legado sem metadados, 2014. Fontes explícitas são conservadas.
Há um teste separado para editar personagem 2014 dentro da campanha 2024.

## Validação executada

- Comparador XMM com `/tmp/xmm-source`: passou; 503 criaturas, 36 seções e
  contratos mecânicos comparados contra fontes pinadas. Ver `xmm-comparison.json`.
- 59 testes dos comparadores XMM: passaram; ver `comparator-tests.log`.
  A primeira invocação sem `PYTHONPATH=tools` falhou por importação; foi corrigida
  a invocação, sem modificar testes.
- Parser JDK dos dois arquivos Java: zero erros de sintaxe. Não equivale a compilação.
- `git diff --check`: passou.
- Wrapper Gradle 8.11.1: download falhou com `SocketException: Operation not permitted`.
  A primeira tentativa também encontrou wrapper sem permissão executável e cache
  padrão não gravável; foi usada chamada `bash` e cache em `/tmp`.
- Gradle instalado 9.7.1, offline: falhou antes da configuração com
  `Could not determine a usable wildcard IP for this machine`.

## Pendências que impedem afirmar uma sessão completa

1. Executar os testes Robolectric novos e as regressões no BRELGA. O cenário
   ainda pode revelar falhas de gateway ou persistência; nenhum resultado verde
   Android é alegado aqui.
2. Compilar o app com a infraestrutura Android compatível e validar o fluxo pela
   UI em dispositivo. Este teste usa gateways, não percorre os diálogos de criação,
   seleção de magias, exploração, combate e backup pela tela.
3. Cobrir salvaguardas reais de criaturas XMM, combate em múltiplas rodadas até
   desfecho, progressão com subida de nível e retomada de uma habilidade complexa
   de criatura pela UI. O cenário atual não certifica esses comportamentos.
4. Validar a seleção normativa de perícias, equipamentos e magias na criação:
   o teste fornece as escolhas à ficha; não certifica o catálogo ou legalidade de
   todas as combinações XPHB. Nenhuma fonte nova foi inventada.
5. O comparador identifica 168 habilidades com efeitos não modelados mantidos
   manuais; isso exige resolução assistida na mesa, não automação por Gemini/RAG.

Comando focado para BRELGA (sem APK):

```sh
bash gradlew :app:testDebugUnitTest -Pbraseiro.qaLiteAssets=true \
  --tests com.braseiro.dnd5e.e2e.Core2024PlayableSessionRobolectricTest \
  --tests com.braseiro.dnd5e.CoreRulesProfileDefaultRobolectricTest \
  --tests com.braseiro.dnd5e.AppDbBackupRobolectricTest
bash gradlew :app:compileDebugJavaWithJavac
```
