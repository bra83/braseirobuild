/* R54 Interactive Tactical Holotable — Star Wars In-Universe Holonet Terminal */
(()=>{
  'use strict';
  if (window.__R54_HOLOTABLE_INTERACTIVE__) return;
  window.__R54_HOLOTABLE_INTERACTIVE__ = true;

  const biomes = [
    { key: 'cantina_outpost', label: 'CANTINA' },
    { key: 'hangar_station', label: 'HANGAR' },
    { key: 'urban_sublevel', label: 'BECO URBANO' },
    { key: 'desert_canyon', label: 'DESERTO' },
    { key: 'imperial_base', label: 'BASE IMPERIAL' },
    { key: 'jungle_wilderness', label: 'SELVA' }
  ];

  function game() {
    return window.BraseiroGameBridge || null;
  }

  function openHolotable() {
    document.querySelector('.r54-holotable-modal')?.remove();
    const g = game();
    if (!g || typeof g.getHolotableState !== 'function') {
      alert('Terminal Tático Holonet indisponível no momento.');
      return;
    }

    const res = g.getHolotableState();
    if (!res || !res.ok) {
      alert('Falha ao inicializar projeção holográfica.');
      return;
    }

    let hData = res.data;
    const modal = document.createElement('div');
    modal.className = 'r54-holotable-modal';

    function renderModalContent() {
      const state = hData.state;
      const targetId = state.selectedTargetId;
      const target = state.adversaryTokens?.find(t => t.id === targetId) || state.adversaryTokens?.[0];

      modal.innerHTML = `
        <section role="dialog" aria-modal="true" aria-label="Terminal Tático Holonet">
          <header class="r54-holo-header">
            <div>
              <small>PROJEÇÃO TÁTICA HOLONET · ${state.biomeName.toUpperCase()}</small>
              <h2>Terminal Tático de Combate</h2>
            </div>
            <button type="button" class="r54-holo-close" aria-label="Fechar Terminal">×</button>
          </header>

          <nav class="r54-holo-biomes">
            ${biomes.map(b => `
              <button type="button" data-holo-biome="${b.key}" class="${state.biomeKey === b.key ? 'active' : ''}">
                ${b.label}
              </button>
            `).join('')}
          </nav>

          <div class="r54-holo-display">
            <div class="r54-holo-svg-wrapper">
              ${hData.svg}
            </div>
          </div>

          <div class="r54-holo-hud">
            <div class="r54-holo-hud-line">
              <strong>STATUS TÁTICO:</strong>
              <span class="r54-holo-cover-chip ${state.playerToken.cover !== 'none' ? 'in-cover' : ''}">
                🛡️ Cobertura: ${state.playerToken.cover === 'heavy' ? 'Pesada (+2 Setbacks)' : (state.playerToken.cover === 'light' ? 'Leve (+1 Setback)' : 'Nenhuma (Exposto)')}
              </span>
            </div>
            <div class="r54-holo-target-line">
              <small>ALVO TRAVADO:</small>
              <span>${target ? `${target.name} [Alcance: ${target.rangeBand.toUpperCase()}${target.cover !== 'none' ? ` · Cobertura ${target.cover}` : ''}]` : 'Nenhum'}</span>
            </div>
            <div class="r54-holo-actions">
              <button type="button" class="r54-holo-btn r54-holo-btn-move">AVANÇAR PARA COBERTURA</button>
              <button type="button" class="r54-holo-btn r54-holo-btn-disengage">RECUAR / DESENGAJAR</button>
              <button type="button" class="r54-holo-btn r54-holo-btn-attack primary">DISPARO TÁTICO</button>
            </div>
            <output class="r54-holo-output"></output>
          </div>
        </section>
      `;

      bindEvents();
    }

    function bindEvents() {
      modal.querySelector('.r54-holo-close').onclick = () => modal.remove();
      modal.onclick = (e) => { if (e.target === modal) modal.remove(); };

      // Biome Switch
      modal.querySelectorAll('[data-holo-biome]').forEach(btn => {
        btn.onclick = () => {
          const key = btn.dataset.holoBiome;
          const r = g.setHolotableBiome(key);
          if (r && r.ok) {
            hData = r.data;
            renderModalContent();
          }
        };
      });

      // Target selection on token tap
      modal.querySelectorAll('.adversary-token').forEach(tokenEl => {
        tokenEl.onclick = (e) => {
          e.stopPropagation();
          const tid = tokenEl.dataset.tokenId;
          hData.state.selectedTargetId = tid;
          renderModalContent();
        };
      });

      // Tap on SVG to move player token
      const svg = modal.querySelector('.holotable-svg');
      if (svg) {
        svg.onclick = (e) => {
          const rect = svg.getBoundingClientRect();
          const clickX = ((e.clientX - rect.left) / rect.width) * 100;
          const clickY = ((e.clientY - rect.top) / rect.height) * 100;
          const moveRes = g.moveHolotableToken('player_hero', clickX, clickY);
          if (moveRes && moveRes.ok) {
            hData = moveRes.data;
            renderModalContent();
          }
        };
      }

      // Move into cover button
      const moveCoverBtn = modal.querySelector('.r54-holo-btn-move');
      if (moveCoverBtn) {
        moveCoverBtn.onclick = () => {
          // Move towards cover coordinates in current biome
          const key = hData.state.biomeKey;
          let targetCoords = { x: 28, y: 35 };
          if (key === 'cantina_outpost') targetCoords = { x: 22, y: 62 };
          if (key === 'urban_sublevel') targetCoords = { x: 22, y: 44 };
          if (key === 'desert_canyon') targetCoords = { x: 18, y: 45 };
          if (key === 'imperial_base') targetCoords = { x: 20, y: 50 };
          if (key === 'jungle_wilderness') targetCoords = { x: 30, y: 35 };

          const moveRes = g.moveHolotableToken('player_hero', targetCoords.x, targetCoords.y);
          if (moveRes && moveRes.ok) {
            hData = moveRes.data;
            renderModalContent();
            modal.querySelector('.r54-holo-output').textContent = 'Herói avançou para trás de cobertura com sucesso.';
          }
        };
      }

      // Disengage button
      const disengageBtn = modal.querySelector('.r54-holo-btn-disengage');
      if (disengageBtn) {
        disengageBtn.onclick = () => {
          const moveRes = g.moveHolotableToken('player_hero', 50, 85);
          if (moveRes && moveRes.ok) {
            hData = moveRes.data;
            renderModalContent();
            modal.querySelector('.r54-holo-output').textContent = 'Herói recuou para distância segura.';
          }
        };
      }

      // Attack check
      const attackBtn = modal.querySelector('.r54-holo-btn-attack');
      if (attackBtn) {
        attackBtn.onclick = () => {
          const tid = hData.state.selectedTargetId || hData.state.adversaryTokens?.[0]?.id;
          if (!tid) return;
          const checkRes = g.calculateTacticalCheck(tid);
          if (checkRes && checkRes.ok) {
            const c = checkRes.data;
            const out = modal.querySelector('.r54-holo-output');
            out.innerHTML = `Disparo contra <strong>${c.targetName}</strong> [Alcance: ${c.rangeBand.toUpperCase()} · Dificuldade: ${c.difficulty}] · Penalidade de Cobertura: <strong>+${c.setbackDice} Dado(s) de Complicação</strong>.`;
          }
        };
      }
    }

    renderModalContent();
    document.body.appendChild(modal);
  }

  window.__R54OpenTacticalHolotable = openHolotable;

  function injectHolotableButton() {
    document.querySelectorAll('dialog.overlay[open] .bottom-sheet, .overlay .bottom-sheet').forEach(sheet => {
      const title = String(sheet.querySelector('header h2')?.textContent || '').toLowerCase();
      if (!title.includes('utilidades') && !title.includes('combate')) return;
      if (!sheet.querySelector('[data-p31-utility="holotable"]')) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'list-item r54-p31-utility';
        btn.dataset.p31Utility = 'holotable';
        btn.innerHTML = `
          <span class="r54-p31-util-icon" style="color:#3bd6f8;border-color:#3bd6f8;box-shadow:0 0 8px rgba(59,214,248,0.3);">⬡</span>
          <span class="list-copy">
            <strong style="color:#3bd6f8;">TERMINAL TÁTICO HOLONET</strong>
            <small style="color:#a5d8ea;">Mesa holográfica de combate e coberturas</small>
          </span>
          <span class="list-arrow" aria-hidden="true" style="color:#3bd6f8;">›</span>
        `;
        btn.onclick = (e) => {
          e.preventDefault();
          e.stopPropagation();
          openHolotable();
        };
        sheet.appendChild(btn);
      }
    });

    // Also look for combat view header to add a quick Holotable pill
    const combatHeader = document.querySelector('.combat-head, #combatData .scene-kicker, .r54-p1-combat');
    if (combatHeader && !document.querySelector('.r54-holo-quick-pill')) {
      const pill = document.createElement('button');
      pill.type = 'button';
      pill.className = 'r54-holo-quick-pill';
      pill.innerHTML = '⬡ ABRIR TERMINAL TÁTICO HOLONET';
      pill.onclick = openHolotable;
      combatHeader.insertAdjacentElement('afterend', pill);
    }
  }

  function injectStyles() {
    if (document.getElementById('r54-holotable-interactive-style')) return;
    const style = document.createElement('style');
    style.id = 'r54-holotable-interactive-style';
    style.textContent = `
      .r54-holotable-modal {
        position: fixed; inset: 0; z-index: 2147483400;
        background: rgba(3, 7, 12, 0.92);
        display: flex; align-items: flex-end; justify-content: center;
        backdrop-filter: blur(8px);
      }
      .r54-holotable-modal > section {
        box-sizing: border-box; width: min(100%, 720px); max-height: 94vh;
        overflow-y: auto; background: #050a0f; color: #dff4fc;
        border: 1px solid #3bd6f8; border-radius: 18px 18px 0 0;
        padding: 14px 14px calc(18px + env(safe-area-inset-bottom, 0px));
        box-shadow: 0 -10px 40px rgba(59, 214, 248, 0.25);
      }
      .r54-holo-header {
        display: flex; align-items: center; justify-content: space-between;
        padding-bottom: 10px; border-bottom: 1px solid rgba(59, 214, 248, 0.3);
      }
      .r54-holo-header small {
        color: #3bd6f8; font-weight: 900; letter-spacing: 0.1em; font-size: 11px;
      }
      .r54-holo-header h2 {
        margin: 2px 0 0; font-size: 1.35rem; color: #fff; text-shadow: 0 0 10px rgba(59, 214, 248, 0.5);
      }
      .r54-holo-close {
        width: 44px; height: 44px; border-radius: 50%;
        border: 1px solid #3bd6f8; background: #0c1a24;
        color: #3bd6f8; font-size: 24px; font-weight: 900;
      }
      .r54-holo-biomes {
        display: flex; gap: 6px; overflow-x: auto; padding: 10px 0;
        scrollbar-width: none;
      }
      .r54-holo-biomes button {
        flex: 0 0 auto; min-height: 38px; padding: 0 12px;
        border: 1px solid rgba(59, 214, 248, 0.4); border-radius: 8px;
        background: #09151e; color: #9edaf0; font-weight: 800; font-size: 11px;
      }
      .r54-holo-biomes button.active {
        border-color: #3bd6f8; background: #13394d; color: #fff;
        box-shadow: 0 0 12px rgba(59, 214, 248, 0.4);
      }
      .r54-holo-display {
        margin: 6px 0 12px; border-radius: 12px; overflow: hidden;
        border: 1px solid #23849c; background: #03070b;
      }
      .r54-holo-hud {
        display: grid; gap: 8px; padding: 12px;
        background: #081219; border: 1px solid rgba(59, 214, 248, 0.35);
        border-radius: 10px;
      }
      .r54-holo-hud-line {
        display: flex; align-items: center; justify-content: space-between;
      }
      .r54-holo-hud-line strong {
        color: #f3a938; font-size: 12px; letter-spacing: 0.08em;
      }
      .r54-holo-cover-chip {
        font-size: 11px; font-weight: 800; color: #84a5b4;
        padding: 3px 8px; border-radius: 99px; background: #0d1e29;
      }
      .r54-holo-cover-chip.in-cover {
        color: #3bd6f8; background: rgba(59, 214, 248, 0.18);
        border: 1px solid #3bd6f8;
      }
      .r54-holo-target-line {
        display: flex; gap: 8px; font-size: 12px; align-items: center;
      }
      .r54-holo-target-line small {
        color: #ff6666; font-weight: 900;
      }
      .r54-holo-target-line span {
        color: #fff; font-weight: 700;
      }
      .r54-holo-actions {
        display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px;
      }
      .r54-holo-btn {
        min-height: 42px; padding: 4px 6px; border: 1px solid #3bd6f8;
        border-radius: 7px; background: #0e2432; color: #9edaf0;
        font-weight: 900; font-size: 10px; text-transform: uppercase;
      }
      .r54-holo-btn.primary {
        background: #5c2f16; border-color: #f3a938; color: #fff;
        box-shadow: 0 0 10px rgba(243, 169, 56, 0.3);
      }
      .r54-holo-output {
        display: block; font-size: 11px; line-height: 1.35; color: #a4e5fb;
        min-height: 1.2em; padding: 4px 6px; border-radius: 5px;
        background: rgba(4, 18, 26, 0.8);
      }
      .r54-holo-quick-pill {
        display: block; width: 100%; min-height: 44px; margin: 10px 0;
        border: 1px solid #3bd6f8; border-radius: 8px;
        background: linear-gradient(135deg, #0d2737, #07151e);
        color: #3bd6f8; font-weight: 900; font-size: 12px; letter-spacing: 0.08em;
        box-shadow: 0 0 14px rgba(59, 214, 248, 0.25);
      }
    `;
    document.head.appendChild(style);
  }

  function tick() {
    injectStyles();
    injectHolotableButton();
  }

  if (window.BraseiroDomScheduler) {
    window.BraseiroDomScheduler.register('holotable-interactive', tick);
  } else {
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', tick, { once: true });
    } else {
      tick();
    }
    addEventListener('popstate', tick);
  }
})();
