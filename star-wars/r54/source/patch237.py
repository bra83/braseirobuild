from pathlib import Path
p=Path('app/src/main/assets/vtt/js/adventure/longArmOfTheHutt.mjs');s=p.read_text()
def rep(a,b):
 global s
 assert s.count(a)==1,(a[:70],s.count(a));s=s.replace(a,b)
rep("import { ensureLivingWorldDepth } from '../world/livingWorldDepth.mjs';", "import { ensureLivingWorldDepth } from '../world/livingWorldDepth.mjs';\nimport { FACTS,ARGUMENTS,guardCount,scoreDimmock,validateGeonosis,recordGeonosisChoice } from './swe02GeonosisP237.mjs';")
rep("choices:[{id:'finish-ship-inspection',label:'Prosseguir até Nabat',to:'ryloth-safehouse'}]", "choices:[{id:'finish-ship-inspection',label:'Prosseguir até Nabat',to:'ryloth-safehouse'},\n        {id:'sivor-shell',label:'Guardar a carapaça do porão três (SWE02 p.7)',to:'krayt-fang-logs'}]")
rep("      visibleNpcIds:[NPC.dromund],checks:[],", "      visibleNpcIds:[NPC.dromund],checks:[{skill:'Computadores',difficulty:'Difícil',purpose:'recuperar mensagens de Drombb para Thwheek',sourcePage:23}],")
rep("        {id:'new-meen-saved-confirmed',label:'Confirmar que Drombb e a ameaça foram neutralizados',to:'new-meen-secured',condition:'flag:drombbThreatNeutralized'},", "        {id:'new-meen-saved-confirmed',label:'Confirmar que Drombb e a ameaça foram neutralizados',to:'new-meen-secured',condition:'flag:drombbThreatNeutralized'},\n        {id:'drombb-messages',label:'Recuperar mensagens do escritório (SWE02 p.23)',to:'drombb-cantina',condition:'flag:geoDrombbCheckPassed'},")
rep("      visibleNpcIds:[NPC.piddock],checks:[{skill:'Enganação',difficulty:'Difícil',purpose:'recolher informações sem alertar os presentes'}],\n      choices:[{id:'speak-with-dimmock',label:'Buscar o apoio de Duke Dimmock',to:'dimmock-negotiation'}]", """      visibleNpcIds:[NPC.piddock],checks:[{skill:'Charme',difficulty:'Difícil',purpose:'conhecer os planos de droides de Teemo',sourcePage:34}],
      choices:[
        {id:'piddock-kubaz',label:'Perguntar por que Piddock rompeu com Teemo',to:'piddock-party',condition:'!flag:geo_kubaz'},
        {id:'piddock-b1',label:'Registrar planos de droides após Charme Difícil',to:'piddock-party',condition:'flag:geoPiddockCharmPassed'},
        {id:'piddock-identify-shell',label:'Pedir identificação da carapaça encontrada',to:'piddock-party',condition:'flag:geo_shell'},
        {id:'piddock-to-dimmock',label:'Ir à audiência pontuada de Dimmock',to:'dimmock-official-audience'},
        {id:'speak-with-dimmock',label:'Buscar o apoio de Duke Dimmock (rota legada)',to:'dimmock-negotiation'}]""")
