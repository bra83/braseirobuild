import type { EncounterCombatant } from '../types';

/** Guia do Mestre, encontros #7 e #8, pp. 150–151, e Urhur típico p. 64.
 * Fichas incorporadas, sem inventar bônus ou substituir por monstros genéricos.
 * Os 12 figurantes do massacre são escravagistas reais; não há feridos reais.
 */
export class OfficialEncounterNpcFactory {
  private static d6(rng:()=>number):number {
    const value=rng();
    if(!Number.isFinite(value)||value<0||value>=1)throw new Error('Fonte de dados inválida.');
    return 1+Math.floor(value*6);
  }
  private static initiative(rng:()=>number):number {
    const value=rng();
    if(!Number.isFinite(value)||value<0||value>=1)throw new Error('Fonte de iniciativa inválida.');
    return 1+Math.floor(value*10);
  }
  private static npc(name:string,entityId:string,attributes:Record<'strength'|'agility'|'wits'|'empathy',number>,
    skills:Record<string,number>,weapons:Record<string,unknown>[],armor:Record<string,unknown>[],
    armorRating:number,talents:Record<string,number>,notes:string,rng:()=>number,
    extras:Record<string,unknown>={}):EncounterCombatant {
    const current=Object.fromEntries(Object.entries(attributes).map(([key,value])=>[key,{current:value,max:value}]));
    return {
      id:`npc:${entityId}:${crypto.randomUUID()}`,entityId,name,kind:'npc',side:'enemy',
      initiative:this.initiative(rng),currentStrength:attributes.strength,maxStrength:attributes.strength,
      armor:armorRating,conditions:[],fastActionAvailable:true,slowActionAvailable:true,
      status:'active',notes,
      quickSheet:{attributes:current,skills,weapons,armor,talents,standardNpcCombat:true,
        source:'Guia do Mestre, encontros oficiais',...extras},
    };
  }
  /** Individual stats are absent in #2/#17. These are explicitly adopted
   * published TYPICAL profiles, never claimed as the named artists' own sheet.
   */
  static adoptedSocialProfiles(id:2|17,orcCount:number,humanCount:number,rng:()=>number=Math.random):EncounterCombatant[]{
    if(id===2){
      const elf=this.npc('Heldemin','official-adopted-heldemin',
        {strength:3,agility:4,wits:3,empathy:2},{Luta:2,Resiliência:2,Furtividade:3,Pontaria:2,Patrulha:2,Sobrevivência:2},
        [{name:'Lança curta',damage:1,damageType:'puncture'},{name:'Arco longo',damage:1,damageType:'puncture'},{name:'Faca',damage:1,damageType:'slash'}],
        [],0,{},'Perfil típico Elfo Inquieto p.54 adotado explicitamente pela Mestra; a entrada #2 não publica ficha individual.',rng,
        {adoptedTypicalProfile:'restless_elf',sourcePrintedPage:54,individualStatisticsPublished:false});
      const orc=this.orcEscort(1,'Roka',rng)[0];
      return[elf,{...orc,name:'Alachran',entityId:'official-adopted-alachran',quickSheet:{...(orc.quickSheet||{}),adoptedTypicalProfile:'roka_orc',sourcePrintedPage:65,individualStatisticsPublished:false}}];
    }
    if(!Number.isInteger(orcCount)||!Number.isInteger(humanCount)||orcCount<1||humanCount<1||orcCount+humanCount>24)
      throw new Error('Informe pelo menos um convidado orc e um humano; máximo 24 participantes explicitamente escolhidos.');
    const actors:EncounterCombatant[]=[];
    for(let i=0;i<orcCount;i++){
      const orc=this.orcEscort(1,'Roka',rng)[0];actors.push({...orc,name:`Convidado orc ${i+1}`,entityId:`official-adopted-wedding-orc:${i+1}`,
        quickSheet:{...(orc.quickSheet||{}),adoptedTypicalProfile:'roka_orc',sourcePrintedPage:65,individualStatisticsPublished:false}});
    }
    for(let i=0;i<humanCount;i++){
      const soldier=this.merchantGuard('Ginda',1,{melee:'broad_sword',ranged:'long_bow',armor:'leather'},rng);
      actors.push({...soldier,name:`Convidado humano ${i+1}`,entityId:`official-adopted-wedding-human:${i+1}`,
        quickSheet:{...(soldier.quickSheet||{}),adoptedTypicalProfile:'human_soldier',sourcePrintedPage:183,individualStatisticsPublished:false}});
    }
    return actors;
  }
  static slavers(rng:()=>number=Math.random):EncounterCombatant[] {
    // A descrição especifica uma dúzia de humanos no massacre encenado.
    return Array.from({length:12},(_,index)=>{
      const harwa=index===0;
      const weapon=index%2===0?{name:'Porrete',damage:1,damageType:'blunt'}:{name:'Maça',damage:2,damageType:'blunt'};
      const copper=this.d6(rng);
      return this.npc(harwa?'Harwa — líder dos escravagistas':`Escravagista ${index+1}`,
        `official-slaver:${index+1}`,
        {strength:3,agility:2,wits:2,empathy:2},{Luta:2},[weapon],
        [{name:'Couro',rating:2}],2,{},
        'Capturar vivos, não matar. Fugitivos não são perseguidos. Ferimentos encenados.',rng,
        {coins:{copper,silver:0,gold:0},captureIntent:true,noPursuitOnEscape:true,
          ...(harwa?{equipment:[{name:'Trombeta de bronze'}],ambushSignal:'trombeta'}:{})});
    });
  }
  /** Encontro #12: Argam e exatamente tantos bandidos quanto aventureiros.
   * Guia do Mestre, p. 152. Armas são alternativas do perfil publicado.
   */
  static hungryBandits(partySize:number,rng:()=>number=Math.random):EncounterCombatant[] {
    if(!Number.isInteger(partySize)||partySize<1||partySize>30)
      throw new Error('Tamanho da companhia inválido para os assaltantes famintos.');
    return Array.from({length:partySize},(_,index)=>this.npc(
      index===0?'Argam Duasbarbas — líder dos assaltantes':`Assaltante faminto ${index+1}`,
      `official-hungry-bandit:${index+1}`,
      {strength:3,agility:3,wits:3,empathy:2},
      {Luta:3,Furtividade:2,Patrulha:1,Sobrevivência:2},
      [{name:index%3===0?'Espada Larga':index%3===1?'Machadinha':'Porrete',damage:index%3===2?1:2,damageType:index%3===2?'blunt':'slash'}],
      [],0,{},'Faminto, cansado; pode aceitar comida e histórias. Não presumir hostilidade sem escolha.',rng,
      {coins:{copper:this.d6(rng),silver:0,gold:0},leader:index===0,sourcePrintedPage:152}
    ));
  }

