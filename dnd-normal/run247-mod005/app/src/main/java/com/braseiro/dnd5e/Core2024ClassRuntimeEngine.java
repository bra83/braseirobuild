package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Iterator;
import java.util.Locale;

/**
 * RUN137 — runtime estrutural das 12 classes do PHB 2024.
 * Nunca rola dados nem escolhe opções pelo jogador. Entradas aleatórias/decisões
 * são aceitas somente por payload explícito.
 */
public final class Core2024ClassRuntimeEngine {
    public static final String SOURCE="Player's Handbook (2024)";
    public static final String SOURCE_SHA="bf02ab275a7344430a5d208403eb9c13e69275d10c223e8db3d0c2c0f62be15a";
    private static final String[] CLASSES={"Bárbaro","Bardo","Bruxo","Clérigo","Druida","Feiticeiro","Guardião","Guerreiro","Ladino","Mago","Monge","Paladino"};
    private Core2024ClassRuntimeEngine(){}

    public static boolean supports(String cls){String c=key(cls);return c.equals("barbarian")||c.equals("bard")||c.equals("warlock")||c.equals("cleric")||c.equals("druid")||c.equals("sorcerer")||c.equals("ranger")||c.equals("fighter")||c.equals("rogue")||c.equals("wizard")||c.equals("monk")||c.equals("paladin");}

    public static JSONObject descriptor(){JSONObject o=new JSONObject();try{return o.put("id","core2024_class_runtime").put("source",SOURCE).put("classes",12).put("auto_rolls",false).put("choices_are_explicit",true).put("runtime","Core2024ClassRuntimeEngine");}catch(Exception e){return o;}}

    public static JSONObject install(){JSONObject out=new JSONObject();JSONArray failed=new JSONArray();int ok=0;try{for(String cls:CLASSES){String eid="run137.core2024.class."+key(cls);JSONObject ev=new JSONObject().put("evidence_id",eid).put("content_kind","class").put("content_name",cls).put("source_book",SOURCE).put("source_locator","class progression and features").put("source_sha256",SOURCE_SHA).put("revision",1);JSONObject er=AuthorizedMechanicalEvidenceRegistry.registerReviewed(ev);if(!er.optBoolean("registered",false)){failed.put(cls+":"+er.optString("authority"));continue;}JSONObject basis=new JSONObject().put("source_type","reviewed_evidence").put("evidence_id",eid).put("content_kind","class").put("content_name",cls).put("source_book",SOURCE).put("source_locator","class progression and features").put("source_sha256",SOURCE_SHA).put("revision",1);JSONObject mech=new JSONObject().put("effect_pipeline_complete",true).put("execution_status","compiled_effect_pipeline").put("runtime_resolver","Core2024ClassRuntimeEngine.execute").put("structured_class_runtime",true).put("auto_rolls",false).put("auto_mutation_allowed",false);JSONObject rr=CompiledContentMechanicsEngine.register("class",cls,mech,basis);if(rr.optBoolean("registered",false))ok++;else failed.put(cls+":"+rr.optString("authority"));}return out.put("installed",ok).put("expected",CLASSES.length).put("failed",failed).put("complete",ok==CLASSES.length).put("auto_rolls",false);}catch(Exception e){try{return out.put("installed",ok).put("expected",CLASSES.length).put("failed",failed).put("complete",false).put("error",e.getClass().getSimpleName());}catch(Exception ignored){return out;}}}

