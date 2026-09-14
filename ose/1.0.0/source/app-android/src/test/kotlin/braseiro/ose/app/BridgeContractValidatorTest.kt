package braseiro.ose.app

import org.junit.Assert.assertEquals
import org.junit.Test

class BridgeContractValidatorTest {
    @Test fun acceptsNarrativeFirstChannels() {
        for(type in listOf("ViewState","PLAYER_REACTION","GM_HELP","TTS_PLAY","TTS_STOP")) {
            assertEquals(type,BridgeContractValidator.validate("{\"version\":1,\"type\":\"$type\",\"payload\":{}}").type)
        }
    }
}
