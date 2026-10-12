from copy import deepcopy
from collections import deque


class MigrationRegistry:
    """Versioned, deterministic migration graph for campaigns/SystemModules.

    Each step is explicit and side-effect free. The registry can chain compatible
    versions, detects ambiguity/cycles by shortest-path traversal and never mutates
    the caller's document.
    """
    def __init__(self): self._steps={}
    def register(self,system_id,from_version,to_version,fn):
        if not all(isinstance(x,str) and x for x in (str(system_id),str(from_version),str(to_version))): raise ValueError('invalid_migration_version')
        if str(from_version)==str(to_version): raise ValueError('invalid_migration_cycle')
        key=(system_id,str(from_version),str(to_version))
        if key in self._steps: raise ValueError('duplicate_migration')
        if not callable(fn): raise ValueError('invalid_migration')
        self._steps[key]=fn

    def path(self,system_id,from_version,to_version):
        start,target=str(from_version),str(to_version)
        if start==target:return ()
        adjacency={}
        for (sid,a,b),fn in self._steps.items():
            if sid==system_id: adjacency.setdefault(a,[]).append((b,fn))
        q=deque([(start,())]); seen={start}
        while q:
            version,steps=q.popleft()
            for next_version,fn in sorted(adjacency.get(version,[]),key=lambda x:x[0]):
                edge=(version,next_version,fn)
                if next_version==target:return steps+(edge,)
                if next_version not in seen:
                    seen.add(next_version); q.append((next_version,steps+(edge,)))
        raise LookupError('migration_not_found')

    def migrate(self,document,system_id,from_version,to_version):
        if not isinstance(document,dict): raise ValueError('invalid_migration_document')
        out=deepcopy(document)
        for old,new,fn in self.path(system_id,from_version,to_version):
            candidate=fn(deepcopy(out))
            if not isinstance(candidate,dict): raise ValueError('invalid_migration_result')
            out=candidate
        return out

    def register_declarative(self,system_id,definition):
        """Register safe data-only set/rename/delete migrations from a module manifest."""
        if not isinstance(definition,dict): raise ValueError('invalid_module_migration')
        allowed={'from','to','operations'}; required={'from','to'}
        if set(definition)-allowed or not required<=set(definition): raise ValueError('invalid_module_migration')
        ops=deepcopy(definition.get('operations',[]))
        if not isinstance(ops,list): raise ValueError('invalid_module_migration')
        for op in ops:
            if not isinstance(op,dict) or op.get('op') not in {'set','rename','delete'}: raise ValueError('unsupported_migration_operation')
        def apply(doc):
            def parent(root,path,create=False):
                parts=str(path).split('.')
                if not parts or any(not p or p.startswith('_') for p in parts): raise ValueError('invalid_migration_path')
                cur=root
                for part in parts[:-1]:
                    if part not in cur:
                        if not create: return None,parts[-1]
                        cur[part]={}
                    if not isinstance(cur[part],dict): raise ValueError('migration_path_conflict')
                    cur=cur[part]
                return cur,parts[-1]
            out=deepcopy(doc)
            for op in ops:
                kind=op['op']
                if kind=='set':
                    if set(op)!={'op','path','value'}: raise ValueError('invalid_migration_operation')
                    p,k=parent(out,op['path'],True); p[k]=deepcopy(op['value'])
                elif kind=='delete':
                    if set(op)!={'op','path'}: raise ValueError('invalid_migration_operation')
                    p,k=parent(out,op['path']);
                    if p is not None:p.pop(k,None)
                else:
                    if set(op)!={'op','from','to'}: raise ValueError('invalid_migration_operation')
                    p,k=parent(out,op['from'])
                    if p is None or k not in p: continue
                    value=p.pop(k); p2,k2=parent(out,op['to'],True); p2[k2]=value
            return out
        self.register(system_id,str(definition['from']),str(definition['to']),apply)
