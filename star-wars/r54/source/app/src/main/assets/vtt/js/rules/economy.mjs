import { availabilityProfile } from './availability.mjs';
import { wheelAndDealLegalSale, masterMerchantOption } from './talents.mjs';
import { weaponCatalogEntry, armorCatalogEntry, generalGearCatalogEntry, equipmentCatalog } from './equipmentCatalog.mjs';
import { catalogWeaponToCharacterWeapon } from './acquisition.mjs';
import { STARTER_VEHICLE_PROFILES, createVehicleFromProfile } from './vehicles.mjs';
import { CYBERNETICS_CATALOG, removeCybernetic } from './cybernetics.mjs';
import { ATTACHMENT_CATALOG, attachmentCatalogEntry } from './attachments.mjs';

export const ECONOMY_POLICY_SOURCE=Object.freeze({kind:'local_policy',label:'Política local de economia do sandbox',note:'Aquisição, recompensa, saque, propriedade, perda, dano, reparo e venda só operam sobre perfis mecânicos já catalogados; o Mestre não cria ficha/preço/raridade livremente.'});
const KINDS=new Set(['weapon','armor','gear','vehicle','cybernetic','attachment']);
const clone=v=>structuredClone(v);
function text(v,max=500){return String(v??'').trim().slice(0,max);}
function int(v,label,{min=0,max=100000000}={}){const n=Number(v);if(!Number.isInteger(n)||n<min||n>max)throw new Error(`${label} inválido.`);return n;}
function kindOf(v){const k=String(v??'');if(!KINDS.has(k))throw new Error('Tipo de ativo inválido.');return k;}
function assetId(kind,itemId,seed=''){const s=String(seed||Date.now()).replace(/[^a-zA-Z0-9_-]+/g,'').slice(-24)||'x';return `asset_${kind}_${itemId}_${s}`;}
function vehicleEntry(id){const p=STARTER_VEHICLE_PROFILES[id];if(!p)return null;return {id:p.key,name:p.name,category:'vehicle',price:p.price??null,rarity:p.rarity??null,restricted:Boolean(p.restricted),profile:clone(p),source:clone(p.source??null)};}
function attachmentEntry(id){const p=attachmentCatalogEntry(id);if(!p)return null;return {id:p.id,name:p.name,category:'attachment',price:p.price,rarity:p.rarity,restricted:Boolean(p.restricted),profile:clone(p),source:clone(p.source)};}
function cyberneticEntry(id){const p=CYBERNETICS_CATALOG?.[id];if(!p)return null;return {id:p.id,name:p.name,category:'cybernetic',price:p.cost??null,rarity:p.rarity??null,restricted:Boolean(p.restricted),profile:clone(p),source:{kind:'official_rule',label:'Implantes cibernéticos EotE/AoR',note:'Perfil mecânico local auditado em cybernetics.mjs'}};}
export function catalogAsset(kind,itemId){
  const k=kindOf(kind), id=String(itemId??'');
  if(k==='weapon') return weaponCatalogEntry(id);
  if(k==='armor') return armorCatalogEntry(id);
  if(k==='gear') return generalGearCatalogEntry(id);
  if(k==='vehicle') return vehicleEntry(id);
  if(k==='cybernetic') return cyberneticEntry(id);
  return attachmentEntry(id);
}
function mechanicallyAvailable(item){return Boolean(item)&&Number.isInteger(item.price)&&item.price>=0&&Number.isInteger(item.rarity)&&item.rarity>=0;}
export function genericEconomyCatalog(){
  const base=equipmentCatalog();
  const vehicles=Object.values(STARTER_VEHICLE_PROFILES).map(p=>vehicleEntry(p.key)).filter(mechanicallyAvailable);
  const cybernetics=Object.values(CYBERNETICS_CATALOG).map(p=>cyberneticEntry(p.id)).filter(mechanicallyAvailable);
  const attachments=Object.values(ATTACHMENT_CATALOG).map(p=>attachmentEntry(p.id)).filter(mechanicallyAvailable);
  return {weapons:base.weapons.filter(mechanicallyAvailable),armor:base.armor.filter(mechanicallyAvailable),gear:base.generalGear.filter(mechanicallyAvailable),vehicles,cybernetics,attachments};
}
export function assetAvailability(kind,itemId,{rarityModifier=null}={}){
  const item=catalogAsset(kind,itemId); if(!mechanicallyAvailable(item)) throw new Error('Ativo sem preço/raridade mecânicos comprovados.');
  return availabilityProfile({rarity:Number(item.rarity),restricted:Boolean(item.restricted),rarityModifier});
}
export function createPendingAcquisition(kind,itemId,character,{requestId,rarityModifier=null,locationId=null,market=null,at=new Date().toISOString()}={}){
  const k=kindOf(kind), item=catalogAsset(k,itemId); if(!mechanicallyAvailable(item)) throw new Error('Ativo não está disponível no catálogo mecânico auditado.');
  if(!character) throw new Error('Carregue um personagem antes de propor uma aquisição.');
  const mod=rarityModifier===null||rarityModifier===undefined?(market&&Number.isInteger(Number(market.rarityModifier))?Number(market.rarityModifier):null):Number(rarityModifier);
  return {requestId:String(requestId||`acq_${Date.now()}`),kind:k,itemId:String(item.id),itemName:String(item.name),listedPrice:Number(item.price),rarity:Number(item.rarity),restricted:Boolean(item.restricted),locationId:locationId?String(locationId):null,market:market?clone(market):null,availability:assetAvailability(k,item.id,{rarityModifier:mod}),status:'aguardando_mestre',proposedAt:at,source:clone(item.source??null)};
}
export function setPendingAcquisitionRarityModifier(pending,rarityModifier){
  if(!pending||pending.status!=='aguardando_mestre') throw new Error('Não há aquisição pendente para configurar.');
  return {...clone(pending),availability:assetAvailability(pending.kind??'weapon',pending.itemId,{rarityModifier})};
}
function ensureGroup(group){const g=clone(group??{});g.ownedAssets=Array.isArray(g.ownedAssets)?g.ownedAssets:[];return g;}
function ensureCharacter(character){if(!character)throw new Error('Carregue um personagem antes desta operação.');const c=clone(character);c.weapons=Array.isArray(c.weapons)?c.weapons:[];c.armor=Array.isArray(c.armor)?c.armor:[];c.gear=Array.isArray(c.gear)?c.gear:[];return c;}
function ensureVehicleRuntime(runtime){const v=clone(runtime??{});v.vehicles=v.vehicles&&typeof v.vehicles==='object'?v.vehicles:{};v.activeVehicleId=v.activeVehicleId??null;return v;}
function registryEntry(kind,item,ownedId,{method,at,locationId=null,note='',sourceRuleIds=[]}={}){
  const base={assetId:ownedId,kind,catalogId:String(item.id),name:String(item.name),profile:clone(item.profile??item),availability:{price:Number(item.price),rarity:Number(item.rarity),restricted:Boolean(item.restricted)},ownership:{status:'owned',method:text(method,40)||'other',acquiredAt:at,locationId:locationId?String(locationId):null,note:text(note,500),sourceRuleIds:[...sourceRuleIds]},state:{condition:'operational',damage:0},visual:{assetId:null,source:null},source:clone(item.source??null)};
  if(kind==='vehicle') base.state={runtimeVehicleId:ownedId,condition:'operational'};
  return base;
}
function addMechanicalAsset(next,entry,item){
  const c=ensureCharacter(next.player?.character); next.player={...(next.player??{}),character:c};
  if(entry.kind==='weapon'){const w=catalogWeaponToCharacterWeapon(item.id,c);w.ownedAssetId=entry.assetId;c.weapons.push(w);}
  else if(entry.kind==='armor'){c.armor.push({...clone(item),sourceCatalogId:item.id,ownedAssetId:entry.assetId,quantity:1});}
  else if(entry.kind==='gear'){c.gear.push({...clone(item),sourceCatalogId:item.id,ownedAssetId:entry.assetId,quantity:1});}
  else if(entry.kind==='cybernetic'||entry.kind==='attachment'){/* Propriedade fica no registro; efeitos só entram após instalação explícita. */}
  else {const runtime=ensureVehicleRuntime(next.vehicles);const veh=createVehicleFromProfile(item.id,{id:entry.assetId,name:item.name});runtime.vehicles[entry.assetId]=veh;if(!runtime.activeVehicleId)runtime.activeVehicleId=entry.assetId;next.vehicles=runtime;}
}
export function grantCatalogAsset(state,{kind,itemId,method='reward',locationId=null,note='',sourceRuleIds=[],at=new Date().toISOString(),ownedAssetId=null}={}){
  const k=kindOf(kind), item=catalogAsset(k,itemId); if(!mechanicallyAvailable(item)) throw new Error('Só é possível conceder ativo com perfil mecânico comprovado.');
  const next=clone(state); next.group=ensureGroup(next.group); const id=ownedAssetId?String(ownedAssetId):assetId(k,item.id,`${Date.now()}_${next.group.ownedAssets.length}`);
  if(next.group.ownedAssets.some(a=>a.assetId===id)) throw new Error('Identificador de ativo já existe.');
  const entry=registryEntry(k,item,id,{method,at,locationId,note,sourceRuleIds}); addMechanicalAsset(next,entry,item); next.group.ownedAssets.push(entry);
  return {state:next,asset:clone(entry),record:{type:'asset_granted',assetId:id,kind:k,itemId:item.id,method,at,sourceRuleIds:[...sourceRuleIds]}};
}
function locateAsset(state,id){const g=ensureGroup(state.group);const index=g.ownedAssets.findIndex(a=>a.assetId===String(id));if(index<0)throw new Error('Ativo possuído não encontrado.');return {group:g,index,asset:g.ownedAssets[index]};}
function removeMechanical(next,asset){let c=ensureCharacter(next.player?.character);next.player={...(next.player??{}),character:c};if(asset.kind==='weapon')c.weapons=c.weapons.filter(x=>x.ownedAssetId!==asset.assetId);else if(asset.kind==='armor')c.armor=c.armor.filter(x=>x.ownedAssetId!==asset.assetId);else if(asset.kind==='gear')c.gear=c.gear.filter(x=>x.ownedAssetId!==asset.assetId);else if(asset.kind==='cybernetic'){const installed=(c.cybernetics??[]).find(x=>x.ownedAssetId===asset.assetId);if(installed)c=removeCybernetic(c,installed.id);next.player.character=c;}else if(asset.kind==='attachment'){const installed=(c.weapons??[]).some(w=>(w.attachments??[]).some(a=>a.ownedAssetId===asset.assetId));if(installed)throw new Error('Remova o attachment da arma antes de perder ou vender o ativo.');}else {const runtime=ensureVehicleRuntime(next.vehicles);delete runtime.vehicles[asset.assetId];if(runtime.activeVehicleId===asset.assetId)runtime.activeVehicleId=Object.keys(runtime.vehicles)[0]??null;next.vehicles=runtime;}}
export function loseOwnedAsset(state,assetId,{reason='',method='loss',sourceRuleIds=[],at=new Date().toISOString()}={}){const next=clone(state);const found=locateAsset(next,assetId);next.group=found.group;removeMechanical(next,found.asset);next.group.ownedAssets.splice(found.index,1);return {state:next,record:{type:'asset_lost',assetId:String(assetId),kind:found.asset.kind,method,reason:text(reason,500),at,sourceRuleIds:[...sourceRuleIds]}};}
function requireEvidence(ids,label){if(!Array.isArray(ids)||ids.length===0)throw new Error(`${label} exige referência mecânica recuperada.`);}
export function damageOwnedAsset(state,assetId,{amount=1,reason='',sourceRuleIds=[],at=new Date().toISOString()}={}){requireEvidence(sourceRuleIds,'Dano de ativo');const n=int(amount,'Dano do ativo',{min:1,max:100000});const next=clone(state);const found=locateAsset(next,assetId);next.group=found.group;const asset=next.group.ownedAssets[found.index];if(asset.kind==='vehicle'){const runtime=ensureVehicleRuntime(next.vehicles);const v=runtime.vehicles[asset.assetId];if(!v)throw new Error('Estado mecânico do veículo não encontrado.');v.hullDamage=Math.max(0,Number(v.hullDamage)||0)+n;runtime.vehicles[asset.assetId]=v;next.vehicles=runtime;}else{asset.state={...(asset.state??{}),condition:'damaged',damage:Math.max(0,Number(asset.state?.damage)||0)+n};}return {state:next,record:{type:'asset_damaged',assetId:asset.assetId,amount:n,reason:text(reason,500),at,sourceRuleIds:[...sourceRuleIds]}};}
export function repairOwnedAsset(state,assetId,{amount=1,cost=0,reason='',sourceRuleIds=[],at=new Date().toISOString()}={}){requireEvidence(sourceRuleIds,'Reparo de ativo');const n=int(amount,'Reparo',{min:1,max:100000}),price=int(cost,'Custo de reparo');const next=clone(state);const found=locateAsset(next,assetId);next.group=found.group;const c=ensureCharacter(next.player?.character);if(Number(c.credits)<price)throw new Error('Créditos insuficientes para o reparo.');c.credits-=price;next.player={...(next.player??{}),character:c};const asset=next.group.ownedAssets[found.index];if(asset.kind==='vehicle'){const runtime=ensureVehicleRuntime(next.vehicles);const v=runtime.vehicles[asset.assetId];if(!v)throw new Error('Estado mecânico do veículo não encontrado.');v.hullDamage=Math.max(0,(Number(v.hullDamage)||0)-n);runtime.vehicles[asset.assetId]=v;next.vehicles=runtime;}else{const d=Math.max(0,(Number(asset.state?.damage)||0)-n);asset.state={...(asset.state??{}),damage:d,condition:d>0?'damaged':'operational'};}return {state:next,record:{type:'asset_repaired',assetId:asset.assetId,amount:n,cost:price,reason:text(reason,500),at,sourceRuleIds:[...sourceRuleIds]}};}
export function sellOwnedAsset(state,assetId,{price,legalSale=false,masterMerchant=false,negotiationSucceeded=false,masterMerchantPrepaid=false,reason='',sourceRuleIds=[],at=new Date().toISOString()}={}){
  requireEvidence(sourceRuleIds,'Venda de ativo');
  if(typeof legalSale!=='boolean')throw new Error('Confirmação de venda legal inválida.');
  const basePrice=int(price,'Preço de venda'),next=clone(state),found=locateAsset(next,assetId);
  // A restricted item cannot be sold as legal merchandise. Illegal sales never
  // receive Wheel and Deal; the GM-supplied price remains authoritative.
  const restricted=Boolean(found.asset.availability?.restricted);
  if(legalSale&&restricted)throw new Error('Ativo restrito não pode receber bonificação de venda legal.');
  const c=ensureCharacter(next.player?.character);
  const sale=wheelAndDealLegalSale(c,{legalSale,restricted,basePrice});
  // This activated talent requires an explicit successful Negotiation result.
  // No bonus or strain may be injected through an unchecked GM sale proposal.
  if(masterMerchant&&!negotiationSucceeded)throw new Error('Mercador especialista exige Negociação bem-sucedida.');
  if(typeof masterMerchantPrepaid!=='boolean'||(masterMerchantPrepaid&&!masterMerchant))throw new Error('Pré-pagamento de Mercador inválido.');
  const merchant=masterMerchantOption(c,{activated:masterMerchant,checkSucceeded:negotiationSucceeded,kind:'sell',basePrice,strainAlreadyPaid:masterMerchantPrepaid});
  const finalPrice=sale.finalPrice+merchant.bonus;
  if(!Number.isSafeInteger(finalPrice)||finalPrice>100000000)throw new Error('Preço final de venda excede limite econômico.');
  if(!Number.isSafeInteger(c.credits+finalPrice))throw new Error('Créditos após venda excedem limite seguro.');
  next.group=found.group;removeMechanical(next,found.asset);next.group.ownedAssets.splice(found.index,1);
  const updated=ensureCharacter(next.player?.character);updated.credits+=finalPrice;
  if(merchant.strainCost&&!masterMerchantPrepaid)updated.derived.strain.current+=merchant.strainCost;
  next.player={...(next.player??{}),character:updated};
  return {state:next,record:{type:'asset_sold',assetId:String(assetId),kind:found.asset.kind,
    price:finalPrice,basePrice,legalSale,talentBonus:sale.bonus,talentRanks:sale.rank,
    masterMerchant:{activated:merchant.activated,strainCost:merchant.strainCost,bonus:merchant.bonus,checkSucceeded:merchant.checkSucceeded,prepaid:masterMerchantPrepaid},
    reason:text(reason,500),at,sourceRuleIds:[...sourceRuleIds]}};
}
export function resolvePendingAcquisition(state,pending,{approved,finalPrice=null,masterMerchant=false,negotiationSucceeded=false,masterMerchantPrepaid=false,reason=null,sourceRuleIds=[]}={}){
  if(!pending||pending.status!=='aguardando_mestre')throw new Error('Não há aquisição pendente para resolver.');if(typeof approved!=='boolean')throw new Error('A decisão de aquisição precisa aprovar ou rejeitar o pedido.');requireEvidence(sourceRuleIds,'Decisão de aquisição');const at=new Date().toISOString();
  if(masterMerchant&&!negotiationSucceeded&&approved)throw new Error('Mercador especialista exige Negociação bem-sucedida.');
  if(typeof masterMerchantPrepaid!=='boolean'||(masterMerchantPrepaid&&!masterMerchant))throw new Error('Pré-pagamento de Mercador inválido.');
  const base=clone(state),c=ensureCharacter(base.player?.character);
  const quoted=approved?int(finalPrice,'Preço final'):0;
  const merchant=masterMerchantOption(c,{activated:masterMerchant,checkSucceeded:negotiationSucceeded&&approved,kind:'buy',basePrice:quoted,strainAlreadyPaid:masterMerchantPrepaid});
  if(merchant.strainCost&&!masterMerchantPrepaid)c.derived.strain.current+=merchant.strainCost;
  base.player={...(base.player??{}),character:c};
  if(!approved)return {state:base,record:{...clone(pending),status:'rejeitada',resolvedAt:at,reason:text(reason,500)||null,
    masterMerchant:{activated:merchant.activated,strainCost:merchant.strainCost,bonus:0,prepaid:masterMerchantPrepaid},sourceRuleIds:[...sourceRuleIds]}};
  const price=merchant.finalPrice;
  if(Number(c.credits)<price)throw new Error('Créditos insuficientes para concluir a aquisição autorizada.');
  c.credits-=price;base.player={...(base.player??{}),character:c};
  const granted=grantCatalogAsset(base,{kind:pending.kind??'weapon',itemId:pending.itemId,method:'purchase',locationId:pending.locationId,note:reason??'',sourceRuleIds,at,ownedAssetId:assetId(pending.kind??'weapon',pending.itemId,pending.requestId)});
  return {state:granted.state,record:{...clone(pending),status:'adquirida',resolvedAt:at,finalPrice:price,
    masterMerchant:{activated:merchant.activated,strainCost:merchant.strainCost,bonus:merchant.bonus,prepaid:masterMerchantPrepaid},reason:text(reason,500)||null,ownedAssetId:granted.asset.assetId,sourceRuleIds:[...sourceRuleIds]}};
}
export function economySummary(state,{market=null}={}){const catalog=genericEconomyCatalog();const compact=x=>({id:x.id,name:x.name,price:x.price,rarity:x.rarity,restricted:Boolean(x.restricted)});return {market:market?clone(market):null,catalog:{weapons:catalog.weapons.map(compact),armor:catalog.armor.map(compact),gear:catalog.gear.map(compact),vehicles:catalog.vehicles.map(compact),cybernetics:catalog.cybernetics.map(compact),attachments:catalog.attachments.map(compact)},ownedAssets:ensureGroup(state.group).ownedAssets.map(a=>({assetId:a.assetId,kind:a.kind,catalogId:a.catalogId,name:a.name,ownership:clone(a.ownership),state:clone(a.state),visual:clone(a.visual)}))};}
