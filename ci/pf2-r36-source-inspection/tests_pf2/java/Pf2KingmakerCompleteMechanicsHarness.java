import com.braseiro.pathfinder2e.KingmakerRulesEngine;
import com.braseiro.pathfinder2e.Pathfinder2eRulesEngine;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Harness completo de validação mecânica semântica PF2e Remaster & Kingmaker.
 * Executado sem dependência de Android SDK através de runtime_json.
 */
public final class Pf2KingmakerCompleteMechanicsHarness {
    private static int checks = 0;

    private static void check(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError("FALHA NA VERIFICAÇÃO: " + label);
        }
        checks++;
    }

    public static void main(String[] args) throws Exception {
        testPf2StackingRules();
        testPf2ConditionsComprehensive();
        testPf2DyingWoundedDoomedAndRecovery();
        testPf2RestDailyPrepAndPersistentDamage();
        testKingmakerKingdomTurnAndActivities();
        testKingmakerUnrestAnarchyAndRuin();
        testKingmakerWarfareMechanics();
        testKingmakerCompanions();
        testKingmakerCampingProcedure();

        System.out.println("PF2_KINGMAKER_COMPLETE_HARNESS_PASS (" + checks + " checks passed)");
    }

    private static void testPf2StackingRules() throws Exception {
        Pathfinder2eRulesEngine pf2 = new Pathfinder2eRulesEngine();

        // Stacking bonuses: item, status, circumstance, untyped
        check(pf2.stackBonuses(2, 1, 3, 0) == 6, "bonuses of different types sum");
        check(pf2.stackBonuses(0, 0, 0, 5) == 5, "untyped bonus applies");

        // Stacking penalties: worst of same type, untyped sums
        check(pf2.stackPenalties(-1, -2, -3, 0) == -6, "penalties of different types sum");
        check(pf2.stackPenalties(0, -4, 0, -2) == -6, "status penalty and untyped penalty combine");

        // Stacking modifiers using JSON array with overlapping types
        JSONArray mods = new JSONArray();
        mods.put(new JSONObject().put("type", "item").put("value", 2));
        mods.put(new JSONObject().put("type", "item").put("value", 1)); // should not stack with +2
        mods.put(new JSONObject().put("type", "status").put("value", 3));
        mods.put(new JSONObject().put("type", "status").put("value", -2)); // negative status penalty
        mods.put(new JSONObject().put("type", "status").put("value", -4)); // worse status penalty (-4 wins)
        mods.put(new JSONObject().put("type", "circumstance").put("value", 2));
        mods.put(new JSONObject().put("type", "untyped").put("value", -1));
        mods.put(new JSONObject().put("type", "untyped").put("value", 5));

        JSONObject res = pf2.stackModifiers(10, mods);
        // Base 10 + item(+2) + status(+3) + circumstance(+2) + untyped(+5) = 22 bonuses
        // Penalties: status(-4) + untyped(-1) = -5 penalties
        // Total = 10 + 2 + 3 + 2 + 5 - 4 - 1 = 17
        check(res.optInt("total") == 17, "stackModifiers total calculation: got " + res.optInt("total"));
        check(res.optInt("item_bonus") == 2, "item bonus picks highest (+2)");
        check(res.optInt("status_penalty") == -4, "status penalty picks worst (-4)");
    }

    private static void testPf2ConditionsComprehensive() {
        Pathfinder2eRulesEngine pf2 = new Pathfinder2eRulesEngine();

        // Test validCondition catalog for 35 conditions
        String[] conditions = {
            "blinded", "broken", "clumsy 1", "concealed", "confused", "controlled",
            "dazzled", "deafened", "doomed 1", "drained 1", "dying 1", "encumbered",
            "enfeebled 1", "fascinated", "fatigued", "fleeing", "frightened 2", "grabbed",
            "hidden", "immobilized", "invisible", "off_guard", "flat_footed", "paralyzed",
            "persistent damage 1d6 fire", "petrified", "prone", "quickened", "restrained",
            "sickened 1", "slowed 1", "stunned 1", "stupefied 1", "unconscious",
            "undetected", "wounded 1"
        };
        for (String c : conditions) {
            check(pf2.validCondition(c), "condition recognized: " + c);
        }

        // Test evaluation of conditions and their mechanical penalties
        // clumsy 2 -> -2 status to Dex-based checks, DCs, AC, Reflex
        // frightened 1 -> -1 status to ALL checks and DCs
        // prone -> -2 circumstance to attack rolls, off-guard (-2 circumstance to AC)
        String raw = "clumsy 2, frightened 1, prone";
        JSONObject eval = pf2.evaluateConditions(raw, 5, 2);

        // AC penalty: status clumsy(-2) vs unconscious(-4) -> clumsy -2
        // plus prone off-guard circumstance penalty -2 -> total AC penalty -4
        check(eval.optInt("ac_penalty") <= -4, "clumsy 2 + frightened 1 + prone AC penalty <= -4");
        check(eval.optBoolean("off_guard"), "prone grants off-guard");
        check(eval.optBoolean("prone"), "prone flag recognized");
        check(eval.optInt("frightened") == 1, "frightened value is 1");

        // End of turn frightened decrement
        check(eval.optInt("frightened_end_of_turn") == 0, "frightened 1 reduced to 0 at end of turn");

        // Test speed penalties with encumbered (-10 ft) and immobilized (speed 0)
        JSONObject encumberedEval = pf2.evaluateConditions("encumbered");
        check(encumberedEval.optInt("speed_penalty_feet") == 10, "encumbered speed penalty is 10 ft");

        JSONObject immobilizedEval = pf2.evaluateConditions("immobilized");
        check(immobilizedEval.optBoolean("speed_zero"), "immobilized sets speed to 0");

        // Test action reductions: slowed 1, quickened (+1 action)
        JSONObject actionEval = pf2.evaluateConditions("slowed 1, quickened");
        check(actionEval.optInt("actions_max") == 3, "base 3 + quickened (+1) - slowed 1 (-1) = 3 actions");
    }

    private static void testPf2DyingWoundedDoomedAndRecovery() {
        Pathfinder2eRulesEngine pf2 = new Pathfinder2eRulesEngine();

        // Death limit: 4 - doomed
        check(pf2.deathLimit(0) == 4, "default death limit is dying 4");
        check(pf2.deathLimit(1) == 3, "doomed 1 reduces death limit to dying 3");
        check(pf2.deathLimit(2) == 2, "doomed 2 reduces death limit to dying 2");

        // Knocked out: lethal hit with wounded 1 -> dying 1 + wounded 1 = dying 2
        JSONObject ko1 = pf2.knockedOut(1, 0, false, false);
        check(ko1.optInt("dying") == 2, "knocked out with wounded 1 starts at dying 2");
        check(!ko1.optBoolean("dead"), "not dead yet");

        // Knocked out from critical hit: dying 2 + wounded 1 = dying 3
        JSONObject koCrit = pf2.knockedOut(1, 0, false, true);
        check(koCrit.optInt("dying") == 3, "critical knock out with wounded 1 starts at dying 3");

        // Recovery check DC = 10 + current dying value
        check(pf2.recoveryCheckDc(1) == 11, "recovery check DC for dying 1 is 11");
        check(pf2.recoveryCheckDc(2) == 12, "recovery check DC for dying 2 is 12");
        check(pf2.recoveryCheckDc(3) == 13, "recovery check DC for dying 3 is 13");

        // Recovery check results:
        // Success: dying reduces by 1
        JSONObject succ = pf2.dying(2, 1, 0, true, false);
        check(succ.optInt("dying") == 1, "success reduces dying 2 to 1");

        // Critical success: dying reduces by 2 (recovers to 0, unconscious, wounded increases)
        JSONObject critSucc = pf2.dying(2, 1, 0, true, true);
        check(critSucc.optInt("dying") == 0, "critical success on recovery removes dying");
        check(critSucc.optInt("wounded") == 1, "recovering maintains wounded tracking");

        // Failure: dying increases by 1
        JSONObject fail = pf2.dying(2, 0, 0, false, false);
        check(fail.optInt("dying") == 3, "failure on recovery increases dying 2 to 3");

        // Critical failure: dying increases by 2 -> reaches 4 -> DEAD!
        JSONObject critFail = pf2.dying(2, 0, 0, false, true);
        check(critFail.optInt("dying") == 4, "critical failure increases dying 2 to 4");
        check(critFail.optBoolean("dead"), "dying 4 results in death");

        // Doomed 1 death threshold: dying reaches 3 -> DEAD!
        JSONObject doomedDeath = pf2.dying(2, 0, 1, false, false);
        check(doomedDeath.optInt("dying") == 3, "dying reaches 3");
        check(doomedDeath.optBoolean("dead"), "doomed 1 causes death at dying 3");
    }

    private static void testPf2RestDailyPrepAndPersistentDamage() throws Exception {
        Pathfinder2eRulesEngine pf2 = new Pathfinder2eRulesEngine();

        // Persistent damage flat check: DC 15 normally, DC 10 with assistance
        JSONObject flatFail = pf2.persistentDamageFlatCheck(14, false);
        check(!flatFail.optBoolean("success"), "roll 14 fails unassisted persistent DC 15");
        JSONObject flatPass = pf2.persistentDamageFlatCheck(15, false);
        check(flatPass.optBoolean("success"), "roll 15 passes unassisted persistent DC 15");

        JSONObject assistedFail = pf2.persistentDamageFlatCheck(9, true);
        check(!assistedFail.optBoolean("success"), "roll 9 fails assisted persistent DC 10");
        JSONObject assistedPass = pf2.persistentDamageFlatCheck(10, true);
        check(assistedPass.optBoolean("success"), "roll 10 passes assisted persistent DC 10");

        // Natural healing: Con modifier * Level (min 1 * level)
        // Con mod 3, Level 5 -> 3 * 5 = 15 HP
        check(pf2.naturalHealing(3, 5, false) == 15, "8-hour rest heals conMod * level");
        // Full 24-hour bed rest: double natural healing -> 30 HP
        check(pf2.naturalHealing(3, 5, true) == 30, "24-hour rest doubles healing");
        // Negative Con mod still heals minimum 1 * level = 5 HP
        check(pf2.naturalHealing(-1, 5, false) == 5, "negative con heals at least 1 * level");

        // Daily Preparation resets fatigued, spell slots, wounded
        JSONObject hero = new JSONObject();
        hero.put("level", 5);
        hero.put("con", 3);
        hero.put("hp", 10);
        hero.put("max_hp", 50);
        hero.put("conditions", "fatigued, wounded 1");
        JSONObject prep = pf2.dailyPreparation(hero, false);
        check(prep.optInt("hp_after") == 25, "daily prep adds natural healing 15 HP to 10 HP = 25");
        check(prep.optBoolean("fatigued_removed"), "daily prep removes fatigued");
    }

    private static void testKingmakerKingdomTurnAndActivities() throws Exception {
        KingmakerRulesEngine km = new KingmakerRulesEngine();

        // Claim Hex activity resolver
        // natural 15, mod 5, DC 15 -> Success
        JSONObject claim = km.resolveClaimHex(15, 5, 15);
        check(claim.optBoolean("success"), "claim hex succeeded");
        check(claim.optInt("size_delta") == 1, "size delta is 1");
        check(claim.optInt("rp_cost") == 1, "rp cost is 1");

        // Claim hex critical failure: increases unrest
        JSONObject claimCritFail = km.resolveClaimHex(1, 0, 20);
        check(!claimCritFail.optBoolean("success"), "claim hex failed");
        check(claimCritFail.optInt("unrest_delta") == 1, "crit fail increases unrest");

        // Build Structure resolver
        // natural 14, mod 6, DC 16, cost 6 RP -> built!
        JSONObject build = km.resolveBuildStructure(14, 6, 16, 6);
        check(build.optBoolean("built"), "structure build succeeded");
        check(build.optInt("rp_cost") == 6, "rp cost matches structure cost");

        // Collect Taxes: natural 15, mod 4, DC 15, economyMod 3
        JSONObject taxes = km.resolveCollectTaxes(15, 4, 15, 3);
        check(taxes.optInt("rp_gain") >= 3, "taxes yield RP on success");

        // Quell Unrest: natural 16, mod 5, DC 15
        JSONObject quell = km.resolveQuellUnrest(16, 5, 15);
        check(quell.optInt("unrest_delta") <= -1, "quell unrest reduces unrest");

        // Celebrate Holiday: natural 18, mod 4, DC 15
        JSONObject holiday = km.resolveCelebrateHoliday(18, 4, 15);
        check(holiday.optInt("unrest_delta") <= -1, "celebrate holiday reduces unrest");

        // Establish Trade Agreement: natural 15, mod 5, DC 15
        JSONObject trade = km.resolveEstablishTradeAgreement(15, 5, 15);
        check(trade.optBoolean("established"), "trade agreement established");
    }

    private static void testKingmakerUnrestAnarchyAndRuin() {
        KingmakerRulesEngine km = new KingmakerRulesEngine();

        // Unrest mechanics & Anarchy
        check(!km.isAnarchy(0), "unrest 0 is not anarchy");
        check(!km.isAnarchy(19), "unrest 19 is not anarchy");
        check(km.isAnarchy(20), "unrest 20 is anarchy");
        check(km.isAnarchy(25), "unrest 25 is anarchy");

        // Apply unrest
        JSONObject u1 = km.applyUnrest(5, 3);
        check(u1.optInt("unrest") == 8, "unrest increased by 3 to 8");
        JSONObject u2 = km.applyUnrest(5, -10);
        check(u2.optInt("unrest") == 0, "unrest clamped at 0");

        // Ruin progression & penalty
        // threshold 10, points 8 + delta 5 = 13 -> 3 points, penalty increases by 1
        JSONObject r1 = km.applyRuin(8, 0, 10, 5);
        check(r1.optInt("points") == 3, "remaining points after threshold is 3");
        check(r1.optInt("penalty") == 1, "penalty increased to 1");

        // Fame and Infamy summary
        JSONObject fi = km.fameInfamySummary(2, 0, "fame");
        check(fi.optInt("active_points") == 2, "active points is 2");
    }

    private static void testKingmakerWarfareMechanics() {
        KingmakerRulesEngine km = new KingmakerRulesEngine();

        // Army damage and defeat
        // HP 10, hits 12 -> damage 12 -> HP 0 -> defeated!
        JSONObject dmg1 = km.applyArmyDamage(10, 10, 12, 0, 0, 3);
        check(dmg1.optBoolean("defeated"), "army defeated at 0 HP");
        check(!dmg1.optBoolean("destroyed"), "army defeated is not automatically destroyed without critical finish");

        // Critical finish marks army as destroyed
        JSONObject dmgCrit = km.applyArmyDamage(10, 10, 12, 0, 0, 3, true);
        check(dmgCrit.optBoolean("defeated"), "army defeated");
        check(dmgCrit.optBoolean("destroyed"), "critical finish destroys army");

        // Rout threshold check: rout threshold 4, damage leaves HP at 3
        JSONObject dmgRout = km.applyArmyDamage(10, 10, 7, 0, 0, 4);
        check(dmgRout.optInt("hp") == 3, "HP reduced to 3");
        check(dmgRout.optBoolean("routed"), "army routed when at or below rout threshold");

        // War attack MAP penalties: index 1 -> 0, index 2 -> -5, index 3 -> -10 (or -4/-8 agile)
        JSONObject atk1 = km.resolveWarAttack(15, 8, 18, 1, false, 0, 0);
        check(atk1.optInt("attack_index") == 1 && atk1.optInt("map_penalty") == 0, "1st attack MAP is 0");
        JSONObject atk2 = km.resolveWarAttack(15, 8, 18, 2, false, 0, 0);
        check(atk2.optInt("attack_index") == 2 && atk2.optInt("map_penalty") == -5, "2nd attack MAP is -5");
        JSONObject atkAgile = km.resolveWarAttack(15, 8, 18, 2, true, 0, 0);
        check(atkAgile.optInt("map_penalty") == -4, "2nd agile attack MAP is -4");

        // Rout check and rally
        JSONObject routCheck = km.resolveWarRoutCheck(15, 4, 16);
        check(!routCheck.optBoolean("routed"), "rout check succeeded, army not routed");

        JSONObject rally = km.resolveWarRally(18, 3, 15);
        check(rally.optBoolean("rallied"), "rally check succeeded");
    }

    private static void testKingmakerCompanions() throws Exception {
        KingmakerRulesEngine km = new KingmakerRulesEngine();

        // Check catalog contains all 12 official companions
        JSONArray catalog = km.companionCatalog();
        check(catalog.length() == 12, "all 12 official companions present in catalog");

        String[] expectedCompanions = {
            "amiri", "ekundayo", "jubilost", "linzi", "nok_nok", "tristian",
            "valerie", "harrim", "jaethal", "kalikke_kanerah", "octavia", "regongar"
        };
        for (String compKey : expectedCompanions) {
            JSONObject prof = km.companionProfile(compKey);
            check(prof.length() > 0, "profile found for companion: " + compKey);
            check(prof.optJSONArray("thresholds") != null && prof.optJSONArray("thresholds").length() >= 3,
                "companion " + compKey + " has discrete thresholds");
            check(prof.optString("name").length() > 0, "companion " + compKey + " has name");
            check(prof.optString("camping_activity").length() > 0, "companion " + compKey + " has camping activity");
            check(prof.optString("favorite_meal").length() > 0, "companion " + compKey + " has favorite meal");
        }

        // Discrete influence thresholds & attitude progression
        // Amiri starts at 0 (indifferent). Gaining 2 influence -> friendly at threshold 2!
        JSONObject inf1 = km.companionInfluence("amiri", 0, 2, new JSONArray());
        check(inf1.optInt("influence") == 2, "influence is 2");
        check("friendly".equals(inf1.optString("attitude")), "amiri attitude is friendly at 2");
        check("Amistoso".equals(inf1.optString("attitude_label")), "attitude_label is Amistoso");
        JSONArray newly1 = inf1.optJSONArray("newly_unlocked_rewards");
        check(newly1 != null && newly1.length() == 1, "reward unlocked at threshold 2");
        check("amiri_friendly".equals(newly1.optJSONObject(0).optString("reward_id")), "amiri_friendly unlocked");

        // Deduplication test: gaining more influence preserves unlocked and prevents duplicate firing
        JSONArray allUnlocked1 = inf1.optJSONArray("all_unlocked_rewards");
        JSONObject inf2 = km.companionInfluence("amiri", 2, 2, allUnlocked1); // total 4 (threshold 4)
        check(inf2.optInt("influence") == 4, "influence increased to 4");
        JSONArray newly2 = inf2.optJSONArray("newly_unlocked_rewards");
        check(newly2 != null && newly2.length() == 1, "only 1 newly unlocked reward at threshold 4");
        check("amiri_helpful".equals(newly2.optJSONObject(0).optString("reward_id")), "amiri_helpful unlocked");
        check(inf2.optJSONArray("all_unlocked_rewards").length() == 2, "total 2 rewards unlocked");

        // Calling again with 0 delta does NOT re-trigger rewards
        JSONObject inf3 = km.companionInfluence("amiri", 4, 0, inf2.optJSONArray("all_unlocked_rewards"));
        check(inf3.optJSONArray("newly_unlocked_rewards").length() == 0, "no duplicate rewards fired");

        // Decrementing influence
        JSONObject infDec = km.companionInfluence("amiri", 4, -4, inf2.optJSONArray("all_unlocked_rewards"));
        check(infDec.optInt("influence") == 0, "influence reduced to 0");
        check("indifferent".equals(infDec.optString("attitude")), "attitude dropped back to indifferent");

        // Helpful attitude at high influence
        JSONObject infHelpful = km.companionInfluence("amiri", 0, 8, new JSONArray());
        check("helpful".equals(infHelpful.optString("attitude")), "attitude is helpful at 8+ influence");
        check("Prestativo".equals(infHelpful.optString("attitude_label")), "attitude_label is Prestativo");
    }

    private static void testKingmakerCampingProcedure() throws Exception {
        KingmakerRulesEngine km = new KingmakerRulesEngine();

        // --- Step 1: Campsite Selection ---
        JSONArray zones = km.campZonesCatalog();
        check(zones.length() == 20, "camp zones catalog has all 20 zones");
        JSONObject site = km.campsiteSelection("Zonaverde", "planicie");
        check("Zonaverde".equals(site.optString("zone_name")), "campsite zone is Zonaverde");
        check(site.optInt("zone_dc") == 16, "Zonaverde zone DC is 16");
        check(site.optInt("encounter_dc") == 14, "Zonaverde encounter DC is 14");
        check(site.optInt("zone_level") == 2, "Zonaverde zone level is 2");

        // --- Step 2: Prepare Camp ---
        // Success vs DC 16
        JSONObject prepSuccess = km.prepareCamp(14, 4, 16, false); // total 18 >= 16 -> SUCCESS
        check("SUCCESS".equals(prepSuccess.optString("degree")), "prepare camp succeeded");
        check(prepSuccess.optBoolean("activities_allowed"), "activities allowed on success");
        check(prepSuccess.optInt("encounter_dc_bonus") == 0, "encounter dc bonus is 0 on normal success");

        // Critical success (+2 encounter DC bonus)
        JSONObject prepCrit = km.prepareCamp(20, 6, 16, false); // CRITICAL_SUCCESS
        check("CRITICAL_SUCCESS".equals(prepCrit.optString("degree")), "prepare camp crit success");
        check(prepCrit.optInt("encounter_dc_bonus") == 2, "encounter dc bonus is +2 on critical success");

        // Critical failure (immediate encounter check, activities forbidden)
        JSONObject prepCritFail = km.prepareCamp(1, 0, 16, false);
        check("CRITICAL_FAILURE".equals(prepCritFail.optString("degree")), "prepare camp crit fail");
        check(!prepCritFail.optBoolean("activities_allowed"), "activities forbidden on crit fail");
        check(prepCritFail.optBoolean("immediate_encounter_check"), "immediate encounter check required on crit fail");

        // --- Step 3: Camping Activities ---
        JSONArray actCatalog = km.campingActivitiesCatalog();
        check(actCatalog.length() >= 10, "camping activities catalog has activities");

        // Hunt / Forage activity
        JSONObject huntRes = km.resolveCampingActivity("cacar_coletar", "Amiri", 15, 5, 16, 2, new JSONObject());
        check("SUCCESS".equals(huntRes.optString("degree")), "hunt succeeded");
        check(huntRes.optInt("basic_ingredients_gained") > 0, "hunt gained basic ingredients");
        check(huntRes.optInt("duration_hours") == 2, "hunt duration is 2 hours");

        // Companion specific activity (Amiri Aprimorar Armas)
        JSONObject amiriAct = km.resolveCampingActivity("amiri_aprimorar_armas", "Amiri", 18, 4, 16, 2, new JSONObject());
        check("SUCCESS".equals(amiriAct.optString("degree")), "amiri activity succeeded");
        check(amiriAct.optString("benefit_summary").length() > 0, "amiri activity grants mechanical benefit");

        // Camouflage Camp activity (increases encounter DC)
        JSONObject camoRes = km.resolveCampingActivity("camuflar_acampamento", "Ekundayo", 16, 4, 16, 2, new JSONObject());
        check(camoRes.optInt("encounter_dc_delta") >= 1, "camouflage camp raises encounter DC");

        // --- Step 4: Special Meals & Cooking ---
        JSONArray recipes = km.recipesCatalog();
        check(recipes.length() >= 10, "recipes catalog has recipes");

        // Cook Guisado de Monstro (favorite of Amiri)
        JSONArray diners = new JSONArray().put("amiri");
        JSONObject cookRes = km.resolveCookingMeal("guisado_de_monstro", "Amiri", 18, 12, diners.length(), 20, 5, diners);
        check(cookRes.optBoolean("success"), "cooking guisado de monstro succeeded");
        check(cookRes.optInt("consumed_basic") >= 1, "consumed basic ingredients");
        check(cookRes.optJSONArray("buffs_24h") != null, "24h buffs generated");
        check(cookRes.optJSONArray("favorite_meal_matches").length() >= 1, "favorite meal bonus applied to Amiri");

        // Cooking with insufficient ingredients fails
        JSONObject failCook = km.resolveCookingMeal("guisado_de_monstro", "Amiri", 15, 3, 1, 0, 0, new JSONArray());
        check(!failCook.optBoolean("success"), "cooking with insufficient ingredients fails");

        // --- Step 5: Sleep & Rest ---
        JSONArray watch = new JSONArray().put(new JSONObject().put("shift", 1).put("guard", "Amiri"));
        JSONObject restRes = km.resolveSleepAndRest(4, 3, 2, "normal", watch, 14, 14, 4);
        check(restRes.optInt("total_rest_hours") >= 8, "rest duration is at least 8 hours");
        check(restRes.optInt("natural_healing_hp") >= 6, "natural healing scales with con*level*comfort");
        check(restRes.optBoolean("spells_prepared_restored"), "spells restored");
        check(restRes.optJSONArray("conditions_cleared") != null, "conditions cleared list present");

        // Aggregate camp effects
        JSONObject agg = km.aggregateCampEffects(prepSuccess, new JSONArray().put(huntRes).put(amiriAct), cookRes, restRes);
        check(agg.optBoolean("spells_and_focus_restored"), "spells restored in aggregate");
        check(agg.optJSONObject("mechanical_effects") != null, "mechanical_effects is structured JSONObject");
        check(!"source_specific".equals(agg.optString("mechanical_effects")), "no source_specific mechanical effects");
        check(agg.optJSONArray("buffs_24h") != null, "buffs_24h consolidated");
    }
}
