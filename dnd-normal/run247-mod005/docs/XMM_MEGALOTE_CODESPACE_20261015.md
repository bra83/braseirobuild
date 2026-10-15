# Braseiro D&D — Megalote XMM no Codespace (15/10/2026)

## O que este pacote faz

Esta branch isolada foi criada a partir de um snapshot do PR #40 do Codex.
O script scripts/xmm_megalote_codespace.py transforma o inventário XMM real
em lotes executáveis por Codex CLI, com ações e ponteiros de fonte específicos,
branches/worktrees independentes e logs por job.

**Não é implementação pronta de 98 efeitos.** Geração de lotes não é código
de regras aplicado. A execução requer um Codex CLI instalado e autenticado
dentro do Codespace, ou o envio manual dos prompts ao Codex aberto.

## Escopo fixado no inventário do snapshot

- 503 monstros / 36 seções: comparação fonte, não certificação semântica.
- 236 entradas incompletas.
- 168 ações MANUAL_UNSUPPORTED: 70 registros Multiattack (68 sem plano
  e 2 parcialmente estruturados, ainda sem execução completa), e 98 outras.
- 67 previews BLOCKED_MISSING_CONTEXT aguardando integração de combate.
- 1 SOURCE_REVIEW_REQUIRED: jamais inventar regras.

Os 98 itens de execução independente são:
- 61 damage_or_save_branch
- 29 other_source_effect
- 4 attack_hit_condition
- 2 swallow_and_escape
- 1 explicit_effect_choice
- 1 monster_spell_action

O gerador calcula as contagens do JSON na worktree, divide em lotes de
até 12 ações de dano, até 10 ações especiais e demais famílias, gera
jobs/JOB_ID.json e jobs/JOB_ID.md, e falha se contagem/pin/ponteiros divergir.
Não inclui Multiattack: Codex no PR #40 já trabalha nessa família.

## Preparar — cole uma vez no Codespace

    cd /workspaces/braseirobuild
    git fetch origin automation/dnd-xmm-megalote-20261015
    git worktree add --detach /workspaces/dnd-xmm-megalote-20261015 FETCH_HEAD
    cd /workspaces/dnd-xmm-megalote-20261015
    python3 -m py_compile dnd-normal/run247-mod005/scripts/xmm_megalote_codespace.py
    python3 dnd-normal/run247-mod005/scripts/xmm_megalote_codespace.py

Isto NÃO altera a worktree do PR #39 ou o arquivo local modificado
scripts/codespace-dnd-autopilot.sh.

Verifique na saída INVENTORY_TOTAL, STATUS_COUNTS, JOBS_CREATED,
ACTIONS_TARGETED e JOBS_DIR. Os jobs são escritos em /tmp,
não em arquivos rastreados pelo Git.

## Executar — somente se Codex CLI estiver autenticado neste Codespace

    command -v codex
    codex login status
    python3 dnd-normal/run247-mod005/scripts/xmm_megalote_codespace.py --execute --workers 2

O argumento --execute abre uma worktree e uma branch por job, executa
codex exec com sandbox workspace-write e grava logs em /tmp.
Não há merge automático. Não instale o CLI sem necessidade e não use
chave paga: o login ChatGPT pode ser usado quando suportado pelo CLI.

Se Codex CLI não existir no terminal, use os prompts .md produzidos
pelo gerador no agente Codex já aberto. NÃO execute o mesmo lote em
ambos ao mesmo tempo. O Codespace sozinho não interpreta prompts nem
programa regras automaticamente.

## Proibições

- Não modificar MainActivity.java, AppDb.java, Backup21,
  scripts/codespace-dnd-autopilot.sh ou workflows GitHub Actions.
- Não tocar em Mystara, aventuras, suplementos, fontes pinadas.
- Não tocar na branch do PR #39 ou PR #40 e não fazer merge automático.
- Não duplicar o que já está compilado. Comparar cada ponteiro à fonte.
- Não rolar dados silenciosamente ou deixar Gemini/RAG arbitrar regras.
- Não fingir que preview sem aplicação é mecânica executável.
- Não rebaixar testes, não declarar 503 monstros certificados por catálogo.
- Não gerar APK final antes de integração e gates de teste.

## Contrato de cada job

Investigar o runtime existente, agrupar mecanismos determinísticos
reutilizáveis, implementar código real e teste por contrato fonte,
preservar falha fechada, e reportar commit, cobertura e bloqueios reais.
Registrar os caminhos alterados, testes executados, resultados e SHA.
Se faltarem API pública de combate, UI ou SQLite, implementar somente
o núcleo independente e deixar um bloqueio rastreável. Não modificar
áreas protegidas para esconder a dependência.

O snapshot do pacote pode ficar atrás do Codex em execução. Comparar
todos os resultados ao PR #40 MAIS RECENTE antes de integrar. O PR #39
precisa ficar verde antes de integrar o combate ao fluxo de sessão real.
