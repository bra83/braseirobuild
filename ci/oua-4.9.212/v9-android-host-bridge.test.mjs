import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const androidRoot = process.env.OUA_ANDROID_ROOT
  ? path.resolve(process.env.OUA_ANDROID_ROOT)
  : 'D:\\OUA_49212_WORKSPACE\\oua-private\\ANDROID_49212_WORK';

const libRoot = path.join(androidRoot, 'lib');

function makeSheet(name = 'Beren de Bri') {
  return {
    identity: {
      name,
      culture: 'Homens de Bri',
      vocation: 'Guardião',
      lifestyle: 'Próspero',
      age: 32,
      description: '',
      patron: 'Tom Cevada',
      company: 'Companhia de Bri',
    },
    attributes: {
      strength: 14,
      heart: 14,
      wits: 14,
      enduranceCurrent: 24,
      enduranceMax: 24,
      hopeCurrent: 12,
      hopeMax: 12,
      shadow: 0,
      fatigue: 0,
      load: 6,
      wounded: false,
      weary: false,
      miserable: false,
      supplies: 10,
    },
    skills: [
      { name: 'Caçada', rank: 3, specialization: '', favorite: true },
      { name: 'Enigma', rank: 2, specialization: '', favorite: false },
      { name: 'Viagem', rank: 2, specialization: '', favorite: false },
      { name: 'Vigilância', rank: 2, specialization: '', favorite: false },
      { name: 'Busca', rank: 2, specialization: '', favorite: false },
      { name: 'Exploração', rank: 2, specialization: '', favorite: false },
      { name: 'Atletismo', rank: 2, specialization: '', favorite: false },
      { name: 'Furtividade', rank: 2, specialization: '', favorite: false },
      { name: 'Batalha', rank: 2, specialization: '', favorite: false },
    ],
    combat: {
      parry: 15,
      armor: 'Cota de Malha',
      helmet: 'Elmo',
      shield: 'Escudo',
      proficiencies: { swords: 3, axes: 2, bows: 2, spears: 2 },
      weapons: [
        { name: 'Espada Longa', damage: 5, injury: 16, group: 'swords', active: true },
      ],
    },
    resources: {
      coins: 2,
      treasure: 0,
      experience: 10,
      adventurePoints: 2,
      skillPoints: 2,
    },
    inventory: [],
    other: {
      virtues: '',
      rewards: '',
      flaws: '',
      notes: '',
      narrativeConditions: '',
    },
    tokenId: null,
    historyLog: [],
  };
}

function createFixture() {
  return import(new URL(`file:///${libRoot.replace(/\\/g, '/')}/master/state.ts`)).then(({ createDefaultMasterState }) => {
    const heroes = [
      { id: 'hero_1', name: 'Beren de Bri', culture: 'Homens de Bri', vocation: 'Guardião', sheet: makeSheet('Beren de Bri') },
    ];
    const campaign = {
      id: 'camp_v9_e2e',
      name: 'Contos das Terras Solitárias',
      characterSheet: structuredClone(heroes[0].sheet),
      activeCharacterId: 'hero_1',
      partyCharacterIds: ['hero_1'],
      currentLocation: 'Bri',
      currentChapter: 'Aventura 1',
      campaignSummary: '',
      recentEvents: '',
      clues: '',
      mysteries: '',
      quests: '',
      worldFlags: {},
      presentNpcIds: [],
      history: [],
      pendingDiceRoll: null,
      pendingTest: null,
      events: [],
      firstImageGenerated: false,
      createdAt: 1000,
      updatedAt: 1000,
    };
    campaign.masterState = createDefaultMasterState(campaign);
    return { campaign, heroes };
  });
}

