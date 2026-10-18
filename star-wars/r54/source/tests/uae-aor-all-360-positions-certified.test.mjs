import test from 'node:test';
import assert from 'node:assert/strict';
import { createCampaign, validateState } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { BraseiroUiBridge } from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import { AOR_TALENT_TREES } from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import { AOR_CAREERS } from '../app/src/main/assets/vtt/js/rules/characterCreation.mjs';
import { SKILLS } from '../app/src/main/assets/vtt/js/rules/character.mjs';
import { aorCanonicalTalentName, AOR_RUNTIME_MECHANICAL_TALENTS } from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import { sourceBackedTalentRank } from '../app/src/main/assets/vtt/js/rules/talents.mjs';

const allSkillKeys = Object.keys(SKILLS);

class TestRepo {
  constructor(name = 'AoR Certification Campaign') {
    this.state = createCampaign(name);
    this.saves = 0;
  }
  load() {
    return structuredClone(this.state);
  }
  save(value) {
    this.state = structuredClone(value);
    this.saves++;
  }
}

function createSpecController(careerKey, specKey) {
  const repo = new TestRepo(`AoR Core ${careerKey}:${specKey}`);
  const gc = new GameController(repo);
  gc.setCampaignMode('AOR');

  const careerDef = AOR_CAREERS[careerKey];
  const specDef = careerDef.specializations[specKey];
  const careerSkills = new Set([...careerDef.skills, ...specDef.skills]);
  const nonCareer = allSkillKeys.filter(k => !careerSkills.has(k)).slice(0, 4);

  gc.createAorCharacter({
    name: `Agente ${specKey}`,
    speciesKey: 'human',
    careerKey,
    specializationKey: specKey,
    partySize: 1,
    careerFreeSkills: careerDef.skills.slice(0, 4),
    specializationFreeSkills: specDef.skills.slice(0, 2),
    speciesChoiceSkills: nonCareer.slice(0, 2),
    motivation: 'Libertação galáctica e apoio à Rebelião',
    dutyType: 'Vitória em combate'
  });

  gc.state.player.character.xp.available = 10000;
  repo.save(gc.state);
  return { repo, gc, nonCareer };
}

