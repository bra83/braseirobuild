import React, { useMemo, useRef, useState } from 'react';
import {
  BookOpen,
  Clock,
  Compass,
  Download,
  FileArchive,
  RotateCcw,
  ShieldCheck,
  Swords,
  Upload,
  Users,
} from 'lucide-react';
import { CampaignState } from '../lib/db';
import {
  generateCompleteBackup,
  generateQuickBackup,
  restoreCompleteBackup,
  restoreQuickBackup,
} from '../lib/backup';
import { exportBackupNative } from '../lib/native';
import { GoogleDriveExplorer } from './GoogleDriveExplorer';
import { FinalHomologationPanel } from './FinalHomologationPanel';

interface CampaignViewProps {
  campaign: CampaignState;
  onRestoreCampaign: (restored: CampaignState) => void;
  onResetCampaign: () => void;
}

function safeFileStamp() {
  return new Date().toISOString().replace(/[:T]/g, '-').replace(/\.\d{3}Z$/, '');
}

export const CampaignView: React.FC<CampaignViewProps> = ({ campaign, onRestoreCampaign, onResetCampaign }) => {
  const [isBusy, setIsBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [isDriveExplorerOpen, setIsDriveExplorerOpen] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const stats = useMemo(() => {
    const events = campaign.events || [];
    const count = (type: string) => events.filter(event => event.type === type).length;
    const encounteredNpcs = new Set(
      events
        .filter(event => event.type === 'npc_encountered')
        .map(event => String(event.data?.npcId || event.data?.name || ''))
        .filter(Boolean),
    );
    return {
      scenes: campaign.history.length,
      journeyDays: count('journey_day_completed'),
      combats: count('combat_started'),
      npcs: encounteredNpcs.size,
    };
  }, [campaign.events, campaign.history.length]);

  const exportJson = async () => {
    setIsBusy(true);
    setMessage('Preparando um backup consistente…');
    try {
      const json = await generateQuickBackup(campaign);
      const fileName = `o_um_anel_backup_${safeFileStamp()}.json`;
      if (window.BraseiroAndroid?.exportBackup) {
        const savedName = await exportBackupNative(fileName, json);
        setMessage(`Backup gravado com sucesso: ${savedName}.`);
      } else {
        const blob = new Blob([json], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = fileName;
        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();
        window.setTimeout(() => URL.revokeObjectURL(url), 10_000);
        setMessage(`Download solicitado com o nome ${fileName}.`);
      }
    } catch (error) {
      setMessage(`Falha no backup: ${error instanceof Error ? error.message : 'erro desconhecido'}`);
    } finally {
      setIsBusy(false);
    }
  };

  const exportZip = async () => {
    setIsBusy(true);
    setMessage('Compactando campanha e tokens…');
    try {
      const blob = await generateCompleteBackup(campaign);
      const fileName = `o_um_anel_completo_${safeFileStamp()}.braseiro.zip`;
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = fileName;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 10_000);
      setMessage(`Backup completo solicitado: ${fileName}. Sons nativos permanecem na biblioteca do aplicativo.`);
    } catch (error) {
      setMessage(`Falha ao compactar: ${error instanceof Error ? error.message : 'erro desconhecido'}`);
    } finally {
      setIsBusy(false);
    }
  };

  const restoreBlob = async (blob: Blob, fileName: string) => {
    setIsBusy(true);
    setMessage(`Validando ${fileName} sem apagar seus dados atuais…`);
    try {
      const restored = fileName.toLowerCase().endsWith('.json')
        ? await restoreQuickBackup(await blob.text())
        : await restoreCompleteBackup(blob);
      await onRestoreCampaign(restored);
      setMessage('Backup restaurado. Um snapshot da campanha anterior foi preservado.');
    } catch (error) {
      setMessage(`Restauração recusada: ${error instanceof Error ? error.message : 'arquivo inválido'}`);
    } finally {
      setIsBusy(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const characterName = campaign.characterSheet.identity.name?.trim() || 'O herói';

  return (
    <div className="mx-auto flex w-full max-w-6xl select-none flex-col gap-6 pb-14 font-serif text-slate-100 animate-fadeIn">
      <section className="grid grid-cols-2 gap-3 md:grid-cols-4">
        {[
          { label: 'Cenas registradas', value: stats.scenes, icon: Compass },
          { label: 'Dias de jornada reais', value: stats.journeyDays, icon: Clock },
          { label: 'Combates iniciados', value: stats.combats, icon: Swords },
          { label: 'NPCs encontrados', value: stats.npcs, icon: Users },
        ].map(({ label, value, icon: Icon }) => (
          <div key={label} className="medieval-panel min-h-28 p-4">
            <div className="flex items-center justify-between text-blue-400/70">
              <Icon className="h-4 w-4" />
              <span className="font-serif text-[8px] font-bold uppercase tracking-widest">Registro real</span>
            </div>
            <div className="mt-3 font-serif text-3xl font-black text-blue-200">{value}</div>
            <div className="mt-1 text-xs text-slate-400">{label}</div>
          </div>
        ))}
      </section>

      <section className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        <div className="medieval-panel flex min-h-[520px] flex-col p-5 lg:col-span-7">
          <div className="mb-4 flex items-center gap-2 border-b border-blue-900/50 pb-3">
            <BookOpen className="h-4 w-4 text-blue-300" />
            <h2 className="medieval-title text-sm">O grande diário da crônica</h2>
          </div>
          <div className="flex-1 space-y-5 overflow-y-auto pr-2 scrollbar-thin">
            {!campaign.history.length ? (
              <p className="py-16 text-center text-sm italic text-slate-500">A primeira página ainda aguarda uma ação.</p>
            ) : campaign.history.map((node, index) => (
              <article key={node.id} className="relative border-l-2 border-blue-900/60 py-1 pl-5">
                <div className="absolute -left-[5px] top-2 h-2 w-2 rounded-full bg-blue-500" />
                <div className="flex flex-wrap justify-between gap-2 font-serif text-[10px] font-bold uppercase tracking-wider text-blue-400/80">
                  <span>Ato {index + 1} · {node.location || 'Local não registrado'}</span>
                  <span>{new Date(node.timestamp).toLocaleDateString('pt-BR')}</span>
                </div>
                <p className="mt-3 line-clamp-5 text-sm leading-relaxed text-slate-300">
                  {node.atmospheric_narration || node.action_result}
                </p>
                {index + 1 < campaign.history.length && (
                  <div className="mt-3 rounded border border-slate-800 bg-slate-950/60 px-3 py-2 text-xs text-slate-400">
                    <strong className="text-blue-200">{characterName} decidiu:</strong>{' '}
                    “{campaign.history[index + 1].playerAction || 'Ação livre'}”
                  </div>
                )}
              </article>
            ))}
          </div>
        </div>

        <div className="flex flex-col gap-5 lg:col-span-5">
          <div className="medieval-panel p-5">
            <div className="mb-4 flex items-center gap-2 border-b border-blue-900/50 pb-3">
              <ShieldCheck className="h-4 w-4 text-blue-300" />
              <h2 className="medieval-title text-sm">Cofre da campanha</h2>
            </div>
            <p className="mb-4 text-xs leading-relaxed text-slate-400">
              O backup JSON abre o seletor nativo do Android e só confirma depois que o arquivo é realmente gravado.
            </p>
            <div className="grid gap-2">
              <button onClick={() => void exportJson()} disabled={isBusy} className="medieval-button flex items-center justify-center gap-2">
                <Download className="h-4 w-4" /> Exportar backup seguro
              </button>
              {!window.BraseiroAndroid?.exportBackup && (
                <button onClick={() => void exportZip()} disabled={isBusy} className="medieval-button-secondary flex items-center justify-center gap-2">
                  <FileArchive className="h-4 w-4" /> Backup completo com tokens
                </button>
              )}
              <button onClick={() => fileInputRef.current?.click()} disabled={isBusy} className="medieval-button-secondary flex items-center justify-center gap-2">
                <Upload className="h-4 w-4" /> Restaurar do aparelho
              </button>
              <button onClick={() => setIsDriveExplorerOpen(true)} disabled={isBusy} className="medieval-button-secondary flex items-center justify-center gap-2">
                <Upload className="h-4 w-4" /> Restaurar do Google Drive
              </button>
              <input
                ref={fileInputRef}
                type="file"
                accept=".json,.zip,.braseiro.zip,application/json,application/zip"
                className="hidden"
                onChange={event => {
                  const file = event.target.files?.[0];
                  if (file) void restoreBlob(file, file.name);
                }}
              />
            </div>
            {message && (
              <div className={`mt-4 rounded border px-3 py-2 text-xs ${/falha|recusada/i.test(message) ? 'border-red-900 bg-red-950/30 text-red-300' : 'border-blue-900 bg-blue-950/30 text-blue-200'}`}>
                {isBusy ? '⏳ ' : ''}{message}
              </div>
            )}
          </div>

          <FinalHomologationPanel campaign={campaign} />

          <div className="medieval-panel border-red-950/70 p-5">
            <h3 className="font-serif text-xs font-bold uppercase tracking-wider text-red-400">Nova crônica</h3>
            <p className="my-3 text-xs leading-relaxed text-slate-500">
              Cria outra campanha sem apagar a atual nem a biblioteca de regras e sons.
            </p>
            <button onClick={onResetCampaign} className="flex w-full items-center justify-center gap-2 rounded border border-red-900/70 bg-red-950/25 px-3 py-2 text-xs font-bold text-red-300 hover:bg-red-950/50">
              <RotateCcw className="h-4 w-4" /> Criar nova crônica
            </button>
          </div>
        </div>
      </section>

      {isDriveExplorerOpen && (
        <GoogleDriveExplorer
          isOpen={isDriveExplorerOpen}
          title="Restaurar backup do Drive"
          allowedExtensions={['json', 'zip']}
          onClose={() => setIsDriveExplorerOpen(false)}
          onFileSelected={async (blob, fileName) => {
            setIsDriveExplorerOpen(false);
            await restoreBlob(blob, fileName);
          }}
        />
      )}
    </div>
  );
};
