package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Post-release source-bound campaign systems from Tasha's Cauldron of Everything.
 * Pure/stateful helpers only: no autonomous PC mutation. The caller/Barbara reviews
 * world consequences before committing them.
 */
public final class TashaCampaignSystemsEngine {
    public static final String BOOK = TashaCompiledSourceRulesEngine.BOOK;
    public static final String SOURCE_FILE = TashaCompiledSourceRulesEngine.SOURCE_FILE;
    public static final String SOURCE_SHA256 = TashaCompiledSourceRulesEngine.SOURCE_SHA256;

    private static final String[] PATRON_TYPES = {
            "academy","aristocrat","military_force","guild","religious_order","ancient_being","criminal_syndicate","sovereign"
    };
    private static final String[] PATRON_LABELS_PT = {
            "Academia","Aristocrata","Força Militar","Guilda","Ordem Religiosa","Ser Antigo","Sindicato Criminoso","Soberano"
    };
    private static final String[] MONSTER_TYPES = {
            "aberration","beast","celestial","construct","dragon","elemental","fey","fiend","giant","humanoid","monstrosity","ooze","plant","undead"
    };
    private static final String[][] RESEARCH_SKILLS = {
            {"Arcana"},
            {"Animal Handling","Nature","Survival"},
            {"Arcana","Religion"},
            {"Arcana"},
            {"Arcana","History","Nature"},
            {"Arcana","Nature"},
            {"Arcana","Nature"},
            {"Arcana","Religion"},
            {"History"},
            {"History"},
            {"Nature","Survival"},
            {"Arcana","Survival"},
            {"Nature","Survival"},
            {"Arcana","Religion"}
    };
    private static final String[][] DESIRES = {
            {"rare biological specimen","flattery or submission","unknown secret or knowledge","accept an unusual organic graft"},
            {"fresh meat","soothing melody","bright trinkets","soft familiar toy"},
            {"heroic story","promise of charitable acts","trophy of a defeated tyrant","sacred relic or family heirloom"},
            {"maintenance oil","charged magic item as fuel","elemental-powered vessel","adamantine or mithral components"},
            {"gold or gems","spoils from draconic rivals","multigenerational antique","flattering artistic likeness"},
            {"valuable gem","pure sample of a favored element","way back to its native plane","performance tied to its plane"},
            {"cherished memory","a personal physical trait","deeply sentimental object","sublime poem"},
            {"soul or grave promise","desecrated sacred object","blood of a loved one","breaking a sacred promise"},
            {"proof of giant craft superiority","strong work animal","several barrels of drink","treasure stolen from a rival giant"},
            {"recover a culturally important item","friendly contest","recover something lost","information on an enemy weakness"},
            {"remove remains from its last meal","favorite food","drive away a rival","imitate a courtship display"},
            {"putrid liquid","foul-smelling cloth","bones or metal","effervescent fluid"},
            {"fertilizer","Feywild-infused spring water","remove invasive vegetation","destroy axes and fire tools"},
            {"blood","personal memento from life","materials to improve its resting place","finish an unfinished mortal task"}
    };

    private TashaCampaignSystemsEngine() {}

    public static JSONObject descriptor() {
        JSONObject o = new JSONObject();
        try {
            o.put("id","tasha_campaign_systems")
                    .put("book",BOOK).put("source_file",SOURCE_FILE).put("source_sha256",SOURCE_SHA256)
                    .put("group_patron_types",PATRON_TYPES.length)
                    .put("sidekick_classes",3)
                    .put("monster_research_types",MONSTER_TYPES.length)
                    .put("hazards",new JSONArray().put("avalanche"))
                    .put("subclass_change",true)
                    .put("persistence_ready",true)
                    .put("barbara_review_required",true)
                    .put("auto_pc_mutation",false);
        } catch (Exception ignored) {}
        return o;
    }

    // ---- Group patron ----
    public static JSONObject emptyPatron() {
        JSONObject o = new JSONObject();
        try {
            o.put("schema",1).put("active",false).put("type","").put("name","")
                    .put("benefits",new JSONArray()).put("assignments",new JSONArray())
                    .put("assist_usage",new JSONObject()).put("rest_serial",0).put("revision",0);
        } catch (Exception ignored) {}
        return o;
    }