    public static JSONObject ensure(JSONObject input,String cls,int level){JSONObject s=copy(input);try{
        if(!supports(cls))return s;int lvl=clamp(level);String c=key(cls);JSONObject r=s.optJSONObject("class_resources");if(r==null)r=new JSONObject();
        s.put("class_rules_source",SOURCE).put("class_rules_runtime","Core2024ClassRuntimeEngine").put("class_rules_profile","2024").put("class_rules_auto_rolls",false);
        if(c.equals("barbarian")){
            resource(r,"rage",rageUses(lvl),"long_rest").put("short_rest_recovery",1).put("damage_bonus",rageDamage(lvl));
            s.put("rage_damage_bonus",rageDamage(lvl)).put("weapon_masteries_known",lvl>=10?4:lvl>=4?3:2);
            if(lvl>=11)resource(r,"relentless_rage_dc_reset",1,"short_or_long_rest").put("dc_start",10);
        } else if(c.equals("bard")){
            int cha=Math.max(1,new DndRulesEngine().modifier(s.optInt("cha",10)));JSONObject x=resource(r,"bardic_inspiration",cha,lvl>=5?"short_or_long_rest":"long_rest");x.put("die",bardDie(lvl));if(lvl>=5)x.put("spell_slot_can_restore_one",true);
        } else if(c.equals("warlock")){
            if(lvl>=2)resource(r,"magical_cunning",1,"long_rest").put("ritual_minutes",1).put("restores_pact_slots_formula","ceil(max/2)");
        } else if(c.equals("cleric")){
            if(lvl>=2)resource(r,"channel_divinity",clericChannel(lvl),"long_rest").put("short_rest_recovery",1);
            if(lvl>=10)resource(r,"divine_intervention",1,"long_rest");
        } else if(c.equals("druid")){
            if(lvl>=2){resource(r,"wild_shape",druidWildShape(lvl),"long_rest").put("short_rest_recovery",1);s.put("wild_shape_forms_known",lvl>=8?8:lvl>=4?6:4).put("wild_shape_max_cr",lvl>=8?"1":lvl>=4?"1/2":"1/4").put("wild_shape_flight_allowed",lvl>=8);}
            if(lvl>=5)resource(r,"wild_resurgence_slot_recovery",1,"long_rest");
        } else if(c.equals("sorcerer")){
            resource(r,"innate_sorcery",2,"long_rest");
            if(lvl>=2)resource(r,"sorcery_points",lvl,"long_rest");
            if(lvl>=5)resource(r,"sorcerous_restoration",1,"long_rest").put("restore_points_max",lvl/2);
        } else if(c.equals("ranger")){
            resource(r,"favored_enemy_free_hunters_mark",rangerFavoredEnemy(lvl),"long_rest").put("spell","Marca do Predador");
            s.put("weapon_masteries_known",2);
        } else if(c.equals("fighter")){
            resource(r,"second_wind",fighterSecondWind(lvl),"long_rest").put("short_rest_recovery",1).put("healing","1d10 + nível de Guerreiro");
            if(lvl>=2)resource(r,"action_surge",lvl>=17?2:1,"short_or_long_rest").put("max_per_turn",1);
            if(lvl>=9)resource(r,"indomitable",lvl>=17?3:lvl>=13?2:1,"long_rest").put("reroll_bonus","nível de Guerreiro");
            s.put("weapon_masteries_known",lvl>=16?6:lvl>=10?5:lvl>=4?4:3);
        } else if(c.equals("rogue")){
            s.put("sneak_attack_dice",((lvl+1)/2)+"d6");if(lvl>=5)s.put("cunning_strike_available",true);
            s.put("weapon_masteries_known",2);
        } else if(c.equals("wizard")){
            resource(r,"arcane_recovery",1,"long_rest").put("slot_level_budget",(lvl+1)/2).put("max_slot_level",5).put("requires_short_rest",true);
        } else if(c.equals("monk")){
            s.put("martial_arts_die",monkDie(lvl)).put("unarmored_movement_bonus_m",monkMove(lvl));
            if(lvl>=2){resource(r,"focus_points",lvl,"short_or_long_rest");resource(r,"uncanny_metabolism",1,"long_rest").put("trigger","initiative").put("requires_roll","martial_arts_roll");}
        } else if(c.equals("paladin")){
            resource(r,"lay_on_hands",5*lvl,"long_rest").put("unit","hp_pool");
            if(lvl>=2)resource(r,"free_divine_smite",1,"long_rest").put("spell","Golpe Divino");
            if(lvl>=3)resource(r,"channel_divinity",lvl>=11?3:2,"long_rest").put("short_rest_recovery",1);
            if(lvl>=5)resource(r,"free_find_steed",1,"long_rest").put("spell","Convocar Montaria");
        }
        s.put("class_resources",r);appendPendingChoices(s,c,lvl);
    }catch(Exception e){AppLog.ignored("Core2024ClassRuntimeEngine","Falha ao estruturar recursos de classe 2024.",e);}return s;}

