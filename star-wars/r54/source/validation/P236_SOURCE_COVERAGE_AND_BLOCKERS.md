# Braseiro Star Wars R54 P236 — evidência e bloqueadores

**Estado:** candidato em validação; não é entrega final, não é APK e não é homologação Android.
**Linhagem:** P234 SHA256 `7ada091ef59a135b04f88add37b1798d3f4f29537ad85528a6d7cc0cd064158d` → P235 interno testado → P236. O ZIP P234 continua sendo a base recuperável; nenhum arquivo prévio deve ser apagado.

## Escopo imutável

Somente Under a Black Sun (SWEP01), Beyond the Rim (SWE05), Long Arm of the Hutt (SWE02), The Jewel of Yavin (SWE09) e Mask of the Pirate Queen (SWE13). Regras EotE/AoR/expansões selecionadas são escopo de motor, não aventuras extras. Dados, combate, fichas, XP, inventário, relógio, companheiros, persistência e rollback somente no executor determinístico; RAG apenas referência; Gemini/Motor Bárbara apenas propostas narrativas validadas. Sem APK antes de fechar as regras e as cinco aventuras.

## Mudanças reais de P235 e P236

**P235, SWE02 New Meen (fonte SWE02 impressa pp.19–22):** combate noturno, Drombb/capangas e grupo por tamanho real da party, derrotas verificadas, ramificação de retirada, combate não duplicável e três reaberturas. Não equiparar esse trecho à aventura inteira.

**P236, SWE02 palácio de Teemo (fonte oficial pública SWE02 impressa pp.41–46):** doze cenas novas conectadas: portões fortificados, disfarce, recepção, oficina, registro B1, cozinha, evidência ryll, comunicações e rotas de retorno. Acesso pelos portões exige Enganação Média; disfarce exige Enganação Difícil e Charme Média, com registro do resultado. Infiltração frontal silenciosa não é opção. Oficina possui registro B1 recuperável, sem conceder créditos/XP. Cozinha exige Percepção Difícil para evidência ryll. Central possui cinco verificações oficiais: Computadores Fácil/Média/Difícil/Difícil/Difícil; +1 dado de revés, +1 quando há alarme, bloqueio de terminal com duas ameaças. Evidências ficam em flags com recibos de perícia; flags adulteradas sem recibo são rejeitadas.

**SWE02 p.41, guarda Gamorreano de elite:** perfil separado do Gamorreano genérico; Brawn 4, Agility 3, Intellect/Cunning/Willpower/Presence 1, soak 4, WT 8, Melee 2, machado dano 7 e Pierce 2. Preservado o perfil legado, sem regressão cruzada.

**SWE02 p.44, sala do trono:** dois guardas de elite, grupo principal de capangas igual ao número de PJs, outro grupo adicional com quantidade **não fixada no livro**; o controlador exige confirmação explícita dessa quantidade, além da conferência da ficha de Hutt Crime Lord do Beginner Game p.46. Não inventa o grupo adicional. Derrota de Teemo não pode ser declarada sem grupo real, IDs únicos, Hutt incapacitado e recibo de combate íntegro; três reaberturas e adulteração testadas. O status de epílogo não concede automaticamente propriedade legal da Krayt Fang ou absolvição de Jabba.

**Fonte:** [PDF oficial SWE02 (Fantasy Flight Games)](https://images-cdn.fantasyflightgames.com/filer_public/80/93/80935c12-24ac-442b-b423-687d4e28ae89/long_arm_of_the_hutt_lr.pdf). Páginas impressas verificadas diretamente: 41, 44, 45 e 46. Isto **não** certifica os demais trechos do livro.

## Bloqueadores para fechar a SWE02

- Ato 1 completo: Nabat, viagem, Thwheek, perseguição e ramificações devem ser comparados com todas as páginas oficiais, inclusive contingências de derrota.
- Ato 2 Geonosis: NPCs/segredos da festa de Piddock, cinco indícios sobre Teemo, negociação com Dimmock, emboscadas, consequências e nave Lucky Guess ainda sem cotejo e teste semântico integral.
- Ato 3: perfil Hutt Crime Lord do Beginner Game p.46 ainda não certificado; grupo adicional do trono requer curadoria; chandelier e ataque à corrente (duas melhorias de dificuldade) ainda não integrados ao executor; fuga de guardas após morte de Teemo, alarmes por rodada, reforços em quatro rodadas, gladiadores, recompensas oficiais, Anatta/Jabba, resolução por chantagem e consequências persistentes não fechados.
- Os checks declarados em nós antigos sem página oficial ficam **manuais/bloqueados**, não recebem um pool inventado. A nova via de checagens oficiais foi exercitada no controlador real.

## Demais aventuras e regras

- Under a Black Sun: inventário e regressão existem, mas cotejo editorial integral e Android real **pendentes**.
- Beyond the Rim: inventário e regressão existem, mas cotejo editorial integral e Android real **pendentes**.
- The Jewel of Yavin: inventário e regressão existem, mas cotejo editorial integral e Android real **pendentes**.
- Mask of the Pirate Queen: inventário e regressão existem, mas cotejo editorial integral e Android real **pendentes**.
- EotE/AoR e expansões selecionadas: auditoria individual fonte→página→semântica→executor→UI→3 reaberturas→teste **pendente** para o universo total, inclusive os 360 consumidores AoR; não confundir testes estruturais com certificação.

## Próximo megalote, sem recomeçar

Completar Geonosis como sistema de evidências/negociação e o restante de Teemo (Anatta, Hutt Crime Lord, chandelier, reforços, desfechos), depois confrontar o grafo SWE02 inteiro com o PDF, incluindo derrota, fuga, recompensas e estado persistido. Somente marcar a SWE02 concluída após o fluxo integral ser demonstrado e todas as páginas verificadas. Em seguida cotejar as outras quatro aventuras e fechar as famílias de regras EotE/AoR; APK e Android real por último.

## Regras de backup

Após gates PASS, ZIP Android Studio integral, SHA sidecar, delta reconstruível, manifesto de partes <=32MiB, handoff, matriz e evidências devem ir para Drive `/BRASEIRO_STAR_WARS_CHECKPOINTS` (`1qTEzIaJi1Ot3jqjVpKFpIvP4oRwJg1an`). O status **BACKUP OK** só pode ser escrito após listagem, readback de **cada** parte, hashes individuais e recomposição do SHA integral. P234/P233/P126/P113 permanecem até a sucessora estar comprovadamente íntegra; retenção cautelosa e nenhuma deleção por idade.
