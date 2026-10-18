import test from 'node:test';
import assert from 'node:assert/strict';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { BraseiroUiBridge } from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import { FND_TALENT_TREES } from '../app/src/main/assets/vtt/js/expansion/fndTalentTreesData.mjs';
import { FND_CAREERS } from '../app/src/main/assets/vtt/js/rules/fndCharacterCreation.mjs';
import { sourceBackedTalentRank } from '../app/src/main/assets/vtt/js/rules/talents.mjs';

class TestRepo {
  constructor(name = 'F&D Certification Campaign') {
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

function createFndSpecController(careerKey, specKey) {
  const repo = new TestRepo(`F&D Core ${careerKey}:${specKey}`);
  const gc = new GameController(repo);

  gc.createFndCharacter({
    name: `Cavaleiro ${specKey}`,
    careerKey,
    specializationKey: specKey,
    moralityScore: 50,
    startingXp: 10000
  });

  repo.save(gc.state);
  return { repo, gc };
}

for (const [careerKey, careerDef] of Object.entries(FND_CAREERS)) {
  for (const [specKey, specDef] of Object.entries(careerDef.specializations)) {
    test(`F&D Core ${careerKey}:${specKey} — certifica individualmente todos os 20 nós com efeito e 3 reloads`, () => {
      const { repo, gc } = createFndSpecController(careerKey, specKey);
      const bridge = new BraseiroUiBridge({ repository: repo });
      const tree = FND_TALENT_TREES[careerKey][specKey];

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
            const canonical = node.originalName;
            const opts = {};
            if (['DEDICACIÓN', 'DEDI'].includes(canonical)) {
              opts.characteristicKey = 'voluntad';
            }
            if (canonical === 'DEADACC') opts.skillKey = 'armas_distancia_pesadas';
            if (canonical === 'KNOWSPEC') opts.skillKey = 'saber_arcaico';
            if (canonical === 'SMOOTHTALK') opts.skillKey = 'negociacion';
            if (canonical === 'WELLROUND') opts.skillKeys = ['medicina', 'sigilo'];

            const xpBefore = currentChar.xp.available;
            const strainBefore = currentChar.derived.strain.threshold;
            const woundsBefore = currentChar.derived.wounds.threshold;
            const soakBefore = currentChar.derived.soak;
            const voluntBefore = currentChar.characteristics.voluntad;
            const forceRatingBefore = currentChar.force?.rating ?? 1;

            const buyRes = bridge.buyTalent(nodeId, opts);
            assert.equal(buyRes.ok, true, `Compra do nó ${nodeId} deve ter sucesso: ${buyRes.error}`);

            const char = repo.load().player.character;
            assert.equal(char.xp.available, xpBefore - node.cost, `XP deve ser descontado em ${node.cost}`);

            const lastPurchase = char.talentPurchases.at(-1);
            assert.equal(lastPurchase.nodeId, nodeId);
            assert.equal(lastPurchase.cost, node.cost);
            assert.equal(lastPurchase.source.book, 'Star Wars: Force and Destiny Core Rulebook');

            // Verify specific mechanical immediate effect
            if (['APLOMO', 'GRIT'].includes(canonical)) {
              assert.equal(char.derived.strain.threshold, strainBefore + 1, `Grit deve elevar strain threshold em +1`);
            }
            if (['ENDURECIDO', 'TOUGH'].includes(canonical)) {
              assert.equal(char.derived.wounds.threshold, woundsBefore + 2, `Toughened deve elevar wounds threshold em +2`);
            }
            if (['RESISTENTE', 'ENDUR'].includes(canonical)) {
              assert.equal(char.derived.soak, soakBefore + 1, `Enduring deve elevar soak em +1`);
            }
            if (canonical === 'FORCE_RATING') {
              assert.equal(char.force.rating, forceRatingBefore + 1, `Force Rating deve aumentar em +1`);
            }
            if (['DEDICACIÓN', 'DEDI'].includes(canonical)) {
              assert.equal(char.characteristics.voluntad, voluntBefore + 1, `Dedicação deve elevar característica em +1`);
            }

            // Verify rank tracking
            const rank = sourceBackedTalentRank(char, canonical);
            assert.ok(rank >= 1, `Talento ${canonical} deve ter rank >= 1 após compra`);

            // 3 Independent Reloads / Reopens
            for (let r = 1; r <= 3; r++) {
              const reloaded = new GameController(repo);
              assert.equal(reloaded.state.player.character.talentPurchases.length, purchasedCount + 1);
              assert.equal(reloaded.state.player.character.xp.available, xpBefore - node.cost);
            }

            purchasedCount++;
            remaining.delete(nodeId);
            progress = true;
          }
        }
      }

      assert.equal(purchasedCount, 20, `Todos os 20 nós de ${careerKey}:${specKey} devem ser comprados e certificados`);
      assert.equal(remaining.size, 0, `Nenhum nó de ${careerKey}:${specKey} deve ficar pendente`);
    });
  }
}
