package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * DMG 2024 pp. 350–352: the ELEVEN Bastion Events, including their nested
 * consequences. No die is rolled here; all rolled faces and DM choices come
 * from an explicit, auditable input. This builds a proposed outcome, not an
 * unauthorised mutation of gold, facilities or Bastion Defenders.
 */
public final class Bastion2024EventResolutionEngine {
    private Bastion2024EventResolutionEngine() { }

    public static JSONObject resolve(JSONObject input) {
        JSONObject in = input == null ? new JSONObject() : input;
        JSONObject out = new JSONObject();
        try {
            out.put("book", Bastion2024RulesEngine.BOOK).put("page", 350)
               .put("status", "blocked").put("auto_rolls", false)
               .put("requires_dm_confirmation", true)
               .put("persisted_to_world", false);
            if (!in.has("d100_roll")) return block(out, "d100_roll_required");
            int roll = dice(in, "d100_roll", 100, true);
            if (roll < 0) return block(out, "invalid_d100_roll");
            String event = eventName(roll);
            out.put("d100_roll", roll).put("event", event).put("event_resolved", false);
            out.put("gold_delta_gp", 0).put("defenders_delta", 0);
            out.put("shut_down_facility_next_turn", false);
            out.put("reroll_bastion_event", false);
            out.put("event_table_follow_up_required", false);

            if ("All Is Well".equals(event)) {
                // The d8 detail is optional flavor, never mechanically required.
                if (in.has("detail_d8")) {
                    int d8 = dice(in, "detail_d8", 8, false);
                    if (d8 < 0) return block(out, "invalid_detail_d8");
                    out.put("detail_d8", d8);
                }
            } else if ("Attack".equals(event)) {
                int defenders = count(in, "defenders_before", 0, 100000);
                if (defenders < 0) return block(out, "defenders_before_required");
                int[] rolls = faces(in.optJSONArray("attack_6d6"), 6, 6);
                if (rolls == null) return block(out, "attack_six_d6_faces_required");
                int ones = 0; for (int die : rolls) if (die == 1) ones++;
                int dead = Math.min(defenders, ones);
                int survivors = defenders - dead;
                out.put("attack_d6_ones", ones).put("defenders_delta", -dead)
                   .put("defenders_after", survivors);
                if (survivors == 0) {
                    if (in.optString("facility_id", "").trim().isEmpty())
                        return block(out, "damaged_special_facility_must_be_selected_by_dm");
                    out.put("shut_down_facility_next_turn", true)
                       .put("facility_id", in.getString("facility_id").trim())
                       .put("auto_repaired_after_one_bastion_turn", true);
                }
            } else if ("Criminal Hireling".equals(event)) {
                if (!in.has("retain_hireling")) return block(out, "retain_hireling_choice_required");
                boolean retain = in.optBoolean("retain_hireling");
                if (retain) {
                    int d6 = dice(in, "bribe_d6", 6, false);
                    if (d6 < 0) return block(out, "bribe_d6_required");
                    out.put("gold_delta_gp", -100 * d6).put("hireling_retained", true);
                } else {
                    out.put("hireling_retained", false)
                       .put("hireling_replaced_after_next_turn", true);
                    if (in.optBoolean("facility_has_no_other_hirelings", false)) {
                        if (in.optString("facility_id", "").trim().isEmpty())
                            return block(out, "affected_facility_id_required");
                        out.put("shut_down_facility_next_turn", true)
                           .put("facility_id", in.getString("facility_id").trim());
                    }
                }
            } else if ("Extraordinary Opportunity".equals(event)) {
                if (!in.has("accept_opportunity"))
                    return block(out, "accept_opportunity_choice_required");
                boolean accept = in.optBoolean("accept_opportunity");
                out.put("gold_delta_gp", accept ? -500 : 0);
                out.put("reroll_bastion_event", accept)
                   .put("reroll_excludes_same_opportunity", accept);
            } else if ("Friendly Visitors".equals(event)) {
                int d6 = dice(in, "payment_d6", 6, false);
                if (d6 < 0) return block(out, "payment_d6_required");
                out.put("gold_delta_gp", 100 * d6)
                   .put("facility_orders_uninterrupted", true);
            } else if ("Guest".equals(event)) {
                int d4 = dice(in, "guest_d4", 4, false);
                if (d4 < 0) return block(out, "guest_d4_required");
                out.put("guest_type", d4);
                if (d4 == 1) {
                    out.put("guest_stay_days", 7)
                       .put("letter_of_recommendation_after_stay", true);
                } else if (d4 == 2) {
                    int d8 = dice(in, "gift_d8", 8, false);
                    if (d8 < 0) return block(out, "guest_gift_d8_required");
                    out.put("guest_stay_days", 7).put("gold_delta_gp", 100 * d8);
                } else if (d4 == 3) {
                    out.put("defenders_delta", 1)
                       .put("guest_stays_until_dismissed_or_killed", true);
                } else {
                    out.put("friendly_monster_protects_next_bastion_attack", true)
                       .put("friendly_monster_leaves_after_protection", true);
                }
            } else if ("Lost Hirelings".equals(event)) {
                if (in.optString("facility_id", "").trim().isEmpty())
                    return block(out, "affected_facility_id_required");
                out.put("facility_id", in.getString("facility_id").trim())
                   .put("shut_down_facility_next_turn", true)
                   .put("hirelings_replaced_after_next_turn", true);
            } else if ("Magical Discovery".equals(event)) {
                String type = in.optString("magic_item_type", "").trim().toLowerCase(java.util.Locale.ROOT);
                if (!"potion".equals(type) && !"scroll".equals(type))
                    return block(out, "uncommon_potion_or_scroll_choice_required");
                String item = in.optString("magic_item_id", "").trim();
                if (item.isEmpty()) return block(out, "named_uncommon_magic_item_required");
                if (!in.optBoolean("chosen_item_is_uncommon", false))
                    return block(out, "uncommon_rarity_must_be_confirmed");
                out.put("magic_item_id", item).put("magic_item_type", type)
                   .put("rarity", "uncommon").put("item_gold_cost_gp", 0);
            } else if ("Refugees".equals(event)) {
                int[] d4 = faces(in.optJSONArray("refugees_2d4"), 2, 4);
                if (d4 == null) return block(out, "two_refugee_d4_faces_required");
                int d6 = dice(in, "payment_d6", 6, false);
                if (d6 < 0) return block(out, "payment_d6_required");
                out.put("refugee_count", d4[0] + d4[1])
                   .put("gold_delta_gp", d6 * 100)
                   .put("refugees_stay_until_new_home_or_attack", true);
                if (in.has("sufficient_basic_facility_space"))
                    out.put("refugees_camp_outside", !in.optBoolean("sufficient_basic_facility_space"));
                else return block(out, "basic_facility_space_choice_required");
            } else if ("Request for Aid".equals(event)) {
                if (!in.has("send_defenders"))
                    return block(out, "send_defenders_choice_required");
                if (in.optBoolean("send_defenders")) {
                    int available = count(in, "defenders_before", 0, 100000);
                    if (available < 1) return block(out, "available_defenders_required");
                    JSONArray diceArray = in.optJSONArray("aid_defender_d6");
                    if (diceArray == null || diceArray.length() < 1 ||
                            diceArray.length() > available || diceArray.length() > 100)
                        return block(out, "one_d6_per_dispatched_defender_required");
                    int[] scores = faces(diceArray, diceArray.length(), 6);
                    if (scores == null) return block(out, "invalid_aid_defender_d6");
                    int reward = dice(in, "reward_d6", 6, false);
                    if (reward < 0) return block(out, "aid_reward_d6_required");
                    int sum = 0; for (int die : scores) sum += die;
                    boolean success = sum >= 10;
                    out.put("dispatched_defenders", scores.length)
                       .put("aid_roll_total", sum)
                       .put("problem_resolved", true)
                       .put("defenders_delta", success ? 0 : -1)
                       .put("gold_delta_gp", success ? reward * 100 : reward * 50);
                } else {
                    out.put("aid_declined", true).put("problem_resolved", false);
                }
            } else if ("Treasure".equals(event)) {
                int d100 = dice(in, "treasure_d100", 100, true);
                if (d100 < 0) return block(out, "second_treasure_d100_required");
                String table = d100 <= 40 ? "art_objects_25_gp" :
                    d100 <= 63 ? "art_objects_250_gp" :
                    d100 <= 73 ? "art_objects_750_gp" :
                    d100 <= 75 ? "art_objects_2500_gp" :
                    d100 <= 90 ? "common_magic_items" :
                    d100 <= 98 ? "uncommon_magic_items" : "rare_magic_items";
                out.put("treasure_d100", d100).put("treasure_table", table)
                   .put("treasure_table_roll_required", true)
                   .put("event_table_follow_up_required", true)
                   .put("immediate_claim_if_present", in.optBoolean("owner_present", false))
                   .put("stored_until_owner_returns", !in.optBoolean("owner_present", false));
            } else return block(out, "unknown_bastion_event");

            return out.put("status", "ok").put("event_resolved", true)
                .put("requires_resolution", false);
        } catch (Exception e) { return block(out, "invalid_bastion_event_inputs"); }
    }

