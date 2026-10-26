package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test

class OfficialSupplementRulesTest {

  @Test
  fun testTgufAlienStagesAndHealAbility() {
    val initialSupp = SupplementCampaignState(
      active = OfficialSupplement.THEY_GROW_UP_SO_FAST,
      tgufStage = TgufAlienStage.HATCHLING,
      mysteryId = "mystery-alien-01"
    )

    // Check that TGUF_BOND_HEAL is active and can be used once
    assertTrue(OfficialSupplementRules.isRuleActive(initialSupp, OfficialSupplementRules.TGUF_BOND_HEAL))
    assertTrue(OfficialSupplementRules.canUseOncePerMystery(initialSupp, OfficialSupplementRules.TGUF_BOND_HEAL))

    // Mark used
    val usedSupp = OfficialSupplementRules.markUsed(initialSupp, OfficialSupplementRules.TGUF_BOND_HEAL)
    assertFalse("Após o uso no mistério corrente, a habilidade não deve mais estar disponível",
      OfficialSupplementRules.canUseOncePerMystery(usedSupp, OfficialSupplementRules.TGUF_BOND_HEAL))

    // Starting a new mystery resets the once-per-mystery ability
    val nextMysterySupp = usedSupp.startMystery("mystery-alien-02")
    assertTrue("Novo mistério deve reativar habilidade 1x/mistério",
      OfficialSupplementRules.canUseOncePerMystery(nextMysterySupp, OfficialSupplementRules.TGUF_BOND_HEAL))

    // Stage progression checks
    assertEquals(TgufAlienStage.HATCHLING, initialSupp.tgufStage)
    val developingSupp = initialSupp.copy(tgufStage = TgufAlienStage.DEVELOPING)
    assertEquals(TgufAlienStage.DEVELOPING, developingSupp.tgufStage)
    val matureSupp = initialSupp.copy(tgufStage = TgufAlienStage.MATURE)
    assertEquals(TgufAlienStage.MATURE, matureSupp.tgufStage)
  }

  @Test
  fun testOurFriendsTheMachines10PlusTriggerAndCaps() {
    // 10+ machines trigger: reactivates ONLY when >= 10 and d6 == 6
    assertFalse(OfficialSupplementRules.chipReactivates(9, 6))
    assertFalse(OfficialSupplementRules.chipReactivates(10, 5))
    assertTrue(OfficialSupplementRules.chipReactivates(10, 6))
    assertTrue(OfficialSupplementRules.chipReactivates(15, 6))

    // Hacking ratio: 10 machines per chip
    assertEquals(0, OfficialSupplementRules.machinesNeededToHackChips(0))
    assertEquals(10, OfficialSupplementRules.machinesNeededToHackChips(1))
    assertEquals(30, OfficialSupplementRules.machinesNeededToHackChips(3))

    // Cap permanent bonus at +3
    assertEquals(0, OfficialSupplementRules.capPermanentBonus(-1))
    assertEquals(2, OfficialSupplementRules.capPermanentBonus(2))
    assertEquals(3, OfficialSupplementRules.capPermanentBonus(3))
    assertEquals(3, OfficialSupplementRules.capPermanentBonus(4))
    assertEquals(3, OfficialSupplementRules.capPermanentBonus(99))
  }

  @Test
  fun testHometownHackActivation() {
    val ofmSupp = SupplementCampaignState(active = OfficialSupplement.OUR_FRIENDS_THE_MACHINES)
    val hackState = HometownHackState(
      city = "Boulder City",
      area = "Deserto de Nevada",
      mapReference = "Mapa B-12",
      loopLayout = "Anel subterrâneo duplo",
      agency = "DOD/DARPA",
      institutions = listOf("Laboratório Hoover"),
      summary = "Instalações secretas sob a represa"
    )
    val updated = ofmSupp.copy(hometownHack = hackState)
    assertEquals("Boulder City", updated.hometownHack.city)
    assertEquals(1, updated.hometownHack.institutions.size)
    assertEquals("DOD/DARPA", updated.hometownHack.agency)
  }

  @Test
  fun testProfileFirewallInvariants() {
    val teen = RandomTeenFactory.create()
    assertEquals("Teen deve sempre ter 0 de sorte", 0, teen.luck)
    assertTrue("Teen deve suportar Vergonha", teen.shameAvailable || teen.shameText.isNotEmpty())

    val kid = RandomKidFactory.create()
    assertEquals("Sorte de Kid deve ser 15 - idade", (15 - kid.age).coerceAtLeast(0), kid.luck)
    assertFalse("Kid nunca deve ter Vergonha disponível", kid.shameAvailable)
  }
}
