from pathlib import Path
root=Path(__file__).resolve().parents[1];base=root/'app/src/main/assets/vtt'
p=base/'js/expansion/aorTalentRuntimeAliases.mjs';s=p.read_text().replace("  // P167: Familiar Suns is a once-per-session Hard Knowledge planetary survey.","  // P167: Mental Fortress suppresses only Intellect/Cunning critical check penalties.\n  'FORTALEZA MENTAL': 'MENTALFORT',\n  // P167: Familiar Suns is a once-per-session Hard Knowledge planetary survey.");p.write_text(s)
p=base/'js/rules/critical.mjs';s=p.read_text().replace("  const ignoreCriticalCheck=heroicCheck||steelyCheck;", """  const mentalCheck=next.combatEffects?.mentalFortress === true && sourceBackedTalentRank(next,'MENTALFORT') > 0 &&
    ['intelecto','astucia'].includes(characteristicKey);
  const ignoreCriticalCheck=heroicCheck||steelyCheck||mentalCheck;""");p.write_text(s)
p=base/'js/core/controller.mjs';s=p.read_text();needle="    /** P161 — Steely Nerves: Destiny flip, only Willpower/Presence CHECK penalties. */";insert="""    /** P167 Mental Fortress: one light Destiny point suppresses ongoing
     * critical-injury penalties only for Intellect/Cunning checks. The injury
     * itself and action restrictions remain; effect ends with the encounter. */
    activateMentalFortress() {
        return this.atomicAction(()=>{
            let character=clone(requireCharacter(this.state.player.character));
            if(!this.state.combat?.active)throw new Error('Fortaleza mental exige encontro ativo.');
            assertCanAct(character);assertPlayerInitiativeTurn(this.state.combat);
            if(talents_mjs_1.sourceBackedTalentRank(character,'MENTALFORT')<1)
                throw new Error('Fortaleza mental não foi adquirida.');
            if(character.combatEffects?.mentalFortress)throw new Error('Fortaleza mental já está ativa neste encontro.');
            const group=destiny_mjs_1.flipDestiny(this.state.group,'light');
            character.combatEffects={...(character.combatEffects??{}),mentalFortress:true};
            const combat=clone(this.state.combat);
            combat.log.push({type:'FORTALEZA_MENTAL',round:combat.round,costDestiny:1});
            this.commit('COMBAT_ACTION',{character,group,combat});
            return {active:true,costDestiny:1};
        });
    }
""";assert needle in s;s=s.replace(needle,insert+needle)
s=s.replace("            familiarSuns:this.familiarSunsStatus(),", "            familiarSuns:this.familiarSunsStatus(),\n            mentalFortress:{available:talents_mjs_1.sourceBackedTalentRank(character,'MENTALFORT')>0,active:Boolean(character.combatEffects?.mentalFortress)},")
s=s.replace('if(character?.combatEffects?.heroicFortitude||character?.combatEffects?.steelyNerves){character=clone(character);delete character.combatEffects.heroicFortitude;delete character.combatEffects.steelyNerves;}','if(character?.combatEffects?.heroicFortitude||character?.combatEffects?.steelyNerves||character?.combatEffects?.mentalFortress){character=clone(character);delete character.combatEffects.heroicFortitude;delete character.combatEffects.steelyNerves;delete character.combatEffects.mentalFortress;}')
s=s.replace('delete character.combatEffects.steelyNerves;delete character.combatEffects.defensiveStanceTalent;', 'delete character.combatEffects.steelyNerves;delete character.combatEffects.mentalFortress;delete character.combatEffects.defensiveStanceTalent;')
p.write_text(s)
p=base/'js/core/uiBridge.mjs';s=p.read_text();needle='    activateSteelyNerves() {\n';insert="""    activateMentalFortress() {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.activateMentalFortress()));}
        catch(e){return fail('COMBAT_FAILED',messageOf(e));}
    }
""";assert needle in s;s=s.replace(needle,insert+needle);p.write_text(s)
p=base/'r46/final-runtime.js';s=p.read_text();s+="""

/* R54 P167 — Mental Fortress AoR: critical check suppression, not injury healing. */
(()=>{'use strict';if(window.__BRASEIRO_P167_MENTAL__)return;window.__BRASEIRO_P167_MENTAL__=true;
 const call=(method,...args)=>{try{return window.BraseiroGameBridge?.[method]?.(...args)||{ok:false,message:'Motor indisponível.'}}catch(e){return {ok:false,message:String(e?.message||e)}}};
 function mount(){const screen=new URLSearchParams(location.search).get('screen')||'00';if(!['01','02'].includes(screen))return;
  const status=call('talentActionStatus');if(!status?.ok||!status.data?.mentalFortress?.available)return;
  const host=document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
  if(host.querySelector('[data-p167-mental]'))return;
  const panel=document.createElement('section');panel.className='r54-p160-panel';panel.dataset.p167Mental='1';
  panel.innerHTML='<small>AGE OF REBELLION · P167</small><h3>FORTALEZA MENTAL</h3><p>Durante um encontro, gaste 1 ponto luminoso de Destino para ignorar penalidades de Ferimentos Críticos em testes de Intelecto e Astúcia. Não cura ferimentos nem remove incapacitação.</p><button data-p167-activate>ATIVAR FORTALEZA MENTAL</button><output aria-live="polite">Pronto.</output>';
  const button=panel.querySelector('[data-p167-activate]'),out=panel.querySelector('output');
  button.disabled=Boolean(status.data.mentalFortress.active||!status.data.combatActive);
  if(button.disabled)out.textContent=status.data.mentalFortress.active?'Já ativo neste encontro.':'Inicie um encontro para ativar.';
  button.addEventListener('click',()=>{const result=call('activateMentalFortress');out.textContent=result?.ok?'Destino gasto; proteção mental ativa até o fim do encontro.':String(result?.message||'Ação rejeitada.');if(result?.ok)button.disabled=true;});
  host.append(panel);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p167-mental-fortress',mount);
 else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0));}
})();
""";p.write_text(s)
print('Mental Fortress source, critical checks, combat state, bridge and Android UI integrated')
