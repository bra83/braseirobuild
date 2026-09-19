


export type GeminiKeyTier = 'free' | 'paid';

const LEGACY_KEY = 'braseiro.geminiApiKey';
const KEYS: Record<GeminiKeyTier, string> = {
  free: 'braseiro.geminiApiKey.free',
  paid: 'braseiro.geminiApiKey.paid',
};

/** Apenas para navegador/PWA. No Android a chave nunca atravessa a WebView. */
export function getBrowserGeminiApiKey(tier: GeminiKeyTier): string {
  const legacy = tier === 'free' ? localStorage.getItem(LEGACY_KEY) : '';
  const environment = tier === 'free' ? (process.env.API_KEY || process.env.GEMINI_API_KEY || '') : '';
  return localStorage.getItem(KEYS[tier]) || legacy || environment;
}

export function saveGeminiApiKey(tier: GeminiKeyTier, value: string) {
  const clean = value.trim();
  if (window.BraseiroAndroid?.setGeminiApiKey) {
    window.BraseiroAndroid.setGeminiApiKey(tier, clean);
    return;
  }
  if (clean) localStorage.setItem(KEYS[tier], clean); else localStorage.removeItem(KEYS[tier]);
  if (tier === 'free') localStorage.removeItem(LEGACY_KEY);
}

export function hasGeminiApiKey(tier?: GeminiKeyTier): boolean {
  if (window.BraseiroAndroid?.hasGeminiApiKey) {
    return tier ? window.BraseiroAndroid.hasGeminiApiKey(tier) : window.BraseiroAndroid.hasGeminiApiKey('free') || window.BraseiroAndroid.hasGeminiApiKey('paid');
  }
  return tier ? Boolean(getBrowserGeminiApiKey(tier)) : Boolean(getBrowserGeminiApiKey('free') || getBrowserGeminiApiKey('paid'));
}
