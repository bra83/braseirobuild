__version__ = "1.1.2"
from .engine import BarbaraEngine
from .state import CampaignState
from .gemini import GeminiProvider
from .bridge import HostBridge, DurableHostBridge
from .state_store import DurableStateStore, StateVersionConflict
from .host_contract import (
    BARBARA_PROTOCOL_VERSION, HostIntegrationContract, HostIntegrationLoader,
    HostIntegrationConformance, external_rpg_contract,
)
from .supervisor import SupervisorBridge
from .packages import (
    BarbaraPackageManifest, InstalledPackage, PackageCompatibility, PackageManifestLoader,
    PackageRepository, WorldRecord, WorldRegistry,
)
from .platform import BarbaraPlatform
from .resources import WorldAtlasIndex, WorldResourceRuntime
from .module_extension import ModuleExtension, ModuleExtensionLoader, compose_system_module
from .vtt import VttAuthority, VttGridMath, VttCommandError
from .platform import BarbaraWorldSession
from .presentation import NarrativePresentationPolicy, VisitMemory, WorldIntroductionValidator, FAMILIARITY_LEVELS
from .poi import DynamicPoiContract, PoiCommandBuilder, AtlasPoiProjection, PoiGenerationPolicy
from .audio import AudioDirector, AudioCatalogContract
