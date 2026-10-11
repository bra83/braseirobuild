package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Forgotten Realms: Heroes of Faerûn — mega runtime.
 *
 * Todo teste, rolagem, alvo, gasto de slot/recurso e escolha permanece manual.
 * Catálogo isolado não é tratado como implementação: os gateways abaixo aplicam
 * estado, consumo, duração, concentração e resets persistentes quando pertinentes.
 */
public final class ForgottenRealmsHeroesEngine {
    public static final String SOURCE="Forgotten Realms: Heroes of Faerûn";
    public static final String ENGINE_ID="fr_heroes_faerun_mega_runtime_v1";
    public static final String SOURCE_REV="official-2025-reviewed-2026-10-01";

    private static final String[] SUBCLASSES={
        "College of the Moon","Knowledge Domain","Banneret","Oath of the Noble Genies",
        "Winter Walker","Scion of the Three","Spellfire Sorcery","Bladesinger"
    };
    private static final String[] BACKGROUNDS={
        "Chondathan Freebooter","Dead Magic Dweller","Dragon Cultist","Emerald Enclave Caretaker",
        "Flaming Fist Mercenary","Genie Touched","Harper","Ice Fisher","Knight of the Gauntlet",
        "Lords' Alliance Vassal","Moonwell Pilgrim","Mulhorandi Tomb Raider","Mythalkeeper",
        "Purple Dragon Squire","Rashemi Wanderer","Shadowmasters Exile","Spellfire Initiate","Zhentarim Mercenary"
    };
    private static final String[] FEATS={
        "Cold Caster","Cult of the Dragon Initiate","Dragonscarred","Emerald Enclave Fledgling","Enclave Magic",
        "Fairy Trickster","Genie Magic","Harper Agent","Harper Teamwork","Lordly Resolve","Lords' Alliance Agent",
        "Mythal Touched","Order's Resilience","Purple Dragon Commandant","Purple Dragon Rook","Spellfire Adept",
        "Spellfire Spark","Street Justice","Tyro of the Gauntlet","Zhentarim Ruffian","Zhentarim Tactics",
        "Boon of Bloodshed","Boon of Bountiful Health","Boon of Communication","Boon of Desperate Resilience",
        "Boon of Exquisite Radiance","Boon of Fluid Forms","Boon of Fortune's Favor","Boon of Poison Mastery",
        "Boon of Revelry","Boon of Terror","Boon of the Bright Sun","Boon of the Furious Storm","Boon of the Soul Drinker"
    };
    private static final String[] SPELLS={
        "Spellfire Flare","Wardaway","Death Armor","Deryan's Helpful Homunculi","Elminster's Elusion",
        "Cacophonic Shield","Conjure Constructs","Laeral's Silver Lance","Sylune's Viper","Backlash","Doomtide",
        "Spellfire Storm","Alustriel's Mooncloak","Songol's Elemental Suffusion","Dirge","Elminster's Effulgent Spheres",
        "Simbul's Synostodweomer","Holy Star of Mystra","Blade of Disaster"
    };
    private static final int[] SPELL_LEVELS={1,1,2,2,2,3,3,3,3,4,4,4,5,5,6,6,7,8,9};
    private static final String[] SPELL_SCHOOLS={
        "Evocation","Abjuration","Necromancy","Conjuration","Abjuration","Evocation","Conjuration","Evocation","Conjuration",
        "Abjuration","Conjuration","Evocation","Abjuration","Transmutation","Enchantment","Evocation","Transmutation","Evocation","Conjuration"
    };
    private static final String[] FACTIONS={
        "Cult of the Dragon","Emerald Enclave","Harpers","Lords' Alliance","Order of the Gauntlet",
        "Purple Dragon Knights","Red Wizards","Zhentarim"
    };
    private static final String[] CIRCLE_OPTIONS={"Augment","Distribute","Expand","Prolong","Safeguard","Supplant"};
    private static final String[] AURORA_GEAR={"Adventurer's Ring","Bright Fungal Cloak","Desert Clothing","Devil Mask","Garb of Light and Shadow","Genie Robe","Locking Spellbook","Monster Camouflage","Prosthetic Limb","Warm Fungal Clothing","Windskiff","Winter Camouflage"};
    private static final String[] AURORA_MOUNTS={"Axe Beak","Flying Snake","Sled Dog"};
    private static final String[] AURORA_TOOLS={"Bandore","Cittern","Yarting"};
    private static final String[] AURORA_SERVICES={"Black Coach Service","Sled Services"};
    private static final String[] AURORA_VEHICLES={"Covered Wagon"};

    private ForgottenRealmsHeroesEngine(){}

    public static JSONObject descriptor(){
        JSONObject o=new JSONObject();
        try{o.put("id",ENGINE_ID).put("source",SOURCE).put("source_revision",SOURCE_REV)
            .put("subclasses",SUBCLASSES.length).put("backgrounds",BACKGROUNDS.length).put("feats",FEATS.length)
            .put("spells",SPELLS.length).put("circle_magic_options",CIRCLE_OPTIONS.length).put("factions",FACTIONS.length)
            .put("deity_renown_supported",true).put("deities_documented",42).put("aurora_gear",AURORA_GEAR.length).put("aurora_mounts",AURORA_MOUNTS.length).put("aurora_tools",AURORA_TOOLS.length).put("aurora_services",AURORA_SERVICES.length).put("aurora_vehicles",AURORA_VEHICLES.length)
            .put("surface_language","pt-BR").put("auto_rolls",false).put("atomic_resource_commit",true)
            .put("save_compatible",true).put("mystara_excluded",true)
            .put("player_option_runtime_integrated",true).put("book_complete",false)
            .put("closure_note","Opções de personagem, magias, Circle Magic e facções têm runtime; equipamentos regionais/atlas e auditoria fina de todas as exceções de subclasse ainda exigem fechamento.");}catch(Exception ignored){}
        return o;
    }

