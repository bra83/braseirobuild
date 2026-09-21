/* BRASEIRO R54 P28 — estado persistente de Sob um Sol Negro.
   Dados resumidos do material do usuário; não substitui regras EotE/AoR. */
(function(root,factory){
  const api=factory();
  if(typeof module==='object'&&module.exports) module.exports=api;
  root.BraseiroBlackSunEngine=api;
})(typeof globalThis!=='undefined'?globalThis:this,function(){
  'use strict';
  const STORAGE_KEY='braseiro.starwars.adventure.under-black-sun.v2';
  const VERSION=2;
  const investigations=['umbra-club','spyder','zelcomm-tower'];
  const nodes={
    'hot-pursuit':{episode:'ep1',episodeTitle:'Episódio I · Sem trabalhos fáceis',title:'Perseguição intensa',visible:'Fuga pelas vias aéreas de Coruscant após o roubo de dados do Sol Negro.',gm:'Conduza somente a perseguição atual. O grupo foge de agentes do Sol Negro; a cena deve terminar em uma decisão do jogador.',checks:['Pilotagem (Planetária) · dificuldade Média','Percepção · dificuldade Média','Furtividade · Fácil ou Média conforme abordagem'],entities:['capangas do Sol Negro','Polícia do Submundo de Coruscant']},
    'chop-talk':{episode:'ep1',episodeTitle:'Episódio I · Sem trabalhos fáceis',title:'Conversa com Chopper',visible:'Contato com Cho’pa’ailor (Chopper), ligado aos Pykes, para interpretar os dados roubados.',gm:'Chopper pode orientar a investigação e apresentar as três linhas de apuração. Não revele conclusões de locais ainda não visitados.',checks:[],entities:['Cho’pa’ailor (Chopper)','Sindicato Pyke']},
    'umbra-club':{episode:'ep2',episodeTitle:'Episódio II · Cidade de agulhas e conflitos',title:'Umbra Club',visible:'Investigação social no Umbra Club.',gm:'Use apenas as informações conquistáveis no Umbra Club. Preserve alternativas sociais e investigativas; não antecipe o esconderijo final.',checks:['Enganação · Média','Percepção · Difícil + 1 Contratempo','Sobrevivência · Média + 1 Contratempo'],entities:['Omacala','Speng','seguranças do clube']},
    'spyder':{episode:'ep2',episodeTitle:'Episódio II · Cidade de agulhas e conflitos',title:'The Spyder',visible:'Investigação social e de jogo no The Spyder.',gm:'A pista de Setor 943 pode ser conquistada aqui. Não revele a planta da NovaCORE antes de o grupo seguir a pista.',checks:['Charme · Média','Negociação · Média','Vida nas Ruas · Difícil','Charme ou Enganação · Difícil + 1 Contratempo'],entities:['Pillaat e Nerro','Korsin Fenn','Verannis','capangas do Sol Negro']},
    'zelcomm-tower':{episode:'ep2',episodeTitle:'Episódio II · Cidade de agulhas e conflitos',title:'Zelcomm Tower',visible:'Infiltração e investigação na Zelcomm Tower.',gm:'Trate a Zelcomm como frente ligada ao contrabando. Revele apenas o que as ações e testes alcançarem.',checks:[],entities:['Coruum Sa’dia','Zelcomm Enforcers']},
    'first-impressions':{episode:'ep2',episodeTitle:'Episódio II · Cidade de agulhas e conflitos',title:'Primeiras impressões',visible:'Encontro opcional de pressão durante a investigação.',gm:'Este encontro é opcional e não substitui o confronto final. Kaa’to pode pressionar o grupo e se retirar conforme a ficção.',checks:[],entities:['Kaa’to Leeachos']},
    'sector-943':{episode:'ep3',episodeTitle:'Episódio III · Fim de jogo',title:'Setor 943 — The Works',visible:'A pista leva o grupo ao Setor 943, na região industrial de Coruscant.',gm:'Conduza a aproximação à instalação NovaCORE. Não antecipe perigos de áreas ainda não exploradas.',checks:[],entities:[]},
    'novacore':{episode:'ep3',episodeTitle:'Episódio III · Fim de jogo',title:'Fábrica NovaCORE',visible:'Exploração da planta de processamento de kelerium NovaCORE.',gm:'Use a estrutura A–G do material: plataforma, escritórios, processamento, entulho, seção abandonada, esconderijo e hangar. Resolva cada perigo somente quando alcançado.',checks:['Resiliência · Média quando o calor extremo for relevante','Mecânica/Percepção/Cool conforme as áreas descritas no material'],entities:['trabalhadores/segurança plausíveis da instalação']},
    'showdown':{episode:'ep3',episodeTitle:'Episódio III · Fim de jogo',title:'Confronto com Kaa’to',visible:'Confronto final com Kaa’to Leeachos e resolução da carga dos Pykes.',gm:'Resolva captura/derrota e a decisão sobre a especiaria. Consequências de Obrigação devem ser arbitradas pelas regras; não aplique números inventados.',checks:[],entities:['Kaa’to Leeachos']}
  };
  function cleanList(v){return [...new Set((Array.isArray(v)?v:[]).map(String).filter(Boolean))];}
  function fresh(){return {version:VERSION,adventureId:'under-a-black-sun',started:false,episode:'ep1',currentNode:'hot-pursuit',completedNodes:[],visitedNodes:[],availableNodes:[],clues:[],flags:{},outcome:null,completed:false,updatedAt:new Date().toISOString()};}
  function normalize(input){
    const base=fresh(),v=input&&typeof input==='object'?input:{};
    const s={...base,...v,version:VERSION,adventureId:'under-a-black-sun'};
    s.completedNodes=cleanList(v.completedNodes);s.visitedNodes=cleanList(v.visitedNodes);s.availableNodes=cleanList(v.availableNodes);s.clues=cleanList(v.clues);s.flags=v.flags&&typeof v.flags==='object'?{...v.flags}:{};
    if(s.currentNode!==null&&!nodes[s.currentNode]) s.currentNode='hot-pursuit';
    if(s.completed){s.currentNode=null;s.availableNodes=[]}
    return s;
  }
  function read(storage){try{const raw=storage&&storage.getItem?storage.getItem(STORAGE_KEY):null;return raw?normalize(JSON.parse(raw)):fresh()}catch{return fresh()}}
  function write(storage,state){const s=normalize({...state,updatedAt:new Date().toISOString()});if(storage&&storage.setItem)storage.setItem(STORAGE_KEY,JSON.stringify(s));return s}
  function start(storage){let s=read(storage);if(!s.started){s.started=true;s.currentNode='hot-pursuit';s.episode='ep1'}return write(storage,s)}
  function availableEpisode2(s){
    const out=investigations.filter(id=>!s.completedNodes.includes(id));
    if(s.completedNodes.some(id=>investigations.includes(id))&&!s.completedNodes.includes('first-impressions')) out.push('first-impressions');
    if(s.completedNodes.includes('spyder')&&!s.completedNodes.includes('sector-943')) out.push('sector-943');
    return cleanList(out);
  }
  function select(storage,nodeId){
    let s=read(storage);const id=String(nodeId||'');
    if(!s.started||s.completed)throw new Error('A aventura não está disponível para seleção.');
    if(!s.availableNodes.includes(id))throw new Error('Esta etapa ainda não foi desbloqueada.');
    s.currentNode=id;s.visitedNodes=cleanList([...s.visitedNodes,id]);s.availableNodes=s.availableNodes.filter(x=>x!==id);
    return write(storage,s);
  }
  function completeCurrent(storage){
    let s=read(storage);const id=s.currentNode;
    if(!s.started||!id||!nodes[id])throw new Error('Nenhuma etapa atual para concluir.');
    s.completedNodes=cleanList([...s.completedNodes,id]);s.visitedNodes=cleanList([...s.visitedNodes,id]);
    if(id==='hot-pursuit'){s.currentNode='chop-talk';s.episode='ep1';}
    else if(id==='chop-talk'){s.currentNode=null;s.episode='ep2';s.availableNodes=availableEpisode2(s);}
    else if(investigations.includes(id)||id==='first-impressions'){
      if(id==='spyder')s.clues=cleanList([...s.clues,'sector-943']);
      s.currentNode=null;s.episode='ep2';s.availableNodes=availableEpisode2(s);
    }
    else if(id==='sector-943'){s.currentNode='novacore';s.episode='ep3';s.availableNodes=[];}
    else if(id==='novacore'){s.currentNode='showdown';s.episode='ep3';s.availableNodes=[];}
    else if(id==='showdown'){s.currentNode=null;s.episode='ep3';s.completed=true;s.availableNodes=[];s.flags.outcomePending=true;}
    return write(storage,s);
  }
  function setOutcome(storage,outcome){let s=read(storage);if(!s.completed)throw new Error('O desfecho só pode ser registrado após o confronto final.');s.outcome=String(outcome||'').trim()||null;s.flags.outcomePending=!s.outcome;return write(storage,s)}
  function reset(storage){const s=fresh();if(storage&&storage.setItem)storage.setItem(STORAGE_KEY,JSON.stringify(s));return s}
  function currentContext(state){const s=normalize(state);const node=s.currentNode?nodes[s.currentNode]:null;return node?{id:s.currentNode,...node,completedNodes:[...s.completedNodes],clues:[...s.clues]}:null}
  function status(state){const s=normalize(state);const node=currentContext(s);return {started:s.started,completed:s.completed,episode:s.episode,currentNode:s.currentNode,currentTitle:node?.title||null,episodeTitle:node?.episodeTitle||null,availableNodes:s.availableNodes.map(id=>({id,title:nodes[id]?.title||id,episode:nodes[id]?.episode||null})),completedNodes:[...s.completedNodes],clues:[...s.clues],outcome:s.outcome};}
  return {STORAGE_KEY,VERSION,nodes,fresh,normalize,read,write,start,select,completeCurrent,setOutcome,reset,currentContext,status};
});
