from pathlib import Path
root=Path(__file__).resolve().parents[1];base=root/'app/src/main/assets/vtt'
p=base/'js/expansion/aorTalentRuntimeAliases.mjs';s=p.read_text().replace("  // P166: Gearhead is a ranked Mechanics setback remover and halves attachment Mod cost.","  // P167: Familiar Suns is a once-per-session Hard Knowledge planetary survey.\n  'SOLES FAMILIARES': 'FAMILIARSUNS',\n  // P166: Gearhead is a ranked Mechanics setback remover and halves attachment Mod cost.");p.write_text(s)
p=base/'js/core/controller.mjs';s=p.read_text();key='    talentActionStatus() {\n';insert="""    /** P167: Familiar Suns, one maneuver/check per session, irrespective of
     * whether both AoR tree slots are purchased. Never fabricate planetary
     * inhabitants or hazards: the GM reveals source-world information only
     * after the real Hard Knowledge check succeeds. */
    familiarSunsStatus() {
        const character=requireCharacter(this.state.player.character);
        const rank=talents_mjs_1.sourceBackedTalentRank(character,'FAMILIARSUNS');
        return {available:rank>0,rank,usedThisSession:talentUsedThisSession(character,'familiarSuns'),
            sessionActive:this.state.session?.status==='ativa',difficulty:3,
            skills:['borde_exterior','mundos_del_nucleo'],
            revelationCategories:['habitação','ambiente','predadores','riscos','oportunidades']};
    }
    useFamiliarSuns({skillKey='borde_exterior',rng=Math.random}={}) {
        return this.atomicAction(()=>{
            const status=this.familiarSunsStatus();
            if(!status.available)throw new Error('Sóis familiares não foi adquirido na árvore AoR.');
            if(!status.sessionActive)throw new Error('Sóis familiares exige sessão ativa.');
            if(status.usedThisSession)throw new Error('Sóis familiares já foi utilizado nesta sessão.');
            if(!status.skills.includes(skillKey))throw new Error('Sóis familiares exige Conhecimento Borda Exterior ou Mundos do Núcleo.');
            if(this.state.combat?.active)throw new Error('Sóis familiares requer uma manobra fora do combate neste painel.');
            const check=this.resolveSkillCheck({skillKey,difficulty:3,rng});
            const character=markTalentUsedThisSession(this.state.player.character,'familiarSuns');
            this.commit('CHARACTER_UPDATED',{character});
            return {used:true,skillKey,difficulty:3,check,success:Boolean(check.success),
                gmRevelationRequired:Boolean(check.success),
                revelationCategories:check.success?status.revelationCategories:[]};
        });
    }
"""
assert key in s;s=s.replace(key,insert+key);s=s.replace("            intenseFocus:talents_mjs_1.intenseFocusOption(character),", "            familiarSuns:this.familiarSunsStatus(),\n            intenseFocus:talents_mjs_1.intenseFocusOption(character),");p.write_text(s)
p=base/'js/core/uiBridge.mjs';s=p.read_text();key='    talentActionStatus() {\n';insert="""    familiarSunsStatus() {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.familiarSunsStatus()));}
        catch(e){return fail('FAMILIAR_SUNS_STATUS_FAILED',messageOf(e));}
    }
    useFamiliarSuns(options={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.useFamiliarSuns(options??{})));}
        catch(e){return fail('FAMILIAR_SUNS_FAILED',messageOf(e));}
    }
""";assert key in s;s=s.replace(key,insert+key);p.write_text(s)
p=base/'r46/final-runtime.js';s=p.read_text();s+="""

/* R54 P167 — Familiar Suns AoR, no invented planetary facts. */
(()=>{'use strict';if(window.__BRASEIRO_P167_SUNS__)return;window.__BRASEIRO_P167_SUNS__=true;
 const call=(method,...args)=>{try{return window.BraseiroGameBridge?.[method]?.(...args)||{ok:false,message:'Motor indisponível.'}}catch(e){return {ok:false,message:String(e?.message||e)}}};
 function mount(){const screen=new URLSearchParams(location.search).get('screen')||'00';if(!['01','02'].includes(screen))return;
  const status=call('familiarSunsStatus');if(!status?.ok||!status.data?.available)return;
  const host=document.querySelector('.app-content,main,.screen-content,.page-content,.r46-content')||document.body;
  if(host.querySelector('[data-p167-suns]'))return;
  const panel=document.createElement('section');panel.className='r54-p160-panel';panel.dataset.p167Suns='1';
  panel.innerHTML='<small>AGE OF REBELLION · P167</small><h3>SÓIS FAMILIARES</h3><p>Uma vez por sessão, faça um teste Difícil (3) de Conhecimento para reconhecer características do planeta. O Mestre revela os detalhes somente após sucesso.</p><label>Conhecimento<select data-p167-skill><option value="borde_exterior">Borda Exterior</option><option value="mundos_del_nucleo">Mundos do Núcleo</option></select></label><button data-p167-activate>EXAMINAR AMBIENTE PLANETÁRIO</button><output aria-live="polite">Pronto.</output>';
  const button=panel.querySelector('[data-p167-activate]'),out=panel.querySelector('output');
  button.disabled=Boolean(status.data.usedThisSession||!status.data.sessionActive);
  if(button.disabled)out.textContent=status.data.usedThisSession?'Talento já usado nesta sessão.':'Inicie uma sessão para usar o talento.';
  button.addEventListener('click',()=>{const result=call('useFamiliarSuns',{skillKey:panel.querySelector('[data-p167-skill]').value});
   if(!result?.ok){out.textContent=String(result?.message||'Ação rejeitada.');return;}
   button.disabled=true;out.textContent=result.data.success?'Teste bem-sucedido. Mestre: revele habitação, ambiente, predadores, riscos e oportunidades presentes neste planeta.':'Teste falhou. Uso da sessão consumido; nenhuma informação é inventada.';});
  host.append(panel);
 }
 if(window.BraseiroDomScheduler)window.BraseiroDomScheduler.register('p167-familiar-suns',mount);
 else{if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();addEventListener('popstate',()=>setTimeout(mount,0));}
})();
""";p.write_text(s)
print('Familiar Suns source, controller, bridge, Android UI integrated')
