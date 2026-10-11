from copy import deepcopy
from .security import public_view


class KnowledgeBoundary:
    NPC_PUBLIC_FIELDS={
        'name','location','current_activity','mood','disposition','appearance','status','alive',
        'known_facts','heard_rumors','knowledge_public','relationships_public'
    }
    def visible_npcs(self,state):
        out={}
        for npc_id,npc in state.npcs.items():
            if not isinstance(npc,dict): continue
            if npc.get('location')!=state.location: continue
            public={k:deepcopy(v) for k,v in npc.items() if k in self.NPC_PUBLIC_FIELDS}
            cleaned=public_view(public)
            if cleaned: out[npc_id]=cleaned
        return out

    def npc_knows(self,state,npc_id,fact_id):
        npc=state.npcs.get(npc_id)
        if not isinstance(npc,dict): return False
        known=npc.get('known_facts',[])
        if isinstance(known,dict): return fact_id in known
        if isinstance(known,(list,tuple,set)): return fact_id in known
        return False

    def fact_visible_to_player(self,state,fact_id):
        if fact_id not in state.facts: return False
        fact=state.facts[fact_id]
        if not isinstance(fact,dict): return True
        visibility=fact.get('visibility','public')
        if visibility=='public': return True
        known_by=fact.get('known_by',[])
        return 'player' in known_by

    def npc_context(self,state,npc_id):
        npc=state.npcs.get(npc_id)
        if not isinstance(npc,dict): raise ValueError('npc_not_found')
        known_ids=npc.get('known_facts',[])
        if isinstance(known_ids,dict): known_ids=list(known_ids)
        if not isinstance(known_ids,(list,tuple,set)): known_ids=[]
        facts={fid:deepcopy(state.facts[fid]) for fid in known_ids if fid in state.facts}
        return {
            'npc_id':npc_id,
            'facts':public_view(facts),
            'beliefs':public_view(deepcopy(npc.get('beliefs',{}))),
            'heard_rumors':public_view(deepcopy(npc.get('heard_rumors',[]))),
            'memory':public_view(deepcopy(npc.get('memory',[]))),
        }


class LoreDisclosureGate:
    """RAG retrieval is not permission to reveal lore."""

    def filter_evidence(self,state,evidence,knowledge=None):
        knowledge=knowledge or KnowledgeBoundary()
        out=[]
        for item in evidence:
            if getattr(item,'secret',False):
                continue
            metadata=getattr(item,'metadata',None) or {}
            if not isinstance(metadata,dict): metadata={}
            visibility=metadata.get('visibility','public')
            if visibility=='public':
                out.append(item); continue
            known_by=set(metadata.get('known_by',[]))
            if 'player' in known_by:
                out.append(item)
        return out

    def can_reveal_fact(self,state,fact_id,knowledge=None):
        return (knowledge or KnowledgeBoundary()).fact_visible_to_player(state,fact_id)
