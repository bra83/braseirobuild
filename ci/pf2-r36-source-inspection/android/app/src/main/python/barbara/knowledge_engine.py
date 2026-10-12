from __future__ import annotations

from copy import deepcopy
from dataclasses import dataclass, field, replace
import hashlib
import re
import io
import json
import sqlite3
from pathlib import Path
from threading import RLock

from .numeric import is_finite_number


@dataclass(frozen=True)
class EmbeddingModelDescriptor:
    model_id: str
    version: str
    dimensions: int
    max_tokens: int
    normalization: str
    tokenizer_id: str
    languages: tuple[str, ...] = ('pt','en')

    def __post_init__(self):
        if not self.model_id or not self.version or not self.tokenizer_id:
            raise ValueError('invalid_embedding_descriptor')
        if not isinstance(self.dimensions,int) or isinstance(self.dimensions,bool) or self.dimensions<1:
            raise ValueError('invalid_embedding_dimensions')
        if not isinstance(self.max_tokens,int) or isinstance(self.max_tokens,bool) or self.max_tokens<1:
            raise ValueError('invalid_embedding_max_tokens')
        if self.normalization not in {'l2','none'}:
            raise ValueError('invalid_embedding_normalization')


@dataclass(frozen=True)
class DocumentChunk:
    chunk_id: str
    source_id: str
    system_id: str
    campaign_id: str
    text: str
    page_start: int | None = None
    page_end: int | None = None
    section: str | None = None
    heading_path: tuple[str, ...] = ()
    language: str = 'pt'
    source_type: str = 'official_rule'
    canonicality: str = 'official'
    priority: int = 50
    content_hash: str = ''
    chunk_index: int = 0
    previous_chunk_id: str | None = None
    next_chunk_id: str | None = None
    embedding_model_id: str | None = None
    embedding_model_version: str | None = None


class SemanticChunker:
    """Structure-aware chunker: headings/pages/paragraphs are preserved, not blind 300/50 slices."""

    def __init__(self, target_chars=1400, overlap_chars=160):
        if not isinstance(target_chars,int) or target_chars<300: raise ValueError('invalid_chunk_target')
        if not isinstance(overlap_chars,int) or not 0<=overlap_chars<target_chars: raise ValueError('invalid_chunk_overlap')
        self.target_chars=target_chars; self.overlap_chars=overlap_chars

    def _blocks(self, text):
        blocks=[]; heading=[]
        for raw in re.split(r'\n+',str(text)):
            line=raw.strip()
            if not line: continue
            is_heading = bool(re.match(r'^(#{1,6}\s+|[A-ZÁÉÍÓÚÇ0-9][A-ZÁÉÍÓÚÇ0-9 :–—-]{3,}$)',line))
            if is_heading:
                title=re.sub(r'^#{1,6}\s+','',line).strip(); heading=[title]; blocks.append(('heading',title,tuple(heading)))
            else:
                blocks.append(('text',line,tuple(heading)))
        return blocks

    def chunk_pages(self, pages, *, source_id, system_id, campaign_id, language='pt', source_type='official_rule', canonicality='official', priority=50):
        if not isinstance(pages,list): raise ValueError('invalid_document_pages')
        staged=[]; current=[]; current_chars=0; start_page=None; end_page=None; heading_path=()
        def flush():
            nonlocal current,current_chars,start_page,end_page,heading_path
            if not current: return
            text='\n'.join(current).strip()
            staged.append({'text':text,'page_start':start_page,'page_end':end_page,'heading_path':heading_path})
            overlap=text[-self.overlap_chars:].strip() if self.overlap_chars else ''
            current=[overlap] if overlap else []; current_chars=len(overlap); start_page=end_page if overlap else None
        for page in pages:
            if not isinstance(page,dict) or not isinstance(page.get('page'),int) or not isinstance(page.get('text'),str): raise ValueError('invalid_document_page')
            for kind,block,hpath in self._blocks(page['text']):
                if kind=='heading':
                    if current and current_chars>self.target_chars//2: flush()
                    heading_path=hpath
                if current and current_chars+len(block)+1>self.target_chars: flush()
                if start_page is None: start_page=page['page']
                end_page=page['page']; current.append(block); current_chars+=len(block)+1
        flush()
        chunks=[]
        for i,item in enumerate(staged):
            digest=hashlib.sha256((source_id+'\0'+item['text']).encode()).hexdigest()
            chunks.append(DocumentChunk(
                chunk_id=f'{source_id}:{digest[:20]}',source_id=source_id,system_id=system_id,campaign_id=campaign_id,
                text=item['text'],page_start=item['page_start'],page_end=item['page_end'],section=(item['heading_path'][-1] if item['heading_path'] else None),
                heading_path=item['heading_path'],language=language,source_type=source_type,canonicality=canonicality,priority=priority,
                content_hash=hashlib.sha256(item['text'].encode()).hexdigest(),chunk_index=i,
            ))
        linked=[]
        for i,c in enumerate(chunks):
            linked.append(DocumentChunk(**{**c.__dict__,'previous_chunk_id':chunks[i-1].chunk_id if i else None,'next_chunk_id':chunks[i+1].chunk_id if i+1<len(chunks) else None}))
        return linked