    public static JSONArray actionsFor(String cls,int level){
        JSONArray a=new JSONArray();String c=key(cls);int lvl=clamp(level);
        if(c.equals("barbarian")){
            a.put("Fúria");
            if(lvl>=11)a.put("Fúria Implacável");
        }else if(c.equals("bard")){
            a.put("Inspiração de Bardo");
            if(lvl>=5)a.put("Fonte de Inspiração");
        }else if(c.equals("cleric")){
            if(lvl>=2){
                a.put("Canalizar Divindade: Expulsar Mortos-Vivos");
                a.put("Canalizar Divindade: Centelha Divina");
            }
            if(lvl>=10)a.put("Intervenção Divina");
        }else if(c.equals("druid")){
            if(lvl>=2)a.put("Forma Selvagem");
            if(lvl>=5){
                a.put("Ressurgimento Selvagem Espaço para Forma");
                a.put("Ressurgimento Selvagem Forma para Espaço");
            }
        }else if(c.equals("fighter")){
            a.put("Retomar o Fôlego");
            if(lvl>=2){
                a.put("Surto de Ação");
                a.put("Mente Tática");
            }
            if(lvl>=9)a.put("Indomável");
        }else if(c.equals("monk")){
            if(lvl>=2){
                a.put("Rajada de Golpes");
                a.put("Defesa Paciente");
                a.put("Passo do Vento");
                a.put("Metabolismo Incomum");
            }
            if(lvl>=5)a.put("Ataque Atordoante");
        }else if(c.equals("paladin")){
            a.put("Imposição das Mãos");
            if(lvl>=2)a.put("Golpe Divino Gratuito");
            if(lvl>=3)a.put("Canalizar Divindade");
            if(lvl>=5)a.put("Convocar Montaria Gratuito");
        }else if(c.equals("ranger")){
            a.put("Marca do Predador Gratuita");
        }else if(c.equals("rogue")){
            if(lvl>=5)a.put("Golpe Astuto");
        }else if(c.equals("sorcerer")){
            a.put("Feitiçaria Inata");
            if(lvl>=2){
                a.put("Criar Espaço de Magia");
                a.put("Converter Espaço em Pontos");
            }
            if(lvl>=5)a.put("Restauração Feiticeira");
            if(lvl>=7)a.put("Feitiçaria Inata com Pontos");
        }else if(c.equals("warlock")){
            if(lvl>=2)a.put("Astúcia Mágica");
        }else if(c.equals("wizard")){
            a.put("Recuperação Arcana");
        }
        return a;
    }

    public static JSONObject shortRest(JSONObject input,String cls,int level){
        JSONObject s=ensure(input,cls,level);
        try{
            JSONObject r=s.optJSONObject("class_resources");if(r==null)return s;
            String c=key(cls);int lvl=clamp(level);
            partial(r,"rage",1);
            partial(r,"channel_divinity",1);
            partial(r,"wild_shape",1);
            partial(r,"second_wind",1);
            if(c.equals("fighter")&&lvl>=2)full(r,"action_surge");
            if(c.equals("monk")&&lvl>=2)full(r,"focus_points");
            if(c.equals("bard")&&lvl>=5)full(r,"bardic_inspiration");
            s.put("class_resources",r);
        }catch(Exception ignored){}
        return s;
    }
    public static JSONObject longRest(JSONObject input,String cls,int level){return ensure(input,cls,level);}

