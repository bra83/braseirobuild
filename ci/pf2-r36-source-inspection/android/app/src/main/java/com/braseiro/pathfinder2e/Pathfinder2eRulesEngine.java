package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/** Núcleo determinístico PF2e Remaster. A IA descreve; este motor resolve a mecânica. */
public final class Pathfinder2eRulesEngine {
    public static final String PROFILE="Pathfinder 2e Remaster";
    public enum Degree { CRITICAL_SUCCESS, SUCCESS, FAILURE, CRITICAL_FAILURE }

    public int proficiency(int level,String rank){
        int r=rankValue(rank);
        return r==0?0:Math.max(1,Math.min(20,level))+r;
    }

    public int rankValue(String rank){
        String n=norm(rank);
        if(n.isEmpty()||n.startsWith("u")||n.contains("untrained")||n.contains("nao trein"))return 0;
        if(n.startsWith("l")||n.contains("legend"))return 8;
        if(n.startsWith("m")||n.contains("mestre")||n.contains("master"))return 6;
        if(n.startsWith("e")||n.contains("especial")||n.contains("expert"))return 4;
        if(n.startsWith("t")||n.contains("trein")||n.contains("trained"))return 2;
        return 0;
    }

    public Degree degree(int natural,int total,int dc){
        Degree d=total>=dc+10?Degree.CRITICAL_SUCCESS:total>=dc?Degree.SUCCESS:total<=dc-10?Degree.CRITICAL_FAILURE:Degree.FAILURE;
        if(natural==20)d=step(d,1); else if(natural==1)d=step(d,-1);
        return d;
    }

    private Degree step(Degree d,int delta){
        int i=d.ordinal()-delta;
        return Degree.values()[Math.max(0,Math.min(3,i))];
    }

    public String degreeLabel(Degree d){
        if(d==Degree.CRITICAL_SUCCESS)return "SUCESSO CRÍTICO";
        if(d==Degree.SUCCESS)return "SUCESSO";
        if(d==Degree.CRITICAL_FAILURE)return "FALHA CRÍTICA";
        return "FALHA";
    }

    public JSONObject resolveCheck(int natural,int modifier,int dc){
        JSONObject o=new JSONObject();
        try{
            int n=Math.max(1,Math.min(20,natural)),t=n+modifier;
            Degree d=degree(n,t,dc);
            o.put("natural_roll",n).put("modifier",modifier).put("total",t).put("dc",dc).put("degree",d.name()).put("degree_pt",degreeLabel(d));
        }catch(Exception ignored){}
        return o;
    }

    /**
     * Regras de empilhamento PF2e (Remaster / Core):
     * - Bônus tipados (item, status, circunstância): apenas o maior bônus de cada tipo se aplica. Bônus sem tipo acumulam.
     * - Penalidades tipadas (item, status, circunstância): apenas a pior penalidade (menor número/maior magnitude) de cada tipo se aplica. Penalidades sem tipo acumulam.
     */
    public int stackBonuses(int item,int status,int circumstance,int untyped){
        return Math.max(0,item)+Math.max(0,status)+Math.max(0,circumstance)+Math.max(0,untyped);
    }

    public int stackPenalties(int item,int status,int circumstance,int untyped){
        int i=Math.min(0,item>0?-item:item);
        int s=Math.min(0,status>0?-status:status);
        int c=Math.min(0,circumstance>0?-circumstance:circumstance);
        int u=Math.min(0,untyped>0?-untyped:untyped);
        return i+s+c+u;
    }

