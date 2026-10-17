// Automated verification suite for ETAPA 03 Gates:
// - G03 (Regras Canonicas 2e - Livro a Livro)
// - G04 (Aventuras Canonicas - 6 Contos das Terras Solitarias + A Estrela da Nevoa)
// - G05 (RAG 182 Entradas Ancoradas + Firewall PLAYER/GM)
// Usage: node --test ci/oua-4.9.212/canonical-books-audit.test.mjs

import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import crypto from 'node:crypto';
import { pathToFileURL } from 'node:url';

const v9Root = process.env.OUA_V9_PATCHED_DIR || 'D:/OUA_49212_WORKSPACE/oua-private/V9_PATCHED';
const androidWorkRoot = process.env.OUA_ANDROID_WORK_DIR || 'D:/OUA_49212_WORKSPACE/oua-private/ANDROID_49212_WORK';

const importV9 = async (relPath) => import(pathToFileURL(path.join(v9Root, relPath)).href);

await importV9('host-authority/Tor2eHostSecretAuthority.mjs');

const {
  CORE_CULTURES_2E,
  MARCO_A_ESTRELA_DA_NEVOA,
  Tor2eContentCatalog
} = await importV9('src/library/Tor2eContentCatalog.mjs');
const { VALFENDA_EXPANSION } = await importV9('src/domain/rpg/expansions/ValfendaExpansion.mjs');
const { POVOS_TERRAS_ERMAS_EXPANSION } = await importV9('src/domain/rpg/expansions/PovosTerrasErmasExpansion.mjs');
const {
  ANTECEDENTES_CULTURAIS_36,
  TABELA_EVENTOS_IMPORTANTES,
  TrilhasPersonagensModule
} = await importV9('src/domain/rpg/expansions/TrilhasPersonagensModule.mjs');
const {
  STRIDER_CREATION_RULES,
  TELLING_TABLE_THRESHOLDS,
  FORTUNE_TABLE,
  ILL_FORTUNE_TABLE,
  EYE_HUNT_THRESHOLDS,
  REVELATION_EPISODES_TABLE,
  SKIRMISH_STANCE_RULES,
  EXPERIENCE_MILESTONES_SOLO,
  PATRON_QUESTS_SOLO,
  ModoPeregrinoModule
} = await importV9('src/domain/rpg/expansions/ModoPeregrinoModule.mjs');
const { CONTOS_TERRAS_SOLITARIAS } = await importV9('src/domain/rpg/expansions/ContosTerrasSolitariasAventuras.mjs');
const { Tor2eAdventureController } = await importV9('src/ui/bridges/Tor2eAdventureController.mjs');
const { Tor2eRAGService, RAG_CORPUS } = await importV9('src/library/Tor2eRAGService.mjs');
const { Tor2eDiceEngine } = await importV9('src/domain/rpg/rules/Tor2eDiceEngine.mjs');
const { Tor2eCombatEngine, COMBAT_STANCE } = await importV9('src/domain/rpg/rules/Tor2eCombatEngine.mjs');
const { Tor2eJourneyEngine, TERRAIN_DIFFICULTY, SEASON } = await importV9('src/domain/rpg/rules/Tor2eJourneyEngine.mjs');
const { Tor2eCouncilEngine, COUNCIL_ATTITUDE } = await importV9('src/domain/rpg/rules/Tor2eCouncilEngine.mjs');
const { Tor2eFellowshipEngine } = await importV9('src/domain/rpg/rules/Tor2eFellowshipEngine.mjs');