    private static String eventName(int n) {
        return n <= 50 ? "All Is Well" : n <= 55 ? "Attack" :
            n <= 58 ? "Criminal Hireling" : n <= 63 ? "Extraordinary Opportunity" :
            n <= 72 ? "Friendly Visitors" : n <= 76 ? "Guest" :
            n <= 79 ? "Lost Hirelings" : n <= 83 ? "Magical Discovery" :
            n <= 91 ? "Refugees" : n <= 98 ? "Request for Aid" : "Treasure";
    }
    private static int dice(JSONObject x, String key, int sides, boolean zeroMeansHundred) {
        if (!x.has(key) || x.isNull(key)) return -1;
        Object raw = x.opt(key);
        if (!(raw instanceof Number) || raw instanceof Float || raw instanceof Double) return -1;
        int n = ((Number)raw).intValue();
        if (zeroMeansHundred && n == 0) n = 100;
        return n < 1 || n > sides ? -1 : n;
    }
    private static int count(JSONObject x, String key, int min, int max) {
        if (!x.has(key) || !(x.opt(key) instanceof Number)) return -1;
        double v = x.optDouble(key, -1);
        if (v != Math.floor(v) || v < min || v > max) return -1;
        return (int)v;
    }
    private static int[] faces(JSONArray input, int count, int sides) {
        if (input == null || input.length() != count) return null;
        int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            Object obj = input.opt(i);
            if (!(obj instanceof Number)) return null;
            double d = input.optDouble(i, -1);
            if (d < 1 || d > sides || d != Math.floor(d)) return null;
            result[i] = (int)d;
        }
        return result;
    }
    private static JSONObject block(JSONObject out, String reason) {
        try { out.put("status", "blocked").put("requires_resolution", true).put("reason", reason); }
        catch (Exception ignored) { }
        return out;
    }
}