class PdfExtractor:
    """Optional local PDF text extractor; no network and no OCR dependency.

    pypdf is imported lazily so Android/core installs that ingest pre-extracted pages
    remain dependency-light. Scanned/image-only pages are reported, never fabricated.
    """
    def extract(self, source):
        try:
            from pypdf import PdfReader
        except ImportError as exc:
            raise RuntimeError('pdf_extractor_unavailable') from exc
        if isinstance(source,(bytes,bytearray)):
            stream=io.BytesIO(bytes(source))
        elif isinstance(source,(str,Path)):
            stream=str(source)
        else:
            raise ValueError('invalid_pdf_source')
        try:
            reader=PdfReader(stream)
        except Exception as exc:
            raise ValueError('invalid_pdf_document') from exc
        pages=[]; image_only=[]
        for index,page in enumerate(reader.pages,1):
            try: raw=page.extract_text() or ''
            except Exception: raw=''
            text='\n'.join(' '.join(line.split()) for line in raw.replace('\x00','').splitlines()).strip()
            if not text: image_only.append(index)
            pages.append({'page':index,'text':text})
        if not pages: raise ValueError('empty_pdf_document')
        return {'pages':pages,'page_count':len(pages),'image_only_pages':image_only}


class DocumentIngestor:
    def __init__(self, chunker=None, pdf_extractor=None): self.chunker=chunker or SemanticChunker(); self.pdf_extractor=pdf_extractor or PdfExtractor()
    def ingest_pages(self, pages, **metadata): return self.chunker.chunk_pages(pages,**metadata)
    def ingest_pdf(self, source, **metadata):
        extracted=self.pdf_extractor.extract(source)
        chunks=self.ingest_pages(extracted['pages'],**metadata)
        return {'chunks':chunks,'page_count':extracted['page_count'],'image_only_pages':tuple(extracted['image_only_pages']),'source_hash':self.source_hash(source)}
    def source_hash(self,source):
        if isinstance(source,(bytes,bytearray)): raw=bytes(source)
        elif isinstance(source,(str,Path)): raw=Path(source).read_bytes()
        else: raise ValueError('invalid_document_source')
        return hashlib.sha256(raw).hexdigest()


@dataclass(frozen=True)
class RuleConflict:
    left_chunk_id: str
    right_chunk_id: str
    reason: str


@dataclass(frozen=True)
class RuleEvidence:
    chunk_id: str
    source_id: str
    text: str
    page: int | None
    section: str | None
    semantic_score: float | None
    lexical_score: float | None
    final_score: float
    canonicality: str
    priority: int


@dataclass(frozen=True)
class RuleEvidenceBundle:
    query: str
    evidence: tuple[RuleEvidence, ...]
    confidence: float
    conflicts: tuple[RuleConflict, ...] = ()
    insufficient_evidence: bool = False


class RuleResolutionCache:
    def __init__(self,max_items=512): self.max_items=max_items; self._items={}; self._order=[]
    def key(self,system_version,normalized_intent,context_tags,rules_revision):
        raw=repr((system_version,normalized_intent,tuple(sorted(context_tags)),rules_revision)).encode(); return hashlib.sha256(raw).hexdigest()
    def get(self,key): return deepcopy(self._items.get(key))
    def put(self,key,value):
        if key not in self._items: self._order.append(key)
        self._items[key]=deepcopy(value)
        while len(self._order)>self.max_items:
            old=self._order.pop(0); self._items.pop(old,None)