rep("      choices:[{id:'geonosis-final-evidence',label:'Reunir os documentos antes do retorno',to:'geonosis-surface'}]", "      choices:[{id:'geonosis-final-evidence',label:'Reunir os documentos antes do retorno',to:'geonosis-surface'},\n        {id:'dimmock-audience',label:'Iniciar audiência oficial de Dimmock',to:'dimmock-official-audience'}]")
rep("    'rylos-escape-aftermath': {", """    'dimmock-official-audience':{
      episode:2,title:'Audiência oficial de Duke Dimmock',locationId:LOC.geonoSurface,
      summary:'SWE02 p.36: pontuação auditável com fatos, conduta e testes reais.',
      narration:'O duque exige fatos concretos e ouve cada argumento com cautela.',visibleNpcIds:[NPC.dimmock],
      checks:[{skill:'Charme',difficulty:'Média',purpose:'ser cortês com Dimmock',sourcePage:36},
        {skill:'Coerção',difficulty:'Formidável',purpose:'ameaçar o duque: falha custa dois pontos',sourcePage:36}],
      choices:[
        {id:'dimmock-anti-empire',label:'Declarar oposição ao Império (+1)',to:'dimmock-official-audience'},
        {id:'dimmock-loyal-empire',label:'Declarar lealdade ao Império (-1)',to:'dimmock-official-audience'},
        {id:'dimmock-rude',label:'Ser rude com o duque (-1)',to:'dimmock-official-audience'},
        {id:'dimmock-kubaz',label:'Apresentar espião Kubaz (+1)',to:'dimmock-official-audience',condition:'flag:geo_kubaz'},
        {id:'dimmock-b1',label:'Apresentar plano de droides B1 (+1)',to:'dimmock-official-audience',condition:'flag:geo_b1'},
        {id:'dimmock-shell',label:'Apresentar carapaça identificada (+1)',to:'dimmock-official-audience',condition:'flag:geo_identifiedShell'},
        {id:'dimmock-sivor',label:'Informar a morte de Sivor pelo Kubaz (+1)',to:'dimmock-official-audience',condition:'flag:geo_messages'},
        {id:'dimmock-messages',label:'Apresentar comunicações de Drombb (+1)',to:'dimmock-official-audience',condition:'flag:geo_messages'},
        {id:'dimmock-finish',label:'Concluir argumentos e ir ao gantry AA7',to:'gantry-aa7'}]
    },
    'gantry-aa7':{
      episode:2,title:'Gantry AA7 — Lucky Guess',locationId:LOC.geonoSurface,
      summary:'Cinco, três ou zero soldados segundo os pontos de Dimmock; Vrixx’tt chega em qualquer caso (SWE02 p.37).',
      narration:'Os contêineres formam barricadas ao lado do cargueiro. Um Gand observa a passarela.',
      visibleNpcIds:[],checks:[],choices:[
        {id:'board-lucky-guess',label:'Embarcar após combate, fuga ou resgate confirmado',to:'lucky-guess-boarding',condition:'flag:geoGantryCleared'},
        {id:'gantry-retreat',label:'Retirar-se sem vitória',to:'geonosis-surface'}]
    },
    'lucky-guess-boarding':{
      episode:2,title:'A bordo da Lucky Guess',locationId:LOC.geonoSurface,
      summary:'Os irmãos Vio mantêm a propriedade do cargueiro; destino Mos Shuuta (SWE02 p.37).',
      narration:'Os motores se acendem e a rampa do cargueiro se fecha.',visibleNpcIds:[],checks:[],
      choices:[{id:'lucky-guess-to-tatooine',label:'Seguir para Mos Shuuta',to:'tatooine-infiltration'}]
    },
    'rylos-escape-aftermath': {""")