  /** Guia do Mestre encontro #3: grupo 2× party; o clã NÃO é definido.
   * A Mestra escolhe explicitamente uma das fichas típicas publicadas.
   * Sargah é prisioneiro sem ficha publicada, não um combatente genérico.
   */
  static orcEscort(partySize:number,clan:'Roka'|'Urhur'|'Isir',rng:()=>number=Math.random):EncounterCombatant[] {
    if(!Number.isInteger(partySize)||partySize<1||partySize>30)
      throw new Error('Tamanho da party inválido para escolta orc.');
    if(!['Roka','Urhur','Isir'].includes(clan))throw new Error('Clã orc precisa ser escolhido explicitamente pela Mestra.');
    const profiles={
      Roka:{attributes:{strength:5,agility:2,wits:2,empathy:2},skills:{Potência:2,Luta:2,Patrulha:1,Sobrevivência:1},weapon:{name:'Mangual',damage:2,damageType:'blunt'},armor:3},
      Urhur:{attributes:{strength:4,agility:3,wits:3,empathy:2},skills:{Potência:2,Luta:2,Tradição:1},weapon:{name:'Cimitarra',damage:2,damageType:'slash'},armor:3},
      Isir:{attributes:{strength:4,agility:3,wits:3,empathy:2},skills:{Potência:2,Luta:3,Pontaria:2,Sobrevivência:2},weapon:{name:'Montante',damage:3,damageType:'slash'},armor:3},
    } as const;
    const profile=profiles[clan];
    return Array.from({length:partySize*2},(_,index)=>this.npc(
      `Escolta ${clan} ${index+1}`,`official-orc-escort:${index+1}`,
      {...profile.attributes},{...profile.skills},[{...profile.weapon}],
      [{name:'Couro Batido',rating:profile.armor}],profile.armor,{},
      `Clã ${clan} escolhido pela Mestra; escolta Sargah amarrado. Não presumir agressão sem provocação.`,rng,
      {clan,escortPrisoner:'Sargah',sourcePrintedPage:clan==='Isir'?66:65,
        ...(clan==='Isir'?{venomousBitePending:true,helmet:'Elmo fechado'}:{})}
    ));
  }
  static sargahPrisoner(rng:()=>number=Math.random):EncounterCombatant {
    const prisoner=this.npc('Sargah — fugitivo amarrado','official-sargah',
      {strength:0,agility:0,wits:0,empathy:0},{},[],[],0,{},
      'Prisioneiro amarrado. Zero é marcador de ficha não definida, NÃO dano. Sem ação, sem ataque.',rng,
      {prisoner:true,restrained:true,statisticsUndetermined:true,sourcePrintedPage:149});
    return {...prisoner,side:'neutral',status:'yielded',fastActionAvailable:false,slowActionAvailable:false};
  }
  /** Encontro #29 (Guia do Mestre pp.159 e 106): D6+3 esqueletos.
   * Armadura é opção publicada, escolhida pela Mestra, não sorteada sem fonte.
   * Ossudo é aplicado no commit real de ataque em EncounterService.
   */
  static skeletonAmbush(armorChoice:'leather'|'mail',rng:()=>number=Math.random):EncounterCombatant[] {
    if(!['leather','mail'].includes(armorChoice))throw new Error('Escolha couro batido ou cota de malha para os esqueletos.');
    const count=this.d6(rng)+3;const armorRating=armorChoice==='mail'?6:3;
    return Array.from({length:count},(_,index)=>this.npc(
      `Esqueleto guerreiro ${index+1}`,`official-skeleton:${index+1}`,
      {strength:3,agility:2,wits:0,empathy:0},{Luta:3,Patrulha:3},
      [{name:'Espada larga enferrujada',damage:2,damageType:'slash'}],
      [{name:armorChoice==='mail'?'Cota de malha':'Couro batido',rating:armorRating},
       {name:'Elmo fechado',rating:0}],armorRating,{},
      'Ossudo: flechas e armas pontiagudas causam no máximo 1 de dano por ataque. Não possui Astúcia/Empatia; perseguirá apenas enquanto o grupo estiver à vista.',rng,
      {officialSkeleton:true,boneDamageCap:1,missingMentalAttributes:true,armorChosenByGm:armorChoice,sourcePrintedPage:106}
    ));
  }
  static khardnal(rng:()=>number=Math.random):EncounterCombatant {
    return this.npc('Khardnal — Grande Insultador','official-khardnal',
      {strength:4,agility:3,wits:3,empathy:2},
      {Potência:2,Luta:2,Tradição:1},
      [{name:'Machadinha',damage:2,damageType:'slash'}],
      [{name:'Couro Batido',rating:3},{name:'Escudo Grande',rating:0}],
      3,{Berserker:1},
      'Urhur típico com Berserker; provoca, mas não ataca primeiro. Uma vez atacado, luta até a morte.',rng,
      {clan:'Urhur',behavior:'retaliates_if_attacked',equipmentChoice:'machado_e_escudo'});
  }

