export const STARTER_CAMPAIGN_ID='sombras-gelo-ferro-starter-v1';
function parse(value){try{return typeof value==='string'?JSON.parse(value):value}catch{return null}}
export function resolveActiveCampaignId(persistence,{preferredCampaignId=STARTER_CAMPAIGN_ID,native=globalThis.BraseiroNative}={}){
  const explicit=globalThis.__BRASEIRO_ACTIVE_CAMPAIGN_ID__;if(explicit)return String(explicit);
  if(native?.getActiveCampaignId){const r=parse(native.getActiveCampaignId());if(r?.status==='OK'&&r.campaignId)return String(r.campaignId);}
  const list=persistence.listCampaigns?.();if(list?.status&&list.status!=='OK')return null;const campaigns=list?.campaigns??[];const preferred=campaigns.find(c=>c.campaignId===preferredCampaignId);return preferred?.campaignId??campaigns[0]?.campaignId??null;
}
export function setRuntimeActiveCampaignId(campaignId,{native=globalThis.BraseiroNative}={}){const id=String(campaignId??'').trim();if(!id)return{status:'CAMPAIGN_ID_REQUIRED'};globalThis.__BRASEIRO_ACTIVE_CAMPAIGN_ID__=id;if(native?.setActiveCampaignId){const r=parse(native.setActiveCampaignId(id));if(r?.status&&r.status!=='OK')return r;}return{status:'OK',campaignId:id}}
