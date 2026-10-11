import { withDbRetry, withTransaction } from '../db/db';
import { CampaignState, GamePeriod, Scene, WorldState } from '../types';
import { WorldStateService } from './WorldStateService';
import { EnvironmentalDirectorService } from './EnvironmentalDirectorService';
import { LivingWorldAutomationService } from './LivingWorldAutomationService';
import { CharacterTimeRulesEngine } from './CharacterTimeRulesEngine';
import { BitterReachRuntimeService } from './BitterReachRuntimeService';
import { BitterReachModule } from './BitterReachModule';
import { CampaignPartyService } from './CampaignPartyService';
import type { Character } from '../types';

export const PERIODS: GamePeriod[] = ['Morning', 'Day', 'Evening', 'Night'];
export const PERIOD_LABELS: Record<GamePeriod, string> = { Morning:'Manhã', Day:'Tarde', Evening:'Noite', Night:'Madrugada' };
const ACTIVE_CAMPAIGN_META_ID='activeCampaignId';

export class CampaignService {
  static normalize(campaign:CampaignState):CampaignState{
    const dayFromText=Number(campaign.currentDate?.match(/\d+/)?.[0]||1);
    const currentDay=Math.max(1,campaign.currentDay||dayFromText||1);
    const currentPeriod=campaign.currentPeriod||'Morning';
    return{...campaign,currentDay,currentPeriod,currentDate:`Dia ${currentDay}`};
  }

  static create(profile:CampaignState['profile']='Aventureiro'):CampaignState{
    const now=Date.now();
    return{id:crypto.randomUUID(),name:'Nova Campanha',profile,currentDate:'Dia 1',currentDay:1,currentPeriod:'Morning',currentHexId:null,activeCharacterId:null,partyCharacterIds:[],discoveredHexIds:[],notes:'',createdAt:now,updatedAt:now};
  }

  static async getActive(createIfMissing=false):Promise<CampaignState|null>{
    let metaValue: string | undefined;
    const snapshot=await withDbRetry(async db=>{
      const meta=await db.get('app_meta',ACTIVE_CAMPAIGN_META_ID);
      metaValue = meta?.value;
      let campaign=meta?.value?await db.get('campaign',meta.value):undefined;
      if(!campaign){
        const campaigns=await db.getAll('campaign');
        campaigns.sort((a,b)=>b.updatedAt-a.updatedAt);
        campaign=campaigns[0];
      }
      return campaign||null;
    });

    // Só cria uma campanha se a leitura do banco terminou com sucesso e
    // confirmou que não há nenhuma. Erro de banco nunca vira estado vazio.
    if(!snapshot){
      if(!createIfMissing)return null;
      return this.create();
    }

    const normalized=this.normalize(snapshot);
    if(metaValue !== normalized.id){
      await withTransaction(['campaign','app_meta'],'readwrite',async tx=>{
        await tx.objectStore('campaign').put(normalized);
        await tx.objectStore('app_meta').put({id:ACTIVE_CAMPAIGN_META_ID,value:normalized.id,updatedAt:Date.now()});
      });
    }
    return normalized;
  }

  static async save(campaign:CampaignState):Promise<CampaignState>{
    if(!campaign?.id)throw new Error('Estado de campanha inválido: id ausente.');
    const normalized=await withTransaction(['campaign','app_meta'],'readwrite',async tx=>{
      const existing=await tx.objectStore('campaign').get(campaign.id);
      // Campos ausentes em um estado parcial nunca apagam a campanha já salva.
      const merged=this.normalize({
        ...(existing||{}),
        ...campaign,
        name:String(campaign.name||existing?.name||'Nova Campanha'),
        profile:campaign.profile||existing?.profile||'Aventureiro',
        discoveredHexIds:Array.from(new Set([...(existing?.discoveredHexIds||[]),...(campaign.discoveredHexIds||[])])),
        notes:campaign.notes??existing?.notes??'',
        createdAt:existing?.createdAt||campaign.createdAt||Date.now(),
        updatedAt:Date.now(),
      } as CampaignState);
      await tx.objectStore('campaign').put(merged);
      await tx.objectStore('app_meta').put({id:ACTIVE_CAMPAIGN_META_ID,value:merged.id,updatedAt:Date.now()});
      return merged;
    });
    this.notify(normalized);
    return normalized;
  }

