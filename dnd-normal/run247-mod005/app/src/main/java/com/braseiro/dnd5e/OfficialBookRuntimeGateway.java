package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Entry point fail-closed para regras verificadas diretamente nos 10 livros oficiais e lore. */
public final class OfficialBookRuntimeGateway {
    private final AppDb db;
    private final NarrativeContentEngine narrativeEngine;
    private final AstarionHungersRulesEngine astarion = new AstarionHungersRulesEngine();
    private final AdventuresInFaerunRulesEngine faerun = new AdventuresInFaerunRulesEngine();
    private final RavenloftHorrorsWithinRulesEngine ravenloft = new RavenloftHorrorsWithinRulesEngine();
    private final LorwynFirstLightRulesEngine lorwyn = new LorwynFirstLightRulesEngine();
    private final NetherilsFallRulesEngine netheril = new NetherilsFallRulesEngine();
    private final SageAdvice2025RulesEngine sageAdvice = new SageAdvice2025RulesEngine();

    public OfficialBookRuntimeGateway(AppDb database) {
        this.db = database;
        this.narrativeEngine = database != null ? new NarrativeContentEngine(database) : null;
    }

    public NarrativeContentEngine getNarrativeEngine() {
        return narrativeEngine;
    }

    public JSONObject execute(String bookId, String actionId, JSONObject input) {
        String book = bookId == null ? "" : bookId.trim().toLowerCase();
        String action = actionId == null ? "" : actionId.trim().toLowerCase();
        JSONObject in = input == null ? new JSONObject() : input;

        if (book.equals(AstarionHungersRulesEngine.SUPPLEMENT_ID) || book.equals("abh") || book.equals("astarion")) {
            return executeAstarion(action, in);
        }
        if (book.equals(AdventuresInFaerunRulesEngine.SUPPLEMENT_ID) || book.equals("fraif") || book.equals("adventures_in_faerun")) {
            return executeFaerun(action, in);
        }
        if (book.equals("frhof") || book.equals("forgotten_realms_heroes_of_faerun") || book.equals("heroes_of_faerun")) {
            return executeFrhof(action, in);
        }
        if (book.equals(RavenloftHorrorsWithinRulesEngine.SUPPLEMENT_ID) || book.equals("rhw") || book.equals("ravenloft_the_horrors_within") || book.equals("ravenloft: the horrors within")) {
            return executeRavenloft(action, in);
        }
        if (book.equals(LorwynFirstLightRulesEngine.SUPPLEMENT_ID) || book.equals("lfl") || book.equals("lorwyn_first_light") || book.equals("lorwyn: first light")) {
            return executeLorwyn(action, in);
        }
        if (book.equals("nf") || book.equals("netherils_fall") || book.equals("netheril_s_fall") || book.equals("netheril's fall")) {
            return executeNetheril(action, in);
        }
        if (book.equals("xmm") || book.equals("monster_manual_2025")) {
            return executeXmm(action, in);
        }
        if (book.equals("xsac") || book.equals("sage_advice_2025") || book.equals("sage_advice_compendium_2025")) {
            return executeXsac(action, in);
        }
        if (book.equals("efa") || book.equals("eberron_forge_of_the_artificer") || book.equals("forge_of_the_artificer")) {
            return executeEfa(action, in);
        }
        if (book.equals("au") || book.equals("arcana_unleashed") || book.equals("arcana: unleashed")) {
            return executeAu(action, in);
        }
        if (book.equals("lore") || action.startsWith("lore_")) {
            return executeLore(action, in);
        }
        return blocked(bookId, actionId, "official_book_not_registered");
    }

