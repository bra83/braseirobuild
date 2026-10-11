# Braseiro D&D NORMAL RUN247+MOD005 — validação BRELGA

- Base: `BRASEIRO_DND_NORMAL_RUN247_MOD005_RECONCILIADO_CANDIDATO_NAO_HOMOLOGADO.zip`
- SHA-256 do arquivo original: `2774b450ecf5be179976199b22bab8584f76939dcf305d82a84171a199282e06`
- Versão `606547`, DB61, Backup21, `applicationId=com.braseiro.dnd5e`.
- Árvore original: 1436 arquivos; não substituída por RUN245 nem Fastplay.
- A fonte exata é armazenada em nove partes SHA-pinned no Google Drive; o runner só a extrai após validar tamanho, SHA individual, SHA final, ZIP CRC e identidade.
- Workflow dedicado: `.github/workflows/dnd-run247-mod005-normal-brelga.yml`.
- O runner BRELGA reconstrói o ZIP numa pasta temporária e deposita a árvore integral exclusivamente em `dnd-normal/run247-mod005/` da branch isolada. Arquivos preexistentes têm de ser byte a byte idênticos ou o processo falha; não existe fallback.
- Após commit/push dessa árvore, roda os quatro gates Android (javac, RUN246, RUN247, suíte completa). Não há `assembleDebug`, assinatura ou geração intencional de APK.
- Falhas e resultados de cada gate são preservados em logs, XMLs e artefato do run; a aprovação exige código de saída 0 em cada gate.
- **NÃO HOMOLOGADO** até a execução Android no BRELGA e conclusão dos demais gates. A matriz G05 conserva 235 linhas pendentes de executor aprovado.

## Referências

- Origem/reconciliação: `RECONCILIATION_EXECUTION_REPORT.md` lido e respeitado; 39 arquivos recuperados, quatro modificados, zero remoções.
- O workflow histórico `dnd-run245-normal-brelga.yml` **não deve ser reutilizado**: recupera outra árvore, aplica patches antigos e valida `606545`.
- Drive: `DND_SANDBOX_WORKING/RUN247_MOD005_G05_BRELGA_VALIDATION_20261010` (manifesto original + partes).

## Restrições operacionais

1. Não promover automaticamente versão ou gate G05 apenas por teste JVM com doubles de `org.json`.
2. Repetir o teste Android ao modificar executores ou fluxo de persistência.
3. Antes de qualquer novo commit de produção, guardar `SOURCE_GIT_COMMIT`, ZIP-SHA, logs/XMLs e diff.
4. Não tocar Fastplay nem projetos de outros sistemas.
