# BRASEIRO UNIVERSAL — GH MÓDULOS 004

Data: 2026-10-10. Base exclusiva: `BRASEIRO_DND_NORMAL_RUN245_GITHUB_MODULOS_003_FULL_SOURCE.zip`, Braseiro D&D NORMAL Java/Android.

## Objetivo
Tornar os cinco adaptadores GitHub da rodada 002/003 úteis durante o jogo: correções confirmadas pelo jogador em ficha, inventário e estado do mundo devem ser transacionais, recuperáveis e incluídas no contexto da próxima chamada Gemini. A Mesa Universal deve permitir retomar múltiplas fichas já criadas.

## Código alterado / introduzido
1. `app/src/main/java/com/braseiro/dnd5e/adventure/UniversalEffectTransaction.java` (novo): `apply_effects` transacional. Efeitos autorizados: `resource`, `item`, `time`, `world_fact`. Valida todos os efeitos em cópia do estado, bloqueia evento duplicado, revisão obsoleta, número fracionário, recurso indisponível, inventário negativo e fato duplicado. Possibilita vincular lote de efeitos a um `request_id` resolvido uma única vez.
2. `UniversalRpgEngine.java`: comando `apply_effects` usando a operação acima; mantém o `AdventureEngine.processUniversal` como único caminho para salvar estado e divulgar commit.
3. `TomeEntityBridge.java`: rejeita recursos fracionários, valores numéricos inválidos, chaves fora do padrão e nomes desproporcionais.
4. `UniversalLiveSession.java`: inclui tempo decorrido e fatos do mundo confirmados no contexto de próxima cena Gemini; não permite que o modelo modifique estado por texto.
5. `MainActivity.java`: na Mesa Universal, botão Android `ALTERAR FICHA · INVENTÁRIO · TEMPO · MUNDO` com edição local confirmada; exibe progresso temporal e fatos; seleciona personagens existentes ou cria vários perfis novos com IDs distintos; retoma teste pendente correspondente ao personagem correto.
6. `UniversalGithubLab.java`: laboratório agora executa inventário, avanço de tempo e fato do mundo e verifica recuperação em sessão reaberta (mock de persistência).
7. `UniversalGithubModulesIntegrationTest.java`: cinco testes adicionais de efeito composto, rollback, validação, vínculo com teste e ficha inválida (total 14 testes).
8. `tools/github_qa/run_java_contract_tests.sh`: inclui classe de transação na compilação dos testes.
9. `tools/github_qa/src/ParseJavaSources.java`: verificador Java puro via parser JDK 21.

## Contrato `apply_effects`
```json
{
  "action": "apply_effects",
  "event_id": "player.20261010.001",
  "character_id": "hero-id",
  "expected_revision": 0,
  "effects": [
    {"kind":"resource","key":"health","delta":-1},
    {"kind":"item","item_id":"torch","name":"Tocha","delta":2},
    {"kind":"time","minutes":30},
    {"kind":"world_fact","fact_id":"bridge.1","text":"A ponte foi danificada."}
  ]
}
```
`request_id` é opcional: quando informado vincula os efeitos a um resultado de teste já resolvido. Cada `request_id` aceita um único lote de efeitos; cada `event_id` é único; operações inválidas são rejeitadas por inteiro. Somente operações confirmadas pela interface do jogador devem emitir esse comando. Textos do Gemini NÃO são executores desse comando.

## Testes e limites
- Host JVM: **14/14 testes** de contrato concluídos sem falha e 2/2 execuções de laboratório com persistência simulada.
- JDK 21 `JavacTask.parse`: **7/7 arquivos Java** selecionados sem falha sintática.
- Ainda **não validado**: compilação Android completa no BRELGA; testes instrumentados de `AppDb`/SQLite real; chamada Gemini com chave real; interação visual e TTS no Galaxy S23 Ultra/A57. Não confundir testes JVM mock com testes Android reais.
- Nenhum APK gerado; não foi feita alteração no D&D clássico, nas regras G05, no Hybrid Fastplay ou no fluxo de build.
- Nenhum novo repositório externo foi portado nesta rodada. O avanço une e torna utilizáveis os cinco adaptadores já integrados: Tome, Loreweaver, AetherTable, Year Zero Roller e SWRPG Dice. Licenças e origens em `docs/THIRD_PARTY_NOTICES.md` e `docs/AUDITORIA_REPOSITORIOS_GITHUB.md`.

## Próximos bloqueios de produção
1. Testes de persistência SQLite real e rollback de falha de escrita no Android.
2. Validação no aparelho da seleção de vários personagens e da mesa Gemini (somente pós-implementação dos testes locais).
3. Melhorar a interface de consequência para associar resultado recém-resolvido sem o jogador precisar selecionar manualmente o request_id.
4. Incorporar mais módulos dos projetos restantes apenas quando puderem operar na mesma sessão, medindo regressões.

## Proibição de regressão
Sempre iniciar da fonte 004, conservar `applicationId`, DB Schema 61, Backup 21, versão e recursos de áudio nativos. Nunca usar FASTPLAY como baseline, não reimplementar a mesa em TypeScript, não alterar regras D&D atuais para encaixar Year Zero, não afirmar que todos os módulos upstream foram importados.