    /** Ações que exigem dados/escolhas recebem tudo via payload. */
    public static JSONObject execute(JSONObject input,String cls,int level,String action,JSONObject payload){
        JSONObject s=ensure(input,cls,level);
        try{
            String c=key(cls),a=n(action);if(payload==null)payload=new JSONObject();
            int lvl=clamp(level);

            // Barbarian
            if(c.equals("barbarian")&&a.equals("furia")){
                if(!spend(s,"rage",1))return blocked(s,"rage");
                s.put("rage_active",true);
                return mark(s,"barbarian_rage",new JSONObject().put("damage_bonus",rageDamage(lvl)).put("rage_active",true));
            }
            if(c.equals("barbarian")&&a.equals("furia implacavel")){
                if(lvl<11)return blocked(s,"requires_level_11");
                if(!payload.has("con_save_roll"))return pending(s,"con_save_roll");
                JSONObject dcRes=res(s,"relentless_rage_dc_reset");
                int currentDc=dcRes!=null?dcRes.optInt("dc_start",10):10;
                int roll=payload.optInt("con_save_roll",0);
                boolean success=roll>=currentDc;
                if(success){s.put("hp",1);if(dcRes!=null)dcRes.put("dc_start",currentDc+5);}
                return mark(s,"barbarian_relentless_rage",new JSONObject().put("con_save_roll",roll).put("dc",currentDc).put("success",success).put("hp_set_to_one",success).put("next_dc",success?currentDc+5:currentDc));
            }

            // Fighter
            if(c.equals("fighter")&&a.equals("retomar o folego")){
                if(!spend(s,"second_wind",1))return blocked(s,"second_wind");
                if(!payload.has("d10_roll"))return pending(s,"d10_roll");
                int roll=boundedRoll(payload.optInt("d10_roll"),1,10);
                int maxHp=Math.max(1,s.optInt("max_hp",1)),hp=Math.max(0,s.optInt("hp",maxHp));
                int heal=Math.min(maxHp-hp,roll+lvl);
                s.put("hp",hp+heal);
                return mark(s,"fighter_second_wind",new JSONObject().put("d10_roll",roll).put("healing",heal));
            }
            if(c.equals("fighter")&&a.equals("surto de acao")){
                if(lvl<2)return blocked(s,"requires_level_2");
                if(!spend(s,"action_surge",1))return blocked(s,"action_surge");
                return mark(s,"fighter_action_surge",new JSONObject().put("additional_action_granted",true));
            }
            if(c.equals("fighter")&&a.equals("mente tatica")){
                if(!payload.has("d10_roll")||!payload.has("success_after_bonus"))return pending(s,"d10_roll + success_after_bonus");
                int roll=boundedRoll(payload.optInt("d10_roll"),1,10);boolean success=payload.optBoolean("success_after_bonus");if(success){if(!spend(s,"second_wind",1))return blocked(s,"second_wind");}
                return mark(s,"fighter_tactical_mind",new JSONObject().put("d10_roll",roll).put("success_after_bonus",success).put("second_wind_spent",success));
            }
            if(c.equals("fighter")&&a.equals("indomavel")){
                if(lvl<9)return blocked(s,"requires_level_9");
                if(!spend(s,"indomitable",1))return blocked(s,"indomitable");
                return mark(s,"fighter_indomitable",new JSONObject().put("reroll_bonus",lvl));
            }

            // Bard
            if(c.equals("bard")&&a.equals("inspiracao de bardo")){
                if(!spend(s,"bardic_inspiration",1))return blocked(s,"bardic_inspiration");
                return mark(s,"bard_bardic_inspiration",new JSONObject().put("die",bardDie(lvl)));
            }
            if(c.equals("bard")&&a.equals("fonte de inspiracao")){
                int slot=payload.optInt("spell_slot_level",0);if(slot<1)return pending(s,"spell_slot_level");JSONObject after=CharacterResourceStateEngine.spendSpellSlot(s,slot);if(after.optBoolean("spell_slot_spend_blocked",false))return blocked(s,"spell_slot_level_"+slot);s=after;restore(s,"bardic_inspiration",1);return mark(s,"bard_font_of_inspiration",new JSONObject().put("spell_slot_level",slot).put("bardic_inspiration_restored",1));
            }

            // Cleric
            if(c.equals("cleric")&&(a.equals("canalizar divindade expulsar mortos vivos")||a.contains("expulsar mortos"))){
                if(lvl<2)return blocked(s,"requires_level_2");
                if(!spend(s,"channel_divinity",1))return blocked(s,"channel_divinity");
                return mark(s,"cleric_turn_undead",new JSONObject().put("channel_divinity_spent",1));
            }
            if(c.equals("cleric")&&(a.equals("canalizar divindade centelha divina")||a.contains("centelha divina"))){
                if(lvl<2)return blocked(s,"requires_level_2");
                if(!spend(s,"channel_divinity",1))return blocked(s,"channel_divinity");
                if(!payload.has("d8_roll"))return pending(s,"d8_roll");
                int roll=boundedRoll(payload.optInt("d8_roll"),1,8);
                int wisMod=Math.max(0,new DndRulesEngine().modifier(s.optInt("wis",10)));
                int total=roll+wisMod;
                String mode=payload.optString("mode","heal");
                if("heal".equalsIgnoreCase(mode)){int maxHp=Math.max(1,s.optInt("max_hp",1)),hp=Math.max(0,s.optInt("hp",maxHp)),healed=Math.min(total,maxHp-hp);s.put("hp",hp+healed);}
                return mark(s,"cleric_divine_spark",new JSONObject().put("d8_roll",roll).put("wis_mod",wisMod).put("total",total).put("mode",mode));
            }
            if(c.equals("cleric")&&a.equals("intervencao divina")){
                if(lvl<10)return blocked(s,"requires_level_10");
                if(!spend(s,"divine_intervention",1))return blocked(s,"divine_intervention");
                return mark(s,"cleric_divine_intervention",new JSONObject().put("spell",payload.optString("spell_name","Magia de Clérigo até 5º círculo")));
            }

            // Druid
            if(c.equals("druid")&&a.equals("forma selvagem")){
                if(lvl<2)return blocked(s,"requires_level_2");
                if(!spend(s,"wild_shape",1))return blocked(s,"wild_shape");
                return mark(s,"druid_wild_shape",new JSONObject().put("beast_form",payload.optString("form_name","Forma de Besta")));
            }
            if(c.equals("druid")&&a.equals("ressurgimento selvagem espaco para forma")){
                JSONObject rr=res(s,"wild_shape");if(rr==null||rr.optInt("current",0)>0)return blocked(s,"requires_zero_wild_shape_uses");int slot=payload.optInt("spell_slot_level",0);if(slot<1)return pending(s,"spell_slot_level");JSONObject after=CharacterResourceStateEngine.spendSpellSlot(s,slot);if(after.optBoolean("spell_slot_spend_blocked",false))return blocked(s,"spell_slot_level_"+slot);s=after;restore(s,"wild_shape",1);return mark(s,"druid_wild_resurgence_slot_to_shape",new JSONObject().put("slot_spent",slot).put("wild_shape_restored",1));
            }
            if(c.equals("druid")&&a.equals("ressurgimento selvagem forma para espaco")){
                JSONObject wr=res(s,"wild_resurgence_slot_recovery"),ws=res(s,"wild_shape");JSONArray cur=s.optJSONArray("spell_slots_current"),mx=s.optJSONArray("spell_slots_max");if(wr==null||wr.optInt("current",0)<1)return blocked(s,"wild_resurgence_slot_recovery");if(ws==null||ws.optInt("current",0)<1)return blocked(s,"wild_shape");if(cur==null||mx==null||cur.length()<1||mx.optInt(0,0)<=cur.optInt(0,0))return blocked(s,"no_spent_first_level_slot");spend(s,"wild_resurgence_slot_recovery",1);spend(s,"wild_shape",1);restoreSpellSlot(s,1,1);return mark(s,"druid_wild_resurgence_shape_to_slot",new JSONObject().put("wild_shape_spent",1).put("slot_level_restored",1));
            }

            // Sorcerer
            if(c.equals("sorcerer")&&a.equals("feiticaria inata")){
                if(!spend(s,"innate_sorcery",1))return blocked(s,"innate_sorcery");
                return mark(s,"sorcerer_innate_sorcery",new JSONObject().put("active",true).put("rounds",10));
            }
            if(c.equals("sorcerer")&&a.equals("criar espaco de magia")){
                if(lvl<2)return blocked(s,"requires_level_2");
                int slot=payload.optInt("spell_slot_level",0);
                int[] cost={0,2,3,5,6,7};
                if(slot<1||slot>5)return pending(s,"spell_slot_level (1 to 5)");
                if(!spend(s,"sorcery_points",cost[slot]))return blocked(s,"sorcery_points");
                restoreSpellSlot(s,slot,1);
                return mark(s,"sorcerer_create_spell_slot",new JSONObject().put("slot_level_created",slot).put("points_spent",cost[slot]));
            }
            if(c.equals("sorcerer")&&a.equals("converter espaco em pontos")){
                if(lvl<2)return blocked(s,"requires_level_2");
                int slot=payload.optInt("spell_slot_level",0);
                if(slot<1||slot>9)return pending(s,"spell_slot_level (1 to 9)");
                JSONObject after=CharacterResourceStateEngine.spendSpellSlot(s,slot);
                if(after.optBoolean("spell_slot_spend_blocked",false))return blocked(s,"spell_slot_level_"+slot);
                s=after;
                int pts=restore(s,"sorcery_points",slot);
                return mark(s,"sorcerer_slot_to_points",new JSONObject().put("slot_level_spent",slot).put("points_restored",pts));
            }
            if(c.equals("sorcerer")&&a.equals("restauracao feiticeira")){
                JSONObject sp=res(s,"sorcery_points");if(sp==null||sp.optInt("current",0)>=sp.optInt("max",0))return blocked(s,"sorcery_points_already_full");int wanted=Math.max(0,payload.optInt("points_to_restore",Math.max(1,level/2)));int possible=Math.min(Math.min(level/2,wanted),sp.optInt("max",0)-sp.optInt("current",0));if(possible<1)return blocked(s,"no_sorcery_points_to_restore");if(!spend(s,"sorcerous_restoration",1))return blocked(s,"sorcerous_restoration");int gain=restore(s,"sorcery_points",possible);return mark(s,"sorcerous_restoration",new JSONObject().put("sorcery_points_restored",gain));
            }
            if(c.equals("sorcerer")&&a.equals("feiticaria inata com pontos")){
                JSONObject innate=res(s,"innate_sorcery");if(innate!=null&&innate.optInt("current",0)>0)return blocked(s,"free_innate_sorcery_still_available");if(level<7)return blocked(s,"requires_level_7");if(!spend(s,"sorcery_points",2))return blocked(s,"sorcery_points");return mark(s,"sorcerer_innate_sorcery_with_points",new JSONObject().put("sorcery_points_spent",2));
            }

            // Warlock
            if(c.equals("warlock")&&a.equals("astucia magica")){
                JSONObject pact=s.optJSONObject("pact_magic");if(pact==null)return blocked(s,"pact_magic");int mx=pact.optInt("max",0),cur=pact.optInt("current",0),gain=Math.min(mx-cur,(mx+1)/2);if(gain<=0)return blocked(s,"no_spent_pact_slots");if(!spend(s,"magical_cunning",1))return blocked(s,"magical_cunning");pact.put("current",cur+gain);s.put("pact_magic",pact);return mark(s,"warlock_magical_cunning",new JSONObject().put("pact_slots_restored",gain));
            }

            // Wizard
            if(c.equals("wizard")&&a.equals("recuperacao arcana"))return recoverSpellSlots(s,"arcane_recovery",(clamp(level)+1)/2,5,payload,"wizard_arcane_recovery");

            // Monk
            if(c.equals("monk")&&a.equals("rajada de golpes")){
                if(lvl<2)return blocked(s,"requires_level_2");
                if(!spend(s,"focus_points",1))return blocked(s,"focus_points");
                return mark(s,"monk_flurry_of_blows",new JSONObject().put("unarmed_strikes",2).put("martial_arts_die",monkDie(lvl)));
            }
            if(c.equals("monk")&&a.equals("defesa paciente")){
                if(lvl<2)return blocked(s,"requires_level_2");
                if(!spend(s,"focus_points",1))return blocked(s,"focus_points");
                return mark(s,"monk_patient_defense",new JSONObject().put("dodge_and_disengage",true));
            }
            if(c.equals("monk")&&a.equals("passo do vento")){
                if(lvl<2)return blocked(s,"requires_level_2");
                if(!spend(s,"focus_points",1))return blocked(s,"focus_points");
                return mark(s,"monk_step_of_the_wind",new JSONObject().put("dash_and_disengage",true).put("jump_distance_doubled",true));
            }
            if(c.equals("monk")&&a.equals("metabolismo incomum")){
                if(lvl<2)return blocked(s,"requires_level_2");if(!payload.has("martial_arts_roll"))return pending(s,"martial_arts_roll");int faces=monkDieFaces(level),roll=boundedRoll(payload.optInt("martial_arts_roll"),1,faces);if(!spend(s,"uncanny_metabolism",1))return blocked(s,"uncanny_metabolism");JSONObject fp=res(s,"focus_points");if(fp!=null)fp.put("current",fp.optInt("max",level));int maxHp=Math.max(1,s.optInt("max_hp",1)),hp=Math.max(0,s.optInt("hp",maxHp)),heal=Math.min(maxHp-hp,clamp(level)+roll);s.put("hp",hp+heal);return mark(s,"monk_uncanny_metabolism",new JSONObject().put("martial_arts_roll",roll).put("focus_restored_to_max",true).put("healing",heal));
            }
            if(c.equals("monk")&&a.equals("ataque atordoante")){
                if(lvl<5)return blocked(s,"requires_level_5");
                if(!spend(s,"focus_points",1))return blocked(s,"focus_points");
                return mark(s,"monk_stunning_strike",new JSONObject().put("condition","Stunned").put("save_ability","Constitution"));
            }

            // Paladin
            if(c.equals("paladin")&&a.equals("imposicao das maos")){
                int points=payload.optInt("healing_points",0);if(points<1)return pending(s,"healing_points");JSONObject pool=res(s,"lay_on_hands");if(pool==null||pool.optInt("current",0)<points)return blocked(s,"lay_on_hands");int maxHp=Math.max(1,s.optInt("max_hp",1)),hp=Math.max(0,s.optInt("hp",maxHp)),healed=Math.min(points,maxHp-hp);if(healed<1)return blocked(s,"target_or_self_already_full_hp");spend(s,"lay_on_hands",healed);s.put("hp",hp+healed);return mark(s,"paladin_lay_on_hands",new JSONObject().put("healing",healed));
            }
            if(c.equals("paladin")&&a.equals("golpe divino gratuito")){
                if(lvl<2)return blocked(s,"requires_level_2");
                if(!spend(s,"free_divine_smite",1))return blocked(s,"free_divine_smite");
                return mark(s,"paladin_free_divine_smite",new JSONObject().put("spell","Golpe Divino"));
            }
            if(c.equals("paladin")&&a.equals("convocar montaria gratuito")){
                if(lvl<5)return blocked(s,"requires_level_5");
                if(!spend(s,"free_find_steed",1))return blocked(s,"free_find_steed");
                return mark(s,"paladin_free_find_steed",new JSONObject().put("spell","Convocar Montaria"));
            }
            if(c.equals("paladin")&&a.equals("canalizar divindade")){
                if(lvl<3)return blocked(s,"requires_level_3");
                if(!spend(s,"channel_divinity",1))return blocked(s,"channel_divinity");
                return mark(s,"paladin_channel_divinity",new JSONObject().put("channel_divinity_spent",1));
            }

            // Ranger
            if(c.equals("ranger")&&a.equals("marca do predador gratuita")){
                if(!spend(s,"favored_enemy_free_hunters_mark",1))return blocked(s,"favored_enemy_free_hunters_mark");
                return mark(s,"ranger_free_hunters_mark",new JSONObject().put("spell","Marca do Predador"));
            }

            // Rogue
            if(c.equals("rogue")&&a.equals("golpe astuto")){
                if(lvl<5)return blocked(s,"requires_level_5");
                String opt=n(payload.optString("option","derrubar"));
                return mark(s,"rogue_cunning_strike",new JSONObject().put("option",opt).put("sneak_dice_cost",1));
            }

            return s.put("class_action_resolved",false).put("class_action_reason","unknown_or_profile_action").put("class_action",action);
        }catch(Exception e){return s;}
    }

