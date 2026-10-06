import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(process.argv[2] || '.');
const p = (...parts) => path.join(root, ...parts);
const read = (...parts) => fs.readFileSync(p(...parts), 'utf8');

function replaceRequired(text, oldText, newText, label) {
  if (text.includes(newText)) return text;
  if (!text.includes(oldText)) throw new Error('R10 patch anchor missing: ' + label);
  return text.replace(oldText, newText);
}
function replaceOptional(text, oldText, newText) {
  return text.includes(oldText) ? text.replace(oldText, newText) : text;
}

// Compatibility patches inherited from R9.
{
  const file = p('scripts','verify-human-master-runtime.mjs');
  let s = fs.readFileSync(file,'utf8');
  s = replaceOptional(s, "assert.match(narrative, /5\\.000 a 6\\.500 caracteres/);", "assert.match(narrative, /CanonicalNarratorProfileService/);");
  fs.writeFileSync(file,s,'utf8');
}
{
  const file = p('src','services','NarrativeIntentService.ts');
  let s = fs.readFileSync(file,'utf8');
  s = replaceOptional(s, '  presentation: ScenePresentationKind;\n  preservesCheckpoint:', '  presentation: ScenePresentationKind;\n  presentationKind?: ScenePresentationKind;\n  preservesCheckpoint:');
  s = replaceOptional(s, '      presentation,\n      preservesCheckpoint,', '      presentation,\n      presentationKind: presentation,\n      preservesCheckpoint,');
  fs.writeFileSync(file,s,'utf8');
}
{
  const file = p('scripts','verify-bugreport-freeze-2026-10-05.mjs');
  let s = fs.readFileSync(file,'utf8');
  s = replaceOptional(s, 'assert.match(adventure, /\\[MASTER_FLOW\\] scene-commit:start/);', "assert.match(adventure, /masterTrace\\('scene-commit:start'/);");
  fs.writeFileSync(file,s,'utf8');
}
{
  const file = p('src','services','api.ts');
  let s = fs.readFileSync(file,'utf8');
  s = replaceOptional(
    s,
    "  const maxOutputTokens = profile === 'Casual' ? 1100 : profile === 'Expert' ? (initial ? 2600 : 1800) : (initial ? 2200 : 1500);\n  const cinematic = Boolean(modelPolicy?.cinematic);\n  const initial = Boolean(modelPolicy?.initial);",
    "  const cinematic = Boolean(modelPolicy?.cinematic);\n  const initial = Boolean(modelPolicy?.initial);\n  const maxOutputTokens = profile === 'Casual' ? 1100 : profile === 'Expert' ? (initial ? 2600 : 1800) : (initial ? 2200 : 1500);"
  );
  fs.writeFileSync(file,s,'utf8');
}
{
  const file = p('scripts','verify-canonical-narrator-profile.mjs');
  let s = fs.readFileSync(file,'utf8');
  const oldMarkers = "  'Ficção antes da mecânica', 'Prioridade de verdade', 'Não controlar pensamento', 'NPCs existem antes da câmera chegar',\n  'Informação deve surgir progressivamente', 'Não terminar toda resposta com “O que você faz?”', 'PT-BR natural e falável',\n  'ABERTURA: âncora temporal/cultural',";
  const newMarkers = "  'Ficção antes da mecânica', 'não invente tempo, clima, posição, regra ou consequência', 'Preserve agência', 'NPCs entram fazendo algo',\n  'Segredos, intenção oculta e conclusão de mistério', 'Não termine mecanicamente com “O que você faz?”', 'PT-BR natural, concreto, sensorial e falável',\n  'ABERTURA: âncora temporal/cultural',";
  s = replaceOptional(s, oldMarkers, newMarkers);
  fs.writeFileSync(file,s,'utf8');
}
{
  const file = p('scripts','audit-deep.ts');
  let s = fs.readFileSync(file,'utf8');
  s = replaceOptional(s, '/finally \\{[\\s\\S]{0,100}connection\\.disconnect/', '/finally \\{[\\s\\S]{0,240}connection\\.disconnect/');
  fs.writeFileSync(file,s,'utf8');
}

