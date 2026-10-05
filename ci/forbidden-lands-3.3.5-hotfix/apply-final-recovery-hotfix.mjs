import { readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(process.argv[2] || '.');
const read = (p) => readFileSync(resolve(root, p), 'utf8');
const write = (p, s) => writeFileSync(resolve(root, p), s, 'utf8');

function replaceOnce(path, from, to, label) {
  let s = read(path);
  if (s.includes(to)) {
    console.log('ALREADY', path, label);
    return;
  }
  if (!s.includes(from)) throw new Error('Missing anchor ' + path + ' :: ' + label);
  s = s.replace(from, to);
  write(path, s);
  console.log('PATCHED', path, label);
}

// 1) Fix the 3D dice component so React key typing cannot break the build.
replaceOnce(
  'src/components/Dice3DStage.tsx',
  `      {dice.map((die, index) => die.sides === 6
        ? <D6Cube key={\`\${rollKey}-\${index}\`} die={die} index={index} rollKey={rollKey} />
        : <PolyDie key={\`\${rollKey}-\${index}\`} die={die} index={index} rollKey={rollKey} />)}`,
  `      {dice.map((die, index) => <React.Fragment key={\`\${rollKey}-\${index}\`}>
        {die.sides === 6
          ? <D6Cube die={die} index={index} rollKey={rollKey} />
          : <PolyDie die={die} index={index} rollKey={rollKey} />}
      </React.Fragment>)}`,
  'React key wrapper'
);

// 2) Cold start: the bugreport showed NativeStateVault blocking the first paint for > 1 minute.
// Bound only the emergency native mirror read; the main IndexedDB path stays authoritative.
{
  const path = 'src/services/DataSafetyService.ts';
  let s = read(path);
  const anchor = "const NativeStateVault = registerPlugin<NativeStateVaultPlugin>('NativeStateVault');";
  const helper = `${anchor}

const withNativeVaultBudget = async <T>(promise: Promise<T>, timeoutMs = 900): Promise<T> => {
  let timer: number | undefined;
  try {
    return await Promise.race([
      promise,
      new Promise<T>((_, reject) => {
        timer = window.setTimeout(() => reject(new Error(\`timeout do cofre nativo no cold start (\${timeoutMs} ms)\`)), timeoutMs);
      }),
    ]);
  } finally {
    if (timer !== undefined) window.clearTimeout(timer);
  }
};`;
  if (!s.includes('withNativeVaultBudget')) {
    if (!s.includes(anchor)) throw new Error('NativeStateVault anchor missing');
    s = s.replace(anchor, helper);
  }
  s = s.replace(
    'const result = await NativeStateVault.readSnapshot();',
    'const result = await withNativeVaultBudget(NativeStateVault.readSnapshot(), 900);'
  );
  write(path, s);
}

// 3) Render the three suggested actions the model was already returning.
// Also guarantee three playable choices and enlarge narration on phone.
{
  const path = 'src/screens/AdventureScreenV3.tsx';
  let s = read(path);

  if (!s.includes('function ensurePlayableChoices')) {
    const anchor = `function naturalPlace(hex: IndividualHex) {
  const terrainKey = hex.terrainResolution?.finalTerrain || hex.physicalTerrain || hex.terrain || '';
  const terrain = (hex.terrainResolution?.finalTerrainLabel || hex.physicalTerrainLabel || TERRAIN_NARRATIVE_LABELS[terrainKey] || 'terras selvagens').toLowerCase();
  const features = new Set((hex.visualEnvironmentAnalysis?.features || []).filter(item => item.confidence >= .55).map(item => item.type));
  const detail = features.has('river') ? ' junto ao rio' : features.has('road') ? ' pela estrada' : features.has('coast') ? ' junto à costa' : '';
  const direction = naturalDirection(hex.coordinate);
  return \`\${terrain}\${direction ? \` do \${direction}\` : ''}\${detail}\`;
}
`;
    const helper = anchor + `
function ensurePlayableChoices(actions: unknown, initial = false) {
  const source = Array.isArray(actions) ? actions.map(item => String(item || '').trim()).filter(Boolean) : [];
  const fallback = initial
    ? ['Observar os arredores e procurar sinais concretos', 'Conversar com o grupo antes de se comprometer', 'Aproximar-se de um ponto visível que pareça relevante']
    : ['Observar a consequência imediata antes de agir', 'Tentar uma abordagem diferente usando o que já foi revelado', 'Interagir com uma pessoa, objeto ou rota já presente na cena'];
  const unique: string[] = [];
  for (const item of [...source, ...fallback]) {
    if (!unique.some(existing => existing.toLocaleLowerCase('pt-BR') === item.toLocaleLowerCase('pt-BR'))) unique.push(item);
    if (unique.length === 3) break;
  }
  return unique;
}
`;
    if (!s.includes(anchor)) throw new Error('naturalPlace anchor missing');
    s = s.replace(anchor, helper);
  }

  const improve = `    raw.suggestedActions = SuggestionDiversifierService.improve(
      raw.suggestedActions,
      { title: raw.sceneTitle, narration: raw.narration, npcDialogue: raw.npcDialogue || [] },
      worldState || undefined,
      npcs,
      scenes.slice(-12).flatMap(item => item.suggestedActions || []),
    );`;
  if (!s.includes('raw.suggestedActions = ensurePlayableChoices(raw.suggestedActions, initial);')) {
    if (!s.includes(improve)) throw new Error('suggestion improvement anchor missing');
    s = s.replace(improve, improve + '\n    raw.suggestedActions = ensurePlayableChoices(raw.suggestedActions, initial);');
  }

  s = s.replace(
    "<div className={\`whitespace-pre-wrap \${utility ? 'font-sans text-base leading-relaxed' : 'font-serif text-lg leading-relaxed'}\`}>{scene.narration}</div>",
    "<div className={\`whitespace-pre-wrap \${utility ? 'font-sans text-[17px] sm:text-lg leading-7' : 'font-serif text-[20px] sm:text-[21px] leading-8 sm:leading-9'}\`}>{scene.narration}</div>"
  );

  if (!s.includes('Escolhas sugeridas')) {
    const anchor = `      {isLatest && !pending.length && <div className="mt-6">
        <button
          type="button"
          onClick={() => setActionComposerOpen(true)}`;
    const replacement = `      {isLatest && !pending.length && !utility && <div className="mt-6 space-y-3">
        <div className="text-sm font-bold uppercase tracking-[0.12em] text-text-secondary">Escolhas sugeridas</div>
        {ensurePlayableChoices(scene.suggestedActions, scene.sequence === 1).map((action, actionIndex) => <button
          key={\`\${scene.id}-choice-\${actionIndex}\`}
          type="button"
          onClick={() => void send(action)}
          disabled={busy}
          className="w-full min-h-14 rounded-xl border border-border bg-bg-base px-4 py-3 text-left text-[17px] leading-6 font-medium hover:border-accent/70 hover:bg-accent/5 disabled:opacity-40"
        ><span className="mr-2 text-accent font-bold">{actionIndex + 1}.</span>{action}</button>)}
      </div>}
      {isLatest && !pending.length && <div className="mt-4">
        <button
          type="button"
          onClick={() => setActionComposerOpen(true)}`;
    if (!s.includes(anchor)) throw new Error('Ação Livre anchor missing');
    s = s.replace(anchor, replacement);
  }

  s = s.replace(
    '<p className="mb-5 text-text-secondary">Forbidden Lands — um jogador, vários heróis</p>',
    '<p className="mb-5 text-[17px] text-text-secondary">Forbidden Lands — um jogador, vários heróis</p>'
  );
  s = s.replace(
    '<span className="block mt-1 text-xs text-text-secondary">Profundidade Expert fixa: cenas substanciais, continuidade rigorosa e riqueza sensorial sem modo econômico.</span>',
    '<span className="block mt-1 text-[15px] leading-6 text-text-secondary">Profundidade Expert fixa: cenas substanciais, continuidade rigorosa e riqueza sensorial sem modo econômico.</span>'
  );
  s = s.replace(
    '<p className="text-xs text-text-secondary mt-3">O Mestre criará a primeira cena somente depois deste toque.</p>',
    '<p className="text-[15px] leading-6 text-text-secondary mt-3">O Mestre criará a primeira cena somente depois deste toque. Se a IA demorar, o Braseiro abre uma cena local jogável em vez de ficar preso.</p>'
  );
  write(path, s);
}

// 4) Legacy overlay must not crash before body/head exists, and character info must not dump JSON.
{
  const path = 'public/assets/braseiro-final-enhancements.js';
  let s = read(path);
  s = s.replace(
    "function injectCss(){const l=document.createElement('link');l.rel='stylesheet';l.href='./assets/braseiro-final-enhancements.css';document.head.appendChild(l);}",
    "function injectCss(){const mount=document.head||document.documentElement;if(!mount)return;const l=document.createElement('link');l.rel='stylesheet';l.href='./assets/braseiro-final-enhancements.css';mount.appendChild(l);}"
  );
  s = s.replace(
    "document.body.appendChild(bar);bar.addEventListener('click'",
    "const mount=document.body||document.documentElement;if(!mount)return;mount.appendChild(bar);bar.addEventListener('click'"
  );
  s = s.replace(
    "document.body.appendChild(el);el.addEventListener('click'",
    "const mount=document.body||document.documentElement;if(!mount)return el;mount.appendChild(el);el.addEventListener('click'"
  );
  s = s.replace(
    "return modal(x.name,\`<div class=\\\"fl-sheet\\\"><b>Iniciativa</b><span>\${x.initiative}</span><b>Força</b><span>\${x.currentStrength}/\${x.maxStrength}</span><b>Armadura</b><span>\${x.armor||0}</span><b>Estado</b><span>\${esc(x.status)}</span></div><pre>\${esc(JSON.stringify(x.quickSheet||{},null,2))}</pre>\`);",
    "return modal(x.name,\`<div class=\\\"fl-sheet\\\"><b>Iniciativa</b><span>\${x.initiative}</span><b>Força</b><span>\${x.currentStrength}/\${x.maxStrength}</span><b>Armadura</b><span>\${x.armor||0}</span><b>Estado</b><span>\${esc(x.status)}</span><b>PFV</b><span>\${esc(x.quickSheet?.willpower??'—')}</span><b>Condições</b><span>\${esc(Array.isArray(x.quickSheet?.conditions)&&x.quickSheet.conditions.length?x.quickSheet.conditions.join(', '):'Nenhuma')}</span></div>\`);"
  );
  write(path, s);
}

// 5) Replace focused gate with checks that match the bugreport and the visible requirements.
write('scripts/verify-runtime-hotfix-3d-startup.mjs', `import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const dice = readFileSync('src/screens/DiceScreenV2.tsx', 'utf8');
const stage = readFileSync('src/components/Dice3DStage.tsx', 'utf8');
const css = readFileSync('src/index.css', 'utf8');
const master = readFileSync('src/services/MasterOrchestratorService.ts', 'utf8');
const adventure = readFileSync('src/screens/AdventureScreenV3.tsx', 'utf8');
const safety = readFileSync('src/services/DataSafetyService.ts', 'utf8');
const legacy = readFileSync('public/assets/braseiro-final-enhancements.js', 'utf8');

assert.match(dice, /<Dice3DStage roll=\\{roll\\}/);
assert.match(stage, /dice3d-cube/);
assert.match(css, /transform-style:preserve-3d/);
assert.match(css, /@keyframes dice3d-roll/);
assert.match(master, /if \\(input\\.initial\\) \\{[\\s\\S]*?plan = this\\.localPlan\\(input\\)/);
assert.match(master, /input\\.initial \\? 24_000 : 38_000/);
assert.match(master, /if \\(!input\\.initial && advancesFiction/);
assert.match(safety, /withNativeVaultBudget\\(NativeStateVault\\.readSnapshot\\(\\), 900\\)/);
assert.match(adventure, /Escolhas sugeridas/);
assert.match(adventure, /ensurePlayableChoices\\(scene\\.suggestedActions/);
assert.match(adventure, /text-\\[20px\\].*sm:text-\\[21px\\]/);
assert.doesNotMatch(legacy, /document\\.body\\.appendChild\\(bar\\)/);
assert.doesNotMatch(legacy, /JSON\\.stringify\\(x\\.quickSheet/);
console.log('PASS hotfix: startup bounded, initial scene nonblocking, three visible choices, larger narration and 3D dice.');
`);

console.log('Forbidden Lands final recovery hotfix applied.');
