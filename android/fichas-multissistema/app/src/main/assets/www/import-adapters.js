(()=>{
const num=v=>v===null||v===undefined||v===''?'':String(v);
const text=v=>v===null||v===undefined?'':String(v);
const lines=a=>(Array.isArray(a)?a:[]).filter(Boolean).join('\n');
const TALES_ARCHETYPES={Popular:'Popular Kid','Popular Kid':'Popular Kid',Bookworm:'Bookworm','Computer Geek':'Computer Geek',Hick:'Hick',Jock:'Jock',Rocker:'Rocker',Troublemaker:'Troublemaker',Weirdo:'Weirdo'};
const TALES_ATTR={CORPO:'body',TECNOLOGIA:'tech','CORAÇÃO':'heart',MENTE:'mind'};
const TALES_SKILLS={Furtividade:'sneak','Força':'force','Movimentação':'move','Mecânica':'tinker','Programação':'program','Cálculo':'calculate','Investigação':'investigate','Conhecimento':'comprehend','Compreender':'comprehend','Empatia':'empathize','Contatos':'contact','Contatar':'contact','Charme':'charm','Encantar':'charm','Liderança':'lead','Liderar':'lead'};
const WWN_ATTR={Strength:'str',Dexterity:'dex',Constitution:'con',Intelligence:'int',Wisdom:'wis',Charisma:'cha'};
const WWN_SKILLS={Administer:'administer',Connect:'connect',Convince:'convince',Craft:'craft',Exert:'exert',Heal:'heal',Know:'know',Lead:'lead',Magic:'magic',Notice:'notice',Perform:'perform',Pray:'pray',Punch:'punch',Ride:'ride',Sail:'sail',Shoot:'shoot',Sneak:'sneak',Stab:'stab',Survive:'survive',Trade:'trade',Work:'work'};
const WWN_CLASS={ADVENTURER:'Aventureiro (classes parciais)',WARRIOR:'Guerreiro Completo',EXPERT:'Especialista Completo',MAGE:'Mago Completo'};
const WWN_FOCUS={ALERT:'Alerta','WEAPON MASTER':'Mestre de Armas',WEAPON_MASTER:'Mestre de Armas',ARTISAN:'Artesão'};
function mapTales(p){
 const k=p.kid||{},out={};
 out.name=text(k.name);out.concept=text(k.description);out.age=num(k.age);out.archetype=TALES_ARCHETYPES[k.archetype]||text(k.archetype);
 out.drive=text(k.drive);out.problem=text(k.problem);out.pride=text(k.pride);out.anchor=text(k.anchor);out.iconic_item=text(k.iconic_item);out.luck=num(k.luck);out.xp=num(k.xp);
 out.portrait_asset=text(k.portrait_asset);
 for(const [src,dst] of Object.entries(TALES_ATTR))if(k.attributes&&k.attributes[src]!==undefined)out[dst]=num(k.attributes[src]);
 for(const [src,dst] of Object.entries(TALES_SKILLS))if(k.skills&&k.skills[src]!==undefined)out[dst]=num(k.skills[src]);
 out.relationships=lines((k.relationships||[]).map(r=>[r?.name,r?.description].filter(Boolean).join(' — ')));
 out.conditions=lines(k.conditions||[]);
 out.broken=k.broken===true?'Sim':k.broken===false?'Não':text(k.broken);
 out.gear=lines((k.inventory||[]).map(i=>`${text(i?.name)}${i?.bonus!==undefined?` (+${i.bonus})`:''}${i?.iconic?' — Item Icônico':''}`));
 out.notes=lines([k.pride_available===false?'Orgulho já utilizado nesta sessão.':'',p.version!==undefined?`Formato importado: ${text(p.format)} v${p.version}`:'']);
 out._externalSource={format:p.format||'braseiro-tales-kid',version:p.version??null,profile:p.profile||'tales_kid'};
 return {system:'tftl',sourceLabel:'Tales from the Loop',character:out};
}
function mapWwn(p){
 const c=p.character||{},out={};
 out.name=text(c.name);out.concept=text(c.goal);out.class=WWN_CLASS[c.classId]||text(c.classId);out.level=num(c.level);out.xp=num(c.xp);
 out.partial_classes=lines(c.partialClasses||c.classPath?.partialClasses||[]);
 const focus=(c.foci||[])[0];out.foci=focus?(WWN_FOCUS[focus.focusId]||WWN_FOCUS[focus.name]||text(focus.name||focus.focusId)):'';
 for(const [src,dst] of Object.entries(WWN_ATTR))if(c.attributes&&c.attributes[src]!==undefined)out[dst]=num(c.attributes[src]);
 out.hp=c.hp?`${num(c.hp.current)} / ${num(c.hp.max)}`:'';out.attack_bonus=num(c.baseAttackBonus);
 out.physical_save=num(c.savingThrows?.Physical);out.evasion_save=num(c.savingThrows?.Evasion);out.mental_save=num(c.savingThrows?.Mental);out.luck_save=num(c.savingThrows?.Luck);
 for(const [src,dst] of Object.entries(WWN_SKILLS))if(c.skills&&c.skills[src]!==undefined)out[dst]=num(c.skills[src]);
 const effort=Object.entries(c.effort||{}).map(([id,e])=>`${id}: ${num(e?.current)} / ${num(e?.max)}`);out.effort=lines(effort);
 const arts=(c.arts||[]).map(a=>`Arte: ${text(a?.name||a?.artId)}`);const spells=(c.magic?.startingSpells||[]).map(s=>`Magia: ${text(s?.name||s?.spellId)}`);out.arts_spells=lines([...arts,...spells]);
 out.languages=lines(c.languages||[]);out.silver=num(c.silver);out.system_strain=c.systemStrain?`${num(c.systemStrain.current)} / ${num(c.systemStrain.max)}`:'';
 out.gear=lines((c.inventory||[]).map(i=>typeof i==='string'?i:text(i?.name||i?.id||JSON.stringify(i))));
 out.notes=lines([c.characterId?`ID: ${c.characterId}`:'',out.partial_classes?`Classes parciais: ${out.partial_classes.replaceAll('\n',' + ')}`:'',c.creationProvenance?.rule?`Criação: ${c.creationProvenance.rule}`:'',p.version!==undefined?`Formato importado: ${text(p.format)} v${p.version}`:'']);
 out._externalSource={format:p.format||'BRASEIRO_WWN_CHARACTER',version:p.version??null,system:p.system||'WWN'};
 return {system:'wwn',sourceLabel:'Worlds Without Number',character:out};
}
function map(payload){
 if(!payload||typeof payload!=='object')return null;
 if(payload.schema==='braseiro.multisheet.v3')return {native:true,payload};
 if((payload.format==='braseiro-tales-kid'||payload.profile==='tales_kid')&&payload.kid)return mapTales(payload);
 if((payload.format==='BRASEIRO_WWN_CHARACTER'||payload.system==='WWN')&&payload.character)return mapWwn(payload);
 return null;
}
globalThis.BRASEIRO_IMPORT_ADAPTERS=Object.freeze({map,mapTales,mapWwn});
})();