  /** R52: salvar uma ficha e sua associação no mesmo commit IndexedDB. */
  static async savePartyCharacter(character: Character, campaignId?: string): Promise<Character> {
    const savedResult = await withTransaction(['campaign', 'app_meta', 'characters'], 'readwrite', async tx => {
      const campaigns = tx.objectStore('campaign');
      const metadata = tx.objectStore('app_meta');
      const activeId = campaignId || String((await metadata.get(ACTIVE_CAMPAIGN_META_ID))?.value || '');
      let campaign = activeId ? await campaigns.get(activeId) as CampaignState | undefined : undefined;
      if (!campaign && campaignId) throw new Error('Campanha da ficha não encontrada.');
      if (!campaign && !campaignId) {
        const existing = await campaigns.getAll() as CampaignState[];
        existing.sort((a, b) => b.updatedAt - a.updatedAt);
        campaign = existing[0];
      }
      // A primeira ficha é a primeira ação significativa em uma instalação nova.
      // Bootstrap its campaign inside this same transaction so the UI has no
      // campaign/character circular dependency and failures roll everything back.
      if (!campaign && !campaignId) {
        campaign = this.create();
        await campaigns.put(campaign);
      }
      if (!campaign) throw new Error('Selecione uma campanha antes de salvar a ficha.');
      campaign = this.normalize(campaign);
      await metadata.put({ id: ACTIVE_CAMPAIGN_META_ID, value: campaign.id, updatedAt: Date.now() });
      const store = tx.objectStore('characters');
      const previous = await store.get(character.id) as Character | undefined;
      if (previous?.campaignId && previous.campaignId !== campaign.id)
        throw new Error('Esta ficha pertence a outra campanha. Importe uma cópia com outro ID.');
      if (character.campaignId && character.campaignId !== campaign.id)
        throw new Error('Não é permitido mover silenciosamente uma ficha entre campanhas.');
      const saved: Character = { ...character, campaignId: campaign.id };
      await store.put(saved);
      let updatedCampaign = campaign;
      if (Array.isArray(campaign.partyCharacterIds) && !campaign.partyCharacterIds.includes(saved.id)) {
        updatedCampaign = { ...campaign, partyCharacterIds: [...campaign.partyCharacterIds, saved.id], updatedAt: Date.now() };
        await campaigns.put(updatedCampaign);
      }
      return { saved, campaign: updatedCampaign };
    });
    this.notify(savedResult.campaign);
    return savedResult.saved;
  }

  /** R52: importa o lote inteiro sem deixar meio grupo salvo em caso de falha. */
  static async importPartyCharacters(characters: Character[], campaignId?: string): Promise<Character[]> {
    const active = campaignId ? { id: campaignId } : await this.getActive(false);
    if (!active?.id) throw new Error('Selecione uma campanha antes de importar personagens.');
    return withTransaction(['campaign', 'characters'], 'readwrite', async tx => {
      const campaigns = tx.objectStore('campaign');
      const campaign = await campaigns.get(active.id) as CampaignState | undefined;
      if (!campaign) throw new Error('Campanha não encontrada para importação.');
      const store = tx.objectStore('characters');
      const existing = await store.getAll() as Character[];
      const reserved = new Set(existing.map(item => item.id));
      const imported: Character[] = [];
      for (const character of characters) {
        let id = character.id;
        if (!id || reserved.has(id)) id = crypto.randomUUID();
        reserved.add(id);
        const saved: Character = { ...character, id, campaignId: campaign.id };
        await store.put(saved);
        imported.push(saved);
      }
      if (Array.isArray(campaign.partyCharacterIds)) {
        await campaigns.put({ ...campaign, partyCharacterIds: [...campaign.partyCharacterIds, ...imported.map(item => item.id)], updatedAt: Date.now() });
      }
      return imported;
    });
  }