test('OUA G03-01: Livro Basico 2e, Valfenda, Povos das Terras Ermas, Trilhas V2 e Modo Peregrino conferem com os PDFs oficiais', () => {
  // 1. Livro Basico 2e (6 culturas base + motores de dados, combate, jornada, conselho, sociedade)
  assert.equal(CORE_CULTURES_2E.length, 6);
  const coreIds = CORE_CULTURES_2E.map(c => c.id);
  assert.deepEqual(coreIds, [
    'bardings',
    'dwarves-durin',
    'elves-lindon',
    'hobbits-shire',
    'men-of-bree',
    'dunedain-rangers'
  ]);

  const dice = new Tor2eDiceEngine();
  const combat = new Tor2eCombatEngine(dice);
  const journey = new Tor2eJourneyEngine();
  const council = new Tor2eCouncilEngine();
  const fellowship = new Tor2eFellowshipEngine();

  assert.equal(combat.calculateAttackTN({ defenderBaseTN: 14, attackerStance: COMBAT_STANCE.FORWARD }).finalTN, 12);
  assert.equal(combat.calculateAttackTN({ defenderBaseTN: 14, attackerStance: COMBAT_STANCE.DEFENSIVE }).finalTN, 16);
  assert.equal(journey.calculateLegFatigue(TERRAIN_DIFFICULTY.WILD_LANDS, SEASON.WINTER), 3);
  assert.equal(council.startCouncil({ title: 'Conselho', attitude: COUNCIL_ATTITUDE.OPEN, highestWisdomOrHeart: 3, targetSuccesses: 2 }).currentTolerance, 5);
  assert.equal(fellowship.trainSkill({ adventurePoints: 4, skills: [{ name: 'Cura', rank: 1 }] }, 'Cura').skills[0].rank, 2);

  // 2. Valfenda 2e (pp. 1-16): 13 locais do mapa (p. 3), 15 salas da Casa de Elrond (p. 5), 4 virtudes impressas (p. 12), regras especiais
  assert.equal(VALFENDA_EXPANSION.canonicalAuditMarker, 'HARDENED_CANON_VALFENDA_V9');
  assert.equal(VALFENDA_EXPANSION.canonicalMapLocations13.length, 13);
  assert.equal(VALFENDA_EXPANSION.canonicalMapLocations13[0].name, 'A Casa de Elrond');
  assert.equal(VALFENDA_EXPANSION.canonicalMapLocations13[12].name, 'A Ponte');
  assert.equal(VALFENDA_EXPANSION.houseOfElrondRooms15.length, 15);
  assert.equal(VALFENDA_EXPANSION.canonicalRules.safeBorders.printedPage, 3);
  assert.equal(VALFENDA_EXPANSION.canonicalRules.makingMusicInRivendell.printedPage, 4);
  assert.equal(VALFENDA_EXPANSION.canonicalRules.elvenBeauty.printedPage, 7);
  assert.equal(VALFENDA_EXPANSION.canonicalRules.miruvorCordial.printedPage, 8);
  assert.equal(VALFENDA_EXPANSION.canonicalRules.struckByDefiance.printedPage, 10);
  assert.equal(VALFENDA_EXPANSION.canonicalRules.highElfPrintedVirtues.length, 4);

  // 3. Povos das Terras Ermas 2e (pp. 1-10): 3 culturas canonicas do suplemento + zero duplicatas de Bardings
  assert.equal(POVOS_TERRAS_ERMAS_EXPANSION.canonicalAuditMarker, 'HARDENED_CANON_POVOS_ERMAS_V9');
  assert.deepEqual(POVOS_TERRAS_ERMAS_EXPANSION.canonicalBookCulturesIds, [
    'beornings',
    'woodland-elves',
    'woodmen-of-wilderland'
  ]);
  for (const cid of POVOS_TERRAS_ERMAS_EXPANSION.canonicalBookCulturesIds) {
    const culture = POVOS_TERRAS_ERMAS_EXPANSION.cultures.find(c => c.id === cid);
    assert.ok(culture, `Cultura ${cid} ausente`);
    assert.equal(culture.attributeTable.length, 6);
    assert.equal(culture.culturalVirtues.length, 6);
  }
  const allCultures = Tor2eContentCatalog.getAllCultures();
  assert.equal(allCultures.filter(c => c.id === 'bardings').length, 1);

  // 4. Trilhas de Personagens V2 (pp. 1-10): 36 antecedentes (6x6) + 12 faces de Eventos Importantes
  const bgKeys = Object.keys(ANTECEDENTES_CULTURAIS_36);
  assert.equal(bgKeys.length, 6);
  let totalBackgrounds = 0;
  for (const k of bgKeys) {
    assert.equal(ANTECEDENTES_CULTURAIS_36[k].length, 6);
    totalBackgrounds += ANTECEDENTES_CULTURAIS_36[k].length;
  }
  assert.equal(totalBackgrounds, 36);
  assert.equal(TABELA_EVENTOS_IMPORTANTES.length, 12);
  for (let face = 1; face <= 12; face++) {
    const ev = TrilhasPersonagensModule.getImportantEvent(face);
    assert.equal(ev.dieFace, face);
  }

  // 5. Modo Peregrino 2e (pp. 1-28): regras de criacao, Tabela de Narracao, Sorte/Ma Sorte, Olho/Revelacao, Escaramuca, Marcos e Missoes
  assert.equal(STRIDER_CREATION_RULES.previousExperiencePoints, 15);
  assert.equal(STRIDER_CREATION_RULES.baseFellowshipPool, 3);
  assert.equal(STRIDER_CREATION_RULES.targetNumberBase, 18);
  assert.equal(FORTUNE_TABLE.length, 12);
  assert.equal(ILL_FORTUNE_TABLE.length, 12);
  assert.equal(REVELATION_EPISODES_TABLE.length, 12);
  assert.equal(EXPERIENCE_MILESTONES_SOLO.length, 10);
  assert.equal(Object.keys(PATRON_QUESTS_SOLO).length, 6);
  assert.equal(EYE_HUNT_THRESHOLDS.BORDER_LAND.huntThreshold, 18);
  assert.equal(EYE_HUNT_THRESHOLDS.WILD_LAND.huntThreshold, 16);
  assert.equal(EYE_HUNT_THRESHOLDS.DARK_LAND.huntThreshold, 14);
  assert.equal(SKIRMISH_STANCE_RULES.combatTask.name, 'Ganhar Terreno (Gain Ground)');
  assert.equal(ModoPeregrinoModule.consultTellingTable('CERTAIN', 1).isYes, true);
  assert.equal(ModoPeregrinoModule.consultTellingTable('UNTHINKABLE', 9).isYes, false);
  assert.equal(ModoPeregrinoModule.consultTellingTable('UNTHINKABLE', 10).isYes, true);
});

