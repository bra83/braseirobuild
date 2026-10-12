from dataclasses import dataclass
import re

@dataclass(frozen=True)
class NarrativeChunk:
    index:int
    text:str
    final:bool=False

class NarrativeStream:
    """Chunk stream emitted only after the canonical mechanical commit is complete."""
    def chunks(self,narration,max_chars=220):
        if not isinstance(narration,str) or not narration.strip(): raise ValueError('invalid_narration')
        parts=[p.strip() for p in re.split(r'(?<=[.!?])\s+|\n\n+',narration) if p.strip()]
        out=[]; buf=''
        for part in parts:
            if buf and len(buf)+1+len(part)>max_chars:
                out.append(buf); buf=part
            else: buf=(buf+' '+part).strip()
        if buf: out.append(buf)
        return tuple(NarrativeChunk(i,text,i==len(out)-1) for i,text in enumerate(out))
