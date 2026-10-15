# damage_or_save_branch-01 — núcleo executável, integração bloqueada

Base: `f8e48242e62434889190d90b0fece746af7ef0a6`.
Fonte: `5etools-mirror-3/5etools-src`, commit
`8c026b807fac21862a309379a0c3228a0683198b`, arquivo
`data/bestiary/bestiary-xmm.json`, blob `9a4137b508ef37562ed948894a70b1594b57e85c`.
O blob foi obtido pelo conector GitHub e seu hash Git foi recalculado localmente.
A fonte original não foi adicionada nem alterada no repositório.

## Resultado e limites

12 contratos implementados no núcleo Java independente; 0 restantes no núcleo.
12 dependem de integração; 0 ações integradas/executadas no combate Android.
Nenhum dos 12 efeitos completos já estava coberto: o comparativo confirma o
guard `manual_unmodeled_source_effect_required` em todos os registros da base.
Os 59 contratos simples de `Xmm2025SaveDamageEngine` permanecem intactos.

`XmmDamageOrSaveBranch01.Session` executa proposta e commit de um ledger em
memória. O commit consome orçamento/evento e registra efeitos temporários;
ele não altera HP, coordenadas, objetos ou condições do combate. Os resultados
tipados contêm dano após defesas e todos os efeitos fonte para um futuro adapter.
Não são previews sem executor: consumo, janela, replay e duração são executados
no ledger. A instância deve ser única por encontro. Reiniciar a sessão não é um
meio permitido de reconstituir consumo. Não há persistência nesta entrega.

O adapter futuro precisa ligar identidades de atores às fichas, obter contexto
atual autorizado, selecionar **todos** os alvos da área/curse, confirmar geometria,
forma do Animal Lord, defesas completas (incluindo modificadores condicionais),
totais manuais de dano/salvaguarda e acerto manual. Deve fornecer orçamentos
autorizados pelo combate (incluindo unidades de multiataque), abrir a janela
lendária só no fim de outro turno e emitir fronteiras de turno uma única vez.
O núcleo rejeita contexto ausente, alvos duplicados, alcance inválido, dano fora
da fórmula, proposta obsoleta e reutilização de evento. Uma proposta rejeitada
não consome orçamento. Objetos de requisição/resultado são imutáveis.

O adapter também deve aplicar dano e efeitos atomicamente e encaminhar **o próximo
ataque**, inclusive erro, a `nextAttack` quando Deadly Aim estiver ativo. A escolha
manual de empurrão deve confirmar caminho reto para longe do dragão. `TEMPORARY_HP`
retorna uma concessão de 20, nunca soma automaticamente com HP temporários antigos.
`BURNING` inicia o hazard externo; esta família não inventa dano, duração ou rolagens
do hazard. Remoção antecipada de curse/efeitos e persistência pertencem à integração.
Não há API pública autorizada usada aqui para essa aplicação/persistência;
`MainActivity.java` e `AppDb.java` são protegidos pelo escopo deste lote.

## Prova por ponteiro

Todos os ponteiros estão em `contracts.json` e `verification.json`, com hash da
ação **inteira**, identidade exata e contrato observado da classe compilada.
Animal Spirit inclui também o objeto irmão `/entries/1`, indispensável às formas.