    public static JSONArray patronTypes() {/*BR_JSON_WRAP_106*/try{
        JSONArray a = new JSONArray();
        for (int i=0;i<PATRON_TYPES.length;i++) a.put(new JSONObject().put("id",PATRON_TYPES[i]).put("label_pt",PATRON_LABELS_PT[i]));
        return a;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject configurePatron(JSONObject state,String type,String name,JSONArray benefits,int day) {
        String canonical = canonicalPatron(type);
        if (canonical.isEmpty()) throw new IllegalArgumentException("unknown Tasha group patron type");
        JSONObject o = copy(state); if (o.length()==0) o=emptyPatron();
        try {
            o.put("active",true).put("type",canonical).put("name",clean(name,patronLabel(canonical)))
                    .put("benefits",benefits==null?new JSONArray():copy(benefits)).put("configured_day",Math.max(1,day))
                    .put("source_book",BOOK).put("source_page",85).put("source_sha256",SOURCE_SHA256)
                    .put("revision",o.optInt("revision",0)+1);
        } catch (Exception ignored) {}
        return o;
    }

    /** Each member may grant advantage once until that member finishes a long rest. */
    public static JSONObject patronAssist(JSONObject state,String member,String target,boolean canSeeOrHear,boolean memberIncapacitated,boolean targetIncapacitated,int restSerial,int day) {
        JSONObject o=copy(state); if(!o.optBoolean("active",false)) throw new IllegalArgumentException("group patron not active");
        String m=clean(member,""); String t=clean(target,""); if(m.isEmpty()||t.isEmpty())throw new IllegalArgumentException("member and target required");
        if(!canSeeOrHear||memberIncapacitated||targetIncapacitated)throw new IllegalArgumentException("patron assistance conditions not satisfied");
        JSONObject usage=o.optJSONObject("assist_usage");if(usage==null)usage=new JSONObject();
        int serial=Math.max(0,restSerial); if(usage.optInt(m,-1)==serial)throw new IllegalArgumentException("member already granted patron advantage this long-rest cycle");
        try {
            usage.put(m,serial);o.put("assist_usage",usage).put("rest_serial",Math.max(o.optInt("rest_serial",0),serial))
                    .put("last_assist",new JSONObject().put("member",m).put("target",t).put("day",Math.max(1,day)).put("advantage",true).put("scope","ability_check_attack_or_save"))
                    .put("revision",o.optInt("revision",0)+1);
        } catch(Exception ignored){}
        return o;
    }

    public static JSONObject addPatronAssignment(JSONObject state,String key,String title,int dueDay,int day) {/*BR_JSON_WRAP_106*/try{
        JSONObject o=copy(state);if(!o.optBoolean("active",false))throw new IllegalArgumentException("group patron not active");String k=clean(key,"");if(k.isEmpty())throw new IllegalArgumentException("assignment key required");
        JSONArray src=o.optJSONArray("assignments");if(src==null)src=new JSONArray();for(int i=0;i<src.length();i++){JSONObject a=src.optJSONObject(i);if(a!=null&&k.equals(a.optString("key")))return o;}
        JSONObject a=new JSONObject().put("key",k).put("title",clean(title,k)).put("status","offered").put("offered_day",Math.max(1,day)).put("due_day",Math.max(0,dueDay)).put("consequence_pending",false);
        try{src.put(a);o.put("assignments",src).put("revision",o.optInt("revision",0)+1);}catch(Exception ignored){}return o;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject resolvePatronAssignment(JSONObject state,String key,String status,String summary,int day) {/*BR_JSON_WRAP_106*/try{
        JSONObject o=copy(state);JSONArray src=o.optJSONArray("assignments");if(src==null)throw new IllegalArgumentException("no patron assignments");JSONArray dst=new JSONArray();boolean found=false;
        for(int i=0;i<src.length();i++){JSONObject a=src.optJSONObject(i);if(a==null)continue;JSONObject n=copy(a);if(key!=null&&key.equals(a.optString("key"))){String s=clean(status,"completed").toLowerCase(Locale.ROOT);if(!s.equals("completed")&&!s.equals("declined")&&!s.equals("failed")&&!s.equals("cancelled"))throw new IllegalArgumentException("invalid patron assignment status");n.put("status",s).put("resolved_day",Math.max(1,day)).put("summary",summary==null?"":summary.trim()).put("consequence_pending",false);found=true;}dst.put(n);}if(!found)throw new IllegalArgumentException("unknown patron assignment");o.put("assignments",dst).put("revision",o.optInt("revision",0)+1);return o;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject advancePatronDay(JSONObject state,int day) {/*BR_JSON_WRAP_106*/try{
        JSONObject o=copy(state);JSONArray src=o.optJSONArray("assignments");if(src==null)return o;JSONArray dst=new JSONArray();int d=Math.max(1,day);
        for(int i=0;i<src.length();i++){JSONObject a=src.optJSONObject(i);if(a==null)continue;JSONObject n=copy(a);String s=n.optString("status");int due=n.optInt("due_day",0);if(due>0&&d>due&&(s.equals("offered")||s.equals("accepted")||s.equals("in_progress"))){n.put("status","overdue").put("overdue_days",d-due).put("consequence_pending",true);}dst.put(n);}try{o.put("assignments",dst);}catch(Exception ignored){}return o;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject academyMinimumCompensation(int memberCount,int serviceDays,int relevantDiscoveries) {/*BR_JSON_WRAP_106*/try{
        int members=Math.max(0,memberCount),days=Math.max(0,serviceDays),discoveries=Math.max(0,relevantDiscoveries);
        return new JSONObject().put("daily_gp",members*days).put("discovery_minimum_gp",discoveries*250).put("minimum_total_gp",members*days+discoveries*250).put("source_page",86).put("requires_patron_review",true);
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    // ---- Subclass change ----
    public static JSONObject subclassChangePlan(int classLevel,boolean returningToPrevious,boolean suddenTransformationApproved) {/*BR_JSON_WRAP_106*/try{
        int lvl=Math.max(1,classLevel);JSONObject o=new JSONObject();
        if(suddenTransformationApproved)return o.put("mode","sudden").put("training_days",0).put("training_cost_gp",0).put("requires_gm_approval",true).put("auto_apply",false).put("source_page",7);
        int days=2*lvl;if(returningToPrevious)days=Math.max(1,days/2);int cost=returningToPrevious?0:100*lvl;
        return o.put("mode","training").put("training_days",days).put("training_cost_gp",cost).put("returning_to_previous",returningToPrevious).put("requires_gm_approval",true).put("auto_apply",false).put("source_page",7);
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    // ---- Sidekicks ----
    public static JSONObject sidekickEligibility(String statblockId,boolean friendshipEstablished,boolean canSpeakLanguage,String sidekickClass) {/*BR_JSON_WRAP_106*/try{
        JSONObject p=CompiledStatblockEngine.runtimeProfile(statblockId==null?"":statblockId.trim());double cr=p.optDouble("challenge_rating",Double.MAX_VALUE);String cls=canonicalSidekickClass(sidekickClass);
        boolean crOk=p.optBoolean("found",false)&&cr<=0.5d;boolean languageOk=!"expert".equals(cls)&&!"spellcaster".equals(cls)||canSpeakLanguage;boolean ok=crOk&&friendshipEstablished&&!cls.isEmpty()&&languageOk;
        return new JSONObject().put("eligible",ok).put("statblock_found",p.optBoolean("found",false)).put("challenge_rating",cr).put("cr_max",0.5).put("friendship_established",friendshipEstablished).put("class",cls).put("language_requirement_met",languageOk).put("source_page",140);
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject createSidekick(JSONObject sidekicks,String id,String statblockId,String name,String sidekickClass,boolean friendshipEstablished,boolean canSpeakLanguage,String role,int day) {/*BR_JSON_WRAP_106*/try{
        JSONObject all=sidekicks==null?new JSONObject():copy(sidekicks);String key=clean(id,"");if(key.isEmpty())throw new IllegalArgumentException("sidekick id required");if(all.optJSONObject(key)!=null)throw new IllegalArgumentException("sidekick already exists");JSONObject e=sidekickEligibility(statblockId,friendshipEstablished,canSpeakLanguage,sidekickClass);if(!e.optBoolean("eligible",false))throw new IllegalArgumentException("sidekick eligibility failed");JSONObject profile=CompiledStatblockEngine.runtimeProfile(statblockId);int hp=Math.max(1,profile.optInt("hit_points",1));String cls=e.optString("class");
        JSONObject s=new JSONObject().put("id",key).put("name",clean(name,key)).put("statblock_id",statblockId).put("class",cls).put("level",1).put("proficiency_bonus",2).put("hit_points_max",hp).put("hit_points_current",hp).put("friendship_established",true).put("status","active").put("created_day",Math.max(1,day)).put("source_page",140).put("features_unlocked",sidekickFeatures(cls,1)).put("runtime",sidekickRuntimeDefaults(cls,1));
        if("spellcaster".equals(cls)){String r=canonicalSpellcasterRole(role);if(r.isEmpty())throw new IllegalArgumentException("spellcaster role must be mage, healer, or prodigy");s.put("role",r).put("spellcasting_ability",r.equals("mage")?"INT":r.equals("healer")?"WIS":"CHA");}
        if("warrior".equals(cls)){String r=canonicalWarriorRole(role);if(r.isEmpty())throw new IllegalArgumentException("warrior role must be attacker or defender");s.put("role",r);}
        all.put(key,s);return all;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject createSidekickAtLevel(JSONObject sidekicks,String id,String statblockId,String name,String sidekickClass,boolean friendshipEstablished,boolean canSpeakLanguage,String role,int startingLevel,JSONArray hpDieRolls,int constitutionModifier,int day){/*BR_JSON_WRAP_106*/try{int target=Math.max(1,Math.min(20,startingLevel));JSONObject all=createSidekick(sidekicks,id,statblockId,name,sidekickClass,friendshipEstablished,canSpeakLanguage,role,day);if(target==1)return all;if(hpDieRolls==null||hpDieRolls.length()<target-1)throw new IllegalArgumentException("starting sidekick above level 1 requires one hit-die roll per gained level");for(int lvl=2;lvl<=target;lvl++){int roll=hpDieRolls.optInt(lvl-2);if(roll<1)throw new IllegalArgumentException("sidekick hit-die rolls must be positive");all=levelSidekick(all,id,roll,constitutionModifier,day);}JSONObject s=all.optJSONObject(id);if(s!=null)s.put("starting_level",target).put("started_at_party_average_level",true).put("source_page",140);return all;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject levelSidekick(JSONObject sidekicks,String id,int hpDieRoll,int constitutionModifier,int day) {/*BR_JSON_WRAP_106*/try{
        JSONObject all=copy(sidekicks);JSONObject old=all.optJSONObject(id);if(old==null)throw new IllegalArgumentException("unknown sidekick");int level=old.optInt("level",1);if(level>=20)throw new IllegalArgumentException("sidekick already level 20");int gain=Math.max(1,Math.max(1,hpDieRoll)+constitutionModifier);int next=level+1;JSONObject s=copy(old);s.put("level",next).put("proficiency_bonus",proficiencyForLevel(next)).put("hit_points_max",old.optInt("hit_points_max",1)+gain).put("hit_points_current",old.optInt("hit_points_current",1)+gain).put("last_level_day",Math.max(1,day)).put("last_hp_gain",gain).put("features_unlocked",sidekickFeatures(s.optString("class"),next));s.put("runtime",syncSidekickRuntime(s.optJSONObject("runtime"),s.optString("class"),next));all.put(id,s);return all;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONArray sidekickFeatures(String sidekickClass,int level) {
        String cls=canonicalSidekickClass(sidekickClass);JSONArray a=new JSONArray();int l=Math.max(1,Math.min(20,level));
        if("expert".equals(cls)){
            if(l>=1){a.put("bonus_proficiencies");a.put("helpful_bonus_action");}if(l>=2)a.put("cunning_action");if(l>=3)a.put("expertise_2");if(l>=4)a.put("asi_4");if(l>=6)a.put("coordinated_strike");if(l>=7)a.put("evasion");if(l>=8)a.put("asi_8");if(l>=10)a.put("asi_10");if(l>=11)a.put("inspiring_help_1d6");if(l>=12)a.put("asi_12");if(l>=13)a.put("expertise_4");if(l>=14)a.put("reliable_talent");if(l>=16)a.put("asi_16");if(l>=18)a.put("sharp_mind");if(l>=19)a.put("asi_19");if(l>=20)a.put("inspiring_help_2d6");
        } else if("spellcaster".equals(cls)){
            if(l>=1)a.put("spellcasting");if(l>=4)a.put("asi_4");if(l>=6)a.put("potent_cantrips");if(l>=8)a.put("asi_8");if(l>=12)a.put("asi_12");if(l>=14)a.put("empowered_spells");if(l>=16)a.put("asi_16");if(l>=18)a.put("asi_18");if(l>=20)a.put("focused_casting");
        } else if("warrior".equals(cls)){
            if(l>=1){a.put("bonus_proficiencies");a.put("martial_role");}if(l>=2)a.put("second_wind_1");if(l>=3)a.put("improved_critical");if(l>=4)a.put("asi_4");if(l>=6)a.put("extra_attack_1");if(l>=7)a.put("battle_readiness");if(l>=8)a.put("asi_8");if(l>=10)a.put("improved_defense");if(l>=11)a.put("indomitable_1");if(l>=12)a.put("asi_12");if(l>=14)a.put("asi_14");if(l>=15)a.put("extra_attack_2");if(l>=16)a.put("asi_16");if(l>=18)a.put("indomitable_2");if(l>=19)a.put("asi_19");if(l>=20)a.put("second_wind_2");
        }
        return a;
    }

    public static int proficiencyForLevel(int level){int l=Math.max(1,Math.min(20,level));return 2+(l-1)/4;}

    // ---- Active sidekick runtime ----
    public static JSONObject sidekickShortRest(JSONObject sidekicks,String id,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(sidekicks);JSONObject s=requireSidekick(all,id);JSONObject rt=syncSidekickRuntime(s.optJSONObject("runtime"),s.optString("class"),s.optInt("level",1));rt.put("short_rest_serial",rt.optInt("short_rest_serial",0)+1).put("second_wind_used",0).put("last_short_rest_day",Math.max(1,day));s.put("runtime",rt);all.put(id,s);return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject sidekickLongRest(JSONObject sidekicks,String id,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(sidekicks);JSONObject s=requireSidekick(all,id);String cls=s.optString("class");int level=s.optInt("level",1);JSONObject rt=syncSidekickRuntime(s.optJSONObject("runtime"),cls,level);rt.put("short_rest_serial",rt.optInt("short_rest_serial",0)+1).put("long_rest_serial",rt.optInt("long_rest_serial",0)+1).put("second_wind_used",0).put("indomitable_used",0).put("last_long_rest_day",Math.max(1,day));if("spellcaster".equals(cls)){JSONArray mx=spellSlotArray(level);rt.put("spell_slots_remaining",mx).put("spell_slots_max",copy(mx));}s.put("runtime",rt);all.put(id,s);return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject warriorProfile(JSONObject sidekick){/*BR_JSON_WRAP_106*/try{JSONObject s=sidekick==null?new JSONObject():sidekick;int level=s.optInt("level",1);String role=canonicalWarriorRole(s.optString("role"));if(!"warrior".equals(canonicalSidekickClass(s.optString("class"))))throw new IllegalArgumentException("warrior sidekick required");return new JSONObject().put("attack_bonus_adjustment","attacker".equals(role)?2:0).put("defender_reaction_available","defender".equals(role)).put("critical_minimum",level>=3?19:20).put("attack_count",level>=15?3:level>=6?2:1).put("initiative_advantage",level>=7).put("armor_class_bonus",level>=10?1:0).put("indomitable_uses_per_long_rest",level>=18?2:level>=11?1:0).put("second_wind_uses_per_short_or_long_rest",level>=20?2:level>=2?1:0).put("source_page",145);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject warriorSecondWind(JSONObject sidekicks,String id,int d10Roll,int day){/*BR_JSON_WRAP_106*/try{if(d10Roll<1||d10Roll>10)throw new IllegalArgumentException("second wind requires a d10 roll from 1 to 10");JSONObject all=copy(sidekicks);JSONObject s=requireClass(all,id,"warrior");int level=s.optInt("level",1);if(level<2)throw new IllegalArgumentException("second wind requires warrior level 2");JSONObject rt=syncSidekickRuntime(s.optJSONObject("runtime"),"warrior",level);int limit=level>=20?2:1,used=rt.optInt("second_wind_used",0);if(used>=limit)throw new IllegalArgumentException("second wind exhausted until short or long rest");int before=s.optInt("hit_points_current",1),max=Math.max(before,s.optInt("hit_points_max",before)),healing=d10Roll+level,after=Math.min(max,before+healing);s.put("hit_points_current",after);rt.put("second_wind_used",used+1).put("last_second_wind",new JSONObject().put("roll",d10Roll).put("level",level).put("healing_rolled",healing).put("healing_applied",after-before).put("day",Math.max(1,day)));s.put("runtime",rt);all.put(id,s);return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject warriorIndomitable(JSONObject sidekicks,String id,boolean originalFailed,int rerollTotal,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(sidekicks);JSONObject s=requireClass(all,id,"warrior");int level=s.optInt("level",1);if(level<11)throw new IllegalArgumentException("indomitable requires warrior level 11");if(!originalFailed)throw new IllegalArgumentException("indomitable only rerolls a failed saving throw");JSONObject rt=syncSidekickRuntime(s.optJSONObject("runtime"),"warrior",level);int limit=level>=18?2:1,used=rt.optInt("indomitable_used",0);if(used>=limit)throw new IllegalArgumentException("indomitable exhausted until long rest");rt.put("indomitable_used",used+1).put("last_indomitable",new JSONObject().put("reroll_total",rerollTotal).put("must_use_new_roll",true).put("day",Math.max(1,day)));s.put("runtime",rt);all.put(id,s);return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject expertHelp(JSONObject sidekicks,String id,String ally,String target,boolean helpsAttack,double targetDistanceFeet,int inspiringRoll,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(sidekicks);JSONObject s=requireClass(all,id,"expert");int level=s.optInt("level",1);if(level<1)throw new IllegalArgumentException("helpful requires expert level 1");if(helpsAttack&&level>=6&&targetDistanceFeet>30d)throw new IllegalArgumentException("coordinated strike target must be within 30 feet");JSONObject rt=syncSidekickRuntime(s.optJSONObject("runtime"),"expert",level);JSONObject h=new JSONObject().put("ally",clean(ally,"ally")).put("target",target==null?"":target.trim()).put("help_as_bonus_action",true).put("helps_attack",helpsAttack).put("day",Math.max(1,day));if(level>=11){int sides=level>=20?12:6; // 2d6 is represented explicitly below at level 20
            if(level>=20){if(inspiringRoll<2||inspiringRoll>12)throw new IllegalArgumentException("level 20 inspiring help expects total of 2d6 (2..12)");h.put("inspiring_help_dice","2d6").put("inspiring_help_roll",inspiringRoll);}else{if(inspiringRoll<1||inspiringRoll>6)throw new IllegalArgumentException("inspiring help expects d6 roll 1..6");h.put("inspiring_help_dice","1d6").put("inspiring_help_roll",inspiringRoll);}h.put("inspiring_help_can_shift_from_attack_to_damage",helpsAttack);}if(level>=6&&helpsAttack){h.put("coordinated_strike_ready",true).put("coordinated_strike_damage","2d6").put("coordinated_strike_range_ft",30);rt.put("coordinated_strike_pending",new JSONObject().put("target",target==null?"":target.trim()).put("expires_end_of_current_turn",true));}rt.put("last_help",h);s.put("runtime",rt);all.put(id,s);return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject expertCoordinatedStrike(JSONObject sidekicks,String id,String target,boolean attackHit,int rolled2d6,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(sidekicks);JSONObject s=requireClass(all,id,"expert");int level=s.optInt("level",1);if(level<6)throw new IllegalArgumentException("coordinated strike requires expert level 6");JSONObject rt=syncSidekickRuntime(s.optJSONObject("runtime"),"expert",level),pending=rt.optJSONObject("coordinated_strike_pending");if(pending==null)throw new IllegalArgumentException("no coordinated strike pending");String expected=pending.optString("target");if(!expected.isEmpty()&&!expected.equals(target))throw new IllegalArgumentException("coordinated strike target mismatch");if(attackHit&&(rolled2d6<2||rolled2d6>12))throw new IllegalArgumentException("coordinated strike expects 2d6 total from 2 to 12");rt.put("coordinated_strike_pending",JSONObject.NULL).put("last_coordinated_strike",new JSONObject().put("target",target==null?"":target).put("attack_hit",attackHit).put("extra_damage",attackHit?rolled2d6:0).put("damage_same_type_as_attack",true).put("day",Math.max(1,day)));s.put("runtime",rt);all.put(id,s);return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject expertEvasion(JSONObject sidekick,boolean incapacitated,boolean dexSaveSuccess,int incomingDamage){/*BR_JSON_WRAP_106*/try{JSONObject s=sidekick==null?new JSONObject():sidekick;if(!"expert".equals(canonicalSidekickClass(s.optString("class")))||s.optInt("level",1)<7)throw new IllegalArgumentException("evasion requires expert level 7");int dmg=Math.max(0,incomingDamage);int applied=incapacitated?dmg:(dexSaveSuccess?0:dmg/2);return new JSONObject().put("incoming_damage",dmg).put("save_success",dexSaveSuccess).put("incapacitated",incapacitated).put("damage_applied",applied).put("source_page",141);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject expertReliableTalent(JSONObject sidekick,boolean includesFullProficiency,int d20Roll){/*BR_JSON_WRAP_106*/try{JSONObject s=sidekick==null?new JSONObject():sidekick;if(!"expert".equals(canonicalSidekickClass(s.optString("class")))||s.optInt("level",1)<14)throw new IllegalArgumentException("reliable talent requires expert level 14");if(d20Roll<1||d20Roll>20)throw new IllegalArgumentException("d20 roll must be 1..20");int used=includesFullProficiency&&d20Roll<=9?10:d20Roll;return new JSONObject().put("original_d20",d20Roll).put("includes_full_proficiency",includesFullProficiency).put("treated_d20",used).put("source_page",142);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject spellcasterProfile(JSONObject sidekick,int spellcastingAbilityModifier){/*BR_JSON_WRAP_106*/try{JSONObject s=sidekick==null?new JSONObject():sidekick;if(!"spellcaster".equals(canonicalSidekickClass(s.optString("class"))))throw new IllegalArgumentException("spellcaster sidekick required");int level=Math.max(1,Math.min(20,s.optInt("level",1))),pb=proficiencyForLevel(level);return new JSONObject().put("level",level).put("role",s.optString("role")).put("spellcasting_ability",s.optString("spellcasting_ability")).put("spell_save_dc",8+pb+spellcastingAbilityModifier).put("spell_attack_modifier",pb+spellcastingAbilityModifier).put("cantrips_known",spellcasterCantripsKnown(level)).put("spells_known",spellcasterSpellsKnown(level)).put("spell_slots",spellSlotArray(level)).put("potent_cantrips",level>=6).put("empowered_spells",level>=14).put("focused_casting",level>=20).put("source_pages",new JSONArray().put(143).put(144));}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject spellcasterCast(JSONObject sidekicks,String id,int spellLevel,int spellcastingAbilityModifier,boolean cantripDealsDamage,boolean empoweredSchoolMatches,int day){/*BR_JSON_WRAP_106*/try{JSONObject all=copy(sidekicks);JSONObject s=requireClass(all,id,"spellcaster");int level=s.optInt("level",1);if(spellLevel<0||spellLevel>5)throw new IllegalArgumentException("sidekick spell level must be 0..5");JSONObject rt=syncSidekickRuntime(s.optJSONObject("runtime"),"spellcaster",level);if(spellLevel>0){JSONArray slots=rt.optJSONArray("spell_slots_remaining");if(slots==null)slots=spellSlotArray(level);int idx=spellLevel-1,current=slots.optInt(idx);if(current<=0)throw new IllegalArgumentException("no spell slot remaining at requested level");slots=setArrayInt(slots,idx,current-1);rt.put("spell_slots_remaining",slots);}JSONObject cast=new JSONObject().put("spell_level",spellLevel).put("spell_save_dc",8+proficiencyForLevel(level)+spellcastingAbilityModifier).put("spell_attack_modifier",proficiencyForLevel(level)+spellcastingAbilityModifier).put("day",Math.max(1,day));if(spellLevel==0&&level>=6&&cantripDealsDamage)cast.put("bonus_damage_from_potent_cantrips",spellcastingAbilityModifier);if(spellLevel>0&&level>=14&&empoweredSchoolMatches)cast.put("bonus_damage_or_healing_from_empowered_spells",spellcastingAbilityModifier);if(level>=20)cast.put("damage_cannot_break_concentration",true);rt.put("last_cast",cast);s.put("runtime",rt);all.put(id,s);return all;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject spellcasterConcentrationAfterDamage(JSONObject sidekick,int damage){/*BR_JSON_WRAP_106*/try{JSONObject s=sidekick==null?new JSONObject():sidekick;if(!"spellcaster".equals(canonicalSidekickClass(s.optString("class"))))throw new IllegalArgumentException("spellcaster sidekick required");int level=s.optInt("level",1);return new JSONObject().put("damage",Math.max(0,damage)).put("focused_casting_active",level>=20).put("concentration_broken_by_damage",false).put("requires_normal_concentration_check",level<20).put("source_page",144);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONArray spellSlotArray(int level){int l=Math.max(1,Math.min(20,level));int[][] slots={{2,0,0,0,0},{2,0,0,0,0},{3,0,0,0,0},{3,0,0,0,0},{4,2,0,0,0},{4,2,0,0,0},{4,3,0,0,0},{4,3,0,0,0},{4,3,2,0,0},{4,3,2,0,0},{4,3,3,0,0},{4,3,3,0,0},{4,3,3,1,0},{4,3,3,1,0},{4,3,3,2,0},{4,3,3,2,0},{4,3,3,3,1},{4,3,3,3,1},{4,3,3,3,2},{4,3,3,3,2}};JSONArray a=new JSONArray();for(int v:slots[l-1])a.put(v);return a;}
    public static int spellcasterCantripsKnown(int level){int l=Math.max(1,Math.min(20,level));return l>=10?4:l>=4?3:2;}
    public static int spellcasterSpellsKnown(int level){int[] a={1,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,11,11};return a[Math.max(1,Math.min(20,level))-1];}

    private static JSONObject sidekickRuntimeDefaults(String cls,int level){/*BR_JSON_WRAP_106*/try{JSONObject rt=new JSONObject().put("short_rest_serial",0).put("long_rest_serial",0).put("second_wind_used",0).put("indomitable_used",0);if("spellcaster".equals(canonicalSidekickClass(cls))){JSONArray mx=spellSlotArray(level);rt.put("spell_slots_remaining",mx).put("spell_slots_max",copy(mx));}return rt;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONObject syncSidekickRuntime(JSONObject runtime,String cls,int level){/*BR_JSON_WRAP_106*/try{JSONObject rt=copy(runtime);if(!rt.has("short_rest_serial"))rt.put("short_rest_serial",0);if(!rt.has("long_rest_serial"))rt.put("long_rest_serial",0);if(!rt.has("second_wind_used"))rt.put("second_wind_used",0);if(!rt.has("indomitable_used"))rt.put("indomitable_used",0);if("spellcaster".equals(canonicalSidekickClass(cls))){JSONArray max=spellSlotArray(level),oldMax=rt.optJSONArray("spell_slots_max"),cur=rt.optJSONArray("spell_slots_remaining"),n=new JSONArray();for(int i=0;i<max.length();i++){int mx=max.optInt(i);if(cur==null)n.put(mx);else{int current=Math.max(0,cur.optInt(i)),old=oldMax==null?mx:oldMax.optInt(i),gain=Math.max(0,mx-old);n.put(Math.min(mx,current+gain));}}rt.put("spell_slots_remaining",n).put("spell_slots_max",copy(max));}return rt;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}
    private static JSONObject requireSidekick(JSONObject all,String id){JSONObject s=all==null?null:all.optJSONObject(id);if(s==null)throw new IllegalArgumentException("unknown sidekick");return copy(s);}
    private static JSONObject requireClass(JSONObject all,String id,String cls){JSONObject s=requireSidekick(all,id);if(!cls.equals(canonicalSidekickClass(s.optString("class"))))throw new IllegalArgumentException(cls+" sidekick required");return s;}
    private static JSONArray setArrayInt(JSONArray a,int idx,int value){JSONArray n=new JSONArray();for(int i=0;i<a.length();i++)n.put(i==idx?value:a.optInt(i));return n;}

    // ---- Monster research / negotiation ----
    public static JSONObject monsterResearch(String monsterType,double challengeRating,String skill,int checkTotal) {/*BR_JSON_WRAP_106*/try{
        String t=canonicalMonsterType(monsterType);if(t.isEmpty())throw new IllegalArgumentException("unsupported monster type");JSONArray skills=skillsForMonsterType(t);boolean skillAllowed=false;for(int i=0;i<skills.length();i++)if(norm(skill).equals(norm(skills.optString(i))))skillAllowed=true;double dc=10d+Math.max(0d,challengeRating);boolean success=skillAllowed&&checkTotal>=dc;
        return new JSONObject().put("type",t).put("suggested_skills",skills).put("skill_allowed",skillAllowed).put("dc",dc).put("check_total",checkTotal).put("success",success).put("reveals_likely_desire",success).put("source_page",146);
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject monsterNegotiation(String monsterType,int desireRoll,boolean offerMatchesDesire) {/*BR_JSON_WRAP_106*/try{
        String t=canonicalMonsterType(monsterType);if(t.isEmpty())throw new IllegalArgumentException("unsupported monster type");int r=Math.max(1,Math.min(4,desireRoll));String desire=DESIRES[indexOf(MONSTER_TYPES,t)][r-1];return new JSONObject().put("type",t).put("desire_roll",r).put("desire",desire).put("offer_matches_desire",offerMatchesDesire).put("social_check_advantage",offerMatchesDesire).put("resolve_with_social_interaction_rules",true).put("source_page",146);
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONArray skillsForMonsterType(String monsterType) {
        String t=canonicalMonsterType(monsterType);JSONArray a=new JSONArray();int x=indexOf(MONSTER_TYPES,t);if(x<0)return a;for(String s:RESEARCH_SKILLS[x])a.put(s);return a;
    }

    // ---- Natural hazard: avalanche ----
    public static JSONObject startAvalanche(String id,int day) {/*BR_JSON_WRAP_106*/try{
        return new JSONObject().put("id",clean(id,"avalanche")).put("kind","avalanche").put("active",true)
                .put("width_m",90).put("length_m",45).put("depth_m",9).put("movement_per_tick_m",90)
                .put("initiative_counts",new JSONArray().put(10).put(0)).put("strength_save_dc",15).put("damage","1d10 bludgeoning")
                .put("distance_travelled_m",0).put("victims",new JSONObject()).put("started_day",Math.max(1,day)).put("source_page",161);
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject avalancheImpact(JSONObject hazard,String victim,int strengthSaveTotal,int rolledDamage) {/*BR_JSON_WRAP_106*/try{
        JSONObject h=copy(hazard);if(!"avalanche".equals(h.optString("kind"))||!h.optBoolean("active",false))throw new IllegalArgumentException("active avalanche required");String v=clean(victim,"");if(v.isEmpty())throw new IllegalArgumentException("victim required");boolean save=strengthSaveTotal>=15;int damage=Math.max(0,rolledDamage);if(save)damage/=2;JSONObject victims=h.optJSONObject("victims");if(victims==null)victims=new JSONObject();JSONObject x=victims.optJSONObject(v);if(x==null)x=new JSONObject();x.put("moved_with_avalanche",true).put("prone",true).put("last_save_total",strengthSaveTotal).put("last_damage",damage).put("buried",false).put("escape_failures",0).put("exhaustion_levels_from_burial",0);victims.put(v,x);h.put("victims",victims).put("distance_travelled_m",h.optInt("distance_travelled_m",0)+90);return h;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject stopAvalanche(JSONObject hazard) {/*BR_JSON_WRAP_106*/try{
        JSONObject h=copy(hazard);JSONObject victims=h.optJSONObject("victims");if(victims==null)victims=new JSONObject();java.util.Iterator<String>it=victims.keys();while(it.hasNext()){String k=it.next();JSONObject v=victims.optJSONObject(k);if(v!=null&&v.optBoolean("moved_with_avalanche",false))v.put("buried",true).put("blinded",true).put("restrained",true).put("total_cover",true).put("buried_minutes",0);}h.put("victims",victims).put("active",false).put("stopped",true);return h;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject avalancheBurialMinutes(JSONObject hazard,String victim,int minutes) {/*BR_JSON_WRAP_106*/try{
        JSONObject h=copy(hazard);JSONObject victims=h.optJSONObject("victims");JSONObject v=victims==null?null:victims.optJSONObject(victim);if(v==null||!v.optBoolean("buried",false))throw new IllegalArgumentException("victim not buried");int total=Math.max(0,v.optInt("buried_minutes",0)+Math.max(0,minutes));v.put("buried_minutes",total).put("exhaustion_levels_from_burial",total/5);return h;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject avalancheEscape(JSONObject hazard,String victim,int athleticsTotal) {/*BR_JSON_WRAP_106*/try{
        JSONObject h=copy(hazard);JSONObject victims=h.optJSONObject("victims");JSONObject v=victims==null?null:victims.optJSONObject(victim);if(v==null||!v.optBoolean("buried",false))throw new IllegalArgumentException("victim not buried");int fails=v.optInt("escape_failures",0);if(fails>=3)throw new IllegalArgumentException("self escape no longer allowed after three failed attempts");if(athleticsTotal>=15){v.put("buried",false).put("blinded",false).put("restrained",false).put("total_cover",false).put("escaped",true);}else v.put("escape_failures",fails+1);return h;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject avalancheRescue(JSONObject hazard,String victim,boolean rescuerFree,int minutesSpent) {/*BR_JSON_WRAP_106*/try{
        JSONObject h=copy(hazard);JSONObject victims=h.optJSONObject("victims");JSONObject v=victims==null?null:victims.optJSONObject(victim);if(v==null||!v.optBoolean("buried",false))throw new IllegalArgumentException("victim not buried");if(!rescuerFree||minutesSpent<1)throw new IllegalArgumentException("rescuer needs one minute and must not be restrained/incapacitated");v.put("buried",false).put("blinded",false).put("restrained",false).put("total_cover",false).put("rescued",true);return h;
    }catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    // ---- Additional natural hazards (Tasha p.161) ----
    public static JSONObject fallIntoWater(int originalFallingDamage,int athleticsOrAcrobaticsTotal){/*BR_JSON_WRAP_106*/try{int dmg=Math.max(0,originalFallingDamage);boolean success=athleticsOrAcrobaticsTotal>=15;return new JSONObject().put("reaction_used",true).put("check","Strength (Athletics) or Dexterity (Acrobatics)").put("dc",15).put("check_total",athleticsOrAcrobaticsTotal).put("success",success).put("original_falling_damage",dmg).put("damage_applied",success?dmg/2:dmg).put("source_page",161);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject fallOntoCreature(int originalFallingDamage,int targetDexSaveTotal,String fallingSize,String targetSize){/*BR_JSON_WRAP_106*/try{int dmg=Math.max(0,originalFallingDamage),f=sizeRank(fallingSize),t=sizeRank(targetSize);boolean ruleApplies=f>0&&t>0;if(!ruleApplies)return new JSONObject().put("rule_applies",false).put("reason","one_or_both_creatures_tiny").put("falling_creature_damage",dmg).put("target_damage",0).put("source_page",161);boolean targetSaved=targetDexSaveTotal>=15;if(targetSaved)return new JSONObject().put("rule_applies",true).put("target_save_dc",15).put("target_save_total",targetDexSaveTotal).put("target_saved",true).put("falling_creature_damage",dmg).put("target_damage",0).put("target_prone",false).put("source_page",161);int each=dmg/2;boolean prone=t<f+2;return new JSONObject().put("rule_applies",true).put("target_save_dc",15).put("target_save_total",targetDexSaveTotal).put("target_saved",false).put("falling_creature_damage",each).put("target_damage",each).put("target_prone",prone).put("damage_split_equally",true).put("source_page",161);}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONArray naturalHazardSpellApproximations(){/*BR_JSON_WRAP_106*/try{String[][] rows={{"ball_lightning","Chromatic Orb"},{"blizzard","Cone of Cold|Ice Storm|Sleet Storm"},{"earthquake","Earthquake"},{"falling_debris","Conjure Barrage|Conjure Volley"},{"flood","Control Water|Tsunami"},{"fog","Fog Cloud"},{"lava_bomb","Fireball|Produce Flame"},{"lightning","Call Lightning|Lightning Bolt"},{"meteor","Fireball|Meteor Swarm"},{"mirage","Hallucinatory Terrain"},{"pyroclastic_flow","Incendiary Cloud"},{"radiation","Blight|Circle of Death"},{"smoke","Fog Cloud"},{"st_elmos_fire","Faerie Fire"},{"swamp_gas","Dancing Lights"},{"tidal_wave","Tsunami"},{"toxic_eruption","Acid Splash"},{"toxic_gas","Cloudkill|Stinking Cloud"},{"thunder","Thunderwave"},{"volcanic_lightning","Storm of Vengeance"},{"whirlpool","Control Water"},{"wildfire","Fire Storm|Wall of Fire"},{"windstorm","Gust of Wind"}};JSONArray a=new JSONArray();for(String[]r:rows)a.put(new JSONObject().put("hazard",r[0]).put("approximate_spells",r[1]).put("source_page",161));return a;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject createSpellEquivalentNaturalHazard(String id,String hazard,String approximateSpell,String location,int day){/*BR_JSON_WRAP_106*/try{String h=norm(hazard).replace(' ','_'),spell=clean(approximateSpell,"");boolean allowed=false;JSONArray rows=naturalHazardSpellApproximations();for(int i=0;i<rows.length();i++){JSONObject r=rows.optJSONObject(i);if(r!=null&&norm(r.optString("hazard")).replace(' ','_').equals(h)){String[] opts=r.optString("approximate_spells").split("\\|");for(String x:opts)if(norm(x).equals(norm(spell)))allowed=true;}}if(!allowed)throw new IllegalArgumentException("hazard/spell approximation not authorized by Tasha table");return new JSONObject().put("id",clean(id,h)).put("kind","spell_equivalent_natural_hazard").put("hazard",h).put("approximate_spell",spell).put("location",location==null?"":location.trim()).put("active",true).put("pending_barbara_review",true).put("created_day",Math.max(1,day)).put("source_page",161).put("resolution_policy","apply referenced spell effect as environmental approximation with GM/Barbara review");}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    public static JSONObject resolveSpellEquivalentNaturalHazard(JSONObject hazard,String resolution,int day){/*BR_JSON_WRAP_106*/try{JSONObject h=copy(hazard);if(!"spell_equivalent_natural_hazard".equals(h.optString("kind")))throw new IllegalArgumentException("spell-equivalent natural hazard required");h.put("active",false).put("pending_barbara_review",false).put("resolution",resolution==null?"":resolution.trim()).put("resolved_day",Math.max(1,day));return h;}catch(RuntimeException __brJsonRuntime106){throw __brJsonRuntime106;}catch(Exception __brJsonChecked106){throw new IllegalStateException(__brJsonChecked106);}}

    // ---- helpers ----
    private static String canonicalPatron(String value){String n=norm(value);for(int i=0;i<PATRON_TYPES.length;i++)if(n.equals(norm(PATRON_TYPES[i]))||n.equals(norm(PATRON_LABELS_PT[i])))return PATRON_TYPES[i];return "";}
    private static String patronLabel(String id){int i=indexOf(PATRON_TYPES,id);return i<0?id:PATRON_LABELS_PT[i];}
    private static String canonicalSidekickClass(String value){String n=norm(value);if(n.equals("expert")||n.equals("especialista"))return "expert";if(n.equals("spellcaster")||n.equals("conjurador"))return "spellcaster";if(n.equals("warrior")||n.equals("combatente"))return "warrior";return "";}
    private static String canonicalSpellcasterRole(String value){String n=norm(value);if(n.equals("mage")||n.equals("mago"))return "mage";if(n.equals("healer")||n.equals("curandeiro"))return "healer";if(n.equals("prodigy")||n.equals("prodigio"))return "prodigy";return "";}
    private static String canonicalWarriorRole(String value){String n=norm(value);if(n.equals("attacker")||n.equals("atacante"))return "attacker";if(n.equals("defender")||n.equals("defensor"))return "defender";return "";}
    private static String canonicalMonsterType(String value){String n=norm(value);String[][] aliases={{"aberration","aberracao"},{"beast","fera"},{"celestial","celestial"},{"construct","constructo","construto"},{"dragon","dragao"},{"elemental","elemental"},{"fey","fada"},{"fiend","demonio","corruptor"},{"giant","gigante"},{"humanoid","humanoide"},{"monstrosity","monstruosidade"},{"ooze","cubo gelatinoso","limo"},{"plant","planta"},{"undead","morto vivo","morto-vivo"}};for(String[] a:aliases)for(String x:a)if(n.equals(norm(x)))return a[0];return "";}
    private static int sizeRank(String size){String n=norm(size);if(n.equals("tiny")||n.equals("miudo")||n.equals("minusculo"))return 0;if(n.equals("small")||n.equals("pequeno"))return 1;if(n.equals("medium")||n.equals("medio"))return 2;if(n.equals("large")||n.equals("grande"))return 3;if(n.equals("huge")||n.equals("enorme"))return 4;if(n.equals("gargantuan")||n.equals("colossal"))return 5;throw new IllegalArgumentException("unknown creature size");}
    private static int indexOf(String[] a,String value){for(int i=0;i<a.length;i++)if(a[i].equals(value))return i;return -1;}
    private static String clean(String s,String d){String x=s==null?"":s.trim();return x.isEmpty()?d:x;}
    private static String norm(String s){String n=Normalizer.normalize(s==null?"":s.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}+","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
    private static JSONObject copy(JSONObject o){JSONObject n=new JSONObject();if(o==null)return n;try{java.util.Iterator<String>it=o.keys();while(it.hasNext()){String k=it.next();Object v=o.opt(k);if(v instanceof JSONObject)v=copy((JSONObject)v);else if(v instanceof JSONArray)v=copy((JSONArray)v);n.put(k,v);}}catch(Exception ignored){}return n;}
    private static JSONArray copy(JSONArray a){JSONArray n=new JSONArray();if(a==null)return n;try{for(int i=0;i<a.length();i++){Object v=a.opt(i);if(v instanceof JSONObject)v=copy((JSONObject)v);else if(v instanceof JSONArray)v=copy((JSONArray)v);n.put(v);}}catch(Exception ignored){}return n;}
}
