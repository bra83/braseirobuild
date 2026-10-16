# HANDOFF CANÔNICO — BRASEIRO STAR WARS VTT R54 P192

**Base:** P191, SHA256 `0783b3fbb8ac710f7b5e3874be644bf165afcade02962e9a3003ae27d8db8787` (98.189.919 bytes; 6.304 arquivos). **Novo checkpoint:** P192. Preservar todas as entregas anteriores. SHA/tamanho/arquivos finais no `.zip.sha256`, no manifesto delta e em `evidence/P192/IMPLEMENTATION_EVIDENCE.json`.

## Implementação real — SWE09 *The Jewel of Yavin*, Tabela 2-1

**Fonte consultada nesta execução:** PDF oficial *The Jewel of Yavin*, Google Drive ID `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, Tabela 2-1 na página 70 do PDF / 69 impressa. O texto foi recuperado do próprio PDF; cotejo **direcionado**, não certificação integral.

1. `app/src/main/assets/vtt/js/adventure/jewelBankSlicingSpends.mjs`: manteve os três resultados P191 e acrescentou escolha **pós-rolagem** para licitante adicional (+10.000 créditos **potenciais**, condicionados a sucesso líquido e a 3 Vantagens ou 1 Triunfo), Tensão real no personagem que realizou o slicing (1 Ameaça, sem ultrapassar o limiar), equipes Wing Guard (3 Ameaças ou 1 Desespero; tamanho igual às fichas de PCs do grupo; ETA +240 minutos) e rastreamento adicional (-60 minutos por 3 Ameaças ou 1 Desespero), além da assinatura eletrônica (1 Desespero). Símbolos são debitados exatamente uma vez; recibos `BANK_SLICE_SPEND` e replay validam IDs, dados, símbolos, custos, estado e tempo. O bônus por licitante **não altera créditos** do personagem nem liquida o banco.
2. `jewelBankingOperations.mjs`: uma assinatura capturada aplica **um upgrade real à dificuldade** de uma nova rolagem de slicing G-100. A relação entre a assinatura e a próxima rolagem é verificada pelo replay; não se aplica duas vezes.
3. `jewelOfYavin.mjs` e `core/controller.mjs`: controlador passa a ficha do slicer e o número de personagens com ficha no grupo. A Tensão é efetivamente gravada em `character.derived.strain.current` sob transação atômica; ação inválida reverte mundo e personagem. O sistema recusa lançar a Tensão no jogador quando quem rolou foi um slicer NPC sem limiar validado.
4. `r46/final-runtime.js`: controles PT-BR para os resultados novos, seleção de licitante, Tensão, equipes, rastreamento e assinatura; exibe bônus condicionado, número de patrulhas e ETA. Android **1.0.192**, bundle offline **91 módulos**, Java/Kokoro **8/8 arquivos idênticos a P191**.
5. Compatibilidade: recibos P191 continuam com estrutura antiga, sem migração destrutiva; teste de reabertura e teste de mistura P191/P192. `tests/uae-p192-jewel-bank-table-completion.test.mjs` inclui **14 testes novos**.

## Limites explícitos

- A **Tabela 2-1 tem as escolhas descritas representadas**, mas a execução no mundo vivo **ainda não está totalmente homologada**: equipes Wing Guard têm ETA persistido e rastreável, porém ainda não geram encontro NPC automaticamente ao chegar; a assinatura é aplicada no G-100, mas não em todos os demais testes de slicing de Cloud City; substituição de ferramentas para remover assinatura não está implementada.
- Bônus por licitante é **direito potencial**, sem pagamento/saque, porque liquidação bancária e comprovação dos saldos ainda faltam. Pos Podura não é aceito como conta adicional com fundos.
- Slicer NPC contratado sem limiar de Tensão validado não recebe a opção automatizada de Tensão; exige futura ficha e integração oficial. Patrulhas usam número de fichas do grupo, sem criar NPCs/combate fictícios.
- Restraining bolt ainda depende de confirmação do Mestre, **não** está vinculado a item de inventário. Não confundir com `restraining_device`.
- Não houve cotejo integral das cinco aventuras nem certificação semântica de todos os talentos AoR/EotE/expansões. Sem SDK/Gradle/adb, CI BRELGA, APK ou homologação física. Testes Node por arquivo **não** equivalem a homologação Android nem a suíte monolítica verde.

## Próximo P193+

Integrar encontro real Wing Guard na chegada com consequências, assinatura eletrônica em todos os testes de slicing de Cloud City e remoção mediante substituição real de ferramentas; concluir liquidação/limpeza de evidências/traição/fuga SWE09; validar restraining bolt contra o inventário e PDF; terminar SWE02 New Meen/Geonosis/Teemo; comparar integralmente cinco aventuras cena a cena e as mecânicas AoR/EotE/expansões. Rodar focal, regressão por arquivo e seis stress, preservar Kokoro, gerar ZIP integral + SHA + delta + handoff, upload Biblioteca e readback. Não declarar CI/APK/certificação sem prova.
