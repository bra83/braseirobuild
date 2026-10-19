/** Automatic vocal direction for Braseiro O Um Anel.
 * Audio-only: NEVER write the augmented transcript into scene persistence or UI.
 * The transport removes inline tags on legacy Gemini TTS models.
 */
export type VocalTurn = { transcript: string; style: string; mood: string; tags: string[] };
const allowed = /<(?:short pause|breath|sigh|laugh|cough)>/gi;
export function stripVocalTags(input: string): string {
  return String(input || '').replace(allowed, '');
}
const delivery: Record<string,string> = {
  fear: 'lower, contained fear, carefully paced, never melodramatic',
  suspense: 'quiet, restrained suspense, slowing slightly at revelations',
  urgent: 'alert and urgent, quicker but clearly articulated',
  anger: 'restrained anger, firm emphasis, not continuously shouting',
  grief: 'soft restrained sadness, warm and thoughtful',
  joy: 'warmth and genuine light amusement, lively natural cadence',
  solemn: 'solemn deliberate storytelling, respectful tone',
  neutral: 'natural human pacing and phrasing, subtle expression',
};
function moodFor(text: string, emotion: string, context: string): string {
  const hint = (emotion + ' ' + context).toLocaleLowerCase('pt-BR');
  if (/medo|assust|pavor|fear|afraid|terror/.test(hint)) return 'fear';
  if (/urgên|urgenc|persegui|corrend|fug|combate|batalha|urgent/.test(hint)) return 'urgent';
  if (/ira|raiva|enfurec|anger|furious/.test(hint)) return 'anger';
  if (/triste|luto|melancol|sad|sorrow/.test(hint)) return 'grief';
  if (/alegr|feliz|júbilo|joy|happy/.test(hint)) return 'joy';
  if (/solen|ritual|reverên|solemn/.test(hint)) return 'solemn';
  if (/suspens|tens|ameaç|sombr|mistér|enigma|mystery/.test(hint)) return 'suspense';
  if (/estremeceu|sussurrou|espreit|escuridão|arrepio|silêncio profundo/i.test(text)) return 'suspense';
  return 'neutral';
}
/** Sparse, evidence-led events; original prose is bit-for-bit recoverable after removing tags. */
export function prepareBraseiroVocalTurn(
  text: string, baseStyle: string, emotion = '', context = '', speakerType = 'narrator'
): VocalTurn {
  const original = String(text || '');
  const mood = moodFor(original, emotion, context);
  let transcript = stripVocalTags(original);
  const tags: string[] = [];
  const insert = (pattern: RegExp, tag: string) => {
    const updated = transcript.replace(pattern, (match) => match + tag);
    if (updated !== transcript) { transcript = updated; tags.push(tag); }
  };
  insert(/\b(?:ela|ele|o homem|a mulher|o anão|a anã|o elfo|a elfa|você)\s+(?:suspirou|soluçou)\b[.!?]?/i, '<sigh>');
  if (!tags.length) insert(/\b(?:ela|ele|você)\s+(?:respirou fundo|tomou fôlego|prendeu a respiração)\b[.!?]?/i, '<breath>');
  if (!tags.length) insert(/\b(?:ela|ele|você)\s+(?:riu|deu uma risada|gargalhou)\b[.!?]?/i, '<laugh>');
  if (!tags.length) insert(/(?:\.\.\.|…)(?!\s*<)/, '<short pause>');
  if (!tags.length && original.length >= 140 && /^(?:fear|suspense)$/.test(mood)) {
    insert(/[.!?](?=\s+\S)/, '<short pause>');
  }
  if (!tags.length && speakerType !== 'narrator' && original.length >= 90 && mood === 'grief') {
    insert(/[.!?](?=\s+\S)/, '<sigh>');
  }
  return {
    transcript, mood, tags,
    style: [String(baseStyle || '').trim(), delivery[mood]].filter(Boolean).join('; ').slice(0,1100),
  };
}
