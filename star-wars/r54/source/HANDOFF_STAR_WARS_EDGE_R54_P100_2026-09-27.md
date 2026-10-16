# HANDOFF — BRASEIRO STAR WARS EDGE R54 P100

Data: 27/09/2026  
Baseline de entrada: `BRASEIRO_STAR_WARS_EDGE_R54_P99_SOURCE_CHECKPOINT.zip`, P99 persistida no Google Drive.  
Princípios: projeto não recriado; GitHub não usado; Android/Gradle não executado; EotE/AoR/MIXED, Obligation, Duty/Contribution, Mundo Vivo, Sob um Sol Negro, Além da Orla e anti-spoiler preservados.

## OBJETIVO DESTE BLOCO
Fechar um ciclo mecânico real de Mods para os cinco attachments source-bound do P99 e adicionar Postura Defensiva como manobra de combate pessoal, sem promover famílias incompletas por catálogo.

## IMPLEMENTADO

### Mods de attachments source-bound
- `attachments.mjs` agora contém Mods estruturados apenas para os cinco perfis já autorizados no P99:
  - Rapid-Recharge XCiter;
  - Spread Barrel;
  - Bowcaster Accelerator Enhancement;
  - Rocket Guidance System;
  - Staged Rocket Booster.
- cada tentativa exige attachment adquirido + instalado + Kit de ferramentas + 100 créditos;
- primeiro teste de Mecânica usa dificuldade 3; cada Mod já instalado aumenta a dificuldade em +1;
- falha consome aquela opção de Mod e não concede efeito;
- opções repetidas mantêm slots independentes;
- não é permitido modificar em combate;
- sem créditos ou sem Kit de ferramentas, a tentativa é bloqueada antes de gastar;
- efeitos source-bound suportados: qualityDelta (Imprecisa/Explosão/Guiada/Precisa), quality grant (Automática) e rangeDelta.

### Persistência correta dos Mods
- Mods/falhas são autoridade do attachment adquirido (`group.ownedAssets[].state`), não apenas da cópia instalada na arma;
- remover attachment preserva `mods` e `modAttempts`;
- save/reopen preserva Mods;
- reinstalar restaura os mesmos Mods/efeitos;
- `attachmentSummary()` mostra progresso de Mods mesmo quando o attachment está removido;
- attachment inutilizado permanece marcado como tal.

### Efeito no ataque real
- `effectiveWeaponWithAttachments()` aplica Mods instalados sobre efeitos-base;
- Rapid-Recharge com duas reduções de Imprecisa reduz de 3 para 1;
- teste comparativo comprova 2 Setbacks a menos no `resolveWeaponAttack` real contra a mesma situação;
- Automática continua usando sua dificuldade real.

### Postura Defensiva
- nova `takeGuardedStance()` no controller/bridge/R46;
- consome manobra real pelo mesmo `spendManeuver`;
- pode usar os métodos legítimos de segunda manobra já suportados;
- não acumula enquanto ativa;
- adiciona 1 Setback aos testes de combate feitos pelo personagem;
- adiciona 1 Setback a ataques melee/Brawl feitos contra o personagem;
- não adiciona esse Setback a ataques ranged contra ele;
- efeito termina pelo limite de round e é removido no encerramento do combate.

### R46 / contratos
- painel de combate recebe botão `POSTURA DEFENSIVA`;
- painel de attachments recebe seção `MODS DE ATTACHMENTS`;
- texto antigo P99 que dizia “Mods continuam bloqueados” foi atualizado;
- `uiBridge.mjs`, contratos TS e bundle offline incluem as novas chamadas;
- nenhum método de mutação bruta de stance/mod foi exposto.

### Proveniência interna
- `RETOMADA_ATUAL.md` e `ESTADO_ATUAL.md` agora apontam P100;
- `LEIA_PRIMEIRO.md` e `README.md` receberam cabeçalho canônico P100 para impedir retorno acidental a P20/P79.

## MATRIZ MECÂNICA P100
- 55 famílias canônicas.
- JOGÁVEL: **31/55 = 56,4%**.
- ENGINE REAL sem UI completa: 4.
- PARCIAL: 3.
- RAG/DECLARAÇÃO: 17.
- Engine determinístico real: **35/55 = 63,6%**.
- `EQUIP.ATTACHMENTS` permanece PARCIAL: Mods estão completos apenas para o subconjunto source-bound autorizado.
- `COMBAT.MANEUVERS` continua ENGINE REAL/PARCIAL na UI: Postura Defensiva foi adicionada, mas ainda não há cobertura integral de todas as manobras contextuais.
- `EXP.EOTE.SIGNATURE` permanece PARCIAL.

## EVIDÊNCIA FINAL
- gate P100 novo: **9/9 PASS**.
- P92→P100 focal: **99/99 PASS**.
- Sob um Sol Negro: **106/106 PASS**.
- Além da Orla: **57/57 PASS**.
- regressão não-stress: **1118/1118 PASS, 0 FAIL**.
- stress: **6/6 PASS, 0 FAIL**.
- total automatizado: **1124/1124 PASS, 0 FAIL**.
- sintaxe dos módulos/runtime/bundle/testes alterados: PASS.
- bundle offline: **64 módulos**.

## CONTRATO LEGADO CORRIGIDO
O teste P77 limitava artificialmente a versão até `1.0.99`; foi alterado para validar numericamente `1.0.x` com `x >= 77`, permitindo `1.0.100` sem enfraquecer o requisito de versão mínima.

## ANDROID
**NÃO EXECUTADO**, conforme instrução explícita do usuário: Android/Gradle/APK somente no fechamento final.

## VERSÃO
- Android versionCode: 10100
- Android versionName: 1.0.100
- shell/runtime: 1.0.100
- bundle label: 1.0.100-r54-p100

## PRÓXIMO BLOCO P101
1. Ampliar attachments/Mods somente com perfis source-bound cuja base + Mods estejam completos.
2. Auditar e conectar ações/manobras/ambiente que realmente tenham engine determinístico, corrigindo a matriz se evidências antigas estiverem stale.
3. Signature Abilities só avançam quando catálogo e ativação de efeitos forem completos; não importar o pacote antigo incompleto como autoridade.
4. Preservar os gates P92→P100 e as duas aventuras.
