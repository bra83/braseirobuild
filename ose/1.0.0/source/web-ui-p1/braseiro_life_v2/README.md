# Braseiro — Motor de Vida V2.2

Implementação executável das ideias discutidas em 20/10/2026, ampliando o núcleo V1 recebido. O arquivo `src/legacy-engine.mjs` foi preservado byte a byte e seus 20 testes continuam passando. O novo módulo não muda as regras mecânicas dos jogos.

**Este pacote atualiza o motor isolado enviado. Não instala alterações nos VTTs, não modifica GitHub e não contém APK.**

## Atualização V2.2

Foram corrigidos o cooldown que consultava a primeira ocorrência e a apresentação de propostas sem revalidação. O motor agora oferece automação ligada ao jogo, arcos biográficos persistentes e pesos por personagem. Cada perfil tem 100 situações distintas com cinco formas de cena, mantendo as 500 entradas temáticas.

Configure o provedor de matrizes e a automação uma vez; descansos, viagens, visitas, eventos e passagem do tempo passam a acionar o processamento sem sorteios manuais repetidos. Há chance explícita de nenhum acontecimento. Veja `docs/CORRECOES_V22.md` para código e critérios de integração. Execute `npm run demo:auto` para o fluxo com três reaberturas reais.

## Executar

Node.js 20 ou superior, sem instalar dependências:

```sh
npm test
npm run demo
```

A demonstração executa sete dias de consumo de alimentos, produz escassez e aumento de preços, reconhece uma solução por transporte fluvial, restaura o abastecimento, apresenta uma escolha ao voltar para casa e reabre o save três vezes.

## O que faz o sistema saber usar as ideias

Há três partes executáveis:

1. `recipes.mjs`: entidades, condições, efeitos e bindings de eventos. As receitas cobrem as vinte ideias propostas e acrescentam uma oportunidade de apoio/recuperação.
2. `life-engine.mjs`: processa eventos confirmados, passagem de dias, pré-condições, consequências, escolhas, conhecimento e seleção de contexto. O texto livre de uma resposta da IA nunca confirma um fato por conta própria.
3. `runtime.mjs`: serializa comandos e só atualiza seu estado após a gravação persistente ter sucesso. O adaptador IndexedDB verifica revisão e grava na mesma transação.

Eventos com tags reconhecidas geram fatos automaticamente. Por exemplo, um evento confirmado `returned-home` alimenta a condição de retorno ao lar. O motor então cria uma oferta de descanso ou visita; não escolhe pelo personagem.

## Uso real pelo host

```js
import {
  createLegacyStore, createLifeStore, installRecipes, transact,
  LifeRuntime, openIndexedDB
} from './src/index.mjs';

// Em um jogo existente, use o legado real em vez de criar outro.
const legacy = createLegacyStore({system:'dnd5e', worldId:'minha-campanha'});
let initial = createLifeStore(legacy, {day:0, seed:1234});
// Receitas de demonstração: adapte IDs e conteúdo para a campanha.
initial = installRecipes(initial, transact, {
  actorId:'garrick', npcId:'kote', locationId:'pousada'
});
const repository = await openIndexedDB();
const runtime = await LifeRuntime.open(repository, 'dnd5e:minha-campanha:principal', initial);

// Execute a partir do código confiável que confirmou a chegada na geografia do jogo.
await runtime.dispatch({
  id:'arrival-001', type:'event',
  event:{id:'arrival-001', tag:'returned-home',
    summary:'Garrick voltou à pousada.', visibility:'private',
    actorIds:['garrick'], locationId:'pousada'}
});
await runtime.dispatch({id:'scene-001',type:'evaluate',locationId:'pousada'});
const context = runtime.scene({actorIds:['garrick'],locationId:'pousada'});
// Passe somente o contexto adequado ao Mestre; apresente context.offers na UI.
// Quando uma lembrança foi efetivamente usada na narração:
await runtime.dispatch({id:'used-001',type:'consume_context',entityIds:context.selectedEntityIds});
// Uma escolha de oferta deve vir de entrada real do jogador:
const offer = context.offers[0];
await runtime.dispatch({id:'choice-001',type:'choose',offerId:offer.id,
  actorId:'garrick',choice:'rest'});
```

