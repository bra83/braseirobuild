# HANDOFF AUTOMÁTICO — BRASEIRO D&D 5E NORMAL
## Identidade e limites (confirmados em 14/10/2026)

- Repositório: `bra83/braseirobuild`.
- Branch canônica: `antigravity/dnd-normal-fechamento-gates`. **Nunca usar main nem outra branch para as alterações D&D Normal.**
- Projeto Android Gradle existente: `dnd-normal/run247-mod005` (relativo à raiz do Git).
- Caminho absoluto do Codespace: **detectado na instalação** via `git rev-parse --show-toplevel`; não presumir nome de Codespace.
- Último checkpoint de build previamente documentado: **RUN247-MOD005**, `dnd-normal/run247-mod005/CHECKPOINT_STATUS.md`; package `com.braseiro.dnd5e`, DB61, Backup21, versionCode 606547. APK debug do checkpoint não prova Android real ou assinatura compatível para atualização.
- PDFs oficiais: pasta Google Drive [D&D5e](https://drive.google.com/drive/folders/13wB-KfYCe2k5xGmb-z2D09KRDJtSyfHu). Contém Player's Handbook 2024, Dungeon Master's Guide 2024, Monster Manual correspondente, entre outros. **Não** assumir que todos os dez suplementos estejam presentes.
- Material auxiliar 5e.tools: esclarecimentos dentro das permissões e disponibilidade; nunca substituir fonte primária, transcrever prosa protegida ou usar RAG como executor mecânico.
- **MYSTARA CONGELADO**: nenhuma alteração em arquivos, mecanismos ou conteúdo Mystara. Aventuras fora do escopo.

## Checklist imutável de fechamento — etapas homologadas

| Etapa | Item | Estado |
|---|---|---|
| 01 | Proveniência e classificação — 483 itens | HOMOLOGADA anteriormente; não reabrir sem regressão demonstrada |
| 02 | XSAC — 14 seções, 163 rulings | HOMOLOGADA anteriormente |
| 03 | XMM — 36 seções, 503 monstros | ABERTA: riders/traits/multiattacks/spellcasting incompletos |
| 04 | FRHOF — 53 seções | PENDENTE |
| 05 | FRAIF — 151 seções | PENDENTE |
| 06 | EFA — 48 seções | PENDENTE |
| 07 | AU — 45 seções | PENDENTE |
| 08 | RHW — 54 seções | PENDENTE |
| 09 | ABH — 16 seções | PENDENTE |
| 10 | LFL — 20 seções | PENDENTE |
| 11 | NF — 46 seções | PENDENTE |
| 12 | Integração global | PENDENTE |
| 13 | APK reproduzível | PENDENTE |
| 14 | Homologação Android físico | PENDENTE |
| 15 | Entrega final | PENDENTE |

**Progresso homologado: 2/15 = 13,3%, não percentual de código.**
As etapas 02–11 correspondem aos dez livros adicionais planejados; não adicionar livros de aventura. Sem autorização do usuário, não avançar a ETAPA 04 enquanto a 03 estiver aberta.

## Fonte de verdade e arquitetura

- Código real: `dnd-normal/run247-mod005/app/src`.
- Comparadores XMM pinados e testes negativos: `dnd-normal/run247-mod005/tools/verify_xmm_503_source.py`, `test_verify_xmm_503_source.py`.
- Matriz e auditoria: `dnd-normal/run247-mod005/docs/XMM_ETAPA03_STATUS_20261013.md` e `CORE_BASE_JOGAVEL_STATUS_20261014.md`.
- Gates de regressão BRELGA existentes, agora reutilizados pelo workflow canônico: `.github/workflows/dnd-adventure-atomicity-brelga.yml`.
- Evidência XML real: branch isolada `dnd-normal-qa-evidence/runs/<RUN_ID>/attempt-1.json`. Conferir sempre SHA, testes, falhas e skipped.
- Evidências locais do último ciclo antes desta configuração: 503 monstros e 36 seções comparados; 120 recargas; 132 registros de spellcasting; algumas ações com execução parcial. Comparação estrutural **não** certifica execução integral.
- Regra: fonte → normalização de dados → executor existente → UI/estado → transação SQLite → testes e logs, sem rolagens ocultas nem duplicação de efeitos, PV, XP ou recursos.

## Como a automação funciona

- `scripts/codespace-dnd-sync.sh install` instala o hook idempotente de terminal, executa em primeiro plano somente para instalação e inicia um monitor desacoplado.
- Cada 120 segundos: apenas `antigravity/dnd-normal-fechamento-gates`; recusa árvore suja/divergência; `git fetch` de leitura e **somente `git pull --ff-only`** para atualizar; regressão uma vez por novo SHA.
- Registros locais: diretório Git interno `.git/dnd-normal-sync/` (ou equivalente em worktree), não versionado.
- Publicação opcional de estado no commit SHA via `gh api` quando a conexão concede `statuses:write`; status de Codespace não cria commit.
- Com Codespace suspenso, processos do ambiente não avançam. Novo terminal invoca o hook; `stop` desabilita o reinício automático até `start`.
- GitHub Actions: `dnd-normal-automation-ci.yml` chama gates já existentes, e atualiza somente a seção AUTO deste handoff após execução do HEAD real, sem duplicar testes caros.
- APK: `dnd-normal-apk-manual.yml` é separado, **somente por workflow_dispatch** e com SHA/CI aprovados; APK debug não é release nem validação em aparelhos.
- GitHub-hosted sem jobs (billing/infra) ≠ erro do código. O Codespace reproduz os testes quando estiver acordado e configurado, diferenciando INFRA de FAIL em log.

## Próximo trabalho de código, em blocos grandes e coesos

Continuar exclusivamente ETAPA 03: integrar ações XMM com efeitos manuais em clusters source-bound reutilizando motores; priorizar riders, traits, 82 Multiattacks complexos e magias dos 132 conjuradores; testar mutação real em SQLite/backup e impedir efeitos automáticos não aprovados. Não registrar cobertura por apenas existir índice, texto, catálogo ou relatório.

## Histórico

Este documento foi criado com a infraestrutura de automação. Não foi usado como prova de conclusão de qualquer etapa. As edições automáticas ficam estritamente na seção abaixo; preservar todo o restante.

<!-- DND_AUTO_BEGIN -->
## Último pacote verificado automaticamente

- Estado inicial: **aguardando primeiro CI do workflow canônico**.
- Última atualização de handoff: configuração da infraestrutura, sem nova certificação.
- Estado Android físico: **não homologado**.
- Próxima ação: conferir CI na branch e continuar implementação mecânica ETAPA 03.

<!-- DND_AUTO_END -->