  /** R88 #29 p.159 remete à ficha típica de esqueleto do bestiário p.106.
   * A armadura pode ser couro batido ou cota de malha; o encontro não fixa a
   * distribuição, portanto a MdJ informa quantos usam cota.
   */
  static skeleton(rng:()=>number=Math.random,armorType:'leather'|'chainmail'='leather'):EncounterCombatant {
    const id=crypto.randomUUID();
    const chainmail=armorType==='chainmail';
    const armor=chainmail?'Cota de Malha':'Couro Batido';
    return {
      id:`npc:official-r88-skeleton:${id}`,entityId:`official-r88-skeleton:${id}`,
      name:'Esqueleto',kind:'npc',side:'enemy',initiative:this.initiative(rng),currentStrength:3,maxStrength:3,
      armor:0,conditions:[],fastActionAvailable:true,slowActionAvailable:true,status:'active',
      notes:`Guia do Mestre R88 p.106: esqueleto — Força 3, Agilidade 2, Luta 3, Patrulha 3; espada larga enferrujada, ${armor.toLocaleLowerCase('pt-BR')} e elmo fechado.`,
      quickSheet:{attributes:{strength:{current:3,max:3},agility:{current:2,max:2}},skills:{Luta:3,Patrulha:3},
        weapons:[{name:'Espada larga enferrujada',damage:2,damageType:'slash'}],
        armorEquipment:[{name:armor},{name:'Elmo Fechado'}],
        armor:0,standardNpcCombat:true,
        source:'Guia do Mestre R88, encontro #29 p.159 e esqueleto p.106',sourcePrintedPage:106,
        officialR88Skeleton:true,armorRatingSourceMissing:true,skeletonArmorType:armorType,
        arrowsAndStabbingWeaponsDamageMax:1},
    };
  }

