from copy import deepcopy
import re
from .security import public_view
from .numeric import is_finite_number


class MemoryTaxonomy:
    EPISODIC='episodic'
    SEMANTIC='semantic'
    BELIEF='belief'
    INFERENCE='inference'
    SUMMARY='summary'
    TYPES={EPISODIC,SEMANTIC,BELIEF,INFERENCE,SUMMARY}


class Memory:
    MAX_ITEMS=1000

    def remember(self,state,item,memory_type=None):
        if not isinstance(item,dict): raise ValueError('invalid_memory_item')
        entry=deepcopy(item)
        kind=memory_type or entry.get('memory_type') or MemoryTaxonomy.EPISODIC
        if kind not in MemoryTaxonomy.TYPES: raise ValueError('invalid_memory_type')
        entry['memory_type']=kind
        entry.setdefault('tick',state.tick)
        entry.setdefault('salience',0.5)
        sal=entry['salience']
        if not is_finite_number(sal) or not 0<=sal<=1: raise ValueError('invalid_salience')
        if not isinstance(entry.get('tick'),int) or isinstance(entry.get('tick'),bool) or entry['tick']<0: raise ValueError('invalid_memory_tick')
        # Canonical facts deliberately stay in state.facts. Memory can reference them,
        # but remembering something can never promote it into canonical truth.
        if entry.get('canonical') is True: raise ValueError('memory_cannot_be_canonical_fact')
        state.memory.append(entry)
        if len(state.memory)>self.MAX_ITEMS:
            indexed=list(enumerate(state.memory)); keep_new={i for i,_ in indexed[-self.MAX_ITEMS//2:]}; older=indexed[:-self.MAX_ITEMS//2]
            older.sort(key=lambda x:(-float(x[1].get('salience',0.5)),-int(x[1].get('tick',0)),x[0])); keep_old={i for i,_ in older[:self.MAX_ITEMS-len(keep_new)]}
            state.memory=[v for i,v in indexed if i in keep_new or i in keep_old]
        return deepcopy(entry)

    def remember_episode(self,state,item): return self.remember(state,item,MemoryTaxonomy.EPISODIC)
    def remember_belief(self,state,item): return self.remember(state,item,MemoryTaxonomy.BELIEF)
    def remember_inference(self,state,item): return self.remember(state,item,MemoryTaxonomy.INFERENCE)
    def remember_summary(self,state,item): return self.remember(state,item,MemoryTaxonomy.SUMMARY)

    def _tokens(self,value): return set(re.findall(r'\w+',str(value).lower()))

    def compact_context(self,state,limit=12,query='',location=None,types=None):
        if not isinstance(limit,int) or isinstance(limit,bool) or limit<1: raise ValueError('invalid_memory_limit')
        allowed=set(types) if types is not None else MemoryTaxonomy.TYPES
        if not allowed <= MemoryTaxonomy.TYPES: raise ValueError('invalid_memory_type_filter')
        public=public_view(state.memory); q=self._tokens(query); scored=[]; now=state.tick
        for i,m in enumerate(public):
            if not isinstance(m,dict): continue
            kind=m.get('memory_type',MemoryTaxonomy.EPISODIC)
            if kind not in allowed: continue
            sal=float(m.get('salience',0.5)); tick=int(m.get('tick',0)); text=m.get('text',m.get('summary',m.get('event',m)))
            toks=self._tokens(text); relevance=(len(q&toks)/max(1,len(q))) if q else 0.0
            loc=m.get('location'); locality=1.0 if location and loc==location else 0.0
            recency=1.0/(1.0+max(0,now-tick)); score=0.45*sal+0.30*relevance+0.15*locality+0.10*recency
            scored.append((score,tick,i,m))
        selected=sorted(scored,key=lambda x:(-x[0],-x[1],-x[2]))[:limit]; selected.sort(key=lambda x:(x[1],x[2]))
        return deepcopy([x[3] for x in selected])

    def layers(self,state):
        out={kind:[] for kind in sorted(MemoryTaxonomy.TYPES)}
        for item in public_view(state.memory):
            if isinstance(item,dict): out.setdefault(item.get('memory_type',MemoryTaxonomy.EPISODIC),[]).append(deepcopy(item))
        return out

    def causal_trace(self,state,event_id):
        by={e.get('id'):e for e in state.events if isinstance(e,dict) and e.get('id') is not None}; out=[]; cur=by.get(event_id); seen=set()
        while cur and cur.get('id') not in seen:
            seen.add(cur.get('id')); out.append(deepcopy(cur)); cur=by.get(cur.get('causes'))
        return out
