export type SkillAttribute = 'strength' | 'heart' | 'wits';

export const CANONICAL_SKILLS = [
  'Fascínio','Atletismo','Vigilância','Caçada','Música','Ofício',
  'Indução','Viagem','Discernimento','Cura','Cortesia','Batalha',
  'Persuasão','Furtividade','Busca','Exploração','Enigma','História',
] as const;
export type CanonicalSkillName = typeof CANONICAL_SKILLS[number];

const SKILL_ATTRIBUTE: Record<CanonicalSkillName, SkillAttribute> = {
  'Fascínio':'strength','Atletismo':'strength','Vigilância':'strength','Caçada':'strength','Música':'strength','Ofício':'strength',
  'Indução':'heart','Viagem':'heart','Discernimento':'heart','Cura':'heart','Cortesia':'heart','Batalha':'heart',
  'Persuasão':'wits','Furtividade':'wits','Busca':'wits','Exploração':'wits','Enigma':'wits','História':'wits',
};

function norm(v: unknown): string { return String(v ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim(); }
const ALIASES: Record<string, CanonicalSkillName> = {
  imponencia:'Fascínio', fascinio:'Fascínio', awe:'Fascínio',
  atletismo:'Atletismo', athletics:'Atletismo',
  percepcao:'Vigilância', vigilancia:'Vigilância', awareness:'Vigilância',
  caca:'Caçada', cacada:'Caçada', hunting:'Caçada',
  canto:'Música', musica:'Música', song:'Música',
  artesanato:'Ofício', oficio:'Ofício', craft:'Ofício',
  inspiracao:'Indução', inducao:'Indução', enhearten:'Indução',
  viagem:'Viagem', travel:'Viagem',
  intuicao:'Discernimento', discernimento:'Discernimento', insight:'Discernimento',
  cura:'Cura', healing:'Cura',
  cortesias:'Cortesia', cortesia:'Cortesia', courtesy:'Cortesia',
  batalha:'Batalha', battle:'Batalha',
  persuasao:'Persuasão', persuade:'Persuasão',
  furtividade:'Furtividade', stealth:'Furtividade',
  busca:'Busca', scan:'Busca',
  exploracao:'Exploração', explore:'Exploração',
  enigma:'Enigma', riddle:'Enigma',
  conhecimento:'História', historia:'História', lore:'História',
};
export function canonicalSkillName(name: unknown): CanonicalSkillName | null { return ALIASES[norm(name)] || null; }
export function attributeForCanonicalSkill(name: unknown): SkillAttribute { const c=canonicalSkillName(name); return c ? SKILL_ATTRIBUTE[c] : 'wits'; }
export function skillAliasesFor(name: CanonicalSkillName): string[] { return Object.entries(ALIASES).filter(([,v])=>v===name).map(([k])=>k); }
