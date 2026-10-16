# HANDOFF STAR WARS EDGE R54 P53 — 2026-09-23

Baseline: P52. Não recriar.

## Concluído
- Beyond the Rim Episódio II implementado em PT-BR no adapter genérico.
- Fluxo: viagem além da Orla -> chegada/busca em Cholganna -> rastro da queda -> Sa Nalaor -> convergência Yiyar -> contato Harsol -> Retiro -> contingência imperial -> boundary Episódio III.
- Novos locais persistentes: órbita de Cholganna, selva/rastro, Sa Nalaor, Retiro.
- Novos NPCs: Rel Harsol e Cratala; anti-spoiler via knownToPlayer apenas quando entram em cena.
- Consequências P1 preservadas: partyAhead, yiyarHasDroid, departureTracked; P2 grava survivorTrust, harsolTrust, imperialExposure.
- Gates de cena exigem testes reais para revelar rastro, avançar aos destroços e resolver crise imperial.
- Registry continua EotE/MIXED; Sob um Sol Negro preservado.
- uiBridge.bundle reconstruído.

## Evidência
- focal P52+P53: 13/13 PASS.
- regressão ampla: 890/890 PASS, 0 FAIL.
- Android assembleDebug: BLOQUEADO por java.net.UnknownHostException: services.gradle.org no Gradle Wrapper. Não declarar Android PASS/APK.

## Próximo bloco
Implementar Beyond the Rim Episódio III (Raxus Prime / Scrapheap Point), consequências finais e fechamento da aventura. Depois ampliar catálogo com a próxima aventura do acervo sem duplicar controller.