// Samsung/Android suggested choice control.
const touchComponent = `import React, { useRef, useState } from 'react';

type Props = {
  key?: React.Key;
  action: string;
  index: number;
  busy: boolean;
  active: boolean;
  onChoose: (action: string) => void;
};

export function SuggestedChoiceButton({ action, index, busy, active, onChoose }: Props) {
  const [pressed, setPressed] = useState(false);
  const start = useRef<{ x: number; y: number; pointerId: number } | null>(null);
  const pointerHandled = useRef(false);

  const choose = () => {
    if (!busy) onChoose(action);
  };

  const release = (event: React.PointerEvent<HTMLButtonElement>) => {
    const current = start.current;
    start.current = null;
    setPressed(false);
    try { event.currentTarget.releasePointerCapture?.(event.pointerId); } catch {}
    if (!current || busy) return;
    const dx = event.clientX - current.x;
    const dy = event.clientY - current.y;
    if (Math.hypot(dx, dy) > 28) return;
    pointerHandled.current = true;
    choose();
  };

  return <button
    data-suggested-choice="true"
    type="button"
    aria-busy={active}
    disabled={busy}
    onPointerDown={event => {
      if (busy) return;
      pointerHandled.current = false;
      start.current = { x: event.clientX, y: event.clientY, pointerId: event.pointerId };
      setPressed(true);
      try { event.currentTarget.setPointerCapture?.(event.pointerId); } catch {}
    }}
    onPointerUp={release}
    onPointerCancel={() => { start.current = null; setPressed(false); pointerHandled.current = false; }}
    onClick={event => {
      event.preventDefault();
      if (pointerHandled.current) { pointerHandled.current = false; return; }
      choose();
    }}
    className={\`suggested-choice-button w-full min-h-16 rounded-xl border bg-bg-base px-4 py-4 text-left text-[17px] leading-6 font-medium disabled:opacity-55 \${active ? 'is-working border-accent' : 'border-border'} \${pressed ? 'is-pressed' : ''}\`}
  >
    <span className="mr-2 text-accent font-bold">{index + 1}.</span>
    <span>{active ? 'Processando esta escolha…' : action}</span>
  </button>;
}
`;
fs.mkdirSync(p('src','components'), { recursive: true });
fs.writeFileSync(p('src','components','SuggestedChoiceButton.tsx'), touchComponent, 'utf8');