    public static JSONObject recoverSpellSlots(JSONObject s,String resourceKey,int budget,int maxSlot,JSONObject payload,String event){try{
        if(payload==null||payload.optJSONArray("slots")==null)return pending(s,"slots [{level,count}, ...]");JSONArray req=payload.optJSONArray("slots");int total=0;for(int i=0;i<req.length();i++){JSONObject x=req.optJSONObject(i);if(x==null)continue;int l=x.optInt("level"),cnt=x.optInt("count",1);if(l<1||l>maxSlot||cnt<1)return blocked(s,"invalid_slot_selection");total+=l*cnt;}if(total<1||total>budget)return blocked(s,"slot_level_budget_"+budget);JSONObject rr=res(s,resourceKey);if(rr==null||rr.optInt("current",0)<1)return blocked(s,resourceKey);JSONArray cur=s.optJSONArray("spell_slots_current"),mx=s.optJSONArray("spell_slots_max");if(cur==null||mx==null)return blocked(s,"spell_slots_unavailable");for(int i=0;i<req.length();i++){JSONObject x=req.optJSONObject(i);int l=x.optInt("level"),cnt=x.optInt("count",1),idx=l-1;int can=Math.max(0,mx.optInt(idx,0)-cur.optInt(idx,0));if(can<cnt)return blocked(s,"requested_unspent_slot_level_"+l);}spend(s,resourceKey,1);for(int i=0;i<req.length();i++){JSONObject x=req.optJSONObject(i);int l=x.optInt("level"),cnt=x.optInt("count",1),idx=l-1;cur.put(idx,cur.optInt(idx,0)+cnt);}s.put("spell_slots_current",cur);return mark(s,event,new JSONObject().put("slot_level_budget_used",total).put("selections",req));
    }catch(Exception e){return s;}}

