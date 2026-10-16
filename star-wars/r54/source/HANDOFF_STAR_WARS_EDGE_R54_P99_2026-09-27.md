# HANDOFF — BRASEIRO STAR WARS EDGE R54 P99

Data local de continuidade: 27/09/2026
Baseline de entrada: `BRASEIRO_STAR_WARS_EDGE_R54_P98_SOURCE_CHECKPOINT.zip` persistida no Google Drive.
Princípios: projeto não recriado; GitHub não usado; Android/Gradle não executado; EotE/AoR/MIXED, Duty/Obligation, Sob um Sol Negro, Além da Orla, Mundo Vivo, anti-spoiler e integrações anteriores preservados.

## OBJETIVO DESTE BLOCO
Fechar Derrubar/Prone no combate pessoal e transformar attachments de leitura em um primeiro ciclo mecânico real, source-bound e transacional, sem importar cegamente catálogo incompleto nem inventar Mods/Signature Abilities.

## IMPLEMENTADO

### Derrubar / Prone
- `Derrubar/Knockdown` entra no ledger único de Vantagens do ataque.
- custo base: 2 Vantagens; +1 Vantagem por ponto de Silhueta do alvo acima do atacante.
- ativação aplica `prone` persistente no alvo.
- melee contra alvo caído recebe Boost; ranged contra alvo caído recebe Setback.
- jogador caído precisa gastar manobra real para levantar; regras de segunda manobra/Tensão/ação continuam valendo.
- NPC caído levanta no próprio turno antes de agir.
- nenhum bridge permite aplicar `prone` arbitrariamente.

### Attachments source-bound
Novo módulo canônico `rules/attachments.mjs` com cinco perfis cujo efeito base está explicitamente validado em Gadgets and Gear:
- Rapid-Recharge XCiter — p. física 89 — 1 HP — 750 cr — R7 — Automática + Imprecisa 3.
- Spread Barrel — p. física 91 — 2 HP — 1725 cr — R4 — Explosão 4 + alcance -1 faixa (mínimo Interação).
- Bowcaster Accelerator Enhancement — p. física 83 — 1 HP — 250 cr — R4 — +1 dano — somente bowcaster compatível.
- Rocket Guidance System — p. física 89 — 1 HP — 50 cr — R4 — Guiada 2 — somente micro-rocket compatível.
- Staged Rocket Booster — p. física 91 — 1 HP — 250 cr — R2 — alcance +1 + Imprecisa 2 — somente micro-rocket compatível.

### Economia / propriedade / hard points
- `attachment` é agora um tipo econômico válido.
- aquisição usa o mesmo fluxo transacional do projeto: disponibilidade/raridade → Mestre → créditos → propriedade.
- compra não instala automaticamente.
- instalação só aceita attachment realmente possuído, arma realmente possuída, compatibilidade válida e HP suficientes.
- remoção desfaz o efeito mas mantém propriedade.
- ativo instalado não pode ser vendido/perdido até ser removido.
- corrigido bug de hard points: armas adquiridas usam `sourceCatalogId`, que agora recupera os HP do catálogo corretamente.

### Efeito no ataque real
- attachments não são apenas metadados: `effectiveWeaponWithAttachments()` entra no `resolveWeaponAttack`.
- Rapid-Recharge aplica Imprecisa 3 ao pool e habilita Automática com aumento real de dificuldade.
- Spread Barrel aplica Explosão 4 e reduz o alcance real em uma faixa.
- runtimeState (Preparação, Munição Limitada, Cadência Lenta) permanece sincronizado entre a arma base e o perfil efetivo; corrigida regressão detectada pelo gate P96.

### UI / Bridge
- R46 rota de equipamento mostra catálogo autorizado, attachments adquiridos, arma-alvo e hard points.
- aquisição passa por `proposeAttachmentAcquisition`.
- instalação/remoção usam `installOwnedAttachment` / `removeOwnedAttachment`.
- `attachmentSummary` é somente leitura de catálogo/propriedade/instalação.
- não existe `modifyAttachment` ou botão de Mod arbitrário.

### Mods e Signature Abilities — NÃO PROMOVIDOS
- opções de Mod dos attachments permanecem bloqueadas: a fonte importada não estrutura de forma suficiente dificuldade/custo/rolagens para todos os Mods.
- auditoria do pacote antigo confirma somente 7 Signature Abilities para 4 carreiras EotE e runtime incompleto; Colonist/Explorer/AoR faltam.
- nada foi importado cegamente e nenhuma dessas famílias foi promovida para JOGÁVEL.

## MATRIZ P99
- 55 famílias canônicas.
- JOGÁVEL: **31/55 = 56,4%**.
- ENGINE REAL: 4.
- PARCIAL: 3.
- RAG/DECLARAÇÃO: 17.
- Engine determinístico real: **35/55 = 63,6%**.
- classificação mantida conservadora: `EQUIP.QUALITIES` e `EQUIP.ATTACHMENTS` continuam PARCIAIS.

## EVIDÊNCIA FINAL
- P99 focal (Derrubar + attachments): incluído no conjunto P92→P99.
- P92→P99 focal final: **90/90 PASS**.
- Sob um Sol Negro: **106/106 PASS**.
- Além da Orla: **57/57 PASS**.
- Regressão não-stress: **1109/1109 PASS, 0 FAIL**.
- Stress: **6/6 PASS, 0 FAIL**.
- Total automatizado: **1115/1115 PASS, 0 FAIL**.
- Sintaxe: PASS.
- Bundle offline: **64 módulos**.

## ANDROID
NÃO EXECUTADO. Compilar Android/APK somente no fechamento final, conforme instrução explícita do usuário.

## VERSÃO
- Android versionCode: 10099
- Android versionName: 1.0.99
- Runtime/shell: 1.0.99
- Bundle label: 1.0.99-r54-p99

## PRÓXIMO BLOCO P100
1. Estruturar Mods de attachments somente quando dificuldade/custo/efeitos estiverem source-bound e testáveis.
2. Completar Signature Abilities por carreira + runtime real de ativação, sem importar o catálogo antigo incompleto como se fosse total.
3. Fechar manobras/ações pessoais/ambientais ainda com engine sem UI.
4. Continuar auditoria de qualidades derivadas de equipamento/veículos e promover `EQUIP.QUALITIES` somente após cobertura integral comprovada.
5. Manter Android/Gradle adiado até o fechamento final.
