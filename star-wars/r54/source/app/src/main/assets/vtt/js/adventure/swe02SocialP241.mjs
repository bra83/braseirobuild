// SWE02 printed pp.13-14,22-23. Player decisions are resolved by live
// narrative dice and atomic controller transactions, never a narrator flag.
import {buildPositivePool,rollPool,resolveSymbols,diceFacesForTests} from '../rules/narrativeDice.mjs';
import {SKILLS} from '../rules/character.mjs';
import {healCriticalInjury} from '../rules/critical.mjs';
import {equipmentCatalog} from '../rules/equipmentCatalog.mjs';
const clone=structuredClone;
const source=page=>({productCode:'SWE02',printedPage:page,authority:'OFFICIAL_PDF_TEXT_VERIFIED'});
const id=c=>String(c?.id||c?.name||'').trim();
function active(world,node){const p=world?.publishedAdventure;if(p?.id!=='long-arm-of-the-hutt'||p.status!=='ACTIVE'||p.currentNodeId!==node)throw Error('SWE02: cena social incorreta.');return p;}
function receipt(p,type,page,data){p.socialReceipts=Array.isArray(p.socialReceipts)?p.socialReceipts:[];const r={type,nodeId:p.currentNodeId,source:source(page),...data};p.socialReceipts.push(r);return r;}
export function skillRoll(character,skillKey,difficulty,rng){if(!character?.characteristics||!character.skills||!(skillKey in SKILLS))throw Error('SWE02: ficha inválida para teste social.');const pool={...buildPositivePool(character.characteristics[SKILLS[skillKey]],character.skills[skillKey]||0),difficulty,challenge:0,boost:0,setback:0};return rollPool(pool,rng);}
export function validateCheck(check,expected){if(!check||!Array.isArray(check.faces)||!check.pool)throw Error('SWE02: recibo sem faces de dados.');for(const [type,count] of Object.entries(expected)){if(check.pool[type]!==count)throw Error('SWE02: pool de dados adulterado.');}const raw={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0},counts={ability:0,proficiency:0,difficulty:0,challenge:0,boost:0,setback:0};for(const f of check.faces){if(!(f.type in counts)||!Number.isInteger(f.index)||f.index<0||f.index>=diceFacesForTests[f.type].length||JSON.stringify(f.face)!==JSON.stringify(diceFacesForTests[f.type][f.index]))throw Error('SWE02: face de dado adulterada.');counts[f.type]++;for(const [symbol,n] of Object.entries(f.face))raw[symbol]+=n;}for(const [type,count] of Object.entries(counts))if(count!==check.pool[type])throw Error('SWE02: quantidade de dados adulterada.');const resolved=resolveSymbols(raw);for(const k of ['success','netSuccess','netAdvantage','triumph','despair'])if(resolved[k]!==check[k])throw Error('SWE02: símbolos de dados adulterados.');}
export function nynCriticalCare(world,character,{injuryId,rng=Math.random}={}){
 const w=clone(world),c=clone(character),p=active(w,'nabat-contact'),who=id(c);
 if(!who||!String(injuryId||'').trim())throw Error('SWE02: paciente e ferimento devem ser identificados.');
 if(!(p.nabatReceipts||[]).some(r=>r.type==='SAFEHOUSE_CARE'&&r.characterKey===who))throw Error('SWE02: atendimento normal deve preceder o tratamento crítico.');
 const injury=c.criticalInjuries?.find(x=>x.id===injuryId);
 if(!injury)throw Error('SWE02: Ferimento Crítico não encontrado.');
 if(injury.code==='dead'||!Number.isInteger(injury.healDifficulty))throw Error('SWE02: lesão não tratável por este médico.');
 if((p.socialReceipts||[]).some(r=>r.type==='NYN_CRITICAL_ATTEMPT'&&r.characterKey===who&&r.injuryId===injuryId))throw Error('SWE02: apenas uma tentativa por Ferimento Crítico.');
 // Official scenario gives the Twi'lek doctor's fixed pool: 1 proficiency, 2 difficulty.
 const check=rollPool({ability:0,proficiency:1,difficulty:2,challenge:0,boost:0,setback:0},rng);
 const healed=Boolean(check.success);const result=healed?healCriticalInjury(c,injuryId).character:c;
 const r=receipt(p,'NYN_CRITICAL_ATTEMPT',13,{characterKey:who,injuryId,injuryCode:injury.code,check,healed});
 return {world:w,character:result,receipt:r};
}
export function nynNegotiate(world,character,{method='charm',rng=Math.random}={}){
 const w=clone(world),c=clone(character),p=active(w,'nabat-contact'),who=id(c);
 if(!who)throw Error('SWE02: personagem sem identidade.');
 if(!['charm','coerce'].includes(method))throw Error('SWE02: negociação exige Charme ou Coerção.');
 if((p.socialReceipts||[]).some(r=>r.type==='NYN_NEGOTIATION'))throw Error('SWE02: negociação com Nyn já realizada.');
 const skillKey=method==='charm'?'carisma':'coaccion',difficulty=method==='charm'?2:3;
 const check=skillRoll(c,skillKey,difficulty,rng),success=check.success;
 // Official symbols: one Advantage = grenades, two Advantages = padded armor,
 // Triumph = blaster rifle; two Threat = defective gear. No fabricated combat stats.
 const grants=success?{grenadeClaim:check.netAdvantage>=1,armor:check.netAdvantage>=2,rifleClaim:check.triumph>=1,defective:check.netAdvantage<=-2}:{grenadeClaim:false,armor:false,rifleClaim:false,defective:false};
 let armorAssetId=null;
 if(grants.armor){const armor=equipmentCatalog().armor.find(x=>x.id==='padded_armor'&&x.source?.activeRulesAuthority===true);if(!armor)throw Error('SWE02: armadura sem ficha primária EotE.');armorAssetId=`swe02-nyn-armor:${(p.socialReceipts||[]).length+1}`;c.armor=[...(c.armor||[]),{...clone(armor),ownedAssetId:armorAssetId,sourceAdventure:source(14),defectiveUntilFirstUse:grants.defective}];}
 // Grenades/rifle are granted as claims, NOT usable weapons, until their
 // EotE primary combat statistics are certified (catalog currently AoR-only).
 const r=receipt(p,'NYN_NEGOTIATION',14,{characterKey:who,method,skillKey,difficulty,check,grants,armorAssetId,
  pendingWeaponProfileCertification:grants.grenadeClaim||grants.rifleClaim});
 return {world:w,character:c,receipt:r};
}
export function bribeNewMeenThug(world,character,{credits=500,rng=Math.random}={}){
 const w=clone(world),c=clone(character),p=active(w,'drombb-cantina'),who=id(c);
 if(p.activeEncounterId||(p.encounterHistory||[]).some(r=>r.encounterId==='laoth-drombb-cantina'&&r.outcome))throw Error('SWE02: negociação deve ocorrer antes do combate e de seu desfecho.');
 if(!Number.isSafeInteger(credits)||credits<400||credits>500)throw Error('SWE02: suborno oficial exige 400–500 créditos por capanga.');
 const partySize=Number(p.flags?.confirmedPartySize);if(!Number.isInteger(partySize)||partySize<2||partySize>8)throw Error('SWE02: tamanho de grupo não confirmado.');
 const total=(p.flags?.newMeenNightRaidStopped?2:3)*Math.min(4,partySize);
 const previous=(p.socialReceipts||[]).filter(r=>r.type==='NEW_MEEN_THUG_BRIBE'&&r.success).length;
 if(previous>=total)throw Error('SWE02: não restam capangas a subornar; Drombb não aceita suborno.');
 if(!Number.isSafeInteger(c.credits)||c.credits<credits)throw Error('SWE02: créditos insuficientes para suborno.');
 const check=skillRoll(c,'negociacion',1,rng),success=check.success;
 if(success)c.credits-=credits;
 const r=receipt(p,'NEW_MEEN_THUG_BRIBE',23,{characterKey:who,creditsOffered:credits,creditsSpent:success?credits:0,creditsRemaining:c.credits,skillKey:'negociacion',difficulty:1,check,success});
 return {world:w,character:c,receipt:r,remainingThugs:total-previous-(success?1:0)};
}
export function remainingDrombbThugs(p){return (p?.socialReceipts||[]).filter(r=>r.type==='NEW_MEEN_THUG_BRIBE'&&r.success).length;}
export function validateSWE02Social(p){if(p?.id!=='long-arm-of-the-hutt')return true;
 const rr=p.socialReceipts||[];if(!Array.isArray(rr))throw Error('SWE02: recibos sociais inválidos.');
 if(rr.filter(r=>r.type==='NYN_NEGOTIATION').length>1)throw Error('SWE02: Nyn negociada duas vezes.');
 const crit=new Set();let bribed=0;
 for(const r of rr){if(r.source?.productCode!=='SWE02'||![13,14,23].includes(r.source.printedPage))throw Error('SWE02: recibo social sem fonte oficial.');
  if(r.type==='NYN_CRITICAL_ATTEMPT'){
   if(r.nodeId!=='nabat-contact'||r.source.printedPage!==13||!r.characterKey||!r.injuryId||crit.has(`${r.characterKey}:${r.injuryId}`))throw Error('SWE02: tentativa médica inválida/duplicada.');crit.add(`${r.characterKey}:${r.injuryId}`);
   validateCheck(r.check,{ability:0,proficiency:1,difficulty:2,challenge:0,boost:0,setback:0});if(r.healed!==r.check.success)throw Error('SWE02: cura crítica adulterada.');
  }else if(r.type==='NYN_NEGOTIATION'){
   if(r.nodeId!=='nabat-contact'||r.source.printedPage!==14||!['charm','coerce'].includes(r.method)||r.skillKey!==(r.method==='charm'?'carisma':'coaccion')||r.difficulty!==(r.method==='charm'?2:3))throw Error('SWE02: negociação de Nyn inválida.');
   validateCheck(r.check,{difficulty:r.difficulty,challenge:0,boost:0,setback:0});
   const g=r.check.success?{grenadeClaim:r.check.netAdvantage>=1,armor:r.check.netAdvantage>=2,rifleClaim:r.check.triumph>=1,defective:r.check.netAdvantage<=-2}:{grenadeClaim:false,armor:false,rifleClaim:false,defective:false};
   if(JSON.stringify(g)!==JSON.stringify(r.grants)||Boolean(r.armorAssetId)!==g.armor)throw Error('SWE02: recompensa de Nyn adulterada.');
  }else if(r.type==='NEW_MEEN_THUG_BRIBE'){
   if(r.nodeId!=='drombb-cantina'||r.source.printedPage!==23||!Number.isInteger(r.creditsOffered)||r.creditsOffered<400||r.creditsOffered>500||r.skillKey!=='negociacion'||r.difficulty!==1||r.success!==r.check?.success||r.creditsSpent!==(r.success?r.creditsOffered:0)||!Number.isSafeInteger(r.creditsRemaining)||r.creditsRemaining<0)throw Error('SWE02: suborno adulterado.');
   validateCheck(r.check,{difficulty:1,challenge:0,boost:0,setback:0});if(r.success)bribed++;
  }else throw Error('SWE02: tipo de recibo social desconhecido.');
 }
 const size=Number(p.flags?.confirmedPartySize);if(bribed&&( !Number.isInteger(size)||size<2||size>8||bribed>(p.flags?.newMeenNightRaidStopped?2:3)*Math.min(size,4)))throw Error('SWE02: mais capangas pagos que os existentes.');
 return true;
}
