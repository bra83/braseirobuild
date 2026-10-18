/* Interface do Terminal Tático Holonet — Star Wars VTT */

import {
  HOLO_BIOMES,
  RANGE_BANDS,
  createInitialHolotableState,
  moveHolotableToken,
  calculateTacticalCheckModifiers,
  detectBiomeFromContext
} from '../rules/tacticalHolotable.mjs';

let currentHolotableState = null;

export function getOrCreateHolotableState(character = null, context = null) {
  if (!currentHolotableState) {
    const biome = context?.locationName ? detectBiomeFromContext(context.locationName, context.description) : 'cantina_outpost';
    currentHolotableState = createInitialHolotableState({ biomeKey: biome, character });
  }
  return currentHolotableState;
}

export function setHolotableBiome(biomeKey) {
  if (!currentHolotableState) getOrCreateHolotableState();
  if (HOLO_BIOMES[biomeKey]) {
    currentHolotableState.biomeKey = biomeKey;
    currentHolotableState.biomeName = HOLO_BIOMES[biomeKey].namePtBr;
  }
  return currentHolotableState;
}

export function renderHolotableSvg(state) {
  const biome = HOLO_BIOMES[state.biomeKey] || HOLO_BIOMES.cantina_outpost;

  // Render range rings
  const ringsSvg = RANGE_BANDS.map(b => {
    const r = b.maxRadius * 4; // Escala para viewBox 0 0 800 800 (centro 400, 400)
    return `
      <circle cx="400" cy="400" r="${r}" fill="none" stroke="rgba(59, 214, 248, 0.22)" stroke-width="1.5" stroke-dasharray="6, 6" />
      <text x="405" y="${400 - r + 15}" fill="rgba(59, 214, 248, 0.65)" font-size="11" font-family="monospace" letter-spacing="1">${b.labelPtBr.toUpperCase()}</text>
    `;
  }).join('');

  // Render props
  const propsSvg = biome.props.map(p => {
    const stroke = p.cover === 'heavy' ? '#f3a938' : (p.cover === 'light' ? '#3bd6f8' : 'rgba(213,216,220,0.4)');
    const fill = p.cover === 'heavy' ? 'rgba(243, 169, 56, 0.12)' : (p.cover === 'light' ? 'rgba(59, 214, 248, 0.08)' : 'none');

    if (p.shape === 'rect') {
      const x = p.x * 8, y = p.y * 8, w = p.w * 8, h = p.h * 8;
      return `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${fill}" stroke="${stroke}" stroke-width="2" rx="4" />
              <text x="${x + 4}" y="${y - 4}" fill="${stroke}" font-size="10" font-family="sans-serif">${p.label}</text>`;
    }
    if (p.shape === 'circle') {
      const cx = p.cx * 8, cy = p.cy * 8, r = p.r * 8;
      return `<circle cx="${cx}" cy="${cy}" r="${r}" fill="${fill}" stroke="${stroke}" stroke-width="2" />
              <text x="${cx - r}" y="${cy - r - 4}" fill="${stroke}" font-size="10" font-family="sans-serif">${p.label}</text>`;
    }
    if (p.shape === 'arc') {
      const cx = p.cx * 8, cy = p.cy * 8, r = p.r * 8;
      return `<path d="M ${cx - r} ${cy} A ${r} ${r} 0 0 1 ${cx + r} ${cy}" fill="none" stroke="${stroke}" stroke-width="3" />
              <text x="${cx - 40}" y="${cy - r - 6}" fill="${stroke}" font-size="10" font-family="sans-serif">${p.label}</text>`;
    }
    if (p.shape === 'line') {
      const x1 = p.x1 * 8, y1 = p.y1 * 8, x2 = p.x2 * 8, y2 = p.y2 * 8;
      return `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${stroke}" stroke-width="3" stroke-dasharray="10, 5" />`;
    }
    return '';
  }).join('');

  // Render Adversary Tokens
  const adversariesSvg = state.adversaryTokens.map(adv => {
    const ax = adv.x * 8, ay = adv.y * 8;
    const isTarget = adv.id === state.selectedTargetId;
    const color = isTarget ? '#ff4d4d' : '#ff9966';
    return `
      <g class="holo-token adversary-token" data-token-id="${adv.id}" transform="translate(${ax}, ${ay})" style="cursor: pointer;">
        <circle cx="0" cy="0" r="22" fill="rgba(255, 77, 77, 0.2)" stroke="${color}" stroke-width="${isTarget ? '3' : '1.5'}" filter="drop-shadow(0 0 8px ${color})" />
        <circle cx="0" cy="0" r="14" fill="${color}" opacity="0.8" />
        <text x="0" y="4" fill="#000" font-size="9" font-weight="900" text-anchor="middle">OP</text>
        <text x="0" y="34" fill="${color}" font-size="11" font-weight="800" text-anchor="middle" font-family="sans-serif">${adv.name}</text>
        ${adv.cover !== 'none' ? `<text x="0" y="-26" fill="#f3a938" font-size="9" text-anchor="middle">🛡️ ${adv.cover === 'heavy' ? 'Pesada' : 'Leve'}</text>` : ''}
      </g>
    `;
  }).join('');

  // Render Player Token
  const px = state.playerToken.x * 8, py = state.playerToken.y * 8;
  const playerSvg = `
    <g class="holo-token player-token" data-token-id="${state.playerToken.id}" transform="translate(${px}, ${py})" style="cursor: move;">
      <circle cx="0" cy="0" r="26" fill="rgba(243, 169, 56, 0.25)" stroke="#f3a938" stroke-width="3" filter="drop-shadow(0 0 12px #f3a938)" />
      <circle cx="0" cy="0" r="17" fill="#f3a938" />
      <text x="0" y="5" fill="#05080c" font-size="11" font-weight="900" text-anchor="middle">VOCÊ</text>
      <text x="0" y="38" fill="#f3a938" font-size="12" font-weight="900" text-anchor="middle" font-family="sans-serif">${state.playerToken.name}</text>
      ${state.playerToken.cover !== 'none' ? `<text x="0" y="-30" fill="#3bd6f8" font-size="10" font-weight="800" text-anchor="middle">🛡️ Cobertura ${state.playerToken.cover === 'heavy' ? 'Pesada' : 'Leve'}</text>` : ''}
    </g>
  `;

  return `
    <svg viewBox="0 0 800 800" class="holotable-svg" style="width: 100%; height: auto; max-height: 480px; background: radial-gradient(circle at center, #091a26 0%, #04090e 85%); border: 1px solid #3bd6f8; border-radius: 12px; box-shadow: inset 0 0 35px rgba(59, 214, 248, 0.25);">
      <!-- Radar grid -->
      <defs>
        <pattern id="holoGrid" width="40" height="40" patternUnits="userSpaceOnUse">
          <path d="M 40 0 L 0 0 0 40" fill="none" stroke="rgba(59, 214, 248, 0.08)" stroke-width="1" />
        </pattern>
        <radialGradient id="radarSweep" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="rgba(59, 214, 248, 0.2)" />
          <stop offset="100%" stop-color="rgba(59, 214, 248, 0.0)" />
        </radialGradient>
      </defs>
      
      <rect width="800" height="800" fill="url(#holoGrid)" />
      
      <!-- Linhas axiais do radar -->
      <line x1="400" y1="20" x2="400" y2="780" stroke="rgba(59, 214, 248, 0.15)" stroke-width="1" />
      <line x1="20" y1="400" x2="780" y2="400" stroke="rgba(59, 214, 248, 0.15)" stroke-width="1" />
      <line x1="120" y1="120" x2="680" y2="680" stroke="rgba(59, 214, 248, 0.1)" stroke-width="1" stroke-dasharray="4, 8" />
      <line x1="680" y1="120" x2="120" y2="680" stroke="rgba(59, 214, 248, 0.1)" stroke-width="1" stroke-dasharray="4, 8" />

      <!-- Anéis concêntricos de alcance -->
      ${ringsSvg}

      <!-- Cenário e Coberturas -->
      ${propsSvg}

      <!-- Tokens -->
      ${adversariesSvg}
      ${playerSvg}

      <!-- Scanline overlay -->
      <line x1="0" y1="400" x2="800" y2="400" stroke="rgba(59, 214, 248, 0.4)" stroke-width="2" opacity="0.6">
        <animate attributeName="y1" from="0" to="800" dur="4s" repeatCount="indefinite" />
        <animate attributeName="y2" from="0" to="800" dur="4s" repeatCount="indefinite" />
      </line>
    </svg>
  `;
}
