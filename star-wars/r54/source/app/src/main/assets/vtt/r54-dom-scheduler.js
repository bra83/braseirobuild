/* R54 P79 — coalesced DOM repair scheduler for physical-device performance. */
(function(){
'use strict';
if(window.BraseiroDomScheduler)return;
const tasks=new Map();
let timer=0,idleToken=0,running=false,dirty=false,lastRun=0,suppressUntil=0;
const MIN_GAP=120,BATCH_DELAY=70;
const now=()=>((window.performance&&performance.now)?performance.now():Date.now());
function run(reason){
  if(running){dirty=true;return}
  running=true;dirty=false;
  const list=[...tasks.values()];
  try{for(const fn of list){try{fn(reason)}catch(_){}}}
  finally{
    lastRun=now();suppressUntil=lastRun+80;running=false;
    if(dirty)queue('dirty');
  }
}
function dispatch(reason,immediate){
  if(document.hidden&&!immediate)return;
  const elapsed=now()-lastRun;
  const delay=immediate?0:Math.max(BATCH_DELAY,MIN_GAP-elapsed);
  if(timer)return;
  timer=setTimeout(()=>{
    timer=0;
    const invoke=()=>{idleToken=0;run(reason)};
    if(!immediate&&typeof requestIdleCallback==='function')idleToken=requestIdleCallback(invoke,{timeout:220});
    else requestAnimationFrame(invoke);
  },delay);
}
function queue(reason,immediate=false){dirty=true;dispatch(reason,immediate)}
function register(key,fn){if(!key||typeof fn!=='function')return()=>{};tasks.set(String(key),fn);queue('register:'+key,true);return()=>tasks.delete(String(key))}
function meaningful(records){
  if(now()<suppressUntil)return false;
  for(const r of records){
    if(r.type!=='childList')continue;
    const nodes=[...r.addedNodes,...r.removedNodes];
    for(const n of nodes){
      if(n.nodeType===1)return true;
      if(n.nodeType===3&&String(n.textContent||'').trim())return true;
    }
  }
  return false;
}
function start(){
  if(start.done)return;start.done=true;
  const target=document.body||document.documentElement;
  if(!target)return;
  new MutationObserver(records=>{if(meaningful(records))queue('dom')}).observe(target,{subtree:true,childList:true});
  queue('start',true);
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',start,{once:true});else start();
window.addEventListener('popstate',()=>queue('popstate',true));
for(const ev of ['braseiro:r21-state-changed','braseiroGeminiNativeEvent','braseiroNpcTokenNativeEvent'])window.addEventListener(ev,()=>queue(ev));
document.addEventListener('click',e=>{if(e.target?.closest?.('button,a,input,select,textarea,[role="button"],[data-point]'))queue('click')},true);
document.addEventListener('visibilitychange',()=>{if(!document.hidden)queue('visible',true)});
window.BraseiroDomScheduler=Object.freeze({register,queue,runNow:()=>run('manual'),taskCount:()=>tasks.size});
})();
