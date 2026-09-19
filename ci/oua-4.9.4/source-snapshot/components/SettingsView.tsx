import React, { useState } from 'react';
import { CheckCircle, DatabaseBackup, Image, KeyRound, MoonStar, Settings, Sparkles, Volume2 } from 'lucide-react';
import { getBrowserGeminiApiKey, hasGeminiApiKey, saveGeminiApiKey } from '../lib/apiKey';
import { APP_VERSION, APP_VERSION_CODE } from '../lib/version';
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
  const [keySaved, setKeySaved] = useState(false);

  return (
    <div className="mx-auto grid w-full max-w-5xl gap-5 pb-16 lg:grid-cols-[340px_1fr]">
      <section className="medieval-panel p-5">
        <div className="mb-5 flex items-center gap-2 border-b border-blue-900/60 pb-3">
          <Settings className="h-5 w-5 text-blue-300" />
          <h2 className="medieval-title text-lg">Configurações da crônica</h2>
        </div>

        <div className="space-y-4 text-sm">
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
            <p className="mt-1 text-xs leading-relaxed text-slate-400">Nenhuma imagem é gerada automaticamente. O modelo só é usado quando você toca em “Gerar imagem desta cena”.</p>
            <div className="mt-3 grid grid-cols-2 gap-2">
              <button type="button" onClick={() => onImageQualityChange('fast')} className={`min-h-12 rounded border px-3 text-xs font-bold ${imageQuality === 'fast' ? 'border-cyan-400 bg-cyan-950/50 text-cyan-100' : 'border-slate-700 bg-slate-950 text-slate-400'}`}>Rápida e econômica</button>
              <button type="button" onClick={() => onImageQualityChange('quality')} className={`min-h-12 rounded border px-3 text-xs font-bold ${imageQuality === 'quality' ? 'border-blue-400 bg-blue-950/60 text-blue-100' : 'border-slate-700 bg-slate-950 text-slate-400'}`}>Maior qualidade</button>
            </div>
          </div>

          <div className="rounded border border-emerald-900/60 bg-emerald-950/15 p-4">
            <span className="flex items-center gap-2 font-bold text-emerald-200"><DatabaseBackup className="h-4 w-4" /> Backup e restauração</span>
            <p className="mt-1 text-xs leading-relaxed text-slate-400">Abra o Cofre da campanha para exportar um backup JSON com integridade, gerar o pacote completo com tokens ou restaurar sem apagar silenciosamente o estado atual.</p>
            <button type="button" onClick={onOpenBackup} className="mt-3 min-h-12 w-full rounded border border-emerald-600 bg-emerald-950/40 font-bold text-emerald-100">Abrir cofre da campanha</button>
          </div>

          <div className="rounded border border-blue-900/60 bg-slate-950/65 p-4">
            <span className="flex items-center gap-2 font-bold text-blue-200"><KeyRound className="h-4 w-4" /> Chaves Gemini</span>
            <p className="mt-1 text-xs leading-relaxed text-slate-400">O app tenta a gratuita primeiro. Só usa a paga depois de dois erros 429. No Android, as chaves ficam criptografadas e não são entregues à WebView.</p>
            <label className="mt-3 block text-xs font-bold text-cyan-200">Projeto gratuito {freeConfigured ? '• configurado' : ''}</label>
            <input type="password" value={freeKey} onChange={event => { setFreeKey(event.target.value); setKeySaved(false); }} placeholder={freeConfigured ? 'Digite outra chave para substituir' : 'Cole a chave gratuita'} className="mt-1 min-h-12 w-full rounded border border-blue-900 bg-[#030712] px-3 text-base text-blue-50" />
            <label className="mt-3 block text-xs font-bold text-amber-200">Projeto pago {paidConfigured ? '• configurado' : ''}</label>
            <input type="password" value={paidKey} onChange={event => { setPaidKey(event.target.value); setKeySaved(false); }} placeholder={paidConfigured ? 'Digite outra chave para substituir' : 'Cole a chave paga (fallback)'} className="mt-1 min-h-12 w-full rounded border border-blue-900 bg-[#030712] px-3 text-base text-blue-50" />
            <button type="button" onClick={() => {
              if (freeKey.trim()) { saveGeminiApiKey('free', freeKey); setFreeConfigured(true); setFreeKey(''); }
              if (paidKey.trim()) { saveGeminiApiKey('paid', paidKey); setPaidConfigured(true); setPaidKey(''); }
              setKeySaved(true);
            }} className="mt-3 min-h-12 w-full rounded border border-blue-500 bg-blue-950/60 font-bold text-blue-100">
              {keySaved ? 'Chaves salvas neste aparelho' : 'Salvar chaves preenchidas'}
            </button>
          </div>
        </div>
      </section>

      <section className="medieval-panel p-5">
        <div className="mb-5 flex items-center gap-2 border-b border-blue-900/60 pb-3">
          <Sparkles className="h-5 w-5 text-cyan-300" />
          <h2 className="medieval-title text-lg">Como o Braseiro trabalha</h2>
        </div>
        <div className="space-y-4 text-sm leading-relaxed text-slate-300">
          <div className="rounded border border-cyan-900/60 bg-cyan-950/10 p-4">
            <h3 className="flex items-center gap-2 font-bold text-cyan-200"><CheckCircle className="h-4 w-4" /> Regras locais primeiro</h3>
            <p className="mt-1">Turnos, testes, jornada, movimento, combate e descanso procuram trechos nos manuais importados antes de a cena ser escrita. Quando a regra não é encontrada, o Mestre não deve inventá-la.</p>
          </div>
          <div className="rounded border border-blue-900/60 bg-blue-950/10 p-4">
            <h3 className="flex items-center gap-2 font-bold text-blue-200"><Volume2 className="h-4 w-4" /> Som contextual</h3>
            <p className="mt-1">A ambiência acompanha o local atual. Detalhes como lareira, chuva e vento usam canais separados. Efeitos pontuais são escolhidos por palavras, sinônimos e nomes reais dos arquivos enquanto a voz continua tocando.</p>
          </div>
          <div className="rounded border border-indigo-900/60 bg-indigo-950/10 p-4">
            <h3 className="font-bold text-indigo-200">Uso econômico dos modelos</h3>
            <p className="mt-1">Cenas comuns usam o modelo econômico. Momentos decisivos usam o modelo mais forte. Charon é dividido em blocos: o primeiro começa antes, enquanto os seguintes são sintetizados e guardados em cache local.</p>
          </div>
          <HealthCenter report={healthReport} isChecking={isHealthChecking} onCheck={onRunHealthCheck} />
          <div className="rounded border border-emerald-900/60 bg-emerald-950/10 p-4">
            <h3 className="font-bold text-emerald-200">Núcleo ambiental autônomo</h3>
            <p className="mt-1">O mapa, a estação, o horário, o relevo e os hexes à frente limitam clima e prenúncios. Nevasca só entra no sorteio onde neve é fisicamente plausível. Ao se aproximar de uma cidade, o app aumenta gradualmente marcas de rodas, viajantes, mercadores, patrulhas e camadas de estrada sem afirmar que a Companhia já chegou.</p>
          </div>
          <p className="text-xs text-slate-500">O acesso ao Google Drive permanece no fluxo nativo já configurado para este aplicativo.</p>
        </div>
      </section>

      <footer className="lg:col-span-2 border-t border-blue-950/70 pt-4 text-center text-xs text-slate-500">
        O Um Anel · versão do jogo <strong className="text-blue-300">{APP_VERSION}</strong> · código {APP_VERSION_CODE}
      </footer>
    </div>
  );
};
