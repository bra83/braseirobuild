from __future__ import annotations

from copy import deepcopy
import re

FAMILIARITY_LEVELS = ('ZERO', 'BASIC', 'FAMILIAR', 'EXPERT')


class NarrativePresentationPolicy:
    """Cross-system world-literacy contract.

    The core decides *when* a rich introduction is mandatory and what categories
    must be covered. System lore itself remains host/RAG/Atlas owned.
    """

    OCCASIONS = {
        'continuation', 'campaign_opening', 'first_arrival',
        'first_visit_settlement', 'first_visit_region', 'first_visit_landmark',
        'enter_new_geographic_area', 'changed_return', 'long_absence_return',
        'major_political_transition',
    }
    DEPTH = {
        'continuation': ('NORMAL', (350, 900), 1),
        'campaign_opening': ('EPIC', (1800, 4200), 7),
        'first_arrival': ('RICH', (1000, 2400), 4),
        'first_visit_settlement': ('EXTENDED', (1400, 3200), 6),
        'first_visit_region': ('EXTENDED', (1400, 3200), 6),
        'first_visit_landmark': ('RICH', (900, 2200), 4),
        'enter_new_geographic_area': ('EXTENDED', (1200, 2800), 5),
        'changed_return': ('RICH', (800, 2000), 3),
        'long_absence_return': ('RICH', (900, 2200), 4),
        'major_political_transition': ('EXTENDED', (1200, 3000), 5),
    }
    CORE_SECTIONS = (
        'immediate_scene', 'sensory_detail', 'immediate_situation', 'agency_return'
    )
    INTRO_SECTIONS = (
        'geographic_orientation', 'region_context', 'political_context',
        'cultural_context', 'historical_context', 'nearby_places',
        'character_knowledge', 'current_conditions',
    )
    SETTLEMENT_KINDS = {
        'city','town','village','settlement','metropolis','capital','hamlet','district',
        'cidade','vila','aldeia','assentamento','metropole','metrópole','capital','bairro',
        'station','starport','spaceport','colony','habitat','outpost','base',
        'estacao','estação','porto espacial','colonia','colônia','habitat','posto avançado',
    }
    LANDMARK_KINDS = {
        'landmark','ruin','ruins','fortress','castle','temple','dungeon','monument','tower',
        'ruina','ruína','fortaleza','castelo','templo','masmorra','monumento','torre',
        'derelict','wreck','anomaly','asteroid base','orbital facility',
        'naufrágio','naufragio','anomalia','base asteroide','instalação orbital','instalacao orbital',
    }

    def normalize_familiarity(self, value):
        level = str(value or 'ZERO').strip().upper()
        if level not in FAMILIARITY_LEVELS:
            raise ValueError('invalid_setting_familiarity')
        return level

    def familiarity_for(self, state):
        player_state = getattr(state, 'player_state', {}) or {}
        notes = getattr(state, 'notes', {}) or {}
        value = player_state.get('setting_familiarity', notes.get('setting_familiarity', 'ZERO'))
        return self.normalize_familiarity(value)

    def location_kind(self, state):
        if not getattr(state, 'location', ''):
            return ''
        site = (getattr(state, 'sites', {}) or {}).get(state.location, {})
        if not isinstance(site, dict):
            return ''
        return str(site.get('kind') or site.get('type') or site.get('location_type') or '').strip().lower()

    def region_id(self, state):
        if not getattr(state, 'location', ''):
            return ''
        site = (getattr(state, 'sites', {}) or {}).get(state.location, {})
        if not isinstance(site, dict):
            return ''
        region = site.get('region_id', site.get('region'))
        if isinstance(region, dict):
            region = region.get('id') or region.get('name')
        return str(region or '').strip()

    def classify_first_visit(self, state):
        kind = self.location_kind(state)
        if kind in self.SETTLEMENT_KINDS:
            return 'first_visit_settlement'
        if kind in self.LANDMARK_KINDS:
            return 'first_visit_landmark'
        return 'first_arrival'

    def build(self, state, occasion, *, context=None, evidence=None):
        if occasion not in self.OCCASIONS:
            raise ValueError('invalid_world_introduction_occasion')
        familiarity = self.familiarity_for(state)
        depth, target, min_paragraphs = self.DEPTH[occasion]
        context = context if isinstance(context, dict) else {}
        evidence = list(evidence or [])
        required = list(self.CORE_SECTIONS)
        available = self._available_sections(state, context, evidence)
        if occasion != 'continuation':
            for section in self.INTRO_SECTIONS:
                if section in available:
                    required.append(section)
        # Familiar players need less exposition, but scene/agency requirements never disappear.
        if familiarity == 'EXPERT' and occasion not in {'campaign_opening'}:
            required = [x for x in required if x not in {'historical_context','nearby_places'}]
        elif familiarity == 'FAMILIAR' and occasion not in {'campaign_opening'}:
            required = [x for x in required if x != 'historical_context']
        return {
            'occasion': occasion,
            'depth': depth,
            'target_chars': list(target),
            'minimum_paragraphs': min_paragraphs,
            'player_setting_familiarity': familiarity,
            'required_coverage': required,
            'available_coverage': sorted(available),
            'evidence_aware': True,
            'do_not_invent_missing_lore': True,
            'no_encyclopedia_dump': True,
            'explain_unfamiliar_proper_nouns': familiarity == 'ZERO',
            'story_before_choices': True,
        }

    def retrieval_terms(self, state, occasion):
        if occasion == 'continuation':
            return ''
        location = str(getattr(state, 'location', '') or '').strip()
        region = self.region_id(state)
        terms = [location, region, 'geografia região política cultura história contexto local lugares próximos situação atual']
        if occasion in {'campaign_opening','first_visit_region','enter_new_geographic_area'}:
            terms += ['mundo cenário nações fronteiras poder religião costumes conflitos']
        if occasion in {'first_visit_settlement','first_arrival'}:
            terms += ['governo comércio população facções bairros serviços tensões']
        return ' '.join(x for x in terms if x).strip()

    def _available_sections(self, state, context, evidence):
        available = set(self.CORE_SECTIONS)
        site = context if context else {}
        location = str(getattr(state, 'location', '') or context.get('location_id') or '')
        if location or site.get('terrain') or site.get('geometry') or site.get('connections'):
            available.add('geographic_orientation')
        region = self.region_id(state)
        if region or site.get('region') or site.get('region_id'):
            available.add('region_context')
        if site.get('landmarks') or site.get('connections') or site.get('nearby_places'):
            available.add('nearby_places')
        if site.get('weather') or site.get('physical_environment') or getattr(state, 'weather', None) or getattr(state, 'economy', None):
            available.add('current_conditions')
        public_blob = ' '.join(
            str(getattr(e, 'text', e.get('text','') if isinstance(e,dict) else ''))
            for e in evidence
        ).lower()
        site_blob = repr(site).lower()
        blob = public_blob + ' ' + site_blob
        if re.search(r'\b(pol[ií]tic|govern|autoridade|lei|jurisdi[cç][aã]o|administra[cç][aã]o|corpora[cç][aã]o|rei|rainha|lord|senhor|fac[cç][aã]o|faction|council|conselh|rep[uú]blic|imp[eé]rio|kingdom|government|authority|law|jurisdiction|administration|corporation)\w*', blob):
            available.add('political_context')
        if re.search(r'\b(cultur|sociedade|costum|tradi[cç]|relig|festival|dialet|idioma|povo|people|culture|society|custom|tradition|faith|language)\w*', blob):
            available.add('cultural_context')
        if re.search(r'\b(hist[oó]ri|guerra|fundad|antig|era|reinado|war|history|founded|ancient)\w*', blob):
            available.add('historical_context')
        if evidence or getattr(state, 'facts', None):
            available.add('character_knowledge')
        return available