    private static void appendPendingChoices(JSONObject s,String c,int lvl)throws Exception{
        JSONArray previous=s.optJSONArray("class_choices_pending"),pending=new JSONArray();
        // Preserve unrelated incomplete selections but remove previously pending
        // first-level entries only after their structured and validated choice.
        if(previous!=null)for(int i=0;i<previous.length();i++){
            String key=previous.optString(i);
            if(("ordem_primal".equals(key)||"ordem_divina".equals(key)||
                "fighting_style".equals(key)||"invocations".equals(key)) &&
                Character2024StartingClassChoices.resolved(s,key))continue;
            add(pending,key);
        }
        if(c.equals("druid")&&lvl>=1&&!Character2024StartingClassChoices.resolved(s,"ordem_primal"))
            add(pending,"ordem_primal");
        if(c.equals("wizard")&&lvl>=2)add(pending,"academic_expertise_skill");
        if(c.equals("fighter")&&lvl>=1&&!Character2024StartingClassChoices.resolved(s,"fighting_style"))
            add(pending,"fighting_style");
        if(c.equals("ranger")&&lvl>=2)add(pending,"fighting_style");
        if(c.equals("cleric")&&lvl>=1&&!Character2024StartingClassChoices.resolved(s,"ordem_divina"))
            add(pending,"ordem_divina");
        if(c.equals("warlock")&&lvl>=1&&!Character2024StartingClassChoices.resolved(s,"invocations"))
            add(pending,"invocations");
        s.put("class_choices_pending",pending);
    }
    private static void add(JSONArray a,String v){for(int i=0;i<a.length();i++)if(v.equals(a.optString(i)))return;a.put(v);}
    private static JSONObject resource(JSONObject all,String key,int max,String recharge)throws Exception{JSONObject r=all.optJSONObject(key);if(r==null)r=new JSONObject().put("current",max);int cur=Math.max(0,Math.min(max,r.optInt("current",max)));r.put("max",max).put("current",cur).put("recharge",recharge).put("source",SOURCE);all.put(key,r);return r;}
    private static JSONObject res(JSONObject s,String key){JSONObject all=s.optJSONObject("class_resources");return all==null?null:all.optJSONObject(key);}
    private static boolean spend(JSONObject s,String key,int n)throws Exception{JSONObject r=res(s,key);if(r==null||r.optInt("current",0)<n)return false;r.put("current",r.optInt("current",0)-n);return true;}
    private static int restore(JSONObject s,String key,int n)throws Exception{JSONObject r=res(s,key);if(r==null)return 0;int before=r.optInt("current",0),after=Math.min(r.optInt("max",before),before+Math.max(0,n));r.put("current",after);return after-before;}
    private static void partial(JSONObject all,String key,int amount)throws Exception{JSONObject r=all.optJSONObject(key);if(r==null)return;int cur=r.optInt("current",0),mx=r.optInt("max",cur);r.put("current",Math.min(mx,cur+Math.max(0,amount)));}
    private static void full(JSONObject all,String key)throws Exception{JSONObject r=all.optJSONObject(key);if(r==null)return;r.put("current",r.optInt("max",0));}
    private static boolean restoreSpellSlot(JSONObject s,int level,int count)throws Exception{JSONArray cur=s.optJSONArray("spell_slots_current"),mx=s.optJSONArray("spell_slots_max");if(cur==null||mx==null||level<1||level>cur.length())return false;int i=level-1,can=Math.max(0,mx.optInt(i)-cur.optInt(i));if(can<count)return false;cur.put(i,cur.optInt(i)+count);s.put("spell_slots_current",cur);return true;}
    private static JSONObject mark(JSONObject s,String event,JSONObject effect)throws Exception{return s.put("class_action_resolved",true).put("class_action_event",event).put("class_action_effect",effect).put("class_action_auto_rolls",false);}
    private static JSONObject pending(JSONObject s,String need)throws Exception{return s.put("class_action_resolved",false).put("class_action_status","requires_player_or_gm_resolution").put("class_action_required_input",need).put("class_action_auto_rolls",false);}
    private static JSONObject blocked(JSONObject s,String why)throws Exception{return s.put("class_action_resolved",false).put("class_action_status","blocked").put("class_action_reason",why).put("class_action_auto_rolls",false);}
    private static int rageUses(int l){return l>=17?6:l>=12?5:l>=6?4:l>=3?3:2;}private static int rageDamage(int l){return l>=16?4:l>=9?3:2;}private static String bardDie(int l){return l>=15?"d12":l>=10?"d10":l>=5?"d8":"d6";}private static int clericChannel(int l){return l>=18?4:l>=6?3:2;}private static int druidWildShape(int l){return l>=17?4:l>=6?3:2;}private static int rangerFavoredEnemy(int l){return l>=17?6:l>=13?5:l>=9?4:l>=5?3:2;}private static int fighterSecondWind(int l){return l>=10?4:l>=4?3:2;}private static String monkDie(int l){return l>=17?"d12":l>=11?"d10":l>=5?"d8":"d6";}private static int monkDieFaces(int l){return l>=17?12:l>=11?10:l>=5?8:6;}private static double monkMove(int l){return l>=18?9:l>=14?7.5:l>=10?6:l>=6?4.5:l>=2?3:0;}
    private static int boundedRoll(int v,int min,int max){return Math.max(min,Math.min(max,v));}private static int clamp(int l){return Math.max(1,Math.min(20,l));}
    private static JSONObject copy(JSONObject o){try{return o==null?new JSONObject():new JSONObject(o.toString());}catch(Exception e){return new JSONObject();}}
    private static String key(String cls){String x=n(cls);if(x.contains("barbar"))return "barbarian";if(x.contains("bardo"))return "bard";if(x.contains("bruxo"))return "warlock";if(x.contains("clerigo"))return "cleric";if(x.contains("druida"))return "druid";if(x.contains("feiticeiro"))return "sorcerer";if(x.contains("guardiao")||x.contains("patrulheiro"))return "ranger";if(x.contains("guerreiro"))return "fighter";if(x.contains("paladino"))return "paladin";if(x.contains("ladino"))return "rogue";if(x.contains("mago"))return "wizard";if(x.contains("monge"))return "monk";return x;}
    private static String n(String s){String x=Normalizer.normalize(s==null?"":s.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}+","");return x.replaceAll("[^a-z0-9]+"," ").trim();}
}