{
  const file = p('src','screens','AdventureScreenV3.tsx');
  let s = fs.readFileSync(file,'utf8');
  if (!s.includes("import { SuggestedChoiceButton } from '../components/SuggestedChoiceButton';")) {
    s = replaceRequired(s,
      "import { CampaignModuleStateService } from '../services/CampaignModuleStateService';",
      "import { CampaignModuleStateService } from '../services/CampaignModuleStateService';\nimport { SuggestedChoiceButton } from '../components/SuggestedChoiceButton';",
      'Adventure import SuggestedChoiceButton');
  }
  s = replaceOptional(s,
    "  const [npcPortraitBusyId, setNpcPortraitBusyId] = useState<string | null>(null);",
    "  const [npcPortraitBusyId, setNpcPortraitBusyId] = useState<string | null>(null);\n  const [activeChoice, setActiveChoice] = useState<string | null>(null);");
  s = replaceOptional(s,
    "    void DataSafetyService.checkpoint('cena confirmada', 'core').catch(() => undefined);",
    "    DataSafetyService.scheduleCheckpoint('cena confirmada', 60000);");
  s = replaceOptional(s,
    "    const text = (forced || input).trim();\n    if (!text || busy) return;",
    "    const text = (forced || input).trim();\n    if (!text || busy) return;\n    DataSafetyService.cancelScheduledCheckpoint();\n    if (source === 'suggested') setActiveChoice(text);");
  s = replaceOptional(s,
    "    setBusy(true);\n    setError('');\n    setMessage('O Mestre está consultando o mundo, as fichas e as regras...');",
    "    setBusy(true);\n    setError('');\n    setMessage(source === 'suggested' ? 'Escolha recebida. O Mestre está preparando a consequência...' : 'O Mestre está consultando o mundo, as fichas e as regras...');");
  s = replaceOptional(s,
    "      const scene = sceneFrom(raw, active, sceneHex, roll ? 'Resultado da rolagem enviado ao Mestre.' : text, actor, false);",
    "      setMessage('Salvando a nova cena...');\n      const scene = sceneFrom(raw, active, sceneHex, roll ? 'Resultado da rolagem enviado ao Mestre.' : text, actor, false);");
  s = replaceOptional(s,
    "      setBusy(false);\n      setMessage('');\n    }\n  };",
    "      setBusy(false);\n      setActiveChoice(null);\n      setMessage('');\n    }\n  };");

  const oldChoice = `{ensurePlayableChoices(scene.suggestedActions, scene.sequence === 1).map((action, actionIndex) => <button
          key={\`\${scene.id}-choice-\${actionIndex}\`}
          type="button"
          onClick={() => void send(action, undefined, false, undefined, 'suggested')}
          disabled={busy}
          className="w-full min-h-14 rounded-xl border border-border bg-bg-base px-4 py-3 text-left text-[17px] leading-6 font-medium hover:border-accent/70 hover:bg-accent/5 disabled:opacity-40"
        ><span className="mr-2 text-accent font-bold">{actionIndex + 1}.</span>{action}</button>)}`;
  const newChoice = `{ensurePlayableChoices(scene.suggestedActions, scene.sequence === 1).map((action, actionIndex) => <SuggestedChoiceButton
          key={\`\${scene.id}-choice-\${actionIndex}\`}
          action={action}
          index={actionIndex}
          busy={busy}
          active={busy && activeChoice === action}
          onChoose={value => void send(value, undefined, false, undefined, 'suggested')}
        />)}`;
  s = replaceRequired(s, oldChoice, newChoice, 'Adventure suggested choice JSX');
  s = replaceOptional(s,
    'className="fixed inset-x-0 top-0 z-[240] bg-black/80 backdrop-blur-sm flex items-end sm:items-center sm:justify-center"',
    'className="fixed inset-0 z-[240] bg-black/80 backdrop-blur-sm flex items-end sm:items-center sm:justify-center"');
  s = replaceOptional(s,
    'className="bg-bg-surface border border-accent rounded-xl p-5 max-w-md w-full"',
    'className="bg-bg-surface border border-accent rounded-xl p-5 max-w-md w-full max-h-[calc(100dvh-2rem)] overflow-y-auto"');
  fs.writeFileSync(file,s,'utf8');
}

// Safety snapshots: defer long snapshot well outside the next action and allow cancellation.
{
  const file = p('src','services','DataSafetyService.ts');
  let s = fs.readFileSync(file,'utf8');
  s = replaceOptional(s,
`const CORE_SNAPSHOT_STORES = [
  'app_meta', 'campaign', 'characters', 'scenes', 'branches', 'npcs', 'bestiary', 'rolls',
  'map_discovery', 'token_assignments', 'map_import_state', 'token_import_state',
  'rule_documents', 'rule_import_state', 'character_generation_drafts', 'xp_sessions', 'world_state', 'settings',
] as const satisfies ReadonlyArray<keyof BraseiroDB>;`,
`const CORE_SNAPSHOT_STORES = [
  'app_meta', 'campaign', 'characters', 'scenes', 'branches', 'npcs', 'rolls',
  'xp_sessions', 'world_state', 'settings',
] as const satisfies ReadonlyArray<keyof BraseiroDB>;`);
  s = replaceOptional(s, "  static scheduleCheckpoint(reason = 'alteração') {", "  static scheduleCheckpoint(reason = 'alteração', delayMs = 1200) {");
  s = replaceOptional(s, "    }, 1200);", "    }, Math.max(1200, delayMs));");
  s = replaceOptional(s,
    "      checkpointTimer = null;\n      void this.checkpoint(reason, 'core').catch(error => console.warn('[DataSafety] Checkpoint adiado sem apagar o anterior:', error));",
    "      checkpointTimer = null;\n      if (typeof document !== 'undefined' && document.visibilityState === 'visible') {\n        this.scheduleCheckpoint(reason, Math.max(60000, delayMs));\n        return;\n      }\n      void this.checkpoint(reason, 'core').catch(error => console.warn('[DataSafety] Checkpoint adiado sem apagar o anterior:', error));");
  if (!s.includes('static cancelScheduledCheckpoint()')) {
    s = replaceRequired(s,
      "  static checkpoint(reason = 'manual', scope: SnapshotScope = 'core', strict = false): Promise<boolean> {",
      "  static cancelScheduledCheckpoint() {\n    if (typeof window === 'undefined') return;\n    if (checkpointTimer !== null) window.clearTimeout(checkpointTimer);\n    checkpointTimer = null;\n  }\n\n  static checkpoint(reason = 'manual', scope: SnapshotScope = 'core', strict = false): Promise<boolean> {",
      'DataSafety cancel scheduled checkpoint');
  }
  fs.writeFileSync(file,s,'utf8');
}

