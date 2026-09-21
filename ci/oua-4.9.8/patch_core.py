from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_core.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")
def write(rel, text):
    p = root / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding="utf-8")

db = read("lib/db.ts")
if "sceneVisualPromptCreatedAt?: number;" not in db:
    db = db.replace("  sceneVisualPrompt?: string;\n", "  sceneVisualPrompt?: string;\n  sceneVisualPromptCreatedAt?: number;\n", 1)
db = db.replace("  imageAspect?: '9:16';", "  imageAspect?: '9:16' | '16:9';", 1)
npc_anchor = "  tokenId: string | null;\n  presentInScene: boolean;\n"
if npc_anchor in db:
    db = db.replace(npc_anchor, """  tokenId: string | null;
  portraitPrompt?: string;
  portraitPromptCreatedAt?: number;
  portraitUpdatedAt?: number;
  presentInScene: boolean;
""", 1)
write("lib/db.ts", db)

write("lib/sceneEntitySync.ts", r"""import type { CampaignEvent, VoiceSegment } from '../types';
import type { Npc } from './db';

function norm(value: unknown): string {
  return String(value ?? '')
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function containsWhole(haystack: string, needle: string): boolean {
  if (!needle || needle.length < 3) return false;
  return (' ' + haystack + ' ').includes(' ' + needle + ' ');
}

export function reconcileSceneNpcPresence(input: {
  canonicalIds: string[];
  npcs: Npc[];
  narration: string;
  actionResult?: string;
  voiceScript?: VoiceSegment[];
  events?: CampaignEvent[];
  location?: string;
}): string[] {
  const known = new Map(input.npcs.map(npc => [npc.id, npc]));
  const byName = new Map(input.npcs.map(npc => [norm(npc.name), npc]));
  const ids = new Set((input.canonicalIds || []).filter(id => known.has(id)));
  const location = norm(input.location || '');
  const visibleText = norm((input.narration || '') + ' ' + (input.actionResult || ''));

  for (const segment of input.voiceScript || []) {
    if (segment.speakerType === 'narrator') continue;
    if (segment.speakerId && known.has(segment.speakerId)) {
      ids.add(segment.speakerId);
      continue;
    }
    const matched = byName.get(norm(segment.speakerName));
    if (matched) ids.add(matched.id);
  }

  for (const event of input.events || []) {
    if (event.type !== 'npc_encountered') continue;
    const eventId = String(event.data?.npcId || '');
    if (eventId && known.has(eventId)) {
      ids.add(eventId);
      continue;
    }
    const matched = byName.get(norm(event.data?.name));
    if (matched) ids.add(matched.id);
  }

  for (const npc of input.npcs) {
    const name = norm(npc.name);
    if (!name || name === location) continue;
    if (containsWhole(visibleText, name)) ids.add(npc.id);
  }
  return [...ids];
}

export function synchronizeNpcPresenceFlags(npcs: Npc[], presentIds: string[]): Npc[] {
  const present = new Set(presentIds);
  return npcs.map(npc => {
    const next = present.has(npc.id);
    return npc.presentInScene === next ? npc : { ...npc, presentInScene: next, updatedAt: Date.now() };
  });
}
""")