`rest` registra uma intenção (`rest-requested`). O adaptador do jogo executa o descanso pelas suas regras e confirma seu resultado. O núcleo não concede cura, XP, itens, diagnósticos ou bônus mecânicos.

**Não exponha `runtime.dispatch` ao Gemini, a scripts importados ou a dados não confiáveis.** Ele é a fachada do host confiável. O JSON pode descrever uma aventura; não pode declarar que um evento de jogo foi confirmado.

## Mapa das ideias implementadas

| Ideia | Execução no motor |
|---|---|
| Lar | Evento de retorno gera oferta localizada; o jogador escolhe. |
| Promessas | Confronta promessa existente, cumprimento e prazo; apresenta cobrança. |
| Vida dos NPCs | Projeto conclui com recursos e condições verificadas, inclusive fora de cena. |
| Laços individuais | Relações possuem participantes e dimensões; efeitos são direcionados ao vínculo. |
| Tradições | Conta repetições de eventos confirmados; exige confirmação para virar costume. |
| Objetos com biografia | Mudança de dono acrescenta proveniência e mantém o objeto. |
| Reputações conflitantes | Gratidão e hostilidade coexistem com visibilidades separadas. |
| Oportunidades | Prazo expira; a expedição parte com outra pessoa. |
| Vitórias cotidianas | Segurança da estrada e retomada do comércio permitem abertura da feira. |
| Reconhecimento | Canção exige feito e conhecimento do músico. |
| Projetos pessoais | Trabalho confirmado conclui o livro de receitas. |
| Aposentadoria/herança | Oferta de aposentadoria; comando do jogador preserva herói como NPC e pode criar sucessor. |
| Sinais anteriores | Reunião dos trabalhadores antecede greve; etapas não viram uma cascata instantânea. |
| Recursos | Estoque muda por dia ficcional; escassez modifica preço disponível ao host. |
| Soluções alternativas | Qualquer solução legítima que restaure abastecimento resolve o problema. |
| Adaptação dos adversários | Exige informação sobre o grupo e capacidade de executar a contramedida. |
| Informação datada | Pista expira e deixa de afirmar ocupação atual; permite rastros posteriores. |
| Causalidade interrompível | Abastecimento restaurado impede ou encerra contrabando associado à escassez. |
| Ausências significativas | Rotina esperada e ausência causal produzem pista do sino silencioso. |
| Limites dos inimigos | Custo inviável interrompe perseguição e deixa adversário dormente. |
| Apoio/recuperação | Experiência marcante e apoio disponível geram oportunidade, sem impor comportamento. |

## Contrato de eventos e de conteúdo

`createRecipes()` devolve `entities`, `rules` e `bindings`; a versão JSON está em `examples/receitas-vida.json`. As tags de eventos aceitas estão nesses bindings. São nomes semânticos explícitos: o adaptador de cada VTT deve mapear seus eventos reais a essas tags, com os IDs da campanha.

As receitas são exemplos executáveis de um arco por assunto. Elas não pretendem descobrir sozinhas, por análise de texto, qualquer promessa ou problema possível. Para vários NPCs, promessas e crises, instancie entidades e regras com IDs distintos. O núcleo resolve as mesmas condições genéricas para todas essas instâncias.

- Condições: `eq`, `ne`, `gte`, `lte`, `exists`, `includes`; referências a fatos ou campos permitidos de entidades.
- Efeitos: definir fato, alterar estado/progresso/quantidade de entidade, movimentar recurso, revelar conhecimento, apresentar oferta.
- Campos de ficha, execução de JavaScript e acesso a caminhos/URLs não fazem parte dessa linguagem.
- Bindings de eventos só produzem fatos e não tornam um evento privado público.
- Estado e histórico usam a identidade de sistema, mundo e timeline do legado.
- Regras são instaladas pelo host após revisão; não use instalação de receitas como importador irrestrito de aventuras.

