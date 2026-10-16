# HANDOFF — BRASEIRO STAR WARS EDGE R54 P98

Data: 27/09/2026  
Baseline de entrada: `BRASEIRO_STAR_WARS_EDGE_R54_P97_SOURCE_CHECKPOINT.zip` persistida no Google Drive.  
Princípios: projeto não recriado; GitHub não usado; Android/Gradle não executado; EotE/AoR/MIXED, Duty/Obligation, Sob um Sol Negro, Além da Orla, Mundo Vivo, anti-spoiler e todo o P97 preservados.

## OBJETIVO DO BLOCO
Aprofundar fidelidade do combate veicular/qualidades sem inflar a matriz: fechar Iônica, Guiada, Raio Trator, Vinculada veicular, Munição Limitada/Cadência Lenta e unificar o gasto de Vantagens/Triunfos no ataque veicular.

## IMPLEMENTADO

### Orçamento único veicular
- Critical Hit, Guiada e Vinculada usam o mesmo `pendingVehicleSpend`.
- Vantagens/Triunfos gastos deixam de poder ser reutilizados por outra ativação.
- escolha de componente de Critical Hit preserva o pagamento/rolagem original e não cobra duas vezes.

### Iônica
- ataque Iônico aplica Tensão de Sistemas, não Trauma de Casco.
- armadura/Brecha continuam sendo consideradas na redução.
- ataque Iônico não abre Critical Hit de casco.

### Guiada
- falha elegível permite ativação por 3 Vantagens.
- ativação agenda nova passagem ao fim da rodada.
- novo ataque usa pool baseado no rating Guiada e defesa do alvo.
- nova falha com >=3 Vantagens entra em `guidedReactivations`.
- reativação legítima passa pelo bridge/R46; sem dano ou acerto implícito.

### Raio Trator
- acerto mantém bloqueio real de movimento no alvo.
- R46 oferece apenas `attemptTractorEscape`, usando o teste de Pilotagem existente.
- nenhum método público limpa Tractor Lock diretamente.

### Vinculada veicular
- cada ativação custa 2 Vantagens.
- respeita o rating máximo.
- acerto adicional usa o mesmo snapshot/dano do ataque original.
- compartilha orçamento com crítico e Guiada.

### Munição Limitada / Cadência Lenta veicular
- contadores persistem no `runtimeState` da arma.
- munição chega a zero e bloqueia disparo.
- Slow-Firing registra round e impede disparo antes do intervalo.

### R46 / Bridge
- painel veicular P98 mostra saldo único de Vantagens/Triunfos.
- permite ativar Guiada/Vinculada quando legitimamente disponíveis.
- mostra reativações Guiada pendentes.
- mostra estado de munição/cadência.
- mostra fuga legítima de Trator quando o veículo ativo está preso.
- controle antigo de ataque veicular redundante foi ocultado para evitar dois fluxos concorrentes.

## MATRIZ P98
- 55 famílias canônicas.
- JOGÁVEL: **31/55 = 56,4%**.
- ENGINE REAL sem UI completa: 4.
- PARCIAL: 3.
- RAG/DECLARAÇÃO: 17.
- Engine determinístico real: **35/55 = 63,6%**.
- `EQUIP.QUALITIES` continua **PARCIAL** deliberadamente; P98 não promove a família enquanto Derrubar e a cobertura integral dos perfis oficiais/attachments não estiverem fechados.

## EVIDÊNCIA FINAL
- P98 focal final P92→P98: **75/75 PASS**.
- Sob um Sol Negro: **106/106 PASS**.
- Além da Orla: **57/57 PASS**.
- Regressão regular: **1094/1094 PASS, 0 FAIL**.
- Stress: **6/6 PASS, 0 FAIL**.
- Total automatizado: **1100/1100 PASS, 0 FAIL**.
- Sintaxe vehicles/controller/uiBridge/runtime/bundle: PASS.
- Bundle: 63 módulos, `1.0.98-r54-p98`.

## ANDROID
**NÃO EXECUTADO**, conforme instrução explícita do usuário: compilar Android/APK somente no fechamento final.

## VERSÃO
- Android versionCode: 10098
- Android versionName: 1.0.98
- Runtime/shell: 1.0.98
- Bundle label: 1.0.98-r54-p98

## PRÓXIMO BLOCO P99
1. Fechar Derrubar/Knockdown com custo/limites source-bound.
2. Estruturar attachments/modificações apenas com perfis mecânicos autorizados e machine-readable.
3. Completar Signature Abilities por carreira + runtime de ativação, sem importar catálogo legado incompleto às cegas.
4. Fechar ações/manobras veiculares restantes e contramedidas de Guiada somente onde houver regra source-bound.
5. Reauditar `EQUIP.QUALITIES`; promover apenas com cobertura integral comprovada.
