# damage_or_save_branch-02 — entrega parcial bloqueada

Base recebida: `f8e48242e62434889190d90b0fece746af7ef0a6`.
Fonte exigida: `5etools-mirror-3/5etools-src@8c026b807fac21862a309379a0c3228a0683198b`,
`data/bestiary/bestiary-xmm.json`, blob esperado `9a4137b508ef37562ed948894a70b1594b57e85c`.

## Resultado e limites

**0/12 ações completas certificadas; 12 restantes; as mesmas 12 dependem de integração.**
Nenhuma ação foi liberada no combate. Nenhuma ação foi contada como já coberta:
os 12 registros compilados preservam `manual_unmodeled_source_effect_required=true`.
O código novo é um executor independente funcional, sem registro de ações XMM,
sem vínculo com UI, SQLite ou persistência. Não satisfaz a homologação completa do lote.

A fonte não existe nesta worktree/cache e não pertence ao Git deste projeto.
O download por curl falhou com `Could not resolve host: raw.githubusercontent.com`;
a tentativa pelo acesso web retornou `Cache miss`.
Consequentemente não inferimos riders ou políticas de sucesso a partir de nomes,
fórmulas ou similaridade textual. `comparison.json` prova somente a correspondência
exata entre os contratos recebidos em `job.json` e os registros compilados atuais,
pelo monstro, categoria, posição da ação, nome e cada campo mecânico fornecido.
Não é uma comparação independente com a fonte pinada.

Há lacunas concretas no resumo: o verificador existente separa Earth Burst em
ataque `2d8 + 6` contundente e salvaguarda secundária `3d6` trovão em área de 10 pés.
Darkmantle possui contexto de vantagem/tamanho e término ao separar-se. Elemental
Claw não fornece tipo de dano no lote. Fell Word não fornece política de sucesso,
alcance, elegibilidade nem custo lendário no lote. Os demais efeitos adicionais
precisam da entrada integral pinada. O teste de cura do Bearded Devil cobre **Beard**,
não Infernal Glaive; não foi reutilizado como prova desta ação.

## Código executável

`XmmDamageOrSaveBranch02` usa contratos tipados para componentes, gatilho hit/save,
política de sucesso explícita, habilidade/CD, quantidade máxima de alvos e economia.
Recebe faces individuais de dados manuais e defesa explícita por tipo/alvo.
Rejeita fórmulas incompletas, dados fora do intervalo, tipos duplicados (exige
agrupar antes das defesas), defesa ausente, alvos duplicados/desconhecidos e orçamento
insuficiente. O cálculo reutiliza `AreaSaveDamageRules`; não duplica suas regras.

`Session.propose` valida todos os alvos sem consumir recursos; `Session.execute`
aplica atomicamente HP e orçamento ao estado independente. Propostas são imutáveis,
vinculadas à sessão/revisão, consumíveis uma vez e rejeitadas após outra execução.
Falha de ataque também consome a ação quando executada. Estados externos não são
alterados. Não há rolagem automática, críticos automáticos ou persistência.

É somente dano instantâneo: condições, duração, ticks, seleção espacial, jogadas de
ataque, críticos, imunidades condicionais e outros traços não estão representados.
Antes de construir um contrato para combate, um adaptador deve verificar a fonte
integral, elegibilidade/alcance, economia e todos os riders. Não se deve usar este
núcleo isolado para aplicar apenas parte do dano de uma ação incompleta.

## Verificação executada

A partir da raiz da worktree:

```sh
python3 dnd-normal/run247-mod005/tools/test_xmm_damage_or_save_branch02.py
python3 -m unittest discover -s dnd-normal/run247-mod005/tools -p 'test_verify_xmm*.py'
python3 dnd-normal/run247-mod005/tools/verify_xmm_damage_or_save_branch02.py --out dnd-normal/run247-mod005/docs/editorial-evidence/xmm-damage-or-save-branch-02/comparison.json
git diff --check
```

Resultados: 2 testes novos passaram, incluindo compilação `javac --release 8` e
455 verificações executadas em Java; 68 testes Python existentes passaram;
12 comparações exatas sem divergência. Os testes incluem sucesso/falha de save,
miss, todos os 13 tipos e 5 estados de defesa, dano composto, arredondamento,
múltiplos alvos, snapshots, proposta sem consumo, orçamento, replay, revisão e
sessão estrangeira. Nenhum desses testes certifica os riders das 12 ações.

Gradle/Robolectric **não executados**. Tentativa com `GRADLE_USER_HOME=/tmp/xmm-branch02-gradle`
e `bash .../gradlew --offline -p ... testDebugUnitTest --tests com.braseiro.dnd5e.XmmSaveDamageGatewayTest`
parou antes do build: Gradle 8.11.1 ausente, download com `SocketException: Operation not permitted`.
Nenhum APK foi compilado. Comparador comum e arquivos protegidos não foram alterados.

## Próxima dependência

Disponibilizar a fonte pinada validada pelo blob esperado, completar cada contrato
com seus riders, e então implementar adaptadores somente pelas APIs públicas
permitidas, mantendo bloqueio explícito onde exigir edição de UI/SQLite protegidos.
Os 12 ponteiros e os registros locais completos estão em `comparison.json`.

## Bloqueio do commit

`git add` e `git commit` foram tentados na branch desta worktree e falharam:
`Unable to create '/workspaces/braseirobuild/.git/worktrees/dnd-xmm-lote-damage-or-save-branch-02/index.lock': Read-only file system`.
Os metadados Git estão fora da raiz gravável autorizada. Nenhum commit foi criado;
HEAD permanece `f8e48242e62434889190d90b0fece746af7ef0a6`. Não houve merge ou push.
Os arquivos novos permanecem na worktree para revisão e commit quando o índice
Git desta worktree estiver gravável. Não foi criada outra base ou repositório.
