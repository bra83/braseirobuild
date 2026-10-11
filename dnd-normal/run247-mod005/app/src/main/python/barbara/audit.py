from copy import deepcopy
import hashlib
import json


class AuditTrail:
    """Local operational audit without exposing LLM chain-of-thought.

    The trail records only observable engine inputs/outputs: intent, executable rule
    provenance, retrieval provenance, RNG trace, typed effects and canonical event IDs.
    """

    def _digest(self,value):
        if value in (None,[],{}): return None
        raw=json.dumps(value,ensure_ascii=False,sort_keys=True,separators=(',',':'),default=str).encode()
        return hashlib.sha256(raw).hexdigest()

    def record_resolution(self,*,intent=None,rule_refs=None,modifiers=None,rng_trace=None,effects=None,event_ids=None,state_version=None,retrieval=None,transaction_id=None,resolution_id=None,system_version=None,rules_revision=None):
        retrieval=deepcopy(retrieval) if isinstance(retrieval,dict) else None
        evidence=deepcopy((retrieval or {}).get('evidence',[]))
        return {
            'intent':deepcopy(intent),
            'rule_refs':deepcopy(rule_refs or []),
            'modifiers':deepcopy(modifiers or []),
            'rng_trace':deepcopy(rng_trace),
            'rng_trace_digest':self._digest(rng_trace),
            'effects':deepcopy(effects or []),
            'event_ids':deepcopy(event_ids or []),
            'state_version':state_version,
            'transaction_id':transaction_id,
            'resolution_id':resolution_id,
            'system_version':system_version,
            'rules_revision':rules_revision,
            'retrieval':retrieval,
            'rule_evidence':evidence,
        }


class MasterEvidenceProjection:
    """Human-facing operational evidence panel projection.

    It intentionally surfaces source/page/score/rule/effect/event facts rather than
    model reasoning or hidden prompts.
    """

    def build(self,result):
        resolution=result.get('resolution') or {}
        metadata=resolution.get('metadata') or {}
        retrieval=result.get('retrieval') if isinstance(result.get('retrieval'),dict) else {}
        return {
            'rules':deepcopy(resolution.get('rule_refs',[])),
            'rule_source':metadata.get('rule_source'),
            'rules_revision':metadata.get('rules_revision'),
            'system_version':resolution.get('system_version') or metadata.get('system_version'),
            'modifiers':deepcopy(resolution.get('modifiers',[])),
            'rolls':deepcopy(resolution.get('rolls',[])),
            'roll':resolution.get('roll'),
            'total':resolution.get('total'),
            'target':resolution.get('target'),
            'margin':resolution.get('margin'),
            'outcome':resolution.get('outcome'),
            'rng_trace_digest':(result.get('audit') or {}).get('rng_trace_digest'),
            'effects':deepcopy(result.get('effects_applied',[])),
            'events':deepcopy(result.get('event_ids',[])),
            'transaction_id':result.get('transaction_id'),
            'resolution_id':result.get('resolution_id'),
            'state_version':result.get('state_version'),
            'retrieval_confidence':retrieval.get('confidence'),
            'insufficient_evidence':retrieval.get('insufficient_evidence'),
            'conflicts':deepcopy(retrieval.get('conflicts',[])),
            'sources':deepcopy(retrieval.get('evidence',[])),
        }
