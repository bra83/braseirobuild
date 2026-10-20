// Braseiro TTS v3 — adapter local real: fala limpa, fila cancelável e sem instruções.
let playbackGeneration = 0;
export function normalizeTtsRate(value) {
  const n = Number(value);
  return Number.isFinite(n) ? Math.max(0.6, Math.min(1.4, n)) : 1;
}
export function ttsAvailable({speech=globalThis.speechSynthesis,Utterance=globalThis.SpeechSynthesisUtterance}={}) {
  return Boolean(speech && typeof speech.speak==='function' && typeof speech.cancel==='function' && typeof Utterance==='function');
}
export function splitNarrationBlocks(text,maxChars=600) {
  const source=String(text??'').normalize('NFC').replace(/\s+/g,' ').trim();
  const out=[];let offset=0;const max=Math.max(180,Number(maxChars)||600);
  while(offset<source.length){
    while(offset<source.length && /\s/.test(source[offset]))offset++;
    if(offset>=source.length)break;
    if(source.length-offset<=max){out.push(source.slice(offset));break}
    const ceiling=Math.min(source.length,offset+max);
    const floor=Math.min(ceiling,offset+Math.floor(max*.55));
    let cut=-1;
    for(let i=ceiling;i>=floor;i--){if(/[.!?;:,]/.test(source[i-1]||'') && /\s/.test(source[i]||'')){cut=i;break}}
    if(cut<0)for(let i=ceiling;i>=floor;i--){if(/\s/.test(source[i]||'')){cut=i;break}}
    if(cut<0){cut=ceiling;while(cut<source.length && !/\s/.test(source[cut]))cut++}
    if(cut<=offset)throw new Error('Falha ao dividir a narração sem cortar palavras.');
    out.push(source.slice(offset,cut).trim());offset=cut;
  }
  return out.filter(Boolean);
}
export function speakNarration(text,{speech=globalThis.speechSynthesis,Utterance=globalThis.SpeechSynthesisUtterance,rate=1,lang='pt-BR'}={}) {
  const content=String(text??'').trim();
  if(!content)throw new Error('Não há narração para ler.');
  if(!ttsAvailable({speech,Utterance}))throw new Error('TTS não está disponível neste navegador/WebView.');
  const generation=++playbackGeneration;
  speech.cancel();
  const blocks=splitNarrationBlocks(content),finalRate=normalizeTtsRate(rate);
  const play=index=>{
    if(generation!==playbackGeneration || index>=blocks.length)return;
    const utterance=new Utterance(blocks[index]);
    utterance.lang=lang;utterance.rate=finalRate;
    utterance.onend=()=>{if(generation===playbackGeneration)play(index+1)};
    utterance.onerror=e=>{
      if(generation!==playbackGeneration)return;
      if(['canceled','interrupted'].includes(e?.error))return;
      // Falhas de um bloco não disparam recursão infinita nem repetição do anterior.
      if(index+1<blocks.length)play(index+1);
    };
    if(generation===playbackGeneration)speech.speak(utterance);
  };
  play(0);
  return {text:content,lang,rate:finalRate,chunks:blocks.length};
}
export function stopNarration({speech=globalThis.speechSynthesis}={}) {
  ++playbackGeneration;
  if(!speech || typeof speech.cancel!=='function')return false;
  speech.cancel();return true;
}
