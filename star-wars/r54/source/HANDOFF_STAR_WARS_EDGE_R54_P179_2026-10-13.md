# Braseiro Star Wars VTT — checkpoint canônico R54 P179

Data: 2026-10-13. **Base: P178 íntegro** (`ee882b506431a468a0a7753e684933d389eaf7d62715a415beb5476a7489885f`). Nunca retornar a P114 ou versões anteriores. **P179 não é versão final homologada**.

## Implementação real P179 — quatro posições AoR restantes

1. `SUPERVIVIENTE` (Commander/Tactician r3c1): corrigida correspondência com **Natural Outdoorsman**, usando consumidor `NATOUT` existente. Antes havia risco de confundir com `Survivor` da habilidade assinatura `Unmatched Courage` de outra árvore. Agora rerrola o teste real de Resistência/Sobrevivência, uma vez por sessão.
2. `CUERPO A TIERRA` (Engineer/Saboteur r3c1): **Time to Go**; 1 Ponto de Destino luminoso permite manobra incidental reativa contra explosão de ataque NPC registrado, desde que Mestre confirme deslocamento para fora da zona. O dano normal da explosão continua quando não há fuga.
3. `CUERPO A TIERRA MEJORADO` (Engineer/Saboteur r4c1): **Improved Time to Go**; pode permitir a um aliado Engajado, identificado e confirmado, a mesma saída da zona, sem inventar distância ou alvo.
4. `NO ES PARA TANTO` (Soldier/Medic r5c3): **It's Not That Bad**; uma vez por sessão, Medicina Difícil (3 dados), quando aliado real está prestes a sofrer ferimento crítico. Sucesso impede o crítico, mas não o dano; falha gasta o uso e mantém o crítico. Preparação antes do ataque NPC ou confirmação direta na cena. Reação preservada em três reaberturas.

**Integração transversal:** novos recibos de Explosão NPC com arma e Vantagens reais, validação da origem/identidade e dano derivado da arma (não fornecido pelo chamador), consumo único, confirmação espacial do Mestre, efeitos de fuga persistidos, rejeição de replay/adulteração, rollback na falha de armazenamento, limpeza no início/fim do combate. Nova ponte e painel Android PT-BR. `versionCode 10179`, `versionName 1.0.179`, shell e diagnóstico atualizados; bundle offline **85 módulos**. As **8 fontes Java de Kokoro/TTS permaneceram idênticas** a P178.

## Cobertura e limites

- **AoR: 360/360 posições com consumidor mecânico declarado.** Isso NÃO comprova equivalência editorial integral aos PDFs nem cobertura integral de expansões. Não afirmar 100% de certificação do jogo.
- Cinco aventuras (`under-black-sun`, `beyond-the-rim`, `long-arm-of-the-hutt`, `jewel-of-yavin`, `mask-of-the-pirate-queen`): testes de estado preservado e três reaberturas no bloco P179. **Cotejo integral das aventuras com os PDFs ainda pendente**.
- Universal Adventure Engine: fluxo de persistência e rollback testado nos novos gatilhos; certificação global de todos os triggers/consequences/objectives permanece pendente.
- EotE/AoR e expansões: certificação editorial integral pendente. Fonte auxiliar e limites em `evidence/P179/RULE_SOURCE_REFERENCES.md`.
- **APK não compilado**: Gradle 8.13/SDK/adb indisponíveis neste ambiente. BRELGA não executado com P179. Teste físico Android/TTS pendente.

## Evidências de teste P179

- **26/26 testes específicos** de reações, medicina, explosões, idempotência, rollback, cinco aventuras e três reaberturas: `evidence/P179/FOCAL_26_PASS.log`.
- **68/68 testes direcionados de integração** (P179 + P178 + compatibilidade P130 + versão Android): `evidence/P179/FOCAL_INTEGRATION_FINAL.log`.
- Suíte global em 298 arquivos regulares: **2.625 testes aprovados**, 1 arquivo `bundle-builder-p20` expirou por concorrência; esse arquivo foi reexecutado isoladamente com **1/1 aprovado** (`BUNDLE_BUILDER_ISOLATED.log`). Não declarar que uma execução monolítica única foi aprovada. Uma tentativa posterior com 4 trabalhadores atingiu limite externo após 150 arquivos sem falhas (`REGRESSION_2628.log`).
- Stress: **6/6 aprovados** em execução isolada/sequencial (`STRESS_AFTERMATH_ISOLATED.log` + `STRESS_FINAL.log`). Primeira tentativa concorrente expirou por disputa de CPU e não é usada como evidência de aprovação.
- Sintaxe: **135 JS, 145 JSON, 2 XML**, zero erros (`SYNTAX_GATE.json`). Kokoro: **8/8 fontes Java com hashes iguais a P178** (`KOKORO_SOURCE_HASH_PARITY.json`).

## Próxima execução obrigatória

1. Verificar SHA/CRC do ZIP P179 e preservar baseline. Não alterar fontes Kokoro/TTS sem motivo e regressão.
2. Priorizar **auditoria semântica real** dos 360 talentos AoR, EotE e expansões contra livros completos, pois a contagem de consumidores não basta. Corrigir divergências com testes por regra.
3. Comparar as cinco aventuras com os PDFs e validar caminhos, objetivos, consequências, personagens e encontros de ponta a ponta.
4. Completar a suíte global numa execução integral sem timeout, com relatório por arquivo, e stress.
5. Enviar fonte canônica P179 ou superior para CI/BRELGA quando houver meio de transferência verificável; compilar APK e homologar em Android real (incluindo Kokoro).
6. Só criar P180+ com código e testes substanciais; gerar ZIP completo Android Studio, delta, SHA256 e handoff e salvar na Biblioteca com readback.
