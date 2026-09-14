package braseiro.ose.model

import java.security.MessageDigest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object CanonicalStateHash {
    private val json = Json { encodeDefaults = true; explicitNulls = false; prettyPrint = false; classDiscriminator = "kind" }
    fun canonicalJson(envelope: CampaignEnvelope): String = json.encodeToString(envelope.canonicalMechanical())
    fun sha256(envelope: CampaignEnvelope): String = MessageDigest.getInstance("SHA-256").digest(canonicalJson(envelope).encodeToByteArray()).joinToString("") { "%02x".format(it) }
}
