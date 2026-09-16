# WWN — avanço macro: mundo vivo + grandes projetos

Implementado neste bloco:
- Grandes Projetos WWN ligados à UI de Gestão: criação com dificuldade canônica, inicialização/compra/contribuição de Renome, apoio/retirada de facção e conclusão com desfecho explícito do Mestre.
- NPCs persistentes de mundo: criar/atualizar, mover, remover, fatos públicos, segredos GM, memória GM/jogador e projeção sem vazamento de segredos.
- Novo NpcWorldService com commits revisionados no mesmo event log da campanha.
- Gestão expandida de 4 para 6 domínios: progressão, magia, crafting, facções, projetos e NPCs.
- Regressão WORLD_LIFECYCLE cobrindo NPC persistente + memória + movimento + projeção player + ciclo completo de projeto + Renome + ajuda de facção + conclusão.

Validação: todos os QA existentes + WORLD_LIFECYCLE PASS; 139 JS source files passam node --check.
Gradle/APK/device permanecem adiados por instrução do usuário.
