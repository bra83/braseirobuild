# AoR 360 Initial Audit (P244 tree, WIP5 working tree)

Read-only source/runtime inventory prepared from the current local working tree. This is a structural audit, not editorial certification.

## Findings

- Runtime catalog read: 6 careers, 18 specializations, 360 source positions, 360 purchase positions, and 360 positions flagged mechanically active. The read returned 360 rows and 165 distinct source talent names.
- Authoritative tree data source in this checkout: `app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs` (`AOR_TALENT_TREES`). The aggregate/classification source is `app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs`: every source node is made purchase-active, while `mechanicallyActive` is computed by membership of `originalName` in `AOR_RUNTIME_MECHANICAL_TALENTS`.
- Runtime alias/name registry: `app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs`. It maps source names to canonical runtime keys; immediate talent effects are separately listed there. Ownership-backed lookup is `sourceBackedTalentRank` in `app/src/main/assets/vtt/js/rules/talents.mjs`, which matches owned purchases to nodes from the AoR source tree. Actual checks/actions are distributed across `rules/talents.mjs`, `core/controller.mjs`, `rules/critical.mjs`, `rules/recovery.mjs`, `rules/vehicles.mjs`, and UI bridge code.
- No `AOR_360` individual proof ledger or per-position source→page→semantics→consumer→UI→persistence/reopen/test ledger was located in the current project tree. This audit supplies a current inventory only; the `Callsite` column deliberately says `not position-linked` where the catalog does not itself provide a consumer callsite.
- The P179 test at `tests/uae-p179-reactive-medicine-blast-five-adventures.test.mjs:35-37` asserts 360 positions and 360 mechanically active, and explicitly labels that assertion “not editorial certification.” This proves a structural/runtime flag count only. P244 validation still names “EotE/AoR/expansion semantic certification and 360 AoR consumers” as remaining (`validation/P244_SWE02_SOURCE_PROOFS.json`).
- Older pending/coverage artifacts are historical snapshots, not current certification: `evidence/P177/AOR_COVERAGE_AND_PENDING.json` records 350 mechanically active and 10 pending; P178 summary records 356 and 4 pending. P179 test source is later than those snapshots, but no matching P179 per-position editorial ledger was found.

## Consumer-source callsite anchors

- `app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs:12-18`: catalog construction and aggregate count.
- `app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs:10-220`: source-name to canonical-key aliases, immediate effects, and runtime-active name list.
- `app/src/main/assets/vtt/js/rules/talents.mjs:232-239`: source-node lookup by ID and ownership-backed canonical rank resolution.
- Example concrete runtime checks include calls in `app/src/main/assets/vtt/js/core/controller.mjs` (e.g. lines 1049, 1127, 1436, 1655, 1755-1760, 5028-5029, 6649-6650), `app/src/main/assets/vtt/js/rules/critical.mjs:59,92-96`, `app/src/main/assets/vtt/js/rules/recovery.mjs:80,99,109-116`, and `app/src/main/assets/vtt/js/rules/vehicles.mjs:738,750`. These are examples, not an exhaustive 360-position callsite map.

## Position inventory

Each row is one source-tree slot. The catalog currently marks all 360 slots active based on the shared `originalName` registry. Repeated names therefore inherit one name-level mapping across multiple positions; this is not evidence that each position received individual source review or that each mapped effect matches its printed rules text. `Runtime key` is the canonical name lookup result. `Callsite` is left unlinked because no source-position-to-consumer ledger is present.

