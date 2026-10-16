import { SKILLS } from '../rules/character.mjs';
import { ADVERSARY_PROFILES } from '../rules/adversaries.mjs';

const ALLOWED_KEYS=new Set(['narration','sceneTitle','sceneLocationId','askPlayer','suggestedActions','suggestedCheck','sourceRuleIds','acquisitionRequest','acquisitionDecision','economyOperations','worldOperations','encounterDirective']);
export const MASTER_NARRATION_MAX_CHARS=12000;

function cleanText(value,label,max) {
  if (value===undefined || value===null) return null;
  if (typeof value!=='string') throw new Error(`${label} deve ser texto.`);
  const text=value.trim();
  if (!text) return null;
  if (text.length>max) throw new Error(`${label} excede o limite de ${max} caracteres.`);
  return text;
}

export function validateMasterProposal(value,{knownRuleIds=[],pendingAcquisition=null}={}) {
  if (!value || typeof value!=='object' || Array.isArray(value)) throw new Error('Proposta do Mestre inválida.');
  for (const key of Object.keys(value)) if (!ALLOWED_KEYS.has(key)) throw new Error(`Campo não autorizado na proposta do Mestre: ${key}`);
  const proposal={
    narration:cleanText(value.narration,'Narração',MASTER_NARRATION_MAX_CHARS),
    sceneTitle:cleanText(value.sceneTitle,'Título da cena',120),
    sceneLocationId:cleanText(value.sceneLocationId,'Local da cena',160),
    askPlayer:cleanText(value.askPlayer,'Pergunta ao jogador',500),
    suggestedActions:[],
    suggestedCheck:null,
    sourceRuleIds:[],
    acquisitionRequest:null,
    acquisitionDecision:null,
    economyOperations:[],
    worldOperations:[],
    encounterDirective:null
  };
  if (value.suggestedActions!==undefined && value.suggestedActions!==null) {
    if (!Array.isArray(value.suggestedActions) || value.suggestedActions.length>6) throw new Error('Ações sugeridas devem conter no máximo 6 opções.');
    proposal.suggestedActions=[...new Set(value.suggestedActions.map((item,index)=>cleanText(item,`Ação sugerida ${index+1}`,180)).filter(Boolean))].slice(0,6);
  }

  if (value.suggestedCheck!==undefined && value.suggestedCheck!==null) {
    const c=value.suggestedCheck;
    if (!c || typeof c!=='object' || Array.isArray(c)) throw new Error('Teste sugerido inválido.');
    const skillKey=String(c.skillKey ?? '');
    if (!(skillKey in SKILLS)) throw new Error('Perícia sugerida pelo Mestre não existe no perfil atual.');
    const number=(key,max)=>{
      const n=Number(c[key] ?? 0);
      if (!Number.isInteger(n) || n<0 || n>max) throw new Error(`Valor inválido em ${key}.`);
      return n;
    };
    proposal.suggestedCheck={
      skillKey,
      difficulty:number('difficulty',5),
      challenge:number('challenge',5),
      boost:number('boost',6),
      setback:number('setback',6),
      reason:cleanText(c.reason,'Motivo do teste',300)
    };
  }

  if (value.encounterDirective!==undefined && value.encounterDirective!==null) {
    const d=value.encounterDirective;
    if(!d||typeof d!=='object'||Array.isArray(d)) throw new Error('Diretriz de encontro inválida.');
    for(const key of Object.keys(d)) if(!['sourceHookId','adversaries','playerInitiativeSkill','npcInitiativeSkill'].includes(key)) throw new Error(`Campo não autorizado na diretriz de encontro: ${key}`);
    if(!Array.isArray(d.adversaries)||d.adversaries.length<1||d.adversaries.length>6) throw new Error('A diretriz de encontro exige de 1 a 6 grupos de adversários.');
    const ranges=new Set(['Interação','Curto','Médio','Longo','Extremo']);
    const adversaries=d.adversaries.map((raw,index)=>{
      if(!raw||typeof raw!=='object'||Array.isArray(raw)) throw new Error(`Grupo de adversários ${index+1} inválido.`);
      for(const key of Object.keys(raw)) if(!['profileKey','count','range','name','worldNpcId','factionId'].includes(key)) throw new Error(`Campo não autorizado no adversário do encontro: ${key}`);
      const profileKey=String(raw.profileKey??''); if(!(profileKey in ADVERSARY_PROFILES)) throw new Error(`Perfil de adversário não permitido: ${profileKey}`);
      const count=Number(raw.count??1); if(!Number.isInteger(count)||count<1||count>20) throw new Error('Quantidade de adversários inválida.');
      const range=String(raw.range??'Médio'); if(!ranges.has(range)) throw new Error('Alcance inicial do adversário inválido.');
      const name=cleanText(raw.name,'Nome do adversário',120);
      const worldNpcId=cleanText(raw.worldNpcId,'ID do PNPC persistente',160);
      const factionId=cleanText(raw.factionId,'ID da facção do adversário',160);
      return {profileKey,count,range,name,worldNpcId,factionId};
    });
    const initiative=(value,label)=>{ const v=String(value??'alerta'); if(!['alerta','frialdad'].includes(v)) throw new Error(`${label} deve usar Alerta ou Frieza.`); return v; };
    proposal.encounterDirective={
      sourceHookId:cleanText(d.sourceHookId,'ID da consequência causal',160),
      adversaries,
      playerInitiativeSkill:initiative(d.playerInitiativeSkill,'Iniciativa do jogador'),
      npcInitiativeSkill:initiative(d.npcInitiativeSkill,'Iniciativa dos PNPCs')
    };
  }

  if (value.acquisitionRequest!==undefined && value.acquisitionRequest!==null) {
    if (pendingAcquisition) throw new Error('Já existe aquisição pendente.');
    const r=value.acquisitionRequest;
    if(!r||typeof r!=='object'||Array.isArray(r)) throw new Error('Pedido de aquisição inválido.');
    for(const key of Object.keys(r)) if(!['kind','itemId'].includes(key)) throw new Error(`Campo não autorizado no pedido de aquisição: ${key}`);
    const kind=String(r.kind??''); if(!['weapon','armor','gear','vehicle'].includes(kind)) throw new Error('Tipo de aquisição inválido.');
    const itemId=cleanText(r.itemId,'ID do item solicitado',160); if(!itemId) throw new Error('Pedido de aquisição exige item do catálogo.');
    proposal.acquisitionRequest={kind,itemId};
  }

  if (value.acquisitionDecision!==undefined && value.acquisitionDecision!==null) {
    const d=value.acquisitionDecision;
    if (!d || typeof d!=='object' || Array.isArray(d)) throw new Error('Decisão de aquisição inválida.');
    for (const key of Object.keys(d)) if (!['itemId','approved','finalPrice','reason'].includes(key)) throw new Error(`Campo não autorizado na decisão de aquisição: ${key}`);
    if (!pendingAcquisition) throw new Error('Não há aquisição pendente para o Mestre resolver.');
    const itemId=String(d.itemId ?? '');
    if (itemId!==pendingAcquisition.itemId) throw new Error('A decisão não corresponde ao item pendente.');
    if (typeof d.approved!=='boolean') throw new Error('A decisão de aquisição precisa aprovar ou rejeitar o pedido.');
    let finalPrice=null;
    if (d.approved) {
      finalPrice=Number(d.finalPrice);
      if (!Number.isInteger(finalPrice)||finalPrice<0||finalPrice>100000000) throw new Error('Preço final inválido na decisão de aquisição.');
    }
    proposal.acquisitionDecision={itemId,approved:d.approved,finalPrice,reason:cleanText(d.reason,'Motivo da aquisição',500)};
  }

  if (value.worldOperations!==undefined && value.worldOperations!==null) {
    if (!Array.isArray(value.worldOperations)) throw new Error('Operações de mundo devem ser uma lista.');
    if (value.worldOperations.length>12) throw new Error('Uma proposta do Mestre pode conter no máximo 12 operações de mundo.');
    const schemas={
      create_location:new Set(['type','name','description','knownToPlayer']),
      create_faction:new Set(['type','name','objective','status','resources','knownToPlayer']),
      create_npc:new Set(['type','name','locationId','factionId','goal','relation','status','knownToPlayer','species','sex','gender','origin','career','specialization','occupation','ageYears','appearance']),
      set_npc_visibility:new Set(['type','npcId','knownToPlayer']),
      move_npc:new Set(['type','npcId','locationId']),
      set_npc_faction:new Set(['type','npcId','factionId']),
      set_npc_goal:new Set(['type','npcId','goal']),
      set_npc_relation:new Set(['type','npcId','relation']),
      set_npc_status:new Set(['type','npcId','status']),
      record_npc_fact:new Set(['type','npcId','text','knownToPlayer']),
      set_faction_objective:new Set(['type','factionId','objective']),
      set_faction_status:new Set(['type','factionId','status']),
      add_faction_resource:new Set(['type','factionId','resource']),
      remove_faction_resource:new Set(['type','factionId','resource']),
      set_faction_relation:new Set(['type','factionId','targetFactionId','relation']),
      record_faction_fact:new Set(['type','factionId','text','knownToPlayer']),
      create_faction_plan:new Set(['type','factionId','title','objective','steps','knownToPlayer']),
      set_faction_plan_status:new Set(['type','factionId','planId','status']),
      advance_faction_plan:new Set(['type','factionId','planId']),
      schedule_event:new Set(['type','eventType','dueTick','payload','dedupeKey']),
      resolve_hook:new Set(['type','hookId','resolution','dismiss']),
      close_aftermath:new Set(['type','aftermathId','resolution']),
      create_opportunity:new Set(['type','kind','title','summary','locationId','factionId','npcId','expiresTick','knownToPlayer','tags','sourceHookId']),
      set_opportunity_status:new Set(['type','opportunityId','status','resolution']),
      advance_clock:new Set(['type','clockId','steps']),
      set_market:new Set(['type','locationId','rarityModifier','restrictedAccess','tags','notes'])
    };
    const smallText=(v,label,max=500)=>{
      if(v===undefined||v===null) return null;
      if(typeof v!=='string') throw new Error(`${label} deve ser texto.`);
      const out=v.trim(); if(out.length>max) throw new Error(`${label} excede ${max} caracteres.`); return out||null;
    };
    proposal.worldOperations=value.worldOperations.map((raw,index)=>{
      if(!raw||typeof raw!=='object'||Array.isArray(raw)) throw new Error(`Operação de mundo ${index+1} inválida.`);
      const type=String(raw.type??''); const allowed=schemas[type]; if(!allowed) throw new Error(`Tipo de operação de mundo não autorizado: ${type}`);
      for(const key of Object.keys(raw)) if(!allowed.has(key)) throw new Error(`Campo não autorizado na operação de mundo: ${key}`);
      if(type==='create_location') return {type,name:smallText(raw.name,'Nome do local',120),description:smallText(raw.description,'Descrição do local',1000)??'',knownToPlayer:Boolean(raw.knownToPlayer)};
      if(type==='create_faction') {
        const resources=Array.isArray(raw.resources)?raw.resources.map((v,i)=>smallText(v,`Recurso ${i+1} da facção`,160)).filter(Boolean):[];
        if(resources.length>20) throw new Error('Uma facção pode receber no máximo 20 recursos textuais por operação.');
        return {type,name:smallText(raw.name,'Nome da facção',120),objective:smallText(raw.objective,'Objetivo da facção',500)??'',status:smallText(raw.status,'Estado da facção',80)??'active',resources,knownToPlayer:Boolean(raw.knownToPlayer)};
      }
      if(type==='create_npc'){const age=raw.ageYears===undefined||raw.ageYears===null?null:Number(raw.ageYears);if(age!==null&&(!Number.isInteger(age)||age<0||age>500))throw new Error('Idade do PNPC inválida.');return {type,name:smallText(raw.name,'Nome do PNPC',120),locationId:smallText(raw.locationId,'ID do local',160),factionId:smallText(raw.factionId,'ID da facção',160),goal:smallText(raw.goal,'Objetivo do PNPC',500)??'',relation:smallText(raw.relation,'Relação do PNPC',80)??'neutral',status:smallText(raw.status,'Estado do PNPC',80)??'active',knownToPlayer:Boolean(raw.knownToPlayer),species:smallText(raw.species,'Espécie',80),sex:smallText(raw.sex,'Sexo',40),gender:smallText(raw.gender,'Gênero',40),origin:smallText(raw.origin,'Origem',120),career:smallText(raw.career,'Carreira',100),specialization:smallText(raw.specialization,'Especialização',100),occupation:smallText(raw.occupation,'Ocupação',120),ageYears:age,appearance:smallText(raw.appearance,'Aparência',500)};}
      if(type==='set_npc_visibility') return {type,npcId:smallText(raw.npcId,'ID do PNPC',160),knownToPlayer:Boolean(raw.knownToPlayer)};
      if(type==='move_npc') return {type,npcId:smallText(raw.npcId,'ID do PNPC',160),locationId:smallText(raw.locationId,'ID do local',160)};
      if(type==='set_npc_faction') return {type,npcId:smallText(raw.npcId,'ID do PNPC',160),factionId:smallText(raw.factionId,'ID da facção',160)};
      if(type==='set_npc_goal') return {type,npcId:smallText(raw.npcId,'ID do PNPC',160),goal:smallText(raw.goal,'Objetivo do PNPC',500)??''};
      if(type==='set_npc_relation') return {type,npcId:smallText(raw.npcId,'ID do PNPC',160),relation:smallText(raw.relation,'Relação do PNPC',80)};
      if(type==='set_npc_status') return {type,npcId:smallText(raw.npcId,'ID do PNPC',160),status:smallText(raw.status,'Estado do PNPC',80)};
      if(type==='record_npc_fact') return {type,npcId:smallText(raw.npcId,'ID do PNPC',160),text:smallText(raw.text,'Fato do PNPC',1000),knownToPlayer:Boolean(raw.knownToPlayer)};
      if(type==='set_faction_objective') return {type,factionId:smallText(raw.factionId,'ID da facção',160),objective:smallText(raw.objective,'Objetivo da facção',500)??''};
      if(type==='set_faction_status') return {type,factionId:smallText(raw.factionId,'ID da facção',160),status:smallText(raw.status,'Estado da facção',80)};
      if(type==='add_faction_resource'||type==='remove_faction_resource') return {type,factionId:smallText(raw.factionId,'ID da facção',160),resource:smallText(raw.resource,'Recurso da facção',160)};
      if(type==='set_faction_relation') return {type,factionId:smallText(raw.factionId,'ID da facção',160),targetFactionId:smallText(raw.targetFactionId,'ID da facção-alvo',160),relation:smallText(raw.relation,'Relação entre facções',120)};
      if(type==='record_faction_fact') return {type,factionId:smallText(raw.factionId,'ID da facção',160),text:smallText(raw.text,'Fato da facção',1000),knownToPlayer:Boolean(raw.knownToPlayer)};
      if(type==='create_faction_plan'){ const steps=Array.isArray(raw.steps)?raw.steps.map((step,i)=>{ const src=typeof step==='string'?{text:step}:step; if(!src||typeof src!=='object'||Array.isArray(src)) throw new Error(`Etapa ${i+1} do plano inválida.`); for(const key of Object.keys(src)) if(!['text','locationId','knownToPlayer'].includes(key)) throw new Error(`Campo não autorizado na etapa do plano: ${key}`); return {text:smallText(src.text,`Etapa ${i+1} do plano`,300),locationId:smallText(src.locationId,`Local da etapa ${i+1}`,160),knownToPlayer:Boolean(src.knownToPlayer)}; }):[]; if(steps.length<1||steps.length>20||steps.some(x=>!x.text)) throw new Error('O plano da facção exige de 1 a 20 etapas válidas.'); return {type,factionId:smallText(raw.factionId,'ID da facção',160),title:smallText(raw.title,'Título do plano',160),objective:smallText(raw.objective,'Objetivo do plano',500)??'',steps,knownToPlayer:Boolean(raw.knownToPlayer)}; }
      if(type==='set_faction_plan_status') return {type,factionId:smallText(raw.factionId,'ID da facção',160),planId:smallText(raw.planId,'ID do plano',160),status:smallText(raw.status,'Estado do plano',40)};
      if(type==='advance_faction_plan') return {type,factionId:smallText(raw.factionId,'ID da facção',160),planId:smallText(raw.planId,'ID do plano',160)};
      if(type==='create_opportunity'){
        const expiresTick=raw.expiresTick===undefined||raw.expiresTick===null?null:Number(raw.expiresTick); if(expiresTick!==null&&(!Number.isInteger(expiresTick)||expiresTick<0||expiresTick>1_000_000_000)) throw new Error('Expiração da oportunidade inválida.');
        const tags=Array.isArray(raw.tags)?raw.tags.map((v,i)=>smallText(v,`Tag ${i+1} da oportunidade`,80)).filter(Boolean).slice(0,20):[];
        return {type,kind:smallText(raw.kind,'Tipo da oportunidade',60)??'rumor',title:smallText(raw.title,'Título da oportunidade',180),summary:smallText(raw.summary,'Resumo da oportunidade',1000)??'',locationId:smallText(raw.locationId,'Local da oportunidade',160),factionId:smallText(raw.factionId,'Facção da oportunidade',160),npcId:smallText(raw.npcId,'PNPC da oportunidade',160),expiresTick,knownToPlayer:Boolean(raw.knownToPlayer),tags,sourceHookId:smallText(raw.sourceHookId,'Consequência de origem',160)};
      }
      if(type==='set_opportunity_status') return {type,opportunityId:smallText(raw.opportunityId,'ID da oportunidade',160),status:smallText(raw.status,'Estado da oportunidade',40),resolution:smallText(raw.resolution,'Resolução da oportunidade',1000)??''};
      if(type==='advance_clock'){const steps=Number(raw.steps??1);if(!Number.isInteger(steps)||steps<0||steps>12)throw new Error('Avanço do relógio inválido.');return {type,clockId:smallText(raw.clockId,'ID do relógio',160),steps};}
      if(type==='set_market'){const rarityModifier=Number(raw.rarityModifier??0);if(!Number.isInteger(rarityModifier)||rarityModifier<-5||rarityModifier>5)throw new Error('Modificador de raridade do mercado inválido.');const tags=Array.isArray(raw.tags)?raw.tags.map((v,i)=>smallText(v,`Tag ${i+1} do mercado`,80)).filter(Boolean).slice(0,20):[];return {type,locationId:smallText(raw.locationId,'Local do mercado',160),rarityModifier,restrictedAccess:smallText(raw.restrictedAccess,'Acesso restrito',80)??'conditional',tags,notes:smallText(raw.notes,'Notas do mercado',600)??''};}
      if(type==='resolve_hook') return {type,hookId:smallText(raw.hookId,'ID da consequência',160),resolution:smallText(raw.resolution,'Resolução da consequência',800)??'',dismiss:Boolean(raw.dismiss)};
      if(type==='close_aftermath') return {type,aftermathId:smallText(raw.aftermathId,'ID da consequência de longo prazo',160),resolution:smallText(raw.resolution,'Resolução da consequência de longo prazo',1000)??''};
      const dueTick=Number(raw.dueTick); if(!Number.isInteger(dueTick)||dueTick<0||dueTick>1_000_000_000) throw new Error('Tick de evento de mundo inválido.');
      if(!raw.payload||typeof raw.payload!=='object'||Array.isArray(raw.payload)) throw new Error('Payload do evento de mundo inválido.');
      return {type,eventType:smallText(raw.eventType,'Tipo do evento de mundo',80),dueTick,payload:structuredClone(raw.payload),dedupeKey:smallText(raw.dedupeKey,'Chave de deduplicação',160)};
    });
    for(const op of proposal.worldOperations){
      if(['create_location','create_npc','create_faction'].includes(op.type) && !op.name) throw new Error('Criação no mundo exige nome.');
      if(['move_npc','set_npc_faction','set_npc_goal','set_npc_relation','set_npc_status','set_npc_visibility','record_npc_fact'].includes(op.type) && !op.npcId) throw new Error('Operação em PNPC exige ID existente.');
      if(['set_faction_objective','set_faction_status','add_faction_resource','remove_faction_resource','set_faction_relation','record_faction_fact','create_faction_plan','set_faction_plan_status','advance_faction_plan'].includes(op.type) && !op.factionId) throw new Error('Operação em facção exige ID existente.');
      if(op.type==='set_npc_relation'&&!op.relation) throw new Error('Relação do PNPC é obrigatória.');
      if(op.type==='set_npc_status'&&!op.status) throw new Error('Estado do PNPC é obrigatório.');
      if(op.type==='record_npc_fact'&&!op.text) throw new Error('Fato do PNPC é obrigatório.');
      if(op.type==='set_faction_status'&&!op.status) throw new Error('Estado da facção é obrigatório.');
      if(['add_faction_resource','remove_faction_resource'].includes(op.type)&&!op.resource) throw new Error('Recurso da facção é obrigatório.');
      if(op.type==='set_faction_relation'&&(!op.targetFactionId||!op.relation)) throw new Error('Relação entre facções exige facção-alvo e descrição.');
      if(op.type==='record_faction_fact'&&!op.text) throw new Error('Fato da facção é obrigatório.');
      if(op.type==='create_faction_plan'&&(!op.title||!op.steps?.length)) throw new Error('Plano da facção exige título e etapas.');
      if(op.type==='set_faction_plan_status'&&(!op.planId||!['planned','active','completed','aborted'].includes(op.status))) throw new Error('Alteração de plano da facção inválida.');
      if(op.type==='advance_faction_plan'&&!op.planId) throw new Error('Avanço de plano exige ID do plano.');
      if(op.type==='create_opportunity'&&!op.title) throw new Error('Oportunidade exige título.');
      if(op.type==='set_opportunity_status'&&(!op.opportunityId||!['available','accepted','resolved','expired','dismissed'].includes(op.status))) throw new Error('Alteração de oportunidade inválida.');
      if(op.type==='advance_clock'&&!op.clockId) throw new Error('Avanço de relógio exige ID existente.');
      if(op.type==='set_market'&&!op.locationId) throw new Error('Mercado exige local existente.');
      if(op.type==='schedule_event'&&!op.eventType) throw new Error('Tipo do evento agendado é obrigatório.');
      if(op.type==='resolve_hook'&&!op.hookId) throw new Error('Resolução de consequência exige ID existente.');
      if(op.type==='close_aftermath'&&!op.aftermathId) throw new Error('Encerramento de consequência de longo prazo exige ID existente.');
    }
  }

  if (value.economyOperations!==undefined && value.economyOperations!==null) {
    if(!Array.isArray(value.economyOperations)||value.economyOperations.length>6) throw new Error('Operações econômicas devem conter no máximo 6 itens.');
    const allowed={grant_asset:new Set(['type','kind','itemId','method','note']),lose_asset:new Set(['type','assetId','reason','method']),damage_asset:new Set(['type','assetId','amount','reason']),repair_asset:new Set(['type','assetId','amount','cost','reason']),sell_asset:new Set(['type','assetId','price','legalSale','reason'])};
    proposal.economyOperations=value.economyOperations.map((raw,index)=>{
      if(!raw||typeof raw!=='object'||Array.isArray(raw)) throw new Error(`Operação econômica ${index+1} inválida.`);
      const type=String(raw.type??''), keys=allowed[type]; if(!keys) throw new Error(`Tipo de operação econômica não autorizado: ${type}`);
      for(const key of Object.keys(raw)) if(!keys.has(key)) throw new Error(`Campo não autorizado na operação econômica: ${key}`);
      if(type==='grant_asset'){const kind=String(raw.kind??'');if(!['weapon','armor','gear','vehicle'].includes(kind))throw new Error('Tipo de recompensa/saque inválido.');const itemId=cleanText(raw.itemId,'Item econômico',160);if(!itemId)throw new Error('Recompensa/saque exige item catalogado.');const method=String(raw.method??'reward');if(!['reward','loot','stolen','gift'].includes(method))throw new Error('Método de aquisição narrativa inválido.');return {type,kind,itemId,method,note:cleanText(raw.note,'Nota econômica',500)};}
      const assetId=cleanText(raw.assetId,'ID do ativo possuído',200); if(!assetId) throw new Error('Operação econômica exige ativo possuído.');
      if(type==='lose_asset') return {type,assetId,reason:cleanText(raw.reason,'Motivo da perda',500),method:cleanText(raw.method,'Método da perda',80)??'loss'};
      if(type==='damage_asset'){const amount=Number(raw.amount??1);if(!Number.isInteger(amount)||amount<1||amount>100000)throw new Error('Dano econômico inválido.');return {type,assetId,amount,reason:cleanText(raw.reason,'Motivo do dano',500)};}
      if(type==='repair_asset'){const amount=Number(raw.amount??1),cost=Number(raw.cost??0);if(!Number.isInteger(amount)||amount<1||amount>100000||!Number.isInteger(cost)||cost<0||cost>100000000)throw new Error('Reparo econômico inválido.');return {type,assetId,amount,cost,reason:cleanText(raw.reason,'Motivo do reparo',500)};}
      const price=Number(raw.price);if(!Number.isInteger(price)||price<0||price>100000000)throw new Error('Preço de venda inválido.');if(raw.legalSale!==undefined&&typeof raw.legalSale!=='boolean')throw new Error('Venda legal exige confirmação booleana.');return {type,assetId,price,legalSale:raw.legalSale===true,reason:cleanText(raw.reason,'Motivo da venda',500)};
    });
  }

  if (value.sourceRuleIds!==undefined) {
    if (!Array.isArray(value.sourceRuleIds)) throw new Error('Referências de regra devem ser uma lista.');
    const known=new Set(knownRuleIds);
    proposal.sourceRuleIds=[...new Set(value.sourceRuleIds.map(String))];
    for (const id of proposal.sourceRuleIds) if (!known.has(id)) throw new Error(`Regra citada não estava no contexto recuperado: ${id}`);
  }
  if (proposal.acquisitionRequest && proposal.acquisitionDecision) throw new Error('Pedido e decisão de aquisição não podem ocorrer na mesma proposta.');
  if (proposal.acquisitionDecision && proposal.sourceRuleIds.length===0) throw new Error('Decisão de aquisição exige referência a regra recuperada.');
  if (proposal.economyOperations.some(op=>['damage_asset','repair_asset','sell_asset'].includes(op.type)) && proposal.sourceRuleIds.length===0) throw new Error('Dano, reparo ou venda exigem referência mecânica recuperada.');
  if (!proposal.narration && !proposal.askPlayer && proposal.suggestedActions.length===0 && !proposal.suggestedCheck && !proposal.sceneTitle && !proposal.sceneLocationId && !proposal.acquisitionRequest && !proposal.acquisitionDecision && proposal.economyOperations.length===0 && !proposal.encounterDirective && proposal.worldOperations.length===0) throw new Error('Proposta do Mestre está vazia.');
  return proposal;
}

export function applyValidatedNarrativeProposal(state,proposal) {
  const next=structuredClone(state);
  if (proposal.sceneTitle) next.scene.title=proposal.sceneTitle;
  if (proposal.narration) next.scene.narration=proposal.narration;
  next.master.pendingQuestion=proposal.askPlayer ?? null;
  next.master.suggestedActions=[...(proposal.suggestedActions ?? [])];
  next.master.suggestedCheck=proposal.suggestedCheck ? structuredClone(proposal.suggestedCheck) : null;
  next.master.lastProposalRuleIds=[...(proposal.sourceRuleIds ?? [])];
  if(proposal.encounterDirective) next.master.pendingEncounter=structuredClone(proposal.encounterDirective);
  next.updatedAt=new Date().toISOString();
  return next;
}