| Ponteiro | Contrato executado além do dano | Asserções |
| --- | --- | --- |
| `/monster/5/legendary/0/entries/0` | DEX 17; falha: desvantagem de concentração até fim do próximo turno do alvo; sucesso zero; bloqueio até próximo turno do ator | loop dos 12, durationsAndEconomy |
| `/monster/7/legendary/2/entries/0` | DEX 16; falha: velocidade pela metade até fim do próximo turno do alvo; sucesso zero; bloqueio por turno | loop dos 12 |
| `/monster/13/legendary/1/entries/0` | DEX 19; linha 60 × 10; falha empurra até 30 pés; sucesso metade; bloqueio por turno | loop dos 12, specialContexts |
| `/monster/17/legendary/0/entries/0` | variante Ancient: DEX 21 e 6d10, mesmo efeito temporal | loop dos 12 |
| `/monster/19/legendary/2/entries/0` | variante Ancient: DEX 20 e 8d8, mesma redução temporal | loop dos 12 |
| `/monster/25/legendary/1/entries/0` | variante Ancient: DEX 23, mesma linha/empurrão/metade | loop dos 12 |
| `/monster/27/action/3/entries/0` | DEX 20; metade; efeito em ambos resultados: Forager 20 HP temporários, Hunter vantagem contra presa, Sage desvantagem em ataques e testes | loop dos 12, specialContexts |
| `/monster/36/action/2/entries/0` | DEX 22; cone 60; metade; curse impede reações em ambos resultados até fim do próximo turno da hag | loop dos 12, durationsAndEconomy |
| `/monster/36/bonus/0/entries/0` | bônus; todas as criaturas amaldiçoadas por esta hag em 60 pés; sem salvaguarda | loop dos 12, specialContexts |
| `/monster/48/action/2/entries/0` | acerto manual, +14, alcance 10; componentes force/lightning separados; impede reações até início do próximo turno do Balor | loop dos 12, durationsAndEconomy |
| `/monster/51/bonus/0/entries/0` | bônus arma vantagem no próximo ataque do turno; acerto acrescenta 8d6 poison; erro também consome | aim |
| `/monster/54/action/3/entries/0` | acerto manual, +5, alcance 150; objeto inflamável não vestido/carregado inicia burning | loop dos 12, specialContexts |

Os loops verificam sucesso/falha e NORMAL, RESISTANT, VULNERABLE,
RESISTANT_AND_VULNERABLE e IMMUNE para cada tipo/componente. Efeitos não são
apagados por imunidade ao dano. Áreas têm múltiplos resultados independentes.

## Validação reproduzível

Na raiz do projeto `dnd-normal/run247-mod005`:

```bash
python3 tools/verify_damage_or_save_branch01.py --source /tmp/damage-branch01-source.json --json-out tools/damage_or_save_branch01/verification.json
python3 -m unittest discover -s tools -p 'test_verify_xmm*.py'
GRADLE_USER_HOME=/tmp/damage-branch01-gradle bash gradlew --offline :app:testDebugUnitTest --tests com.braseiro.dnd5e.XmmDamageOrSaveBranch01Test -Pbraseiro.qaLiteAssets=true
```

Resultados observados: compilação `javac --release 8` passou; 784 asserções Java
passaram; verificação de blob, 12 contratos compilados e hashes passou;
68 testes Python existentes passaram. A bridge JUnit chama exatamente os mesmos
cenários Java. Gradle/Robolectric **não executaram**: a distribuição 8.11.1 não
está no cache gravável e o wrapper falhou no download com `SocketException:
Operation not permitted`. A primeira tentativa com cache padrão também encontrou
bloqueio de escrita no diretório de lock. Nenhum APK foi compilado.

Não houve alteração do comparador comum, dos arquivos protegidos, de outros jobs,
de PRs, da branch canônica ou de worktrees externas. Sem merge ou push.

## Bloqueio de commit na worktree

A tentativa de `git add` falhou ao criar
`/workspaces/braseirobuild/.git/worktrees/dnd-xmm-lote-damage-or-save-branch-01/index.lock`:
`Read-only file system`. O ambiente permite leitura dos metadados, sem atualização
do índice ou refs originais. Portanto a branch da worktree permanece na base.
Um commit real foi preparado em metadados temporários, com o mesmo pai e nome de
branch, e exportado como bundle/patch em `/tmp/damage-branch01-delivery/`.
Esses artefatos são transportáveis, mas não equivalem a ter avançado a branch
desta worktree. Nenhuma permissão ou acesso de escrita externo foi contornado.