class VisitMemory:
    """Presentation-memory stored inside the host-projected discovery document.

    It records what has actually been introduced, separately from canonical location
    truth. This prevents a rejected/unfinished first scene from consuming the intro.
    """

    LONG_ABSENCE_SECONDS = 30 * 86400

    def location_entry(self, state, *, create=False):
        location = str(getattr(state, 'location', '') or '')
        if not location:
            return None
        discovery = state.discovery
        locations = discovery.setdefault('locations', {}) if create else discovery.get('locations', {})
        if not isinstance(locations, dict):
            raise ValueError('invalid_discovery_locations')
        if create:
            return locations.setdefault(location, {})
        return locations.get(location)

    def region_entry(self, state, region_id, *, create=False):
        if not region_id:
            return None
        regions = state.discovery.setdefault('regions', {}) if create else state.discovery.get('regions', {})
        if not isinstance(regions, dict):
            raise ValueError('invalid_discovery_regions')
        if create:
            return regions.setdefault(region_id, {})
        return regions.get(region_id)

    def introduction_delivered(self, state):
        entry = self.location_entry(state, create=False)
        return bool(isinstance(entry, dict) and entry.get('introduction_delivered'))

    def long_absence(self, state):
        entry = self.location_entry(state, create=False)
        if not isinstance(entry, dict):
            return False
        previous = entry.get('last_seen_elapsed_seconds')
        current = (getattr(state, 'game_time', {}) or {}).get('elapsed_seconds')
        if not isinstance(previous, (int,float)) or isinstance(previous,bool):
            return False
        if not isinstance(current, (int,float)) or isinstance(current,bool):
            return False
        threshold = (getattr(state, 'player_state', {}) or {}).get('world_intro_long_absence_seconds', self.LONG_ABSENCE_SECONDS)
        try:
            threshold = float(threshold)
        except (TypeError, ValueError):
            threshold = float(self.LONG_ABSENCE_SECONDS)
        return current - previous >= max(0.0, threshold)

    def mark_visit(self, state, *, imprint, region_id=''):
        if not getattr(state, 'location', ''):
            return
        entry = self.location_entry(state, create=True)
        last_location = state.discovery.get('last_location_id')
        current = state.location
        if last_location != current:
            entry['visit_count'] = int(entry.get('visit_count', 0)) + 1
            entry.setdefault('first_visit_tick', state.tick)
            entry.setdefault('first_visit_elapsed_seconds', (state.game_time or {}).get('elapsed_seconds', 0))
        entry['last_seen_tick'] = state.tick
        entry['last_seen_elapsed_seconds'] = (state.game_time or {}).get('elapsed_seconds', 0)
        entry['imprint'] = imprint
        state.discovery['last_location_id'] = current
        if region_id:
            region = self.region_entry(state, region_id, create=True)
            region.setdefault('first_visit_tick', state.tick)
            region['last_seen_tick'] = state.tick

    def mark_introduction_delivered(self, state, occasion):
        if occasion == 'campaign_opening':
            state.discovery['campaign_introduction_delivered'] = True
        entry = self.location_entry(state, create=True) if getattr(state, 'location', '') else None
        if isinstance(entry, dict) and occasion != 'continuation':
            entry['introduction_delivered'] = True
            entry['introduction_occasion'] = occasion
            entry['introduction_tick'] = state.tick
        if occasion in {'campaign_opening','first_visit_region'} and getattr(state, 'location', ''):
            site=(getattr(state,'sites',{}) or {}).get(state.location,{})
            region=(site.get('region_id',site.get('region')) if isinstance(site,dict) else None)
            if isinstance(region,dict): region=region.get('id') or region.get('name')
            region=str(region or '').strip()
            if region:
                region_entry=self.region_entry(state,region,create=True)
                region_entry['introduction_delivered']=True
                region_entry['introduction_tick']=state.tick
        return True

    def receipt(self, state, occasion):
        return {
            'location_id': state.location or None,
            'occasion': occasion,
            'tick': state.tick,
            'state_version': state.state_version,
        }


