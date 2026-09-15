function parse(value){try{return typeof value==='string'?JSON.parse(value):value}catch{return {status:'ERROR',reason:'INVALID_NATIVE_JSON'}}}
export class RagClient{
  constructor({native=globalThis.BraseiroNative}={}){this.native=native}
  status(){return this.native?.ragStatus?parse(this.native.ragStatus()):{status:'UNAVAILABLE',documentCount:0}}
  list(){return this.native?.listRagDocuments?parse(this.native.listRagDocuments()):{status:'UNAVAILABLE',documents:[]}}
  importPdf(authorityProfile='WWN_RULEBOOK_AUTHORIZED'){if(!this.native?.openRulesPdf)return{status:'UNAVAILABLE'};return parse(this.native.openRulesPdf(String(authorityProfile)))}
  remove(documentId){if(!this.native?.removeRagDocument)return{status:'UNAVAILABLE'};return parse(this.native.removeRagDocument(String(documentId)))}
  search(query,{limit=5,scope='RULES'}={}){if(!this.native?.ragSearch)return{status:'UNAVAILABLE',results:[]};return parse(this.native.ragSearch(JSON.stringify({query:String(query??''),limit,scope})))}
  searchAsync(query,{limit=5,scope='RULES',timeoutMs=45000}={}){if(!this.native?.ragSearchAsync)return Promise.resolve(this.search(query,{limit,scope}));return new Promise(resolve=>{let requestId=null,done=false,timer=null;const finish=r=>{if(done)return;done=true;if(timer)clearTimeout(timer);window.removeEventListener('braseiro-rag',onResult);resolve(r)};const onResult=e=>{const d=e?.detail??{};if(requestId&&d.requestId===requestId)finish(d)};window.addEventListener('braseiro-rag',onResult);const start=parse(this.native.ragSearchAsync(JSON.stringify({query:String(query??''),limit,scope})));if(start.status!=='PENDING'||!start.requestId){finish(start);return;}requestId=start.requestId;timer=setTimeout(()=>finish({status:'TIMEOUT',reason:'A consulta aos manuais excedeu o tempo limite.',results:[]}),timeoutMs);});}
  narrativeSearch(query,{limit=4}={}){const r=this.search(query,{limit,scope:'NARRATIVE_PUBLIC'});return r.status==='OK'?(r.results??[]):[]}
  async narrativeSearchAsync(query,{limit=4}={}){const r=await this.searchAsync(query,{limit,scope:'NARRATIVE_PUBLIC'});return r.status==='OK'?(r.results??[]):[]}
}
export function createNativeRagSearch(native=globalThis.BraseiroNative){if(!native?.ragSearch)return null;const c=new RagClient({native});return async query=>c.narrativeSearch(query,{limit:4})}
