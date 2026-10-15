# XMM — consolidação isolada dos três lotes de dano/salvaguarda

Data: 2026-10-15
Branch: `integration/dnd-xmm-core-3-lotes-20261015`
Base imutável: `automation/dnd-xmm-megalote-20261015`
Fonte pinada XMM: `8c026b807fac21862a309379a0c3228a0683198b`

## Proveniência

Os 18 arquivos foram incorporados **sem substituir os originais** dos seguintes commits:
- Lote 01: `444fae04dfbb9be2d22f835124e0baddc15c5f2e` (7 arquivos)
- Lote 02: `fd97f77f81c24fba6b0ebd17ef1d99023ea3ab6b` (7 arquivos)
- Lote 03: `6e0a1fcec1733656c0dadba6f5f4c020073e14f4` (4 arquivos)

O diff contra a base contém somente arquivos novos sob `dnd-normal/run247-mod005/`.
Não toca MainActivity, AppDb, Backup21, Mystara, aventuras, PR #39 ou PR #40.
Não foi realizado merge na branch canônica. O checkout da branch só pode servir de
base para revisão e testes; o código não foi ligado ao fluxo real de combate.

## Evidência fornecida pelo usuário no Codespace

- **01**: 784 asserções Java aprovadas, catálogo de 12 contratos no núcleo independente;
  os 12 requerem integração de combate/SQLite, ainda sem persistência.
- **02**: 455 verificações Java aprovadas, 2 testes Python aprovados.
  Nenhuma das 12 ações foi certificada: o executor genérico não registra ações
  e ainda requer fonte integral, riders e integração.
- **03**: 65 asserções Java aprovadas; o ledger marca 4/12 núcleos
  independentes implementados, 8/12 pendentes e 12/12 sem integração.

Total observável: **1304 asserções/verificações Java + 2 testes Python** aprovados
em três execuções distintas. Isto é uma soma de verificações, não 1304
testes Robolectric independentes.

Esta branch de consolidação **não executou ainda** regressão Gradle/Robolectric,
testes Android físico, fluxo persistente, Backup21, BRELGA nem APK.
A fonte pinada foi usada na verificação histórica de 01; a prova de 02
explicitamente diz `pinned_source_verified=false`, e 03 precisa concluir
8 núcleos. Não inflar a cobertura.

## Próximas gates obrigatórias

1. Reexecutar os testes focais sobre **a branch consolidada**, não só nos
   commits individuais; conferir colisões de classes/catálogo/recursos.
2. Conferir fonte pinada e ponteiros integrais de 02 e 03.
3. Resolver os oito contratos restantes de 03 e os riders de 02; só contar
   como completos os que tiverem contexto e prova individual.
4. Integrar pela API pública de combate (somente após PR #39 estar verde):
   estados persistidos, reservas e replay, dano/condições, durações, backup e
   reabertura de SQLite com transações atômicas.
5. Comparar com PR #40 mais recente e eliminar sobreposição de executores.
6. Executar BRELGA sem regressões e homologação Android real; sem APK antes
   dos gates exigidos.

**Status: material preservado e consolidado para revisão, implementação
inteiramente jogável AINDA NÃO homologada.**
