// R54 P99 — attachments source-bound. Only base effects whose source mechanics are explicit are executable.
const clone=v=>structuredClone(v);
const norm=v=>String(v??'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('pt-BR').trim();
const source=(page)=>Object.freeze({book:'Gadgets and Gear',physicalPdfPage:page,authority:'PRIMARY_VISUAL_VALIDATED',mechanicalStatus:'PRIMARY_PAGE_MECHANICS_VALIDATED_PENDING_MASTER'});

export const ATTACHMENT_CATALOG=Object.freeze({
  rapid_recharge_xciter:Object.freeze({id:'rapid_recharge_xciter',name:'Rapid-Recharge XCiter',hardPointsRequired:1,price:750,rarity:7,restricted:false,compatibility:'any blaster',effects:Object.freeze({grantedQualities:Object.freeze(['Automática','Imprecisa 3'])}),modOptions:Object.freeze(['2 Decrease Inaccurate quality by 1 Mods']),mods:Object.freeze([Object.freeze({id:'reduce_inaccurate',name:'Reduzir Imprecisa em 1',max:2,effect:Object.freeze({qualityDelta:Object.freeze({inaccurate:-1})})})]),source:source(89)}),
  spread_barrel:Object.freeze({id:'spread_barrel',name:'Spread Barrel',hardPointsRequired:2,price:1725,rarity:4,restricted:false,compatibility:'blaster rifles and blaster carbines',effects:Object.freeze({grantedQualities:Object.freeze(['Explosão 4']),rangeDelta:-1}),modOptions:Object.freeze(['2 Item Quality (Blast +1) Mods']),mods:Object.freeze([Object.freeze({id:'blast_plus_1',name:'Explosão +1',max:2,effect:Object.freeze({qualityDelta:Object.freeze({blast:1})})})]),source:source(91)}),
  bowcaster_accelerator:Object.freeze({id:'bowcaster_accelerator',name:'Bowcaster Accelerator Enhancement',hardPointsRequired:1,price:250,rarity:4,restricted:false,compatibility:'Wookiee bowcasters',effects:Object.freeze({damageBonus:1}),modOptions:Object.freeze(['1 Item Quality (Auto-fire) Mod']),mods:Object.freeze([Object.freeze({id:'grant_autofire',name:'Adicionar Automática',max:1,effect:Object.freeze({grantedQuality:'Automática'})})]),source:source(83)}),
  rocket_guidance_system:Object.freeze({id:'rocket_guidance_system',name:'Rocket Guidance System',hardPointsRequired:1,price:50,rarity:4,restricted:false,compatibility:'any micro-rocket',effects:Object.freeze({grantedQualities:Object.freeze(['Guiada 2'])}),modOptions:Object.freeze(['3 Item Quality (Guided +1) Mods','1 Item Quality (Accurate +1) Mod']),mods:Object.freeze([Object.freeze({id:'guided_plus_1',name:'Guiada +1',max:3,effect:Object.freeze({qualityDelta:Object.freeze({guided:1})})}),Object.freeze({id:'accurate_plus_1',name:'Precisa +1',max:1,effect:Object.freeze({qualityDelta:Object.freeze({accurate:1})})})]),source:source(89)}),
  staged_rocket_booster:Object.freeze({id:'staged_rocket_booster',name:'Staged Rocket Booster',hardPointsRequired:1,price:250,rarity:2,restricted:false,compatibility:'any micro-rocket',effects:Object.freeze({grantedQualities:Object.freeze(['Imprecisa 2']),rangeDelta:1}),modOptions:Object.freeze(['1 Increase range one additional band Mod','2 Reduce Inaccurate by 1 Mods']),mods:Object.freeze([Object.freeze({id:'range_plus_1',name:'Aumentar alcance em 1 faixa',max:1,effect:Object.freeze({rangeDelta:1})}),Object.freeze({id:'reduce_inaccurate',name:'Reduzir Imprecisa em 1',max:2,effect:Object.freeze({qualityDelta:Object.freeze({inaccurate:-1})})})]),source:source(91)})
});

export function attachmentCatalog(){return Object.values(ATTACHMENT_CATALOG).map(clone);}
export function attachmentCatalogEntry(id){const key=norm(id);return Object.values(ATTACHMENT_CATALOG).find(a=>norm(a.id)===key||norm(a.name)===key)??null;}

function weaponCatalogId(weapon){return norm(weapon?.sourceCatalogId??weapon?.id??weapon?.name);}
export function attachmentCompatibility(attachment,weapon){
  const a=typeof attachment==='string'?attachmentCatalogEntry(attachment):attachment;
  if(!a||!weapon)return {compatible:false,reason:'Attachment ou arma inválidos.'};
  const id=weaponCatalogId(weapon),name=norm(weapon?.name),hay=`${id} ${name}`;
  let compatible=false;
  if(a.id==='rapid_recharge_xciter') compatible=hay.includes('blaster');
  else if(a.id==='spread_barrel') compatible=['blaster_rifle','heavy_blaster_rifle','blaster_carbine'].some(v=>id.includes(v));
  else if(a.id==='bowcaster_accelerator') compatible=hay.includes('bowcaster');
  else if(a.id==='rocket_guidance_system'||a.id==='staged_rocket_booster') compatible=hay.includes('micro')&&hay.includes('rocket');
  return {compatible,reason:compatible?null:`${a.name} não é compatível com ${weapon?.name??'esta arma'} (${a.compatibility}).`};
}

const RANGE=['Interação','Curto','Médio','Longo','Extremo'];
function shiftedRange(value,delta){
  const aliases={engaged:'Interação',interaction:'Interação',short:'Curto',medium:'Médio',long:'Longo',extreme:'Extremo'};
  const raw=aliases[norm(value)]??String(value??'');
  const i=RANGE.findIndex(x=>norm(x)===norm(raw)); if(i<0||!delta)return value;
  return RANGE[Math.max(0,Math.min(RANGE.length-1,i+Number(delta)))];
}

export function attachmentModDefinitions(attachmentOrId){
  const a=typeof attachmentOrId==='string'?attachmentCatalogEntry(attachmentOrId):attachmentOrId;
  return Array.isArray(a?.mods)?a.mods.map(clone):[];
}
function modCounts(installed,modId){
  const successes=(installed?.mods??[]).filter(m=>m?.modId===modId&&m?.status==='installed').length;
  const failures=(installed?.modAttempts??[]).filter(m=>m?.modId===modId&&m?.status==='failed').length;
  return {successes,failures,used:successes+failures};
}
export function attachmentModStatus(installed){
  const base=attachmentCatalogEntry(installed?.catalogId??installed?.id??installed?.name);
  const defs=attachmentModDefinitions(base);
  const successfulTotal=(installed?.mods??[]).filter(m=>m?.status==='installed').length;
  return defs.map(d=>{const c=modCounts(installed,d.id);return {...d,installed:c.successes,failed:c.failures,remaining:Math.max(0,Number(d.max??1)-c.used),difficulty:3+successfulTotal,cost:100};});
}
// Ranked qualities are normalized before the attack resolver reads them. The
// old concatenation could leave two Blast/Guided/Inaccurate entries and make
// weaponQualityRank() silently read only the first one.
const QUALITY_ALIASES = Object.freeze({
  inaccurate: ['imprecisa', 'inaccurate'], blast: ['explosao', 'blast'],
  guided: ['guiada', 'guided'], accurate: ['precisa', 'accurate']
});
const QUALITY_LABELS = Object.freeze({ inaccurate: 'Imprecisa', blast: 'Explosão', guided: 'Guiada', accurate: 'Precisa' });
function parseRankedQuality(label) {
  const normalized = norm(label);
  for (const [key, aliases] of Object.entries(QUALITY_ALIASES)) {
    for (const alias of aliases) {
      const match = normalized.match(new RegExp(`^${alias}(?:\\s+(\\d+))?$`));
      if (match) return { key, rank: Number(match[1] ?? 1) };
    }
  }
  return null;
}
function mergeQuality(qualities, label) {
  const list = [...(qualities ?? [])];
  const incoming = parseRankedQuality(label);
  if (!incoming) {
    if (!list.some(value => norm(value) === norm(label))) list.push(String(label));
    return list;
  }
  const matches = list.map((value, index) => ({ parsed: parseRankedQuality(value), index }))
    .filter(entry => entry.parsed?.key === incoming.key);
  const rank = Math.max(incoming.rank, ...matches.map(entry => entry.parsed.rank));
  const retained = list.filter((_, index) => !matches.some(entry => entry.index === index));
  if (rank > 0) retained.push(`${QUALITY_LABELS[incoming.key]} ${rank}`);
  return retained;
}
function addOrAdjustQuality(qualities, key, delta) {
  const list = [...(qualities ?? [])];
  const matches = list.map((value, index) => ({ parsed: parseRankedQuality(value), index }))
    .filter(entry => entry.parsed?.key === key);
  const current = Math.max(0, ...matches.map(entry => entry.parsed.rank));
  const next = Math.max(0, current + Number(delta || 0));
  const retained = list.filter((_, index) => !matches.some(entry => entry.index === index));
  if (next > 0) retained.push(`${QUALITY_LABELS[key] ?? key} ${next}`);
  return retained;
}
function authoritativeMods(installed, base) {
  const definitions = new Map((base?.mods ?? []).map(def => [def.id, def]));
  // A failed modification attempt permanently uses one of the attachment's
  // allowed attempts. Snapshot data cannot grant extra successful slots by
  // appending mods after failures. The source catalog is the only authority.
  const failures = new Map();
  for (const entry of (Array.isArray(installed?.modAttempts) ? installed.modAttempts : [])) {
    if (entry?.status !== 'failed' || !definitions.has(entry?.modId)) continue;
    failures.set(entry.modId, (failures.get(entry.modId) ?? 0) + 1);
  }
  const successes = new Map();
  const accepted = [];
  let ignored = 0;
  for (const entry of (Array.isArray(installed?.mods) ? installed.mods : [])) {
    const definition = definitions.get(entry?.modId);
    if (entry?.status !== 'installed' || !definition ||
        (successes.get(definition.id) ?? 0) + (failures.get(definition.id) ?? 0) >= definition.max) {
      ignored++; continue;
    }
    successes.set(definition.id, (successes.get(definition.id) ?? 0) + 1);
    accepted.push(definition);
  }
  return { accepted, ignored };
}
function applyInstalledModsToWeapon(weapon, installed, base) {
  const next = clone(weapon);
  if (installed?.disabled) return next;
  const { accepted } = authoritativeMods(installed, base);
  for (const mod of accepted) {
    const effect = mod.effect ?? {};
    if (effect.damageBonus) next.damage = Math.max(0, Number(next.damage ?? 0) + Number(effect.damageBonus));
    if (effect.rangeDelta) next.range = shiftedRange(next.range, Number(effect.rangeDelta));
    if (effect.grantedQuality) next.qualities = mergeQuality(next.qualities, effect.grantedQuality);
    for (const [key, delta] of Object.entries(effect.qualityDelta ?? {}))
      next.qualities = addOrAdjustQuality(next.qualities, key, delta);
  }
  return next;
}

/** Resolve installed items against the immutable mechanical catalog. A
 * persisted inventory entry is not permission to bypass compatibility,
 * hard-point limits or unique owned-asset identity. Legacy weapons without a
 * known capacity keep their compatible attachments rather than losing mods. */
function authorizedInstalledAttachments(weapon) {
  const entries = Array.isArray(weapon?.attachments) ? weapon.attachments : [];
  const rawCapacity = weapon?.hardPoints ?? weapon?.hardpoints ?? weapon?.hardPointsTotal;
  const capacity = rawCapacity == null || rawCapacity === '' ? Infinity : Math.max(0, Number(rawCapacity) || 0);
  const seenAssets = new Set();
  const accepted = [];
  let used = 0;
  let ignored = 0;
  for (const entry of entries) {
    const base = attachmentCatalogEntry(entry?.catalogId ?? entry?.id ?? entry?.name);
    if (!base || entry?.disabled) { if (!base) ignored++; continue; }
    const assetId = entry?.ownedAssetId == null ? null : String(entry.ownedAssetId);
    const cost = Number(base.hardPointsRequired);
    if (!attachmentCompatibility(base, weapon).compatible ||
        !Number.isFinite(cost) || cost < 0 || used + cost > capacity ||
        (assetId !== null && seenAssets.has(assetId))) { ignored++; continue; }
    if (assetId !== null) seenAssets.add(assetId);
    used += cost;
    accepted.push({ entry, base });
  }
  return { accepted, ignored, hardPointsUsed: used };
}

export function installedAttachmentEffects(weapon) {
  const authorization = authorizedInstalledAttachments(weapon);
  const effects = { damageBonus: 0, rangeDelta: 0, grantedQualities: [], attachments: [], ignoredMods: 0,
    ignoredAttachments: authorization.ignored, hardPointsUsed: authorization.hardPointsUsed };
  for (const { entry, base } of authorization.accepted) {
    // Never execute effects serialized in an inventory snapshot: catalog
    // source mechanics are the only executable authority.
    const effect = base.effects ?? {};
    effects.damageBonus += Number(effect.damageBonus ?? 0);
    effects.rangeDelta += Number(effect.rangeDelta ?? 0);
    effects.grantedQualities.push(...(effect.grantedQualities ?? []));
    effects.ignoredMods += authoritativeMods(entry, base).ignored;
    effects.attachments.push({ id: base.id, name: base.name, ownedAssetId: entry?.ownedAssetId ?? null });
  }
  return effects;
}
export function effectiveWeaponWithAttachments(weapon) {
  if (!weapon || typeof weapon !== 'object') throw new Error('Arma inválida.');
  let next = clone(weapon);
  const fx = installedAttachmentEffects(weapon);
  next.damage = Math.max(0, Number(next.damage ?? 0) + fx.damageBonus);
  if (fx.rangeDelta) next.range = shiftedRange(next.range, fx.rangeDelta);
  next.qualities = (Array.isArray(next.qualities) ? next.qualities : []).reduce(mergeQuality, []);
  for (const quality of fx.grantedQualities) next.qualities = mergeQuality(next.qualities, quality);
  for (const { entry, base } of authorizedInstalledAttachments(weapon).accepted)
    next = applyInstalledModsToWeapon(next, entry, base);
  next.attachmentEffects = fx;
  return next;
}
export function attachmentInstallPayload(attachment,ownedAssetId){
  const a=typeof attachment==='string'?attachmentCatalogEntry(attachment):attachment;if(!a)throw new Error('Attachment não encontrado no catálogo mecânico autorizado.');
  return {catalogId:a.id,id:a.id,name:a.name,ownedAssetId:String(ownedAssetId),hardPointsRequired:a.hardPointsRequired,effects:clone(a.effects),source:clone(a.source),mods:[],modAttempts:[],disabled:false};
}
