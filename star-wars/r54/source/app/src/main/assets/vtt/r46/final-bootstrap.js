(function(){
'use strict';
try{
  const bridge=window.BraseiroGameBridge;
  if(!bridge||typeof bridge.readState!=='function'||typeof bridge.initializeCampaign!=='function')return;
  const current=bridge.readState();
  if(current&&current.ok===true&&current.data===null){
    const created=bridge.initializeCampaign();
    window.__BRASEIRO_FIRST_BOOT__={ok:!!created?.ok,created:!!created?.data?.created};
  }else{
    window.__BRASEIRO_FIRST_BOOT__={ok:!!current?.ok,created:false};
  }
}catch(error){
  window.__BRASEIRO_FIRST_BOOT__={ok:false,created:false,error:String(error&&error.message||error)};
}
})();
