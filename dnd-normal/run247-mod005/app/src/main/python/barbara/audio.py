from __future__ import annotations

from copy import deepcopy


class AudioCatalogContract:
    """Validates host-provided logical audio assets.

    Barbara owns selection/mix policy; each app owns actual files, decoding/player,
    licensing and system-specific catalog aliases. No audio bytes live in the core.
    """
    CHANNELS=frozenset({'sfx','ambience','music','voice','ui'})

    def validate(self,catalog):
        if catalog is None:return {}
        if not isinstance(catalog,dict):raise ValueError('invalid_audio_catalog')
        out={}
        for asset_id,raw in catalog.items():
            if not isinstance(asset_id,str) or not asset_id or len(asset_id)>200:raise ValueError('invalid_audio_asset_id')
            if not isinstance(raw,dict):raise ValueError('invalid_audio_asset')
            channel=raw.get('channel')
            if channel not in self.CHANNELS:raise ValueError('invalid_audio_channel')
            descriptor={
                'channel':channel,
                'host_asset_id':str(raw.get('host_asset_id') or asset_id),
                'loop':bool(raw.get('loop',channel in {'ambience','music'})),
                'gain':float(raw.get('gain',1.0)),
                'tags':[str(x).strip().lower() for x in raw.get('tags',[]) if str(x).strip()],
                'duration_ms':raw.get('duration_ms'),
            }
            if not 0<=descriptor['gain']<=4:raise ValueError('invalid_audio_gain')
            if descriptor['duration_ms'] is not None and (not isinstance(descriptor['duration_ms'],(int,float)) or isinstance(descriptor['duration_ms'],bool) or descriptor['duration_ms']<0):raise ValueError('invalid_audio_duration')
            out[asset_id]=descriptor
        return out


