import { skillLabelPtBr, focusLabelPtBr, artLabelPtBr, spellLabelPtBr, wiseConceptLabelPtBr } from './core/ptbr-labels.js';

const PT_SKILLS=Object.freeze({Administer:'ADMINISTRAR',Connect:'CONECTAR',Convince:'CONVENCER',Craft:'OFÍCIO',Exert:'ESFORÇO',Heal:'CURAR',Know:'CONHECIMENTO',Lead:'LIDERAR',Magic:'MAGIA',Notice:'PERCEPÇÃO',Perform:'ATUAÇÃO',Pray:'REZAR',Punch:'BRIGA',Ride:'CAVALGAR',Sail:'NAVEGAR',Shoot:'ATIRAR',Sneak:'FURTIVIDADE',Stab:'ARMAS BRANCAS',Survive:'SOBREVIVÊNCIA',Trade:'COMÉRCIO',Work:'TRABALHO'});
const page=location.pathname.split('/').pop()||'';
const raf=fn=>requestAnimationFrame(()=>requestAnimationFrame(fn));
const PENDING_SCENE_IMAGE_KEY='braseiro.pending.scene-image.v1';

// Android may deliver a shared image while jornada.js is still awaiting its boot data.
// Capture the native event immediately and replay it only after the Jornada listener is ready.
if(page==='jornada.html'){
  window.addEventListener('braseiro-native-file',event=>{
    const detail=event?.detail;
    if(detail?.kind!=='SCENE_IMAGE_IMPORT')return;
    globalThis.__BRASEIRO_PENDING_SCENE_IMAGE=detail;
    try{sessionStorage.setItem(PENDING_SCENE_IMAGE_KEY,JSON.stringify(detail))}catch{}
  },{capture:true});
}

function ensureCss(){if(document.querySelector('link[href="ux-hotfix.css"]'))return;const l=document.createElement('link');l.rel='stylesheet';l.href='ux-hotfix.css';document.head.appendChild(l)}

function replayPendingSceneImage(){
  let detail=globalThis.__BRASEIRO_PENDING_SCENE_IMAGE??null;let raw=null;try{raw=sessionStorage.getItem(PENDING_SCENE_IMAGE_KEY)}catch{}if(!detail&&!raw)return;
  if(!detail&&raw){try{detail=JSON.parse(raw)}catch{try{sessionStorage.removeItem(PENDING_SCENE_IMAGE_KEY)}catch{}return;}}
  let attempts=0;
  const tick=()=>{
    const status=document.getElementById('sceneImageStatus');
    const image=document.getElementById('sceneImportedImage');
    const done=/imagem (importada|vinculada)/i.test(status?.textContent??'')||Boolean(image?.getAttribute('src'));
    if(done){globalThis.__BRASEIRO_PENDING_SCENE_IMAGE=null;try{sessionStorage.removeItem(PENDING_SCENE_IMAGE_KEY)}catch{}return;}
    window.dispatchEvent(new CustomEvent('braseiro-native-file',{detail}));
    attempts+=1;
    if(attempts<10)setTimeout(tick,650);else if(status&&!String(status.textContent??'').trim())status.textContent='A imagem chegou ao aplicativo, mas ainda não foi vinculada. Toque em IMPORTAR IMAGEM para tentar novamente.';
  };
  setTimeout(tick,900);
}

