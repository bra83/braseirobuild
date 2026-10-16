import { createBlankCharacter } from './character.mjs';

function baseStarter({id,name,species,profession,specialization,characteristics,skills,careerSkills=[],soak,wounds,strain,credits,weapons,gear}) {
  const c=createBlankCharacter(name);
  c.source={
    type:'eote_test_fixture',
    title:`EotE local test fixture — ${name}`,
    language:'pt-BR',
    authority:'NONE',
    official:false,
    notes:'Fixture determinística local para regressão do VTT. Não é personagem oficial nem fonte de regra.'
  };
  c.species=species; c.profession=profession; c.specializations=specialization?[specialization]:[]; c.characteristics=characteristics; c.careerSkills=[...careerSkills];
  c.creation={rulesProfile:'EOTE_CORE',fixtureId:id,fixture:true,official:false};
  Object.assign(c.skills,skills);
  c.customSkills=[{key:'conhecimentos',label:'Conhecimentos',characteristic:'intelecto',rank:skills.conhecimentos ?? 0}];
  c.derived={soak,wounds:{threshold:wounds,current:0},strain:{threshold:strain,current:0},defense:{ranged:0,melee:0}};
  c.credits=credits; c.weapons=weapons; c.gear=gear;
  return c;
}

export function createZalStarterCharacter(){
  return baseStarter({
    id:'zal',name:'Zal',species:'Humana',profession:'Contrabandista',specialization:'Piloto',
    characteristics:{fortaleza:2,agilidad:4,intelecto:2,astucia:3,voluntad:2,presencia:2},
    skills:{actividad_criminal:1,astronavegacion:1,engano:1,frialdad:1,pilotar:2,armas_distancia_ligeras:1,artilleria:1},
    careerSkills:['armas_distancia_ligeras','artilleria','astronavegacion','frialdad','mecanica','percepcion','pilotar'],
    soak:4,wounds:12,strain:12,credits:300,
    weapons:[
      {name:'Pistola blaster pesada',skill:'armas_distancia_ligeras',range:'Médio',damage:7,crit:3},
      {name:'Granada de fragmentação',skill:'armas_distancia_ligeras',range:'Curto',damage:8,crit:4,blast:6,blastActivationCost:2,quantity:2},
      {name:'Granada perfurante',skill:'armas_distancia_ligeras',range:'Curto',damage:16,crit:3,pierce:3,quantity:1},
      {name:'Punhos',skill:'pelea',range:'Interação',damage:2,crit:5}
    ],
    gear:[{name:'Injetor de estimulantes',quantity:2},{name:'Comunicador',quantity:1},{name:'Traje de voo',quantity:1},{name:'Gazua eletrônica',quantity:1}]
  });
}

export function createVendriStarterCharacter(){
  return baseStarter({
    id:'vendri',name:'Vendri',species:'Duros',profession:'Explorador',specialization:'Batedor',
    characteristics:{fortaleza:2,agilidad:3,intelecto:3,astucia:3,voluntad:2,presencia:2},
    skills:{computadoras:1,coordinacion:1,frialdad:1,medicina:1,percepcion:1,pilotar:1,sigilo:1,supervivencia:1,armas_distancia_pesadas:1},
    careerSkills:['actividad_criminal','atletismo','computadoras','conhecimentos','coordinacion','engano','frialdad','medicina','percepcion','pilotar','sigilo','supervivencia'],
    soak:4,wounds:13,strain:12,credits:75,
    weapons:[
      {name:'Fuzil blaster',skill:'armas_distancia_pesadas',range:'Longo',damage:9,crit:3},
      {name:'Faca de combate',skill:'armas_cuerpo_a_cuerpo',range:'Interação',damage:3,crit:3},
      {name:'Granada atordoante',skill:'armas_distancia_ligeras',range:'Curto',damage:8,damageType:'strain',blast:8,blastActivationCost:2,quantity:1}
    ],
    gear:[{name:'Injetor de estimulantes',quantity:3},{name:'Equipamento de comunicações',quantity:1},{name:'Armadura acolchoada',quantity:1},{name:'Visor noturno',quantity:1}]
  });
}

export function createTendaarStarterCharacter(){
  return baseStarter({
    id:'tendaar',name:'Tendaar',species:'Mon Calamari',profession:'Técnico',specialization:'Mecânico',
    characteristics:{fortaleza:2,agilidad:2,intelecto:4,astucia:1,voluntad:3,presencia:3},
    skills:{alerta:1,computadoras:2,conhecimentos:1,mecanica:1,medicina:1,armas_distancia_ligeras:1},
    careerSkills:['alerta','armas_distancia_ligeras','atletismo','computadoras','conhecimentos','mecanica','medicina','percepcion','pilotar'],
    soak:4,wounds:12,strain:13,credits:100,
    weapons:[
      {name:'Pistola blaster',skill:'armas_distancia_ligeras',range:'Médio',damage:6,crit:3},
      {name:'Granada atordoante',skill:'armas_distancia_ligeras',range:'Curto',damage:8,damageType:'strain',blast:8,blastActivationCost:2,quantity:1},
      {name:'Punhos',skill:'pelea',range:'Interação',damage:2,crit:5}
    ],
    gear:[{name:'Injetor de estimulantes',quantity:2},{name:'Comunicador',quantity:1},{name:'Colete blindado',quantity:1},{name:'Lanterna de fusão',quantity:1},{name:'Módulo de dados e ferramentas',quantity:1},{name:'Medpac',quantity:1}]
  });
}

