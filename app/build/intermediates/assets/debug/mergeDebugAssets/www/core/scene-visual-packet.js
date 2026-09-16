function clean(v,max=900){return String(v??'').replace(/\s+/g,' ').trim().slice(0,max)}
function clone(v){return structuredClone(v)}
function rosterOf(state){const out={...(state?.charactersState??{})};const active=state?.characterState;if(active?.characterId&&!out[active.characterId])out[active.characterId]=active;return out}
function activeGroup(state){const party=state?.partyState,groups=party?.groups??{};return groups?.[party?.activeGroupId]??Object.values(groups)[0]??null}
function visibleEquipment(character){return (character?.inventory??[]).filter(i=>String(i?.carryState??'').toUpperCase()==='READIED').map(i=>clean(i?.name??i?.itemName??i?.itemId??'',100)).filter(Boolean).slice(0,6)}
function profileFor(character){const p=character?.visualProfile??{};return Object.freeze({
  description:clean(p.description??p.generationPrompt??'',800),
  mustKeep:Object.freeze((p.mustKeep??[]).map(v=>clean(v,140)).filter(Boolean).slice(0,16)),
  paletteNotes:clean(p.paletteNotes??'',300),
  portraitRef:clean(p.portraitRef??p.imageRef??'',500)||null,
  portraitSha256:clean(p.portraitSha256??p.sha256??'',96)||null,
});}

export function buildCharacterBible(state,{memberIds=null}={}){
  const roster=rosterOf(state);const group=activeGroup(state);const ids=(memberIds??group?.memberIds??Object.keys(roster)).filter(id=>roster[id]);
  return Object.freeze(ids.map(id=>{const c=roster[id],visual=profileFor(c);return Object.freeze({
    characterId:id,name:clean(c?.name??id,120),visualDescription:visual.description||null,mustKeep:visual.mustKeep,paletteNotes:visual.paletteNotes||null,portraitRef:visual.portraitRef,portraitSha256:visual.portraitSha256,currentReadiedEquipment:Object.freeze(visibleEquipment(c))
  })}));
}

export function buildSceneVisualPacket({state,scene=null,narrative=null,showParty=true}={}){
  const current=scene??state?.encounterState?.currentScene?.publicScene??state?.encounterState?.currentScene??{};
  const group=activeGroup(state);const bible=buildCharacterBible(state,{memberIds:showParty?(group?.memberIds??null):[]});
  const position=state?.worldState?.position??group?.position??{};const time=state?.worldState?.time??{};const weather=state?.worldState?.weather??{};
  return Object.freeze({
    schemaVersion:1,sceneTitle:clean(current?.title??current?.name??position?.name??position?.label??'Cena atual',180),
    localVisibleSituation:clean(narrative?.text??current?.summary??current?.situation??current?.premise??'',1600),
    location:clean(position?.name??position?.label??current?.location??'',220)||null,
    terrain:clean(position?.terrain??position?.biome??current?.terrain??'',220)||null,
    timeOfDay:clean(time?.timeOfDay??time?.dayPhase??state?.worldState?.timeOfDay??'',100)||null,
    weather:clean(weather?.label??weather?.name??weather,180)||null,
    mood:clean(current?.mood??current?.tone??'',180)||'sombrio, concreto e atmosférico',
    showParty:Boolean(showParty&&bible.length),partyCount:showParty?bible.length:0,visibleCharacters:Object.freeze(bible),
    mustShow:Object.freeze((current?.visualMustShow??[]).map(v=>clean(v,180)).filter(Boolean).slice(0,20)),
    mustNotShow:Object.freeze(['HUD','interface do aplicativo','texto ou legendas','hexágonos ou grid de mapa','logotipos','segredos não descobertos','criaturas ou locais ocultos',...(current?.visualMustNotShow??[]).map(v=>clean(v,180)).filter(Boolean)].slice(0,24)),
    styleProfile:'pintura digital de fantasia sombria, atmosférica e detalhada; não fotográfica; não cartunesca; ambiente legível em tela de celular; paleta natural e iluminação coerente',
    composition:'cena ambiental cinematográfica, com o lugar como protagonista; personagens não devem dominar a composição salvo quando a cena exigir'
  });
}

export function sceneReferenceImages(packet){return Object.freeze((packet?.visibleCharacters??[]).map(c=>c.portraitRef).filter(Boolean).slice(0,5))}

export function buildSceneGenerationPrompt(packet){
  const p=packet??{};const chars=p.visibleCharacters??[];
  const characterBlock=p.showParty?chars.map((c,i)=>{
    const desc=c.visualDescription?`Aparência canônica: ${c.visualDescription}.`:'Aparência canônica não definida: mantenha esta pessoa pequena/distante ou em silhueta; NÃO invente traços identificadores permanentes.';
    const keep=c.mustKeep?.length?`Manter obrigatoriamente: ${c.mustKeep.join('; ')}.`:'';
    const pal=c.paletteNotes?`Paleta/roupa persistente: ${c.paletteNotes}.`:'';
    const gear=c.currentReadiedEquipment?.length?`Equipamento atualmente visível/preparado: ${c.currentReadiedEquipment.join(', ')}. Não acrescente equipamento não listado.`:'';
    return `${i+1}. ${c.name}. ${desc} ${keep} ${pal} ${gear}`.trim();
  }).join('\n'):'Nenhum integrante do grupo deve aparecer.';
  return `Gere UMA ilustração da cena atual de um RPG de fantasia sombria. Use SOMENTE o contexto local abaixo; não complete a imagem com lore amplo do mundo nem com informações não fornecidas.\n\nCENA LOCAL\nTítulo: ${p.sceneTitle||'Cena atual'}\nLocal: ${p.location||'não especificado'}\nTerreno: ${p.terrain||'não especificado'}\nMomento: ${p.timeOfDay||'não especificado'}\nClima: ${p.weather||'não especificado'}\nSituação perceptível: ${p.localVisibleSituation||'sem descrição adicional'}\nAtmosfera: ${p.mood||'sombria'}\n\nGRUPO VISÍVEL\n${p.showParty?`Mostre EXATAMENTE ${p.partyCount} integrante(s). Não crie pessoas extras para representar o grupo.`:'Não mostrar o grupo.'}\n${characterBlock}\n\nCOMPOSIÇÃO\n${p.composition}\nEstilo: ${p.styleProfile}\n${p.mustShow?.length?`Elementos obrigatórios: ${p.mustShow.join('; ')}.`:''}\nNão mostrar: ${(p.mustNotShow??[]).join('; ')}.\n\nREGRAS DE CONSISTÊNCIA\n- Quando houver imagem de referência de um personagem, preserve rosto, cabelo, idade aparente, proporções e identidade visual dessa referência.\n- A descrição canônica e as imagens de referência têm precedência sobre qualquer lembrança da conversa anterior.\n- Não transforme descrição narrativa em regra mecânica ou spoiler.\n- Não invente personagens, poderes, equipamentos, monstros ou descobertas que não estejam explicitamente visíveis nesta cena.\n- Produza somente a imagem, sem texto dentro da arte.`;
}
