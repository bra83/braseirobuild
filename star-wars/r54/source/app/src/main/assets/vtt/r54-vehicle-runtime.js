/* R54_VEHICLE_BRIDGE_RUNTIME_V1 — expose the canonical controller vehicle API to the live game bridge. */
(()=>{'use strict';
const GUARD='__R54_VEHICLE_BRIDGE_RUNTIME_V1__';if(window[GUARD])return;window[GUARD]=true;
const STATE_EVENT='braseiro:r21-state-changed';
const wrap=(fn)=>{try{return{ok:true,data:fn()}}catch(error){return{ok:false,data:null,message:String(error?.message||error)}}};
function controller(){
 const resolved=window.BraseiroGameBridge?._controllerForExistingCampaign?.();
 if(!resolved?.ok||!resolved.data)throw new Error(resolved?.message||'Campanha indisponível para operações de nave.');
 return resolved.data;
}
function install(){
 const original=window.BraseiroGameBridge;
 if(!original||typeof original._controllerForExistingCampaign!=='function'){setTimeout(install,25);return}
 if(typeof original.addVehicle==='function'&&typeof original.selectVehicle==='function')return;
 let bridge=original;
 const methods={
  vehicleProfiles:()=>wrap(()=>controller().vehicleProfiles()),
  activeVehicle:()=>wrap(()=>controller().activeVehicle()),
  addVehicle:(profileKey,options={})=>{const r=wrap(()=>controller().addVehicle(profileKey,options));if(r.ok)window.dispatchEvent(new Event(STATE_EVENT));return r},
  selectVehicle:(vehicleId)=>{const r=wrap(()=>controller().selectVehicle(vehicleId));if(r.ok)window.dispatchEvent(new Event(STATE_EVENT));return r}
 };
 try{
  for(const [name,fn] of Object.entries(methods))if(typeof bridge[name]!=='function')Object.defineProperty(bridge,name,{value:fn,writable:false,configurable:true,enumerable:true});
 }catch(error){
  const facade=Object.create(original);
  for(const [name,fn] of Object.entries(methods))Object.defineProperty(facade,name,{value:fn,writable:false,configurable:true,enumerable:true});
  try{window.BraseiroGameBridge=facade;bridge=facade}catch{}
 }
 window.__R54_VEHICLE_BRIDGE_STATUS__={ok:typeof window.BraseiroGameBridge?.addVehicle==='function'&&typeof window.BraseiroGameBridge?.selectVehicle==='function'};
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',install,{once:true});else install();
})();