    public JSONObject stackModifiers(int base,JSONArray modifiers){
        JSONObject out=new JSONObject();
        try{
            int maxItem=0,maxStatus=0,maxCirc=0,sumUntypedBonus=0;
            int worstItem=0,worstStatus=0,worstCirc=0,sumUntypedPen=0;
            if(modifiers!=null){
                for(int idx=0;idx<modifiers.length();idx++){
                    JSONObject m=modifiers.optJSONObject(idx);
                    if(m==null)continue;
                    String type=norm(m.optString("type","untyped"));
                    int val=m.optInt("value",0);
                    if(val>0){
                        if(type.contains("item"))maxItem=Math.max(maxItem,val);
                        else if(type.contains("stat")||type.contains("estado"))maxStatus=Math.max(maxStatus,val);
                        else if(type.contains("circ")||type.contains("circuns"))maxCirc=Math.max(maxCirc,val);
                        else sumUntypedBonus+=val;
                    }else if(val<0){
                        if(type.contains("item"))worstItem=Math.min(worstItem,val);
                        else if(type.contains("stat")||type.contains("estado"))worstStatus=Math.min(worstStatus,val);
                        else if(type.contains("circ")||type.contains("circuns"))worstCirc=Math.min(worstCirc,val);
                        else sumUntypedPen+=val;
                    }
                }
            }
            int totalBonus=maxItem+maxStatus+maxCirc+sumUntypedBonus;
            int totalPenalty=worstItem+worstStatus+worstCirc+sumUntypedPen;
            int netModifier=totalBonus+totalPenalty;
            out.put("base",base).put("item_bonus",maxItem).put("status_bonus",maxStatus).put("circumstance_bonus",maxCirc).put("untyped_bonus",sumUntypedBonus).put("total_bonus",totalBonus).put("item_penalty",worstItem).put("status_penalty",worstStatus).put("circumstance_penalty",worstCirc).put("untyped_penalty",sumUntypedPen).put("total_penalty",totalPenalty).put("net_modifier",netModifier).put("total",base+netModifier);
        }catch(Exception ignored){}
        return out;
    }

    /** Teste simples de dano persistente: CD 15 pura ou CD 10 com ajuda assistida. */
    public JSONObject persistentDamageFlatCheck(int naturalRoll,boolean assisted){
        JSONObject out=new JSONObject();
        try{
            int dc=assisted?10:15,n=Math.max(1,Math.min(20,naturalRoll));
            out.put("natural_roll",n).put("dc",dc).put("assisted",assisted).put("recovered",n>=dc).put("success",n>=dc);
        }catch(Exception ignored){}
        return out;
    }

    /** Testes simples PF2e (d20 natural puro contra CD fixa sem modificadores). */
    public JSONObject flatCheck(int naturalRoll,int dc){
        JSONObject out=new JSONObject();
        try{
            int n=Math.max(1,Math.min(20,naturalRoll));
            out.put("natural_roll",n).put("dc",dc).put("success",n>=dc);
        }catch(Exception ignored){}
        return out;
    }

    /** Cura natural por descanso PF2e: 8 horas recupera CON mod * nível (mínimo 1 * nível). 24 horas recupera o dobro. */
    public int naturalHealing(int conModifier,int level,boolean fullRest24Hours){
        int con=Math.max(1,conModifier),lvl=Math.max(1,Math.min(20,level));
        return con*lvl*(fullRest24Hours?2:1);
    }

    /** Preparação diária / descanso longo: remove fatigued, calcula cura natural e renova recursos diários. */
    public JSONObject dailyPreparation(JSONObject hero,boolean fullRest24Hours){
        JSONObject out=new JSONObject();
        try{
            int level=Math.max(1,Math.min(20,hero==null?1:hero.optInt("level",1)));
            int conMod=attributeModifier(hero,"Constituição");
            int heal=naturalHealing(conMod,level,fullRest24Hours);
            int currentHp=hero==null?0:hero.optInt("hp",0);
            int maxHp=hero==null?1:Math.max(1,hero.optInt("max_hp",1));
            int nextHp=Math.min(maxHp,currentHp+heal);
            out.put("hp_healed",nextHp-currentHp).put("hp_after",nextHp).put("fatigued_removed",true).put("daily_resources_reset",true).put("daily_prep_complete",true);
        }catch(Exception ignored){}
        return out;
    }

