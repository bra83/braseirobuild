package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/** Regras mecânicas determinísticas que não devem ser delegadas ao texto da IA. */
public final class DndRulesEngine {
    public static final String PROFILE_2014="5e 2014";
    public static final String PROFILE_2024="5.5e revisada (2024/2025)";
    public static final String PROFILE_HYBRID="Híbrido compatível";

    public int modifier(int score){return (int)Math.floor((score-10)/2.0);}
    public int proficiencyBonus(int level){return 2+(clamp(level,1,20)-1)/4;}

    public int classHitDie(String cls){
        return DndClassHitDie.forClass(cls);
    }

    public int suggestedMaxHp(String cls,int level,int constitution){
        int die=classHitDie(cls),con=modifier(constitution),lvl=clamp(level,1,20);
        return Math.max(lvl,die+con+(lvl-1)*Math.max(1,die/2+1+con));
    }

    public int suggestedArmorClass(String cls,int dexterity,int constitution,int wisdom){
        int dex=modifier(dexterity),con=modifier(constitution),wis=modifier(wisdom);String c=norm(cls);
        if(c.contains("barbar"))return 10+dex+con;
        if(c.contains("monge"))return 10+dex+wis;
        if(c.contains("guerreiro")||c.contains("paladino")||c.contains("clerigo"))return 18;
        if(c.contains("cacador de sangue")||c.contains("cavaleiro da morte"))return 14+Math.min(2,dex);
        if(c.contains("patrulheiro")||c.contains("guardiao")||c.contains("cozinheiro"))return 14+Math.min(2,dex);
        if(c.contains("druida"))return 13+Math.min(2,dex);
        if(c.contains("ladino")||c.contains("bardo")||c.contains("bruxo"))return 11+dex;
        return 10+dex;
    }

    /** Calcula a CA a partir do equipamento realmente registrado na ficha. */
    public int armorClassFromEquipment(String equipment,String cls,int dexterity,int constitution,int wisdom){
        String e=norm(equipment),c=norm(cls);int dex=modifier(dexterity),con=modifier(constitution),wis=modifier(wisdom);
        boolean shield=e.contains("escudo")||e.contains("shield");int ac;
        // Armaduras pesadas: Destreza não altera a CA.
        if(e.contains("plate armor")||containsArmorPhrase(e,"armadura de placas")||containsArmorPhrase(e,"placas"))ac=18;
        else if(e.contains("splint")||e.contains("talas"))ac=17;
        else if(e.contains("chain mail")||e.contains("cota de malha"))ac=16;
        else if(e.contains("ring mail")||e.contains("cota de aneis")||e.contains("cota de anéis"))ac=14;
        // Armaduras médias: máximo +2 de Destreza.
        else if(e.contains("half plate")||e.contains("meia armadura"))ac=15+Math.min(2,dex);
        else if(e.contains("breastplate")||e.contains("peitoral"))ac=14+Math.min(2,dex);
        else if(e.contains("scale mail")||e.contains("brunea")||e.contains("escamas"))ac=14+Math.min(2,dex);
        else if(e.contains("chain shirt")||e.contains("camisao de malha")||e.contains("camisão de malha"))ac=13+Math.min(2,dex);
        else if(e.contains("hide armor")||e.contains("armadura de peles")||e.contains("peles"))ac=12+Math.min(2,dex);
        // Armaduras leves.
        else if(e.contains("studded leather")||e.contains("couro batido")||e.contains("couro cravejado"))ac=12+dex;
        else if(e.contains("leather armor")||e.contains("armadura de couro"))ac=11+dex;
        else if(e.contains("padded")||e.contains("acolchoada"))ac=11+dex;
        else if(c.contains("barbar"))ac=10+dex+con;
        else if(c.contains("monge")&&!shield)ac=10+dex+wis;
        else ac=10+dex;
        if(shield)ac+=2;
        return Math.max(1,ac);
    }
    private static boolean containsArmorPhrase(String normalizedEquipment,String phrase){return normalizedEquipment.contains(norm(phrase));}

