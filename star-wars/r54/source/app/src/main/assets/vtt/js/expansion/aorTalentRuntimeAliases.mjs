/**
 * P132 — Explicit AoR Core -> already wired EotE runtime talent identifiers.
 *
 * The AoR source tree stores printed Spanish names while the legacy combat,
 * skill, vehicle and recovery engines use short EotE keys. These mappings
 * identify source semantics and runtime keys; the separate
 * AOR_RUNTIME_MECHANICAL_TALENTS list gates catalog claims of an integrated
 * consumer. BRACE is source-backed for rank accounting but stays excluded
 * from that list until its maneuver and one-action consumer are wired.
 * Mappings do not grant effects from display-label similarity.
 */
export const AOR_RUNTIME_TALENT_ALIASES = Object.freeze({
  // The source catalog uses FIRMEZA for the active Brace talent, not Grit.
  // Keep it distinct from APLOMO, which is the source-backed Grit key.
  'FIRMEZA': 'BRACE',
  'SEXTO SENTIDO': 'SIXTHSENSE',
  'REFLEJOS SUPERIORES': 'SUPREF',
  'PRECISIÓN LETAL': 'DEADACC',
  'ACELERACIÓN MÁXIMA': 'FULLTH',
  'ACELERACIÓN MÁXIMA MEJORADA': 'FULLTHIMP',
  'ACELERACIÓN MÁXIMA SUPREMA': 'FULLTHSUP',
  'PUESTA A PUNTO': 'FINETUN',
  'PILOTO CONSUMADO': 'SKILLJOCK',
  'REACCIÓN RÁPIDA': 'RAPREA',
  'CONDUCCIÓN DEFENSIVA': 'DEFDRI',
  'CARTÓGRAFO GALÁCTICO': 'GALMAP',
  'RECUPERACIÓN RÁPIDA': 'RAPREC',
  'PILOTO NATO': 'NATPIL',
  'MANDO': 'COM',
  'ATAQUE RÁPIDO': 'QUICKST',
  'DESENFUNDADO RÁPIDO': 'QUICKDR',
  'VELOZ': 'SWIFT',
  'ESQUIVA': 'DODGE',
  'NEGACIÓN PLAUSIBLE': 'PLAUSDEN',
  'INTIMIDANTE': 'INTIM',
  'INSTINTO CALLEJERO': 'STRSMART',
  'ACTITUD CONVINCENTE': 'CONV',
  'NEGOCIADOR NATO': 'NATNEG',
  'ENTEREZA': 'RESOLVE',
  'FUERZAS DE FLAQUEZA': 'SECWIND',
  'A QUEMARROPA': 'POINTBL',
  'SUPERVIVIENTE NATO': 'NATOUT',
  // AoR Tactician r3c1 (p.75): Natural Outdoorsman, not the Soldier signature Survivor.
  'SUPERVIVIENTE': 'NATOUT',
  // AoR Saboteur r3c1/r4c1 and Medic r5c3: reactive, source-checked consumers.
  'CUERPO A TIERRA': 'TIME_TO_GO',
  'CUERPO A TIERRA MEJORADO': 'TIME_TO_GO_IMPROVED',
  'NO ES PARA TANTO': 'ITS_NOT_THAT_BAD',
  'CIRUJANO': 'SURG',
  'MÉDICO NATO': 'NATDOC',
  'GOLPES LETALES': 'LETHALBL',
  'TIRADOR NATO': 'NATMAR',
  'POSTURA DEFENSIVA': 'DEFSTA',
  'CAMORRISTA NATO': 'NATBRAW',
  'CAZADOR NATO': 'NATHUN',
  'PROGRAMADOR NATO': 'NATPRO',
  // P135: source-backed AoR names whose effects already have EotE consumers.
  // No blanket equivalence: only the same ranked/activated mechanical talent.
  'CONCENTRACIÓN INTENSA': 'INTFOC',
  'LECCIONES DE ANATOMÍA': 'ANAT',
  'FUERZA SALVAJE': 'FERSTR',
  'ATAQUE FURIOSO': 'FRENZ',
  'GOLPE DIRIGIDO': 'TARGBL',
  'DISPARO DE PRECISIÓN': 'SNIPSHOT',
  'DOMINIO DE BLINDAJES': 'ARM',
  'DOMINIO DE BLINDAJES MEJORADO': 'ARMIMP',
  'ACECHADOR': 'STALK',
  'ENTRENAMIENTO FÍSICO': 'PHYTRAIN',
  'DELINCUENTE NATO': 'NATROG',
  'ERUDITO NATO': 'NATSCH',
  'MATÓN NATO': 'NATENF',
  'CHAPUCERO NATO': 'NATTIN',
  'PIRATA INFORMÁTICO CONSUMADO': 'SKILSLI',
  // AoR Jump Up / EotE Jump Up: the Android combat action already consumes JUMP.
  'LEVANTARSE DE UN SALTO': 'JUMP',
  // P137: core talents with verified, executable effects in rules modules.
  // Do not map SUBIR DE UN SALTO (vehicle boarding) to JUMP (standing up).
  'REPARACIÓN EFICAZ': 'SOLREP',
  'ROBUSTO': 'DURA',
  'CONFIANZA': 'CONF',
  'DISPARO CERTERO': 'TRUEAIM',
  'ELUDIR SEGURIDAD': 'BYP',
  'ESPECIALISTA EN ESTIMULANTES': 'STIMSPEC',
  // P138: ranked time/skill talents, activated by the context-aware skill engine.
  'INVESTIGADOR': 'RESEARCHER',
  'RASTREADOR EXPERTO': 'EXPTRACK',
  'APTITUD TECNOLÓGICA': 'TECHAPT',
  // P141: Tinkerer — each purchased rank can upgrade one distinct owned item.
  'MAÑOSO': 'TINK',
  // P144: explicit context-gated AoR talent consumers (never global bonuses).
  'DOTES DE MANDO': 'COMMANDPRES',
  'OBSERVADOR': 'KEENEYED',
  'DESCIFRADOR': 'CODEBREAKER',
  'MONTARAZ': 'OUTDOORSMAN',
  // P149: source-backed defensive social and on-hit Disorient effects.
  'NI UN PELO DE TONTO': 'NOBODYSFOOL',
  'DESORIENTAR': 'DISOR',
  // P150: AoR Slicer and pilot passive defense, both contextual.
  'PROGRAMACIÓN DEFENSIVA': 'DEFSLI',
  'PROGRAMACIÓN DEFENSIVA MEJORADA': 'DEFSLIIMP',
  'ESCURRIDIZO': 'TRICK',
  // P151: contextual, source-specific skill and NPC effects.
  'ATAJO': 'SHORTCUT',
  'INVENTOR': 'INVENTOR',
  'ERUDITO RESPETADO': 'RESSCH',
  'HABLAR BINARIO': 'SPEAKBIN',
  'CONDUCTOR TODOTERRENO': 'ALLTERR',
  'CONDUCTOR NATO': 'NATDRI',
  'ASTRONAVEGANTE EXPERTO': 'MASSTAR',
  // P154: ranked Blast damage, thrown-range extension and Triumph-based knockdown.
  'EXPLOSIÓN POTENTE': 'POWERBLAST',
  'BRAZO FUERTE': 'STRONGARM',
  'DERRIBO': 'KNOCKDOWN_TALENT',
  // P155: Utility Belt — one Destiny flip to reveal small undocumented gear.
  'CINTURÓN DE ACCESORIOS': 'UTILITYBELT',
  // P157: verified AoR Core trees (Diplomat and Commander), contextual consumers.
  'DON DE GENTES': 'KILL',
  'INDISTINGUIBLE': 'INDISTINGUISHABLE',
  'CHANCHULLERO': 'WHEELDEAL',
  'FINTA': 'FEINT',
  // P158: verified AoR core: selective explosive safety, activation cost, and field hazards.
  'DETONACIÓN SELECTIVA': 'SELECTDET',
  'GRANADERO EXPERTO': 'MASTERGREN',
  'ACLIMATACIÓN': 'ACCLIM',
  'RESISTENCIA A VENENOS': 'POISONRES',
  // P159: Natural Charmer and Natural Leader share the established once/session reroll ledger.
  'EMBAUCADOR NATO': 'NATCHARM',
  'LÍDER NATO': 'NATLEAD',
  // P160: verified AoR Core counterparts, with real ammunition, pilot and critical consumers.
  'MUNICIÓN DE RESERVA': 'SPARECL',
  'PILOTO EXPERTO': 'SKILLJOCK',
  'MADERA DE HÉROE': 'HERFORT',
  // P161: source-backed check suppression, boarding, knowledge and acquisition.
  'NERVIOS DE ACERO': 'STEELYNERVES',
  'SUBIR DE UN SALTO': 'LETSRIDE',
  'CONOCIMIENTO ESPECIALIZADO': 'KNOWSPEC',
  'PROVEEDOR HABITUAL': 'KNOWSOMEBODY',
  // P162: session income and Triumph-spend social checks, source-backed.
  'INVERSIONES RENTABLES': 'SOUNDINVEST',
  'LABIA': 'SMOOTHTALK',
  // P163: AoR Medic, career breadth and vehicle Full Stop.
  'ESPECIALISTA EN BACTA': 'BACTASPEC',
  'APLICACIÓN DE ESTIMULANTES': 'STIMAPP',
  'APLICACIÓN DE ESTIMULANTES MEJORADA': 'STIMAPPIMP',
  'APLICACIÓN DE ESTIMULANTES SUPREMA': 'STIMAPPSUP',
  'MÉDICO EXPERTO': 'MASDOC',
  'POLIFACÉTICO': 'WELLROUND',
  'PARADA EN SECO': 'FULLSTOP',
  // P168: AoR Diplomat, six source-backed action/improved/supreme nodes.
  'RETÓRICA INSPIRADORA': 'INSPIRINGRHET',
  'RETÓRICA INSPIRADORA MEJORADA': 'INSPIRINGRHETIMP',
  'RETÓRICA INSPIRADORA SUPREMA': 'INSPIRINGRHETSUP',
  'LENGUA AFILADA': 'SCATHINGTIRADE',
  'LENGUA AFILADA MEJORADA': 'SCATHINGTIRADEIMP',
  'LENGUA AFILADA SUPREMA': 'SCATHINGTIRADESUP',
  'PRESENCIA IMPONENTE': 'INTENSEPRES',
  // P168: AoR Commodore/Mechanic Solid Repairs, source-backed hull recovery.
  'DE UNA PIEZA': 'SOLREP',
  // P167: Mental Fortress suppresses only Intellect/Cunning critical check penalties.
  'FORTALEZA MENTAL': 'MENTALFORT',
  // P169: Master of Shadows is consumed by the existing real stealth skill
  // check (2 strain, difficulty -1, once per round when in combat).
  'SOMBRA FURTIVA': 'MASSHAD',
  // P167: Familiar Suns is a once-per-session Hard Knowledge planetary survey.
  'SOLES FAMILIARES': 'FAMILIARSUNS',
  // P166: Gearhead is a ranked Mechanics setback remover and halves attachment Mod cost.
  'MANITAS': 'GEARHD',
  // P165: AoR Gunner — explicit post-attack Advantage spends.
  'ABRUMAR DEFENSAS': 'OVERWHELMDEF',
  'DISPARO DEBILITANTE': 'DEBILSHOT',
  // P170: real ranked consumers, including improved strain recovery.
  'TENACIDAD': 'HARDHD',
  'TENACIDAD MEJORADA': 'HARDHDIMP',
  // P170: permanent source-backed gear assignment and live attack modifiers.
  'MEJORAS CASERAS': 'JURY',
  // P170: ranked protection of an engaged companion during incoming attacks.
  'GUARDAESPALDAS': 'BOD',
  // P172: 2 Tensão antes de Negociação, +25% venda ou -25% compra em sucesso.
  'MERCADER EXPERTO': 'MASTMERCH',
  // P175: source-backed AoR command actions consumed by live companion/skill checks.
  'COORDINACIÓN TÁCTICA': 'FIELD_COMMANDER',
  'COORDINACIÓN TÁCTICA MEJORADA': 'FIELD_COMMANDER_IMPROVED',
  'LÍDER EXPERTO': 'MASTER_LEADER',
  'ATAQUE COORDINADO': 'COORDINATED_ASSAULT',
  'EVASIÓN IMPECABLE': 'BRILLIANT_EVASION',
  'CONOCIMIENTO DEL ENTORNO': 'SITUATIONAL_AWARENESS',
  'SOLUCIÓN INGENIOSA': 'CLEVER_SOLUTION',
  // P176: independent AoR Core actions with real consumers.
  'PIRATA INFORMÁTICO EXPERTO': 'MASSLIC',
  'FUEGO CONTROLADO': 'FIRE_CONTROL',
  'SALIDA TÉRMICA': 'EXHAUST_PORT',
  'IMPACTO DIRECTO': 'DEAD_TO_RIGHTS',
  'IMPACTO DIRECTO MEJORADO': 'DEAD_TO_RIGHTS_IMPROVED',
  'DISEÑO FAMILIAR': 'KNOWN_SCHEMATIC',
  'COMPRAR INFORMACIÓN': 'BOUGHT_INFO',
  // P177: independently wired talents; no catalog-only coverage.
  'ARTILUGIO': 'CONTRAPTION',
  'METICULOSO': 'CAREFUL_PLANNING',
  'INCITAR A LA REBELIÓN': 'INCITE_REBELLION',
  'COMPARTIMENTO OCULTO': 'HIDDEN_STORAGE',
  'UNTAR MANOS': 'GREASED_PALMS',
  'GOLPE CONMOCIONADOR': 'STUNBL',
  'GOLPE CONMOCIONADOR MEJORADO': 'STUNBLIMP',
  'PUNTO DÉBIL': 'SOFT_SPOT',
  // P178: attribute substitutions, real critical immunity, and crippling hits.
  'FORMACIÓN DE COMBATE': 'FORM_ON_ME',
  'MANO DE SANTO': 'WORKS_LIKE_A_CHARM',
  'INGENIOSO': 'STROKE_OF_GENIUS',
  'SOLUCIÓN IMPROVISADA': 'QUICK_FIX',
  'IMPARABLE': 'UNSTOPPABLE',
  'GOLPE INCAPACITANTE': 'CRIPPLING_BLOW',
  // P174: source-backed, once/session Mechanic equipment actions.
  'SISTEMAS REDUNDANTES': 'REDUNDANT_SYS',
  'PIEZA DEFECTUOSA': 'BAD_MOTIVATOR'
});

export const AOR_IMMEDIATE_TALENT_EFFECTS = Object.freeze([
  'APLOMO', 'ENDURECIDO', 'RESISTENTE', 'DEDICACIÓN',
  'SEXTO SENTIDO', 'REFLEJOS SUPERIORES', 'PRECISIÓN LETAL'
]);

export const AOR_RUNTIME_MECHANICAL_TALENTS = Object.freeze([
  // BRACE has a deterministic rank/relief helper, but remains catalog-inactive
  // until its maneuver and one-action controller consumer are integrated.
  ...new Set([...AOR_IMMEDIATE_TALENT_EFFECTS,
    ...Object.keys(AOR_RUNTIME_TALENT_ALIASES).filter(name=>AOR_RUNTIME_TALENT_ALIASES[name]!=='BRACE')])
]);

export function aorCanonicalTalentName(originalName) {
  return AOR_RUNTIME_TALENT_ALIASES[originalName] ?? originalName;
}
