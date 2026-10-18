export const CHARACTERISTICS = Object.freeze(['fortaleza','agilidad','intelecto','astucia','voluntad','presencia']);

export const SKILLS = Object.freeze({
  actividad_criminal:'astucia', aguante:'fortaleza', alerta:'voluntad', astronavegacion:'intelecto', atletismo:'fortaleza',
  callejeo:'astucia', carisma:'presencia', coaccion:'voluntad', computadoras:'intelecto', coordinacion:'agilidad', disciplina:'voluntad',
  engano:'astucia', frialdad:'presencia', liderazgo:'presencia', mecanica:'intelecto', medicina:'intelecto', negociacion:'presencia',
  percepcion:'astucia', pilotar:'agilidad', pilotar_vehiculo_espacial:'agilidad', pilotar_vehiculo_planetario:'agilidad', sigilo:'agilidad', supervivencia:'astucia',
  armas_distancia_ligeras:'agilidad', armas_distancia_pesadas:'agilidad', armas_cuerpo_a_cuerpo:'fortaleza', artilleria:'agilidad', pelea:'fortaleza',
  bajos_fondos:'intelecto', borde_exterior:'intelecto', educacion:'intelecto', estrategia_militar:'intelecto', mundos_del_nucleo:'intelecto',
  saber_arcaico:'intelecto', xenologia:'intelecto'
});

export function createBlankCharacter(name='Personagem') {
  return {
    schemaVersion:1,
    name,
    species:'', profession:'', specializations:[], player:'', source:null, careerSkills:[],
    characteristics:{ fortaleza:2, agilidad:2, intelecto:2, astucia:2, voluntad:2, presencia:2 },
    skills:Object.fromEntries(Object.keys(SKILLS).map(k=>[k,0])),
    derived:{ soak:0, wounds:{threshold:0,current:0}, strain:{threshold:0,current:0}, defense:{ranged:0,melee:0} },
    xp:{total:0,available:0},
    motivations:[], obligations:[], legacyAoR:null,
    force:{rating:0,committed:0,darkSideUser:false,ongoing:[],powers:{}},
    credits:0, weapons:[], armor:[], gear:[], criticalInjuries:[], statusEffects:[], talents:[], description:{},
    customSkills:[], signatureAbilities:[], mounts:[]
  };
}

function intIn(v,min,max,label){
  if(!Number.isInteger(v)||v<min||v>max) throw new Error(`${label} deve estar entre ${min} e ${max}.`);
}

export function validateCharacter(c) {
  if(!c || typeof c!=='object') throw new Error('Ficha inválida.');
  if(c.schemaVersion!==1) throw new Error('Versão de ficha não suportada.');
  if(typeof c.name!=='string' || !c.name.trim()) throw new Error('Nome do personagem é obrigatório.');
  for(const key of CHARACTERISTICS){
    intIn(c.characteristics?.[key],1,6,`Característica ${key}`);
  }
  for(const key of Object.keys(SKILLS)){
    intIn(c.skills?.[key],0,5,`Habilidade ${key}`);
  }
  for(const [label,v] of [['proteção',c.derived?.soak],['ferimentos atuais',c.derived?.wounds?.current],['ferimentos limite',c.derived?.wounds?.threshold],['tensão atual',c.derived?.strain?.current],['tensão limite',c.derived?.strain?.threshold],['defesa à distância',c.derived?.defense?.ranged],['defesa corpo a corpo',c.derived?.defense?.melee],['PE total',c.xp?.total],['PE disponíveis',c.xp?.available],['créditos',c.credits]]){
    if(!Number.isInteger(v)||v<0) throw new Error(`${label} inválido.`);
  }
  if(c.force!==undefined){
    if(!c.force||typeof c.force!=='object') throw new Error('Estado da Força inválido.');
    intIn(Number(c.force.rating??0),0,10,'Force Rating');
    intIn(Number(c.force.committed??0),0,10,'Dados da Força comprometidos');
    if(Number(c.force.committed??0)>Number(c.force.rating??0)) throw new Error('Dados da Força comprometidos excedem o Force Rating.');
  }
  for(const key of ['specializations','careerSkills','motivations','obligations','weapons','armor','gear','criticalInjuries','statusEffects','talents','customSkills']){
    if(!Array.isArray(c[key])) throw new Error(`Campo ${key} inválido.`);
  }
  if(c.signatureAbilities !== undefined && !Array.isArray(c.signatureAbilities)) throw new Error('Campo signatureAbilities inválido.');
  if(c.mounts !== undefined && !Array.isArray(c.mounts)) throw new Error('Campo mounts inválido.');
  return true;
}

export function exportCharacterJson(c){ validateCharacter(c); return JSON.stringify(c,null,2); }
export function importCharacterJson(text){ const c=JSON.parse(text); validateCharacter(c); return c; }
