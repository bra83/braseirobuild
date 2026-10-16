import * as equipmentCatalog_mjs_1 from './equipmentCatalog.mjs';
import * as availability_mjs_1 from './availability.mjs';
function clone(v) { return structuredClone(v); }
function int(v, label, { min = 0, max = 100000000 } = {}) { const n = Number(v); if (!Number.isInteger(n) || n < min || n > max)
    throw new Error(`${label} inválido.`); return n; }
function qualityNumber(qualities, label) {
    const q = (qualities ?? []).find(v => String(v).toLocaleLowerCase('pt-BR').startsWith(label.toLocaleLowerCase('pt-BR')));
    if (!q)
        return 0;
    const m = String(q).match(/(\d+)/);
    return m ? Number(m[1]) : 0;
}
export function createPendingWeaponAcquisition(itemId, character, { requestId, at = new Date().toISOString() } = {}) {
    const item = (0, equipmentCatalog_mjs_1.weaponCatalogEntry)(itemId);
    if (!item)
        throw new Error('Arma não encontrada no catálogo auditado.');
    if (!character)
        throw new Error('Carregue um personagem antes de propor uma aquisição.');
    return {
        requestId: String(requestId || `acq_${Date.now()}`), itemId: item.id, itemName: item.name, listedPrice: item.price,
        rarity: item.rarity, restricted: Boolean(item.restricted), availability: (0, availability_mjs_1.availabilityProfile)({ rarity: item.rarity, restricted: item.restricted }), status: 'aguardando_mestre', proposedAt: at
    };
}
export function weaponAcquisitionAvailability(itemId, { rarityModifier = null } = {}) {
    const item = (0, equipmentCatalog_mjs_1.weaponCatalogEntry)(itemId);
    if (!item)
        throw new Error('Arma não encontrada no catálogo auditado.');
    return (0, availability_mjs_1.availabilityProfile)({ rarity: item.rarity, restricted: item.restricted, rarityModifier });
}
export function setPendingWeaponAcquisitionRarityModifier(pending, rarityModifier) {
    if (!pending || pending.status !== 'aguardando_mestre')
        throw new Error('Não há aquisição pendente para configurar.');
    return { ...clone(pending), availability: (0, availability_mjs_1.availabilityProfile)({ rarity: pending.rarity, restricted: pending.restricted, rarityModifier }) };
}
export function catalogWeaponToCharacterWeapon(itemId, character) {
    const item = (0, equipmentCatalog_mjs_1.weaponCatalogEntry)(itemId);
    if (!item)
        throw new Error('Arma não encontrada no catálogo auditado.');
    const baseDamage = item.damage !== null && item.damage !== undefined ? Number(item.damage) : Number(character?.characteristics?.fortaleza ?? 0) + Number(item.damageBonus ?? 0);
    if (!Number.isInteger(baseDamage) || baseDamage < 0)
        throw new Error('Não foi possível calcular o dano da arma adquirida.');
    const pierce = qualityNumber(item.qualities, 'Perfurante');
    const blast = qualityNumber(item.qualities, 'Explosão');
    const damageType = (item.qualities ?? []).some(q => String(q).toLocaleLowerCase('pt-BR').includes('dano de atordoamento')) ? 'strain' : 'wounds';
    return {
        name: item.name, skill: item.skill, range: item.range, damage: baseDamage, crit: item.crit,
        ...(pierce ? { pierce } : {}), ...(blast ? { blast, blastActivationCost: 2 } : {}), ...(damageType === 'strain' ? { damageType } : {}),
        qualities: [...(item.qualities ?? [])], sourceCatalogId: item.id, quantity: 1
    };
}
export function resolvePendingWeaponAcquisition(character, pending, { approved, finalPrice = null, reason = null, sourceRuleIds = [] } = {}) {
    if (!pending || pending.status !== 'aguardando_mestre')
        throw new Error('Não há aquisição pendente para resolver.');
    if (typeof approved !== 'boolean')
        throw new Error('A decisão de aquisição precisa aprovar ou rejeitar o pedido.');
    if (!Array.isArray(sourceRuleIds) || sourceRuleIds.length === 0)
        throw new Error('A decisão de aquisição precisa citar ao menos uma regra recuperada.');
    const at = new Date().toISOString();
    if (!approved) {
        return { character: clone(character), record: { ...clone(pending), status: 'rejeitada', resolvedAt: at, reason: reason ? String(reason) : null, sourceRuleIds: [...sourceRuleIds] } };
    }
    const price = int(finalPrice, 'Preço final');
    if (Number(character?.credits ?? 0) < price)
        throw new Error('Créditos insuficientes para concluir a aquisição autorizada.');
    const next = clone(character);
    next.credits -= price;
    next.weapons = Array.isArray(next.weapons) ? next.weapons : [];
    next.weapons.push(catalogWeaponToCharacterWeapon(pending.itemId, next));
    return { character: next, record: { ...clone(pending), status: 'adquirida', resolvedAt: at, finalPrice: price, reason: reason ? String(reason) : null, sourceRuleIds: [...sourceRuleIds] } };
}
