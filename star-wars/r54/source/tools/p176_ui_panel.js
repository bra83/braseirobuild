/* P176 — AoR Engineering, Intelligence and Slicing. PT-BR, no invented results. */
(()=>{'use strict';
 if(window.__BRASEIRO_P176_ADVANCED__)return;window.__BRASEIRO_P176_ADVANCED__=true;
 const call=(method,args)=>{try{const b=window.BraseiroGameBridge;return typeof b?.[method]==='function'?b[method](args??{}):{ok:false,message:'Ponte indisponível'}}catch(e){return {ok:false,message:String(e?.message??e)}}};
 const uid=()=>`p176-${Date.now()}-${Math.floor(Math.random()*1e8)}`;
 const route=()=>new URLSearchParams(location.search).get('screen')||'00';
 function mount(){
  const screen=route();document.querySelectorAll('[data-p176-advanced]').forEach(p=>{if(p.dataset.p176Advanced!==screen)p.remove()});
  if(!['02','06'].includes(screen)||document.querySelector(`[data-p176-advanced="${screen}"]`))return;
  const r=call('aorP176Status');if(!r?.ok)return;const s=r.data;
  if(!s?.sessionActive||!(s.boughtInfo||s.knownSchematic||s.fireControl||s.skilledSlicer||s.masterSlicer))return;
  const host=document.querySelector(`[data-p93-mechanics="${screen}"]`)||document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
  const p=document.createElement('section');p.dataset.p176Advanced=screen;p.style.cssText='margin:12px 14px 110px;padding:12px;border:1px solid #557b91;border-radius:10px;background:#0c1720;color:#e3f2ff;display:grid;gap:8px';
  const title=document.createElement('h3');title.textContent='P176 · ENGENHARIA, INFORMAÇÃO E SLICER AoR';p.append(title);
  const out=document.createElement('output');out.style.cssText='display:block;white-space:pre-wrap;padding:8px;background:#071018';out.textContent='Escolha uma ação. O Mestre deve confirmar fatos do cenário; o jogo não inventa informação.';
  const row=(label,control)=>{const e=document.createElement('label');e.textContent=label+' ';e.append(control);p.append(e);return control};
  const field=(label,placeholder)=>{const i=document.createElement('input');i.type='text';i.placeholder=placeholder;i.maxLength=120;return row(label,i)};
  const check=(label)=>{const i=document.createElement('input');i.type='checkbox';return row(label,i)};
  const button=(label,run)=>{const b=document.createElement('button');b.textContent=label;b.style.cssText='min-height:44px;background:#23445b;color:white;border:1px solid #668ba2;border-radius:6px';b.addEventListener('click',()=>{const r=run();out.textContent=r?.ok?JSON.stringify(r.data,null,2).slice(0,1000):String(r?.message||'Ação recusada');out.style.color=r?.ok?'#b9f6c2':'#ffb8b8';});p.append(b);return b};
  if(s.knownSchematic){const subject=field('Nave/edifício conhecido pelo Mestre','identificador_do_local'),approved=check('Mestre confirma alvo arquitetônico real');button('PROJETO CONHECIDO · CONHECIMENTO DIFÍCIL (3)',()=>call('useAoRKnownSchematic',{actionId:uid(),subjectId:subject.value,approved:approved.checked}));}
  if(s.boughtInfo){const subject=field('Assunto pesquisado','identificador_da_informacao'),approved=check('Mestre confirma informação disponível para compra');const d=document.createElement('select');for(let i=1;i<=5;i++){const o=document.createElement('option');o.value=i;o.textContent=`Dificuldade ${i} · ${i*50} créditos`;d.append(o)}row('Dificuldade da informação',d);const skills=document.createElement('select');for(const [key,label] of [['educacion','Educação'],['borde_exterior','Borda Exterior'],['mundos_del_nucleo','Mundos do Núcleo'],['bajos_fondos','Submundo'],['estrategia_militar','Estratégia militar'],['xenologia','Xenologia']]){const o=document.createElement('option');o.value=key;o.textContent=label;skills.append(o)}row('Perícia de Conhecimento',skills);button('COMPRAR INFORMAÇÃO · 1 SUCESSO',()=>call('useAoRBoughtInfo',{actionId:uid(),subjectId:subject.value,skillKey:skills.value,difficulty:Number(d.value),approved:approved.checked}));}
  if(s.fireControl&&s.vehicleCombatActive)button('FOGO CONTROLADO · MANOBRA VEICULAR',()=>call('useAoRFireControl',{}));
  if(s.skilledSlicer){const system=field('Sistema informático real','identificador_do_sistema');button('SLICER CONSUMADO · GASTAR TRIUNFO',()=>call('resolveSkillCheck',{skillKey:'computadoras',difficulty:2,slicingSystemId:system.value,spendSkilledSlicerTriumph:true}));button('SLICER CONSUMADO · COMPUTADORES COMO MANOBRA',()=>call('resolveSkillCheck',{skillKey:'computadoras',difficulty:2,slicingSystemId:system.value,useSkilledSlicerManeuver:true}));}
  if(s.masterSlicer)button('SLICER EXPERTO · 2 TENSÃO / −1 DIFICULDADE',()=>call('resolveSkillCheck',{skillKey:'computadoras',difficulty:2,useMasterSlicer:true}));
  p.append(out);host.append(p);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p176-aor-advanced',mount);
 else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0));}
})();