## Tempo e apresentação

O relógio usa dias ficcionais inteiros. Uma chamada avança até 100 anos e processa cada dia sequencialmente. Não há servidor permanente nem dependência do relógio real do aparelho. Viagens e descansos do VTT devem chamar `advance`.

O orçamento limita apresentações e ofertas; mudanças materiais e projetos de NPCs continuam acontecendo quando o orçamento está cheio. Regras têm prioridade, execução única ou cooldown e chance d100 opcional com seed e versão registrados. Uma passagem de avaliação é limitada; uma etapa recém-criada entra na avaliação seguinte. O selecionador considera local, envolvidos, atualização, prazo e uso anterior. `consume_context` confirma uso e impede repetição imediata.

A V2.1 acrescenta as tabelas extensas e o cruzamento com filtro de coerência, ausentes na entrega V2. Cada tabela temática passa de uma base explícita de 100 entradas a 500. Há JSON independente para GURPS investigação, CSI e serial killer; O Um Anel; D&D; Forbidden Lands; PF2e Kingmaker; Star Wars AoR e EotE. Cada JSON tem cinco dimensões completas de 500 entradas, combinando a matriz humana comum com manifestações próprias do sistema.

O perfil salvo impede mistura de sistema/modalidade. O filtro verifica origem confirmada, tempo, relações existentes, capacidades, intensidade, cânone e repetição. O resultado é uma proposta reservada, sem mudança automática de ficha. A especificação atual está em `docs/CORRECOES_V22.md`; `docs/MATRIZES_V21.md` preserva o histórico da versão anterior. Execute `npm run demo:matrices` para uma captura cujas repercussões são consideradas três anos depois, preservando o estado de prisão.

## Conhecimento e segredos

`buildSceneContext` separa público e GM. `buildNpcContext` entrega somente conhecimento transmitido ou testemunhado por aquele NPC. Boatos mantêm sua alegação e não reescrevem o fato. Uma transmissão exige fonte, data de disponibilidade e, quando indicado um transmissor, conhecimento prévio dele.

Fatos locais não aparecem automaticamente em outra região. Segredos `gm_only` não saem no contexto público. O host deve manter verdade privada e apresentação ao jogador em canais distintos, inclusive TTS.

## Persistência e limites da entrega

- `node-persistence.mjs`: arquivo com gravação temporária, fsync, renomeação atômica, lock de escritor e comparação de revisão. Exercitado em testes com três reaberturas reais e conflito de gravação. Um lock deixado por encerramento abrupto deve ser investigado pelo host; não é removido silenciosamente.
- `persistence.mjs`: backup/restauração JSON e adaptador IndexedDB para browser/WebView. O código IndexedDB foi incluído, mas não foi homologado em navegador ou Android nesta execução.
- `runtime.mjs`: fila de comandos, isolamento do estado e publicação somente após persistência. Em conflito entre instâncias, recarregue antes de repetir com IDs novos ou retome o mesmo comando idempotente conforme o caso.
- Não existe sincronização SQLite, integração Bárbara/Gemini, interface Android ou adaptador de regras específico de cada VTT neste pacote. O motor fornece contexto e efeitos narrativos para esses componentes integrarem.
- O V1 permanece com seu contrato original. A V2 envolve o legado V1 em um save maior; use `synchronizeLegacy` quando o motor antigo confirmar novos fatos. Não altere manualmente os dois estados de forma concorrente.
- As rotinas do V1 que geram contexto continuam intactas por compatibilidade; para a nova camada, use os filtros de contexto V2. Os filtros V2 não corrigem chamadas antigas que ainda exponham contexto inadequado.

Veja `TEST_RESULTS.txt` para os resultados desta entrega e `docs/INTEGRACAO_V2.md` para os pontos que devem ser conectados no VTT.
