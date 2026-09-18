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

const DND_SKILLS={'Acrobacia':'acrobatics','Adestrar Animais':'animal_handling','Arcanismo':'arcana','Atletismo':'athletics','Atuação':'performance','Enganação':'deception','Furtividade':'stealth','História':'history','Intimidação':'intimidation','Intuição':'insight','Investigação':'investigation','Medicina':'medicine','Natureza':'nature','Percepção':'perception','Persuasão':'persuasion','Prestidigitação':'sleight','Religião':'religion','Sobrevivência':'survival'};
const TOR_SKILLS={'Fascínio':'awe','Atletismo':'athletics','Vigilância':'awareness','Caçada':'hunting','Música':'song','Ofício':'craft','Indução':'enhearten','Encorajar':'enhearten','Viagem':'travel','Discernimento':'insight','Cura':'healing','Cortesia':'courtesy','Batalha':'battle','Persuasão':'persuade','Furtividade':'stealth','Busca':'scan','Exploração':'explore','Enigma':'riddle','História':'lore','Saber':'lore'};
const PF_RANK={untrained:0,trained:1,expert:2,master:3,legendary:4};
const PF_SKILLS={acrobatics:'acrobatics',arcana:'arcana',athletics:'athletics',crafting:'crafting',deception:'deception',diplomacy:'diplomacy',intimidation:'intimidation',medicine:'medicine',nature:'nature',occultism:'occultism',performance:'performance',religion:'religion',society:'society',stealth:'stealth',survival:'survival',thievery:'thievery'};
const FL_AGE={Young:'Jovem',Adult:'Adulto',Old:'Idoso',Jovem:'Jovem',Adulto:'Adulto',Idoso:'Idoso'};
const FL_SKILLS={'Força Bruta':'might','Might':'might','Resistência':'endurance','Endurance':'endurance','Combate Corpo a Corpo':'melee','Melee':'melee','Artesanato':'crafting','Crafting':'crafting','Furtividade':'stealth','Stealth':'stealth','Prestidigitação':'sleight','Sleight of Hand':'sleight','Movimentação':'move','Move':'move','Pontaria':'marksmanship','Marksmanship':'marksmanship','Exploração':'scouting','Scouting':'scouting','Tradição':'lore','Conhecimento':'lore','Lore':'lore','Sobrevivência':'survival','Survival':'survival','Discernimento':'insight','Insight':'insight','Manipulação':'manipulation','Manipulation':'manipulation','Performance':'performance','Cura':'healing','Healing':'healing','Trato com Animais':'animal_handling','Animal Handling':'animal_handling'};
const TRAV_SKILLS={'Atletismo':'athletics','Athletics':'athletics','Mecânica':'mechanic','Mechanic':'mechanic','Pilotagem':'pilot','Pilot':'pilot','Profissão':'profession','Profession':'profession','Reconhecimento':'recon','Recon':'recon','Furtividade':'stealth','Stealth':'stealth','Corpo a Corpo':'melee','Melee':'melee','Trajes de Proteção':'vacc_suit','Vacc Suit':'vacc_suit','Traquejo de Rua':'streetwise','Streetwise':'streetwise','Admin':'admin','Advocate':'advocate','Animals':'animals','Broker':'broker','Carouse':'carouse','Deception':'deception','Diplomat':'diplomat','Drive':'drive','Electronics':'electronics','Engineer':'engineer','Explosives':'explosives','Flyer':'flyer','Gun Combat':'gun_combat','Gunner':'gunner','Heavy Weapons':'heavy_weapons','Investigate':'investigate','Jack-of-all-Trades':'jack_of_all_trades','Language':'language','Leadership':'leadership','Medic':'medic','Navigation':'navigation','Persuade':'persuade','Science':'science','Seafarer':'seafarer','Steward':'steward','Survival':'survival','Tactics':'tactics'};
function parseSigned(v){const m=String(v??'').match(/[+-]?\d+/);return m?String(Number(m[0])):''}
function mapDndSheet(p){
 const c=p.character||{},out={};out.name=text(c.name);out.concept=lines([c.concept,c.role?'Papel: '+c.role:'']);out.species=text(c.species);out.background=text(c.background);out.class=text(c.class_name);out.level=num(c.level);
 out.for=num(c.str);out.des=num(c.dex);out.con=num(c.con);out.int=num(c.int_score);out.sab=num(c.wis);out.car=num(c.cha);out.ac=num(c.ac);out.hp=num(c.max_hp??c.hp);out.speed=text(c.speed);out.initiative=parseSigned(c.initiative);out.prof_bonus=parseSigned(c.proficiency_bonus);
 for(const part of String(c.skills||'').split('•')){const m=part.trim().match(/^(.+?)\s+([+-]?\d+)/);if(m&&DND_SKILLS[m[1].trim()])out[DND_SKILLS[m[1].trim()]]=String(Number(m[2]));}
 out.saving_throws=text(c.saving_throws);out.proficiencies=lines([c.proficiencies,c.languages?'Idiomas: '+c.languages:'',c.tools?'Ferramentas: '+c.tools:'',c.hit_dice?'Dado de Vida: '+c.hit_dice:'',c.carrying_capacity?'Carga: '+c.carrying_capacity:'']);
 out.features=lines([c.features,c.role?'Papel: '+c.role:'']);out.spells=lines([c.spellcasting,c.spells]);out.attacks=text(c.attacks);out.equipment=text(c.equipment||c.inventory);
 out.notes=lines([c.nickname?'Apelido: '+c.nickname:'',c.title?'Título: '+c.title:'',c.alignment?'Alinhamento: '+c.alignment:'',c.passive_senses?'Sentidos passivos: '+c.passive_senses:'',c.appearance?'Aparência: '+c.appearance:'',c.personality?'Personalidade: '+c.personality:'',c.ideal?'Ideal: '+c.ideal:'',c.bond?'Vínculo: '+c.bond:'',c.fear?'Medo: '+c.fear:'',c.flaw?'Falha: '+c.flaw:'',c.contact?'Contato: '+c.contact:'',c.rival?'Rival: '+c.rival:'',c.debt?'Dívida: '+c.debt:'',c.motivation?'Motivação: '+c.motivation:'',c.history,c.life_path,c.level_up?'Subida de nível: '+c.level_up:'',c.extra_notes,c.campaign_notes,p.app_version?'Origem: '+p.format+' '+p.app_version:'']);
 out._externalSource={format:p.format,version:p.format_version??null,profile:p.rules_profile||'dnd55'};return {system:'dnd55',sourceLabel:'D&D / Braseiro Character Sheet',character:out};
}
function mapTor(p){
 const c=p.character||{},sh=c.sheet||{},id=sh.identity||{},a=sh.attributes||{},combat=sh.combat||{},other=sh.other||{},out={};
 out.name=text(id.name||c.name);out.concept=text(id.description);out.culture=text(id.culture||c.culture);out.calling=text(id.vocation||c.vocation);out.patron=text(id.patron);out.distinctive=text(other.notes);
 out.strength=num(a.strength);out.heart=num(a.heart);out.wits=num(a.wits);out.endurance=num(a.enduranceCurrent);out.hope=num(a.hopeCurrent);out.parry=num(combat.parry);out.protection=text(combat.armor);out.load=num(a.load);out.shadow=num(a.shadow);out.valour=num(a.valor);out.wisdom=num(a.wisdom);
 const meta=[];for(const sk of sh.skills||[]){const dst=TOR_SKILLS[sk.name];if(dst)out[dst]=num(sk.rank);if(sk.favorite||sk.specialization)meta.push(sk.name+' '+sk.rank+(sk.favorite?' ★':'')+(sk.specialization?' — '+sk.specialization:''));}
 out.combat_proficiencies=lines((combat.proficiencies||[]).map(x=>x.name+' '+x.rank+(x.favorite?' ★':'')));out.virtues_rewards=lines([other.virtues?'Virtudes: '+other.virtues:'',other.rewards?'Recompensas: '+other.rewards:'',other.flaws?'Falhas: '+other.flaws:'']);
 const inv=(sh.inventory||[]).map(x=>x.name+(x.quantity&&x.quantity!==1?' x'+x.quantity:'')+(x.description?' — '+x.description:''));const weap=(combat.weapons||[]).map(x=>x.name+' — Dano '+x.damage+', Ferimento '+x.injury+', Carga '+x.load+(x.reward?' — '+x.reward:''));
 out.gear=lines([combat.armor?'Armadura: '+combat.armor:'',combat.helmet&&combat.helmet!=='Nenhum'?'Elmo: '+combat.helmet:'',combat.shield&&combat.shield!=='Nenhum'?'Escudo: '+combat.shield:'',...weap,...inv]);
 out.notes=lines([id.lifestyle?'Padrão de vida: '+id.lifestyle:'',id.age!==undefined?'Idade: '+id.age:'',id.company?'Companhia: '+id.company:'',a.enduranceMax!==undefined?'Resistência máxima: '+a.enduranceMax:'',a.hopeMax!==undefined?'Esperança máxima: '+a.hopeMax:'',a.fatigue!==undefined?'Fadiga: '+a.fatigue:'','Ferido: '+(a.wounded?'sim':'não')+'; Cansado: '+(a.weary?'sim':'não')+'; Miserável: '+(a.miserable?'sim':'não'),a.supplies!==undefined?'Suprimentos: '+a.supplies:'',meta.length?'Perícias favoritas/especializações:\n'+meta.join('\n'):'',other.notes,other.narrativeConditions,sh.resources?'Recursos: '+JSON.stringify(sh.resources):'',p.version!==undefined?'Formato importado: '+p.format+' v'+p.version:'']);
 out._externalSource={format:p.format,version:p.version??null,profile:p.systemProfileId||c.systemProfileId};return {system:'tor2e',sourceLabel:'O Um Anel 2e',character:out};
}
function mapPf2(p){
 const c=p.character||{},out={};out.name=text(c.name);out.concept=lines([c.visual_description,c.personality]);out.ancestry=text(c.ancestry||c.species);out.heritage=text(c.heritage);out.background=text(c.background);out.class=text(c.class_name);out.level=num(c.level);
 out.str=num(c.str);out.dex=num(c.dex);out.con=num(c.con);out.int=num(c.int_score);out.wis=num(c.wis);out.cha=num(c.cha);out.ac=num(c.ac);out.hp=num(c.max_hp??c.hp);
 const ranks=[];for(const [src,dst] of Object.entries(PF_SKILLS)){const rank=c.skill_ranks?.[src];if(rank!==undefined){out[dst]=num(PF_RANK[String(rank).toLowerCase()]??rank);ranks.push(src+': '+rank);}}
 for(const pair of [['perception_rank','perception'],['fortitude_rank','fortitude'],['reflex_rank','reflex'],['will_rank','will']]){const rank=c[pair[0]];if(rank!==undefined){out[pair[1]]=num(PF_RANK[String(rank).toLowerCase()]??rank);ranks.push(pair[0].replace('_rank','')+': '+rank);}}
 out.equipment=text(c.equipment);out.spells=text(c.spells);out.notes=lines([c.notes,c.attribute_model?'Modelo de atributos: '+c.attribute_model:'',ranks.length?'Graus importados (0=destreinado, 1=treinado, 2=especialista, 3=mestre, 4=lendário):\n'+ranks.join('\n'):'',p.app_version?'Origem: '+p.format+' '+p.app_version:'']);
 out._externalSource={format:p.format,version:p.format_version??null,profile:p.rules_profile||p.system_id};return {system:'pf2e',sourceLabel:'Pathfinder 2e',character:out};
}
function mapForbidden(p){
 const c=p.character||{},out={};out.name=text(c.name);out.concept=lines([c.background,c.appearance,c.personality,c.motivation]);out.kin=text(c.kin);out.profession=text(c.profession);out.age=FL_AGE[c.age]||text(c.age);out.pride=text(c.pride);out.dark_secret=text(c.darkSecret);
 out.relationships=lines((c.relationships||[]).map(r=>[r?.characterId,r?.description].filter(Boolean).join(' — ')));for(const id of ['strength','agility','wits','empathy'])if(c.attributes?.[id])out[id]=num(c.attributes[id].current??c.attributes[id].max);
 for(const [name,val] of Object.entries(c.skills||{})){const dst=FL_SKILLS[name];if(dst)out[dst]=num(Math.max(Number(out[dst])||0,Number(val)||0));}
 out.willpower=num(c.willpowerPoints);out.talents=lines(Object.entries(c.talents||{}).map(([k,v])=>k+' '+v));out.conditions=lines([...(c.conditions||[]).map(String),...(c.criticalInjuries||[]).map(x=>typeof x==='string'?x:text(x?.name||JSON.stringify(x)))]);
 const itemLines=(arr,label)=>(arr||[]).map(x=>label+': '+text(x?.name||x?.id)+(x?.quantity!==undefined?' x'+x.quantity:''));
 out.gear=lines([...itemLines(c.weapons,'Arma'),...itemLines(c.armor,'Armadura'),...itemLines(c.gear,'Item'),c.encumbrance?'Carga: '+c.encumbrance.current+'/'+c.encumbrance.max:'',c.coins?'Moedas: '+c.coins.copper+' cobre, '+c.coins.silver+' prata, '+c.coins.gold+' ouro':'',c.resourceDice?'Dados de recurso: comida '+(c.resourceDice.food||'-')+', água '+(c.resourceDice.water||'-')+', flechas '+(c.resourceDice.arrows||'-')+', tochas '+(c.resourceDice.torches||'-'):'']);
 out.notes=lines([c.homeland?'Terra natal: '+c.homeland:'',c.reputation!==undefined?'Reputação: '+c.reputation:'',c.xp!==undefined?'XP: '+c.xp:'',c.notes,p.appVersion?'Origem: '+p.format+' '+p.appVersion:'']);out._externalSource={format:p.format,version:p.schemaVersion??null};return {system:'forbidden_lands',sourceLabel:'Forbidden Lands',character:out};
}
function mapTraveller(p){
 const c=p.payload||{},out={};out.name=text(c.name);out.homeworld=text(c.homeworld);out.age=num(c.age);out.concept=lines([c.title?'Título: '+c.title:'',c.species?'Espécie: '+c.species:'',c.gender?'Gênero: '+c.gender:'',c.background]);
 const careers=c.careers||[],last=careers[careers.length-1]||{};out.career=text(last.career);out.assignment=text(last.assignment);out.rank=text(last.rankTitle??last.rank);
 out.career_history=lines(careers.map(x=>'Termo '+x.term+' ('+x.ageStart+'–'+x.ageEnd+') — '+x.career+' / '+x.assignment+'; qualificação '+(x.qualificationTotal??'-')+'; sobrevivência '+(x.survivalTotal??'-')+'; progressão '+(x.advancementTotal??'-')+'; rank '+(x.rankTitle??x.rank??'-')+'; '+(x.event||'')+(x.mishap?' Incidente: '+x.mishap:'')));
 const A=c.attributes||{};out.str=num(A.STR);out.dex=num(A.DEX);out.end=num(A.END);out.int=num(A.INT);out.edu=num(A.EDU);out.soc=num(A.SOC);if(A.PSI!==undefined)out.psi=num(A.PSI);
 const other=[];for(const [name,val] of Object.entries(c.skills||{})){const dst=TRAV_SKILLS[name];if(dst)out[dst]=num(val);else other.push(name+' '+val);}out.other_skills=lines(other);
 out.contacts=lines((c.relations||[]).map(r=>(r.name||'Sem nome')+' — '+(r.type||'Relação')+(r.note?' — '+r.note:'')));out.benefits=lines(c.benefits||[]);out.finances=lines(['Créditos: '+(c.credits??0),'Pensão: '+(c.pension??0),'Dívida médica: '+(c.medicalDebt??0)]);out.ship='Ship Shares: '+(c.shipShares??0);
 const fmt=(arr,label)=>(arr||[]).map(x=>label+': '+text(x?.name||x?.id)+(x?.quantity!==undefined?' x'+x.quantity:'')+(x?.mass!==undefined?' — massa '+x.mass:''));out.equipment=lines([...fmt(c.weapons,'Arma'),...fmt(c.armor,'Armadura'),...fmt(c.gear,'Item')]);
 out.notes=lines([c.injuries?.length?'Ferimentos: '+c.injuries.map(x=>typeof x==='string'?x:text(x?.name||JSON.stringify(x))).join('; '):'',c.notes,c.portraitPrompt?'Retrato: '+c.portraitPrompt:'',...(c.creationLog||[]).map(x=>x.label+': '+x.detail+(x.manualReference?' ['+x.manualReference+']':'')),p.ruleset?.title?'Fonte: '+p.ruleset.title:'']);out._externalSource={format:p.format,version:p.version??null,profile:p.ruleset?.system};return {system:'traveller',sourceLabel:'Traveller 2ª edição',character:out};
}
function mapGenericSheet(p){const profile=String(p.system_id||p.rules_profile||'').toLowerCase(),c=p.character||{};if(profile.includes('pathfinder')||profile==='pf2e')return mapPf2(p);if(!profile&&c.class_name&&c.species&&c.background&&(c.saving_throws||c.spellcasting||c.proficiency_bonus))return mapDndSheet(p);if(profile.includes('dnd')||profile.includes('5e'))return mapDndSheet(p);return null;}
function map(payload){
 if(!payload||typeof payload!=='object')return null;
 if(payload.schema==='braseiro.multisheet.v3')return {native:true,payload};
 if((payload.format==='braseiro-tales-kid'||payload.profile==='tales_kid')&&payload.kid)return mapTales(payload);
 if((payload.format==='BRASEIRO_WWN_CHARACTER'||payload.system==='WWN')&&payload.character)return mapWwn(payload);
 if(payload.format==='braseiro-character'&&(payload.systemProfileId==='the-one-ring-2e'||payload.character?.systemProfileId==='the-one-ring-2e'))return mapTor(payload);
 if(payload.format==='braseiro-forbidden-lands-character'&&payload.character)return mapForbidden(payload);
 if(payload.format==='braseiro-traveller-character'&&payload.payload)return mapTraveller(payload);
 if(payload.format==='braseiro-character-sheet'&&payload.character)return mapGenericSheet(payload);
 return null;
}
globalThis.BRASEIRO_IMPORT_ADAPTERS=Object.freeze({map,mapTales,mapWwn,mapDndSheet,mapTor,mapPf2,mapForbidden,mapTraveller});
})();