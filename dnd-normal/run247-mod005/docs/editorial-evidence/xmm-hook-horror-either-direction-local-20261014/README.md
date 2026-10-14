# XMM: deslocamento forçado, penalidades temporárias e vantagem no próximo ataque

- Fonte XMM fixada no commit `8c026b807fac21862a309379a0c3228a0683198b`; comparação independente dos 503 monstros, 36 seções e campos de combate preservada em `source-verification.json`.
- Hook Horror / Hook aceita movimento em linha reta até 5 pés para perto ou para longe, com direção indicada pelo destino escolhido. Tamanho máximo Grande, caminho livre, dano, recibo e transação são conferidos pelo executor.
- A ação bônus Scout Captain / Aim concede vantagem na próxima rolagem de ataque do turno; o efeito fica salvo no combatente, entra no cálculo do ataque simples e do primeiro ataque de um multiataque, e é consumido após a rolagem. Fonte comparada e sinalização manual removida após o teste do fluxo.
- O ledger `xmm_movement_speed_effect_v1` aplica e persiste penalidades temporárias de deslocamento e expira na fronteira de turno correspondente para: Lacedon Ghoul / Icy Bite; Berserker Commander / Javelin; Merfolk Skirmisher / Ocean Spear; Warrior Commander / Longbow; Hobgoblin Warlord / Javelin; Frost Giant / Great Bow.
- O inventário independente registra 168 habilidades ainda exigindo resolução manual em `manual-effect-backlog-168.json`. Nenhum desses itens foi removido do controle; automação total do combate permanece não certificada.
- Suíte Android completa: 2.573 testes, 0 falhas, 0 erros e 0 ignorados. Comparador Python: 55 testes aprovados. `git diff --check` passou.
- Execução local: `JAVA_HOME=/workspace/jdk21`, `ANDROID_HOME=/workspace/android-sdk`, `ANDROID_SDK_ROOT=/workspace/android-sdk`, `bash gradlew testDebugUnitTest`.
- Os arquivos do backlog contêm ponteiros e nomes de habilidades, sem reproduzir prosa protegida.
