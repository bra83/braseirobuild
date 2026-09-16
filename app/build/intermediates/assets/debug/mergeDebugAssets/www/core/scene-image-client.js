function parseNative(value){try{return typeof value==='string'?JSON.parse(value):value}catch{return {status:'ERROR',reason:'INVALID_NATIVE_JSON'}}}
export class SceneImageClient{
  constructor({native=globalThis.BraseiroNative,eventTarget=globalThis}={}){this.native=native;this.eventTarget=eventTarget;}
  requestImport(){if(!this.native?.openSceneImage)return{status:'UNAVAILABLE'};return parseNative(this.native.openSceneImage());}
  onImport(listener){if(typeof listener!=='function')throw new TypeError('listener required');const handler=event=>{const d=event?.detail;if(d?.kind!=='SCENE_IMAGE_IMPORT')return;let result={status:d.status};if(d.text){const parsed=parseNative(d.text);result={...result,...parsed};}if(d.reason)result.reason=d.reason;listener(Object.freeze(result));};this.eventTarget?.addEventListener?.('braseiro-native-file',handler);return()=>this.eventTarget?.removeEventListener?.('braseiro-native-file',handler);}
}
export function createSceneImageClient(options={}){return new SceneImageClient(options)}
