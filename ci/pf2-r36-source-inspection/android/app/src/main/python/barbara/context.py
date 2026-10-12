import re
import math
from copy import deepcopy

from .security import public_view
from .environment import VisibilityEngine, DiscoveryEngine


class ContextEngine:
    """Build typed, canonical scene context from campaign state.

    Geography is never inferred by the narrator. The adapter/atlas populates
    canonical site data and this projection is the only geography the narrator
    is allowed to treat as real.
    """

    def __init__(self, visibility=None, discovery=None, atlas_provider=None):
        self.visibility = visibility or VisibilityEngine()
        self.discovery = discovery or DiscoveryEngine()
        self.atlas_provider = atlas_provider

    def _merge_site(self, base, overlay):
        if not isinstance(base, dict):
            base = {}
        if not isinstance(overlay, dict):
            overlay = {}
        merged = deepcopy(base)
        for key, value in overlay.items():
            if isinstance(value, dict) and isinstance(merged.get(key), dict):
                merged[key] = self._merge_site(merged[key], value)
            else:
                merged[key] = deepcopy(value)
        return merged

    _WATER_WORDS = {
        'rio', 'river', 'riacho', 'stream', 'córrego', 'corrego', 'lago', 'lake',
        'mar', 'sea', 'oceano', 'ocean', 'pântano', 'pantano', 'swamp', 'canal',
        'cachoeira', 'waterfall', 'nascente', 'spring', 'delta', 'estuário', 'estuario',
    }
    _DRY_TERRAINS = {
        'desert', 'deserto', 'dunes', 'duna', 'dunas', 'badlands', 'semiarid',
        'semiárido', 'semiarido', 'wasteland', 'ermo', 'estepe seca', 'dry steppe',
    }

    def _as_words(self, value):
        if value is None:
            return []
        if isinstance(value, str):
            return [value]
        if isinstance(value, (list, tuple, set)):
            return [str(x) for x in value if x is not None]
        if isinstance(value, dict):
            return [str(k) for k, v in value.items() if v]
        return [str(value)]

    def _has_water_word(self, *values):
        blob = ' '.join(str(v).lower() for v in values if v is not None)
        return any(re.search(r'(?<!\w)'+re.escape(word)+r'(?!\w)',blob,re.IGNORECASE) for word in self._WATER_WORDS)

    def _normalize_hazards(self, value):
        if value is None:
            return ()
        rows=value if isinstance(value,(list,tuple)) else (value,)
        out=[]
        for item in rows:
            if isinstance(item,dict):
                public=public_view(deepcopy(item))
                if public: out.append(public)
            elif isinstance(item,str) and item.strip():
                out.append(item.strip())
            elif item is not None:
                out.append(str(item))
        return tuple(out)

    def _position(self, value):
        if not isinstance(value,dict): return None
        allowed={k:value[k] for k in ('x','y','z','q','r','level','zone') if k in value}
        return public_view(deepcopy(allowed)) if allowed else None

    def _distance(self, left, right):
        if not isinstance(left,dict) or not isinstance(right,dict): return None
        if all(k in left and k in right for k in ('q','r')):
            try:
                dq=float(left['q'])-float(right['q']); dr=float(left['r'])-float(right['r']); ds=-(float(left['q'])+float(left['r']))+(float(right['q'])+float(right['r']))
                return {'value':max(abs(dq),abs(dr),abs(ds)),'unit':'hex'}
            except (TypeError,ValueError): return None
        if all(k in left and k in right for k in ('x','y')):
            try:
                dz=(float(left.get('z',0))-float(right.get('z',0))) if ('z' in left or 'z' in right) else 0.0
                value=math.sqrt((float(left['x'])-float(right['x']))**2+(float(left['y'])-float(right['y']))**2+dz**2)
                return {'value':value,'unit':'map_unit'}
            except (TypeError,ValueError): return None
        return None

    def build(self, state):
        location_id = state.location or ''
        atlas_site = None
        if location_id and self.atlas_provider is not None:
            getter = getattr(self.atlas_provider, 'get_site', None)
            if callable(getter):
                try:
                    atlas_site = getter(location_id)
                except ValueError as exc:
                    if not str(exc).startswith('atlas_site_ambiguous:'):
                        raise
            if atlas_site is None:
                resolver=getattr(self.atlas_provider,'resolve_site',None)
                if callable(resolver):
                    atlas_site=resolver(location_id)
                    if isinstance(atlas_site,dict):
                        location_id=str((atlas_site.get('_atlas_resolution') or {}).get('site_id') or atlas_site.get('id') or location_id)
        dynamic_site = deepcopy(state.sites.get(location_id, {})) if location_id else {}
        raw_site = self._merge_site(atlas_site or {}, dynamic_site)
        site = public_view(raw_site) if isinstance(raw_site, dict) else {}
        if not isinstance(site, dict):
            site = {}

        terrain = site.get('terrain') or site.get('biome') or site.get('environment') or site.get('kind')
        features = []
        for key in ('features', 'tags', 'traits', 'landmarks'):
            features.extend(self._as_words(site.get(key)))
        features = [x.strip() for x in features if x and x.strip()]

        description = site.get('description') or site.get('public_description') or ''
        hydrology = site.get('hydrology')
        explicit_water = site.get('water_present')
        if explicit_water is None:
            explicit_water = site.get('has_water')

        geography_known = bool(location_id and site)
        water_present = None
        if isinstance(explicit_water, bool):
            water_present = explicit_water
        elif hydrology is not None:
            if isinstance(hydrology, str):
                low = hydrology.strip().lower()
                water_present = low not in {'', 'none', 'dry', 'absent', 'no', 'false'}
            else:
                water_present = bool(hydrology)
        elif self._has_water_word(' '.join(features), description):
            water_present = True
        elif terrain is not None and str(terrain).strip().lower() in self._DRY_TERRAINS:
            water_present = False
        elif geography_known and any(k in site for k in ('features', 'tags', 'traits', 'landmarks')):
            water_present = False

        name = site.get('name') or location_id
        hazards = self._normalize_hazards(site.get('hazards'))
        visibility = self.visibility.visibility_for(state, 'player', location_id) if location_id else {'can_see': True, 'quality': 'good', 'physical': {'ambient': 0.7, 'effective_level': 0.7, 'active_sources': []}}
        knowledge = self.discovery.projection(state, location_id) if location_id else {'visible_now': (), 'discovered': (), 'remembered': False}
        player_position=self._position(state.player_state.get('position'))
        actor_positions={}
        actor_distances={}
        for npc_id,npc in state.npcs.items():
            if not isinstance(npc,dict) or npc.get('location')!=location_id: continue
            pos=self._position(npc.get('position'))
            if pos is not None:
                actor_positions[npc_id]=pos
                distance=self._distance(player_position,pos)
                if distance is not None: actor_distances[npc_id]=distance
        explicit_distances=public_view(deepcopy(site.get('distances',{}))) if isinstance(site.get('distances',{}),dict) else {}

        # Cross-system public setting context. The host remains authoritative for the
        # values; Barbara only projects explicitly public fields for narration and
        # consistency checks. This is intentionally genre-neutral so a location may
        # be a medieval town, a modern district, a starport, an orbital habitat, etc.
        setting_keys = (
            'region', 'region_id', 'hierarchy', 'nearby_places', 'landmarks',
            'politics', 'government', 'authority', 'law', 'culture', 'society',
            'history', 'economy', 'technology', 'services', 'factions', 'population',
            'trade', 'security', 'infrastructure', 'customs', 'language',
        )
        setting_context = public_view(deepcopy({
            key: site.get(key) for key in setting_keys if key in site and site.get(key) is not None
        }))
        if not isinstance(setting_context, dict):
            setting_context = {}
        location_hierarchy = setting_context.get('hierarchy')

        return {
            'location_id': location_id,
            'location_name': str(name or ''),
            'terrain': str(terrain) if terrain is not None else None,
            'features': tuple(features),
            'description': str(description or ''),
            'hydrology': deepcopy(hydrology),
            'water_present': water_present,
            'geography_known': geography_known,
            'lighting': deepcopy(site.get('lighting')),
            'visibility_legacy': deepcopy(site.get('visibility')),
            'physical_environment': {
                'lighting': deepcopy(visibility.get('physical')),
                'temperature': deepcopy(site.get('temperature')),
                'noise': deepcopy(site.get('noise')),
                'weather': public_view(deepcopy(state.weather)),
            },
            'visibility': deepcopy(site.get('visibility')),
            'visibility_context': deepcopy(visibility),
            'line_of_sight': bool(visibility.get('line_of_sight', True)),
            'player_position': deepcopy(player_position),
            'actor_positions': deepcopy(actor_positions),
            'distances': {**deepcopy(explicit_distances), **deepcopy(actor_distances)},
            'conditions': tuple(str(x) for x in (state.player_state.get('conditions') or []) if str(x)),
            'knowledge': deepcopy(knowledge),
            'cover': deepcopy(site.get('cover')),
            'noise': deepcopy(site.get('noise')),
            'temperature': deepcopy(site.get('temperature')),
            'hazards': deepcopy(hazards),
            'geometry': deepcopy(site.get('geometry')),
            'connections': deepcopy(site.get('connections')),
            'location_hierarchy': deepcopy(location_hierarchy),
            'setting_context': deepcopy(setting_context),
            'region': deepcopy(setting_context.get('region')),
            'region_id': deepcopy(setting_context.get('region_id')),
            'nearby_places': deepcopy(setting_context.get('nearby_places')),
            'landmarks': deepcopy(setting_context.get('landmarks')),
            'weather': public_view(deepcopy(state.weather)),
            'game_time': public_view(deepcopy(getattr(state, 'game_time', {}))),
            'tick': state.tick,
            'state_version': getattr(state, 'state_version', 0),
        }