function patchJourney(){
  // Controles de Mestre não pertencem à superfície do jogador.
  document.querySelector('.ops-link')?.remove();
  for(const link of document.querySelectorAll('a[href="gestao.html"]'))link.remove();

  const bars=document.getElementById('statusButton');
  if(bars&&!bars.dataset.labeled){bars.dataset.labeled='1';const hp=bars.querySelector('.track.hp'),strain=bars.querySelector('.track.strain');if(hp){const label=document.createElement('small');label.className='status-bar-label';label.textContent='PV';hp.before(label)}if(strain){const label=document.createElement('small');label.className='status-bar-label';label.textContent='TENSÃO';strain.before(label)}bars.title='PV e Tensão do Sistema — toque para ver os valores'}
  const skill=document.getElementById('mechanicsSkill');const translate=()=>{if(!skill)return;for(const o of skill.options){const raw=o.value;const suffix=o.textContent.match(/(?:\s[+-]?\d+| \(não treinado\))$/)?.[0]??'';const wanted=PT_SKILLS[raw]?PT_SKILLS[raw]+suffix:null;if(wanted&&o.textContent!==wanted)o.textContent=wanted}};translate();if(skill&&!skill.dataset.ptObserved){skill.dataset.ptObserved='1';new MutationObserver(translate).observe(skill,{childList:true,subtree:true})}

  // Os três ganchos do pacote são sugestões iniciais, nunca uma missão obrigatória.
  const hooks=document.getElementById('hooks');
  const relabelHooks=()=>{
    if(!hooks)return;
    const heading=hooks.previousElementSibling;
    const headingTitle=heading?.querySelector('span');const headingMeta=heading?.querySelector('small');
    if(headingTitle)headingTitle.textContent='RUMORES E OPORTUNIDADES';
    if(headingMeta)headingMeta.textContent='opcionais • você pode ignorar todos';
    if(!document.getElementById('sandboxFreedomNote')){
      const note=document.createElement('div');note.id='sandboxFreedomNote';note.className='sandbox-freedom-note';note.textContent='Você não precisa escolher um destes caminhos. Pode explorar o mapa, visitar qualquer local conhecido ou simplesmente dizer ao Mestre o que quer fazer.';hooks.before(note);
    }
    for(const card of hooks.querySelectorAll('.card')){const kind=card.querySelector('.kind');if(kind&&kind.textContent!=='OPORTUNIDADE OPCIONAL')kind.textContent='OPORTUNIDADE OPCIONAL';}
  };
  relabelHooks();if(hooks)new MutationObserver(relabelHooks).observe(hooks,{childList:true,subtree:true});

  const scene=document.getElementById('sceneCard'),freeAction=document.querySelector('.player-reaction');if(scene&&freeAction&&freeAction.parentElement===scene){freeAction.classList.add('sandbox-action');scene.after(freeAction);const actionLabel=freeAction.querySelector('label');if(actionLabel)actionLabel.textContent='O QUE VOCÊ FAZ? • AÇÃO LIVRE';const actionHelp=freeAction.querySelector('small');if(actionHelp)actionHelp.textContent='Você pode agir sem escolher nenhum gancho. Se a intenção exigir regra, risco, viagem ou gasto de recurso, o Mestre para antes do resultado e encaminha para a resolução mecânica.';}
  const narrative=document.getElementById('sceneNarrative'),title=document.getElementById('sceneTitle'),master=document.getElementById('sceneMasterButton');
  const autoExpand=()=>{if(!scene||scene.classList.contains('hidden')||!master||master.disabled)return;const visible=String(narrative?.textContent??'').trim();const key='braseiro-long-scene:'+String(title?.textContent??'CENA').trim();if(visible.length>=700||sessionStorage.getItem(key)==='1')return;sessionStorage.setItem(key,'1');master.click()};
  setTimeout(autoExpand,900);if(scene)new MutationObserver(()=>setTimeout(autoExpand,250)).observe(scene,{attributes:true,childList:true,subtree:true});
  replayPendingSceneImage();
}

function patchCharacter(){
  if(document.getElementById('characterQuickActions'))return;const screen=document.getElementById('screen');if(!screen)return;const noCharacter=document.getElementById('txt_nome_iria')?.textContent?.trim()==='SEM PERSONAGEM';if(noCharacter){screen.classList.add('character-empty');const notice=document.createElement('section');notice.className='character-empty-notice';notice.innerHTML='<small>CAMPANHA SEM PERSONAGEM</small><b>COMECE QUANDO QUISER</b><p>Este mundo já existe. Crie manualmente, gere um personagem aleatório pelas regras WWN ou importe uma ficha JSON.</p>';screen.appendChild(notice);}const panel=document.createElement('section');panel.id='characterQuickActions';panel.className='character-quick-actions';panel.innerHTML='<button type="button" data-open>PERSONAGENS</button><button type="button" data-create>CRIAR PERSONAGEM</button><button type="button" class="primary" data-random>GERAR ALEATÓRIO</button>';screen.appendChild(panel);const open=()=>document.getElementById('btn_header_camadas')?.click();panel.querySelector('[data-open]').onclick=open;panel.querySelector('[data-create]').onclick=()=>{location.href='criacao.html'};panel.querySelector('[data-random]').onclick=()=>{open();raf(()=>document.querySelector('#characterDrawer [data-generate]')?.click())}
}