class RuleCompiler:
    """Compiles explicitly structured authoritative rule records; it never invents rules from prose."""
    ALLOWED_OPS={'compare','roll','modifier','effect','cost','requirement'}
    def compile(self, rule):
        if not isinstance(rule,dict): raise ValueError('invalid_structured_rule')
        required={'rule_id','system_id','version','when','operations','source_refs'}
        if not required <= set(rule): raise ValueError('incomplete_structured_rule')
        if not isinstance(rule['operations'],list) or not rule['operations']: raise ValueError('invalid_rule_operations')
        for op in rule['operations']:
            if not isinstance(op,dict) or op.get('op') not in self.ALLOWED_OPS: raise ValueError('unsupported_rule_operation')
        out=deepcopy(rule); out['compiled']=True; out['runtime']='barbara_rules_ast_v1'; return out


class IngestionCheckpointStore:
    """Resumable ingestion checkpoints, optionally persisted in SQLite.

    Completion and staged embedding vectors are persisted independently from the
    canonical RAG store. The source becomes visible to retrieval only after all
    chunks complete and the RAG source replacement succeeds atomically.
    """
    def __init__(self, db_path=None):
        self._lock=RLock()
        self._items={}
        self.db_path=db_path
        self._db=sqlite3.connect(db_path,timeout=30,check_same_thread=False) if db_path else None
        if self._db:
            self._db.execute('PRAGMA journal_mode=WAL')
            self._db.execute(
                'CREATE TABLE IF NOT EXISTS ingestion_checkpoint('
                'source_id TEXT, source_hash TEXT, embedding_model_id TEXT, embedding_version TEXT, '
                'total_chunks INTEGER, completed_json TEXT, vectors_json TEXT, status TEXT, '
                'PRIMARY KEY(source_id,source_hash,embedding_model_id,embedding_version))'
            )
            self._db.commit(); self._load()

    def close(self):
        with self._lock:
            if self._db:
                self._db.close(); self._db=None

    def _key(self,source_id,source_hash,embedding_model_id,embedding_version):
        values=(source_id,source_hash,embedding_model_id,embedding_version)
        if not all(isinstance(v,str) and v for v in values): raise ValueError('invalid_ingestion_checkpoint_key')
        return values

    def _load(self):
        with self._lock:
            self._items={}
            for src,h,mid,ver,total,completed_raw,vectors_raw,status in self._db.execute(
                'SELECT source_id,source_hash,embedding_model_id,embedding_version,total_chunks,completed_json,vectors_json,status FROM ingestion_checkpoint'
            ):
                try:
                    if not isinstance(total,int) or isinstance(total,bool):raise ValueError
                    completed_raw_value=json.loads(completed_raw or '[]')
                    vectors_raw_value=json.loads(vectors_raw or '{}') or {}
                    if total<1 or not isinstance(completed_raw_value,list) or not all(isinstance(x,int) and not isinstance(x,bool) and 0<=x<total for x in completed_raw_value):raise ValueError
                    if not isinstance(vectors_raw_value,dict):raise ValueError
                    completed=set(completed_raw_value); vectors={}
                    for key,vector in vectors_raw_value.items():
                        if not isinstance(key,str) or not key.isdigit():raise ValueError
                        index=int(key)
                        if not 0<=index<total or not isinstance(vector,list) or not vector or not all(is_finite_number(x) for x in vector):raise ValueError
                        vectors[index]=vector
                    if status not in {'running','complete',None}:raise ValueError
                except (TypeError,ValueError,json.JSONDecodeError) as exc:
                    raise ValueError('corrupt_ingestion_checkpoint') from exc
                self._items[(src,h,mid,ver)]={
                    'source_id':src,'source_hash':h,'embedding_model_id':mid,'embedding_version':ver,
                    'total_chunks':total,'completed':completed,'vectors':vectors,'status':status or 'running'
                }

    def _persist(self,key):
        if not self._db:return
        state=self._items[key]
        with self._db:
            self._db.execute(
                'INSERT OR REPLACE INTO ingestion_checkpoint VALUES(?,?,?,?,?,?,?,?)',
                (key[0],key[1],key[2],key[3],state['total_chunks'],json.dumps(sorted(state['completed'])),json.dumps({str(k):v for k,v in state.get('vectors',{}).items()},sort_keys=True),state['status'])
            )

    def begin(self,source_id,source_hash,embedding_model_id,embedding_version,total_chunks):
        with self._lock:
            key=self._key(source_id,source_hash,embedding_model_id,embedding_version)
            if not isinstance(total_chunks,int) or isinstance(total_chunks,bool) or total_chunks<1: raise ValueError('invalid_ingestion_chunk_count')
            state=self._items.get(key)
            if state is None:
                state={'source_id':source_id,'source_hash':source_hash,'embedding_model_id':embedding_model_id,'embedding_version':embedding_version,'total_chunks':total_chunks,'completed':set(),'vectors':{},'status':'running'}
                self._items[key]=state
            elif state['total_chunks']!=total_chunks:
                # Same source hash/model must deterministically produce the same chunks.
                raise ValueError('ingestion_chunk_count_conflict')
            self._persist(key)
            return deepcopy({**state,'completed':sorted(state['completed']),'vectors':deepcopy(state.get('vectors',{}))})

    def stage_vector(self,source_id,source_hash,embedding_model_id,embedding_version,chunk_index,vector):
        with self._lock:
            key=self._key(source_id,source_hash,embedding_model_id,embedding_version); state=self._items.get(key)
            if state is None: raise KeyError('ingestion_checkpoint_not_found')
            if not isinstance(chunk_index,int) or isinstance(chunk_index,bool): raise ValueError('invalid_ingestion_chunk_index')
            if not 0<=chunk_index<state['total_chunks']: raise ValueError('invalid_ingestion_chunk_index')
            if vector is not None:
                if not isinstance(vector,(list,tuple)) or not vector or not all(is_finite_number(x) for x in vector):
                    raise ValueError('invalid_ingestion_vector')
                state.setdefault('vectors',{})[chunk_index]=[float(x) for x in vector]
            self._persist(key)

    def mark(self,source_id,source_hash,embedding_model_id,embedding_version,chunk_index):
        with self._lock:
            key=self._key(source_id,source_hash,embedding_model_id,embedding_version); state=self._items.get(key)
            if state is None: raise KeyError('ingestion_checkpoint_not_found')
            if not isinstance(chunk_index,int) or isinstance(chunk_index,bool): raise ValueError('invalid_ingestion_chunk_index')
            if not 0<=chunk_index<state['total_chunks']: raise ValueError('invalid_ingestion_chunk_index')
            state['completed'].add(chunk_index); state['status']='complete' if len(state['completed'])>=state['total_chunks'] else 'running'
            self._persist(key); return state['status']

    def pending(self,source_id,source_hash,embedding_model_id,embedding_version):
        with self._lock:
            key=self._key(source_id,source_hash,embedding_model_id,embedding_version); state=self._items.get(key)
            if state is None:return []
            return [i for i in range(state['total_chunks']) if i not in state['completed']]

    def staged_vectors(self,source_id,source_hash,embedding_model_id,embedding_version):
        with self._lock:
            key=self._key(source_id,source_hash,embedding_model_id,embedding_version); state=self._items.get(key)
            if state is None: raise KeyError('ingestion_checkpoint_not_found')
            return deepcopy(state.get('vectors',{}))

    def needs_reindex(self,source_id,source_hash,embedding_model_id,embedding_version):
        with self._lock:
            key=self._key(source_id,source_hash,embedding_model_id,embedding_version)
            matching=[k for k in self._items if k[0]==source_id]
            return bool(matching and key not in self._items)


