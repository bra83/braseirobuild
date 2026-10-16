normalizeFringeGroup = normalizeFringeGroup;
validateFringeGroup = validateFringeGroup;
updateShip = updateShip;
setTextList = setTextList;
removeObligationEntry = removeObligationEntry;
function text(v, max = 2000) { return String(v ?? '').trim().slice(0, max); }
function int(v, label, { min = 0, max = 9999 } = {}) { const n = Number(v); if (!Number.isInteger(n) || n < min || n > max)
    throw new Error(`${label} deve estar entre ${min} e ${max}.`); return n; }
export function createFringeGroup() { return { schemaVersion: 2, base: { name: 'Refúgio / Base', location: '', description: '' }, ship: { vehicleId: null, name: '', notes: '' }, obligationEntries: [], dutyEntries: [], contributionRank: 0, resources: [], belongings: [], contacts: [], ownedAssets: [], companions: [], pendingAllianceReward: null, allianceRewardHistory: [], destiny: { light: 0, dark: 0 }, legacyAoR: null }; }
export function normalizeFringeGroup(value) { const d = createFringeGroup(), v = value && typeof value === 'object' ? value : {}; if(Object.hasOwn(v,'companions')&&!Array.isArray(v.companions))throw new Error('Lista de companheiros inválida.'); const legacyDuty=Array.isArray(v.legacyAoR?.dutyEntries)?v.legacyAoR.dutyEntries:[]; const legacyContribution=Number.isInteger(v.legacyAoR?.contributionRank)?v.legacyAoR.contributionRank:0; return { schemaVersion: 2, base: { ...d.base, ...(v.base ?? {}) }, ship: { ...d.ship, ...(v.ship ?? {}) }, obligationEntries: Array.isArray(v.obligationEntries) ? v.obligationEntries : [], dutyEntries: Array.isArray(v.dutyEntries) ? v.dutyEntries : structuredClone(legacyDuty), contributionRank: Number.isInteger(v.contributionRank) ? Math.max(0,Math.min(10,v.contributionRank)) : Math.max(0,Math.min(10,legacyContribution)), resources: Array.isArray(v.resources) ? v.resources : [], belongings: Array.isArray(v.belongings) ? v.belongings : [], contacts: Array.isArray(v.contacts) ? v.contacts : [], ownedAssets: Array.isArray(v.ownedAssets) ? v.ownedAssets : [], companions: Array.isArray(v.companions) ? structuredClone(v.companions) : [], pendingAllianceReward: v.pendingAllianceReward && typeof v.pendingAllianceReward === 'object' ? structuredClone(v.pendingAllianceReward) : null, allianceRewardHistory: Array.isArray(v.allianceRewardHistory) ? structuredClone(v.allianceRewardHistory) : [], destiny: { ...d.destiny, ...(v.destiny ?? {}) }, legacyAoR: v.legacyAoR && typeof v.legacyAoR === 'object' ? structuredClone(v.legacyAoR) : null }; }
export function migrateRebelGroupToFringe(value) { const old = value && typeof value === 'object' ? value : {}; const group = createFringeGroup(); group.base = { ...group.base, ...(old.base ?? {}), name: text(old.base?.name, 120) || 'Refúgio / Base' }; group.resources = Array.isArray(old.resources) ? structuredClone(old.resources) : []; group.belongings = Array.isArray(old.belongings) ? structuredClone(old.belongings) : []; group.contacts = Array.isArray(old.contacts) ? structuredClone(old.contacts) : []; group.destiny = { ...group.destiny, ...(old.destiny ?? {}) }; if (Array.isArray(old.dutyEntries) || Number.isInteger(old.contributionRank)) {
    group.dutyEntries = Array.isArray(old.dutyEntries) ? structuredClone(old.dutyEntries) : []; group.contributionRank = Number.isInteger(old.contributionRank) ? Math.max(0,Math.min(10,old.contributionRank)) : 0; group.legacyAoR = { dutyEntries: structuredClone(group.dutyEntries), contributionRank: group.contributionRank, note: 'Dados AoR preservados e disponíveis somente quando o modo de campanha AoR/misto estiver ativo; não convertidos em Obligation.' };
} return group; }
export function validateFringeGroup(value) { if(value && Object.hasOwn(value,'companions'))validateCompanions(value.companions); const v = normalizeFringeGroup(value); int(Number(v.destiny.light), 'Destino do Lado Luminoso', { min: 0, max: 20 }); int(Number(v.destiny.dark), 'Destino do Lado Sombrio', { min: 0, max: 20 }); int(Number(v.contributionRank), 'Faixa de Contribuição', { min: 0, max: 10 }); for (const key of ['resources', 'belongings', 'contacts', 'ownedAssets', 'companions', 'obligationEntries', 'dutyEntries'])
    if (!Array.isArray(v[key]))
        throw new Error(`Campo ${key} inválido.`); validateCompanions(v.companions); return true; }