    /** Aceita fichas PF2 nativas (modificadores) e migra fichas antigas com scores sem quebrar a campanha. */
    public int attributeModifier(JSONObject hero,String ability){
        if(hero==null)return 0;
        JSONObject sheet=sheet(hero);
        String key=abilityKey(ability);
        JSONObject mods=object(sheet.opt("attribute_modifiers"));
        if(mods!=null&&mods.has(key))return mods.optInt(key,0);
        if(sheet.has(key+"_mod"))return sheet.optInt(key+"_mod",0);
        if(sheet.has(key)){
            int raw=sheet.optInt(key,0);
            if(raw>=-5&&raw<=10)return raw;
            return Math.floorDiv(raw-10,2);
        }
        return 0;
    }

    public int checkBonus(JSONObject hero,String ability,String skill){
        if(hero==null)return 0;
        JSONObject sheet=sheet(hero);
        String skillKey=skillKey(skill);
        String explicit=skillKey.length()==0?"":skillKey+"_bonus";
        if(explicit.length()>0&&sheet.has(explicit))return sheet.optInt(explicit,0);
        if("perception".equals(skillKey)&&sheet.has("perception_bonus"))return sheet.optInt("perception_bonus",0);
        int level=Math.max(1,Math.min(20,sheet.optInt("level",hero.optInt("level",1))));
        String resolvedAbility=(ability==null||ability.trim().isEmpty())?abilityForSkill(skillKey):ability;
        int mod=attributeModifier(hero,resolvedAbility);
        String rank=rankFor(sheet,skillKey);
        return mod+proficiency(level,rank);
    }

    private String rankFor(JSONObject sheet,String skillKey){
        if(skillKey==null||skillKey.isEmpty())return "untrained";
        String direct=sheet.optString(skillKey+"_rank","");
        if(!direct.isEmpty())return direct;
        JSONObject ranks=object(sheet.opt("skill_ranks"));
        if(ranks!=null){String r=ranks.optString(skillKey,"");if(!r.isEmpty())return r;}
        if("perception".equals(skillKey))return sheet.optString("perception_rank",sheet.optString("perception_proficiency","untrained"));
        return "untrained";
    }

    public int mapPenalty(int attackIndex,boolean agile){
        if(attackIndex<=1)return 0;
        if(attackIndex==2)return agile?-4:-5;
        return agile?-8:-10;
    }

    public int initiativeBonus(JSONObject hero,String skill){
        String s=skill==null||skill.trim().isEmpty()?"Percepção":skill;
        return checkBonus(hero,"",s);
    }

    /** Jogadas de salvamento PF2e: Fortitude(CON), Reflexos(DES), Vontade(SAB) + proficiência. */
    public int savingThrowBonus(JSONObject hero,String save){
        if(hero==null)return 0;
        JSONObject sheet=sheet(hero);
        String n=norm(save);
        String key,ability;
        if(n.contains("reflex")){key="reflex";ability="Destreza";}
        else if(n.contains("vont")||n.contains("will")){key="will";ability="Sabedoria";}
        else {key="fortitude";ability="Constituição";}
        if(sheet.has(key+"_bonus"))return sheet.optInt(key+"_bonus",0);
        int level=Math.max(1,Math.min(20,sheet.optInt("level",hero.optInt("level",1))));
        String rank=sheet.optString(key+"_rank",sheet.optString(key+"_proficiency","untrained"));
        return attributeModifier(hero,ability)+proficiency(level,rank);
    }

    public JSONObject turnBudget(int actionsUsed,boolean reactionUsed,boolean quickened,boolean slowed,int stunned){
        JSONObject o=new JSONObject();
        try{
            int max=3+(quickened?1:0)-Math.max(slowed?1:0,Math.max(0,stunned));
            max=Math.max(0,Math.min(4,max));
            int used=Math.min(max,Math.max(0,actionsUsed));
            o.put("actions_max",max).put("actions_used",used).put("actions_remaining",Math.max(0,max-used)).put("reaction_available",!reactionUsed).put("quickened_action_restricted",quickened);
        }catch(Exception ignored){}
        return o;
    }

    public JSONObject dying(int dying,int wounded,int doomed,boolean recoverySuccess,boolean critical){
        JSONObject o=new JSONObject();
        try{
            int d=Math.max(0,dying),w=Math.max(0,wounded),doom=Math.max(0,doomed);
            if(recoverySuccess)d=Math.max(0,d-(critical?2:1));else d+=critical?2:1;
            o.put("dying",d).put("wounded",w).put("doomed",doom).put("dead",d>=Math.max(1,4-doom));
        }catch(Exception ignored){}
        return o;
    }

