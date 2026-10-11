package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Regras e exemplos fonte-fiéis de Forgotten Realms: Adventures in Faerûn. */
public final class AdventuresInFaerunRulesEngine {
    public static final String SUPPLEMENT_ID="adventures_in_faerun";

    /** Os dois destinos abaixo são exemplos publicados, não um subsistema de níveis 21–30. */
    public JSONObject resolveEpicDestiny(String destinyName,int level){JSONObject res=new JSONObject();try{String n=destinyName==null?"":destinyName.trim();int lvl=Math.max(1,level);res.put("destiny",n).put("level",lvl).put("uses_normal_progression",true).put("auto_rolls",false).put("source_verified",true);
        JSONArray milestones=new JSONArray();
        if("Heir to the Throne".equalsIgnoreCase(n)||"Herdeiro do Trono".equalsIgnoreCase(n)){
            milestones.put(ms(1,"Noble background","Origem sugerida no início do arco."));
            milestones.put(ms(4,"Skill Expert feat","O personagem passa a ser esperado como herdeiro e recebe treinamento intenso."));
            milestones.put(ms(11,"Legendary magic item","Ao ascender ao trono, recebe um item lendário símbolo do poder real."));
            milestones.put(ms(17,"Charm: Foresight (self only, once)","Uma visão do futuro leva o personagem a abandonar o trono."));
        }else if("Pupil of the Archmage".equalsIgnoreCase(n)||"Pupilo do Arquimago".equalsIgnoreCase(n)){
            milestones.put(ms(1,"Sage background","O personagem começa como aprendiz do arquimago."));
            milestones.put(ms(5,"Archmage spellbook","Com o desaparecimento do mestre, herda o grimório, inclusive magias além da capacidade atual."));
            milestones.put(ms(12,"Fey Touched or Shadow Touched","O jogador escolhe um desses feats como consequência das viagens planares."));
            milestones.put(ms(17,"Demiplane Bastion special facility","Após a morte do arquimago, herda a torre mística com a instalação especial Demiplane."));
        }else{res.put("source_verified",false).put("status","example_not_defined_by_source");return res;}
        res.put("milestones",milestones).put("current_milestone",latestMilestone(milestones,lvl));
    }catch(Exception ignored){}return res;}

    /** O livro não publica uma tabela universal para os perigos inventados na versão anterior. */
    public JSONObject resolveRegionalHazard(String regionName,int survivalRoll,int conSaveRoll){JSONObject o=new JSONObject();try{o.put("region",regionName==null?"":regionName).put("resolved",false).put("source_verified",false).put("status","no_universal_hazard_rule_in_source").put("auto_rolls",false);}catch(Exception ignored){}return o;}
    public JSONArray listFaerunRelics(){return new JSONArray();}
    public JSONArray listPublishedExampleDestinies(){JSONArray a=new JSONArray();try{a.put("Heir to the Throne").put("Pupil of the Archmage");}catch(Exception ignored){}return a;}

    private static JSONObject ms(int level,String benefit,String note)throws Exception{return new JSONObject().put("level",level).put("benefit",benefit).put("note",note);}
    private static JSONObject latestMilestone(JSONArray a,int level){JSONObject latest=new JSONObject();for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);if(m!=null&&m.optInt("level",999)<=level)latest=m;}return latest;}
}
