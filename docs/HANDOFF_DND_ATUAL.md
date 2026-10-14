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

## Codespace — autopiloto mecânico com Codex (opcional)

- Controlador: `scripts/codespace-dnd-autopilot.sh`; política: `scripts/dnd-autopilot-policy.md`.
- Instalação: `bash scripts/codespace-dnd-autopilot.sh install` **no terminal do Codespace** com a branch canônica limpa.
- Pré-requisitos: `gh auth status`, `codex login status` com login ChatGPT, Python 3, Java e Android SDK. Se faltar autenticação, a instalação interrompe e fornece instrução concreta; não usa chave de API paga.
- O controlador não tem acesso remoto ao Codespace a partir deste chat. Só roda com o Codespace ligado.
- Uma rodada por hora, no máximo; única instância via lock `flock`; `status`, `stop`, `start`, `once`.
- Trabalho em worktree e branch isolados. O Codex edita somente fontes Android D&D, testes, verificador e evidências locais XMM. Caminhos diferentes, Mystara, fonte sem validação, mudanças do HEAD e testes/CI não verdes bloqueiam **antes** do push.
- Regressão local Gradle completa + comparação pinada antes do commit. CI GitHub canônico verde no SHA-base antes de fazer o trabalho. Push sem force só quando HEAD remoto ainda igual à base e sem substituição de trabalho. Integração gera novo CI; nova rodada espera seu resultado.
- Em infra parada/runner BRELGA na fila, o agente não presume PASS nem integra código sem testes. Pode consultar GitHub e tentar novamente quando houver CI verde.
- O agente de IA não é garantido pela automação GitHub: depende do Codex CLI instalado, login válido, limites da conta e terminal Codespace iniciado.

## Arquitetura híbrida verificada em 14/10/2026

- Motor Bárbara 1.1.3: última versão encontrada em [Motor Barbara/LATEST](https://drive.google.com/drive/folders/1oVAD0khD3dqNF1gu6EmjjBi2MSAWxplG) em 14/10/2026. O D&D já integra `BarbaraSupervisor` (Android/Chaquopy e contrato `BarbaraHostContractV1`); nenhuma migração de motor foi feita.
- Integração existente, preservada: `GeminiService` e tratamento de indisponibilidade; `RuleRetrievalEngine` e `RuleProcedureCompiler` para RAG; `MasterRulesArbiter`/`MasterResponseValidator` para autoridade de regras; `OfficialBookRuntimeGateway`, `CompiledContentRuntimeGateway` e SQLite para efeitos autorizados.
- Pasta oficial D&D: [Google Drive D&D5e](https://drive.google.com/drive/folders/13wB-KfYCe2k5xGmb-z2D09KRDJtSyfHu). Verificar edição e disponibilidade por arquivo; não publicar PDFs privados nem artefatos que os reconstituam.
- Pacote novo: `HybridXmmBatchResolutionEngine` e operação `hybrid_batch_preview` no `OfficialBookRuntimeGateway`; até 24 propostas num lote, com XMM 2025, SHA pinado, ponteiro da fonte, IDs exclusivos e 8 operações mecânicas existentes (damage/save, traits, recarga e uso de magias). Não grava banco nem rola dados automaticamente.
- Ponte host: `CompiledPlayerActionDispatcher` recebe `mechanic_family=xmm_batch_preview` sob canal de ação do jogador; resultados continuam exigindo aplicação pelo host. Não dá autoridade mecânica ao Gemini ou ao RAG.
- Testes criados: dados explícitos para Cold Breath e Legendary Resistance, casos inválidos de edição, fonte, duplicação, tentativa de mutação e integração do despachante do jogador. **Criar teste não equivale a executá-lo**; conferir BRELGA do HEAD exato antes de afirmar PASS.
- Autopiloto Codespace: a política exige testes locais e CI verde antes da integração. Enquanto o runner estiver na fila, trabalhos podem ser preparados isoladamente, mas não publicados. O monitor confere se um commit automático de handoff alterou **somente** `docs/HANDOFF_DND_ATUAL.md` antes de aceitar o CI do código-pai. Não confundir essa equivalência com execução nova de testes no commit documental.
- Escopo da entrega: ETAPA 03 ainda aberta; 2/15 etapas homologadas, Android físico não homologado, nenhum APK novo por este pacote.

## Histórico

Este documento foi criado com a infraestrutura de automação. Não foi usado como prova de conclusão de qualquer etapa. As edições automáticas ficam estritamente na seção abaixo; preservar todo o restante.

<!-- DND_AUTO_BEGIN -->
## Último pacote verificado automaticamente

- Fonte: antigravity/dnd-normal-fechamento-gates @ `dff6d953b01c8e78f0d6ec1a21924a6df9f6da73`.
- GitHub CI: https://github.com/bra83/braseirobuild/actions/runs/38611619591
- Regressão BRELGA: **success** (XMLs na branch de evidências QA quando publicados).
- TypeScript: **success**; NOT_APPLICABLE se não existe package.json no D&D.
- APK deste workflow: **não compilado**.
- Etapa 03 XMM: **aberta**. Não converter testes PASS em certificação integral.
- Próxima ação: se FAIL, corrigir o teste/causa exata no SHA; se PASS, implementar próximo efeito XMM source-bound.

<!-- DND_AUTO_END -->