| ID | Career / specialization | Printed page | Source label (`originalName`) | UI label | Runtime key | Flag | Callsite |
|---|---|---:|---|---|---|---|---|
| ace:gunner:r1c1 | ace / gunner | 67 | ROBUSTO | Robusto | DURA | active | not position-linked |
| ace:gunner:r1c2 | ace / gunner | 67 | APLOMO | Compostura | APLOMO | active | not position-linked |
| ace:gunner:r1c3 | ace / gunner | 67 | ABRUMAR DEFENSAS | Sobrecarregar defesas | OVERWHELMDEF | active | not position-linked |
| ace:gunner:r1c4 | ace / gunner | 67 | DISPARO DEBILITANTE | Disparo debilitante | DEBILSHOT | active | not position-linked |
| ace:gunner:r2c1 | ace / gunner | 67 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| ace:gunner:r2c2 | ace / gunner | 67 | FIRMEZA | Firmeza | GRIT | active | not position-linked |
| ace:gunner:r2c3 | ace / gunner | 67 | MUNICIÓN DE RESERVA | Munição reserva | SPARECL | active | not position-linked |
| ace:gunner:r2c4 | ace / gunner | 67 | DISPARO CERTERO | Disparo certeiro | TRUEAIM | active | not position-linked |
| ace:gunner:r3c1 | ace / gunner | 67 | ROBUSTO | Robusto | DURA | active | not position-linked |
| ace:gunner:r3c2 | ace / gunner | 67 | RESISTENTE | Resistente | RESISTENTE | active | not position-linked |
| ace:gunner:r3c3 | ace / gunner | 67 | MEJORAS CASERAS | Melhorias caseiras | JURY | active | not position-linked |
| ace:gunner:r3c4 | ace / gunner | 67 | ABRUMAR DEFENSAS | Sobrecarregar defesas | OVERWHELMDEF | active | not position-linked |
| ace:gunner:r4c1 | ace / gunner | 67 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| ace:gunner:r4c2 | ace / gunner | 67 | RESISTENTE | Resistente | RESISTENTE | active | not position-linked |
| ace:gunner:r4c3 | ace / gunner | 67 | FIRMEZA | Firmeza | GRIT | active | not position-linked |
| ace:gunner:r4c4 | ace / gunner | 67 | SALIDA TÉRMICA | Saída térmica | EXHAUST_PORT | active | not position-linked |
| ace:gunner:r5c1 | ace / gunner | 67 | MADERA DE HÉROE | Fibra de herói | HERFORT | active | not position-linked |
| ace:gunner:r5c2 | ace / gunner | 67 | MEJORAS CASERAS | Melhorias caseiras | JURY | active | not position-linked |
| ace:gunner:r5c3 | ace / gunner | 67 | DEDICACIÓN | Dedicação | DEDICACIÓN | active | not position-linked |
| ace:gunner:r5c4 | ace / gunner | 67 | DISPARO CERTERO | Disparo certeiro | TRUEAIM | active | not position-linked |
| ace:driver:r1c1 | ace / driver | 68 | ACELERACIÓN MÁXIMA | Aceleração máxima | FULLTH | active | not position-linked |
| ace:driver:r1c2 | ace / driver | 68 | CONDUCTOR TODOTERRENO | Condutor todo-terreno | ALLTERR | active | not position-linked |
| ace:driver:r1c3 | ace / driver | 68 | PUESTA A PUNTO | Ajuste fino | FINETUN | active | not position-linked |
| ace:driver:r1c4 | ace / driver | 68 | MAÑOSO | Habilidoso | TINK | active | not position-linked |
| ace:driver:r2c1 | ace / driver | 68 | APLOMO | Compostura | APLOMO | active | not position-linked |
| ace:driver:r2c2 | ace / driver | 68 | PILOTO CONSUMADO | Piloto consumado | SKILLJOCK | active | not position-linked |
| ace:driver:r2c3 | ace / driver | 68 | REACCIÓN RÁPIDA | Reação rápida | RAPREA | active | not position-linked |
| ace:driver:r2c4 | ace / driver | 68 | APLOMO | Compostura | APLOMO | active | not position-linked |
| ace:driver:r3c1 | ace / driver | 68 | ACELERACIÓN MÁXIMA MEJORADA | Aceleração máxima aprimorada | FULLTHIMP | active | not position-linked |
| ace:driver:r3c2 | ace / driver | 68 | ESCURRIDIZO | Evasivo | TRICK | active | not position-linked |
| ace:driver:r3c3 | ace / driver | 68 | PUESTA A PUNTO | Ajuste fino | FINETUN | active | not position-linked |
| ace:driver:r3c4 | ace / driver | 68 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| ace:driver:r4c1 | ace / driver | 68 | CONDUCCIÓN DEFENSIVA | Condução defensiva | DEFDRI | active | not position-linked |
| ace:driver:r4c2 | ace / driver | 68 | PILOTO CONSUMADO | Piloto consumado | SKILLJOCK | active | not position-linked |
| ace:driver:r4c3 | ace / driver | 68 | CONDUCTOR NATO | Condutor nato | NATDRI | active | not position-linked |
| ace:driver:r4c4 | ace / driver | 68 | MAÑOSO | Habilidoso | TINK | active | not position-linked |
| ace:driver:r5c1 | ace / driver | 68 | ACELERACIÓN MÁXIMA SUPREMA | Aceleração máxima suprema | FULLTHSUP | active | not position-linked |
| ace:driver:r5c2 | ace / driver | 68 | PARADA EN SECO | Parada brusca | FULLSTOP | active | not position-linked |
| ace:driver:r5c3 | ace / driver | 68 | PILOTO EXPERTO | Piloto especialista | SKILLJOCK | active | not position-linked |
| ace:driver:r5c4 | ace / driver | 68 | DEDICACIÓN | Dedicação | DEDICACIÓN | active | not position-linked |
| ace:pilot:r1c1 | ace / pilot | 69 | ACELERACIÓN MÁXIMA | Aceleração máxima | FULLTH | active | not position-linked |
| ace:pilot:r1c2 | ace / pilot | 69 | PILOTO CONSUMADO | Piloto consumado | SKILLJOCK | active | not position-linked |
| ace:pilot:r1c3 | ace / pilot | 69 | CARTÓGRAFO GALÁCTICO | Cartógrafo galáctico | GALMAP | active | not position-linked |
| ace:pilot:r1c4 | ace / pilot | 69 | SUBIR DE UN SALTO | Embarque rápido | LETSRIDE | active | not position-linked |
| ace:pilot:r2c1 | ace / pilot | 69 | PILOTO CONSUMADO | Piloto consumado | SKILLJOCK | active | not position-linked |
| ace:pilot:r2c2 | ace / pilot | 69 | IMPACTO DIRECTO | Impacto direto | DEAD_TO_RIGHTS | active | not position-linked |
| ace:pilot:r2c3 | ace / pilot | 69 | CARTÓGRAFO GALÁCTICO | Cartógrafo galáctico | GALMAP | active | not position-linked |
| ace:pilot:r2c4 | ace / pilot | 69 | RECUPERACIÓN RÁPIDA | Recuperação rápida | RAPREC | active | not position-linked |
| ace:pilot:r3c1 | ace / pilot | 69 | ACELERACIÓN MÁXIMA MEJORADA | Aceleração máxima aprimorada | FULLTHIMP | active | not position-linked |
| ace:pilot:r3c2 | ace / pilot | 69 | IMPACTO DIRECTO MEJORADO | Impacto direto aprimorado | DEAD_TO_RIGHTS_IMPROVED | active | not position-linked |
| ace:pilot:r3c3 | ace / pilot | 69 | APLOMO | Compostura | APLOMO | active | not position-linked |
| ace:pilot:r3c4 | ace / pilot | 69 | PILOTO NATO | Piloto nato | NATPIL | active | not position-linked |
| ace:pilot:r4c1 | ace / pilot | 69 | APLOMO | Compostura | APLOMO | active | not position-linked |
| ace:pilot:r4c2 | ace / pilot | 69 | ACELERACIÓN MÁXIMA SUPREMA | Aceleração máxima suprema | FULLTHSUP | active | not position-linked |
| ace:pilot:r4c3 | ace / pilot | 69 | ESCURRIDIZO | Evasivo | TRICK | active | not position-linked |
| ace:pilot:r4c4 | ace / pilot | 69 | CONDUCCIÓN DEFENSIVA | Condução defensiva | DEFDRI | active | not position-linked |
| ace:pilot:r5c1 | ace / pilot | 69 | PILOTO EXPERTO | Piloto especialista | SKILLJOCK | active | not position-linked |
| ace:pilot:r5c2 | ace / pilot | 69 | DEDICACIÓN | Dedicação | DEDICACIÓN | active | not position-linked |
| ace:pilot:r5c3 | ace / pilot | 69 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| ace:pilot:r5c4 | ace / pilot | 69 | EVASIÓN IMPECABLE | Evasão impecável | BRILLIANT_EVASION | active | not position-linked |
| commander:commodore:r1c1 | commander / commodore | 73 | REPARACIÓN EFICAZ | Reparo eficaz | SOLREP | active | not position-linked |
| commander:commodore:r1c2 | commander / commodore | 73 | MANDO | Comando | COM | active | not position-linked |
| commander:commodore:r1c3 | commander / commodore | 73 | REACCIÓN RÁPIDA | Reação rápida | RAPREA | active | not position-linked |
| commander:commodore:r1c4 | commander / commodore | 73 | CARTÓGRAFO GALÁCTICO | Cartógrafo galáctico | GALMAP | active | not position-linked |
| commander:commodore:r2c1 | commander / commodore | 73 | DISEÑO FAMILIAR | Projeto familiar | KNOWN_SCHEMATIC | active | not position-linked |
| commander:commodore:r2c2 | commander / commodore | 73 | DOTES DE MANDO | Dotes de comando | COMMANDPRES | active | not position-linked |
| commander:commodore:r2c3 | commander / commodore | 73 | APLOMO | Compostura | APLOMO | active | not position-linked |
| commander:commodore:r2c4 | commander / commodore | 73 | SOLES FAMILIARES | Sóis familiares | FAMILIARSUNS | active | not position-linked |
| commander:commodore:r3c1 | commander / commodore | 73 | REPARACIÓN EFICAZ | Reparo eficaz | SOLREP | active | not position-linked |
| commander:commodore:r3c2 | commander / commodore | 73 | MANDO | Comando | COM | active | not position-linked |
| commander:commodore:r3c3 | commander / commodore | 73 | REACCIÓN RÁPIDA | Reação rápida | RAPREA | active | not position-linked |
| commander:commodore:r3c4 | commander / commodore | 73 | CARTÓGRAFO GALÁCTICO | Cartógrafo galáctico | GALMAP | active | not position-linked |
| commander:commodore:r4c1 | commander / commodore | 73 | DE UNA PIEZA | De uma peça só | SOLREP | active | not position-linked |
| commander:commodore:r4c2 | commander / commodore | 73 | DOTES DE MANDO | Dotes de comando | COMMANDPRES | active | not position-linked |
| commander:commodore:r4c3 | commander / commodore | 73 | APLOMO | Compostura | APLOMO | active | not position-linked |
| commander:commodore:r4c4 | commander / commodore | 73 | ASTRONAVEGANTE EXPERTO | Astronavegador especialista | MASSTAR | active | not position-linked |
| commander:commodore:r5c1 | commander / commodore | 73 | REPARACIÓN EFICAZ | Reparo eficaz | SOLREP | active | not position-linked |
| commander:commodore:r5c2 | commander / commodore | 73 | FUEGO CONTROLADO | Fogo controlado | FIRE_CONTROL | active | not position-linked |
| commander:commodore:r5c3 | commander / commodore | 73 | DEDICACIÓN | Dedicação | DEDICACIÓN | active | not position-linked |
| commander:commodore:r5c4 | commander / commodore | 73 | CARTÓGRAFO GALÁCTICO | Cartógrafo galáctico | GALMAP | active | not position-linked |
| commander:squadron_leader:r1c1 | commander / squadron_leader | 74 | APLOMO | Compostura | APLOMO | active | not position-linked |
| commander:squadron_leader:r1c2 | commander / squadron_leader | 74 | ATAQUE RÁPIDO | Ataque rápido | QUICKST | active | not position-linked |
| commander:squadron_leader:r1c3 | commander / squadron_leader | 74 | SUBIR DE UN SALTO | Embarque rápido | LETSRIDE | active | not position-linked |
| commander:squadron_leader:r1c4 | commander / squadron_leader | 74 | CONDUCCIÓN DEFENSIVA | Condução defensiva | DEFDRI | active | not position-linked |
| commander:squadron_leader:r2c1 | commander / squadron_leader | 74 | COORDINACIÓN TÁCTICA | Coordenação tática | FIELD_COMMANDER | active | not position-linked |
| commander:squadron_leader:r2c2 | commander / squadron_leader | 74 | CONFIANZA | Confiança | CONF | active | not position-linked |
| commander:squadron_leader:r2c3 | commander / squadron_leader | 74 | ATAQUE RÁPIDO | Ataque rápido | QUICKST | active | not position-linked |
| commander:squadron_leader:r2c4 | commander / squadron_leader | 74 | CONOCIMIENTO DEL ENTORNO | Conhecimento do ambiente | SITUATIONAL_AWARENESS | active | not position-linked |
| commander:squadron_leader:r3c1 | commander / squadron_leader | 74 | MANDO | Comando | COM | active | not position-linked |
| commander:squadron_leader:r3c2 | commander / squadron_leader | 74 | APLOMO | Compostura | APLOMO | active | not position-linked |
| commander:squadron_leader:r3c3 | commander / squadron_leader | 74 | PARADA EN SECO | Parada brusca | FULLSTOP | active | not position-linked |
| commander:squadron_leader:r3c4 | commander / squadron_leader | 74 | CONDUCCIÓN DEFENSIVA | Condução defensiva | DEFDRI | active | not position-linked |
| commander:squadron_leader:r4c1 | commander / squadron_leader | 74 | COORDINACIÓN TÁCTICA MEJORADA | Coordenação tática aprimorada | FIELD_COMMANDER_IMPROVED | active | not position-linked |
| commander:squadron_leader:r4c2 | commander / squadron_leader | 74 | MANDO | Comando | COM | active | not position-linked |
| commander:squadron_leader:r4c3 | commander / squadron_leader | 74 | FORMACIÓN DE COMBATE | Formação de combate | FORM_ON_ME | active | not position-linked |
| commander:squadron_leader:r4c4 | commander / squadron_leader | 74 | ESCURRIDIZO | Evasivo | TRICK | active | not position-linked |
| commander:squadron_leader:r5c1 | commander / squadron_leader | 74 | LÍDER EXPERTO | Líder especialista | MASTER_LEADER | active | not position-linked |
| commander:squadron_leader:r5c2 | commander / squadron_leader | 74 | CONFIANZA | Confiança | CONF | active | not position-linked |
| commander:squadron_leader:r5c3 | commander / squadron_leader | 74 | DEDICACIÓN | Dedicação | DEDICACIÓN | active | not position-linked |
| commander:squadron_leader:r5c4 | commander / squadron_leader | 74 | EVASIÓN IMPECABLE | Evasão impecável | BRILLIANT_EVASION | active | not position-linked |
| commander:tactician:r1c1 | commander / tactician | 75 | MONTARAZ | Mateiro | OUTDOORSMAN | active | not position-linked |
| commander:tactician:r1c2 | commander / tactician | 75 | DOTES DE MANDO | Dotes de comando | COMMANDPRES | active | not position-linked |
| commander:tactician:r1c3 | commander / tactician | 75 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| commander:tactician:r1c4 | commander / tactician | 75 | FINTA | Finta | FEINT | active | not position-linked |
| commander:tactician:r2c1 | commander / tactician | 75 | MONTARAZ | Mateiro | OUTDOORSMAN | active | not position-linked |
| commander:tactician:r2c2 | commander / tactician | 75 | CONFIANZA | Confiança | CONF | active | not position-linked |
| commander:tactician:r2c3 | commander / tactician | 75 | DESENFUNDADO RÁPIDO | Saque rápido | QUICKDR | active | not position-linked |
| commander:tactician:r2c4 | commander / tactician | 75 | VELOZ | Veloz | SWIFT | active | not position-linked |
| commander:tactician:r3c1 | commander / tactician | 75 | SUPERVIVIENTE | Sobrevivente | NATOUT | active | not position-linked |
| commander:tactician:r3c2 | commander / tactician | 75 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| commander:tactician:r3c3 | commander / tactician | 75 | GUARDAESPALDAS | Guarda-costas | BOD | active | not position-linked |
| commander:tactician:r3c4 | commander / tactician | 75 | GUARDAESPALDAS | Guarda-costas | BOD | active | not position-linked |
| commander:tactician:r4c1 | commander / tactician | 75 | CONFIANZA | Confiança | CONF | active | not position-linked |
| commander:tactician:r4c2 | commander / tactician | 75 | DOTES DE MANDO | Dotes de comando | COMMANDPRES | active | not position-linked |
| commander:tactician:r4c3 | commander / tactician | 75 | COORDINACIÓN TÁCTICA | Coordenação tática | FIELD_COMMANDER | active | not position-linked |
| commander:tactician:r4c4 | commander / tactician | 75 | FINTA | Finta | FEINT | active | not position-linked |
| commander:tactician:r5c1 | commander / tactician | 75 | ATAQUE COORDINADO | Ataque coordenado | COORDINATED_ASSAULT | active | not position-linked |
| commander:tactician:r5c2 | commander / tactician | 75 | LÍDER NATO | Líder nato | NATLEAD | active | not position-linked |
| commander:tactician:r5c3 | commander / tactician | 75 | COORDINACIÓN TÁCTICA MEJORADA | Coordenação tática aprimorada | FIELD_COMMANDER_IMPROVED | active | not position-linked |
| commander:tactician:r5c4 | commander / tactician | 75 | DEDICACIÓN | Dedicação | DEDICACIÓN | active | not position-linked |
| diplomat:ambassador:r1c1 | diplomat / ambassador | 79 | INDISTINGUIBLE | Indistinguível | INDISTINGUISHABLE | active | not position-linked |
| diplomat:ambassador:r1c2 | diplomat / ambassador | 79 | DON DE GENTES | Dom de gente | KILL | active | not position-linked |
| diplomat:ambassador:r1c3 | diplomat / ambassador | 79 | NI UN PELO DE TONTO | Não é bobo | NOBODYSFOOL | active | not position-linked |
| diplomat:ambassador:r1c4 | diplomat / ambassador | 79 | CONFIANZA | Confiança | CONF | active | not position-linked |
| diplomat:ambassador:r2c1 | diplomat / ambassador | 79 | INDISTINGUIBLE | Indistinguível | INDISTINGUISHABLE | active | not position-linked |
| diplomat:ambassador:r2c2 | diplomat / ambassador | 79 | APLOMO | Compostura | APLOMO | active | not position-linked |
| diplomat:ambassador:r2c3 | diplomat / ambassador | 79 | APLOMO | Compostura | APLOMO | active | not position-linked |
| diplomat:ambassador:r2c4 | diplomat / ambassador | 79 | ESQUIVA | Esquiva | DODGE | active | not position-linked |
| diplomat:ambassador:r3c1 | diplomat / ambassador | 79 | DON DE GENTES | Dom de gente | KILL | active | not position-linked |
| diplomat:ambassador:r3c2 | diplomat / ambassador | 79 | RETÓRICA INSPIRADORA | Retórica inspiradora | INSPIRINGRHET | active | not position-linked |
| diplomat:ambassador:r3c3 | diplomat / ambassador | 79 | NERVIOS DE ACERO | Nervos de aço | STEELYNERVES | active | not position-linked |
| diplomat:ambassador:r3c4 | diplomat / ambassador | 79 | CONFIANZA | Confiança | CONF | active | not position-linked |
| diplomat:ambassador:r4c1 | diplomat / ambassador | 79 | RETÓRICA INSPIRADORA MEJORADA | Retórica inspiradora aprimorada | INSPIRINGRHETIMP | active | not position-linked |
| diplomat:ambassador:r4c2 | diplomat / ambassador | 79 | PRESENCIA IMPONENTE | Presença imponente | INTENSEPRES | active | not position-linked |
| diplomat:ambassador:r4c3 | diplomat / ambassador | 79 | MANO DE SANTO | Mão santa | WORKS_LIKE_A_CHARM | active | not position-linked |
| diplomat:ambassador:r4c4 | diplomat / ambassador | 79 | ESQUIVA | Esquiva | DODGE | active | not position-linked |
| diplomat:ambassador:r5c1 | diplomat / ambassador | 79 | RETÓRICA INSPIRADORA SUPREMA | Retórica inspiradora suprema | INSPIRINGRHETSUP | active | not position-linked |
| diplomat:ambassador:r5c2 | diplomat / ambassador | 79 | EMBAUCADOR NATO | Embusteiro nato | NATCHARM | active | not position-linked |
| diplomat:ambassador:r5c3 | diplomat / ambassador | 79 | DEDICACIÓN | Dedicação | DEDICACIÓN | active | not position-linked |
| diplomat:ambassador:r5c4 | diplomat / ambassador | 79 | SEXTO SENTIDO | Sexto sentido | SIXTHSENSE | active | not position-linked |
| diplomat:agitator:r1c1 | diplomat / agitator | 80 | NEGACIÓN PLAUSIBLE | Negação plausível | PLAUSDEN | active | not position-linked |
| diplomat:agitator:r1c2 | diplomat / agitator | 80 | NI UN PELO DE TONTO | Não é bobo | NOBODYSFOOL | active | not position-linked |
| diplomat:agitator:r1c3 | diplomat / agitator | 80 | APLOMO | Compostura | APLOMO | active | not position-linked |
| diplomat:agitator:r1c4 | diplomat / agitator | 80 | INTIMIDANTE | Intimidador | INTIM | active | not position-linked |
| diplomat:agitator:r2c1 | diplomat / agitator | 80 | INSTINTO CALLEJERO | Instinto das ruas | STRSMART | active | not position-linked |
| diplomat:agitator:r2c2 | diplomat / agitator | 80 | INSTINTO CALLEJERO | Instinto das ruas | STRSMART | active | not position-linked |
| diplomat:agitator:r2c3 | diplomat / agitator | 80 | ACTITUD CONVINCENTE | Atitude convincente | CONV | active | not position-linked |
| diplomat:agitator:r2c4 | diplomat / agitator | 80 | INTIMIDANTE | Intimidador | INTIM | active | not position-linked |
| diplomat:agitator:r3c1 | diplomat / agitator | 80 | ACTITUD CONVINCENTE | Atitude convincente | CONV | active | not position-linked |
| diplomat:agitator:r3c2 | diplomat / agitator | 80 | NEGACIÓN PLAUSIBLE | Negação plausível | PLAUSDEN | active | not position-linked |
| diplomat:agitator:r3c3 | diplomat / agitator | 80 | LENGUA AFILADA | Língua afiada | SCATHINGTIRADE | active | not position-linked |
| diplomat:agitator:r3c4 | diplomat / agitator | 80 | APLOMO | Compostura | APLOMO | active | not position-linked |
| diplomat:agitator:r4c1 | diplomat / agitator | 80 | MATÓN NATO | Valentão nato | NATENF | active | not position-linked |
| diplomat:agitator:r4c2 | diplomat / agitator | 80 | NI UN PELO DE TONTO | Não é bobo | NOBODYSFOOL | active | not position-linked |
| diplomat:agitator:r4c3 | diplomat / agitator | 80 | LENGUA AFILADA MEJORADA | Língua afiada aprimorada | SCATHINGTIRADEIMP | active | not position-linked |
| diplomat:agitator:r4c4 | diplomat / agitator | 80 | INTIMIDANTE | Intimidador | INTIM | active | not position-linked |
| diplomat:agitator:r5c1 | diplomat / agitator | 80 | INTIMIDANTE | Intimidador | INTIM | active | not position-linked |
| diplomat:agitator:r5c2 | diplomat / agitator | 80 | DEDICACIÓN | Dedicação | DEDICACIÓN | active | not position-linked |
| diplomat:agitator:r5c3 | diplomat / agitator | 80 | LENGUA AFILADA SUPREMA | Língua afiada suprema | SCATHINGTIRADESUP | active | not position-linked |
| diplomat:agitator:r5c4 | diplomat / agitator | 80 | INCITAR A LA REBELIÓN | Incitar à rebelião | INCITE_REBELLION | active | not position-linked |
| diplomat:quartermaster:r1c1 | diplomat / quartermaster | 81 | PROVEEDOR HABITUAL | Fornecedor habitual | KNOWSOMEBODY | active | not position-linked |
| diplomat:quartermaster:r1c2 | diplomat / quartermaster | 81 | LABIA | Lábia | SMOOTHTALK | active | not position-linked |
| diplomat:quartermaster:r1c3 | diplomat / quartermaster | 81 | CHANCHULLERO | Trambiqueiro | WHEELDEAL | active | not position-linked |
| diplomat:quartermaster:r1c4 | diplomat / quartermaster | 81 | APLOMO | Compostura | APLOMO | active | not position-linked |
| diplomat:quartermaster:r2c1 | diplomat / quartermaster | 81 | LABIA | Lábia | SMOOTHTALK | active | not position-linked |
| diplomat:quartermaster:r2c2 | diplomat / quartermaster | 81 | UNTAR MANOS | Subornar | GREASED_PALMS | active | not position-linked |
| diplomat:quartermaster:r2c3 | diplomat / quartermaster | 81 | MERCADER EXPERTO | Mercador especialista | MASTMERCH | active | not position-linked |
| diplomat:quartermaster:r2c4 | diplomat / quartermaster | 81 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| diplomat:quartermaster:r3c1 | diplomat / quartermaster | 81 | APLOMO | Compostura | APLOMO | active | not position-linked |
| diplomat:quartermaster:r3c2 | diplomat / quartermaster | 81 | CHANCHULLERO | Trambiqueiro | WHEELDEAL | active | not position-linked |
| diplomat:quartermaster:r3c3 | diplomat / quartermaster | 81 | COMPRAR INFORMACIÓN | Comprar informação | BOUGHT_INFO | active | not position-linked |
| diplomat:quartermaster:r3c4 | diplomat / quartermaster | 81 | APLOMO | Compostura | APLOMO | active | not position-linked |
| diplomat:quartermaster:r4c1 | diplomat / quartermaster | 81 | PROVEEDOR HABITUAL | Fornecedor habitual | KNOWSOMEBODY | active | not position-linked |
| diplomat:quartermaster:r4c2 | diplomat / quartermaster | 81 | INVERSIONES RENTABLES | Investimentos rentáveis | SOUNDINVEST | active | not position-linked |
| diplomat:quartermaster:r4c3 | diplomat / quartermaster | 81 | INVERSIONES RENTABLES | Investimentos rentáveis | SOUNDINVEST | active | not position-linked |
| diplomat:quartermaster:r4c4 | diplomat / quartermaster | 81 | CONCENTRACIÓN INTENSA | Concentração intensa | INTFOC | active | not position-linked |
| diplomat:quartermaster:r5c1 | diplomat / quartermaster | 81 | DEDICACIÓN | Dedicação | DEDICACIÓN | active | not position-linked |
| diplomat:quartermaster:r5c2 | diplomat / quartermaster | 81 | NEGOCIADOR NATO | Negociador nato | NATNEG | active | not position-linked |
| diplomat:quartermaster:r5c3 | diplomat / quartermaster | 81 | REFLEJOS SUPERIORES | Reflexos superiores | SUPREF | active | not position-linked |
| diplomat:quartermaster:r5c4 | diplomat / quartermaster | 81 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| engineer:mechanic:r1c1 | engineer / mechanic | 85 | MAÑOSO | Habilidoso | TINK | active | not position-linked |
| engineer:mechanic:r1c2 | engineer / mechanic | 85 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| engineer:mechanic:r1c3 | engineer / mechanic | 85 | PUESTA A PUNTO | Ajuste fino | FINETUN | active | not position-linked |
| engineer:mechanic:r1c4 | engineer / mechanic | 85 | REPARACIÓN EFICAZ | Reparo eficaz | SOLREP | active | not position-linked |
| engineer:mechanic:r2c1 | engineer / mechanic | 85 | SISTEMAS REDUNDANTES | Sistemas redundantes | REDUNDANT_SYS | active | not position-linked |
| engineer:mechanic:r2c2 | engineer / mechanic | 85 | REPARACIÓN EFICAZ | Reparo eficaz | SOLREP | active | not position-linked |
| engineer:mechanic:r2c3 | engineer / mechanic | 85 | MAÑOSO | Habilidoso | TINK | active | not position-linked |
| engineer:mechanic:r2c4 | engineer / mechanic | 85 | APLOMO | Compostura | APLOMO | active | not position-linked |
| engineer:mechanic:r3c1 | engineer / mechanic | 85 | REPARACIÓN EFICAZ | Reparo eficaz | SOLREP | active | not position-linked |
| engineer:mechanic:r3c2 | engineer / mechanic | 85 | RESISTENTE | Resistente | RESISTENTE | active | not position-linked |
| engineer:mechanic:r3c3 | engineer / mechanic | 85 | PIEZA DEFECTUOSA | Peça defeituosa | BAD_MOTIVATOR | active | not position-linked |
| engineer:mechanic:r3c4 | engineer / mechanic | 85 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| engineer:mechanic:r4c1 | engineer / mechanic | 85 | ARTILUGIO | Engenhoca | CONTRAPTION | active | not position-linked |
| engineer:mechanic:r4c2 | engineer / mechanic | 85 | REPARACIÓN EFICAZ | Reparo eficaz | SOLREP | active | not position-linked |
| engineer:mechanic:r4c3 | engineer / mechanic | 85 | PUESTA A PUNTO | Ajuste fino | FINETUN | active | not position-linked |
| engineer:mechanic:r4c4 | engineer / mechanic | 85 | TENACIDAD | Tenacidade | HARDHD | active | not position-linked |
| engineer:mechanic:r5c1 | engineer / mechanic | 85 | CHAPUCERO NATO | Improvisador nato | NATTIN | active | not position-linked |
| engineer:mechanic:r5c2 | engineer / mechanic | 85 | DE UNA PIEZA | De uma peça só | SOLREP | active | not position-linked |
| engineer:mechanic:r5c3 | engineer / mechanic | 85 | DEDICACIÓN | Dedicação | DEDICACIÓN | active | not position-linked |
| engineer:mechanic:r5c4 | engineer / mechanic | 85 | TENACIDAD MEJORADA | Tenacidade aprimorada | HARDHDIMP | active | not position-linked |
| engineer:saboteur:r1c1 | engineer / saboteur | 86 | ENTEREZA | Determinação | RESOLVE | active | not position-linked |
| engineer:saboteur:r1c2 | engineer / saboteur | 86 | FUERZAS DE FLAQUEZA | Segundo fôlego | SECWIND | active | not position-linked |
| engineer:saboteur:r1c3 | engineer / saboteur | 86 | APLOMO | Compostura | APLOMO | active | not position-linked |
| engineer:saboteur:r1c4 | engineer / saboteur | 86 | RECUPERACIÓN RÁPIDA | Recuperação rápida | RAPREC | active | not position-linked |
| engineer:saboteur:r2c1 | engineer / saboteur | 86 | APLOMO | Compostura | APLOMO | active | not position-linked |
| engineer:saboteur:r2c2 | engineer / saboteur | 86 | EXPLOSIÓN POTENTE | Explosão potente | POWERBLAST | active | not position-linked |
| engineer:saboteur:r2c3 | engineer / saboteur | 86 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| engineer:saboteur:r2c4 | engineer / saboteur | 86 | FUERZAS DE FLAQUEZA | Segundo fôlego | SECWIND | active | not position-linked |
| engineer:saboteur:r3c1 | engineer / saboteur | 86 | CUERPO A TIERRA | Jogar-se ao chão | TIME_TO_GO | active | not position-linked |
| engineer:saboteur:r3c2 | engineer / saboteur | 86 | RECUPERACIÓN RÁPIDA | Recuperação rápida | RAPREC | active | not position-linked |
| engineer:saboteur:r3c3 | engineer / saboteur | 86 | ENTEREZA | Determinação | RESOLVE | active | not position-linked |
| engineer:saboteur:r3c4 | engineer / saboteur | 86 | TENACIDAD | Tenacidade | HARDHD | active | not position-linked |
| engineer:saboteur:r4c1 | engineer / saboteur | 86 | CUERPO A TIERRA MEJORADO | Jogar-se ao chão aprimorado | TIME_TO_GO_IMPROVED | active | not position-linked |
| engineer:saboteur:r4c2 | engineer / saboteur | 86 | EXPLOSIÓN POTENTE | Explosão potente | POWERBLAST | active | not position-linked |
| engineer:saboteur:r4c3 | engineer / saboteur | 86 | DETONACIÓN SELECTIVA | Detonação seletiva | SELECTDET | active | not position-linked |
| engineer:saboteur:r4c4 | engineer / saboteur | 86 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| engineer:saboteur:r5c1 | engineer / saboteur | 86 | DEDICACIÓN | Dedicação | DEDICACIÓN | active | not position-linked |
| engineer:saboteur:r5c2 | engineer / saboteur | 86 | GRANADERO EXPERTO | Granadeiro especialista | MASTERGREN | active | not position-linked |
| engineer:saboteur:r5c3 | engineer / saboteur | 86 | DETONACIÓN SELECTIVA | Detonação seletiva | SELECTDET | active | not position-linked |
| engineer:saboteur:r5c4 | engineer / saboteur | 86 | TENACIDAD MEJORADA | Tenacidade aprimorada | HARDHDIMP | active | not position-linked |
| engineer:scientist:r1c1 | engineer / scientist | 87 | CONOCIMIENTO ESPECIALIZADO | Conhecimento especializado | KNOWSPEC | active | not position-linked |
| engineer:scientist:r1c2 | engineer / scientist | 87 | ERUDITO RESPETADO | Erudito respeitado | RESSCH | active | not position-linked |
| engineer:scientist:r1c3 | engineer / scientist | 87 | INVESTIGADOR | Investigador | RESEARCHER | active | not position-linked |
| engineer:scientist:r1c4 | engineer / scientist | 87 | HABLAR BINARIO | Falar binário | SPEAKBIN | active | not position-linked |
| engineer:scientist:r2c1 | engineer / scientist | 87 | INVESTIGADOR | Investigador | RESEARCHER | active | not position-linked |
| engineer:scientist:r2c2 | engineer / scientist | 87 | CONOCIMIENTO ESPECIALIZADO | Conhecimento especializado | KNOWSPEC | active | not position-linked |
| engineer:scientist:r2c3 | engineer / scientist | 87 | COMPARTIMENTO OCULTO | Compartimento oculto | HIDDEN_STORAGE | active | not position-linked |
| engineer:scientist:r2c4 | engineer / scientist | 87 | MANITAS | Faz-tudo | GEARHD | active | not position-linked |
| engineer:scientist:r3c1 | engineer / scientist | 87 | ERUDITO RESPETADO | Erudito respeitado | RESSCH | active | not position-linked |
| engineer:scientist:r3c2 | engineer / scientist | 87 | FORTALEZA MENTAL | Fortaleza mental | MENTALFORT | active | not position-linked |
| engineer:scientist:r3c3 | engineer / scientist | 87 | HABLAR BINARIO | Falar binário | SPEAKBIN | active | not position-linked |
| engineer:scientist:r3c4 | engineer / scientist | 87 | INVENTOR | Inventor | INVENTOR | active | not position-linked |
| engineer:scientist:r4c1 | engineer / scientist | 87 | ERUDITO NATO | Erudito nato | NATSCH | active | not position-linked |
| engineer:scientist:r4c2 | engineer / scientist | 87 | INGENIOSO | Engenhoso | STROKE_OF_GENIUS | active | not position-linked |
| engineer:scientist:r4c3 | engineer / scientist | 87 | INVENTOR | Inventor | INVENTOR | active | not position-linked |
| engineer:scientist:r4c4 | engineer / scientist | 87 | MANITAS | Faz-tudo | GEARHD | active | not position-linked |
| engineer:scientist:r5c1 | engineer / scientist | 87 | CONCENTRACIÓN INTENSA | Concentração intensa | INTFOC | active | not position-linked |
| engineer:scientist:r5c2 | engineer / scientist | 87 | METICULOSO | Meticuloso | CAREFUL_PLANNING | active | not position-linked |
| engineer:scientist:r5c3 | engineer / scientist | 87 | DEDICACIÓN | Dedicação | DEDICACIÓN | active | not position-linked |
| engineer:scientist:r5c4 | engineer / scientist | 87 | CINTURÓN DE ACCESORIOS | Cinto de acessórios | UTILITYBELT | active | not position-linked |
| soldier:commando:r1c1 | soldier / commando | 91 | ENTRENAMIENTO FÍSICO | Treinamento físico | PHYTRAIN | active | not position-linked |
| soldier:commando:r1c2 | soldier / commando | 91 | APLOMO | Compostura | APLOMO | active | not position-linked |
| soldier:commando:r1c3 | soldier / commando | 91 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| soldier:commando:r1c4 | soldier / commando | 91 | A QUEMARROPA | À queima-roupa | POINTBL | active | not position-linked |
| soldier:commando:r2c1 | soldier / commando | 91 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| soldier:commando:r2c2 | soldier / commando | 91 | ROBUSTO | Robusto | DURA | active | not position-linked |
| soldier:commando:r2c3 | soldier / commando | 91 | ENTRENAMIENTO FÍSICO | Treinamento físico | PHYTRAIN | active | not position-linked |
| soldier:commando:r2c4 | soldier / commando | 91 | BRAZO FUERTE | Braço forte | STRONGARM | active | not position-linked |
| soldier:commando:r3c1 | soldier / commando | 91 | RESISTENCIA A VENENOS | Resistência a venenos | POISONRES | active | not position-linked |
| soldier:commando:r3c2 | soldier / commando | 91 | DOMINIO DE BLINDAJES | Domínio de blindagens | ARM | active | not position-linked |
| soldier:commando:r3c3 | soldier / commando | 91 | SUPERVIVIENTE NATO | Sobrevivente nato | NATOUT | active | not position-linked |
| soldier:commando:r3c4 | soldier / commando | 91 | FUERZA SALVAJE | Força selvagem | FERSTR | active | not position-linked |
| soldier:commando:r4c1 | soldier / commando | 91 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| soldier:commando:r4c2 | soldier / commando | 91 | MADERA DE HÉROE | Fibra de herói | HERFORT | active | not position-linked |
| soldier:commando:r4c3 | soldier / commando | 91 | ROBUSTO | Robusto | DURA | active | not position-linked |
| soldier:commando:r4c4 | soldier / commando | 91 | DERRIBO | Derrubada | KNOCKDOWN_TALENT | active | not position-linked |
| soldier:commando:r5c1 | soldier / commando | 91 | DOMINIO DE BLINDAJES MEJORADO | Domínio de blindagens aprimorado | ARMIMP | active | not position-linked |
| soldier:commando:r5c2 | soldier / commando | 91 | DEDICACIÓN | Dedicação | DEDICACIÓN | active | not position-linked |
| soldier:commando:r5c3 | soldier / commando | 91 | IMPARABLE | Imparável | UNSTOPPABLE | active | not position-linked |
| soldier:commando:r5c4 | soldier / commando | 91 | FUERZA SALVAJE | Força selvagem | FERSTR | active | not position-linked |
| soldier:medic:r1c1 | soldier / medic | 92 | ACLIMATACIÓN | Aclimatação | ACCLIM | active | not position-linked |
| soldier:medic:r1c2 | soldier / medic | 92 | ESPECIALISTA EN ESTIMULANTES | Especialista em estimulantes | STIMSPEC | active | not position-linked |
| soldier:medic:r1c3 | soldier / medic | 92 | APLOMO | Compostura | APLOMO | active | not position-linked |
| soldier:medic:r1c4 | soldier / medic | 92 | CIRUJANO | Cirurgião | SURG | active | not position-linked |
| soldier:medic:r2c1 | soldier / medic | 92 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| soldier:medic:r2c2 | soldier / medic | 92 | CIRUJANO | Cirurgião | SURG | active | not position-linked |
| soldier:medic:r2c3 | soldier / medic | 92 | ESPECIALISTA EN ESTIMULANTES | Especialista em estimulantes | STIMSPEC | active | not position-linked |
| soldier:medic:r2c4 | soldier / medic | 92 | ESPECIALISTA EN BACTA | Especialista em bacta | BACTASPEC | active | not position-linked |
| soldier:medic:r3c1 | soldier / medic | 92 | POLIFACÉTICO | Versátil | WELLROUND | active | not position-linked |
| soldier:medic:r3c2 | soldier / medic | 92 | APLOMO | Compostura | APLOMO | active | not position-linked |
| soldier:medic:r3c3 | soldier / medic | 92 | APLICACIÓN DE ESTIMULANTES | Aplicação de estimulantes | STIMAPP | active | not position-linked |
| soldier:medic:r3c4 | soldier / medic | 92 | MÉDICO EXPERTO | Médico especialista | MASDOC | active | not position-linked |
| soldier:medic:r4c1 | soldier / medic | 92 | ESQUIVA | Esquiva | DODGE | active | not position-linked |
| soldier:medic:r4c2 | soldier / medic | 92 | MÉDICO NATO | Médico nato | NATDOC | active | not position-linked |
| soldier:medic:r4c3 | soldier / medic | 92 | APLICACIÓN DE ESTIMULANTES MEJORADA | Aplicação de estimulantes aprimorada | STIMAPPIMP | active | not position-linked |
| soldier:medic:r4c4 | soldier / medic | 92 | ESPECIALISTA EN ESTIMULANTES | Especialista em estimulantes | STIMSPEC | active | not position-linked |
| soldier:medic:r5c1 | soldier / medic | 92 | LECCIONES DE ANATOMÍA | Lições de anatomia | ANAT | active | not position-linked |
| soldier:medic:r5c2 | soldier / medic | 92 | DEDICACIÓN | Dedicação | DEDICACIÓN | active | not position-linked |
| soldier:medic:r5c3 | soldier / medic | 92 | NO ES PARA TANTO | Não é tão grave | ITS_NOT_THAT_BAD | active | not position-linked |
| soldier:medic:r5c4 | soldier / medic | 92 | APLICACIÓN DE ESTIMULANTES SUPREMA | Aplicação de estimulantes suprema | STIMAPPSUP | active | not position-linked |
| soldier:sharpshooter:r1c1 | soldier / sharpshooter | 93 | RASTREADOR EXPERTO | Rastreador especialista | EXPTRACK | active | not position-linked |
| soldier:sharpshooter:r1c2 | soldier / sharpshooter | 93 | DISPARO DE PRECISIÓN | Disparo de precisão | SNIPSHOT | active | not position-linked |
| soldier:sharpshooter:r1c3 | soldier / sharpshooter | 93 | FIRMEZA | Firmeza | GRIT | active | not position-linked |
| soldier:sharpshooter:r1c4 | soldier / sharpshooter | 93 | APLOMO | Compostura | APLOMO | active | not position-linked |
| soldier:sharpshooter:r2c1 | soldier / sharpshooter | 93 | APLOMO | Compostura | APLOMO | active | not position-linked |
| soldier:sharpshooter:r2c2 | soldier / sharpshooter | 93 | DISPARO CERTERO | Disparo certeiro | TRUEAIM | active | not position-linked |
| soldier:sharpshooter:r2c3 | soldier / sharpshooter | 93 | PRECISIÓN LETAL | Precisão letal | DEADACC | active | not position-linked |
| soldier:sharpshooter:r2c4 | soldier / sharpshooter | 93 | GOLPES LETALES | Golpes letais | LETHALBL | active | not position-linked |
| soldier:sharpshooter:r3c1 | soldier / sharpshooter | 93 | FIRMEZA | Firmeza | GRIT | active | not position-linked |
| soldier:sharpshooter:r3c2 | soldier / sharpshooter | 93 | GOLPES LETALES | Golpes letais | LETHALBL | active | not position-linked |
| soldier:sharpshooter:r3c3 | soldier / sharpshooter | 93 | DISPARO DE PRECISIÓN | Disparo de precisão | SNIPSHOT | active | not position-linked |
| soldier:sharpshooter:r3c4 | soldier / sharpshooter | 93 | DISPARO CERTERO | Disparo certeiro | TRUEAIM | active | not position-linked |
| soldier:sharpshooter:r4c1 | soldier / sharpshooter | 93 | RASTREADOR EXPERTO | Rastreador especialista | EXPTRACK | active | not position-linked |
| soldier:sharpshooter:r4c2 | soldier / sharpshooter | 93 | PRECISIÓN LETAL | Precisão letal | DEADACC | active | not position-linked |
| soldier:sharpshooter:r4c3 | soldier / sharpshooter | 93 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| soldier:sharpshooter:r4c4 | soldier / sharpshooter | 93 | GOLPE INCAPACITANTE | Golpe incapacitante | CRIPPLING_BLOW | active | not position-linked |
| soldier:sharpshooter:r5c1 | soldier / sharpshooter | 93 | SOLUCIÓN IMPROVISADA | Solução improvisada | QUICK_FIX | active | not position-linked |
| soldier:sharpshooter:r5c2 | soldier / sharpshooter | 93 | TIRADOR NATO | Atirador nato | NATMAR | active | not position-linked |
| soldier:sharpshooter:r5c3 | soldier / sharpshooter | 93 | DEDICACIÓN | Dedicação | DEDICACIÓN | active | not position-linked |
| soldier:sharpshooter:r5c4 | soldier / sharpshooter | 93 | GOLPE DIRIGIDO | Golpe dirigido | TARGBL | active | not position-linked |
| spy:infiltrator:r1c1 | spy / infiltrator | 97 | APLOMO | Compostura | APLOMO | active | not position-linked |
| spy:infiltrator:r1c2 | spy / infiltrator | 97 | ESQUIVA | Esquiva | DODGE | active | not position-linked |
| spy:infiltrator:r1c3 | spy / infiltrator | 97 | ATAQUE FURIOSO | Ataque furioso | FRENZ | active | not position-linked |
| spy:infiltrator:r1c4 | spy / infiltrator | 97 | POSTURA DEFENSIVA | Postura defensiva | DEFSTA | active | not position-linked |
| spy:infiltrator:r2c1 | spy / infiltrator | 97 | GOLPE CONMOCIONADOR | Golpe atordoante | STUNBL | active | not position-linked |
| spy:infiltrator:r2c2 | spy / infiltrator | 97 | APLOMO | Compostura | APLOMO | active | not position-linked |
| spy:infiltrator:r2c3 | spy / infiltrator | 97 | PUNTO DÉBIL | Ponto fraco | SOFT_SPOT | active | not position-linked |
| spy:infiltrator:r2c4 | spy / infiltrator | 97 | LEVANTARSE DE UN SALTO | Levantar-se num salto | JUMP | active | not position-linked |
| spy:infiltrator:r3c1 | spy / infiltrator | 97 | DERRIBO | Derrubada | KNOCKDOWN_TALENT | active | not position-linked |
| spy:infiltrator:r3c2 | spy / infiltrator | 97 | ATAQUE FURIOSO | Ataque furioso | FRENZ | active | not position-linked |
| spy:infiltrator:r3c3 | spy / infiltrator | 97 | APLOMO | Compostura | APLOMO | active | not position-linked |
| spy:infiltrator:r3c4 | spy / infiltrator | 97 | ESQUIVA | Esquiva | DODGE | active | not position-linked |
| spy:infiltrator:r4c1 | spy / infiltrator | 97 | CAMORRISTA NATO | Brigão nato | NATBRAW | active | not position-linked |
| spy:infiltrator:r4c2 | spy / infiltrator | 97 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| spy:infiltrator:r4c3 | spy / infiltrator | 97 | GOLPE CONMOCIONADOR MEJORADO | Golpe atordoante aprimorado | STUNBLIMP | active | not position-linked |
| spy:infiltrator:r4c4 | spy / infiltrator | 97 | POSTURA DEFENSIVA | Postura defensiva | DEFSTA | active | not position-linked |
| spy:infiltrator:r5c1 | spy / infiltrator | 97 | DEDICACIÓN | Dedicação | DEDICACIÓN | active | not position-linked |
| spy:infiltrator:r5c2 | spy / infiltrator | 97 | SOLUCIÓN INGENIOSA | Solução engenhosa | CLEVER_SOLUTION | active | not position-linked |
| spy:infiltrator:r5c3 | spy / infiltrator | 97 | SOMBRA FURTIVA | Sombra furtiva | MASSHAD | active | not position-linked |
| spy:infiltrator:r5c4 | spy / infiltrator | 97 | DELINCUENTE NATO | Delinquente nato | NATROG | active | not position-linked |
| spy:scout:r1c1 | spy / scout | 98 | RECUPERACIÓN RÁPIDA | Recuperação rápida | RAPREC | active | not position-linked |
| spy:scout:r1c2 | spy / scout | 98 | ACECHADOR | Espreitador | STALK | active | not position-linked |
| spy:scout:r1c3 | spy / scout | 98 | APLOMO | Compostura | APLOMO | active | not position-linked |
| spy:scout:r1c4 | spy / scout | 98 | ATAJO | Atalho | SHORTCUT | active | not position-linked |
| spy:scout:r2c1 | spy / scout | 98 | ACLIMATACIÓN | Aclimatação | ACCLIM | active | not position-linked |
| spy:scout:r2c2 | spy / scout | 98 | ATAQUE RÁPIDO | Ataque rápido | QUICKST | active | not position-linked |
| spy:scout:r2c3 | spy / scout | 98 | SUBIR DE UN SALTO | Embarque rápido | LETSRIDE | active | not position-linked |
| spy:scout:r2c4 | spy / scout | 98 | DESORIENTAR | Desorientar | DISOR | active | not position-linked |
| spy:scout:r3c1 | spy / scout | 98 | RECUPERACIÓN RÁPIDA | Recuperação rápida | RAPREC | active | not position-linked |
| spy:scout:r3c2 | spy / scout | 98 | CAZADOR NATO | Caçador nato | NATHUN | active | not position-linked |
| spy:scout:r3c3 | spy / scout | 98 | SOLES FAMILIARES | Sóis familiares | FAMILIARSUNS | active | not position-linked |
| spy:scout:r3c4 | spy / scout | 98 | ATAJO | Atalho | SHORTCUT | active | not position-linked |
| spy:scout:r4c1 | spy / scout | 98 | APLOMO | Compostura | APLOMO | active | not position-linked |
| spy:scout:r4c2 | spy / scout | 98 | OBSERVADOR | Observador | KEENEYED | active | not position-linked |
| spy:scout:r4c3 | spy / scout | 98 | ENDURECIDO | Endurecido | ENDURECIDO | active | not position-linked |
| spy:scout:r4c4 | spy / scout | 98 | ATAQUE RÁPIDO | Ataque rápido | QUICKST | active | not position-linked |
| spy:scout:r5c1 | spy / scout | 98 | CINTURÓN DE ACCESORIOS | Cinto de acessórios | UTILITYBELT | active | not position-linked |
| spy:scout:r5c2 | spy / scout | 98 | DEDICACIÓN | Dedicação | DEDICACIÓN | active | not position-linked |
| spy:scout:r5c3 | spy / scout | 98 | ACECHADOR | Espreitador | STALK | active | not position-linked |
| spy:scout:r5c4 | spy / scout | 98 | DESORIENTAR | Desorientar | DISOR | active | not position-linked |
| spy:slicer:r1c1 | spy / slicer | 99 | DESCIFRADOR | Decifrador | CODEBREAKER | active | not position-linked |
| spy:slicer:r1c2 | spy / slicer | 99 | APLOMO | Compostura | APLOMO | active | not position-linked |
| spy:slicer:r1c3 | spy / slicer | 99 | APTITUD TECNOLÓGICA | Aptidão tecnológica | TECHAPT | active | not position-linked |
| spy:slicer:r1c4 | spy / slicer | 99 | ELUDIR SEGURIDAD | Burlar segurança | BYP | active | not position-linked |
| spy:slicer:r2c1 | spy / slicer | 99 | PROGRAMACIÓN DEFENSIVA | Programação defensiva | DEFSLI | active | not position-linked |
| spy:slicer:r2c2 | spy / slicer | 99 | APTITUD TECNOLÓGICA | Aptidão tecnológica | TECHAPT | active | not position-linked |
| spy:slicer:r2c3 | spy / slicer | 99 | APLOMO | Compostura | APLOMO | active | not position-linked |
| spy:slicer:r2c4 | spy / slicer | 99 | ELUDIR SEGURIDAD | Burlar segurança | BYP | active | not position-linked |
| spy:slicer:r3c1 | spy / slicer | 99 | PROGRAMADOR NATO | Programador nato | NATPRO | active | not position-linked |
| spy:slicer:r3c2 | spy / slicer | 99 | ELUDIR SEGURIDAD | Burlar segurança | BYP | active | not position-linked |
| spy:slicer:r3c3 | spy / slicer | 99 | PROGRAMACIÓN DEFENSIVA | Programação defensiva | DEFSLI | active | not position-linked |
| spy:slicer:r3c4 | spy / slicer | 99 | APLOMO | Compostura | APLOMO | active | not position-linked |
| spy:slicer:r4c1 | spy / slicer | 99 | PROGRAMACIÓN DEFENSIVA | Programação defensiva | DEFSLI | active | not position-linked |
| spy:slicer:r4c2 | spy / slicer | 99 | PROGRAMACIÓN DEFENSIVA MEJORADA | Programação defensiva aprimorada | DEFSLIIMP | active | not position-linked |
| spy:slicer:r4c3 | spy / slicer | 99 | DESCIFRADOR | Decifrador | CODEBREAKER | active | not position-linked |
| spy:slicer:r4c4 | spy / slicer | 99 | ENTEREZA | Determinação | RESOLVE | active | not position-linked |
| spy:slicer:r5c1 | spy / slicer | 99 | PIRATA INFORMÁTICO CONSUMADO | Hacker consumado | SKILSLI | active | not position-linked |
| spy:slicer:r5c2 | spy / slicer | 99 | PIRATA INFORMÁTICO EXPERTO | Hacker especialista | MASSLIC | active | not position-linked |
| spy:slicer:r5c3 | spy / slicer | 99 | FORTALEZA MENTAL | Fortaleza mental | MENTALFORT | active | not position-linked |
| spy:slicer:r5c4 | spy / slicer | 99 | DEDICACIÓN | Dedicação | DEDICACIÓN | active | not position-linked |

## Pending for an individual proof ledger

For each of the 360 IDs, establish and record the primary source and exact printed page, independently reviewed semantics and rank behavior, the concrete runtime consumer and UI path, ownership/activation gates, persistence and three reopen checks where applicable, and a focused test/evidence reference. Then reconcile every row against the current source and runtime; a count assertion or `mechanicallyActive` flag must not be promoted to editorial certification.
