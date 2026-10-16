import * as storage_mjs_1 from "./storage.mjs";
import * as snapshots_mjs_1 from "./snapshots.mjs";
import * as character_mjs_1 from "../rules/character.mjs";
import * as eoteCharacterCreation_mjs_1 from "../rules/eoteCharacterCreation.mjs";
import * as characterCreation_mjs_1 from "../rules/characterCreation.mjs";
import * as labelsPtBr_mjs_1 from "../rules/labelsPtBr.mjs";
import * as controller_mjs_1 from "./controller.mjs";
import * as vehicles_mjs_1 from "../rules/vehicles.mjs";
import * as talents_mjs_1 from "../rules/talents.mjs";
import * as imageLibraryData_mjs_1 from "../library/imageLibraryData.mjs";
import * as sceneLibrary_mjs_1 from "../library/sceneLibrary.mjs";
import * as imageLibrary_mjs_1 from "../library/imageLibrary.mjs";
import { R54_LOCATION_MAP_ASSETS } from "../library/r54LocationMapData.mjs";
export const UI_BRIDGE_VERSION = 3;
function clone(value) {
    return value === undefined ? undefined : structuredClone(value);
}
function messageOf(error) {
    return error instanceof Error ? error.message : String(error ?? 'Erro desconhecido.');
}
function ok(data) {
    return { ok: true, data };
}
function fail(error, message) {
    return { ok: false, error, message };
}
const nativeMasterRequests = new Map();
let nativeMasterHookInstalled = false;
function nativeGeminiStatus(target = globalThis) {
    const bridge = target?.BraseiroGemini;
    if (!bridge || typeof bridge.getStatus !== 'function' || typeof bridge.requestMasterProposal !== 'function')
        return { available: false, configured: false };
    try {
        const raw = bridge.getStatus();
        const status = typeof raw === 'string' ? JSON.parse(raw) : raw;
        return { available: true, configured: Boolean(status?.configured), model: status?.masterModel ?? null, generating: Boolean(status?.masterGenerating) };
    }
    catch {
        return { available: true, configured: false, invalid: true };
    }
}
function ensureNativeMasterHook(target = globalThis) {
    if (nativeMasterHookInstalled || !target)
        return;
    nativeMasterHookInstalled = true;
    const previous = typeof target.braseiroMasterNativeEvent === 'function' ? target.braseiroMasterNativeEvent : null;
    target.braseiroMasterNativeEvent = (raw) => {
        try {
            previous?.(raw);
        }
        catch { }
        let event;
        try {
            event = typeof raw === 'string' ? JSON.parse(raw) : raw;
        }
        catch {
            return;
        }
        const requestId = String(event?.requestId ?? '');
        const pending = nativeMasterRequests.get(requestId);
        if (!pending)
            return;
        nativeMasterRequests.delete(requestId);
        clearTimeout(pending.timer);
        if (event?.ok === true && event.proposal && typeof event.proposal === 'object')
            pending.resolve({ proposal: event.proposal, model: event.model ?? null });
        else
            pending.reject(new Error(String(event?.message ?? 'O Mestre Gemini não retornou uma proposta válida.')));
    };
}
function requestNativeGeminiMaster(providerPayload, { timeoutMs = 60000, target = globalThis } = {}) {
    const status = nativeGeminiStatus(target);
    if (!status.available)
        return Promise.reject(new Error('Host Gemini nativo indisponível.'));
    if (!status.configured)
        return Promise.reject(new Error('Configure a chave Gemini em AJUSTES → Gemini / provedor antes de iniciar a narração.'));
    ensureNativeMasterHook(target);
    const requestId = `master_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 10)}`;
    return new Promise((resolve, reject) => {
        const timer = setTimeout(() => { nativeMasterRequests.delete(requestId); reject(new Error('Tempo limite do Mestre Gemini excedido.')); }, Math.max(5000, Number(timeoutMs) || 60000));
        nativeMasterRequests.set(requestId, { resolve, reject, timer });
        try {
            const raw = target.BraseiroGemini.requestMasterProposal(requestId, JSON.stringify(providerPayload));
            const started = typeof raw === 'string' ? JSON.parse(raw) : raw;
            if (!started?.ok) {
                nativeMasterRequests.delete(requestId);
                clearTimeout(timer);
                reject(new Error(String(started?.message ?? 'Não foi possível iniciar o Mestre Gemini.')));
            }
        }
        catch (error) {
            nativeMasterRequests.delete(requestId);
            clearTimeout(timer);
            reject(error instanceof Error ? error : new Error(String(error)));
        }
    });
}
export class BraseiroUiBridge {
    constructor({ repository = null, storage = globalThis.localStorage, snapshots = null } = {}) {
        this.repository = repository ?? new storage_mjs_1.LocalStorageRepository(storage);
        const snapshotStorage = this.repository?.storage ?? storage;
        this.snapshots = snapshots ?? new snapshots_mjs_1.LocalSnapshotRepository(snapshotStorage);
    }
    readState() {
        try {
            const state = this.repository.load();
            return ok(state ? clone(state) : null);
        }
        catch (error) {
            return fail('STATE_READ_FAILED', messageOf(error));
        }
    }
    initializeCampaign() {
        try {
            const existing = this.repository.load();
            if (existing)
                return ok({ created: false, state: clone(existing) });
            const game = new controller_mjs_1.GameController(this.repository);
            return ok({ created: true, state: clone(game.state) });
        }
        catch (error) {
            return fail('CAMPAIGN_INIT_FAILED', messageOf(error));
        }
    }
    campaignModeSummary() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.campaignModeSummary())); }
        catch (error) { return fail('CAMPAIGN_MODE_FAILED', messageOf(error)); }
    }
    newCampaign(name = 'Nova campanha') {
        try {
            const game = new controller_mjs_1.GameController(this.repository);
            return ok(clone(game.newCampaign(String(name ?? '').trim() || 'Nova campanha')));
        }
        catch (error) { return fail('CAMPAIGN_RESET_FAILED', messageOf(error)); }
    }
    setCampaignMode(mode = 'EOTE') {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.setCampaignMode(mode))); }
        catch (error) { return fail('CAMPAIGN_MODE_FAILED', messageOf(error)); }
    }
    initializeLivingSandbox(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.initializeLivingSandbox(options ?? {}))); }
        catch (error) { return fail('SANDBOX_INIT_FAILED', messageOf(error)); }
    }
    livingSandboxStatus() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.livingSandboxStatus())); }
        catch (error) { return fail('SANDBOX_READ_FAILED', messageOf(error)); }
    }
    sandboxOpening(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.sandboxOpening(options ?? {}))); }
        catch (error) { return fail('SANDBOX_READ_FAILED', messageOf(error)); }
    }
    startSandboxSession(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.startSandboxSession(options ?? {}))); }
        catch (error) { return fail('SANDBOX_START_FAILED', messageOf(error)); }
    }
    publishedAdventureCatalog() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.publishedAdventureCatalog())); }
        catch (error) { return fail('PUBLISHED_ADVENTURE_FAILED', messageOf(error)); }
    }
    publishedAdventureStatus(adventureId = 'under-black-sun') {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.publishedAdventureStatus(String(adventureId ?? 'under-black-sun')))); }
        catch (error) { return fail('PUBLISHED_ADVENTURE_FAILED', messageOf(error)); }
    }
    /** Single Android/WebView entrypoint for replay-safe published actions. */
    executePublishedAdventureCommand(command = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.executePublishedAdventureCommand(command ?? {}))); }
        catch (error) { return fail('PUBLISHED_ADVENTURE_COMMAND_FAILED', messageOf(error)); }
    }
    adjudicateJewelOperation(kind, details = {}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.adjudicateJewelOperation(String(kind??''),details??{})));}
        catch(error){return fail('JEWEL_OPERATION_FAILED',messageOf(error));}
    }
    /** P187: real banking dice, persistent receipts and rollback. */
    resolveJewelBankStep(details={}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.resolveJewelBankStep(details??{})));}
        catch(error){return fail('JEWEL_BANK_STEP_FAILED',messageOf(error));}
    }
    /** P200: settle verified bank funds to an in-world account, not PC credits. */
    settleJewelBank(details={}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.settleJewelBank(details??{})));}
        catch(error){return fail('JEWEL_BANK_SETTLEMENT_FAILED',messageOf(error));}
    }
    /** P201: real SWE09 airspeeder bomb checks and persistent outcome. */
    resolveJewelEscapeBomb(details={}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.resolveJewelEscapeBomb(details??{})));}
        catch(error){return fail('JEWEL_ESCAPE_BOMB_FAILED',messageOf(error));}
    }
    /** P203: source-backed blast and long/extreme falls with forced criticals. */
    resolveJewelEscapeImpact(details={}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.resolveJewelEscapeImpact(details??{})));}
        catch(error){return fail('JEWEL_ESCAPE_IMPACT_FAILED',messageOf(error));}
    }
    /** P204: explicit GM-confirmed companion blast/fall with independent receipts. */
    resolveJewelCompanionImpact(details={}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.resolveJewelCompanionImpact(details??{})));}
        catch(error){return fail('JEWEL_COMPANION_IMPACT_FAILED',messageOf(error));}
    }
    /** P205: queue Arrow-7 aft turret for the goons' real NPC initiative slot. */
    queueJewelArrow7Turret(details={}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.queueJewelArrow7Turret(details??{})));}
        catch(error){return fail('JEWEL_ARROW7_TURRET_FAILED',messageOf(error));}
    }
    /** P206: competitive Arrow-7 pursuit with real narrative dice and GM-confirmed NPC pilot. */
    resolveJewelArrow7Chase(details={}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.resolveJewelArrow7Chase(details??{})));}
        catch(error){return fail('JEWEL_ARROW7_CHASE_FAILED',messageOf(error));}
    }
    /** P206: confirm a source-backed chase outcome after real competitive rolls. */
    confirmJewelArrow7ChaseOutcome(details={}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.confirmJewelArrow7ChaseOutcome(details??{})));}
        catch(error){return fail('JEWEL_ARROW7_CHASE_END_FAILED',messageOf(error));}
    }
    /** P209: initial published Wing Guard encounter and Holo-Park entry. */
    beginJewelHoloParkWingGuard(details={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.beginJewelHoloParkWingGuard(details??{})));}
        catch(error){return fail('JEWEL_HOLO_WING_GUARD_FAILED',messageOf(error));}
    }
    /** P217: GM-confirmed Arend/Grayson pursuers and real combat. */
    beginJewelHoloParkFaction(details={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.beginJewelHoloParkFaction(details??{})));}
        catch(error){return fail('JEWEL_HOLO_FACTION_FAILED',messageOf(error));}
    }
    /** P208: Holo-Park scene state tied to actual combat rounds. */
    enterJewelHoloPark(details={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.enterJewelHoloPark(details??{})));}
        catch(error){return fail('JEWEL_HOLO_ENTER_FAILED',messageOf(error));}
    }
    recordJewelHoloParkRound(details={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.recordJewelHoloParkRound(details??{})));}
        catch(error){return fail('JEWEL_HOLO_ROUND_FAILED',messageOf(error));}
    }
    confirmJewelHoloParkOutcome(details={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.confirmJewelHoloParkOutcome(details??{})));}
        catch(error){return fail('JEWEL_HOLO_END_FAILED',messageOf(error));}
    }
    /** P232: Holo-Park narrative clock uses only persistent task minutes. */
    enterJewelHoloNarrativeClock(details={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.enterJewelHoloNarrativeClock(details??{})));}catch(e){return fail('JEWEL_HOLO_NARRATIVE_START_FAILED',messageOf(e));}}
    advanceJewelHoloNarrativeClock(details={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.advanceJewelHoloNarrativeClock(details??{})));}catch(e){return fail('JEWEL_HOLO_NARRATIVE_SHIFT_FAILED',messageOf(e));}}
    endJewelHoloNarrativeClock(details={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.endJewelHoloNarrativeClock(details??{})));}catch(e){return fail('JEWEL_HOLO_NARRATIVE_END_FAILED',messageOf(e));}}
    /** P210: one real landscape effect with a spent narrative symbol. */
    confirmJewelHoloNpcFountainMovement(details={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.confirmJewelHoloNpcFountainMovement(details??{})));}
        catch(error){return fail('JEWEL_HOLO_FOUNTAIN_MOVE_FAILED',messageOf(error));}
    }
    confirmJewelHoloNpcVisibility(details={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.confirmJewelHoloNpcVisibility(details??{})));}
        catch(error){return fail('JEWEL_HOLO_VISIBILITY_FAILED',messageOf(error));}
    }
    settleJewelHoloNpcDespair(details={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.settleJewelHoloNpcDespair(details??{})));}
        catch(error){return fail('JEWEL_HOLO_DESPAIR_FALLBACK_FAILED',messageOf(error));}
    }
    applyJewelHoloParkEffect(details={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.applyJewelHoloParkEffect(details??{})));}
        catch(error){return fail('JEWEL_HOLO_EFFECT_FAILED',messageOf(error));}
    }
    /** P214: GM-confirmed physical weapon recovery. */
    recoverJewelHoloWeapon(details={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.recoverJewelHoloWeapon(details??{})));}
        catch(error){return fail('JEWEL_HOLO_WEAPON_RECOVERY_FAILED',messageOf(error));}
    }
    /** P214: return a reserved NPC Threat to the original strain fallback. */
    settleJewelHoloNpcThreat(details={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.settleJewelHoloNpcThreat(details??{})));}
        catch(error){return fail('JEWEL_HOLO_NPC_THREAT_FALLBACK_FAILED',messageOf(error));}
    }
    /** P213: real companion movement clears only that companion's transport setback. */
    confirmJewelHoloCompanionMovement(details={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.confirmJewelHoloCompanionMovement(details??{})));}
        catch(error){return fail('JEWEL_HOLO_COMPANION_MOVEMENT_FAILED',messageOf(error));}
    }
    /** P221: verified outdoor Wing Guard patrol checks and Venture alert. */
    recordJewelArendPatrol(details={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.recordJewelArendPatrol(details??{})));}
        catch(error){return fail('JEWEL_AREND_PATROL_FAILED',messageOf(error));}
    }
    resolveJewelResidencePatrolCheck(details={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.resolveJewelResidencePatrolCheck(details??{})));}
        catch(error){return fail('JEWEL_RESIDENCE_PATROL_CHECK_FAILED',messageOf(error));}
    }
    /** P226: travel pursuit roll and sequential GM-confirmed physical factions. */
    resolveJewelPursuitTravelCheck(details={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.resolveJewelPursuitTravelCheck(details??{})));}
        catch(error){return fail('JEWEL_PURSUIT_TRAVEL_CHECK_FAILED',messageOf(error));}
    }
    beginJewelPursuitGroup(details={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.beginJewelPursuitGroup(details??{})));}
        catch(error){return fail('JEWEL_PURSUIT_GROUP_FAILED',messageOf(error));}
    }
    /** P225: Wing Guard physically confirmed after a failed residence patrol. */
    beginJewelResidenceWingResponse(details={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.beginJewelResidenceWingResponse(details??{})));}
        catch(error){return fail('JEWEL_RESIDENCE_WING_RESPONSE_FAILED',messageOf(error));}
    }
    /** P223: four separate Storm IV patrol vehicles and source-backed real combat. */
    deployJewelStormIvPatrol(details = {}) { const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.deployJewelStormIvPatrol(details??{})));}catch(e){return fail('JEWEL_STORM_IV_DEPLOY_FAILED',messageOf(e));} }
    beginJewelStormIvCombat(details = {}) { const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.beginJewelStormIvCombat(details??{})));}catch(e){return fail('JEWEL_STORM_IV_COMBAT_FAILED',messageOf(e));} }
    resolveJewelStormIvCombat(details = {}) { const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.resolveJewelStormIvCombat(details??{})));}catch(e){return fail('JEWEL_STORM_IV_OUTCOME_FAILED',messageOf(e));} }
    clearJewelStormIvPatrol(details = {}) { const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.clearJewelStormIvPatrol(details??{})));}catch(e){return fail('JEWEL_STORM_IV_CLEAR_FAILED',messageOf(e));} }
    resolveJewelStormIvIncomingAttack(details = {}) { const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.resolveJewelStormIvIncomingAttack(details??{})));}catch(e){return fail('JEWEL_STORM_IV_INCOMING_FAILED',messageOf(e));} }
    /** P231: Wing Guard pursuit AFTER actual boarding/takeoff, four at a time. */
    arriveJewelStarportSquadron(details={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.arriveJewelStarportSquadron(details??{})));}catch(e){return fail('JEWEL_STARPORT_CLOUD_ARRIVAL_FAILED',messageOf(e));}}
    beginJewelStarportCloudCombat(details={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.beginJewelStarportCloudCombat(details??{})));}catch(e){return fail('JEWEL_STARPORT_CLOUD_COMBAT_FAILED',messageOf(e));}}
    resolveJewelStarportCloudCombat(details={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.resolveJewelStarportCloudCombat(details??{})));}catch(e){return fail('JEWEL_STARPORT_CLOUD_OUTCOME_FAILED',messageOf(e));}}
    clearJewelStarportSquadron(details={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.clearJewelStarportSquadron(details??{})));}catch(e){return fail('JEWEL_STARPORT_CLOUD_CLEAR_FAILED',messageOf(e));}}
    resolveJewelStarportCloudIncomingAttack(details={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.resolveJewelStarportCloudIncomingAttack(details??{})));}catch(e){return fail('JEWEL_STARPORT_CLOUD_INCOMING_FAILED',messageOf(e));}}
    /** P224: real physical custody, ship boarding and astrogation escape. */
    registerJewelCustody(details={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.registerJewelCustody(details??{})));}catch(e){return fail('JEWEL_BESPIN_CUSTODY_FAILED',messageOf(e));}}
    transferJewelCustody(details={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.transferJewelCustody(details??{})));}catch(e){return fail('JEWEL_CUSTODY_TRANSFER_FAILED',messageOf(e));}}
    confirmJewelStarportRoute(details={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.confirmJewelStarportRoute(details??{})));}catch(e){return fail('JEWEL_STARPORT_ROUTE_FAILED',messageOf(e));}}
    registerJewelStarportDestruction(details={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.registerJewelStarportDestruction(details??{})));}catch(e){return fail('JEWEL_STARPORT_DESTRUCTION_FAILED',messageOf(e));}}
    attemptJewelStarportShipAccess(details={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.attemptJewelStarportShipAccess(details??{})));}catch(e){return fail('JEWEL_STARPORT_ACCESS_FAILED',messageOf(e));}}
    registerJewelBoarding(details={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.registerJewelBoarding(details??{})));}catch(e){return fail('JEWEL_BESPIN_BOARD_FAILED',messageOf(e));}}
    executeJewelBespinEscape(details={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.executeJewelBespinEscape(details??{})));}catch(e){return fail('JEWEL_BESPIN_ESCAPE_FAILED',messageOf(e));}}
    /** P222: pay source-backed Episode III XP after verified real events. */
    grantJewelEpisode3Reward(details={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.grantJewelEpisode3Reward(details??{})));}
        catch(error){return fail('JEWEL_EP3_XP_FAILED',messageOf(error));}
    }
    /** P220: official holding security and Arend nemesis, separate real combats. */
    beginJewelArendPropertyEncounter(details={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.beginJewelArendPropertyEncounter(details??{})));}
        catch(error){return fail('JEWEL_AREND_PROPERTY_FAILED',messageOf(error));}
    }
    /** P219: Arend's fate and Aris's real alliance, GM-confirmed. */
    recordJewelArendOutcome(details={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.recordJewelArendOutcome(details??{})));}
        catch(error){return fail('JEWEL_AREND_OUTCOME_FAILED',messageOf(error));}
    }
    recordJewelArisAlliance(details={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.recordJewelArisAlliance(details??{})));}
        catch(error){return fail('JEWEL_ARIS_ALLIANCE_FAILED',messageOf(error));}
    }
    /** P218: official Lando pardon and 5 Favor per confirmed player character. */
    grantJewelLandoPardon(details={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.grantJewelLandoPardon(details??{})));}
        catch(error){return fail('JEWEL_LANDO_PARDON_FAILED',messageOf(error));}
    }
    /** P207: source-backed Arend holdings and d100 visits. */
    investigateJewelArendHoldings(details={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.investigateJewelArendHoldings(details??{})));}
        catch(error){return fail('JEWEL_AREND_LEADS_FAILED',messageOf(error));}
    }
    visitJewelArendHolding(details={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.visitJewelArendHolding(details??{})));}
        catch(error){return fail('JEWEL_AREND_VISIT_FAILED',messageOf(error));}
    }
    /** P199: buy a real restraining bolt from the official EotE gear table. */
    buyJewelRestrainingBolt(details={}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.buyJewelRestrainingBolt(details??{})));}
        catch(error){return fail('JEWEL_BOLT_BUY_FAILED',messageOf(error));}
    }
    /** P199: purchase/replace full slicing gear; remove future signature penalty. */
    replaceJewelSlicerTools(details={}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.replaceJewelSlicerTools(details??{})));}
        catch(error){return fail('JEWEL_SLICER_TOOLS_FAILED',messageOf(error));}
    }
    /** P194: real custody/disposal decisions for the G-100 evidence. */
    resolveJewelBankEvidence(details={}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.resolveJewelBankEvidence(details??{})));}
        catch(error){return fail('JEWEL_BANK_EVIDENCE_FAILED',messageOf(error));}
    }
    /** P195: real opposed-narrative negotiation to preserve G-100 intact. */
    resolveJewelLiveDroid(details={}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.resolveJewelLiveDroid(details??{})));}
        catch(error){return fail('JEWEL_LIVE_DROID_FAILED',messageOf(error));}
    }
    /** P191: explicit post-roll choices for Table 2-1. */
    spendJewelBankSymbols(details={}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.spendJewelBankSymbols(details??{})));}
        catch(error){return fail('JEWEL_BANK_SPEND_FAILED',messageOf(error));}
    }
    /** P188: approved encryption/Lobot preparation and actual spike fabrication. */
    prepareJewelBank(details={}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.prepareJewelBank(details??{})));}
        catch(error){return fail('JEWEL_BANK_PREP_FAILED',messageOf(error));}
    }
    /** P190: real NPC slicer hire, GM price and persisted payment. */
    hireJewelBankSlicer(details={}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.hireJewelBankSlicer(details??{})));}
        catch(error){return fail('JEWEL_SLICER_HIRE_FAILED',messageOf(error));}
    }
    /** P186: dice and actual vehicle consequences, not a decorative GM result. */
    resolveJewelGrandPrixLeg(details = {}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.resolveJewelGrandPrixLeg(details??{})));}
        catch(error){return fail('JEWEL_RACE_ROLL_FAILED',messageOf(error));}
    }
    startPublishedAdventureSession(adventureId = 'under-black-sun', options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.startPublishedAdventureSession(String(adventureId ?? 'under-black-sun'), options ?? {}))); }
        catch (error) { return fail('PUBLISHED_ADVENTURE_START_FAILED', messageOf(error)); }
    }
    advancePublishedAdventure(choiceId, options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.advancePublishedAdventure(String(choiceId ?? ''), options ?? {}))); }
        catch (error) { return fail('PUBLISHED_ADVENTURE_ADVANCE_FAILED', messageOf(error)); }
    }
    resolvePublishedAdventureCheck(checkIndex = 0, options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.resolvePublishedAdventureCheck(Number(checkIndex), options ?? {}))); }
        catch (error) { return fail('PUBLISHED_ADVENTURE_CHECK_FAILED', messageOf(error)); }
    }
    beginPublishedAdventureEncounter(encounterId = null, options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.beginPublishedAdventureEncounter(encounterId ? String(encounterId) : null, options ?? {}))); }
        catch (error) { return fail('PUBLISHED_ADVENTURE_ENCOUNTER_FAILED', messageOf(error)); }
    }
    advancePublishedImperialClock(options={}){
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.advancePublishedImperialClock(options??{})));}
        catch(error){return fail('PUBLISHED_IMPERIAL_CLOCK_FAILED',messageOf(error));}
    }
    restartPublishedAdventure(adventureId = 'under-black-sun', options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.restartPublishedAdventure(String(adventureId ?? 'under-black-sun'), options ?? {}))); }
        catch (error) { return fail('PUBLISHED_ADVENTURE_RESTART_FAILED', messageOf(error)); }
    }
    continuePublishedAdventureToSandbox(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.continuePublishedAdventureToSandbox(options ?? {}))); }
        catch (error) { return fail('PUBLISHED_ADVENTURE_CONTINUE_FAILED', messageOf(error)); }
    }
    restartSandboxPreservingParty() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.restartSandboxPreservingParty())); }
        catch (error) { return fail('SANDBOX_RESTART_FAILED', messageOf(error)); }
    }
    travelToLocation(locationId, options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.travelToLocation(String(locationId ?? ''), options ?? {}))); }
        catch (error) { return fail('TRAVEL_FAILED', messageOf(error)); }
    }
    scenePlayerView() {
        let state;
        try {
            state = this.repository.load();
        }
        catch (error) {
            return fail('STATE_READ_FAILED', messageOf(error));
        }
        if (!state)
            return fail('NO_CAMPAIGN', 'Nenhuma campanha carregada.');
        try {
            const world = state.world ?? {};
            const sceneLocationId = state.scene?.locationId ?? null;
            const locationCandidate = sceneLocationId ? world.locations?.[sceneLocationId] ?? null : null;
            const location = locationCandidate?.knownToPlayer
                ? { id: locationCandidate.id, name: String(locationCandidate.name ?? ''), description: String(locationCandidate.description ?? '') }
                : null;
            const participants = [];
            const seenParticipants = new Set();
            const sceneVisibleNpcIds = Array.isArray(state.scene?.visibleNpcIds) ? new Set(state.scene.visibleNpcIds.map(String)) : null;
            const sceneNarrativeText = String(state.scene?.narration ?? state.master?.narration ?? '').toLocaleLowerCase('pt-BR');
            const pushParticipant = (value) => {
                const name = String(value?.name ?? '').trim();
                const identityKey = value?.id ? `id:${String(value.id)}` : `name:${name.toLocaleLowerCase('pt-BR')}`;
                if (!name || seenParticipants.has(identityKey) || participants.length >= 3)
                    return;
                seenParticipants.add(identityKey);
                participants.push({
                    id: value?.id ? String(value.id) : null,
                    kind: String(value?.kind ?? 'npc'),
                    tokenEligible: value?.tokenEligible !== false && Boolean(value?.id),
                    name,
                    meta: String(value?.meta ?? '').trim(),
                    status: String(value?.status ?? '').trim(),
                    species: String(value?.species ?? '').trim(),
                    sex: String(value?.sex ?? '').trim(),
                    gender: String(value?.gender ?? '').trim(),
                    origin: String(value?.origin ?? '').trim(),
                    career: String(value?.career ?? '').trim(),
                    specialization: String(value?.specialization ?? '').trim(),
                    occupation: String(value?.occupation ?? '').trim(),
                    ageYears: Number.isFinite(Number(value?.ageYears)) ? Math.trunc(Number(value.ageYears)) : null,
                    appearance: String(value?.appearance ?? '').trim(),
                    visualFacts: Array.isArray(value?.visualFacts) ? value.visualFacts.map(String).filter(Boolean).slice(0, 8) : [],
                    visualTags: Array.isArray(value?.visualTags) ? value.visualTags.map(String).filter(Boolean).slice(0, 12) : [],
                });
            };
            for (const npc of Object.values(world.npcs ?? {})) {
                if (!npc?.knownToPlayer)
                    continue;
                if (sceneVisibleNpcIds?.size) {
                    if (!sceneVisibleNpcIds.has(String(npc.id)))
                        continue;
                }
                else {
                    if (!sceneNarrativeText)
                        continue;
                    const needle = String(npc.name ?? '').trim().toLocaleLowerCase('pt-BR');
                    if (!needle || !sceneNarrativeText.includes(needle))
                        continue;
                }
                if (sceneLocationId && npc.locationId && npc.locationId !== sceneLocationId)
                    continue;
                pushParticipant({
                    id: npc.id,
                    kind: 'npc',
                    name: npc.name,
                    meta: npc.relation || 'Conhecido',
                    status: npc.status || '',
                    species: npc.species || '', sex: npc.sex || '', gender: npc.gender || '', origin: npc.origin || '', career: npc.career || '', specialization: npc.specialization || '', occupation: npc.occupation || '', ageYears: npc.ageYears ?? null, appearance: npc.appearance || '',
                    visualFacts: (npc.facts ?? []).filter(f => f?.knownToPlayer).map(f => f.text),
                    visualTags: npc.tags ?? [],
                });
            }
            if (state.combat?.active) {
                for (const adversary of state.combat.adversaries ?? []) {
                    pushParticipant({
                        id: adversary.id ? `combat:${adversary.id}` : null,
                        kind: 'adversary',
                        name: adversary.name || adversary.id,
                        meta: 'Em confronto',
                        status: adversary.status || '',
                        visualFacts: Array.isArray(adversary.facts) ? adversary.facts.map(f => typeof f === 'string' ? f : f?.text).filter(Boolean) : [],
                        visualTags: Array.isArray(adversary.tags) ? adversary.tags : [],
                    });
                }
            }
            if (state.vehicles?.combat?.active) {
                const targetId = state.vehicles.combat.targetVehicleId;
                const target = targetId ? state.vehicles.vehicles?.[targetId] ?? null : null;
                if (target)
                    pushParticipant({ id: target.id ? `vehicle:${target.id}` : null, kind: 'vehicle', tokenEligible: false, name: target.name, meta: 'Veículo / nave alvo', status: 'Em confronto' });
            }
            const activeVehicleId = state.vehicles?.activeVehicleId ?? null;
            const activeVehicle = activeVehicleId ? state.vehicles?.vehicles?.[activeVehicleId] ?? null : null;
            const defense = activeVehicle?.defense ?? null;
            const vehicle = activeVehicle ? {
                name: String(activeVehicle.name ?? ''),
                hull: `${Number(activeVehicle.hullDamage ?? 0)}/${Number(activeVehicle.hullThreshold ?? 0)}`,
                strain: `${Number(activeVehicle.systemStrain ?? 0)}/${Number(activeVehicle.systemStrainThreshold ?? 0)}`,
                speed: `${Number(activeVehicle.speed ?? 0)}/${Number(activeVehicle.speedMax ?? 0)}`,
                defense: defense ? `P${Number(defense.proa ?? 0)} · B${Number(defense.babor ?? 0)} · E${Number(defense.estribor ?? 0)} · R${Number(defense.popa ?? 0)}` : '—',
            } : null;
            let situation = '';
            const pendingQuestion = String(state.master?.pendingQuestion ?? '').trim();
            const pendingCheck = state.master?.suggestedCheck ?? null;
            if (pendingQuestion) {
                situation = pendingQuestion;
            }
            else if (pendingCheck?.reason) {
                situation = String(pendingCheck.reason).trim();
            }
            else if (state.vehicles?.combat?.active) {
                situation = 'A nave está sob pressão. Escolha o próximo movimento.';
            }
            else if (state.combat?.active) {
                situation = 'O confronto está aberto. Escolha sua próxima ação.';
            }
            else if (String(state.scene?.narration ?? '').trim()) {
                situation = String(state.scene.narration).trim();
            }
            else if (state.session?.status === 'ativa') {
                situation = 'Escolha uma das ações abaixo ou descreva outra ação ao Mestre.';
            }
            else {
                situation = 'Inicie a sessão para receber a próxima decisão do Mestre.';
            }
            const sceneVisual = sceneLibrary_mjs_1.resolveSceneVisualForState(state);
            return ok({
                scene: { title: String(state.scene?.title ?? ''), location, imageAssetId: sceneVisual.asset?.assetId ?? null, imageSource: sceneVisual.source, image: sceneVisual.descriptor, imageFallback: sceneVisual.asset ? null : 'gemini' },
                situation: String(situation).slice(0, 900),
                participants,
                suggestedActions: Array.isArray(state.master?.suggestedActions) ? state.master.suggestedActions.map(String).filter(Boolean).slice(0, 6) : [],
                suggestedCheck: state.master?.suggestedCheck ? clone(state.master.suggestedCheck) : null,
                vehicle,
                session: state.session ? { status: state.session.status, phase: state.session.phase, number: state.session.number } : null,
                lastCheck: state.lastCheck ? clone(state.lastCheck) : null,
            });
        }
        catch (error) {
            return fail('PLAYER_VIEW_FAILED', messageOf(error));
        }
    }
    resolveSuggestedMasterCheck() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok)
            return controllerResult;
        try {
            const check = controllerResult.data.resolveSuggestedMasterCheck();
            return ok({ check: clone(check), state: clone(controllerResult.data.state) });
        }
        catch (error) {
            return fail('CHECK_REJECTED', messageOf(error));
        }
    }
    _controllerForExistingCampaign() {
        let existing;
        try {
            existing = this.repository.load();
        }
        catch (error) {
            return fail('STATE_READ_FAILED', messageOf(error));
        }
        if (!existing)
            return fail('NO_CAMPAIGN', 'Nenhuma campanha carregada.');
        try {
            return ok(new controller_mjs_1.GameController(this.repository));
        }
        catch (error) {
            return fail('STATE_READ_FAILED', messageOf(error));
        }
    }
    exportCharacterJson() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok)
            return controllerResult;
        try {
            const game = controllerResult.data;
            const json = game.exportCharacter();
            const name = String(game.state.player?.character?.name ?? 'personagem').trim() || 'personagem';
            const safeName = name.normalize('NFKD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-zA-Z0-9._-]+/g, '_').replace(/^_+|_+$/g, '') || 'personagem';
            return ok({ fileName: `${safeName}.json`, json });
        }
        catch (error) {
            return fail('CHARACTER_EXPORT_FAILED', messageOf(error));
        }
    }
    generateRandomCharacter(options = {}, { createSnapshot = true } = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok)
            return controllerResult;
        const game = controllerResult.data;
        let character;
        try {
            character = String(game.state.campaignMode ?? 'EOTE').toUpperCase() === 'AOR'
                ? characterCreation_mjs_1.createRandomAorCoreCharacter(options)
                : game.buildRandomCharacter(options);
        }
        catch (error) {
            return fail('RANDOM_CHARACTER_FAILED', messageOf(error));
        }
        try {
            let snapshotId = null;
            if (createSnapshot && game.state.player?.character)
                snapshotId = this.snapshots.create(game.state, 'Antes de gerar ficha aleatória').id;
            const created = game.setCharacter(character);
            return ok({ character: clone(created), snapshotId, state: clone(game.state) });
        }
        catch (error) {
            return fail('RANDOM_CHARACTER_FAILED', messageOf(error));
        }
    }
    importCharacterJson(text, { createSnapshot = true } = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok)
            return controllerResult;
        let character;
        try {
            character = character_mjs_1.importCharacterJson(String(text ?? ''));
        }
        catch (error) {
            return fail('CHARACTER_IMPORT_FAILED', messageOf(error));
        }
        const game = controllerResult.data;
        try {
            let snapshotId = null;
            if (createSnapshot)
                snapshotId = this.snapshots.create(game.state, 'Antes de importar ficha').id;
            const imported = game.setCharacter(character);
            return ok({ character: clone(imported), snapshotId, state: clone(game.state) });
        }
        catch (error) {
            return fail('CHARACTER_IMPORT_FAILED', messageOf(error));
        }
    }
    exportCampaignBackup() {
        let state;
        try {
            state = this.repository.load();
        }
        catch (error) {
            return fail('STATE_READ_FAILED', messageOf(error));
        }
        if (!state)
            return fail('NO_CAMPAIGN', 'Nenhuma campanha carregada.');
        try {
            const json = storage_mjs_1.exportStateJson(state);
            const name = String(state.name ?? 'campanha').trim() || 'campanha';
            const safeName = name.normalize('NFKD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-zA-Z0-9._-]+/g, '_').replace(/^_+|_+$/g, '') || 'campanha';
            return ok({ fileName: `${safeName}.backup.json`, json });
        }
        catch (error) {
            return fail('BACKUP_EXPORT_FAILED', messageOf(error));
        }
    }
    importCampaignBackup(text, { createSnapshot = true } = {}) {
        let imported;
        try {
            imported = storage_mjs_1.importStateJson(String(text ?? ''));
        }
        catch (error) {
            return fail('BACKUP_IMPORT_FAILED', messageOf(error));
        }
        let existing = null;
        try {
            existing = this.repository.load();
        }
        catch (error) {
            return fail('STATE_READ_FAILED', messageOf(error));
        }
        try {
            let snapshotId = null;
            if (createSnapshot && existing)
                snapshotId = this.snapshots.create(existing, 'Antes de restaurar backup').id;
            this.repository.save(imported);
            return ok({ state: clone(imported), snapshotId });
        }
        catch (error) {
            return fail('BACKUP_IMPORT_FAILED', messageOf(error));
        }
    }
    listSnapshots() {
        try {
            return ok(this.snapshots.list().map(item => ({ id: item.id, label: item.label, createdAt: item.createdAt })));
        }
        catch (error) {
            return fail('SNAPSHOT_READ_FAILED', messageOf(error));
        }
    }
    createSnapshot(label = 'backup manual') {
        let state;
        try {
            state = this.repository.load();
        }
        catch (error) {
            return fail('STATE_READ_FAILED', messageOf(error));
        }
        if (!state)
            return fail('NO_CAMPAIGN', 'Nenhuma campanha carregada.');
        try {
            const item = this.snapshots.create(state, String(label ?? 'backup manual').trim() || 'backup manual');
            return ok({ id: item.id, label: item.label, createdAt: item.createdAt });
        }
        catch (error) {
            return fail('SNAPSHOT_CREATE_FAILED', messageOf(error));
        }
    }
    creationCatalog(origin = null) {
        try {
            const state = this.repository.load();
            const mode = String(state?.campaignMode ?? 'EOTE').toUpperCase();
            const requestedOrigin = String(origin ?? (mode === 'AOR' ? 'AOR' : 'EOTE')).toUpperCase();
            const useAor = mode === 'AOR' || (mode === 'MIXED' && requestedOrigin === 'AOR');
            const speciesSource = useAor ? characterCreation_mjs_1.AOR_SPECIES : eoteCharacterCreation_mjs_1.EOTE_SPECIES;
            const careerSource = useAor ? characterCreation_mjs_1.AOR_CAREERS : eoteCharacterCreation_mjs_1.EOTE_CAREERS;
            const species = Object.entries(speciesSource).map(([key, value]) => ({
                key,
                label: value.label,
                characteristics: clone(value.characteristics),
                woundBase: value.woundBase,
                strainBase: value.strainBase,
                xp: value.xp,
                careerFreeCount: value.careerFreeCount ?? 4,
                specializationFreeCount: value.specFreeCount ?? 2,
                freeSkill: value.freeSkill ? clone(value.freeSkill) : null,
                freeTalents: clone(value.freeTalents ?? []),
                speciesChoice: value.speciesChoice ? clone(value.speciesChoice) : null,
                abilities: clone(value.abilities ?? []),
            }));
            const careers = Object.entries(careerSource).map(([key, value]) => ({
                key,
                label: value.label,
                skills: clone(value.skills),
                specializations: Object.entries(value.specializations ?? {}).map(([specializationKey, specialization]) => ({ key: specializationKey, label: specialization.label, skills: clone(specialization.skills) })),
            }));
            return ok({
                profile: 'EOTE_CORE',
                activeProfile: useAor ? 'AOR_CORE' : 'EOTE_CORE',
                campaignMode: mode,
                creationOrigin: useAor ? 'AOR' : 'EOTE',
                species,
                careers,
                obligationTypes: clone(useAor ? characterCreation_mjs_1.DUTY_TYPES_PT_BR : eoteCharacterCreation_mjs_1.OBLIGATION_TYPES_PT_BR),
                characteristicLabels: clone(labelsPtBr_mjs_1.CHARACTERISTIC_LABELS_PT_BR),
                skillLabels: clone(labelsPtBr_mjs_1.SKILL_LABELS_PT_BR),
            });
        }
        catch (error) { return fail('CREATION_CATALOG_FAILED', messageOf(error)); }
    }
    creationPreview(options = {}) {
        try {
            const state = this.repository.load();
            const mode = String(state?.campaignMode ?? 'EOTE').toUpperCase();
            const requestedOrigin = String(options?.creationOrigin ?? (mode === 'AOR' ? 'AOR' : 'EOTE')).toUpperCase();
            const useAor = mode === 'AOR' || (mode === 'MIXED' && requestedOrigin === 'AOR');
            if (!useAor) {
                const preview = eoteCharacterCreation_mjs_1.eoteCreationPreview(options);
                if (!preview) return fail('INVALID_INPUT', 'Selecione uma espécie válida para calcular o orçamento de criação.');
                return ok(clone(preview));
            }
            const mapped = { ...clone(options), dutyType: options.obligationType, dutyReductions: clone(options.obligationBenefits ?? {}) };
            const preview = characterCreation_mjs_1.creationPreview(mapped);
            if (!preview) return fail('INVALID_INPUT', 'Selecione uma espécie válida para calcular o orçamento de criação.');
            return ok({
                xpBudget: preview.xpBudget, characteristicCost: preview.characteristicCost, xpRemaining: preview.xpRemaining, credits: preview.credits,
                obligation: preview.duty, startingObligation: preview.duty + preview.dutySpent, additionalObligation: -preview.dutySpent,
                obligationOfficial: preview.dutyOfficial, obligationNote: preview.dutyNote ?? null, speciesXpBonus: 0,
                activeProfile: 'AOR_CORE', duty: preview.duty, dutySpent: preview.dutySpent
            });
        }
        catch (error) { return fail('CREATION_PREVIEW_FAILED', messageOf(error)); }
    }
    createCustomCharacter(options = {}, { createSnapshot = true } = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        const game = controllerResult.data;
        let snapshotId = null;
        try {
            const mode = String(game.state.campaignMode ?? 'EOTE').toUpperCase();
            const requestedOrigin = String(options?.creationOrigin ?? (mode === 'AOR' ? 'AOR' : 'EOTE')).toUpperCase();
            const useAor = mode === 'AOR' || (mode === 'MIXED' && requestedOrigin === 'AOR');
            let mapped = null;
            if (useAor) {
                mapped = { ...clone(options), dutyType: options.obligationType, dutyReductions: clone(options.obligationBenefits ?? {}) };
                characterCreation_mjs_1.createAorCoreCharacter(mapped);
            } else {
                eoteCharacterCreation_mjs_1.createEoteCoreCharacter(options);
            }
            if (createSnapshot && game.state.player?.character) snapshotId = this.snapshots.create(game.state, 'Antes de criar ficha manual').id;
            const created = useAor ? game.createAorCharacter(mapped) : game.createCustomCharacter(options);
            return ok({ character: clone(created), snapshotId, state: clone(game.state) });
        }
        catch (error) {
            if (snapshotId) { try { this.snapshots.remove(snapshotId); } catch {} }
            return fail('CUSTOM_CHARACTER_FAILED', messageOf(error));
        }
    }
    ownedSpecializations() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.ownedSpecializations())); }
        catch (error) { return fail('PROGRESSION_FAILED', messageOf(error)); }
    }
    specializationPurchaseCost(careerKey, specializationKey) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.specializationPurchaseCost(String(careerKey ?? ''), String(specializationKey ?? '')))); }
        catch (error) { return fail('PROGRESSION_FAILED', messageOf(error)); }
    }
    buySpecialization(careerKey, specializationKey) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.buySpecialization(String(careerKey ?? ''), String(specializationKey ?? '')))); }
        catch (error) { return fail('PROGRESSION_FAILED', messageOf(error)); }
    }
    talentTree(selector = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.talentTree(selector ?? {}))); }
        catch (error) { return fail('PROGRESSION_FAILED', messageOf(error)); }
    }
    eligibleTalents(selector = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.eligibleTalents(selector ?? {}))); }
        catch (error) { return fail('PROGRESSION_FAILED', messageOf(error)); }
    }
    buyTalent(nodeId, options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.buyTalent(String(nodeId ?? ''), options ?? {}))); }
        catch (error) { return fail('PROGRESSION_FAILED', messageOf(error)); }
    }
    forceStatus() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.forceStatus())); }
        catch (error) { return fail('FORCE_FAILED', messageOf(error)); }
    }
    forceCatalog() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.forceCatalog())); }
        catch (error) { return fail('FORCE_FAILED', messageOf(error)); }
    }
    buyForcePower(powerKey) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.buyForcePower(String(powerKey ?? '')))); }
        catch (error) { return fail('FORCE_FAILED', messageOf(error)); }
    }
    buyForceUpgrade(powerKey, upgradeId) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.buyForceUpgrade(String(powerKey ?? ''), String(upgradeId ?? '')))); }
        catch (error) { return fail('FORCE_FAILED', messageOf(error)); }
    }
    rollForcePowerCheck(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.rollForcePowerCheck(options ?? {}))); }
        catch (error) { return fail('FORCE_FAILED', messageOf(error)); }
    }
    commitForce(effectId, count = 1) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.commitForce(String(effectId ?? ''), Number(count)))); }
        catch (error) { return fail('FORCE_FAILED', messageOf(error)); }
    }
    releaseForce(effectId) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.releaseForce(String(effectId ?? '')))); }
        catch (error) { return fail('FORCE_FAILED', messageOf(error)); }
    }
    activateSenseOngoing(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.activateSenseOngoing(options ?? {}))); }
        catch (error) { return fail('FORCE_FAILED', messageOf(error)); }
    }
    endSenseOngoing(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.endSenseOngoing(options ?? {}))); }
        catch (error) { return fail('FORCE_FAILED', messageOf(error)); }
    }
    activateInfluenceBasic(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.activateInfluenceBasic(options ?? {}))); }
        catch (error) { return fail('FORCE_FAILED', messageOf(error)); }
    }
    equipmentOwnedSummary() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.equipmentOwnedSummary())); }
        catch (error) { return fail('EQUIPMENT_STATE_FAILED', messageOf(error)); }
    }
    utilityBeltStatus(){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.utilityBeltStatus()));}
        catch(error){return fail('UTILITY_BELT_READ_FAILED',messageOf(error));}
    }
    useUtilityBelt(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.useUtilityBelt(options??{})));}
        catch(error){return fail('UTILITY_BELT_ACTION_FAILED',messageOf(error));}
    }
    tinkererStatus() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.tinkererStatus())); }
        catch (error) { return fail('TINKERER_STATUS_FAILED', messageOf(error)); }
    }
    applyTinkererHardPoint(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.applyTinkererHardPoint(options ?? {}))); }
        catch (error) { return fail('TINKERER_APPLY_FAILED', messageOf(error)); }
    }
    attachmentSummary() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.attachmentSummary())); }
        catch (error) { return fail('ATTACHMENT_STATE_FAILED', messageOf(error)); }
    }
    proposeAttachmentAcquisition(itemId) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.proposeAttachmentAcquisition(String(itemId ?? '')))); }
        catch (error) { return fail('ATTACHMENT_ACQUISITION_FAILED', messageOf(error)); }
    }
    installOwnedAttachment(assetId, weaponId) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.installOwnedAttachment(String(assetId ?? ''), String(weaponId ?? '')))); }
        catch (error) { return fail('ATTACHMENT_INSTALL_FAILED', messageOf(error)); }
    }
    removeOwnedAttachment(assetId) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.removeOwnedAttachment(String(assetId ?? '')))); }
        catch (error) { return fail('ATTACHMENT_REMOVE_FAILED', messageOf(error)); }
    }
    attemptAttachmentMod(assetId, modId) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.attemptAttachmentMod(String(assetId ?? ''), String(modId ?? '')))); }
        catch (error) { return fail('ATTACHMENT_MOD_FAILED', messageOf(error)); }
    }
    equipWeapon(weaponId) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.equipWeapon(String(weaponId ?? '')))); }
        catch (error) { return fail('EQUIPMENT_STATE_FAILED', messageOf(error)); }
    }
    equipArmor(itemId) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.equipArmor(String(itemId ?? '')))); }
        catch (error) { return fail('EQUIPMENT_STATE_FAILED', messageOf(error)); }
    }
    unequipArmor(itemId = null) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.unequipArmor(itemId === null ? null : String(itemId)))); }
        catch (error) { return fail('EQUIPMENT_STATE_FAILED', messageOf(error)); }
    }
    cyberneticsSummary() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.cyberneticsSummary())); }
        catch (error) { return fail('CYBERNETICS_FAILED', messageOf(error)); }
    }
    proposeCyberneticAcquisition(itemId) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.proposeCyberneticAcquisition(String(itemId ?? '')))); }
        catch (error) { return fail('CYBERNETICS_ACQUISITION_FAILED', messageOf(error)); }
    }
    installOwnedCybernetic(assetId) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.installOwnedCybernetic(String(assetId ?? '')))); }
        catch (error) { return fail('CYBERNETICS_INSTALL_FAILED', messageOf(error)); }
    }
    removeOwnedCybernetic(assetId) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.removeOwnedCybernetic(String(assetId ?? '')))); }
        catch (error) { return fail('CYBERNETICS_REMOVE_FAILED', messageOf(error)); }
    }
    talentProgressionStatus() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try {
            const game=controllerResult.data;
            const character=game.state.player?.character;
            const owned=game.ownedSpecializations();
            return ok(clone({
                xp: character?.xp ?? null,
                ownedSpecializations: owned,
                trees: owned.map(ref=>({
                    ...ref,
                    tree: game.talentTree({careerKey:ref.careerKey,specializationKey:ref.specializationKey}),
                    eligible: game.eligibleTalents({careerKey:ref.careerKey,specializationKey:ref.specializationKey})
                }))
            }));
        }
        catch (error) { return fail('PROGRESSION_FAILED', messageOf(error)); }
    }
    recoveryRuntimeStatus() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try {
            const game=controllerResult.data;
            return ok(clone({
                health: game.healthStatus(),
                status: game.statusSummary(),
                combat: game.state.combat ?? null,
                character: game.state.player?.character ?? null,
                companions: game.companionRoster()
            }));
        }
        catch (error) { return fail('RECOVERY_FAILED', messageOf(error)); }
    }
    usePlayerStimpack(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.usePlayerStimpack(options ?? {}))); }
        catch (error) { return fail('RECOVERY_FAILED', messageOf(error)); }
    }
    useEmergencyRepairPatch() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.useEmergencyRepairPatch())); }
        catch (error) { return fail('RECOVERY_FAILED', messageOf(error)); }
    }
    companionRoster(){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.companionRoster()));}
        catch(error){return fail('COMPANION_READ_FAILED',messageOf(error));}
    }
    addCompanion(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.addCompanion(options??{})));}
        catch(error){return fail('COMPANION_CREATE_FAILED',messageOf(error));}
    }
    removeCompanion(companionId){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.removeCompanion(companionId)));}
        catch(error){return fail('COMPANION_REMOVE_FAILED',messageOf(error));}
    }
    applyCompanionDamage(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.applyCompanionDamage(options??{})));}
        catch(error){return fail('COMPANION_DAMAGE_FAILED',messageOf(error));}
    }
    applyStimApplication(options = {}) {
        const r = this._controllerForExistingCampaign();
        if (!r.ok) return r;
        try { return ok(clone(r.data.applyStimApplication(options ?? {}))); }
        catch (error) { return fail('STIM_APPLICATION_FAILED', messageOf(error)); }
    }
    recoverInBacta(options = {}) {
        const r = this._controllerForExistingCampaign();
        if (!r.ok) return r;
        try { return ok(clone(r.data.recoverInBacta(options ?? {}))); }
        catch (error) { return fail('BACTA_RECOVERY_FAILED', messageOf(error)); }
    }
    useMedicine(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.useMedicine(options ?? {}))); }
        catch (error) { return fail('RECOVERY_FAILED', messageOf(error)); }
    }
    restFullNight() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.restFullNight())); }
        catch (error) { return fail('RECOVERY_FAILED', messageOf(error)); }
    }
    environmentStatus() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.environmentStatus())); }
        catch (error) { return fail('ENVIRONMENT_FAILED', messageOf(error)); }
    }
    configureEnvironment(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.configureEnvironment(options ?? {}))); }
        catch (error) { return fail('ENVIRONMENT_FAILED', messageOf(error)); }
    }
    clearEnvironment() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.clearEnvironment())); }
        catch (error) { return fail('ENVIRONMENT_FAILED', messageOf(error)); }
    }
    applyEnvironmentTurnStart(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.applyEnvironmentTurnStart(options ?? {}))); }
        catch (error) { return fail('ENVIRONMENT_FAILED', messageOf(error)); }
    }
    resolveImpassableTerrain(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.resolveImpassableTerrain(options ?? {}))); }
        catch (error) { return fail('ENVIRONMENT_FAILED', messageOf(error)); }
    }
    resolveFall(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.resolveFall(options ?? {}))); }
        catch (error) { return fail('ENVIRONMENT_FAILED', messageOf(error)); }
    }
    resolveSkillCheck(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.resolveSkillCheck(options ?? {}))); }
        catch (error) { return fail('SKILL_CHECK_FAILED', messageOf(error)); }
    }
    commitLastTaskTime(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.commitLastTaskTime(options ?? {}))); }
        catch (error) { return fail('TASK_TIME_COMMIT_FAILED', messageOf(error)); }
    }
    naturalRerollStatus(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.naturalRerollStatus(options ?? {}))); }
        catch (error) { return fail('NATURAL_REROLL_STATUS_FAILED', messageOf(error)); }
    }
    aorSocialStatus() {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.aorSocialStatus()));}
        catch(e){return fail('AOR_SOCIAL_STATUS_FAILED',messageOf(e));}
    }
    performAoRSocialTalent(options={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.performAoRSocialTalent(options??{})));}
        catch(e){return fail('AOR_SOCIAL_FAILED',messageOf(e));}
    }
    resolveCompanionSkillCheck(options={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.resolveCompanionSkillCheck(options??{})));}
        catch(e){return fail('COMPANION_SKILL_FAILED',messageOf(e));}
    }
    useIntensePresence() {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.useIntensePresence()));}
        catch(e){return fail('INTENSE_PRESENCE_FAILED',messageOf(e));}
    }
    familiarSunsStatus() {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.familiarSunsStatus()));}
        catch(e){return fail('FAMILIAR_SUNS_STATUS_FAILED',messageOf(e));}
    }
    useFamiliarSuns(options={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.useFamiliarSuns(options??{})));}
        catch(e){return fail('FAMILIAR_SUNS_FAILED',messageOf(e));}
    }
    talentActionStatus() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.talentActionStatus())); }
        catch (error) { return fail('TALENT_ACTION_FAILED', messageOf(error)); }
    }
    resolveOpposedSkillCheck(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.resolveOpposedSkillCheck(options ?? {}))); }
        catch (error) { return fail('CHECK_TYPE_FAILED', messageOf(error)); }
    }
    resolveIncomingSocialInfluenceCheck(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.resolveIncomingSocialInfluenceCheck(options ?? {}))); }
        catch (error) { return fail('SOCIAL_CHECK_FAILED', messageOf(error)); }
    }
    resolveIncomingComputerIntrusionCheck(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.resolveIncomingComputerIntrusionCheck(options ?? {}))); }
        catch (error) { return fail('COMPUTER_INTRUSION_FAILED', messageOf(error)); }
    }
    resolveDirectedDroidSkillCheck(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.resolveDirectedDroidSkillCheck(options ?? {}))); }
        catch (error) { return fail('DROID_CHECK_FAILED', messageOf(error)); }
    }
    resolveAssistedSkillCheck(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.resolveAssistedSkillCheck(options ?? {}))); }
        catch (error) { return fail('CHECK_TYPE_FAILED', messageOf(error)); }
    }
    resolveCompetitiveSkillCheck(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.resolveCompetitiveSkillCheck(options ?? {}))); }
        catch (error) { return fail('CHECK_TYPE_FAILED', messageOf(error)); }
    }
    resolveSocialInfluenceCheck(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.resolveSocialInfluenceCheck(options ?? {}))); }
        catch (error) { return fail('SOCIAL_CHECK_FAILED', messageOf(error)); }
    }
    combatRuntimeStatus() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try {
            const game = controllerResult.data;
            return ok(clone({ combat: game.state.combat ?? null, health: game.healthStatus(), equipment: game.equipmentOwnedSummary(), character: game.state.player?.character ?? null, status: game.statusSummary(), destiny: game.destinySummary() }));
        }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    changeCombatRange(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.changeCombatRange(options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    takeCover(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.takeCover(options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    takeGuardedStance(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.takeGuardedStance(options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    activateFrenziedAttack(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.activateFrenziedAttack(options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    useDefensiveStanceTalent(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.useDefensiveStanceTalent(options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    confirmAoRFormationAlly(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
      try{return ok(clone(r.data.confirmAoRFormationAlly(options??{})));}catch(e){return fail('AOR_FORMATION_CONFIRM_FAILED',messageOf(e));}}
    aorP178Status(){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
      try{return ok(clone(r.data.aorP178Status()));}catch(e){return fail('AOR_P178_STATUS_FAILED',messageOf(e));}}
    aorP177Status(){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
      try{return ok(clone(r.data.aorP177Status()));}catch(e){return fail('AOR_P177_STATUS_FAILED',messageOf(e));}}
    useAoRP177Action(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
      try{return ok(clone(r.data.useAoRP177Action(options??{})));}catch(e){return fail('AOR_P177_ACTION_FAILED',messageOf(e));}}
    activatePendingImprovedStunningBlow(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
      try{return ok(clone(r.data.activatePendingImprovedStunningBlow(options??{})));}catch(e){return fail('AOR_P177_STUN_FAILED',messageOf(e));}}
    aorP176Status(){const r=this._controllerForExistingCampaign();if(!r.ok)return r;try{return ok(clone(r.data.aorP176Status()));}catch(e){return fail('AOR_P176_STATUS_FAILED',messageOf(e));}}
    useAoRBoughtInfo(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
      try{return ok(clone(r.data.useAoRBoughtInfo(options??{})));}catch(e){return fail('AOR_BOUGHT_INFO_FAILED',messageOf(e));}}
    useAoRKnownSchematic(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
      try{return ok(clone(r.data.useAoRKnownSchematic(options??{})));}catch(e){return fail('AOR_KNOWN_SCHEMATIC_FAILED',messageOf(e));}}
    useAoRFireControl(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
      try{return ok(clone(r.data.useAoRFireControl(options??{})));}catch(e){return fail('AOR_FIRE_CONTROL_FAILED',messageOf(e));}}
    aorCommandStatus(){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.aorCommandStatus()));}
        catch(e){return fail('AOR_COMMAND_STATUS_FAILED',messageOf(e));}
    }
    useAoRFieldCommander(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.useAoRFieldCommander(options??{})));}
        catch(e){return fail('AOR_FIELD_COMMANDER_FAILED',messageOf(e));}
    }
    useAoRCommandedCover(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.useAoRCommandedCover(options??{})));}
        catch(e){return fail('AOR_COMMANDED_COVER_FAILED',messageOf(e));}
    }
    useAoRCoordinatedAssault(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.useAoRCoordinatedAssault(options??{})));}
        catch(e){return fail('AOR_COORDINATED_ASSAULT_FAILED',messageOf(e));}
    }
    aorP170Status(){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.aorP170Status()));}
        catch(e){return fail('P170_STATUS_FAILED',messageOf(e));}
    }
    aorP179Status(){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
      try{return ok(clone(r.data.aorP179Status()));}catch(e){return fail('AOR_P179_STATUS_FAILED',messageOf(e));}}
    prepareItsNotThatBad(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
      try{return ok(clone(r.data.prepareItsNotThatBad(options??{})));}catch(e){return fail('AOR_P179_MEDIC_FAILED',messageOf(e));}}
    resolveIncomingBlast(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
      try{return ok(clone(r.data.resolveIncomingBlast(options??{})));}catch(e){return fail('AOR_P179_BLAST_FAILED',messageOf(e));}}
    setNpcCompanionTarget(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.setNpcCompanionTarget(options??{})));}
        catch(e){return fail('NPC_COMPANION_TARGET_FAILED',messageOf(e));}
    }
    useBodyGuard(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.useBodyGuard(options??{})));}
        catch(e){return fail('BODY_GUARD_FAILED',messageOf(e));}
    }
    resolveIncomingCompanionAttack(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.resolveIncomingCompanionAttack(options??{})));}
        catch(e){return fail('COMPANION_INCOMING_ATTACK_FAILED',messageOf(e));}
    }
    assignJuryRigged(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.assignJuryRigged(options??{})));}
        catch(e){return fail('JURY_RIGGED_FAILED',messageOf(e));}
    }
    useHardHeaded(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.useHardHeaded(options??{})));}
        catch(e){return fail('HARD_HEADED_FAILED',messageOf(e));}
    }
    useSideStep(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.useSideStep(options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }

useAoRMechanicFieldAction(options={}){
    const result=this._controllerForExistingCampaign();
    if(!result.ok)return result;
    try{return ok(clone(result.data.useAoRMechanicFieldAction(options??{})));}
    catch(error){return fail('AOR_MECHANIC_ACTION_FAILED',messageOf(error));}
}
resolveMerchantNegotiation(options={}){
    const result=this._controllerForExistingCampaign();
    if(!result.ok)return result;
    try{return ok(clone(result.data.resolveMerchantNegotiation(options??{})));}
    catch(error){return fail('MERCHANT_CHECK_FAILED',messageOf(error));}
}
completeMerchantSale(options={}){
    const result=this._controllerForExistingCampaign();
    if(!result.ok)return result;
    try{return ok(clone(result.data.completeMerchantSale(options??{})));}
    catch(error){return fail('MERCHANT_SALE_FAILED',messageOf(error));}
}
completeMerchantAcquisition(options={}){
    const result=this._controllerForExistingCampaign();
    if(!result.ok)return result;
    try{return ok(clone(result.data.completeMerchantAcquisition(options??{})));}
    catch(error){return fail('MERCHANT_PURCHASE_FAILED',messageOf(error));}
}
    legalSaleQuote(options={}){
        const controllerResult=this._controllerForExistingCampaign();
        if(!controllerResult.ok)return controllerResult;
        try{return ok(clone(controllerResult.data.legalSaleQuote(options??{})));}
        catch(error){return fail('ECONOMY_FAILED',messageOf(error));}
    }
    useFeint(options={}){
        const controllerResult=this._controllerForExistingCampaign();
        if(!controllerResult.ok)return controllerResult;
        try{return ok(clone(controllerResult.data.useFeint(options??{})));}
        catch(error){return fail('COMBAT_FAILED',messageOf(error));}
    }
    resolveIncomingIdentificationCheck(options={}){
        const controllerResult=this._controllerForExistingCampaign();
        if(!controllerResult.ok)return controllerResult;
        try{return ok(clone(controllerResult.data.resolveIncomingIdentificationCheck(options??{})));}
        catch(error){return fail('CHECK_FAILED',messageOf(error));}
    }
    useSecondWind() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.useSecondWind())); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    prepareDodge(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.prepareDodge(options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    usePreciseAim(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.usePreciseAim(options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    useSniperShot(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.useSniperShot(options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    prepareTargetedBlow() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.prepareTargetedBlow())); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    prepareAnatomyLessons() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.prepareAnatomyLessons())); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    dropProne(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.dropProne(options ?? {})));
        } catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    standUpFromProne(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.standUpFromProne(options ?? {})));
        } catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    drawWeaponInCombat(weaponId, options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.drawWeaponInCombat(String(weaponId ?? ''), options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    prepareWeaponForAttack(weaponId, options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.prepareWeaponForAttack(String(weaponId ?? ''), options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    toggleWeaponStunSetting(weaponId, options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.toggleWeaponStunSetting(String(weaponId ?? ''), options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    reloadLimitedAmmoWeapon(weaponId, options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.reloadLimitedAmmoWeapon(String(weaponId ?? ''), options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    adjudicateAttackDespairAmmoLoss(options = {}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.adjudicateAttackDespairAmmoLoss(options??{})));}
        catch(error){return fail('COMBAT_FAILED',messageOf(error));}
    }
    assignKnowledgeSpecialization(options={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.assignKnowledgeSpecialization(options??{})));}
        catch(error){return fail('PROGRESSION_FAILED',messageOf(error));}
    }
    assignSmoothTalker(options={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.assignSmoothTalker(options??{})));}
        catch(error){return fail('PROGRESSION_FAILED',messageOf(error));}
    }
    activateMentalFortress() {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.activateMentalFortress()));}
        catch(e){return fail('COMBAT_FAILED',messageOf(e));}
    }
    activateSteelyNerves() {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.activateSteelyNerves()));}
        catch(error){return fail('COMBAT_FAILED',messageOf(error));}
    }
    boardActiveVehicle(options={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.boardActiveVehicle(options??{})));}
        catch(error){return fail('VEHICLE_FAILED',messageOf(error));}
    }
    proposeAorAcquisition(kind,itemId,options={}) {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.proposeAcquisition(kind,itemId,options??{})));}
        catch(error){return fail('ACQUISITION_FAILED',messageOf(error));}
    }
    activateHeroicFortitude() {
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.activateHeroicFortitude()));}
        catch(error){return fail('COMBAT_FAILED',messageOf(error));}
    }
    advancePlayerTurn(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.advancePlayerTurn(options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    queueWingGuardTactic(options={}) {
        const r=this._controllerForExistingCampaign();
        if(!r.ok)return r;
        try{return ok(clone(r.data.queueWingGuardTactic(options??{})));}
        catch(error){return fail('WING_GUARD_TACTIC_FAILED',messageOf(error));}
    }
    finishCombat(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.finishCombat(options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    aim(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.aim(options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    resolveWeaponAttack(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.resolveWeaponAttack(options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    activatePendingPlayerCritical(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.activatePendingPlayerCritical(options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    activatePendingDisorientTalent() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.activatePendingDisorientTalent())); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    activatePendingKnockdownTalent() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.activatePendingKnockdownTalent())); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    activatePendingWeaponQuality(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.activatePendingWeaponQuality(options ?? {}))); }
        catch (error) { return fail('COMBAT_FAILED', messageOf(error)); }
    }
    vehicleRuntimeStatus() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try {
            const game = controllerResult.data;
            const runtime=game.state.vehicles??null,round=runtime?.combat?.active?runtime.combat.round:null;
            const targetId=runtime?.combat?.targetVehicleId,target=targetId?runtime.vehicles?.[targetId]:null;
            return ok(clone({activeVehicle:game.activeVehicle(),profiles:game.vehicleProfiles(),vehicles:runtime,publishedAdventure:game.state.world?.publishedAdventure??null,
                aorBrilliantEvasionAvailable:talents_mjs_1.sourceBackedTalentRank(game.state.player?.character,'BRILLIANT_EVASION')>0,
                activeStatus:game.activeVehicle()?vehicles_mjs_1.vehicleStatus(game.activeVehicle(),{currentRound:round}):null,
                targetStatus:target?vehicles_mjs_1.vehicleStatus(target,{currentRound:round}):null}));
        }
        catch (error) { return fail('VEHICLE_FAILED', messageOf(error)); }
    }
    selectVehicle(vehicleId) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.selectVehicle(String(vehicleId ?? '')))); }
        catch (error) { return fail('VEHICLE_FAILED', messageOf(error)); }
    }
    resolveSWE02ThwheekCombat(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.resolveSWE02ThwheekCombat(options??{})));}
        catch(e){return fail('SWE02_COMBAT_FAILED',messageOf(e));}
    }
    activateSWE02ThwheekLinked(){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.activateSWE02ThwheekLinked()));}
        catch(e){return fail('SWE02_COMBAT_FAILED',messageOf(e));}
    }
    resolveSWE02CrashPilot(){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.resolveSWE02CrashPilot()));}
        catch(e){return fail('SWE02_CRASH_FAILED',messageOf(e));}
    }
    resolveSWE02CrashResilience(){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.resolveSWE02CrashResilience()));}
        catch(e){return fail('SWE02_CRASH_FAILED',messageOf(e));}
    }
    swe02NabatMarket(){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.swe02NabatMarket()));}catch(e){return fail('SWE02_NABAT_FAILED',messageOf(e));}
    }
    purchaseSWE02NabatItem(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.purchaseSWE02NabatItem(options??{})));}catch(e){return fail('SWE02_NABAT_FAILED',messageOf(e));}
    }
    requestSWE02SpaceportWatch(){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.requestSWE02SpaceportWatch()));}catch(e){return fail('SWE02_NABAT_FAILED',messageOf(e));}
    }
    receiveSWE02SpaceportAlert(){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.receiveSWE02SpaceportAlert()));}catch(e){return fail('SWE02_NABAT_FAILED',messageOf(e));}
    }
    repairSWE02KraytFangAtNabat(){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.repairSWE02KraytFangAtNabat()));}catch(e){return fail('SWE02_NABAT_FAILED',messageOf(e));}
    }
    treatSWE02NabatWounds(){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.treatSWE02NabatWounds()));}catch(e){return fail('SWE02_NABAT_FAILED',messageOf(e));}
    }
    treatSWE02NynCritical(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.treatSWE02NynCritical(options??{})));}catch(e){return fail('SWE02_SOCIAL_FAILED',messageOf(e));}
    }
    negotiateSWE02Nyn(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.negotiateSWE02Nyn(options??{})));}catch(e){return fail('SWE02_SOCIAL_FAILED',messageOf(e));}
    }
    bribeSWE02NewMeenThug(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.bribeSWE02NewMeenThug(options??{})));}catch(e){return fail('SWE02_SOCIAL_FAILED',messageOf(e));}
    }
    prepareSWE02NewMeenEscort(){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.prepareSWE02NewMeenEscort()));}catch(e){return fail('SWE02_ESCORT_FAILED',messageOf(e));}}
    departSWE02NewMeenEscort(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.departSWE02NewMeenEscort(options??{})));}catch(e){return fail('SWE02_ESCORT_FAILED',messageOf(e));}}
    arriveSWE02NewMeenEscort(){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.arriveSWE02NewMeenEscort()));}catch(e){return fail('SWE02_ESCORT_FAILED',messageOf(e));}}
    abandonSWE02NewMeenEscort(){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.abandonSWE02NewMeenEscort()));}catch(e){return fail('SWE02_ESCORT_FAILED',messageOf(e));}}
    armSWE02NewMeenMilitia(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.armSWE02NewMeenMilitia(options??{})));}catch(e){return fail('SWE02_MILITIA_FAILED',messageOf(e));}}
    returnSWE02NewMeenMilitiaWeapon(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.returnSWE02NewMeenMilitiaWeapon(options??{})));}catch(e){return fail('SWE02_MILITIA_FAILED',messageOf(e));}}
    applySWE02NewMeenMilitiaRound(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.applySWE02NewMeenMilitiaRound(options??{})));}catch(e){return fail('SWE02_MILITIA_FAILED',messageOf(e));}}
    adjudicateSWE02NewMeenDeaths(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.adjudicateSWE02NewMeenDeaths(options??{})));}catch(e){return fail('SWE02_NEW_MEEN_FAILED',messageOf(e));}
    }
    coerceSWE02NewMeenThugs(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.coerceSWE02NewMeenThugs(options??{})));}catch(e){return fail('SWE02_NEW_MEEN_FAILED',messageOf(e));}
    }
    openSWE02DrombbSafe(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.openSWE02DrombbSafe(options??{})));}catch(e){return fail('SWE02_NEW_MEEN_FAILED',messageOf(e));}
    }
    askSWE02Piddock(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.askSWE02Piddock(options??{})));}catch(e){return fail('SWE02_ACT2_FAILED',messageOf(e));}}
    showSWE02PiddockTradeInterest(){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.showSWE02PiddockTradeInterest()));}catch(e){return fail('SWE02_ACT2_FAILED',messageOf(e));}}
    hireSWE02Vio(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.hireSWE02Vio(options??{})));}catch(e){return fail('SWE02_ACT2_FAILED',messageOf(e));}}
    swe02PiddockArmsCatalog(){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.swe02PiddockArmsCatalog()));}catch(e){return fail('SWE02_ACT2_FAILED',messageOf(e));}}
    agreeSWE02PiddockArms(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.agreeSWE02PiddockArms(options??{})));}catch(e){return fail('SWE02_ACT2_FAILED',messageOf(e));}}
    deliverSWE02PiddockArms(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.deliverSWE02PiddockArms(options??{})));}catch(e){return fail('SWE02_ACT2_FAILED',messageOf(e));}}
    bribeSWE02Vrixxtt(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.bribeSWE02Vrixxtt(options??{})));}catch(e){return fail('SWE02_ACT2_FAILED',messageOf(e));}}
    swe02PalaceOperationCatalog(){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.swe02PalaceOperationCatalog()));}catch(e){return fail('SWE02_PALACE_FAILED',messageOf(e));}}
    executeSWE02PalaceOperation(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.executeSWE02PalaceOperation(options??{})));}catch(e){return fail('SWE02_PALACE_FAILED',messageOf(e));}}
    swe02AnattaCatalog(){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.swe02AnattaCatalog()));}catch(e){return fail('SWE02_ACT2_FAILED',messageOf(e));}}
    buySWE02AnattaInformation(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.buySWE02AnattaInformation(options??{})));}catch(e){return fail('SWE02_ACT2_FAILED',messageOf(e));}}
    recruitSWE02Gladiators(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.recruitSWE02Gladiators(options??{})));}catch(e){return fail('SWE02_PALACE_FAILED',messageOf(e));}}
    queueSWE02ThroneAlarm(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.queueSWE02ThroneAlarm(options??{})));}catch(e){return fail('SWE02_PALACE_FAILED',messageOf(e));}}
    prepareSWE02AnattaDossier(){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.prepareSWE02AnattaDossier()));}catch(e){return fail('SWE02_ACT3_FAILED',messageOf(e));}}
    resolveSWE02TeemoWithoutCombat(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.resolveSWE02TeemoWithoutCombat(options??{})));}catch(e){return fail('SWE02_ACT3_FAILED',messageOf(e));}}
    claimSWE02TeemoStrongbox(){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.claimSWE02TeemoStrongbox()));}catch(e){return fail('SWE02_ACT3_FAILED',messageOf(e));}}
    shootSWE02TeemoChandelier(options={}){const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.shootSWE02TeemoChandelier(options??{})));}catch(e){return fail('SWE02_ACT3_FAILED',messageOf(e));}}
    beginVehicleCombat(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.beginVehicleCombat(options ?? {}))); }
        catch (error) { return fail('VEHICLE_FAILED', messageOf(error)); }
    }
    nextVehicleRound() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.nextVehicleRound())); }
        catch (error) { return fail('VEHICLE_FAILED', messageOf(error)); }
    }
    endVehicleCombat() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.endVehicleCombat())); }
        catch (error) { return fail('VEHICLE_FAILED', messageOf(error)); }
    }
    useVehiclePilotManeuver(kind, options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.useVehiclePilotManeuver(String(kind ?? ''), options ?? {}))); }
        catch (error) { return fail('VEHICLE_FAILED', messageOf(error)); }
    }
    useFullThrottle(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.useFullThrottle(options ?? {}))); }
        catch (error) { return fail('VEHICLE_FAILED', messageOf(error)); }
    }
    gainVehicleAdvantage(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.gainVehicleAdvantage(options ?? {}))); }
        catch (error) { return fail('VEHICLE_FAILED', messageOf(error)); }
    }
    useVehicleCrewAction(kind, options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.useVehicleCrewAction(String(kind ?? ''), options ?? {}))); }
        catch (error) { return fail('VEHICLE_FAILED', messageOf(error)); }
    }
    useControlledFire() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.useControlledFire())); }
        catch (error) { return fail('VEHICLE_FAILED', messageOf(error)); }
    }
    useAoRBrilliantEvasion(options={}){
        const r=this._controllerForExistingCampaign();if(!r.ok)return r;
        try{return ok(clone(r.data.useAoRBrilliantEvasion(options??{})));}
        catch(e){return fail('AOR_BRILLIANT_EVASION_FAILED',messageOf(e));}
    }
    resolveIncomingVehicleAttack(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.resolveIncomingVehicleAttack(options ?? {}))); }
        catch (error) { return fail('VEHICLE_INCOMING_FAILED', messageOf(error)); }
    }
    activateIncomingVehicleCritical(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.activateIncomingVehicleCritical(options ?? {}))); }
        catch (error) { return fail('VEHICLE_INCOMING_CRITICAL_FAILED', messageOf(error)); }
    }
    declineIncomingVehicleCritical() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.declineIncomingVehicleCritical())); }
        catch (error) { return fail('VEHICLE_INCOMING_CRITICAL_FAILED', messageOf(error)); }
    }
    resolveVehicleAttack(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.resolveVehicleAttack(options ?? {}))); }
        catch (error) { return fail('VEHICLE_FAILED', messageOf(error)); }
    }
    activatePendingVehicleCritical(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.activatePendingVehicleCritical(options ?? {}))); }
        catch (error) { return fail('VEHICLE_FAILED', messageOf(error)); }
    }
    activatePendingVehicleQuality(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.activatePendingVehicleQuality(options ?? {}))); }
        catch (error) { return fail('VEHICLE_FAILED', messageOf(error)); }
    }
    reactivateGuidedProjectile(projectileId) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.reactivateGuidedProjectile(String(projectileId ?? '')))); }
        catch (error) { return fail('VEHICLE_FAILED', messageOf(error)); }
    }
    attemptTractorEscape(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.attemptTractorEscape(options ?? {}))); }
        catch (error) { return fail('VEHICLE_FAILED', messageOf(error)); }
    }
    vehicleDamageControl(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.vehicleDamageControl(options ?? {}))); }
        catch (error) { return fail('VEHICLE_FAILED', messageOf(error)); }
    }
    attemptRepairActiveVehicleCritical(index, options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.attemptRepairActiveVehicleCritical(Number(index), options ?? {}))); }
        catch (error) { return fail('VEHICLE_FAILED', messageOf(error)); }
    }
    plotHyperspaceCourse(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.plotHyperspaceCourse(options ?? {}))); }
        catch (error) { return fail('HYPERSPACE_PLOT_FAILED', messageOf(error)); }
    }
    astrogationPlan(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.astrogationPlan(options ?? {}))); }
        catch (error) { return fail('ASTROGATION_FAILED', messageOf(error)); }
    }
    resolveHyperspaceJump(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.resolveHyperspaceJump(options ?? {}))); }
        catch (error) { return fail('ASTROGATION_FAILED', messageOf(error)); }
    }
    groupSummary() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.groupSummary())); }
        catch (error) { return fail('GROUP_FAILED', messageOf(error)); }
    }
    resolveCampaignMilestone() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.resolveContributionMilestone())); }
        catch (error) { return fail('CAMPAIGN_PROGRESSION_FAILED', messageOf(error)); }
    }
    claimAllianceReward(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.claimAllianceReward(options ?? {}))); }
        catch (error) { return fail('CAMPAIGN_PROGRESSION_FAILED', messageOf(error)); }
    }
    allianceRewardOptions() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.allianceRewardOptions())); }
        catch (error) { return fail('CAMPAIGN_PROGRESSION_FAILED', messageOf(error)); }
    }
    startSession(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.startSession(options ?? {}))); }
        catch (error) { return fail('SESSION_RULES_FAILED', messageOf(error)); }
    }
    closeSession(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.closeSession(options ?? {}))); }
        catch (error) { return fail('SESSION_RULES_FAILED', messageOf(error)); }
    }
    encumbranceStatus() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.encumbranceStatus())); }
        catch (error) { return fail('ENCUMBRANCE_FAILED', messageOf(error)); }
    }
    liftingDifficulty(objectEncumbrance, options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.liftingDifficulty(Number(objectEncumbrance), options ?? {}))); }
        catch (error) { return fail('ENCUMBRANCE_FAILED', messageOf(error)); }
    }
    statusSummary() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.statusSummary())); }
        catch (error) { return fail('STATUS_FAILED', messageOf(error)); }
    }
    applyStatus(payload = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.applyStatus(payload ?? {}))); }
        catch (error) { return fail('STATUS_FAILED', messageOf(error)); }
    }
    destinySummary() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.destinySummary())); }
        catch (error) { return fail('DESTINY_FAILED', messageOf(error)); }
    }
    spendDestiny(side) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.spendDestiny(String(side ?? '')))); }
        catch (error) { return fail('DESTINY_FAILED', messageOf(error)); }
    }
    resolveFearCheck(options = {}) {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok) return controllerResult;
        try { return ok(clone(controllerResult.data.resolveFearCheck(options ?? {}))); }
        catch (error) { return fail('FEAR_FAILED', messageOf(error)); }
    }
    equipmentCatalog() {
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok)
            return controllerResult;
        try {
            const game = controllerResult.data;
            return ok(clone(game.equipmentCatalog()));
        }
        catch (error) {
            return fail('EQUIPMENT_CATALOG_FAILED', messageOf(error));
        }
    }
    proposeWeaponAcquisition(itemId) {
        const id = String(itemId ?? '').trim();
        if (!id)
            return fail('INVALID_INPUT', 'Selecione uma arma do catálogo auditado.');
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok)
            return controllerResult;
        try {
            const game = controllerResult.data;
            const result = game.proposeWeaponAcquisition(id);
            return ok({
                result: clone(result),
                pendingAcquisition: clone(game.state.master?.pendingAcquisition ?? null),
                state: clone(game.state),
            });
        }
        catch (error) {
            return fail('EQUIPMENT_ACQUISITION_FAILED', messageOf(error));
        }
    }
    restoreSnapshot(id) {
        let before = null;
        try {
            before = this.repository.load();
        }
        catch (error) {
            return fail('STATE_READ_FAILED', messageOf(error));
        }
        if (!before)
            return fail('NO_CAMPAIGN', 'Nenhuma campanha carregada.');
        try {
            const restored = this.snapshots.restore(String(id ?? ''));
            this.repository.save(restored);
            return ok({ state: clone(restored) });
        }
        catch (error) {
            return fail('SNAPSHOT_RESTORE_FAILED', messageOf(error));
        }
    }
    imageLibraryInfo() {
        try {
            const validation = imageLibrary_mjs_1.validateImageManifest(imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST);
            if (!validation.ok)
                return fail('IMAGE_LIBRARY_INVALID', validation.errors.join('; '));
            return ok({
                libraryId: imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST.libraryId,
                schemaVersion: imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST.schemaVersion,
                counts: clone(imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST.counts),
                policy: clone(imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST.policy),
            });
        }
        catch (error) {
            return fail('IMAGE_LIBRARY_FAILED', messageOf(error));
        }
    }
    searchImageLibrary(criteria = {}) {
        try {
            const validation = imageLibrary_mjs_1.validateImageManifest(imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST);
            if (!validation.ok)
                return fail('IMAGE_LIBRARY_INVALID', validation.errors.join('; '));
            return ok(clone(imageLibrary_mjs_1.searchImageAssets(imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST, criteria)));
        }
        catch (error) {
            return fail('IMAGE_LIBRARY_SEARCH_FAILED', messageOf(error));
        }
    }
    imageAsset(assetId) {
        const id = String(assetId ?? '').trim();
        if (!id)
            return fail('INVALID_INPUT', 'Informe o assetId da imagem.');
        try {
            const asset = imageLibrary_mjs_1.getImageAsset(imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST, id);
            if (!asset)
                return fail('IMAGE_ASSET_NOT_FOUND', 'Imagem não encontrada na biblioteca.');
            return ok(clone(asset));
        }
        catch (error) {
            return fail('IMAGE_LIBRARY_FAILED', messageOf(error));
        }
    }
    imageMapCatalog(criteria = {}) {
        try {
            const validation = imageLibrary_mjs_1.validateImageManifest(imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST);
            if (!validation.ok)
                return fail('IMAGE_LIBRARY_INVALID', validation.errors.join('; '));
            const base = imageLibrary_mjs_1.searchImageAssets(imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST, { ...criteria, category: 'map', limit: criteria?.limit ?? 500 });
            if (!criteria?.includePlayableLocations) return ok(clone(base));
            const query = String(criteria?.query ?? '').trim().toLocaleLowerCase('pt-BR');
            const extra = R54_LOCATION_MAP_ASSETS.filter(asset => !query || [asset.assetId, asset.title, asset.mapKind, ...(asset.tags ?? [])].join(' ').toLocaleLowerCase('pt-BR').includes(query));
            const merged = [...extra, ...base].filter((asset, index, all) => all.findIndex(other => other.assetId === asset.assetId) === index).slice(0, Math.max(0, Math.min(Number(criteria?.limit) || 500, 500)));
            return ok(clone(merged));
        }
        catch (error) {
            return fail('IMAGE_LIBRARY_SEARCH_FAILED', messageOf(error));
        }
    }
    imageVisualEntityCatalog(criteria = {}) {
        try {
            const validation = imageLibrary_mjs_1.validateImageManifest(imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST);
            if (!validation.ok)
                return fail('IMAGE_LIBRARY_INVALID', validation.errors.join('; '));
            const kind = String(criteria?.kind ?? 'all').trim().toLowerCase();
            const limit = Math.max(0, Math.min(Number(criteria?.limit) || 500, 500));
            const common = { ...criteria, limit: 500 };
            delete common.kind;
            const groups = [];
            if (kind === 'all' || kind === 'npc')
                groups.push(...imageLibrary_mjs_1.searchImageAssets(imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST, { ...common, category: 'portrait', subtype: 'npc', limit: 500 }));
            if (kind === 'all' || kind === 'droid')
                groups.push(...imageLibrary_mjs_1.searchImageAssets(imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST, { ...common, category: 'droid', limit: 500 }));
            if (kind === 'all' || kind === 'beast' || kind === 'creature')
                groups.push(...imageLibrary_mjs_1.searchImageAssets(imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST, { ...common, category: 'creature', subtype: 'beast', limit: 500 }));
            if (kind === 'all' || kind === 'vehicle' || kind === 'vehicles' || kind === 'ship' || kind === 'starship' || kind === 'starships')
                groups.push(...imageLibrary_mjs_1.searchImageAssets(imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST, { ...common, category: 'vehicle', limit: 500 }));
            if (!['all', 'npc', 'droid', 'beast', 'creature', 'vehicle', 'vehicles', 'ship', 'starship', 'starships'].includes(kind))
                return fail('INVALID_INPUT', 'Tipo visual inválido.');
            groups.sort((a, b) => a.title.localeCompare(b.title, 'pt-BR') || a.assetId.localeCompare(b.assetId));
            return ok(clone(groups.slice(0, limit)));
        }
        catch (error) {
            return fail('IMAGE_LIBRARY_SEARCH_FAILED', messageOf(error));
        }
    }
    imageAssetRuntime(assetId) {
        const id = String(assetId ?? '').trim();
        if (!id)
            return fail('INVALID_INPUT', 'Informe o assetId da imagem.');
        try {
            const asset = imageLibrary_mjs_1.getImageAsset(imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST, id);
            if (!asset)
                return fail('IMAGE_ASSET_NOT_FOUND', 'Imagem não encontrada na biblioteca.');
            return ok(clone(imageLibrary_mjs_1.imageAssetRuntimeDescriptor(imageLibraryData_mjs_1.IMAGE_LIBRARY_MANIFEST, asset)));
        }
        catch (error) {
            return fail('IMAGE_LIBRARY_FAILED', messageOf(error));
        }
    }
    async submitPlayerIntent(text, { fetchImpl = globalThis.fetch, timeoutMs = 60000, signal = null } = {}) {
        const intentText = String(text ?? '').trim();
        if (!intentText)
            return fail('INVALID_INPUT', 'Digite uma ação.');
        const controllerResult = this._controllerForExistingCampaign();
        if (!controllerResult.ok)
            return controllerResult;
        const game = controllerResult.data;
        try {
            // Provider HTTP explícito continua sendo autoridade quando configurado.
            if (game.state.settings?.masterProvider === 'http') {
                const result = await game.requestMasterTurn({
                    intentText,
                    endpoint: game.state.settings.masterEndpoint,
                    fetchImpl,
                    timeoutMs,
                    signal,
                });
                return ok({ mode: 'http', result: clone(result), state: clone(game.state) });
            }
            // No Android final, Gemini é o narrador padrão quando a chave está configurada.
            // A mutação continua passando exclusivamente por requestMasterTurn/validateMasterProposal.
            const native = nativeGeminiStatus(globalThis);
            if (native.available && native.configured) {
                if (game.state.player?.character && game.state.session?.status !== 'ativa')
                    game.startSession();
                const nativeFetch = async (_url, init = {}) => {
                    let providerPayload;
                    try {
                        providerPayload = JSON.parse(String(init.body ?? '{}'));
                    }
                    catch {
                        throw new Error('Contexto serializado do Motor Barbara inválido.');
                    }
                    const remote = await requestNativeGeminiMaster(providerPayload, { timeoutMs, target: globalThis });
                    const allowedRuleIds = new Set((providerPayload?.context?.rules ?? []).map(rule => String(rule?.id ?? '')).filter(Boolean));
                    const proposal = clone(remote.proposal ?? {});
                    if (Array.isArray(proposal.sourceRuleIds)) {
                        // Fail closed: never erase fabricated references to make an unsafe
                        // Gemini proposal appear valid to the deterministic validator.
                        const unknown = proposal.sourceRuleIds.map(String).filter(id => !allowedRuleIds.has(id));
                        if (unknown.length) throw new Error('Gemini citou regra fora das fontes recuperadas; proposta rejeitada.');
                    }
                    return { ok: true, status: 200, json: async () => ({ proposal, meta: { provider: 'gemini-native', model: remote.model ?? native.model ?? null } }) };
                };
                try {
                    const result = await game.requestMasterTurn({
                        intentText,
                        endpoint: 'https://braseiro.native/master',
                        fetchImpl: nativeFetch,
                        timeoutMs,
                        signal,
                    });
                    return ok({ mode: 'gemini-native', result: clone(result), state: clone(game.state) });
                }
                catch (providerError) {
                    // Network/LLM/validation failure: requestMasterTurn rolls back the
                    // remote proposal. Record ONLY the user's original intent locally.
                    // No claimed narration, new NPCs, credits, dice or XP are fabricated.
                    const result = game.submitIntent(intentText);
                    return ok({
                        mode: 'local-fallback',
                        result: clone(result),
                        state: clone(game.state),
                        masterNotice: 'Gemini indisponível ou proposta rejeitada; intenção preservada, nenhuma consequência mecânica remota aplicada.',
                        providerError: messageOf(providerError),
                    });
                }
            }
            // Fallback local deliberadamente não inventa prosa: registra a intenção, consulta o
            // contexto/regras canônicas e mantém a campanha utilizável mesmo sem rede/chave.
            const result = game.submitIntent(intentText);
            return ok({
                mode: 'local',
                result: clone(result),
                state: clone(game.state),
                masterNotice: native.available && !native.configured
                    ? 'Configure a chave Gemini em AJUSTES → Gemini / provedor para habilitar a narração automática.'
                    : 'Narrador remoto indisponível; o Motor Barbara local preservou estado e contexto de regras.',
            });
        }
        catch (error) {
            return fail('ENGINE_REJECTED', messageOf(error));
        }
    }
}
export function createBraseiroUiBridge(options = {}) {
    return new BraseiroUiBridge(options);
}
export function installBraseiroUiBridge(target = globalThis, options = {}) {
    if (!target || (typeof target !== 'object' && typeof target !== 'function'))
        throw new Error('Alvo global inválido para a ponte de UI.');
    if (target.BraseiroGameBridge)
        return target.BraseiroGameBridge;
    const bridge = createBraseiroUiBridge(options);
    Object.defineProperty(target, 'BraseiroGameBridge', {
        value: bridge,
        configurable: false,
        enumerable: false,
        writable: false,
    });
    return bridge;
}
