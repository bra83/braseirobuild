> Histórico da V2.1. Para o estado atual, consulte `CORRECOES_V22.md` e `TEST_RESULTS.txt`.

# Matrizes de vida V2.1 — expansão e coerência

## Correção da entrega anterior

O pacote V2 tinha receitas, condições e d100 por regra, mas não tinha a tabela extensa de cruzamentos. Não havia, portanto, uma contagem anterior que pudesse ser honestamente multiplicada. Esta entrega cria uma base explícita e auditável de 100 entradas por tabela e a amplia para 500.

## Tabelas próprias dos sistemas solicitados

| Pacote | Arquivo independente | Entradas temáticas base → ampliadas |
|---|---|---|
| GURPS investigação geral | `data/matrices/packs/gurps-investigacao.json` | 100 → 500 |
| GURPS CSI / perícia | `data/matrices/packs/gurps-csi.json` | 100 → 500 |
| GURPS serial killer | `data/matrices/packs/gurps-serial-killer.json` | 100 → 500 |
| O Um Anel | `data/matrices/packs/o-um-anel.json` | 100 → 500 |
| D&D | `data/matrices/packs/dnd5e.json` | 100 → 500 |
| Forbidden Lands | `data/matrices/packs/forbidden-lands.json` | 100 → 500 |
| PF2e Kingmaker | `data/matrices/packs/pf2e-kingmaker.json` | 100 → 500 |
| Star Wars Age of Rebellion | `data/matrices/packs/star-wars-aor.json` | 100 → 500 |
| Star Wars Edge of the Empire | `data/matrices/packs/star-wars-eote.json` | 100 → 500 |

GURPS tem três subtabelas de especialidade. AoR e EotE são perfis distintos; Dever e Obrigação aparecem em seus contextos próprios, sem alteração automática de pontuação. Pacotes de Cyberpunk ou GURPS genérico não foram acrescentados a esta solicitação.

Cada arquivo independente possui as cinco dimensões completas, com 500 entradas por dimensão: causa, manifestação, domínio da vida, momento e resposta possível. As partes humanas comuns são compartilhadas na autoria, sem duplicar a contagem como se fossem conteúdo novo para cada sistema.

## O que significa o aumento de cinco vezes

- Cada tabela temática parte de 20 situações com cinco operações de cena: 100 entradas-base.
- Acrescenta vinte operações de cena por situação, chegando a 25 operações: 500 entradas temáticas com textos distintos.
- As novas operações incluem origem do relato, testemunha, objeto, registro, comparação de versões, privacidade, apoio, custo, alternativa, adiamento, tarefa compartilhada, recusa, passo reversível, revisão, limites, agradecimento, notícia, retorno, transmissão e encerramento. Elas têm pré-condições próprias.
- As tabelas humanas comuns têm 100 entradas-base cada e cinco variantes temporais por entrada: imediato, dias seguintes, primeiras semanas, meses e anos. São variantes de elegibilidade, não 500 histórias independentes.
- Há 14 tabelas de autoria, 1.400 entradas-base e 7.000 entradas expandidas. Nove tabelas temáticas contêm 4.500 entradas. As cinco tabelas comuns contêm 2.500 entradas, incluindo uma tabela universal de manifestações disponível como referência.
- Os arquivos independentes repetem as dimensões humanas necessárias à instalação. Essas cópias não aumentam a contagem de 7.000.
- Não se afirma que o produto cartesiano dessas listas corresponda a histórias coerentes ou únicas. O filtro reduz o conjunto antes do sorteio e valida novamente a tupla escolhida.

O gerador editorial está em `scripts/build-matrices.py`. As bases de 100 e as ampliações de 500 permanecem em JSON para comparação. A geração é por modelos de conteúdo explícitos; não é uma coleção de milhares de narrativas escritas individualmente.

## Cruzamento que o motor executa

1. O host registra um acontecimento confirmado com `lifeCauseId` que o classifica na tabela de causas. Uma frase da IA não confirma captura, morte ou vitória.
2. O perfil salvo determina sistema e modalidade. O pacote carregado precisa corresponder a ele.
3. O filtro de causas mantém apenas a classificação do acontecimento real e sua janela temporal. O sorteio não troca captura por morte ou inocência demonstrada.
4. A manifestação deve combinar com essa causa, com o sistema, com os limites e com as capacidades reais do contexto.
5. Domínio, momento e resposta são escolhidos entre entradas compatíveis com a manifestação selecionada.
6. Uma segunda verificação examina a combinação completa. Se não houver combinação válida, o resultado é `none`, sem fabricar evento alternativo.
7. O resultado é armazenado como proposta reservada ao Mestre. Não muda a ficha nem o cânone.
8. O host decide quando usar a proposta segundo ritmo, localização e política narrativa. Essa seleção pode ser automática; não exige pedir ao jogador autorização para toda lembrança ou notícia.
9. Quando a situação envolver decisão voluntária, o jogador escolhe o que fará. A matriz não confirma a ação por ele.

