/** Braseiro TTS Mestre Único — integration candidate, not installed until OUA gateway wired. */
export const DEFAULT_TTS = Object.freeze({
  model:'gemini-3.8-flash-tts',fallbackModel:'gemini-3.8-flash-lite-tts',
  voice:'Charon',profile:'Voz madura, calorosa, natural, PT-BR.',
  style:'natural, warm, conversational, immersive', modulation:25,
  mode:'single-narrator',allowFallback:true,
});
const moods={calma:'warm and unhurried',medo:'hesitant, tense, lower volume',
  suspense:'hushed, measured, suspenseful',raiva:'firm, controlled anger',
  alegre:'gentle smile, lively',triste:'subdued, delicate',
  urgente:'focused, brisk',misterioso:'soft, intriguing'};
const ids={narrator:'neutral narrator voice',male:'slightly lower register',
  female:'slightly lighter register',elder:'slower, weathered texture',
  child:'lively, slightly brighter register'};
export function prepareSpeech(text,{settings=DEFAULT_TTS,mood='calma',role='narrator'}={}){
  const clean=String(text||'').normalize('NFC').trim();
  if(!clean)throw Error('Empty narration');
  if(/^\s*(sintetize fala|perfil vocal\s*:|leia exatamente|narre como)/i.test(clean))
    throw Error('Instruction leaked into narration. Put vocal direction in metadata.');
  const emotional=moods[mood]||moods.calma;
  const characterization=ids[role]||ids.narrator;
  const power=Math.max(0,Math.min(100,Number(settings.modulation??25)));
  const voice=settings.voice||'Charon';
  return {
    model:settings.model||DEFAULT_TTS.model,
    voice, text:clean,
    style:[settings.style||DEFAULT_TTS.style,emotional,characterization,
      'single persistent narrator identity, subtly distinguish dialogue voices',
      'subtle character distinction intensity '+power+' percent'].join('; '),
  };
}
export function fallbackModels(settings=DEFAULT_TTS){
  const ordered=[settings.model||DEFAULT_TTS.model];
  if(settings.allowFallback!==false && settings.fallbackModel && !ordered.includes(settings.fallbackModel))
    ordered.push(settings.fallbackModel);
  return ordered;
}
export function isQuotaOrTransient(error){
  const status=Number(error?.status||error?.code||0);
  return [429,500,502,503,504].includes(status)||/RESOURCE_EXHAUSTED|quota|rate.?limit|temporar/i.test(String(error?.message||''));
}
export class MestreUnicoTts{
  constructor({synthesize,play,settings={}}){
    if(typeof synthesize!=='function'||typeof play!=='function')throw Error('Adapter required');
    Object.assign(this,{synthesize,play,settings:{...DEFAULT_TTS,...settings},generation:0});
  }
  stop(){this.generation++;}
  async narrate(text,ctx={}){
    const generation=++this.generation;
    const data=prepareSpeech(text,{...ctx,settings:this.settings});
    let lastError;
    for(const model of fallbackModels(this.settings)){
      if(generation!==this.generation)return {cancelled:true};
      try{
        const audio=await this.synthesize({...data,model});
        if(generation!==this.generation)return {cancelled:true};
        await this.play(audio);
        return {model,voice:data.voice};
      }catch(e){lastError=e;if(!isQuotaOrTransient(e))break;}
    }
    throw lastError||Error('TTS unavailable');
  }
}