    public int spellSaveDc(int abilityScore,int level,boolean proficient){return 8+modifier(abilityScore)+(proficient?proficiencyBonus(level):0);}
    public int spellAttackBonus(int abilityScore,int level,boolean proficient){return modifier(abilityScore)+(proficient?proficiencyBonus(level):0);}


    /** Bônus de salvaguarda com proficiência declarada na ficha. */
    public int savingThrowBonus(JSONObject hero,String ability){
        JSONObject h=hero==null?new JSONObject():hero;JSONObject sheet=embeddedSheet(h);int bonus=modifier(abilityScore(h,ability));
        String saves=h.optString("saving_throws",h.optString("saves",h.optString("proficiencies","")));if(saves.trim().length()==0)saves=sheet.optString("saving_throws",sheet.optString("saves",sheet.optString("proficiencies","")));
        if(hasProficiency(saves,ability))bonus+=proficiencyBonus(h.optInt("level",sheet.optInt("level",1)));
        return bonus-exhaustionD20Penalty(h);
    }

    public int initiativeBonus(JSONObject hero){return modifier(hero==null?10:hero.optInt("dex",10))-exhaustionD20Penalty(hero);}
    public int passiveScore(JSONObject hero,String ability,String skill){return 10+checkBonus(hero,ability,skill);}
    /**
     * 2014 SRD5 "Passive Checks": passive total includes +5 with advantage,
     * -5 with disadvantage, and no modifier when both cancel. Also preserves
     * the three-argument call sites already in use.
     * Source: BTMorton/dnd-5e-srd json/06 mechanics.json, SHA
     * 004cc01b5e6228b7eafcae29830cd2d9d47584db (OGL 1.0a).
     */
    public int passiveScore(JSONObject hero,String ability,String skill,
                            boolean advantage,boolean disadvantage){
        int base=passiveScore(hero,ability,skill);
        return base+(advantage==disadvantage?0:(advantage?5:-5));
    }

    public int carryingCapacity(int strength){return Math.max(0,strength)*15;}
    public int pushDragLiftCapacity(int strength){return carryingCapacity(strength)*2;}
    /** SRD5 (2014): Small/Medium normal; Tiny half; each step over Medium doubles. */
    public int carryingCapacity(int strength,String creatureSize){
        String size=norm(creatureSize);
        int base=carryingCapacity(strength);
        if(size.equals("tiny")||size.equals("miudo")||size.equals("minusculo"))return base/2;
        if(size.equals("small")||size.equals("pequeno")||
           size.equals("medium")||size.equals("medio"))return base;
        if(size.equals("large")||size.equals("grande"))return base*2;
        if(size.equals("huge")||size.equals("enorme"))return base*4;
        if(size.equals("gargantuan")||size.equals("colossal"))return base*8;
        throw new IllegalArgumentException("creature_size_required");
    }

    public int pushDragLiftCapacity(int strength,String creatureSize){
        return carryingCapacity(strength,creatureSize)*2;
    }

    public int concentrationDc(int damage){return Math.max(10,Math.max(0,damage)/2);}
    /** Revisão 2024 limita a CD de concentração a 30; 2014 preserva a fórmula histórica. */
    public int concentrationDc(int damage,String profile){int dc=concentrationDc(damage);return is2024(profile)?Math.min(30,dc):dc;}

    /** Resolve vantagem e desvantagem sem permitir que ambas sejam acumuladas. */
    public int chooseD20(int first,int second,boolean advantage,boolean disadvantage){
        int a=clamp(first,1,20),b=clamp(second,1,20);
        if(advantage==disadvantage)return a;
        return advantage?Math.max(a,b):Math.min(a,b);
    }

