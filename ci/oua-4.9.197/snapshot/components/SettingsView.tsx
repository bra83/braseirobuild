import React, { useEffect, useState } from 'react';
import { CheckCircle, DatabaseBackup, Image, KeyRound, MoonStar, Settings, Sparkles, Volume2 } from 'lucide-react';
import { getBrowserGeminiApiKey, hasGeminiApiKey, saveGeminiApiKey } from '../lib/apiKey';
import { APP_VERSION, APP_VERSION_CODE } from '../lib/version';
import {
  GEMINI_TEXT_MODEL_OPTIONS,
  GEMINI_TTS_MODEL_OPTIONS,
  getGeminiTextModel,
  getGeminiTtsModel,
  resetGeminiModels,
  saveGeminiTextModel,
  saveGeminiTtsModel,
} from '../lib/geminiModels';
import { generateGemini, responseText } from '../lib/geminiTransport';
import { HealthCenter } from './HealthCenter';
import type { HealthReport } from '../lib/autonomy/types';

interface SettingsViewProps {
  atmosphereActive: boolean;
  onAtmosphereChange: (active: boolean) => void;
  imageQuality: 'fast' | 'quality';
  onImageQualityChange: (quality: 'fast' | 'quality') => void;
  healthReport: HealthReport | null;
  isHealthChecking: boolean;
  onRunHealthCheck: (repair: boolean) => Promise<void>;
  onOpenBackup: () => void;
}