class AudioDirector:
    """Event-driven, bounded audio director shared by all systems.

    It never parses narration prose and never mutates canonical campaign state.
    Apps provide actual audio assets and the native/web player implementation.
    """
    DEFAULT={
        'door_knocked':{'channel':'sfx','asset':'door_knock','priority':60},
        'door_opened':{'channel':'sfx','asset':'door_open','priority':45},
        'weapon_fired':{'channel':'sfx','asset':'weapon_fire','priority':85},
        'thunder':{'channel':'sfx','asset':'thunder','requires_any':('storm','rain','thunder'),'priority':90},
        'weather_changed':{'channel':'ambience','asset':'weather_dynamic','priority':40},
        'rain_started':{'channel':'ambience','asset':'rain','priority':45},
        'wind_started':{'channel':'ambience','asset':'wind','priority':35},
        'fire_started':{'channel':'ambience','asset':'fire','priority':35},
        'combat_started':{'channel':'music','asset':'combat','priority':100},
        'combat_ended':{'channel':'music','asset':'exploration','priority':80},
        'journey_started':{'channel':'music','asset':'journey','priority':55},
        'journey_ended':{'channel':'music','asset':'exploration','priority':50},
        'settlement_entered':{'channel':'ambience','asset':'settlement','priority':50},
        'tavern_entered':{'channel':'ambience','asset':'tavern','priority':60},
        'dungeon_entered':{'channel':'ambience','asset':'dungeon','priority':65},
        'master_narration_started':{'channel':'voice','asset':'__host_tts__','priority':120},
        'master_narration_ended':{'channel':'voice','asset':'__voice_end__','priority':120},
    }
    DEFAULT_MIX={
        'music_gain':1.0,'ambience_gain':1.0,'sfx_gain':1.0,'voice_gain':1.0,
        'narration_duck_music':0.42,'narration_duck_ambience':0.62,
        'transition_ms':650,'sfx_cooldown_ms':100,
    }
    def __init__(self,mapping=None,*,max_sfx_per_batch=3,catalog=None,mix=None):
        self.mapping={**self.DEFAULT,**(mapping or {})}
        self.max_sfx_per_batch=max(0,int(max_sfx_per_batch))
        self.catalog=AudioCatalogContract().validate(catalog)
        self.mix={**self.DEFAULT_MIX,**(mix or {})}
        for key in ('music_gain','ambience_gain','sfx_gain','voice_gain','narration_duck_music','narration_duck_ambience'):
            value=self.mix.get(key)
            if not isinstance(value,(int,float)) or isinstance(value,bool) or not 0<=float(value)<=4:raise ValueError('invalid_audio_mix')

    @staticmethod
    def _tags(context,payload):
        tags=set()
        for source in (context or {}, payload or {}):
            if not isinstance(source,dict): continue
            for key in ('terrain','location_name','weather','kind','environment','audio_context','region','location_type'):
                value=source.get(key)
                if isinstance(value,str): tags.add(value.strip().lower())
                elif isinstance(value,dict):
                    for k,v in value.items():
                        if v not in (None,False,''): tags.add(str(k).strip().lower()); tags.add(str(v).strip().lower())
            for key in ('features','tags','traits'):
                value=source.get(key,())
                if isinstance(value,str): value=(value,)
                if isinstance(value,(list,tuple,set)):
                    tags.update(str(x).strip().lower() for x in value if str(x).strip())
        return tags

    def _resolve_asset(self,cmd):
        descriptor=self.catalog.get(cmd.get('asset'))
        if descriptor:
            cmd['host_asset_id']=descriptor['host_asset_id']
            cmd['loop']=descriptor['loop']
            cmd['asset_gain']=descriptor['gain']
        return cmd

    def commands_for(self,event,listener_position=None,context=None):
        if not isinstance(event,dict): raise ValueError('invalid_game_event')
        spec=self.mapping.get(event.get('type'))
        if spec is None:return []
        payload=event.get('payload',{}) if isinstance(event.get('payload',{}),dict) else {}
        tags=self._tags(context,payload)
        required=spec.get('requires_any',())
        excluded=spec.get('exclude_if',())
        if required and not any(str(x).lower() in tags for x in required): return []
        if excluded and any(str(x).lower() in tags for x in excluded): return []
        cmd={k:deepcopy(v) for k,v in spec.items() if k not in {'requires_any','exclude_if'}}
        cmd['event_id']=event.get('event_id'); cmd['intensity']=payload.get('intensity',1.0); cmd['distance']=payload.get('distance')
        if cmd.get('channel') in {'ambience','music'}: cmd.setdefault('transition_ms',self.mix['transition_ms'])
        return [self._resolve_asset(cmd)]

    def commands_for_events(self,events,*,context=None):
        if not isinstance(events,(list,tuple)): raise ValueError('invalid_game_events')
        out=[]; seen=set(); sfx=0; latest_layers={}; voice=[]
        for event in events:
            for cmd in self.commands_for(event,context=context):
                channel=cmd.get('channel'); key=(channel,cmd.get('asset'))
                if key in seen and channel!='voice': continue
                seen.add(key)
                if channel=='sfx':
                    if sfx>=self.max_sfx_per_batch: continue
                    sfx+=1; out.append(cmd)
                elif channel in {'ambience','music'}:
                    previous=latest_layers.get(channel)
                    if previous is None or int(cmd.get('priority',50))>=int(previous.get('priority',50)):
                        latest_layers[channel]=cmd
                elif channel=='voice':
                    voice.append(cmd)
                else:
                    out.append(cmd)
        out.extend(latest_layers[channel] for channel in ('ambience','music') if channel in latest_layers)
        out.extend(voice[-1:])
        return out

    def scene_plan(self,events,*,context=None,narration_active=False):
        commands=self.commands_for_events(events,context=context)
        mix=deepcopy(self.mix)
        if narration_active:
            mix['effective_music_gain']=mix['music_gain']*mix['narration_duck_music']
            mix['effective_ambience_gain']=mix['ambience_gain']*mix['narration_duck_ambience']
        else:
            mix['effective_music_gain']=mix['music_gain']
            mix['effective_ambience_gain']=mix['ambience_gain']
        mix['effective_sfx_gain']=mix['sfx_gain']; mix['effective_voice_gain']=mix['voice_gain']
        return {
            'commands':commands,
            'mix':mix,
            'narration_active':bool(narration_active),
            'asset_owner':'host',
            'selection_owner':'barbara',
            'canonical_state_affected':False,
        }