    public JSONObject resolveAttack(int d20,int attackBonus,int targetAc,boolean advantage,boolean disadvantage){
        return resolveAttack(d20,d20,attackBonus,targetAc,advantage,disadvantage);
    }
    public JSONObject resolveAttack(int firstD20,int secondD20,int attackBonus,int targetAc,boolean advantage,boolean disadvantage){
        JSONObject out=new JSONObject();try{
            int used=chooseD20(firstD20,secondD20,advantage,disadvantage);int total=used+attackBonus;
            boolean critical=used==20,automaticMiss=used==1,hit=!automaticMiss&&(critical||total>=targetAc);
            out.put("first_roll",clamp(firstD20,1,20)).put("second_roll",clamp(secondD20,1,20)).put("roll",used).put("attack_bonus",attackBonus).put("total",total).put("target_ac",targetAc).put("hit",hit).put("critical",critical).put("automatic_miss",automaticMiss).put("advantage",advantage&&!disadvantage).put("disadvantage",disadvantage&&!advantage);
        }catch(Exception ignored){AppLog.ignored("DndRulesEngine","Falha ao montar resultado mecânico local.",ignored);}
        return out;
    }

    /** Aplica dano com PV temporários e modificadores de tipo declarados. */
    public JSONObject applyDamage(int currentHp,int maxHp,int tempHp,int amount,String damageType,String resistances,String vulnerabilities,String immunities){
        JSONObject out=new JSONObject();try{
            int raw=Math.max(0,amount),applied=raw;
            // RUN192B: immunity takes precedence; resistance + vulnerability cancel exactly,
            // including odd damage values. Saturate doubling instead of overflowing int.
            applied=DamageMitigationRules.apply(raw,DamageTypeNormalizer.matches(immunities,damageType),
                DamageTypeNormalizer.matches(resistances,damageType),DamageTypeNormalizer.matches(vulnerabilities,damageType));
            int tempBefore=Math.max(0,tempHp),absorbed=Math.min(tempBefore,applied),remaining=applied-absorbed;
            int hpBefore=clamp(currentHp,0,Math.max(1,maxHp));int hpAfter=Math.max(0,hpBefore-remaining);
            out.put("raw_damage",raw).put("applied_damage",applied).put("absorbed_by_temp_hp",absorbed).put("temp_hp_after",tempBefore-absorbed).put("hp_before",hpBefore).put("hp_after",hpAfter).put("hp_delta",hpAfter-hpBefore).put("damage_type",damageType==null?"":damageType);
        }catch(Exception ignored){AppLog.ignored("DndRulesEngine","Falha ao montar resultado mecânico local.",ignored);}
        return out;
    }

    public JSONObject applyHealing(int currentHp,int maxHp,int amount){
        JSONObject out=new JSONObject();try{int before=clamp(currentHp,0,Math.max(1,maxHp));int after=CombatTurnGuard.safeHealing(before,maxHp,amount);out.put("hp_before",before).put("hp_after",after).put("hp_delta",after-before);}catch(Exception ignored){AppLog.ignored("DndRulesEngine","Falha ao montar resultado mecânico local.",ignored);}return out;
    }

    public JSONObject resolveDeathSave(int d20,int successes,int failures){
        JSONObject out=new JSONObject();try{
            int roll=clamp(d20,1,20),s=clamp(successes,0,3),f=clamp(failures,0,3);
            boolean regained=false;
            if(roll==20){s=0;f=0;regained=true;out.put("regains_hp",1);}else if(roll==1)f=Math.min(3,f+2);else if(roll>=10)s=Math.min(3,s+1);else f=Math.min(3,f+1);
            out.put("roll",roll).put("successes",s).put("failures",f).put("stable",!regained&&s>=3).put("dead",!regained&&f>=3).put("death_saves_reset",regained);
        }catch(Exception ignored){AppLog.ignored("DndRulesEngine","Falha ao montar resultado mecânico local.",ignored);}return out;
    }

    public boolean validCondition(String value){String n=norm(value);String[] allowed={"agarrado","amedrontado","aturdido","caido","cego","enfeiticado","envenenado","impedido","incapacitado","inconsciente","invisivel","paralisado","petrificado","surdo","exaustao"};for(String condition:allowed)if(n.equals(condition))return true;return false;}

