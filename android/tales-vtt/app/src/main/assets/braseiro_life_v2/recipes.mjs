/** Executable recipes. Bind fact IDs to verified host events; no LLM string matching. */
const fact=(key,value=true)=>({ref:`fact:${key}`,op:'eq',value});
const entity=(key,field,op,value)=>({ref:`entity:${key}:${field}`,op,value});
const set=(key,value=true,visibility='public',actorIds=[])=>({type:'set_fact',id:key,value,visibility,actorIds});
const status=(id,value)=>({type:'set_entity',id,field:'status',value});
const offer=(id,text,actors,choices,visibility='private')=>({type:'offer',id,text,actorIds:actors,visibility,choices});
const rule=(id,when,effects,extra={})=>({id,version:'2.0.0',when,effects,agency:'world',once:true,...extra});
export function createRecipes({actorId='hero',npcId='innkeeper',locationId='village'}={}) {
 const actors=[actorId];
 const entities=[
 ['home','home','Um lugar para voltar'],['promise','promise','A promessa ao aprendiz'],['npc-project','npc_project','A oficina do ferreiro'],
 ['bond','relationship','Confiança e receio'],['tradition','tradition','Um brinde antes da partida'],['keepsake','heirloom','O casaco remendado'],
 ['reputation','reputation','Libertadores da vila'],['expedition','opportunity','Uma expedição parte amanhã'],['road','world_change','A estrada reaberta'],
 ['recognition','recognition','Uma canção sobre os heróis'],['cookbook','personal_project','Escrever um livro de receitas'],['legacy','succession','Um aprendiz recebe o legado'],
 ['warning','foreshadowing','Sinais de paralisação'],['food','resource','Estoque de alimentos'],['supply','problem','Abastecimento interrompido'],
 ['countermeasure','adaptation','O rival prepara uma resposta'],['clue','information','Um esconderijo ocupado ontem'],['escalation','causal_chain','Da escassez ao contrabando'],
 ['bell','routine','O sino toca todas as manhãs'],['rival','adversary','Um rival com recursos limitados'],['recovery','life_arc','Uma oportunidade de apoio']
 ].map(([id,kind,title])=>({id,kind,title,visibility:'public',locationId,actorIds:actors,status:'active',...(kind==='resource'?{quantity:10,dailyDelta:-1}:{}),...(kind==='information'?{validUntil:1}:{}),...(kind==='opportunity'?{dueDay:1}:{}),...(kind==='tradition'?{eventTag:'departure-toast',threshold:3,progress:0,visibility:'private'}:{}),...(kind==='relationship'?{actorIds:[actorId,npcId],dimensions:{trust:2,fear:1}}:{}),...(kind==='heirloom'?{ownerId:actorId,provenance:[]}:{}),...(kind==='life_arc'?{visibility:'private'}:{})}));
 const rules=[
 rule('home-return',[fact('returned-home')],[offer('home-scene','Ao voltar, você encontra a casa marcada pelo tempo. O que deseja fazer?',actors,{rest:[set('rest-requested',true,'private',actors)],visit:[set('visit-requested',true,'private',actors)]})],{agency:'player_choice',locationId}),
 rule('promise-due',[fact('promise-made'),fact('promise-fulfilled',false),fact('promise-due')],[offer('promise-reminder','O aprendiz pergunta quando terão a aula prometida.',actors,{teach:[set('lesson-requested',true,'private',actors)],postpone:[set('lesson-postponed',true,'private',actors)]})],{agency:'player_choice'}),
 rule('npc-workshop',[fact('smith-funded'),fact('smith-has-tools')],[status('npc-project','resolved'),set('workshop-open')]),
 rule('individual-bond',[fact('protected-innkeeper')],[set(`trust-${npcId}-${actorId}`,3,'private',[npcId,actorId])]),
 // Tradition recognition occurs only after three verified repetition events in the engine.
 rule('heirloom-return',[fact('keepsake-recovered')],[offer('keepsake-memory','O remendo ainda está no casaco recuperado.',actors,{keep:[set('keepsake-kept',true,'private',actors)],give:[]})],{agency:'player_choice'}),
 rule('conflicting-reputation',[fact('village-liberated')],[set('villagers-grateful'),set('former-rulers-hostile',true,'gm_only')]),
 rule('expedition-departs',[entity('expedition','status','eq','expired')],[set('expedition-left'),set('expedition-leader','another-adventurer')]),
 rule('road-economy',[fact('road-safe'),fact('trade-resumed')],[set('market-open'),status('road','resolved')]),
 rule('recognition-earned',[fact('village-liberated'),fact('bard-knows-feat')],[set('song-performed'),status('recognition','resolved')]),
 rule('cooking-project',[fact('recipe-tested'),fact('ingredients-recorded')],[status('cookbook','resolved'),set('cookbook-finished',true,'private',actors)]),
 rule('succession-offer',[fact('retirement-requested')],[offer('retirement','Deseja encerrar sua atuação como aventureiro e assumir uma função no mundo?',actors,{confirm:[set('retirement-confirmed',true,'private',actors)],continue:[]})],{agency:'player_choice'}),
 rule('strike-warning',[fact('wages-unpaid')],[set('workers-meeting'),status('warning','resolved')]),
 rule('strike-after-warning',[fact('workers-meeting'),fact('negotiations-failed')],[set('strike-active')]),
 rule('scarcity',[entity('food','quantity','lte',3),fact('supply-restored',false)],[set('scarcity-active'),set('food-price-factor',2)],{narrative:false}),
 // Any host-verified solution may set supply-restored; no prescribed quest action.
 rule('supply-solved',[fact('supply-restored')],[status('supply','resolved'),set('scarcity-active',false),set('food-price-factor',1),{type:'set_entity',id:'food',field:'quantity',value:20}],{narrative:false}),
 rule('rival-adapts',[fact('rival-knows-negotiation-pattern'),fact('rival-can-afford-decoy')],[set('rival-decoy-prepared',true,'gm_only'),status('countermeasure','resolved')]),
 rule('clue-stale',[entity('clue','status','eq','expired')],[set('hideout-occupancy','unknown'),set('departure-traces-available')]),
 rule('scarcity-smuggling',[fact('scarcity-active'),fact('smuggler-has-route'),fact('supply-restored',false)],[set('smuggling-active'),status('escalation','active')]),
 rule('prevention',[fact('supply-restored')],[set('smuggling-active',false),status('escalation','resolved')],{priority:10,narrative:false}),
 rule('bell-absence',[fact('bell-expected'),fact('bellringer-missing')],[set('bell-silent'),set('bell-absence-clue')]),
 rule('rival-retreats',[fact('pursuit-cost-unaffordable')],[set('rival-pursuing',false),status('rival','dormant')]),
 rule('support-opportunity',[fact('distressing-event'),fact('trusted-support-present')],[offer('support','Uma pessoa de confiança se dispõe a conversar, sem pressioná-lo.',actors,{accept:[set('support-accepted',true,'private',actors)],decline:[]})],{agency:'player_choice'})
 ];
 const keys=[...new Set(rules.flatMap(r=>r.when.filter(c=>c.ref.startsWith('fact:')&&c.value===true).map(c=>c.ref.slice(5))))];
 const bindings=keys.map(key=>({id:`bind-${key}`,tag:key,effects:[{...set(key),locationId}]}));
 return {entities,rules:rules.map(r=>({...r,locationId})),bindings};
}
export function installRecipes(store,transact,options,hostOptions={verifiedByHost:true}) {
 let s=store;const pack=createRecipes(options);
 for(const entity of pack.entities)s=transact(s,{id:`install-${entity.id}`,type:'register',entity},hostOptions);
 for(const binding of pack.bindings)s=transact(s,{id:`install-${binding.id}`,type:'binding',binding},hostOptions);
 for(const rule of pack.rules)s=transact(s,{id:`install-${rule.id}`,type:'rule',rule},hostOptions);
 return s;
}
