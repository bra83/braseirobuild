# HANDOFF CANÔNICO — BRASEIRO STAR WARS VTT R54 P184

**Base imutável:** P183 SHA256 `cbd90414d8b117150bb8737c0d53e4f52870e51f0e3df9558fdce0835aa41e52` (97.228.432 bytes). **P184** é uma evolução incremental real, sem reinicialização de campanha.

## Código implementado

1. **SWE09 — Grande Prêmio:** `jewelOfYavinOperations.mjs` implementa sete trechos adjudicados na ordem, Place Value acumulado por velocidade e ajustes, tensão do veículo, boias perdidas, colisões e classificação dependente dos Place Values rivais. Não atribui vitória ou prêmios automaticamente. O resultado do Mestre e os recibos sobrevivem às reaberturas.
2. **SWE09 — Leilão:** sete licitantes oficiais, valores de lance inicial e progressão por rodada, Kaltho vencedor padrão por 200.000 créditos em dez rodadas sem intervenção, manipulação adjudicada dos licitantes (+1 por sucesso, +3 por Triunfo e +10.000 créditos na rodada influenciada), eliminação por diferença de Bid Value >10, encerramento e recibos idempotentes. Não transfere dinheiro sem adjudicação bancária nem inventa prêmios.
3. **Universal Adventure Engine:** operações registradas pelo adaptador publicado e reconciliadas no journal UAE, persistidas no progresso da aventura. Auditoria de consistência de recibos, ordem dos trechos, Place Value, tensão e lances; rejeição de valores forjados, IDs reservados, repetição e ações fora da cena. Preserva rotas e saves P183.
4. **Android:** `GameController.adjudicateJewelOperation` e `BraseiroUiBridge.adjudicateJewelOperation` expõem quatro ações (race_leg, race_placement, bidder_influence, auction_round). Bundle offline recompilado com 86 módulos. **Não** afirmar controles visuais específicos homologados.
5. **SWE02 — Lylek Den:** dois perfis de caçadores conforme SWE02 p.16, um Gand com fuzil e os demais humanos com pistolas; quantidade por número real de personagens da sessão. Encontro tem início/resolução/recibo, não permite derrotar Teemo por engano. Grupo sem tamanho confirmado é bloqueado em vez de assumir quatro PCs.
6. **Android:** versão de fonte `1.0.184` (`versionCode=10184`). Oito fontes Java, incluindo Kokoro/TTS, preservadas byte a byte em relação ao P183.

## Evidência e limites

- `evidence/P184/PDF_TARGETED_SOURCE_CROSSCHECK.json`: comparação DIRECIONADA ao Drive oficial The Jewel of Yavin SWE09 (pp.48-55, 58-68) e Long Arm of the Hutt SWE02 (pp.15-17). **Não certificação editorial integral.**
- `tests/uae-p184-jewel-race-auction-pdf.test.mjs`: 16 novos testes (ordem, integridade, leilão, fonte, save 3 reaberturas, encontro, UI).
- `evidence/P184/TEST_SUMMARY.json`: 303 arquivos regulares, 2708 testes aprovados, mais 6/6 stress = **2714/2714** na execução por arquivo; não suíte monolítica.
- Sintaxe de 120 MJS, 22 JSON e 2 XML verificada; bundle offline 86 módulos; Kokoro/TTS 8/8 hashes iguais ao P183.
- **Sem APK, SDK/Gradle/adb funcional, CI BRELGA ou homologação física comprovada.**

## Próxima execução

Continuar sobre P184. Certificar e integrar o restante do Grand Prix (riscos de terreno, competição, NPCs, itens e consequências), as particularidades de cada licitante, banco, traição e fuga; concluir New Meen/Geonosis/Teemo com perfis e escolhas do PDF. Auditar cena por cena cinco aventuras e 360 talentos AoR e expansões EotE/AoR contra PDFs, sem confundir consumidores declarados com equivalência. Testar toda regressão, preservar Kokoro/TTS e checkpoints; só declarar APK com CI/Gradle e instalação física comprovados.
