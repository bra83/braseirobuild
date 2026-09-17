#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit('usage: fix_vehicle_bridge_runtime.py <android-project-root>')
root=Path(sys.argv[1]).resolve()
vtt=root/'app/src/main/assets/vtt'
html=vtt/'r46-shell.html'
js=vtt/'r54-vehicle-runtime.js'
if not html.is_file():
    raise SystemExit(f'missing shell: {html}')

runtime=r'''/* R54_VEHICLE_BRIDGE_RUNTIME_V1 — expose the canonical controller vehicle API to the live game bridge. */
(()=>{'use strict';
const GUARD='__R54_VEHICLE_BRIDGE_RUNTIME_V1__';if(window[GUARD])return;window[GUARD]=true;
const STATE_EVENT='braseiro:r21-state-changed';
const wrap=(fn)=>{try{return{ok:true,data:fn()}}catch(error){return{ok:false,data:null,message:String(error?.message||error)}}};
function install(){
 const original=window.BraseiroGameBridge;
 if(!original||typeof original._controllerForExistingCampaign!=='function'){setTimeout(install,25);return}
 if(typeof original.addVehicle==='function'&&typeof original.selectVehicle==='function')return;
 let bridge=original;
 const methods={
  vehicleProfiles:()=>wrap(()=>original._controllerForExistingCampaign().vehicleProfiles()),
  activeVehicle:()=>wrap(()=>original._controllerForExistingCampaign().activeVehicle()),
  addVehicle:(profileKey,options={})=>{const r=wrap(()=>original._controllerForExistingCampaign().addVehicle(profileKey,options));if(r.ok)window.dispatchEvent(new Event(STATE_EVENT));return r},
  selectVehicle:(vehicleId)=>{const r=wrap(()=>original._controllerForExistingCampaign().selectVehicle(vehicleId));if(r.ok)window.dispatchEvent(new Event(STATE_EVENT));return r}
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
'''
js.write_text(runtime,encoding='utf-8')
text=html.read_text('utf-8')
tag='<script src="r54-vehicle-runtime.js"></script>'
if tag not in text:
    if '</body>' in text:text=text.replace('</body>',tag+'\n</body>',1)
    else:text+='\n'+tag+'\n'
    html.write_text(text,encoding='utf-8')
final=js.read_text('utf-8'); h=html.read_text('utf-8')
checks={
 'mounted':tag in h,
 'controller':"_controllerForExistingCampaign" in final,
 'profiles':"vehicleProfiles" in final,
 'add':"addVehicle" in final,
 'select':"selectVehicle" in final,
 'event':STATE_EVENT if False else ('braseiro:r21-state-changed' in final),
}
bad=[k for k,v in checks.items() if not v]
if bad:raise SystemExit('R54 vehicle bridge patch failed: '+', '.join(bad))
print('R54_VEHICLE_BRIDGE_RUNTIME_PASS',checks)