class WorldConsistencyGate:
    """Reject obvious geography hallucinations against typed canonical context."""

    _WATER_TERMS = (
        ' rio ', ' river ', ' riacho ', ' stream ', ' córrego ', ' corrego ',
        ' lago ', ' lake ', ' oceano ', ' ocean ', ' cachoeira ', ' waterfall ',
        ' pântano ', ' pantano ', ' swamp ', ' canal ', ' nascente ', ' spring ',
    )
    _TERRAIN_TERMS = {
        'desert': (' floresta densa ', ' dense forest ', ' selva ', ' jungle ', ' pântano ', ' pantano ', ' swamp '),
        'deserto': (' floresta densa ', ' dense forest ', ' selva ', ' jungle ', ' pântano ', ' pantano ', ' swamp '),
        'ocean': (' deserto ', ' desert ', ' floresta densa ', ' dense forest '),
        'sea': (' deserto ', ' desert ', ' floresta densa ', ' dense forest '),
        'oceano': (' deserto ', ' desert ', ' floresta densa ', ' dense forest '),
        'mar': (' deserto ', ' desert ', ' floresta densa ', ' dense forest '),
    }

    def _blob(self, text):
        return ' ' + ' '.join(str(text).lower().split()) + ' '

    def _contains_term(self, text, term):
        phrase=' '.join(str(term).strip().split())
        return bool(phrase and re.search(r'(?<!\w)'+re.escape(phrase)+r'(?!\w)', str(text), re.I))

    def validate(self, narration, context):
        if not isinstance(narration, str) or not narration.strip():
            raise ValueError('invalid_narration')
        if not isinstance(context, dict) or not context.get('geography_known'):
            return True

        blob = self._blob(narration)
        feature_blob = self._blob(' '.join(context.get('features') or ()))

        if context.get('water_present') is False:
            for term in self._WATER_TERMS:
                if self._contains_term(narration,term) and not self._contains_term(' '.join(context.get('features') or ()),term):
                    raise ValueError('narrative_geography_contradiction:water')

        terrain = str(context.get('terrain') or '').strip().lower()
        for term in self._TERRAIN_TERMS.get(terrain, ()):
            if self._contains_term(narration,term) and not self._contains_term(' '.join(context.get('features') or ()),term):
                raise ValueError('narrative_geography_contradiction:terrain')
        return True

    def validate_depth(self, narration, context, mode='fiction', importance='normal', story_obligation='continuation'):
        """Reject collapsed scenes even when the host has not yet attached an Atlas location."""
        if mode != 'fiction' or importance == 'routine':
            return True
        text = str(narration).strip()
        paragraphs = [p.strip() for p in re.split(r'\n\s*\n', text) if p.strip()]
        intro_targets={
            'campaign_opening':(3,700),'first_arrival':(2,560),'first_visit_settlement':(2,560),
            'first_visit_region':(2,560),'first_visit_landmark':(2,560),'enter_new_geographic_area':(2,560),
            'changed_return':(2,450),'long_absence_return':(2,450),'major_political_transition':(2,560),
        }
        opening = story_obligation in intro_targets
        mapped = isinstance(context, dict) and bool(context.get('location_id'))
        if not opening and not mapped:
            return True
        min_paragraphs,minimum = intro_targets.get(story_obligation,(2,{'normal':320,'meaningful':600,'climax':950}.get(importance,320)))
        if len(paragraphs) < min_paragraphs:
            raise ValueError('narrative_too_few_paragraphs')
        if len(text) < minimum:
            raise ValueError('narrative_too_short')
        return True
