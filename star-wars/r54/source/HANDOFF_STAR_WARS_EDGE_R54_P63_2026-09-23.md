# HANDOFF STAR WARS EDGE R54 P63 — 2026-09-23

## Baseline
P63 foi consolidado sobre o **checkpoint fonte P62 autêntico** do Google Drive (`BRASEIRO_STAR_WARS_EDGE_R54_P62_SOURCE_CHECKPOINT.zip`). Não houve recriação do projeto e não foi usado GitHub para corrigir.

A descoberta do ZIP P62 ocorreu no mount completo do Drive; uma busca anterior do conector não o havia indexado. Antes de fechar P63 foi feito diff e o trabalho foi rebaseado sobre esse ZIP real, preservando o teste oficial P62.

## Trabalho real deste bloco
### Sob um Sol Negro — persistência e integridade
- preservado o gate P62 que impede ativação/resultado de encontro fora da cena;
- endurecida a gravação para rejeitar resolução duplicada de encontro já encerrado;
- conflito entre resultado recebido e outro encontro publicado ativo é rejeitado.

### Save/reopen durante combate publicado
- `advancePublishedAdventure` não permite trocar de cena enquanto o combate da aventura publicada está ativo;
- `finishCombat` agora valida aventura, cena e encontro **antes** de encerrar/persistir o combate;
- se o save reaberto estiver divergente, o encerramento falha sem deixar combate parcialmente encerrado no armazenamento;
- reabrir no confronto com Kaa’to conserva `publishedAdventureContext` e o encontro ativo.

### Conclusão -> Mundo Livre
- `continuePublishedAdventureToSandbox` ficou retry-safe/idempotente;
- exige aventura concluída e combate encerrado;
- inicializa/valida o sandbox e o local de continuidade antes de arquivar a aventura;
- arquiva somente após o sandbox iniciar com sucesso;
- chamada repetida após arquivamento não duplica eventos nem recompensas;
- persistência das consequências da aventura é mantida no mundo livre.

### Preservação de sistemas
Validado explicitamente: EotE, AoR, MIXED, Duty e Obligation. O teste P63 cobre campanha MIXED com Duty + Obligation customizados atravessando save/reopen, fim de Sob um Sol Negro, recompensa e continuidade ao sandbox.

## Evidência
- integridade P62 + reopen P63: **7/7 PASS**;
- focal Sob um Sol Negro: **98/98 PASS**;
- gate de preservação EotE/AoR/MIXED/Duty/Obligation: **106/106 PASS**;
- regressão canônica (`tests/*.test.mjs`): **922/922 PASS**, 0 FAIL;
- bundle offline reconstruído: **61 módulos**;
- `node --check` do bundle e runtime: PASS.

### Gate extra não canônico
Executar também os dois testes legados na raiz dá **922 PASS / 13 FAIL em 935**. As 13 falhas são de resolução de caminho nesses arquivos antigos: eles procuram `../app` e `../build.gradle.kts`, ou seja, fora da raiz real `sw_p52_work`. O log foi preservado como FAIL e **não** foi usado para declarar PASS.

## Android
Tentativa de marco: `./gradlew assembleDebug --offline`.
Resultado: **FAIL antes da compilação**, `java.net.UnknownHostException: github.com` no Gradle Wrapper ao tentar obter a distribuição.
Portanto **Android/APK NÃO está em PASS** neste checkpoint.

## Estado funcional entregue
Sob um Sol Negro permanece em PT-BR com cenas, NPCs, locais, testes, consequências, ramificações, encontro final, estado persistente, journal/timeline, tracker mobile e anti-spoiler; P63 fecha o ciclo de retomada de combate e transição persistente ao mundo livre sem duplicar recompensa/consequência.

## Próximo bloco recomendado
1. smoke Android real quando houver Gradle/cache disponível, começando por JOGAR SOB UM SOL NEGRO -> save/reopen em combate -> conclusão -> mundo livre;
2. corrigir ou aposentar formalmente os dois testes legados de raiz para eliminar o ruído do gate extra;
3. avançar a próxima aventura publicada sobre o mesmo registry/contrato persistente, sem alterar EotE/AoR/MIXED nem Duty/Obligation.
