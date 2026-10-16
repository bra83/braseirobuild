# P168 — Age of Rebellion Core: implementação integrada, sem certificação editorial completa

Árvores canônicas no checkpoint: `aorTalentTreesData.mjs` (AoR Core, páginas impressas 73, 75, 76 e 81). Identificadores de compra validados por `sourceBackedTalentRank`; a UI não autoriza efeitos com nomes exibidos falsificados.

| Talent AoR (nome original) | Novas posições | Efeito executável | Consumidores |
|---|---:|---|---|
| RETÓRICA INSPIRADORA | 1 | Ação: teste Médio (2) Liderança; cada sucesso recupera 1 Tensão de um companheiro selecionado no alcance Curto; Vantagens adicionais recuperam Tensão de um afetado. | `performAoRSocialTalent` → `resolveSkillCheck` → `applyInspiringRhetoric` |
| RETÓRICA INSPIRADORA MEJORADA | 1 | 1 Impulso nos testes de perícia do companheiro afetado durante rodadas iguais à Liderança, sem acumular; marcado com serial do encontro para não ressurgir em outro combate. | `resolveCompanionSkillCheck` → `activeInspiringBoost` |
| RETÓRICA INSPIRADORA SUPREMA | 1 | Manobra em vez de ação, pagando 1 Tensão voluntária além do custo normal da manobra. | `resolveSkillCheck` → `spendPlayerManeuver` |
| LENGUA AFILADA | 1 | Ação: teste Médio (2) Coerção; cada sucesso causa 1 Tensão a um adversário distinto em alcance Curto; Vantagens extras atingem um afetado. Rival/lacaio converte Tensão em Ferimentos, sem Soak. | `performAoRSocialTalent` → `sufferAdversaryStrain` |
| LENGUA AFILADA MEJORADA | 1 | +1 Contratempo, sem acumular, aos testes de perícia de NPC afetado durante rodadas iguais à Coerção. | `_resolveNpcTurn`, `resolveIncomingSocialInfluenceCheck`, `resolveIncomingComputerIntrusionCheck` |
| LENGUA AFILADA SUPREMA | 1 | Manobra em vez de ação, pagando 1 Tensão voluntária. | `resolveSkillCheck` → `spendPlayerManeuver` |
| PRESENCIA IMPONENTE | 1 | Gasta 1 Destino luminoso e recupera Tensão igual à Presença, com rollback. | `useIntensePresence` |
| DE UNA PIEZA | 2 | +1 Trauma de Casco reparado por graduação em reparos de sucesso; nunca concede cura com zero êxitos ou compras falsas. | `applyDamageControl`, `applyManualRepairs` |

**Limite explícito:** companheiros são fichas customizadas da party, não NPCs oficiais das aventuras; a proximidade é confirmada pelo jogador e não inferida automaticamente. Efeitos do Impulso são consumidos pelos testes de perícia dos companheiros, sem criar turnos completos de NPC. O motor não reivindica transcrição integral das estatísticas e encontros das cinco aventuras.

Referências mecânicas secundárias usadas para conferir os talentos sociais, **não substituem cotejo dos PDFs do usuário**: Age of Rebellion Core pp. 149 e 155; resumo das árvores FFG `https://mman.kapsi.fi/rpg/starwars/FFG_Star_Wars_Talent_Trees.pdf`; Inspiring Rhetoric e Scathing Tirade na Star Wars RPG FFG Wiki.
