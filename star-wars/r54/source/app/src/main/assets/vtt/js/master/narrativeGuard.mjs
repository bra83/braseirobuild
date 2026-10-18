/* Narrative Guard & Immersive Human Master Service — Star Wars VTT */

export const NARRATIVE_BLACKLIST_PATTERNS = Object.freeze([
  /\b(?:como uma ia|como inteligência artificial|assistente virtual|prompt|pipeline|sistema de ia)\b/gi,
  /\b(?:a cena responde|antes que qualquer explicação|a cena não pede uma resposta|o sistema calculou)\b/gi,
  /\b(?:você decide|você sente medo|você fica impressionado|você odeia|você se apaixona)\b/gi,
  /\b(?:opções disponíveis|você pode:|ações sugeridas:|status:|local atual:|clima atualizado:)\b/gi,
  /\b(?:hex\s*\d+|actor_\d+|loc_[a-z0-9_]+|npc_[a-z0-9_]+)\b/gi
]);

export class NarrativeGuardService {
  /**
   * Limpa metalinguagem de software, rótulos de interface e muletas da prosa visível.
   */
  static sanitize(text) {
    let clean = String(text || '').replace(/\r\n/g, '\n');

    // Remove rótulos de interface que vazem para a prosa narrativa
    clean = clean.replace(/^\s*(?:local atual|clima(?: atualizado)?|horário|período|objetivo|status|hex|coordenada)\s*:\s*.*$/gim, '');
    clean = clean.replace(/^\s*(?:opções disponíveis|você pode|ações sugeridas)\s*:\s*$/gim, '');

    // Remove metalinguagem de IA e software
    clean = clean.replace(/\b(?:a cena responde|antes que qualquer explicação|a cena não pede uma resposta|o sistema calculou)[^.!?]*[.!?]/gi, '');
    clean = clean.replace(/\b(?:como uma (?:ia|inteligência artificial|assistente virtual))\b[^.!?]*[.!?]/gi, '');

    // Elimina muleta repetitiva de final de turno "O que você faz?"
    clean = clean.replace(/\s*(?:e agora[?,]?\s*)?[Oo] que voc[êe](?:s)? faz(?:em)?[?]?\s*$/gi, '');

    // Normaliza quebras de linha múltiplas
    return clean.replace(/\n{3,}/g, '\n\n').trim();
  }

  /**
   * Valida se a prosa segue as diretrizes do Mestre Humano (5 Pilares).
   */
  static validate(text) {
    const errors = [];
    const lower = String(text || '').toLowerCase();

    if (/\b(?:como uma (?:ia|inteligência artificial)|assistente virtual|prompt|pipeline|modelo json)\b/i.test(lower)) {
      errors.push('Metalinguagem de IA detectada na narração.');
    }
    if (/\b(?:você sente medo|você fica impressionado|você odeia)\b/i.test(lower)) {
      errors.push('Sequestro de emoção/reação do personagem detectado.');
    }
    if (/\b(?:o que você faz\?|o que vocês fazem\?)\b/i.test(lower)) {
      errors.push('Muleta genérica de encerramento detectada.');
    }

    return {
      valid: errors.length === 0,
      errors
    };
  }
}

/**
 * Fallback Local Imersivo Star Wars (Sem dependência remota de IA).
 * Constrói uma cena cinematográfica de alta atmosfera fundamentada no estado real.
 */
export function generateStarWarsImmersiveFallback({ character, location, situation } = {}) {
  const name = character?.name || 'Seu personagem';
  const species = character?.species || 'humano';
  const locName = location?.name || location?.title || 'setor desconhecido da Orla Exterior';
  const armor = character?.armor?.[0]?.name || 'colete acolchoado e vestes reforçadas';
  const weapon = character?.weapons?.[0]?.name || 'pistola blaster no coldre';

  const introSensory = [
    `O ar de ${locName} carrega o cheiro denso de combustível queimado, ozônio residual de disparos e poeira industrial suspensa na penumbra.`,
    `A iluminação oscila em tons alaranjados, refletindo nas chapas gastas de durasteel e nos consoles secundários que zumbem com estática contínua.`
  ].join(' ');

  const characterAnchor = `${name}, ajustando as correias de seu ${armor}, sente o peso reconfortante da ${weapon} pronta ao alcance dos dedos. Sendo um ${species} acostumado às intempéries da galáxia, cada ruído metálico distante ganha significado tático imediato.`;

  const worldMotion = situation || 'Trabalhadores portuários descarregam caixotes pressurizados sob o olhar severo de guardas mercenários, enquanto os repulsores de um cargueiro distante emitem um silvo agudo ao desacelerar nas docas.';

  const cameraZoomFocal = 'Um estalo mecânico curto ressoa perto das sombras de um corredor transversal, seguido pelo brilho tênue de um feixe de mira se ajustando na penumbra.';

  const tensionHandoff = `A tensão se instala no ambiente antes de qualquer palavra ser dita. Os arredores oferecem cobertura parcial entre os contêineres e um duto de ventilação lateral semiaberto.`;

  return [introSensory, characterAnchor, worldMotion, cameraZoomFocal, tensionHandoff].join('\n\n');
}