function patchAtlas(){
  const canvas=document.getElementById('mapCanvas');if(!canvas)return;
  const refreshLabels=()=>{let state=null;try{state=window.__SCR014_RUNTIME__?.state?.()}catch{}for(const h of canvas.querySelectorAll('.hex[data-hex-key]')){const label=h.querySelector('.poi-symbol>i');if(!label)continue;if(state){const [q,r]=h.dataset.hexKey.split(',').map(Number);const rec=state?.worldState?.atlas?.hexes?.[`${17+q},${22+r}`];if(rec?.poiName&&label.textContent!==rec.poiName)label.textContent=rec.poiName}label.title=label.textContent}for(const l of document.querySelectorAll('.barline label'))if(l.textContent.trim()==='STRAIN')l.textContent='TENSÃO'};
  refreshLabels();new MutationObserver(refreshLabels).observe(canvas,{childList:true,subtree:true});

  const narrative=document.getElementById('worldNarrativeCard'),text=document.getElementById('worldNarrativeText'),travel=document.getElementById('travelHex'),explore=document.getElementById('exploreHex');
  let flowLocked=false,redirectTimer=null;
  const goJourney=()=>{if(redirectTimer)return;redirectTimer=setTimeout(()=>{location.href='jornada.html'},650)};
  const narrativeReady=()=>!narrative?.hidden&&Boolean(String(text?.textContent??'').trim());
  const lockWorldFlow=()=>{
    if(flowLocked)return;flowLocked=true;
    for(const button of [travel,explore])if(button){button.style.display='none';button.setAttribute('aria-hidden','true')}
    const status=document.getElementById('worldActionStatus');if(status)status.textContent='Resolvendo viagem e preparando a cena do Mestre…';
    let attempts=0;const wait=()=>{if(narrativeReady()){goJourney();return}attempts+=1;if(attempts<40)setTimeout(wait,300);else{if(status)status.textContent='A viagem foi registrada. Abrindo a Jornada para continuar a cena…';goJourney()}};setTimeout(wait,250);
  };
  for(const button of [travel,explore])if(button&&!button.dataset.narrativeGate){button.dataset.narrativeGate='1';button.addEventListener('click',lockWorldFlow,{capture:true})}
  if(narrative&&!narrative.dataset.sceneGate){narrative.dataset.sceneGate='1';const gate=()=>{if(narrativeReady())goJourney()};new MutationObserver(gate).observe(narrative,{attributes:true,childList:true,subtree:true});if(text)new MutationObserver(gate).observe(text,{childList:true,subtree:true});}
}

function translateSelect(select,fn){if(!select)return;for(const o of select.options){if(!o.value)continue;const translated=fn(o.value,o.textContent);if(translated&&o.textContent!==translated)o.textContent=translated}}
function patchCreation(){
  let applying=false;const apply=()=>{if(applying)return;applying=true;try{for(const s of document.querySelectorAll('#freeSkill,#learning1,#learning2,[data-class-bonus],[data-focus-bonus]'))translateSelect(s,v=>skillLabelPtBr(v));for(const s of document.querySelectorAll('[data-art-pick],[data-required-art],[data-wise-art]'))translateSelect(s,v=>artLabelPtBr(v));for(const s of document.querySelectorAll('[data-spell-pick]'))translateSelect(s,v=>spellLabelPtBr(v));for(const s of document.querySelectorAll('[data-focus-id]'))translateSelect(s,v=>focusLabelPtBr(v));for(const s of document.querySelectorAll('[data-wise-concept]'))translateSelect(s,v=>wiseConceptLabelPtBr(v));const bg=document.getElementById('background');if(bg)for(const o of bg.options){const pieces=o.textContent.split(' — ');if(pieces.length>1){const raw=Object.keys(PT_SKILLS).find(k=>pieces.at(-1).toUpperCase()===k.toUpperCase()||pieces.at(-1).toUpperCase()===PT_SKILLS[k]);if(raw){const wanted=`${pieces[0]} — ${skillLabelPtBr(raw)}`;if(o.textContent!==wanted)o.textContent=wanted}}}const hint=document.getElementById('backgroundFree');if(hint){const wanted=hint.textContent.replace(/\b(Administer|Connect|Convince|Craft|Exert|Heal|Know|Lead|Magic|Notice|Perform|Pray|Punch|Ride|Sail|Shoot|Sneak|Stab|Survive|Trade|Work)\b/g,m=>skillLabelPtBr(m));if(hint.textContent!==wanted)hint.textContent=wanted}}finally{applying=false}};apply();const root=document.getElementById('app');if(root){let queued=false;new MutationObserver(()=>{if(queued)return;queued=true;requestAnimationFrame(()=>{queued=false;apply()})}).observe(root,{childList:true,subtree:true})}
}

