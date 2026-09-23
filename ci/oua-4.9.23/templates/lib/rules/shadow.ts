import type { CharacterSheet } from '../db';

function norm(value: unknown): string { return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim(); }
function clamp(value:number,min:number,max:number){ return Math.max(min,Math.min(max,Number(value)||0)); }

const FLAWS: Record<string,string[]> = {
  'maldicao da vinganca':['Rancoroso','Brutal','Cruel','Assassino'],
  'doenca do dragao':['Avarento','Desconfiado','Desonesto','Ladrão'],
  'seducao do poder':['Ressentido','Arrogante','Presunçoso','Tirânico'],
  'tentacao do poder':['Ressentido','Arrogante','Presunçoso','Tirânico'],
  'seducao dos segredos':['Soberbo','Desdenhoso','Manipulador','Traidor'],
  'tentacao dos segredos':['Soberbo','Desdenhoso','Manipulador','Traidor'],
  'caminho do desespero':['Perturbado','Hesitante','Culpado','Receoso'],
  'loucura errante':['Ocioso','Esquecido','Indiferente','Covarde'],
  'loucura da vagancia':['Ocioso','Esquecido','Indiferente','Covarde'],
};

export function recomputeShadowState(input: CharacterSheet): CharacterSheet {
  const next=structuredClone(input);
  const max=Math.max(0,Number(next.attributes.hopeMax)||0);
  const scars=Math.max(0,Number((next.attributes as any).shadowScars)||0);
  next.attributes.shadow=clamp(next.attributes.shadow,scars,max);
  next.attributes.miserable=next.attributes.shadow>=next.attributes.hopeCurrent;
  (next.attributes as any).shadowAtLimit=next.attributes.shadow>=max&&max>0;
  return next;
}

export function shadowAtLimit(sheet: CharacterSheet): boolean {
  return Math.max(0,Number(sheet.attributes.hopeMax)||0)>0
    && Number(sheet.attributes.shadow)>=Number(sheet.attributes.hopeMax);
}

export function adjustShadowCanonical(input: CharacterSheet, amount: number): CharacterSheet {
  const next=structuredClone(input);
  const scars=Math.max(0,Number((next.attributes as any).shadowScars)||0);
  next.attributes.shadow=clamp(Number(next.attributes.shadow)+amount,scars,Math.max(scars,Number(next.attributes.hopeMax)||0));
  return recomputeShadowState(next);
}

export function resolveShadowGain(
  input: CharacterSheet,
  incoming: number,
  source: 'dread'|'greed'|'sorcery'|'transgression',
  roll?: {success:boolean;successIcons:number},
): {sheet:CharacterSheet;gained:number;prevented:number} {
  const raw=Math.max(0,Math.trunc(Number(incoming)||0));
  const resistible=source!=='transgression';
  const prevented=resistible&&roll?.success ? Math.min(raw,1+Math.max(0,Math.trunc(Number(roll.successIcons)||0))) : 0;
  const gained=Math.max(0,raw-prevented);
  return {sheet:adjustShadowCanonical(input,gained),gained,prevented};
}

export function firmWill(input: CharacterSheet): {sheet:CharacterSheet;allowed:boolean} {
  if(shadowAtLimit(input)||Number(input.attributes.shadow)<=0) return {sheet:input,allowed:false};
  const next=structuredClone(input);
  const scars=Math.max(0,Number((next.attributes as any).shadowScars)||0)+1;
  (next.attributes as any).shadowScars=scars;
  next.attributes.shadow=scars;
  return {sheet:recomputeShadowState(next),allowed:true};
}

export function healShadowScar(input: CharacterSheet): {sheet:CharacterSheet;allowed:boolean} {
  const current=Math.max(0,Number((input.attributes as any).shadowScars)||0);
  if(current<=0) return {sheet:input,allowed:false};
  const next=structuredClone(input);
  (next.attributes as any).shadowScars=current-1;
  next.attributes.shadow=Math.max(current-1,Number(next.attributes.shadow)-1);
  return {sheet:recomputeShadowState(next),allowed:true};
}

export function resolveBoutOfMadness(input: CharacterSheet): {sheet:CharacterSheet;flaw:string|null;retired:boolean;allowed:boolean} {
  if(!shadowAtLimit(input)) return {sheet:input,flaw:null,retired:false,allowed:false};
  const next=structuredClone(input);
  const currentStep=Math.max(0,Math.trunc(Number((next.other as any).shadowPathStep)||0));
  if(currentStep>=4){
    (next.other as any).retiredByShadow=true;
    return {sheet:next,flaw:null,retired:true,allowed:true};
  }
  const path=norm(next.other.shadowPath);
  const sequence=FLAWS[path]||[];
  const flaw=sequence[currentStep]||('Defeito do Caminho da Sombra '+String(currentStep+1));
  const existing=String(next.other.flaws||'').split(/[;\n,]+/).map(x=>x.trim()).filter(Boolean);
  if(!existing.some(x=>norm(x)===norm(flaw))) existing.push(flaw);
  next.other.flaws=existing.join('; ');
  (next.other as any).shadowPathStep=currentStep+1;
  const scars=Math.max(0,Number((next.attributes as any).shadowScars)||0);
  next.attributes.shadow=scars;
  return {sheet:recomputeShadowState(next),flaw,retired:false,allowed:true};
}