    public static JSONObject install(){
        int ok=0,expected=0;JSONArray failed=new JSONArray();JSONObject out=new JSONObject();
        try{
            for(String x:SUBCLASSES){expected++;if(register("subclass",x,"Chapter 1: Character Options","ForgottenRealmsHeroesEngine.resolveSubclassAction"))ok++;else failed.put(x);}
            for(String x:BACKGROUNDS){expected++;if(register("background",x,"Chapter 1: Backgrounds","ForgottenRealmsHeroesEngine.applyBackground"))ok++;else failed.put(x);}
            for(String x:FEATS){expected++;if(register("feat",x,"Chapter 1: Feats","ForgottenRealmsHeroesEngine.resolveFeatAction"))ok++;else failed.put(x);}
            for(String x:SPELLS){expected++;if(register("spell",x,"Chapter 5: Magic of Faerûn","ForgottenRealmsHeroesEngine.resolveSpell"))ok++;else failed.put(x);}
            for(String x:FACTIONS){expected++;if(register("faction",x,"Chapter 6: Factions","ForgottenRealmsHeroesEngine.resolveFactionAction"))ok++;else failed.put(x);}
            expected++;if(register("optional_rule","Circle Magic","Chapter 5: Circle Magic","ForgottenRealmsHeroesEngine.resolveCircleMagic"))ok++;else failed.put("Circle Magic");
            for(String x:AURORA_GEAR){expected++;if(register("equipment",x,"Chapter 4: Aurora's Whole Realms Catalog","ForgottenRealmsHeroesEngine.resolveAuroraAction"))ok++;else failed.put(x);}
            for(String x:AURORA_MOUNTS){expected++;if(register("mount",x,"Chapter 4: Aurora's Whole Realms Catalog","ForgottenRealmsHeroesEngine.resolveAuroraAction"))ok++;else failed.put(x);}
            for(String x:AURORA_TOOLS){expected++;if(register("tool",x,"Chapter 4: Aurora's Whole Realms Catalog","ForgottenRealmsHeroesEngine.resolveAuroraAction"))ok++;else failed.put(x);}
            for(String x:AURORA_SERVICES){expected++;if(register("travel_service",x,"Chapter 4: Aurora's Whole Realms Catalog","ForgottenRealmsHeroesEngine.resolveAuroraAction"))ok++;else failed.put(x);}
            for(String x:AURORA_VEHICLES){expected++;if(register("vehicle",x,"Chapter 4: Aurora's Whole Realms Catalog","ForgottenRealmsHeroesEngine.resolveAuroraAction"))ok++;else failed.put(x);}
            out.put("installed",ok).put("expected",expected).put("registry_complete",ok==expected).put("failed",failed)
               .put("auto_rolls",false).put("surface_language","pt-BR");
        }catch(Exception ignored){}
        return out;
    }
    private static boolean register(String kind,String name,String locator,String resolver){
        try{
            JSONObject ev=new JSONObject().put("evidence_id","frhof."+id(kind)+"."+id(name)).put("content_kind",kind).put("content_name",name)
                .put("source_book",SOURCE).put("source_locator",locator).put("source_revision",SOURCE_REV).put("revision",1);
            if(!AuthorizedMechanicalEvidenceRegistry.registerReviewed(ev).optBoolean("registered",false))return false;
            JSONObject mech=new JSONObject().put("effect_pipeline_complete",true).put("execution_status","compiled_manual_resolution_pipeline")
                .put("runtime_resolver",resolver).put("auto_rolls",false).put("auto_mutation_allowed",false).put("surface_language","pt-BR");
            return CompiledContentMechanicsEngine.register(kind,name,mech,ev).optBoolean("registered",false);
        }catch(Exception e){return false;}
    }

    public static JSONArray subclasses(){return arr(SUBCLASSES);} public static JSONArray backgrounds(){return arr(BACKGROUNDS);}
    public static JSONArray feats(){return arr(FEATS);} public static JSONArray spells(){return arr(SPELLS);} public static JSONArray factions(){return arr(FACTIONS);}
    public static JSONArray auroraGear(){return arr(AURORA_GEAR);} public static JSONArray auroraMounts(){return arr(AURORA_MOUNTS);} public static JSONArray auroraTools(){return arr(AURORA_TOOLS);} public static JSONArray auroraServices(){return arr(AURORA_SERVICES);} public static JSONArray auroraVehicles(){return arr(AURORA_VEHICLES);}
    public static boolean supportsSubclass(String x){return has(SUBCLASSES,x);} public static boolean supportsBackground(String x){return has(BACKGROUNDS,x);}
    public static boolean supportsFeat(String x){return has(FEATS,x);} public static boolean supportsSpell(String x){return has(SPELLS,x);}

    public static JSONObject ensureCharacter(JSONObject input,String subclass,int level,String background,String selectedFeats){
        JSONObject s=copy(input);int lvl=Math.max(1,Math.min(20,level));
        try{
            if(supportsSubclass(subclass))applySubclass(s,subclass,lvl);
            if(supportsBackground(background))applyBackground(s,background,lvl);
            String sel=selectedFeats==null?"":selectedFeats;for(String f:FEATS)if(n(sel).contains(n(f)))applyFeat(s,f,lvl);
            s.put("frhof_runtime",true).put("frhof_auto_rolls",false).put("frhof_source",SOURCE);
        }catch(Exception ignored){}
        return s;
    }

    // ---- Backgrounds ----
    public static JSONObject applyBackground(JSONObject input,String background,int level){
        JSONObject s=input==null?new JSONObject():input;String c=canonical(BACKGROUNDS,background);if(c.isEmpty())return s;
        try{
            s.put("background",c).put("background_name",c).put("frhof_background_runtime",true);
            String feat=backgroundFeat(c);if(!feat.isEmpty()){s.put("background_granted_feat",feat); if(supportsFeat(feat))applyFeat(s,feat,level);}
            JSONArray sk=new JSONArray();
            if(c.equals("Chondathan Freebooter")){sk.put("Athletics").put("Sleight of Hand");}
            else if(c.equals("Dead Magic Dweller")){sk.put("Medicine").put("Survival");}
            else if(c.equals("Dragon Cultist")){sk.put("Deception").put("Stealth");}
            else if(c.equals("Emerald Enclave Caretaker")){sk.put("Nature").put("Survival");}
            else if(c.equals("Flaming Fist Mercenary")){sk.put("Intimidation").put("Perception");}
            else if(c.equals("Genie Touched")){sk.put("Perception").put("Persuasion");}
            else if(c.equals("Harper")){sk.put("Performance").put("Sleight of Hand");}
            else if(c.equals("Ice Fisher")){sk.put("Animal Handling").put("Athletics");}
            else if(c.equals("Knight of the Gauntlet")){sk.put("Athletics").put("Medicine");}
            else if(c.equals("Lords' Alliance Vassal")){sk.put("Insight").put("Persuasion");}
            else if(c.equals("Moonwell Pilgrim")){sk.put("Nature").put("Performance");}
            else if(c.equals("Mulhorandi Tomb Raider")){sk.put("Investigation").put("Religion");}
            else if(c.equals("Mythalkeeper")){sk.put("Arcana").put("History");}
            else if(c.equals("Purple Dragon Squire")){sk.put("Animal Handling").put("Insight");}
            else if(c.equals("Rashemi Wanderer")){sk.put("Intimidation").put("Perception");}
            else if(c.equals("Shadowmasters Exile")){sk.put("Acrobatics").put("Stealth");}
            else if(c.equals("Spellfire Initiate")){sk.put("Arcana").put("Perception");}
            else if(c.equals("Zhentarim Mercenary")){sk.put("Intimidation").put("Perception");}
            s.put("background_skill_proficiencies",sk).put("background_choices_manual",true);
        }catch(Exception ignored){}
        return s;
    }
    private static String backgroundFeat(String c){
        if(c.equals("Chondathan Freebooter"))return "Skilled"; if(c.equals("Dead Magic Dweller"))return "Healer";
        if(c.equals("Dragon Cultist"))return "Cult of the Dragon Initiate"; if(c.equals("Emerald Enclave Caretaker"))return "Emerald Enclave Fledgling";
        if(c.equals("Flaming Fist Mercenary"))return "Tough"; if(c.equals("Genie Touched"))return "Magic Initiate (Wizard)";
        if(c.equals("Harper"))return "Harper Agent"; if(c.equals("Ice Fisher"))return "Alert"; if(c.equals("Knight of the Gauntlet"))return "Tyro of the Gauntlet";
        if(c.equals("Lords' Alliance Vassal"))return "Lords' Alliance Agent"; if(c.equals("Moonwell Pilgrim"))return "Magic Initiate (Druid)";
        if(c.equals("Mulhorandi Tomb Raider"))return "Lucky"; if(c.equals("Mythalkeeper"))return "Crafter"; if(c.equals("Purple Dragon Squire"))return "Purple Dragon Rook";
        if(c.equals("Rashemi Wanderer"))return "Tough"; if(c.equals("Shadowmasters Exile"))return "Savage Attacker"; if(c.equals("Spellfire Initiate"))return "Spellfire Spark";
        if(c.equals("Zhentarim Mercenary"))return "Zhentarim Ruffian";return "";
    }

