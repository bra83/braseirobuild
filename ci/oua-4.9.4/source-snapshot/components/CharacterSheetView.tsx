import React, { useEffect, useMemo, useState } from 'react';
import { CharacterSheet, IndividualToken, getStoreData } from '../lib/db';
import { 
  Shield, Sword, Target, Coins, Package, 
  Plus, Trash2, Clock, CheckCircle, Edit3, User, 
  Heart, Sparkles, Feather, Archive, ClipboardList, Search, X
} from 'lucide-react';
import { TokenPicker } from './TokenPicker';

interface CharacterSheetViewProps {
  sheet: CharacterSheet;
  onSaveSheet: (updatedSheet: CharacterSheet) => void;
}

import { ITEM_CATALOG, ITEM_CATEGORIES, CatalogItem } from '../lib/itemCatalog';
import { virtuesForCulture } from '../lib/supplementRules';

export const CharacterSheetView: React.FC<CharacterSheetViewProps> = ({ sheet, onSaveSheet }) => {
  const [logReason, setLogReason] = useState('Atualização manual');
  const [newSkillName, setNewSkillName] = useState('');
  const [newWeaponName, setNewWeaponName] = useState('');
  const [newItemName, setNewItemName] = useState('');
  const [itemCategory, setItemCategory] = useState('Todos');
  const [showCatalog, setShowCatalog] = useState(false);
  const [tokens, setTokens] = useState<Record<string, string>>({});
  const [isPickerOpen, setIsPickerOpen] = useState(false);

  useEffect(() => {
    let disposed = false;
    const urls: string[] = [];
    void getStoreData<IndividualToken>('individual_tokens').then(allTokens => {
      const tokenMap: Record<string, string> = {};
      allTokens.forEach(token => {
        const url = URL.createObjectURL(token.blob);
        urls.push(url);
        tokenMap[token.id] = url;
      });
      if (!disposed) setTokens(tokenMap);
    }).catch(error => console.error('BRASEIRO_SHEET: falha ao carregar tokens', error));
    return () => { disposed = true; urls.forEach(url => URL.revokeObjectURL(url)); };
  }, []);

  const sourceBoundVirtues = useMemo(() => virtuesForCulture(sheet.identity.culture), [sheet.identity.culture]);
  const currentVirtueNames = useMemo(() => sheet.other.virtues.split(/[;\n,]+/).map(value => value.trim()).filter(Boolean), [sheet.other.virtues]);
  const wisdom = Number(sheet.attributes.wisdom || 1);
  const mayAddCulturalVirtue = wisdom > currentVirtueNames.length;
  const addCulturalVirtue = (name: string) => {
    if (!mayAddCulturalVirtue || currentVirtueNames.includes(name)) return;
    const next = [...currentVirtueNames, name].join('; ');
    handleOtherChange('virtues', next);
  };

  const totalLoad = (inventory: CharacterSheet['inventory']) => inventory.reduce(
    (sum, item) => sum + Math.max(0, Number(item.load ?? item.weight ?? 0)) * Math.max(0, Number(item.quantity || 0)),
    0,
  );

  const commit = (next: CharacterSheet, field?: string, oldValue?: unknown, newValue?: unknown) => {
    const log = field ? [{
      timestamp: Date.now(), field,
      oldValue: String(oldValue ?? '') || 'Vazio',
      newValue: String(newValue ?? '') || 'Vazio',
      reason: logReason.trim() || 'Atualização manual',
    }, ...(next.historyLog || [])].slice(0, 50) : (next.historyLog || []);
    const safeInventory = next.inventory || [];
    onSaveSheet({
      ...next,
      inventory: safeInventory,
      historyLog: log,
      attributes: { ...next.attributes, load: totalLoad(safeInventory) },
    });
  };

  const handleSelectToken = (tokenId: string | null) => {
    commit({ ...sheet, tokenId }, 'Retrato', sheet.tokenId, tokenId);
    setIsPickerOpen(false);
  };

  const handleIdentityChange = (key: keyof CharacterSheet['identity'], value: string | number) => {
    if (sheet.identity[key] === value) return;
    commit({ ...sheet, identity: { ...sheet.identity, [key]: value } }, `Identidade.${key}`, sheet.identity[key], value);
  };
  const handleAttributeChange = (key: keyof CharacterSheet['attributes'], value: number | boolean) => {
    if (sheet.attributes[key] === value) return;
    commit({ ...sheet, attributes: { ...sheet.attributes, [key]: value } }, `Atributos.${key}`, sheet.attributes[key], value);
  };
  const handleResourceChange = (key: keyof CharacterSheet['resources'], value: number) => {
    if (sheet.resources[key] === value) return;
    commit({ ...sheet, resources: { ...sheet.resources, [key]: value } }, `Recursos.${key}`, sheet.resources[key], value);
  };
  const handleCombatChange = (key: keyof CharacterSheet['combat'], value: string | number) => {
    if ((sheet.combat as any)[key] === value) return;
    commit({ ...sheet, combat: { ...sheet.combat, [key]: value } }, `Combate.${String(key)}`, (sheet.combat as any)[key], value);
  };
  const handleOtherChange = (key: keyof CharacterSheet['other'], value: string) => {
    if (sheet.other[key] === value) return;
    commit({ ...sheet, other: { ...sheet.other, [key]: value } }, `Outros.${key}`, sheet.other[key], value);
  };

  const handleAddSkill = () => {
    const name = newSkillName.trim(); if (!name) return;
    const skill = { name, rank: 0, specialization: '', favorite: false };
    commit({ ...sheet, skills: [...sheet.skills, skill] }, 'Perícias', '', name);
    setNewSkillName('');
  };
  const handleRemoveSkill = (index: number) => {
    const removed = sheet.skills[index];
    commit({ ...sheet, skills: sheet.skills.filter((_, itemIndex) => itemIndex !== index) }, 'Perícias', removed?.name, 'Removida');
  };
  const handleSkillItemChange = (index: number, field: 'rank' | 'specialization' | 'favorite', value: unknown) => {
    const skills = sheet.skills.map((skill, itemIndex) => itemIndex === index ? { ...skill, [field]: value } : skill);
    commit({ ...sheet, skills });
  };

  const handleAddWeapon = () => {
    const name = newWeaponName.trim(); if (!name) return;
    const weapon = { name, damage: 4, injury: 14, group: 'Espada', active: true };
    commit({ ...sheet, combat: { ...sheet.combat, weapons: [...sheet.combat.weapons, weapon] } }, 'Armas', '', name);
    setNewWeaponName('');
  };
  const handleRemoveWeapon = (index: number) => {
    const removed = sheet.combat.weapons[index];
    commit({ ...sheet, combat: { ...sheet.combat, weapons: sheet.combat.weapons.filter((_, itemIndex) => itemIndex !== index) } }, 'Armas', removed?.name, 'Removida');
  };
  const handleWeaponItemChange = (index: number, field: string, value: unknown) => {
    const weapons = sheet.combat.weapons.map((weapon, itemIndex) => itemIndex === index ? { ...weapon, [field]: value } : weapon);
    commit({ ...sheet, combat: { ...sheet.combat, weapons } });
  };

  const addCatalogItem = (catalogItem: CatalogItem) => {
    const inventoryItem = {
      id: `item_${catalogItem.id}_${Date.now()}`,
      name: catalogItem.name,
      quantity: 1,
      weight: catalogItem.load,
      load: catalogItem.load,
      category: catalogItem.category,
      description: catalogItem.description,
      equipped: false,
      value: catalogItem.value,
      damage: catalogItem.damage,
      injury: catalogItem.injury,
      protection: catalogItem.protection,
      tags: catalogItem.tags,
    };
    const nextInventory = [...sheet.inventory, inventoryItem];
    const weapons = catalogItem.category === 'Arma' && catalogItem.damage != null
      ? [...sheet.combat.weapons, { name: catalogItem.name, damage: catalogItem.damage, injury: catalogItem.injury || 0, group: catalogItem.tags[0] || 'Arma', active: false }]
      : sheet.combat.weapons;
    commit({ ...sheet, inventory: nextInventory, combat: { ...sheet.combat, weapons } }, 'Inventário', '', catalogItem.name);
    setNewItemName('');
    setShowCatalog(false);
  };

  const handleAddItem = () => {
    const name = newItemName.trim(); if (!name) { setShowCatalog(true); return; }
    const match = ITEM_CATALOG.find(item => item.name.toLocaleLowerCase('pt-BR') === name.toLocaleLowerCase('pt-BR'));
    if (match) { addCatalogItem(match); return; }
    const custom: CatalogItem = { id: `custom_${Date.now()}`, name, category: itemCategory === 'Todos' ? 'Geral' : itemCategory, description: '', load: 0, tags: ['personalizado'] };
    addCatalogItem(custom);
  };
  const handleRemoveItem = (id: string) => {
    const removed = sheet.inventory.find(item => item.id === id);
    const inventory = sheet.inventory.filter(item => item.id !== id);
    commit({ ...sheet, inventory }, 'Inventário', removed?.name, 'Removido');
  };
  const handleInventoryItemChange = (index: number, field: string, value: unknown) => {
    const inventory = sheet.inventory.map((item, itemIndex) => itemIndex === index ? {
      ...item,
      [field]: value,
      ...(field === 'weight' ? { load: Number(value) || 0 } : {}),
      ...(field === 'load' ? { weight: Number(value) || 0 } : {}),
    } : item);
    commit({ ...sheet, inventory });
  };

  const filteredCatalog = useMemo(() => {
    const query = newItemName.trim().toLocaleLowerCase('pt-BR');
    return ITEM_CATALOG.filter(item =>
      (itemCategory === 'Todos' || item.category === itemCategory) &&
      (!query || `${item.name} ${item.category} ${item.tags.join(' ')}`.toLocaleLowerCase('pt-BR').includes(query)),
    ).slice(0, 40);
  }, [newItemName, itemCategory]);

  return (
    <div className="w-full max-w-5xl mx-auto flex flex-col gap-6 text-blue-100 font-serif pb-12 select-none">
      
      {/* Logger Reason Panel */}
      <div className="bg-zinc-950/60 border border-blue-900/30 p-3 rounded flex flex-col sm:flex-row justify-between items-center gap-2">
        <div className="flex items-center gap-2 text-xs">
          <Clock className="w-4 h-4 text-blue-500" />
          <span className="font-bold text-blue-400 font-serif">MOTIVO DA ALTERAÇÃO DA FICHA:</span>
        </div>
        <input
          type="text"
          value={logReason}
          onChange={(e) => setLogReason(e.target.value)}
          placeholder="Ex: Recebeu dano de Warg, Encontrou tesouro..."
          className="flex-1 max-w-lg px-2.5 py-1 bg-zinc-900 border border-blue-900/40 rounded text-xs text-blue-100 focus:outline-none focus:border-blue-500"
        />
      </div>

      {(sheet.other.creationTrace?.length || sheet.other.culturalBlessing || sheet.combat.proficiencies) && (
        <div className="rounded border border-cyan-900/60 bg-cyan-950/10 p-4 text-xs text-slate-300">
          <div className="mb-2 flex flex-wrap items-center justify-between gap-2"><strong className="text-cyan-200">Criação e regras de O Um Anel</strong><span>Valor {sheet.attributes.valor ?? '—'} · Sabedoria {sheet.attributes.wisdom ?? '—'}</span></div>
          {sheet.other.culturalBlessing && <p><b>Bênção cultural:</b> {sheet.other.culturalBlessing}</p>}
          <div className="mt-2 grid gap-2 sm:grid-cols-2">
            <p><b>Traço da Vocação:</b> {sheet.other.callingFeature || '—'}<br/><b>Caminho da Sombra:</b> {sheet.other.shadowPath || '—'}</p>
            <p><b>Características:</b> {(sheet.other.distinctiveFeatures || []).join(', ') || '—'}<br/><b>Proficiências:</b> {Object.entries(sheet.combat.proficiencies || {}).filter(([,rank]) => Number(rank) > 0).map(([name,rank]) => `${name} ${rank}`).join(' · ') || '—'}</p>
          </div>
          {sheet.other.creationTrace?.length ? <details className="mt-2"><summary className="cursor-pointer text-cyan-300">Auditoria da criação automática</summary><ul className="mt-2 list-disc space-y-1 pl-5">{sheet.other.creationTrace.map((line,index) => <li key={`${line}-${index}`}>{line}</li>)}</ul></details> : null}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column: Identidade and Atributos */}
        <div className="lg:col-span-4 flex flex-col gap-6">
          
          {/* Card 1: Identidade */}
          <div className="bg-zinc-950/90 border-2 border-blue-900 p-4 rounded-md ambient-glow relative">
            <div className="absolute top-2 left-2 w-3 h-3 border-l-2 border-t-2 border-blue-700/60"></div>
            <div className="flex items-center gap-2 border-b border-blue-900/40 pb-2 mb-4">
              <User className="w-4 h-4 text-blue-500" />
              <span className="text-xs font-black text-blue-500 font-serif tracking-wider">IDENTIDADE</span>
            </div>
            
            <div className="flex flex-col items-center mb-4">
              <div 
                onClick={() => setIsPickerOpen(true)}
                className="w-20 h-20 rounded-full border-2 border-blue-800/80 hover:border-blue-400 bg-zinc-950 overflow-hidden relative shrink-0 flex items-center justify-center cursor-pointer shadow-md mb-2"
                title="Atribuir retrato"
              >
                {sheet.tokenId && tokens[sheet.tokenId] ? (
                  <img src={tokens[sheet.tokenId]} alt={sheet.identity.name} className="w-full h-full object-cover" />
                ) : (
                  <User className="w-8 h-8 text-blue-800" />
                )}
                <div className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 hover:opacity-100 transition-opacity">
                  <Edit3 className="w-4 h-4 text-blue-100" />
                </div>
              </div>
            </div>
            
            <div className="space-y-3 text-xs">
              <div>
                <label className="text-[11px] text-blue-600 font-semibold uppercase font-serif">Nome do Personagem</label>
                <input
                  type="text"
                  value={sheet.identity.name}
                  onChange={(e) => handleIdentityChange('name', e.target.value)}
                  className="w-full px-2 py-1.5 bg-zinc-900/60 border border-blue-900/30 rounded text-blue-100 focus:outline-none focus:border-blue-600 font-bold"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-[11px] text-blue-600 font-semibold uppercase font-serif">Cultura</label>
                  <input
                    type="text"
                    value={sheet.identity.culture}
                    onChange={(e) => handleIdentityChange('culture', e.target.value)}
                    className="w-full px-2 py-1 bg-zinc-900/60 border border-blue-900/30 rounded text-blue-100 focus:outline-none focus:border-blue-600"
                  />
                </div>
                <div>
                  <label className="text-[11px] text-blue-600 font-semibold uppercase font-serif">Vocação</label>
                  <input
                    type="text"
                    value={sheet.identity.vocation}
                    onChange={(e) => handleIdentityChange('vocation', e.target.value)}
                    className="w-full px-2 py-1 bg-zinc-900/60 border border-blue-900/30 rounded text-blue-100 focus:outline-none focus:border-blue-600"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-[11px] text-blue-600 font-semibold uppercase font-serif">Padrão de Vida</label>
                  <input
                    type="text"
                    value={sheet.identity.lifestyle}
                    onChange={(e) => handleIdentityChange('lifestyle', e.target.value)}
                    className="w-full px-2 py-1 bg-zinc-900/60 border border-blue-900/30 rounded text-blue-100 focus:outline-none focus:border-blue-600"
                  />
                </div>
                <div>
                  <label className="text-[11px] text-blue-600 font-semibold uppercase font-serif">Idade (anos)</label>
                  <input
                    type="number"
                    value={sheet.identity.age || 0}
                    onChange={(e) => handleIdentityChange('age', parseInt(e.target.value) || 0)}
                    className="w-full px-2 py-1 bg-zinc-900/60 border border-blue-900/30 rounded text-blue-100 focus:outline-none focus:border-blue-600"
                  />
                </div>
              </div>

              <div>
                <label className="text-[11px] text-blue-600 font-semibold uppercase font-serif">Patrono Protetor</label>
                <input
                  type="text"
                  value={sheet.identity.patron}
                  onChange={(e) => handleIdentityChange('patron', e.target.value)}
                  className="w-full px-2 py-1 bg-zinc-900/60 border border-blue-900/30 rounded text-blue-100 focus:outline-none focus:border-blue-600"
                />
              </div>

              <div>
                <label className="text-[11px] text-blue-600 font-semibold uppercase font-serif">Companhia de Viagem</label>
                <input
                  type="text"
                  value={sheet.identity.company}
                  onChange={(e) => handleIdentityChange('company', e.target.value)}
                  className="w-full px-2 py-1 bg-zinc-900/60 border border-blue-900/30 rounded text-blue-100 focus:outline-none focus:border-blue-600"
                />
              </div>

              <div>
                <label className="text-[11px] text-blue-600 font-semibold uppercase font-serif">Descrição do Herói</label>
                <textarea
                  rows={3}
                  value={sheet.identity.description}
                  onChange={(e) => handleIdentityChange('description', e.target.value)}
                  className="w-full px-2 py-1 bg-zinc-900/60 border border-blue-900/30 rounded text-blue-100 focus:outline-none focus:border-blue-600 resize-none"
                />
              </div>
            </div>
          </div>

          {/* Card 2: Atributos & Condições */}
          <div className="bg-zinc-950/90 border-2 border-blue-900 p-4 rounded-md ambient-glow relative">
            <div className="absolute top-2 left-2 w-3 h-3 border-l-2 border-t-2 border-blue-700/60"></div>
            <div className="flex items-center gap-2 border-b border-blue-900/40 pb-2 mb-4">
              <Heart className="w-4 h-4 text-blue-500" />
              <span className="text-xs font-black text-blue-500 font-serif tracking-wider">ATRIBUTOS & VITALIDADE</span>
            </div>

            <div className="space-y-4 text-xs">
              {/* Three Core Attributes */}
              <div className="grid grid-cols-3 gap-2 text-center bg-blue-950/10 p-2 rounded border border-blue-900/30">
                <div>
                  <div className="text-[10px] text-blue-500 font-bold font-serif">FORÇA</div>
                  <input
                    type="number"
                    value={sheet.attributes.strength}
                    onChange={(e) => handleAttributeChange('strength', parseInt(e.target.value) || 0)}
                    className="w-full bg-transparent border-b border-blue-900 text-center font-bold text-base focus:outline-none focus:border-blue-500 text-blue-300"
                  />
                </div>
                <div>
                  <div className="text-[10px] text-blue-500 font-bold font-serif">CORAÇÃO</div>
                  <input
                    type="number"
                    value={sheet.attributes.heart}
                    onChange={(e) => handleAttributeChange('heart', parseInt(e.target.value) || 0)}
                    className="w-full bg-transparent border-b border-blue-900 text-center font-bold text-base focus:outline-none focus:border-blue-500 text-blue-300"
                  />
                </div>
                <div>
                  <div className="text-[10px] text-blue-500 font-bold font-serif">ASTÚCIA</div>
                  <input
                    type="number"
                    value={sheet.attributes.wits}
                    onChange={(e) => handleAttributeChange('wits', parseInt(e.target.value) || 0)}
                    className="w-full bg-transparent border-b border-blue-900 text-center font-bold text-base focus:outline-none focus:border-blue-500 text-blue-300"
                  />
                </div>
              </div>

              {/* Endurance & Hope Currents vs Maxes */}
              <div className="grid grid-cols-2 gap-3">
                <div className="p-2 border border-red-950 rounded bg-red-950/10">
                  <div className="text-[10px] text-red-400 font-bold font-serif uppercase">Resistência</div>
                  <div className="flex items-center gap-1.5 mt-1">
                    <input
                      type="number"
                      value={sheet.attributes.enduranceCurrent}
                      onChange={(e) => handleAttributeChange('enduranceCurrent', parseInt(e.target.value) || 0)}
                      className="w-12 px-1 py-0.5 bg-zinc-900 border border-red-900 rounded text-center text-red-400 font-bold text-sm focus:outline-none"
                    />
                    <span className="text-blue-800 font-bold">/</span>
                    <input
                      type="number"
                      value={sheet.attributes.enduranceMax}
                      onChange={(e) => handleAttributeChange('enduranceMax', parseInt(e.target.value) || 0)}
                      className="w-12 px-1 py-0.5 bg-zinc-900 border border-red-900 rounded text-center text-red-700 font-bold text-sm focus:outline-none"
                    />
                  </div>
                </div>

                <div className="p-2 border border-cyan-950 rounded bg-cyan-950/10">
                  <div className="text-[10px] text-cyan-400 font-bold font-serif uppercase">Esperança</div>
                  <div className="flex items-center gap-1.5 mt-1">
                    <input
                      type="number"
                      value={sheet.attributes.hopeCurrent}
                      onChange={(e) => handleAttributeChange('hopeCurrent', parseInt(e.target.value) || 0)}
                      className="w-12 px-1 py-0.5 bg-zinc-900 border border-cyan-900 rounded text-center text-cyan-400 font-bold text-sm focus:outline-none"
                    />
                    <span className="text-blue-800 font-bold">/</span>
                    <input
                      type="number"
                      value={sheet.attributes.hopeMax}
                      onChange={(e) => handleAttributeChange('hopeMax', parseInt(e.target.value) || 0)}
                      className="w-12 px-1 py-0.5 bg-zinc-900 border border-cyan-900 rounded text-center text-cyan-700 font-bold text-sm focus:outline-none"
                    />
                  </div>
                </div>
              </div>

              {/* Shadow, Fatigue, Load, Supplies */}
              <div className="grid grid-cols-2 gap-2 text-xs">
                <div>
                  <label className="text-[11px] text-blue-600 font-semibold font-serif">Pontos de Sombra</label>
                  <input
                    type="number"
                    value={sheet.attributes.shadow}
                    onChange={(e) => handleAttributeChange('shadow', parseInt(e.target.value) || 0)}
                    className="w-full px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded focus:outline-none"
                  />
                </div>
                <div>
                  <label className="text-[11px] text-blue-600 font-semibold font-serif">Fadiga</label>
                  <input
                    type="number"
                    value={sheet.attributes.fatigue}
                    onChange={(e) => handleAttributeChange('fatigue', parseInt(e.target.value) || 0)}
                    className="w-full px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded focus:outline-none"
                  />
                </div>
                <div>
                  <label className="text-[11px] text-blue-600 font-semibold font-serif">Carga Total</label>
                  <input
                    type="number"
                    value={sheet.attributes.load}
                    onChange={(e) => handleAttributeChange('load', parseInt(e.target.value) || 0)}
                    className="w-full px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded focus:outline-none"
                  />
                </div>
                <div>
                  <label className="text-[11px] text-blue-600 font-semibold font-serif">Provisões (%)</label>
                  <input
                    type="number"
                    value={sheet.attributes.supplies}
                    onChange={(e) => handleAttributeChange('supplies', parseInt(e.target.value) || 0)}
                    className="w-full px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded focus:outline-none"
                  />
                </div>
              </div>

              {/* Core Checkboxes */}
              <div className="grid grid-cols-3 gap-2 pt-2 border-t border-blue-900/30">
                <label className="flex items-center gap-1.5 p-1.5 bg-zinc-900 rounded border border-blue-900/10 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={sheet.attributes.wounded}
                    onChange={(e) => handleAttributeChange('wounded', e.target.checked)}
                    className="accent-blue-500 cursor-pointer"
                  />
                  <span className="text-[11px] font-bold text-blue-300">Ferido</span>
                </label>
                <label className="flex items-center gap-1.5 p-1.5 bg-zinc-900 rounded border border-blue-900/10 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={sheet.attributes.weary}
                    onChange={(e) => handleAttributeChange('weary', e.target.checked)}
                    className="accent-blue-500 cursor-pointer"
                  />
                  <span className="text-[11px] font-bold text-blue-300">Exausto</span>
                </label>
                <label className="flex items-center gap-1.5 p-1.5 bg-zinc-900 rounded border border-blue-900/10 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={sheet.attributes.miserable}
                    onChange={(e) => handleAttributeChange('miserable', e.target.checked)}
                    className="accent-blue-500 cursor-pointer"
                  />
                  <span className="text-[11px] font-bold text-blue-300">Arrasado</span>
                </label>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Skills, Combat, Resources, Inventory */}
        <div className="lg:col-span-8 flex flex-col gap-6">
          
          {/* Card 3: Perícias */}
          <div className="bg-zinc-950/90 border-2 border-blue-900 p-4 rounded-md ambient-glow relative">
            <div className="absolute top-2 left-2 w-3 h-3 border-l-2 border-t-2 border-blue-700/60"></div>
            <div className="flex items-center justify-between border-b border-blue-900/40 pb-2 mb-4">
              <div className="flex items-center gap-2">
                <Feather className="w-4 h-4 text-blue-500" />
                <span className="text-xs font-black text-blue-500 font-serif tracking-wider">PERÍCIAS</span>
              </div>
              <div className="flex items-center gap-1.5">
                <input
                  type="text"
                  placeholder="Nova perícia..."
                  value={newSkillName}
                  onChange={(e) => setNewSkillName(e.target.value)}
                  className="px-2 py-0.5 bg-zinc-900 border border-blue-900/40 rounded text-xs focus:outline-none"
                />
                <button
                  onClick={handleAddSkill}
                  className="p-1 bg-blue-950/40 hover:bg-blue-600/20 border border-blue-600 text-blue-400 rounded cursor-pointer"
                >
                  <Plus className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs space-y-1.5">
                <thead>
                  <tr className="text-blue-600 text-[11px] font-serif font-bold border-b border-blue-900/30">
                    <th className="py-1">Nome</th>
                    <th className="py-1 text-center">Graduação</th>
                    <th className="py-1">Especialização</th>
                    <th className="py-1 text-center">Favorito</th>
                    <th className="py-1 text-center">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-blue-950/20">
                  {sheet.skills.map((skill, idx) => (
                    <tr key={idx} className="hover:bg-blue-950/10">
                      <td className="py-2 font-bold text-blue-200">{skill.name}</td>
                      <td className="py-2 text-center">
                        <select
                          value={skill.rank}
                          onChange={(e) => handleSkillItemChange(idx, 'rank', parseInt(e.target.value) || 0)}
                          className="bg-zinc-900 border border-blue-900/40 rounded text-xs text-blue-300 py-0.5 px-1.5 cursor-pointer"
                        >
                          {[0, 1, 2, 3, 4, 5, 6].map(num => (
                            <option key={num} value={num}>{num} {Array(num).fill('◆').join('')}</option>
                          ))}
                        </select>
                      </td>
                      <td className="py-2">
                        <input
                          type="text"
                          value={skill.specialization}
                          onChange={(e) => handleSkillItemChange(idx, 'specialization', e.target.value)}
                          placeholder="Nenhuma"
                          className="px-2 py-0.5 bg-transparent border-b border-blue-900/30 focus:border-blue-500 rounded text-xs text-blue-100 max-w-[120px]"
                        />
                      </td>
                      <td className="py-2 text-center">
                        <input
                          type="checkbox"
                          checked={skill.favorite}
                          onChange={(e) => handleSkillItemChange(idx, 'favorite', e.target.checked)}
                          className="accent-blue-500 cursor-pointer"
                        />
                      </td>
                      <td className="py-2 text-center">
                        <button
                          onClick={() => handleRemoveSkill(idx)}
                          className="text-blue-800 hover:text-red-500 cursor-pointer p-0.5"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Card 4: Combate e Armas */}
          <div className="bg-zinc-950/90 border-2 border-blue-900 p-4 rounded-md ambient-glow relative">
            <div className="absolute top-2 left-2 w-3 h-3 border-l-2 border-t-2 border-blue-700/60"></div>
            <div className="flex items-center justify-between border-b border-blue-900/40 pb-2 mb-4">
              <div className="flex items-center gap-2">
                <Sword className="w-4 h-4 text-blue-500" />
                <span className="text-xs font-black text-blue-500 font-serif tracking-wider">COMBATE & DEFESAS</span>
              </div>
              <div className="flex items-center gap-1.5">
                <input
                  type="text"
                  placeholder="Nova arma..."
                  value={newWeaponName}
                  onChange={(e) => setNewWeaponName(e.target.value)}
                  className="px-2 py-0.5 bg-zinc-900 border border-blue-900/40 rounded text-xs focus:outline-none"
                />
                <button
                  onClick={handleAddWeapon}
                  className="p-1 bg-blue-950/40 hover:bg-blue-600/20 border border-blue-600 text-blue-400 rounded cursor-pointer"
                >
                  <Plus className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-4 text-xs">
              <div>
                <label className="text-[11px] text-blue-600 font-semibold uppercase font-serif">Bloqueio</label>
                <input
                  type="number"
                  value={sheet.combat.parry}
                  onChange={(e) => handleCombatChange('parry', parseInt(e.target.value) || 0)}
                  className="w-full px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded focus:outline-none"
                />
              </div>
              <div>
                <label className="text-[11px] text-blue-600 font-semibold uppercase font-serif">Armadura</label>
                <input
                  type="text"
                  value={sheet.combat.armor}
                  onChange={(e) => handleCombatChange('armor', e.target.value)}
                  className="w-full px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded focus:outline-none"
                />
              </div>
              <div>
                <label className="text-[11px] text-blue-600 font-semibold uppercase font-serif">Capacete</label>
                <input
                  type="text"
                  value={sheet.combat.helmet}
                  onChange={(e) => handleCombatChange('helmet', e.target.value)}
                  className="w-full px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded focus:outline-none"
                />
              </div>
              <div>
                <label className="text-[11px] text-blue-600 font-semibold uppercase font-serif">Escudo</label>
                <input
                  type="text"
                  value={sheet.combat.shield}
                  onChange={(e) => handleCombatChange('shield', e.target.value)}
                  className="w-full px-2 py-1 bg-zinc-900 border border-blue-900/30 rounded focus:outline-none"
                />
              </div>
            </div>

            {/* Weapons list */}
            <div className="space-y-2 text-xs">
              {sheet.combat.weapons.map((weapon, idx) => (
                <div key={idx} className="p-2 border border-blue-900/20 bg-zinc-900/40 rounded flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 hover:border-blue-900/40">
                  <div className="flex items-center gap-2">
                    <Sword className="w-3.5 h-3.5 text-blue-600" />
                    <input
                      type="text"
                      value={weapon.name}
                      onChange={(e) => handleWeaponItemChange(idx, 'name', e.target.value)}
                      className="bg-transparent border-b border-transparent focus:border-blue-600 font-bold text-blue-200 focus:outline-none py-0.5"
                    />
                  </div>
                  <div className="flex flex-wrap items-center gap-3">
                    <div className="flex items-center gap-1.5">
                      <span className="text-[9px] text-blue-700 font-bold font-serif uppercase">Dano:</span>
                      <input
                        type="number"
                        value={weapon.damage}
                        onChange={(e) => handleWeaponItemChange(idx, 'damage', parseInt(e.target.value) || 0)}
                        className="w-8 text-center bg-zinc-900 border border-blue-900/30 rounded"
                      />
                    </div>
                    <div className="flex items-center gap-1.5">
                      <span className="text-[9px] text-blue-700 font-bold font-serif uppercase">Ferim.:</span>
                      <input
                        type="number"
                        value={weapon.injury}
                        onChange={(e) => handleWeaponItemChange(idx, 'injury', parseInt(e.target.value) || 0)}
                        className="w-8 text-center bg-zinc-900 border border-blue-900/30 rounded"
                      />
                    </div>
                    <div className="flex items-center gap-1.5">
                      <span className="text-[9px] text-blue-700 font-bold font-serif uppercase">Grupo:</span>
                      <input
                        type="text"
                        value={weapon.group}
                        onChange={(e) => handleWeaponItemChange(idx, 'group', e.target.value)}
                        className="w-16 bg-zinc-900 border border-blue-900/30 rounded px-1 text-[10px]"
                      />
                    </div>
                    <label className="flex items-center gap-1 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={weapon.active}
                        onChange={(e) => handleWeaponItemChange(idx, 'active', e.target.checked)}
                        className="accent-blue-500 cursor-pointer"
                      />
                      <span className="text-[10px] text-blue-500 font-bold font-serif uppercase">Ativo</span>
                    </label>
                    <button
                      onClick={() => handleRemoveWeapon(idx)}
                      className="text-blue-800 hover:text-red-500 cursor-pointer p-0.5 ml-1"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Card 5: Recursos & Inventário */}
          <div className="bg-zinc-950/90 border-2 border-blue-900 p-4 rounded-md ambient-glow relative">
            <div className="absolute top-2 left-2 w-3 h-3 border-l-2 border-t-2 border-blue-700/60"></div>
            
            <div className="flex items-center gap-2 border-b border-blue-900/40 pb-2 mb-4">
              <Coins className="w-4 h-4 text-blue-500" />
              <span className="text-xs font-black text-blue-500 font-serif tracking-wider">RECURSOS DO VIAJANTE</span>
            </div>

            <div className="grid grid-cols-5 gap-2 text-center text-xs mb-6">
              <div className="p-1.5 bg-blue-950/10 border border-blue-900/30 rounded">
                <div className="text-[8px] text-blue-600 font-bold font-serif">MOEDAS</div>
                <input
                  type="number"
                  value={sheet.resources.coins}
                  onChange={(e) => handleResourceChange('coins', parseInt(e.target.value) || 0)}
                  className="w-full bg-transparent text-center font-bold text-blue-200 text-sm focus:outline-none"
                />
              </div>
              <div className="p-1.5 bg-blue-950/10 border border-blue-900/30 rounded">
                <div className="text-[8px] text-blue-600 font-bold font-serif">TESOURO</div>
                <input
                  type="number"
                  value={sheet.resources.treasure}
                  onChange={(e) => handleResourceChange('treasure', parseInt(e.target.value) || 0)}
                  className="w-full bg-transparent text-center font-bold text-blue-200 text-sm focus:outline-none"
                />
              </div>
              <div className="p-1.5 bg-blue-950/10 border border-blue-900/30 rounded">
                <div className="text-[8px] text-blue-600 font-bold font-serif">EXP.</div>
                <input
                  type="number"
                  value={sheet.resources.experience}
                  onChange={(e) => handleResourceChange('experience', parseInt(e.target.value) || 0)}
                  className="w-full bg-transparent text-center font-bold text-blue-200 text-sm focus:outline-none"
                />
              </div>
              <div className="p-1.5 bg-blue-950/10 border border-blue-900/30 rounded">
                <div className="text-[8px] text-blue-600 font-bold font-serif">P. AVENTURA</div>
                <input
                  type="number"
                  value={sheet.resources.adventurePoints}
                  onChange={(e) => handleResourceChange('adventurePoints', parseInt(e.target.value) || 0)}
                  className="w-full bg-transparent text-center font-bold text-blue-200 text-sm focus:outline-none"
                />
              </div>
              <div className="p-1.5 bg-blue-950/10 border border-blue-900/30 rounded">
                <div className="text-[8px] text-blue-600 font-bold font-serif">P. PERÍCIA</div>
                <input
                  type="number"
                  value={sheet.resources.skillPoints}
                  onChange={(e) => handleResourceChange('skillPoints', parseInt(e.target.value) || 0)}
                  className="w-full bg-transparent text-center font-bold text-blue-200 text-sm focus:outline-none"
                />
              </div>
            </div>

            <div className="mb-3 border-b border-blue-900/50 pb-3">
              <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-1.5 text-xs font-bold uppercase text-blue-300 font-serif">
                  <Package className="h-4 w-4" />
                  <span>Mochila e equipamento</span>
                </div>
                <div className="rounded border border-blue-900/60 bg-blue-950/30 px-2 py-1 text-[10px] text-blue-200">
                  Carga total: <strong>{sheet.attributes.load}</strong>
                </div>
              </div>
              <div className="grid grid-cols-1 gap-2 sm:grid-cols-[150px_1fr_auto]">
                <select
                  value={itemCategory}
                  onChange={event => { setItemCategory(event.target.value); setShowCatalog(true); }}
                  className="rounded border border-slate-700 bg-slate-950 px-2 py-2 text-xs text-slate-200"
                >
                  <option value="Todos">Todas as categorias</option>
                  {ITEM_CATEGORIES.map(category => <option key={category} value={category}>{category}</option>)}
                </select>
                <div className="relative">
                  <Search className="absolute left-2 top-2.5 h-3.5 w-3.5 text-slate-500" />
                  <input
                    type="text"
                    placeholder="Buscar espada, provisões, corda…"
                    value={newItemName}
                    onFocus={() => setShowCatalog(true)}
                    onChange={event => { setNewItemName(event.target.value); setShowCatalog(true); }}
                    className="w-full rounded border border-slate-700 bg-slate-950 py-2 pl-8 pr-2 text-xs text-slate-100 focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <button onClick={handleAddItem} className="medieval-button flex items-center justify-center gap-1.5 px-3">
                  <Plus className="h-3.5 w-3.5" /> Adicionar
                </button>
              </div>
              {showCatalog && (
                <div className="relative mt-2 max-h-64 overflow-y-auto rounded border border-blue-900/70 bg-[#050a13] p-2 shadow-2xl">
                  <button onClick={() => setShowCatalog(false)} className="sticky left-full top-0 z-10 rounded p-1 text-slate-500 hover:text-white" title="Fechar catálogo"><X className="h-4 w-4" /></button>
                  {filteredCatalog.length ? filteredCatalog.map(catalogItem => (
                    <button
                      key={catalogItem.id}
                      onClick={() => addCatalogItem(catalogItem)}
                      className="mb-1 grid w-full grid-cols-[1fr_auto] gap-3 rounded border border-transparent px-3 py-2 text-left hover:border-blue-800 hover:bg-blue-950/30"
                    >
                      <span>
                        <strong className="block text-xs text-blue-100">{catalogItem.name}</strong>
                        <span className="text-[10px] text-slate-500">{catalogItem.category} · {catalogItem.description}</span>
                      </span>
                      <span className="self-center rounded bg-slate-900 px-2 py-1 text-[10px] text-blue-300">Carga {catalogItem.load}</span>
                    </button>
                  )) : (
                    <button onClick={handleAddItem} className="w-full rounded border border-dashed border-slate-700 p-3 text-xs text-slate-400">
                      Criar “{newItemName || 'item personalizado'}” como item personalizado
                    </button>
                  )}
                </div>
              )}
            </div>

            <div className="space-y-2">
              {sheet.inventory.map((item, idx) => (
                <div key={item.id} className="p-2 bg-zinc-900/30 border border-blue-900/20 hover:border-blue-900/40 rounded grid grid-cols-1 md:grid-cols-12 gap-2 text-xs items-center">
                  <div className="md:col-span-4 flex items-center gap-1.5">
                    <Package className="w-3.5 h-3.5 text-blue-700" />
                    <input
                      type="text"
                      value={item.name}
                      onChange={(e) => handleInventoryItemChange(idx, 'name', e.target.value)}
                      className="bg-transparent text-blue-200 font-bold focus:outline-none border-b border-transparent focus:border-blue-600 py-0.5"
                    />
                  </div>
                  <div className="md:col-span-2 flex items-center gap-1">
                    <span className="text-[9px] text-blue-700 uppercase font-serif font-bold">Qtd:</span>
                    <input
                      type="number"
                      value={item.quantity}
                      onChange={(e) => handleInventoryItemChange(idx, 'quantity', parseInt(e.target.value) || 0)}
                      className="w-10 text-center bg-zinc-900 border border-blue-900/30 rounded"
                    />
                  </div>
                  <div className="md:col-span-2 flex items-center gap-1">
                    <span className="text-[9px] text-blue-400 uppercase font-serif font-bold">Carga:</span>
                    <input
                      type="number"
                      value={item.load ?? item.weight}
                      onChange={(e) => handleInventoryItemChange(idx, 'load', parseFloat(e.target.value) || 0)}
                      className="w-10 text-center bg-zinc-900 border border-blue-900/30 rounded"
                    />
                    
                  </div>
                  <div className="md:col-span-2">
                    <select
                      value={item.category}
                      onChange={(e) => handleInventoryItemChange(idx, 'category', e.target.value)}
                      className="bg-zinc-900 border border-blue-900/30 text-[10px] text-blue-300 rounded p-1 cursor-pointer w-full"
                    >
                      {ITEM_CATEGORIES.map(category => <option key={category} value={category}>{category}</option>)}
                    </select>
                  </div>
                  <div className="md:col-span-1 text-center">
                    <label className="flex items-center justify-center gap-1 cursor-pointer" title="Equipado">
                      <input
                        type="checkbox"
                        checked={item.equipped}
                        onChange={(e) => handleInventoryItemChange(idx, 'equipped', e.target.checked)}
                        className="accent-blue-500 cursor-pointer"
                      />
                      <span className="text-[8px] text-blue-600 font-bold font-serif uppercase">Eq</span>
                    </label>
                  </div>
                  <div className="md:col-span-1 text-center">
                    <button
                      onClick={() => handleRemoveItem(item.id)}
                      className="text-blue-800 hover:text-red-500 cursor-pointer p-0.5"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Card 6: Virtudes, Recompensas e Outros */}
          <div className="bg-zinc-950/90 border-2 border-blue-900 p-4 rounded-md ambient-glow relative text-xs">
            <div className="absolute top-2 left-2 w-3 h-3 border-l-2 border-t-2 border-blue-700/60"></div>
            
            <div className="flex items-center gap-2 border-b border-blue-900/40 pb-2 mb-4">
              <Sparkles className="w-4 h-4 text-blue-500" />
              <span className="text-xs font-black text-blue-500 font-serif tracking-wider">VIRTUDES, RECOMPENSAS & CONDIÇÕES NARRATIVAS</span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="text-[11px] text-blue-600 font-semibold uppercase font-serif block mb-1">Virtudes</label>
                <textarea
                  rows={2}
                  value={sheet.other.virtues}
                  onChange={(e) => handleOtherChange('virtues', e.target.value)}
                  className="w-full px-2 py-1.5 bg-zinc-900/60 border border-blue-900/30 rounded focus:outline-none resize-none"
                  placeholder="Virtudes adquiridas..."
                />
                {sourceBoundVirtues.length>0&&<div className="mt-2 rounded border border-cyan-900/50 bg-cyan-950/10 p-2">
                  <div className="mb-1 flex items-center justify-between gap-2"><span className="text-[9px] font-bold uppercase tracking-wide text-cyan-300">Opções culturais 2e — suplemento</span><span className="text-[9px] text-slate-500">Sabedoria {wisdom}</span></div>
                  <div className="space-y-1">{sourceBoundVirtues.map(virtue=><button key={virtue.id} type="button" disabled={!mayAddCulturalVirtue||currentVirtueNames.includes(virtue.name)} onClick={()=>addCulturalVirtue(virtue.name)} title={virtue.summary} className="flex min-h-[44px] w-full items-center justify-between gap-2 rounded border border-cyan-900/40 px-2 py-1.5 text-left text-[10px] text-cyan-100 disabled:cursor-not-allowed disabled:opacity-40"><span><b>{virtue.name}</b><span className="ml-1 text-slate-400">— {virtue.summary}</span></span><span className="shrink-0 text-[8px] uppercase text-cyan-500">{virtue.sourceId==='tor2e-rivendell'?'Valfenda':'Terras Ermas'}</span></button>)}</div>
                  {!mayAddCulturalVirtue&&<p className="mt-1 text-[9px] text-slate-500">Aumente Sabedoria antes de adquirir outra Virtude.</p>}
                </div>}
              </div>
              <div>
                <label className="text-[11px] text-blue-600 font-semibold uppercase font-serif block mb-1">Recompensas & Armas Nomeadas</label>
                <textarea
                  rows={2}
                  value={sheet.other.rewards}
                  onChange={(e) => handleOtherChange('rewards', e.target.value)}
                  className="w-full px-2 py-1.5 bg-zinc-900/60 border border-blue-900/30 rounded focus:outline-none resize-none"
                  placeholder="Relíquias antigas ou saques heróicos..."
                />
              </div>
              <div>
                <label className="text-[11px] text-blue-600 font-semibold uppercase font-serif block mb-1">Falhas Místicas / Sombra na Alma</label>
                <textarea
                  rows={2}
                  value={sheet.other.flaws}
                  onChange={(e) => handleOtherChange('flaws', e.target.value)}
                  className="w-full px-2 py-1.5 bg-zinc-900/60 border border-blue-900/30 rounded focus:outline-none resize-none"
                  placeholder="Sussurros do Um Anel, tentações ou cicatrizes profundas..."
                />
              </div>
              <div>
                <label className="text-[11px] text-blue-600 font-semibold uppercase font-serif block mb-1">Condições Narrativas & Notas de Viagem</label>
                <textarea
                  rows={2}
                  value={sheet.other.narrativeConditions}
                  onChange={(e) => handleOtherChange('narrativeConditions', e.target.value)}
                  className="w-full px-2 py-1.5 bg-zinc-900/60 border border-blue-900/30 rounded focus:outline-none resize-none"
                  placeholder="Ex: Promessa selada com Elrond, Juramento de vingança..."
                />
              </div>
            </div>
          </div>

          {/* Card 7: Diário de Alterações (Audit trail) */}
          <div className="bg-zinc-950/60 border border-blue-900/30 p-4 rounded text-xs relative">
            <div className="flex items-center gap-2 border-b border-blue-900/40 pb-2 mb-3">
              <ClipboardList className="w-4 h-4 text-blue-600" />
              <span className="text-[10px] font-bold text-blue-500 font-serif">LOG HISTÓRICO DE MUTAÇÕES DA FICHA (APPEND-ONLY)</span>
            </div>
            <div className="space-y-1.5 max-h-[160px] overflow-y-auto pr-1 scrollbar-thin">
              {sheet.historyLog.length === 0 ? (
                <p className="text-blue-800 text-[10px] italic">A ficha permanece em seu estado puro primordial.</p>
              ) : (
                sheet.historyLog.map((log, index) => (
                  <div key={index} className="py-1 border-b border-blue-950/10 flex flex-col md:flex-row justify-between items-start md:items-center text-[10px] gap-1">
                    <span className="text-blue-700 font-bold shrink-0">{new Date(log.timestamp).toLocaleTimeString()} {new Date(log.timestamp).toLocaleDateString()}</span>
                    <span className="text-blue-200">Alterou <strong className="text-blue-400 font-bold">{log.field}</strong></span>
                    <span className="text-blue-700/80 truncate max-w-[200px]" title={`Anterior: ${log.oldValue} -> Novo: ${log.newValue}`}>
                      De [{log.oldValue}] para [{log.newValue}]
                    </span>
                    <span className="bg-blue-950/30 px-1.5 py-0.5 rounded border border-blue-900/20 text-blue-400 font-semibold italic text-[9px] truncate max-w-[150px]">
                      {log.reason}
                    </span>
                  </div>
                ))
              )}
            </div>
          </div>

        </div>
      </div>
      {/* Shared Token Picker Modal */}
      <TokenPicker
        isOpen={isPickerOpen}
        onClose={() => setIsPickerOpen(false)}
        selectedTokenId={sheet.tokenId}
        culture={sheet.identity.culture}
        playerOnly
        onSelect={handleSelectToken}
      />

    </div>
  );
};