  static skeletons(count:number,rng:()=>number=Math.random,chainmailCount=0):EncounterCombatant[] {
    if(!Number.isInteger(count)||count<4||count>9)throw new Error('A quantidade oficial do esqueleto precisa ser D6+3, entre 4 e 9.');
    if(!Number.isInteger(chainmailCount)||chainmailCount<0||chainmailCount>count)
      throw new Error('A quantidade de esqueletos com cota de malha precisa ficar entre zero e o total.');
    return Array.from({length:count},(_,index)=>this.skeleton(rng,index<chainmailCount?'chainmail':'leather'));
  }

  /** R88 #30 uses Teramalda's own attributes and the death-knight attack table p.76. */
  static teramalda(deathKnight:EncounterCombatant):EncounterCombatant {
    const attributes={strength:{current:12,max:12},agility:{current:4,max:4},wits:{current:3,max:3},empathy:{current:2,max:2}};
    return {...deathKnight,id:`monster:official-teramalda:${crypto.randomUUID()}`,entityId:'official-teramalda',name:'Teramalda',
      currentStrength:12,maxStrength:12,armor:0,
      notes:'Guia do Mestre R88 p.160 e Cavaleiro da Morte p.76. Força 12, Agilidade 4, Astúcia 3, Empatia 2, Patrulha 2, montante. Imune a todo dano; só morre se imobilizada e o espigão for retirado com Potência Formidável (−3), ação lenta.',
      quickSheet:{...(deathKnight.quickSheet||{}),category:'Cavaleira da Morte · encontro oficial #30',attributes,skills:{Patrulha:2},
        weapons:[{name:'Montante',damage:3,damageType:'slash'}],officialTeramalda:true,physicalDamageImmune:true,
        armorRatingIrrelevantBecauseImmune:true,
        source:'Guia do Mestre R88, encontro #30 p.160; tabela de Cavaleiro da Morte p.76',sourcePrintedPage:160,
        spikeRemovalRequiresImmobilized:true,spikeRemovalModifier:-3,spikeRemovalSkill:'Potência',spikeRemovalAction:'slow'}};
  }

  /** Guia do Mestre, encontro #18, p. 155: vinte massas formam uma criatura.
   * Ela tem somente Força (número de massas / 5, arredondado para cima), sem perícias,
   * e seu ataque causa 1 de dano perfurante. Não atribuir atributos ausentes.
   */
  static demonicDough(masses=20,rng:()=>number=Math.random):EncounterCombatant {
    if(!Number.isInteger(masses)||masses<1||masses>200)
      throw new Error('Quantidade de massas inválida para a massa demoníaca.');
    const strength=Math.ceil(masses/5);
    const initiative=this.initiative(rng);
    return {
      id:`npc:official-demonic-dough:${crypto.randomUUID()}`,entityId:'official-demonic-dough',
      name:`Massa demoníaca (${masses} massas)`,kind:'npc',side:'enemy',initiative,
      currentStrength:strength,maxStrength:strength,armor:0,conditions:[],fastActionAvailable:true,
      slowActionAvailable:true,status:'active',notes:'Guia do Mestre p. 155: criatura de massas; somente Força, sem perícias.',
      quickSheet:{attributes:{strength:{current:strength,max:strength}},skills:{},
        weapons:[{name:'Massa demoníaca',damage:1,damageType:'puncture'}],
        attacks:[{name:'Massa demoníaca',damage:1,damageType:'puncture'}],
        standardNpcCombat:true,source:'Guia do Mestre, encontro #18, p. 155',
        doughMasses:masses,sourcePrintedPage:155},
    };
  }

