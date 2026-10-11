import type { JourneyEvent } from '../types';
import { OfficialEncounterRulesEngine } from './OfficialEncounterRulesEngine.ts';

/**
 * U07 — player-safe receipt derived ONLY from the persisted journey event.
 * The UI never infers a successful save from an optimistic click, nor displays
 * gmDetail, NPC secrets, unrevealed table entries or uncommitted mechanics.
 * A pending official consequence is deliberately distinct from a resolved one.
 */
export type OfficialEncounterReceipt = Readonly<{
  encounterId: number;
  eventId: string;
  status: 'open' | 'resolved';
  visit: number;
  choiceId: string;
  stage: string;
  committed: boolean;
  /** A resolved encounter can still have a follow-up, such as Linn's daily care. */
  followUpPending: boolean;
  /** True only for transfers already recorded in the persisted event. */
  resourceTransferCommitted: boolean;
  label: string;
}>;

const integer = (value: unknown, fallback: number) =>
  Number.isSafeInteger(Number(value)) && Number(value) > 0 ? Number(value) : fallback;

export class OfficialEncounterReceiptService {
  static fromEvent(event: JourneyEvent): OfficialEncounterReceipt | null {
    const result = event.result || {};
    const mechanic = result.mechanic as Record<string, unknown> | undefined;
    const id = Number(mechanic?.encounterId);
    if (mechanic?.kind !== 'official_random_encounter' || !Number.isInteger(id) || id < 1 || id > 43)
      return null;
    const status = event.status;
    const visit = integer(result.officialEncounterVisitNumber, 1);
    const decision = result.officialSceneDecision as Record<string, unknown> | undefined;
    // U10: não projetar identificadores arbitrários de saves importados na UI.
    // Somente opções existentes na tabela de regras da visita são públicas.
    const candidate = typeof decision?.choiceId === 'string' ? decision.choiceId : '';
    const choiceId = OfficialEncounterRulesEngine.specialSceneChoice(id, candidate, visit) ? candidate : '';
    const rawStage = typeof decision?.stage === 'string' ? decision.stage : '';
    const stage = choiceId && /^[a-z][a-z0-9_]{0,80}$/.test(rawStage) &&
      !/(?:secret|gm_notes|gm_detail|password)/.test(rawStage) ? rawStage : '';
    let committed = Boolean(choiceId);
    let followUpPending = false;
    let resourceTransferCommitted = false;
    let label = `Encontro oficial ${id}: aguardando escolha do grupo.`;
    if (id === 9) {
      const rolled = result.ancientRuinRoll;
      // U11: D66 é 11–66, com ambos os algarismos entre 1 e 6.
      // Valores importados como strings, NaN ou 00 não são rolagens reais.
      committed = typeof rolled === 'number' && Number.isInteger(rolled) &&
        Math.floor(rolled / 10) >= 1 && Math.floor(rolled / 10) <= 6 &&
        rolled % 10 >= 1 && rolled % 10 <= 6;
      label = !committed ? 'Ruínas antigas: D66 ainda não lançado.'
        : status === 'resolved' ? `Ruínas antigas: D66 ${rolled} registrado; descoberta concluída.`
        : rolled === 63 && result.tableGapRequiresRuling === true
          ? 'Ruínas antigas: D66 63 registrado; arbitragem da lacuna ainda pendente.'
          : `Ruínas antigas: D66 ${rolled} registrado; resolução pendente.`;
    } else if (id === 13) {
      const touched = Array.isArray(result.plagueTouchedCharacterIds)
        ? new Set(result.plagueTouchedCharacterIds.filter(value => typeof value === 'string' && value.trim().length > 0)).size : 0;
      const medallion = typeof result.plagueMedallionCharacterId === 'string' && !!result.plagueMedallionCharacterId;
      committed = touched > 0 || medallion || status === 'resolved';
      label = `Irmão da Praga: ${touched} contato(s) registrado(s); medalhão ${medallion ? 'retirado' : 'não retirado'}; ${status === 'resolved' ? 'cena concluída' : 'cena pendente'}.`;
    } else if (id === 15) {
      const reaction = result.beggarsReaction;
      committed = ['observed', 'spoke', 'continued'].includes(String(reaction));
      label = committed
        ? `Pedintes: reação ${String(reaction)} registrada; sem transferência automática; ${status === 'resolved' ? 'cena concluída' : 'cena pendente'}.`
        : 'Pedintes: reação ainda não registrada.';
    } else if (choiceId) {
      // U09: receipt progress comes from the persisted decision, never from
      // optimistic React state, NPC gmNotes, hidden rolls or arbitrary prose.
      label = status === 'resolved'
        ? `Encontro oficial ${id}: escolha ${choiceId} salva; cena concluída.`
        : `Encontro oficial ${id}: escolha ${choiceId} salva; consequências pendentes.`;
      if (id === 12 && choiceId === 'offer_camp') {
        const gift = decision?.banditFoodGift as Record<string, unknown> | undefined;
        const qty = gift?.quantity;
        const delivered = gift && typeof qty === 'number' && Number.isSafeInteger(qty) && qty >= 1 && qty <= 30;
        const reaction = decision?.banditReaction;
        resourceTransferCommitted = Boolean(delivered);
        followUpPending = Boolean(delivered && reaction !== 'accept' && reaction !== 'decline');
        label += delivered
          ? ` ${qty} unidade(s) de comida efetivamente entregue(s). ${reaction === 'accept' ? 'Convite aceito.' : reaction === 'decline' ? 'Convite recusado.' : 'Resposta de Argam ainda pendente.'}`
          : ' Comida e história ainda não confirmadas.';
      } else if (id === 27 && decision?.linnResponse === 'accompany') {
        const fed = Array.isArray(decision.linnFedDays)
          ? new Set(decision.linnFedDays.filter(day => Number.isSafeInteger(day) && Number(day) > 0)).size : 0;
        const required = Number(decision.linnFeedingDaysRequired);
        const hasRequired = Number.isSafeInteger(required) && required >= 1 && required <= 6;
        const disclosed = Number.isFinite(Number(decision.linnDisclosedAt)) && Number(decision.linnDisclosedAt) > 0;
        resourceTransferCommitted = fed > 0;
        followUpPending = decision.linnFollowerActive === true && !disclosed;
        label += disclosed ? ` Linn recebeu alimento por ${fed} dia(s) e contou seu relato.`
          : ` Linn foi alimentada em ${fed}${hasRequired ? `/${required}` : ''} dia(s); acompanhamento ${followUpPending ? 'pendente' : 'não ativo'}.`;
      } else if (id === 35) {
        const gift = decision?.kargGift as Record<string, unknown> | undefined;
        const sacrifice = decision?.kargSacrifice as Record<string, unknown> | undefined;
        const giftCommitted = Boolean(gift && typeof gift.transferredAt === 'number' &&
          Number.isFinite(gift.transferredAt) && gift.transferredAt > 0);
        // Sacrifices are a real loss of a party member or mount; the mere
        // existence of an imported object is not proof of adjudication.
        const expectedKind = choiceId === 'offer_adventurer' ? 'adventurer'
          : choiceId === 'offer_horse' ? 'horse' : '';
        const sacrificeCommitted = Boolean(sacrifice && expectedKind && status === 'resolved' &&
          decision?.stage === 'karg_sacrifice_adjudicated' &&
          decision?.pending == null &&
          decision?.kargOfferAccepted === true &&
          decision?.adjudicationMode === 'official_karg_sacrifice' &&
          sacrifice.kind === expectedKind &&
          typeof sacrifice.targetId === 'string' && sacrifice.targetId.trim().length > 0 &&
          typeof sacrifice.targetName === 'string' && sacrifice.targetName.trim().length > 0 &&
          typeof sacrifice.evidence === 'string' && sacrifice.evidence.trim().length >= 12 &&
          typeof sacrifice.adjudicatedAt === 'number' &&
          Number.isFinite(sacrifice.adjudicatedAt) && sacrifice.adjudicatedAt > 0 &&
          typeof decision?.resolvedAt === 'number' &&
          Number.isFinite(decision.resolvedAt) && decision.resolvedAt === sacrifice.adjudicatedAt);
        resourceTransferCommitted = giftCommitted || sacrificeCommitted;
        followUpPending = decision?.stage === 'karg_offer_refused' ||
          (decision?.kargOfferAccepted === true && decision?.kargGiftTransferRequired === true && !resourceTransferCommitted) ||
          (decision?.stage === 'karg_sacrifice_pending' && !sacrificeCommitted) ||
          (Boolean(expectedKind) && !sacrificeCommitted);
        if (decision?.stage === 'karg_offer_refused') label += ' Karg recusou a oferta; escolha outra abordagem. Nenhum presente foi transferido.';
        else if (sacrificeCommitted) label += ' Sacrifício adjudicado; personagem ou montaria removida da companhia.';
        else if (giftCommitted) label += ' Presente aceito e transferido do inventário para Karg.';
        else if (decision?.kargGiftTransferRequired === true) label += ' Karg aceitou o presente; transferência física pendente.';
        else if (decision?.stage === 'karg_sacrifice_pending') label += ' Sacrifício aceito; destino da vítima ainda pendente.';
        else if (sacrifice) label += ' Registro de sacrifício incompleto; adjudicação ainda pendente.';
      } else if (id === 41) {
        // U13: a projeção pública precisa contar exatamente os mesmos registros
        // íntegros que o mecanismo de retenção. Uma entrada inválida não é
        // evidência de transferência; tokens repetidos não são novas almas.
        const validBarters = Array.isArray(decision?.soulBottleBarters)
          ? decision.soulBottleBarters.filter(entry => entry && typeof entry === 'object' &&
              typeof (entry as Record<string, unknown>).characterId === 'string' &&
              Boolean((entry as Record<string, unknown>).characterId)) : [];
        const barters = validBarters.length;
        const purchases = Array.isArray(decision?.soulBottlePurchases)
          ? decision.soulBottlePurchases.filter(entry => {
            if (!entry || typeof entry !== 'object') return false;
            const purchase = entry as Record<string, unknown>;
            const quantity = purchase.quantity;
            return typeof quantity === 'number' && Number.isSafeInteger(quantity) && quantity >= 1 && quantity <= 99 &&
              purchase.priceSilver === quantity * 10 &&
              typeof purchase.paidAt === 'number' && Number.isFinite(purchase.paidAt) && purchase.paidAt > 0;
          }).reduce((sum, entry) => sum + Number((entry as Record<string, unknown>).quantity), 0) : 0;
        const validSoulIds = Array.isArray(decision?.releasedSoulTokens)
          ? decision.releasedSoulTokens.filter(token => token && typeof token === 'object' &&
              typeof (token as Record<string, unknown>).id === 'string' &&
              Boolean((token as Record<string, unknown>).id))
              .map(token => (token as Record<string, unknown>).id as string) : [];
        const released = new Set(validSoulIds).size;
        const bought = purchases;
        const traded = barters;
        const stock = decision?.soulBottleStock;
        const stockKnown = typeof stock === 'number' && Number.isSafeInteger(stock) && stock >= 0 && stock <= 999;
        resourceTransferCommitted = barters > 0 || purchases > 0 || released > 0;
        const sealed = Math.max(0, bought + traded - released);
        followUpPending = sealed > 0 || (released > 0 && decision?.soulResurrectionD6Pending === true);
        label += ` Mercador de Almas: ${barters} troca(s), ${purchases} garrafa(s) comprada(s), ${released} alma(s) libertada(s)${stockKnown ? `, ${stock} garrafa(s) no estoque` : ''}.`;
        if (sealed > 0) label += ` ${sealed} garrafa(s) lacrada(s) continuam sob responsabilidade do grupo após a partida.`;
        if (released > 0 && decision?.soulResurrectionD6Pending === true) label += ' Ressurreição ainda pendente; nenhuma magia lançada automaticamente.';
      }
    }
    return { encounterId: id, eventId: event.id, status, visit, choiceId, stage, committed, followUpPending, resourceTransferCommitted, label };
  }
}
