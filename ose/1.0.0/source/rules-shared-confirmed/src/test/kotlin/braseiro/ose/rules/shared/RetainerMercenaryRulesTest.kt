package braseiro.ose.rules.shared

import kotlin.test.*

class RetainerMercenaryRulesTest {
    @Test fun charismaTableMatchesClassicAndAdvancedRetainerValues() {
        val expected=listOf(
            1 to 4, 2 to 5, 2 to 5, 3 to 6, 3 to 6, 3 to 6,
            4 to 7, 4 to 7, 4 to 7, 4 to 7,
            5 to 8, 5 to 8, 5 to 8, 6 to 9, 6 to 9, 7 to 10
        )
        for(score in 3..18) {
            val row=RetainerMercenaryRules.charisma(score)
            assertEquals(expected[score-3].first,row.maxRetainers,"limit $score")
            assertEquals(expected[score-3].second,row.baseLoyalty,"loyalty $score")
        }
        assertEquals(-2,RetainerMercenaryRules.charisma(3).hiringReactionModifier)
        assertEquals(2,RetainerMercenaryRules.charisma(18).hiringReactionModifier)
    }

    @Test fun recruitmentTableChecksAllLegalTwoDiceRollsAndModifiers() {
        for(a in 1..6) for(b in 1..6) for(mod in -2..2) {
            val result=RetainerMercenaryRules.recruitment(a,b,mod)
            val expected=when {
                a+b+mod<=2 -> RetainerMercenaryRules.RecruitmentOutcome.OFFENDED
                a+b+mod<=5 -> RetainerMercenaryRules.RecruitmentOutcome.REFUSED
                a+b+mod<=8 -> RetainerMercenaryRules.RecruitmentOutcome.REROLL_REQUIRED
                a+b+mod<=11 -> RetainerMercenaryRules.RecruitmentOutcome.ACCEPTED
                else -> RetainerMercenaryRules.RecruitmentOutcome.ACCEPTED_PLUS_ONE_LOYALTY
            }
            assertEquals(expected,result.outcome)
            assertEquals(expected.name.startsWith("ACCEPTED"),result.accepted)
        }
    }

    @Test fun loyaltyRunsWholeDiceAndThresholdMatrix() {
        for(a in 1..6) for(b in 1..6) for(loyalty in 2..12) {
            assertEquals(a+b<=loyalty,RetainerMercenaryRules.loyalty(a,b,loyalty).holds)
        }
    }

    @Test fun mercenaryMoraleDoesNotUseEmployerCharismaOrStackUnprovenTraits() {
        val rules=RetainerMercenaryRules
        assertEquals(6,rules.mercenaryMorale(RetainerMercenaryRules.Troop.MILITIA))
        assertEquals(7,rules.mercenaryMorale(RetainerMercenaryRules.Troop.BARBARIAN_HORDE))
        assertEquals(8,rules.mercenaryMorale(RetainerMercenaryRules.Troop.TRAINED_WARRIORS))
        assertEquals(9,rules.mercenaryMorale(RetainerMercenaryRules.Troop.TRAINED_WARRIORS,mounted=true))
        assertEquals(10,rules.mercenaryMorale(RetainerMercenaryRules.Troop.TRAINED_WARRIORS,fanatic=true))
        assertFailsWith<IllegalArgumentException> {
            rules.mercenaryMorale(RetainerMercenaryRules.Troop.TRAINED_WARRIORS,mounted=true,elite=true)
        }
    }

    @Test fun exactHalfSharesDoNotInventCoinRounding() {
        val rules=RetainerMercenaryRules
        val divided=assertNotNull(rules.divideTreasureExactly(2750L,5,listOf(1)))
        assertEquals(500L,divided.playerShareGp)
        assertEquals(listOf(250L),divided.retainerSharesGp)
        assertNull(rules.divideTreasureExactly(10L,2,listOf(1)))
        assertEquals(10L,assertNotNull(rules.divideTreasureExactly(20L,1,emptyList())).playerShareGp)
    }
}
