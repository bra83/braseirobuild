from pathlib import Path
p=Path(__file__).resolve().parent/'app/src/main/assets/vtt/r46/final-runtime.js'
s=p.read_text()
needle=' const holoFactionUi=holoFactionReady?'
assert s.count(needle)==1
ui=''' const arendPatrol=status?.arendPatrols||null;
 const patrolResidence=arendPatrol?.residence||null,patrolVenture=arendPatrol?.venture||null;
 const arendPatrolUi=arendPropertyReady&&['plaza-residence','plaza-yacht'].includes(arendHolding)?`<section class="final-published-action-zone"><div class="final-published-action-title">SWE09 PP.90–91 · GUARDA ALADA · RELÓGIO REAL</div><small>Tempo confirmado: ${Number(arendPatrol?.elapsedMinutes??0)} minutos (tarefas persistidas). O relógio NÃO avança sozinho; só tarefas concluídas e contabilizadas contam.</small>${arendHolding==='plaza-residence'?(!patrolResidence?`<label><input type="checkbox" data-p221-outdoors> Mestre confirma grupo fisicamente na rua da residência</label><button type="button" data-p221-residence-start>INICIAR EXPOSIÇÃO ÀS PATRULHAS</button>`:patrolResidence.endedAt!==null?`<strong>Exposição encerrada aos ${patrolResidence.endedAt} minutos.</strong>`:`<small>Patrulha a cada 15 minutos. Próximo teste no minuto ${patrolResidence.nextDueMinute}. ${patrolResidence.due?'PATRULHA DEVIDA — teste Difícil obrigatório.':`Faltam ${patrolResidence.minutesUntilDue} minutos reais.`}</small><label>Perícia do PJ <select data-p221-patrol-skill><option value="engano">Engano</option><option value="sigilo">Furtividade</option><option value="callejeo">Conhecimento das Ruas</option></select></label><button type="button" data-p221-patrol-roll ${patrolResidence.due?'':'disabled'}>ROLAR TESTE DIFÍCIL REAL CONTRA PATRULHA</button><label><input type="checkbox" data-p221-outdoors-end> Mestre confirma saída física da rua</label><button type="button" data-p221-residence-end ${patrolResidence.due?'disabled':''}>ENCERRAR EXPOSIÇÃO</button><small>Falha e símbolos narrativos ficam registrados; chegada de guardas e consequências exigem decisão do Mestre, sem teleporte.</small>`):(!patrolVenture?`<label><input type="checkbox" data-p221-hostilities> Mestre confirma hostilidades reais contra a tripulação do Venture</label><label><input type="checkbox" data-p221-transponder> Mestre confirma Arend reativando o transponder</label><button type="button" data-p221-venture-alert>REGISTRAR ALERTA DOS CLOUD CARS</button>`:`<small>Alerta no minuto ${patrolVenture.alertAt}; prazo oficial até minuto ${patrolVenture.deadlineMinute}. Estado: ${h(patrolVenture.status)}. Dois voos de dois cloud cars (quatro veículos).</small>${!patrolVenture.arrival?`<label><input type="checkbox" data-p221-physical-arrival> Mestre confirma chegada FÍSICA dos voos ao iate</label><label><input type="checkbox" data-p221-four-vehicles> Mestre confirma quatro veículos distintos (dois voos de dois)</label><button type="button" data-p221-venture-arrival>REGISTRAR CHEGADA REAL</button>`:'<strong>Chegada registrada com recibo, sem combate ou movimentação inventados.</strong>'}`)}</section>`:'';
'''
s=s.replace(needle,ui+needle)
needle='${arendPropertyUi}${shenUi}'
assert s.count(needle)==1;s=s.replace(needle,'${arendPropertyUi}${arendPatrolUi}${shenUi}')
needle=" panel.querySelector('[data-p220-start]')?.addEventListener('click',()=>{"
assert s.count(needle)==1
handlers=''' const p221Id=(label)=>'p221-'+label+'-'+Date.now()+'-'+Math.floor(Math.random()*1e6);
 const p221Action=(selector,method,details,message)=>panel.querySelector(selector)?.addEventListener('click',()=>{
  const btn=panel.querySelector(selector);btn.disabled=true;
  const r=bridgeResult(method,{actionId:p221Id(selector.replace(/[^a-z0-9]/gi,'')),...details()});
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Ação temporal recusada.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent=message;
 });
 p221Action('[data-p221-residence-start]','recordJewelArendPatrol',()=>({type:'RESIDENCE_START',gmOutdoorsConfirmed:Boolean(panel.querySelector('[data-p221-outdoors]')?.checked)}),'Exposição à Guarda Alada registrada no relógio persistente.');
 p221Action('[data-p221-residence-end]','recordJewelArendPatrol',()=>({type:'RESIDENCE_END',gmLeftOutdoorsConfirmed:Boolean(panel.querySelector('[data-p221-outdoors-end]')?.checked)}),'Saída da rua registrada sem apagar patrulhas devidas.');
 p221Action('[data-p221-patrol-roll]','resolveJewelResidencePatrolCheck',()=>({skillKey:panel.querySelector('[data-p221-patrol-skill]')?.value}),'Teste Difícil rolado no motor de dados real; consequências aguardam Mestre.');
 p221Action('[data-p221-venture-alert]','recordJewelArendPatrol',()=>({type:'VENTURE_ALERT',gmCrewHostilitiesConfirmed:Boolean(panel.querySelector('[data-p221-hostilities]')?.checked),gmTransponderActivatedConfirmed:Boolean(panel.querySelector('[data-p221-transponder]')?.checked)}),'Transponder e prazo oficial dos cloud cars registrados.');
 p221Action('[data-p221-venture-arrival]','recordJewelArendPatrol',()=>({type:'VENTURE_ARRIVAL',gmPhysicalArrivalConfirmed:Boolean(panel.querySelector('[data-p221-physical-arrival]')?.checked),gmFourDistinctVehiclesConfirmed:Boolean(panel.querySelector('[data-p221-four-vehicles]')?.checked)}),'Dois voos de dois cloud cars chegaram fisicamente, sem combate automático.');
'''
s=s.replace(needle,handlers+needle)
s=s.replace('A chegada de patrulha ao iate em dez minutos requer relógio real e ainda não é automatizada.','Patrulhas e alerta de dez minutos usam o relógio real no painel abaixo.')
p.write_text(s)
print('UI P221 wired')
