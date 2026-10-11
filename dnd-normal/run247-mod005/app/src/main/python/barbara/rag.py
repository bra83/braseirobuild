from dataclasses import dataclass
from .knowledge_engine import RuleConflict, RuleEvidence, RuleEvidenceBundle
from copy import deepcopy
import hashlib, math, re, sqlite3, json
from threading import RLock

from .state import _validate_canonical_value
from .numeric import is_finite_number

INJECTION_PATTERNS=(r'ignore\s+(all|previous|prior)\s+(instructions|rules)',r'(reveal|print|expose)\s+.*(secret|system prompt|director)',r'you\s+are\s+now\s+',r'follow\s+these\s+instructions')
VALID_KINDS={"RULE","LORE","MEMORY","NPC","LOCATION","EVENT","INFERENCE"}
VALID_CANONICALITY={'official','errata','supplement','setting','campaign_rule','house_rule','memory','inference'}
CANONICAL_BONUS={'errata':.12,'official':.10,'campaign_rule':.08,'supplement':.05,'setting':.04,'house_rule':.03,'memory':.0,'inference':-.02}

@dataclass(frozen=True)
class Evidence:
    source_id:str
    text:str
    kind:str
    campaign_id:str
    system_id:str
    authority:float=1.0
    secret:bool=False
    vector:tuple|None=None
    page:int|None=None
    section:str|None=None
    language:str='pt'
    source_type:str='unknown'
    canonicality:str='official'
    priority:int=50
    metadata:dict|None=None
    @property
    def checksum(self): return hashlib.sha256(self.text.encode()).hexdigest()
    @property
    def provenance(self):
        return {
            'source_id':self.source_id,'checksum':self.checksum,'page':self.page,'section':self.section,
            'language':self.language,'source_type':self.source_type,'canonicality':self.canonicality,
            'priority':self.priority,'authority':self.authority,'metadata':deepcopy(self.metadata or {}),
        }

