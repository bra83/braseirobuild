from pathlib import Path

p=Path('app/src/main/assets/www/jornada.js')
s=p.read_text()

imp="import { PlayerDeclarationService } from './core/player-declaration-service.js';"
new_imp=imp+"\nimport { PlayerMechanicsFacade } from './core/player-mechanics-facade.js';"
if "player-mechanics-facade.js" not in s:
    if imp not in s: raise SystemExit('PlayerDeclarationService import marker not found')
    s=s.replace(imp,new_imp,1)

inst="const declarations=new PlayerDeclarationService({persistence,barbara});"
new_inst=inst+"\nconst mechanics=new PlayerMechanicsFacade({persistence});"
if "const mechanics=new PlayerMechanicsFacade" not in s:
    if inst not in s: raise SystemExit('declarations marker not found')
    s=s.replace(inst,new_inst,1)

state_marker="let loaded=scenes.load({campaignId});let projection=loaded.projection;"
if "let mechanicsSummary=" not in s:
    if state_marker not in s: raise SystemExit('loaded/projection marker not found')
    s=s.replace(state_marker,state_marker+"let mechanicsSummary=mechanics.summary({campaignId});",1)

anchor="function card(item,type){"
block=r'''const WWN_SKILLS=['Administer','Connect','Convince','Craft','Exert','Heal','Know','Lead','Magic','Notice','Perform','Pray','Punch','Ride','Sail','Shoot','Sneak','Stab','Survive','Trade','Work'];
const ATTR_LABELS={Strength:'FORÇA',Dexterity:'DESTREZA',Constitution:'CONSTITUIÇÃO',Intelligence:'INTELIGÊNCIA',Wisdom:'SABEDORIA',Charisma:'CARISMA'};
function selectOptions(select,rows,{placeholder=null,preserve=true}={}){if(!select)return;const prior=preserve?select.value:null;select.innerHTML='';if(placeholder!==null){const o=document.createElement('option');o.value='';o.textContent=placeholder;select.appendChild(o)}for(const row of rows){const o=document.createElement('option');o.value=String(row.value);o.textContent=String(row.label);select.appendChild(o)}if(prior&&[...select.options].some(o=>o.value===prior))select.value=prior;}
function mechanicsStatus(text,tone=''){const el=$('mechanicsResult');if(!el)return;el.textContent=text;el.className=`mechanics-result${tone?` ${tone}`:''}`;}
function diceText(resolution){const dice=resolution?.dice??[];return dice.map(d=>typeof d==='number'?d:d?.result).filter(Number.isFinite).join(' + ')}
function resultText(result,kind){if(!result)return{tone:'warn',text:'A resolução não retornou resultado.'};if(result.status!=='OK')return{tone:'warn',text:`Não executado: ${String(result.status).replaceAll('_',' ')}.`};const r=result.resolution??result.mechanicalEvent?.resolution??null;if(kind==='TEST'&&r)return{tone:r.success?'ok':'fail',text:`${r.success?'SUCESSO':'FALHA'} • ${diceText(r)||'dados'} • total ${r.total} vs dificuldade ${r.difficulty}.`};if(kind==='SAVE'&&r)return{tone:r.success?'ok':'fail',text:`${r.success?'SAVE BEM-SUCEDIDO':'SAVE FALHOU'} • d20 ${r.roll??diceText(r)} • alvo ${r.target??r.difficulty??'—'}.`};if(kind==='ATTACK'){const hit=r?.hit??r?.success??result.hit??null;const damage=r?.damage??result.damage??null;const shock=r?.shockDamage??result.shockDamage??null;const parts=[hit===true?'ACERTO':hit===false?'ERRO':'ATAQUE RESOLVIDO'];if(Number.isFinite(damage))parts.push(`dano ${damage}`);if(Number.isFinite(shock)&&shock>0)parts.push(`Shock ${shock}`);return{tone:hit===false?'fail':'ok',text:parts.join(' • ')+'.'}}if(kind==='COMBAT')return{tone:'ok',text:'Combate iniciado. Iniciativa e estado foram persistidos.'};if(kind==='RELOAD')return{tone:'ok',text:`Arma recarregada${result.automaticUsed?' automaticamente':''}${result.ammoConsumed?'; munição consumida':''}.`};return{tone:'ok',text:'Mecânica executada e persistida.'}}
function renderMechanics(){if(!mechanicsSummary||mechanicsSummary.status!=='OK'){mechanicsStatus(`Mecânica indisponível: ${mechanicsSummary?.status??'estado ausente'}.`,'warn');return}const c=mechanicsSummary.character;$('mechanicsCharacter').textContent=`${c.name} • NV ${c.level}`;selectOptions($('mechanicsAttribute'),Object.keys(c.attributes??{}).map(k=>({value:k,label:ATTR_LABELS[k]??k})));const known=new Set(Object.keys(c.skills??{}));selectOptions($('mechanicsSkill'),WWN_SKILLS.map(k=>({value:k,label:`${k}${known.has(k)?` ${c.skills[k]>=0?`+${c.skills[k]}`:c.skills[k]}`:' (não treinado)'}`})));selectOptions($('mechanicsEnemy'),mechanicsSummary.combat.enemies.map(e=>({value:e.id,label:`${e.name} • PV ${e.hp}${Number.isFinite(e.maxHp)?`/${e.maxHp}`:''}`})),{placeholder:mechanicsSummary.combat.enemies.length?'SELECIONE O ALVO':'SEM ALVOS'});selectOptions($('mechanicsWeapon'),mechanicsSummary.inventory.weapons.map(w=>({value:w.itemId,label:`${w.name}${w.itemId!=='UNARMED'&&w.carryState!=='READIED'?' • guardada':''}${w.loaded===false?' • descarregada':''}`})));const cs=$('combatState');if(cs)cs.textContent=mechanicsSummary.combat.active?`Combate • rodada ${mechanicsSummary.combat.round??'—'} • vez: ${mechanicsSummary.combat.activeSideId??'—'}`:`Fora de combate • ${mechanicsSummary.combat.enemies.length} alvo(s) mecânico(s) disponível(is).`;const enemyCount=mechanicsSummary.combat.enemies.length;$('mechanicsStartCombatButton').disabled=mechanicsSummary.combat.active||enemyCount===0;$('mechanicsAttackButton').disabled=enemyCount===0;$('mechanicsReloadButton').disabled=$('mechanicsWeapon').value==='UNARMED';}
function refreshMechanics(){mechanicsSummary=mechanics.summary({campaignId});renderMechanics();renderBars();return mechanicsSummary}
function applyMechanicsResult(result,kind){if(result?.summary?.status==='OK')mechanicsSummary=result.summary;else mechanicsSummary=mechanics.summary({campaignId});renderMechanics();renderBars();const view=resultText(result,kind);mechanicsStatus(view.text,view.tone);}
function setupMechanicsUi(){for(const b of document.querySelectorAll('[data-mechanics-tab]'))b.onclick=()=>{for(const x of document.querySelectorAll('[data-mechanics-tab]'))x.classList.toggle('active',x===b);for(const id of ['mechanicsTestPane','mechanicsSavePane','mechanicsCombatPane'])$(id).classList.add('hidden');$(`mechanics${b.dataset.mechanicsTab[0].toUpperCase()+b.dataset.mechanicsTab.slice(1)}Pane`).classList.remove('hidden')};$('mechanicsTestButton').onclick=()=>{const difficulty=Number($('mechanicsDifficulty').value),skill=$('mechanicsSkill').value,attribute=$('mechanicsAttribute').value;const result=mechanics.skillCheck({campaignId,skill,attribute,difficulty});applyMechanicsResult(result,'TEST')};for(const b of document.querySelectorAll('[data-save]'))b.onclick=()=>applyMechanicsResult(mechanics.rollSave({campaignId,saveType:b.dataset.save}),'SAVE');$('mechanicsStartCombatButton').onclick=()=>applyMechanicsResult(mechanics.startCombat({campaignId}),'COMBAT');$('mechanicsAttackButton').onclick=()=>{const npcId=$('mechanicsEnemy').value,weaponId=$('mechanicsWeapon').value;if(!npcId){mechanicsStatus('Selecione um alvo antes de atacar.','warn');return}applyMechanicsResult(mechanics.attack({campaignId,npcId,weaponId}),'ATTACK')};$('mechanicsReloadButton').onclick=()=>{const weaponId=$('mechanicsWeapon').value;if(!weaponId||weaponId==='UNARMED'){mechanicsStatus('Selecione uma arma recarregável.','warn');return}applyMechanicsResult(mechanics.reload({campaignId,weaponId}),'RELOAD')};$('mechanicsWeapon').onchange=()=>{$('mechanicsReloadButton').disabled=$('mechanicsWeapon').value==='UNARMED'};refreshMechanics();}
'''
if "function setupMechanicsUi()" not in s:
    if anchor not in s: raise SystemExit('card anchor not found')
    s=s.replace(anchor,block+anchor,1)

end="renderBars();renderScene(loaded.narrative);renderMasterStatus();syncAudio();window.__BRASEIRO_JORNADA__={campaignId,projection:()=>structuredClone(projection),openScene};"
new_end="renderBars();renderScene(loaded.narrative);renderMasterStatus();syncAudio();setupMechanicsUi();window.__BRASEIRO_JORNADA__={campaignId,projection:()=>structuredClone(projection),mechanicsSummary:()=>structuredClone(mechanicsSummary),refreshMechanics,openScene};"
if end in s:s=s.replace(end,new_end,1)
elif "setupMechanicsUi();window.__BRASEIRO_JORNADA__" not in s: raise SystemExit('final render anchor not found')

p.write_text(s)
