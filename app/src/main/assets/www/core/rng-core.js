export const RNG_ALGORITHM = 'splitmix64-counter-v1';
const MASK64 = 0xffffffffffffffffn;
const GAMMA = 0x9e3779b97f4a7c15n;
const TWO64 = 1n << 64n;

function fnv1a64(text) {
  let hash = 0xcbf29ce484222325n;
  for (const byte of new TextEncoder().encode(text)) {
    hash ^= BigInt(byte);
    hash = (hash * 0x100000001b3n) & MASK64;
  }
  return hash;
}

function mix64(value) {
  let z = value & MASK64;
  z = ((z ^ (z >> 30n)) * 0xbf58476d1ce4e5b9n) & MASK64;
  z = ((z ^ (z >> 27n)) * 0x94d049bb133111ebn) & MASK64;
  return (z ^ (z >> 31n)) & MASK64;
}

function rawAt(masterSeed, streamName, counter) {
  const streamSeed = fnv1a64(`${RNG_ALGORITHM}\u241f${masterSeed}\u241f${streamName}`);
  return mix64((streamSeed + GAMMA * BigInt(counter + 1)) & MASK64);
}

export class DeterministicRng {
  constructor(masterSeed, counters = {}) {
    if (typeof masterSeed !== 'string' || masterSeed.length === 0) throw new Error('masterSeed required');
    this.masterSeed = masterSeed;
    this.counters = { ...counters };
  }

  raw64(streamName = 'default') {
    const counter = this.counters[streamName] ?? 0;
    const raw = rawAt(this.masterSeed, streamName, counter);
    this.counters[streamName] = counter + 1;
    return { raw, counter };
  }

  die(sides, streamName = 'dice') {
    if (!Number.isInteger(sides) || sides < 2) throw new Error('sides must be integer >= 2');
    const span = BigInt(sides);
    const limit = TWO64 - (TWO64 % span);
    while (true) {
      const { raw, counter } = this.raw64(streamName);
      if (raw < limit) {
        const result = Number(raw % span) + 1;
        return {
          algorithm: RNG_ALGORITHM,
          stream: streamName,
          counterBefore: counter,
          rawHex: raw.toString(16).padStart(16, '0'),
          sides,
          result,
        };
      }
    }
  }

  exportState() {
    return {
      algorithm: RNG_ALGORITHM,
      masterSeed: this.masterSeed,
      counters: { ...this.counters },
    };
  }

  static fromState(state) {
    if (state.algorithm !== RNG_ALGORITHM) throw new Error(`Unsupported RNG algorithm ${state.algorithm}`);
    return new DeterministicRng(state.masterSeed, state.counters);
  }
}