    public int deathLimit(int doomed){return Math.max(1,4-Math.max(0,doomed));}
    public int recoveryCheckDc(int dying){return 10+Math.max(1,dying);}


    /** Resolve o início de dying em PF2e sem delegar regra à IA. */
    public JSONObject knockedOut(int wounded,int doomed,boolean nonlethal){return knockedOut(wounded,doomed,nonlethal,false);}

    /** Ao cair a 0 por acerto crítico, dying começa em 2 + wounded; dano não letal não inicia dying. */
    public JSONObject knockedOut(int wounded,int doomed,boolean nonlethal,boolean critical){
        JSONObject o=new JSONObject();
        try{
            int w=Math.max(0,wounded), doom=Math.max(0,doomed);
            if(nonlethal){o.put("unconscious",true).put("dying",0).put("wounded",w).put("doomed",doom).put("dead",false);return o;}
            int d=(critical?2:1)+w;
            o.put("unconscious",true).put("dying",d).put("wounded",w).put("doomed",doom).put("dead",d>=Math.max(1,4-doom));
        }catch(Exception ignored){}
        return o;
    }

    /** Recuperação de dying: CD 10 + valor atual de dying, quatro graus e wounded ao sair de dying. */
    public JSONObject recoveryCheck(int natural,int dying,int wounded,int doomed){
        JSONObject o=new JSONObject();
        try{
            int d=Math.max(1,dying),w=Math.max(0,wounded),doom=Math.max(0,doomed),dc=10+d;
            Degree degree=degree(Math.max(1,Math.min(20,natural)),natural,dc);
            if(degree==Degree.CRITICAL_SUCCESS)d=Math.max(0,d-2);
            else if(degree==Degree.SUCCESS)d=Math.max(0,d-1);
            else if(degree==Degree.FAILURE)d+=1; else d+=2;
            boolean stabilized=d==0; if(stabilized)w+=1;
            o.put("natural_roll",natural).put("dc",dc).put("degree",degree.name()).put("degree_pt",degreeLabel(degree)).put("dying",d).put("wounded",w).put("doomed",doom).put("stabilized",stabilized).put("dead",d>=Math.max(1,4-doom));
        }catch(Exception ignored){}
        return o;
    }

    /** Valida uma mutação de condição antes de persistir estado. */
    public boolean validConditionMutation(String condition,int value){
        if(!validCondition(condition))return false;
        String n=norm(condition);
        boolean valued=n.contains("fright")||n.contains("amedront")||n.contains("clumsy")||n.contains("desajeit")||n.contains("doomed")||n.contains("conden")||n.contains("drained")||n.contains("dren")||n.contains("dying")||n.contains("morr")||n.contains("enfeeb")||n.contains("enfraq")||n.contains("sick")||n.contains("enjo")||n.contains("slowed")||n.contains("lento")||n.contains("stunned")||n.contains("atordo")||n.contains("stupef")||n.contains("wounded")||n.contains("ferido");
        return valued?value>=1&&value<=20:value==0||value==1;
    }


    /** Valida uma lista textual de condições PF2e separadas por vírgula/ponto e vírgula/quebra de linha. */
    public boolean validConditionList(String raw){
        if(raw==null||raw.trim().isEmpty())return true;
        String[] parts=raw.split("[,;\n]+");
        for(String part:parts){
            String x=part==null?"":part.trim();if(x.isEmpty())continue;
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("^(.*?)(?:\\s+(\\d+))?$").matcher(x);
            if(!m.matches())return false;
            String name=m.group(1)==null?"":m.group(1).trim();int value=m.group(2)==null?0:Integer.parseInt(m.group(2));
            if(!validConditionMutation(name,value))return false;
        }
        return true;
    }