class KnowledgeIngestionPipeline:
    """Real ingestion orchestrator tying extraction/chunking/embeddings/checkpoints to RAG.

    It is intentionally provider-agnostic and local-first. Partial embedding work may
    be resumed, while retrieval sees either the previous complete source revision or
    the new complete revision — never a half-indexed document.
    """
    def __init__(self, rag, *, ingestor=None, checkpoints=None, embedder=None):
        if rag is None: raise ValueError('missing_rag_store')
        self.rag=rag
        self.ingestor=ingestor or DocumentIngestor()
        self.checkpoints=checkpoints or IngestionCheckpointStore()
        self.embedder=embedder

    def _embedding_identity(self):
        if self.embedder is None:return ('lexical-only','1')
        descriptor=getattr(self.embedder,'descriptor',None)
        if descriptor is not None:
            model_id=getattr(descriptor,'model_id',None) if not isinstance(descriptor,dict) else descriptor.get('model_id')
            version=getattr(descriptor,'version',None) if not isinstance(descriptor,dict) else descriptor.get('version')
            if model_id and version:return (str(model_id),str(version))
        return (self.embedder.__class__.__name__ or 'embedder','1')

    def _embed(self,text):
        if self.embedder is None:return None
        fn=getattr(self.embedder,'embed',None)
        if not callable(fn):raise ValueError('invalid_embedder')
        vector=fn(text)
        if not isinstance(vector,(list,tuple)) or not vector or not all(is_finite_number(x) for x in vector):
            raise ValueError('invalid_ingestion_vector')
        descriptor=getattr(self.embedder,'descriptor',None)
        dimensions=getattr(descriptor,'dimensions',None) if not isinstance(descriptor,dict) else descriptor.get('dimensions')
        if dimensions is not None and len(vector)!=dimensions: raise ValueError('embedding_dimension_mismatch')
        return [float(x) for x in vector]

    def _commit_chunks(self,chunks,source_hash,*,kind='RULE',authority=1.0,secret=False):
        chunks=list(chunks)
        if not chunks: raise ValueError('empty_ingestion_source')
        source_id=chunks[0].source_id; model_id,version=self._embedding_identity()
        self.checkpoints.begin(source_id,source_hash,model_id,version,len(chunks))
        pending=self.checkpoints.pending(source_id,source_hash,model_id,version)
        for index in pending:
            vector=self._embed(chunks[index].text)
            self.checkpoints.stage_vector(source_id,source_hash,model_id,version,index,vector)
            self.checkpoints.mark(source_id,source_hash,model_id,version,index)
        if self.checkpoints.pending(source_id,source_hash,model_id,version):
            raise RuntimeError('ingestion_incomplete')
        staged=self.checkpoints.staged_vectors(source_id,source_hash,model_id,version)
        vectors=None
        if self.embedder is not None:
            if any(i not in staged for i in range(len(chunks))): raise RuntimeError('ingestion_vectors_incomplete')
            vectors=[staged[i] for i in range(len(chunks))]
        enriched=[replace(c,embedding_model_id=(model_id if self.embedder is not None else None),embedding_model_version=(version if self.embedder is not None else None)) for c in chunks]
        checksums=self.rag.ingest_chunks(enriched,kind=kind,vectors=vectors,authority=authority,secret=secret)
        return {
            'source_id':source_id,'source_hash':source_hash,'chunks':len(enriched),'checksums':tuple(checksums),
            'embedding_model_id':model_id,'embedding_version':version,'status':'complete',
            'rag_revision':getattr(self.rag,'revision',None),
        }

    def ingest_pages(self,pages,*,source_id,system_id,campaign_id,kind='RULE',authority=1.0,secret=False,**metadata):
        normalized=[deepcopy(p) for p in pages]
        raw=json.dumps(normalized,ensure_ascii=False,sort_keys=True,separators=(',',':')).encode()
        source_hash=hashlib.sha256(raw).hexdigest()
        chunks=self.ingestor.ingest_pages(normalized,source_id=source_id,system_id=system_id,campaign_id=campaign_id,**metadata)
        return self._commit_chunks(chunks,source_hash,kind=kind,authority=authority,secret=secret)

    def ingest_pdf(self,source,*,source_id,system_id,campaign_id,kind='RULE',authority=1.0,secret=False,**metadata):
        extracted=self.ingestor.ingest_pdf(source,source_id=source_id,system_id=system_id,campaign_id=campaign_id,**metadata)
        result=self._commit_chunks(extracted['chunks'],extracted['source_hash'],kind=kind,authority=authority,secret=secret)
        result['page_count']=extracted['page_count']; result['image_only_pages']=tuple(extracted['image_only_pages'])
        return result
