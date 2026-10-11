package com.braseiro.dnd5e;

import org.json.JSONObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An intention submitted for player curation is not an accomplished action.
 * A master envelope cannot bundle that intention with mechanical mutations:
 * otherwise a repeated or rejected proposal could affect the world before
 * its separate approval and deterministic rule resolution.
 */
public final class CompanionProposalIsolationGuard {
    private CompanionProposalIsolationGuard() {}

    public static String reject(JSONObject envelope, List<JSONObject> heroes, AppDb db) {
        if (envelope == null || db == null || heroes == null) return "";
        org.json.JSONArray proposals = envelope.optJSONArray("companion_proposals");
        if (proposals == null || proposals.length() == 0) return "";
        Set<Long> curatedIds = new HashSet<Long>();
        for (JSONObject hero : heroes) {
            if (hero == null) continue;
            long id = hero.optLong("id", 0);
            if (id <= 0) continue;
            if ("curated".equals(CompanionAgencyEngine.mode(
                    db.getCharacterControl(id).optString("control_mode", "manual"))))
                curatedIds.add(id);
        }
        return CompanionProposalIsolationRules.reject(envelope, curatedIds);
    }
}