test('OUA G04-01: Walkthrough completo de ponta a ponta das 6 Aventuras de Contos das Terras Solitarias + A Estrela da Nevoa (p. 223-230)', () => {
  const allAdventures = Tor2eContentCatalog.getAllAdventures();
  assert.equal(allAdventures.length, 7);

  // Verifica enriquecimento canonico de A Estrela da Nevoa (pp. 223-230)
  assert.equal(MARCO_A_ESTRELA_DA_NEVOA.canonicalAuditMarker, 'HARDENED_CANON_STAR_OF_THE_MIST_V9');
  assert.equal(MARCO_A_ESTRELA_DA_NEVOA.loreAndRumorsTable.length, 4);
  const sanctumScene = MARCO_A_ESTRELA_DA_NEVOA.scenes[4];
  assert.equal(sanctumScene.scenarioConditions.elwenCanonicalBookProfile.attributeLevel, 5);
  assert.equal(sanctumScene.scenarioConditions.elwenCanonicalBookProfile.endurance, 24);
  assert.equal(sanctumScene.scenarioConditions.elwenCanonicalBookProfile.might, 2);
  assert.equal(sanctumScene.scenarioConditions.elwenCanonicalBookProfile.hate, 5);
  assert.equal(sanctumScene.scenarioConditions.explorationLocations.length, 7);

  // Executa walkthrough deterministico de todas as 7 aventuras do inicio a conclusao
  const controller = new Tor2eAdventureController(allAdventures);

  for (const adv of allAdventures) {
    let view = controller.startAdventure(adv.id);
    assert.equal(view.adventureId, adv.id);

    const visitedScenes = [];
    while (true) {
      visitedScenes.push(view.sceneId);
      const currentSceneDef = adv.scenes.find(s => s.id === view.sceneId);
      assert.ok(currentSceneDef, `Cena ${view.sceneId} nao encontrada em ${adv.id}`);

      // Resolve todos os checks obrigatorios da cena atual
      for (const chk of view.checks) {
        if (!chk.optional) {
          const checkRes = controller.performSceneCheck(chk.checkId, {
            skillRank: 4,
            mockFeatRolls: [10],
            mockSuccessRolls: [6, 6, 6, 6]
          });
          assert.equal(checkRes.rollResult.isSuccess, true);
        }
      }

      // Se houver encontro de combate na cena, derrota todos os inimigos ativos
      if (view.combatEncounter && view.combatEncounter.enemies.length > 0) {
        let safetyCounter = 0;
        while (!controller.isCurrentSceneResolved() && safetyCounter < 20) {
          safetyCounter++;
          controller.performSceneCombatAttack({
            attacker: { name: 'Guardiao de Arnor', weary: false, strengthBonus: 4 },
            attackerStance: 'FORWARD',
            weapon: { name: 'Lamina de Arthedain', damage: 30, injury: 22, group: 'Espadas' },
            skillRank: 4,
            mockFeatRolls: [10],
            mockSuccessRolls: [6, 6, 6, 6],
            mockProtectionFeatRolls: [1],
            mockProtectionSuccessRolls: [1, 1, 1]
          });
        }
      }

      assert.equal(
        controller.isCurrentSceneResolved(),
        true,
        `Cena ${view.sceneId} de ${adv.id} deveria estar resolvida`
      );

      // Se a cena permite conclusao ou nao possui proxima cena, conclui a aventura
      if (currentSceneDef.allowConclusion || view.nextSceneIds.length === 0) {
        const hero = {
          name: 'Targon',
          resources: { adventurePoints: 0, treasure: 0 },
          claimedAdventureRewards: []
        };
        const conclusion = controller.concludeAdventure(hero);
        assert.equal(
          conclusion.status,
          'ADVENTURE_CONCLUDED',
          `Falha ao concluir aventura ${adv.id} na cena ${view.sceneId}`
        );
        assert.ok(conclusion.rewardsGranted.adventurePoints >= 1);
        break;
      }

      // Avanca para a proxima cena
      view = controller.transitionToScene(view.nextSceneIds[0]);
    }

    assert.ok(visitedScenes.length >= 3, `Walkthrough de ${adv.id} visitou poucas cenas`);
  }
});

