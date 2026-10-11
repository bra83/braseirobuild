import json
from copy import deepcopy
from .state import CampaignState
from .pipeline import ActionPipeline

class HostBridge:
    """Stable JSON boundary for Android/desktop hosts integrating Barbara 1.0."""
    REQUEST_FIELDS={'text','request_id','mechanical','importance','resolution','expected_state_version','resume_action_id','input_channel','defer_narrative'}
    def __init__(self,engine):
        self.engine=engine
        self.pipeline=ActionPipeline(engine)
    def close(self):
        close=getattr(self.engine,'close',None)
        if callable(close):close()
    def new_campaign(self,campaign_id,system_id,**initial):
        state=CampaignState(campaign_id,system_id,**initial); state.validate(); return state.to_json()
    def turn(self,state_json,request):
        if not isinstance(request,dict): raise ValueError('invalid_host_request')
        unknown=set(request)-self.REQUEST_FIELDS
        if unknown: raise ValueError('unknown_host_request_fields:'+','.join(sorted(unknown)))
        if 'request_id' not in request: raise ValueError('missing_host_request_field')
        state=CampaignState.from_json(state_json)
        if request.get('resume_action_id') is not None:
            result=self.pipeline.resume(
                state,
                request['resume_action_id'],
                request['request_id'],
                deepcopy(request.get('resolution')),
                expected_state_version=request.get('expected_state_version'),
                input_channel=request.get('input_channel'),
                defer_narrative=bool(request.get('defer_narrative',False)),
            )
            return {'state':state.to_json(),'result':deepcopy(result)}
        if 'text' not in request: raise ValueError('missing_host_request_field')
        text=request['text']; request_id=request['request_id']
        if not isinstance(text,str) or not text.strip(): raise ValueError('invalid_host_text')
        result=self.pipeline.execute(
            state,
            text,
            request_id,
            mechanical=request.get('mechanical',False),
            importance=request.get('importance','normal'),
            resolution=deepcopy(request.get('resolution')),
            expected_state_version=request.get('expected_state_version'),
            input_channel=request.get('input_channel'),
            defer_narrative=bool(request.get('defer_narrative',False)),
        )
        return {'state':state.to_json(),'result':deepcopy(result)}
    def accept_narrative(self,state_json,request):
        if not isinstance(request,dict): raise ValueError('invalid_host_request')
        required={'text','request_id','result','candidate'}
        if not required<=set(request): raise ValueError('missing_host_request_field')
        unknown=set(request)-required-{'importance'}
        if unknown: raise ValueError('unknown_host_request_fields:'+','.join(sorted(unknown)))
        state=CampaignState.from_json(state_json)
        result=self.pipeline.accept_external_narrative(
            state,request['text'],request['request_id'],deepcopy(request['result']),deepcopy(request['candidate']),request.get('importance','normal')
        )
        return {'state':state.to_json(),'result':result}
    def accept_narrative_json(self,state_json,request_json):
        try: request=json.loads(request_json)
        except (TypeError,json.JSONDecodeError) as exc: raise ValueError('invalid_host_request_json') from exc
        return json.dumps(self.accept_narrative(state_json,request),ensure_ascii=False,sort_keys=True,separators=(',',':'))

    def turn_json(self,state_json,request_json):
        try: request=json.loads(request_json)
        except (TypeError,json.JSONDecodeError) as exc: raise ValueError('invalid_host_request_json') from exc
        return json.dumps(self.turn(state_json,request),ensure_ascii=False,sort_keys=True,separators=(',',':'))

class DurableHostBridge:
    """Server/runtime bridge backed by DurableStateStore CAS transactions."""
    def __init__(self,engine,store,contract=None):
        from .host_contract import HostIntegrationConformance
        self.bridge=HostBridge(engine); self.store=store; self.contract=contract
        if contract is not None: HostIntegrationConformance().validate(contract)
    def close(self):
        try:self.bridge.close()
        finally:
            close=getattr(self.store,'close',None)
            if callable(close):close()
    def new_campaign(self,campaign_id,system_id,**initial):
        if self.contract is not None and self.contract.system_id!=system_id: raise ValueError('host_contract_system_mismatch')
        raw=self.bridge.new_campaign(campaign_id,system_id,**initial)
        state=CampaignState.from_json(raw); self.store.create_campaign(state,self.contract)
        return raw
    def state(self,campaign_id): return self.store.load(campaign_id).to_json()
    def turn(self,campaign_id,request):
        if not isinstance(request,dict) or not isinstance(request.get('request_id'),str): raise ValueError('invalid_host_request')
        fingerprint=self.store.fingerprint(request)
        prior=self.store.lookup_command(campaign_id,request['request_id'],fingerprint)
        if prior is not None:return deepcopy(prior['response'])
        state=self.store.load(campaign_id); expected=state.state_version
        out=self.bridge.turn(state.to_json(),deepcopy(request))
        new_state=CampaignState.from_json(out['state'])
        committed=self.store.compare_and_swap(expected_state_version=expected,new_state=new_state,command_id=request['request_id'],fingerprint=fingerprint,response=out)
        return deepcopy(committed['response'])
