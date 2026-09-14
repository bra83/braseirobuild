export const BRIDGE_VERSION = 1 as const;
export type BridgeType = 'ViewState' | 'UiCommand' | 'CaptureFixtureCommand';
export interface BridgeEnvelope<T = unknown> { version: typeof BRIDGE_VERSION; type: BridgeType; payload: T; }

export function isBridgeEnvelope(value: unknown): value is BridgeEnvelope {
  if (!value || typeof value !== 'object') return false;
  const v = value as Record<string, unknown>;
  return v.version === BRIDGE_VERSION && (v.type === 'ViewState' || v.type === 'UiCommand' || v.type === 'CaptureFixtureCommand') && 'payload' in v;
}

export function postBridge(message: BridgeEnvelope): void {
  if (!isBridgeEnvelope(message)) throw new Error('Invalid bridge envelope');
  const bridge = (window as unknown as { BraseiroBridge?: { postMessage(raw: string): void } }).BraseiroBridge;
  bridge?.postMessage(JSON.stringify(message));
}
