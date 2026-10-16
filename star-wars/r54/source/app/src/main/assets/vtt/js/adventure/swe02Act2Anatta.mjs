// SWE02 printed pp.34-35 (PDF pages 33-34): Anatta sells information at
// fixed prices. Payment is an actual atomic character inventory transaction.
const clone=structuredClone;
const SOURCE={productCode:'SWE02',printedPages:[34,35],authority:'OFFICIAL_PDF_TEXT_VERIFIED'};
export const ANATTA_CATALOG=Object.freeze({
 kubazSpy:{credits:40,fact:'Teemo emprega o Kubaz Thwheek como espião.'},
 jabbaRelationship:{credits:60,fact:'Teemo é parente subordinado de Jabba e tenta ampliar seu poder.'},
 sivorShell:{credits:60,fact:'A carapaça pertencia ao geonosiano Sivor.'},
 sivorIdentity:{credits:20,fact:'Sivor trabalhava para Piddock como mensageiro e técnico de droides.'},
 sivorMurder:{credits:100,fact:'Teemo promoveu a morte de Sivor em combate e Thwheek deu o golpe final.'},
 anattaJabbaContact:{credits:40,fact:'Anatta trabalha para Jabba.'},
 muNanbWeaponsDeal:{credits:100,fact:'Mu Nanb conversou com Piddock sobre uma grande venda de armas.'},
 vioSmugglers:{credits:40,fact:'Os irmãos Vio são contrabandistas rápidos e confiáveis.'},
 vrixxttBountyHunter:{credits:80,fact:'Vrixx’tt é um caçador de recompensas.'},
 maruAntiDroid:{credits:60,fact:'Maru Jakkar financia uma organização contrária a direitos de droides.'},
 dukeRivalry:{credits:80,fact:'Piddock e Dimmock são rivais, apesar da oposição comum ao Império.'}
});
export function buySWE02AnattaInformation(world,character,{topicId}={}){
 const w=clone(world),c=clone(character),p=w?.publishedAdventure;
 if(p?.id!=='long-arm-of-the-hutt'||p.status!=='ACTIVE'||p.currentNodeId!=='piddock-party'||p.activeEncounterId)
  throw Error('SWE02: Anatta negocia somente durante a recepção de Piddock, fora de combate.');
 const topic=ANATTA_CATALOG[topicId];if(!topic)throw Error('SWE02: assunto não consta do catálogo oficial de Anatta.');
 if((p.anattaTradeReceipts||[]).some(r=>r.topicId===topicId))throw Error('SWE02: esta informação já foi comprada.');
 if(!Number.isSafeInteger(c.credits)||c.credits<topic.credits)throw Error('SWE02: créditos insuficientes para o preço fixo de Anatta.');
 c.credits-=topic.credits;
 const receipt={type:'ANATTA_INFORMATION_PURCHASE',source:clone(SOURCE),topicId,creditsSpent:topic.credits,
  characterKey:String(c.id||c.name||''),creditsRemaining:c.credits,fact:topic.fact};
 if(!receipt.characterKey)throw Error('SWE02: comprador sem identidade.');
 p.anattaTradeReceipts=[...(p.anattaTradeReceipts||[]),receipt];
 if(topicId==='jabbaRelationship')p.flags={...(p.flags||{}),anattaJabbaAngleKnown:true};
 return {world:w,character:c,receipt};
}
export function validateSWE02Anatta(world){
 const p=world?.publishedAdventure;if(p?.id!=='long-arm-of-the-hutt')return true;
 const receipts=p.anattaTradeReceipts||[];
 if(!Array.isArray(receipts)||receipts.length>Object.keys(ANATTA_CATALOG).length)throw Error('SWE02: compras de Anatta adulteradas.');
 const seen=new Set();
 for(const r of receipts){const topic=ANATTA_CATALOG[r.topicId];
  if(!topic||seen.has(r.topicId)||r.type!=='ANATTA_INFORMATION_PURCHASE'||r.source?.productCode!=='SWE02'||
   JSON.stringify(r.source.printedPages)!=='[34,35]'||r.source.authority!=='OFFICIAL_PDF_TEXT_VERIFIED'||
   r.creditsSpent!==topic.credits||r.fact!==topic.fact||!r.characterKey||!Number.isSafeInteger(r.creditsRemaining)||r.creditsRemaining<0)
   throw Error('SWE02: recibo de Anatta adulterado.');seen.add(r.topicId);
 }
 if(Boolean(p.flags?.anattaJabbaAngleKnown)!==seen.has('jabbaRelationship'))throw Error('SWE02: informação de Jabba sem compra real.');
 return true;
}
