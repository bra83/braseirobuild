import { FORCE_DIE_FACES } from './forceData.mjs';
const clone=v=>structuredClone(v);
function int(v,label,{min=0,max=99}={}){const n=Number(v);if(!Number.isInteger(n)||n<min||n>max)throw new Error(`${label} inválido.`);return n;}
function rollOne(rng){const raw=Number(rng());const idx=Math.max(0,Math.min(11,Math.floor((Number.isFinite(raw)?raw:0)*12)));return {index:idx+1,...FORCE_DIE_FACES[idx]};}
export function prepareDestinyPool(playerCount=1,{rng=Math.random}={}){
  if(playerCount&&typeof playerCount==='object'){const opts=playerCount;playerCount=opts.playerCount??1;rng=opts.rng??rng;}
  const count=int(playerCount,'Quantidade de jogadores',{min:1,max:20});const faces=[];let light=0,dark=0;for(let i=0;i<count;i++){const f=rollOne(rng);faces.push(f);light+=Number(f.light||0);dark+=Number(f.dark||0);}
  return {light,dark,faces,rolls:clone(faces),playerCount:count,total:light+dark};
}
export function setPreparedDestiny(group,pool){const next=clone(group);next.destiny={light:int(pool?.light,'Destino luminoso',{max:100}),dark:int(pool?.dark,'Destino sombrio',{max:100})};return next;}
export function flipDestiny(group,side){const next=clone(group);if(!next?.destiny)throw new Error('Reserva de Destino inválida.');if(side==='light'){if(Number(next.destiny.light)<1)throw new Error('Não há Ponto de Destino luminoso disponível.');next.destiny.light-=1;next.destiny.dark+=1;}else if(side==='dark'){if(Number(next.destiny.dark)<1)throw new Error('Não há Ponto de Destino sombrio disponível.');next.destiny.dark-=1;next.destiny.light+=1;}else throw new Error('Lado de Destino inválido.');return next;}
export function upgradePositivePool(pool,upgrades=1){let ability=Math.max(0,Number(pool?.ability??0)),proficiency=Math.max(0,Number(pool?.proficiency??0));let n=int(upgrades,'Aprimoramentos',{max:20});while(n-->0){if(ability>0){ability--;proficiency++;}else ability++;}return {...pool,ability,proficiency};}
export function upgradeNegativePool(pool,upgrades=1){let difficulty=Math.max(0,Number(pool?.difficulty??0)),challenge=Math.max(0,Number(pool?.challenge??0));let n=int(upgrades,'Aprimoramentos',{max:20});while(n-->0){if(difficulty>0){difficulty--;challenge++;}else difficulty++;}return {...pool,difficulty,challenge};}

export function upgradePositivePoolOnce(pool){return upgradePositivePool(pool,1);}