export const SettingsView: React.FC<SettingsViewProps> = ({
  atmosphereActive,
  onAtmosphereChange,
  imageQuality,
  onImageQualityChange,
  healthReport,
  isHealthChecking,
  onRunHealthCheck,
  onOpenBackup,
}) => {
  const [freeKey, setFreeKey] = useState(() => window.BraseiroAndroid ? '' : getBrowserGeminiApiKey('free'));
  const [paidKey, setPaidKey] = useState(() => window.BraseiroAndroid ? '' : getBrowserGeminiApiKey('paid'));
  const [freeConfigured, setFreeConfigured] = useState(() => hasGeminiApiKey('free'));
  const [paidConfigured, setPaidConfigured] = useState(() => hasGeminiApiKey('paid'));
  const [textModel, setTextModel] = useState(() => getGeminiTextModel());
  const [ttsModel, setTtsModel] = useState(() => getGeminiTtsModel());
  const [status, setStatus] = useState('');
  const [testing, setTesting] = useState<'text' | 'tts' | null>(null);
  const [narrationBackend,setNarrationBackend]=useState<'charon'|'kokoro'>(()=>localStorage.getItem('braseiro.narrationVoiceBackend')==='kokoro'?'kokoro':'charon');
  const changeNarrationBackend=(value:'charon'|'kokoro')=>{setNarrationBackend(value);localStorage.setItem('braseiro.narrationVoiceBackend',value);setAndroidTtsStatus(value==='kokoro'?'Narração principal: Kokoro / TTS local.':'Narração principal: Charon / Gemini TTS.');};
  const [androidTtsCatalog,setAndroidTtsCatalog]=useState<any>(null);
  const [androidTtsStatus,setAndroidTtsStatus]=useState('');
  const refreshAndroidTts=async()=>{if(!window.BraseiroAndroid?.getTtsCatalog)return;try{setAndroidTtsCatalog(await window.BraseiroAndroid.getTtsCatalog())}catch(e){setAndroidTtsStatus(e instanceof Error?e.message:String(e))}};
  useEffect(()=>{void refreshAndroidTts();const f=()=>void refreshAndroidTts();window.addEventListener('braseiro-native-ready',f);return()=>window.removeEventListener('braseiro-native-ready',f)},[]);
  const changeAndroidTtsEngine=async(pkg:string)=>{if(!window.BraseiroAndroid?.selectTtsEngine)return;setAndroidTtsStatus('Carregando motor TTS…');try{const c=await window.BraseiroAndroid.selectTtsEngine(pkg);setAndroidTtsCatalog(c);setAndroidTtsStatus(c?.neuralProfile?'TTS local neural / Kokoro carregado e pré-aquecendo.':'Motor Android selecionado.')}catch(e){setAndroidTtsStatus(e instanceof Error?e.message:String(e))}};
  const changeAndroidTtsVoice=async(name:string)=>{if(!window.BraseiroAndroid?.selectTtsVoice)return;setAndroidTtsStatus('Selecionando voz real do motor…');try{const c=await window.BraseiroAndroid.selectTtsVoice(name);setAndroidTtsCatalog(c);const v=(c?.voices||[]).find((x:any)=>x.name===c?.selectedVoice);setAndroidTtsStatus(v?'Voz ativa: '+(v.label||v.name):'Voz automática do motor ativa.')}catch(e){setAndroidTtsStatus(e instanceof Error?e.message:String(e))}};
  const changeNeuralRate=async(rate:number)=>{if(window.BraseiroAndroid?.setNeuralTtsRate)setAndroidTtsCatalog(await window.BraseiroAndroid.setNeuralTtsRate(rate))};
  const testAndroidTts=async()=>{if(!window.BraseiroAndroid?.speakText){setAndroidTtsStatus('TTS Android indisponível.');return}setAndroidTtsStatus('Testando voz local real…');try{await window.BraseiroAndroid.speakText('A porta range. Há marcas no chão. Algo se move no corredor. A chama vacila e o vento toca a madeira, mas ninguém ainda vê quem está além da curva.',androidTtsCatalog?.neuralProfile?Number(androidTtsCatalog?.neuralRate||1):0.96);setAndroidTtsStatus('Teste concluído até o fim.')}catch(e){setAndroidTtsStatus(e instanceof Error?e.message:String(e))}};

  const saveAll = () => {
    if (freeKey.trim()) { saveGeminiApiKey('free', freeKey); setFreeConfigured(true); setFreeKey(''); }
    if (paidKey.trim()) { saveGeminiApiKey('paid', paidKey); setPaidConfigured(true); setPaidKey(''); }
    saveGeminiTextModel(textModel);
    saveGeminiTtsModel(ttsModel);
    setStatus('Configuração salva. O próximo turno e a próxima narração já usam estes modelos.');
  };

  const testTextModel = async () => {
    if (!hasGeminiApiKey()) { setStatus('Salve ao menos uma chave Gemini antes do teste.'); return; }
    setTesting('text');
    setStatus('Testando modelo de texto principal…');
    try {
      const response = await generateGemini(textModel.trim(), [{ parts: [{ text: 'Responda apenas com OK.' }] }], { temperature: 0, maxOutputTokens: 24 });
      const text = responseText(response).trim();
      setStatus(text ? 'Texto OK — ' + textModel + ' respondeu: ' + text.slice(0, 80) : 'O modelo respondeu sem texto.');
    } catch (error) {
      setStatus('Falha no modelo de texto ' + textModel + ': ' + (error instanceof Error ? error.message : String(error)));
    } finally {
      setTesting(null);
    }
  };

  const testTtsModel = async () => {
    if (!hasGeminiApiKey()) { setStatus('Salve ao menos uma chave Gemini antes do teste.'); return; }
    setTesting('tts');
    setStatus('Testando modelo TTS…');
    try {
      const model = ttsModel.trim();
      if (window.BraseiroAndroid?.generateTts) {
        const artifact = await window.BraseiroAndroid.generateTts(model, 'Teste completo da voz Charon no Braseiro.', 'Charon', 'natural', false);
        if (!artifact?.token) throw new Error('O Android não devolveu artefato de áudio.');
        if (!window.BraseiroAndroid.playGeneratedVoice) throw new Error('A ponte de reprodução da voz Gemini não está disponível.');
        await window.BraseiroAndroid.playGeneratedVoice(artifact.token, 1);
      } else {
        const response = await generateGemini(model, [{ parts: [{ text: 'Leia exatamente: Teste de voz do Braseiro.' }] }], {
          responseModalities: ['AUDIO'],
          speechConfig: { languageCode: 'pt-BR', voiceConfig: { prebuiltVoiceConfig: { voiceName: 'Charon' } } },
        });
        const hasAudio = Boolean(response.candidates?.[0]?.content?.parts?.some(part => part.inlineData?.data));
        if (!hasAudio) throw new Error('O modelo não devolveu áudio.');
      }
      setStatus('TTS OK — ' + model + ' gerou e reproduziu a voz Charon até o fim.');
    } catch (error) {
      setStatus('Falha no TTS ' + ttsModel + ': ' + (error instanceof Error ? error.message : String(error)));
    } finally {
      setTesting(null);
    }
  };

  return (
    <div className="mx-auto grid w-full max-w-5xl gap-5 pb-24 lg:grid-cols-[360px_1fr]">
      <section className="medieval-panel p-5">
        <div className="mb-5 flex items-center gap-2 border-b border-blue-900/60 pb-3">
          <Settings className="h-5 w-5 text-blue-300" />
          <h2 className="medieval-title text-lg">Configurações da crônica</h2>
        </div>

        <div className="space-y-4 text-sm">
          <div className="rounded border border-blue-900/60 bg-slate-950/65 p-4">
            <span className="flex items-center gap-2 font-bold text-blue-200"><KeyRound className="h-4 w-4" /> Gemini — chaves e modelos</span>
            <p className="mt-1 text-xs leading-relaxed text-slate-400">Os nomes dos modelos são editáveis manualmente. Você pode usar um ID novo sem esperar atualização do app. No Android, as chaves continuam no cofre nativo.</p>

            <label className="mt-3 block text-xs font-bold text-cyan-200">Chave gratuita {freeConfigured ? '• configurada' : ''}</label>
            <input type="password" value={freeKey} onChange={event => setFreeKey(event.target.value)} placeholder={freeConfigured ? 'Digite outra chave para substituir' : 'Cole a chave gratuita'} className="mt-1 min-h-12 w-full rounded border border-blue-900 bg-[#030712] px-3 text-base text-blue-50" />

            <label className="mt-3 block text-xs font-bold text-amber-200">Chave paga {paidConfigured ? '• configurada' : ''}</label>
            <input type="password" value={paidKey} onChange={event => setPaidKey(event.target.value)} placeholder={paidConfigured ? 'Digite outra chave para substituir' : 'Cole a chave paga (fallback)'} className="mt-1 min-h-12 w-full rounded border border-blue-900 bg-[#030712] px-3 text-base text-blue-50" />
<label className="mt-3 block text-xs font-bold text-amber-200">Modelo do Mestre — texto — ID manual</label>
            <input list="gemini-text-models" value={textModel} onChange={event => setTextModel(event.target.value)} className="mt-1 min-h-12 w-full rounded border border-blue-900 bg-[#030712] px-3 text-sm text-blue-50" />

            <datalist id="gemini-text-models">
              {GEMINI_TEXT_MODEL_OPTIONS.map(model => <option key={model.id} value={model.id}>{model.label}</option>)}
            </datalist>

            <button type="button" disabled={Boolean(testing)} onClick={() => void testTextModel()} className="mt-2 min-h-11 w-full rounded border border-cyan-700 bg-cyan-950/30 text-xs font-bold text-cyan-100 disabled:opacity-50">
              {testing === 'text' ? 'Testando texto…' : 'Testar modelo de texto principal'}
            </button>

            <label className="mt-4 block text-xs font-bold text-violet-200">TTS — ID manual</label>
            <input list="gemini-tts-models" value={ttsModel} onChange={event => setTtsModel(event.target.value)} className="mt-1 min-h-12 w-full rounded border border-violet-900 bg-[#030712] px-3 text-sm text-violet-50" />
            <datalist id="gemini-tts-models">
              {GEMINI_TTS_MODEL_OPTIONS.map(model => <option key={model.id} value={model.id}>{model.label}</option>)}
            </datalist>

            <button type="button" disabled={Boolean(testing)} onClick={() => void testTtsModel()} className="mt-2 min-h-11 w-full rounded border border-violet-700 bg-violet-950/30 text-xs font-bold text-violet-100 disabled:opacity-50">
              {testing === 'tts' ? 'Testando TTS…' : 'Testar modelo TTS com Charon'}
            </button>

            <button type="button" onClick={saveAll} className="mt-3 min-h-12 w-full rounded border border-blue-500 bg-blue-950/60 font-bold text-blue-100">Salvar chaves e modelos</button>
            <button type="button" onClick={() => {
              resetGeminiModels();
              setTextModel(getGeminiTextModel());
              setTtsModel(getGeminiTtsModel());
              setStatus('Modelos restaurados para os padrões do app. As chaves não foram apagadas.');
            }} className="mt-2 min-h-10 w-full rounded border border-slate-700 bg-slate-950 text-xs font-bold text-slate-300">Restaurar modelos padrão</button>

            {status && <p className="mt-3 rounded border border-slate-800 bg-black/25 px-3 py-2 text-xs leading-relaxed text-slate-300">{status}</p>}
          </div>

          <div className="rounded border border-violet-900/60 bg-violet-950/10 p-4">
            <span className="flex items-center gap-2 font-bold text-violet-200"><Volume2 className="h-4 w-4" /> TTS Android / Kokoro</span>
            <p className="mt-1 text-xs text-slate-400">Motores e vozes vêm do Android. Braseiro Kokoro PT-BR usa o motor Sherpa/DB Next-gen instalado e suas vozes reais; Samsung/Google seguem o fluxo Android normal.</p>            <label className="mt-3 block text-xs font-bold text-violet-100">Preferência de narração</label>
            <select value={narrationBackend} onChange={e=>changeNarrationBackend(e.target.value as 'charon'|'kokoro')} className="mt-1 min-h-11 w-full rounded border border-violet-900 bg-[#030712] px-2 text-sm text-violet-50">
              <option value="charon">Charon · Gemini TTS</option>
              <option value="kokoro">Kokoro · TTS local selecionado</option>
            </select>
            <p className="mt-1 text-[11px] text-slate-500">{narrationBackend==='kokoro'?'O botão Ouvir narração usará o motor local selecionado abaixo.':'O botão Ouvir narração usará Charon; o Kokoro continua disponível para teste local.'}</p>
            <label className="mt-3 block text-xs font-bold text-violet-100">Motor instalado</label>
            <select value={androidTtsCatalog?.selectedEnginePackage||''} onChange={e=>void changeAndroidTtsEngine(e.target.value)} className="mt-1 min-h-11 w-full rounded border border-violet-900 bg-[#030712] px-2 text-sm text-violet-50">{(androidTtsCatalog?.engines||[{packageName:'',label:'Padrão do Android'}]).map((x:any)=><option key={x.packageName||'__default__'} value={x.packageName}>{x.label}{x.neuralProfile?' · neural':''}</option>)}</select>
            <label className="mt-3 block text-xs font-bold text-violet-100">Voz do motor selecionado</label>
            <select value={androidTtsCatalog?.selectedVoice||''} onChange={e=>void changeAndroidTtsVoice(e.target.value)} className="mt-1 min-h-11 w-full rounded border border-violet-900 bg-[#030712] px-2 text-sm text-violet-50"><option value="">Automática / padrão do motor</option>{(androidTtsCatalog?.voices||[]).map((v:any)=><option key={v.name} value={v.name}>{v.label||[v.name,v.locale,v.networkRequired?'rede':'local'].filter(Boolean).join(' · ')}</option>)}</select>
            {androidTtsCatalog?.neuralProfile&&<div className="mt-3 rounded border border-cyan-900/70 bg-cyan-950/20 p-3"><div className="font-bold text-cyan-200">TTS local neural / Kokoro</div><p className="mt-1 text-[11px] text-slate-400">Blocos ~450 caracteres (250–650), maxNumSentences 2, silenceScale 0,06, 4 threads; próximo bloco somente após onDone real.</p><label className="mt-3 block text-xs font-bold text-cyan-100">Velocidade Kokoro: {Number(androidTtsCatalog?.neuralRate||1).toFixed(2)}×</label><input type="range" min="0.65" max="1.5" step="0.05" value={Number(androidTtsCatalog?.neuralRate||1)} onChange={e=>void changeNeuralRate(Number(e.target.value))} className="mt-1 w-full"/><div className="mt-1 text-[10px] text-slate-500">1.00× = velocidade neutra da engine; sem multiplicação dupla.</div></div>}
            <div className="mt-3 grid grid-cols-2 gap-2"><button type="button" onClick={()=>void refreshAndroidTts()} className="min-h-10 rounded border border-slate-700 bg-slate-950 text-xs font-bold text-slate-300">Atualizar lista</button><button type="button" onClick={()=>void testAndroidTts()} className="min-h-10 rounded border border-violet-700 bg-violet-950/40 text-xs font-bold text-violet-100">Testar voz local</button></div>
            {androidTtsStatus&&<p className="mt-2 text-xs text-slate-300">{androidTtsStatus}</p>}
          </div>

          <label className="block rounded border border-blue-900/60 bg-slate-950/65 p-4">
            <span className="flex items-center gap-2 font-bold text-blue-200"><MoonStar className="h-4 w-4" /> Atmosfera visual</span>
            <span className="mt-1 block text-xs leading-relaxed text-slate-400">Mantém o tema medieval noturno em preto, azul profundo e prata.</span>
            <span className="mt-3 flex items-center gap-2">
              <input type="checkbox" checked={atmosphereActive} onChange={event => onAtmosphereChange(event.target.checked)} className="h-5 w-5 accent-blue-500" />
              Névoa e textura de Eriador
            </span>
          </label>

          <div className="rounded border border-blue-900/60 bg-slate-950/65 p-4">
            <span className="flex items-center gap-2 font-bold text-blue-200"><Image className="h-4 w-4" /> Imagens sob demanda</span>
            <p className="mt-1 text-xs leading-relaxed text-slate-400">Nenhuma imagem é gerada automaticamente. O modelo só é usado quando você pede a ilustração.</p>
            <div className="mt-3 grid grid-cols-2 gap-2">
              <button type="button" onClick={() => onImageQualityChange('fast')} className={`min-h-12 rounded border px-3 text-xs font-bold ${imageQuality === 'fast' ? 'border-cyan-400 bg-cyan-950/50 text-cyan-100' : 'border-slate-700 bg-slate-950 text-slate-400'}`}>Rápida</button>
              <button type="button" onClick={() => onImageQualityChange('quality')} className={`min-h-12 rounded border px-3 text-xs font-bold ${imageQuality === 'quality' ? 'border-blue-400 bg-blue-950/60 text-blue-100' : 'border-slate-700 bg-slate-950 text-slate-400'}`}>Qualidade</button>
            </div>
          </div>

          <div className="rounded border border-emerald-900/60 bg-emerald-950/15 p-4">
            <span className="flex items-center gap-2 font-bold text-emerald-200"><DatabaseBackup className="h-4 w-4" /> Backup e restauração</span>
            <p className="mt-1 text-xs leading-relaxed text-slate-400">Exporta e restaura campanha, configuração e tokens sem apagar silenciosamente o estado atual.</p>
            <button type="button" onClick={onOpenBackup} className="mt-3 min-h-12 w-full rounded border border-emerald-600 bg-emerald-950/40 font-bold text-emerald-100">Abrir cofre da campanha</button>
          </div>
        </div>
      </section>

      <section className="medieval-panel p-5">
        <div className="mb-5 flex items-center gap-2 border-b border-blue-900/60 pb-3">
          <Sparkles className="h-5 w-5 text-cyan-300" />
          <h2 className="medieval-title text-lg">Diagnóstico do Mestre</h2>
        </div>
        <div className="space-y-4 text-sm leading-relaxed text-slate-300">
          <div className="rounded border border-cyan-900/60 bg-cyan-950/10 p-4">
            <h3 className="flex items-center gap-2 font-bold text-cyan-200"><CheckCircle className="h-4 w-4" /> Aberturas e cenas importantes</h3>
            <p className="mt-1">Todas as cenas usam o mesmo modelo do Mestre. A narração segue o Motor Barbara: cinematográfica, substancial, sensorial e falada como Mestre para os jogadores, sem decidir ações pela Companhia.</p>
          </div>
          <div className="rounded border border-blue-900/60 bg-blue-950/10 p-4">
            <h3 className="flex items-center gap-2 font-bold text-blue-200"><Volume2 className="h-4 w-4" /> TTS</h3>
            <p className="mt-1">O TTS usa primeiro o ID manual configurado acima. Charon continua sendo a voz padrão; o app mantém fallback compatível se o modelo escolhido falhar.</p>
          </div>
          <HealthCenter report={healthReport} isChecking={isHealthChecking} onCheck={onRunHealthCheck} />
          <p className="text-xs text-slate-500">O acesso ao Google Drive permanece no fluxo nativo já configurado para este aplicativo.</p>
        </div>
      </section>

      <footer className="lg:col-span-2 border-t border-blue-950/70 pt-4 text-center text-xs text-slate-500">
        O Um Anel · versão <strong className="text-blue-300">{APP_VERSION}</strong> · código {APP_VERSION_CODE}
      </footer>
    </div>
  );
};
