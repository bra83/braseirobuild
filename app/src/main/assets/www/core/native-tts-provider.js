function parseNative(value){try{return typeof value==='string'?JSON.parse(value):value}catch{return {status:'ERROR',reason:'RESPOSTA_NATIVA_INVÁLIDA'}}}
const terminalTtsEvents=new Map();
if(typeof window!=='undefined'&&window?.addEventListener)window.addEventListener('braseiro-tts',e=>{const d=e?.detail??{};const id=String(d.utteranceId??'');if(id&&['DONE','ERROR','STOPPED'].includes(String(d.status))){terminalTtsEvents.set(id,d);setTimeout(()=>terminalTtsEvents.delete(id),120000);}});
export class NativeTtsProvider{
  constructor({native=globalThis.BraseiroNative}={}){this.native=native;}
  status(){return this.native?.ttsStatus?parseNative(this.native.ttsStatus()):{status:'UNAVAILABLE',ready:false};}
  configure({provider='GEMINI',model='',voice='Charon'}={}){if(!this.native?.ttsConfigure)return{status:'UNAVAILABLE'};return parseNative(this.native.ttsConfigure(JSON.stringify({provider,model,voice})));}
  async speak({text,voice=null,model=null,provider=null,style=null,rate=1,pitch=1,volume=1,language='pt-BR',sessionId=null,append=false,role='NARRADOR',allowFallback=true}={}){
    if(!this.native?.ttsSpeak)return{status:'UNAVAILABLE'};
    const request={text:String(text??''),rate,pitch,volume,language,append,role,allowFallback};
    if(voice)request.voice=String(voice);if(model)request.model=String(model);if(provider)request.provider=String(provider);if(style)request.style=String(style);if(sessionId)request.sessionId=String(sessionId);
    return parseNative(this.native.ttsSpeak(JSON.stringify(request)));
  }
  waitForUtterance(utteranceId,{timeoutMs=90000}={}){const id=String(utteranceId??'');if(!id)return Promise.resolve({status:'INVALID_UTTERANCE'});const cached=terminalTtsEvents.get(id);if(cached){terminalTtsEvents.delete(id);return Promise.resolve(cached);}return new Promise(resolve=>{let done=false,timer=null;const finish=r=>{if(done)return;done=true;if(timer)clearTimeout(timer);window.removeEventListener('braseiro-tts',onEvent);terminalTtsEvents.delete(id);resolve(r)};const onEvent=e=>{const d=e?.detail??{};if(String(d.utteranceId??'')!==id)return;if(['DONE','ERROR','STOPPED'].includes(String(d.status)))finish(d)};window.addEventListener('braseiro-tts',onEvent);const after=terminalTtsEvents.get(id);if(after){finish(after);return;}timer=setTimeout(()=>finish({status:'TIMEOUT',utteranceId:id}),timeoutMs);});}
  stop(){return this.native?.ttsStop?parseNative(this.native.ttsStop()):{status:'UNAVAILABLE'};}
}
export function createNativeTtsProvider(options={}){return new NativeTtsProvider(options)}
