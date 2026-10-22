#!/usr/bin/env python3
"""Aplica a camada de importação à fonte OUA Android Studio, sem modificar o sistema de regras."""
from pathlib import Path
import sys, shutil
root=Path(sys.argv[1]).resolve()
base=Path(__file__).parent
p=root/'lib/braseiroJsonAdventure.ts';p.parent.mkdir(parents=True,exist_ok=True)
shutil.copy2(base/'braseiroJsonAdventure.ts',p)
app=root/'App.tsx';s=app.read_text(encoding='utf-8')
anchor="import { CampaignView } from './components/CampaignView';"
assert anchor in s
if "import { masterAdventureContext, synchronizeConfirmedEvents }" not in s:
 s=s.replace(anchor,anchor+"\nimport { masterAdventureContext, synchronizeConfirmedEvents } from './lib/braseiroJsonAdventure';")
old="      const adventureRuntimeContext = buildAdventureRuntimeContext(base);\n      directorContext += adventureRuntimeContext;\n      publicContext += adventureRuntimeContext;"
new="      const adventureRuntimeContext = buildAdventureRuntimeContext(base);\n      // Dados privados da aventura entram SOMENTE no contexto diretor, nunca no contexto público/TTS.\n      synchronizeConfirmedEvents(base);\n      directorContext += adventureRuntimeContext + masterAdventureContext(base.id);\n      publicContext += adventureRuntimeContext;"
assert old in s or new in s
s=s.replace(old,new);app.write_text(s,encoding='utf-8')
v=root/'components/CampaignView.tsx';s=v.read_text(encoding='utf-8')
anchor="import { CampaignState } from '../lib/db';"
assert anchor in s
if "../lib/braseiroJsonAdventure" not in s:s=s.replace(anchor,anchor+"\nimport { importAdventure, finishAdventure, loadAdventure } from '../lib/braseiroJsonAdventure';")
needle="  const fileInputRef = useRef<HTMLInputElement>(null);"
extra="""
  const adventureInputRef = useRef<HTMLInputElement>(null);
  const [adventureRevision, setAdventureRevision] = useState(0);
  const ledger = useMemo(() => loadAdventure(campaign.id), [campaign.id,adventureRevision]);
  const handleAdventureJson = async (file: File) => {
    setIsBusy(true);
    try {
      const state=importAdventure(campaign,await file.text());
      setAdventureRevision(v=>v+1);
      setMessage('Aventura importada: '+(state.active?.title||'')+'. O Mestre receberá o roteiro privado na próxima cena.');
    } catch (e) {setMessage('Importação recusada: '+(e instanceof Error?e.message:String(e)));}
    finally {setIsBusy(false);if(adventureInputRef.current)adventureInputRef.current.value='';}
  };
"""
assert needle in s;s=s.replace(needle,needle+extra)
anchor='          <FinalHomologationPanel campaign={campaign} />'
section="""          <section className="medieval-panel p-5" aria-label="Aventura JSON e vida persistente">
            <h3 className="medieval-title text-sm">Aventura JSON · Vida persistente</h3>
            <p className="my-2 text-xs text-slate-400">Importe uma aventura Braseiro (não é backup de campanha). Personagens, mundo, relações e feitos continuam existindo.</p>
            {ledger?.active && <p className="my-2 text-xs text-blue-300" role="status">Aventura ativa: {ledger.active.title}</p>}
            <button type="button" disabled={isBusy} onClick={() => adventureInputRef.current?.click()} className="medieval-button w-full">Importar aventura JSON</button>
            <input aria-label="Escolher aventura JSON" ref={adventureInputRef} type="file" accept=".json,application/json" className="sr-only" onChange={event => {const file=event.target.files?.[0];if(file)void handleAdventureJson(file);}} />
            {ledger?.active && <button type="button" disabled={isBusy} className="medieval-button-secondary mt-2 w-full" onClick={() => {if(window.confirm('Concluir roteiro importado? O mundo e todos os personagens serão preservados.')){finishAdventure(campaign.id);setAdventureRevision(v=>v+1);setMessage('Roteiro concluído. Memórias e mundo preservados.');}}}>Concluir aventura · preservar mundo</button>}
            {!!ledger?.completed?.length && <p className="mt-2 text-xs text-slate-500">Aventuras concluídas: {ledger.completed.length}</p>}
          </section>

"""
assert anchor in s;s=s.replace(anchor,section+anchor)
v.write_text(s,encoding='utf-8')
print('OUA_ADVENTURE_IMPORT_PATCHED',root)