rep("  if (c.startsWith('flag:')) return Boolean(progress.flags?.[c.slice(5)]);", "  if (c.startsWith('flag:')) return Boolean(progress.flags?.[c.slice(5)]);\n  if (c.startsWith('!flag:')) return !Boolean(progress.flags?.[c.slice(6)]);")
rep("    checkHistory: [],\n    consequencesApplied", "    checkHistory: [],geonosisEvidence:[],geonosisArguments:[],\n    consequencesApplied")
# in advance and status, two occurrences
assert s.count('  assertPalaceCausality(p);')==2
s=s.replace('  assertPalaceCausality(p);','  assertPalaceCausality(p);\n  validateGeonosis(p);')
rep("  if (!choice || !conditionOk(choice, p)) throw new Error('Escolha inválida.');", "  if (!choice || !conditionOk(choice, p)) throw new Error('Escolha inválida.');")
rep("  if (p.activeEncounterId && ['new-meen-night-raid','drombb-cantina','teemo-showdown'].includes(p.currentNodeId))", "  if (p.activeEncounterId && ['new-meen-night-raid','drombb-cantina','teemo-showdown','gantry-aa7'].includes(p.currentNodeId))")
rep("  p.flags = { ...(p.flags || {}), ...(choice.effects || {}) };", "  p.flags = { ...(p.flags || {}), ...(choice.effects || {}) };\n  recordGeonosisChoice(p,choiceId,p.currentNodeId);")
rep("    choices: (node.choices || []).filter(c => conditionOk(c, progress)).map(c => ({ id: c.id, label: c.label }))", """    choices: (node.choices || []).filter(c => conditionOk(c, progress) &&
      !(FACTS[c.id]&&(progress.geonosisEvidence||[]).some(x=>x.choiceId===c.id)) &&
      !(ARGUMENTS[c.id]&&(progress.geonosisArguments||[]).some(x=>x.choiceId===c.id)) &&
      !(['dimmock-anti-empire','dimmock-loyal-empire'].includes(c.id)&&(progress.geonosisArguments||[]).some(x=>['antiEmpire','loyalEmpire'].includes(x.key)))).map(c => ({ id: c.id, label: c.label }))""")
rep("const SWE02_SKILLS=Object.freeze({'Enganação':'engano','Charme':'carisma','Percepção':'percepcion','Computadores':'computadoras','Astrogação':'astronavegacion','Mecânica':'mecanica'});", "const SWE02_SKILLS=Object.freeze({'Enganação':'engano','Charme':'carisma','Coerção':'coaccion','Percepção':'percepcion','Computadores':'computadoras','Astrogação':'astronavegacion','Mecânica':'mecanica'});")
rep("check.difficulty==='Difícil'?3:null;", "check.difficulty==='Difícil'?3:check.difficulty==='Formidável'?4:null;")
rep("  if(p.currentNodeId==='palace-communications'&&p.flags?.palaceComputersLocked)return [];", "  if(p.currentNodeId==='palace-communications'&&p.flags?.palaceComputersLocked)return [];\n  if(p.currentNodeId==='drombb-cantina'&&!p.flags?.drombbThreatNeutralized)return [];")
rep("    if(p.currentNodeId==='palace-front-gate'&&checkIndex===0)", "    if(p.currentNodeId==='piddock-party'&&checkIndex===0)p.flags.geoPiddockCharmPassed=true;\n    if(p.currentNodeId==='drombb-cantina'&&checkIndex===0)p.flags.geoDrombbCheckPassed=true;\n    if(p.currentNodeId==='palace-front-gate'&&checkIndex===0)")
rep("  if (p.currentNodeId === 'teemo-showdown') {", "  if (p.currentNodeId === 'gantry-aa7') {id='laoth-gantry-aa7';label='Gantry AA7 — soldados e Vrixx’tt';}\n  if (p.currentNodeId === 'teemo-showdown') {")
rep("  if (id === 'teemo-palace-guards') {", """  if(id==='laoth-gantry-aa7'){
    const p=world?.publishedAdventure;validateGeonosis(p);
    if(p?.currentNodeId!=='gantry-aa7'||!p.flags?.geoFinalized)throw Error('Gantry AA7 exige audiência pontuada.');
    return {id,label:'Gantry AA7',source:{productCode:'SWE02',printedPages:[36,37]},
      playerInitiativeSkill:'alerta',npcInitiativeSkill:'alerta',
      environment:{cargoCoverSetbackDice:1,requiresCoverTargeting:true},
      geonosianGuardCount:p.flags.geoGuards,
      adversaries:[...Array.from({length:p.flags.geoGuards},(_,i)=>({profileKey:'laoth_geonosian_soldier',count:1,range:'Médio',name:`Soldado AA7 ${i+1}`})),
        {profileKey:'laoth_vrixxtt_findsman',count:1,range:'Longo'}]};
  }
  if (id === 'teemo-palace-guards') {""")
