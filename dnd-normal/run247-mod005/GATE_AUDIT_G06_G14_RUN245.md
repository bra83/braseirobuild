# Auditoria detalhada G06–G14 — RUN245

Execução de referência: [BRELGA run 38061684317](https://github.com/bra83/braseirobuild/actions/runs/38061684317), commit `24364e46d74768131ab569989ba64ab402efd6d3`. Suite Android verde: 76 testes, 0 falhas/erros/ignorados. O resultado comprova os casos focados abaixo, não as partes ainda listadas como ausentes. Status completo em `COVERAGE_MATRIX_G01_G16.csv`.

## G06 — Magias: parcial

- 3 testes Bless e 3 testes de seleção/validação de alvo e componente material passaram.
- Famílias/edições completas, concentração, efeitos persistentes, geometrias e rollback abrangentes faltam.

## G07 — Ficha e progressão: parcial

- Transição de nível pura cobre XP, hit die, HP fixo, escolhas pendentes e pré-requisito de multiclasse.
- 4 testes focados passaram no BRELGA.
- Progressão 1–20, subclasses, talentos, antecedentes, equipamentos e fluxos multiclasse completos continuam sem homologação.

## G08 — Tática e geometria: parcial

- Grade/rota, custo de movimento e recibo têm casos dirigidos. Os 3 testes do movimento narrado passaram, incluindo replay.
- Elevação, voo, sentidos, hazards, footprints e cobertura/linha de visão completa continuam abertos.

## G09 — Ações narradas e ponte UI→executor→DB: parcial

- `NarratedTacticalMovementExecutor` valida identidade/cena/destino, deriva rota, exige tratamento de ataque de oportunidade e delega orçamento/posição/recibo ao executor persistente.
- Testes cobriram movimento, replay, destino obstruído, coordenada inválida e OA sem resolução.
- E2E amplo de `tactical_updates` da UI até aprovação, transação, recibo e rollback não foi homologado.

## G10 — Modos de controle e curadoria: parcial

- Política impede que proposta curada carregue mutação de estado ou avanço do tick local. Os 4 testes focados passaram.
- Filas/atrasos e execução aprovada até recibos continuam sem teste integrado suficiente.

## G11 — Mundo vivo: parcial

- Preview determinístico de catch-up não grava estado; 4 testes focados passaram.
- Commit offline transacional, concorrência/reentrada, import/export de relações/clima e E2E permanecem abertos.

## G12 — Gemini, perfil do Mestre e imagem: parcial

- Política de importação exige cena ativa e entrada válida; 3 testes passaram (cena atual, obsoleta e URI/MIME/ação inválidos).
- Gemini/perfil, geração, storage/import real, quota e rede E2E faltam.

## G13 — Música, ambiente e TTS: parcial

- Chunker preserva texto e limites de Charon/Android/Kokoro; 4 testes passaram.
- Playback, engines reais, quota/cache, música e cross-fade continuam sem teste integrado.

## G14 — Backup21: parcial

- 9 testes existentes de `AppDbBackupRobolectricTest` passaram: round-trip, legado, remapeamento e rollback.
- Teste focado de riders temporários incluiu round-trip Backup21, remapeamento, replay e expiração do timer.
- Snapshot consistente de todas as seções temporais e interoperabilidade total seguem sem homologação.

O build/teste está verde, mas esses gates permanecem parciais pelos requisitos enumerados. Nenhuma dessas pendências foi marcada como concluída por inferência do build.
