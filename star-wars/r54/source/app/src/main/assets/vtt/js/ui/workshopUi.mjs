// Star Wars: Edge Studio / Fantasy Flight Games
// Interactive UI Controller for Crafting, Kyber Lightsabers, Squads, Beast Riding & Signature Abilities.

export function initWorkshopUi(game, $, render) {
  const craftCategory = $('craftCategory');
  const craftTemplate = $('craftTemplate');
  const craftDetails = $('craftTemplateDetails');
  const craftFeedback = $('craftFeedback');
  const craftBtn = $('craftExecuteBtn');

  // 1. Populate Crafting Templates
  function refreshCraftingTemplates() {
    if (!craftCategory || !craftTemplate) return;
    const cat = craftCategory.value || 'weapon';
    const catalog = game.craftingCatalog();
    const templates = catalog.templates[cat === 'weapon' ? 'weapons' : cat === 'armor' ? 'armor' : 'droids'] || {};

    craftTemplate.replaceChildren();
    for (const [key, tpl] of Object.entries(templates)) {
      const opt = document.createElement('option');
      opt.value = key;
      opt.textContent = `${tpl.name} (${tpl.creditCost} cr · ${tpl.timeHours}h)`;
      craftTemplate.append(opt);
    }
    updateCraftDetails();
  }

  function updateCraftDetails() {
    if (!craftCategory || !craftTemplate || !craftDetails) return;
    const cat = craftCategory.value || 'weapon';
    const key = craftTemplate.value;
    const catalog = game.craftingCatalog();
    const templates = catalog.templates[cat === 'weapon' ? 'weapons' : cat === 'armor' ? 'armor' : 'droids'] || {};
    const tpl = templates[key];

    if (!tpl) {
      craftDetails.textContent = 'Selecione um projeto de manufatura.';
      return;
    }

    if (cat === 'weapon') {
      craftDetails.textContent = `Dano base: ${tpl.baseDamage} | Crítico: ${tpl.baseCritical} | Alcance: ${tpl.range} | Dificuldade Mecânica: ${tpl.difficulty} | Encaixes: ${tpl.baseHardPoints}`;
    } else if (cat === 'armor') {
      craftDetails.textContent = `Proteção: +${tpl.baseSoak} | Defesa C/D: ${tpl.baseDefenseMelee}/${tpl.baseDefenseRanged} | Carga: ${tpl.baseEncumbrance} | Encaixes: ${tpl.baseHardPoints}`;
    } else {
      craftDetails.textContent = `Ferimentos: ${tpl.woundsThreshold} | Proteção: ${tpl.soak} | Tensão: ${tpl.strainThreshold} | Droide de trabalho/combate`;
    }
  }

  if (craftCategory) craftCategory.addEventListener('change', refreshCraftingTemplates);
  if (craftTemplate) craftTemplate.addEventListener('change', updateCraftDetails);

  if (craftBtn) {
    craftBtn.addEventListener('click', () => {
      try {
        const cat = craftCategory.value;
        const key = craftTemplate.value;
        const advBonus = $('craftAdvantageOption')?.checked;
        const triumphBonus = $('craftTriumphOption')?.checked;

        const rollResult = {
          successes: 2,
          advantages: advBonus ? 3 : 1,
          triumphs: triumphBonus ? 1 : 0
        };

        const result = game.craftItem({
          templateType: cat,
          templateId: key,
          rollResult,
          spentAdvantage: advBonus ? ['damage_1'] : []
        });

        if (craftFeedback) {
          craftFeedback.textContent = result.message || 'Item fabricado com sucesso!';
          craftFeedback.className = 'feedback success';
        }
        if (typeof render === 'function') render();
      } catch (err) {
        if (craftFeedback) {
          craftFeedback.textContent = err.message;
          craftFeedback.className = 'feedback fail';
        }
      }
    });
  }

  // 2. Lightsabers & Kyber Crystals
  const saberChassis = $('saberChassis');
  const saberCrystal = $('saberCrystal');
  const saberBtn = $('saberAssembleBtn');
  const saberFeedback = $('saberFeedback');

  function initLightsaberOptions() {
    if (!saberChassis || !saberCrystal) return;
    const cat = game.fndLightsaberCatalog();

    saberChassis.replaceChildren();
    for (const [key, item] of Object.entries(cat.chassis)) {
      const opt = document.createElement('option');
      opt.value = key;
      opt.textContent = `${item.name} (${item.type})`;
      saberChassis.append(opt);
    }

    saberCrystal.replaceChildren();
    for (const [key, item] of Object.entries(cat.crystals)) {
      const opt = document.createElement('option');
      opt.value = key;
      opt.textContent = `${item.name} (Dano +${item.baseDamageModifier}, Crítico ${item.baseCritical})`;
      saberCrystal.append(opt);
    }
  }

  if (saberBtn) {
    saberBtn.addEventListener('click', () => {
      try {
        const chassisId = saberChassis.value;
        const crystalId = saberCrystal.value;
        const weapon = game.createLightsaberWeapon(chassisId, { crystalId });

        const char = game.state.player?.character;
        if (char) {
          char.weapons = Array.isArray(char.weapons) ? char.weapons : [];
          char.weapons.push(weapon);
          game.setCharacter(char);
        }

        if (saberFeedback) {
          saberFeedback.textContent = `Sabre de Luz forjado com sucesso: ${weapon.name}! Adicionado ao inventário.`;
          saberFeedback.className = 'feedback success';
        }
        if (typeof render === 'function') render();
      } catch (err) {
        if (saberFeedback) {
          saberFeedback.textContent = err.message;
          saberFeedback.className = 'feedback fail';
        }
      }
    });
  }

  // 3. Beast Riding
  const beastSelect = $('beastSelect');
  const beastTameBtn = $('beastTameBtn');
  const beastMountBtn = $('beastMountBtn');
  const beastChargeBtn = $('beastChargeBtn');
  const beastFeedback = $('beastFeedback');

  function initBeastOptions() {
    if (!beastSelect) return;
    const mounts = game.mountsCatalog();
    beastSelect.replaceChildren();
    for (const [key, mount] of Object.entries(mounts)) {
      const opt = document.createElement('option');
      opt.value = key;
      opt.textContent = `${mount.name} (Silhueta ${mount.silhouette} · Vel ${mount.speed})`;
      beastSelect.append(opt);
    }
  }

  if (beastTameBtn) {
    beastTameBtn.addEventListener('click', () => {
      try {
        const mountKey = beastSelect.value;
        const res = game.tameBeast(mountKey, { successes: 2, advantages: 2, triumphs: 1 });
        if (beastFeedback) {
          beastFeedback.textContent = res.message;
          beastFeedback.className = res.tamed ? 'feedback success' : 'feedback fail';
        }
        if (typeof render === 'function') render();
      } catch (err) {
        if (beastFeedback) {
          beastFeedback.textContent = err.message;
          beastFeedback.className = 'feedback fail';
        }
      }
    });
  }

  if (beastMountBtn) {
    beastMountBtn.addEventListener('click', () => {
      try {
        const char = game.state.player?.character;
        const mount = (char?.mounts || [])[0];
        if (!mount) throw new Error('Domestique uma montaria primeiro.');

        if (!mount.mounted) {
          game.mountBeast(mount.id);
          beastMountBtn.textContent = 'Desmontar Fera';
          if (beastFeedback) beastFeedback.textContent = `Montado em ${mount.name}. Pronto para cavalgar e investir!`;
        } else {
          game.dismountBeast(mount.id);
          beastMountBtn.textContent = 'Montar Fera';
          if (beastFeedback) beastFeedback.textContent = `Desmontou de ${mount.name}.`;
        }
        if (typeof render === 'function') render();
      } catch (err) {
        if (beastFeedback) {
          beastFeedback.textContent = err.message;
          beastFeedback.className = 'feedback fail';
        }
      }
    });
  }

  if (beastChargeBtn) {
    beastChargeBtn.addEventListener('click', () => {
      try {
        const char = game.state.player?.character;
        const mount = (char?.mounts || []).find(m => m.mounted);
        if (!mount) throw new Error('É necessário estar montado para investir.');
        const res = game.executeMountedCharge(mount.id, 6);
        if (beastFeedback) {
          beastFeedback.textContent = res.description;
          beastFeedback.className = 'feedback success';
        }
      } catch (err) {
        if (beastFeedback) {
          beastFeedback.textContent = err.message;
          beastFeedback.className = 'feedback fail';
        }
      }
    });
  }

  // 4. Squads & Mass Combat
  const squadCreateBtn = $('squadCreateBtn');
  const squadAbsorbBtn = $('squadAbsorbBtn');
  const squadFeedback = $('squadFeedback');
  const massCombatBtn = $('massCombatBtn');
  const massCombatFeedback = $('massCombatFeedback');

  let activeSquad = null;

  if (squadCreateBtn) {
    squadCreateBtn.addEventListener('click', () => {
      try {
        activeSquad = game.createSquad({ minionProfileName: 'Tropa de Choque Rebelde', minionCount: 4, formation: 'skirmish' });
        if (squadFeedback) {
          squadFeedback.textContent = `Esquadrão ativado: 4 subordinados em formação de tiroteio escoltando o líder.`;
          squadFeedback.className = 'feedback success';
        }
      } catch (err) {
        if (squadFeedback) {
          squadFeedback.textContent = err.message;
          squadFeedback.className = 'feedback fail';
        }
      }
    });
  }

  if (squadAbsorbBtn) {
    squadAbsorbBtn.addEventListener('click', () => {
      try {
        if (!activeSquad || activeSquad.minionCount <= 0) throw new Error('Nenhum esquadrão ativo com subordinados.');
        const res = game.redirectSquadDamage(activeSquad, 8);
        activeSquad = res.squad;
        if (squadFeedback) {
          squadFeedback.textContent = `${res.message} (Subordinados restantes: ${res.remainingMinions}).`;
          squadFeedback.className = 'feedback';
        }
      } catch (err) {
        if (squadFeedback) {
          squadFeedback.textContent = err.message;
          squadFeedback.className = 'feedback fail';
        }
      }
    });
  }

  if (massCombatBtn) {
    massCombatBtn.addEventListener('click', () => {
      try {
        const friendlyTier = Number($('massFriendlyTier')?.value || 2);
        const enemyTier = Number($('massEnemyTier')?.value || 2);
        const res = game.resolveMassCombatRound({
          friendlyForceTier: friendlyTier,
          enemyForceTier: enemyTier,
          commanderLeadershipRank: 3,
          rollResult: { successes: 2, advantages: 2, triumphs: 1 }
        });
        if (massCombatFeedback) {
          massCombatFeedback.textContent = `${res.tacticalOutcome} (Baixas Inimigas: ${res.enemyCasualtiesPercent}% | Baixas Aliadas: ${res.friendlyCasualtiesPercent}%).`;
          massCombatFeedback.className = 'feedback success';
        }
      } catch (err) {
        if (massCombatFeedback) {
          massCombatFeedback.textContent = err.message;
          massCombatFeedback.className = 'feedback fail';
        }
      }
    });
  }

  // 5. Signature Abilities & Expanded Force Powers
  const sigSelect = $('sigSelect');
  const sigBuyBtn = $('sigBuyBtn');
  const sigActivateBtn = $('sigActivateBtn');
  const sigFeedback = $('sigFeedback');

  function initSignatureOptions() {
    if (!sigSelect) return;
    const cat = game.signatureAbilitiesCatalog();
    sigSelect.replaceChildren();
    for (const [key, item] of Object.entries(cat)) {
      const opt = document.createElement('option');
      opt.value = key;
      opt.textContent = `${item.name} [${item.careerKey}] (${item.baseCost} PE)`;
      sigSelect.append(opt);
    }
  }

  if (sigBuyBtn) {
    sigBuyBtn.addEventListener('click', () => {
      try {
        const key = sigSelect.value;
        const res = game.purchaseSignatureAbility(key);
        if (sigFeedback) {
          sigFeedback.textContent = `Habilidade de Assinatura adquirida: ${res.ability.name}! (30 PE gastos).`;
          sigFeedback.className = 'feedback success';
        }
        if (typeof render === 'function') render();
      } catch (err) {
        if (sigFeedback) {
          sigFeedback.textContent = err.message;
          sigFeedback.className = 'feedback fail';
        }
      }
    });
  }

  if (sigActivateBtn) {
    sigActivateBtn.addEventListener('click', () => {
      try {
        const key = sigSelect.value;
        const res = game.activateSignatureAbility(key);
        if (sigFeedback) {
          sigFeedback.textContent = `Habilidade ativada na sessão: ${res.abilityName}! Ponto de Destino Luminoso invertido para Sombrio.`;
          sigFeedback.className = 'feedback success';
        }
        if (typeof render === 'function') render();
      } catch (err) {
        if (sigFeedback) {
          sigFeedback.textContent = err.message;
          sigFeedback.className = 'feedback fail';
        }
      }
    });
  }

  // Initialize catalogs
  refreshCraftingTemplates();
  initLightsaberOptions();
  initBeastOptions();
  initSignatureOptions();
}