rep("  if(encounterId==='teemo-palace-guards'){", """  if(encounterId==='laoth-gantry-aa7'){
    const expected=longArmOfTheHuttEncounterSpec(next,encounterId).adversaries;
    if(!Array.isArray(adversaries)||adversaries.length!==expected.length||expected.some((x,i)=>x.profileKey!==adversaries[i]?.profileKey))throw Error('Gantry: grupos do combate ausentes.');
    if(adversaries.some(x=>!x.id)||new Set(adversaries.map(x=>x.id)).size!==adversaries.length)throw Error('Gantry: IDs não únicos.');
    const eliminated=adversaries.every(x=>['incapacitated','defeated'].includes(x.status));
    if(outcome==='adversaries_defeated'&&!eliminated)throw Error('Gantry: vitória sem inimigos incapacitados.');
    if(outcome==='negotiated')throw Error('Gantry: suborno requer pagamento verificado; não implementado.');
    confirmedCombat={groupCount:expected.length,enemyIds:adversaries.map(x=>x.id),eliminated};
  }
  if(encounterId==='teemo-palace-guards'){""")
rep("  if(encounterId==='teemo-palace-guards')p.flags.teemoDefeated=outcome==='adversaries_defeated';", "  if(encounterId==='teemo-palace-guards')p.flags.teemoDefeated=outcome==='adversaries_defeated';\n  if(encounterId==='laoth-gantry-aa7'){p.flags.geoGantryCleared=['adversaries_defeated','escaped','player_incapacitated'].includes(outcome);p.flags.geoRescuedByVios=outcome==='player_incapacitated';}")
p.write_text(s)
# profile
p=Path('app/src/main/assets/vtt/js/rules/adversaries.mjs');s=p.read_text();anchor='  laoth_elite_gamorrean_guard: Object.freeze({';assert s.count(anchor)==1
s=s.replace(anchor,"""  laoth_geonosian_soldier: Object.freeze({
    key:'laoth_geonosian_soldier',name:'Soldado geonosiano AA7',kind:'rival',
    characteristics:{fortaleza:3,agilidad:3,intelecto:2,astucia:2,voluntad:2,presencia:2},
    soak:5,woundsThreshold:8,strainThreshold:null,defense:{melee:0,ranged:0},
    groupSkills:[],skills:{pelea:1,armas_distancia_ligeras:1},talents:[],abilities:[],
    weapons:[{name:'Pistola blaster pesada geonosiana',skill:'armas_distancia_ligeras',range:'Médio',damage:7,crit:2,stunSetting:true}],
    source:{title:'The Long Arm of the Hutt',productCode:'SWE02',printedPage:37,authority:'OFFICIAL_PDF_TEXT_VERIFIED'}
  }),
  laoth_vrixxtt_findsman: Object.freeze({
    key:'laoth_vrixxtt_findsman',name:'Vrixx’tt — Gand Findsman',kind:'rival',
    characteristics:{fortaleza:3,agilidad:4,intelecto:2,astucia:3,voluntad:2,presencia:2},
    soak:5,woundsThreshold:13,strainThreshold:null,defense:{melee:0,ranged:0},
    groupSkills:[],skills:{pelea:1,coaccion:1,coordinacion:1,armas_distancia_pesadas:1,armas_distancia_ligeras:1,supervivencia:2,alerta:1},
    talents:[{key:'golpe_letal',name:'Lethal Blow',rank:1}],abilities:[],
    weapons:[{name:'Fuzil blaster geonosiano',skill:'armas_distancia_pesadas',range:'Longo',damage:9,crit:2,stunSetting:true}],
    source:{title:'The Long Arm of the Hutt',productCode:'SWE02',printedPage:37,authority:'OFFICIAL_PDF_TEXT_VERIFIED'}
  }),
"""+anchor);p.write_text(s)
print('P237 patched')