function normalizedItemText(text){return String(text??'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toUpperCase()}
function artKind(text){
  const t=normalizedItemText(text);
  if(/\b(TOCHA|TORCH|BRANDAO)\b/.test(t))return'torch';
  if(/\b(OLEO|OIL)\b/.test(t)&&/\b(LANTERNA|LANTERN)\b/.test(t))return'liquid';
  if(/\b(LANTERNA|LANTERN)\b/.test(t))return'lantern';
  if(/\b(OLEO|OIL|FRASCO|GARRAFA|CANTIL|WATERSKIN|AGUA)\b/.test(t))return'liquid';
  if(/\b(CORDA|ROPE|CORDAME)\b/.test(t))return'rope';
  if(/\b(RACAO|RACOES|COMIDA|RATION|RATIONS|FOOD|PROVISAO|PROVISOES)\b/.test(t))return'food';
  if(/\b(MOCHILA|BACKPACK|ALFORJE|SACO|BOLSA|PACK)\b/.test(t))return'pack';
  if(/\b(BARRACA|TENT|BEDROLL|COBERTOR|SACO DE DORMIR)\b/.test(t))return'camp';
  if(/\b(MARTELO|HAMMER|ALAVANCA|CROWBAR|PE DE CABRA|PICARETA|PICKAXE|PA|SHOVEL|FERRAMENTA|TOOLS?|KIT)\b/.test(t))return'tool';
  if(t.includes('ARMADURA')||/\b(LINOTORAX|COTA DE MALHA|COURACA|GREVA|CAMISA DE GUERRA|CASACO REFORCADO|MANTO DE GUERRA)\b/.test(t))return'armor';
  if(t.includes('ESCUDO')||t.includes('SHIELD'))return'shield';
  if(t.includes('MUNICAO')||t.includes('FLECHA')||t.includes('VIROTE')||t.includes('ARROW')||t.includes('BOLT'))return'ammo';
  if(t.includes('ARMA')||/\b(ESPADA|SWORD|ADAGA|DAGGER|FACA|KNIFE|LANCA|SPEAR|MACHADO|AXE|ARCO|BOW|BESTA|CROSSBOW|MAÇA|MACE)\b/.test(t))return'weapon';
  if(t.includes('TRANSPORTE')||t.includes('VEICULO')||t.includes('CAVALO')||t.includes('CARROCA')||t.includes('MULA'))return'transport';
  if(t.includes('FERA')||t.includes('ANIMAL'))return'beast';
  if(t.includes('SERVICO'))return'service';
  return'gear';
}
function artSvg(kind){
  const common='<circle cx="32" cy="32" r="29" fill="#0c1512" stroke="#705838" stroke-width="2"/><circle cx="32" cy="32" r="24" fill="none" stroke="#2d382f"/>';
  const shapes={
    torch:'<path class="wood" d="M29 30h6l3 25H26z"/><path class="metal" d="M25 29h14l-2 7H27z"/><path class="glow" d="M32 8c8 8 9 14 5 19-2 3-8 4-11 0-5-6 1-12 6-19z"/><path fill="#f0cf79" d="M32 15c4 5 4 8 2 11-1 2-4 2-5 0-2-3 0-7 3-11z"/>',
    lantern:'<path class="metal" d="M24 17h16l4 8v27H20V25z"/><path class="dark" d="M24 28h16v18H24z"/><path class="glow" d="M27 31h10v12H27z"/><path class="metal" fill="none" d="M25 18c0-8 14-8 14 0"/>',
    liquid:'<path class="dark" d="M27 13h10v9l5 8v23H22V30l5-8z"/><path class="metal" d="M27 13h10v5H27z"/><path class="glow" d="M25 37h14v12H25z"/>',
    rope:'<path fill="none" class="wood" stroke-width="4" d="M20 21c12-9 28 1 22 12-5 10-25 6-24-3 1-7 16-10 24-3 9 8 0 23-14 19-9-2-12-10-7-16"/>',
    food:'<path class="wood" d="M17 37c2-12 11-19 22-17 9 2 13 9 10 18-3 10-13 16-22 13-8-2-12-7-10-14z"/><path class="metal" fill="none" d="M23 24c6 7 12 15 20 21M18 36c10-1 19 3 28 8"/>',
    pack:'<path class="wood" d="M20 23h24l5 31H15z"/><path class="dark" d="M24 15h16l4 10H20z"/><path class="metal" fill="none" d="M24 29h16v11H24zM18 43h28"/>',
    camp:'<path class="dark" d="M9 51l23-35 23 35z"/><path class="wood" d="M32 16v35"/><path class="metal" d="M32 30l12 21H32z" opacity=".45"/>',
    tool:'<path class="wood" d="M31 23h6v32h-6z" transform="rotate(-35 34 39)"/><path class="metal" d="M17 18l9-7 17 14-8 10z"/><path class="dark" d="M38 45l9 8"/>',
    weapon:'<path class="metal" d="M16 47l25-31 6 4-25 31z"/><path class="wood" d="M13 50l9-9 5 5-9 9z"/><path class="metal" d="M19 43l8 7-3 4-8-8z"/>',
    armor:'<path class="dark" d="M20 18l12-6 12 6 7 12-7 6v17H20V36l-7-6z"/><path class="metal" d="M27 18h10l4 31H23z" opacity=".58"/>',
    shield:'<path class="dark" d="M32 11l18 7-3 25c-3 8-9 14-15 18-6-4-12-10-15-18l-3-25z"/><path class="metal" d="M32 16v38c7-5 10-10 12-15l2-17z" opacity=".7"/>',
    ammo:'<path class="wood" d="M15 49l31-31 3 3-31 31z"/><path class="metal" d="M44 16l8-4-4 8zM19 45l-7 8 9-5z"/><path class="wood" d="M22 51l25-25 3 3-25 25z" opacity=".8"/>',
    transport:'<path class="wood" d="M13 35h34l5 12H19z"/><circle class="dark" cx="22" cy="51" r="7"/><circle class="dark" cx="45" cy="51" r="7"/><path class="metal" d="M18 32h25l-3-11H24z"/>',
    beast:'<path class="wood" d="M17 39c2-13 9-20 18-19 10 1 16 8 15 19-1 7-7 13-16 13-11 0-18-5-17-13z"/><circle class="glow" cx="28" cy="34" r="2"/><circle class="glow" cx="41" cy="34" r="2"/><path class="metal" d="M22 23l-5-9 10 5M44 22l5-8-10 5"/>',
    service:'<path class="dark" d="M18 18h28v36H18z"/><path class="metal" d="M23 25h18v3H23zm0 8h18v3H23zm0 8h12v3H23z"/>',
    gear:'<path class="wood" d="M14 25h36v27H14z"/><path class="dark" d="M11 20h42v9H11z"/><path class="metal" d="M29 20h6v32h-6zM14 38h36v4H14z"/>'
  };
  return `<svg viewBox="0 0 64 64" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">${common}${shapes[kind]??shapes.gear}</svg>`;
}
function patchBackpack(){
  const apply=()=>{for(const row of document.querySelectorAll('.item:not(.has-art),.catalog-item:not(.has-art)')){if(row.closest('.heroes'))continue;const visual=document.createElement('div');visual.className='item-art';visual.setAttribute('aria-hidden','true');const kind=artKind(row.textContent);visual.dataset.artKind=kind;visual.innerHTML=artSvg(kind);row.classList.add('has-art');row.prepend(visual)}};apply();const root=document.getElementById('screen');if(root)new MutationObserver(()=>queueMicrotask(apply)).observe(root,{childList:true,subtree:true})
}

ensureCss();raf(()=>{if(page==='jornada.html')patchJourney();if(page==='ficha.html')patchCharacter();if(page==='scr014.html')patchAtlas();if(page==='criacao.html')patchCreation();if(page==='mochila.html')patchBackpack()});
