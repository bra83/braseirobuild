# D&D normal — correção da compatibilidade JSON Android

A verificação de integridade falhava pela conversão Git de CRLF para LF. Foram preservados os 1437 hashes originais e acrescentadas 123 equivalências provadas com o arquivo de origem; corrupção de conteúdo, binário, ausência e adição continuam reprovando.

Os runs 38106137702 e 38106538626 confirmaram a integridade no BRELGA e expuseram chamadas JSON sem tratamento de JSONException em UniversalAiProposalPort e UniversalEffectTransaction. Corrigidos também os métodos equivalentes de UniversalRpgEngine, UniversalLiveSession e UniversalGithubLab, preservando instruções e exceções RuntimeException existentes. A dependência org.json:json:20240303 é exclusiva de testes JVM.

## Verificação local executada

- Java 17: 14 classes de produção compiladas com o JSON do Android obtido do espelho oficial google/j2objc (jre_emul/android/platform/libcore/json/src/main/java/org/json). JSONException é checked. Apenas anotações de metadata Android foram removidas da cópia temporária; lógica e assinaturas JSON preservadas.
- 21 contratos: 21 PASS, 0 FAIL com implementação JSON real, não com os doubles JSON do harness antigo. As mesmas declarações throws Exception foram incorporadas ao teste canônico em 0b29da6686b09f1bc684c5b38e5ef0977ce6ce8c; as 21 asserções e corpos dos testes foram preservados.
- Laboratório: duas execuções PASS, reabertura com persistência simulada. AdventureEngine, AdventureModule e AdventureRulesAdapter são adaptadores locais de teste; esta prova não homologa SQLite Android nem a aplicação inteira.
- Oito verificações de integridade PASS, incluindo recusa de corrupção de CSV, deriva no código, corrupção binária, arquivo ausente e arquivo adicional, além de checkouts LF e CRLF.

Fonte permanece com 1437 arquivos; manifesto autoriza exatamente 18 modificações. Package com.braseiro.dnd5e, versionCode 606547, DB61 e Backup21 preservados. O workflow BRELGA executará compileDebugJavaWithJavac, suíte completa e exigirá XMLs verdes de RUN246/RUN247. Sem APK nesta rodada. G05 mecânico de 235 linhas não foi promovido por estas correções de compilação.

## Android no BRELGA — correções posteriores

- Commit 7d1b847e5188575f4dd56268fd341b80e08198bc: tratamento de JSON em seis limites de callbacks da MainActivity, preservando diálogos, Gemini, aplicação/recusa de efeitos e rolagem manual.
- Run 38107624298: integridade PASS; :app:compileDebugJavaWithJavac PASS. A suíte parou na compilação de UniversalGithubModulesIntegrationTest por JSONException checked; não houve homologação dos testes nesse run.
- Commit 0b29da6686b09f1bc684c5b38e5ef0977ce6ce8c: apenas declarações throws Exception nos helpers e métodos @Test que precisavam delas, sem ignorar testes ou reduzir expectativas.
- Validação de confirmação: https://github.com/bra83/braseirobuild/actions/runs/38107907616. Resultado ainda pendente no momento deste registro; consultar logs e XMLs da execução para a decisão final.

O runner BRELGA é compartilhado com Forbidden Lands, mas branches, código, evidências e gates dos projetos permanecem independentes.
