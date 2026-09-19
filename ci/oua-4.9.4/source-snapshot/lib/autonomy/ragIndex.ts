import { bulkSaveStoreData, getStoreData, type ManualChunk } from '../db';

export interface ManualSearchIndexRecord {
  id: string;
  manualId: string;
  manualName: string;
  pageNum: number;
  physicalPageNum?: number;
  pageLabel?: string;
  chunkIndex: number;
  tokenCount: number;
  terms: Record<string, number>;
  updatedAt: number;
}

const STOP = new Set([
  'para','com','uma','uns','umas','que','por','como','seu','sua','seus','suas','dos','das','de','do','da','em','no','na','nos','nas','ao','aos','pela','pelo','pelos','pelas','este','esta','isso','isto','ele','ela','eles','elas','mais','menos','muito','muita','entre','sobre','sem','quando','onde','qual','quais','agora','depois','antes','ainda','the','and','for','with','from','into','that','this','these','those','are','was','were','have','has','had','not','but','you','your','their','his','her','its','can','may','will','would','should',
]);

export function normalizeIndexText(value: unknown): string {
  return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
}

export function tokenizeForIndex(value: string): string[] {
  return normalizeIndexText(value)
    .split(/[^a-z0-9]+/i)
    .map(item => item.trim())
    .filter(item => item.length > 2 && !STOP.has(item));
}

export function createManualIndexRecord(chunk: ManualChunk): ManualSearchIndexRecord {
  const tokens = tokenizeForIndex(chunk.text);
  const terms: Record<string, number> = {};
  for (const token of tokens) terms[token] = (terms[token] || 0) + 1;
  return {
    id: chunk.id,
    manualId: chunk.manualId,
    manualName: chunk.manualName,
    pageNum: chunk.pageNum,
    physicalPageNum: chunk.physicalPageNum,
    pageLabel: chunk.pageLabel,
    chunkIndex: chunk.index,
    tokenCount: tokens.length,
    terms,
    updatedAt: Date.now(),
  };
}

export async function indexManualChunks(chunks: ManualChunk[]): Promise<void> {
  if (!chunks.length) return;
  const batchSize = 120;
  for (let index = 0; index < chunks.length; index += batchSize) {
    await bulkSaveStoreData('manual_search_index', chunks.slice(index, index + batchSize).map(createManualIndexRecord));
    await new Promise<void>(resolve => setTimeout(resolve, 0));
  }
}

export async function rebuildManualSearchIndex(): Promise<number> {
  const chunks = await getStoreData<ManualChunk>('manual_chunks');
  await indexManualChunks(chunks);
  return chunks.length;
}

export function rankManualIndex(
  records: ManualSearchIndexRecord[],
  queryValues: string[],
  priorityByManual: Map<string, number>,
  limit = 24,
): Array<{ id: string; score: number }> {
  const terms = Array.from(new Set(queryValues.flatMap(tokenizeForIndex))).slice(0, 80);
  if (!terms.length || !records.length) return [];
  const documentCount = records.length;
  const documentFrequency = new Map<string, number>();
  for (const term of terms) {
    let count = 0;
    for (const record of records) if (record.terms[term]) count += 1;
    documentFrequency.set(term, count);
  }
  const avgLength = records.reduce((sum, record) => sum + Math.max(1, record.tokenCount), 0) / documentCount;
  const k1 = 1.35;
  const b = 0.72;
  return records.map(record => {
    let score = 0;
    for (const term of terms) {
      const tf = record.terms[term] || 0;
      if (!tf) continue;
      const df = documentFrequency.get(term) || 0;
      const idf = Math.log(1 + (documentCount - df + 0.5) / (df + 0.5));
      const norm = tf + k1 * (1 - b + b * record.tokenCount / Math.max(1, avgLength));
      score += idf * (tf * (k1 + 1)) / norm;
    }
    const priority = priorityByManual.get(record.manualId) || 4;
    score *= 1 + Math.max(0, 5 - priority) * 0.12;
    return { id: record.id, score };
  }).filter(item => item.score > 0).sort((a, b) => b.score - a.score).slice(0, limit);
}
