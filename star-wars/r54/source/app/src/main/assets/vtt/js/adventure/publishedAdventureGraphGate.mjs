/** P141 — source-graph structural certification, not PDF/rules certification.
 * Validates that the published adventure's authored node graph has a start,
 * reachable chapters, and at least one reachable ending. Conditions remain
 * governed by each official adapter; no alternate encounter is fabricated.
 */
export function validatePublishedAdventureGraph(module) {
  const nodes = module?.nodes;
  if (!nodes || !Object.hasOwn(nodes, module.startNodeId))
    throw new Error('Grafo publicado sem início válido.');
  const ids = Object.keys(nodes);
  const outgoing = new Map(ids.map(id => [id, (nodes[id].choices || []).map(c => c.to)]));
  const reachable = new Set([module.startNodeId]);
  const queue = [module.startNodeId];
  for (const id of queue) for (const next of outgoing.get(id) || []) {
    if (!Object.hasOwn(nodes, next)) throw new Error(`Grafo publicado aponta para cena inexistente: ${next}`);
    if (!reachable.has(next)) { reachable.add(next); queue.push(next); }
  }
  if (reachable.size !== ids.length)
    throw new Error(`Grafo publicado com cenas inalcançáveis: ${ids.filter(id => !reachable.has(id)).join(', ')}`);
  const endings = ids.filter(id => nodes[id].terminal || !outgoing.get(id).length);
  if (!endings.length) throw new Error('Grafo publicado sem desfecho.');
  const reverse = new Map(ids.map(id => [id, []]));
  for (const [id, destinations] of outgoing) for (const dest of destinations) reverse.get(dest).push(id);
  const canFinish = new Set(endings);
  const backwards = [...endings];
  for (const id of backwards) for (const previous of reverse.get(id)) {
    if (!canFinish.has(previous)) { canFinish.add(previous); backwards.push(previous); }
  }
  if (canFinish.size !== ids.length)
    throw new Error(`Grafo publicado com caminho sem desfecho: ${ids.filter(id => !canFinish.has(id)).join(', ')}`);
  const episodes = [...new Set(ids.map(id => nodes[id].episode).filter(x => Number.isInteger(x) && x > 0))].sort((a, b) => a - b);
  return { adventureId: module.id, nodes: ids.length,
    choices: [...outgoing.values()].reduce((sum, choices) => sum + choices.length, 0),
    endings: endings.length, episodes, structurallyReachable: true };
}
