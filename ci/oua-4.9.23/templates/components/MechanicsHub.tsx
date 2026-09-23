import React, { useMemo, useState } from 'react';
import type { Character } from '../lib/db';
import type { MasterCommand, MasterState } from '../lib/master/types';
import { findSkill, targetNumberForSkill } from '../lib/characterRules';
import { rollOneRing, secureRoll } from '../lib/dice';
import { shadowAtLimit } from '../lib/rules/shadow';

interface Props {
  masterState: MasterState;
  characters: Character[];
  onApplyCommands: (commands: MasterCommand[], reason: string) => void | Promise<void>;
}
function heroName(character?:Character){return character?.sheet.identity.name||character?.name||'Herói';}
function rollSkill(character:Character,skillName:string,extraDice=0,hopeDice=0){
  const skill=findSkill(character.sheet,skillName);
  const rank=Math.max(0,Number(skill?.rank)||0);
  const mode=shadowAtLimit(character.sheet)?'ill-favoured':skill?.favorite?'favoured':'normal';
  return rollOneRing(Math.max(0,rank+extraDice+hopeDice),0,targetNumberForSkill(character.sheet,skillName),skillName,mode,Boolean(character.sheet.attributes.weary),Boolean(character.sheet.attributes.miserable));
}
function featResults(mode:'favoured'|'normal'|'ill-favoured'){
  const count=mode==='normal'?1:2;return Array.from({length:count},()=>secureRoll(1,12));
}

