# Kingmaker R36 — Codespace para testes sem APK

## Abrir Codespace
No repositório `bra83/braseirobuild`, selecione a branch
`pf2-kingmaker-r36-official-rule-fixes-20261011`.
Abra **Code → Codespaces → Create codespace on this branch**.
O `.devcontainer/devcontainer.json` prepara Java 21, Python 3.12, pytest e GitHub CLI.
O Codespace precisa ser ligado pelo proprietário: o conector GitHub disponível
nesta sessão permite commits, mas não inicializa máquinas Codespaces.

## Comandos no terminal do Codespace
```bash
bash ci/pf2-kingmaker-r36/codespace_fast_gates.sh fast
bash ci/pf2-kingmaker-r36/codespace_fast_gates.sh full
```
O modo `fast` confere a fonte R35 por SHA256, aplica a mesma cadeia de
patches R36 do BRELGA, executa catálogo de 73 estruturas, semântica Java,
testes direcionados e auditorias dos 11 capítulos/40 partes. O modo
`full` acrescenta todos os testes PF2. Ambos terminam em falha
se algum gate falhar, **sem compilar APK**.
A fonte é extraída em `/tmp`, o ZIP verificado é cacheado no home, e
relatórios são guardados em `.git/pf2-r36-codespace-evidence/`.
Não colocamos livros ou chaves de API em commits.

## Jobs automáticos
Cada `git push` da branch R36 dispara automaticamente
`.github/workflows/pf2-r36-brelga-independent.yml` no runner BRELGA.
Verifica regras, capítulos, campanha, semântica Java e compilação
de tipos Android. **Não executa assembleDebug, emulador ou publicação.**
A execução dos jobs não depende de o Codespace permanecer ligado.

No fechamento, escolha **Actions → PF2 Kingmaker R36 BRELGA Independent Build
And Book Gates → Run workflow**, branch R36, opção
`compile_apk=true`. Somente essa execução manual compila o APK,
testa no emulador e pode publicar pré-release de **TESTE** após aprovação.
A R35 e outros VTTs permanecem intocados.

Testes estruturais e de nomes não homologam integralmente os 40 segmentos
narrativos, os resultados de companheiros nem a persistência Android.

### Atualizar corretamente a branch do Codespace

Se `git pull --ff-only` disser `Cannot fast-forward to multiple branches`,
use a referência explícita, sem reconfigurar outras branches:

```bash
git fetch origin pf2-kingmaker-r36-official-rule-fixes-20261011
git pull --ff-only origin pf2-kingmaker-r36-official-rule-fixes-20261011
bash ci/pf2-kingmaker-r36/codespace_fast_gates.sh fast
```

A biblioteca JSON auxiliar de testes é corrigida apenas na cópia temporária R36:
a expressão ternária antiga promovia inteiros `Long` a `Double`, transformando
IDs `100` em `100.0`. A comparação de participantes agora exige `long`
exato e rejeita identificadores ilegíveis. A fonte R35 e o arquivo de
inspeção original são preservados.