test('OUA E2E-01 (G02 + G09): Single ingress architecture — only lib/tor2eV9HostBridge.ts imports Tor2eRulesGuard, zero direct controller/engine imports in UI or lib', () => {
  const dirsToScan = [path.join(androidRoot, 'lib'), path.join(androidRoot, 'components')];
  const guardImporters = [];
  const forbiddenDirectImporters = [];

  function walk(dir) {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) walk(full);
      else if (/\.(ts|tsx|js|mjs)$/.test(entry.name)) {
        const text = fs.readFileSync(full, 'utf8');
        const rel = path.relative(androidRoot, full).replace(/\\/g, '/');
        if (text.includes('tor2e-v49212/application/Tor2eRulesGuard')) {
          guardImporters.push(rel);
        }
        if (
          text.includes('tor2e-v49212/ui/bridges/Tor2eAdventureController') ||
          text.includes('tor2e-v49212/domain/rpg/rules/Tor2eDiceEngine') ||
          text.includes('tor2e-v49212/domain/rpg/rules/Tor2eCombatEngine')
        ) {
          forbiddenDirectImporters.push(rel);
        }
      }
    }
  }
  for (const d of dirsToScan) walk(d);

  assert.deepEqual(guardImporters, ['lib/tor2eV9HostBridge.ts'], 'Apenas lib/tor2eV9HostBridge.ts pode importar Tor2eRulesGuard');
  assert.deepEqual(forbiddenDirectImporters, [], 'Nenhum arquivo de lib/ ou components/ pode importar Tor2eAdventureController/DiceEngine/CombatEngine diretamente');
});

test('OUA E2E-02 (G02 + G09): End-to-end UI command -> applyMasterCommands -> Tor2eRulesGuard -> save/reopen -> replay rejected', async () => {
  const { applyMasterCommands } = await import(new URL(`file:///${libRoot.replace(/\\/g, '/')}/master/commands.ts`));
  const { campaign, heroes } = await createFixture();

  // 1. Start Adventure 1 (tales-01-troll-hole)
  const step1 = applyMasterCommands(campaign, heroes, [], [
    {
      type: 'v9_dispatch_adventure_action',
      commandId: 'cmd_e2e_start_01',
      kind: 'PLAYER_ACTION',
      actionType: 'START_ADVENTURE',
      adventureId: 'tales-01-troll-hole',
      targetCharacterId: 'hero_1',
      reason: 'Iniciar Aventura 1 pelo painel LoneLandsPanel.',
    },
  ]);
  assert.equal(step1.rejected.length, 0);
  assert.equal(step1.campaign.masterState.company.loneLands.v9AdventureView.adventureId, 'tales-01-troll-hole');
  assert.equal(step1.campaign.masterState.company.loneLands.v9AdventureView.sceneId, 'scene-01-investigation');
  assert.ok(step1.campaign.masterState.company.loneLands.v9ExecutedCommandIds.includes('cmd_e2e_start_01'));

  // 2. Simulate app save, close, and reopen (JSON roundtrip of persisted CampaignState + Character[])
  const reopenedCampaign = JSON.parse(JSON.stringify(step1.campaign));
  const reopenedHeroes = JSON.parse(JSON.stringify(step1.characters));

  // 3. Replay of cmd_e2e_start_01 after reopen MUST be rejected!
  const replayAttempt = applyMasterCommands(reopenedCampaign, reopenedHeroes, [], [
    {
      type: 'v9_dispatch_adventure_action',
      commandId: 'cmd_e2e_start_01',
      kind: 'PLAYER_ACTION',
      actionType: 'START_ADVENTURE',
      adventureId: 'tales-01-troll-hole',
      targetCharacterId: 'hero_1',
      reason: 'Tentativa de replay após reabrir o app.',
    },
  ]);
  assert.equal(replayAttempt.rejected.length, 1);
  assert.match(replayAttempt.rejected[0].reason, /DUPLICATE_REJECTED/);

  // 4. Perform mandatory scene check (Caçada TN 14) on reopened state
  const step2 = applyMasterCommands(reopenedCampaign, reopenedHeroes, [], [
    {
      type: 'v9_dispatch_adventure_action',
      commandId: 'cmd_e2e_check_cacada',
      kind: 'PLAYER_ACTION',
      actionType: 'PERFORM_SCENE_CHECK',
      targetCharacterId: 'hero_1',
      checkId: 'scene-01-investigation:Caçada:1',
      skill: 'Caçada',
      spendHope: true,
      reason: 'Rolar Caçada na Cena 1.',
    },
  ]);
  assert.equal(step2.rejected.length, 0);
  const viewAfterCheck = step2.campaign.masterState.company.loneLands.v9AdventureView;
  const passed = viewAfterCheck.passedCheckIds.includes('scene-01-investigation:Caçada:1');
  const failed = viewAfterCheck.failedCheckIds.includes('scene-01-investigation:Caçada:1');
  assert.ok(passed || failed, 'O teste obrigatório deve ter sido registrado como passado ou falhado');

  // 5. If mandatory check failed, apply Fail-Forward so scene resolves; then transition to scene-01b-east-road-journey
  let resolvedCampaign = step2.campaign;
  let resolvedHeroes = step2.characters;
  if (!viewAfterCheck.isSceneResolved) {
    const ffStep = applyMasterCommands(resolvedCampaign, resolvedHeroes, [], [
      {
        type: 'v9_dispatch_adventure_action',
        commandId: 'cmd_e2e_ff_01',
        kind: 'PLAYER_ACTION',
        actionType: 'APPLY_FAIL_FORWARD',
        targetCharacterId: 'hero_1',
        confirmSkipOptional: true,
        reason: 'Avanço sob custo na Cena 1.',
      },
    ]);
    assert.equal(ffStep.rejected.length, 0);
    resolvedCampaign = ffStep.campaign;
    resolvedHeroes = ffStep.characters;
  }
  assert.equal(resolvedCampaign.masterState.company.loneLands.v9AdventureView.isSceneResolved, true);

  const step3 = applyMasterCommands(resolvedCampaign, resolvedHeroes, [], [
    {
      type: 'v9_dispatch_adventure_action',
      commandId: 'cmd_e2e_trans_01b',
      kind: 'PLAYER_ACTION',
      actionType: 'TRANSITION_SCENE',
      targetCharacterId: 'hero_1',
      nextSceneId: 'scene-01b-east-road-journey',
      reason: 'Avançar para a Cena 1b.',
    },
  ]);
  assert.equal(step3.rejected.length, 0);
  assert.equal(step3.campaign.masterState.company.loneLands.v9AdventureView.sceneId, 'scene-01b-east-road-journey');
});

