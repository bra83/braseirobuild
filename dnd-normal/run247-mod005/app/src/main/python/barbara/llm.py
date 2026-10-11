from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass
from copy import deepcopy


@dataclass(frozen=True)
class SamplingPolicy:
    temperature: float
    top_p: float = 0.95
    top_k: int = 40

    def __post_init__(self):
        if not 0 <= self.temperature <= 2: raise ValueError('invalid_temperature')
        if not 0 < self.top_p <= 1: raise ValueError('invalid_top_p')
        if not isinstance(self.top_k,int) or isinstance(self.top_k,bool) or self.top_k<1: raise ValueError('invalid_top_k')


TASK_SAMPLING={
    'intent':SamplingPolicy(.05,.9,20),
    'rules':SamplingPolicy(.0,.8,10),
    'narration':SamplingPolicy(.65,.95,40),
    'brainstorm':SamplingPolicy(.9,.98,80),
}


class LlmProvider(ABC):
    @abstractmethod
    def generate(self,text,context,state): ...


class LlmGateway:
    """Provider-neutral LLM boundary with explicit local/cloud failover."""
    def __init__(self,local=None,cloud=None,prefer_local=True):
        self.local=local; self.cloud=cloud; self.prefer_local=bool(prefer_local)
    def _order(self): return (self.local,self.cloud) if self.prefer_local else (self.cloud,self.local)
    def generate(self,text,context,state):
        errors=[]
        for provider in self._order():
            if provider is None: continue
            try: return provider.generate(text,deepcopy(context),state)
            except Exception as exc: errors.append(exc)
        if errors: raise errors[-1]
        raise RuntimeError('no_llm_provider_available')
