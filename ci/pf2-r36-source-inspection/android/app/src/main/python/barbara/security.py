PROTECTED_ROOTS={"campaign_id","system_id","tick","facts","world_flags","npcs","factions","rumors","events","economy","clocks","weather","living_world","authorizations","sites","public_ledger","secret_ledger"}
SENSITIVE_KEYS={"private_agenda","secret","secrets","director_notes","has_private_agenda","private_goal","private_goals","hidden_goal","hidden_goals","knowledge_private","gm_only","director_only","secret_ledger"}
PRIVATE_VISIBILITY={"private","director","gm","gm_only","director_only"}
_DROP=object()

def _normalized(value):
    return '_'.join(part for part in ''.join(ch.lower() if ch.isalnum() else '_' for ch in str(value)).split('_') if part)

_SENSITIVE_COMPACT={key.replace('_','') for key in SENSITIVE_KEYS}

def _public(value,seen):
    if isinstance(value,dict):
        identity=id(value)
        if identity in seen:return _DROP
        seen.add(identity)
        private_marker=value.get("private")
        visibility=_normalized(value.get("visibility",""))
        if private_marker is True or str(private_marker).strip().lower() in {"true","yes","private"} or visibility in PRIVATE_VISIBILITY:
            seen.remove(identity)
            return _DROP
        out={}
        try:
            for k,v in value.items():
                normalized=_normalized(k)
                if normalized in SENSITIVE_KEYS or normalized.replace('_','') in _SENSITIVE_COMPACT: continue
                sv=_public(v,seen)
                if sv is not _DROP: out[k]=sv
        finally:
            seen.remove(identity)
        return out
    if isinstance(value,list):
        identity=id(value)
        if identity in seen:return _DROP
        seen.add(identity)
        out=[]
        try:
            for v in value:
                sv=_public(v,seen)
                if sv is not _DROP: out.append(sv)
        finally:
            seen.remove(identity)
        return out
    if isinstance(value,tuple):
        identity=id(value)
        if identity in seen:return _DROP
        seen.add(identity)
        out=[]
        try:
            for v in value:
                sv=_public(v,seen)
                if sv is not _DROP: out.append(sv)
        finally:
            seen.remove(identity)
        return tuple(out)
    return value

def public_view(value):
    cleaned=_public(value,set())
    return {} if cleaned is _DROP else cleaned

def validate_patch(path,value):
    if not isinstance(path,str) or not path or path.startswith('.') or '..' in path:
        raise ValueError('invalid_patch_path')
    parts=path.split('.')
    if any(not p or p.startswith('_') for p in parts): raise ValueError('invalid_patch_path')
    root=parts[0]
    if root in PROTECTED_ROOTS: raise ValueError(f"protected_state:{root}")
    if not root.startswith("player_") and root not in {"scene","notes"}: raise ValueError(f"namespace_not_allowed:{root}")
    return True
