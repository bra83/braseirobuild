package braseiro.ose.campaign

import braseiro.ose.model.RuleProfile
import kotlin.test.*

class ProfileRegistryTest {
    @Test fun `classic and advanced are distinct tagged profiles`() { assertEquals(2, RuleProfile.entries.size); assertNotEquals(RuleProfile.OSE_CLASSIC_FANTASY, RuleProfile.OSE_ADVANCED_FANTASY) }
    @Test fun `missing exact profile never falls back`() {
        val registry = ProfileRegistry(mapOf(RuleProfile.OSE_CLASSIC_FANTASY to "classic"))
        assertEquals("classic", registry.resolve(RuleProfile.OSE_CLASSIC_FANTASY))
        assertFailsWith<MissingProfileProviderException> { registry.resolve(RuleProfile.OSE_ADVANCED_FANTASY) }
    }
}
