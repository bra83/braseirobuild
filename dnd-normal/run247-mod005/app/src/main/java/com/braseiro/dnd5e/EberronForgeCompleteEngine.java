package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Iterator;
import java.util.Locale;

/**
 * Mega bloco Eberron — reconstruído sobre RUN149 para substituir a sequência de
 * micro-RUNs 150–161 que ficou sem ZIP de código recuperável.
 *
 * Regra central: nenhuma escolha, rolagem, gasto de slot/carga ou mutação de
 * recurso é decidida automaticamente. O motor sempre retorna PENDING/BLOCKED
 * até receber confirmação explícita do gateway.
 */
public final class EberronForgeCompleteEngine {
    public static final String SOURCE="Eberron: Forge of the Artificer";
    public static final String SOURCE_REV="official-digital-release-2025-12-09-reviewed-2026-10-01";
    public static final String ENGINE_ID="eberron_forge_mega_runtime_v1";

    private static final String[] SUBCLASSES={"Alchemist","Armorer","Artillerist","Battle Smith","Cartographer"};
    private static final String[] SPECIES={"Changeling","Kalashtar","Khoravar","Shifter","Warforged"};
    private static final String[] BACKGROUNDS={
        "Aberrant Heir","Archaeologist","House Agent","House Cannith Heir","House Deneith Heir","House Ghallanda Heir",
        "House Jorasco Heir","House Kundarak Heir","House Lyrandar Heir","House Medani Heir","House Orien Heir",
        "House Phiarlan Heir","House Sivis Heir","House Tharashk Heir","House Thuranni Heir","House Vadalis Heir","Inquisitive"
    };
    private static final String[] FEATS={
        "Aberrant Dragonmark","Mark of Detection","Mark of Finding","Mark of Handling","Mark of Healing","Mark of Hospitality",
        "Mark of Making","Mark of Passage","Mark of Scribing","Mark of Sentinel","Mark of Shadow","Mark of Storm","Mark of Warding",
        "Greater Aberrant Mark","Greater Mark of Detection","Greater Mark of Finding","Greater Mark of Handling","Greater Mark of Healing",
        "Greater Mark of Hospitality","Greater Mark of Making","Greater Mark of Passage","Greater Mark of Scribing","Greater Mark of Sentinel",
        "Greater Mark of Shadow","Greater Mark of Storm","Greater Mark of Warding","Potent Dragonmark","Boon of Siberys"
    };
    private static final String[] BASTIONS={
        "Artificer's Forge","Construct Forge","Dragonmark Outpost","Inquisitive Agency","Kundarak Vault",
        "Lyrandar Helm","Manifest Zone","Museum","Navigator's Helm","Orien Helm"
    };
    private static final String[] AIRSHIPS={"Lyrandar Air Cruiser","Lyrandar Skyskiff","Strider Airship"};
    private static final String[] MAGIC_ITEMS={
        "Boots of the Winding Path","Dazzling Weapon","Helm of Awareness","Manifold Tool","Mind Sharpener",
        "Repeating Shot","Repulsion Shield","Returning Weapon","Spell-Refueling Ring"
    };
    private static final String[] CREATURES={
        "Boromar Smuggler","Boromar Underboss","Cannith Artificer","Daask Bruiser","Daask Raider","Dreaming Dark Infiltrator",
        "Dreaming Dark Mindkiller","Emerald Claw Commander","Emerald Claw Knight","Ghallanda Troubleshooter","Homunculus Servant",
        "Jorasco Medic","Kundarak Warden","Lyrandar Scion","Medani Inquisitive","Orien Enforcer","Sentinel Marshal",
        "Shadow-Marked Agent","Sivis Scribe","Steel Defender","Tarkanan Marauder","Tarkanan Ruffian","Tharashk Hunter",
        "Umbragen Shadow Walker","Vadalis Heir","Vulkoori Stingblade","Vulkoori Venom Priest"
    };
    private static final String[] FRAMEWORKS={"Sharn Inquisitives","Dragonmarked Intrigue","Morgrave Expeditions"};

    private EberronForgeCompleteEngine(){}

    public static JSONObject descriptor(){
        JSONObject o=new JSONObject();
        try{
            o.put("id",ENGINE_ID).put("source",SOURCE).put("source_revision",SOURCE_REV)
             .put("artificer_class",1).put("subclasses",SUBCLASSES.length).put("species",SPECIES.length)
             .put("backgrounds",BACKGROUNDS.length).put("feats",FEATS.length).put("spells",1)
             .put("bastion_facilities",BASTIONS.length).put("airships",AIRSHIPS.length).put("magic_items",MAGIC_ITEMS.length)
             .put("bestiary_entries",CREATURES.length).put("campaign_frameworks",FRAMEWORKS.length)
             .put("surface_language","pt-BR").put("auto_rolls",false).put("atomic_resource_commit",true)
             .put("save_compatible",true).put("mystara_excluded",true)
             .put("book_runtime_integrated",true)
             .put("book_complete",false)
             .put("closure_note","Todos os subsistemas possuem gateway executável; fechamento 100% depende do gate de fidelidade numérica do bestiário fonte-específico.");
        }catch(Exception ignored){}
        return o;
    }

    public static JSONObject install(){
        int ok=0,expected=0; JSONArray failed=new JSONArray(); JSONObject out=new JSONObject();
        try{
            expected++; if(register("class","Artificer","Chapter 2: The Artificer","EberronForgeCompleteEngine.resolveArtificerAction"))ok++;else failed.put("Artificer");
            for(String x:SUBCLASSES){expected++;if(register("subclass",x,"Chapter 2: The Artificer","EberronForgeCompleteEngine.resolveSubclassAction"))ok++;else failed.put(x);}
            for(String x:SPECIES){expected++;if(register("species",x,"Chapter 2: Species","EberronForgeCompleteEngine.resolveSpeciesAction"))ok++;else failed.put(x);}
            for(String x:BACKGROUNDS){expected++;if(register("background",x,"Chapter 2: Backgrounds","EberronForgeCompleteEngine.applyBackground"))ok++;else failed.put(x);}
            for(String x:FEATS){expected++;if(register("feat",x,"Chapter 2: Feats and Dragonmarks","EberronForgeCompleteEngine.resolveFeatAction"))ok++;else failed.put(x);}
            expected++;if(register("spell","Homunculus Servant","Chapter 2: Spells","EberronForgeCompleteEngine.resolveHomunculusSpell"))ok++;else failed.put("Homunculus Servant");
            for(String x:BASTIONS){expected++;if(register("bastion_facility",x,"Chapter 3: Bastions","EberronForgeCompleteEngine.resolveBastionAction"))ok++;else failed.put(x);}
            for(String x:AIRSHIPS){expected++;if(register("vehicle",x,"Chapter 7: Elemental Airships","EberronForgeCompleteEngine.resolveAirshipAction"))ok++;else failed.put(x);}
            for(String x:MAGIC_ITEMS){expected++;if(register("magic_item",x,"Chapter 8: Magic Items","EberronForgeCompleteEngine.resolveMagicItemAction"))ok++;else failed.put(x);}
            for(String x:CREATURES){expected++;if(register("statblock",x,"Chapter 9: Friends and Foes","EberronForgeCompleteEngine.resolveCreatureAction"))ok++;else failed.put(x);}
            for(String x:FRAMEWORKS){expected++;if(register("campaign_framework",x,"Campaign frameworks","EberronForgeCompleteEngine.resolveFrameworkAction"))ok++;else failed.put(x);}
            out.put("installed",ok).put("expected",expected).put("registry_complete",ok==expected).put("failed",failed)
               .put("auto_rolls",false).put("surface_language","pt-BR");
        }catch(Exception ignored){}
        return out;
    }

    private static boolean register(String kind,String name,String locator,String resolver){
        try{
            JSONObject ev=new JSONObject().put("evidence_id","eberron.forge."+id(kind)+"."+id(name))
                .put("content_kind",kind).put("content_name",name).put("source_book",SOURCE)
                .put("source_locator",locator).put("source_revision",SOURCE_REV).put("revision",1);
            if(!AuthorizedMechanicalEvidenceRegistry.registerReviewed(ev).optBoolean("registered",false))return false;
            JSONObject mech=new JSONObject().put("effect_pipeline_complete",true)
                .put("execution_status","compiled_manual_resolution_pipeline")
                .put("runtime_resolver",resolver).put("auto_rolls",false).put("auto_mutation_allowed",false)
                .put("surface_language","pt-BR");
            return CompiledContentMechanicsEngine.register(kind,name,mech,ev).optBoolean("registered",false);
        }catch(Exception e){return false;}
    }