  /** Guia do Mestre, encontro #20, p. 156; ficha e ataques dos Ents na p. 89.
   * Cinco árvores vivas carregam o corpo do sexto Ent. Não criar ficha para os
   * elfos ocultos: a entrada não informa sua quantidade nem suas estatísticas.
   */
  static processionEnt(ordinal=1,rng:()=>number=Math.random):EncounterCombatant {
    if(!Number.isInteger(ordinal)||ordinal<1||ordinal>5)
      throw new Error('Posição inválida para os cinco Ents da procissão.');
    const attackTable=[
      {roll:1,name:'Rugido aterrorizante',baseDice:7,damage:0,damageType:'other',tags:['fear','area'],range:'near'},
      {roll:2,name:'Golpe devastador',baseDice:8,damage:1,damageType:'blunt',tags:['two_targets','prone'],range:'near'},
      {roll:3,name:'Raízes nodosas',baseDice:9,damage:1,damageType:'blunt',tags:['ent_roots'],range:'near'},
      {roll:4,name:'Abraço esmagador',baseDice:10,damage:1,damageType:'blunt',tags:[],range:'near'},
      {roll:5,name:'Abraço do Ent',baseDice:8,damage:1,damageType:'blunt',tags:['ent_embrace'],range:'near'},
      {roll:6,name:'Arremessar pedra',baseDice:12,damage:2,damageType:'blunt',tags:['prone'],range:'short'},
    ];
    const attributes={strength:16,agility:3,wits:5,empathy:3};
    const current=Object.fromEntries(Object.entries(attributes).map(([key,value])=>[key,{current:value,max:value}]));
    return {
      id:`monster:official-procession-ent:${ordinal}:${crypto.randomUUID()}`,
      entityId:`official-procession-ent:${ordinal}`,name:`Ent da procissão ${ordinal}`,
      kind:'monster',side:'enemy',initiative:this.initiative(rng),currentStrength:16,maxStrength:16,
      armor:6,conditions:[],fastActionAvailable:true,slowActionAvailable:true,status:'active',
      notes:'R88, Guia do Mestre pp. 89 e 156. A procissão foi atacada; os Ents pousaram o corpo e lutam, mas não querem matar se puderem evitar. Ataques contra a companhia são narrados sem presumir a destruição de itens ou ferimentos específicos.',
      quickSheet:{category:'Monstro · Encontro oficial #20',attributes:current,skills:{},armor:6,
        movement:1,attacks:attackTable.map(({roll,name,baseDice,damage,damageType,tags,range})=>({roll,name,dice:`${baseDice} Dados Base`,damage,damageType,tags,range})),
        monsterAttackTable:attackTable,standardNpcCombat:true,
        source:'Guia do Mestre R88, encontro #20 p. 156; Ent p. 89',sourcePrintedPage:89,
        physicalDamageImmune:true,fireDamageMultiplier:2,officialProcessionEnt:true},
    };
  }

  static processionEnts(rng:()=>number=Math.random):EncounterCombatant[] {
    return Array.from({length:5},(_,index)=>this.processionEnt(index+1,rng));
  }

  /** Guia do Mestre R88, encontro #21 p.156 e Fantasma p.90 / tabela p.91. */
  static vengefulGhostGarmagol(rng:()=>number=Math.random):EncounterCombatant {
    const attackTable=[
      {roll:1,name:'Golpe Fantasma',baseDice:8,damage:2,damageType:'blunt',tags:['prone','ghost_throw_near'],range:'gm_confirm'},
      {roll:2,name:'Toque da Morte',baseDice:9,damage:0,damageType:'other',tags:['fear','cold'],range:'gm_confirm'},
      {roll:3,name:'Grito Fantasma',baseDice:7,damage:0,damageType:'other',tags:['fear','cold','area'],range:'near'},
      {roll:4,name:'Visão da Morte',baseDice:10,damage:0,damageType:'other',tags:['fear','prone'],range:'gm_confirm'},
      {roll:5,name:'Abraço Fantasma',baseDice:10,damage:2,damageType:'blunt',tags:['cold'],range:'gm_confirm'},
      {roll:6,name:'Golpe Gélido',baseDice:12,damage:1,damageType:'blunt',tags:['cold'],range:'gm_confirm'},
    ];
    const attributes={strength:8,agility:3,wits:3,empathy:2};
    const current=Object.fromEntries(Object.entries(attributes).map(([key,value])=>[key,{current:value,max:value}]));
    return {
      id:`monster:official-vengeful-ghost:${crypto.randomUUID()}`,entityId:'official-vengeful-ghost:garmagol',
      name:'Garmagol — comandante amiense vingativo',kind:'monster',side:'enemy',initiative:this.initiative(rng),
      currentStrength:8,maxStrength:8,armor:0,conditions:[],fastActionAvailable:true,slowActionAvailable:true,status:'active',
      notes:'Guia do Mestre R88 pp. 90–91: só pode ser ferido por fogo ou magia; quando derrotado, é banido por um Quarto de Dia e retorna. Somente Purgar Morto-vivo o destrói.',
      quickSheet:{category:'Fantasma · encontro oficial #21',attributes:current,skills:{},armor:0,movement:1,
        attacks:attackTable.map(({roll,name,baseDice,damage,damageType,tags,range})=>({roll,name,dice:`${baseDice} Dados Base`,damage,damageType,tags,range})),
        monsterAttackTable:attackTable,standardNpcCombat:true,source:'Guia do Mestre R88, encontro #21 p.156; Fantasma pp.90–91',
        sourcePrintedPage:90,officialBookMonster:true,officialVengefulGhost:true,physicalDamageImmune:true,
        destroyedOnlyByPurgeUndead:true,returnsAfterQuarterDay:true},
    };
  }