write("lib/sceneVisualPrompt.ts", r"""import type { Npc } from './db';

function compact(value: unknown, max: number): string {
  const text = String(value ?? '').replace(/\s+/g, ' ').trim();
  return text.length <= max ? text : text.slice(0, max - 1).trim() + '…';
}

export function buildStoredSceneVisualPrompt(input: {
  modelPrompt?: string;
  narration?: string;
  actionResult?: string;
  location: string;
  weather?: string;
  timeOfDay?: string;
  presentNpcs?: Npc[];
}): string {
  const modelBrief = compact(input.modelPrompt, 720);
  const fallbackBrief = compact((input.narration || '') + ' ' + (input.actionResult || ''), 620);
  const cast = (input.presentNpcs || []).slice(0, 8).map(npc => {
    const appearance = compact(npc.appearance, 90);
    return npc.name + ' (' + (npc.role || 'NPC') + (appearance ? '; ' + appearance : '') + ')';
  }).join(', ');

  return compact([
    'Horizontal cinematic illustration, 16:9 aspect ratio, The One Ring / Middle-earth atmosphere.',
    'Current location: ' + input.location + '.',
    input.timeOfDay ? 'Time: ' + input.timeOfDay + '.' : '',
    input.weather ? 'Weather: ' + input.weather + '.' : '',
    cast ? 'Characters visibly present: ' + cast + '.' : 'No invented characters.',
    'Scene focus: ' + (modelBrief || fallbackBrief || 'the current established scene') + '.',
    'Keep identities, clothing, mood, geography and action coherent with the established scene.',
    'No written text, captions, interface, collage, split screen or modern objects.',
  ].filter(Boolean).join(' '), 1200);
}

export function buildNpcPortraitPrompt(npc: Npc, sceneContext?: string): string {
  return compact([
    'Create a single-character portrait illustration for The One Ring / Middle-earth.',
    'Composition: chest-up portrait, face prominent and clearly readable, one character only.',
    'Persistent NPC identity: ' + npc.name + '.',
    npc.role ? 'Role: ' + npc.role + '.' : '',
    npc.culture || npc.peoples ? 'Culture/people: ' + [npc.culture, npc.peoples].filter(Boolean).join(' / ') + '.' : '',
    npc.appearance ? 'Appearance and clothing: ' + npc.appearance + '.' : '',
    npc.attitude ? 'Visible attitude: ' + npc.attitude + '.' : '',
    npc.emotionalState || npc.state ? 'Current mood/state: ' + (npc.emotionalState || npc.state) + '.' : '',
    sceneContext ? 'Current narrative context: ' + compact(sceneContext, 220) + '.' : '',
    'Preserve this as one unique individual. Do not add companions, crowds, duplicate faces or other characters.',
    'No text, labels, interface, frame, character sheet or collage.',
  ].filter(Boolean).join(' '), 1200);
}
""")

svc = read("services/geminiService.ts")
old = "- visualPrompt/sceneVisualPrompt: descrição visual em inglês, fiel à cena e sem texto escrito na imagem."
new = """- visualPrompt: descrição visual fiel à cena e sem texto escrito na imagem.
- sceneVisualPrompt: resumo VISUAL CURTO da cena atual para ilustração horizontal 16:9. Inclua somente o local atual, clima/período, ação visual principal e personagens realmente presentes. Não invente figurantes, não use notas secretas e não escreva texto na imagem."""
if old in svc:
    svc = svc.replace(old, new, 1)
svc = svc.replace("imageConfig: { aspectRatio: '9:16' }", "imageConfig: { aspectRatio: '16:9' }", 1)
write("services/geminiService.ts", svc)

fallback = read("lib/autonomy/localFallbackDirector.ts")
fallback = fallback.replace("vertical cinematic scene in Middle-earth", "horizontal 16:9 cinematic scene in Middle-earth", 1)
write("lib/autonomy/localFallbackDirector.ts", fallback)

visualizer = read("components/SceneVisualizer.tsx")
visualizer = visualizer.replace('className="group relative mx-auto aspect-[9/16] w-full max-w-[360px]', 'className="group relative mx-auto aspect-video w-full max-w-full', 1)
visualizer = visualizer.replace("A TERRA-MÉDIA ESPERA", "ILUSTRAÇÃO DA CENA")
visualizer = visualizer.replace("Inicie a crônica para pintar sua jornada...", "Gere uma ilustração 16:9 para esta cena.")
write("components/SceneVisualizer.tsx", visualizer)

assert "'9:16' | '16:9'" in read("lib/db.ts")
assert "aspectRatio: '16:9'" in read("services/geminiService.ts")
assert "aspect-video" in read("components/SceneVisualizer.tsx")
assert "reconcileSceneNpcPresence" in read("lib/sceneEntitySync.ts")
print("PATCH_CORE_498_OK")
