import { canonicalJson } from './canonical-json.js';
import { mundaneEquipmentItem, COPPER_PER_SILVER } from './wwn-mundane-equipment-core.js';
import { inventoryLoad, moveInventoryItem, breakBundledInventoryItem } from './wwn-inventory-core.js';

function clone(v){return structuredClone(v);}
function load(persistence,campaignId){const loaded=persistence.loadCampaign(campaignId);if(loaded.status!=='OK')return{loaded,state:null};const state=JSON.parse(loaded.stateJson);if(state.revision!==loaded.revision)throw new Error('State/load revision mismatch');return{loaded,state};}
function roster(state){const out={...(state.charactersState??{})};const c=state.characterState;if(c?.characterId&&!out[c.characterId])out[c.characterId]=clone(c);return out;}
function resolvePc(state,pcId){const r=roster(state),id=String(pcId??state.characterState?.characterId??'');return{id,pc:r[id]??null};}
function savePc(next,id,pc){const r=roster(next);r[id]=clone(pc);next.charactersState=r;if(next.characterState?.characterId===id)next.characterState=clone(pc);}
function wallet(pc){const silver=pc?.silver,copper=pc?.copper??0;if(!Number.isInteger(silver)||silver<0||!Number.isInteger(copper)||copper<0||copper>=COPPER_PER_SILVER)return null;return{silver,copper,totalCopper:silver*COPPER_PER_SILVER+copper};}
function setWallet(pc,totalCopper){if(!Number.isInteger(totalCopper)||totalCopper<0)throw new RangeError('totalCopper must be non-negative integer');pc.silver=Math.floor(totalCopper/COPPER_PER_SILVER);pc.copper=totalCopper%COPPER_PER_SILVER;}
function commit({persistence,loaded,next,campaignId,kind,suffix,payload}){const revision=loaded.revision+1;next.revision=revision;const eventId=`mech:${campaignId}:r${revision}:${suffix}`;const receipt=persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind:kind,eventPayloadJson:canonicalJson({eventId,revision,...payload}),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState?.counters??{})});return{receipt,revision,eventId};}
function isPortable(cat){return ['GEAR','AMMO','BUNDLE'].includes(String(cat.kind??'GEAR').toUpperCase());}
function materialize(cat,{itemId,carryState}){return{itemId,catalogId:cat.catalogId,name:cat.name,kind:cat.kind??'GEAR',priceCopper:cat.costCp,priceSilver:cat.priceSilver,encumbrance:Number.isInteger(cat.encumbrance)?cat.encumbrance:0,carryState,weightRule:cat.weightRule??'NORMAL',quantity:cat.quantity??1,notes:clone(cat.notes??[]),sourceRefs:clone(cat.sourceRefs??[])};}

export class MundaneEquipmentService{
  constructor({persistence}){if(!persistence)throw new TypeError('persistence required');this.persistence=persistence;}