    private JSONObject executeAstarion(String action, JSONObject in) {
        try {
            if ("list_feats".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("feats", astarion.listVampiricFeats());
            if ("epic_boon".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", astarion.getEpicBoon(in.optString("boon")));
            if ("feat_feature".equals(action)) {
                String rule=in.optString("feature_id","");
                JSONObject result=new AstarionPdfFeatRulesEngine().evaluate(rule,in);
                return new JSONObject().put("executed",result.optBoolean("resolved",false))
                        .put("status",result.optBoolean("resolved",false)?"manual_application_required":"blocked")
                        .put("auto_mutation_allowed",false).put("persisted",false)
                        .put("result",result);
            }
            if ("bloodlust_powerful_recovery".equals(action)) {
                JSONObject result=astarion.evaluateBloodlustRecovery(
                        in.optInt("rolled_hit_die",0), in.optInt("hit_die_sides",0));
                return new JSONObject().put("executed",result.optBoolean("resolved",false))
                        .put("status",result.optBoolean("resolved",false)?"resolved":"blocked")
                        .put("auto_mutation_allowed",false).put("persisted",false)
                        .put("result",result);
            }
            if ("bloodlust_sanguine_feast".equals(action)) {
                JSONObject result=astarion.evaluateBloodlustSanguineFeast(
                        in.optBoolean("hit_with_attack_roll",false),
                        in.optBoolean("target_bloodied",false),
                        in.optString("target_creature_type",""),
                        in.optBoolean("already_used_this_turn",true),
                        in.optBoolean("hit_die_available",false),
                        in.optInt("rolled_hit_die",0),
                        in.optInt("hit_die_sides",0),
                        in.optInt("constitution_modifier",0),
                        in.optInt("proficiency_bonus",0),
                        in.optInt("uses_spent",0));
                return new JSONObject().put("executed",result.optBoolean("resolved",false))
                        .put("status",result.optBoolean("resolved",false)?"manual_application_required":"blocked")
                        .put("auto_mutation_allowed",false).put("persisted",false)
                        .put("result",result);
            }
            if ("blood_frenzy".equals(action)) return astarion.evaluateBloodFrenzy(in.optInt("hunger_level"), in.optInt("target_hp"), in.optInt("target_max_hp"));
            if ("dhampir_bite_empower".equals(action)) {
                final long characterId = in.optLong("character_id", 0);
                if (characterId <= 0) return blocked(AstarionHungersRulesEngine.SUPPLEMENT_ID, action, "character_id_required");
                final JSONObject character = db.getCharacter(characterId);
                if (character.length() == 0) return blocked(AstarionHungersRulesEngine.SUPPLEMENT_ID, action, "character_not_found");
                final JSONObject sheet = parseSheet(character);
                final JSONObject bookState = bookState(sheet, "astarion");
                int pb = new DndRulesEngine().proficiencyBonus(character.optInt("level", 1));
                int spent = bookState.optInt("dhampir_bite_empower_uses", 0);
                final JSONObject resolved = astarion.resolveVampiricBiteEmpowerment(in.optInt("piercing_damage", -1), in.optString("target_creature_type"), in.optString("choice"), pb, spent);
                if (!resolved.optBoolean("resolved", false)) return new JSONObject().put("executed", false).put("status", "blocked").put("auto_mutation_allowed", false).put("result", resolved);
                final int newSpent = resolved.optInt("uses_spent_after", spent);
                final int heal = resolved.optInt("healing", 0);
                db.runInTransactionChecked(new AppDb.CheckedTransaction() {
                    public void run() throws Exception {
                        JSONObject s = parseSheet(db.getCharacter(characterId));
                        JSONObject bs = bookState(s, "astarion");
                        bs.put("dhampir_bite_empower_uses", newSpent);
                        setBookState(s, "astarion", bs);
                        db.updateCharacterSheetChecked(characterId, s.toString());
                        if (heal > 0) {
                            JSONObject now = db.getCharacter(characterId);
                            db.setCharacterHpChecked(characterId, Math.min(now.optInt("max_hp", 1), now.optInt("hp", 0) + heal));
                        }
                    }
                });
                return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("result", resolved);
            }
            if ("long_rest_reset".equals(action)) {
                final long characterId = in.optLong("character_id", 0);
                if (characterId <= 0 || db.getCharacter(characterId).length() == 0) return blocked(AstarionHungersRulesEngine.SUPPLEMENT_ID, action, "character_required");
                db.runInTransactionChecked(new AppDb.CheckedTransaction() {
                    public void run() throws Exception {
                        JSONObject s = parseSheet(db.getCharacter(characterId));
                        JSONObject bs = bookState(s, "astarion");
                        bs.put("dhampir_bite_empower_uses", 0);
                        setBookState(s, "astarion", bs);
                        db.updateCharacterSheetChecked(characterId, s.toString());
                    }
                });
                return new JSONObject().put("executed", true).put("persisted", true).put("uses_after", 0).put("auto_mutation_allowed", false);
            }
        } catch (Exception e) {
            return blocked(AstarionHungersRulesEngine.SUPPLEMENT_ID, action, "runtime_error:" + e.getMessage());
        }
        return blocked(AstarionHungersRulesEngine.SUPPLEMENT_ID, action, "action_not_implemented");
    }

    private JSONObject executeRavenloft(String action, JSONObject in) {
        try {
            if ("descriptor".equals(action) || "coverage".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", "coverage".equals(action) ? ravenloft.coverage() : ravenloft.descriptor());
            if ("list_species".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("species", ravenloft.listSpecies());
            if ("list_backgrounds".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("backgrounds", ravenloft.listBackgrounds());
            if ("list_feats".equals(action) || "list_dark_gifts".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("feats", ravenloft.listFeats());
            if ("list_subclasses".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("subclasses", ravenloft.listSubclasses());
            if ("bestiary_install".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", RavenloftBestiaryRuntimeEngine.install());
            if ("bestiary_coverage".equals(action)) { RavenloftBestiaryRuntimeEngine.install(); return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", RavenloftBestiaryRuntimeEngine.coverage()); }
            if ("statblock".equals(action)) { RavenloftBestiaryRuntimeEngine.install(); String id = in.optString("name"); JSONObject profile = CompiledStatblockEngine.runtimeProfile(id); return new JSONObject().put("executed", profile.optBoolean("found", false)).put("auto_mutation_allowed", false).put("result", profile); }
            if ("species".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", ravenloft.species(in.optString("name")));
            if ("background".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", ravenloft.background(in.optString("name")));
            if ("feat".equals(action) || "dark_gift".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", ravenloft.feat(in.optString("name")));
            if ("subclass".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", ravenloft.subclass(in.optString("name")));
            if ("mists_travel".equals(action)) { JSONObject r = ravenloft.resolveMistsTravel(in); return new JSONObject().put("executed", r.optBoolean("resolved", false)).put("status", r.optBoolean("resolved", false) ? "resolved" : r.optString("status", "blocked")).put("auto_mutation_allowed", false).put("result", r); }
            if ("magic_in_mists".equals(action)) { JSONObject r = ravenloft.magicInMists(in.optString("effect")); return new JSONObject().put("executed", r.optBoolean("resolved", false)).put("auto_mutation_allowed", false).put("result", r); }
            final long characterId = in.optLong("character_id", 0);
            if (characterId <= 0) return blocked(RavenloftHorrorsWithinRulesEngine.SUPPLEMENT_ID, action, "character_id_required");
            final JSONObject character = db.getCharacter(characterId);
            if (character.length() == 0) return blocked(RavenloftHorrorsWithinRulesEngine.SUPPLEMENT_ID, action, "character_not_found");
            final JSONObject sheet = parseSheet(character);
            final JSONObject state = bookState(sheet, "rhw");
            state.put("character_level", Math.max(1, character.optInt("level", 1)));
            if ("short_rest".equals(action) || "long_rest".equals(action)) {
                final JSONObject after = "long_rest".equals(action) ? ravenloft.longRest(state) : ravenloft.shortRest(state);
                db.runInTransactionChecked(new AppDb.CheckedTransaction() {
                    public void run() throws Exception {
                        JSONObject s = parseSheet(db.getCharacter(characterId));
                        setBookState(s, "rhw", after);
                        db.updateCharacterSheetChecked(characterId, s.toString());
                    }
                });
                return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("state", after);
            }
            final JSONObject result = ravenloft.resolveCharacterAction(action, in, character, state);
            if (!result.optBoolean("resolved", false)) return new JSONObject().put("executed", false).put("status", result.optString("status", "blocked")).put("auto_mutation_allowed", false).put("result", result);
            final JSONObject after = result.optJSONObject("state_after");
            db.runInTransactionChecked(new AppDb.CheckedTransaction() {
                public void run() throws Exception {
                    JSONObject now = db.getCharacter(characterId);
                    JSONObject s = parseSheet(now);
                    if (after != null) setBookState(s, "rhw", after);
                    db.updateCharacterSheetChecked(characterId, s.toString());
                    if (result.has("set_hp")) db.setCharacterHpChecked(characterId, result.optInt("set_hp", now.optInt("hp", 0)));
                    else if (result.has("hp_delta")) db.setCharacterHpChecked(characterId, Math.min(now.optInt("max_hp", 1), Math.max(0, now.optInt("hp", 0) + result.optInt("hp_delta", 0))));
                }
            });
            return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("result", result);
        } catch (Exception e) {
            return blocked(RavenloftHorrorsWithinRulesEngine.SUPPLEMENT_ID, action, "runtime_error:" + e.getMessage());
        }
    }

    private JSONObject executeLorwyn(String action, JSONObject in) {
        try {
            if ("descriptor".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", lorwyn.descriptor());
            if ("coverage".equals(action)) { LorwynBestiaryRuntimeEngine.install(); return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", lorwyn.coverage()); }
            if ("list_species".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("species", lorwyn.listSpecies());
            if ("list_backgrounds".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("backgrounds", lorwyn.listBackgrounds());
            if ("list_feats".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("feats", lorwyn.listFeats());
            if ("list_items".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("items", lorwyn.listItems());
            if ("species".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", lorwyn.species(in.optString("name")));
            if ("background".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", lorwyn.background(in.optString("name")));
            if ("feat".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", lorwyn.feat(in.optString("name")));
            if ("item".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", lorwyn.item(in.optString("name")));
            if ("bestiary_install".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", LorwynBestiaryRuntimeEngine.install());
            if ("bestiary_coverage".equals(action)) { LorwynBestiaryRuntimeEngine.install(); return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", LorwynBestiaryRuntimeEngine.coverage()); }
            if ("statblock".equals(action)) { LorwynBestiaryRuntimeEngine.install(); JSONObject profile = CompiledStatblockEngine.runtimeProfile(in.optString("name")); return new JSONObject().put("executed", profile.optBoolean("found", false)).put("auto_mutation_allowed", false).put("result", profile); }
            if ("bestiary_trait".equals(action)) { LorwynBestiaryRuntimeEngine.install(); JSONObject r = LorwynBestiaryRuntimeEngine.resolveTrait(in.optString("statblock"), in.optString("trait"), in.optJSONObject("context")); return new JSONObject().put("executed", r.optBoolean("resolved", false)).put("auto_mutation_allowed", false).put("result", r); }
            final long characterId = in.optLong("character_id", 0);
            if (characterId <= 0) return blocked(LorwynFirstLightRulesEngine.SUPPLEMENT_ID, action, "character_id_required");
            final JSONObject character = db.getCharacter(characterId);
            if (character.length() == 0) return blocked(LorwynFirstLightRulesEngine.SUPPLEMENT_ID, action, "character_not_found");
            final JSONObject sheet = parseSheet(character);
            final JSONObject state = bookState(sheet, "lfl");
            state.put("character_level", Math.max(1, character.optInt("level", 1)));
            if ("long_rest".equals(action)) {
                final JSONObject after = lorwyn.longRest(state);
                db.runInTransactionChecked(new AppDb.CheckedTransaction() {
                    public void run() throws Exception {
                        JSONObject s = parseSheet(db.getCharacter(characterId));
                        setBookState(s, "lfl", after);
                        db.updateCharacterSheetChecked(characterId, s.toString());
                    }
                });
                return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("state", after);
            }
            final JSONObject result = lorwyn.resolve(action, in, character, state);
            if (!result.optBoolean("resolved", false)) return new JSONObject().put("executed", false).put("status", result.optString("status", "blocked")).put("auto_mutation_allowed", false).put("result", result);
            final JSONObject after = result.optJSONObject("state_after");
            db.runInTransactionChecked(new AppDb.CheckedTransaction() {
                public void run() throws Exception {
                    JSONObject now = db.getCharacter(characterId);
                    JSONObject s = parseSheet(now);
                    if (after != null) setBookState(s, "lfl", after);
                    db.updateCharacterSheetChecked(characterId, s.toString());
                }
            });
            return new JSONObject().put("executed", true).put("persisted", after != null).put("auto_mutation_allowed", false).put("result", result);
        } catch (Exception e) {
            return blocked(LorwynFirstLightRulesEngine.SUPPLEMENT_ID, action, "runtime_error:" + e.getMessage());
        }
    }

    private JSONObject executeFaerun(String action, JSONObject in) {
        try {
            if ("epic_destiny".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", faerun.resolveEpicDestiny(in.optString("destiny"), in.optInt("level", 1)));
            if ("list_example_destinies".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("destinies", faerun.listPublishedExampleDestinies());
            if ("regional_hazard".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", faerun.resolveRegionalHazard(in.optString("region"), in.optInt("survival_roll", 0), in.optInt("con_save_roll", 0)));
        } catch (Exception e) {
            return blocked(AdventuresInFaerunRulesEngine.SUPPLEMENT_ID, action, "runtime_error:" + e.getMessage());
        }
        return blocked(AdventuresInFaerunRulesEngine.SUPPLEMENT_ID, action, "action_not_implemented");
    }

    private JSONObject executeFrhof(String action, JSONObject in) {
        try {
            if ("descriptor".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", ForgottenRealmsHeroesEngine.descriptor());
            if ("coverage".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", ForgottenRealmsHeroesEngine.coverage());
            if ("install".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", ForgottenRealmsHeroesEngine.install());
            if ("subclasses".equals(action) || "list_subclasses".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("subclasses", ForgottenRealmsHeroesEngine.subclasses());
            if ("backgrounds".equals(action) || "list_backgrounds".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("backgrounds", ForgottenRealmsHeroesEngine.backgrounds());
            if ("feats".equals(action) || "list_feats".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("feats", ForgottenRealmsHeroesEngine.feats());
            if ("spells".equals(action) || "list_spells".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("spells", ForgottenRealmsHeroesEngine.spells());
            if ("factions".equals(action) || "list_factions".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("factions", ForgottenRealmsHeroesEngine.factions());
            if ("spell_info".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", ForgottenRealmsHeroesEngine.spellInfo(in.optString("spell")));
            if ("aurora_info".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", ForgottenRealmsHeroesEngine.auroraInfo(in.optString("item")));
            final long characterId = in.optLong("character_id", 0);
            if (characterId > 0 && db != null) {
                final JSONObject character = db.getCharacter(characterId);
                if (character.length() > 0) {
                    JSONObject sheet = parseSheet(character);
                    int lvl = in.optInt("level", character.optInt("level", 1));
                    if ("short_rest".equals(action)) {
                        final JSONObject after = ForgottenRealmsHeroesEngine.shortRest(sheet);
                        db.updateCharacterSheetChecked(characterId, after.toString());
                        return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("sheet", after);
                    }
                    if ("long_rest".equals(action)) {
                        final JSONObject after = ForgottenRealmsHeroesEngine.longRest(sheet);
                        db.updateCharacterSheetChecked(characterId, after.toString());
                        return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("sheet", after);
                    }
                    if ("apply_background".equals(action)) {
                        JSONObject after = ForgottenRealmsHeroesEngine.applyBackground(sheet, in.optString("background"), lvl);
                        boolean modified = after.optBoolean("frhof_background_runtime", false);
                        if (modified) {
                            db.updateCharacterSheetChecked(characterId, after.toString());
                            return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("sheet", after);
                        }
                        return blocked("frhof", action, "background_not_supported");
                    }
                    if ("apply_subclass".equals(action)) {
                        JSONObject after = ForgottenRealmsHeroesEngine.applySubclass(sheet, in.optString("subclass"), lvl);
                        boolean modified = after.optBoolean("frhof_subclass_runtime", false);
                        if (modified) {
                            db.updateCharacterSheetChecked(characterId, after.toString());
                            return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("sheet", after);
                        }
                        return blocked("frhof", action, "subclass_not_supported");
                    }
                    if ("apply_feat".equals(action)) {
                        JSONObject after = ForgottenRealmsHeroesEngine.applyFeat(sheet, in.optString("feat"), lvl);
                        boolean modified = after.optBoolean("frhof_feat_runtime", false);
                        if (modified) {
                            db.updateCharacterSheetChecked(characterId, after.toString());
                            return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("sheet", after);
                        }
                        return blocked("frhof", action, "feat_not_supported");
                    }
                    if ("resolve_circle_magic".equals(action)) {
                        JSONObject res = ForgottenRealmsHeroesEngine.resolveCircleMagic(sheet, in.optString("option"), in);
                        if (res.optBoolean("resolved", false)) {
                            db.updateCharacterSheetChecked(characterId, res.optJSONObject("sheet") != null ? res.optJSONObject("sheet").toString() : sheet.toString());
                            return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("result", res);
                        }
                        return new JSONObject().put("executed", false).put("status", res.optString("status", "blocked")).put("auto_mutation_allowed", false).put("result", res);
                    }
                    if ("resolve_deity_renown".equals(action)) {
                        JSONObject res = ForgottenRealmsHeroesEngine.resolveDeityRenown(sheet, in.optString("deity"), in.optInt("delta", 0), in);
                        if (res.optBoolean("resolved", false)) {
                            db.updateCharacterSheetChecked(characterId, res.optJSONObject("sheet") != null ? res.optJSONObject("sheet").toString() : sheet.toString());
                            return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("result", res);
                        }
                        return new JSONObject().put("executed", false).put("status", res.optString("status", "blocked")).put("auto_mutation_allowed", false).put("result", res);
                    }
                }
            }
        } catch (Exception e) {
            return blocked("frhof", action, "runtime_error:" + e.getMessage());
        }
        return blocked("frhof", action, "action_not_implemented");
    }

    private JSONObject executeEfa(String action, JSONObject in) {
        try {
            if ("coverage".equals(action) || "descriptor".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", EberronForgeCompleteEngine.coverage());
            if ("creature_info".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", EberronForgeCompleteEngine.creatureInfo(in.optString("creature"), in));
            if ("airship_info".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", EberronForgeCompleteEngine.airshipInfo(in.optString("ship")));
            final long characterId = in.optLong("character_id", 0);
            if (characterId > 0 && db != null) {
                final JSONObject character = db.getCharacter(characterId);
                if (character.length() > 0) {
                    JSONObject sheet = parseSheet(character);
                    if ("short_rest".equals(action)) {
                        JSONObject after = EberronForgeCompleteEngine.shortRest(sheet);
                        db.updateCharacterSheetChecked(characterId, after.toString());
                        return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("sheet", after);
                    }
                    if ("long_rest".equals(action)) {
                        JSONObject after = EberronForgeCompleteEngine.longRest(sheet);
                        db.updateCharacterSheetChecked(characterId, after.toString());
                        return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("sheet", after);
                    }
                    if ("airship_action".equals(action)) {
                        JSONObject res = EberronForgeCompleteEngine.resolveAirshipAction(sheet, in.optString("ship"), in.optString("airship_action"), in);
                        if (res.optBoolean("eberron_resolved", false) || "resolved".equals(res.optString("eberron_status")) || res.optBoolean("resolved", false)) {
                            db.updateCharacterSheetChecked(characterId, res.toString());
                            return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("result", res);
                        }
                        return new JSONObject().put("executed", false).put("status", res.optString("status", "blocked")).put("auto_mutation_allowed", false).put("result", res);
                    }
                    if ("framework_action".equals(action)) {
                        JSONObject res = EberronForgeCompleteEngine.resolveFrameworkAction(sheet, in.optString("framework"), in.optString("framework_action"), in);
                        if (res.optBoolean("eberron_resolved", false) || "resolved".equals(res.optString("eberron_status")) || res.optBoolean("resolved", false)) {
                            db.updateCharacterSheetChecked(characterId, res.toString());
                            return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("result", res);
                        }
                        return new JSONObject().put("executed", false).put("status", res.optString("status", "blocked")).put("auto_mutation_allowed", false).put("result", res);
                    }
                }
            }
        } catch (Exception e) {
            return blocked("efa", action, "runtime_error:" + e.getMessage());
        }
        return blocked("efa", action, "action_not_implemented");
    }

    private JSONObject executeAu(String action, JSONObject in) {
        try {
            if ("coverage".equals(action) || "descriptor".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", ArcanaUnleashedRun143Engine.coverage());
            if ("subclasses".equals(action) || "list_subclasses".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("subclasses", ArcanaUnleashedRun143Engine.allSubclasses());
            if ("backgrounds".equals(action) || "list_backgrounds".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("backgrounds", ArcanaUnleashedRun143Engine.allBackgrounds());
            if ("feats".equals(action) || "list_feats".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("feats", ArcanaUnleashedRun143Engine.allFeats());
            if ("actions_for_subclass".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("actions", ArcanaUnleashedRun143Engine.actionsForSubclass(in.optString("subclass"), in.optInt("level", 3)));
            if ("actions_for_feat".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("actions", ArcanaUnleashedRun143Engine.actionsForFeat(in.optString("feat")));
            final long characterId = in.optLong("character_id", 0);
            if (characterId > 0 && db != null) {
                final JSONObject character = db.getCharacter(characterId);
                if (character.length() > 0) {
                    JSONObject sheet = parseSheet(character);
                    int lvl = in.optInt("level", character.optInt("level", 1));
                    if ("short_rest".equals(action)) {
                        ArcanaUnleashedRun143Engine.shortRest(sheet);
                        db.updateCharacterSheetChecked(characterId, sheet.toString());
                        return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("sheet", sheet);
                    }
                    if ("long_rest".equals(action)) {
                        ArcanaUnleashedRun143Engine.longRest(sheet);
                        db.updateCharacterSheetChecked(characterId, sheet.toString());
                        return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("sheet", sheet);
                    }
                    if ("resolve_subclass".equals(action)) {
                        JSONObject res = ArcanaUnleashedRun143Engine.resolveSubclass(sheet, in.optString("subclass"), lvl, in.optString("subclass_action"), in);
                        if (res.optBoolean("subclass_action_resolved", false) || res.optBoolean("resolved", false)) {
                            db.updateCharacterSheetChecked(characterId, sheet.toString());
                            return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("result", res);
                        }
                        return new JSONObject().put("executed", false).put("status", res.optString("status", "blocked")).put("auto_mutation_allowed", false).put("result", res);
                    }
                    if ("resolve_feat".equals(action)) {
                        JSONObject res = ArcanaUnleashedRun143Engine.resolveFeat(sheet, in.optString("feat"), in.optString("feat_action"), in);
                        if (res.optBoolean("feat_action_resolved", false) || res.optBoolean("resolved", false)) {
                            db.updateCharacterSheetChecked(characterId, sheet.toString());
                            return new JSONObject().put("executed", true).put("persisted", true).put("auto_mutation_allowed", false).put("result", res);
                        }
                        return new JSONObject().put("executed", false).put("status", res.optString("status", "blocked")).put("auto_mutation_allowed", false).put("result", res);
                    }
                }
            }
        } catch (Exception e) {
            return blocked("au", action, "runtime_error:" + e.getMessage());
        }
        return blocked("au", action, "action_not_implemented");
    }

    private JSONObject executeLore(String action, JSONObject in) {
        if (narrativeEngine == null) return blocked("lore", action, "narrative_engine_unavailable");
        try {
            if ("lore_save".equals(action) || "save_entity".equals(action)) {
                NarrativeContentEngine.Entity entity = NarrativeContentEngine.Entity.fromJson(in.optJSONObject("entity"));
                if (entity == null) return blocked("lore", action, "invalid_entity_payload");
                boolean ok = narrativeEngine.saveEntity(entity);
                return new JSONObject().put("executed", ok).put("persisted", ok).put("auto_mutation_allowed", false).put("entity_id", entity.id);
            }
            if ("lore_get".equals(action) || "get_entity".equals(action)) {
                String id = in.optString("id");
                NarrativeContentEngine.Entity entity = narrativeEngine.getEntity(id);
                if (entity == null) return new JSONObject().put("executed", false).put("found", false);
                return new JSONObject().put("executed", true).put("found", true).put("entity", entity.toJson());
            }
            if ("lore_list".equals(action) || "list_entities".equals(action)) {
                List<NarrativeContentEngine.Entity> list = narrativeEngine.listEntitiesBySetting(in.optString("setting"), in.optString("type"));
                JSONArray arr = new JSONArray();
                for (NarrativeContentEngine.Entity e : list) arr.put(e.toJson());
                return new JSONObject().put("executed", true).put("count", list.size()).put("entities", arr);
            }
            if ("lore_discover".equals(action) || "discover_entity".equals(action)) {
                String id = in.optString("id");
                String note = in.optString("note", "");
                boolean ok = narrativeEngine.discoverEntity(id, note);
                return new JSONObject().put("executed", ok).put("discovered", ok).put("id", id);
            }
            if ("lore_player_view".equals(action) || "player_view".equals(action)) {
                List<String> ids = new ArrayList<>();
                JSONArray arr = in.optJSONArray("ids");
                if (arr != null) for (int i = 0; i < arr.length(); i++) ids.add(arr.optString(i));
                JSONObject view = narrativeEngine.buildPlayerView(in.optString("setting"), ids);
                return new JSONObject().put("executed", true).put("player_view", view);
            }
            if ("lore_gm_view".equals(action) || "gm_view".equals(action)) {
                List<String> ids = new ArrayList<>();
                JSONArray arr = in.optJSONArray("ids");
                if (arr != null) for (int i = 0; i < arr.length(); i++) ids.add(arr.optString(i));
                JSONObject view = narrativeEngine.buildGmView(in.optString("setting"), ids);
                return new JSONObject().put("executed", true).put("gm_view", view);
            }
        } catch (Exception e) {
            return blocked("lore", action, "runtime_error:" + e.getMessage());
        }
        return blocked("lore", action, "action_not_implemented");
    }

    private static JSONObject parseSheet(JSONObject c) {
        try {
            String raw = c.optString("sheet_json", "{}");
            return raw.trim().startsWith("{") ? new JSONObject(raw) : new JSONObject();
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static JSONObject bookState(JSONObject sheet, String key) throws Exception {
        JSONObject all = sheet.optJSONObject("official_book_state");
        if (all == null) {
            all = new JSONObject();
            sheet.put("official_book_state", all);
        }
        JSONObject state = all.optJSONObject(key);
        if (state == null) {
            state = new JSONObject();
            all.put(key, state);
        }
        return state;
    }

    private static void setBookState(JSONObject sheet, String key, JSONObject state) throws Exception {
        JSONObject all = sheet.optJSONObject("official_book_state");
        if (all == null) all = new JSONObject();
        all.put(key, state);
        sheet.put("official_book_state", all);
    }

    private JSONObject executeNetheril(String action, JSONObject in) {
        try {
            if ("coverage".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", netheril.coverage());
            if ("list_environmental_effects".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("effects", netheril.listEnvironmentalEffects());
            if ("environmental_effect".equals(action)) { JSONObject r = netheril.environmental(in.optString("name")); return new JSONObject().put("executed", r.optBoolean("found", false)).put("auto_mutation_allowed", false).put("result", r); }
            if ("apply_environmental_effect".equals(action)) {
                JSONObject resolved=netheril.applyEnvironmentalEffect(in.optString("name"),in);
                return new JSONObject().put("executed",resolved.optBoolean("resolved",false))
                        .put("persisted",false).put("auto_mutation_allowed",false)
                        .put("status",resolved.optBoolean("resolved",false)?"manual_application_required":"blocked")
                        .put("result",resolved);
            }
            if ("list_hazards".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("hazards", netheril.listHazards());
            if ("hazard".equals(action)) { JSONObject r = netheril.hazard(in.optString("name"), Math.max(1, in.optInt("level", 1))); return new JSONObject().put("executed", r.optBoolean("found", false)).put("auto_mutation_allowed", false).put("result", r); }
            if ("list_items".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("items", netheril.listItems());
            if ("item".equals(action)) { JSONObject r = netheril.item(in.optString("name")); return new JSONObject().put("executed", r.optBoolean("found", false)).put("auto_mutation_allowed", false).put("result", r); }
            if ("bestiary_install".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", NetherilsFallBestiaryRuntimeEngine.install());
            if ("bestiary_coverage".equals(action)) { NetherilsFallBestiaryRuntimeEngine.install(); return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", NetherilsFallBestiaryRuntimeEngine.coverage()); }
            if ("statblock".equals(action)) { NetherilsFallBestiaryRuntimeEngine.install(); JSONObject profile = CompiledStatblockEngine.runtimeProfile(in.optString("name")); return new JSONObject().put("executed", profile.optBoolean("found", false)).put("auto_mutation_allowed", false).put("result", profile); }
        } catch (Exception e) {
            return blocked("nf", action, "runtime_error:" + e.getMessage());
        }
        return blocked("nf", action, "action_not_implemented");
    }

    private JSONObject executeXmm(String action, JSONObject in) {
        try {
            if ("install".equals(action) || "bestiary_install".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", Xmm2025CompiledBestiaryEngine.install());
            if ("coverage".equals(action) || "bestiary_coverage".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", Xmm2025CompiledBestiaryEngine.coverage());
            if ("dependency_coverage".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", Xmm2025CompiledBestiaryEngine.dependencyCoverage());
            if ("catalog".equals(action) || "list_statblocks".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", Xmm2025CompiledBestiaryEngine.catalog());
            if ("book_section".equals(action)) {JSONObject r=Xmm2025BookIndex.section(in.optString("chapter_id"),in.optString("section_id"));return new JSONObject().put("executed",r.optBoolean("resolved",false)).put("auto_mutation_allowed",false).put("result",r);}
            if ("book_index".equals(action)) {JSONObject r=Xmm2025BookIndex.indexedMonsters(in.optString("dimension"),in.optString("label"));return new JSONObject().put("executed",r.optBoolean("resolved",false)).put("auto_mutation_allowed",false).put("result",r);}
            if ("convert_2014".equals(action)) {JSONObject r=Xmm2025BookIndex.conversion(in.optString("name"));return new JSONObject().put("executed",r.optBoolean("resolved",false)).put("auto_mutation_allowed",false).put("result",r);}
            if ("recharge".equals(action)) {JSONObject r=Xmm2025SourceIndex.rechargeDecision(in);return new JSONObject().put("executed",r.optBoolean("resolved",false)).put("auto_mutation_allowed",false).put("result",r);}
            if ("source_reference".equals(action)) {JSONObject r=Xmm2025CompiledBestiaryEngine.source(in.optString("name"));return new JSONObject().put("executed",r.length()>0).put("auto_mutation_allowed",false).put("result",r);}
            if ("filter".equals(action)) {JSONObject r=Xmm2025CompiledBestiaryEngine.search(in.optString("dimension"),in.optString("value"));return new JSONObject().put("executed",r.optBoolean("resolved",false)).put("auto_mutation_allowed",false).put("result",r);}
            if ("save_damage_catalog".equals(action)) return new JSONObject().put("executed",true).put("auto_mutation_allowed",false).put("result",Xmm2025SaveDamageEngine.index());
            if ("save_damage_rule".equals(action)) {JSONObject r=Xmm2025SaveDamageEngine.rule(in.optString("monster"),in.optString("category"),in.optString("action"));return new JSONObject().put("executed",r.optBoolean("resolved",false)).put("auto_mutation_allowed",false).put("result",r);}
            if ("save_damage_resolve".equals(action)) {JSONObject r=Xmm2025SaveDamageEngine.resolve(in);return new JSONObject().put("executed",r.optBoolean("resolved",false)).put("auto_mutation_allowed",false).put("result",r);}
            if ("spell_options".equals(action)) {JSONObject r=Xmm2025SpellUsageIndex.options(in.optString("name"));return new JSONObject().put("executed",r.optBoolean("resolved",false)).put("auto_mutation_allowed",false).put("result",r);}
            if ("spell_usage_check".equals(action)) {JSONObject r=Xmm2025SpellUsageIndex.check(in);return new JSONObject().put("executed",r.optBoolean("resolved",false)).put("auto_mutation_allowed",false).put("result",r);}
            if ("statblock".equals(action)) { JSONObject r = Xmm2025CompiledBestiaryEngine.statblock(in.optString("name")); return new JSONObject().put("executed", r.optBoolean("resolved", false)).put("auto_mutation_allowed", false).put("result", r); }
        } catch (Exception e) {
            return blocked("xmm", action, "runtime_error:" + e.getMessage());
        }
        return blocked("xmm", action, "action_not_implemented");
    }

    private JSONObject executeXsac(String action, JSONObject in) {
        try {
            if ("rules_references".equals(action) || "bibliography".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", sageAdvice.rulesReferences());
            if ("official_rulings".equals(action) || "ruling_policy".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", sageAdvice.officialRulingsPolicy());
            if ("coverage".equals(action) || "descriptor".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("result", sageAdvice.coverage());
            if ("categories".equals(action) || "list_categories".equals(action)) return new JSONObject().put("executed", true).put("auto_mutation_allowed", false).put("categories", sageAdvice.categories());
            if ("resolve".equals(action) || "ruling".equals(action)) { JSONObject r = sageAdvice.resolve(in.optString("rule_id"), in); return new JSONObject().put("executed", r.optBoolean("resolved", false)).put("status", r.optString("status", r.optBoolean("resolved", false) ? "resolved" : "blocked")).put("auto_mutation_allowed", false).put("result", r); }
        } catch (Exception e) {
            return blocked("xsac", action, "runtime_error:" + e.getMessage());
        }
        return blocked("xsac", action, "action_not_implemented");
    }

    private static JSONObject blocked(String book, String action, String reason) {
        JSONObject o = new JSONObject();
        try {
            o.put("book_id", book == null ? "" : book).put("action_id", action == null ? "" : action).put("executed", false).put("status", "blocked").put("auto_mutation_allowed", false).put("reason", reason);
        } catch (Exception ignored) {}
        return o;
    }
}
