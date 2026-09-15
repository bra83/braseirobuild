import { DeterministicNarrativeProvider, buildFilteredNarrativeContext } from './narrative-provider.js';
import { canonicalJson } from './canonical-json.js';

const SYSTEM_INSTRUCTION=`Você é Barbara, Mestre de uma campanha solo de Worlds Without Number.
A mecânica já foi resolvida pelo Rules Engine canônico. Você NUNCA altera dados, sucesso/falha, dano, HP, System Strain, posição, inventário, tempo, iniciativa, recursos ou qualquer outro resultado mecânico recebido.
Narre de forma cinematográfica, concreta e concisa em português do Brasil. Preserve continuidade, NPCs, fatos públicos e consequências já estabelecidas.
Não revele conteúdo GM-only, segredos não descobertos ou informação ausente do contexto. Se faltar informação, narre apenas o que é perceptível e não invente regra.`;

function parseNative(value){try{return typeof value==='string'?JSON.parse(value):value}catch{return {status:'ERROR',reason:'INVALID_NATIVE_JSON'}}}
function safeStatus(native){if(!native?.geminiStatus)return {status:'UNAVAILABLE',configured:false};return parseNative(native.geminiStatus())}
function defaultRagSearch(native){if(!native?.ragSearch)return null;return query=>{const result=parseNative(native.ragSearch(JSON.stringify({query:String(query??''),limit:4,scope:'NARRATIVE_PUBLIC'})));return result.status==='OK'?(result.results??[]):[]}}
function defaultRagSearchAsync(native){if(!native?.ragSearchAsync)return null;return query=>new Promise(resolve=>{let requestId=null,done=false,timer=null;const finish=r=>{if(done)return;done=true;if(timer)clearTimeout(timer);window.removeEventListener('braseiro-rag',onResult);resolve(r?.status==='OK'?(r.results??[]):[])};const onResult=e=>{const d=e?.detail??{};if(requestId&&d.requestId===requestId)finish(d)};window.addEventListener('braseiro-rag',onResult);const start=parseNative(native.ragSearchAsync(JSON.stringify({query:String(query??''),limit:4,scope:'NARRATIVE_PUBLIC'})));if(start.status!=='PENDING'||!start.requestId){finish(start);return;}requestId=start.requestId;timer=setTimeout(()=>finish({status:'TIMEOUT',results:[]}),30000);})}
function requestText(context,{hasImage=false}={}){const declaration=context?.mechanicalEvent?.kind==='PLAYER_DECLARATION';return `${declaration?'DECLARAÇÃO DO JOGADOR E CONTEXTO PÚBLICO':'RESULTADO MECÂNICO AUTORITATIVO E CONTEXTO PÚBLICO'}:
${canonicalJson(context)}

${hasImage?'A imagem anexada é a referência visual privada da cena atual. Use apenas o que for visualmente perceptível nela; não trate a imagem como autoridade mecânica.\n\n':''}${declaration?'Nenhuma resolução mecânica foi feita para esta declaração. Interprete a intenção, faça NPCs responderem e descreva apenas consequências imediatamente perceptíveis que não dependam de teste. Se sucesso, falha, dano, gasto de recurso, deslocamento, combate ou qualquer outro resultado mecânico for necessário, pare antes de decidir o resultado e diga claramente que a ação exige resolução mecânica.':'Narre somente a consequência perceptível deste evento. Não reinterprete a mecânica.'}`}

function requestPayload(status,context,imageUrl){return {model:status.textModel??status.model,systemInstruction:SYSTEM_INSTRUCTION,userText:requestText(context,{hasImage:Boolean(imageUrl)}),imageUrl}}
function awaitNativeGemini(native,payload,{timeoutMs=135000}={}){
  if(!native?.geminiGenerateTextAsync){
    if(!native?.geminiGenerateText)return Promise.resolve({status:'UNAVAILABLE'});
    return Promise.resolve(parseNative(native.geminiGenerateText(JSON.stringify(payload))));
  }
  return new Promise(resolve=>{
    let timer=null,requestId=null,done=false;
    const finish=result=>{if(done)return;done=true;if(timer)clearTimeout(timer);window.removeEventListener('braseiro-gemini',onResult);resolve(result)};
    const onResult=e=>{const detail=e?.detail??{};if(requestId&&detail.requestId===requestId)finish(detail)};
    window.addEventListener('braseiro-gemini',onResult);
    const start=parseNative(native.geminiGenerateTextAsync(JSON.stringify(payload)));
    if(start.status!=='PENDING'||!start.requestId){finish(start);return;}
    requestId=start.requestId;
    timer=setTimeout(()=>finish({status:'TIMEOUT',reason:'Gemini demorou mais que o limite de resposta.'}),timeoutMs);
  });
}