    /** Tabela determinística de espaços de magia para conjuradores completos. */
    public JSONArray fullCasterSlots(int level){
        int[][] rows={{2,0,0,0,0,0,0,0,0},{3,0,0,0,0,0,0,0,0},{4,2,0,0,0,0,0,0,0},{4,3,0,0,0,0,0,0,0},{4,3,2,0,0,0,0,0,0},{4,3,3,0,0,0,0,0,0},{4,3,3,1,0,0,0,0,0},{4,3,3,2,0,0,0,0,0},{4,3,3,3,1,0,0,0,0},{4,3,3,3,2,0,0,0,0},{4,3,3,3,2,1,0,0,0},{4,3,3,3,2,1,0,0,0},{4,3,3,3,2,1,1,0,0},{4,3,3,3,2,1,1,0,0},{4,3,3,3,2,1,1,1,0},{4,3,3,3,2,1,1,1,0},{4,3,3,3,2,1,1,1,1},{4,3,3,3,3,1,1,1,1},{4,3,3,3,3,2,1,1,1},{4,3,3,3,3,2,2,1,1}};
        JSONArray out=new JSONArray();int[] row=rows[clamp(level,1,20)-1];for(int value:row)out.put(value);return out;
    }

    public JSONArray casterSlots(String progression,int level){
        String p=norm(progression);int effective=clamp(level,1,20);if(p.contains("half")||p.contains("meio"))effective=Math.max(1,(effective+1)/2);else if(p.contains("third")||p.contains("terco"))effective=Math.max(1,(effective+2)/3);return fullCasterSlots(effective);
    }

    public JSONObject restPreview(JSONObject hero,boolean longRest){
        JSONObject out=new JSONObject();try{
            JSONObject h=hero==null?new JSONObject():hero;JSONObject sheet=embeddedSheet(h);String profile=heroRulesProfile(h,sheet);
            int hp=h.optInt("hp",sheet.optInt("hp",0)),max=Math.max(1,h.optInt("max_hp",sheet.optInt("max_hp",1))),temp=Math.max(0,sheet.optInt("temp_hp",h.optInt("temp_hp",0))),level=Math.max(1,h.optInt("level",sheet.optInt("level",1)));
            out.put("type",longRest?"long":"short").put("rules_profile",profile).put("hp_before",hp).put("hp_after",longRest?max:hp).put("temp_hp_after",longRest?0:temp);
            if(longRest){
                out.put("requires_positive_hp_to_start",true).put("eligible_by_hp",hp>0).put("hit_dice_recovered",is2024(profile)?level:Math.max(1,level/2));
                int ex=exhaustionLevel(h,sheet);out.put("exhaustion_before",ex).put("exhaustion_after",is2024(profile)?Math.max(0,ex-1):ex);
                JSONArray maxima=sheet.optJSONArray("spell_slots_max");if(maxima!=null)out.put("spell_slots",new JSONArray(maxima.toString()));else out.put("spell_slot_recovery_requires_structured_resource_state",true);
            }else out.put("requires_hit_dice_roll",true);
        }catch(Exception ignored){AppLog.ignored("DndRulesEngine","Falha ao montar resultado mecânico local.",ignored);}return out;
    }