  /** Guia do Mestre R88, encontro #23 p.157 e Troll pp.114–115. */
  static encounterTroll(name:'Ghormald'|'Karg',strength:number,rng:()=>number=Math.random):EncounterCombatant {
    if(!Number.isInteger(strength)||strength<12||strength>16)
      throw new Error('Força de troll fora da faixa oficial 12–16 da p.114.');
    const attackTable=[
      {roll:1,name:'Ataque Dilacerador',baseDice:9,damage:1,damageType:'slash',tags:['disease'],diseaseVirulence:6,range:'gm_confirm'},
      {roll:2,name:'Mordida Terrível',baseDice:8,damage:2,damageType:'blunt',tags:['grapple'],range:'gm_confirm'},
      {roll:3,name:'Arremesso de Troll',baseDice:12,damage:1,damageType:'blunt',tags:['prone','troll_throw_short'],range:'gm_confirm'},
      {roll:4,name:'Ataque em Área',baseDice:8,damage:1,damageType:'blunt',tags:['area'],range:'arms_reach'},
      {roll:5,name:'Boa Surra',baseDice:10,damage:1,damageType:'blunt',tags:['prone','troll_followup_attack'],range:'gm_confirm'},
      {roll:6,name:'Hálito Fedorento',baseDice:7,damage:0,damageType:'empathy',tags:['area','damage_empathy'],range:'near'},
    ];
    const attributes={strength,agility:2};
    const current=Object.fromEntries(Object.entries(attributes).map(([key,value])=>[key,{current:value,max:value}]));
    return {
      id:`monster:official-troll:${crypto.randomUUID()}`,entityId:`official-encounter-troll:${name.toLowerCase()}`,
      name:`${name} — troll`,kind:'monster',side:'enemy',initiative:this.initiative(rng),currentStrength:strength,
      maxStrength:strength,armor:4,conditions:[],fastActionAvailable:true,slowActionAvailable:true,status:'active',
      notes:'Ficha de troll da MdJ R88 p.114: a MdJ escolheu Força dentro de 12–16. Regeneração, fedor, luz solar e efeitos não cobertos pela resolução atual permanecem visíveis para adjudicação.',
      quickSheet:{category:`Troll · encontro oficial ${name==='Karg'?'#35':'#23'}`,attributes:current,skills:{Patrulha:4},armor:4,movement:1,
        attacks:attackTable.map(({roll,name,baseDice,damage,damageType,tags,range})=>({roll,name,dice:`${baseDice} Dados Base`,damage,damageType,tags,range})),
        monsterAttackTable:attackTable,standardNpcCombat:true,source:`Guia do Mestre R88, encontro #${name==='Karg'?35:23} p.${name==='Karg'?162:157}; Troll pp.114–115`,
        sourcePrintedPage:114,officialBookMonster:true,officialEncounterTroll:true,officialEncounterId:name==='Karg'?35:23,
        trollRegenerationPerRound:1,trollStenchAgilityDamage:1,trollSunlightStrengthDamage:1},
    };
  }

  /** R88 #33 p.161 and Minotaur p.104. Keep this profile independent from the
   * legacy Bestiary import, which belongs to a different source/version. */
  static minotaurMarkut(rng:()=>number=Math.random):EncounterCombatant {
    const attackTable=[
      {roll:1,name:'Punho de Touro',baseDice:8,damage:1,damageType:'blunt',tags:[],range:'arms_reach'},
      {roll:2,name:'Coice',baseDice:9,damage:1,damageType:'blunt',tags:['prone','push_to_near'],pushTargetRange:'near',range:'arms_reach'},
      {roll:3,name:'Investida de Chifres',baseDice:10,damage:2,damageType:'puncture',tags:[],range:'gm_confirm'},
      {roll:4,name:'Corte Trespassante',baseDice:10,damage:2,damageType:'slash',tags:['armor_ignores_3','blockable'],armorIgnore:3,blockable:true,range:'gm_confirm'},
      {roll:5,name:'Ataque em Área',baseDice:8,damage:2,damageType:'slash',tags:['area'],range:'arms_reach'},
      {roll:6,name:'Pisotear',baseDice:12,damage:1,damageType:'blunt',tags:['prone'],range:'arms_reach'},
    ];
    const combatant=this.npc('Markut — minotauro','official-r88-minotaur-markut',
      {strength:10,agility:4,wits:2,empathy:2},{Patrulha:3},
      [{name:'Machado de Duas Mãos',damage:2,damageType:'slash'}],
      [{name:'Pele',rating:2}],2,{},
      'Guia do Mestre R88 p.104: Força 10, Agilidade 4, Astúcia 2, Empatia 2, Patrulha 3, armadura 2, movimento 1. Parceira Kaun morreu na luta contra a patrulha; Markut não recua.',rng,
      {category:'Minotauro · encontro oficial #33',attributes:{strength:{current:10,max:10},agility:{current:4,max:4},wits:{current:2,max:2},empathy:{current:2,max:2}},
        armor:2,movement:1,monsterAttackTable:attackTable,officialBookMonster:true,officialR88Minotaur:true,
        source:'Guia do Mestre R88, encontro #33 p.161 e Minotauro p.104',sourcePrintedPage:104,
        minotaurPartner:'Kaun',minotaurWontRetreat:true,
        manualEffectsNotAppliedByCombatEngine:['push_to_near','armor_ignores_3','blockable']});
    return {...combatant,kind:'monster',currentStrength:10,maxStrength:10,armor:2,
      quickSheet:{...(combatant.quickSheet||{}),standardNpcCombat:true}};
  }

