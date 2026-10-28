# Braseiro D&D — Kvothe, Mundo Vivo e continuidade entre aventuras
Data: 2026-10-28
Branch isolada: `feature/dnd-kvothe-story-bridge-20261028`
Escopo: D&D Normal 2024, sem alterações em Mystara, APK ou saves do usuário.

## Personagem para gate
Kvothe, 17 anos, Humano Mago nível 1, Andarilho, músico, órfão de uma trupe destruída, sobrevivente de anos nas ruas. Epíteto "O Matador de Rei" é um rumor misterioso **não comprovado**. Perfil de personalidade com 39 traços, agência exclusiva do jogador.

## Leitura da implementação
- `LivingWorldSimulationEngine.preview` é chamado por `WorldStateEngine.preview`; `LivingWorldSimulationEngine.commit` é chamado por `WorldStateEngine.commit` na transação de avanço do mundo. NPCs, facções, atividades, trupes e guildas têm processamento persistente e filtros de agentes inativos.
- `NpcAgencyEngine.applyConfirmed` integra memória/cognição NPC e `WorldAutonomyPlanner` propõe ações, sem o narrador poder confirmar efeitos por conta própria.
- `WorldSixCoordinator.tick` integra `WorldHistoryEngine`, `LivingSettlementEngine`, `RumorNetworkEngine`, `CampaignLegacyEngine`; `MasterGameContext` expõe contexto ao Mestre.
- `WorldDomainEventLedger.recordEnvelopeChecked` encaminha atualizações aceitas ao `AdventureContinuityEngine.recordConfirmed` dentro da confirmação de cena.
- `AdventureContinuityEngine` só grava fatos com `canonical_fact=true`, `actor_character_id` válido e aventura JSON ativa com ID. O bridge fornece conhecimento a NPCs testemunhas apenas por IDs persistidos. Sem aventura JSON importada, essa rota de causalidade não grava feitos; `WorldHistoryEngine` registra fatos globais, porém sem o mesmo vínculo de ator/testemunha para reconhecimento individual.
- `RumorNetworkEngine` espalha rumores de eventos canônicos na escala do mundo, mas não foi comprovado que alimenta um índice `npc x herói x fato x certeza`, tampouco uma reputação regional positiva/negativa longitudinal por herói com controle de testemunhas.

## Alterações isoladas efetuadas
1. `BraseiroPersonalityProfile.forNarrator`: injeta fatos confirmados do `story_profile` e lacunas desconhecidas; preserva fallback do `history` quando a importação antiga descarta campos novos. Nunca obriga ações do jogador nem gera bônus.
2. `AdventureContinuityEngine.recordConfirmed`: fato verificado sem testemunha também persiste, rotulado `private_unwitnessed`; a falta de testemunha **não** passa a significar reconhecimento por NPC.
3. `MasterGameContext.snapshot` e `narratorSnapshot`: acrescentam `present_npc_adventure_memory` baseado exclusivamente em conhecimento reconhecido por testemunha, em vez de tornar cada NPC onisciente.
4. `BraseiroCharacterStoryTest`: quatro testes Robolectric declarados cobrindo contexto de história, personalidade, fallback legado e proteção de fatos desconhecidos.

## Bloqueadores restantes — NÃO homologado
- Persistir feitos ligados à identidade canônica do herói mesmo em aventuras geradas pelo Mestre, sem JSON importado; reutilizar mundo/cronologia e garantir idempotência transacional.
- Criar `npc_knowledge` explícito por fonte, data, certeza, testemunho e propagação, sem transformar rumor em verdade.
- Reputação regional e interpessoal persistente, com admiração/ódio/temor independentes, mudanças por consequências confirmadas e recuperação.
- Testar mundo vivo ao longo de dias/meses/anos: trabalho, prosperidade, relações, mudanças físicas, casamentos, mortes, limitações; não inventar fatos fora das regras/tempo.
- Verificar eventos canonizados e rumor network integrados em todos os tipos de aventura (JSON, gerada, continuada), inclusive personagens importados e múltiplos mundos.
- Executar `./gradlew :app:compileDebugJavaWithJavac :app:testDebugUnitTest :app:assembleDebug` na branch após integração com HEAD atualizado; validar backup/restore e 3 reaberturas SQLite.
- Testar no Galaxy A57; nenhum APK/OTA foi construído ou publicado nesta rodada.

## Cenários obrigatórios
A. Kvothe termina aventura A salvando uma pessoa, testemunha NPC-1 presente -> NPC-1 reconhece; NPC-2 distante e sem fonte não reconhece.
B. Feito confirmado sem testemunha -> permanece como fato particular; não gera reputação pública até ocorrer comunicação verificável.
C. NPC-1 conta um rumor falso ao NPC-3 -> NPC-3 pode acreditar na alegação, mas o fato histórico não muda.
D. Aventura B no mesmo mundo/cronologia -> reputações, relações, dívida, data e testemunhas permanecem.
E. Aventura C em outro mundo/cronologia -> nenhum efeito vaza.
F. NPC comerciante prospera após meses, e a mudança permanece ao carregar; NPC morto não age em ticks posteriores.
G. Ficha Kvothe exporta/reimporta 39 traços, histórico, epíteto incerto, saldo e itens; sem alterar D&D 2024.
H. Jogador sempre escolhe ações e sentimentos; nenhuma perda de PV, vantagem ou Inspiração Heroica por biografia.

Este documento é evidência de auditoria/alterações em branch isolada, **não** certificação funcional do APK instalado.