export function updateBase(value, { name, location, description } = {}) { const v = normalizeFringeGroup(value); v.base = { name: text(name ?? v.base.name, 120) || 'Refúgio / Base', location: text(location ?? v.base.location, 240), description: text(description ?? v.base.description, 2000) }; return v; }
export function updateShip(value, { vehicleId, name, notes } = {}) { const v = normalizeFringeGroup(value); v.ship = { vehicleId: text(vehicleId ?? v.ship.vehicleId, 120) || null, name: text(name ?? v.ship.name, 160), notes: text(notes ?? v.ship.notes, 1200) }; return v; }
export function setDestinyReserve(value, { light, dark }) { const v = normalizeFringeGroup(value); v.destiny = { light: int(Number(light), 'Destino do Lado Luminoso', { min: 0, max: 20 }), dark: int(Number(dark), 'Destino do Lado Sombrio', { min: 0, max: 20 }) }; return v; }
export function setTextList(value, key, items) { if (!['resources', 'belongings', 'contacts'].includes(key))
    throw new Error('Lista de grupo inválida.'); const v = normalizeFringeGroup(value); v[key] = (Array.isArray(items) ? items : []).map(x => text(x, 500)).filter(Boolean).slice(0, 100); return v; }
export function addObligationEntry(value, entry = {}) { const v = normalizeFringeGroup(value); const e = { id: text(entry.id, 100) || `obligation_${Date.now()}`, value: int(Number(entry.value), 'Valor de Obligation', { min: 0, max: 1000 }), member: text(entry.member, 120), type: text(entry.type, 160), description: text(entry.description, 600) }; v.obligationEntries.push(e); return v; }
export function removeObligationEntry(value, id) { const v = normalizeFringeGroup(value); v.obligationEntries = v.obligationEntries.filter(e => e.id !== id); return v; }
export function groupObligationTotal(value) { return normalizeFringeGroup(value).obligationEntries.reduce((sum, e) => sum + (Number(e.value) || 0), 0); }

export function addDutyEntry(value, entry = {}) { const v = normalizeFringeGroup(value); const e = { id: text(entry.id, 100) || `duty_${Date.now()}`, value: int(Number(entry.value), 'Valor de Duty', { min: 0, max: 1000 }), member: text(entry.member, 120), type: text(entry.type, 160), description: text(entry.description, 600), motivations: text(entry.motivations, 600) }; v.dutyEntries.push(e); return v; }
export function removeDutyEntry(value, id) { const v = normalizeFringeGroup(value); v.dutyEntries = v.dutyEntries.filter(e => e.id !== id); return v; }
export function groupDutyTotal(value) { return normalizeFringeGroup(value).dutyEntries.reduce((sum, e) => sum + (Number(e.value) || 0), 0); }
export function setContributionRank(value, rank) { const v = normalizeFringeGroup(value); v.contributionRank = int(Number(rank), 'Faixa de Contribuição', { min: 0, max: 10 }); return v; }

/** P164: actual party character sheets, not NPC names or an unverified lore roster.
 * Validate BEFORE any commit, and again on persisted campaign reload. */
export function validateCompanions(companions){
  if(!Array.isArray(companions)||companions.length>8)throw new Error('Lista de companheiros inválida (máximo 8).');
  const ids=new Set();
  for(const member of companions){
    if(!member||typeof member!=='object'||Array.isArray(member)||
       typeof member.id!=='string'||!/^[a-zA-Z0-9][a-zA-Z0-9_-]{0,79}$/.test(member.id)||
       ids.has(member.id)||!member.character||typeof member.character!=='object'||Array.isArray(member.character))
      throw new Error('Companheiro inválido ou duplicado.');
    ids.add(member.id);
    const c=member.character;
    if(typeof c.name!=='string'||!c.name.trim()||c.name.length>120||
       !c.characteristics||!c.skills||!c.derived||
       !c.derived.wounds||!c.derived.strain)
      throw new Error('Ficha de companheiro incompleta.');
    for(const key of ['wounds','strain']){
      const {current,threshold}=c.derived[key];
      if(!Number.isSafeInteger(current)||current<0||current>999||
         !Number.isSafeInteger(threshold)||threshold<1||threshold>999)
        throw new Error('Saúde de companheiro inválida.');
    }
    for(const value of Object.values(c.characteristics))
      if(!Number.isSafeInteger(value)||value<0||value>6)throw new Error('Características de companheiro inválidas.');
    if(c.combatEffects?.stimApplicationBonuses){
      for(const [key,bonus] of Object.entries(c.combatEffects.stimApplicationBonuses))
        if(!['fortaleza','agilidad','intelecto','astucia','voluntad','presencia'].includes(key)||bonus!==1)
          throw new Error('Estimulantes de companheiro inválidos.');
    }
  }
  return true;
}