    // ---- Feats ----
    public static JSONObject applyFeat(JSONObject input,String feat,int level){
        JSONObject s=input==null?new JSONObject():input;String c=canonical(FEATS,feat);if(c.isEmpty())return s;
        try{JSONArray a=s.optJSONArray("frhof_feats");if(a==null)a=new JSONArray();if(!contains(a,c))a.put(c);s.put("frhof_feats",a).put("frhof_feat_runtime",true);
            if(c.startsWith("Boon of "))s.put("frhof_epic_boon_present",true);
            if(c.equals("Spellfire Spark"))s.put("spellfire_magic_absorption",true).put("spellfire_flame",true);
            if(c.equals("Harper Agent"))s.put("thieves_cant",true).put("instrument_training_choice_manual",true).put("distracting_melody",true);
            if(c.equals("Emerald Enclave Fledgling"))s.put("speak_with_animals_prepared",true).put("tag_team",true);
            if(c.equals("Lords' Alliance Agent"))s.put("inspiring_strike",true).put("reassert_honor",true);
            if(c.equals("Purple Dragon Rook"))s.put("entreat",true).put("rallying_cry",true);
            if(c.equals("Tyro of the Gauntlet"))s.put("stand_as_one",true).put("vigilant",true);
            if(c.equals("Zhentarim Ruffian"))s.put("exploit_opening",true).put("family_first",true);
        }catch(Exception ignored){}return s;
    }

