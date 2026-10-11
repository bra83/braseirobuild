from __future__ import annotations

from abc import ABC, abstractmethod
from collections import defaultdict
from copy import deepcopy
import math
import re

from .knowledge_engine import RuleConflict, RuleEvidence, RuleEvidenceBundle


class EmbeddingProvider(ABC):
    @property
    @abstractmethod
    def descriptor(self): ...
    @abstractmethod
    def embed(self,text): ...


class VectorIndex(ABC):
    @abstractmethod
    def upsert(self,chunk_id,vector,metadata): ...
    @abstractmethod
    def search(self,vector,limit=12,filters=None): ...
    @abstractmethod
    def delete_source(self,source_id): ...
    @abstractmethod
    def rebuild(self,records): ...


class LexicalIndex(ABC):
    @abstractmethod
    def upsert(self,chunk_id,text,metadata): ...
    @abstractmethod
    def search(self,query,limit=12,filters=None): ...
    @abstractmethod
    def delete_source(self,source_id): ...


class InMemoryVectorIndex(VectorIndex):
    def __init__(self): self._rows={}
    def _cosine(self,a,b):
        if len(a)!=len(b): return 0.0
        dot=sum(x*y for x,y in zip(a,b)); na=math.sqrt(sum(x*x for x in a)); nb=math.sqrt(sum(y*y for y in b))
        return dot/(na*nb) if na and nb else 0.0
    def upsert(self,chunk_id,vector,metadata):
        if not isinstance(chunk_id,str) or not chunk_id: raise ValueError('invalid_chunk_id')
        if not isinstance(vector,(list,tuple)) or not vector: raise ValueError('invalid_vector')
        self._rows[chunk_id]=(tuple(float(x) for x in vector),deepcopy(metadata))
    def search(self,vector,limit=12,filters=None):
        filters=filters or {}; scored=[]
        for chunk_id,(stored,meta) in self._rows.items():
            if any(meta.get(k)!=v for k,v in filters.items()): continue
            score=self._cosine(vector,stored)
            if score>0: scored.append((score,chunk_id,deepcopy(meta)))
        scored.sort(key=lambda x:(-x[0],x[1])); return scored[:limit]
    def delete_source(self,source_id):
        for key in [k for k,(_,m) in self._rows.items() if m.get('source_id')==source_id]: self._rows.pop(key,None)
    def rebuild(self,records): self._rows={}; [self.upsert(*r) for r in records]


class InMemoryLexicalIndex(LexicalIndex):
    def __init__(self): self._rows={}
    def upsert(self,chunk_id,text,metadata): self._rows[chunk_id]=(str(text),deepcopy(metadata))
    def search(self,query,limit=12,filters=None):
        filters=filters or {}; q=set(re.findall(r'\w+',str(query).lower())); scored=[]
        for chunk_id,(text,meta) in self._rows.items():
            if any(meta.get(k)!=v for k,v in filters.items()): continue
            toks=set(re.findall(r'\w+',text.lower())); score=len(q&toks)/max(1,len(q|toks))
            if score>0: scored.append((score,chunk_id,deepcopy(meta)))
        scored.sort(key=lambda x:(-x[0],x[1])); return scored[:limit]
    def delete_source(self,source_id):
        for key in [k for k,(_,m) in self._rows.items() if m.get('source_id')==source_id]: self._rows.pop(key,None)


