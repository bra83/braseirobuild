# Braseiro D&D 5e Normal — evidências QA (armazenamento alternativo)

**Finalidade:** guardar resultados de testes automatizados sem depender da cota de **GitHub Actions artifacts**.

## Como funciona
- A execução do workflow `.github/workflows/dnd-adventure-atomicity-brelga.yml` salva as suítes XML em `runs/<run_id>/attempt-<n>.json` nesta branch.
- Cada JSON contém o SHA do código testado, o URL da execução, totais de testes/falhas/erros/ignorados, SHA-256 de cada XML e o conteúdo completo dos XMLs.
- O envio é realizado diretamente no histórico Git e **não** utiliza `actions/upload-artifact`.
- Cada execução do BRELGA mantém sua própria evidência; uma execução com erro não é tratada como aprovada.

## Último resultado validado antes da migração
- Execução: https://github.com/bra83/braseirobuild/actions/runs/38292059669
- Commit testado: `301243e39cf41d4eeef7fc8262cf33c34835f2c2`
- 51 suítes, **229 testes**, zero falhas, zero erros, zero ignorados.
- Esse resultado é comprovado pelo log da execução, **não** por um arquivo XML armazenado nesta branch.

## Limites de homologação
- Testes Robolectric/Gradle não substituem validação num aparelho Android real.
- As 483 seções editoriais dos dez livros continuam pendentes de comprovação individual.
- Mystara permanece congelado; a branch de evidências não é usada no jogo.