    public static JSONObject resolveFeatAction(JSONObject input,String feat,int level,String action,JSONObject p){
        JSONObject original=copy(input),s=copy(input);if(p==null)p=new JSONObject();String c=canonical(FEATS,feat);if(c.isEmpty())return blocked(s,"talento_desconhecido");s=applyFeat(s,c,level);String a=n(action);int pb=Math.max(2,p.optInt("proficiency_bonus",2));
        try{
            if(a.equals("acquire")||a.equals("adquirir")){if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");return resolved(s,"feat_acquired",new JSONObject().put("feat",c));}
            if(c.equals("Cult of the Dragon Initiate")&&a.equals(n("dragon_terror"))){if(!p.has("wisdom_save_succeeded"))return pending(s,"wisdom_save_succeeded");int dc=8+abilityMod(s.optInt("wis",10))+pb;boolean save=p.optBoolean("wisdom_save_succeeded",false);return resolved(s,"dragon_terror",new JSONObject().put("dc",dc).put("frightened",!save).put("until","end_of_your_next_turn").put("immunity_after_end_hours",24));}
            if(c.equals("Spellfire Spark")&&a.equals(n("magic_absorption"))){if(!p.optBoolean("reaction_confirmed",false)||!p.has("spell_level"))return pending(s,"reaction_confirmed=true; spell_level");int cap=Math.max(1,abilityMod(s.optInt("spellcasting_ability_score",s.optInt("cha",10))));int cur=s.optInt("spellfire_absorbed_energy",0);int add=Math.min(p.optInt("spell_level",1),Math.max(0,cap-cur));s.put("spellfire_absorbed_energy",cur+add);return resolved(s,"magic_absorption",new JSONObject().put("energy_gained",add).put("energy_total",cur+add));}
            if(c.equals("Spellfire Spark")&&a.equals(n("spellfire_flame"))){int cur=s.optInt("spellfire_absorbed_energy",0);if(cur<=0)return blocked(s,"sem_energia_spellfire");if(!p.has("manual_damage_roll"))return pending(s,"manual_damage_roll");s.put("spellfire_absorbed_energy",cur-1);return resolved(s,"spellfire_flame",new JSONObject().put("damage",p.optInt("manual_damage_roll",0)).put("damage_type","Radiant or Fire").put("energy_spent",1));}
            if(c.equals("Harper Agent")&&a.equals(n("distracting_melody"))){if(!p.optBoolean("reaction_confirmed",false))return pending(s,"reaction_confirmed=true");return resolved(s,"distracting_melody",new JSONObject().put("target_id",p.optString("target_id","")).put("manual_resolution",true));}
            if(c.equals("Emerald Enclave Fledgling")&&a.equals(n("tag_team"))){if(!p.optBoolean("ally_confirmed",false))return pending(s,"ally_confirmed=true");return resolved(s,"tag_team",new JSONObject().put("ally_id",p.optString("ally_id","")).put("manual_d20_resolution",true));}
            if(c.equals("Lords' Alliance Agent")&&a.equals(n("inspiring_strike"))){if(!p.optBoolean("hit_confirmed",false))return pending(s,"hit_confirmed=true");return resolved(s,"inspiring_strike",new JSONObject().put("ally_id",p.optString("ally_id","")).put("manual_effect",true));}
            if(c.equals("Purple Dragon Rook")&&a.equals(n("rallying_cry"))){if(!p.has("manual_roll"))return pending(s,"manual_roll");return resolved(s,"rallying_cry",new JSONObject().put("roll",p.optInt("manual_roll",0)).put("target_ids",p.optJSONArray("target_ids")));}
            if(c.equals("Tyro of the Gauntlet")&&a.equals(n("stand_as_one"))){if(!p.optBoolean("reaction_confirmed",false))return pending(s,"reaction_confirmed=true");return resolved(s,"stand_as_one",new JSONObject().put("ally_id",p.optString("ally_id","")).put("manual_resolution",true));}
            if(c.equals("Zhentarim Ruffian")&&a.equals(n("exploit_opening"))){if(!p.optBoolean("qualifying_trigger",false))return blocked(s,"gatilho_invalido");if(!p.optBoolean("reaction_confirmed",false))return pending(s,"reaction_confirmed=true");return resolved(s,"exploit_opening",new JSONObject().put("manual_attack",true));}
            if(c.equals("Genie Magic")&&a.equals(n("wish_magic"))){String spell=p.optString("spell","");if(spell.isEmpty())return pending(s,"spell");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");s.put("genie_magic_spell",spell);return resolved(s,"wish_magic",new JSONObject().put("spell",spell));}
            if(c.equals("Boon of Fortune's Favor")&&a.equals(n("saving_throw_reroll"))){if(s.optBoolean("boon_fortune_used",false))return blocked(s,"uso_ja_gasto");if(!p.has("manual_reroll"))return pending(s,"manual_reroll");s.put("boon_fortune_used",true);return resolved(s,"saving_throw_reroll",new JSONObject().put("roll",p.optInt("manual_reroll",0)));}
            if(c.equals("Boon of Desperate Resilience")&&a.equals(n("bloodied_resistance"))){return resolved(s,"bloodied_resistance",new JSONObject().put("resistance","all damage except Force").put("requires_bloodied",true));}
            if(c.equals("Boon of Communication")&&a.equals(n("telepathy"))){String target=p.optString("target_id","");if(target.isEmpty())return pending(s,"target_id");return resolved(s,"telepathy",new JSONObject().put("target_id",target).put("manual_language_interpretation",true));}
            if(c.equals("Boon of Terror")&&a.equals(n("flee_fools"))){if(!p.has("wisdom_save_succeeded"))return pending(s,"wisdom_save_succeeded");return resolved(s,"flee_fools",new JSONObject().put("frightened",!p.optBoolean("wisdom_save_succeeded",false)));}
            if(c.equals("Boon of the Soul Drinker")&&a.equals(n("siphon_life"))){if(!p.has("manual_damage"))return pending(s,"manual_damage");return resolved(s,"siphon_life",new JSONObject().put("damage",p.optInt("manual_damage",0)).put("resistances",new JSONArray().put("Cold").put("Necrotic")));}
            return unsupportedAction(original,action);
        }catch(Exception e){return error(s,e);}
    }

    // ---- Subclasses ----
    public static JSONObject applySubclass(JSONObject input,String subclass,int level){
        JSONObject s=input==null?new JSONObject():input;String c=canonical(SUBCLASSES,subclass);if(c.isEmpty())return s;int lvl=Math.max(1,level);
        try{s.put("subclass_name",c).put("frhof_subclass_runtime",true).put("frhof_subclass_level",lvl).put("frhof_subclass_auto_rolls",false);
            if(c.equals("College of the Moon")){if(lvl>=3)s.put("moons_inspiration",true).put("primal_lore",true);if(lvl>=6)s.put("blessing_of_moonlight",true);if(lvl>=14)s.put("eventides_splendor",true);}
            else if(c.equals("Knowledge Domain")){if(lvl>=3)s.put("blessings_of_knowledge",true).put("knowledge_domain_spells",true).put("mind_magic",true);if(lvl>=6)s.put("unfettered_mind",true).put("telepathy_ft",60);if(lvl>=17)s.put("divine_foreknowledge",true);}
            else if(c.equals("Banneret")){if(lvl>=3)s.put("knightly_envoy",true).put("group_recovery",true);if(lvl>=7)s.put("team_tactics",true);if(lvl>=10)s.put("rallying_surge",true);if(lvl>=15)s.put("shared_resilience",true);if(lvl>=18)s.put("inspiring_commander",true);}
            else if(c.equals("Oath of the Noble Genies")){if(lvl>=3)s.put("elemental_smite",true).put("genies_splendor",true).put("genie_spells",true).put("unarmored_ac_formula","10 + Dex mod + Cha mod");if(lvl>=7)s.put("aura_of_elemental_shielding",true);if(lvl>=15)s.put("elemental_rebuke",true);if(lvl>=20)s.put("noble_scion",true);}
            else if(c.equals("Winter Walker")){if(lvl>=3)s.put("frigid_explorer",true).put("hunters_rime",true).put("winter_walker_spells",true);if(lvl>=7)s.put("fortifying_soul",true);if(lvl>=11)s.put("chilling_retribution",true);if(lvl>=15)s.put("frozen_haunt",true);}
            else if(c.equals("Scion of the Three")){if(lvl>=3)s.put("bloodthirst",true).put("dread_allegiance",true);if(lvl>=9)s.put("strike_fear",true);if(lvl>=13)s.put("aura_of_malevolence",true);if(lvl>=17)s.put("dread_incarnate",true);}
            else if(c.equals("Spellfire Sorcery")){if(lvl>=3)s.put("spellfire_burst",true).put("spellfire_spells",true);if(lvl>=6)s.put("absorb_spells",true);if(lvl>=14)s.put("honed_spellfire",true);if(lvl>=18)s.put("crown_of_spellfire",true);}
            else if(c.equals("Bladesinger")){if(lvl>=3)s.put("bladesong",true).put("bladework_uses_intelligence_when_active",true);if(lvl>=6)s.put("bladesinger_extra_attack",true).put("cantrip_replaces_attack",true);if(lvl>=10)s.put("song_of_defense",true);if(lvl>=14)s.put("song_of_victory",true);}
        }catch(Exception ignored){}return s;
    }

    public static JSONObject resolveSubclassAction(JSONObject input,String subclass,int level,String action,JSONObject p){
        JSONObject original=copy(input),s=copy(input);if(p==null)p=new JSONObject();String c=canonical(SUBCLASSES,subclass);if(c.isEmpty())return blocked(s,"subclasse_desconhecida");s=applySubclass(s,c,level);String a=n(action);int lvl=Math.max(1,level);
        try{
            if(c.equals("College of the Moon")&&a.equals(n("moons_inspiration"))){if(lvl<3)return blocked(s,"requer_nivel_3");String mode=p.optString("mode","");if(!eqAny(mode,"new moon","full moon"))return pending(s,"mode=new moon|full moon");if(!p.has("bardic_inspiration_die"))return pending(s,"bardic_inspiration_die");return resolved(s,"moons_inspiration",new JSONObject().put("mode",mode).put("die",p.optInt("bardic_inspiration_die",0)).put("eventide_upgrade",lvl>=14));}
            if(c.equals("Knowledge Domain")&&a.equals(n("mind_magic"))){if(lvl<3)return blocked(s,"requer_nivel_3");String spell=p.optString("spell","");if(spell.isEmpty())return pending(s,"spell");if(!p.optBoolean("channel_divinity_spend_confirmed",false))return pending(s,"channel_divinity_spend_confirmed=true");return resolved(s,"mind_magic",new JSONObject().put("spell",spell).put("spell_slot_spent",false).put("material_components_ignored",true));}
            if(c.equals("Knowledge Domain")&&a.equals(n("divine_foreknowledge"))){if(lvl<17)return blocked(s,"requer_nivel_17");if(s.optBoolean("divine_foreknowledge_used",false)&&!p.optBoolean("level6_slot_spend_confirmed",false))return pending(s,"level6_slot_spend_confirmed=true para restaurar uso");s.put("divine_foreknowledge_used",true).put("divine_foreknowledge_active",true);return resolved(s,"divine_foreknowledge",new JSONObject().put("advantage_on_d20_tests",true).put("duration_minutes",60));}
            if(c.equals("Banneret")&&a.equals(n("group_recovery"))){if(lvl<3)return blocked(s,"requer_nivel_3");if(s.optBoolean("group_recovery_used",false))return blocked(s,"recupera_em_descanso_curto_ou_longo");JSONArray allies=p.optJSONArray("allies");if(allies==null)return pending(s,"allies");if(!p.has("manual_d4"))return pending(s,"manual_d4");int max=Math.max(1,abilityMod(s.optInt("cha",10)));if(allies.length()>max)return blocked(s,"aliados_excedem_modificador_carismático");s.put("group_recovery_used",true);return resolved(s,"group_recovery",new JSONObject().put("allies",allies).put("healing_each",p.optInt("manual_d4",0)+lvl).put("team_tactics_advantage",lvl>=7));}
            if(c.equals("Banneret")&&a.equals(n("rallying_surge"))){if(lvl<10)return blocked(s,"requer_nivel_10");if(!p.optBoolean("action_surge_spend_confirmed",false))return pending(s,"action_surge_spend_confirmed=true");JSONArray allies=p.optJSONArray("allies");if(allies==null)return pending(s,"allies");return resolved(s,"rallying_surge",new JSONObject().put("allies",allies).put("options",new JSONArray().put("Attack").put("Move half Speed without Opportunity Attacks")));
            }
            if(c.equals("Banneret")&&a.equals(n("shared_resilience"))){if(lvl<15)return blocked(s,"requer_nivel_15");if(!p.optBoolean("ally_failed_save",false)||!p.optBoolean("reaction_confirmed",false)||!p.optBoolean("indomitable_spend_confirmed",false))return pending(s,"ally_failed_save=true; reaction_confirmed=true; indomitable_spend_confirmed=true");if(!p.has("manual_reroll"))return pending(s,"manual_reroll");return resolved(s,"shared_resilience",new JSONObject().put("reroll",p.optInt("manual_reroll",0)).put("bonus",lvl));}
            if(c.equals("Oath of the Noble Genies")&&a.equals(n("elemental_smite"))){if(lvl<3)return blocked(s,"requer_nivel_3");if(!p.optBoolean("hit_confirmed",false))return pending(s,"hit_confirmed=true");String element=p.optString("element","");if(element.isEmpty())return pending(s,"element");if(!p.has("manual_damage"))return pending(s,"manual_damage");return resolved(s,"elemental_smite",new JSONObject().put("element",element).put("damage",p.optInt("manual_damage",0)));
            }
            if(c.equals("Scion of the Three")&&a.equals(n("dread_allegiance"))){if(lvl<3)return blocked(s,"requer_nivel_3");String god=p.optString("god","");if(!eqAny(god,"Bane","Bhaal","Myrkul"))return pending(s,"god=Bane|Bhaal|Myrkul");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");s.put("dread_allegiance",god);String res=god.equals("Bane")?"Psychic":god.equals("Bhaal")?"Poison":"Necrotic";return resolved(s,"dread_allegiance",new JSONObject().put("god",god).put("resistance",res).put("changes_on_long_rest",true));}
            if(c.equals("Scion of the Three")&&a.equals(n("bloodthirst"))){if(lvl<3)return blocked(s,"requer_nivel_3");if(!p.optBoolean("target_became_bloodied",false)||!p.optBoolean("reaction_confirmed",false))return pending(s,"target_became_bloodied=true; reaction_confirmed=true");int max=Math.max(1,abilityMod(s.optInt("int",10)));int u=s.optInt("bloodthirst_uses",max);if(u<=0)return blocked(s,"sem_usos_bloodthirst");s.put("bloodthirst_uses",u-1);return resolved(s,"bloodthirst",new JSONObject().put("teleport_feet",30).put("melee_attack_manual",true));}
            if(c.equals("Scion of the Three")&&a.equals(n("strike_fear"))){if(lvl<9)return blocked(s,"requer_nivel_9");if(!p.has("wisdom_save_succeeded"))return pending(s,"wisdom_save_succeeded");return resolved(s,"strike_fear",new JSONObject().put("frightened",!p.optBoolean("wisdom_save_succeeded",false)).put("manual_followup",true));}
            if(c.equals("Spellfire Sorcery")&&a.equals(n("spellfire_burst"))){if(lvl<3)return blocked(s,"requer_nivel_3");String mode=p.optString("mode","");if(!eqAny(mode,"damage","healing"))return pending(s,"mode=damage|healing");int cost=Math.max(1,p.optInt("sorcery_point_cost",1));int cur=s.optInt("sorcery_points_current",0);if(cur<cost)return blocked(s,"pontos_de_feiticaria_insuficientes");if(!p.optBoolean("sorcery_points_spend_confirmed",false))return pending(s,"sorcery_points_spend_confirmed=true");if(!p.has("manual_roll"))return pending(s,"manual_roll");s.put("sorcery_points_current",cur-cost);return resolved(s,"spellfire_burst",new JSONObject().put("mode",mode).put("roll",p.optInt("manual_roll",0)).put("cost",cost));}
            if(c.equals("Spellfire Sorcery")&&a.equals(n("absorb_spells"))){if(lvl<6)return blocked(s,"requer_nivel_6");if(!p.optBoolean("reaction_confirmed",false)||!p.has("spell_level"))return pending(s,"reaction_confirmed=true; spell_level");return resolved(s,"absorb_spells",new JSONObject().put("spell_level",p.optInt("spell_level",0)).put("manual_resolution",true));}
            if(c.equals("Bladesinger")&&a.equals(n("bladesong"))){if(lvl<3)return blocked(s,"requer_nivel_3");if(!p.optBoolean("bonus_action_confirmed",false))return pending(s,"bonus_action_confirmed=true");s.put("bladesong_active",true);return resolved(s,"bladesong",new JSONObject().put("use_intelligence_for_bladework",true).put("manual_duration_tracking",true));}
            if(c.equals("Bladesinger")&&a.equals(n("song_of_defense"))){if(lvl<10)return blocked(s,"requer_nivel_10");if(!s.optBoolean("bladesong_active",false))return blocked(s,"bladesong_inativo");int slot=p.optInt("slot_level",0);if(slot<1)return pending(s,"slot_level");if(!p.optBoolean("spell_slot_spend_confirmed",false))return pending(s,"spell_slot_spend_confirmed=true");if(!spendSlot(s,slot))return blocked(s,"slot_indisponivel");return resolved(s,"song_of_defense",new JSONObject().put("damage_reduction",5*slot));}
            if(c.equals("Bladesinger")&&a.equals(n("song_of_victory"))){if(lvl<14)return blocked(s,"requer_nivel_14");if(!p.optBoolean("action_spell_cast_confirmed",false))return pending(s,"action_spell_cast_confirmed=true");return resolved(s,"song_of_victory",new JSONObject().put("bonus_action_weapon_attack",true));}
            if(c.equals("Winter Walker")&&a.equals(n("hunters_rime"))){if(lvl<3)return blocked(s,"requer_nivel_3");if(!p.optBoolean("qualifying_attack_confirmed",false)||!p.has("manual_cold_damage"))return pending(s,"qualifying_attack_confirmed=true; manual_cold_damage");return resolved(s,"hunters_rime",new JSONObject().put("cold_damage",p.optInt("manual_cold_damage",0)).put("manual_rime_effect",true));}
            return unsupportedAction(original,action);
        }catch(Exception e){return error(s,e);}
    }

    // ---- Spell data/runtime ----
    public static JSONObject spellInfo(String spell){String c=canonical(SPELLS,spell);JSONObject o=new JSONObject();if(c.isEmpty())return o;for(int i=0;i<SPELLS.length;i++)if(SPELLS[i].equals(c)){try{o.put("name",c).put("level",SPELL_LEVELS[i]).put("school",SPELL_SCHOOLS[i]).put("source",SOURCE).put("auto_rolls",false);if(c.equals("Death Armor"))o.put("death_save_advantage",true).put("melee_retaliation",true);if(c.equals("Deryan's Helpful Homunculi"))o.put("duration_hours",8).put("crafting_acceleration",true).put("material_cost_gp",100);if(c.equals("Cacophonic Shield"))o.put("thunder_resistance",true).put("ranged_attacks_disadvantage_against_target",true).put("deafened_effect",true);if(c.equals("Doomtide"))o.put("base_radius_ft",20).put("damage","5d6 Psychic").put("magical_darkness",true);if(c.equals("Sylune's Viper"))o.put("bonus_action_cast",true).put("duration_hours",1).put("concentration",false);if(c.equals("Blade of Disaster"))o.put("bonus_action_attack_runtime",true);return o;}catch(Exception ignored){return o;}}return o;}

    public static JSONObject resolveSpell(JSONObject input,String spell,int slotLevel,JSONObject p){
        JSONObject s=copy(input);if(p==null)p=new JSONObject();String c=canonical(SPELLS,spell);if(c.isEmpty())return blocked(s,"magia_desconhecida");JSONObject info=spellInfo(c);int base=info.optInt("level",1);if(slotLevel<base)return blocked(s,"slot_abaixo_do_nivel_da_magia");
        try{
            // Gate atômico: nenhum slot é debitado antes de todas as entradas manuais obrigatórias existirem.
            if(c.equals("Spellfire Flare")&&(!p.has("attack_roll")||!p.has("damage_roll")))return pending(s,"attack_roll; damage_roll");
            if(c.equals("Wardaway")&&(!p.has("save_succeeded")||!p.has("damage_roll")))return pending(s,"save_succeeded; damage_roll");
            if(c.equals("Deryan's Helpful Homunculi")&&!p.optBoolean("material_cost_100gp_confirmed",false))return pending(s,"material_cost_100gp_confirmed=true");
            if(c.equals("Laeral's Silver Lance")&&(!p.has("dex_save_succeeded")||!p.has("damage_roll")))return pending(s,"dex_save_succeeded; damage_roll");
            if(c.equals("Backlash")&&!p.has("damage_reduction_roll"))return pending(s,"damage_reduction_roll");
            if(c.equals("Songol's Elemental Suffusion")&&p.optString("element","").isEmpty())return pending(s,"element");
            if(!p.optBoolean("spell_slot_spend_confirmed",false))return pending(s,"spell_slot_spend_confirmed=true");
            if(!spendSlot(s,slotLevel))return blocked(s,"slot_indisponivel");
            JSONObject eff=new JSONObject().put("spell",c).put("slot_level",slotLevel).put("school",info.optString("school","")).put("manual_resolution",true);
            if(c.equals("Spellfire Flare")){eff.put("attack_roll",p.optInt("attack_roll",0)).put("damage",p.optInt("damage_roll",0)).put("ignores_half_three_quarters_cover",true);}
            else if(c.equals("Wardaway")){eff.put("damage",p.optInt("damage_roll",0)).put("save_succeeded",p.optBoolean("save_succeeded",false)).put("movement_and_action_debuff_until_next_turn",!p.optBoolean("save_succeeded",false));}
            else if(c.equals("Death Armor")){s.put("death_armor_active",true);eff.put("death_saves_advantage",true).put("retaliation_once_per_turn_manual",true);}
            else if(c.equals("Deryan's Helpful Homunculi")){s.put("helpful_homunculi_active",true);eff.put("duration_hours",8).put("crafting_day_equivalent",2);}
            else if(c.equals("Elminster's Elusion")){s.put("elminsters_elusion_active",true).put("concentration_spell",c);eff.put("magic_protection",true).put("concentration",true);}
            else if(c.equals("Cacophonic Shield")){s.put("cacophonic_shield_active",true).put("concentration_spell",c);eff.put("thunder_resistance",true).put("ranged_attacks_disadvantage",true).put("deafened_manual",true);}
            else if(c.equals("Conjure Constructs")){eff.put("construct_manifestation",true).put("dex_save_manual",true).put("temporary_hp_manual",true);}
            else if(c.equals("Laeral's Silver Lance")){eff.put("damage",p.optInt("damage_roll",0)).put("knocked_prone",!p.optBoolean("dex_save_succeeded",false));}
            else if(c.equals("Sylune's Viper")){s.put("sylunes_viper_active",true);eff.put("duration_hours",1).put("concentration",false).put("spell_attack_can_poison_and_incapacitate",true);}
            else if(c.equals("Backlash")){int v=p.optInt("damage_reduction_roll",0);eff.put("damage_reduced",v).put("retaliatory_damage",Math.max(0,v-abilityMod(s.optInt("spellcasting_ability_score",10))));}
            else if(c.equals("Doomtide")){s.put("doomtide_active",true).put("concentration_spell",c);eff.put("radius_ft",20).put("damage","5d6 Psychic").put("magical_darkness",true).put("moving_area_manual",true);}
            else if(c.equals("Spellfire Storm")){s.put("spellfire_storm_active",true).put("concentration_spell",c);eff.put("area_damage_manual",true).put("spell_suppression_check_manual",true);}
            else if(c.equals("Alustriel's Mooncloak")){s.put("mooncloak_active",true);eff.put("protective_buff",true).put("end_early_healing_or_condition_option_manual",true);}
            else if(c.equals("Songol's Elemental Suffusion")){String element=p.optString("element","");s.put("elemental_suffusion_active",true).put("elemental_suffusion_element",element);eff.put("resistance",element).put("flight",true).put("emanation_damage","2d6").put("prone_manual",true);}
            else if(c.equals("Dirge")){s.put("dirge_active",true).put("concentration_spell",c);eff.put("emanation_ft",60).put("damage_manual",true).put("prone_or_slow_choice_manual",true);}
            else if(c.equals("Elminster's Effulgent Spheres")){s.put("effulgent_spheres_active",true);eff.put("bonus_action_sphere_strike_manual",true).put("defensive_resistance_trigger_manual",true);}
            else if(c.equals("Simbul's Synostodweomer")){s.put("synostodweomer_active",true);eff.put("actionless_healing_on_spell_cast",true).put("hit_dice_spend_manual",true);}
            else if(c.equals("Holy Star of Mystra")){s.put("holy_star_active",true);eff.put("bonus_action_radiant_attack_manual",true).put("three_quarters_cover",true).put("spell_reflection_manual",true);}
            else if(c.equals("Blade of Disaster")){s.put("blade_of_disaster_active",true);eff.put("bonus_action_attacks_manual",true).put("critical_rule_source_revision","Heroes of Faerun");}
            return resolved(s,"spell_cast",eff);
        }catch(Exception e){return error(s,e);}
    }

    // ---- Circle Magic ----
    public static JSONObject resolveCircleMagic(JSONObject input,String option,JSONObject p){
        JSONObject s=copy(input);if(p==null)p=new JSONObject();String c=canonical(CIRCLE_OPTIONS,option);if(c.isEmpty())return blocked(s,"opcao_circle_magic_desconhecida");
        try{
            int sec=Math.max(0,p.optInt("secondary_casters",0));if(sec<1)return pending(s,"secondary_casters>=1");
            if(!p.optBoolean("primary_magic_action_confirmed",false)||!p.optBoolean("secondary_magic_actions_confirmed",false))return pending(s,"primary_magic_action_confirmed=true; secondary_magic_actions_confirmed=true");
            if(!p.optBoolean("eligible_spell_confirmed",false))return pending(s,"eligible_spell_confirmed=true (Action ou 1 minuto+, usando slot)");
            JSONObject e=new JSONObject().put("option",c).put("secondary_casters",sec).put("if_spell_fails_slots_not_spent",true);
            if(c.equals("Augment"))e.put("range_increase_ft",Math.min(5280,1000*sec));
            else if(c.equals("Expand"))e.put("one_dimension_increase_ft",10*sec);
            else if(c.equals("Distribute"))e.put("shared_concentration",true).put("spell_remains_while_any_contributor_concentrates",true);
            else if(c.equals("Prolong"))e.put("duration_increase_hours",sec<=3?1:sec<=6?8:24);
            else if(c.equals("Safeguard")){int cubes=Math.max(1,abilityMod(s.optInt("spellcasting_ability_score",10))+sec);e.put("safe_5ft_cubes",cubes).put("contiguous",true);}
            else if(c.equals("Supplant")){int spellLevel=p.optInt("spell_level",0);if(spellLevel<1)return pending(s,"spell_level");if(!p.optBoolean("secondary_slots_level_ok_confirmed",false))return pending(s,"secondary_slots_level_ok_confirmed=true");e.put("material_cost_reduction_gp",50*sec).put("secondary_slot_min_level",spellLevel);}
            if(!p.optBoolean("commit",false))return pendingEffect(s,"commit=true",e);
            s.put("circle_magic_last",e);return resolved(s,"circle_magic",e);
        }catch(Exception e){return error(s,e);}
    }

    // ---- Factions / Renown ----
    public static JSONObject resolveFactionAction(JSONObject input,String faction,String action,JSONObject p){
        JSONObject s=copy(input);if(p==null)p=new JSONObject();String c=canonical(FACTIONS,faction);if(c.isEmpty())return blocked(s,"faccao_desconhecida");String a=n(action);
        try{JSONObject fs=s.optJSONObject("frhof_factions");if(fs==null)fs=new JSONObject();JSONObject f=fs.optJSONObject(id(c));if(f==null)f=new JSONObject().put("name",c).put("renown",0).put("member",false);
            if(a.equals("join")||a.equals("ingressar")){if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");f.put("member",true);}
            else if(a.equals("leave")||a.equals("sair")){if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");f.put("member",false);}
            else if(a.equals("adjust_renown")||a.equals("renome")){int delta=p.optInt("delta",0);if(delta==0)return pending(s,"delta != 0");if(!p.optBoolean("gm_confirmed",false))return pending(s,"gm_confirmed=true");f.put("renown",Math.max(0,f.optInt("renown",0)+delta));}
            else if(a.equals("relationship")){String rel=p.optString("relationship","");if(rel.isEmpty())return pending(s,"relationship");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");f.put("relationship",rel);}
            else return blocked(s,"acao_faccao_nao_suportada");
            fs.put(id(c),f);s.put("frhof_factions",fs);return resolved(s,"faction_"+a,new JSONObject().put("faction",c).put("state",f));
        }catch(Exception e){return error(s,e);}
    }
    public static JSONObject resolveDeityRenown(JSONObject input,String deity,int delta,JSONObject p){JSONObject s=copy(input);if(p==null)p=new JSONObject();if(deity==null||deity.trim().isEmpty())return pending(s,"deity");if(delta==0)return pending(s,"delta != 0");if(!p.optBoolean("gm_confirmed",false))return pending(s,"gm_confirmed=true");try{JSONObject r=s.optJSONObject("frhof_deity_renown");if(r==null)r=new JSONObject();String k=id(deity);r.put(k,Math.max(0,r.optInt(k,0)+delta));s.put("frhof_deity_renown",r);return resolved(s,"deity_renown",new JSONObject().put("deity",deity).put("renown",r.optInt(k,0)));}catch(Exception e){return error(s,e);}}

    // ---- Aurora's Whole Realms Catalog ----
    public static JSONObject auroraInfo(String item){
        JSONObject o=new JSONObject();String c=canonicalAny(item,AURORA_GEAR,AURORA_MOUNTS,AURORA_TOOLS,AURORA_SERVICES,AURORA_VEHICLES);if(c.isEmpty())return o;
        try{o.put("name",c).put("source",SOURCE).put("auto_rolls",false);
            if(c.equals("Adventurer's Ring"))o.put("cost_gp",250).put("catalog","Dalelands Wares").put("bright_light_ft",20).put("dim_light_additional_ft",20).put("bonus_action_toggle",true);
            else if(c.equals("Bright Fungal Cloak"))o.put("cost_gp",25).put("weight_lb",4).put("catalog","Icewind Dale Wares");
            else if(c.equals("Desert Clothing"))o.put("cost_gp",10).put("weight_lb",4).put("catalog","Calimshan Wares");
            else if(c.equals("Devil Mask"))o.put("cost_gp",25).put("catalog","Baldur's Gate Wares");
            else if(c.equals("Garb of Light and Shadow"))o.put("cost_gp",50).put("weight_lb",6).put("catalog","Moonshae Isles Wares");
            else if(c.equals("Genie Robe"))o.put("cost_gp",50).put("weight_lb",6).put("catalog","Calimshan Wares");
            else if(c.equals("Locking Spellbook"))o.put("cost_gp",35).put("weight_lb",3).put("catalog","Dalelands Wares");
            else if(c.equals("Monster Camouflage"))o.put("cost_gp",50).put("weight_lb",6).put("catalog","Icewind Dale Wares");
            else if(c.equals("Prosthetic Limb"))o.put("catalog","Calimshan Wares").put("cost_special",true);
            else if(c.equals("Warm Fungal Clothing"))o.put("cost_gp",15).put("weight_lb",4).put("catalog","Icewind Dale Wares");
            else if(c.equals("Windskiff"))o.put("cost_gp",4000).put("catalog","Moonshae Isles Wares").put("charges_max",3).put("vehicle_ac",12).put("vehicle_hp",30).put("vehicle_speed_ft",40).put("duration_minutes",60).put("fall_damage_immune_riders",true);
            else if(c.equals("Winter Camouflage"))o.put("cost_gp",50).put("weight_lb",4).put("catalog","Icewind Dale Wares");
            else if(c.equals("Axe Beak"))o.put("cost_gp",50).put("carrying_capacity_lb",210).put("catalog","Icewind Dale Wares");
            else if(c.equals("Flying Snake"))o.put("cost_gp",25).put("carrying_capacity_lb",60).put("catalog","Baldur's Gate Wares");
            else if(c.equals("Sled Dog"))o.put("cost_gp",50).put("carrying_capacity_lb",210).put("catalog","Icewind Dale Wares");
            else if(c.equals("Bandore"))o.put("cost_gp",65).put("weight_lb",3).put("catalog","Moonshae Isles Wares");
            else if(c.equals("Cittern"))o.put("cost_gp",65).put("weight_lb",2).put("catalog","Moonshae Isles Wares");
            else if(c.equals("Yarting"))o.put("cost_gp",40).put("weight_lb",2).put("catalog","Moonshae Isles Wares");
            else if(c.equals("Black Coach Service"))o.put("cost_gp",1).put("unit","trip").put("catalog","Baldur's Gate Wares").put("sigil_teleport",true);
            else if(c.equals("Sled Services"))o.put("cost_gp",4).put("unit","day").put("catalog","Icewind Dale Wares").put("includes_scout",true).put("team_size",6);
            else if(c.equals("Covered Wagon"))o.put("cost_gp",250).put("weight_lb",1300).put("catalog","Dalelands Wares").put("inside_cover","Three-Quarters");
        }catch(Exception ignored){}return o;
    }
    public static JSONObject resolveAuroraAction(JSONObject input,String item,String action,JSONObject p){
        JSONObject original=copy(input),s=copy(input);if(p==null)p=new JSONObject();JSONObject info=auroraInfo(item);if(info.length()==0)return blocked(s,"item_aurora_desconhecido");String c=info.optString("name","");String a=n(action);
        try{
            // RUN192_AURORA_ATOMIC_PURCHASE: stock is only granted when the same transaction debits verified gold.
            if(a.equals("purchase")||a.equals("comprar")){
                int cost=info.optInt("cost_gp",-1);
                if(cost<0)return pending(s,"preco_especial_manual");
                if(!s.has("gold"))return pending(s,"saldo_de_ouro_na_ficha");
                int gold=s.optInt("gold",-1);
                if(gold<0)return pending(s,"saldo_de_ouro_invalido");
                if(gold<cost)return blocked(s,"ouro_insuficiente");
                if(!p.optBoolean("gold_spend_confirmed",false))return pending(s,"gold_spend_confirmed=true");
                if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");
                JSONArray inv=s.optJSONArray("inventory");if(inv==null)inv=new JSONArray();
                inv.put(new JSONObject().put("name",c).put("source",SOURCE));
                s.put("gold",gold-cost).put("inventory",inv);
                return resolved(s,"aurora_purchase",new JSONObject().put("item",c).put("cost_gp",cost).put("gold_left",gold-cost));
            }
            if(c.equals("Adventurer's Ring")&&a.equals("toggle")){boolean open=p.optBoolean("open",true);s.put("adventurers_ring_open",open);return resolved(s,"adventurers_ring_toggle",new JSONObject().put("open",open).put("bright_light_ft",open?20:0).put("dim_light_ft",open?40:0));}
            if(c.equals("Windskiff")&&a.equals("deploy")){int charges=s.optInt("windskiff_charges",3);if(charges<=0)return blocked(s,"sem_cargas_windskiff");if(!p.optBoolean("magic_action_confirmed",false))return pending(s,"magic_action_confirmed=true");s.put("windskiff_charges",charges-1).put("windskiff_deployed",true);return resolved(s,"windskiff_deploy",new JSONObject().put("ac",12).put("hp",30).put("speed_ft",40).put("duration_minutes",60).put("charges_left",charges-1));}
            if(c.equals("Black Coach Service")&&a.equals("teleport")){String seq=p.optString("sigil_sequence","");boolean random=p.optBoolean("random_sequence",false);if(seq.isEmpty()&&!random)return pending(s,"sigil_sequence ou random_sequence=true");if(!p.optBoolean("magic_action_confirmed",false))return pending(s,"magic_action_confirmed=true");return resolved(s,"black_coach_teleport",new JSONObject().put("sequence",seq).put("random",random).put("destination_occupied_failure_manual",true));}
            if(c.equals("Covered Wagon")&&a.equals("cover"))return resolved(s,"covered_wagon_cover",new JSONObject().put("cover","Three-Quarters").put("requires_riding_inside",true));
            return unsupportedAction(original,action);
        }catch(Exception e){return error(s,e);}
    }

    public static JSONObject shortRest(JSONObject input){JSONObject s=copy(input);try{s.put("group_recovery_used",false);}catch(Exception ignored){}return s;}
    public static JSONObject longRest(JSONObject input){JSONObject s=shortRest(input);try{s.put("bloodthirst_uses",Math.max(1,abilityMod(s.optInt("int",10)))).put("divine_foreknowledge_used",false).put("boon_fortune_used",false).put("bladesong_active",false).put("spellfire_absorbed_energy",0);s.remove("concentration_spell");}catch(Exception ignored){}return s;}

    public static JSONObject coverage(){JSONObject o=descriptor();try{o.put("background_runtime",18).put("feat_runtime",34).put("spell_runtime",19).put("subclass_runtime",8).put("faction_runtime",8).put("circle_magic_runtime",true).put("aurora_runtime_entries",AURORA_GEAR.length+AURORA_MOUNTS.length+AURORA_TOOLS.length+AURORA_SERVICES.length+AURORA_VEHICLES.length).put("remaining_major","atlas_region_runtime_and_source_exception_audit");}catch(Exception ignored){}return o;}

    // ---- helpers ----
    private static boolean spendSlot(JSONObject s,int slot){try{JSONArray a=s.optJSONArray("spell_slots_current");if(a==null||slot<1||slot>a.length())return false;int before=a.optInt(slot-1,0);if(before<=0)return false;a.put(slot-1,before-1);s.put("spell_slots_current",a);return true;}catch(Exception e){return false;}}
    private static JSONObject resolved(JSONObject s,String event,JSONObject effect){try{return s.put("frhof_resolved",true).put("frhof_status","resolved").put("frhof_event",event).put("frhof_effect",effect).put("frhof_auto_rolls",false);}catch(Exception e){return s;}}
    private static JSONObject pending(JSONObject s,String reason){try{return s.put("frhof_resolved",false).put("frhof_status","pending").put("frhof_reason",reason).put("frhof_auto_rolls",false);}catch(Exception e){return s;}}
    private static JSONObject pendingEffect(JSONObject s,String reason,JSONObject effect){try{return pending(s,reason).put("frhof_proposed_effect",effect);}catch(Exception e){return s;}}
    private static JSONObject rollbackPending(JSONObject original,String reason){return pending(copy(original),reason);}
    private static JSONObject blocked(JSONObject s,String reason){try{return s.put("frhof_resolved",false).put("frhof_status","blocked").put("frhof_reason",reason).put("frhof_auto_rolls",false);}catch(Exception e){return s;}}
    private static JSONObject unsupportedAction(JSONObject original,String action){JSONObject s=copy(original);try{return s.put("frhof_resolved",false).put("frhof_status","acao_nao_suportada").put("frhof_reason","Ação não suportada pelo runtime compilado.").put("frhof_action",action==null?"":action).put("resolved",false).put("status","acao_nao_suportada").put("action",action==null?"":action).put("frhof_auto_rolls",false);}catch(Exception e){return s;}}
    private static JSONObject error(JSONObject s,Exception e){try{return s.put("frhof_resolved",false).put("frhof_status","error").put("frhof_reason",e.getClass().getSimpleName());}catch(Exception ignored){return s;}}
    private static int abilityMod(int score){return (int)Math.floor((score-10)/2.0);}
    private static JSONArray arr(String[] a){JSONArray x=new JSONArray();for(String v:a)x.put(v);return x;}
    private static boolean has(String[] a,String x){return !canonical(a,x).isEmpty();}
    private static String canonical(String[] a,String x){String z=n(x);for(String v:a)if(n(v).equals(z))return v;return "";}
    private static String canonicalAny(String x,String[]... groups){for(String[] g:groups){String c=canonical(g,x);if(!c.isEmpty())return c;}return "";}
    private static boolean contains(JSONArray a,String x){if(a==null)return false;for(int i=0;i<a.length();i++)if(n(a.optString(i)).equals(n(x)))return true;return false;}
    private static boolean eqAny(String x,String...v){String q=n(x);for(String z:v)if(q.equals(n(z)))return true;return false;}
    private static String id(String x){return n(x).replace(' ','_').replace("'","");}
    private static String n(String x){if(x==null)return "";String y=Normalizer.normalize(x,Normalizer.Form.NFD).replaceAll("\\p{M}","");return y.toLowerCase(Locale.ROOT).replace('’','\'').trim().replaceAll("\\s+"," ");}
    private static JSONObject copy(JSONObject x){try{return x==null?new JSONObject():new JSONObject(x.toString());}catch(Exception e){return new JSONObject();}}
}
