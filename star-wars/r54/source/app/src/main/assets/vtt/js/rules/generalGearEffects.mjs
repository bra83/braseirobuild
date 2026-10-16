const aorReference=(source)=>Object.freeze({...source,authority:'CROSS_LINE_REFERENCE',rulesProfile:'AOR_REFERENCE_ONLY',activeRulesAuthority:false});

const SRC_PAGE35=aorReference({book:'La Era de la Rebelión - Libro de reglas',pdfPage:36,printedPage:35,section:'Electrobinoculares / Grilletes / Macrobinoculares'});
const SRC_PAGE36=aorReference({book:'La Era de la Rebelión - Libro de reglas',pdfPage:37,printedPage:36,section:'Mando de retención'});

function int(value,label,{min=0,max=12}={}){
  const n=Number(value);
  if(!Number.isInteger(n)||n<min||n>max) throw new Error(`${label} inválido.`);
  return n;
}

export function opticalSetbackRelief(itemId,{setback=0,longRangeSetback=0,poorLightingSetback=0}={}){
  const total=int(setback,'Dados de Complicação');
  const longRange=int(longRangeSetback,'Complicação por longo alcance');
  const poorLighting=int(poorLightingSetback,'Complicação por iluminação ruim');
  if(longRange+poorLighting>total) throw new Error('As causas de Complicação não podem exceder o total informado.');

  let eligible=0;
  let source=SRC_PAGE35;
  if(itemId==='electrobinoculars') eligible=longRange+poorLighting;
  else if(itemId==='macrobinoculars') eligible=longRange;
  else throw new Error('Equipamento óptico sem efeito auditado.');

  const removed=Math.min(1,eligible,total);
  return {itemId,totalSetback:total,removedSetback:removed,remainingSetback:total-removed,source:structuredClone(source)};
}


export function mechanicsToolSetbackRelief(itemId,{setback=0,missingToolsSetback=0}={}){
  if(itemId!=='tool_kit') throw new Error('Equipamento sem efeito de ferramentas auditado.');
  const total=int(setback,'Dados de Complicação');
  const missingTools=int(missingToolsSetback,'Complicação por falta de ferramentas');
  if(missingTools>total) throw new Error('A Complicação por falta de ferramentas não pode exceder o total informado.');
  return {itemId,totalSetback:total,removedSetback:missingTools,remainingSetback:total-missingTools,requiredForDroidRepair:true,source:structuredClone(SRC_PAGE35)};
}

export function emergencyRepairPatchProfile(){
  return {itemId:'emergency_repair_patch',target:'droid',cost:'action',sameAs:'stimpack',automated:false,source:structuredClone(SRC_PAGE36)};
}

export function ammoReloadProfile(){
  return {itemId:'power_pack_or_ammo_reload',trigger:'despair_on_ranged_attack_ammo_depleted',cost:'maneuver',effect:'reload_and_continue',automated:false,source:structuredClone(SRC_PAGE36)};
}

export function restraintEscapeProfile(itemId){
  if(itemId==='binders') return {
    itemId,target:'living',difficulty:3,skills:['atletismo','coordinacion'],retry:'normal',source:structuredClone(SRC_PAGE35)
  };
  if(itemId==='restraining_device') return {
    itemId,target:'droid',difficulty:3,skills:['disciplina'],retry:'gm_consent_after_failure',source:structuredClone(SRC_PAGE36)
  };
  throw new Error('Dispositivo de restrição sem perfil auditado.');
}
