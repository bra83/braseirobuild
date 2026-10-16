from pathlib import Path
root=Path(__file__).resolve().parent.parent
v=root/'app/src/main/assets/vtt'
def edit(path,needle,replacement):
 p=v/path;s=p.read_text();assert needle in s,(path,needle[:70]);p.write_text(s.replace(needle,replacement,1))
edit(Path('js/core/controller.mjs'),"import {queueJewelArrow7Turret", "import {resolveJewelArrow7Chase} from '../adventure/jewelArrow7Chase.mjs';\nimport {queueJewelArrow7Turret")
edit(Path('js/core/controller.mjs'),"    /** P199: GM-approved, real-price acquisition of a physical restraining bolt. */", """    /** P206: competitive planetary-scale chase at round start; NPC pilot sheet confirmed by GM. */
    resolveJewelArrow7Chase(details={}) {
        return this.atomicAction(()=>{
            const character=requireCharacter(this.state.player.character);
            const result=resolveJewelArrow7Chase(this.state.world,this.state.combat,character,details);
            this.commit('WORLD_UPDATED',{world:result.world});
            this.commit('COMBAT_ACTION',{combat:result.combat});
            return {adjudication:result.adjudication,chase:result.chase,state:clone(this.state)};
        });
    }
    /** P199: GM-approved, real-price acquisition of a physical restraining bolt. */""")
edit(Path('js/core/uiBridge.mjs'),"    /** P199: buy a real restraining bolt", """    /** P206: competitive Arrow-7 pursuit with real narrative dice and GM-confirmed NPC pilot. */
    resolveJewelArrow7Chase(details={}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.resolveJewelArrow7Chase(details??{})));}
        catch(error){return fail('JEWEL_ARROW7_CHASE_FAILED',messageOf(error));}
    }
    /** P199: buy a real restraining bolt""")
