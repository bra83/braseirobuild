# Proposta separada — validação RUN245 no BRELGA (não aplicada)

Se a RUN245 for publicada em uma branch normal remota e a disponibilidade do BRELGA for confirmada, criar um workflow de validação **manual**, sem gatilho de `push`.

## Forma proposta

- Receber uma única branch/ref explícita e fazer checkout desse ref exato.
- Usar os labels históricos `[self-hosted, Windows, X64]` e abortar se `RUNNER_NAME` não for `BRELGA`.
- Executar `:app:compileDebugJavaWithJavac` e `:app:testDebugUnitTest`; armazenar somente logs textuais.
- O job de validação não executa `assembleDebug`, não empacota APK e não publica artefatos binários.
- Não exigir segredos para compilar/rodar testes; não imprimir valores de variáveis.

## Condições antes de usar

- O runner inventory/health atual não foi exposto pelo endpoint GitHub read-only disponível; os labels acima vieram dos workflows RUN229/RUN230 existentes e não provam que o runner esteja online agora.
- O branch RUN245 do checkout local não tem remote e não aparece no índice de código RUN245 do repositório. O ref precisa ser disponibilizado antes de executar essa proposta.
- O workflow manual precisa existir na branch padrão para que GitHub permita iniciá-lo por `workflow_dispatch` e selecionar outro ref.

Nenhum arquivo de workflow foi criado, alterado ou disparado nesta frente.
