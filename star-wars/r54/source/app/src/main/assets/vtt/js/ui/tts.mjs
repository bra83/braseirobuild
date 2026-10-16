export function normalizeTtsRate(value){
  const n=Number(value);
  if(!Number.isFinite(n)) return 1;
  return Math.max(0.6,Math.min(1.4,n));
}

export function ttsAvailable({speech=globalThis.speechSynthesis,Utterance=globalThis.SpeechSynthesisUtterance}={}){
  return Boolean(speech && typeof speech.speak==='function' && typeof speech.cancel==='function' && typeof Utterance==='function');
}

export function splitNarrationBlocks(text,maxChars=1800){
  let rest=String(text??'').replace(/\s+/g,' ').trim(); const out=[]; const max=Math.max(400,Number(maxChars)||1800);
  while(rest.length>max){let cut=-1;for(const mark of ['. ','! ','? ','; ',': ',', ',' ']){const at=rest.lastIndexOf(mark,max);if(at>=Math.max(180,max/2)){cut=at+mark.length;break}}if(cut<1)cut=max;out.push(rest.slice(0,cut).trim());rest=rest.slice(cut).trim()}
  if(rest)out.push(rest);return out;
}

export function speakNarration(text,{speech=globalThis.speechSynthesis,Utterance=globalThis.SpeechSynthesisUtterance,rate=1,lang='pt-BR'}={}){
  const content=String(text??'').trim();
  if(!content) throw new Error('Não há narração para ler.');
  if(!ttsAvailable({speech,Utterance})) throw new Error('TTS não está disponível neste navegador/WebView.');
  speech.cancel(); const blocks=splitNarrationBlocks(content); const finalRate=normalizeTtsRate(rate);
  const play=index=>{if(index>=blocks.length)return;const u=new Utterance(blocks[index]);u.lang=lang;u.rate=finalRate;u.onend=()=>play(index+1);u.onerror=()=>play(index+1);speech.speak(u)}; play(0);
  return {text:content,lang,rate:finalRate,chunks:blocks.length};
}

export function stopNarration({speech=globalThis.speechSynthesis}={}){
  if(!speech || typeof speech.cancel!=='function') return false;
  speech.cancel(); return true;
}
