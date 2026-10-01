export function classifyTokenFilename(filename: string): string {
  const name = filename.toLowerCase();

  if (name.includes('hobbit') || name.includes('halfling') || name.includes('bolseiro') || name.includes('baggins') || name.includes('bilbo') || name.includes('frodo') || name.includes('samwise') || name.includes('pipin') || name.includes('merry')) {
    return 'hobbits';
  }
  if (name.includes('barding') || name.includes('bardo') || name.includes('vale') || name.includes('esgaroth') || name.includes('dale')) {
    return 'bardings';
  }
  if (name.includes('beorn') || name.includes('beorning') || name.includes('urso') || name.includes('bear')) {
    return 'beornings';
  }
  if (name.includes('anão') || name.includes(' dwarf') || name.includes('dwarves') || name.includes('gimli') || name.includes('thorin') || name.includes('balin')) {
    return 'anões';
  }
  if (name.includes('elf') || name.includes('elfo') || name.includes('legolas') || name.includes('elrond') || name.includes('galadriel') || name.includes('thranduil') || name.includes('lothlorien') || name.includes('mirkwood')) {
    return 'elfos';
  }
  if (name.includes('warg') || name.includes('direwolf')) {
    return 'wargs';
  }
  if (name.includes('lobo') || name.includes('wolf') || name.includes('wolves')) {
    return 'lobos';
  }
  if (name.includes('aranha') || name.includes('spider') || name.includes('shelob') || name.includes('ungoliant')) {
    return 'aranhas';
  }
  if (name.includes('homens das terras ermas') || name.includes('woodman') || name.includes('woodmen') || name.includes('radagast')) {
    return 'homens das terras ermas';
  }
  if (name.includes('orc') || name.includes('goblin') || name.includes('sauron') || name.includes('saruman') || name.includes('morgoth') || name.includes('nazgul') || name.includes('cavaleiro negro') || name.includes('evil') || name.includes('mal')) {
    return 'homens malignos';
  }
  if (name.includes('taverna') || name.includes('pub') || name.includes('inn') || name.includes('poney') || name.includes('cerveja') || name.includes('beer')) {
    return 'taverna';
  }
  if (name.includes('trabalhador') || name.includes('ferreiro') || name.includes('smith') || name.includes('miner') || name.includes('peasant')) {
    return 'trabalhadores';
  }
  if (name.includes('musico') || name.includes('músico') || name.includes('harpa') || name.includes('cantor') || name.includes('flute') || name.includes('bard_music')) {
    return 'músicos';
  }
  if (name.includes('misterioso') || name.includes('sombra') || name.includes('shadow') || name.includes('hood') || name.includes('ranger') || name.includes('passos-largos') || name.includes('strider')) {
    return 'misteriosos';
  }
  if (name.includes('criatura') || name.includes('monstro') || name.includes('troll') || name.includes('gollum') || name.includes('smaug') || name.includes('dragon') || name.includes('dragão')) {
    return 'criaturas';
  }
  
  return 'personagens personalizados';
}
