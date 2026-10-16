# P179 — mudanças de código integradas

- Corrigida a posição AoR Tactician r3c1 (SUPERVIVIENTE) para consumidor `NATOUT` de rerrolagem real, evitando confusão com melhoria de habilidade assinatura Unmatched Courage.
- Adicionados `TIME_TO_GO`, `TIME_TO_GO_IMPROVED`, `ITS_NOT_THAT_BAD` ao mapa explícito de compras AoR.
- Criado `rules/aorP179Reactions.mjs` para emissão/validação de recibos de Explosão NPC baseados em armas reais e Vantagens roladas, sem dano fornecido pelo chamador.
- O combate NPC passa a registrar recibos de explosão quando rola Vantagens suficientes; o Mestre confirma os alvos atingidos. Uma ativação gasta o recibo, e a repetição é rejeitada.
- `resolveIncomingBlast` aplica dano real com absorção e limite de ferimentos, ou usa Time to Go mediante gasto de 1 Destino luminoso e confirmação de saída; a versão aprimorada pode retirar um aliado Engajado. Deslocamentos ficam registrados em estado persistente e log.
- `prepareItsNotThatBad` permite preparar reação médica para aliado real. A tentativa ocorre apenas no momento em que seria criado um ferimento crítico (por limiar ou ataque), exige Medicina Difícil e consome uso único da sessão mesmo na falha; não elimina dano.
- `applyCompanionDamage` também suporta a reação médica explicitamente confirmada pelo Mestre em cenas sem iniciativa, com rollback.
- Estado validado contra recibos, alvos, fonte NPC/arma e eventos, e limpo no início/fim de combate.
- Bridge Android PT-BR e painel de ações reativas; bundle offline recompilado. Fontes Java Kokoro/TTS preservadas sem alteração.

PENDENTE: certificação editorial integral EotE/AoR/expansões e cinco aventuras contra PDFs; APK Android real; homologação de TTS físico.
