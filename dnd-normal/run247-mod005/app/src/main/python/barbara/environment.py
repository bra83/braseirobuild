from __future__ import annotations

from copy import deepcopy


class VisibilityEngine:
    """Separates physical lighting from actor visibility and player discovery."""

    LIGHT_LEVELS = {'dark': 0.0, 'dim': 0.35, 'normal': 0.7, 'bright': 1.0}

    def _site(self, state, location_id=None):
        location_id = state.location if location_id is None else location_id
        site = state.sites.get(location_id, {}) if location_id else {}
        return site if isinstance(site, dict) else {}

    def physical(self, state, location_id=None):
        site = self._site(state, location_id)
        ambient = site.get('ambient_light', site.get('lighting', 'normal'))
        if isinstance(ambient, dict):
            ambient_level = ambient.get('level', 'normal')
        else:
            ambient_level = ambient
        if isinstance(ambient_level, (int, float)) and not isinstance(ambient_level, bool):
            base = max(0.0, min(1.0, float(ambient_level)))
        else:
            base = self.LIGHT_LEVELS.get(str(ambient_level or 'normal').lower(), 0.7)
        sources = site.get('light_sources', [])
        if not isinstance(sources, list):
            raise ValueError('invalid_light_sources')
        active = []
        boost = 0.0
        elapsed = float((state.game_time or {}).get('elapsed_seconds', 0))
        for raw in sources:
            if not isinstance(raw, dict):
                raise ValueError('invalid_light_source')
            source_id = raw.get('id')
            if not isinstance(source_id, str) or not source_id:
                raise ValueError('invalid_light_source')
            if raw.get('status', 'lit') != 'lit':
                continue
            expires = raw.get('expires_at_seconds')
            if expires is not None and elapsed >= float(expires):
                continue
            intensity = raw.get('intensity', 0.5)
            if not isinstance(intensity, (int, float)) or isinstance(intensity, bool):
                raise ValueError('invalid_light_source_intensity')
            boost = max(boost, max(0.0, min(1.0, float(intensity))))
            active.append({
                'id': source_id,
                'position': deepcopy(raw.get('position')),
                'radius': raw.get('radius'),
                'intensity': float(intensity),
                'owner': raw.get('owner'),
            })
        return {'ambient': base, 'effective_level': max(base, boost), 'active_sources': active}

    def visibility_for(self, state, actor_id='player', location_id=None):
        site = self._site(state, location_id)
        physical = self.physical(state, location_id)
        actor = state.player_state if actor_id == 'player' else state.npcs.get(actor_id, {})
        if not isinstance(actor, dict):
            actor = {}
        senses = actor.get('senses', {}) if isinstance(actor.get('senses', {}), dict) else {}
        darkvision = bool(senses.get('darkvision') or senses.get('night_vision'))
        blocked = bool(site.get('visibility_blocked', False))
        smoke = float(site.get('smoke', 0) or 0)
        effective = physical['effective_level']
        if darkvision:
            effective = max(effective, 0.55)
        effective = max(0.0, effective - min(1.0, max(0.0, smoke)))
        can_see = bool(effective >= 0.2 and not blocked)
        quality = 'none' if not can_see else ('poor' if effective < 0.45 else ('good' if effective < 0.8 else 'excellent'))
        return {
            'actor_id': actor_id,
            'can_see': can_see,
            'quality': quality,
            'effective_light': round(effective, 4),
            'line_of_sight': not blocked,
            'physical': physical,
        }


class DiscoveryEngine:
    """Knowledge/fog-of-war projection. Visibility never auto-discovers secrets."""

    def _location_state(self, state, location_id=None, create=True):
        location_id = state.location if location_id is None else location_id
        if not location_id:
            return {}
        if create:
            locations = state.discovery.setdefault('locations', {})
            entry = locations.setdefault(location_id, {})
        else:
            locations = state.discovery.get('locations', {})
            if not isinstance(locations, dict):
                raise ValueError('invalid_discovery_locations')
            entry = locations.get(location_id, {})
        if entry and not isinstance(entry, dict):
            raise ValueError('invalid_discovery_location')
        return entry

    def mark_visible(self, state, feature_ids, location_id=None):
        if not isinstance(feature_ids, (list, tuple, set)):
            raise ValueError('invalid_visible_features')
        entry = self._location_state(state, location_id)
        entry['visible_now'] = sorted({str(x) for x in feature_ids if str(x)})
        return deepcopy(entry['visible_now'])

    def discover(self, state, feature_id, *, location_id=None, method='observation'):
        if not isinstance(feature_id, str) or not feature_id:
            raise ValueError('invalid_discovery_feature')
        entry = self._location_state(state, location_id)
        discovered = entry.setdefault('discovered', {})
        if not isinstance(discovered, dict):
            raise ValueError('invalid_discovery_state')
        discovered[feature_id] = {'tick': state.tick, 'method': method}
        return deepcopy(discovered[feature_id])

    def projection(self, state, location_id=None):
        entry = self._location_state(state, location_id, create=False)
        return {
            'visible_now': tuple(entry.get('visible_now', ())),
            'discovered': tuple(sorted((entry.get('discovered') or {}).keys())),
            'remembered': bool(entry),
        }