for (const [careerKey, careerDef] of Object.entries(AOR_CAREERS)) {
  for (const [specKey, specDef] of Object.entries(careerDef.specializations)) {
    test(`AoR Core ${careerKey}:${specKey} — certifica individualmente todos os 20 nós com efeito e 3 reloads`, () => {
      const { repo, gc, nonCareer } = createSpecController(careerKey, specKey);
      const bridge = new BraseiroUiBridge({ repository: repo });
      const tree = AOR_TALENT_TREES[careerKey][specKey];

      assert.ok(tree, `Árvore ${careerKey}:${specKey} deve existir`);
      assert.equal(tree.nodes.length, 20, `Árvore ${careerKey}:${specKey} deve ter exatamente 20 nós`);

      const remaining = new Map(tree.nodes.map(n => [n.id, n]));
      let purchasedCount = 0;
      let progress = true;

      while (progress && remaining.size > 0) {
        progress = false;
        for (const [nodeId, node] of [...remaining.entries()]) {
          const isRow1 = node.row === 1;
          const currentChar = repo.load().player.character;
          const purchasedIds = new Set((currentChar.talentPurchases || []).map(p => p.nodeId));
          const hasNeighborPurchased = tree.edges.some(([a, b]) => {
            if (a === nodeId && purchasedIds.has(b)) return true;
            if (b === nodeId && purchasedIds.has(a)) return true;
            return false;
          });

          if (isRow1 || hasNeighborPurchased) {
            const canonical = aorCanonicalTalentName(node.originalName);
            const opts = {};
            if (['DEDICACIÓN', 'DEDI'].includes(canonical) || ['DEDICACIÓN', 'DEDI'].includes(node.originalName)) {
              opts.characteristicKey = 'agilidad';
            }
            if (canonical === 'DEADACC') opts.skillKey = 'armas_distancia_pesadas';
            if (canonical === 'KNOWSPEC') opts.skillKey = 'educacion';
            if (canonical === 'SMOOTHTALK') opts.skillKey = 'negociacion';
            if (canonical === 'WELLROUND') opts.skillKeys = nonCareer.slice(2, 4);

            const xpBefore = currentChar.xp.available;
            const strainBefore = currentChar.derived.strain.threshold;
            const woundsBefore = currentChar.derived.wounds.threshold;
            const soakBefore = currentChar.derived.soak;
            const agilBefore = currentChar.characteristics.agilidad;

            const buyRes = bridge.buyTalent(nodeId, opts);
            assert.equal(buyRes.ok, true, `Compra do nó ${nodeId} deve ter sucesso: ${buyRes.error}`);

            const char = repo.load().player.character;
            assert.equal(char.xp.available, xpBefore - node.cost, `XP deve ser descontado em ${node.cost}`);

            const lastPurchase = char.talentPurchases.at(-1);
            assert.equal(lastPurchase.nodeId, nodeId);
            assert.equal(lastPurchase.cost, node.cost);
            assert.equal(lastPurchase.source.book, 'Star Wars - Age of Rebellion - Core Rulebook');
            assert.equal(lastPurchase.source.printedPage, node.source.printedPage);

            // Verify specific mechanical immediate effect
            if (['APLOMO', 'GRIT'].includes(canonical) || ['APLOMO', 'GRIT'].includes(node.originalName)) {
              assert.equal(char.derived.strain.threshold, strainBefore + 1, `Grit deve elevar strain threshold em +1`);
            }
            if (['ENDURECIDO', 'TOUGH'].includes(canonical) || ['ENDURECIDO', 'TOUGH'].includes(node.originalName)) {
              assert.equal(char.derived.wounds.threshold, woundsBefore + 2, `Toughened deve elevar wounds threshold em +2`);
            }
            if (['RESISTENTE', 'ENDUR'].includes(canonical) || ['RESISTENTE', 'ENDUR'].includes(node.originalName)) {
              assert.equal(char.derived.soak, soakBefore + 1, `Enduring deve elevar soak em +1`);
            }
            if (['DEDICACIÓN', 'DEDI'].includes(canonical) || ['DEDICACIÓN', 'DEDI'].includes(node.originalName)) {
              assert.equal(char.characteristics.agilidad, agilBefore + 1, `Dedicação deve elevar característica em +1`);
            }

            // Verify rank tracking
            const rank = sourceBackedTalentRank(char, canonical);
            assert.ok(rank >= 1, `Talento ${canonical} deve ter rank >= 1 após compra`);

            // 3 Independent Reloads / Reopens
            for (let r = 1; r <= 3; r++) {
              const reloaded = new GameController(repo);
              validateState(reloaded.state);
              const rc = reloaded.state.player.character;
              assert.equal(rc.xp.available, char.xp.available, `Reload ${r}: XP disponível deve persistir`);
              assert.equal(rc.derived.strain.threshold, char.derived.strain.threshold, `Reload ${r}: Strain deve persistir`);
              assert.equal(rc.derived.wounds.threshold, char.derived.wounds.threshold, `Reload ${r}: Wounds deve persistir`);
              assert.equal(rc.derived.soak, char.derived.soak, `Reload ${r}: Soak deve persistir`);
              assert.equal(rc.characteristics.agilidad, char.characteristics.agilidad, `Reload ${r}: Agilidade deve persistir`);
              assert.equal(rc.talentPurchases.length, char.talentPurchases.length, `Reload ${r}: Compras devem persistir`);
            }

            remaining.delete(nodeId);
            purchasedCount++;
            progress = true;
          }
        }
      }

      assert.equal(purchasedCount, 20, `Todos os 20 nós de ${careerKey}:${specKey} devem ser comprados e certificados`);
    });
  }
}