    public JSONObject turnBudget(boolean actionUsed,boolean bonusUsed,boolean reactionUsed,double movementUsed,double speed){
        JSONObject out=new JSONObject();try{double safeSpeed=Math.max(0d,speed),used=Math.max(0d,movementUsed);out.put("action_available",!actionUsed).put("bonus_action_available",!bonusUsed).put("reaction_available",!reactionUsed).put("movement_speed",safeSpeed).put("movement_used",Math.min(safeSpeed,used)).put("movement_remaining",Math.max(0d,safeSpeed-used)).put("can_opportunity_attack",!reactionUsed);}catch(Exception ignored){AppLog.ignored("DndRulesEngine","Falha ao calcular economia de ações.",ignored);}return out;
    }
    public int coverArmorBonus(String cover){return coverAcBonus(cover);}
    public int coverAcBonus(String cover){String n=norm(cover);if(n.contains("three quarters")||n.contains("tres quartos"))return 5;if(n.contains("half")||n.contains("meia")||n.contains("meio"))return 2;return 0;}
    public int coverDexSaveBonus(String cover){return coverAcBonus(cover);}
    public boolean hasTotalCover(String cover){String n=norm(cover);return n.contains("total")||n.contains("completa");}
    public boolean withinRange(double distance,double normalRange,double longRange,boolean longRangeAllowed){return distance>=0d&&distance<=(longRangeAllowed?Math.max(normalRange,longRange):normalRange);}
    public boolean grappleSizeAllowed(String actorSize,String targetSize){return Math.abs(sizeRank(actorSize)-sizeRank(targetSize))<=1||sizeRank(actorSize)>sizeRank(targetSize);}
    public int criticalDamageDice(int normalDiceCount){return Math.max(0,normalDiceCount)*2;}
    public JSONObject resolveAbilityCheck(int d20,int modifier,int dc,boolean advantage,boolean disadvantage){
        return resolveAbilityCheck(d20,d20,modifier,dc,advantage,disadvantage);
    }
    /** Testes de habilidade/perícia não têm sucesso/falha automática em 20/1 natural. */
    public JSONObject resolveAbilityCheck(int firstD20,int secondD20,int modifier,int dc,boolean advantage,boolean disadvantage){
        JSONObject out=new JSONObject();try{int used=chooseD20(firstD20,secondD20,advantage,disadvantage),total=used+modifier;out.put("first_roll",clamp(firstD20,1,20)).put("second_roll",clamp(secondD20,1,20)).put("natural_roll",used).put("modifier",modifier).put("total",total).put("dc",Math.max(0,dc)).put("success",total>=dc).put("critical",false).put("critical_failure",false).put("natural_twenty",used==20).put("natural_one",used==1).put("advantage",advantage&&!disadvantage).put("disadvantage",disadvantage&&!advantage);}catch(Exception ignored){AppLog.ignored("DndRulesEngine","Falha ao resolver teste de habilidade.",ignored);}return out;
    }
    public JSONObject resolveSavingThrow(int natural,int modifier,int dc,boolean advantage,boolean disadvantage){
        return resolveSavingThrow(natural,natural,modifier,dc,advantage,disadvantage);
    }
    /** Salvaguardas comuns de D&D 5e não transformam 20/1 natural em sucesso/falha automática. */
    public JSONObject resolveSavingThrow(int firstD20,int secondD20,int modifier,int dc,boolean advantage,boolean disadvantage){
        JSONObject out=new JSONObject();try{int used=chooseD20(firstD20,secondD20,advantage,disadvantage),total=used+modifier;out.put("first_roll",clamp(firstD20,1,20)).put("second_roll",clamp(secondD20,1,20)).put("natural_roll",used).put("modifier",modifier).put("total",total).put("dc",Math.max(0,dc)).put("success",total>=dc).put("critical",false).put("critical_failure",false).put("natural_twenty",used==20).put("natural_one",used==1).put("advantage",advantage&&!disadvantage).put("disadvantage",disadvantage&&!advantage);}catch(Exception ignored){AppLog.ignored("DndRulesEngine","Falha ao resolver salvaguarda.",ignored);}return out;
    }
    public JSONObject resolveConcentration(int damage,int natural,int constitutionSaveBonus){return resolveConcentration(damage,natural,constitutionSaveBonus,PROFILE_2014);}
    public JSONObject resolveConcentration(int damage,int natural,int constitutionSaveBonus,String profile){JSONObject out=resolveSavingThrow(natural,constitutionSaveBonus,concentrationDc(damage,profile),false,false);try{out.put("damage",Math.max(0,damage)).put("concentration_maintained",out.optBoolean("success",false)).put("rules_profile",canonicalProfile(profile));}catch(Exception ignored){AppLog.ignored("DndRulesEngine","Falha ao resolver concentração.",ignored);}return out;}
    public JSONObject changeBoundedResource(int current,int minimum,int maximum,int delta,boolean canSpend,boolean canRecover){
        JSONObject out=new JSONObject();try{int min=Math.min(minimum,maximum),max=Math.max(minimum,maximum),before=clamp(current,min,max);boolean allowed=(delta<0?canSpend:delta>0?canRecover:true)&&before+delta>=min&&before+delta<=max;out.put("current_before",before).put("minimum",min).put("maximum",max).put("requested_delta",delta).put("allowed",allowed).put("current_after",allowed?before+delta:before).put("reason",allowed?"":(delta<0&&!canSpend?"recurso não pode ser gasto":delta>0&&!canRecover?"recurso não pode ser recuperado":"quantidade fora dos limites"));}catch(Exception ignored){AppLog.ignored("DndRulesEngine","Falha ao alterar recurso limitado.",ignored);}return out;
    }
    public JSONObject encumbrance(int strength,double carriedWeight,boolean variant){JSONObject out=new JSONObject();try{double carrying=carryingCapacity(strength),push=pushDragLiftCapacity(strength),weight=Math.max(0d,carriedWeight);String state=weight>push?"immobile":weight>carrying?"over_capacity":variant&&weight>strength*10d?"heavily_encumbered":variant&&weight>strength*5d?"encumbered":"normal";out.put("weight",weight).put("carrying_capacity",carrying).put("push_drag_lift",push).put("state",state).put("movement_penalty",state.equals("heavily_encumbered")?20:state.equals("encumbered")?10:0);}catch(Exception ignored){AppLog.ignored("DndRulesEngine","Falha ao calcular carga.",ignored);}return out;}