    public boolean validCondition(String value){
        String n=norm(value).replaceAll(" \\d+$","");
        if(n.startsWith("persistent damage")||n.startsWith("dano persistente"))return true;
        String allowed="blinded broken clumsy concealed confused controlled dazzled deafened doomed drained dying encumbered enfeebled fascinated fatigued fleeing frightened grabbed hidden immobilized invisible observed off guard flat footed flat_footed off_guard paralyzed persistent damage petrified prone quickened restrained sickened slowed stunned stupefied unconscious undetected wounded cego quebrado desajeitado ocultado confuso controlado ofuscado surdo condenado drenado morrendo sobrecarregado enfraquecido fascinado fatigado fugindo amedrontado agarrado escondido imobilizado invisivel observado desprevenido paralisado dano persistente petrificado caido acelerado restringido enjoado lento atordoado estupefato inconsciente indetectado ferido";
        return (" "+allowed+" ").contains(" "+n+" ");
    }

    public JSONObject evaluateConditions(String raw){
        return evaluateConditions(raw,1,0);
    }

    public JSONObject evaluateConditions(String raw,int level,int conMod){
        JSONObject out=new JSONObject();
        try{
            int lvl=Math.max(1,Math.min(20,level));
            int clumsy=0, doomed=0, drained=0, dying=0, enfeebled=0, frightened=0, sickened=0, slowed=0, stunned=0, stupefied=0, wounded=0;
            boolean blinded=false, broken=false, concealed=false, confused=false, controlled=false, dazzled=false, deafened=false;
            boolean encumbered=false, fascinated=false, fatigued=false, fleeing=false, grabbed=false, hidden=false, immobilized=false;
            boolean invisible=false, offGuard=false, paralyzed=false, persistentDamage=false, petrified=false, prone=false;
            boolean quickened=false, restrained=false, unconscious=false, undetected=false;

            JSONArray list=new JSONArray();
            if(raw!=null&&!raw.trim().isEmpty()){
                String[] parts=raw.split("[,;\n]+");
                for(String part:parts){
                    String x=part==null?"":part.trim();if(x.isEmpty())continue;
                    java.util.regex.Matcher m=java.util.regex.Pattern.compile("^(.*?)(?:\\s+(\\d+))?$").matcher(x);
                    if(!m.matches())continue;
                    String name=m.group(1)==null?"":m.group(1).trim();
                    int val=m.group(2)==null?1:Integer.parseInt(m.group(2));
                    String n=norm(name);
                    list.put(new JSONObject().put("name",name).put("normalized",n).put("value",val));

                    if(n.contains("blind")||n.contains("cego"))blinded=true;
                    if(n.contains("broken")||n.contains("quebrad"))broken=true;
                    if(n.contains("clumsy")||n.contains("desajeit"))clumsy=Math.max(clumsy,val);
                    if(n.contains("conceal")||n.contains("ocultad"))concealed=true;
                    if(n.contains("confus"))confused=true;
                    if(n.contains("control"))controlled=true;
                    if(n.contains("dazzl")||n.contains("ofusc"))dazzled=true;
                    if(n.contains("deafen")||n.contains("surdo"))deafened=true;
                    if(n.contains("doomed")||n.contains("conden"))doomed=Math.max(doomed,val);
                    if(n.contains("drain")||n.contains("dren"))drained=Math.max(drained,val);
                    if(n.contains("dying")||n.contains("morr"))dying=Math.max(dying,val);
                    if(n.contains("encumb")||n.contains("sobrecarg")){encumbered=true;clumsy=Math.max(clumsy,1);}
                    if(n.contains("enfeeb")||n.contains("enfraq"))enfeebled=Math.max(enfeebled,val);
                    if(n.contains("fascin"))fascinated=true;
                    if(n.contains("fatig"))fatigued=true;
                    if(n.contains("flee")||n.contains("fugind"))fleeing=true;
                    if(n.contains("fright")||n.contains("amedront"))frightened=Math.max(frightened,val);
                    if(n.contains("grab")||n.contains("agarr"))grabbed=true;
                    if(n.contains("hidden")||n.contains("escond"))hidden=true;
                    if(n.contains("immob")||n.contains("imobil"))immobilized=true;
                    if(n.contains("invis"))invisible=true;
                    if(n.contains("off guard")||n.contains("flat foot")||n.contains("despreven"))offGuard=true;
                    if(n.contains("paraly")||n.contains("paralis"))paralyzed=true;
                    if(n.contains("persist")||n.contains("dano persist"))persistentDamage=true;
                    if(n.contains("petrif"))petrified=true;
                    if(n.contains("prone")||n.contains("caido"))prone=true;
                    if(n.contains("quicken")||n.contains("aceler"))quickened=true;
                    if(n.contains("restrain")||n.contains("restring"))restrained=true;
                    if(n.contains("sick")||n.contains("enjo"))sickened=Math.max(sickened,val);
                    if(n.contains("slow")||n.contains("lento"))slowed=Math.max(slowed,val);
                    if(n.contains("stun")||n.contains("atordo"))stunned=Math.max(stunned,val);
                    if(n.contains("stupef")||n.contains("estupef"))stupefied=Math.max(stupefied,val);
                    if(n.contains("unconsc")||n.contains("inconsc"))unconscious=true;
                    if(n.contains("undetect")||n.contains("indetect"))undetected=true;
                    if(n.contains("wound")||n.contains("ferid"))wounded=Math.max(wounded,val);
                }
            }

            if(dying>0)unconscious=true;
            if(unconscious){prone=true;offGuard=true;}
            if(prone||grabbed||restrained||paralyzed||blinded)offGuard=true;

            int circAcPen=offGuard?-2:0;
            int statusAcPen=Math.min(0,Math.min(-clumsy,Math.min(fatigued?-1:0,unconscious?-4:0)));
            int totalAcPen=circAcPen+statusAcPen;

            int allChecksPen=Math.min(0,Math.min(-frightened,-sickened));
            int strPen=allChecksPen-enfeebled;
            int dexPen=allChecksPen-clumsy;
            int conPen=allChecksPen-drained;
            int mentalPen=allChecksPen-stupefied;

            int perceptionPen=Math.min(allChecksPen,Math.min(blinded?-4:0,Math.min(deafened?-2:0,Math.min(fascinated?-2:0,unconscious?-4:0))));
            int attackCircPen=Math.min(0,Math.min(prone?-2:0,restrained?-2:0));

            JSONObject saves=new JSONObject();
            saves.put("fortitude",Math.min(allChecksPen,Math.min(-drained,fatigued?-1:0)));
            saves.put("reflex",Math.min(allChecksPen,Math.min(-clumsy,Math.min(fatigued?-1:0,unconscious?-4:0))));
            saves.put("will",Math.min(allChecksPen,Math.min(-stupefied,fatigued?-1:0)));

            boolean canAct=!(paralyzed||unconscious||petrified);
            boolean speedZero=immobilized||grabbed||restrained||paralyzed||unconscious||petrified;
            int speedPenFeet=encumbered?10:0;
            int actionsBudget=canAct?Math.max(0,Math.min(4,3+(quickened?1:0)-Math.max(slowed,stunned))):0;

            out.put("parsed_conditions",list)
               .put("off_guard",offGuard)
               .put("ac_circumstance_penalty",circAcPen)
               .put("ac_status_penalty",statusAcPen)
               .put("ac_penalty",totalAcPen)
               .put("all_checks_status_penalty",allChecksPen)
               .put("str_checks_penalty",strPen)
               .put("dex_checks_penalty",dexPen)
               .put("con_checks_penalty",conPen)
               .put("mental_checks_penalty",mentalPen)
               .put("perception_penalty",perceptionPen)
               .put("attack_circumstance_penalty",attackCircPen)
               .put("saving_throws_penalties",saves)
               .put("can_act",canAct)
               .put("speed_zero",speedZero)
               .put("speed_penalty_feet",speedPenFeet)
               .put("actions_max",actionsBudget)
               .put("actions_lost",Math.max(slowed,stunned))
               .put("quickened",quickened)
               .put("frightened",frightened)
               .put("frightened_end_of_turn",Math.max(0,frightened-1))
               .put("sickened",sickened)
               .put("doomed",doomed)
               .put("death_threshold",Math.max(1,4-doomed))
               .put("drained",drained)
               .put("drained_hp_penalty",drained*lvl)
               .put("dying",dying)
               .put("wounded",wounded)
               .put("recovery_dc",10+dying)
               .put("unconscious",unconscious)
               .put("prone",prone)
               .put("persistent_damage",persistentDamage)
               .put("persistent_damage_dc",15)
               .put("persistent_damage_assisted_dc",10)
               .put("concealed_dc",5)
               .put("hidden_dc",11)
               .put("grabbed_manipulate_dc",grabbed?5:0)
               .put("stupefied_cast_dc",stupefied>0?5+stupefied:0);
        }catch(Exception ignored){}
        return out;
    }

