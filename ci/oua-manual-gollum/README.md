# O Um Anel — prototipos de rolagem manual e Conselheiro Gollum

Este branch **nao e um APK final**. O pacote completo, codigo e testes, esta em:
- Google Drive /Oua/BRASEIRO_OUA_DUAS_IMPLEMENTACOES_PROTOTIPO.zip
- SHA256: e8334080c3a270f824ff098a9d4680c0e0a6a0c6e9634537a25fa3bc6c95a8ea

## Trabalho realizado
1. Nucleo transacional sem gerador de resultados: pedido persistivel, clique obrigatorio, observacao de faces da fisica 3D, interpretacao deterministica, commit atomico com auditoria. Nao aceita fallback WebCrypto nem forced_settle e nao inicia rolagens sozinho apos reabrir.
2. Gollum consultivo, somente leitura, com whitelist de estado publico, alertas por pendencias e uso opcional do TTS; evita spoilers.
3. Dez testes locais Node aprovados.

## Integracao que AINDA FALTA para permitir testes no APK
- Colocar o estado pendente em CampaignState e suas transacoes canonicas junto aos efeitos. No OUA real, ja existe pendingTest/pendingDiceRoll mas nao cobre todos os tipos de rolagens.
- Redirecionar TODOS os sorteios aleatorios dos motores de Jornada, Combate, inimigos, encontros, clima dependente de dados e tabelas; auditavel, sem excecoes ocultas.
- Usar o PhysicalDiceEngine e Dice3DOverlay Android ja presentes no repositorio; conferir correspondencia de runa de Gandalf/Olho, d12 de Proeza e d6 de Sucesso. Rejeitar resultados forçados.
- Adicionar GollumAdvisor a raiz da tela, com callback TTS do motor existente e estado limitado as informacoes publicas.
- Compilar experimentalmente no BRELGA, testar com dados reais, fechar e reabrir com pendencias 3 vezes. Nao tocar no release 4.9.215 ate passar.

**Importante**: nos arquivos inspecionados, handleGenerateJourneyEventManual chama secureRoll diretamente, portanto versao 4.9.215 NAO obedece ao requisito zero rolagens ocultas. Nao marcar este escopo como homologado antes de substituir essas chamadas e auditar as demais.