    public JSONObject mechanicsCatalog(String profile){
        JSONObject out=new JSONObject();try{
            JSONArray progression=new JSONArray();for(int level=1;level<=20;level++)progression.put(proficiencyBonus(level));out.put("profile",profile).put("proficiency_by_level",progression);
            out.put("implemented",new JSONArray().put("action_economy").put("ability_checks").put("saving_throws").put("initiative").put("passive_scores").put("attacks").put("critical_damage_dice").put("damage_resistance_vulnerability_immunity").put("healing").put("concentration").put("death_saves").put("condition_effects_core").put("cover").put("range").put("grapple_size").put("carrying_capacity").put("encumbrance").put("bounded_resources").put("spell_slots").put("rest_preview").put("character_validation"));
            out.put("guard",rulesGuard(profile)).put("authority","Somente estes cálculos determinísticos podem produzir alterações automáticas; características específicas de classe, magia, monstro ou item exigem fonte local e resolução estruturada.");
        }catch(Exception ignored){AppLog.ignored("DndRulesEngine","Falha ao montar resultado mecânico local.",ignored);}return out;
    }

    public int checkBonus(JSONObject hero,String ability,String skill){
        JSONObject h=hero==null?new JSONObject():hero,sheet=embeddedSheet(h);int score=abilityScore(h,ability);int result=modifier(score);
        if(skill!=null&&skill.trim().length()>0){int rank=0;rank=Math.max(rank,proficiencyRank(h.optString("skills"),skill));rank=Math.max(rank,proficiencyRank(h.optString("proficiencies"),skill));rank=Math.max(rank,proficiencyRank(sheet.optString("skills"),skill));rank=Math.max(rank,proficiencyRank(sheet.optString("proficiencies"),skill));result+=rank*proficiencyBonus(h.optInt("level",sheet.optInt("level",1)));}
        return result;
    }
    /** Bônus aplicado a uma rolagem de D20 Test; Exaustão 2024 reduz o resultado em 2 por nível. */
    public int d20CheckBonus(JSONObject hero,String ability,String skill){return checkBonus(hero,ability,skill)-exhaustionD20Penalty(hero);}
    public int exhaustionD20Penalty(JSONObject hero){JSONObject h=hero==null?new JSONObject():hero;JSONObject sheet=embeddedSheet(h);return is2024(heroRulesProfile(h,sheet))?Math.max(0,exhaustionLevel(h,sheet))*2:0;}
    public int exhaustionSpeedPenaltyFeet(JSONObject hero){JSONObject h=hero==null?new JSONObject():hero;JSONObject sheet=embeddedSheet(h);return is2024(heroRulesProfile(h,sheet))?Math.max(0,exhaustionLevel(h,sheet))*5:0;}

