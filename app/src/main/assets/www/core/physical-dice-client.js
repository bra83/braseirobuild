function parseNative(value){try{return typeof value==='string'?JSON.parse(value):value}catch{return{status:'ERROR',reason:'RESPOSTA_NATIVA_INVALIDA'}}}
let fallbackCounter=0;function requestId(){const a=globalThis.crypto?.getRandomValues?globalThis.crypto.getRandomValues(new Uint32Array(2)):null;fallbackCounter+=1;return a?`dice:${a[0].toString(16)}${a[1].toString(16)}`:`dice:${Date.now()}:${fallbackCounter}`}

export class PhysicalDiceClient{
  constructor({native=globalThis.BraseiroNative,eventTarget=globalThis,timeoutMs=32000}={}){this.native=native;this.eventTarget=eventTarget;this.timeoutMs=timeoutMs;}
  available(){return Boolean(this.native&&typeof this.native.startPhysicalDice==='function')}
  roll({formula,material='bone'}={}){
    if(!this.available())return Promise.resolve(Object.freeze({status:'UNAVAILABLE',reason:'DADOS_3D_NATIVOS_INDISPONIVEIS'}));
    const id=requestId();
    return new Promise(resolve=>{
      let done=false;const finish=result=>{if(done)return;done=true;clearTimeout(timer);this.eventTarget?.removeEventListener?.('braseiro-physical-dice',handler);resolve(Object.freeze(result));};
      const handler=event=>{const d=event?.detail;if(!d||d.requestId!==id)return;finish(d)};
      this.eventTarget?.addEventListener?.('braseiro-physical-dice',handler);
      const timer=setTimeout(()=>finish({status:'ERROR',requestId:id,reason:'TEMPO_LIMITE_DADOS_3D'}),this.timeoutMs);
      const start=parseNative(this.native.startPhysicalDice(JSON.stringify({requestId:id,formula:String(formula??''),material:String(material??'bone')})));
      if(start.status!=='PENDING')finish({...start,requestId:start.requestId??id});
    });
  }
}
export function createPhysicalDiceClient(options={}){return new PhysicalDiceClient(options)}