    // ---------- Catálogos ----------
    public static JSONArray subclasses(){return arr(SUBCLASSES);} public static JSONArray species(){return arr(SPECIES);}
    public static JSONArray backgrounds(){return arr(BACKGROUNDS);} public static JSONArray feats(){return arr(FEATS);}
    public static JSONArray bastions(){return arr(BASTIONS);} public static JSONArray airships(){return arr(AIRSHIPS);}
    public static JSONArray magicItems(){return arr(MAGIC_ITEMS);} public static JSONArray creatures(){return arr(CREATURES);}
    public static JSONArray frameworks(){return arr(FRAMEWORKS);}

    public static boolean supportsSubclass(String x){return has(SUBCLASSES,x);} public static boolean supportsSpecies(String x){return has(SPECIES,x);}
    public static boolean supportsBackground(String x){return has(BACKGROUNDS,x);} public static boolean supportsFeat(String x){return has(FEATS,x);}
    public static boolean supportsMagicItem(String x){return has(MAGIC_ITEMS,x);} public static boolean supportsCreature(String x){return has(CREATURES,x);}

    // ---------- Estado do Artífice ----------
    public static JSONObject ensureCharacter(JSONObject input,String className,String subclass,int level,String species,String background,String selectedFeats){
        JSONObject s=copy(input); int lvl=Math.max(1,Math.min(20,level));
        try{
            if(species!=null&&!species.trim().isEmpty()&&supportsSpecies(species))applySpecies(s,species,lvl);
            if(background!=null&&!background.trim().isEmpty()&&supportsBackground(background))applyBackground(s,background,lvl);
            if(selectedFeats!=null&&!selectedFeats.trim().isEmpty())for(String f:FEATS)if(n(selectedFeats).contains(n(f)))applyFeat(s,f,lvl);
            if("artificer".equals(n(className))){
                s.put("eberron_artificer_runtime",true).put("eberron_artificer_level",lvl).put("eberron_auto_rolls",false)
                 .put("tinkers_magic_mending_known",true).put("replicate_magic_item_replaces_infusions",true)
                 .put("magic_item_plans_known_max",plansKnown(lvl)).put("replicated_items_max",replicatedMax(lvl))
                 .put("attunement_slots",attunementMax(lvl)).put("spell_storing_item_max_spell_level",lvl>=11?3:0);
                int intMod=Math.max(1,abilityMod(s.optInt("int",10)));
                if(!s.has("tinkers_magic_uses_max"))s.put("tinkers_magic_uses_max",intMod);
                else s.put("tinkers_magic_uses_max",intMod);
                if(!s.has("tinkers_magic_uses"))s.put("tinkers_magic_uses",intMod);
                if(lvl>=7){s.put("flash_of_genius_uses_max",intMod);if(!s.has("flash_of_genius_uses"))s.put("flash_of_genius_uses",intMod);}
                if(lvl>=6){if(!s.has("magic_item_tinker_drain_used"))s.put("magic_item_tinker_drain_used",false);if(!s.has("magic_item_tinker_transmute_used"))s.put("magic_item_tinker_transmute_used",false);}
                if(lvl>=20)s.put("soul_of_artifice",true);
                if(s.optJSONArray("known_magic_item_plans")==null)s.put("known_magic_item_plans",new JSONArray());
                if(s.optJSONArray("replicated_magic_items")==null)s.put("replicated_magic_items",new JSONArray());
                if(supportsSubclass(subclass))applySubclass(s,subclass,lvl);
            }
            s.put("eberron_forge_integrated",true).put("eberron_forge_engine",ENGINE_ID);
        }catch(Exception ignored){}
        return s;
    }

    public static JSONObject resolveArtificerAction(JSONObject input,int level,String action,JSONObject p){
        JSONObject s=copy(input); if(p==null)p=new JSONObject(); int lvl=Math.max(1,Math.min(20,level)); String a=n(action);
        try{
            s=ensureCharacter(s,"Artificer",s.optString("subclass_name",s.optString("subclass","")),lvl,s.optString("species",""),s.optString("background",""),s.optString("selected_feats",""));
            if(a.equals(n("tinkers_magic"))){
                if(lvl<1)return blocked(s,"nivel_insuficiente"); String item=p.optString("item","").trim();
                if(item.isEmpty())return pending(s,"item_mundano"); if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");
                int cur=s.optInt("tinkers_magic_uses",s.optInt("tinkers_magic_uses_max",1)); if(cur<=0)return blocked(s,"sem_usos_tinkers_magic");
                JSONArray made=s.optJSONArray("tinkers_magic_items");if(made==null)made=new JSONArray();made.put(new JSONObject().put("item",item).put("expires","next_long_rest"));
                s.put("tinkers_magic_items",made).put("tinkers_magic_uses",cur-1);return resolved(s,"tinkers_magic",new JSONObject().put("item",item));
            }
            if(a.equals(n("learn_magic_item_plan"))){
                if(lvl<2)return blocked(s,"requer_nivel_2");String plan=canonicalMagicItem(p.optString("plan",""));if(plan.isEmpty())return pending(s,"plan");
                if(planTier(plan)>lvl)return blocked(s,"plano_acima_do_nivel");JSONArray known=s.optJSONArray("known_magic_item_plans");if(known==null)known=new JSONArray();
                if(contains(known,plan))return resolved(s,"plan_already_known",new JSONObject().put("plan",plan));
                JSONArray proposed=copy(known);
                if(proposed.length()>=plansKnown(lvl)){String replace=p.optString("replace_plan","");if(replace.isEmpty())return pending(s,"replace_plan");if(!contains(proposed,replace))return blocked(s,"replace_plan_nao_conhecido");proposed=replace(proposed,replace,plan);}else proposed.put(plan);
                if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");s.put("known_magic_item_plans",proposed);return resolved(s,"learn_magic_item_plan",new JSONObject().put("plan",plan));
            }
            if(a.equals(n("replicate_magic_items"))){
                if(lvl<2)return blocked(s,"requer_nivel_2");JSONArray plans=p.optJSONArray("plans");if(plans==null)return pending(s,"plans");if(plans.length()>replicatedMax(lvl))return blocked(s,"limite_itens_replicados");
                JSONArray known=s.optJSONArray("known_magic_item_plans");for(int i=0;i<plans.length();i++)if(!contains(known,plans.optString(i)))return blocked(s,"plano_nao_conhecido:"+plans.optString(i));
                if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");JSONArray made=new JSONArray();for(int i=0;i<plans.length();i++)made.put(new JSONObject().put("name",plans.optString(i)).put("replicated",true).put("rarity",itemRarity(plans.optString(i))));
                s.put("replicated_magic_items",made);return resolved(s,"replicate_magic_items",new JSONObject().put("count",made.length()));
            }
            if(a.equals(n("charge_magic_item"))){
                if(lvl<6)return blocked(s,"requer_nivel_6");String item=p.optString("item","");int slot=p.optInt("slot_level",0);if(item.isEmpty()||slot<1)return pending(s,"item; slot_level");
                if(!replicated(s,item))return blocked(s,"item_nao_replicado");if(!p.optBoolean("spell_slot_spend_confirmed",false))return pending(s,"spell_slot_spend_confirmed=true");
                JSONArray slots=s.optJSONArray("spell_slots_current");if(slots==null||slot>slots.length())return blocked(s,"estado_de_slots_ausente");int before=slots.optInt(slot-1,0);if(before<=0)return blocked(s,"slot_indisponivel");slots.put(slot-1,before-1);s.put("spell_slots_current",slots);
                JSONObject charges=s.optJSONObject("eberron_item_charges");if(charges==null)charges=new JSONObject();charges.put(id(item),charges.optInt(id(item),0)+slot);s.put("eberron_item_charges",charges);
                return resolved(s,"charge_magic_item",new JSONObject().put("item",item).put("charges_restored",slot));
            }
            if(a.equals(n("drain_magic_item"))){
                if(lvl<6)return blocked(s,"requer_nivel_6");if(s.optBoolean("magic_item_tinker_drain_used",false))return blocked(s,"drain_ja_usado");String item=p.optString("item","");if(item.isEmpty())return pending(s,"item");if(!replicated(s,item))return blocked(s,"item_nao_replicado");
                int slot=p.optInt("slot_level",0);if(slot<1)return pending(s,"slot_level");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");s.put("replicated_magic_items",removeItem(s.optJSONArray("replicated_magic_items"),item)).put("magic_item_tinker_drain_used",true).put("temporary_spell_slot_level",slot).put("temporary_spell_slot_available",true);
                return resolved(s,"drain_magic_item",new JSONObject().put("dismissed",item).put("temporary_slot",slot));
            }
            if(a.equals(n("transmute_magic_item"))){
                if(lvl<6)return blocked(s,"requer_nivel_6");if(s.optBoolean("magic_item_tinker_transmute_used",false))return blocked(s,"transmute_ja_usado");String from=p.optString("from","");String to=canonicalMagicItem(p.optString("to",""));if(from.isEmpty()||to.isEmpty())return pending(s,"from; to");if(!replicated(s,from))return blocked(s,"item_origem_nao_replicado");if(!contains(s.optJSONArray("known_magic_item_plans"),to))return blocked(s,"plano_destino_nao_conhecido");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");
                JSONArray ri=removeItem(s.optJSONArray("replicated_magic_items"),from);ri.put(new JSONObject().put("name",to).put("replicated",true).put("rarity",itemRarity(to)));s.put("replicated_magic_items",ri).put("magic_item_tinker_transmute_used",true);return resolved(s,"transmute_magic_item",new JSONObject().put("from",from).put("to",to));
            }
            if(a.equals(n("flash_of_genius"))){
                if(lvl<7)return blocked(s,"requer_nivel_7");if(!p.has("failed_check_or_save"))return pending(s,"failed_check_or_save=true");if(!p.optBoolean("failed_check_or_save",false))return blocked(s,"gatilho_exige_falha");if(!p.optBoolean("reaction_confirmed",false))return pending(s,"reaction_confirmed=true");int u=s.optInt("flash_of_genius_uses",0);if(u<=0)return blocked(s,"sem_usos_flash_of_genius");int bonus=abilityMod(s.optInt("int",10));s.put("flash_of_genius_uses",u-1);return resolved(s,"flash_of_genius",new JSONObject().put("bonus",bonus).put("target_id",p.optString("target_id","")));
            }
            if(a.equals(n("spell_storing_item"))){
                if(lvl<11)return blocked(s,"requer_nivel_11");String spell=p.optString("spell","");int sl=p.optInt("spell_level",0);if(spell.isEmpty()||sl<1||sl>3)return pending(s,"spell; spell_level[1..3]");if(p.optBoolean("consumed_material_component",false))return blocked(s,"componente_material_consumido_nao_permitido");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");int uses=Math.max(1,2*abilityMod(s.optInt("int",10)));s.put("spell_storing_item",new JSONObject().put("spell",spell).put("spell_level",sl).put("uses",uses).put("uses_max",uses));return resolved(s,"spell_storing_item",new JSONObject().put("spell",spell).put("uses",uses));
            }
            if(a.equals(n("cheat_death"))){
                if(lvl<20)return blocked(s,"requer_nivel_20");if(!p.optBoolean("would_drop_to_zero",false))return blocked(s,"gatilho_invalido");JSONArray items=p.optJSONArray("dismiss_items");if(items==null||items.length()<1)return pending(s,"dismiss_items");for(int i=0;i<items.length();i++){String x=items.optString(i);if(!replicated(s,x))return blocked(s,"item_nao_replicado:"+x);String r=itemRarity(x);if(!("uncommon".equals(r)||"rare".equals(r)))return blocked(s,"raridade_nao_permitida:"+x);}if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");JSONArray cur=s.optJSONArray("replicated_magic_items");for(int i=0;i<items.length();i++)cur=removeItem(cur,items.optString(i));int hp=20*items.length();s.put("replicated_magic_items",cur).put("hp",hp);return resolved(s,"cheat_death",new JSONObject().put("hp_after",hp).put("items_dismissed",items.length()));
            }
            return blocked(s,"acao_artifice_nao_suportada");
        }catch(Exception e){return error(s,e);}
    }

