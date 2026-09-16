function parseNative(value){try{return typeof value==='string'?JSON.parse(value):value}catch{return {status:'ERROR',reason:'INVALID_NATIVE_JSON'}}}

export class GeminiImageClient{
  constructor({native=globalThis.BraseiroNative,timeoutMs=195000}={}){this.native=native;this.timeoutMs=timeoutMs;}
  status(){return this.native?.geminiStatus?parseNative(this.native.geminiStatus()):{status:'UNAVAILABLE',configured:false};}
  generate({prompt,model=null,aspectRatio='16:9',imageSize='1K',previousInteractionId=null,referenceImageUrls=[]}={}){
    if(!this.native?.geminiGenerateImageAsync)return Promise.resolve({status:'UNAVAILABLE',reason:'NATIVE_IMAGE_GENERATION_UNAVAILABLE'});
    const payload={prompt:String(prompt??'').trim(),aspectRatio,imageSize,referenceImageUrls:[...(referenceImageUrls??[])].filter(Boolean).slice(0,5)};
    if(model)payload.model=model;if(previousInteractionId)payload.previousInteractionId=previousInteractionId;
    return new Promise(resolve=>{
      let requestId=null,done=false,timer=null;
      const finish=result=>{if(done)return;done=true;if(timer)clearTimeout(timer);window.removeEventListener('braseiro-gemini',onResult);resolve(result)};
      const onResult=e=>{const d=e?.detail??{};if(requestId&&d.requestId===requestId)finish(d)};
      window.addEventListener('braseiro-gemini',onResult);
      const start=parseNative(this.native.geminiGenerateImageAsync(JSON.stringify(payload)));
      if(start.status!=='PENDING'||!start.requestId){finish(start);return;}
      requestId=start.requestId;timer=setTimeout(()=>finish({status:'TIMEOUT',reason:'A geração de imagem excedeu o tempo limite.'}),this.timeoutMs);
    });
  }
}
export function createGeminiImageClient(options={}){return new GeminiImageClient(options)}