export class RuntimeBarbaraProvider{
  constructor({native=globalThis.BraseiroNative,fallback=new DeterministicNarrativeProvider(),ragSearch=undefined,ragSearchAsync=undefined}={}){this.native=native;this.fallback=fallback;this.ragSearch=ragSearch===undefined?defaultRagSearch(native):ragSearch;this.ragSearchAsync=ragSearchAsync===undefined?defaultRagSearchAsync(native):ragSearchAsync;}
  status(){return safeStatus(this.native)}
  localFallback({mechanicalEvent,state,ragSnippets=[]}){
    const scene=mechanicalEvent?.scene;
    if(mechanicalEvent?.kind==='SCENE_OPENED'&&scene){
      const visible=[scene.summary,scene.situation,scene.premise].map(v=>String(v??'').trim()).find(Boolean);
      if(visible)return Object.freeze({status:'OK',provider:'LOCAL_SCENE',parentMechanicalEventId:mechanicalEvent.eventId,mechanicalRevision:mechanicalEvent.revision,text:visible});
    }
    return this.fallback.generate({mechanicalEvent,state,ragSnippets});
  }
  _context({mechanicalEvent,state,ragSnippets=[]}){
    let snippets=[...(ragSnippets??[])];
    if(!snippets.length&&typeof this.ragSearch==='function'){
      try{const q=[mechanicalEvent?.kind,mechanicalEvent?.scene?.title,mechanicalEvent?.scene?.summary].filter(Boolean).join(' ');if(q.trim())snippets=this.ragSearch(q)??[]}catch{}
    }
    return {snippets,context:buildFilteredNarrativeContext({mechanicalEvent,state,ragSnippets:snippets})};
  }
  async _contextAsync({mechanicalEvent,state,ragSnippets=[]}){
    let snippets=[...(ragSnippets??[])];
    const q=[mechanicalEvent?.kind,mechanicalEvent?.scene?.title,mechanicalEvent?.scene?.summary,mechanicalEvent?.declaration].filter(Boolean).join(' ');
    if(!snippets.length&&q.trim()&&typeof this.ragSearchAsync==='function'){
      try{snippets=await this.ragSearchAsync(q)??[]}catch{}
    }else if(!snippets.length&&q.trim()&&typeof this.ragSearch==='function'){
      try{snippets=this.ragSearch(q)??[]}catch{}
    }
    return {snippets,context:buildFilteredNarrativeContext({mechanicalEvent,state,ragSnippets:snippets})};
  }
  _image(status,state,mechanicalEvent){return status.visionEnabled===false?null:(state?.encounterState?.currentScene?.imageRef??state?.encounterState?.currentScene?.imageUrl??mechanicalEvent?.scene?.imageRef??mechanicalEvent?.scene?.imageUrl??null)}
  generate({mechanicalEvent,state,ragSnippets=[]}){
    const status=this.status();
    // On current Android builds Gemini and RAG both have asynchronous bridges. Synchronous
    // mechanical services must return immediately: do not run even a synchronous RAG lookup
    // here, because a large local manual index can stall the WebView before narration recovery.
    if(this.native?.geminiGenerateTextAsync&&status.status==='OK'&&status.configured===true){
      return Object.freeze({...this.localFallback({mechanicalEvent,state,ragSnippets}),geminiDeferred:true});
    }
    const {snippets,context}=this._context({mechanicalEvent,state,ragSnippets});
    if(!this.native?.geminiGenerateText||status.status!=='OK'||status.configured!==true)return this.localFallback({mechanicalEvent,state,ragSnippets:snippets});
    try{
      const imageUrl=this._image(status,state,mechanicalEvent);
      const result=parseNative(this.native.geminiGenerateText(JSON.stringify(requestPayload(status,context,imageUrl))));
      if(result.status!=='OK'||!String(result.text??'').trim())return Object.freeze({...this.localFallback({mechanicalEvent,state,ragSnippets:snippets}),geminiFallbackReason:result.reason??result.status??'EMPTY'});
      return Object.freeze({status:'OK',provider:'GEMINI_NATIVE',model:result.model,imageAttached:result.imageAttached===true,parentMechanicalEventId:mechanicalEvent.eventId,mechanicalRevision:mechanicalEvent.revision,text:String(result.text).trim()});
    }catch(error){return Object.freeze({...this.localFallback({mechanicalEvent,state,ragSnippets:snippets}),geminiFallbackReason:String(error?.message??error)});}
  }
  async generateAsync({mechanicalEvent,state,ragSnippets=[]}){
    const {snippets,context}=await this._contextAsync({mechanicalEvent,state,ragSnippets});
    const status=this.status();
    if(status.status!=='OK'||status.configured!==true||(!this.native?.geminiGenerateTextAsync&&!this.native?.geminiGenerateText))return this.localFallback({mechanicalEvent,state,ragSnippets:snippets});
    try{
      const imageUrl=this._image(status,state,mechanicalEvent);
      const result=await awaitNativeGemini(this.native,requestPayload(status,context,imageUrl));
      if(result.status!=='OK'||!String(result.text??'').trim())return Object.freeze({...this.localFallback({mechanicalEvent,state,ragSnippets:snippets}),geminiFallbackReason:result.reason??result.status??'EMPTY'});
      return Object.freeze({status:'OK',provider:'GEMINI_NATIVE',model:result.model,imageAttached:result.imageAttached===true,parentMechanicalEventId:mechanicalEvent.eventId,mechanicalRevision:mechanicalEvent.revision,text:String(result.text).trim()});
    }catch(error){return Object.freeze({...this.localFallback({mechanicalEvent,state,ragSnippets:snippets}),geminiFallbackReason:String(error?.message??error)});}
  }
}

export function createRuntimeNarrativeProvider(options={}){return new RuntimeBarbaraProvider(options)}
export function geminiRuntimeStatus(native=globalThis.BraseiroNative){return safeStatus(native)}
