# XMM — ETAPA 03 (aberta)

- Multiattack: 306 registros; 224 planos estruturados; 82 casos exigem resolução manual. Isso não certifica execução integral de combate.
- Correção desta rodada: roteamento `spell_options` e `spell_usage_check` corrigido para `xmm`; cobertura e prova no registro `XMM_ETAPA03_GATEWAY_FIX_EVIDENCE_20261013.md`.
- BRELGA no SHA `5fb607be60fdd5baef6eefd225fe2fc6410e34e5`: 1.642 testes / 84 suites, zero falhas; ver run 38381018048. XMLs e hashes no branch `dnd-normal-qa-evidence`, arquivo `runs/38381018048/attempt-1.json`.
- ETAPA 03 permanece aberta: 35/36 seções estão `IMPLEMENTED_NEEDS_TEST`; 503 blocos não têm certificação semântica de todos os efeitos, traits e riders de combate. Não compilar APK nesta etapa.

- Nova implementação conservadora: `Xmm2025SaveDamageEngine` indexa 59 ações sem riders adicionais (um único save, dano simples, metade no sucesso), com fonte pinada e verificação de alvo/recarga/resultado de salvaguarda/rolagem fornecidos pelo Mestre; calcula dano pré-resistências, sem mutar estado. Testes parametrizados por ação e testes de falha fechada. Não cobre as demais ações com efeitos adicionais. CI pendente para esta alteração.