    public String authority(){
        return "PF2e Remaster: três ações e uma reação; quatro graus de sucesso; 20 natural melhora um grau e 1 natural piora um grau; proficiência por nível e graduação; MAP -5/-10 ou -4/-8 ágil; iniciativa normalmente por Percepção, podendo usar outra perícia quando a abordagem justificar; dying/wounded/doomed; Mundo Vivo e narrativa nunca concedem regra por inferência; nenhuma regra D&D/PF1 é autoridade ou fallback; nenhuma mecânica D&D, Mystara ou PF1 é fallback.";
    }

    private static JSONObject sheet(JSONObject hero){
        JSONObject s=object(hero.opt("sheet_json"));
        return s==null?hero:s;
    }
    private static JSONObject object(Object value){
        if(value instanceof JSONObject)return (JSONObject)value;
        if(value instanceof String){try{return new JSONObject((String)value);}catch(Exception ignored){}}
        return null;
    }
    private static String abilityKey(String ability){
        String n=norm(ability);
        if(n.startsWith("for")||n.equals("str")||n.contains("strength"))return "str";
        if(n.startsWith("des")||n.equals("dex")||n.contains("dexterity"))return "dex";
        if(n.startsWith("con")||n.contains("constitution"))return "con";
        if(n.startsWith("int")||n.contains("intelligence"))return "int_score";
        if(n.startsWith("sab")||n.equals("wis")||n.contains("wisdom"))return "wis";
        if(n.startsWith("car")||n.equals("cha")||n.contains("charisma"))return "cha";
        return n;
    }
    private static String skillKey(String skill){
        String n=norm(skill);
        if(n.contains("percepc")||n.contains("perception"))return "perception";
        if(n.contains("acroba"))return "acrobatics";
        if(n.contains("arcana"))return "arcana";
        if(n.contains("atlet")||n.contains("athletic"))return "athletics";
        if(n.contains("oficio")||n.contains("craft"))return "crafting";
        if(n.contains("engan")||n.contains("deception"))return "deception";
        if(n.contains("diploma"))return "diplomacy";
        if(n.contains("intimida"))return "intimidation";
        if(n.contains("medicina")||n.contains("medicine"))return "medicine";
        if(n.contains("natureza")||n.contains("nature"))return "nature";
        if(n.contains("ocult")||n.contains("occult"))return "occultism";
        if(n.contains("performance")||n.contains("atuacao")||n.contains("atuac"))return "performance";
        if(n.contains("relig"))return "religion";
        if(n.contains("socied")||n.contains("society"))return "society";
        if(n.contains("furt")||n.contains("stealth"))return "stealth";
        if(n.contains("sobreviv")||n.contains("survival"))return "survival";
        if(n.contains("ladro")||n.contains("thievery"))return "thievery";
        return n.replace(' ','_');
    }
    private static String abilityForSkill(String skill){
        if("acrobatics".equals(skill)||"stealth".equals(skill)||"thievery".equals(skill))return "dex";
        if("arcana".equals(skill)||"crafting".equals(skill)||"occultism".equals(skill)||"society".equals(skill))return "int_score";
        if("athletics".equals(skill))return "str";
        if("deception".equals(skill)||"diplomacy".equals(skill)||"intimidation".equals(skill)||"performance".equals(skill))return "cha";
        return "wis";
    }
    private static String norm(String s){
        if(s==null)return "";
        return Normalizer.normalize(s.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-z0-9]+"," ").trim();
    }
}
