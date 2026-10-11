from __future__ import annotations

from copy import deepcopy
from dataclasses import dataclass
from types import MappingProxyType

BARBARA_PROTOCOL_VERSION = 2

# Domains whose authoritative mutation must belong to exactly one side. Presentation
# domains may be shared because they are projections and can be rebuilt.
CANONICAL_DOMAINS = frozenset({
    'canonical_state', 'rules', 'atlas', 'rag_sources', 'travel', 'time',
    'world', 'inventory', 'combat',
})
PRESENTATION_DOMAINS = frozenset({'narrative', 'vtt', 'audio', 'ui'})
ALL_DOMAINS = CANONICAL_DOMAINS | PRESENTATION_DOMAINS
OWNERS = frozenset({'host', 'barbara', 'shared', 'none'})


def _freeze_dict(value, code='mapping_required'):
    if not isinstance(value, dict):
        raise ValueError(code)
    return MappingProxyType(deepcopy(value))


@dataclass(frozen=True)
class HostIntegrationContract:
    """Versioned boundary between Barbara and a complete external RPG host.

    A host such as Braseiro GURPS may remain authoritative for its database, rules,
    atlas, travel and living-world simulation while Barbara supplies reusable
    narrative, continuity, RAG infrastructure, VTT and audit services. The contract
    makes that ownership explicit so two engines cannot silently mutate the same
    canonical domain.
    """

    system_id: str
    system_version: str
    protocol_version: int
    authority: MappingProxyType
    capabilities: frozenset[str]
    datasets: MappingProxyType
    metadata: MappingProxyType

    def __post_init__(self):
        if not isinstance(self.system_id, str) or not self.system_id or len(self.system_id) > 160:
            raise ValueError('invalid_host_system_id')
        if not isinstance(self.system_version, str) or not self.system_version or len(self.system_version) > 80:
            raise ValueError('invalid_host_system_version')
        if not isinstance(self.protocol_version, int) or isinstance(self.protocol_version, bool) or self.protocol_version <= 0:
            raise ValueError('invalid_barbara_protocol_version')
        if not isinstance(self.authority, MappingProxyType):
            raise ValueError('mutable_host_authority')
        if set(self.authority) != ALL_DOMAINS:
            missing = ALL_DOMAINS - set(self.authority)
            extra = set(self.authority) - ALL_DOMAINS
            detail = ','.join(sorted(missing or extra))
            raise ValueError(('missing_host_authority:' if missing else 'unknown_host_authority:') + detail)
        for domain, owner in self.authority.items():
            if owner not in OWNERS:
                raise ValueError('invalid_host_authority_owner:' + domain)
            if domain in CANONICAL_DOMAINS and owner in {'shared', 'none'}:
                raise ValueError('canonical_domain_requires_single_authority:' + domain)
        if not isinstance(self.capabilities, frozenset) or any(not isinstance(x, str) or not x for x in self.capabilities):
            raise ValueError('invalid_host_capabilities')
        if not isinstance(self.datasets, MappingProxyType) or not isinstance(self.metadata, MappingProxyType):
            raise ValueError('mutable_host_contract')
        for dataset_id, descriptor in self.datasets.items():
            if not isinstance(dataset_id, str) or not dataset_id or not isinstance(descriptor, dict):
                raise ValueError('invalid_host_dataset')
            unknown = set(descriptor) - {'kind', 'namespace', 'revision', 'owner', 'schema_version', 'content_hash'}
            if unknown:
                raise ValueError('unknown_host_dataset_fields:' + ','.join(sorted(unknown)))
            if not isinstance(descriptor.get('kind'), str) or not descriptor['kind']:
                raise ValueError('invalid_host_dataset_kind')
            if not isinstance(descriptor.get('namespace'), str) or not descriptor['namespace']:
                raise ValueError('invalid_host_dataset_namespace')
            owner = descriptor.get('owner', 'host')
            if owner not in {'host', 'barbara'}:
                raise ValueError('invalid_host_dataset_owner')
            for key in ('revision', 'schema_version', 'content_hash'):
                if key in descriptor and descriptor[key] is not None and not isinstance(descriptor[key], str):
                    raise ValueError('invalid_host_dataset_' + key)

    def owner(self, domain: str) -> str:
        if domain not in ALL_DOMAINS:
            raise ValueError('unknown_host_authority:' + str(domain))
        return self.authority[domain]

    def barbara_may_mutate(self, domain: str) -> bool:
        if domain not in CANONICAL_DOMAINS:
            return self.owner(domain) in {'barbara', 'shared'}
        return self.owner(domain) == 'barbara'

    def host_may_mutate(self, domain: str) -> bool:
        if domain not in CANONICAL_DOMAINS:
            return self.owner(domain) in {'host', 'shared'}
        return self.owner(domain) == 'host'

    def compatible_with(self, runtime_protocol: int = BARBARA_PROTOCOL_VERSION) -> bool:
        return self.protocol_version == runtime_protocol

    def snapshot(self) -> dict:
        return {
            'system_id': self.system_id,
            'system_version': self.system_version,
            'protocol_version': self.protocol_version,
            'authority': deepcopy(dict(self.authority)),
            'capabilities': sorted(self.capabilities),
            'datasets': deepcopy(dict(self.datasets)),
            'metadata': deepcopy(dict(self.metadata)),
        }


