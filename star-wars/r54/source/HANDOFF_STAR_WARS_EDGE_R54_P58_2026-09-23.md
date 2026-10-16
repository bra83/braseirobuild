# R54 P58 — SOB UM SOL NEGRO PRONTA PARA JOGAR

Baseline: P57. Sem GitHub.

## Fechado neste marco
- Entrada inicial não bloqueia mais criação/carregamento de personagem antes de existir sandbox.
- Após existir personagem, tela inicial oferece `JOGAR SOB UM SOL NEGRO` como entrada direta.
- Quick start fixa EotE para esta aventura, prepara o mundo persistente em `Coruscant — Setor 943` e inicia `under-black-sun` imediatamente.
- Mundo livre permanece disponível separadamente.
- Fluxo real validado até a primeira cena: Perseguição em alta velocidade, testes e escolhas disponíveis.
- Toda a implementação P57 permanece: investigação ramificada, Umbra/Spyder/Zelcomm/NovaCORE, Kaa'to, combate/negociação, consequências, persistência, epílogo e anti-spoiler.
- EotE/AoR/MIXED e Duty/Obligation gerais preservados; a aventura inicia em EotE por ser EotE.

## Evidência
- Focal P58 + compatibilidade P52: PASS.
- Regressão completa: 906/906 PASS, 0 FAIL.
- Android: tentativa de assembleDebug --offline falhou antes da compilação porque o wrapper tentou acessar services.gradle.org e o ambiente retornou UnknownHostException. Não declarar APK PASS.
