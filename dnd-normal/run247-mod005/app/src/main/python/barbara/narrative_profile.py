from dataclasses import dataclass

@dataclass(frozen=True)
class NarrativeProfile:
    profile_id:str
    genre:str='system_default'
    tone:str='grounded'
    pace:str='adaptive'
    verbosity:str='cinematic'
    language:str='pt-BR'
    def __post_init__(self):
        if not self.profile_id: raise ValueError('invalid_narrative_profile')