class WorldIntroductionValidator:
    """Validates both depth and semantic coverage commitments.

    Providers that return structured coverage are checked strictly. Legacy providers
    remain compatible, but still face depth and evidence-anchor checks.
    """

    _SENSORY = re.compile(r'\b(ouve|ouvi|som|ru[ií]do|cheiro|odor|frio|calor|vento|chuva|luz|sombra|vê|vejo|avist|hear|sound|smell|cold|heat|wind|rain|light|shadow|see)\w*', re.I)
    _AGENCY = re.compile(r'\b(o que (?:você|voc[eê])|o que faz|como reage|sua decis[aã]o|você decide|you decide|what do you|your move)\b', re.I)
    _CATEGORY_PATTERNS = {
        'geographic_orientation': re.compile(r'\b(norte|sul|leste|oeste|nordeste|noroeste|sudeste|sudoeste|fronteir|costa|litoral|vale|rio|montanh|florest|estrada|rota|regi[aã]o|setor|subsetor|sistema|mundo|planeta|[oó]rbita|esta[cç][aã]o|porto espacial|north|south|east|west|border|coast|valley|river|mountain|forest|road|route|region|sector|subsector|system|world|planet|orbit|station|starport|spaceport)\w*', re.I),
        'region_context': re.compile(r'\b(regi[aã]o|terras|prov[ií]ncia|reino|dom[ií]nio|vale|costa|sert[aã]o|territ[oó]rio|setor|subsetor|sistema|region|lands|province|realm|domain|valley|coast|territory|sector|subsector|system)\w*', re.I),
        'political_context': re.compile(r'\b(pol[ií]tic|govern|autoridade|lei|jurisdi[cç][aã]o|administra[cç][aã]o|corpora[cç][aã]o|rei|rainha|coroa|conselh|senhor|lord|fac[cç][aã]o|rep[uú]blic|imp[eé]rio|duque|bar[aã]o|prefeit|magistr|government|authority|law|jurisdiction|administration|corporation|king|queen|crown|council|faction|republic|empire|duke|baron|mayor|magistrate)\w*', re.I),
        'cultural_context': re.compile(r'\b(cultur|sociedade|costum|tradi[cç]|relig|f[eé]|templo|festival|dialet|idioma|povo|ritual|deus|deusa|culture|society|custom|tradition|religion|faith|temple|festival|dialect|language|people|ritual|god|goddess)\w*', re.I),
        'historical_context': re.compile(r'\b(hist[oó]ri|guerra|fundad|antig|era|reinado|conquista|rebeli[aã]o|tratado|war|history|founded|ancient|reign|conquest|rebellion|treaty)\w*', re.I),
        'nearby_places': re.compile(r'\b(pr[oó]xim|perto|adiante|ao norte|ao sul|a leste|a oeste|vizinh|arredor|rota|salto|orbital|lua|esta[cç][aã]o|nearby|near |ahead|to the north|to the south|to the east|to the west|neighbor|surround|route|jump|orbital|moon|station)\w*', re.I),
        'character_knowledge': re.compile(r"\b(você sabe|voc[eê] conhece|voc[eê] reconhece|lhe [ée] conhecido|j[aá] ouviu|ouviu falar|you know|you recognize|known to you|you have heard|you've heard)\b", re.I),
        'current_conditions': re.compile(r'\b(agora|hoje|neste momento|chuva|vento|frio|calor|neblina|seca|mercado|pre[cç]os|fome|guerra|tens[aã]o|patrulh|tr[aá]fego|alf[aâ]ndega|seguran[cç]a|quarentena|now|today|at present|rain|wind|cold|heat|fog|drought|market|prices|hunger|war|tension|patrol|traffic|customs|security|quarantine)\w*', re.I),
    }

    @staticmethod
    def _flatten_names(value):
        names=[]
        if isinstance(value,str):
            names.append(value)
        elif isinstance(value,dict):
            for key in ('id','name','title','label'):
                if isinstance(value.get(key),str): names.append(value[key])
            for item in value.values():
                if isinstance(item,(dict,list,tuple,set)): names.extend(WorldIntroductionValidator._flatten_names(item))
        elif isinstance(value,(list,tuple,set)):
            for item in value: names.extend(WorldIntroductionValidator._flatten_names(item))
        return [x.strip() for x in names if isinstance(x,str) and len(x.strip())>=3]

    def _semantic_section_present(self, section, text, context):
        pattern=self._CATEGORY_PATTERNS.get(section)
        if pattern is not None and pattern.search(text):
            return True
        context=context if isinstance(context,dict) else {}
        if section=='region_context':
            names=self._flatten_names(context.get('region'))+self._flatten_names(context.get('region_id'))
            return any(name.lower() in text.lower() for name in names)
        if section=='nearby_places':
            names=[]
            for key in ('nearby_places','landmarks','connections'):
                names.extend(self._flatten_names(context.get(key)))
            return any(name.lower() in text.lower() for name in names)
        if section=='current_conditions':
            names=[]
            for key in ('weather','physical_environment','environment'):
                names.extend(self._flatten_names(context.get(key)))
            return any(name.lower() in text.lower() for name in names)
        return False

    def validate(self, narration, obligation, *, presentation=None, context=None, evidence=None, strict_structured=False):
        if not isinstance(obligation, dict):
            raise ValueError('invalid_world_introduction_obligation')
        if obligation.get('occasion') == 'continuation':
            return True
        text = str(narration or '').strip()
        minimum = int(obligation.get('target_chars', [0])[0] * .82)
        paragraphs = [p.strip() for p in re.split(r'\n\s*\n', text) if p.strip()]
        if len(text) < minimum:
            raise ValueError('world_introduction_too_short')
        if len(paragraphs) < int(obligation.get('minimum_paragraphs', 1)):
            raise ValueError('world_introduction_too_few_paragraphs')
        required = set(obligation.get('required_coverage') or [])
        coverage = set()
        if isinstance(presentation, dict):
            raw = presentation.get('coverage') or presentation.get('covered_sections') or []
            if isinstance(raw, (list, tuple, set)):
                coverage = {str(x) for x in raw}
        if strict_structured and required and not coverage:
            raise ValueError('world_introduction_missing_coverage_manifest')
        if coverage:
            missing = required - coverage
            if missing:
                raise ValueError('world_introduction_missing_coverage:' + ','.join(sorted(missing)))
            if strict_structured:
                semantic_sections={
                    'geographic_orientation','region_context','political_context','cultural_context',
                    'historical_context','nearby_places','character_knowledge','current_conditions',
                }
                unsupported=[section for section in sorted(required & semantic_sections) if not self._semantic_section_present(section,text,context)]
                if unsupported:
                    raise ValueError('world_introduction_claimed_but_missing:' + ','.join(unsupported))
        # Semantic safety net for legacy providers: ensure it is a scene and returns agency.
        if 'sensory_detail' in required and not self._SENSORY.search(text):
            raise ValueError('world_introduction_missing_sensory_scene')
        if 'agency_return' in required and not self._AGENCY.search(text[-700:]):
            raise ValueError('world_introduction_missing_agency_return')
        # When a named mapped location exists, an extended intro should actually anchor it.
        context = context if isinstance(context, dict) else {}
        name = str(context.get('location_name') or context.get('location_id') or '').strip()
        if name and 'geographic_orientation' in required and name.lower() not in text.lower():
            raise ValueError('world_introduction_missing_location_anchor')
        return True
