# P157 — conferência mecânica de talentos AoR

Fonte editorial de referência: árvores espanholas *La era de la rebelión — Árboles de talentos* (Comandante: Estratega; Diplomático: Embaixador e Intendente), https://es.scribd.com/document/863888834/La-era-de-la-rebelion-Arboles-de-talentos . O checkpoint P156 permanece a autoridade dos perfis de combate e das cinco aventuras; este crosswalk NÃO certifica integralmente os livros de expansão.

| Nome na árvore AoR | Posições | Mecânica codificada | Consumidor / teste |
|---|---:|---|---|
| DON DE GENTES | 2 | Remove 1 dado de Contratempo por graduação apenas em Carisma e Liderança | `talentSkillPoolModifiers` → `GameController.resolveSkillCheck`; teste de dados e persistência |
| INDISTINGUIBLE | 2 | Melhora 1 vez a dificuldade por graduação apenas em testes feitos para identificar o personagem | `indistinguishableIdentificationDefense` → `resolveIncomingIdentificationCheck`; resultado persistente no journal de testes avançados |
| CHANCHULLERO | 2 | Aumenta em 10% por graduação o valor-base ao vender itens legalmente; sem bônus para comércio ilegal ou itens restritos | `wheelAndDealLegalSale` → `sellOwnedAsset` → proposta estruturada do Mestre; registro de preço-base, bônus, créditos e remoção de propriedade |
| FINTA | 2 | Manobra 1 vez por rodada, sofre até o número de graduações de Tensão e melhora nessa quantidade a dificuldade dos ataques à distância recebidos até o próximo turno | `feintTalentOption` → `useFeint` → `_resolveNpcTurn`; término do efeito no próximo turno; teste de rollback |

Regras de segurança: sem conceder efeito por texto traduzido arbitrário; sem aplicar bônus social a outras perícias; sem aplicar Indistinguível a ataques; sem bônus comercial a vendas ilegais; sem Finta contra ataques corpo a corpo. Todos os efeitos ativos passam pelo estado real do controlador e são persistidos ou revertidos integralmente.

Pendências: demais 98 posições AoR; cotejo integral de todos os livros EotE/AoR e de todas as cenas/estatísticas das cinco aventuras; APK Android e áudio real Kokoro.