edit(Path('js/adventure/jewelOfYavin.mjs'),"import {validateJewelArrow7Turret", "import {validateJewelArrow7Chase} from './jewelArrow7Chase.mjs';\nimport {validateJewelArrow7Turret")
s=(v/'js/adventure/jewelOfYavin.mjs').read_text();s=s.replace('  validateJewelArrow7Turret(p);','  validateJewelArrow7Turret(p);\n  validateJewelArrow7Chase(p);');s=s.replace("    arrow7Turret: p.operations?.arrow7Turret ? clone(p.operations.arrow7Turret) : null,","    arrow7Turret: p.operations?.arrow7Turret ? clone(p.operations.arrow7Turret) : null,\n    arrow7Chase: p.operations?.arrow7Chase ? clone(p.operations.arrow7Chase) : null,");(v/'js/adventure/jewelOfYavin.mjs').write_text(s)
# UI: source-backed chase controls in same official encounter panel, without pretending pilot stats exist in the PDF.
path=v/'r46/final-runtime.js';s=path.read_text();anchor=" const companionImpactUi=(adventureId==='jewel-of-yavin'";assert anchor in s
ui=""" const chaseState=status?.arrow7Chase||null;
 const arrow7ChaseUi=arrow7Active?`<section class="final-published-action-zone"><div class="final-published-action-title">ARROW-7 · PERSEGUIÇÃO COMPETITIVA · ESCALA PLANETÁRIA</div><small>Edge of the Empire, The Chase: ambos pilotos rolam Pilotagem Planetária no INÍCIO da rodada. Vitória muda uma faixa; velocidades verificadas permitem faixas adicionais. ${chaseState?`Rodada ${chaseState.round}: ${h(chaseState.range)} (${h(chaseState.status)}).`:'Sem perseguição iniciada.'} Não inventar velocidade ou ficha do piloto de Arend.</small><label>Jogador é <select data-jewel-chase-role><option value="evader">Perseguido</option><option value="pursuer">Perseguidor</option></select></label><label>Distância planetária inicial <select data-jewel-chase-range><option>Curto</option><option selected>Médio</option><option>Longo</option><option>Extremo</option><option>Bocajarro</option></select></label><label>Ficha confirmada do piloto NPC (ID) <input data-jewel-chase-npc-id placeholder="ID da ficha do piloto"></label><label>Agilidade do piloto NPC <input data-jewel-chase-npc-agility type="number" min="0" max="7" placeholder="conferir ficha"></label><label>Pilotagem Planetária do NPC <input data-jewel-chase-npc-rank type="number" min="0" max="7" placeholder="conferir ficha"></label><label>Dificuldade do terreno (0 = simples) <input data-jewel-chase-difficulty type="number" min="0" max="5" value="0"></label><label>Revés do terreno <input data-jewel-chase-setback type="number" min="0" max="6" value="0"></label><label><input type="checkbox" data-jewel-chase-both> Mestre confirma DOIS veículos na perseguição</label><label><input type="checkbox" data-jewel-chase-terrain> Mestre confirma terreno e dificuldade</label><label><input type="checkbox" data-jewel-chase-start> Mestre confirma distância planetária inicial real</label><label><input type="checkbox" data-jewel-chase-pilot> Mestre confirma ficha real do piloto NPC (não publicada em SWE09)</label><label><input type="checkbox" data-jewel-chase-speeds> Velocidades atuais de AMBOS os veículos verificadas</label><label>Velocidade atual do jogador <input data-jewel-chase-player-speed type="number" min="0" max="6" placeholder="somente se verificada"></label><label>Velocidade atual Arrow-7 <input data-jewel-chase-arrow-speed type="number" min="0" max="6" placeholder="somente se verificada"></label><button type="button" data-jewel-chase-roll>ROLAR PILOTAGEM COMPETITIVA DA RODADA</button><small>Empates mantêm distância. Alcance e velocidades são independentes da distância PESSOAL do canhão. Contato/fuga exigem desfecho do Mestre; sem teleporte, sem blindagem/casco inventados.</small></section>`:'';
"""
s=s.replace(anchor,ui+anchor,1)
# Find where UI assembled
import re
hits=[(m.start(),s[m.start()-110:m.start()+140]) for m in re.finditer('arrow7Ui',s)];print('UI occurrences',hits[-5:]);
# insert chase UI immediately following turret UI in interpolation
if '${arrow7Ui}' in s:s=s.replace('${arrow7Ui}','${arrow7Ui}${arrow7ChaseUi}',1)
else: raise ValueError('no UI injection')
# add event listener before turret
anchor=" panel.querySelector('[data-jewel-arrow7-queue]')?.addEventListener('click',()=>{";assert anchor in s
handler=""" panel.querySelector('[data-jewel-chase-roll]')?.addEventListener('click',()=>{
  const btn=panel.querySelector('[data-jewel-chase-roll]');btn.disabled=true;
  const v=key=>panel.querySelector(`[data-jewel-chase-${key}]`),n=key=>v(key)?.value===''?null:Number(v(key)?.value),yes=key=>Boolean(v(key)?.checked);
  const details={actionId:'p206-arrow7-chase-'+Date.now()+'-'+Math.floor(Math.random()*1e6),playerRole:v('role')?.value,initialRange:v('range')?.value,
   arrow7Pilot:{id:v('npc-id')?.value,agility:n('npc-agility'),rank:n('npc-rank')},terrainDifficulty:n('difficulty'),terrainSetback:n('setback'),
   gmBothVehiclesConfirmed:yes('both'),gmTerrainConfirmed:yes('terrain'),gmStartingRangeConfirmed:yes('start'),gmNpcPilotSheetConfirmed:yes('pilot'),
   gmSpeedsVerified:yes('speeds'),playerSpeed:n('player-speed'),arrow7Speed:n('arrow-speed')};
  const r=bridgeResult('resolveJewelArrow7Chase',details);
  if(!r?.ok){btn.disabled=false;panel.querySelector('output').textContent=r?.message||'Perseguição recusada.';return;}
  enhancePublishedAdventurePanel();const out=document.querySelector('.final-published-adventure-panel output');
  if(out)out.textContent=`Perseguição rodada ${r.data?.adjudication?.round}: ${r.data?.adjudication?.newRange} (escala planetária), ${r.data?.adjudication?.status}.`;
 });
"""
s=s.replace(anchor,handler+anchor,1)
s=s.replace('R54 P205 · Android 1.0.205','R54 P206 · Android 1.0.206');path.write_text(s)
# Android version
p=root/'app/build.gradle.kts';s=p.read_text().replace('versionCode = 10205','versionCode = 10206').replace('versionName = "1.0.205"','versionName = "1.0.206"');p.write_text(s)
print('Integrated P206')
