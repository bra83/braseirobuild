# Motor de Vida V2.2 — correções completas dos seis pontos revisados

Esta versão corrige o núcleo isolado entregue. Nenhum aplicativo, repositório ou APK foi alterado.

## 1. Repetição corrigida

O cooldown procura agora a ocorrência mais recente da situação para o personagem, considerando também os registros que não estão em ordem cronológica. Um registro antigo não permite ignorar um uso recente. Personagens distintos não herdam o bloqueio uns dos outros. O mesmo assunto também recebe proteção contra repetição a partir de outra origem.

## 2. Propostas revalidadas quando usadas

Antes de mostrar uma proposta, o motor reconstrói o contexto atual e verifica fatos exigidos/proibidos, afirmações, tempo, perfil de sistema, modalidade, capacidades, intensidade, personagem vivo e estado do arco. A proposta não usa o próprio sorteio como bloqueio de cooldown, mas continua respeitando usos posteriores da situação.

Uma proposta inválida é ocultada do contexto e expira quando o host tenta apresentá-la. Uma escolha já apresentada também é novamente validada antes de ser aceita. O mundo não precisa permanecer congelado enquanto uma proposta aguarda uso.

## 3. Variedade efetiva do conteúdo

Cada um dos nove perfis mantém 500 entradas temáticas, agora distribuídas por **100 situações distintas com cinco formas de cena cada**. A V2.1 tinha 20 situações com 25 formas de apresentação. Foram escritos 80 novos núcleos de situação por perfil, totalizando 720 acréscimos editoriais entre os nove perfis.

Os novos núcleos estão em `scripts/extra-situations.txt`; o gerador verifica a contagem e a ausência de duplicações literais. As bases de 100 entradas continuam disponíveis para comparação. A contagem de entradas nas tabelas de autoria permanece em 7.000; cópias dos JSON completos não são contadas como novo conteúdo.

As situações incluem vida doméstica e afetiva, trabalho, fontes de informação, provisões, comunidade, recuperação, sucessão, projetos pessoais e consequências públicas. Mantêm o tema de cada sistema. Perícia exige evidência disponível; uma carta suspeita não prova autoria; nenhuma tabela concede bônus, diagnósticos ou alterações de Dever/Obrigação.

## 4. Acionamento automático

`LifeRuntime` pode receber um provedor de matrizes na abertura. Após uma configuração única por campanha, eventos confirmados, descanso, viagem, visita, passagem do tempo, avaliação e preparação de cena acionam a procura de oportunidades.

A automação usa o perfil salvo, os personagens configurados, acontecimentos confirmados, pertinência de local, histórico de uso e um intervalo por personagem. Não pede um comando de sorteio para cada ação. O número de tentativas por gatilho é limitado e o orçamento narrativo continua válido.

Por padrão, a chance de haver oportunidade é 35%, o intervalo entre tentativas para um personagem é sete dias ficcionais e há no máximo uma tentativa por gatilho. Esses parâmetros podem ser configurados pelo host. Proteções verificadas podem diminuir a frequência; a chance também pode ser explicitamente configurada por personagem. O resultado `ordinary_life` registra que nenhum acontecimento foi escolhido.

O pipeline completo — ação, tempo, sorteio, arco, apresentação e agendamento ficcional — é salvo numa única gravação com controle de revisão. Falhas de asset ou persistência não publicam uma atualização pela metade. Reexecutar o mesmo comando não sorteia novamente.

A automação fica desabilitada em saves antigos até o host configurá-la, evitando alteração silenciosa de campanhas já existentes. Habilitá-la exige perfil salvo e, quando houver processamento, um provedor de assets. Ausência de provedor é um erro explícito.

## 5. Arcos que evoluem

Um resultado coerente cria um arco associado a personagem, origem, sistema e situação. Estados possíveis: `candidate`, `active`, `dormant`, `resurfaced`, `resolving`, `resolved`, `rejected`, `expired`.

- A proposta nasce candidata e só fica ativa quando apresentada.
- O jogador pode explorar a oportunidade ou deixá-la passar. Isso altera o acompanhamento narrativo, sem confirmar uma ação concreta ou efeito mecânico.
- Uma oportunidade recusada pode permanecer latente; não volta antes do prazo próprio do arco.
- Apoio ou melhora confirmados fazem o arco evoluir para resolução gradual. A passagem de tempo sozinha não concede cura ou encerramento.
- Um revés pode reativar uma tensão quando houver acontecimento que o sustente.
- Encerramento e rejeição preservam a história. Um arco encerrado só reabre com novo gatilho confirmado.
- O mesmo acontecimento não faz o arco progredir duas vezes ao ser reprocessado.
- O contexto reservado do Mestre inclui o estado e as transições recentes dos arcos pertinentes. Candidatos e prazos ocultos não aparecem automaticamente para o jogador.