    public int abilityScore(JSONObject hero,String ability){
        JSONObject h=hero==null?new JSONObject():hero,sheet=embeddedSheet(h);String a=norm(ability);
        // Standard SRD ability identifiers (STR/DEX/CON/INT/WIS/CHA)
        // must resolve as well as English/PT-BR long-form names.
        if(a.equals("str")||a.startsWith("for")||a.contains("strength"))
            return h.has("str")?h.optInt("str",10):sheet.optInt("str",10);
        if(a.equals("dex")||a.startsWith("des")||a.contains("dexterity"))
            return h.has("dex")?h.optInt("dex",10):sheet.optInt("dex",10);
        if(a.equals("con")||a.startsWith("const")||a.contains("constitution"))
            return h.has("con")?h.optInt("con",10):sheet.optInt("con",10);
        if(a.equals("int")||a.startsWith("int")||a.contains("intelligence"))
            return h.has("int_score")?h.optInt("int_score",10):
                   h.has("int")?h.optInt("int",10):
                   sheet.has("int_score")?sheet.optInt("int_score",10):sheet.optInt("int",10);
        if(a.equals("wis")||a.startsWith("sab")||a.contains("wisdom"))
            return h.has("wis")?h.optInt("wis",10):sheet.optInt("wis",10);
        if(a.equals("cha")||a.startsWith("car")||a.contains("charisma"))
            return h.has("cha")?h.optInt("cha",10):sheet.optInt("cha",10);
        return 10;
    }

    public String validateCharacter(String profile,String species,String cls,String background,int level,int[] scores,int hp,int maxHp,int ac){
        if(level<1||level>20)return "O nível deve ficar entre 1 e 20.";
        if(species==null||species.trim().length()==0)return "Escolha uma espécie.";
        if(cls==null||cls.trim().length()==0)return "Escolha uma classe.";
        if(background==null||background.trim().length()==0)return "Escolha um antecedente.";
        for(int score:scores)if(score<3||score>20)return "Atributos devem ficar entre 3 e 20 antes de bônus temporários.";
        if(maxHp<1)return "PV máximo deve ser maior que zero.";
        if(hp<0||hp>maxHp)return "PV atual deve ficar entre zero e o PV máximo.";
        if(ac<1||ac>30)return "A CA informada parece inválida.";
        if(is2024(profile)&&(norm(species).contains("meio elfo")||norm(species).contains("meio orc")))return "Meio-elfo e meio-orc pertencem ao perfil 2014 deste app. No perfil revisado, escolha uma espécie revisada ou use o modo híbrido explicitamente.";
        return null;
    }

    public boolean is2024(String profile){String canonical=canonicalProfile(profile);return PROFILE_2024.equals(canonical);}
    public boolean isHybrid(String profile){return PROFILE_HYBRID.equals(canonicalProfile(profile));}