  /** The encounter states exactly four surviving soldiers and their complete
   * profile. The caller assigns them to the side the party supports. */
  static minotaurEncounterSoldiers(rng:()=>number=Math.random):EncounterCombatant[] {
    return Array.from({length:4},(_,index)=>this.npc(`Soldado sobrevivente ${index+1}`,`official-r88-minotaur-soldier:${index+1}`,
      {strength:4,agility:3,wits:2,empathy:2},{Luta:2},
      [{name:'Espada larga',damage:2,damageType:'slash'}],
      [{name:'Escudo pequeno',rating:0},{name:'Couro batido',rating:3}],3,{},
      'Guia do Mestre R88, encontro #33 p.161: Força 4, Agilidade 3, Astúcia 2, Empatia 2, Luta 2, espada larga, escudo pequeno e armadura de couro batido.',rng,
      {category:'Soldado · encontro oficial #33',source:'Guia do Mestre R88, encontro #33 p.161',sourcePrintedPage:161,
        officialR88MinotaurEncounterSoldier:true,fallenBannerRecoveryPending:true}));
  }

  /** R88 encounter #37, printed p.164. The six dwarves use the single
   * published warrior profile; the combat bridge controls when the latter
   * three arrive, after the saved D6-round delay. */
  static roadBlockadeDwarves(startOrdinal=1,count=3,rng:()=>number=Math.random):EncounterCombatant[] {
    if(!Number.isInteger(startOrdinal)||startOrdinal<1||startOrdinal>6||!Number.isInteger(count)||count<1||startOrdinal+count-1>6)
      throw new Error('Grupo de guerreiros anões inválido para o bloqueio R88.');
    return Array.from({length:count},(_,offset)=>{
      const ordinal=startOrdinal+offset;
      return this.npc(`Guerreiro anão ${ordinal}`,`official-r88-road-blockade-dwarf:${ordinal}`,
        {strength:4,agility:3,wits:2,empathy:2},
        {Luta:3,Movimentação:2,Pontaria:3,Discernimento:4},
        [{name:'Machado de batalha',damage:2,damageType:'slash'}],
        [{name:'Couro Batido',rating:3}],3,{},
        'Guia do Mestre R88, encontro #37 p.164: Força 4, Agilidade 3, Astúcia 2, Empatia 2, Luta 3, Movimentação 2, Pontaria 3 e Discernimento 4; machado de batalha, couro batido e besta leve.',rng,
        {sourcePrintedPage:164,officialR88RoadBlockadeDwarf:true,lightCrossbow:true,
          rangedWeapon:{name:'Besta leve',damage:1,damageType:'puncture'}});
    });
  }

