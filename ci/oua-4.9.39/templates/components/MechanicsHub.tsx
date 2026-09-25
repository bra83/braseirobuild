import React, { useState } from 'react';
import type { Character } from '../lib/db';
import type { MasterCommand, MasterState } from '../lib/master/types';
import { SupplementRulesPanel } from './SupplementRulesPanel';
import { shadowAtLimit } from '../lib/rules/shadow';
import { findSkill, targetNumberForSkill } from '../lib/characterRules';
import { rollOneRing } from '../lib/dice';
import { supplementBonusDice, supplementRollMode } from '../lib/rules/supplementMechanics';

interface Props {
  masterState: MasterState;
  characters: Character[];
  onApplyCommands: (commands: MasterCommand[], reason: string) => void | Promise<void>;
}
function heroName(character?:Character){return character?.sheet.identity.name||character?.name||'Herói';}

export const MechanicsHub:React.FC<Props>=({masterState,characters,onApplyCommands})=>{
  const [busy,setBusy]=useState(false);
  const [message,setMessage]=useState('');
  const [shadowHero,setShadowHero]=useState(characters[0]?.id||'');
  const [shadowSource,setShadowSource]=useState<'dread'|'greed'|'sorcery'|'transgression'>('dread');
  const [shadowAmount,setShadowAmount]=useState(1);
  const [progressHero,setProgressHero]=useState(characters[0]?.id||'');
  const [progressSkill,setProgressSkill]=useState('Viagem');
  const [progressProf,setProgressProf]=useState('Espadas');
  const [choice,setChoice]=useState('');
  const [danger,setDanger]=useState(1);
  const selectedShadow=characters.find(c=>c.id===shadowHero)||characters[0];
  const selectedProgress=characters.find(c=>c.id===progressHero)||characters[0];

  const run=async(commands:MasterCommand[],reason:string)=>{
    setBusy(true);setMessage('');
    try{await onApplyCommands(commands,reason);setMessage('Aplicado: '+reason);}
    catch(error){setMessage(error instanceof Error?error.message:String(error));}
    finally{setBusy(false);}
  };

  const shadowTest=()=>{
    if(!selectedShadow)return;
    const skillName=shadowSource==='dread'?'Valor':'Sabedoria';
    const rating=shadowSource==='dread'?Number(selectedShadow.sheet.attributes.valor||1):Number(selectedShadow.sheet.attributes.wisdom||1);
    const baseMode=shadowAtLimit(selectedShadow.sheet)?'ill-favoured':shadowSource==='greed'&&/hobbit/i.test(selectedShadow.sheet.identity.culture||'')?'favoured':'normal';
    const dreadSource=shadowSource==='dread'?'ordinary':undefined;
    const mode=supplementRollMode(selectedShadow.sheet,{kind:'shadow',skill:skillName,dreadSource},baseMode);
    const extra=supplementBonusDice(selectedShadow.sheet,{kind:'shadow',skill:skillName,dreadSource});
    const roll=shadowSource==='transgression'?null:rollOneRing(Math.max(0,rating+extra),0,targetNumberForSkill(selectedShadow.sheet,skillName),skillName,mode,Boolean(selectedShadow.sheet.attributes.weary),Boolean(selectedShadow.sheet.attributes.miserable));
    void run([{type:'resolve_shadow_gain',targetCharacterId:selectedShadow.id,amount:shadowAmount,source:shadowSource,testSuccess:Boolean(roll?.success),successIcons:Number(roll?.tengwarCount)||0,reason:'Ganho de Sombra: '+shadowSource} as MasterCommand],'resolver ganho de Sombra');
  };

  return <div className="space-y-4">
    <section className="rounded-lg border border-cyan-900/70 bg-zinc-950/80 p-4">
      <h2 className="text-sm font-black uppercase text-cyan-200">Jornada — controle explícito</h2>
      <p className="mt-1 text-xs text-slate-400">Papéis, Jogada de Marcha, Eventos, Fadiga e encerramento são resolvidos na tela Sessão. Este painel não executa mais etapas escondidas.</p>
      <div className="mt-3 flex items-center gap-2 text-xs">
        <input type="number" min={1} max={9} value={danger} onChange={e=>setDanger(Math.max(1,Number(e.target.value)||1))} className="w-16 rounded bg-black p-2"/>
        <button disabled={busy} onClick={()=>void run([{type:'set_journey_danger',danger,reason:'Área Perigosa encontrada.'} as MasterCommand],'Área Perigosa '+danger)} className="rounded border border-red-900 px-3 py-2 text-red-200">Definir Área Perigosa</button>
      </div>
    </section>

    <SupplementRulesPanel masterState={masterState} characters={characters} onApplyCommands={onApplyCommands}/>

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