class HybridRetriever:
    """Fuses lexical + vector candidates, filters metadata, reranks and can fail closed."""
    CANONICAL_BONUS={'official':.12,'errata':.15,'campaign_rule':.10,'supplement':.06,'house_rule':.04,'inference':0.0}
    def __init__(self,lexical_index,vector_index=None,embedding_provider=None,threshold=.12):
        self.lexical=lexical_index; self.vector=vector_index; self.embedder=embedding_provider; self.threshold=threshold; self._chunks={}
    def upsert_chunk(self,chunk,vector=None):
        meta={'source_id':chunk.source_id,'system_id':chunk.system_id,'campaign_id':chunk.campaign_id,'canonicality':chunk.canonicality,'priority':chunk.priority,'page':chunk.page_start,'section':chunk.section,'text':chunk.text}
        self._chunks[chunk.chunk_id]=deepcopy(chunk); self.lexical.upsert(chunk.chunk_id,chunk.text,meta)
        if self.vector is not None:
            if vector is None:
                if self.embedder is None: raise ValueError('embedding_required')
                vector=self.embedder.embed(chunk.text)
            self.vector.upsert(chunk.chunk_id,vector,meta)
    def retrieve(self,query,*,system_id,campaign_id,limit=6):
        filters={'system_id':system_id,'campaign_id':campaign_id}; candidates=defaultdict(dict)
        for score,cid,meta in self.lexical.search(query,limit=max(limit*3,12),filters=filters): candidates[cid].update(lexical=float(score),meta=meta)
        if self.vector is not None and self.embedder is not None:
            qvec=self.embedder.embed(query)
            for score,cid,meta in self.vector.search(qvec,limit=max(limit*3,12),filters=filters): candidates[cid].update(semantic=float(score),meta=meta)
        ranked=[]
        for cid,data in candidates.items():
            chunk=self._chunks.get(cid); meta=data.get('meta',{})
            lexical=float(data.get('lexical',0)); semantic=float(data.get('semantic',0))
            canonicality=getattr(chunk,'canonicality',meta.get('canonicality','official')); priority=int(getattr(chunk,'priority',meta.get('priority',50)))
            final=.5*lexical+.38*semantic+self.CANONICAL_BONUS.get(canonicality,0)+min(.1,max(0,priority)/1000)
            ranked.append((final,cid,lexical,semantic,chunk,meta))
        ranked.sort(key=lambda x:(-x[0],x[1])); ranked=ranked[:limit]
        evidence=[]
        for final,cid,lexical,semantic,chunk,meta in ranked:
            text=getattr(chunk,'text',meta.get('text','')); source=getattr(chunk,'source_id',meta.get('source_id',''))
            evidence.append(RuleEvidence(cid,source,text,getattr(chunk,'page_start',meta.get('page')),getattr(chunk,'section',meta.get('section')),semantic or None,lexical or None,final,getattr(chunk,'canonicality',meta.get('canonicality','official')),int(getattr(chunk,'priority',meta.get('priority',50)))))
        confidence=evidence[0].final_score if evidence else 0.0
        conflicts=[]
        by_section=defaultdict(list)
        for e in evidence:
            by_section[(e.section or '',e.canonicality)].append(e)
        # Conflict marker is deliberately conservative: same section, different canonicality and materially different text.
        for i,left in enumerate(evidence):
            for right in evidence[i+1:]:
                if left.section and left.section==right.section and left.canonicality!=right.canonicality and left.text!=right.text:
                    conflicts.append(RuleConflict(left.chunk_id,right.chunk_id,'same_section_different_authority'))
        return RuleEvidenceBundle(str(query),tuple(evidence),confidence,tuple(conflicts),confidence<self.threshold)


class SourceProvider(ABC):
    @abstractmethod
    def list_sources(self): ...
    @abstractmethod
    def read_source(self,source_id): ...


class FileSourceProvider(SourceProvider):
    def __init__(self,paths):
        from pathlib import Path
        self.paths={Path(p).stem:Path(p) for p in paths}
    def list_sources(self): return tuple(sorted(self.paths))
    def read_source(self,source_id):
        path=self.paths.get(source_id)
        if path is None: raise KeyError(source_id)
        return path.read_bytes()


class DriveSourceProvider(SourceProvider):
    """Optional Drive adapter contract. Network/auth implementation is injected, not baked into core."""
    def __init__(self,client,folder_id):
        if client is None or not folder_id: raise ValueError('invalid_drive_source_provider')
        self.client=client; self.folder_id=folder_id
    def list_sources(self):
        rows=self.client.list_files(self.folder_id)
        if not isinstance(rows,(list,tuple)): raise ValueError('invalid_drive_listing')
        return tuple(str(row['id']) for row in rows if isinstance(row,dict) and row.get('id'))
    def read_source(self,source_id):
        raw=self.client.download_file(source_id)
        if not isinstance(raw,(bytes,bytearray)): raise ValueError('invalid_drive_download')
        return bytes(raw)