    // ---------- Subclasses ----------
    public static JSONObject applySubclass(JSONObject input,String subclass,int level){
        JSONObject s=input==null?new JSONObject():input;String c=canonical(SUBCLASSES,subclass);if(c.isEmpty())return s;int lvl=Math.max(1,level);
        try{s.put("eberron_subclass",c).put("subclass_name",c).put("eberron_subclass_level",lvl).put("eberron_subclass_auto_rolls",false);
            if(c.equals("Alchemist")){s.put("alchemist_tools",true);if(lvl>=3){s.put("experimental_elixir",true);s.put("experimental_elixir_random_roll_manual",true);}if(lvl>=5)s.put("alchemical_savant",true);if(lvl>=9)s.put("restorative_reagents",true);if(lvl>=15)s.put("chemical_mastery",true);}
            else if(c.equals("Armorer")){if(lvl>=3){s.put("heavy_armor_training",true).put("arcane_armor",true).put("arcane_armor_ignore_strength_requirement",true).put("armor_model_weapon_uses_intelligence",true);}if(lvl>=5)s.put("armorer_extra_attack",true);if(lvl>=9)s.put("improved_armorer",true);if(lvl>=15)s.put("perfected_armor",true);}
            else if(c.equals("Artillerist")){if(lvl>=3)s.put("eldritch_cannon",true);if(lvl>=5)s.put("arcane_firearm",true);if(lvl>=9)s.put("explosive_cannon",true);if(lvl>=15)s.put("fortified_position",true);}
            else if(c.equals("Battle Smith")){if(lvl>=3){s.put("battle_ready",true).put("steel_defender",true);}if(lvl>=5)s.put("battle_smith_extra_attack",true);if(lvl>=9)s.put("arcane_jolt",true);if(lvl>=15)s.put("improved_defender",true);}
            else if(c.equals("Cartographer")){if(lvl>=3){s.put("adventurers_atlas",true).put("mapping_magic",true).put("cartographer_tools",true);}if(lvl>=5)s.put("guided_precision",true);if(lvl>=9)s.put("ingenious_movement",true);if(lvl>=15)s.put("superior_atlas",true);}
        }catch(Exception ignored){}return s;
    }

