package braseiro.ose.rng

import kotlin.test.*

class Pcg32Test {
    @Test fun `pcg32 canonical golden vectors`() {
        var state = Pcg32.seeded(42u, 54u)
        val actual = buildList {
            repeat(5) { val draw = Pcg32.next(state); add(draw.value); state = draw.next }
        }
        val golden = listOf(0xA15C02B7u, 0x7B47F409u, 0xBA1D3330u, 0x83D2F293u, 0xBFA4784Bu)
        assertEquals(golden, actual)
    }
    @Test fun `named streams are isolated`() {
        val a = NamedRngStreams.fromRootSeed(1234u); val b = NamedRngStreams.fromRootSeed(1234u)
        val r1 = a.draw(RngStreamId.RULES_DICE); a.draw(RngStreamId.DUNGEON_GEN); val r2 = a.draw(RngStreamId.RULES_DICE)
        val c1 = b.draw(RngStreamId.RULES_DICE); val c2 = b.draw(RngStreamId.RULES_DICE)
        assertEquals(c1, r1); assertEquals(c2, r2)
        assertNotEquals(a.snapshot().streams.getValue(RngStreamId.DUNGEON_GEN).counter, b.snapshot().streams.getValue(RngStreamId.DUNGEON_GEN).counter)
    }
}