test('OUA G05-01: RAG possui 182 entradas ancoradas nos 8 livros/partes e mantem isolamento estrito PLAYER vs GM', () => {
  assert.equal(RAG_CORPUS.length, 182);

  const byBook = RAG_CORPUS.reduce((acc, item) => {
    acc[item.book] = (acc[item.book] || 0) + 1;
    return acc;
  }, {});

  assert.deepEqual(byBook, {
    'Livro Básico 2e (pp. 1-80)': 23,
    'Livro Básico 2e (pp. 81-150)': 13,
    'Livro Básico 2e (pp. 151-240)': 7,
    'Valfenda 2e': 29,
    'Povos das Terras Ermas 2e': 22,
    'Modo Peregrino 2e': 56,
    'Contos das Terras Solitárias 2e': 13,
    'Trilhas de Personagens 2e': 19
  });

  for (const item of RAG_CORPUS) {
    assert.ok(item.id);
    assert.ok(item.book);
    assert.ok(item.driveFileId);
    assert.ok(typeof item.printedPage === 'number' && item.printedPage >= 1);
    assert.equal(typeof item.sha256Anchor, 'string');
    assert.equal(item.sha256Anchor.length, 16);
    assert.equal(typeof item.sourcePdfFileSha256, 'string');
    assert.equal(item.sourcePdfFileSha256.length, 64);
    assert.notEqual(item.scope, 'GM_SECRET');
  }

  // 1. Chave nao provisionada no cofre DEK/KEK do Host nao consegue decifrar o envelope AES-256-GCM
  const unprovisionedSecret = 'CHAVE_NAO_PROVISIONADA';
  const unprovisionedHash = crypto.createHash('sha256').update(unprovisionedSecret).digest('hex');
  const unprovRag = new Tor2eRAGService(RAG_CORPUS, { hostGmSecretHash: unprovisionedHash });
  const unprovSearch = unprovRag.query({ text: 'Amon Guruthos', userRole: 'GM', gmSecretKey: unprovisionedSecret });
  assert.equal(unprovSearch.results.some(r => r.scope === 'GM_SECRET'), false);

  // 2. Chave provisionada do Host ('HOST_PRIVATE_KEY_998877') desenvelopa a DEK e libera GM_SECRET
  const hostSecret = 'HOST_PRIVATE_KEY_998877';
  const hostHash = crypto.createHash('sha256').update(hostSecret).digest('hex');
  const rag = new Tor2eRAGService(RAG_CORPUS, { hostGmSecretHash: hostHash });

  const playerSearch = rag.query({ text: 'Amon Guruthos', userRole: 'PLAYER' });
  assert.equal(playerSearch.isMasterAuthorized, false);
  assert.equal(playerSearch.results.some(r => r.scope === 'GM_SECRET'), false);

  const gmSearch = rag.query({ text: 'Amon Guruthos', userRole: 'GM', gmSecretKey: hostSecret });
  assert.equal(gmSearch.isMasterAuthorized, true);
  assert.equal(gmSearch.results.some(r => r.scope === 'GM_SECRET'), true);
});