  /** Corrige apenas a composição, sem apagar o histórico nem as fichas legadas. */
  static async removePartyCharacter(characterId: string, campaignId?: string): Promise<void> {
    const active = campaignId ? { id: campaignId } : await this.getActive(false);
    if (!active?.id) throw new Error('Campanha não selecionada.');
    await withTransaction(['campaign','characters'], 'readwrite', async tx => {
      const campaigns = tx.objectStore('campaign');
      const campaign = await campaigns.get(active.id) as CampaignState | undefined;
      if (!campaign) throw new Error('Campanha não encontrada.');
      const store = tx.objectStore('characters');
      const character = await store.get(characterId) as Character | undefined;
      CampaignPartyService.assertMember(campaign, character);
      await store.delete(characterId);
      if (Array.isArray(campaign.partyCharacterIds))
        await campaigns.put({ ...campaign, partyCharacterIds: campaign.partyCharacterIds.filter(id => id !== characterId), updatedAt: Date.now() });
    });
  }

  static async getScenes(campaignId?:string):Promise<Scene[]>{
    const campaign=campaignId?await withDbRetry(db=>db.get('campaign',campaignId)):await this.getActive(false);
    if(!campaign)return[];
    const scenes=await withDbRetry(db=>db.getAll('scenes'));
    return scenes.filter(scene=>!scene.campaignId||scene.campaignId===campaign.id).sort((a,b)=>(a.sequence??a.createdAt)-(b.sequence??b.createdAt));
  }

  static advanceState(campaign: CampaignState, steps = 1): CampaignState {
    // A corrupted/restored period count (Infinity/NaN) must never hang the
    // main UI. Arithmetic is constant-time even for long, legitimate skips.
    const numberOfSteps=Number(steps);
    if(!Number.isFinite(numberOfSteps)||numberOfSteps>Number.MAX_SAFE_INTEGER)
      throw new Error('Período inválido: quantidade não finita ou fora do intervalo seguro.');
    const safeSteps=Math.max(0,Math.ceil(numberOfSteps));
    const day=Number(campaign.currentDay||1);
    const periodIndex=PERIODS.indexOf(campaign.currentPeriod||'Morning');
    if(!Number.isSafeInteger(day)||day<1||periodIndex<0||
      !Number.isSafeInteger(day+Math.floor((periodIndex+safeSteps)/PERIODS.length)))
      throw new Error('Calendário da campanha inválido; preserve o save e corrija o período.');
    const nextDay=day+Math.floor((periodIndex+safeSteps)/PERIODS.length);
    const nextPeriod=PERIODS[(periodIndex+safeSteps)%PERIODS.length];
    return this.normalize({...campaign,currentDay:nextDay,currentPeriod:nextPeriod,currentDate:`Dia ${nextDay}`});
  }

  static async advancePeriods(steps=1):Promise<CampaignState|null>{
    const quantity=Number(steps);
    if(!Number.isFinite(quantity)||quantity>20000)
      throw new Error('Avanço de tempo inválido: limite de 20.000 Quartos de Dia por operação.');
    const safeSteps=Math.max(0,Math.trunc(quantity));
    const snapshot=await this.getActive(false);if(!snapshot)return null;
    if(!safeSteps)return snapshot;
    const result=await withTransaction(['campaign','app_meta','world_state','characters'],'readwrite',async tx=>{
      const campaignStore=tx.objectStore('campaign');
      const current=this.normalize(await campaignStore.get(snapshot.id)||snapshot);
      const worldStore=tx.objectStore('world_state');
      const world=WorldStateService.normalize(await worldStore.get(current.id),current.id);
      if(world.encounter?.status==='active')throw new Error('Não avance o tempo enquanto houver um encontro ativo.');
      const partyForSleep=CampaignPartyService.members(current, await tx.objectStore('characters').getAll());
      if(partyForSleep.some(member=>member.survivalState?.mustSleep))throw new Error('Uma personagem desabada precisa dormir por um Quarto de Dia antes de o relógio avançar.');
      const now=Date.now();
      const next={...this.advanceState(current,safeSteps),updatedAt:now};
      const advancedWorld=WorldStateService.advanceTime(world,current,next,safeSteps,now);
      const nextWorld={...advancedWorld,journey:{...advancedWorld.journey,quarterTravelProgress:0,updatedAt:now},updatedAt:now};
      const characters=tx.objectStore('characters');
      const party=CampaignPartyService.members(current, await characters.getAll());
      for(const member of party){
        await characters.put(CharacterTimeRulesEngine.advanceQuarters(member, safeSteps));
      }
      await campaignStore.put(next);
      await tx.objectStore('app_meta').put({id:ACTIVE_CAMPAIGN_META_ID,value:next.id,updatedAt:now});
      await worldStore.put(nextWorld);
      return{campaign:next,world:nextWorld,previous:current};
    });
    this.notify(result.campaign);
    const reconciledWorld=await this.reconcileElapsedTime(result.previous,result.campaign,`${safeSteps} período(s) transcorrido(s)`,result.world);
    if(typeof window!=='undefined')window.dispatchEvent(new CustomEvent('world-state-updated',{detail:reconciledWorld}));
    return result.campaign;
  }

