# HANDOFF — BRASEIRO STAR WARS EDGE R54 P96

Data: 27/09/2026
Baseline de entrada: `BRASEIRO_STAR_WARS_EDGE_R54_P95_SOURCE_CHECKPOINT.zip`.
Princípios: projeto não recriado; GitHub não usado; Android/Gradle não executado; EotE/AoR/MIXED, Duty/Obligation, Sob um Sol Negro, Além da Orla, Mundo Vivo, anti-spoiler e TTS preservados.

## OBJETIVO DO BLOCO
Fechar Critical Hits veiculares ligados ao combate real e ampliar qualidades de armas source-bound sem promover mecanicamente aquilo que ainda depende de adjudicação/catálogo.

## IMPLEMENTADO

### Critical Hits veiculares
- `resolveVehicleAttack()` cria `pendingTargetCritical` somente quando:
  - o ataque real acertou;
  - houve dano no alvo;
  - o resultado permite Critical Hit.
- `activatePendingVehicleCritical()`:
  - atua somente no veículo-alvo real do combate;
  - usa a tabela d100 existente;
  - considera críticos prévios;
  - persiste o resultado;
  - não permite bridge público para criar crítico arbitrariamente.
- Quando a entrada d100 exige componente, a primeira chamada retorna `componentRequired` e lista escolhas válidas.
- O d100 original fica armazenado no pending e é reutilizado depois da escolha do componente; não há rerrolagem.
- Corrigido bug em que `rawRoll=null` virava `0` via `Number(null)` e era tratado como rolagem válida.
- P95 já havia fechado reparo de Critical Hit via teste real de Mecânica; P96 completa o ciclo ataque→crítico→persistência→reparo.

### Qualidades de armas source-bound
Implementadas/aprofundadas no fluxo real:
- Precisa/Accurate — já preservada do P92.
- Automática/Auto-fire — preservada.
- Brecha/Perfurante — preservadas no soak.
- Letal/Viciosa — preservada no d100 crítico.
- **Imprecisa/Inaccurate** — Contratempos reais no pool.
- **Aparatosa/Cumbersome** — dificuldade extra pelo déficit de Fortaleza/Brawn.
- **Desajeitada/Unwieldy** — dificuldade extra pelo déficit de Agilidade.
- **Defensiva/Defensive** — defesa corpo a corpo real.
- **Deflexão/Deflection** — defesa ranged real.
- **Preparação/Prepare** — exige manobras reais; progresso persiste e é consumido no ataque.
- **Munição Limitada/Limited Ammo** — contador por arma; bloqueia quando chega a zero; recarga exige e consome `Recarga adicional` realmente possuída.
- **Cadência Lenta/Slow-Firing** — persiste assalto do último disparo e bloqueia novo disparo durante o cooldown.

`EQUIP.QUALITIES` continua PARCIAL: ainda faltam qualidades ativas e seus custos/efeitos completos, como Explosão, Desorientação, Concussiva, Derrubar, Ensnare, Hendedura e Vinculada.

### R46 / bridge
- painel de qualidades da arma mostra estado real de Prepare/Limited Ammo/Slow-Firing e modificadores passivos.
- comandos seguros para Preparar e Recarga adicional.
- painel de Critical Hit veicular do alvo permite somente ativar um crítico criado pelo ataque real.
- escolha de componente é solicitada quando a tabela exige.
- bridge não expõe criação arbitrária de Critical Hit.

## MATRIZ P96
- 55 famílias canônicas.
- **31 JOGÁVEIS = 56,4%** (P95: 30/55).
- 4 ENGINE REAL sem UI completa.
- 3 PARCIAIS.
- 17 RAG/DECLARAÇÃO.
- 35/55 = 63,6% continuam com engine determinístico real sem RAG.
- `VEHICLE.CRITICAL` promovido de ENGINE REAL para JOGÁVEL.
- `EQUIP.QUALITIES` permanece PARCIAL por decisão conservadora.

## EVIDÊNCIA FINAL
- Focal P92→P96: **53/53 PASS**.
- Sob um Sol Negro: **106/106 PASS**.
- Além da Orla: **57/57 PASS**.
- Regressão não-stress: **1072/1072 PASS, 0 FAIL**.
- Stress: **6/6 PASS, 0 FAIL**.
- Total automatizado: **1078/1078 PASS, 0 FAIL**.
- Sintaxe: PASS.
- Bundle: 63 módulos, `1.0.96-r54-p96`.

## ANDROID
NÃO EXECUTADO. Usuário determinou compilação Android/APK somente no fechamento final.

## VERSÃO
- versionCode: 10096
- versionName: 1.0.96
- runtime/shell: 1.0.96
- bundle: 1.0.96-r54-p96

## PRÓXIMO BLOCO P97
1. Qualidades ativas restantes source-bound.
2. Attachments/modificações completos somente se houver perfis mecânicos autorizados suficientes.
3. Manobras/ações e hazards/medo contextuais.
4. Signature Abilities completas e ativação dos efeitos.
5. Requisição AoR somente com fonte mecânica autorizada suficiente.