test('OUA E2E-03 (G09 + G10): Client-injected mock dice, forged sheets/weapons, and unverified GM_HELP are blocked; atomic rollback on save failure', async () => {
  const { applyMasterCommands } = await import(new URL(`file:///${libRoot.replace(/\\/g, '/')}/master/commands.ts`));
  const { dispatchV9AdventureCommand } = await import(new URL(`file:///${libRoot.replace(/\\/g, '/')}/tor2eV9HostBridge.ts`));
  const { campaign, heroes } = await createFixture();

  const started = applyMasterCommands(campaign, heroes, [], [
    {
      type: 'v9_dispatch_adventure_action',
      commandId: 'cmd_sec_start',
      kind: 'PLAYER_ACTION',
      actionType: 'START_ADVENTURE',
      adventureId: 'tales-01-troll-hole',
      targetCharacterId: 'hero_1',
      reason: 'Iniciar aventura para auditoria de segurança.',
    },
  ]);
  assert.equal(started.rejected.length, 0);

  // 1. Forged mock dice in production must be rejected
  const forgedDice = applyMasterCommands(started.campaign, started.characters, [], [
    {
      type: 'v9_dispatch_adventure_action',
      commandId: 'cmd_sec_forged_dice',
      kind: 'PLAYER_ACTION',
      actionType: 'PERFORM_SCENE_CHECK',
      targetCharacterId: 'hero_1',
      skill: 'Caçada',
      clientInjectedPayload: { mockFeatRolls: [12], mockSuccessRolls: [6, 6, 6] },
      reason: 'Tentativa de injetar dados viciados.',
    },
  ]);
  assert.equal(forgedDice.rejected.length, 1);
  assert.match(forgedDice.rejected[0].reason, /proibidos|EST-12/i);

  // 2. Forged character sheet in production must be rejected
  const forgedSheet = applyMasterCommands(started.campaign, started.characters, [], [
    {
      type: 'v9_dispatch_adventure_action',
      commandId: 'cmd_sec_forged_sheet',
      kind: 'PLAYER_ACTION',
      actionType: 'PERFORM_SCENE_CHECK',
      targetCharacterId: 'hero_1',
      skill: 'Caçada',
      clientInjectedPayload: { authorizedCharacterSheet: { skills: [{ name: 'Caçada', rank: 6 }] } },
      reason: 'Tentativa de injetar ficha falsa.',
    },
  ]);
  assert.equal(forgedSheet.rejected.length, 1);
  assert.match(forgedSheet.rejected[0].reason, /Ficha enviada pelo cliente não é autorizada/i);

  // 3. Unverified GM_HELP cannot elevate to GM via clientInjectedPayload.userRole='GM'
  const gmAttempt = applyMasterCommands(started.campaign, started.characters, [], [
    {
      type: 'v9_dispatch_adventure_action',
      commandId: 'cmd_sec_gm_help',
      kind: 'GM_HELP',
      query: 'segredo Nelly Longarms',
      clientInjectedPayload: { userRole: 'GM' },
      reason: 'Consulta GM_HELP sem PIN nativo.',
    },
  ]);
  assert.equal(gmAttempt.rejected.length, 0);
  const helpRes = gmAttempt.campaign.masterState.company.loneLands.v9LastHelpResponse;
  assert.ok(helpRes);
  assert.equal(JSON.stringify(helpRes).includes('gmSecret'), false);

  // 4. Atomic rollback if persistenceHook fails or returns a Promise
  const preSnap = JSON.stringify(started.campaign.masterState);
  assert.throws(() => {
    dispatchV9AdventureCommand({
      state: started.campaign.masterState,
      characters: started.characters,
      commandId: 'cmd_sec_async_fail',
      kind: 'PLAYER_ACTION',
      actionType: 'PERFORM_SCENE_CHECK',
      targetCharacterId: 'hero_1',
      skill: 'Caçada',
      persistenceHook: () => Promise.resolve(true),
    });
  }, /Promise não é persistência atômica síncrona/);
  assert.equal(JSON.stringify(started.campaign.masterState), preSnap, 'Estado deve permanecer 100% intacto após rollback atômico');
});