  static periodIndex(campaign: Pick<CampaignState,'currentDay'|'currentPeriod'>) {
    return (Math.max(1,campaign.currentDay||1)-1)*4+Math.max(0,PERIODS.indexOf(campaign.currentPeriod||'Morning'));
  }

  /**
   * Finalizador único para qualquer ação que consumiu tempo fora de advancePeriods
   * (jornada, crafting, reparo, viagem etc.). Não altera o calendário: apenas
   * reconcilia ambiente + Mundo Vivo com os ticks que JÁ foram persistidos.
   */
  static async reconcileElapsedTime(previous:CampaignState,current:CampaignState,reason:string,worldHint?:WorldState|null):Promise<WorldState>{
    const from=this.periodIndex(previous),to=this.periodIndex(current);
    let world=worldHint||await WorldStateService.get(current.id);
    if(to<=from){
      await LivingWorldAutomationService.run(current.id,reason,{fromPeriodIndex:from}).catch(error=>console.warn('Mundo Vivo preservado após falha de automação:',error));
      return world;
    }
    if(current.currentHexId){
      const [currentHex,allHexes]=await Promise.all([
        withDbRetry(db=>db.get('individual_hexes',current.currentHexId!)),
        withDbRetry(db=>db.getAll('individual_hexes')),
      ]);
      if(currentHex){
        const envDay=world.environment.lastEvaluatedDay;
        const envPeriod=world.environment.lastEvaluatedPeriod;
        let envIndex=envDay!==undefined&&envPeriod?((Math.max(1,envDay)-1)*4+Math.max(0,PERIODS.indexOf(envPeriod))):-1;
        for(let tick=from+1;tick<=to;tick+=1){
          if(envIndex>=tick)continue;
          const day=Math.floor(tick/4)+1,period=PERIODS[tick%4];
          const at={...current,currentDay:day,currentPeriod:period,currentDate:`Dia ${day}`};
          const analysis=EnvironmentalDirectorService.evaluate({campaign:at,currentHex,worldState:world,allHexes,now:Date.now()});
          world={...world,environment:analysis.environment,updatedAt:Date.now()};
          await withDbRetry(db=>db.put('world_state',world).then(()=>undefined));
          if(BitterReachModule.isBitterReachHex(currentHex)){
            world=await BitterReachRuntimeService.reconcileQuarter(at,currentHex);
          }
          envIndex=tick;
        }
        await withDbRetry(db=>db.put('world_state',world).then(()=>undefined));
        if(typeof window!=='undefined')window.dispatchEvent(new CustomEvent('environment-updated',{detail:world.environment}));
      }
    }
    const living=await LivingWorldAutomationService.run(current.id,reason,{fromPeriodIndex:from}).catch(error=>{
      console.warn('Mundo Vivo preservado após falha de automação:',error);
      return null;
    });
    return living?.world||world;
  }

  static status(campaign:CampaignState|null){
    if(!campaign)return{day:'Sem aventura',period:'—',hex:'—'};
    return{day:`Dia ${campaign.currentDay||1}`,period:PERIOD_LABELS[campaign.currentPeriod||'Morning'],hex:campaign.currentHexId||'—'};
  }

  static async clearActiveReference(){await withDbRetry(db=>db.delete('app_meta',ACTIVE_CAMPAIGN_META_ID).then(()=>undefined));this.notify(null);}
  static notify(campaign:CampaignState|null){if(typeof window!=='undefined')window.dispatchEvent(new CustomEvent('campaign-updated',{detail:campaign}));}
}