export const MechanicsHub:React.FC<Props>=({masterState,characters,onApplyCommands})=>{
  const [busy,setBusy]=useState(false);
  const [message,setMessage]=useState('');
  const [hope,setHope]=useState(false);
  const [inspired,setInspired]=useState(false);
  const [region,setRegion]=useState<'border'|'wild'|'dark'>('wild');
  const [danger,setDanger]=useState(1);\n  const [mounted,setMounted]=useState(false);\n  const [forcedMarch,setForcedMarch]=useState(false);\n  const [mountVigourByHero,setMountVigourByHero]=useState<Record<string,number>>({});
  const [shadowHero,setShadowHero]=useState(characters[0]?.id||'');
  const [shadowSource,setShadowSource]=useState<'dread'|'greed'|'sorcery'|'transgression'>('dread');
  const [shadowAmount,setShadowAmount]=useState(1);
  const [progressHero,setProgressHero]=useState(characters[0]?.id||'');
  const [progressSkill,setProgressSkill]=useState('Viagem');
  const [progressProf,setProgressProf]=useState('Espadas');
  const [choice,setChoice]=useState('');
  const planned=masterState.journey.plannedRouteHexIds||[];
  const guide=characters.find(c=>c.id===masterState.company.journeyRoles.guide);
  const event=masterState.journey.pendingEvent;
  const eventTarget=event?characters.find(c=>c.id===masterState.company.journeyRoles[event.targetRole]):undefined;
  const selectedShadow=characters.find(c=>c.id===shadowHero)||characters[0];
  const selectedProgress=characters.find(c=>c.id===progressHero)||characters[0];
  const run=async(commands:MasterCommand[],reason:string)=>{
    setBusy(true);setMessage('');
    try{await onApplyCommands(commands,reason);setMessage('Aplicado: '+reason);}
    catch(error){setMessage(error instanceof Error?error.message:String(error));}
    finally{setBusy(false);}
  };
  const hopeCommands=(character:Character|undefined):MasterCommand[]=>{
    if(!character||!hope)return[];
    return [{type:'spend_hope',targetCharacterId:character.id,attribute:'heart',reason:'Esperança gasta na resolução mecânica.'} as MasterCommand];
  };
  const hopeDice=()=>hope?(inspired?2:1):0;

  const startJourney=()=>run([{
    type:'start_journey',destinationHexId:planned.at(-1)||masterState.journey.destinationHexId||null,
    roles:masterState.company.journeyRoles,routeHexIds:planned,mounted,forcedMarch,
    reason:'Jornada iniciada pela rota planejada do Atlas.',
  } as MasterCommand],'iniciar Jornada');

  const march=()=>{
    if(!guide){setMessage('Defina o Guia da Jornada na Companhia.');return;}
    const roll=rollSkill(guide,'Viagem',0,hopeDice());
    void run([...hopeCommands(guide),{type:'resolve_journey_march',success:Boolean(roll.success),successIcons:Number(roll.tengwarCount)||0,roll,reason:'Jogada de Marcha do Guia.'} as MasterCommand],
      'Jogada de Marcha: '+(roll.success?'sucesso':'falha')+'; '+(roll.tengwarCount||0)+' ícone(s)');
  };
  const generateEvent=()=>{
    const mode=region==='border'?'favoured':region==='dark'?'ill-favoured':'normal';
    void run([{type:'generate_journey_event',region,targetDie:secureRoll(1,6),featResults:featResults(mode),terrain:masterState.journey.segments.at(-1)?.terrain||'',reason:'Geração canônica de Evento de Jornada.'} as MasterCommand],'gerar Evento de Jornada');
  };
  const resolveEvent=()=>{
    if(!event||!eventTarget){setMessage('O papel alvo do Evento não possui Herói atribuído.');return;}
    const mod=Number(event.successDiceModifier||0);
    const roll=rollSkill(eventTarget,event.skill,mod,hopeDice());
    void run([...hopeCommands(eventTarget),{type:'resolve_journey_event',targetCharacterId:eventTarget.id,success:Boolean(roll.success),successIcons:Number(roll.tengwarCount)||0,roll,reason:'Resolução do Evento '+event.description+'.'} as MasterCommand],
      'Evento '+event.description+': '+(roll.success?'sucesso':'falha'));
  };
  const finishJourney=()=>{
    const endRolls:Record<string,{success:boolean;successIcons:number;mountVigour:number}>={};
    for(const hero of characters){
      const roll=rollSkill(hero,'Viagem');
      endRolls[hero.id]={success:Boolean(roll.success),successIcons:Number(roll.tengwarCount)||0,mountVigour:mounted?Math.max(0,Number(mountVigourByHero[hero.id])||0):0};
    }
    void run([{type:'complete_journey',endRolls,reason:'Encerramento canônico da Jornada: montaria e jogadas de Viagem reduzem Fadiga.'} as MasterCommand],'encerrar Jornada');
  };
  const shadowTest=()=>{
    if(!selectedShadow)return;
    const skillName=shadowSource==='dread'?'Valor':'Sabedoria';
    const rating=shadowSource==='dread'?Number(selectedShadow.sheet.attributes.valor||1):Number(selectedShadow.sheet.attributes.wisdom||1);
    const favoured=shadowSource==='greed'&&/hobbit/i.test(selectedShadow.sheet.identity.culture||'');
    const mode=shadowAtLimit(selectedShadow.sheet)?'ill-favoured':favoured?'favoured':'normal';
    const roll=shadowSource==='transgression'?null:rollOneRing(rating,0,14,skillName,mode,Boolean(selectedShadow.sheet.attributes.weary),Boolean(selectedShadow.sheet.attributes.miserable));
    void run([{type:'resolve_shadow_gain',targetCharacterId:selectedShadow.id,amount:shadowAmount,source:shadowSource,testSuccess:Boolean(roll?.success),successIcons:Number(roll?.tengwarCount)||0,reason:'Ganho de Sombra: '+shadowSource} as MasterCommand],'resolver ganho de Sombra');
  };

  return <div className="space-y-4">
    <section className="rounded-lg border border-cyan-900/70 bg-zinc-950/80 p-4">
      <h2 className="text-sm font-black uppercase text-cyan-200">Jornada — sequência oficial</h2>
      <p className="mt-1 text-xs text-slate-400">Rota restante: {planned.length} hex(es) · etapa {masterState.journey.stageNumber||1} · {masterState.journey.day||0} dia(s) · marcha: {masterState.journey.marchHexesRemaining||0} hex(es).</p>
      <div className="mt-3 grid grid-cols-2 gap-2">
        {masterState.journey.status==='idle'||masterState.journey.status==='completed'
          ?<button disabled={busy||!planned.length} onClick={startJourney} className="rounded border border-cyan-700 px-3 py-2 text-cyan-100 disabled:opacity-40">Iniciar pela rota</button>
          :<button disabled={busy||Boolean(event)||Boolean(masterState.journey.awaitingEventGeneration)||Boolean(masterState.journey.awaitingEndRoll)||Number(masterState.journey.marchHexesRemaining||0)>0} onClick={march} className="rounded border border-cyan-700 px-3 py-2 text-cyan-100 disabled:opacity-40">Jogada de Marcha</button>}
        <label className="flex items-center gap-2 rounded border border-slate-800 px-2 text-xs"><input type="checkbox" checked={hope} onChange={e=>setHope(e.target.checked)}/> Esperança</label>
        <label className="flex items-center gap-2 rounded border border-slate-800 px-2 text-xs"><input type="checkbox" checked={inspired} disabled={!hope} onChange={e=>setInspired(e.target.checked)}/> Inspirado</label>
        {(masterState.journey.status==='idle'||masterState.journey.status==='completed')&&<>
          <label className="flex items-center gap-2 rounded border border-slate-800 px-2 py-2 text-xs"><input type="checkbox" checked={mounted} onChange={e=>setMounted(e.target.checked)}/> Toda a Companhia montada</label>
          <label className="flex items-center gap-2 rounded border border-slate-800 px-2 py-2 text-xs"><input type="checkbox" checked={forcedMarch} onChange={e=>setForcedMarch(e.target.checked)}/> Marcha Forçada</label>
        </>}
        {masterState.journey.awaitingEventGeneration&&<><select value={region} onChange={e=>setRegion(e.target.value as any)} className="rounded border border-slate-700 bg-black px-2 py-2 text-xs"><option value="border">Terra de Fronteira</option><option value="wild">Terra Erma</option><option value="dark">Terra Sombria</option></select><button disabled={busy} onClick={generateEvent} className="rounded border border-amber-700 px-3 py-2 text-amber-100">Gerar Evento</button></>}
        {event&&<button disabled={busy||!eventTarget} onClick={resolveEvent} className="col-span-2 rounded border border-amber-600 bg-amber-950/30 px-3 py-2 text-amber-100">Resolver {event.description} · {event.skill} · {eventTarget?heroName(eventTarget):'papel sem Herói'}</button>}
        {masterState.journey.stageComplete&&<button disabled={busy} onClick={()=>void run([{type:'continue_journey_stage',reason:'Nova etapa após 20 hexes.'} as MasterCommand],'continuar etapa da Jornada')} className="col-span-2 rounded border border-violet-700 px-3 py-2">Continuar próxima etapa</button>}
        {masterState.journey.awaitingEndRoll&&<>
          {Boolean(masterState.journey.mounted)&&<div className="col-span-2 rounded border border-emerald-900/60 p-2"><div className="mb-1 text-[10px] uppercase text-emerald-300">Vigor das montarias</div>{characters.map(hero=><label key={hero.id} className="mb-1 flex items-center justify-between gap-2 text-xs"><span>{heroName(hero)}</span><input type="number" min={0} max={12} value={mountVigourByHero[hero.id]||0} onChange={e=>setMountVigourByHero(current=>({...current,[hero.id]:Math.max(0,Number(e.target.value)||0)}))} className="w-16 rounded bg-black p-1"/></label>)}</div>}
          <button disabled={busy} onClick={finishJourney} className="col-span-2 rounded border border-emerald-700 px-3 py-2 text-emerald-200">Encerrar Jornada e aplicar Fadiga restante</button>
        </>}
      </div>
      <div className="mt-3 flex items-center gap-2"><input type="number" min={1} max={9} value={danger} onChange={e=>setDanger(Math.max(1,Number(e.target.value)||1))} className="w-16 rounded bg-black p-2"/><button disabled={busy} onClick={()=>void run([{type:'set_journey_danger',danger,reason:'Área Perigosa encontrada.'} as MasterCommand],'Área Perigosa '+danger)} className="rounded border border-red-900 px-3 py-2 text-xs text-red-200">Área Perigosa</button></div>
    </section>

    <section className="rounded-lg border border-violet-900/70 bg-zinc-950/80 p-4">
      <h2 className="text-sm font-black uppercase text-violet-200">Sombra</h2>
      <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
        <select value={selectedShadow?.id||''} onChange={e=>setShadowHero(e.target.value)} className="rounded border border-slate-700 bg-black p-2">{characters.map(c=><option key={c.id} value={c.id}>{heroName(c)}</option>)}</select>
        <select value={shadowSource} onChange={e=>setShadowSource(e.target.value as any)} className="rounded border border-slate-700 bg-black p-2"><option value="dread">Pavor · Valor</option><option value="greed">Ganância · Sabedoria</option><option value="sorcery">Feitiçaria · Sabedoria</option><option value="transgression">Transgressão · sem teste</option></select>
        <input type="number" min={1} max={9} value={shadowAmount} onChange={e=>setShadowAmount(Math.max(1,Number(e.target.value)||1))} className="rounded border border-slate-700 bg-black p-2"/>
        <button disabled={busy||!selectedShadow} onClick={shadowTest} className="rounded border border-violet-700 px-3 py-2">Resolver Sombra</button>
        <button disabled={busy||!selectedShadow} onClick={()=>selectedShadow&&void run([{type:'firm_will',targetCharacterId:selectedShadow.id,reason:'Firmar Vontade.'} as MasterCommand],'Firmar Vontade')} className="rounded border border-slate-700 px-3 py-2">Firmar Vontade</button>
        <button disabled={busy||!selectedShadow||!shadowAtLimit(selectedShadow.sheet)} onClick={()=>selectedShadow&&void run([{type:'bout_of_madness',targetCharacterId:selectedShadow.id,reason:'Ataque de Loucura.'} as MasterCommand],'Ataque de Loucura')} className="rounded border border-red-800 px-3 py-2 text-red-200 disabled:opacity-40">Ataque de Loucura</button>
      </div>
    </section>

    <section className="rounded-lg border border-emerald-900/70 bg-zinc-950/80 p-4">
      <h2 className="text-sm font-black uppercase text-emerald-200">Fase de Sociedade e progressão</h2>
      <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
        {masterState.fellowshipPhase.status!=='active'?<>
          <button disabled={busy} onClick={()=>void run([{type:'start_fellowship_phase',yule:false,reason:'Início da Fase de Sociedade.'} as MasterCommand],'iniciar Fase de Sociedade')} className="rounded border border-emerald-700 px-3 py-2">Iniciar</button>
          <button disabled={busy} onClick={()=>void run([{type:'start_fellowship_phase',yule:true,reason:'Início da Fase de Sociedade de Yule.'} as MasterCommand],'iniciar Yule')} className="rounded border border-amber-700 px-3 py-2">Yule</button>
        </>:<button disabled={busy} onClick={()=>void run([{type:'end_fellowship_phase',reason:'Fim da Fase de Sociedade.'} as MasterCommand],'encerrar Fase de Sociedade')} className="col-span-2 rounded border border-red-900 px-3 py-2">Encerrar fase</button>}
        <select value={selectedProgress?.id||''} onChange={e=>setProgressHero(e.target.value)} className="rounded border border-slate-700 bg-black p-2">{characters.map(c=><option key={c.id} value={c.id}>{heroName(c)}</option>)}</select>
        <select value={progressSkill} onChange={e=>setProgressSkill(e.target.value)} className="rounded border border-slate-700 bg-black p-2">{(selectedProgress?.sheet.skills||[]).map(s=><option key={s.name}>{s.name}</option>)}</select>
        <button disabled={busy||masterState.fellowshipPhase.status!=='active'||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'advance_skill',targetCharacterId:selectedProgress.id,skillName:progressSkill,reason:'Treinamento na Fase de Sociedade.'} as MasterCommand],'elevar Perícia')} className="rounded border border-emerald-800 px-3 py-2">Elevar Perícia</button>
        <select value={progressProf} onChange={e=>setProgressProf(e.target.value)} className="rounded border border-slate-700 bg-black p-2">{['Machados','Arcos','Lanças','Espadas'].map(x=><option key={x}>{x}</option>)}</select>
        <button disabled={busy||masterState.fellowshipPhase.status!=='active'||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'advance_combat_proficiency',targetCharacterId:selectedProgress.id,group:progressProf,reason:'Treinamento de Proficiência.'} as MasterCommand],'elevar Proficiência')} className="rounded border border-emerald-800 px-3 py-2">Elevar Proficiência</button>
        <input value={choice} onChange={e=>setChoice(e.target.value)} placeholder="Recompensa ou Virtude escolhida" className="col-span-2 rounded border border-slate-700 bg-black p-2"/>
        <button disabled={busy||!choice.trim()||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'advance_valor',targetCharacterId:selectedProgress.id,reward:choice,reason:'Elevação de Valor.'} as MasterCommand],'elevar Valor')} className="rounded border border-amber-800 px-3 py-2">Valor + Recompensa</button>
        <button disabled={busy||!choice.trim()||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'advance_wisdom',targetCharacterId:selectedProgress.id,virtue:choice,reason:'Elevação de Sabedoria.'} as MasterCommand],'elevar Sabedoria')} className="rounded border border-cyan-800 px-3 py-2">Sabedoria + Virtude</button>
        <button disabled={busy||!selectedProgress} onClick={()=>selectedProgress&&void run([{type:'heal_shadow_scar',targetCharacterId:selectedProgress.id,reason:'Empreendimento Curar Cicatrizes.'} as MasterCommand],'Curar Cicatriz de Sombra')} className="col-span-2 rounded border border-violet-800 px-3 py-2">Curar Cicatriz · 5 PA · Yule</button>
        <button disabled={busy} onClick={()=>void run([{type:'record_undertaking',targetCharacterId:selectedProgress?.id||'',undertaking:'strengthen-fellowship',reason:'Empreendimento Fortalecer Sociedade.'} as MasterCommand],'Fortalecer Sociedade')} className="col-span-2 rounded border border-cyan-800 px-3 py-2">Empreendimento: Fortalecer Sociedade</button>
        <button disabled={busy} onClick={()=>void run([{type:'resolve_rest',kind:'long',safeRefuge:true,reason:'Descanso Prolongado em refúgio protegido e seguro.'} as MasterCommand],'Descanso em refúgio seguro')} className="col-span-2 rounded border border-blue-800 px-3 py-2">Descanso Prolongado em refúgio seguro · −1 Fadiga</button>
      </div>
    </section>
    {message&&<div className="rounded border border-slate-700 bg-black/60 p-3 text-xs text-slate-200">{message}</div>}
  </div>;
};
