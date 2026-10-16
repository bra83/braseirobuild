# Braseiro Star Wars VTT — handoff canônico R54 P181

Data: 2026-10-13. **Base P180 verificada:** SHA-256 `ec2bdd10f62991300994b96a7312966d9dbe914e1976c2b114f2f6fac0091918`. **Não regredir a P114/P173.** Este ZIP é o projeto Android Studio; **não é APK**.

## Implementação executável P181

- **SWE13 — Mask of the Pirate Queen:** corrige o início editorial incorreto em Ord Mantell. O primeiro episódio passa a começar com a oferta de Venlana Sipal e a investigação em **Saleucami/Taleucema**, segue pela Paradise, Graf, Krezo Wasanti, Blackwind Crater, Vault e Revah; o segundo episódio ocorre em **Worlport/Ord Mantell**, com Jander, Illo Vandin, os caminhos Fight Fixing/Rusty Reach, consorte, palácio e Ryale Wei; o terceiro segue a **Renegade’s Blood**, culminando em Noira. **23 nós**, antes 5, com rotas alternativas, IDs estáveis, flags e checkpoints.
- **NPCs e mundo:** registra nove NPCs nomeados, doze locais e duas facções em estado persistido; preserva modificações do usuário. NPCs não são certificados como fichas estatísticas oficiais.
- **Migração:** save legado `schemaVersion=1` com início `ord-mantell-arrival` migra para `saleucami-contract-offer` e atualiza também o checkpoint UAE, sem reescrever os eventos históricos. Mantém flags, testes, relógios e IDs dos recibos; migração idempotente. Pontes de status/cena/testes/encontros aplicam normalização.
- **Testes e encontros:** testes de perícia gravam sucesso/falha e vantagem; falhas em zonas de risco elevam o relógio de alerta. Encontros situados no Vault, palácio, corredores e ponte; bloqueio de resultados incompatíveis e de reaplicação; histórico persistente.
- **Economia:** ao aceitar aliança com a Irmandade, o controlador não paga automaticamente a recompensa do Consórcio nem reduz Obligation. A captura segue o valor de política legado, **não certificado como valor do livro**. Corrigido fallback `0 || 4000` que convertia recompensas zero em pagamentos indevidos.
- **Android:** `versionCode=10181`, `versionName=1.0.181`, shell e diagnóstico alinhados; bundle offline recompilado, 85 módulos. **8 arquivos Java/Kokoro idênticos byte a byte aos do P180.**

## Fontes efetivamente inspecionadas

- `Mask of the Pirate Queen.pdf` (98 páginas), Episode I Search on Saleucami, Episode II Going to Ground, Episode III Prepare for Boarders; SHA-256 e correspondências de capítulos em `evidence/P181/PDF_SOURCE_CROSSCHECK.json`.
- `Under a Black Sun.pdf` (40 páginas) e `Long Arm of the Hutt.pdf` (46 páginas) recuperados do Drive nesta execução; cabeçalhos de episódios/atos identificados. **Isso não certifica** seus encontros, estatísticas nem cenas.
- `Beyond the Rim.pdf` e `The Jewel of Yavin.pdf` tiveram somente sumários comparados no P180. Os cinco livros **ainda não** foram cotejados integralmente com as implementações.

## Testes comprovados

- 300 arquivos regulares e seis stress: **306/306 arquivos, 2670/2670 testes aprovados** via execução por arquivo. A primeira rodada final revelou duas falhas reais de sincronização da versão no shell, corrigidas e reexecutadas com sucesso. `evidence/P181/TEST_SUMMARY.json` e `evidence/P181/test-results/*.json` preservam as provas.
- Testes específicos P181: **6/6**, cobrindo grafo, dois caminhos de Illo Vandin, captura/aliança, migração de saves + três reaberturas, encontros, recibos, relógio de alerta e recompensas reais do controlador.
- 119 módulos JS verificados sintaticamente, 22 JSON e 2 XML validados; oito Java preservados.
- **Não declarar execução monolítica verde**: os testes foram executados por arquivo, e duas falhas foram corrigidas com rerun direcionado.

## Pendências reais (não homologadas)

1. Certificação editorial **integral** dos cinco livros de aventura: o P181 corrige a sequência de Mask, mas não reproduz cada encontro, mapa, condição ou ficha do livro. As demais aventuras seguem parciais.
2. Comparação individual de 360 consumidores AoR e expansões EotE/AoR com os PDFs: 360/360 consumidores declarados **não é** equivalência semântica demonstrada.
3. Homologação Android física, instalação, três reaberturas SQLite, Kokoro/TTS real e APK compilado/assinado. Sem SDK/Gradle/adb no ambiente atual; não houve APK.

## Próximo bloco

Priorizar auditoria página a página das aventuras, correção de divergências de encontros e recompensas, certificação das expansões e teste Android real. Preservar P181 antes de qualquer mudança. Só promover P182+ após alteração substancial com regressão, ZIP integral, SHA/delta e upload recuperável.
