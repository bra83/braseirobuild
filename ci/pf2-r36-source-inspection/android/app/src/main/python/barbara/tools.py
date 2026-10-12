from copy import deepcopy


class ReadOnlyToolRegistry:
    """LLMs may read through tools; mutations must return typed commands for core validation."""
    def __init__(self): self._tools={}
    def register(self,name,fn):
        if not isinstance(name,str) or not name or not callable(fn): raise ValueError('invalid_tool')
        self._tools[name]=fn
    def call(self,name,**kwargs):
        if name not in self._tools: raise KeyError(name)
        result=self._tools[name](**deepcopy(kwargs))
        if isinstance(result,dict) and any(k in result for k in ('state_patch','commit','mutate')): raise ValueError('mutable_tool_result_forbidden')
        return deepcopy(result)
