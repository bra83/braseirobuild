package braseiro.ose.app

import org.json.JSONObject

const val BRIDGE_VERSION = 1
private val ALLOWED_BRIDGE_TYPES = setOf(
    "ViewState",
    "CharacterCreationCommit",
    "CharacterCreationStart",
    "CharacterSelect",
    "CharacterControlSet",
    "MasterActionPropose",
    "MasterActionReview",
    "MasterActionContinue",
    "CompanionOpinionsQuery",
    "CharacterDelete",
    "UiCommand",
    "CaptureFixtureCommand",
    "PlayerReaction",
    "StartAdventure",
    "ResetAdventure",
    "RuleEvidenceList",
    "CharacterCreationRandom",
    "CharacterSheetExportRequest",
    "CharacterSheetImportRequest",
    "CharacterSheetUpdate",
    "CharacterRecreateBeforeStart",
    "GMHelp",
    "TtsCommand",
    "RuleEvidenceQuery",
    "BackupExport",
    "BackupImport",
    "GeminiSettingsGet",
    "GeminiRuntimeSettingsSet",
    "GeminiCredentialSet",
    "GeminiCredentialDelete",
    "GeminiCredentialTest",
    "SceneImageGenerate",
    "ScenePromptShare",
    "PresentationImageSave",
    "ManualDiceRoll",
    // Legacy spellings remain accepted only for backward-compatible transport parsing.
    // They do not create alternate mutation channels.
    "PLAYER_REACTION",
    "GM_HELP",
    "TTS_PLAY",
    "TTS_STOP"
)

data class BridgeEnvelopeMetadata(val version: Int, val type: String)

object BridgeContractValidator {
    fun validate(raw: String): BridgeEnvelopeMetadata {
        val obj = JSONObject(raw)
        val version = obj.optInt("version", -1)
        val type = obj.optString("type", "")
        require(version == BRIDGE_VERSION) { "Unsupported bridge version $version" }
        require(type in ALLOWED_BRIDGE_TYPES) { "Unsupported bridge type $type" }
        require(obj.has("payload")) { "Bridge payload missing" }
        return BridgeEnvelopeMetadata(version, type)
    }
}