Resultados verificáveis chegam por `lifeArcSignals` em eventos do host ou por `arc_progress` associado a um evento existente. São sinais de continuidade narrativa, não substitutos das regras médicas, condições oficiais ou ficha do jogo.

## 6. Pesos por personagem

As chances de seleção usam preferências configuradas, histórico de acontecimentos, vínculos registrados, apoio confirmado e respostas anteriores a oportunidades. A seleção registra o contexto e os pesos totais efetivos para reprodução.

- `domainWeights` e `causeWeights`: multiplicadores de 0 a 5; zero exclui o assunto.
- `protections`: apoio, estabilidade e outros contrapesos que o host efetivamente conhece. Favorecem oportunidades positivas e reduzem pressão narrativa, sem diagnosticar o personagem.
- Relações com confiança registrada e apoio confirmado nos arcos alimentam o apoio disponível.
- Experiência vivida favorece contextos de trabalho, conhecimento e legado. Isso não concede perícias.
- Escolhas recentes de explorar ou passar por um assunto influenciam o ritmo dessas oportunidades, com limites e janela de 180 dias. Não definem personalidade nem decidem ações futuras.
- `maxIntensity` e `capabilities` continuam sendo condições reais de elegibilidade. Limites novos também se aplicam a propostas já sorteadas.

## Configuração mínima de integração

```js
import {LifeRuntime} from './src/index.mjs';
import {loadMatrixBundle} from './src/node-matrices.mjs';

// Em WebView, o provedor deve carregar os JSON locais de data/matrices/packs/.
const runtime=await LifeRuntime.open(repository,saveKey,initialState,{
  matrixProvider:loadMatrixBundle
});
await runtime.dispatch({id:'setup-profile',type:'matrix_configure',packId:'dnd5e'});
await runtime.dispatch({id:'setup-character',type:'matrix_character_profile',profile:{
  characterId:'garrick',domainWeights:{home:2,legacy:2},
  capabilities:['record_available'],protections:{support:1},maxIntensity:2
}});
await runtime.dispatch({id:'setup-auto',type:'matrix_automation_configure',options:{
  enabled:true,characterIds:['garrick'],cooldownDays:7,chancePercent:35,autoPresent:true
}});

// O host confirma a ação pelo jogo e informa a categoria correta.
await runtime.dispatch({id:'captured-01',type:'event',event:{
  id:'captured-01',summary:'Garrick capturou o suspeito.',
  lifeCauseId:'cause-justice-1',actorIds:['garrick'],visibility:'private',locationId:'village'
}});

// O descanso normal aciona o pipeline sem chamar matrix_draw.
await runtime.dispatch({id:'rest-01',type:'rest',toDay:12,
  locationId:'village',actorIds:['garrick']});
const scene=runtime.scene({actorIds:['garrick'],locationId:'village'});
```

O host continua responsável por classificar eventos reais, manter capacidades e perfis atualizados, usar o calendário da campanha e encaminhar o contexto correto ao Mestre e à UI/TTS. O motor não deduz fatos confirmados de uma frase livre da IA.

## Evolução por acontecimento

```js
await runtime.dispatch({id:'help-01',type:'event',event:{
  id:'help-01',summary:'Ajuda prática recebida.',actorIds:['garrick'],visibility:'private',
  lifeArcSignals:[{arcId:'ID_REAL_DO_ARCO',outcome:'support_received'}]
}});
```

Outcomes aceitos: `support_received`, `practical_improvement`, `setback`, `resolved`, `new_trigger`, `rejected`, `no_change`. A decisão do jogador e a confirmação pelo host precisam ocorrer antes do sinal correspondente.

## Verificação

A suíte cobre as duas regressões reproduzidas, variedade, eventos automáticos, intervalos, nenhuma consequência, orçamento, falhas atômicas, idempotência, estados e evolução dos arcos, preferências, vínculos, histórico e persistência. A demonstração `npm run demo:auto` executa descanso → oportunidade automática → apoio confirmado → três reaberturas reais.

Os resultados de Node não constituem homologação de WebView, Android físico ou integração aos VTTs. A persistência IndexedDB e os adaptadores específicos continuam sujeitos aos testes de cada aplicativo.

Propostas da V2.1 são mantidas no histórico, mas exigem novo sorteio antes de apresentação. Entradas que pressupõem promessas, reparações, vínculos ou limitações também possuem capacidades explícitas de elegibilidade. O teste de regressão impede inventar uma reparação prometida a partir de uma captura.
