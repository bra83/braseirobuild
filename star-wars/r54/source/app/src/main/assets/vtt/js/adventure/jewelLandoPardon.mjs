/** SWE09 The Jewel of Yavin, printed p.90, "Extended Campaign: Going Respectable".
 * Lando can pardon the party after hearing a credible plan to restore Cloud City's
 * reputation; each *actual player character* incurs 5 Obligation: Favor to Lando.
 * The GM, not the engine, confirms the audience, accepted plan and actual pardon.
 * No automatic gem transfer, XP, Arend capture or clearing of unrelated bounties.
 */
const clone=structuredClone;
const SOURCE='SWE09 The Jewel of Yavin, p.90, Extended Campaign: Going Respectable';
const validId=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const MEMBER_LIMIT=8;
const keys=['id','kind','version','source','members','obligationPerMember','gmLandoAudienceConfirmed','gmRestorationPlanAccepted','gmPardonGranted','gmPartyRosterConfirmed','gmMembersArePlayerCharactersConfirmed'];
export const LANDO_FAVOR_PER_CHARACTER=5;
export function replayJewelLandoPardon(progress){
 const receipts=Object.entries(progress?.operations?.receipts??{}).filter(([,r])=>r?.kind==='JEWEL_LANDO_PARDON');
 if(receipts.length>1)throw Error('Perdão de Lando registrado mais de uma vez.');
 if(!receipts.length)return null;
 const [id,r]=receipts[0];
 if(progress?.id!=='jewel-of-yavin'||!validId(id)||r.id!==id||r.version!==1||r.source!==SOURCE||
   Object.keys(r).some(k=>!keys.includes(k))||r.obligationPerMember!==LANDO_FAVOR_PER_CHARACTER||
   !Array.isArray(r.members)||r.members.length<1||r.members.length>MEMBER_LIMIT||
   r.members.some(m=>typeof m!=='string'||!m.trim()||m!==m.trim()||m.length>120)||
   new Set(r.members.map(x=>x.toLocaleLowerCase('pt-BR'))).size!==r.members.length||
   r.gmLandoAudienceConfirmed!==true||r.gmRestorationPlanAccepted!==true||
   r.gmPardonGranted!==true||r.gmPartyRosterConfirmed!==true||
   r.gmMembersArePlayerCharactersConfirmed!==true)
   throw Error('Recibo de perdão de Lando adulterado ou sem gatilhos oficiais.');
 return clone(r);
}
export function validateJewelLandoPardon(progress){
 const expected=replayJewelLandoPardon(progress),stored=progress?.operations?.landoPardon??null;
 if(!same(expected,stored))throw Error('Estado do perdão de Lando diverge do recibo.');
 return expected;
}
export function grantJewelLandoPardon(world,group,combat,character,details={}){
 const w=clone(world),g=clone(group),p=w?.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||!['ACTIVE','COMPLETED'].includes(p.status))
   throw Error('O perdão de Lando exige The Jewel of Yavin iniciada.');
 if(combat?.active||p.activeEncounterId)throw Error('Encerre o combate antes da audiência com Lando.');
 if(validateJewelLandoPardon(p))throw Error('Lando já concedeu o perdão; não repetir a Obrigação.');
 if(!validId(details.actionId)||p.operations?.receipts?.[details.actionId])throw Error('ID do perdão inválido ou duplicado.');
 for(const k of ['gmLandoAudienceConfirmed','gmRestorationPlanAccepted','gmPardonGranted','gmPartyRosterConfirmed','gmMembersArePlayerCharactersConfirmed'])
   if(details[k]!==true)throw Error('A audiência, o plano, o perdão e a lista real de PJs exigem confirmação do Mestre.');
 const members=details.members;
 if(!Array.isArray(members)||members.length<1||members.length>MEMBER_LIMIT||
   members.some(m=>typeof m!=='string'||!m.trim()||m!==m.trim()||m.length>120)||
   new Set(members.map(x=>x.toLocaleLowerCase('pt-BR'))).size!==members.length)
   throw Error('Lista de personagens jogadores inválida ou duplicada.');
 const current=String(character?.name??'').trim();
 if(!current||!members.some(m=>m.toLocaleLowerCase('pt-BR')===current.toLocaleLowerCase('pt-BR')))
   throw Error('O personagem jogador da ficha ativa deve constar na lista confirmada.');
 if(!Array.isArray(g.obligationEntries))throw Error('Livro de Obrigação do grupo inválido.');
 // Group entries supersede character.obligations in the EotE total. Preserve
 // legacy personal obligations on the first group entry rather than hiding them.
 if(!g.obligationEntries.length&&Array.isArray(character.obligations)){
   g.obligationEntries=character.obligations.map((e,i)=>({
     id:`jewel_lando_legacy_${details.actionId}_${i}`,member:String(e.member??current),
     type:String(e.type??'Obrigação'),description:String(e.description??''),
     value:Number(e.value??e.startingValue??0)}));
   if(g.obligationEntries.some(e=>!Number.isSafeInteger(e.value)||e.value<0||e.value>1000))
     throw Error('Obrigação pessoal legada inválida.');
 }
 for(const [i,member] of members.entries()){
   const id=`jewel_lando_${details.actionId}_${i}`;
   if(g.obligationEntries.some(e=>e.id===id))throw Error('Obrigação de Lando já existente.');
   g.obligationEntries.push({id,member,type:'Favor',value:LANDO_FAVOR_PER_CHARACTER,
     description:'Lando Calrissian — perdão condicionado à reparação da reputação de Cloud City (SWE09 p.90).'});
 }
 const receipt={id:details.actionId,kind:'JEWEL_LANDO_PARDON',version:1,source:SOURCE,
   members:clone(members),obligationPerMember:LANDO_FAVOR_PER_CHARACTER,
   gmLandoAudienceConfirmed:true,gmRestorationPlanAccepted:true,gmPardonGranted:true,
   gmPartyRosterConfirmed:true,gmMembersArePlayerCharactersConfirmed:true};
 p.operations??={};p.operations.receipts??={};
 p.operations.receipts[receipt.id]=clone(receipt);
 p.operations.landoPardon=replayJewelLandoPardon(p);
 validateJewelLandoPardon(p);
 return {world:w,group:g,adjudication:receipt};
}
