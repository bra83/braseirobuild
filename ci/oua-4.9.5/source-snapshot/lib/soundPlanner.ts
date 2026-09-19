import { SoundItem, SoundKeywordRule } from './db';
import { AudioCue, CampaignEvent, SceneAudioPlan } from '../types';
import { describeSoundName, soundDictionaryTagScore } from './sfxDictionary';

export interface SoundPlan {
  environment: string[];
  ambience?: string;
  ambienceDetails: string[];
  music?: string;
  sfx: SoundEffectEvent[];
  stopPreviousAmbience: boolean;
}

export interface SoundEffectEvent {
  id: string;
  keyword: string;
  charIndex: number;
  matchedPhrase: string;
  aliases: string[];
  priority: number;
  preferredSoundNames?: string[];
  source?: 'ontology' | 'library' | 'explicit';
}

type SceneMode = 'calm' | 'travel' | 'tension' | 'combat';
type Rule = { id: string; phrases: string[]; aliases: string[]; priority: number };
type SfxRule = { id: string; pattern: RegExp; aliases: string[]; priority: number; modes?: SceneMode[] };

interface NormalizedMappedText {
  text: string;
  originalIndex: number[];
}

export function normalizeSoundText(value: unknown): string {
  return String(value ?? '')
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/\.[a-z0-9]{2,5}$/i, '')
    .replace(/[_\-.\\/]+/g, ' ')
    .replace(/[^a-z0-9 ]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function normalizeWithMap(value: string): NormalizedMappedText {
  value = String(value ?? '');
  let text = '';
  const originalIndex: number[] = [];
  let lastWasSpace = true;
  for (let index = 0; index < value.length; index += 1) {
    const expanded = value[index].normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    for (const char of expanded) {
      const output = /[a-z0-9]/.test(char) ? char : ' ';
      if (output === ' ') {
        if (lastWasSpace) continue;
        lastWasSpace = true;
      } else lastWasSpace = false;
      text += output;
      originalIndex.push(index);
    }
  }
  if (text.endsWith(' ')) {
    text = text.slice(0, -1);
    originalIndex.pop();
  }
  return { text, originalIndex };
}

const AMBIENCE_RULES: Rule[] = [
  { id: 'tavern', phrases: ['taverna', 'estalagem', 'taberna', 'sala comum', 'pousada', 'inn', 'tavern'], aliases: ['ambiencia taverna', 'tavern ambience', 'medieval tavern', 'estalagem hospedaria'], priority: 100 },
  { id: 'cave', phrases: ['caverna', 'gruta', 'subterraneo', 'tunel', 'mina', 'moria', 'cave', 'dungeon'], aliases: ['cave ambience', 'damp cave ambience', 'subterraneo com goteiras', 'dungeon ambience'], priority: 98 },
  { id: 'swamp', phrases: ['pantano', 'brejo', 'charco', 'lamacal', 'midgewater', 'swamp', 'marsh'], aliases: ['the swamps', 'sons pantano', 'swamp ambience'], priority: 96 },
  { id: 'forest', phrases: ['floresta', 'bosque', 'mata', 'chetwood', 'floresta velha', 'old forest', 'forest', 'woods'], aliases: ['forest ambience', 'floresta', 'natureza', 'vegetacao'], priority: 92 },
  { id: 'town', phrases: ['cidade', 'vila', 'aldeia', 'mercado', 'bri', 'hobbiton', 'condado', 'shire', 'town', 'village'], aliases: ['town ambience', 'busy medieval town', 'countryside village ambience', 'medieval village atmosphere'], priority: 90 },
  { id: 'river', phrases: ['rio', 'riacho', 'correnteza', 'margem', 'bruinen', 'brandivino', 'brandywine', 'river', 'stream'], aliases: ['river flow', 'calm river', 'water rushing river', 'lake shore'], priority: 88 },
  { id: 'ruins', phrases: ['ruinas', 'fortaleza abandonada', 'salao vazio', 'pedras antigas', 'torre abandonada', 'ruins'], aliases: ['ruins ambience', 'ancient hall', 'dark drone', 'cursed lands', 'cave ambience', 'dungeon ambience', 'night ambience', 'wind calm night loop'], priority: 99 },
  { id: 'road', phrases: ['estrada', 'caminho', 'trilha', 'jornada', 'viajando', 'road', 'path', 'trail'], aliases: ['a walk in nature', 'summer day in nature', 'village on a windy day'], priority: 76 },
  { id: 'rain', phrases: ['chuva', 'garoa', 'temporal', 'gotas no telhado', 'rain', 'drizzle', 'downpour'], aliases: ['rain ambience', 'rain heavy', 'chuva telhado', 'rain with distant thunder'], priority: 95 },
  { id: 'fireplace', phrases: ['lareira', 'fogueira', 'brasas', 'lenha crepitando', 'campfire', 'fireplace'], aliases: ['campfire', 'fire crackle fireplace', 'wood crackling', 'torch fire'], priority: 97 },
  { id: 'wind', phrases: ['vento', 'ventania', 'rajadas', 'brisa fria', 'wind', 'gusts'], aliases: ['wind calm night loop', 'wind through the trees', 'som de vento mais longo'], priority: 72 },
  { id: 'night', phrases: ['noite', 'anoitecer', 'crepusculo', 'sob as estrelas', 'night', 'dusk'], aliases: ['night ambience', 'crickets night', 'forest night crickets'], priority: 66 },
  { id: 'distant-road-traffic', phrases: ['trafego remoto', 'tráfego remoto', 'movimento distante na estrada'], aliases: ['distant road traffic', 'far road ambience', 'distant travellers', 'quiet road traffic'], priority: 74 },
  { id: 'distant-wagons', phrases: ['carrocas distantes', 'carroças distantes', 'rodas ao longe'], aliases: ['distant wagons ambience', 'far wagon traffic', 'distant cart road'], priority: 75 },
  { id: 'distant-settlement', phrases: ['povoado distante', 'cidade ao longe', 'vozes distantes'], aliases: ['distant settlement ambience', 'far medieval village', 'distant town walla'], priority: 73 },
  { id: 'outskirts', phrases: ['arredores da cidade', 'arredores do povoado', 'entrada da cidade'], aliases: ['town outskirts ambience', 'village outskirts', 'quiet medieval outskirts'], priority: 77 },
  { id: 'outskirts-shelter', phrases: ['arredores sob temporal', 'abrigo nos arredores'], aliases: ['sheltered outskirts ambience', 'distant settlement storm shelter'], priority: 76 },
];

const MUSIC_RULES: Rule[] = [
  { id: 'battle', phrases: ['combate', 'batalha', 'luta', 'iniciativa', 'ataque', 'battle', 'combat'], aliases: ['battle music', 'sword battle', 'in the arena'], priority: 100 },
  { id: 'tension', phrases: ['ameaca', 'emboscada', 'perigo', 'silencio opressor', 'inimigos', 'tension', 'danger'], aliases: ['tension music', 'dark drone', 'cursed lands', 'dungeon of sorrow'], priority: 90 },
  { id: 'mystery', phrases: ['misterio', 'segredo', 'runa', 'inscricao', 'voz distante', 'mystery', 'secret'], aliases: ['mystery music', 'crystal mountain secret', 'the secret cave'], priority: 78 },
  { id: 'travel', phrases: ['jornada', 'viagem', 'estrada', 'partida', 'travel', 'road'], aliases: ['travel music', 'a walk in nature', 'basic rpg intro track', 'bardic melody'], priority: 70 },
  { id: 'heroic', phrases: ['esperanca', 'coragem', 'vitoria', 'juramento', 'heroic', 'victory'], aliases: ['heroic music', 'light of a hero shines'], priority: 68 },
  { id: 'calm', phrases: ['descanso', 'paz', 'seguranca', 'refugio', 'calma', 'calm'], aliases: ['calm music', 'calm loop', 'chill rpg theme', 'chill song'], priority: 60 },
];

const SFX_RULES: SfxRule[] = [
  { id: 'door-knock', pattern: /\b(?:batida|batidas|bate|bateu|batem|bateram|pancada|pancadas|knock|knocks|knocking)\b.{0,48}\b(?:porta|portao|madeira|door|gate|wood)\b|\b(?:porta|portao|door|gate)\b.{0,48}\b(?:batida|batidas|bate|bateu|batem|knock|knocks|knocking)\b/g, aliases: ['batida porta madeira', 'door knock', 'knocking on wood', 'wood knock'], priority: 100 },
  { id: 'door-open', pattern: /\b(?:porta|portao|door|gate)\b.{0,36}\b(?:abre|abriu|range|rangeu|open|opened|creak)\b|\b(?:abre|abriu|empurra|empurrou|open|opened)\b.{0,30}\b(?:porta|portao|door|gate)\b/g, aliases: ['wooden door open', 'door opening', 'gate open'], priority: 92 },
  { id: 'door-close', pattern: /\b(?:porta|portao|door|gate)\b.{0,36}\b(?:fecha|fechou|bateu|slam|close|closed)\b|\b(?:fecha|fechou|close|closed)\b.{0,30}\b(?:porta|portao|door|gate)\b/g, aliases: ['door close', 'door slam', 'portao fechar'], priority: 90 },
  { id: 'hammer-anvil', pattern: /\b(?:martelo|hammer|ferreiro|blacksmith)\b.{0,55}\b(?:bigorna|anvil|metal|ferro)\b|\b(?:bigorna|anvil)\b.{0,55}\b(?:martelo|hammer|golpe|impacto)\b/g, aliases: ['martelo bigorna anvil', 'hammer anvil blacksmith', 'metal impact blacksmith'], priority: 99 },
  { id: 'wagon', pattern: /\b(?:carroca|carruagem|wagon|cart)\b|\b(?:cavalo|horse)\b.{0,36}\b(?:carroca|wagon|cart)\b/g, aliases: ['wagon horse wagon carroca', 'cavalo andando carroca'], priority: 96 },
  { id: 'horse', pattern: /\b(?:cavalo|cavalos|montaria|horse|horses)\b.{0,42}\b(?:relincha|relinchou|galopa|galopou|bufa|bufou|neigh|gallop|walk)\b|\b(?:cascos|hoof|hooves)\b.{0,28}\b(?:retumbam|ressoam|galopam|aproximam|afastam|gallop)\b/g, aliases: ['horse neigh', 'horse gallop', 'cavalo relinchando'], priority: 88 },
  { id: 'footsteps-stone', pattern: /\b(?:passos|passo|botas|pes|footsteps|footstep)\b.{0,50}\b(?:pedra|pedras|rocha|rochas|laje|lajes|cascalho|gravel|stone|rocks)\b/g, aliases: ['footsteps stone', 'footsteps gravel', 'passos pedra'], priority: 78 },
  { id: 'footsteps-mud', pattern: /\b(?:passos|passo|botas|pes|footsteps|footstep)\b.{0,50}\b(?:lama|barro|lodo|charco|mud|muddy)\b/g, aliases: ['footsteps mud', 'passos lama'], priority: 80 },
  { id: 'footsteps-dirt', pattern: /\b(?:passos|passo|botas|pes|footsteps|footstep)\b.{0,50}\b(?:terra|solo|trilha|chao|dirt|soil)\b/g, aliases: ['footsteps dirt', 'passos terra'], priority: 74 },
  { id: 'footsteps-grass', pattern: /\b(?:passos|passo|botas|pes|footsteps|footstep)\b.{0,50}\b(?:grama|relva|capim|grass)\b/g, aliases: ['footsteps grass', 'passos grama'], priority: 72 },
  { id: 'footsteps-wood', pattern: /\b(?:passos|passo|botas|pes|footsteps|footstep)\b.{0,50}\b(?:madeira|tabuas|assoalho|wood|wooden)\b/g, aliases: ['footsteps wood', 'passos madeira'], priority: 75 },
  { id: 'footsteps-water', pattern: /\b(?:passos|passo|botas|pes|footsteps|footstep)\b.{0,50}\b(?:agua|riacho|poca|water)\b/g, aliases: ['footsteps water', 'passos agua'], priority: 76 },
  { id: 'armor-hit', pattern: /\b(?:machado|espada|golpe|axe|sword|hit)\b.{0,50}\b(?:armadura|armaduras|cota|cotas|malha|malhas|armor)\b|\b(?:armadura|armaduras|cota|cotas|malha|malhas|armor)\b.{0,50}\b(?:atinge|atingiu|golpe|impacto|hit)\b/g, aliases: ['axe hit armor', 'sword hit armor', 'armor impact'], priority: 98, modes: ['tension', 'combat'] },
  { id: 'armor', pattern: /\b(?:cota|malha|armadura|placas|armor)\b.{0,55}\b(?:soa|soou|range|rangeu|tilinta|tilintou|raspa|raspou|clang|clangs)\b|\b(?:tilintar|ranger|choque|clang)\b.{0,38}\b(?:metal|armadura|malha|armor)\b/g, aliases: ['armor movement', 'clanging metal', 'metal impact'], priority: 86 },
  { id: 'sword-hit', pattern: /\b(?:espada|lamina|sword|blade)\b.{0,46}\b(?:atinge|atingiu|acerta|acertou|golpeia|golpeou|corta|cortou|hit|strike|stab)\b|\b(?:atinge|atingiu|acerta|acertou|golpeia|golpeou|corta|cortou|hit|strike)\b.{0,34}\b(?:espada|lamina|sword|blade)\b/g, aliases: ['sword hit', 'critical sword hit', 'sword hit flesh', 'dagger stab'], priority: 96, modes: ['tension', 'combat'] },
  { id: 'sword-swing', pattern: /\b(?:espada|lamina|sword|blade)\b.{0,42}\b(?:saca|sacou|balanca|corta o ar|whoosh|swing|draw)\b|\b(?:saca|sacou|brandiu|swing|draw)\b.{0,34}\b(?:espada|lamina|sword|blade)\b/g, aliases: ['sword swing', 'sword draw', 'heavy weapon whoosh'], priority: 91, modes: ['tension', 'combat'] },
  { id: 'bow-shot', pattern: /\b(?:arco|corda|flecha|bow|arrow)\b.{0,50}\b(?:dispara|disparou|solta|soltou|vibra|vibrou|voa|shot|shoot|release)\b/g, aliases: ['bow and arrow shot', 'bow fire', 'arrow shot'], priority: 95, modes: ['tension', 'combat'] },
  { id: 'shield-block', pattern: /\b(?:escudo|shield)\b.{0,40}\b(?:bloqueia|bloqueou|aparou|impacto|block|blocked)\b/g, aliases: ['shield block'], priority: 94, modes: ['tension', 'combat'] },
  { id: 'growl', pattern: /\b(?:rosna|rosnou|rosnado|grunhido|growl|growls)\b|\b(?:warg|lobo|wolf|fera)\b.{0,36}\b(?:rosna|rosnou|growl)\b/g, aliases: ['wolf growl', 'monster growl'], priority: 96, modes: ['tension', 'combat'] },
  { id: 'goblin-laugh', pattern: /\b(?:goblin|goblin[s]?)\b.{0,36}\b(?:ri|riu|risada|laugh|laughs)\b/g, aliases: ['goblin laugh'], priority: 90 },
  { id: 'stone-scrape', pattern: /\b(?:pedra|rocha|bloco|stone|rock)\b.{0,42}\b(?:raspa|raspou|move|moveu|desliza|deslizou|cai|caiu|scrape|slides|falls)\b/g, aliases: ['pedra raspando', 'stone scrape', 'rocks tumbling'], priority: 86 },
  { id: 'thunder', pattern: /\b(?:trovao|trovoada|relampago|thunder|lightning)\b.{0,38}\b(?:ribomba|ribombou|explode|explodiu|ressoa|ressoou|rumbles|cracks)\b|\b(?:estrondo|ribombo|rumble)\b.{0,34}\b(?:trovao|ceu|thunder|sky)\b/g, aliases: ['rolling thunder', 'distant thunder', 'lightning'], priority: 92 },
  { id: 'coins', pattern: /\b(?:moeda|moedas|coin|coins)\b.{0,34}\b(?:tilintam|tilintaram|cai|caiu|entrega|entregou|pickup|drop|clink)\b/g, aliases: ['coin pickup', 'coins drop'], priority: 68 },
  { id: 'book', pattern: /\b(?:livro|pagina|paginas|book|page|pages)\b.{0,34}\b(?:abre|abriu|folheia|folheou|vira|virou|open|flip|turn)\b/g, aliases: ['book open', 'book flip page'], priority: 66 },
  { id: 'parchment', pattern: /\b(?:pergaminho|papel antigo|parchment|scroll)\b.{0,36}\b(?:abre|abriu|desenrola|desenrolou|rasga|rasgou|open|unroll|tear)\b/g, aliases: ['scroll unrolling', 'parchment'], priority: 68 },
  { id: 'bell', pattern: /\b(?:sino|sinos|bell|bells)\b.{0,28}\b(?:toca|tocou|soa|soou|ring|rings)\b|\b(?:toque|badalada|ringing)\b.{0,28}\b(?:sino|bell)\b/g, aliases: ['sino metalico', 'bell church tower'], priority: 82 },
  { id: 'cat', pattern: /\b(?:gato|gata|cat)\b.{0,24}\b(?:mia|miou|meow|meows)\b/g, aliases: ['gato meow'], priority: 64 },
  { id: 'rooster', pattern: /\b(?:galo|rooster)\b.{0,24}\b(?:canta|cantou|crow|crows)\b/g, aliases: ['galo'], priority: 64 },
  { id: 'goose', pattern: /\b(?:ganso|goose)\b.{0,24}\b(?:grasna|grasnou|honk|honks)\b/g, aliases: ['ganso'], priority: 62 },
  { id: 'pig', pattern: /\b(?:porco|pig)\b.{0,24}\b(?:grunhe|grunhiu|oink|grunts)\b/g, aliases: ['porco'], priority: 62 },
];

const STRUCTURAL_AMBIENCES = new Set(['tavern', 'forest', 'cave', 'swamp', 'river', 'town', 'ruins', 'road']);
const DISTANT_AMBIENCES = new Set(['distant-road-traffic', 'distant-wagons', 'distant-settlement', 'outskirts', 'outskirts-shelter']);
const DETAIL_AMBIENCES = new Set(['rain', 'fireplace', 'wind', 'night', ...DISTANT_AMBIENCES]);
const GENERIC_LIBRARY_TERMS = new Set([
  'audio', 'sound', 'sounds', 'efeito', 'efeitos', 'sfx', 'ambience', 'ambiencia', 'ambient', 'loop', 'loopable',
  'impact', 'impacto', 'light', 'heavy', 'soft', 'small', 'large', 'main', 'public', 'place', 'misc', 'object', 'objects',
  'metal', 'wood', 'wooden', 'madeira', 'porta', 'door', 'doors', 'pass', 'passagem', 'people', 'pessoas', 'male', 'female',
  'contra', 'against', 'interior', 'indoors', 'ranged', 'medio', 'alcance', 'very', 'short', 'number', 'take', 'one', 'two',
]);
const DISTINCTIVE_SINGLETONS = new Set([
  'carroca', 'wagon', 'bigorna', 'anvil', 'martelo', 'hammer', 'trovao', 'thunder', 'relampago', 'lightning',
  'galo', 'rooster', 'ganso', 'goose', 'porco', 'pig', 'gato', 'meow', 'sino', 'bell', 'pergaminho', 'parchment',
  'moedas', 'coins', 'relincha', 'neigh', 'rosna', 'growl', 'rugido', 'roar', 'goblin', 'zombie', 'skeleton', 'aranha', 'spider',
]);

function firstMatchingRule(text: string, rules: Rule[], allow?: (rule: Rule, index: number) => boolean): Rule | undefined {
  const normalized = normalizeSoundText(text);
  const matches: Array<{ rule: Rule; index: number }> = [];
  for (const rule of rules) {
    for (const phrase of rule.phrases) {
      const index = normalized.indexOf(normalizeSoundText(phrase));
      if (index >= 0 && (!allow || allow(rule, index))) matches.push({ rule, index });
    }
  }
  return matches.sort((a, b) => b.rule.priority - a.rule.priority || a.index - b.index)[0]?.rule;
}

function isRetrospectiveOrNegated(text: string, index: number): boolean {
  const before = text.slice(Math.max(0, index - 120), index);
  const immediate = text.slice(Math.max(0, index - 58), index);
  const after = text.slice(index, Math.min(text.length, index + 110));
  return /\b(?:nao|nunca|nada|sem|nenhum|nenhuma|nothing|never|without|no sound|did not|didnt|lembrava|recordava|contava|falava|disse que|contou que|ouviu falar|historia de|vestigios de|marcas de|depois da|apos a|antiga|antigo|silencio de|esperava ouvir|esperou ouvir|expected to hear|would hear)\b/.test(before)
    || /\b(?:se|caso|talvez|poderia|deveria|iria|imaginava|maybe|perhaps|could|would)\b/.test(immediate)
    || /\b(?:nao (?:veio|soou|aconteceu|ocorreu)|nada (?:veio|soou|aconteceu)|did not (?:come|sound|happen)|nothing (?:came|sounded|happened)|never (?:came|sounded|happened))\b/.test(after);
}

function inferSceneMode(location: string, narrative: string, action: string, events: CampaignEvent[] = []): SceneMode {
  const text = normalizeSoundText(`${location} ${narrative} ${action}`);
  const eventTypes = events.map(event => event.type);
  const lastCombatStart = eventTypes.lastIndexOf('combat_started');
  const lastCombatEnd = eventTypes.lastIndexOf('combat_ended');
  if (lastCombatStart >= 0 && lastCombatStart > lastCombatEnd) return 'combat';
  if (/\b(?:combate comeca|luta comeca|iniciativa|atacam|ataca|golpeia|golpeou|atinge|atingiu|acerta|acertou|batalha)\b/.test(text)) return 'combat';
  if (/\b(?:emboscada|ameaca|perigo|perseguicao|inimigos se aproximam|silencio opressor)\b/.test(text)) return 'tension';
  if (/\b(?:jornada|viagem|estrada|caminho|trilha|partida|marcham|cavalgam)\b/.test(text)) return 'travel';
  return 'calm';
}

function mergeAliases(id: string, explicitAliases: string[] = []): string[] {
  const rule = SFX_RULES.find(item => item.id === id);
  return Array.from(new Set([...(rule?.aliases || []), ...explicitAliases].map(normalizeSoundText).filter(Boolean)));
}

function explicitCueIsGrounded(cue: AudioCue, normalizedText: string): boolean {
  const trigger = normalizeSoundText(cue.triggerText || '');
  if (!trigger || trigger.length < 3) return false;
  const index = normalizedText.indexOf(trigger);
  return index >= 0 && !isRetrospectiveOrNegated(normalizedText, index);
}

function ontologyEvents(text: string, mode: SceneMode): SoundEffectEvent[] {
  const mapped = normalizeWithMap(text);
  const events: SoundEffectEvent[] = [];
  for (const rule of SFX_RULES) {
    if (rule.modes && !rule.modes.includes(mode)) continue;
    const regex = new RegExp(rule.pattern.source, rule.pattern.flags.includes('g') ? rule.pattern.flags : `${rule.pattern.flags}g`);
    for (const match of mapped.text.matchAll(regex)) {
      const normalizedIndex = match.index || 0;
      if (isRetrospectiveOrNegated(mapped.text, normalizedIndex)) continue;
      const originalIndex = mapped.originalIndex[normalizedIndex] ?? normalizedIndex;
      events.push({
        id: rule.id,
        keyword: rule.id,
        charIndex: originalIndex,
        matchedPhrase: text.slice(originalIndex, Math.min(text.length, originalIndex + Math.max(12, match[0].length))),
        aliases: rule.aliases,
        priority: rule.priority,
        source: 'ontology',
      });
    }
  }
  return events;
}

function explicitEvents(text: string, explicit: AudioCue[]): SoundEffectEvent[] {
  const mapped = normalizeWithMap(text);
  const events: SoundEffectEvent[] = [];
  for (const cue of explicit) {
    const id = normalizeSoundText(cue.keyword).replace(/ /g, '-');
    if (!id || !explicitCueIsGrounded(cue, mapped.text)) continue;
    const trigger = normalizeSoundText(cue.triggerText || '');
    const normalizedIndex = mapped.text.indexOf(trigger);
    const originalIndex = mapped.originalIndex[normalizedIndex] ?? normalizedIndex;
    events.push({
      id,
      keyword: id,
      charIndex: originalIndex,
      matchedPhrase: cue.triggerText || id,
      aliases: mergeAliases(id, cue.aliases),
      priority: Math.min(100, Math.max(1, cue.priority || 80)),
      source: 'explicit',
    });
  }
  return events;
}

function soundRuleFor(sound: SoundItem, rules: SoundKeywordRule[]): SoundKeywordRule | undefined {
  const name = normalizeSoundText(sound.name);
  return rules.find(rule => name === rule.normalizedSoundName || name.includes(rule.normalizedSoundName) || rule.normalizedSoundName.includes(name));
}

function sentenceWindow(mapped: NormalizedMappedText, normalizedIndex: number): { start: number; end: number; text: string } {
  let start = normalizedIndex;
  let end = normalizedIndex;
  while (start > 0 && !/[.!?\n]/.test(mapped.text[start - 1])) start -= 1;
  while (end < mapped.text.length && !/[.!?\n]/.test(mapped.text[end])) end += 1;
  return { start, end, text: mapped.text.slice(start, end) };
}

function libraryEvents(text: string, sounds: SoundItem[], rules: SoundKeywordRule[]): SoundEffectEvent[] {
  if (!sounds.length || !rules.length) return [];
  const mapped = normalizeWithMap(text);
  const candidates: Array<SoundEffectEvent & { score: number }> = [];

  for (const sound of sounds) {
    if (inferChannel(sound) !== 'sfx') continue;
    const rule = soundRuleFor(sound, rules);
    if (!rule) continue;
    const rawTerms = Array.from(new Set([
      ...rule.keywords,
      ...rule.aliases,
      ...rule.tags,
      ...normalizeSoundText(sound.name).split(' '),
    ].map(normalizeSoundText).filter(Boolean)));
    const phrases = rawTerms.filter(term => term.includes(' ') && term.length >= 5);
    const tokens = Array.from(new Set(rawTerms.flatMap(term => term.split(' '))))
      .filter(token => token.length >= 4 && !GENERIC_LIBRARY_TERMS.has(token) && !/^\d+$/.test(token));

    let bestIndex = -1;
    let bestScore = 0;
    let matchedTerms: string[] = [];
    for (const phrase of phrases) {
      const index = mapped.text.indexOf(phrase);
      if (index >= 0 && !isRetrospectiveOrNegated(mapped.text, index)) {
        const score = 115 + Math.min(40, phrase.length);
        if (score > bestScore) { bestScore = score; bestIndex = index; matchedTerms = [phrase]; }
      }
    }
    const tokenOccurrences = tokens
      .map(token => ({ token, index: mapped.text.search(new RegExp(`\\b${token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`)) }))
      .filter(item => item.index >= 0 && !isRetrospectiveOrNegated(mapped.text, item.index));
    for (const occurrence of tokenOccurrences) {
      const window = sentenceWindow(mapped, occurrence.index);
      const inWindow = tokenOccurrences.filter(item => item.index >= window.start && item.index <= window.end);
      const distinct = Array.from(new Set(inWindow.map(item => item.token)));
      const hasDistinctive = DISTINCTIVE_SINGLETONS.has(occurrence.token);
      const score = distinct.length >= 2 ? 78 + distinct.length * 18 : hasDistinctive ? 92 : 0;
      if (score > bestScore) {
        bestScore = score;
        bestIndex = Math.min(...inWindow.map(item => item.index));
        matchedTerms = distinct;
      }
    }
    if (bestIndex < 0 || bestScore < 78) continue;
    const originalIndex = mapped.originalIndex[bestIndex] ?? bestIndex;
    candidates.push({
      id: `library-${normalizeSoundText(sound.name).replace(/ /g, '-').slice(0, 72)}`,
      keyword: matchedTerms.join(' ') || normalizeSoundText(sound.name),
      charIndex: originalIndex,
      matchedPhrase: text.slice(originalIndex, Math.min(text.length, originalIndex + 70)),
      aliases: Array.from(new Set([...matchedTerms, ...rule.keywords, ...rule.aliases, sound.name].map(normalizeSoundText).filter(Boolean))),
      priority: Math.min(95, 55 + Math.round(bestScore / 3)),
      preferredSoundNames: [rule.soundName, sound.name],
      source: 'library',
      score: bestScore,
    });
  }
  return candidates.sort((a, b) => b.score - a.score || a.charIndex - b.charIndex);
}

function semanticOverlap(a: SoundEffectEvent, b: SoundEffectEvent): boolean {
  const left = new Set([a.keyword, ...a.aliases].flatMap(value => normalizeSoundText(value).split(' ')).filter(token => token.length > 3));
  const right = new Set([b.keyword, ...b.aliases].flatMap(value => normalizeSoundText(value).split(' ')).filter(token => token.length > 3));
  return [...left].some(token => right.has(token));
}

export function planSfxEvents(
  text: string,
  maxEvents = 16,
  explicit: AudioCue[] = [],
  forcedMode?: SceneMode,
  sounds: SoundItem[] = [],
  keywordRules: SoundKeywordRule[] = [],
): SoundEffectEvent[] {
  const mode = forcedMode || inferSceneMode('', text, '');
  const candidates = [...ontologyEvents(text, mode), ...explicitEvents(text, explicit), ...libraryEvents(text, sounds, keywordRules)]
    .sort((a, b) => a.charIndex - b.charIndex || b.priority - a.priority);
  const selected: SoundEffectEvent[] = [];
  for (const candidate of candidates) {
    const near = selected.find(item => Math.abs(item.charIndex - candidate.charIndex) < 70 && semanticOverlap(item, candidate));
    if (near) {
      near.priority = Math.max(near.priority, candidate.priority);
      near.aliases = Array.from(new Set([...near.aliases, ...candidate.aliases]));
      near.preferredSoundNames = Array.from(new Set([...(near.preferredSoundNames || []), ...(candidate.preferredSoundNames || [])]));
      continue;
    }
    const duplicate = selected.find(item => item.id === candidate.id && Math.abs(item.charIndex - candidate.charIndex) < 120);
    if (duplicate) continue;
    selected.push(candidate);
    if (selected.length >= Math.max(0, maxEvents)) break;
  }
  return selected.sort((a, b) => a.charIndex - b.charIndex);
}

function activeAmbienceMatch(text: string, rule: Rule): boolean {
  const normalized = normalizeSoundText(text);
  return rule.phrases.some(phrase => {
    const index = normalized.indexOf(normalizeSoundText(phrase));
    if (index < 0) return false;
    const before = normalized.slice(Math.max(0, index - 60), index);
    return !/\b(?:deixa|deixou|sai|saiu|partiu de|afastou se de|longe de|recorda|lembra)\b/.test(before);
  });
}

function canonicalExplicit(values: string[] | undefined, allowed: Set<string>): string[] {
  return (values || []).map(normalizeSoundText).filter(value => allowed.has(value));
}

export function planSounds(
  location: string,
  narrative: string,
  action: string,
  explicit?: SceneAudioPlan,
  events: CampaignEvent[] = [],
  sounds: SoundItem[] = [],
  keywordRules: SoundKeywordRule[] = [],
  forcedBaseAmbience?: string,
): SoundPlan {
  const mode = inferSceneMode(location, narrative, action, events);
  const locationRule = firstMatchingRule(location, AMBIENCE_RULES, rule => STRUCTURAL_AMBIENCES.has(rule.id));
  const narrativeRule = firstMatchingRule(narrative, AMBIENCE_RULES, rule => STRUCTURAL_AMBIENCES.has(rule.id) && activeAmbienceMatch(narrative, rule));
  const explicitStructural = canonicalExplicit(explicit?.ambience, STRUCTURAL_AMBIENCES)
    .find(id => {
      const rule = AMBIENCE_RULES.find(item => item.id === id);
      return Boolean(rule && activeAmbienceMatch(`${location} ${narrative}`, rule));
    });
  const canonicalForced = forcedBaseAmbience && STRUCTURAL_AMBIENCES.has(normalizeSoundText(forcedBaseAmbience))
    ? normalizeSoundText(forcedBaseAmbience)
    : undefined;
  // A posição cartográfica/estado formal da cena tem prioridade sobre nomes
  // soltos da narração. Assim, uma lembrança de taverna nunca substitui ruínas.
  const base = canonicalForced || locationRule?.id || narrativeRule?.id || explicitStructural || (mode === 'travel' ? 'road' : undefined);

  const details = new Set<string>();
  for (const detail of canonicalExplicit([...(explicit?.ambience || []), ...(explicit?.ambienceDetails || [])], DETAIL_AMBIENCES)) {
    const rule = AMBIENCE_RULES.find(item => item.id === detail);
    // Prenúncios cartográficos são produzidos pelo núcleo local e podem entrar
    // como ambiência distante mesmo antes de serem citados literalmente.
    if (rule && (DISTANT_AMBIENCES.has(detail) || activeAmbienceMatch(`${location} ${narrative}`, rule))) details.add(detail);
  }
  for (const rule of AMBIENCE_RULES) {
    if (DETAIL_AMBIENCES.has(rule.id) && activeAmbienceMatch(`${location} ${narrative}`, rule)) details.add(rule.id);
  }

  const text = `${location} ${narrative} ${action}`;
  let music: string;
  if (mode === 'combat') music = 'battle';
  else if (mode === 'tension') music = 'tension';
  else if (mode === 'travel') music = 'travel';
  else music = firstMatchingRule(text, MUSIC_RULES)?.id || (/\b(?:runa|segredo|misterio|antigo|ruinas)\b/.test(normalizeSoundText(text)) ? 'mystery' : 'calm');
  const explicitMusic = normalizeSoundText(explicit?.musicMood?.[0] || '');
  if (['battle', 'tension', 'travel', 'mystery', 'heroic', 'calm'].includes(explicitMusic)) {
    if (!(mode === 'calm' && ['battle', 'tension'].includes(explicitMusic))) music = explicitMusic;
  }

  const sfx = planSfxEvents(narrative, 16, explicit?.sfx || [], mode, sounds, keywordRules);
  return {
    environment: Array.from(new Set([base || '', ...details].filter(Boolean))),
    ambience: base,
    ambienceDetails: [...details]
      .filter(id => id !== base)
      .sort((a, b) => (AMBIENCE_RULES.find(rule => rule.id === b)?.priority || 0) - (AMBIENCE_RULES.find(rule => rule.id === a)?.priority || 0))
      .slice(0, 4),
    music,
    sfx,
    stopPreviousAmbience: explicit?.stopPreviousAmbience ?? Boolean(base),
  };
}

export function inferChannel(sound: SoundItem): 'ambience' | 'music' | 'sfx' {
  const dictionary = describeSoundName(sound.name);
  if (dictionary.known) return dictionary.channel;
  const text = normalizeSoundText(`${sound.name} ${(sound.tags || []).join(' ')} ${sound.category} ${sound.channel}`);
  if (sound.channel === 'music' || /(^| )(bgm|music|theme|song|melody|bardic|track|trilha|musica)( |$)/.test(text)) return 'music';
  if (sound.channel === 'ambience' || /(^| )(amb|ambience|ambient|forest|floresta|tavern|estalagem|fireplace|campfire|rain|chuva|wind|vento|cave|swamp|river|night|walla|town)( |$)/.test(text)) return 'ambience';
  return 'sfx';
}

function hasRequiredEventTag(sound: SoundItem, query: string): boolean {
  const tags = new Set([...describeSoundName(sound.name).tags, ...(sound.tags || []), ...normalizeSoundText(sound.name).split(' ')].map(normalizeSoundText));
  const groups: Record<string, string[]> = {
    horse: ['horse', 'cavalo', 'neigh', 'relincha', 'gallop'],
    wagon: ['wagon', 'carroca', 'carruagem'],
    'hammer-anvil': ['hammer', 'martelo', 'anvil', 'bigorna', 'blacksmith', 'ferreiro'],
    'sword-hit': ['sword', 'espada', 'hit', 'critical', 'flesh', 'stab'],
    'sword-swing': ['sword', 'espada', 'swing', 'whoosh', 'draw'],
    'bow-shot': ['bow', 'arco', 'arrow', 'flecha', 'shot'],
    'armor-hit': ['armor', 'armadura', 'axe', 'machado', 'sword', 'espada', 'hit'],
    armor: ['armor', 'armadura', 'metal', 'malha'],
    'door-knock': ['door', 'porta', 'knock', 'batida', 'bater'],
    'door-open': ['door', 'porta', 'open', 'abre'],
    'door-close': ['door', 'porta', 'close', 'fecha', 'slam'],
    growl: ['growl', 'rosna', 'wolf', 'lobo'],
    'footsteps-stone': ['footstep', 'footsteps', 'passos', 'stone', 'pedra', 'gravel'],
    'footsteps-mud': ['footstep', 'footsteps', 'passos', 'mud', 'lama'],
    'footsteps-dirt': ['footstep', 'footsteps', 'passos', 'dirt', 'terra'],
    'footsteps-grass': ['footstep', 'footsteps', 'passos', 'grass', 'grama'],
    'footsteps-wood': ['footstep', 'footsteps', 'passos', 'wood', 'madeira'],
    'footsteps-water': ['footstep', 'footsteps', 'passos', 'water', 'agua'],
    'stone-scrape': ['stone', 'pedra', 'scrape', 'raspando', 'rocks'],
    thunder: ['thunder', 'trovao', 'lightning', 'relampago'],
    coins: ['coin', 'coins', 'moeda', 'moedas'],
    book: ['book', 'livro', 'page', 'pagina'],
    parchment: ['parchment', 'pergaminho', 'scroll'],
    bell: ['bell', 'sino'],
    cat: ['cat', 'gato', 'meow'],
    rooster: ['rooster', 'galo'],
    goose: ['goose', 'ganso'],
    pig: ['pig', 'porco'],
  };
  const wanted = groups[normalizeSoundText(query).replace(/ /g, '-')];
  if (!wanted) return true;
  const matches = wanted.filter(tag => tags.has(normalizeSoundText(tag)) || [...tags].some(value => value.includes(normalizeSoundText(tag))));
  return matches.length >= (wanted.length >= 4 ? 1 : 1);
}

const AMBIENCE_EVENT_WORDS = /\b(?:battle|combat|fight|attack|sword|weapon|batalha|combate|luta|espada|golpe|gallop|neigh|horse|cavalo|chicken|rooster|galinha|porco|pig|scream|grito)\b/;

function querySemanticTags(query: string, aliases: string[]): Set<string> {
  const requested = new Set<string>();
  for (const value of [query, ...aliases]) {
    const normalized = normalizeSoundText(value);
    if (!normalized) continue;
    requested.add(normalized.replace(/-/g, ' '));
    for (const token of normalized.split(' ')) if (token.length > 2) requested.add(token);
  }
  return requested;
}

function isUnsafeBaseAmbience(haystack: string, requested: Set<string>): boolean {
  if (AMBIENCE_EVENT_WORDS.test(haystack) && ![...requested].some(tag => AMBIENCE_EVENT_WORDS.test(tag))) return true;
  if (/\b(?:agitada|agitado|agitated|cheering|panic|screaming|correria|tumulto)\b/.test(haystack) && !requested.has('crowd')) return true;
  return false;
}

function preferredNameMatch(sound: SoundItem, preferred: string[]): number {
  const name = normalizeSoundText(sound.name);
  let score = 0;
  for (const value of preferred.map(normalizeSoundText).filter(Boolean)) {
    if (name === value) score = Math.max(score, 500);
    else if (name.includes(value) || value.includes(name)) score = Math.max(score, 360);
  }
  return score;
}

export function findBestSoundForQuery(
  sounds: SoundItem[],
  channel: 'ambience' | 'music' | 'sfx',
  query: string,
  extraAliases: string[] = [],
  preferredSoundNames: string[] = [],
): SoundItem | null {
  const aliases = Array.from(new Set([query, ...extraAliases].map(normalizeSoundText).filter(Boolean)));
  const requestedTags = querySemanticTags(query, aliases);
  let best: { item: SoundItem; score: number } | null = null;

  for (const sound of sounds) {
    const inferred = inferChannel(sound);
    if (inferred !== channel && sound.channel !== channel) continue;
    if (channel === 'sfx' && !hasRequiredEventTag(sound, query)) continue;
    const dictionary = describeSoundName(sound.name);
    const haystack = normalizeSoundText(`${sound.name} ${(sound.tags || []).join(' ')} ${sound.category} ${dictionary.tags.join(' ')}`);
    if (channel === 'ambience' && isUnsafeBaseAmbience(haystack, requestedTags)) continue;
    if (channel === 'music') {
      const mood = normalizeSoundText(query);
      if (mood !== 'battle' && /\b(?:battle|combat|arena|batalha|combate)\b/.test(haystack)) continue;
      if (mood === 'battle' && !/\b(?:battle|combat|arena|batalha|combate|fight)\b/.test(haystack)) continue;
    }

    let score = preferredNameMatch(sound, preferredSoundNames);
    score += soundDictionaryTagScore(sound.name, [query, ...extraAliases]);
    let directMatch = score >= 300;
    for (const alias of aliases) {
      if (haystack === alias) { score += 180; directMatch = true; }
      else if (haystack.includes(alias)) { score += 95 + Math.min(35, alias.length); directMatch = true; }
      const tokens = alias.split(' ').filter(token => token.length > 2);
      const matched = tokens.filter(token => haystack.includes(token)).length;
      score += matched * 16;
      if (tokens.length > 1 && matched >= Math.ceil(tokens.length * .65)) { score += 45; directMatch = true; }
    }
    const tagMatch = [...requestedTags].some(tag => haystack.includes(tag));
    if (!directMatch && !tagMatch) {
      if (channel === 'music' && inferred === 'music') score += 32;
      else continue;
    }

    const size = Math.max(0, Number(sound.size) || 0);
    if (channel === 'ambience') {
      if (dictionary.loopHint || dictionary.durationHint === 'long') score += 80;
      if (dictionary.durationHint === 'short') score -= 100;
      if (size >= 2_000_000) score += 18;
    } else if (channel === 'sfx') {
      if (dictionary.durationHint === 'long' || dictionary.loopHint) score -= 80;
      if (dictionary.durationHint === 'short') score += 30;
      if (size > 0 && size < 5_000_000) score += 10;
    } else {
      if (dictionary.durationHint === 'long' || dictionary.loopHint) score += 28;
      if (dictionary.durationHint === 'short') score -= 45;
    }
    if (!best || score > best.score) best = { item: sound, score };
  }

  const threshold = channel === 'music' ? 24 : channel === 'ambience' ? 72 : 70;
  return best && best.score >= threshold ? best.item : null;
}

export function findBestSoundForEvent(sounds: SoundItem[], event: SoundEffectEvent): SoundItem | null {
  return findBestSoundForQuery(sounds, 'sfx', event.keyword, event.aliases, event.preferredSoundNames || []);
}


export function ambienceSearchAliases(id: string): string[] {
  const normalized = normalizeSoundText(id);
  return AMBIENCE_RULES.find(rule => rule.id === normalized)?.aliases || [normalized];
}

export function musicSearchAliases(id: string): string[] {
  const normalized = normalizeSoundText(id);
  return MUSIC_RULES.find(rule => rule.id === normalized)?.aliases || [normalized];
}