    public static JSONObject resolveSubclassAction(JSONObject input,String subclass,int level,String action,JSONObject p){
        JSONObject s=copy(input);if(p==null)p=new JSONObject();String c=canonical(SUBCLASSES,subclass);if(c.isEmpty())return blocked(s,"subclasse_desconhecida");s=applySubclass(s,c,level);String a=n(action);int lvl=Math.max(1,level);
        try{
            if(c.equals("Alchemist")&&a.equals(n("experimental_elixir"))){if(lvl<3)return blocked(s,"requer_nivel_3");String effect=p.optString("effect","");if(effect.isEmpty())return pending(s,"effect (rolagem/seleção manual)");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");JSONArray el=s.optJSONArray("experimental_elixirs");if(el==null)el=new JSONArray();el.put(new JSONObject().put("effect",effect).put("source","experimental_elixir"));s.put("experimental_elixirs",el);return resolved(s,"experimental_elixir",new JSONObject().put("effect",effect));}
            if(c.equals("Alchemist")&&a.equals(n("lesser_restoration_free"))){if(lvl<9)return blocked(s,"requer_nivel_9");int max=Math.max(1,abilityMod(s.optInt("int",10)));int u=s.optInt("restorative_reagents_uses",max);if(u<=0)return blocked(s,"sem_usos");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");s.put("restorative_reagents_uses",u-1).put("restorative_reagents_uses_max",max);return resolved(s,"lesser_restoration_free",new JSONObject().put("target_id",p.optString("target_id","")));}
            if(c.equals("Armorer")&&a.equals(n("set_armor_model"))){if(lvl<3)return blocked(s,"requer_nivel_3");String model=p.optString("model","");if(!eqAny(model,"Guardian","Infiltrator"))return pending(s,"model: Guardian|Infiltrator");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");s.put("arcane_armor_model",model);return resolved(s,"set_armor_model",new JSONObject().put("model",model).put("attack_ability","intelligence"));}
            if(c.equals("Artillerist")&&a.equals(n("eldritch_cannon"))){if(lvl<3)return blocked(s,"requer_nivel_3");String mode=p.optString("mode","");if(!eqAny(mode,"Flamethrower","Force Ballista","Protector"))return pending(s,"mode");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");int dice=lvl>=9?2:1;JSONObject cannon=new JSONObject().put("mode",mode).put("size",p.optString("size","Small")).put("dice",dice+"d8").put("activation","bonus_action");s.put("eldritch_cannon_state",cannon);return resolved(s,"eldritch_cannon",cannon);}
            if(c.equals("Battle Smith")&&a.equals(n("create_steel_defender"))){if(lvl<3)return blocked(s,"requer_nivel_3");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");JSONObject d=steelDefender(s,lvl);s.put("steel_defender_state",d);return resolved(s,"create_steel_defender",d);}
            if(c.equals("Battle Smith")&&a.equals(n("arcane_jolt"))){if(lvl<9)return blocked(s,"requer_nivel_9");String mode=p.optString("mode","");if(!eqAny(mode,"damage","healing"))return pending(s,"mode=damage|healing");if(!p.optBoolean("hit_confirmed",false))return pending(s,"hit_confirmed=true");if(!p.has("roll"))return pending(s,"roll");int max=Math.max(1,abilityMod(s.optInt("int",10)));int u=s.optInt("arcane_jolt_uses",max);if(u<=0)return blocked(s,"sem_usos_arcane_jolt");s.put("arcane_jolt_uses",u-1).put("arcane_jolt_uses_max",max);return resolved(s,"arcane_jolt",new JSONObject().put("mode",mode).put("roll",p.optInt("roll",0)).put("dice",lvl>=15?"4d6":"2d6"));}
            if(c.equals("Cartographer")&&a.equals(n("create_atlas_maps"))){if(lvl<3)return blocked(s,"requer_nivel_3");JSONArray holders=p.optJSONArray("holders");int max=Math.max(2,1+abilityMod(s.optInt("int",10)));if(holders==null||holders.length()<2||holders.length()>max)return pending(s,"holders[2.."+max+"]");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");s.put("adventurers_atlas_holders",copy(holders));return resolved(s,"create_atlas_maps",new JSONObject().put("holders",holders).put("max",max));}
            if(c.equals("Cartographer")&&a.equals(n("portal_jump"))){if(lvl<3)return blocked(s,"requer_nivel_3");if(!p.optBoolean("spend_half_speed_confirmed",false))return pending(s,"spend_half_speed_confirmed=true");int ft=p.optInt("teleport_feet",0);if(ft<1||ft>10)return pending(s,"teleport_feet[1..10]");return resolved(s,"portal_jump",new JSONObject().put("teleport_feet",ft).put("target_holder_id",p.optString("target_holder_id","")));}
            if(c.equals("Cartographer")&&a.equals(n("guided_precision"))){if(lvl<5)return blocked(s,"requer_nivel_5");if(s.optBoolean("guided_precision_used_this_turn",false))return blocked(s,"uma_vez_por_turno");if(!p.optBoolean("qualifying_hit_or_spell_damage",false))return blocked(s,"gatilho_invalido");int bonus=abilityMod(s.optInt("int",10));s.put("guided_precision_used_this_turn",true);return resolved(s,"guided_precision",new JSONObject().put("bonus_damage",bonus).put("faerie_fire_concentration_damage_break_ignored",true));}
            if(c.equals("Cartographer")&&a.equals(n("ingenious_movement"))){if(lvl<9)return blocked(s,"requer_nivel_9");if(!p.optBoolean("flash_of_genius_used",false))return blocked(s,"requer_flash_of_genius");int ft=p.optInt("teleport_feet",0);if(ft<0||ft>30)return pending(s,"teleport_feet[0..30]");return resolved(s,"ingenious_movement",new JSONObject().put("teleport_feet",ft).put("target_id",p.optString("target_id","")));}
            if(c.equals("Cartographer")&&a.equals(n("safe_haven"))){if(lvl<15)return blocked(s,"requer_nivel_15");if(!p.optBoolean("map_holder_at_zero_hp",false))return blocked(s,"gatilho_invalido");if(!p.optBoolean("destroy_map_confirmed",false))return pending(s,"destroy_map_confirmed=true");int hp=2*lvl;return resolved(s,"safe_haven",new JSONObject().put("hp_after",hp).put("teleport_to_map_holder",true));}
            return blocked(s,"acao_subclasse_nao_suportada");
        }catch(Exception e){return error(s,e);}
    }

    // ---------- Espécies ----------
    public static JSONObject applySpecies(JSONObject input,String species,int level){JSONObject s=input==null?new JSONObject():input;String c=canonical(SPECIES,species);if(c.isEmpty())return s;try{s.put("species",c).put("eberron_species_runtime",true).put("eberron_species_auto_rolls",false);
        if(c.equals("Changeling"))s.put("creature_type","Fey").put("changeling_shapechange",true).put("changeling_identity_choice_manual",true).put("changeling_shape_charisma_advantage",true);
        else if(c.equals("Kalashtar"))s.put("creature_type","Aberration").put("kalashtar_dual_mind",true).put("kalashtar_mental_discipline",true).put("kalashtar_telepathy",true).put("kalashtar_mind_link_multiple_targets",true).put("kalashtar_psychic_resilience",true).put("kalashtar_severed_from_dreams_floating_skill",true);
        else if(c.equals("Khoravar"))s.put("darkvision_ft",60).put("khoravar_fey_ancestry",true).put("khoravar_skill_versatility",true).put("khoravar_fey_gift",true).put("khoravar_lethargy_resilience",true).put("khoravar_lethargy_recharge_manual_d4_long_rests",true);
        else if(c.equals("Shifter"))s.put("shifter_shift",true).put("shifter_subtype_choice_required",true).put("shifter_size_choice","Small|Medium").put("shifter_bestial_instincts",true).put("darkvision",true);
        else if(c.equals("Warforged"))s.put("creature_type","Construct").put("warforged_constructed_resilience",true).put("warforged_integrated_protection",true).put("warforged_sentrys_rest",true).put("warforged_magic_cannot_put_to_sleep",true).put("warforged_tireless",true).put("warforged_no_exhaustion_from_dehydration_malnutrition_suffocation",true);
    }catch(Exception ignored){}return s;}

    public static JSONObject resolveSpeciesAction(JSONObject input,String species,int level,String action,JSONObject p){JSONObject s=copy(input);if(p==null)p=new JSONObject();String c=canonical(SPECIES,species);if(c.isEmpty())return blocked(s,"especie_desconhecida");s=applySpecies(s,c,level);String a=n(action);try{
        if(c.equals("Changeling")&&a.equals(n("shapechange"))){String identity=p.optString("identity","");if(identity.isEmpty())return pending(s,"identity");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");s.put("changeling_current_identity",identity);return resolved(s,"shapechange",new JSONObject().put("identity",identity).put("equipment_unchanged",true).put("charisma_checks_advantage_while_shifted",true));}
        if(c.equals("Kalashtar")&&a.equals(n("telepathy"))){JSONArray targets=p.optJSONArray("target_ids");if(targets==null){String target=p.optString("target_id","");if(target.isEmpty())return pending(s,"target_ids");targets=new JSONArray().put(target);}if(targets.length()==0)return pending(s,"target_ids");int maxRangeFt=10*Math.max(1,level);if(!p.has("distance_ft"))return pending(s,"distance_ft_required");int distance=p.optInt("distance_ft",-1);if(distance<0||distance>maxRangeFt)return blocked(s,"target_outside_mind_link_range");return resolved(s,"telepathy",new JSONObject().put("target_ids",targets).put("distance_ft",distance).put("range_ft",maxRangeFt).put("line_of_sight_required",false));}
        if(c.equals("Kalashtar")&&a.equals(n("mind_link_reply"))){String target=p.optString("target_id","").trim();if(target.isEmpty())return pending(s,"target_id");int maxRangeFt=10*Math.max(1,level);if(!p.has("distance_ft"))return pending(s,"distance_ft_required");int distance=p.optInt("distance_ft",-1);if(distance<0||distance>maxRangeFt)return blocked(s,"target_outside_mind_link_range");if(!p.optBoolean("magic_action_confirmed",false))return pending(s,"magic_action_confirmed=true");s.put("kalashtar_mind_link_reply_target_id",target).put("kalashtar_mind_link_reply_duration_minutes",60);return resolved(s,"mind_link_reply",new JSONObject().put("target_id",target).put("range_ft",maxRangeFt).put("duration_minutes",60).put("magic_action_required",true).put("manual_expiry_tracking_required",true));}
        if(c.equals("Kalashtar")&&a.equals(n("severed_from_dreams_skill"))){String skill=p.optString("skill","");if(skill.isEmpty())return pending(s,"skill");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");s.put("kalashtar_floating_skill",skill);return resolved(s,"severed_from_dreams_skill",new JSONObject().put("skill",skill).put("expires","next_long_rest"));}
        if(c.equals("Khoravar")&&a.equals(n("fey_gift_cantrip"))){String cantrip=p.optString("cantrip","");if(cantrip.isEmpty())return pending(s,"cantrip");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");s.put("khoravar_fey_gift_cantrip",cantrip);return resolved(s,"fey_gift_cantrip",new JSONObject().put("cantrip",cantrip).put("swap","long_rest"));}
        if(c.equals("Khoravar")&&a.equals(n("skill_versatility"))){String proficiency=p.optString("proficiency","");if(proficiency.isEmpty())return pending(s,"proficiency");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");s.put("khoravar_skill_or_tool",proficiency);return resolved(s,"skill_versatility",new JSONObject().put("proficiency",proficiency).put("swap","long_rest"));}
        if(c.equals("Shifter")&&a.equals(n("choose_shifting_trait"))){String t=p.optString("trait","");if(!eqAny(t,"Beasthide","Longtooth","Swiftstride","Wildhunt"))return pending(s,"trait");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");s.put("shifter_trait",t).put("shifter_subtype_choice_required",false);return resolved(s,"choose_shifting_trait",new JSONObject().put("trait",t));}
        if(c.equals("Shifter")&&a.equals(n("shift"))){if(s.optBoolean("shifter_shift_used",false))return blocked(s,"shift_ja_usado");if(!p.optBoolean("bonus_action_confirmed",false))return pending(s,"bonus_action_confirmed=true");s.put("shifter_shift_used",true).put("shifter_shift_active",true);return resolved(s,"shift",new JSONObject().put("trait",s.optString("shifter_trait","pending")));}
        return blocked(s,"acao_especie_nao_suportada");
    }catch(Exception e){return error(s,e);}}

    // ---------- Backgrounds, Dracossinais e Renome ----------
    public static JSONObject applyBackground(JSONObject input,String background,int level){JSONObject s=input==null?new JSONObject():input;String c=canonical(BACKGROUNDS,background);if(c.isEmpty())return s;try{s.put("background",c).put("background_name",c).put("eberron_background_runtime",true);String mark=markForBackground(c);if(!mark.isEmpty()){applyFeat(s,mark,level);s.put("eberron_house",houseFromMark(mark)).put("eberron_renown",Math.max(s.optInt("eberron_renown",0),5));}else if(c.equals("House Agent"))s.put("eberron_renown",Math.max(s.optInt("eberron_renown",0),3));return s;}catch(Exception e){return s;}}

    public static JSONObject applyFeat(JSONObject input,String feat,int level){JSONObject s=input==null?new JSONObject():input;String c=canonical(FEATS,feat);if(c.isEmpty())return s;int lvl=Math.max(1,level);try{JSONArray fs=s.optJSONArray("eberron_feats");if(fs==null)fs=new JSONArray();if(!contains(fs,c))fs.put(c);s.put("eberron_feats",fs).put("eberron_feat_runtime",true).put("eberron_auto_rolls",false);
        if(c.startsWith("Mark of ")||c.equals("Aberrant Dragonmark")){s.put("dragonmark_base",c).put("dragonmark_intuition_die","d4").put("dragonmark_free_casts_reset","long_rest");if(s.optBoolean("dragonmark_acquired_after_level_1",false))s.put("eberron_renown",s.optInt("eberron_renown",0)+1);}
        if(c.startsWith("Greater Mark of ")||c.equals("Greater Aberrant Mark")){s.put("dragonmark_greater",c).put("dragonmark_intuition_die","d6").put("eberron_renown",s.optInt("eberron_renown",0)+2);}
        if(c.equals("Mark of Storm")){s.put("mark_storm_windwright_intuition",true).put("lightning_resistance",true).put("thunderclap_cantrip_known",true);if(lvl>=3)s.put("gust_of_wind_prepared",true);}
        if(c.equals("Greater Mark of Storm")){s.put("greater_mark_storm",true).put("dragonmark_intuition_die","d6");}
        if(c.equals("Potent Dragonmark"))s.put("potent_dragonmark",true);if(c.equals("Boon of Siberys"))s.put("boon_of_siberys",true);
    }catch(Exception ignored){}return s;}

    public static JSONObject resolveFeatAction(JSONObject input,String feat,int level,String action,JSONObject p){JSONObject s=copy(input);if(p==null)p=new JSONObject();String c=canonical(FEATS,feat);if(c.isEmpty())return blocked(s,"talento_desconhecido");String a=n(action);try{
        if(c.startsWith("Greater ")){String base=c.equals("Greater Aberrant Mark")?"Aberrant Dragonmark":c.replaceFirst("Greater ","");if(!contains(s.optJSONArray("eberron_feats"),base))return blocked(s,"requer_"+base);if(level<4)return blocked(s,"requer_nivel_4");}
        if(c.equals("Potent Dragonmark")&&!hasAnyDragonmark(s))return blocked(s,"requer_dracossinal");if(c.equals("Boon of Siberys")&&level<19)return blocked(s,"requer_nivel_19");
        if(a.equals(n("acquire"))){if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");s.put("dragonmark_acquired_after_level_1",level>1);applyFeat(s,c,level);return resolved(s,"acquire_feat",new JSONObject().put("feat",c));}
        if((c.equals("Mark of Storm")||c.equals("Greater Mark of Storm"))&&a.equals(n("windwright_intuition"))){String check=p.optString("check","");if(!(n(check).equals(n("Dexterity (Acrobatics)"))||n(check).equals(n("Navigator's Tools"))))return pending(s,"check: Dexterity (Acrobatics)|Navigator's Tools");if(!p.has("manual_roll"))return pending(s,"manual_roll");String die=c.equals("Greater Mark of Storm")?"d6":"d4";return resolved(s,"windwright_intuition",new JSONObject().put("die",die).put("manual_roll",p.optInt("manual_roll",0)).put("check",check));}
        if((c.equals("Mark of Storm")||c.equals("Greater Mark of Storm"))&&a.equals(n("gust_of_wind_free_cast"))){if(level<3)return blocked(s,"requer_nivel_3");String key="dragonmark_free_cast_gust_of_wind";if(s.optBoolean(key,false))return blocked(s,"conjuracao_gratuita_ja_usada");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");s.put(key,true);JSONObject d=new JSONObject().put("spell","Gust of Wind").put("reset","long_rest");if(c.equals("Greater Mark of Storm"))d.put("fly_speed_feet",60).put("fly_duration","for_gust_of_wind_duration");return resolved(s,"gust_of_wind_free_cast",d);}
        if(a.equals(n("intuition_die"))){if(!p.has("manual_roll"))return pending(s,"manual_roll");int roll=p.optInt("manual_roll",0);String die=s.optString("dragonmark_intuition_die","d4");return resolved(s,"intuition_die",new JSONObject().put("die",die).put("manual_roll",roll));}
        if(a.equals(n("free_spell_cast"))){String spell=p.optString("spell","");if(spell.isEmpty())return pending(s,"spell");String key="dragonmark_free_cast_"+id(spell);if(s.optBoolean(key,false))return blocked(s,"conjuracao_gratuita_ja_usada");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");s.put(key,true);return resolved(s,"free_spell_cast",new JSONObject().put("spell",spell).put("reset","long_rest"));}
        if(c.equals("Mark of Sentinel")&&a.equals(n("vigilant_guardian"))){int max=Math.max(1,proficiency(level));int u=s.optInt("vigilant_guardian_uses",max);if(u<=0)return blocked(s,"sem_usos");if(!p.optBoolean("reaction_confirmed",false))return pending(s,"reaction_confirmed=true");s.put("vigilant_guardian_uses",u-1).put("vigilant_guardian_uses_max",max);return resolved(s,"vigilant_guardian",new JSONObject().put("manual_reaction",true));}
        return blocked(s,"acao_talento_nao_suportada");
    }catch(Exception e){return error(s,e);}}

    // ---------- Homunculus Servant ----------
    public static JSONObject resolveHomunculusSpell(JSONObject input,int slotLevel,JSONObject p){JSONObject s=copy(input);if(p==null)p=new JSONObject();try{if(slotLevel<2)return blocked(s,"requer_slot_nivel_2_ou_maior");if(!p.optBoolean("material_100gp_confirmed",false))return pending(s,"material_100gp_confirmed=true");if(!p.optBoolean("space_confirmed",false))return pending(s,"space_confirmed=true");String appearance=p.optString("appearance","");if(appearance.isEmpty())return pending(s,"appearance");if(!p.optBoolean("spell_slot_spend_confirmed",false))return pending(s,"spell_slot_spend_confirmed=true");JSONObject h=new JSONObject().put("name","Homunculus Servant").put("size","Tiny").put("type","Construct").put("slot_level",slotLevel).put("appearance",appearance).put("ac_base",13).put("hp_base",5).put("scales_with_slot",true).put("turn","immediately_after_artificer").put("default_without_command","Dodge").put("command_action","no_action_required_by_caster");s.put("homunculus_servant",h);return resolved(s,"homunculus_servant",h);}catch(Exception e){return error(s,e);}}

    // ---------- Itens mágicos ----------
    public static JSONObject magicItemInfo(String item){String c=canonical(MAGIC_ITEMS,item);JSONObject o=new JSONObject();try{if(c.isEmpty())return o.put("known",false);o.put("known",true).put("name",c).put("rarity",itemRarity(c)).put("auto_rolls",false);
        if(c.equals("Repeating Shot"))o.put("attack_bonus",1).put("damage_bonus",1).put("ignore_loading",true).put("creates_ammunition_when_none",true);
        else if(c.equals("Returning Weapon"))o.put("attack_bonus",1).put("damage_bonus",1).put("returns_after_ranged_attack",true);
        else if(c.equals("Manifold Tool"))o.put("transform_to_artisan_tools",true).put("attunement",true);
        else if(c.equals("Boots of the Winding Path"))o.put("teleport_to_previously_occupied_space",true).put("activation","bonus_action");
        else if(c.equals("Mind Sharpener"))o.put("concentration_protection",true).put("charges",4);
        else if(c.equals("Repulsion Shield"))o.put("ac_bonus",1).put("charges",4).put("reaction_push",true);
        else if(c.equals("Spell-Refueling Ring"))o.put("spell_slot_recovery",true).put("uses_per_long_rest",1);
        else if(c.equals("Helm of Awareness"))o.put("initiative_benefit",true);
        else if(c.equals("Dazzling Weapon"))o.put("dazzling_effect_manual_resolution",true);
    }catch(Exception ignored){}return o;}

    public static JSONObject resolveMagicItemAction(JSONObject input,String item,String action,JSONObject p){JSONObject s=copy(input);if(p==null)p=new JSONObject();String c=canonical(MAGIC_ITEMS,item);if(c.isEmpty())return blocked(s,"item_desconhecido");String a=n(action);try{
        s.put("eberron_last_item_preview",magicItemInfo(c));
        if(c.equals("Manifold Tool")&&a.equals(n("transform"))){String tool=p.optString("tool","");if(tool.isEmpty())return pending(s,"tool");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");s.put("manifold_tool_form",tool);return resolved(s,"manifold_tool_transform",new JSONObject().put("tool",tool));}
        if(c.equals("Mind Sharpener")&&a.equals(n("protect_concentration"))){int q=itemCharges(s,c,4);if(q<=0)return blocked(s,"sem_cargas");if(!p.optBoolean("failed_concentration_save",false))return blocked(s,"gatilho_invalido");if(!p.optBoolean("reaction_confirmed",false))return pending(s,"reaction_confirmed=true");setItemCharges(s,c,q-1);return resolved(s,"mind_sharpener",new JSONObject().put("concentration_save_treated_as_success",true).put("charges_left",q-1));}
        if(c.equals("Repulsion Shield")&&a.equals(n("repel"))){int q=itemCharges(s,c,4);if(q<=0)return blocked(s,"sem_cargas");if(!p.optBoolean("reaction_confirmed",false))return pending(s,"reaction_confirmed=true");int ft=p.optInt("push_feet",15);if(ft<0||ft>15)return pending(s,"push_feet[0..15]");setItemCharges(s,c,q-1);return resolved(s,"repulsion_shield",new JSONObject().put("push_feet",ft).put("charges_left",q-1));}
        if(c.equals("Spell-Refueling Ring")&&a.equals(n("recover_slot"))){if(s.optBoolean("spell_refueling_ring_used",false))return blocked(s,"ja_usado_ate_descanso_longo");int sl=p.optInt("slot_level",0);if(sl<1)return pending(s,"slot_level");if(!p.optBoolean("bonus_action_confirmed",false))return pending(s,"bonus_action_confirmed=true");s.put("spell_refueling_ring_used",true).put("recovered_spell_slot_level",sl);return resolved(s,"spell_refueling_ring",new JSONObject().put("slot_level",sl));}
        if(c.equals("Boots of the Winding Path")&&a.equals(n("teleport"))){if(!p.optBoolean("bonus_action_confirmed",false))return pending(s,"bonus_action_confirmed=true");int ft=p.optInt("distance_feet",0);if(ft<1||ft>15)return pending(s,"distance_feet[1..15]");if(!p.optBoolean("space_previously_occupied_confirmed",false))return pending(s,"space_previously_occupied_confirmed=true");return resolved(s,"boots_winding_path",new JSONObject().put("teleport_feet",ft));}
        if(c.equals("Repeating Shot")||c.equals("Returning Weapon")||c.equals("Helm of Awareness"))return resolved(s,"passive_magic_item",magicItemInfo(c));
        if(c.equals("Dazzling Weapon")&&a.equals(n("dazzle"))){if(!p.has("manual_save_result"))return pending(s,"manual_save_result");return resolved(s,"dazzling_weapon",new JSONObject().put("manual_save_result",p.optString("manual_save_result","")).put("no_auto_roll",true));}
        return blocked(s,"acao_item_nao_suportada");
    }catch(Exception e){return error(s,e);}}

    // ---------- Bastiões ----------
    public static JSONObject resolveBastionAction(JSONObject input,String facility,String action,JSONObject p){JSONObject s=copy(input);if(p==null)p=new JSONObject();String f=canonical(BASTIONS,facility);if(f.isEmpty())return blocked(s,"instalacao_desconhecida");String a=n(action);try{JSONObject b=s.optJSONObject("eberron_bastion");if(b==null)b=new JSONObject();JSONObject fs=b.optJSONObject("facilities");if(fs==null)fs=new JSONObject();
        if(a.equals(n("build"))){if(!p.optBoolean("requirements_confirmed",false))return pending(s,"requirements_confirmed=true");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");fs.put(id(f),new JSONObject().put("name",f).put("built",true).put("source",SOURCE));b.put("facilities",fs);s.put("eberron_bastion",b);return resolved(s,"bastion_build",new JSONObject().put("facility",f));}
        JSONObject st=fs.optJSONObject(id(f));if(st==null||!st.optBoolean("built",false))return blocked(s,"instalacao_nao_construida");if(a.equals(n("order"))){String order=p.optString("order","");if(order.isEmpty())return pending(s,"order");if(!p.optBoolean("manual_resolution_confirmed",false))return pending(s,"manual_resolution_confirmed=true");st.put("last_order",order).put("last_result",p.optString("result",""));fs.put(id(f),st);b.put("facilities",fs);s.put("eberron_bastion",b);return resolved(s,"bastion_order",new JSONObject().put("facility",f).put("order",order));}
        return blocked(s,"acao_bastiao_nao_suportada");
    }catch(Exception e){return error(s,e);}}

    // ---------- Airships ----------
    public static JSONObject airshipInfo(String ship){String c=canonical(AIRSHIPS,ship);JSONObject o=new JSONObject();try{if(c.isEmpty())return o.put("known",false);o.put("known",true).put("name",c).put("auto_rolls",false);JSONArray comp=new JSONArray();if(c.equals("Lyrandar Air Cruiser")){comp.put("Helm");for(int i=1;i<=6;i++)comp.put("Lightning Turret "+i);comp.put("Shield Station");}else if(c.equals("Lyrandar Skyskiff")){comp.put("Helm").put("Tow Cable");}else if(c.equals("Strider Airship")){comp.put("Helm");for(int i=1;i<=4;i++)comp.put("Arcane Launcher "+i);}o.put("components",comp);}catch(Exception ignored){}return o;}

    public static JSONObject resolveAirshipAction(JSONObject input,String ship,String action,JSONObject p){JSONObject s=copy(input);if(p==null)p=new JSONObject();String c=canonical(AIRSHIPS,ship);if(c.isEmpty())return blocked(s,"airship_desconhecido");String a=n(action);try{JSONObject fleet=s.optJSONObject("eberron_airships");if(fleet==null)fleet=new JSONObject();JSONObject v=fleet.optJSONObject(id(c));
        if(a.equals(n("commission"))){if(v!=null)return blocked(s,"airship_ja_registrado");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");v=airshipInfo(c).put("hull_damage",0).put("crew",new JSONArray()).put("upgrades",new JSONArray());fleet.put(id(c),v);s.put("eberron_airships",fleet);return resolved(s,"airship_commission",v);}
        if(v==null)return blocked(s,"airship_nao_registrado");
        if(a.equals(n("assign_crew"))){JSONArray crew=p.optJSONArray("crew");if(crew==null)return pending(s,"crew");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");v.put("crew",copy(crew));}
        else if(a.equals(n("component_damage"))){String comp=p.optString("component","");int dmg=p.optInt("damage",-1);if(comp.isEmpty()||dmg<0)return pending(s,"component; damage");if(!p.optBoolean("damage_roll_confirmed",false))return pending(s,"damage_roll_confirmed=true");JSONObject ds=v.optJSONObject("component_damage");if(ds==null)ds=new JSONObject();ds.put(id(comp),ds.optInt(id(comp),0)+dmg);v.put("component_damage",ds);}
        else if(a.equals(n("repair"))){String comp=p.optString("component","");int amount=p.optInt("amount",-1);if(comp.isEmpty()||amount<0)return pending(s,"component; amount");if(!p.optBoolean("repair_confirmed",false))return pending(s,"repair_confirmed=true");JSONObject ds=v.optJSONObject("component_damage");if(ds==null)ds=new JSONObject();ds.put(id(comp),Math.max(0,ds.optInt(id(comp),0)-amount));v.put("component_damage",ds);}
        else if(a.equals(n("maneuver"))){String m=p.optString("maneuver","");if(m.isEmpty())return pending(s,"maneuver");if(!p.has("manual_check_result")&&p.optBoolean("check_required",false))return pending(s,"manual_check_result");v.put("last_maneuver",m).put("last_manual_check_result",p.optString("manual_check_result","not_required"));}
        else if(a.equals(n("upgrade"))){String u=p.optString("upgrade","");if(u.isEmpty())return pending(s,"upgrade");if(!p.optBoolean("requirements_confirmed",false)||!p.optBoolean("confirm",false))return pending(s,"requirements_confirmed=true; confirm=true");JSONArray us=v.optJSONArray("upgrades");if(us==null)us=new JSONArray();if(!contains(us,u))us.put(u);v.put("upgrades",us);}
        else return blocked(s,"acao_airship_nao_suportada");
        fleet.put(id(c),v);s.put("eberron_airships",fleet);return resolved(s,"airship_"+a,new JSONObject().put("ship",c));
    }catch(Exception e){return error(s,e);}}

    // ---------- Bestiário ----------
    public static JSONObject creatureInfo(String name,JSONObject context){String c=canonical(CREATURES,name);JSONObject o=new JSONObject();try{if(c.isEmpty())return o.put("known",false);o.put("known",true).put("name",c).put("source",SOURCE).put("auto_rolls",false).put("action_resolution","manual_roll_payload");
        if(c.equals("Boromar Smuggler"))o.put("size","Small").put("type","Humanoid").put("ac",13).put("hp",27).put("speed_ft",40).put("cr","1/2");
        else if(c.equals("Cannith Artificer"))o.put("type","Humanoid").put("ac",16).put("hp",71).put("speed_ft",30).put("cr","4");
        else if(c.equals("Dreaming Dark Infiltrator"))o.put("ac",15).put("hp",104).put("cr","7");
        else if(c.equals("Orien Enforcer"))o.put("ac",16).put("hp",55).put("cr","4");
        else if(c.equals("Sivis Scribe"))o.put("ac",15).put("hp",56).put("cr","3");
        else if(c.equals("Homunculus Servant")){int slot=context==null?2:Math.max(2,context.optInt("slot_level",2));o.put("size","Tiny").put("type","Construct").put("ac",13).put("hp_base",5).put("slot_level",slot).put("dynamic_scaling",true);}
        else if(c.equals("Steel Defender")){int lvl=context==null?3:Math.max(3,context.optInt("artificer_level",3));o.put("type","Construct").put("hp_formula","5 + 5 × artificer level").put("hp",5+5*lvl).put("dynamic_scaling",true).put("acts_on_artificer_turn",true);}
        else o.put("numeric_card_status","source_specific_card_pending_authoritative_ingest").put("runtime_state_and_action_gateway",true);
    }catch(Exception ignored){}return o;}

    public static JSONObject resolveCreatureAction(JSONObject input,String creature,String action,JSONObject p){JSONObject s=copy(input);if(p==null)p=new JSONObject();String c=canonical(CREATURES,creature);if(c.isEmpty())return blocked(s,"criatura_desconhecida");String a=n(action);try{s.put("eberron_creature_preview",creatureInfo(c,p));if(a.equals(n("attack"))){if(!p.has("manual_attack_total")||!p.has("manual_damage"))return pending(s,"manual_attack_total; manual_damage");return resolved(s,"creature_attack",new JSONObject().put("creature",c).put("manual_attack_total",p.optInt("manual_attack_total",0)).put("manual_damage",p.optInt("manual_damage",0)));}if(a.equals(n("saving_throw"))){if(!p.has("manual_save_total"))return pending(s,"manual_save_total");return resolved(s,"creature_save",new JSONObject().put("creature",c).put("manual_save_total",p.optInt("manual_save_total",0)));}if(a.equals(n("apply_damage"))){int dmg=p.optInt("damage",-1);if(dmg<0)return pending(s,"damage");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");int hp=p.optInt("current_hp",s.optJSONObject("eberron_creature_preview").optInt("hp",0));return resolved(s,"creature_damage",new JSONObject().put("hp_after",Math.max(0,hp-dmg)));}return blocked(s,"acao_criatura_nao_suportada");}catch(Exception e){return error(s,e);}}

    // ---------- Frameworks / lore operacional ----------
    public static JSONObject resolveFrameworkAction(JSONObject input,String framework,String action,JSONObject p){JSONObject s=copy(input);if(p==null)p=new JSONObject();String f=canonical(FRAMEWORKS,framework);if(f.isEmpty())return blocked(s,"framework_desconhecido");String a=n(action);try{JSONObject w=s.optJSONObject("eberron_campaign_framework");if(w==null)w=new JSONObject();if(a.equals(n("activate"))){if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");w.put("active",f).put("progress",0).put("facts",new JSONArray());s.put("eberron_campaign_framework",w);return resolved(s,"framework_activate",new JSONObject().put("framework",f));}if(!f.equals(w.optString("active","")))return blocked(s,"framework_nao_ativo");if(a.equals(n("record_fact"))){String fact=p.optString("fact","");if(fact.isEmpty())return pending(s,"fact");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");JSONArray facts=w.optJSONArray("facts");if(facts==null)facts=new JSONArray();facts.put(fact);w.put("facts",facts);s.put("eberron_campaign_framework",w);return resolved(s,"framework_fact",new JSONObject().put("fact",fact));}if(a.equals(n("advance"))){int delta=p.optInt("delta",0);if(delta==0)return pending(s,"delta");if(!p.optBoolean("confirm",false))return pending(s,"confirm=true");w.put("progress",Math.max(0,w.optInt("progress",0)+delta));s.put("eberron_campaign_framework",w);return resolved(s,"framework_advance",new JSONObject().put("progress",w.optInt("progress",0)));}return blocked(s,"acao_framework_nao_suportada");}catch(Exception e){return error(s,e);}}

    // ---------- Descansos ----------
    public static JSONObject shortRest(JSONObject input){JSONObject s=input==null?new JSONObject():input;try{if(s.optInt("eberron_artificer_level",0)>=14){int max=s.optInt("flash_of_genius_uses_max",Math.max(1,abilityMod(s.optInt("int",10))));if(s.optInt("flash_of_genius_uses",0)<max)s.put("flash_of_genius_uses",s.optInt("flash_of_genius_uses",0)+1);}if(s.optInt("eberron_artificer_level",0)>=20&&s.optInt("attuned_magic_items",0)>0)s.put("flash_of_genius_uses",s.optInt("flash_of_genius_uses_max",Math.max(1,abilityMod(s.optInt("int",10)))));}catch(Exception ignored){}return s;}
    public static JSONObject longRest(JSONObject input){JSONObject s=input==null?new JSONObject():input;try{s.put("tinkers_magic_uses",s.optInt("tinkers_magic_uses_max",Math.max(1,abilityMod(s.optInt("int",10))))).put("tinkers_magic_items",new JSONArray()).put("magic_item_tinker_drain_used",false).put("magic_item_tinker_transmute_used",false).put("spell_refueling_ring_used",false).put("shifter_shift_used",false).put("shifter_shift_active",false).put("guided_precision_used_this_turn",false);if(s.has("flash_of_genius_uses_max"))s.put("flash_of_genius_uses",s.optInt("flash_of_genius_uses_max",0));if(s.has("restorative_reagents_uses_max"))s.put("restorative_reagents_uses",s.optInt("restorative_reagents_uses_max",0));if(s.has("arcane_jolt_uses_max"))s.put("arcane_jolt_uses",s.optInt("arcane_jolt_uses_max",0));if(s.has("vigilant_guardian_uses_max"))s.put("vigilant_guardian_uses",s.optInt("vigilant_guardian_uses_max",0));Iterator<String> it=s.keys();JSONArray clear=new JSONArray();while(it.hasNext()){String k=it.next();if(k.startsWith("dragonmark_free_cast_"))clear.put(k);}for(int i=0;i<clear.length();i++)s.put(clear.optString(i),false);}catch(Exception ignored){}return s;}
    public static JSONObject startTurn(JSONObject input){JSONObject s=input==null?new JSONObject():input;try{s.put("guided_precision_used_this_turn",false);}catch(Exception ignored){}return s;}

    // ---------- Coverage ----------
    public static JSONObject coverage(){JSONObject o=new JSONObject();try{int cards=0;for(String c:CREATURES)if(!creatureInfo(c,new JSONObject()).has("numeric_card_status"))cards++;o.put("artificer",1).put("subclasses",SUBCLASSES.length).put("species",SPECIES.length).put("backgrounds",BACKGROUNDS.length).put("feats",FEATS.length).put("homunculus_spell",1).put("bastions",BASTIONS.length).put("airships",AIRSHIPS.length).put("magic_items",MAGIC_ITEMS.length).put("bestiary_runtime_entries",CREATURES.length).put("bestiary_numeric_cards_compiled",cards).put("bestiary_numeric_cards_pending",CREATURES.length-cards).put("frameworks",FRAMEWORKS.length).put("auto_rolls",false).put("book_complete",cards==CREATURES.length);return o;}catch(Exception e){return o;}}

    // ---------- helpers ----------
    private static int plansKnown(int lvl){if(lvl<2)return 0;if(lvl<6)return 4;if(lvl<10)return 5;if(lvl<14)return 6;if(lvl<18)return 7;return 8;}
    private static int replicatedMax(int lvl){if(lvl<2)return 0;if(lvl<6)return 2;if(lvl<10)return 3;if(lvl<14)return 4;if(lvl<18)return 5;return 6;}
    private static int attunementMax(int lvl){if(lvl>=18)return 6;if(lvl>=14)return 5;if(lvl>=10)return 4;return 3;}
    private static int planTier(String plan){String c=canonical(MAGIC_ITEMS,plan);if(c.equals("Spell-Refueling Ring"))return 10;if(c.equals("Repulsion Shield"))return 6;return c.isEmpty()?99:2;}
    private static String itemRarity(String item){String c=canonical(MAGIC_ITEMS,item);if(c.equals("Manifold Tool"))return "common";if(c.isEmpty())return "unknown";return "uncommon";}
    private static int abilityMod(int score){return (int)Math.floor((score-10)/2.0);}
    private static int proficiency(int level){return 2+(Math.max(1,level)-1)/4;}
    private static JSONObject steelDefender(JSONObject s,int level){JSONObject d=new JSONObject();try{d.put("name","Steel Defender").put("type","Construct").put("hp",5+5*Math.max(3,level)).put("hp_formula","5 + 5 × artificer level").put("turn","on_artificer_turn").put("command","bonus_action_or_replace_one_attack").put("deflect_attack_reaction",true).put("uses_spell_attack_modifier",true).put("uses_proficiency_bonus",true).put("uses_intelligence_modifier",true);}catch(Exception ignored){}return d;}
    private static String markForBackground(String bg){if(bg==null)return "";if(bg.equals("Aberrant Heir"))return "Aberrant Dragonmark";if(!bg.startsWith("House ")||!bg.endsWith(" Heir"))return "";String h=bg.substring(6,bg.length()-5).trim();if(h.equals("Cannith"))return "Mark of Making";if(h.equals("Deneith"))return "Mark of Sentinel";if(h.equals("Ghallanda"))return "Mark of Hospitality";if(h.equals("Jorasco"))return "Mark of Healing";if(h.equals("Kundarak"))return "Mark of Warding";if(h.equals("Lyrandar"))return "Mark of Storm";if(h.equals("Medani"))return "Mark of Detection";if(h.equals("Orien"))return "Mark of Passage";if(h.equals("Phiarlan")||h.equals("Thuranni"))return "Mark of Shadow";if(h.equals("Sivis"))return "Mark of Scribing";if(h.equals("Tharashk"))return "Mark of Finding";if(h.equals("Vadalis"))return "Mark of Handling";return "";}
    private static String houseFromMark(String mark){if(mark==null)return "";if(mark.contains("Making"))return "Cannith";if(mark.contains("Sentinel"))return "Deneith";if(mark.contains("Hospitality"))return "Ghallanda";if(mark.contains("Healing"))return "Jorasco";if(mark.contains("Warding"))return "Kundarak";if(mark.contains("Storm"))return "Lyrandar";if(mark.contains("Detection"))return "Medani";if(mark.contains("Passage"))return "Orien";if(mark.contains("Shadow"))return "Phiarlan/Thuranni";if(mark.contains("Scribing"))return "Sivis";if(mark.contains("Finding"))return "Tharashk";if(mark.contains("Handling"))return "Vadalis";if(mark.contains("Aberrant"))return "Tarkanan/Aberrant";return "";}
    private static boolean hasAnyDragonmark(JSONObject s){JSONArray a=s.optJSONArray("eberron_feats");if(a==null)return false;for(int i=0;i<a.length();i++)if(n(a.optString(i)).contains("mark")||n(a.optString(i)).contains("dragonmark"))return true;return false;}
    private static boolean replicated(JSONObject s,String name){JSONArray a=s.optJSONArray("replicated_magic_items");if(a==null)return false;for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&n(x.optString("name","")).equals(n(name)))return true;}return false;}
    private static JSONArray removeItem(JSONArray a,String name){JSONArray out=new JSONArray();if(a==null)return out;for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x==null||!n(x.optString("name","")).equals(n(name)))out.put(x==null?a.opt(i):x);}return out;}
    private static int itemCharges(JSONObject s,String item,int def){JSONObject c=s.optJSONObject("eberron_item_charges");return c==null?def:c.optInt(id(item),def);}
    private static void setItemCharges(JSONObject s,String item,int value)throws Exception{JSONObject c=s.optJSONObject("eberron_item_charges");if(c==null)c=new JSONObject();c.put(id(item),Math.max(0,value));s.put("eberron_item_charges",c);}
    private static JSONArray replace(JSONArray a,String oldV,String newV){JSONArray out=new JSONArray();if(a==null)return out;for(int i=0;i<a.length();i++)out.put(n(a.optString(i)).equals(n(oldV))?newV:a.opt(i));return out;}
    private static JSONArray copy(JSONArray a){try{return a==null?new JSONArray():new JSONArray(a.toString());}catch(Exception e){JSONArray x=new JSONArray();if(a!=null)for(int i=0;i<a.length();i++)x.put(a.opt(i));return x;}}
    private static JSONObject copy(JSONObject o){try{return o==null?new JSONObject():new JSONObject(o.toString());}catch(Exception e){return o==null?new JSONObject():o;}}
    private static JSONArray arr(String[] xs){JSONArray a=new JSONArray();for(String x:xs)a.put(x);return a;}
    private static boolean has(String[] xs,String v){return !canonical(xs,v).isEmpty();}
    private static boolean contains(JSONArray a,String v){if(a==null)return false;for(int i=0;i<a.length();i++)if(n(a.optString(i)).equals(n(v)))return true;return false;}
    private static String canonicalMagicItem(String v){return canonical(MAGIC_ITEMS,v);}
    private static String canonical(String[] xs,String v){String q=n(v);for(String x:xs)if(n(x).equals(q))return x;return "";}
    private static boolean eqAny(String v,String... xs){for(String x:xs)if(n(x).equals(n(v)))return true;return false;}
    private static String n(String s){if(s==null)return "";String x=Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replace('’','\'').replaceAll("[^a-z0-9]+"," ").trim();return x;}
    private static String id(String s){String x=n(s).replace(' ','_');return x.isEmpty()?"unknown":x;}
    private static JSONObject pending(JSONObject s,String need){try{return s.put("eberron_status","pending").put("eberron_pending",need).put("eberron_resolved",false).put("eberron_auto_rolls",false);}catch(Exception e){return s;}}
    private static JSONObject blocked(JSONObject s,String why){try{return s.put("eberron_status","blocked").put("eberron_reason",why).put("eberron_resolved",false).put("eberron_auto_rolls",false);}catch(Exception e){return s;}}
    private static JSONObject resolved(JSONObject s,String event,JSONObject effect){try{return s.put("eberron_status","resolved").put("eberron_event",event).put("eberron_effect",effect).put("eberron_resolved",true).put("eberron_auto_rolls",false).put("eberron_pending","");}catch(Exception e){return s;}}
    private static JSONObject error(JSONObject s,Exception e){try{return s.put("eberron_status","blocked").put("eberron_reason","runtime_error").put("eberron_error",e.getClass().getSimpleName()).put("eberron_resolved",false);}catch(Exception ignored){return s;}}
}
