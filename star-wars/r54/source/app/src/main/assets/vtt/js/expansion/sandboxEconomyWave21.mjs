const c=x=>structuredClone(x);
const CATS=new Set(["weapon","armor","gear","vehicle"]);
export function ensureEconomy(state={}){
 const s=c(state);s.expansion=s.expansion||{};s.expansion.schema=Math.max(Number(s.expansion.schema)||0,21);
 s.expansion.economy=s.expansion.economy||{ledger:[],ownership:{},assets:{},proposals:[]};
 return s;
}
function assetKey(category,id){if(!CATS.has(category)||!id)throw new Error("asset");return `${category}:${id}`}
export function registerAsset(state,{category,id,profile={},visual=null,availability={},ownerId=null,condition="SERVICEABLE"}){
 const s=ensureEconomy(state),key=assetKey(category,id);
 s.expansion.economy.assets[key]={key,category,id,profile:c(profile),visual:c(visual),availability:c(availability),state:{condition},ownership:{ownerId}};
 if(ownerId)s.expansion.economy.ownership[key]=ownerId; return s;
}
export function assetView(state,category,id){const s=ensureEconomy(state);return c(s.expansion.economy.assets[assetKey(category,id)]||null)}
function proposal(state,type,payload){
 const s=ensureEconomy(state),e=s.expansion.economy;
 const p={id:`eco_${e.proposals.length}`,type,...c(payload),status:"PENDING_MASTER",requiresControllerCommit:true,mutatesCredits:false,mutatesOwnership:false,mutatesState:false};
 e.proposals.push(p);return {state:s,proposal:c(p)};
}
export const proposePurchase=(s,x)=>proposal(s,"PURCHASE",x);
export const proposeSale=(s,x)=>proposal(s,"SALE",x);
export const proposeReward=(s,x)=>proposal(s,"REWARD",x);
export const proposeLoot=(s,x)=>proposal(s,"LOOT",x);
export const proposeLoss=(s,x)=>proposal(s,"LOSS",x);
export const proposeRepair=(s,x)=>proposal(s,"REPAIR",x);
export function masterResolve(state,id,{approved=false,masterId=null}={}){
 const s=ensureEconomy(state),p=s.expansion.economy.proposals.find(x=>x.id===id);if(!p||p.status!=="PENDING_MASTER")throw new Error("proposal");
 p.status=approved?"MASTER_APPROVED_CONTROLLER_PENDING":"MASTER_REJECTED";p.masterId=masterId;p.requiresControllerCommit=approved;return {state:s,proposal:c(p)};
}
export function controllerCommit(state,id,{controllerId="controller"}={}){
 const s=ensureEconomy(state),e=s.expansion.economy,p=e.proposals.find(x=>x.id===id);if(!p||p.status!=="MASTER_APPROVED_CONTROLLER_PENDING")throw new Error("approval");
 const amount=Number(p.amount||0),credits=Number(s.credits||0),key=p.category&&p.assetId?assetKey(p.category,p.assetId):null;
 if(["PURCHASE","REPAIR"].includes(p.type)&&amount>credits)throw new Error("credits");
 if(p.type==="PURCHASE"){s.credits=credits-amount;if(key){e.ownership[key]=p.ownerId||"party";if(e.assets[key])e.assets[key].ownership.ownerId=e.ownership[key]}}
 if(p.type==="SALE"){s.credits=credits+amount;if(key){delete e.ownership[key];if(e.assets[key])e.assets[key].ownership.ownerId=null}}
 if(p.type==="REWARD"){s.credits=credits+amount}
 if(p.type==="LOOT"&&key){e.ownership[key]=p.ownerId||"party";if(e.assets[key])e.assets[key].ownership.ownerId=e.ownership[key]}
 if(p.type==="LOSS"&&key){delete e.ownership[key];if(e.assets[key])e.assets[key].ownership.ownerId=null}
 if(p.type==="REPAIR"){s.credits=credits-amount;if(key&&e.assets[key])e.assets[key].state.condition=p.resultCondition||"SERVICEABLE"}
 p.status="COMMITTED";p.controllerId=controllerId;p.mutatesCredits=["PURCHASE","SALE","REWARD","REPAIR"].includes(p.type);p.mutatesOwnership=["PURCHASE","SALE","LOOT","LOSS"].includes(p.type);p.mutatesState=p.type==="REPAIR";
 e.ledger.push({proposalId:p.id,type:p.type,amount,controllerId});return {state:s,proposal:c(p)};
}
export function marketOffer(asset,{locationModifier=0}={}){
 const rarity=Number(asset?.availability?.rarity??asset?.profile?.rarity??0),restricted=Boolean(asset?.availability?.restricted??asset?.profile?.restricted);
 return {assetKey:asset?.key||null,rarity,restricted,locationModifier,availabilityCheckRequired:true,authority:"RARITY_AVAILABILITY_EXISTING_RULES"};
}
export function economySummary(state){const s=ensureEconomy(state),e=s.expansion.economy;return {credits:Number(s.credits||0),assets:Object.keys(e.assets).length,owned:Object.keys(e.ownership).length,pending:e.proposals.filter(x=>x.status==="PENDING_MASTER").length,controllerPending:e.proposals.filter(x=>x.status==="MASTER_APPROVED_CONTROLLER_PENDING").length,ledger:e.ledger.length}}
export function barbaraEconomyContext(state){const s=ensureEconomy(state);return {summary:economySummary(s),assets:c(s.expansion.economy.assets),proposals:c(s.expansion.economy.proposals),policy:"MASTER_THEN_CONTROLLER_COMMIT"}}