class RAG:
    MAX_DOCS_PER_SOURCE=10_000
    MAX_VECTOR_DIMENSIONS=8_192
    MAX_EVIDENCE_TEXT=2_000_000
    MAX_QUERY=20_000
    def __init__(self,db_path=None):
        self._lock=RLock(); self._docs={}; self._quarantine=[]; self.db_path=db_path; self._ranked_cache={}
        self._db=sqlite3.connect(db_path,timeout=30,check_same_thread=False) if db_path else None
        if self._db:
            self._db.execute('PRAGMA journal_mode=WAL')
            self._db.execute('CREATE TABLE IF NOT EXISTS docs(campaign TEXT, system TEXT, source TEXT, idx INTEGER, text TEXT, kind TEXT, authority REAL, secret INTEGER, vector TEXT, checksum TEXT, metadata TEXT, PRIMARY KEY(campaign,system,source,idx))')
            cols={row[1] for row in self._db.execute('PRAGMA table_info(docs)')}
            if 'metadata' not in cols:
                self._db.execute('ALTER TABLE docs ADD COLUMN metadata TEXT')
            self._db.execute('CREATE TABLE IF NOT EXISTS quarantine(campaign TEXT, system TEXT, source TEXT, checksum TEXT, PRIMARY KEY(campaign,system,source,checksum))')
            self._db.commit(); self._load()
    def close(self):
        with self._lock:
            if self._db: self._db.close(); self._db=None
    def _meta_payload(self,d):
        return {
            'page':d.page,'section':d.section,'language':d.language,'source_type':d.source_type,
            'canonicality':d.canonicality,'priority':d.priority,'metadata':deepcopy(d.metadata or {}),
        }
    def _load(self):
        with self._lock:
            self._docs={}
            rows=self._db.execute('SELECT campaign,system,source,idx,text,kind,authority,secret,vector,checksum,metadata FROM docs')
            for c,s,src,i,text,kind,auth,secret,vec,stored_checksum,meta_raw in rows:
                try:
                    if secret not in (0,1) or not isinstance(i,int) or isinstance(i,bool) or i<0:raise ValueError
                    v=tuple(json.loads(vec)) if vec else None
                    meta=json.loads(meta_raw) if meta_raw else {}
                    if not isinstance(meta,dict):raise ValueError
                    evidence=Evidence(
                        src,text,kind,c,s,auth,bool(secret),v,meta.get('page'),meta.get('section'),
                        meta.get('language','pt'),meta.get('source_type','unknown'),meta.get('canonicality','official'),
                        int(meta.get('priority',50)),deepcopy(meta.get('metadata') or {}),
                    )
                    self._validate(evidence,src)
                except (TypeError,ValueError,json.JSONDecodeError) as exc:raise ValueError('corrupt_rag_store') from exc
                if stored_checksum!=evidence.checksum:raise ValueError('corrupt_rag_store')
                self._docs[(c,s,src,i)]=evidence
            self._quarantine=[tuple(r) for r in self._db.execute('SELECT campaign,system,source,checksum FROM quarantine')]
    @property
    def quarantine(self):
        with self._lock:return tuple(self._quarantine)
    @property
    def revision(self):
        with self._lock:
            rows=sorted((k[0],k[1],k[2],k[3],v.checksum,v.authority,v.canonicality,v.priority) for k,v in self._docs.items())
            return hashlib.sha256(repr(rows).encode()).hexdigest()
    def _unsafe(self,text): return any(re.search(p,text,re.I) for p in INJECTION_PATTERNS)
    def _validate(self,d,source_id):
        if not isinstance(d,Evidence): raise ValueError('invalid_evidence')
        if not isinstance(source_id,str) or not source_id or len(source_id)>160 or not isinstance(d.source_id,str) or not d.source_id or len(d.source_id)>160: raise ValueError('invalid_source_id')
        if d.source_id!=source_id: raise ValueError('source_mismatch')
        if not isinstance(d.campaign_id,str) or not d.campaign_id or len(d.campaign_id)>160: raise ValueError('invalid_campaign_id')
        if not isinstance(d.system_id,str) or not d.system_id or len(d.system_id)>160: raise ValueError('invalid_system_id')
        if d.kind not in VALID_KINDS: raise ValueError('invalid_evidence_kind')
        if not isinstance(d.text,str) or not d.text.strip(): raise ValueError('empty_evidence')
        if len(d.text)>self.MAX_EVIDENCE_TEXT: raise ValueError('evidence_too_large')
        if not is_finite_number(d.authority) or not 0<=d.authority<=1: raise ValueError('invalid_authority')
        if not isinstance(d.secret,bool): raise ValueError('invalid_evidence_secret')
        if d.vector is not None:
            if not isinstance(d.vector,(tuple,list)) or not d.vector or len(d.vector)>self.MAX_VECTOR_DIMENSIONS or not all(is_finite_number(x) for x in d.vector): raise ValueError('invalid_vector')
        if d.page is not None and (not isinstance(d.page,int) or isinstance(d.page,bool) or d.page<1): raise ValueError('invalid_evidence_page')
        if d.section is not None and (not isinstance(d.section,str) or not d.section): raise ValueError('invalid_evidence_section')
        if not isinstance(d.language,str) or not d.language: raise ValueError('invalid_evidence_language')
        if not isinstance(d.source_type,str) or not d.source_type: raise ValueError('invalid_source_type')
        if d.canonicality not in VALID_CANONICALITY: raise ValueError('invalid_canonicality')
        if not isinstance(d.priority,int) or isinstance(d.priority,bool) or not 0<=d.priority<=100: raise ValueError('invalid_priority')
        if d.metadata is not None:
            if not isinstance(d.metadata,dict): raise ValueError('invalid_evidence_metadata')
            try:_validate_canonical_value(d.metadata)
            except ValueError as exc:raise ValueError('invalid_evidence_metadata') from exc
    def replace_source(self,source_id,docs):
        with self._lock:
            try:staged=list(docs)
            except TypeError as exc:raise ValueError('invalid_evidence_batch') from exc
            if len(staged)>self.MAX_DOCS_PER_SOURCE:raise ValueError('evidence_batch_too_large')
            for d in staged:self._validate(d,source_id)
            scopes={(d.campaign_id,d.system_id) for d in staged}
            if len(scopes)>1: raise ValueError('mixed_source_scope')
            accepted=[]; quarantined=[]
            for d in staged:
                (quarantined if self._unsafe(d.text) else accepted).append(d)
            if not staged: return
            c,s=next(iter(scopes))
            if self._db:
                try:
                    with self._db:
                        self._db.execute('DELETE FROM docs WHERE campaign=? AND system=? AND source=?',(c,s,source_id))
                        for i,d in enumerate(accepted):
                            self._db.execute('INSERT INTO docs(campaign,system,source,idx,text,kind,authority,secret,vector,checksum,metadata) VALUES(?,?,?,?,?,?,?,?,?,?,?)',(
                                c,s,source_id,i,d.text,d.kind,float(d.authority),int(d.secret),json.dumps(list(d.vector),allow_nan=False) if d.vector is not None else None,d.checksum,json.dumps(self._meta_payload(d),ensure_ascii=False,sort_keys=True,allow_nan=False)
                            ))
                        for d in quarantined:
                            self._db.execute('INSERT OR IGNORE INTO quarantine VALUES(?,?,?,?)',(c,s,source_id,d.checksum))
                except Exception:
                    self._load(); raise
                self._load(); self._ranked_cache.clear(); return
            fresh={k:v for k,v in self._docs.items() if not (k[0]==c and k[1]==s and k[2]==source_id)}
            for i,d in enumerate(accepted): fresh[(c,s,source_id,i)]=deepcopy(d)
            self._docs=fresh; self._ranked_cache.clear()
            for d in quarantined:
                q=(c,s,source_id,d.checksum)
                if q not in self._quarantine:self._quarantine.append(q)
    def delete_source(self,campaign_id,system_id,source_id):
        """Remove one source atomically from a campaign/system projection."""
        if not isinstance(campaign_id,str) or not campaign_id: raise ValueError('invalid_campaign_id')
        if not isinstance(system_id,str) or not system_id: raise ValueError('invalid_system_id')
        if not isinstance(source_id,str) or not source_id or len(source_id)>160: raise ValueError('invalid_source_id')
        with self._lock:
            if self._db:
                with self._db:
                    self._db.execute('DELETE FROM docs WHERE campaign=? AND system=? AND source=?',(campaign_id,system_id,source_id))
                    self._db.execute('DELETE FROM quarantine WHERE campaign=? AND system=? AND source=?',(campaign_id,system_id,source_id))
                self._load(); self._ranked_cache.clear(); return True
            before=len(self._docs)
            self._docs={k:v for k,v in self._docs.items() if not (k[0]==campaign_id and k[1]==system_id and k[2]==source_id)}
            self._quarantine=[q for q in self._quarantine if not (q[0]==campaign_id and q[1]==system_id and q[2]==source_id)]
            self._ranked_cache.clear()
            return len(self._docs)!=before

    def ingest_chunks(self,chunks,*,kind='RULE',vectors=None,authority=1.0,secret=False):
        """Atomic bridge from DocumentIngestor chunks into the canonical RAG store."""
        chunks=list(chunks)
        if not chunks: return []
        if kind not in VALID_KINDS: raise ValueError('invalid_evidence_kind')
        source_ids={getattr(c,'source_id',None) for c in chunks}; systems={getattr(c,'system_id',None) for c in chunks}; campaigns={getattr(c,'campaign_id',None) for c in chunks}
        if len(source_ids)!=1 or len(systems)!=1 or len(campaigns)!=1 or None in source_ids|systems|campaigns:
            raise ValueError('mixed_chunk_scope')
        vectors=list(vectors) if vectors is not None else [None]*len(chunks)
        if len(vectors)!=len(chunks): raise ValueError('chunk_vector_count_mismatch')
        docs=[]
        for chunk,vector in zip(chunks,vectors):
            docs.append(Evidence(
                chunk.source_id,chunk.text,kind,chunk.campaign_id,chunk.system_id,authority,secret,
                tuple(vector) if vector is not None else None,chunk.page_start,chunk.section,chunk.language,
                chunk.source_type,chunk.canonicality,int(chunk.priority),{
                    'page_end':chunk.page_end,'heading_path':list(chunk.heading_path),'content_hash':chunk.content_hash,
                    'chunk_index':chunk.chunk_index,'previous_chunk_id':chunk.previous_chunk_id,'next_chunk_id':chunk.next_chunk_id,
                    'embedding_model_id':getattr(chunk,'embedding_model_id',None),'embedding_model_version':getattr(chunk,'embedding_model_version',None),
                }
            ))
        self.replace_source(next(iter(source_ids)),docs)
        return [d.checksum for d in docs]

    def _cosine(self,a,b):
        if a is None or b is None or len(a)!=len(b): return 0.0
        scale_a=max(abs(x) for x in a); scale_b=max(abs(x) for x in b)
        if not scale_a or not scale_b:return 0.0
        normalized_a=[x/scale_a for x in a]; normalized_b=[x/scale_b for x in b]
        dot=sum(x*y for x,y in zip(normalized_a,normalized_b)); na=math.sqrt(sum(x*x for x in normalized_a)); nb=math.sqrt(sum(x*x for x in normalized_b))
        return dot/(na*nb) if na and nb else 0.0
    def _ranked(self,query,campaign_id,system_id,kinds=None,allow_secret=False,limit=6,query_vector=None):
        with self._lock:
            if not isinstance(query,str) or not query.strip() or len(query)>self.MAX_QUERY: raise ValueError('invalid_query')
            if not isinstance(campaign_id,str) or not campaign_id: raise ValueError('invalid_campaign_id')
            if not isinstance(system_id,str) or not system_id: raise ValueError('invalid_system_id')
            if not isinstance(allow_secret,bool): raise ValueError('invalid_secret_flag')
            if not isinstance(limit,int) or isinstance(limit,bool) or not 1<=limit<=100: raise ValueError('invalid_limit')
            if kinds is not None:
                if not isinstance(kinds,(set,frozenset,list,tuple)) or not all(isinstance(kind,str) and kind in VALID_KINDS for kind in kinds): raise ValueError('invalid_kinds')
                kinds=frozenset(kinds)
            if query_vector is not None:
                if not isinstance(query_vector,(list,tuple)) or not query_vector or len(query_vector)>self.MAX_VECTOR_DIMENSIONS or not all(is_finite_number(x) for x in query_vector): raise ValueError('invalid_query_vector')
                query_vector=tuple(float(x) for x in query_vector)
            cache_key=(query,campaign_id,system_id,tuple(sorted(kinds)) if kinds else None,allow_secret,limit,query_vector,self.revision)
            cached=self._ranked_cache.get(cache_key)
            if cached is not None:
                return deepcopy(cached)
            q=set(re.findall(r'\w+',query.lower())); scored=[]
            for (c,s,_,_),d in self._docs.items():
                if c!=campaign_id or s!=system_id or (kinds and d.kind not in kinds) or (d.secret and not allow_secret): continue
                toks=set(re.findall(r'\w+',d.text.lower()))
                lexical=len(q&toks)/(1+math.log1p(len(toks)))
                vector=max(0.0,self._cosine(query_vector,d.vector))
                base=0.55*lexical+0.35*vector
                if base<=0:
                    continue
                authority=float(d.authority)
                bonus=CANONICAL_BONUS.get(d.canonicality,0.0)
                priority_bonus=(d.priority-50)/1000.0
                score=max(0.0,base*authority+bonus+priority_bonus)
                if score>0: scored.append((score,lexical,vector,d))
            scored.sort(key=lambda x:(-x[0],-x[3].priority,x[3].source_id,x[3].checksum)); out=[]; per_source={}
            for row in scored:
                d=row[3]
                if per_source.get(d.source_id,0)>=2: continue
                out.append(row); per_source[d.source_id]=per_source.get(d.source_id,0)+1
                if len(out)>=limit: break
            self._ranked_cache[cache_key]=deepcopy(out)
            if len(self._ranked_cache)>256:
                self._ranked_cache.pop(next(iter(self._ranked_cache)))
            return deepcopy(out)

    def retrieve(self,query,campaign_id,system_id,kinds=None,allow_secret=False,limit=6,query_vector=None):
        return [deepcopy(d) for _,_,_,d in self._ranked(query,campaign_id,system_id,kinds,allow_secret,limit,query_vector)]

    def retrieve_bundle(self,query,campaign_id,system_id,kinds=None,allow_secret=False,limit=6,query_vector=None,threshold=.08):
        """Typed evidence bundle with source/page/canonicality/conflict metadata."""
        if not is_finite_number(threshold) or not 0<=threshold<=1:
            raise ValueError('invalid_retrieval_threshold')
        ranked=self._ranked(query,campaign_id,system_id,kinds,allow_secret,limit,query_vector)
        evidence=[]
        for score,lexical,vector,d in ranked:
            evidence.append(RuleEvidence(
                d.checksum,d.source_id,d.text,d.page,d.section,vector or None,lexical or None,float(score),d.canonicality,d.priority,
            ))
        confidence=float(evidence[0].final_score) if evidence else 0.0
        conflicts=[]
        for i,left in enumerate(evidence):
            for right in evidence[i+1:]:
                if left.section and left.section==right.section and left.text!=right.text and left.canonicality!=right.canonicality:
                    conflicts.append(RuleConflict(left.chunk_id,right.chunk_id,'same_section_different_authority'))
        return RuleEvidenceBundle(str(query),tuple(evidence),confidence,tuple(conflicts),confidence<threshold)
