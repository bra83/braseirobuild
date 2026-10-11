from copy import deepcopy


class KnowledgeGraphProjection:
    """Rebuildable graph projection over canonical facts/events/relationships."""
    def build(self,state):
        nodes={}; edges=[]
        for npc_id,npc in state.npcs.items():
            nodes[f'npc:{npc_id}']={'type':'npc','data':{'name':npc.get('name',npc_id)} if isinstance(npc,dict) else {}}
            if isinstance(npc,dict):
                for other,rel in (npc.get('relationships') or {}).items(): edges.append({'from':f'npc:{npc_id}','to':f'npc:{other}','type':'relationship','data':deepcopy(rel)})
        for faction_id in state.factions:nodes[f'faction:{faction_id}']={'type':'faction','data':{}}
        for fact_id,value in state.facts.items():nodes[f'fact:{fact_id}']={'type':'fact','data':deepcopy(value)}
        return {'nodes':nodes,'edges':edges,'rebuildable':True,'state_version':state.state_version}