    /** Preserva o perfil híbrido inclusive quando backups antigos usam grafias diferentes. */
    public static String canonicalProfile(String profile){
        if(profile==null)return PROFILE_2014;
        String value=profile.trim();
        String folded=Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT);
        if(folded.contains("hibrid")||folded.contains("hybrid"))return PROFILE_HYBRID;
        if(folded.startsWith("5.5")||folded.contains("2024")||folded.contains("2025")||folded.contains("revisad"))return PROFILE_2024;
        if(folded.contains("5e")||folded.contains("2014"))return PROFILE_2014;
        return value.length()==0?PROFILE_2014:value;
    }

    public String rulesGuard(String profile){
        if(is2024(profile))return "Use exclusivamente a revisão 2024/2025 selecionada. A origem do personagem reúne antecedente e espécie; não transfira automaticamente traços raciais ou antecedentes da edição 2014.";
        if(isHybrid(profile))return "O modo híbrido é opcional: identifique claramente qualquer regra importada de outra versão antes de aplicá-la e nunca misture silenciosamente.";
        return "Use exclusivamente D&D 5e 2014. Não aplique alterações de espécie, antecedente, talento ou classe da revisão 2024.";
    }

    private JSONObject embeddedSheet(JSONObject hero){try{String raw=hero==null?"":hero.optString("sheet_json","").trim();if(raw.startsWith("{"))return new JSONObject(raw);}catch(Exception ignored){AppLog.ignored("DndRulesEngine","Ficha estruturada inválida ao ler estado mecânico.",ignored);}return new JSONObject();}
    private String heroRulesProfile(JSONObject hero,JSONObject sheet){String p=hero==null?"":hero.optString("rules_source","").trim();if(p.length()==0&&sheet!=null)p=sheet.optString("rules_source",sheet.optString("rules_profile","")).trim();return canonicalProfile(p.length()==0?PROFILE_2014:p);}
    private int exhaustionLevel(JSONObject hero,JSONObject sheet){int value=hero==null?0:hero.optInt("exhaustion",-1);if(value<0&&sheet!=null)value=sheet.optInt("exhaustion",0);return clamp(value,0,6);}

    private boolean hasProficiency(String list,String skill){
        if(list==null||skill==null||skill.trim().isEmpty())return false;String needle=norm(skill);
        for(String part:list.split("[•\n;]")){
            String n=norm(part);if(!containsSkillName(n,needle))continue;
            // Uma ficha completa lista também perícias não proficientes com seu bônus.
            // Só os marcadores ou listas textuais sem valor numérico concedem proficiência.
            if(part.contains("★")||part.contains("◈")||n.contains("proficiente")||n.contains("especializacao"))return true;
            if(!part.matches(".*[+-]\\d+.*")&&(n.equals(needle)||n.startsWith(needle+" ")||n.contains("pericias "+needle)))return true;
        }
        return false;
    }
    private int proficiencyRank(String list,String skill){
        if(list==null||skill==null||skill.trim().isEmpty())return 0;String needle=norm(skill);
        for(String part:list.split("[•\n;]")){
            String n=norm(part);if(!containsSkillName(n,needle))continue;
            if(part.contains("◈")||n.contains("especializacao")||n.contains("expertise"))return 2;
            if(part.contains("★")||n.contains("proficiente"))return 1;
            if(!part.matches(".*[+-]\\d+.*")&&(n.equals(needle)||n.startsWith(needle+" ")||n.contains("pericias "+needle)))return 1;
        }
        return 0;
    }
    private boolean containsSkillName(String segment,String skill){return segment.equals(skill)||segment.startsWith(skill+" ")||segment.contains(" "+skill+" ")||segment.endsWith(" "+skill);}
    private static boolean listContains(String list,String normalizedNeedle){if(normalizedNeedle==null||normalizedNeedle.length()==0||list==null)return false;String n=norm(list);if(n.length()==0)return false;for(String part:n.split("[ ,;•/\n]+"))if(part.length()>0&&(part.equals("all")||part.equals("todos")||part.equals("tudo")||part.equals(normalizedNeedle)||part.startsWith(normalizedNeedle)||normalizedNeedle.startsWith(part)))return true;return false;}
    private static int clamp(int value,int min,int max){return Math.max(min,Math.min(max,value));}
    private static int sizeRank(String size){String n=norm(size);if(n.contains("tiny")||n.contains("minuscul"))return 0;if(n.contains("small")||n.contains("pequen"))return 1;if(n.contains("large")||n.contains("grand"))return 3;if(n.contains("huge")||n.contains("enorm"))return 4;if(n.contains("gargant"))return 5;return 2;}
    private static String norm(String value){if(value==null)return "";String n=Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
}
