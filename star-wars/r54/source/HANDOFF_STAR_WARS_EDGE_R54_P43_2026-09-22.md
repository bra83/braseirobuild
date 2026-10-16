# HANDOFF — BRASEIRO STAR WARS EDGE OF THE GALAXY — R54 P43
## Sob um Sol Negro — feedback player-safe + cartões NPC resilientes

Baseline: P42 do Google Drive. Alteração incremental, sem GitHub.

## Concluído
- Resultados de testes da aventura agora geram `checkFeedback` persistente e player-safe, limitado a cenas já visitadas.
- Feedback não expõe flags internas como `kaatoAlerted`, nós futuros ou planos ocultos.
- UI mostra até 3 efeitos conhecidos recentes em PT-BR.
- Cartões de NPC só aceitam NPC `knownToPlayer` e presente na cena.
- `portraitAsset` ganhou allowlist de caminho local `assets/...` e extensão de imagem; valor inválido é descartado.
- Quando não há retrato local válido, UI usa fallback CSS com iniciais; nenhum asset inexistente foi inventado.
- Bundle canônico reconstruído: 59 módulos.
- EotE/AoR/MIXED e Duty/Obligation preservados; Sob um Sol Negro continua EOTE/MIXED.

## Evidência
- `P43_ADVENTURE_REGRESSION.txt`: 12/12 PASS (P41/P42/P43).
- `P43_MODE_REGRESSION.txt`: 4/4 PASS Duty/AoR.
- sintaxe `underBlackSun.mjs` e bundle: PASS; builder reportou 59 módulos.
- `P43_LEGACY_CONTRACT_CLASSIFICATION.txt`: 15/21 PASS, 6 FAIL. NÃO é regressão global limpa.

## FAILs amplos classificados, ainda não quitados
- file picker: teste antigo espera contagem literal 3, runtime atual retorna 2 ocorrências; comportamento Android JSON segue passando nos subtestes funcionais.
- R38 mobile UX: asserts procuram marcadores/strings de uma shell anterior; precisam ser confrontados com a shell final atual antes de qualquer alteração de produto.
- bridge R46: teste fixa 53 métodos, implementação atual possui 60; contrato precisa ser atualizado pela baseline real, não removendo APIs às cegas.
- R54 device P6: assert de texto fixo antigo conflita com copy atual de ações do Mestre.

## Não declarar
- NÃO declarar regressão global PASS: suíte de contratos legados ainda tem 6 FAILs no lote classificado.
- NÃO declarar Android/APK PASS: não houve compilação Android neste marco.

## Próximo bloco
1. Atualizar/normalizar os contratos legados contra a baseline P43 quando forem comprovadamente stale, sem mascarar regressão real.
2. Procurar/cadastrar retratos locais válidos para NPCs de Sob um Sol Negro; manter fallback quando ausentes.
3. Ampliar consequências específicas dos testes/ramificações para feedback contextual, sem spoilers.
4. Compilar Android somente após contratos estabilizados ou mudança Android relevante.
