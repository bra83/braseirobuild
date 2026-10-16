from pathlib import Path
root=Path(__file__).resolve().parents[1]
base=root/'app/src/main/assets/vtt/js/adventure'
p=base/'adventureSemanticGate.mjs';s=p.read_text()
s=s.replace("export function validateAuthoredConsequence(consequence, path = 'consequence') {", "export function validateAuthoredConsequence(consequence, path = 'consequence', module = null) {")
s=s.replace("  for (const [key, value] of Object.entries(consequence)) {\n    requireId(key, `${path}.${key}`);", "  for (const [key, value] of Object.entries(consequence)) {\n    if (key === 'onResolveEffects') {\n      if (!module) throw Error(`UAE semantic: ${path} sem módulo para efeitos de resolução.`);\n      validateAuthoredEffects(value, module, `${path}.onResolveEffects`);\n      continue;\n    }\n    if (key === 'resolveWhen') {\n      if (!module) throw Error(`UAE semantic: ${path} sem módulo para condição de resolução.`);\n      validateAuthoredCondition(value, module, `${path}.resolveWhen`);\n      continue;\n    }\n    requireId(key, `${path}.${key}`);")
s=s.replace("export function validateAdventureSemantics(module) {\n  let conditions", """export function validateAdventureSemantics(module) {
  // Optional source-authored objective prerequisites are a DAG. They cannot
  // be bypassed by direct commands, nested triggers or imported checkpoints.
  const objectives = module.initialObjectives || {};
  const visiting = new Set(), visited = new Set();
  const visit = id => {
    if (visiting.has(id)) throw Error(`UAE semantic: dependência circular de objetivo ${id}.`);
    if (visited.has(id)) return;
    visiting.add(id);
    const def = objectives[id];
    if (def.requires !== undefined) {
      if (!Array.isArray(def.requires) || def.requires.length > 32 ||
          new Set(def.requires).size !== def.requires.length)
        throw Error(`UAE semantic: pré-requisitos inválidos em ${id}.`);
      for (const dependency of def.requires) {
        if (!identifier(dependency) || !own(objectives, dependency) || dependency === id)
          throw Error(`UAE semantic: pré-requisito inexistente em ${id}.`);
        visit(dependency);
      }
    }
    visiting.delete(id); visited.add(id);
  };
  for (const id of Object.keys(objectives)) visit(id);
  let conditions""")
s=s.replace("validateAuthoredConsequence(trigger.consequence, `${trigger.id}.consequence`);", "validateAuthoredConsequence(trigger.consequence, `${trigger.id}.consequence`, module);")
p.write_text(s)
p=base/'adventureRuntime.mjs';s=p.read_text()
s=s.replace("import { validateAdventureCheckpoint } from './adventureCheckpointGate.mjs';", "import { validateAdventureCheckpoint } from './adventureCheckpointGate.mjs';\nimport { validateAdventureSemantics } from './adventureSemanticGate.mjs';")
s=s.replace("    validateAdventureModule(module);\n    this.module = clone(module);", "    validateAdventureModule(module);\n    validateAdventureSemantics(module);\n    this.module = clone(module);")
s=s.replace("      const record = typeof consequence === 'string' ? { id: cId, resolved: false, recordedAt: new Date().toISOString() } : { ...consequence, resolved: false, recordedAt: new Date().toISOString() };", """      // Resolution scripts belong to the source module, not to save files.
      // Never persist executable effect instructions in consequence records.
      const metadata = typeof consequence === 'string' ? {id:cId} : {...consequence};
      delete metadata.onResolveEffects;
      delete metadata.resolveWhen;
      const record = {...metadata, resolved:false, recordedAt:new Date().toISOString()};""")
s=s.replace("      if (c && !c.resolved) {\n        c.resolved = true;", """      if (c && !c.resolved) {
        // Resolve only the original, validated source-authored script. A
        // tampered checkpoint cannot supply a replacement reward payload.
        const authored = (this.module.triggers || [])
          .map(trigger => trigger.consequence)
          .find(value => value && typeof value === 'object' && value.id === consequenceId);
        if (authored?.resolveWhen && !this.isConditionMet(authored.resolveWhen))
          throw new Error('Condição de resolução da consequência não satisfeita.');
        c.resolved = true;""")
s=s.replace("        if (onResolveEffects) this.applyEffects(onResolveEffects);\n        this.#dispatchEngineEvent('CONSEQUENCE_RESOLVED'", "        if (authored?.onResolveEffects) this.applyEffects(authored.onResolveEffects);\n        if (onResolveEffects) this.applyEffects(onResolveEffects);\n        this.#dispatchEngineEvent('CONSEQUENCE_RESOLVED'")
s=s.replace("    if (!this.state.objectives[objectiveId]) return;\n    const obj = this.state.objectives[objectiveId];\n    if (obj.status !== 'ACTIVE') return;\n\n    obj.status = 'COMPLETED';", """    if (!this.state.objectives[objectiveId]) return;
    const obj = this.state.objectives[objectiveId];
    if (obj.status !== 'ACTIVE') return;
    const requires = this.module.initialObjectives?.[objectiveId]?.requires || [];
    if (requires.some(id => this.state.objectives[id]?.status !== 'COMPLETED'))
      throw new Error(`Pré-requisitos do objetivo ${objectiveId} não concluídos.`);

    obj.status = 'COMPLETED';""")
s=s.replace("    obj.progress = Math.max(0, Math.min(obj.target, nextProgress));\n    this.dispatchEvent('OBJECTIVE_PROGRESS'", """    const next = Math.max(0, Math.min(obj.target, nextProgress));
    const requires = this.module.initialObjectives?.[objectiveId]?.requires || [];
    if (next >= obj.target && requires.some(id => this.state.objectives[id]?.status !== 'COMPLETED'))
      throw new Error(`Pré-requisitos do objetivo ${objectiveId} não concluídos.`);
    obj.progress = next;
    this.dispatchEvent('OBJECTIVE_PROGRESS'""")
p.write_text(s)
p=base/'adventureCheckpointGate.mjs';s=p.read_text()
s=s.replace("    if (!record(obj) || !['ACTIVE', 'COMPLETED', 'FAILED'].includes(obj.status) ||", """    if (obj.status === 'COMPLETED' && (authored.requires || []).some(
        dependency => state.objectives[dependency]?.status !== 'COMPLETED'))
      throw new Error(`Objetivo UAE concluído sem pré-requisitos: ${id}`);
    if (!record(obj) || !['ACTIVE', 'COMPLETED', 'FAILED'].includes(obj.status) ||""")
p.write_text(s)
p=base/'publishedAdventureUaeBridge.mjs';s=p.read_text()
s=s.replace("  validateAdventureModule(module);\n  validatePublishedAdventureGraph(module);", "  validateAdventureModule(module);\n  validatePublishedAdventureGraph(module);")
p.write_text(s)
print('P167 UAE implementation written')