  /** R88 #41 p.165 / typical soldier p.183. Each sister's equipment is an
   * explicit table choice because the encounter does not select the printed
   * alternatives. */
  static merchantGuard(name:'Ginda'|'Aneta',ordinal:1|2,loadout:{melee:'broad_sword'|'battle_axe';ranged:'long_spear'|'long_bow'|'heavy_crossbow';armor:'leather'|'chainmail'},rng:()=>number=Math.random):EncounterCombatant {
    const melee=loadout.melee==='broad_sword'?{name:'Espada larga',damage:2,damageType:'slash'}:{name:'Machado de batalha',damage:2,damageType:'slash'};
    const ranged=loadout.ranged==='long_spear'?{name:'Lança longa',damage:2,damageType:'puncture'}:
      loadout.ranged==='long_bow'?{name:'Arco longo',damage:1,damageType:'puncture'}:{name:'Besta pesada',damage:2,damageType:'puncture'};
    const armorName=loadout.armor==='chainmail'?'Cota de malha':'Couro';
    const armorRating=loadout.armor==='chainmail'?6:2;
    return this.npc(name,`official-r88-soul-merchant-guard:${ordinal}`,
      {strength:4,agility:3,wits:3,empathy:2},{Resiliência:1,Luta:2,Pontaria:2,Sobrevivência:1},
      [melee,ranged], [{name:'Escudo grande',rating:0},{name:armorName,rating:armorRating},{name:'Elmo aberto',rating:0}],
      armorRating,{},'Soldado típico do Guia do Mestre R88 p.183; protetora das mercadorias de Vergevil. Equipamento alternativo escolhido pela Mestra.',rng,
      {sourcePrintedPage:183,officialR88SoulMerchantGuard:true,loadout:{...loadout},rangedWeapon:ranged});
  }

  /** R88 #42 pp.165–166. Number is party size plus a previously rolled D6. */
  static saurianHunters(partySize:number,bonusD6:number,spear:'trident'|'long_spear',rng:()=>number=Math.random):EncounterCombatant[] {
    if(!Number.isInteger(partySize)||partySize<1||partySize>30||!Number.isInteger(bonusD6)||bonusD6<1||bonusD6>6)
      throw new Error('Quantidade dos caçadores sáurios precisa ser tamanho do grupo + D6 (1–6).');
    const weapon=spear==='trident'?{name:'Tridente',damage:2,damageType:'puncture'}:{name:'Lança longa',damage:2,damageType:'puncture'};
    return Array.from({length:partySize+bonusD6},(_,index)=>this.npc(
      index===0?'Sheeerz — caçador sáurio':`Caçador sáurio ${index+1}`,
      `official-r88-saurian-hunter:${index+1}`,
      {strength:4,agility:3,wits:2,empathy:2},{Furtividade:2,Luta:2,Sobrevivência:2,Patrulha:2},
      [weapon,{name:'Porrete',damage:1,damageType:'blunt'}],[{name:'Escamas',rating:4}],4,{},
      'Guia do Mestre R88 pp.165–166: Sheeerz odeia humanos por causa dos caçadores que mataram seus pais; não é especialmente hostil às outras raças.',rng,
      {sourcePrintedPage:166,officialR88SaurianHunter:true,leader:index===0,spearChoice:spear,club:{name:'Porrete',damage:1,damageType:'blunt'}}
    ));
  }

  /** R88 #43 p.166: seven bounty hunters use their published common profile. */
  static bountyHunters(rng:()=>number=Math.random):EncounterCombatant[] {
    return Array.from({length:7},(_,index)=>this.npc(
      index===0?'Penazul — caçadora de recompensas':`Caçador de recompensas ${index+1}`,
      `official-r88-bounty-hunter:${index+1}`,
      {strength:2,agility:4,wits:3,empathy:2},
      {Luta:3,Movimentação:3,Pontaria:3,Patrulha:4,Sobrevivência:3,Discernimento:3},
      [{name:'Espada larga',damage:2,damageType:'slash'},{name:'Arco',damage:1,damageType:'puncture'}],
      [{name:'Couro batido',rating:3}],3,{},
      'Guia do Mestre R88 p.166: caçador de recompensas; Penazul capturou a família Ylendidra e dois algozes estão enterrando os cativos vivos.',rng,
      {sourcePrintedPage:166,officialR88BountyHunter:true,leader:index===0,rangedWeapon:{name:'Arco',damage:1,damageType:'puncture'}}
    ));
  }

  /** Guia do Mestre R88, encontro #24 p.158 / ogro típico p.61. */
  static encounterOgre(name:'Kurge'|'Gonlab',rng:()=>number=Math.random):EncounterCombatant {
    return this.npc(`${name} — ogro`, `official-encounter-ogre:${name.toLowerCase()}`,
      {strength:6,agility:2,wits:1,empathy:2},{Potência:4,Resiliência:2,Luta:2},
      [{name:'Grande clava',damage:2,damageType:'blunt'}],[],0,{},
      'Ogro típico do Guia do Mestre R88 p.61. Resistência mágica: reduza o Nível de Poder de qualquer magia lançada contra ele por D6.',rng,
      {sourcePrintedPage:61,officialEncounterOgre:true,magicResistanceDie:6,
        ...(name==='Kurge'?{prisoner:'Oker',capturedBrotherOfRust:true}:{prisoner:'filho de Engsle',capturedChild:true})});
  }
}
