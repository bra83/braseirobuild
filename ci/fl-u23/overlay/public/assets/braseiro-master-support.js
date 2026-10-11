(() => {
  'use strict';
  if (window.__braseiroMasterSupportInstalled) return;
  window.__braseiroMasterSupportInstalled = true;

  const DB = 'braseiro-db';
  const PANEL_ID = 'braseiro-master-support';
  const MODELS = ['gemini-3.5-flash-lite','gemini-3.6-flash','gemini-3.5-flash','gemini-3.1-flash-lite'];
  const FREE_KEY = 'braseiro-gemini-api-key-free';
  const PAID_KEY = 'braseiro-gemini-api-key-paid';
  const LAST_MODEL = 'braseiro-last-working-text-model';
  const state = { sceneId: '', history: [], busy: false, refreshTimer: 0, renderSig: '' };

  const norm = value => String(value || '').toLocaleLowerCase('pt-BR').normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-z0-9 ]/g,' ').replace(/\s+/g,' ').trim();
  const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const compact = (value, max=700) => String(value ?? '').replace(/\s+/g,' ').trim().slice(0,max);
  const uniq = list => Array.from(new Set((list || []).map(v => String(v || '').trim()).filter(Boolean)));


  const QUESTION_WORDS=/^(?:quem|qual|quais|quando|onde|como|por que|porque|quanto|quantos|quantas|ha |há |tem |existe |essa |esse |isso |aquilo |posso |pode |seria |devo )/;
  const EXPLICIT_SPEECH=/\b(?:pergunto|digo|falo|respondo|grito|sussurro|conto|peço|peco|ordeno|aviso|chamo|cumprimento|questiono|interrogo)\b|^["“']/;
  const EXPLICIT_ACTION=/\b(?:eu |n[oó]s )?(?:abro|entro|saio|ando|caminho|corro|subo|desço|desco|pego|toco|movo|empurro|puxo|ataco|golpeio|disparo|escalo|nado|procuro|examino|investigo|rastreio|esgueiro|escondo|acendo|apago|quebro|forço|forco|sigo|viajo|atravesso|vasculho|revisto|construo|reparo|curo|trato|conjuro|uso|entrego|ofereço|ofereco|compro|vendo|roubo|furto|ameaço|ameaco|intimido|persuado|engano|negoceio|negocio|barganho)\b/;
  const RULE_TERMS=/\b(?:regra|regras|manual|mecanic|teste|rolagem|dados?|pool|atributo|pericia|perícia|talento|magia|armadura|dano|critico|crítico|vontade|willpower|iniciativa|acao rapida|ação rápida|acao lenta|ação lenta|recurso|forçar|forcar a rolagem|empurrar a rolagem)\b/;
  const SPATIAL_TERMS=/\b(?:onde (?:estou|estamos|fica|estava|estavam)|a minha esquerda|à minha esquerda|a minha direita|à minha direita|na frente|atr[aá]s de mim|distancia|distância|perto|longe|quem est[aá] aqui|quem est[aá] presente|qual porta|qual caminho|qual lado|posi[cç][aã]o|sa[ií]da|entrada)\b/;
  const RECAP_TERMS=/\b(?:resume|resuma|recapitule|recapitula|onde paramos|o que aconteceu|o que ja aconteceu|o que já aconteceu|me lembra|lembre-me|qual era mesmo|o que sabemos ate agora|o que sabemos até agora)\b/;
  const PLANNING_TERMS=/\b(?:talvez|penso em|estou pensando|pretendo|planejo|seria melhor|devo |e se eu|e se nos|e se nós|eu poderia|nos poderiamos|nós poderíamos|posso tentar|quero tentar|acho que vou|antes de decidir|estou considerando)\b/;
  const CONTINUE_TERMS=/^(?:continua|continue|continuar|prossiga|prossegue|prossiga a cena|retome|retomar|segue a cena|seguir a cena)[.! ]*$/;

  function isNonAdvancingText(raw){
    const original=String(raw||'').trim(), value=norm(original); if(!value) return false;
    if(CONTINUE_TERMS.test(value)||EXPLICIT_SPEECH.test(value)) return false;
    if(RECAP_TERMS.test(value)) return true;
    if(SPATIAL_TERMS.test(value)&&!EXPLICIT_ACTION.test(value)) return true;
    const questionLike=original.endsWith('?')||QUESTION_WORDS.test(value);
    if((questionLike||/\bcomo funciona\b|\bqual a regra\b|\bprecisa rolar\b/.test(value))&&RULE_TERMS.test(value)) return true;
    if(PLANNING_TERMS.test(value)&&!EXPLICIT_ACTION.test(value)) return true;
    if(questionLike&&!EXPLICIT_ACTION.test(value)) return true;
    return false;
  }

  function openDb(){ return new Promise((resolve,reject)=>{ const r=indexedDB.open(DB); r.onsuccess=()=>resolve(r.result); r.onerror=()=>reject(r.error); }); }
  async function all(store){
    const db=await openDb().catch(()=>null);
    if(!db) return [];
    try {
      if(!db.objectStoreNames.contains(store)) return [];
      return await new Promise((resolve,reject)=>{ const tx=db.transaction(store,'readonly'),r=tx.objectStore(store).getAll(); r.onsuccess=()=>resolve(r.result||[]); r.onerror=()=>reject(r.error); });
    } finally {
      try { db.close(); } catch(_) {}
    }
  }
  async function get(store,key){
    const db=await openDb().catch(()=>null);
    if(!db) return null;
    try {
      if(!db.objectStoreNames.contains(store)) return null;
      return await new Promise((resolve,reject)=>{ const tx=db.transaction(store,'readonly'),r=tx.objectStore(store).get(key); r.onsuccess=()=>resolve(r.result||null); r.onerror=()=>reject(r.error); });
    } finally {
      try { db.close(); } catch(_) {}
    }
  }
  async function count(store){
    const db=await openDb().catch(()=>null);
    if(!db) return 0;
    try {
      if(!db.objectStoreNames.contains(store)) return 0;
      return await new Promise((resolve,reject)=>{ const tx=db.transaction(store,'readonly'),r=tx.objectStore(store).count(); r.onsuccess=()=>resolve(Number(r.result||0)); r.onerror=()=>reject(r.error); });
    } finally {
      try { db.close(); } catch(_) {}
    }
  }

  function latestCampaign(items){ return [...items].sort((a,b)=>(b.updatedAt||0)-(a.updatedAt||0))[0] || null; }
  function latestCanonicalScene(items, campaignId){
    return items.filter(s => s.advancesFiction !== false && (!campaignId || !s.campaignId || s.campaignId === campaignId))
      .sort((a,b)=>(Number(a.sequence||0)-Number(b.sequence||0)) || ((a.createdAt||0)-(b.createdAt||0))).at(-1) || null;
  }

  function skillAlias(label){
    const key=norm(label).replace(/ /g,'');
    const map={might:'Potência',potencia:'Potência',endurance:'Resistência',resistencia:'Resistência',melee:'Luta',luta:'Luta',crafting:'Manufatura',manufatura:'Manufatura',stealth:'Furtividade',furtividade:'Furtividade',sleightofhand:'Prestidigitação',prestidigitacao:'Prestidigitação',move:'Movimentação',movimentacao:'Movimentação',marksmanship:'Pontaria',pontaria:'Pontaria',scouting:'Exploração',exploracao:'Exploração',lore:'Conhecimento',conhecimento:'Conhecimento',survival:'Sobrevivência',sobrevivencia:'Sobrevivência',insight:'Intuição',intuicao:'Intuição',manipulation:'Manipulação',manipulacao:'Manipulação',performance:'Performance',healing:'Cura',cura:'Cura',animalhandling:'Adestramento',adestramento:'Adestramento'};
    return map[key] || String(label || '');
  }

  function localSkillSuggestions(scene, character){
    const text=norm(`${scene?.playerText||''} ${scene?.title||''} ${scene?.narration||''}`);
    const map=[
      [/atac|golpe|espada|machado|adaga|corpo a corpo/,['Força','Luta','combate corpo a corpo']],
      [/arco|besta|dispar|atir|distancia/,['Agilidade','Pontaria','ataques ou tiros à distância']],
      [/furtiv|esgueir|silencio|ocult|sombra/,['Agilidade','Furtividade','mover-se sem ser percebido']],
      [/escal|salt|nadar|fug|persegu|moviment/,['Agilidade','Movimentação','deslocamento difícil ou arriscado']],
      [/pista|exam|observ|vigia|porta|sala|ruina|explor/,['Astúcia','Exploração','notar detalhes e ler o espaço']],
      [/mentir|nervos|intenc|humor|comport|reacao|reação/,['Astúcia','Intuição','avaliar comportamento ou intenção percebida']],
      [/rastre|cac|caç|forrage|orient|selvagem|trilha/,['Astúcia','Sobrevivência','rastrear, orientar-se ou lidar com o ermo']],
      [/runa|historia|história|lenda|antig|magia|ritual/,['Astúcia','Conhecimento','interpretar saber antigo ou arcano']],
      [/repar|fabric|constru|mecanismo|ferrament/,['Força','Manufatura','fabricar, reparar ou compreender construção prática']],
      [/ferimento|curar|tratar|sangr/,['Empatia','Cura','tratar ferimentos quando a regra pedir']],
      [/negoci|convenc|persuad|barganh|amea|intimid|engan/,['Empatia','Manipulação','influenciar alguém quando houver resistência real']],
      [/animal|cavalo|montaria|fera/,['Empatia','Adestramento','lidar com animais']],
    ];
    const found=[];
    if(scene?.masterDecision?.requiresCheck && scene.masterDecision.skill) found.push([scene.masterDecision.attribute||'Atributo', skillAlias(scene.masterDecision.skill), 'teste atualmente indicado pelo Mestre']);
    for(const [re,data] of map){ if(re.test(text)) found.push(data); if(found.length>=4) break; }
    if(found.length<3) found.push(['Astúcia','Exploração','observar melhor o espaço antes de se comprometer']);
    if(found.length<3) found.push(['Astúcia','Intuição','avaliar pessoas presentes sem presumir o que pensam']);
    const skills=character?.skills||{};
    return uniq(found.map(([attr,skill,why])=>`${attr} + ${skill}${Object.keys(skills).length ? ` (${Number(skills[skill] ?? skills[Object.keys(skills).find(k=>skillAlias(k)===skill)] ?? 0)} em perícia)` : ''} — ${why}`)).slice(0,3);
  }

  function localActions(scene){
    const out=uniq(scene?.suggestedActions || []).slice(0,3);
    const text=`${scene?.title||''}. ${scene?.narration||''}`;
    const npc=(scene?.npcDialogue||[]).find(x=>x?.name)?.name;
    const object=text.match(/\b(porta|janela|trilha|ponte|carroça|baú|altar|runa|corpo|pegadas?|torre|poço|mapa|mecanismo|fogueira|barco)\b/i)?.[0];
    const route=text.match(/\b(caminho|trilha|estrada|passagem|portão|ponte|vau|escada|corredor)\b/i)?.[0];
    if(out.length<3 && npc) out.push(`Conversar com ${npc} sobre algo que ele realmente possa saber, sem assumir que dirá a verdade.`);
    if(out.length<3 && object) out.push(`Examinar ${object} pelo que é perceptível antes de decidir se vale tocar, abrir ou atravessar.`);
    if(out.length<3 && route) out.push(`Examinar ${route} pelo terreno visível e decidir se vale avançar, recuar ou buscar cobertura.`);
    if(out.length<3) out.push('Observar o espaço e pedir ao Mestre apenas os detalhes que sua posição permite perceber.');
    if(out.length<3) out.push('Escolher uma abordagem cautelosa que preserve uma rota de retirada caso a situação mude.');
    return uniq(out).slice(0,3);
  }

  const STOP = new Set(['para','como','uma','umas','uns','das','dos','que','com','sem','por','isso','esta','esse','essa','quem','qual','quais','onde','quando','quanto','regra','regras','cena']);
  function terms(value){ return uniq(norm(value).split(' ').filter(t=>t.length>3&&!STOP.has(t))); }
  function ruleMatches(chunks, query, limit=7){
    const ts=terms(query); const phrase=norm(query);
    return chunks.map(chunk=>{ const hay=norm(`${chunk.sourceLabel||''} ${chunk.text||''}`); let score=phrase.length>10&&hay.includes(phrase)?30:0; for(const t of ts){ if(hay.includes(t)) score += 5 + Math.min(4,(hay.split(t).length-1)); } return {chunk,score}; })
      .filter(x=>x.score>0).sort((a,b)=>b.score-a.score).slice(0,limit).map(x=>x.chunk);
  }

  async function snapshot(question=''){
    const chunkPromise = question ? all('rule_chunks') : Promise.resolve([]);
    const [campaigns, scenes, characters, npcs, chunks, ruleChunkCount] = await Promise.all([all('campaign'),all('scenes'),all('characters'),all('npcs'),chunkPromise,count('rule_chunks')]);
    const campaign=latestCampaign(campaigns); const scene=latestCanonicalScene(scenes,campaign?.id); if(!campaign||!scene) return {campaign,scene,characters,npcs,chunks,ruleChunkCount,world:null,hex:null,actor:null,rules:[]};
    const world=await get('world_state',campaign.id).catch(()=>null);
    const hex=campaign.currentHexId ? await get('individual_hexes',campaign.currentHexId).catch(()=>null) : null;
    const actor=characters.find(c=>c.id===(scene.actingCharacterId||campaign.activeCharacterId)) || characters.find(c=>c.id===campaign.activeCharacterId) || characters[0] || null;
    const ruleQuery=`${question} ${scene.playerText||''} ${scene.masterDecision?.skill||''} ${scene.masterDecision?.attribute||''}`;
    const rules=ruleMatches(chunks,ruleQuery,7);
    return {campaign,scene,characters,npcs,chunks,ruleChunkCount,world,hex,actor,rules};
  }

  function publicContext(data){
    const {campaign,scene,world,hex,npcs,actor}=data;
    const currentHex=campaign?.currentHexId || scene?.hexId || '';
    const dialogueNames=uniq((scene?.npcDialogue||[]).map(x=>x.name));
    const locationNpcs=(npcs||[]).filter(n=>n.status!=='Dead' && currentHex && (n.currentLocationId===currentHex || n.currentLocationId===hex?.coordinate)).map(n=>({name:n.name,activity:compact(n.currentActivity,120),attitude:n.attitude}));
    const encounter=world?.encounter?.status==='active' ? (world.encounter.combatants||[]).map(c=>({name:c.name,side:c.side,status:c.status,x:c.x,y:c.y})) : [];
    const publicLocations=(world?.locations||[]).filter(l=>!currentHex || l.hexId===currentHex || l.hexId===hex?.coordinate).map(l=>({name:l.name,type:l.type,state:compact(l.state,160),publicNotes:compact(l.publicNotes,180)}));
    const clues=(world?.clues||[]).filter(c=>c.discovered).slice(-12).map(c=>({title:c.title,detail:compact(c.detail,180)}));
    return {
      day:campaign?.currentDay, period:campaign?.currentPeriod, currentHex, terrain:hex?.terrainResolution?.finalTerrainLabel||hex?.physicalTerrainLabel||hex?.terrain||'',
      actor:actor?{name:actor.name,skills:actor.skills,attributes:actor.attributes}:null,
      lastCanonicalScene:{title:scene?.title,playerAction:compact(scene?.playerText,400),narration:compact(scene?.narration,1800),dialogueNames,decision:scene?.masterDecision?{requiresCheck:scene.masterDecision.requiresCheck,attribute:scene.masterDecision.attribute,skill:scene.masterDecision.skill,risk:scene.masterDecision.risk,consequence:scene.masterDecision.consequence,ruling:compact(scene.masterDecision.ruling,500),ruleLocated:scene.masterDecision.ruleLocated}:null},
      npcsRegisteredAtLocation:locationNpcs, activeEncounter:encounter, publicLocations, discoveredClues:clues,
      journey:world?.journey?{pace:world.journey.pace,distanceKm:world.journey.distanceKm,periodsTravelled:world.journey.periodsTravelled}:null,
    };
  }

  function parseModelJson(raw){
    const data=typeof raw==='string'?JSON.parse(raw):raw;
    const text=(data?.candidates?.[0]?.content?.parts||[]).map(p=>p?.text||'').join('').trim();
    if(!text) throw new Error('O Mestre auxiliar não retornou conteúdo.');
    const clean=text.replace(/^```(?:json)?\s*/i,'').replace(/\s*```$/,'').trim();
    try{return JSON.parse(clean);}catch{return {answer:clean,ruleExplanation:'',skillSuggestions:[],optionalActions:[],citations:[]};}
  }

  async function callModel(systemInstruction,prompt){
    const preferred=localStorage.getItem(LAST_MODEL)||MODELS[0];
    const models=uniq([preferred,...MODELS]);
    const body={contents:[{role:'user',parts:[{text:prompt}]}],systemInstruction:{parts:[{text:systemInstruction}]},generationConfig:{responseMimeType:'application/json',maxOutputTokens:1100,temperature:.18}};
    let lastError=null;
    for(const model of models){
      try{
        if(window.Capacitor?.getPlatform?.() && window.Capacitor.getPlatform()!=='web' && window.Capacitor?.Plugins?.GeminiGateway){
          const result=await window.Capacitor.Plugins.GeminiGateway.generate({model,body:JSON.stringify(body),timeoutMs:42000});
          localStorage.setItem(LAST_MODEL,model); return parseModelJson(JSON.parse(result.raw));
        }
        const free=localStorage.getItem(FREE_KEY)||''; const paid=localStorage.getItem(PAID_KEY)||''; if(!free&&!paid) throw new Error('Chave Gemini não configurada.');
        let response=null;
        for(const key of uniq([free,paid])){
          response=await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`,{method:'POST',headers:{'Content-Type':'application/json','x-goog-api-key':key},body:JSON.stringify(body)});
          if(response.ok) break;
          if(![429,500,502,503,504].includes(response.status)) break;
        }
        if(!response?.ok) throw new Error(`HTTP ${response?.status||0}`);
        localStorage.setItem(LAST_MODEL,model); return parseModelJson(await response.json());
      }catch(error){ lastError=error; }
    }
    throw lastError||new Error('Nenhum modelo de texto respondeu.');
  }

  async function ask(question){
    const data=await snapshot(question); if(!data.scene) throw new Error('Nenhuma cena canônica ativa.');
    const publicState=publicContext(data);
    const rules=data.rules.map(r=>`[FONTE: ${r.sourceLabel||'Manual'} | página ${r.pageNumber}]\n${compact(r.text,900)}`).join('\n\n') || '[Nenhum trecho de regra relevante localizado.]';
    const system=`Você é a AJUDA DO MESTRE do Braseiro Forbidden Lands. Esta conversa fica FORA DA FICÇÃO. Nunca narre continuação, nunca mova personagem/NPC, nunca avance tempo, clima, mapa, encontro, relógio, recurso ou consequência. Nunca revele segredos, intenções ocultas, gmNotes ou informação que não esteja no ESTADO PÚBLICO fornecido. Responda em português brasileiro. Para presença: dialogueNames e activeEncounter são presença confirmada; npcsRegisteredAtLocation são apenas candidatos registrados no local e NÃO podem ser declarados presentes sem outro fato público. Para distância, posição, presença, porta, saída ou geometria: use somente fatos estabelecidos; se não houver medida/posição suficiente, diga claramente “isso ainda não foi estabelecido na cena” em vez de inventar. Para regras: use somente os TRECHOS DE MANUAIS fornecidos e cite fonte/página; se não houver suporte, diga “regra não localizada” e não improvise mecânica. Sugestões de perícia são possibilidades, não ordens de rolagem: uma perícia só vira teste se houver incerteza + consequência + competência relevante, ou se um procedimento oficial de Forbidden Lands exigir. Não transforme pergunta simples a NPC em teste automático. Se o usuário fizer várias perguntas, responda a todas em itens separados. Retorne JSON com: answer (string), ruleExplanation (string), skillSuggestions (array de strings), optionalActions (array de até 3 strings), citations (array de strings), certainty (string).`;
    const prompt=`PERGUNTA DO JOGADOR, FORA DA FICÇÃO:\n${question}\n\nESTADO PÚBLICO E CANÔNICO (somente leitura):\n${JSON.stringify(publicState,null,2)}\n\nTRECHOS DE MANUAIS RECUPERADOS:\n${rules}\n\nIMPORTANTE: a resposta não será salva como cena. Não produza narração diegética nem consequência.`;
    const result=await callModel(system,prompt);
    return {question,answer:String(result.answer||'').trim()||'Não consegui formular uma resposta segura.',ruleExplanation:String(result.ruleExplanation||'').trim(),skillSuggestions:uniq(result.skillSuggestions||[]).slice(0,4),optionalActions:uniq(result.optionalActions||[]).slice(0,3),citations:uniq(result.citations||[]).slice(0,8),certainty:String(result.certainty||'').trim()};
  }

  function ruleStatus(scene, ruleChunkCount=0){
    const d=scene?.masterDecision||{}; const citations=scene?.ruleCitations||[];
    if(!ruleChunkCount) return `<b>Biblioteca de regras vazia.</b> <span class="bms-muted">A consulta foi tentada, mas não há trechos indexados. Qualquer mecânica sem fonte deve ficar bloqueada até importar/sincronizar os manuais.</span>`;
    if(d.requiresCheck) return `<b>Teste indicado após consulta:</b> ${esc(d.characterName||'personagem em foco')} · ${esc(d.attribute||'Atributo')} + ${esc(skillAlias(d.skill)||'Perícia')}.${d.risk?` <span class="bms-muted">Risco: ${esc(d.risk)}.</span>`:''}${d.consequence?` <span class="bms-muted">Consequência: ${esc(d.consequence)}.</span>`:''}`;
    if(d.ruleLocated && citations.length) return `<b>Regra consultada:</b> ${citations.length} trecho(s) de manual foram associados à resolução. <span class="bms-muted">Nenhum teste deve ser criado só para “dar movimento” à cena.</span>`;
    if(citations.length) return `<b>Consulta de regras executada:</b> ${citations.length} trecho(s) relevante(s) recuperado(s). <span class="bms-muted">A resolução atual não exigiu uma mecânica comprovada.</span>`;
    return `<b>Consulta de regras executada:</b> nenhum trecho relevante foi associado a esta batida entre ${esc(ruleChunkCount)} trecho(s) indexado(s). <span class="bms-muted">Sem fonte relevante, o sistema não deve inventar teste, modificador, dano ou custo.</span>`;
  }

  function renderAnswer(item){
    return `<div class="bms-answer-block"><div class="bms-q">Você: ${esc(item.question)}</div><div class="bms-answer">${esc(item.answer)}</div>${item.ruleExplanation?`<div class="bms-muted" style="margin-top:7px"><b>Regra:</b> ${esc(item.ruleExplanation)}</div>`:''}${item.skillSuggestions?.length?`<div class="bms-pills" style="margin-top:8px">${item.skillSuggestions.map(x=>`<span class="bms-pill">${esc(x)}</span>`).join('')}</div>`:''}${item.citations?.length?`<div class="bms-citations">${item.citations.map(x=>`<span>• ${esc(x)}</span>`).join('')}</div>`:''}</div>`;
  }

  function mainActionTextarea(){
    return [...document.querySelectorAll('textarea')].find(t=>t.id!=='bms-question' && (t.dataset.braseiroVoiceAction==='true' || /^O que .+ faz\??$/i.test(t.placeholder||''))) || null;
  }
  async function fillMainAction(value){
    let target=mainActionTextarea();
    if(!target){
      const opener=[...document.querySelectorAll('button')].find(btn=>/escrever (?:outra |ação |acao )?(?:ação|acao)|ação personalizada|acao personalizada/i.test(btn.textContent||''));
      if(opener && !opener.disabled){ opener.click(); await new Promise(resolve=>setTimeout(resolve,180)); target=mainActionTextarea(); }
    }
    if(!target) return;
    const setter=Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype,'value')?.set; if(setter) setter.call(target,value); else target.value=value; target.dispatchEvent(new Event('input',{bubbles:true})); target.focus(); target.scrollIntoView({behavior:'smooth',block:'center'});
  }

  function cleanNarrativeHost(host){
    if(!host) return;
    host.querySelectorAll('aside').forEach(el=>{ if(/regra em foco/i.test(el.textContent||'')) el.hidden=true; });
    host.querySelectorAll('details').forEach(el=>{ const text=el.querySelector('summary')?.textContent||''; if(/fontes de regras consultadas|decisão do mestre/i.test(text)) el.hidden=true; });
    host.querySelectorAll('div').forEach(el=>{ if(/alteração\(ões\) persistida\(s\) no mundo/i.test(el.textContent||'')) el.hidden=true; });
  }

  let cachedSnapshot = null;
  let cachedSnapshotSceneId = '';
  async function panelData(force = false){
    if(!force && cachedSnapshot && cachedSnapshotSceneId === state.sceneId && cachedSnapshot.scene?.id === state.sceneId){
      return cachedSnapshot;
    }
    const data = await snapshot('');
    if(data?.scene?.id){
      cachedSnapshot = data;
      cachedSnapshotSceneId = data.scene.id;
    }
    return data;
  }

  async function submitQuestion(question){
    const clean=String(question||'').trim(); if(!clean||state.busy) return null;
    state.busy=true; cachedSnapshot = null; await ensurePanel();
    try{ const answer=await ask(clean); state.history=[answer,...state.history].slice(0,5); return answer; }
    catch(error){ const failed={question:clean,answer:`Não consegui consultar o Mestre auxiliar: ${error?.message||String(error)}`,ruleExplanation:'A aventura não foi alterada.',skillSuggestions:[],optionalActions:[],citations:[]}; state.history=[failed,...state.history].slice(0,5); return failed; }
    finally{ state.busy=false; await ensurePanel(); document.getElementById(PANEL_ID)?.scrollIntoView({behavior:'smooth',block:'nearest'}); }
  }

  let docObserver = null;
  function updateDomSafely(fn){
    if(docObserver) docObserver.disconnect();
    try {
      return fn();
    } finally {
      if(docObserver && document.documentElement){
        docObserver.observe(document.body || document.documentElement, { subtree: true, childList: true });
      }
    }
  }

  async function ensurePanel(){
    const host=[...document.querySelectorAll('article.adventure-card')].find(a=>a.querySelector('.narration-toolbar'));
    if(!host){
      updateDomSafely(()=>document.getElementById(PANEL_ID)?.remove());
      return;
    }
    cleanNarrativeHost(host);
    const data=await panelData().catch(()=>null); if(!data?.scene) return;
    const scene=data.scene;
    let panel=document.getElementById(PANEL_ID);
    if(!panel){
      panel=document.createElement('section');
      panel.id=PANEL_ID;
      updateDomSafely(()=>host.insertAdjacentElement('afterend',panel));
    }
    else if(panel.previousElementSibling!==host) {
      updateDomSafely(()=>host.insertAdjacentElement('afterend',panel));
    }
    if(state.sceneId!==scene.id){ state.sceneId=scene.id; state.history=[]; }
    const skills=localSkillSuggestions(scene,data.actor); const actions=localActions(scene);
    const renderSig = JSON.stringify([scene.id, state.busy, state.history.map(item => [item.question,item.answer]), skills, actions, scene.masterDecision?.ruling, scene.ruleCitations?.length, data.ruleChunkCount]);
    if (state.renderSig === renderSig && panel.innerHTML) return;
    state.renderSig = renderSig;
    updateDomSafely(()=>{
      panel.innerHTML=`<div class="bms-head"><div><strong>AJUDA DO MESTRE</strong><small>Regras, contexto e dúvidas ficam aqui — separadas da narração.</small></div><span class="bms-lock">🔒 cena não avança</span></div><div class="bms-body"><div class="bms-grid"><div class="bms-box"><div class="bms-title">O que o sistema está fazendo</div><div class="bms-rule-status">${ruleStatus(scene,data.ruleChunkCount)}</div><div class="bms-muted" style="margin-top:7px">Biblioteca local: ${esc(data.ruleChunkCount||0)} trecho(s) de regra indexado(s).</div>${scene.masterDecision?.ruling?`<div class="bms-muted" style="margin-top:7px">Arbitragem: ${esc(scene.masterDecision.ruling)}</div>`:''}${scene.ruleCitations?.length?`<div class="bms-citations">${scene.ruleCitations.slice(0,4).map(c=>`<span>• ${esc(c.sourceLabel)} · pág. ${esc(c.pageNumber)}</span>`).join('')}</div>`:''}</div><div class="bms-box"><div class="bms-title">Perícias que podem ser úteis</div><div class="bms-pills">${skills.map(s=>`<span class="bms-pill">${esc(s)}</span>`).join('')}</div><div class="bms-muted" style="margin-top:7px">São caminhos possíveis. Não significam que uma rolagem seja necessária.</div></div></div><div class="bms-box"><div class="bms-title">Sugestões opcionais</div><div class="bms-actions">${actions.map((a,i)=>`<button type="button" class="bms-action" data-bms-action="${i}">${esc(a)}</button>`).join('')}</div><div class="bms-muted" style="margin-top:7px">Tocar apenas coloca a sugestão na Ação Livre; nunca executa nem envia automaticamente.</div></div><div class="bms-box"><div class="bms-title">Pergunte ao Mestre sem mexer na aventura</div><textarea id="bms-question" data-braseiro-voice-action="true" placeholder="Ex.: quem está na cena? Qual a distância até a porta? Que perícias podem ajudar? Essa ação exige teste?"></textarea><div class="bms-row"><button type="button" class="bms-ask" ${state.busy?'disabled':''}>${state.busy?'Consultando…':'Perguntar ao Mestre'}</button><span class="bms-muted">Não cria cena, não passa tempo e não entra no histórico canônico.</span></div><div class="bms-results">${state.history.map(renderAnswer).join('')}</div></div></div>`;
      panel.querySelectorAll('[data-bms-action]').forEach(btn=>btn.addEventListener('click',()=>void fillMainAction(actions[Number(btn.dataset.bmsAction)]||'')));
      panel.querySelector('.bms-ask')?.addEventListener('click', async()=>{
        const textarea=panel.querySelector('#bms-question'); const question=textarea?.value?.trim(); if(!question||state.busy) return;
        if(textarea) textarea.value=''; await submitQuestion(question);
      });
    });
  }

  // Ajuda do Mestre é opt-in. Este módulo NÃO intercepta Enter, botões nem o
  // textarea de Ação Livre. Isso evita transformar ações ficcionais que mencionam
  // posição/distância/saídas em perguntas fora da ficção.

  let updating = false;
  async function safeScheduleRun(){
    if(updating) return;
    updating = true;
    try {
      await ensurePanel();
    } catch(_) {
    } finally {
      updating = false;
    }
  }
  function schedule(){ clearTimeout(state.refreshTimer); state.refreshTimer=setTimeout(safeScheduleRun,250); }
  function scheduleInvalidating(){ cachedSnapshot = null; schedule(); }
  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',schedule,{once:true}); else schedule();
  docObserver = new MutationObserver(schedule);
  if(document.documentElement) docObserver.observe(document.body || document.documentElement, { subtree:true, childList:true });
  window.addEventListener('world-state-updated',scheduleInvalidating);
  window.addEventListener('braseiro-critical-write',schedule);
  window.addEventListener('braseiro-master-support:ask',event=>void submitQuestion(event?.detail?.question||''));
  window.BraseiroMasterSupport={version:'3.3.0',ask,submit:submitQuestion,refresh:ensurePanel,sceneFrozen:true};
})();
