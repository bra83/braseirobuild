import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";

const androidWorkRoot = path.resolve(
  process.env.OUA_ANDROID_WORK_DIR ||
    "D:\\OUA_49212_WORKSPACE\\oua-private\\ANDROID_49212_WORK",
);
const libRoot = path.join(androidWorkRoot, "lib");
const importLib = (rel) => import(pathToFileURL(path.join(libRoot, rel)).href);

function makeSheet(name = "Beran de Bri", culture = "Homens de Bri", vocation = "Guardião") {
  return {
    identity: {
      name,
      culture,
      vocation,
      lifestyle: "Próspero",
      age: 32,
      description: "",
      patron: "Tom Cevada",
      company: "Companhia de Bri",
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
      { name: "Vigilância", rank: 3, specialization: "", favorite: true },
      { name: "Exploração", rank: 3, specialization: "", favorite: true },
      { name: "História", rank: 3, specialization: "", favorite: true },
      { name: "Batalha", rank: 2, specialization: "", favorite: false },
    ],
    combat: {
      parry: 15,
      armor: "Cota de Malha",
      helmet: "Elmo",
      shield: "Escudo",
      proficiencies: { swords: 3, axes: 2, bows: 2, spears: 2 },
      weapons: [
        { name: "Espada Longa", damage: 5, injury: 16, group: "swords", active: true },
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
      virtues: "",
      rewards: "",
      flaws: "",
      notes: "",
      narrativeConditions: "",
    },
    tokenId: null,
    historyLog: [],
  };
}

async function createFixture() {
  const { createDefaultMasterState } = await importLib("master/state.ts");
  const heroes = [
    {
      id: "hero_1",
      name: "Beran de Bri",
      culture: "Homens de Bri",
      vocation: "Guardião",
      sheet: makeSheet("Beran de Bri", "Homens de Bri", "Guardião"),
      createdAt: 1000,
      updatedAt: 1000,
    },
    {
      id: "hero_2",
      name: "Elrohir",
      culture: "Elfos de Lindon",
      vocation: "Erudito",
      sheet: makeSheet("Elrohir", "Elfos de Lindon", "Erudito"),
      createdAt: 1000,
      updatedAt: 1000,
    },
  ];
  const campaign = {
    id: "camp_etapa04_g10",
    name: "Campanha OUA 4.9.212 G10",
    characterSheet: structuredClone(heroes[0].sheet),
    activeCharacterId: "hero_1",
    partyCharacterIds: ["hero_1", "hero_2"],
    currentLocation: "Bri",
    currentChapter: "Aventura 1",
    campaignSummary: "",
    recentEvents: "",
    clues: "",
    mysteries: "",
    quests: "",
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
}

test("OUA G10-01: Motor Bárbara 1.1.3 — protocolo v2, manifesto SHA-256, bloqueio de meta-linguagem/agência/ferimento não autorizado e perfis Galaxy A57 / S23 Ultra", async () => {
  const versionMod = await importLib("version.ts");
  const bridgeMod = await importLib("tor2eV9HostBridge.ts");

  assert.equal(versionMod.APP_VERSION, "4.9.212");
  assert.equal(versionMod.APP_VERSION_CODE, 49212);
  assert.equal(versionMod.BARBARA_VERSION, "1.1.3");
  assert.equal(bridgeMod.BARBARA_113_MANIFEST.version, "1.1.3");
  assert.equal(bridgeMod.BARBARA_113_MANIFEST.host_contract, "barbara-protocol-v2");
  assert.equal(
    bridgeMod.BARBARA_113_MANIFEST.wheel_sha256,
    "3e9bbb1142040ca7caa042e8a75cc26837bc40dfdb22ce1efc4481102841e896",
  );

  // Unsupported device profile must throw
  assert.throws(
    () => bridgeMod.createBarbaraSupervisor("unsupported_device"),
    /BARBARA_UNSUPPORTED_DEVICE_PROFILE/,
  );

  // Valid PT-BR narrative
  const cleanCheck = bridgeMod.superviseV9NarrativeWithBarbara(
    "A neblina fria desce sobre o vau de pedra enquanto os corvos grasnam ao longe.",
    {
      engineWoundTriggered: false,
      companionMode: "EU_CONTROLO",
      deviceProfile: "samsung_a57",
    },
  );
  assert.equal(cleanCheck.valid, true);
  assert.equal(cleanCheck.violations.length, 0);
  assert.equal(cleanCheck.ttsSynthesisConfig.language, "pt-BR");
  assert.equal(cleanCheck.ttsSynthesisConfig.chunkSizeMs, 180);

  const s23Check = bridgeMod.superviseV9NarrativeWithBarbara(
    "O vento sopra entre as ruínas antigas de Arnor.",
    {
      engineWoundTriggered: false,
      deviceProfile: "samsung_s23_ultra",
    },
  );
  assert.equal(s23Check.valid, true);
  assert.equal(s23Check.ttsSynthesisConfig.chunkSizeMs, 120);

  // Meta-term violation (d12, TN, skill check)
  const metaCheck = bridgeMod.superviseV9NarrativeWithBarbara(
    "Role 1d12 contra TN 14 para passar no skill check.",
    { engineWoundTriggered: false },
  );
  assert.equal(metaCheck.valid, false);
  assert.ok(metaCheck.violations.some((v) => v.type === "FORBIDDEN_META_TERM"));

  // Agency violation ("você decide" / autonomous master action on EU_CONTROLO)
  const agencyCheck = bridgeMod.superviseV9NarrativeWithBarbara(
    "Sem hesitar, você decide atacar o guarda do portão.",
    {
      engineWoundTriggered: false,
      companionMode: "EU_CONTROLO",
      autonomousMasterActionAttempted: true,
    },
  );
  assert.equal(agencyCheck.valid, false);
  assert.ok(agencyCheck.violations.some((v) => v.type === "AGENCY_VIOLATION"));
  assert.ok(
    agencyCheck.violations.some((v) => v.type === "COMPANION_AGENCY_VIOLATION"),
  );

  // Unbacked wound violation
  const woundCheck = bridgeMod.superviseV9NarrativeWithBarbara(
    "A lâmina abre uma ferida mortal no seu ombro.",
    { engineWoundTriggered: false },
  );
  assert.equal(woundCheck.valid, false);
  assert.ok(woundCheck.violations.some((v) => v.type === "UNBACKED_WOUND_CLAIM"));
});

test("OUA G10-02: TTS Charon/Kokoro PT-BR — segmentação em duas camadas sem corte no meio de palavra e preservação de motor", async () => {
  const narrationMod = await importLib("narration.ts");

  const paragraph =
    "O Sr. Todo-Mundo observou o mapa de Eriador na p. 223 do livro antigo. — Não devemos atravessar o pântano à noite! — avisou o Guia, apontando para as colinas de Ered Luin... O vento uivava entre as pedras partidas, trazendo o eco distante dos tambores sob a terra.";
  const longText = Array.from({ length: 14 }, (_, i) => `Capítulo ${i + 1}: ${paragraph}`).join("\n\n");

  const chapters = narrationMod.splitIntoChapters(longText, 900);
  assert.ok(chapters.length >= 3, "Long narrative must be split into multiple chapters");
  for (const ch of chapters) {
    const text = typeof ch === "string" ? ch : ch.text;
    assert.ok(text.length <= 900, `Chapter exceeded maxChars: ${text.length}`);
  }

  const androidSentences = chapters.flatMap((ch) => narrationMod.splitForAndroidTts(ch, 160));
  assert.ok(androidSentences.length >= 10);
  for (const s of androidSentences) {
    assert.ok(s.text.length <= 170, `Android TTS sentence exceeded 170 chars: ${s.text.length}`);
    assert.ok(!s.text.endsWith("Eria"), "Must not cut mid-word");
  }

  const chunks = narrationMod.splitNarrationIntoChunks(longText, 600);
  assert.ok(chunks.length >= 4);

  // Verify Kotlin NarrationSegmenter.kt exists and implements two-layer segmentation
  const ktSegmenter = fs.readFileSync(
    path.join(
      androidWorkRoot,
      "android/app/src/main/java/com/braseiro/oumanel/NarrationSegmenter.kt",
    ),
    "utf8",
  );
  assert.ok(ktSegmenter.includes("object NarrationSegmenter"));
  assert.ok(ktSegmenter.includes("splitIntoChapters"));
  assert.ok(ktSegmenter.includes("splitForAndroidTts"));
});

test("OUA G10-03: Companheiros — modos EU_CONTROLO, MESTRE_COM_CURADORIA (aprovar/alterar/recusar ANTES do efeito), MESTRE_CONTROLA e O QUE OS COMPANHEIROS ACHAM?", async () => {
  const { applyMasterCommands } = await importLib("master/commands.ts");
  const fixture = await createFixture();
  let campaign = fixture.campaign;
  let currentChars = fixture.heroes;
  let npcs = [];

  // 1. Default mode is EU_CONTROLO -> autonomous action by Master MUST be rejected with zero effect
  const hopeBefore = currentChars[0].sheet.attributes.hopeCurrent;
  const resEuControlo = applyMasterCommands(campaign, currentChars, npcs, [
    {
      type: "propose_companion_action",
      targetCharacterId: "hero_1",
      source: "master",
      proposedCommand: {
        type: "adjust_hope",
        targetCharacterId: "hero_1",
        amount: -3,
        reason: "Ação autônoma não autorizada",
      },
      reason: "Mestre tentando agir pelo herói em EU_CONTROLO",
    },
  ]);
  assert.equal(resEuControlo.rejected.length, 1);
  assert.match(resEuControlo.rejected[0].reason, /COMPANION_AGENCY_VIOLATION/);
  assert.equal(
    resEuControlo.characters[0].sheet.attributes.hopeCurrent,
    hopeBefore,
    "Zero effect when EU_CONTROLO blocks autonomous action",
  );

  // 2. Switch hero_2 to MESTRE_COM_CURADORIA
  let step = applyMasterCommands(campaign, currentChars, npcs, [
    {
      type: "set_companion_control_mode",
      targetCharacterId: "hero_2",
      mode: "MESTRE_COM_CURADORIA",
      reason: "Ativar curadoria para Elrohir",
    },
  ]);
  assert.equal(step.rejected.length, 0);
  campaign = step.campaign;
  currentChars = step.characters;
  assert.equal(
    campaign.masterState.company.companionControlByHero["hero_2"],
    "MESTRE_COM_CURADORIA",
  );

  // Propose action under MESTRE_COM_CURADORIA -> must queue proposal with ZERO immediate effect
  const h2HopeBefore = currentChars[1].sheet.attributes.hopeCurrent;
  step = applyMasterCommands(campaign, currentChars, npcs, [
    {
      type: "propose_companion_action",
      proposalId: "prop_reject_test",
      targetCharacterId: "hero_2",
      source: "master",
      summary: "Gastar 2 de Esperança em canção élfica",
      proposedCommand: {
        type: "adjust_hope",
        targetCharacterId: "hero_2",
        amount: -2,
        reason: "Canção élfica proposta pelo Mestre",
      },
      reason: "Proposta sob curadoria",
    },
  ]);
  assert.equal(step.rejected.length, 0);
  campaign = step.campaign;
  currentChars = step.characters;
  assert.equal(
    currentChars[1].sheet.attributes.hopeCurrent,
    h2HopeBefore,
    "Must NOT apply effect before curation approval",
  );
  assert.equal(campaign.masterState.company.pendingCompanionProposals.length, 1);
  assert.equal(
    campaign.masterState.company.pendingCompanionProposals[0].status,
    "pending",
  );

  // Reject the proposal -> status becomes rejected, still ZERO effect
  step = applyMasterCommands(campaign, currentChars, npcs, [
    {
      type: "resolve_companion_curation",
      proposalId: "prop_reject_test",
      decision: "reject",
      reason: "Jogador recusou a proposta",
    },
  ]);
  assert.equal(step.rejected.length, 0);
  campaign = step.campaign;
  currentChars = step.characters;
  assert.equal(currentChars[1].sheet.attributes.hopeCurrent, h2HopeBefore);
  assert.equal(
    campaign.masterState.company.pendingCompanionProposals[0].status,
    "rejected",
  );

  // Propose a second action and EDIT it before effect (-1 instead of -4)
  step = applyMasterCommands(campaign, currentChars, npcs, [
    {
      type: "propose_companion_action",
      proposalId: "prop_edit_test",
      targetCharacterId: "hero_2",
      source: "master",
      summary: "Gastar 4 de Esperança",
      proposedCommand: {
        type: "adjust_hope",
        targetCharacterId: "hero_2",
        amount: -4,
        reason: "Gasto excessivo proposto",
      },
      reason: "Segunda proposta sob curadoria",
    },
  ]);
  campaign = step.campaign;
  currentChars = step.characters;
  assert.equal(currentChars[1].sheet.attributes.hopeCurrent, h2HopeBefore);

  step = applyMasterCommands(campaign, currentChars, npcs, [
    {
      type: "resolve_companion_curation",
      proposalId: "prop_edit_test",
      decision: "edit",
      editedCommand: {
        type: "adjust_hope",
        targetCharacterId: "hero_2",
        amount: -1,
        reason: "Gasto ajustado pelo jogador na curadoria",
      },
      reason: "Jogador alterou a proposta antes do efeito",
    },
  ]);
  assert.equal(step.rejected.length, 0);
  campaign = step.campaign;
  currentChars = step.characters;
  assert.equal(currentChars[1].sheet.attributes.hopeCurrent, h2HopeBefore - 1);

  // Propose a third action and APPROVE it
  step = applyMasterCommands(campaign, currentChars, npcs, [
    {
      type: "propose_companion_action",
      proposalId: "prop_approve_test",
      targetCharacterId: "hero_2",
      source: "master",
      proposedCommand: {
        type: "adjust_fatigue",
        targetCharacterId: "hero_2",
        amount: 2,
        reason: "Marcha atenta aprovada",
      },
      reason: "Terceira proposta sob curadoria",
    },
    {
      type: "resolve_companion_curation",
      proposalId: "prop_approve_test",
      decision: "approve",
      reason: "Jogador aprovou a proposta",
    },
  ]);
  assert.equal(step.rejected.length, 0);
  campaign = step.campaign;
  currentChars = step.characters;
  assert.equal(currentChars[1].sheet.attributes.fatigue, 2);

  // 3. Switch hero_2 to MESTRE_CONTROLA -> executes immediately
  step = applyMasterCommands(campaign, currentChars, npcs, [
    {
      type: "set_companion_control_mode",
      targetCharacterId: "hero_2",
      mode: "MESTRE_CONTROLA",
      reason: "Mestre passa a controlar Elrohir",
    },
    {
      type: "propose_companion_action",
      targetCharacterId: "hero_2",
      source: "master",
      proposedCommand: {
        type: "adjust_fatigue",
        targetCharacterId: "hero_2",
        amount: 1,
        reason: "Ação imediata sob MESTRE_CONTROLA",
      },
      reason: "Execução autônoma autorizada",
    },
  ]);
  assert.equal(step.rejected.length, 0);
  campaign = step.campaign;
  currentChars = step.characters;
  assert.equal(currentChars[1].sheet.attributes.fatigue, 3);

  // 4. Button/command "O QUE OS COMPANHEIROS ACHAM?"
  step = applyMasterCommands(campaign, currentChars, npcs, [
    {
      type: "ask_what_companions_think",
      topic: "As ruínas da torre sobre o pântano",
      reason: "Consultar opinião dos companheiros",
    },
  ]);
  assert.equal(step.rejected.length, 0);
  campaign = step.campaign;
  const opinions = campaign.masterState.company.lastCompanionOpinions;
  assert.equal(opinions.length, 2);
  for (const op of opinions) {
    assert.equal(op.barbaraValidated, true);
    assert.ok(op.opinionText.includes(op.characterName));
    assert.ok(op.opinionText.includes("As ruínas da torre sobre o pântano"));
  }
});

test("OUA G10-04: Mundo Vivo (hora/dia/estações/clima/NPCs/organizações) e UI Galaxy A57 / S23 Ultra", async () => {
  const { applyMasterCommands } = await importLib("master/commands.ts");
  const { simulateLivingWorldDetailed } = await importLib("livingWorldAutomation.ts");
  const fixture = await createFixture();
  let campaign = fixture.campaign;
  let currentChars = fixture.heroes;
  let npcs = [
    {
      id: "npc_butterbur",
      name: "Cevado Carrapicho",
      culture: "Homens de Bri",
      role: "Estalajadeiro",
      location: "Bri",
      attitude: "Amigável",
      trust: 60,
      presentInScene: true,
      voiceProfile: "Charon",
      emotionalState: "Atarefado",
      nextAction: "Cuidar do Pônei Saltitante",
      createdAt: 1000,
      updatedAt: 1000,
    },
  ];

  // Advance time by 36 hours (2160 minutes) -> crosses day boundary & triggers living world
  const step = applyMasterCommands(campaign, currentChars, npcs, [
    {
      type: "advance_time",
      minutes: 2160,
      reason: "Passagem de um dia e meio em Bri",
    },
  ]);
  assert.equal(step.rejected.length, 0);
  assert.ok(step.campaign.masterState.worldClock.day >= 2);

  const sim = simulateLivingWorldDetailed(
    step.campaign.masterState,
    step.campaign,
    step.npcs,
    1440,
  );
  assert.ok(sim.master.livingWorld);
  assert.ok(Array.isArray(sim.master.livingWorld.organizations));

  // Verify CompanyManager.tsx has the companion modes and "O QUE OS COMPANHEIROS ACHAM?" button
  const companyMgrSrc = fs.readFileSync(
    path.join(androidWorkRoot, "components/CompanyManager.tsx"),
    "utf8",
  );
  assert.ok(companyMgrSrc.includes("EU_CONTROLO"));
  assert.ok(companyMgrSrc.includes("MESTRE_COM_CURADORIA"));
  assert.ok(companyMgrSrc.includes("MESTRE_CONTROLA"));
  assert.ok(companyMgrSrc.includes("O QUE OS COMPANHEIROS ACHAM?"));
});

test("OUA G11-01: Bundle Android (android/app/src/main/assets/public/assets) e APK 4.9.212 contêm a ponte V9 integrada, Bárbara 1.1.3 e zero segredos", () => {
  const assetsDir = path.join(
    androidWorkRoot,
    "android/app/src/main/assets/public/assets",
  );
  if (!fs.existsSync(assetsDir)) {
    return;
  }
  const jsFiles = fs
    .readdirSync(assetsDir)
    .filter((f) => f.endsWith(".js") && !f.includes("pdf.worker"));
  assert.ok(jsFiles.length >= 1, "Android public/assets must contain compiled JS bundle");

  const combined = jsFiles
    .map((f) => fs.readFileSync(path.join(assetsDir, f), "utf8"))
    .join("\n");

  // Must contain wired 4.9.212 features (not quarantined)
  assert.ok(combined.includes("4.9.212"), "Bundle must include version 4.9.212");
  assert.ok(
    combined.includes("v9_dispatch_adventure_action"),
    "Bundle must include wired V9 command handler",
  );
  assert.ok(
    combined.includes("core-star-of-the-mist"),
    "Bundle must include A Estrela da Névoa adventure",
  );
  assert.ok(
    combined.includes("tales-06-amon-guruthos"),
    "Bundle must include Contos das Terras Solitárias",
  );
  assert.ok(
    combined.includes("barbara-protocol-v2"),
    "Bundle must include Motor Bárbara 1.1.3 supervisor protocol",
  );
  assert.ok(
    combined.includes("MESTRE_COM_CURADORIA"),
    "Bundle must include Companion Curation mode",
  );
  assert.ok(
    combined.includes("O QUE OS COMPANHEIROS ACHAM?"),
    "Bundle must include O QUE OS COMPANHEIROS ACHAM? button",
  );

  // Must NOT contain private GM secrets or Node modules
  assert.ok(!combined.includes("gm-master-pin-2026"));
  assert.ok(!combined.includes("HOST_PRIVATE_KEY_998877"));
  assert.ok(!combined.includes("Tor2eGmSecretsVault"));
});

