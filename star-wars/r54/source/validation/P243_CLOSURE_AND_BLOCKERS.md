# R54 P243 — New Meen: escolta rastreada e milícia Twi'lek

**Fonte primária:** SWE02 *The Long Arm of the Hutt*, páginas impressas 20–21 (PDF oficial gratuito Fantasy Flight Games). **Base íntegra:** P242, SHA-256 `97a5c6165f6f7cb85948650240c61fb1ff21d8a68d1ee8d9cc1ff5b9100fd54b`.

## Implementado neste lote

- Escolta explícita: 20 adultos Twi'lek + algumas crianças (quantidade das crianças **não especificada** pela fonte); estados ASSEMBLED → EN_ROUTE → ARRIVED, ou ABANDONED. O Mestre escolhe os ticks de viagem (a aventura **não especifica duração**); os ticks são consumidos pelo executor de mundo vivo e não duplicados quando parte do tempo já passou. Recibos de partida, chegada e abandono persistem; chegada registra favor significativo a B'ura B'an, mas **não liberta New Meen** nem concede PE/créditos automáticos.
- A rota legada que permite seguir diretamente para Ota permanece compatível, mas agora registra **ABANDONED** sem qualquer favor. A rota de chegada confirmada tem escolha própria e gate `newMeenEscortArrived`. Uma derrota `LOST` não é transformada em `SECURED`.
- Milícia Twi'lek: empréstimo real de uma arma blaster própria, removida do inventário do personagem por transação atômica; um adulto armado por arma emprestada, no máximo 20. Ações exigem aprovação explícita do jogador, não são autônomas. Implementado o método **opcional de combate abstrato sugerido na p.21**: cada miliciano armado causa um Ferimento direto a um grupo de capangas de Drombb, e um aliado é eliminado do combate por fogo de resposta a cada rodada. No máximo uma ação por rodada e encontro, sem atingir Drombb ou agir depois do desfecho. Perfil de referência: mesmos minions humanos/Aqualish oficiais. Sem inventar rolagens.
- Controller, bridge e UI offline PT-BR; os dois painéis aparecem mesmo quando nenhuma nave está ativa. Validação de recibos, perdas e sequência de escolta, rollback e três reaberturas.

## Limites explícitos — não certificar como concluído

1. A **escolta integral com possíveis assédios/combates de Drombb na estrada** ainda exige encontros/negociações reais e adjudicação de consequências, não basta um relógio de viagem. O módulo não gera um encontro ou vitória fictícios.
2. A milícia usa o método abstrato opcional **sob aprovação do jogador**, não iniciativa individual completa, IA de aliados, nem perfis separados de combate para cada Twi'lek. Empréstimos não foram convertidos em sistema de devolução automática de equipamentos após a batalha.
3. SWE02: Nyn — granadas e fuzil com ficha EotE primária e defeitos na primeira utilização; Krayt Fang Beginner, combustível/carga e iniciativa veicular; restante dos atos 2/3, Jabba, Teemo e finais; auditoria integral das 46 páginas do PDF.
4. Outras quatro aventuras (Under a Black Sun, Beyond the Rim, The Jewel of Yavin, Mask of the Pirate Queen) e regras EotE/AoR/expansões, incluindo 360 consumidores AoR, continuam sem certificação semântica individual.
5. APK/Gradle/CI BRELGA/GitHub push/ADB e Galaxy A57/S23 Ultra **não executados**; somente nos gates finais.

**Proteção de checkpoint:** promover P243 apenas depois de ZIP completo + SHA/CRC, delta reconstruível e upload Drive em partes <=32 MiB com readback de TODAS as partes e SHA do ZIP remontado. P242 continua canônico remoto até esse gate. Nenhuma exclusão de backup autorizada neste lote.