// Foreground gameplay must never launch the heavy safety snapshot after a critical write.
{
  const file = p('src','main.tsx');
  let s = fs.readFileSync(file,'utf8');
  s = replaceOptional(s, '    DataSafetyService.scheduleCheckpoint(store);', '    DataSafetyService.scheduleCheckpoint(store, 60000);');
  s = replaceOptional(s, "  DataSafetyService.scheduleCheckpoint('inicialização validada');", "  DataSafetyService.scheduleCheckpoint('inicialização validada', 60000);");
  fs.writeFileSync(file,s,'utf8');
}

// Bound action model time without allowing a minimum timeout to exceed remaining budget.
{
  const file = p('src','services','ModelRouterService.ts');
  let s = fs.readFileSync(file,'utf8');
  s = replaceOptional(s, '      if (remainingBudget <= 4_000) {', '      if (remainingBudget <= 8_800) {');
  s = replaceOptional(s,
    '            ? Math.max(8_000, Math.min(Number(payload.timeoutMs || remainingBudget), remainingBudget - 800))',
    '            ? Math.min(Number(payload.timeoutMs || remainingBudget), Math.max(1_000, remainingBudget - 800))');
  fs.writeFileSync(file,s,'utf8');
}
{
  const file = p('src','services','api.ts');
  let s = fs.readFileSync(file,'utf8');
  s = replaceOptional(s,
    "const maxOutputTokens = profile === 'Casual' ? 1100 : profile === 'Expert' ? (initial ? 2600 : 1800) : (initial ? 2200 : 1500);",
    "const maxOutputTokens = profile === 'Casual' ? 1000 : profile === 'Expert' ? (initial ? 2600 : 1400) : (initial ? 2200 : 1250);");
  s = replaceOptional(s, 'timeoutMs: initial ? 34_000 : 26_000,', 'timeoutMs: initial ? 34_000 : 14_000,');
  s = replaceOptional(s, 'totalBudgetMs: initial ? 36_000 : 28_000,', 'totalBudgetMs: initial ? 36_000 : 16_000,');
  fs.writeFileSync(file,s,'utf8');
}

