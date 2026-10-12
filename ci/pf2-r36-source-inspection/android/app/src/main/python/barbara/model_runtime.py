from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import hashlib
import json


@dataclass(frozen=True)
class ModelProfile:
    model_id:str
    family:str
    version:str
    file_name:str
    sha256:str
    min_ram_mb:int
    capabilities:frozenset[str]
    preferred_backends:tuple[str,...]=('gpu','cpu')


class ModelRepository:
    def __init__(self,root): self.root=Path(root); self.root.mkdir(parents=True,exist_ok=True)
    def _manifest(self): return self.root/'models.json'
    def _read(self):
        if not self._manifest().exists(): return {}
        return json.loads(self._manifest().read_text())
    def _write(self,data): self._manifest().write_text(json.dumps(data,sort_keys=True,indent=2))
    def install(self,profile,source_path):
        if not isinstance(profile,ModelProfile): raise ValueError('invalid_model_profile')
        raw=Path(source_path).read_bytes(); digest=hashlib.sha256(raw).hexdigest()
        if digest!=profile.sha256: raise ValueError('model_hash_mismatch')
        dest=self.root/profile.file_name; dest.write_bytes(raw)
        data=self._read(); data[profile.model_id]={'family':profile.family,'version':profile.version,'file_name':profile.file_name,'sha256':digest,'min_ram_mb':profile.min_ram_mb,'capabilities':sorted(profile.capabilities),'preferred_backends':list(profile.preferred_backends)}; self._write(data)
        return dest
    def remove(self,model_id):
        data=self._read(); item=data.pop(model_id,None)
        if item: (self.root/item['file_name']).unlink(missing_ok=True); self._write(data)
    def installed(self): return self._read()
    def verify(self,model_id):
        item=self._read().get(model_id)
        if not item:return False
        path=self.root/item['file_name']
        return path.exists() and hashlib.sha256(path.read_bytes()).hexdigest()==item['sha256']


class DeviceCapabilityDetector:
    def detect(self,*,ram_mb,cpu=True,gpu=False,npu=False,thermal='normal'):
        if not isinstance(ram_mb,int) or ram_mb<1: raise ValueError('invalid_ram')
        return {'ram_mb':ram_mb,'cpu':bool(cpu),'gpu':bool(gpu),'npu':bool(npu),'thermal':thermal}


class BackendPolicy:
    def choose(self,profile,capabilities,benchmarks=None):
        if capabilities['ram_mb']<profile.min_ram_mb: raise RuntimeError('insufficient_device_memory')
        benchmarks=benchmarks or {}
        available=[b for b in profile.preferred_backends if capabilities.get(b,False)]
        if not available and capabilities.get('cpu'): available=['cpu']
        if not available: raise RuntimeError('no_supported_backend')
        if benchmarks:
            available.sort(key=lambda b:benchmarks.get(b,float('inf')))
        return available[0]


class ModelResidencyManager:
    STATES={'UNLOADED','DOWNLOADING','LOADING','READY','BUSY','COOLING','UNLOADING','ERROR'}
    TRANSITIONS={
        'UNLOADED':{'DOWNLOADING','LOADING','ERROR'}, 'DOWNLOADING':{'LOADING','ERROR','UNLOADED'},
        'LOADING':{'READY','ERROR','UNLOADING'}, 'READY':{'BUSY','COOLING','UNLOADING','ERROR'},
        'BUSY':{'READY','COOLING','ERROR'}, 'COOLING':{'READY','UNLOADING','ERROR'},
        'UNLOADING':{'UNLOADED','ERROR'}, 'ERROR':{'UNLOADED','LOADING'},
    }
    def __init__(self): self.state='UNLOADED'; self.model_id=None
    def transition(self,new_state,model_id=None):
        if new_state not in self.STATES or new_state not in self.TRANSITIONS[self.state]: raise ValueError('invalid_model_residency_transition')
        self.state=new_state
        if model_id is not None:self.model_id=model_id
        if new_state=='UNLOADED': self.model_id=None
        return {'state':self.state,'model_id':self.model_id}