class HostIntegrationLoader:
    REQUIRED = {'system_id', 'system_version', 'protocol_version', 'authority', 'capabilities', 'datasets'}
    OPTIONAL = {'metadata'}

    def load(self, manifest: dict) -> HostIntegrationContract:
        if not isinstance(manifest, dict):
            raise ValueError('invalid_host_contract_manifest')
        missing = self.REQUIRED - set(manifest)
        unknown = set(manifest) - self.REQUIRED - self.OPTIONAL
        if missing:
            raise ValueError('missing_host_contract_fields:' + ','.join(sorted(missing)))
        if unknown:
            raise ValueError('unknown_host_contract_fields:' + ','.join(sorted(unknown)))
        capabilities = manifest['capabilities']
        if not isinstance(capabilities, (list, tuple, set, frozenset)):
            raise ValueError('invalid_host_capabilities')
        return HostIntegrationContract(
            system_id=manifest['system_id'],
            system_version=manifest['system_version'],
            protocol_version=manifest['protocol_version'],
            authority=_freeze_dict(manifest['authority'], 'invalid_host_authority'),
            capabilities=frozenset(capabilities),
            datasets=_freeze_dict(manifest['datasets'], 'invalid_host_datasets'),
            metadata=_freeze_dict(manifest.get('metadata', {}), 'invalid_host_metadata'),
        )


class HostIntegrationConformance:
    """Fail closed when an external host would create dual canonical authority."""

    def validate(self, contract: HostIntegrationContract, *, runtime_protocol: int = BARBARA_PROTOCOL_VERSION, system_id: str | None = None) -> dict:
        if not isinstance(contract, HostIntegrationContract):
            raise ValueError('invalid_host_contract')
        if not contract.compatible_with(runtime_protocol):
            raise ValueError('barbara_protocol_incompatible')
        if system_id is not None and contract.system_id != system_id:
            raise ValueError('host_contract_system_mismatch')
        host_owned = tuple(sorted(k for k in CANONICAL_DOMAINS if contract.owner(k) == 'host'))
        barbara_owned = tuple(sorted(k for k in CANONICAL_DOMAINS if contract.owner(k) == 'barbara'))
        return {
            'valid': True,
            'system_id': contract.system_id,
            'system_version': contract.system_version,
            'protocol_version': contract.protocol_version,
            'host_owned': host_owned,
            'barbara_owned': barbara_owned,
            'datasets': tuple(sorted(contract.datasets)),
        }


def external_rpg_contract(system_id: str, system_version: str, *, capabilities=(), datasets=None, metadata=None) -> HostIntegrationContract:
    """Safe default for the current Braseiro architecture: the RPG app owns truth.

    Barbara remains reusable infrastructure/supervisor and may own presentation
    domains, while the system-specific app retains canonical rules, atlas, PDFs,
    travel, calendar, inventory/combat and living-world state.
    """
    authority = {domain: 'host' for domain in CANONICAL_DOMAINS}
    authority.update({'narrative': 'shared', 'vtt': 'shared', 'audio': 'shared', 'ui': 'host'})
    return HostIntegrationContract(
        system_id=system_id,
        system_version=system_version,
        protocol_version=BARBARA_PROTOCOL_VERSION,
        authority=_freeze_dict(authority),
        capabilities=frozenset(capabilities),
        datasets=_freeze_dict(datasets or {}),
        metadata=_freeze_dict(metadata or {}),
    )