  purchase({campaignId,pcId=null,catalogId,quantity=1,carryState='STOWED'}={}){
    if(!Number.isInteger(quantity)||quantity<1)return Object.freeze({status:'INVALID_QUANTITY_NO_MUTATION'});
    const {loaded,state}=load(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    const {id,pc}=resolvePc(state,pcId);if(!pc)return Object.freeze({status:'CHARACTER_NOT_FOUND_NO_MUTATION',revision:loaded.revision});
    if(state.encounterState?.combat?.active===true)return Object.freeze({status:'PURCHASE_NOT_ALLOWED_IN_COMBAT_NO_MUTATION',revision:loaded.revision});
    const cat=mundaneEquipmentItem(catalogId);if(!cat)return Object.freeze({status:'UNKNOWN_CATALOG_ITEM_NO_MUTATION',revision:loaded.revision});
    const currentWallet=wallet(pc);if(!currentWallet)return Object.freeze({status:'MISSING_OR_INVALID_CURRENCY_EVIDENCE_NO_MUTATION',revision:loaded.revision});
    const costCopper=cat.costCp*quantity;if(currentWallet.totalCopper<costCopper)return Object.freeze({status:'INSUFFICIENT_FUNDS_NO_MUTATION',revision:loaded.revision,requiredCopper:costCopper,availableCopper:currentWallet.totalCopper});
    let target=String(carryState??'STOWED').toUpperCase();if(!['STOWED','READIED','WORN'].includes(target))return Object.freeze({status:'INVALID_CARRY_STATE_NO_MUTATION',revision:loaded.revision});
    if(target==='WORN'&&cat.weightRule!=='NO_ENCUMBRANCE_WHILE_WORN')return Object.freeze({status:'ITEM_CANNOT_BE_WORN_NO_MUTATION',revision:loaded.revision});
    const next=clone(state),npc=clone(pc);setWallet(npc,currentWallet.totalCopper-costCopper);
    const newRevision=loaded.revision+1;const acquired=[];
    if(isPortable(cat)){
      if(!Array.isArray(npc.inventory))return Object.freeze({status:'MISSING_EVIDENCE_INVENTORY_NO_MUTATION',revision:loaded.revision});
      for(let n=0;n<quantity;n++){const item=materialize(cat,{itemId:`mundane:${cat.catalogId}:r${newRevision}:${n+1}`,carryState:target});npc.inventory.push(item);acquired.push(item);}
      const loadResult=inventoryLoad(npc);if(loadResult.status!=='OK')return Object.freeze({status:`${loadResult.status}_NO_MUTATION`,revision:loaded.revision});
    } else if(['BEAST','TRANSPORT'].includes(String(cat.kind).toUpperCase())){
      npc.ownedAssets=Array.isArray(npc.ownedAssets)?npc.ownedAssets:[];
      for(let n=0;n<quantity;n++){const asset={assetId:`asset:${cat.catalogId}:r${newRevision}:${n+1}`,catalogId:cat.catalogId,name:cat.name,kind:cat.kind,sourceRefs:clone(cat.sourceRefs)};npc.ownedAssets.push(asset);acquired.push(asset);}
    } else if(String(cat.kind).toUpperCase()==='SERVICE'){
      acquired.push({catalogId:cat.catalogId,name:cat.name,kind:'SERVICE',units:quantity,consumedOnPurchase:true,sourceRefs:clone(cat.sourceRefs)});
    } else return Object.freeze({status:'UNSUPPORTED_MUNDANE_ITEM_KIND_NO_MUTATION',revision:loaded.revision});
    savePc(next,id,npc);
    const c=commit({persistence:this.persistence,loaded,next,campaignId,kind:'MUNDANE_PURCHASED',suffix:`mundane:${id}:${cat.catalogId}`,payload:{pcId:id,catalogId:cat.catalogId,quantity,costCopper,walletBefore:currentWallet,walletAfter:wallet(npc),acquired}});
    return Object.freeze({status:c.receipt.status,receipt:c.receipt,pcId:id,catalogId:cat.catalogId,quantity,costCopper,walletAfter:wallet(npc),acquired:Object.freeze(clone(acquired)),character:c.receipt.status==='OK'?npc:null});
  }

  setCarryState({campaignId,pcId=null,itemId,to}){
    const {loaded,state}=load(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});const {id,pc}=resolvePc(state,pcId);if(!pc)return Object.freeze({status:'CHARACTER_NOT_FOUND_NO_MUTATION',revision:loaded.revision});
    const moved=moveInventoryItem(pc,{itemId,to});if(moved.status!=='OK')return Object.freeze({status:`${moved.status}_NO_MUTATION`,revision:loaded.revision});
    const next=clone(state);savePc(next,id,moved.character);const c=commit({persistence:this.persistence,loaded,next,campaignId,kind:'MUNDANE_CARRY_STATE_CHANGED',suffix:`mundane-carry:${id}:${itemId}`,payload:{pcId:id,itemId,to:String(to).toUpperCase(),actionCost:moved.actionCost,load:moved.load}});
    return Object.freeze({status:c.receipt.status,receipt:c.receipt,load:moved.load,character:c.receipt.status==='OK'?moved.character:null});
  }

  breakBundle({campaignId,pcId=null,itemId}){
    const {loaded,state}=load(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});const {id,pc}=resolvePc(state,pcId);if(!pc)return Object.freeze({status:'CHARACTER_NOT_FOUND_NO_MUTATION',revision:loaded.revision});
    const result=breakBundledInventoryItem(pc,{itemId});if(result.status!=='OK')return Object.freeze({status:`${result.status}_NO_MUTATION`,revision:loaded.revision});
    const next=clone(state);savePc(next,id,result.character);const c=commit({persistence:this.persistence,loaded,next,campaignId,kind:'GEAR_BUNDLE_OPENED',suffix:`open-bundle:${id}:${itemId}`,payload:{pcId:id,itemId,createdItemIds:result.createdItemIds,actionCost:'MAIN_ACTION',load:result.load}});
    return Object.freeze({status:c.receipt.status,receipt:c.receipt,createdItemIds:result.createdItemIds,load:result.load,character:c.receipt.status==='OK'?result.character:null});
  }
}