test('OUA E2E-04: auditoria arquitetural de ingress único e bundle Vite sem segredos', () => {
  function walkFiles(dir, acc = []) {
    if (!fs.existsSync(dir)) return acc;
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) walkFiles(full, acc);
      else if (/\.(ts|tsx|js|mjs)$/.test(entry.name)) acc.push(full);
    }
    return acc;
  }

  const appFiles = [
    ...walkFiles(path.join(androidRoot, 'lib')),
    ...walkFiles(path.join(androidRoot, 'components')),
  ];

  const rulesGuardImporters = [];
  const forbiddenControllerImporters = [];
  for (const file of appFiles) {
    const content = fs.readFileSync(file, 'utf8');
    const rel = path.relative(androidRoot, file).replace(/\\/g, '/');
    if (content.includes('Tor2eRulesGuard')) rulesGuardImporters.push(rel);
    if (content.includes('Tor2eAdventureController')) forbiddenControllerImporters.push(rel);
  }

  assert.deepEqual(
    rulesGuardImporters,
    ['lib/tor2eV9HostBridge.ts'],
    'Apenas lib/tor2eV9HostBridge.ts pode importar Tor2eRulesGuard',
  );
  assert.deepEqual(
    forbiddenControllerImporters,
    [],
    'Nenhum arquivo em lib/ ou components/ pode importar Tor2eAdventureController diretamente',
  );

  const distAssetsDir = path.join(androidRoot, 'dist', 'assets');
  assert.equal(fs.existsSync(distAssetsDir), true, 'Bundle Vite dist/assets deve existir');
  const jsBundles = fs.readdirSync(distAssetsDir).filter((f) => f.endsWith('.js'));
  assert.equal(jsBundles.length > 0, true, 'Pelo menos um bundle JS deve existir em dist/assets');
  const combinedBundle = jsBundles.map((f) => fs.readFileSync(path.join(distAssetsDir, f), 'utf8')).join('\n');

  assert.equal(combinedBundle.includes('v9_dispatch_adventure_action'), true);
  assert.equal(combinedBundle.includes('6 Aventuras Canônicas'), true);
  assert.equal(combinedBundle.includes('ouaExecutedCommandIds'), true);
  assert.equal(combinedBundle.includes('gm-master-pin-2026'), false, 'PIN GM nunca deve vazar no bundle público');
  assert.equal(combinedBundle.includes('Tor2eGmSecretsVault'), false, 'Tor2eGmSecretsVault nunca deve estar no bundle');
  assert.equal(combinedBundle.includes('node:fs'), false, 'node:fs nunca deve estar no bundle WebView');
});

