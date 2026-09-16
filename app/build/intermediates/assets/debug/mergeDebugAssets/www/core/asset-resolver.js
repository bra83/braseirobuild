const BLOCKED_PROVENANCE = new Set(['PRIVATE_REFERENCE_ONLY','STYLE_REFERENCE_ONLY','MISSING_EVIDENCE']);

export class AssetResolver {
  constructor(records) {
    this.records = new Map(records.map(r => [r.assetId, r]));
  }
  resolve(assetId) {
    const record = this.records.get(assetId);
    if (!record) throw new Error(`Unknown asset ${assetId}`);
    if (BLOCKED_PROVENANCE.has(record.provenance)) throw new Error(`Asset ${assetId} provenance blocked`);
    return record;
  }
}