test('OUA G04-02: Bridge Android lib/tor2eV9HostBridge.ts expoe as 7 aventuras canonicas (Estrela da Nevoa + 6 Contos) e executa core-star-of-the-mist via Tor2eRulesGuard', async () => {
  const bridgeUrl = pathToFileURL(path.join(androidWorkRoot, 'lib', 'tor2eV9HostBridge.ts')).href;
  const { V9_LONE_LANDS_ADVENTURES, dispatchV9AdventureCommand } = await import(bridgeUrl);

  assert.equal(V9_LONE_LANDS_ADVENTURES.length, 7);
  assert.equal(V9_LONE_LANDS_ADVENTURES[0].id, 'core-star-of-the-mist');
  assert.equal(V9_LONE_LANDS_ADVENTURES[0].sceneCount, 5);

  const mockHero = {
    id: 'hero-star-1',
    name: 'Berethor',
    sheet: {
      identity: { name: 'Berethor' },
      attributes: {
        strength: 14,
        heart: 14,
        wits: 14,
        enduranceCurrent: 24,
        hopeCurrent: 12,
        hopeMax: 12,
        load: 6,
        fatigue: 0,
        shadow: 0,
        weary: false,
        miserable: false,
        wounded: false,
        dying: false,
        dead: false,
        unconscious: false
      },
      skills: [
        { name: 'Exploração', rank: 4 },
        { name: 'História', rank: 3 },
        { name: 'Ofício', rank: 3 },
        { name: 'Busca', rank: 3 },
        { name: 'Atletismo', rank: 3 }
      ],
      combat: {
        proficiencies: { swords: 3 },
        weapons: [{ name: 'Espada Longa', damage: 5, injury: 16, group: 'swords', active: true }]
      },
      resources: { adventurePoints: 0, treasure: 0 }
    }
  };

  const baseState = {
    schemaVersion: 3,
    activeMode: 'adventure',
    currentSessionId: 'sess-star-01',
    position: { hexId: 'hex-sarn-ford', locationName: 'Vau Sarn' },
    company: { loneLands: {} },
    updatedAt: Date.now()
  };

  const started = dispatchV9AdventureCommand({
    state: baseState,
    characters: [mockHero],
    commandId: 'cmd-star-start-01',
    actionType: 'START_ADVENTURE',
    adventureId: 'core-star-of-the-mist',
    targetCharacterId: 'hero-star-1'
  });

  assert.equal(started.dispatchResult.status, 'APPLIED');
  assert.equal(started.nextState.company.loneLands.v9AdventureView.adventureId, 'core-star-of-the-mist');
  assert.equal(started.nextState.company.loneLands.v9AdventureView.sceneId, 'scene-star-01-rumors');
});

