const PT_SKILLS=Object.freeze({Administer:'ADMINISTRAR',Connect:'CONECTAR',Convince:'CONVENCER',Craft:'OFÍCIO',Exert:'ESFORÇO',Heal:'CURAR',Know:'CONHECIMENTO',Lead:'LIDERAR',Magic:'MAGIA',Notice:'PERCEPÇÃO',Perform:'ATUAÇÃO',Pray:'REZAR',Punch:'BRIGA',Ride:'CAVALGAR',Sail:'NAVEGAR',Shoot:'ATIRAR',Sneak:'FURTIVIDADE',Stab:'ARMAS BRANCAS',Survive:'SOBREVIVÊNCIA',Trade:'COMÉRCIO',Work:'TRABALHO'});
const page=location.pathname.split('/').pop()||'';
const raf=fn=>requestAnimationFrame(()=>requestAnimationFrame(fn));

function patchJourney(){
  const bars=document.getElementById('statusButton');
  if(bars&&!bars.dataset.labeled){
    bars.dataset.labeled='1';
    const hp=bars.querySelector('.track.hp'),strain=bars.querySelector('.track.strain');
    if(hp){const label=document.createElement('small');label.className='status-bar-label';label.textContent='PV';hp.before(label)}
    if(strain){const label=document.createElement('small');label.className='status-bar-label';label.textContent='TENSÃO';strain.before(label)}
    bars.title='PV e Tensão do Sistema — toque para ver os valores';
  }
  const skill=document.getElementById('mechanicsSkill');
  const translate=()=>{if(!skill)return;for(const o of skill.options){const raw=o.value;const suffix=o.textContent.match(/(?:\s[+-]?\d+| \(não treinado\))$/)?.[0]??'';if(PT_SKILLS[raw])o.textContent=PT_SKILLS[raw]+suffix}}
  translate();
  if(skill&&!skill.dataset.ptObserved){skill.dataset.ptObserved='1';new MutationObserver(translate).observe(skill,{childList:true,subtree:true})}
}

function patchCharacter(){
  if(document.getElementById('characterQuickActions'))return;
  const screen=document.getElementById('screen');if(!screen)return;
  const panel=document.createElement('section');panel.id='characterQuickActions';panel.className='character-quick-actions';
  panel.innerHTML='<button type="button" data-open>PERSONAGENS</button><button type="button" data-create>CRIAR PERSONAGEM</button><button type="button" class="primary" data-random>GERAR ALEATÓRIO</button>';
  screen.appendChild(panel);
  const open=()=>document.getElementById('btn_header_camadas')?.click();
  panel.querySelector('[data-open]').onclick=open;
  panel.querySelector('[data-create]').onclick=()=>{location.href='criacao.html'};
  panel.querySelector('[data-random]').onclick=()=>{open();raf(()=>document.querySelector('#characterDrawer [data-generate]')?.click())};
}

function patchAtlas(){
  const canvas=document.getElementById('mapCanvas');if(!canvas)return;
  const refreshLabels=()=>{
    let state=null;try{state=window.__SCR014_RUNTIME__?.state?.()}catch{}
    for(const h of canvas.querySelectorAll('.hex[data-hex-key]')){
      const label=h.querySelector('.poi-symbol>i');if(!label)continue;
      if(state){const [q,r]=h.dataset.hexKey.split(',').map(Number);const rec=state?.worldState?.atlas?.hexes?.[`${17+q},${22+r}`];if(rec?.poiName)label.textContent=rec.poiName}
      label.title=label.textContent;
    }
    for(const l of document.querySelectorAll('.barline label'))if(l.textContent.trim()==='STRAIN')l.textContent='TENSÃO';
  };
  refreshLabels();new MutationObserver(refreshLabels).observe(canvas,{childList:true,subtree:true});
  const narrative=document.getElementById('worldNarrativeCard'),text=document.getElementById('worldNarrativeText');
  if(narrative&&!narrative.dataset.sceneGate){narrative.dataset.sceneGate='1';let timer=null;const gate=()=>{if(narrative.hidden||!String(text?.textContent??'').trim())return;clearTimeout(timer);const status=document.getElementById('worldActionStatus');if(status)status.textContent='Cena criada. Abrindo a Jornada para continuar a história…';timer=setTimeout(()=>{location.href='jornada.html'},900)};new MutationObserver(gate).observe(narrative,{attributes:true,childList:true,subtree:true});new MutationObserver(gate).observe(text,{childList:true,subtree:true});}
}

raf(()=>{if(page==='jornada.html')patchJourney();if(page==='ficha.html')patchCharacter();if(page==='scr014.html')patchAtlas()});