// Galaxy A57 layout and touch behavior.
{
  const file = p('src','components','Layout.tsx');
  let s = fs.readFileSync(file,'utf8');
  s = replaceOptional(s,
    'className="bg-bg-surface border-t border-border p-4 rounded-t-2xl mb-16 animate-in slide-in-from-bottom-4"',
    'className="more-menu-sheet bg-bg-surface border-t border-border p-4 rounded-t-2xl animate-in slide-in-from-bottom-4"');
  fs.writeFileSync(file,s,'utf8');
}
{
  const file = p('src','index.css');
  let css = fs.readFileSync(file,'utf8');
  if (!css.includes('R10 suggested action touch reliability')) css += `

/* Galaxy A57 / Samsung compact portrait compatibility R10 */
@media (max-width: 430px) {
  html, body, #root { width:100%; max-width:100%; min-width:0; overflow-x:hidden; }
  .app-shell { width:100%; height:100dvh; min-height:100dvh; max-height:100dvh; overflow:hidden; }
  .app-header { min-width:0; min-height:calc(3.35rem + var(--braseiro-safe-top)); padding-left:.65rem; padding-right:.65rem; gap:.4rem; }
  .app-header h2 { min-width:0; max-width:36vw; font-size:.95rem; letter-spacing:.055em; }
  .app-header button { min-width:2.75rem; min-height:2.75rem; padding-left:.65rem; padding-right:.65rem; }
  .mobile-bottom-nav { gap:.125rem; padding-top:.35rem; padding-left:.25rem; padding-right:.25rem; padding-bottom:max(.65rem,var(--braseiro-safe-bottom)); min-height:calc(4.25rem + max(.65rem,var(--braseiro-safe-bottom))); }
  .mobile-bottom-nav button { min-width:0; min-height:3.35rem; padding:.25rem .1rem; }
  .mobile-bottom-nav button span { display:block; max-width:100%; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:9px; letter-spacing:0; }
  .more-menu-sheet { box-sizing:border-box; margin-bottom:calc(4.25rem + max(.65rem,var(--braseiro-safe-bottom))); max-height:calc(100dvh - 5.25rem - var(--braseiro-safe-top) - max(.65rem,var(--braseiro-safe-bottom))); overflow-y:auto; overscroll-behavior:contain; padding:.85rem; }
  .more-menu-sheet .grid { grid-template-columns:repeat(3,minmax(0,1fr)); gap:.5rem; }
  .more-menu-sheet button { min-width:0; min-height:4.2rem; padding:.65rem .25rem; }
  .more-menu-sheet button span { line-height:1.15; overflow-wrap:anywhere; text-align:center; }
  .adventure-composer { padding-left:.75rem; padding-right:.75rem; padding-bottom:max(.75rem,var(--braseiro-safe-bottom)); }
  .action-composer-sheet { width:100%; max-height:calc(100dvh - var(--braseiro-safe-top) - .5rem); overscroll-behavior:contain; }
  .action-composer-input { min-height:7.25rem; max-height:34dvh; font-size:16px; }
  .narration-toolbar { grid-template-columns:1fr; align-items:stretch; }
  .narration-replay-button { width:100%; }
}
.suggested-choice-button { position:relative; z-index:2; touch-action:pan-y; -webkit-user-select:none; user-select:none; -webkit-tap-highlight-color:transparent; }
.suggested-choice-button.is-pressed { transform:scale(.985); border-color:var(--accent-color); background:color-mix(in srgb,var(--accent-color) 14%,var(--bg-color)); }
.suggested-choice-button.is-working { opacity:.86 !important; box-shadow:0 0 0 2px color-mix(in srgb,var(--accent-color) 28%,transparent); }
`;
  fs.writeFileSync(file,css,'utf8');
}

// Install-over-existing identity: same package/signer, higher versionCode.
{
  const pkgPath = p('package.json');
  const pkg = JSON.parse(fs.readFileSync(pkgPath,'utf8'));
  pkg.version = '3.3.6';
  if (String(pkg.scripts?.build || '').includes('node node_modules/esbuild/bin/esbuild')) {
    pkg.scripts.build = pkg.scripts.build.replace('node node_modules/esbuild/bin/esbuild', 'npx --no-install esbuild');
  }
  fs.writeFileSync(pkgPath,JSON.stringify(pkg,null,2)+'\n','utf8');

  const gradlePath = p('android','app','build.gradle');
  let gradle = fs.readFileSync(gradlePath,'utf8');
  gradle = gradle.replace(/versionCode\s+\d+/, 'versionCode 303600');
  gradle = gradle.replace(/versionName\s+"[^"]+"/, 'versionName "3.3.6"');
  if (!/applicationId\s+"com\.braseiro\.forbiddenlands"/.test(gradle)) throw new Error('Wrong applicationId; refusing non-updatable APK');
  fs.writeFileSync(gradlePath,gradle,'utf8');
}

console.log('R10 patch applied: update-in-place 3.3.6 / 303600; Samsung touch; nonblocking checkpoint; bounded action budget.');
