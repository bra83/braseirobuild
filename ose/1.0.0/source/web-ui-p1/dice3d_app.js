/* Braseiro OSE: Forbidden Lands Dice3DStage 3.3.24 visual port.
   Source visual: bra83/braseiro-forbidden-lands/src/components/Dice3DStage.tsx
   CSS: bra83/braseiro-forbidden-lands/src/index.css
   No RNG occurs in the presentation layer. The OSE referee provides every face. */
(function(){
'use strict';
const overlay=document.getElementById('dice3dOverlay');
const tray=document.getElementById('dice3dTray');
const status=document.getElementById('status');
const result=document.getElementById('result');
const detail=document.getElementById('detail');
const close=document.getElementById('close');
const title=document.getElementById('title');
if(!overlay||!tray||!status||!result||!detail||!close)return;
const finalRotation={
  1:[0,0],2:[-90,0],3:[0,-90],4:[0,90],5:[90,0],6:[0,180]
};
let renderToken=0;
function htmlEscape(x){return String(x).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
function buildFaces(die,i,token){
  const sides=die.sides;
  const label=die.label ? htmlEscape(die.label):('D'+sides);
  const delay=Math.min(i*32,220);
  if(sides===6){
    const r=finalRotation[die.value];
    const style=`--dice-final-x:${r[0]}deg;--dice-final-y:${r[1]}deg;--dice-spin-x:${720+(i%4)*90}deg;--dice-spin-y:${900+(i%5)*90}deg;--dice-delay:${delay}ms;`;
    return `<div class="dice3d-slot" aria-label="${label}: ${die.value}"><div class="dice3d-cube dice3d-base" style="${style}" data-roll-key="${token}-${i}"><span class="dice3d-face dice3d-front">1</span><span class="dice3d-face dice3d-back">6</span><span class="dice3d-face dice3d-right">3</span><span class="dice3d-face dice3d-left">4</span><span class="dice3d-face dice3d-top">2</span><span class="dice3d-face dice3d-bottom">5</span></div><span class="dice3d-caption">${label}</span></div>`;
  }
  const shape=[4,8,10,12,20,100].includes(sides)?sides:20;
  const style=`--dice-spin-x:${630+(i%4)*90}deg;--dice-spin-y:${810+(i%5)*72}deg;--dice-delay:${delay}ms;`;
  return `<div class="dice3d-slot" aria-label="${label}: ${die.value}"><div class="dice3d-poly dice3d-poly-${shape} dice3d-artifact" data-roll-key="${token}-${i}" style="${style}"><span class="dice3d-poly-core">${die.value}</span><span class="dice3d-poly-shine"></span></div><span class="dice3d-caption">${label}</span></div>`;
}
function normalizeRolls(cfg){
  const raw=Array.isArray(cfg.rolls)?cfg.rolls:Array.isArray(cfg.targetRolls)?cfg.targetRolls:null;
  if(!raw || !raw.length)throw new Error('Rolagem sem faces canônicas: nenhuma animação foi executada.');
  const terms=Array.isArray(cfg.terms)&&cfg.terms.length?cfg.terms:[{count:raw.length,sides:cfg.sides,sign:1}];
  const out=[];let k=0;
  for(const t of terms){
    const sides=Number(t.sides),count=Number(t.count);
    if(!Number.isInteger(sides)||![4,6,8,10,12,20,100].includes(sides)||!Number.isInteger(count)||count<1||count>60)throw new Error('Tipo ou quantidade de dados não suportados.');
    for(let i=0;i<count;i++){
      const n=Number(raw[k++]);
      if(!Number.isInteger(n)||n<1||n>sides)throw new Error('Face canônica fora do intervalo permitido.');
      out.push({sides,value:n,sign:t.sign===-1?-1:1,label:t.label||''});
    }
  }
  if(k!==raw.length)throw new Error('A contagem de dados não coincide com o recibo oficial.');
  return out;
}
function launch(cfg){
  ++renderToken;
  window.OSEPhysicsDice?.stop();
  try{
    cfg=cfg||{};
    const dice=normalizeRolls(cfg);
    const implied=dice.reduce((sum,d)=>sum+d.sign*d.value,Number(cfg.modifier)||0);
    const official=cfg.canonicalTotal!==undefined?Number(cfg.canonicalTotal):cfg.total!==undefined?Number(cfg.total):implied;
    if(!Number.isFinite(official))throw new Error('Resultado oficial inválido.');
    const token=renderToken;
    tray.innerHTML=dice.map((die,i)=>buildFaces(die,i,token)).join('');
    title.textContent='MESA 3D · FORBIDDEN LANDS / OSE';
    status.textContent='Faces registradas pelo árbitro OSE';
    result.textContent=String(official);
    const expression=dice.map(d=>(d.sign<0?'− ':'')+'d'+d.sides+'['+d.value+']').join(' + ').replace(/\+ −/g,'−');
    detail.textContent=expression+(Number(cfg.modifier)?' '+(cfg.modifier>0?'+':'−')+' '+Math.abs(Number(cfg.modifier)):'')+' = '+official;
    overlay.classList.remove('hidden');
    const physics=window.OSEPhysicsDice?.start(cfg,dice,()=>{
      status.textContent='Simulação concluída · faces confirmadas pelo árbitro OSE';
    });
    if(physics)status.textContent='Dados físicos em movimento · resultado definido pelo árbitro OSE';
    return {ok:true,rolls:dice.map(d=>d.value),total:official,visualMode:physics?'physics':'fl-css'};
  }catch(error){
    tray.innerHTML='';
    status.textContent='Não foi possível representar a rolagem';
    result.textContent='';
    detail.textContent=String(error&&error.message||error);
    overlay.classList.remove('hidden');
    return {ok:false,error:String(error&&error.message||error)};
  }
}
function stop(){++renderToken;window.OSEPhysicsDice?.stop();tray.innerHTML='';}
close.addEventListener('click',()=>{stop();overlay.classList.add('hidden')});
overlay.addEventListener('click',e=>{if(e.target===overlay){stop();overlay.classList.add('hidden')}});
window.BraseiroDice={start:launch,stop};
})();