## Filtro de coerência

O código valida sistema/modalidade, vínculo do personagem com a origem, tempo ficcional, intensidade, domínios permitidos, condições necessárias, capacidades, fatos exigidos, fatos proibidos, afirmações apoiadas pelo cânone e cooldown por situação. Capacidades devem vir do estado real do host.

Exemplos de capacidades: existe uma pessoa relacionada, há uma testemunha disponível, existe registro acessível, há duas fontes, existe contato familiar, existe vínculo íntimo, existe um lar, existe apoio disponível, a decisão é reversível, há ocasião de descanso.

Cartas atribuídas ao criminoso exigem que uma correspondência suspeita já exista no mundo. A tabela não confirma sua autenticidade nem liberta um criminoso preso. Entradas forenses que dependem de resultados exigem evidência disponível. Um rumor permanece rumor. Uma relação familiar ou íntima não é criada para viabilizar uma entrada.

A coerência textual aberta continua dependendo da autoria e do Mestre: o filtro verifica campos estruturados, não compreende perfeitamente prosa arbitrária. Novas entradas devem declarar condições e capacidades em vez de esconder exigências somente na descrição.

## Dados e auditoria

Cada eixo usa dois d100 independentes, interpretados como um número em base 100. Não são somados. O sorteio filtra primeiro e usa rejeição limitada para distribuir os resultados entre entradas elegíveis com seus pesos. O limite é de 16 tentativas por eixo; falha retorna ausência de proposta.

A auditoria guarda versão do pacote, seed inicial/final, dados por eixo, contagem e motivos de rejeição, entradas escolhidas, origem, tempo e contexto empregado. Ela fica no save, fora do contexto público e do texto TTS. A seed usa um PRNG determinístico, não aleatoriedade criptográfica.

## Como conectar ao motor

```js
import {loadMatrixBundle} from './src/node-matrices.mjs';
// Em WebView, carregar o JSON independente da pasta packs como asset local.
const bundle = await loadMatrixBundle('gurps-serial-killer');

await runtime.dispatch({id:'matrix-profile',type:'matrix_configure',
  packId:'gurps-serial-killer'});

// O código confiável de jogo confirma e classifica a captura.
await runtime.dispatch({id:'capture-001',type:'event',event:{
  id:'capture-001',summary:'Marrone capturou o suspeito.',
  lifeCauseId:'cause-justice-1',actorIds:['marrone'],visibility:'private'
}});

// Em uma cena posterior pertinente:
await runtime.dispatch({id:'draw-001',type:'matrix_draw',bundle,
  context:{originEventId:'capture-001',characterId:'marrone',
    mode:'serial-killer',capabilities:['record_available','related_person'],
    maxIntensity:2}
});

// O host pode selecionar uma proposta GM segundo sua política narrativa.
const privateScene=runtime.scene({actorIds:['marrone'],audience:'gm'});
const suggestion=privateScene.offers.find(o=>o.id.startsWith('draw-001:matrix-'));
if(suggestion) await runtime.dispatch({id:'present-001',type:'matrix_present',
  offerId:suggestion.id});

// Apenas o contexto público apropriado pode seguir para UI/TTS.
const playerScene=runtime.scene({actorIds:['marrone']});
```

Para consultar as propostas sem apresentá-las, o Mestre pode usar `matrixHistory` internamente. `matrix_present` cria uma oportunidade, não um resultado consumado. `engage` ou `pass` são entradas de escolha, sem efeitos mecânicos automáticos. A opção temática sugerida está em `matrixHistory[*].result.proposal.playerOption`; a narração deve oferecê-la como possibilidade e aceitar respostas livres do jogador.

O perfil salvo impede trocar silenciosamente AoR por EotE. Mudar de perfil é uma operação explícita de configuração do host. `loadMatrixBundle` valida hashes dos assets no Node; no app, mantenha os JSON como assets versionados e confiáveis. Não aceite matrizes executáveis ou comandos vindos de resposta da IA.

## Estado de integração

O núcleo e os testes foram atualizados. Os nove JSON completos acompanham o pacote. Não houve alteração nos repositórios ou nos APKs dos jogos. Conectar eventos reais, calendário, capacidades, contexto do Mestre e persistência Android continua sendo trabalho de integração de cada VTT.
