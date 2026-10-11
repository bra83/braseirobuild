import type { Npc } from '../types';

/** Guia do Mestre, encontro #10 p.151–152: Grelf segue o grupo por curiosidade.
 * O deslocamento NÃO pode teleportar NPCs, cruzar campanhas, ou revelar sua identidade.
 * Chamado somente dentro da mesma transação que confirma uma mudança real de hex.
 */
export class OfficialEncounterFollowerEngine {
  static advance(npcs: readonly Npc[], campaignId: string, fromHexId: string | null | undefined,
    toHexId: string | null | undefined, now: number): Npc[] {
    if (!fromHexId || !toHexId || fromHexId === toHexId) return [];
    return npcs.filter(npc => npc.campaignId === campaignId && npc.status === 'Alive' &&
      npc.officialFollowParty === true && npc.currentLocationId === fromHexId &&
      Boolean(npc.officialFollowEventId) &&
      (npc.id === `official-npc:${campaignId}:${npc.officialFollowEventId}:grelf` ||
       npc.id === `official-npc:${campaignId}:${npc.officialFollowEventId}:linn` ||
       (npc.id === `official-npc:${campaignId}:${npc.officialFollowEventId}:black-dragon-hatchling` &&
        npc.officialHatchling === true && 'bondedCharacterId' in npc &&
        typeof npc.bondedCharacterId === 'string' && npc.bondedCharacterId.length > 0 &&
        npc.officialHatchlingState?.bondedCharacterId === npc.bondedCharacterId))
    ).map(npc => { const dragon=npc.officialHatchling===true && npc.id.endsWith(':black-dragon-hatchling');
      return { ...npc, currentLocationId: toHexId, officialFollowLastMovedAt: now,
      currentActivity: dragon?'O filhote vinculado acompanha a companhia, sem crescimento adicional por movimento.':
        npc.id.endsWith(':linn')?'Segue o grupo em silêncio; cuidados médicos não foram aplicados.':'Segue o grupo discretamente; repousa próximo ao acampamento.',
      lastAction: 'Acompanhou a viagem para o hex seguinte.',
      memories: [...(npc.memories || []), {
        id: `official-follow:${npc.officialFollowEventId}:${now}:${toHexId}`,
        sceneId: npc.officialFollowEventId!, summary: dragon?'O filhote vinculado acompanhou a viagem sem alterar seus atributos.':
          npc.id.endsWith(':linn')?'Acompanhou a viagem; ainda precisa de acolhimento e cuidados.':'Acompanhou o grupo ao hex seguinte sem revelar sua natureza.',
        valence: 'neutral' as const, createdAt: now,
      }].slice(-80),
    }; });
  }

  /** U18/U19: companions follow both land and water travel, Meroggha only water. */
  static advanceForTravel(npcs: readonly Npc[], campaignId: string,
    fromHexId: string | null | undefined, toHexId: string | null | undefined,
    now: number, waterTravel: boolean): Npc[] {
    const companions = this.advance(npcs, campaignId, fromHexId, toHexId, now);
    if (!waterTravel) return companions;
    const stowaways = this.advanceWaterStowaways(npcs, campaignId, fromHexId, toHexId, now);
    const byId = new Map(companions.map(npc => [npc.id, npc]));
    for (const npc of stowaways) if (!byId.has(npc.id)) byId.set(npc.id, npc);
    return [...byId.values()];
  }

  /** R88 #39: Meroggha remains hidden aboard the party's boat after the party
   * resumes lake travel. This separate path is only called for water movement. */
  static advanceWaterStowaways(npcs: readonly Npc[], campaignId: string, fromHexId: string | null | undefined,
    toHexId: string | null | undefined, now: number): Npc[] {
    if (!fromHexId || !toHexId || fromHexId === toHexId) return [];
    return npcs.filter(npc => npc.campaignId === campaignId && npc.status === 'Alive' &&
      npc.hiddenFromParty === true && npc.officialStowaway === true &&
      npc.currentLocationId === fromHexId && Boolean(npc.firstSeenSceneId) &&
      npc.id === `official-npc:${campaignId}:${npc.firstSeenSceneId}:meroggha`)
      .map(npc => ({ ...npc, currentLocationId: toHexId, officialFollowLastMovedAt: now,
        currentActivity: 'Permanece escondido a bordo do barco da companhia.',
        lastAction: 'Acompanhou o barco no hex seguinte sem ser descoberto.',
        memories: [...(npc.memories || []), {
          id: `official-stowaway:${npc.firstSeenSceneId}:${now}:${toHexId}`,
          sceneId: npc.firstSeenSceneId!, summary: 'Meroggha permaneceu escondido durante a viagem do barco.',
          valence: 'neutral' as const, createdAt: now,
        }].slice(-80),
      }));
  }
}
