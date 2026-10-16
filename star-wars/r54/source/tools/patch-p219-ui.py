from pathlib import Path
p=Path(__file__).resolve().parents[1]/'app/src/main/assets/vtt/r46/final-runtime.js'
s=p.read_text()
anchor=' const holoFactionUi=holoFactionReady?'
assert anchor in s
new=''' const shenAftermath=status?.shenAftermath||null;
 const shenReady=adventureId==='jewel-of-yavin'&&['ACTIVE','COMPLETED'].includes(status?.progress?.status)&&
  status?.progress?.operations?.escapeBomb&&status?.progress?.encounterHistory?.some(x=>x.encounterId==='arend-shen-four-goons'&&x.outcome)&&
  !currentState()?.combat?.active;
 const shenUi=shenReady?`<section class="final-published-action-zone"><div class="final-published-action-title">SWE09 PP.90–91,95 · DESTINOS DE AREND E ARIS</div><small>Registre apenas acontecimentos efetivamente jogados. A morte de Arend encerra seus planos pessoais; se livre ou preso, continua inimigo recorrente. Aliança com Aris requer consentimento mútuo. Não concede XP, recompensa, teleporte ou captura automaticamente.</small>${shenAftermath?.arend?`<strong>Arend: ${h(shenAftermath.arend.status)} · local ${h(shenAftermath.arend.holdingId)}.</strong>`:status?.arendRevenge?.foundAt?`<label>Destino após confronto <select data-p219-arend-status><option value="DEAD">Morto — comprovado pelo Mestre</option><option value="INCARCERATED">Preso — custódia confirmada</option><option value="AT_LARGE">Livre — fuga/sobrevivência confirmada</option></select></label><label><input type="checkbox" data-p219-arend-identity> Mestre confirma que era realmente Arend Shen</label><label><input type="checkbox" data-p219-arend-confront> Mestre confirma confronto físico ocorrido no local encontrado</label><label><input type="checkbox" data-p219-arend-result> Mestre confirma o resultado selecionado (morte/custódia/fuga)</label><button type="button" data-p219-arend-record>REGISTRAR DESTINO DE AREND</button>`:'<small>Para registrar Arend, localize-o primeiro pelos quatro imóveis oficiais.</small>'}${shenAftermath?.aris?'<strong>Aris: aliança confirmada e persistida.</strong>':`<label><input type="checkbox" data-p219-aris-meeting> Mestre confirma encontro real com Aris</label><label><input type="checkbox" data-p219-aris-agree> Mestre confirma que Aris concordou com a aliança</label><label><input type="checkbox" data-p219-aris-party> Mestre confirma que os PJs aceitaram a aliança</label><label><input type="checkbox" data-p219-aris-peace> Mestre confirma que Aris não está mais hostil</label><button type="button" data-p219-aris-record>REGISTRAR ALIANÇA COM ARIS</button>`}</section>`:'';
'''
s=s.replace(anchor,new+anchor,1)
s=s.replace('${holoFactionUi}${landoPardonUi}', '${holoFactionUi}${shenUi}${landoPardonUi}',1)
anchor=" panel.querySelector('[data-p218-lando-grant]')?.addEventListener('click',()=>{"
assert anchor in s
new=''' panel.querySelector('[data-p219-arend-record]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-p219-arend-record]');btn.disabled=true;
  const status=panel.querySelector('[data-p219-arend-status]')?.value;
  const outcome=Boolean(panel.querySelector('[data-p219-arend-result]')?.checked);
  const r=bridgeResult('recordJewelArendOutcome',{
   actionId:'p219-arend-'+Date.now()+'-'+Math.floor(Math.random()*1e6),status,
   gmArendIdentified:Boolean(panel.querySelector('[data-p219-arend-identity]')?.checked),
   gmPhysicalConfrontationConfirmed:Boolean(panel.querySelector('[data-p219-arend-confront]')?.checked),
   gmOutcomeConfirmed:outcome,gmDeathConfirmed:outcome&&status==='DEAD',
   gmCustodyConfirmed:outcome&&status==='INCARCERATED',gmEscapeConfirmed:outcome&&status==='AT_LARGE'});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Desfecho não registrado.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent='Destino de Arend registrado com recibo. Nenhum XP ou recompensa concedido automaticamente.';
 });
 panel.querySelector('[data-p219-aris-record]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-p219-aris-record]');btn.disabled=true;
  const r=bridgeResult('recordJewelArisAlliance',{
   actionId:'p219-aris-'+Date.now()+'-'+Math.floor(Math.random()*1e6),
   gmArisMetInPerson:Boolean(panel.querySelector('[data-p219-aris-meeting]')?.checked),
   gmArisAgreedToAlliance:Boolean(panel.querySelector('[data-p219-aris-agree]')?.checked),
   gmPartyAcceptedAlliance:Boolean(panel.querySelector('[data-p219-aris-party]')?.checked),
   gmArisNoLongerHostile:Boolean(panel.querySelector('[data-p219-aris-peace]')?.checked)});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Aliança não registrada.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent='Aliança com Aris registrada com recibo. XP depende de adjudicação separada.';
 });
'''
s=s.replace(anchor,new+anchor,1)
p.write_text(s)