export function createJinRioStarterCharacter(){
  return baseStarter({
    id:'jin-rio',name:'Jin-Rio',species:'Humana',profession:'Colono',specialization:'Político',
    characteristics:{fortaleza:2,agilidad:2,intelecto:3,astucia:2,voluntad:2,presencia:4},
    skills:{callejeo:1,carisma:1,conhecimentos:1,engano:2,negociacion:1,sigilo:1,armas_distancia_ligeras:1},
    careerSkills:['callejeo','carisma','coaccion','conhecimentos','engano','liderazgo','negociacion'],
    soak:3,wounds:12,strain:12,credits:750,
    weapons:[
      {name:'Pistola blaster',skill:'armas_distancia_ligeras',range:'Médio',damage:6,crit:3},
      {name:'Faca de combate',skill:'armas_cuerpo_a_cuerpo',range:'Interação',damage:2,crit:5}
    ],
    gear:[{name:'Injetor de estimulantes',quantity:4},{name:'Holocom',quantity:1},{name:'Roupa pesada',quantity:1},{name:'Maquiagem para disfarce',quantity:1},{name:'Módulo de dados',quantity:1}]
  });
}

export function createCaelStarterCharacter(){
  return baseStarter({
    id:'cael',name:'Cael',species:'Humano',profession:'Mercenário',specialization:'Soldado Mercenário',
    characteristics:{fortaleza:3,agilidad:3,intelecto:2,astucia:2,voluntad:3,presencia:2},
    skills:{aguante:1,atletismo:1,coaccion:1,disciplina:1,armas_distancia_ligeras:1,armas_distancia_pesadas:2,armas_cuerpo_a_cuerpo:1,artilleria:1,pelea:1},
    careerSkills:['aguante','armas_distancia_ligeras','armas_distancia_pesadas','armas_cuerpo_a_cuerpo','artilleria','atletismo','medicina','pelea','supervivencia'],
    soak:5,wounds:13,strain:13,credits:50,
    weapons:[
      {name:'Fuzil blaster pesado',skill:'armas_distancia_pesadas',range:'Longo',damage:10,crit:3},
      {name:'Vibrofaca',skill:'armas_cuerpo_a_cuerpo',range:'Interação',damage:5,crit:2,pierce:2},
      {name:'Granada de fragmentação',skill:'armas_distancia_ligeras',range:'Curto',damage:8,crit:4,blast:6,blastActivationCost:2,quantity:1},
      {name:'Granada perfurante',skill:'armas_distancia_ligeras',range:'Curto',damage:16,crit:3,pierce:3,quantity:1}
    ],
    gear:[{name:'Injetor de estimulantes',quantity:5},{name:'Comunicador',quantity:1},{name:'Armadura acolchoada',quantity:1},{name:'Visor noturno',quantity:1}]
  });
}

export function createArkhanStarterCharacter(){
  return baseStarter({
    id:'arkhan',name:'Arkhan',species:'Bothan',profession:'Mercenário',specialization:'Soldado Mercenário',
    characteristics:{fortaleza:1,agilidad:3,intelecto:2,astucia:3,voluntad:3,presencia:3},
    skills:{alerta:1,callejeo:1,disciplina:1,frialdad:1,liderazgo:2,percepcion:1,armas_distancia_ligeras:1,armas_distancia_pesadas:1},
    careerSkills:['alerta','armas_distancia_ligeras','armas_distancia_pesadas','coaccion','conhecimentos','disciplina','frialdad','liderazgo','pelea','percepcion'],
    soak:3,wounds:11,strain:14,credits:175,
    weapons:[
      {name:'Pistola blaster pesada',skill:'armas_distancia_ligeras',range:'Médio',damage:7,crit:3},
      {name:'Faca de combate',skill:'armas_cuerpo_a_cuerpo',range:'Interação',damage:2,crit:3},
      {name:'Granada de fragmentação',skill:'armas_distancia_ligeras',range:'Curto',damage:8,crit:4,blast:6,blastActivationCost:2,quantity:2},
      {name:'Granada perfurante',skill:'armas_distancia_ligeras',range:'Curto',damage:16,crit:3,pierce:3,quantity:1}
    ],
    gear:[{name:'Injetor de estimulantes',quantity:4},{name:'Comunicador de longo alcance',quantity:1},{name:'Armadura acolchoada',quantity:1},{name:'Máscara respiratória',quantity:1},{name:'Bastão luminoso',quantity:2},{name:'Eletrobinóculos',quantity:1}]
  });
}

export const STARTER_PRESETS=Object.freeze({
  zal:createZalStarterCharacter,
  vendri:createVendriStarterCharacter,
  tendaar:createTendaarStarterCharacter,
  'jin-rio':createJinRioStarterCharacter,
  cael:createCaelStarterCharacter,
  arkhan:createArkhanStarterCharacter
});
