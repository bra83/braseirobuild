package com.braseiro.dnd5e;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Perfil de aventura de Strixhaven dentro do host D&D.
 *
 * Mantém Faerûn persistido no AppDb sem projetá-lo enquanto Strixhaven está
 * ativa. O estado de exploração do campus é local e independente: mapa atual,
 * POI/meeple, período acadêmico e colégio escolhido.
 */
public final class StrixhavenAdventureProfile {
    public static final String PROFILE_FREE_FAERUN="faerun_free";
    public static final String PROFILE_STRIXHAVEN="strixhaven_curriculum";
    public static final String PREF_PROFILE="dnd_adventure_profile_v1";
    public static final String PREF_MAP="strixhaven_map_id_v1";
    public static final String PREF_YEAR="strixhaven_year_v1";
    public static final String PREF_SEMESTER="strixhaven_semester_v1";
    public static final String PREF_COLLEGE="strixhaven_college_v1";
    public static final String PREF_GRADUATED="strixhaven_graduated_v1";
    public static final String PREF_GRADUATED_COLLEGE="strixhaven_graduated_college_v1";
    public static final String PREF_CURRENT_POI="strixhaven_current_poi_v2";
    public static final String PREF_CURRENT_POI_LABEL="strixhaven_current_poi_label_v2";
    public static final String PREF_ACADEMIC_STATE="strixhaven_academic_state_v1";
    public static final String LOGICAL_MAP_ID="strixhaven:university";

    public static final String[] MAP_IDS={"central","university","lorehold","prismari","quandrix","silverquill","witherbloom"};
    public static final String[] MAP_LABELS={"Campus Central","Universidade","Lorehold","Prismari","Quandrix","Silverquill","Witherbloom"};
    public static final String[] MAP_ASSETS={
            "strixhaven/Central_campus_map.webp",
            "strixhaven/Strixhaven_map.webp",
            "strixhaven/Lorehold_map.webp",
            "strixhaven/Prismari_map.webp",
            "strixhaven/Quandrix_map.webp",
            "strixhaven/Silverquill_map.webp",
            "strixhaven/Witherbloom_map.webp"
    };

    private final Context context;
    private final SharedPreferences prefs;

    public StrixhavenAdventureProfile(Context context){
        this.context=context==null?null:context.getApplicationContext();
        this.prefs=context==null?null:context.getSharedPreferences("settings",Context.MODE_PRIVATE);
    }

    public boolean isActive(){return prefs!=null&&PROFILE_STRIXHAVEN.equals(prefs.getString(PREF_PROFILE,PROFILE_FREE_FAERUN));}
    public static boolean isActive(Context context){return new StrixhavenAdventureProfile(context).isActive();}

    public void activate(){if(prefs!=null){SharedPreferences.Editor e=prefs.edit().putString(PREF_PROFILE,PROFILE_STRIXHAVEN);String existing=prefs.getString(PREF_MAP,"");if(existing==null||existing.trim().length()==0)e.putString(PREF_MAP,"university");e.apply();}}
    /** All three Strixhaven exit buttons use this profile transition.
     * A first-ever exit places Kvothe in Yartar, whereas later temporary
     * returns retain his current Faerun map location. */
    public void exitToFaerun(){
        boolean leaving=isActive();
        if(prefs==null || !prefs.edit().putString(PREF_PROFILE,PROFILE_FREE_FAERUN).commit())return;
        finalizeFirstFaerunArrival(leaving);
    }
    private void finalizeFirstFaerunArrival(boolean leaving){
        if(!leaving || context==null || prefs==null)return;
        if(!StrixhavenFaerunArrivalEngine.requestFirstArrival(prefs))return;
        AppDb arrivalDb=null;
        try{
            arrivalDb=new AppDb(context);
            arrivalDb.setActiveMapProfile("dnd");
            if(StrixhavenFaerunArrivalEngine.applyPending(arrivalDb,prefs))
                prefs.edit().putString("last_scene_cast_preview","[]").apply();
        }catch(Exception error){
            // The pending flag survives a missing Atlas. MapPackageManager
            // will finish the position once the verified map is imported.
            AppLog.e("StrixhavenArrival",
                "Primeira chegada a Yartar ainda pendente da validacao do Atlas.",error);
        }finally{if(arrivalDb!=null)arrivalDb.close();}
    }

    public int year(){return prefs==null?1:Math.max(1,Math.min(4,prefs.getInt(PREF_YEAR,1)));}
    public int semester(){return prefs==null?1:Math.max(1,Math.min(2,prefs.getInt(PREF_SEMESTER,1)));}
    public void setAcademicPeriod(int year,int semester){if(prefs!=null)prefs.edit().putInt(PREF_YEAR,Math.max(1,Math.min(4,year))).putInt(PREF_SEMESTER,Math.max(1,Math.min(2,semester))).apply();}

    public String currentMapId(){String id=prefs==null?"university":prefs.getString(PREF_MAP,"university");return indexForId(id)>=0?id:"university";}
    public void setCurrentMapId(String id){if(prefs!=null)prefs.edit().putString(PREF_MAP,indexForId(id)>=0?id:"university").apply();}
    public String currentMapLabel(){return MAP_LABELS[indexForId(currentMapId())];}
    public String currentMapAsset(){return MAP_ASSETS[indexForId(currentMapId())];}

    public static int indexForId(String id){if(id!=null)for(int i=0;i<MAP_IDS.length;i++)if(MAP_IDS[i].equalsIgnoreCase(id))return i;return 1;}
    public static int indexForLabel(String label){if(label!=null)for(int i=0;i<MAP_LABELS.length;i++)if(MAP_LABELS[i].equalsIgnoreCase(label))return i;return 1;}

    private String posKey(String mapId,String axis){return "strixhaven_pos_v2_"+(mapId==null?currentMapId():mapId)+"_"+axis;}
    public float positionX(){return positionX(currentMapId());}
    public float positionY(){return positionY(currentMapId());}
    public float positionX(String mapId){return prefs==null?defaultX(mapId):clamp(prefs.getInt(posKey(mapId,"x"),Math.round(defaultX(mapId)*10000f))/10000f);}
    public float positionY(String mapId){return prefs==null?defaultY(mapId):clamp(prefs.getInt(posKey(mapId,"y"),Math.round(defaultY(mapId)*10000f))/10000f);}
    public void setPosition(String mapId,float x,float y,String poiId,String poiLabel){if(prefs==null)return;String m=indexForId(mapId)>=0?mapId:"university";prefs.edit().putString(PREF_MAP,m).putInt(posKey(m,"x"),Math.round(clamp(x)*10000f)).putInt(posKey(m,"y"),Math.round(clamp(y)*10000f)).putString(PREF_CURRENT_POI,poiId==null?"":poiId).putString(PREF_CURRENT_POI_LABEL,poiLabel==null?"":poiLabel).apply();}
    public String currentPoiId(){return prefs==null?"":prefs.getString(PREF_CURRENT_POI,"");}
    public String currentPoiLabel(){return prefs==null?"":prefs.getString(PREF_CURRENT_POI_LABEL,"");}
    public void resetForNewAcademicAdventure(){if(prefs==null)return;prefs.edit().putString(PREF_MAP,"university").putInt(posKey("university","x"),5000).putInt(posKey("university","y"),6000).putInt(PREF_YEAR,1).putInt(PREF_SEMESTER,1).remove(PREF_COLLEGE).remove(PREF_GRADUATED).remove(PREF_GRADUATED_COLLEGE).remove(PREF_CURRENT_POI).remove(PREF_CURRENT_POI_LABEL).apply();}


    public JSONObject academicState(){
        if(prefs==null)return StrixhavenAcademicRulesEngine.blank();
        try{String raw=prefs.getString(PREF_ACADEMIC_STATE,"");if(raw!=null&&raw.trim().startsWith("{"))return StrixhavenAcademicRulesEngine.normalize(new JSONObject(raw));}catch(Exception ignored){AppLog.ignored("StrixhavenProfile","Falha ao ler estado acadêmico.",ignored);}
        return StrixhavenAcademicRulesEngine.blank();
    }
    public void saveAcademicState(JSONObject state){if(prefs!=null)prefs.edit().putString(PREF_ACADEMIC_STATE,StrixhavenAcademicRulesEngine.normalize(state).toString()).apply();}
    public JSONObject academicSummary(long characterId){return StrixhavenAcademicRulesEngine.summary(academicState(),String.valueOf(characterId));}
    public JSONObject recordExam(long characterId,String examId,String skillOne,String skillTwo,JSONObject examResult){JSONObject next=StrixhavenAcademicRulesEngine.recordExam(academicState(),String.valueOf(characterId),examId,year(),skillOne,skillTwo,examResult);saveAcademicState(next);return StrixhavenAcademicRulesEngine.summary(next,String.valueOf(characterId));}
    public JSONObject joinExtracurricular(long characterId,String name,JSONArray skills,String memberNpc,boolean startOfAcademicYear,int relationshipDirection){JSONObject next=StrixhavenAcademicRulesEngine.joinExtracurricular(academicState(),String.valueOf(characterId),name,skills,memberNpc,startOfAcademicYear,relationshipDirection);saveAcademicState(next);return next;}
    public JSONObject quitExtracurricular(long characterId,String name){JSONObject next=StrixhavenAcademicRulesEngine.quitExtracurricular(academicState(),String.valueOf(characterId),name);saveAcademicState(next);return next;}
    public JSONObject setJob(long characterId,String employer,String title,String coworker,int relationshipDirection){JSONObject next=StrixhavenAcademicRulesEngine.setJob(academicState(),String.valueOf(characterId),employer,title,coworker,relationshipDirection);saveAcademicState(next);return next;}
    public JSONObject quitJob(long characterId){JSONObject next=StrixhavenAcademicRulesEngine.quitJob(academicState(),String.valueOf(characterId));saveAcademicState(next);return next;}
    public JSONObject adjustRelationship(long characterId,String npc,int delta,String boon,String bane){JSONObject next=StrixhavenAcademicRulesEngine.adjustRelationship(academicState(),String.valueOf(characterId),npc,delta,boon,bane);saveAcademicState(next);return next;}
    public JSONObject deepenBeloved(long characterId,String npc){JSONObject next=StrixhavenAcademicRulesEngine.deepenBeloved(academicState(),String.valueOf(characterId),npc);saveAcademicState(next);return next;}
    public JSONObject academicLongRest(long characterId,int proficiencyBonus){JSONObject next=StrixhavenAcademicRulesEngine.longRest(academicState(),String.valueOf(characterId),proficiencyBonus);saveAcademicState(next);return next;}
    public JSONObject finishAcademicYear(long characterId){JSONObject next=StrixhavenAcademicRulesEngine.finishAcademicYear(academicState(),String.valueOf(characterId));saveAcademicState(next);return next;}

    public String college(){return prefs==null?"":prefs.getString(PREF_COLLEGE,"");}
    public boolean hasCollege(){return college().trim().length()>0;}
    public boolean enrollmentAvailable(){return year()>=2;}
    public boolean enroll(String collegeId){String c=canonicalCollege(collegeId);if(prefs==null||c.length()==0||!enrollmentAvailable())return false;prefs.edit().putString(PREF_COLLEGE,c).apply();return true;}
    public String collegeLabel(){String c=college();if(c.length()==0)return "";return MAP_LABELS[indexForId(c)];}
    public boolean canGraduate(){return year()==4&&semester()==2&&hasCollege()&&!graduated();}
    public boolean graduated(){return prefs!=null&&prefs.getBoolean(PREF_GRADUATED,false);}
    public boolean graduate(){
        if(prefs==null||!canGraduate())return false;
        boolean leaving=isActive();String c=college();
        if(!prefs.edit().putBoolean(PREF_GRADUATED,true)
                .putString(PREF_GRADUATED_COLLEGE,c)
                .putString(PREF_PROFILE,PROFILE_FREE_FAERUN).commit())return false;
        finalizeFirstFaerunArrival(leaving);
        return true;
    }
    public JSONObject academicHistory(){/*BR_JSON_WRAP*/try{JSONObject out=new JSONObject();try{if(graduated()){String c=prefs==null?"":prefs.getString(PREF_GRADUATED_COLLEGE,college());out.put("institution","Universidade de Strixhaven").put("graduated",true).put("college_id",c).put("college",c.length()==0?"":MAP_LABELS[indexForId(c)]).put("completed_years",4).put("completed_semesters",8).put("continuity_policy","Histórico acadêmico persistente; não substitui regras, níveis ou benefícios mecânicos sem fonte do host/RAG.");}}catch(Exception ignored){AppLog.ignored("StrixhavenProfile","Falha tolerada ao montar histórico acadêmico.",ignored);}return out;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public JSONObject mapProjection(){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        try{
            out.put("logical_map_id",LOGICAL_MAP_ID)
                    .put("map_package_id","")
                    .put("coordinate_frame_id","strixhaven:campus:illustrated:v2")
                    .put("current_hex_id","")
                    .put("campus_map_id",currentMapId())
                    .put("campus_map_label",currentMapLabel())
                    .put("current_poi_id",currentPoiId())
                    .put("current_poi_label",currentPoiLabel())
                    .put("token_x",positionX())
                    .put("token_y",positionY())
                    .put("position_mode",currentPoiId().trim().length()>0?"campus_landmark":"free_exploration")
                    .put("faerun_atlas_enabled",false)
                    .put("faerun_atlas_preserved",true);
        }catch(Exception ignored){AppLog.ignored("StrixhavenProfile","Falha tolerada ao projetar mapa.",ignored);}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public JSONObject atlasProjection(){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        try{
            out.put("logical_map_id",LOGICAL_MAP_ID)
                    .put("active",true)
                    .put("kind","campus")
                    .put("location",currentPoiLabel().length()>0?currentPoiLabel():currentMapLabel())
                    .put("faerun_hidden",true)
                    .put("faerun_preserved",true)
                    .put("maps",new JSONArray(MAP_LABELS));
        }catch(Exception ignored){AppLog.ignored("StrixhavenProfile","Falha tolerada ao projetar Atlas.",ignored);}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public JSONObject sceneProjection(JSONObject source){/*BR_JSON_WRAP*/try{
        JSONObject out;try{out=source==null?new JSONObject():new JSONObject(source.toString());}catch(Exception e){out=new JSONObject();}
        try{
            String place=currentPoiLabel().length()>0?currentMapLabel()+" • "+currentPoiLabel():currentMapLabel();
            out.put("location","Strixhaven • "+place).put("world_region","Universidade de Strixhaven").put("adventure_profile",PROFILE_STRIXHAVEN).remove("hex_id");
        }catch(Exception ignored){AppLog.ignored("StrixhavenProfile","Falha tolerada ao projetar cena.",ignored);}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public JSONObject descriptor(){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        try{
            out.put("id",PROFILE_STRIXHAVEN).put("title","Strixhaven: A Curriculum of Chaos").put("system_authority","dnd")
                    .put("academic_year",year()).put("semester",semester()).put("campus_map",currentMapLabel()).put("current_poi",currentPoiLabel())
                    .put("college",collegeLabel()).put("college_choice_available",enrollmentAvailable()).put("first_year_general_program",year()==1).put("can_graduate",canGraduate()).put("graduated",graduated())
                    .put("five_colleges",new JSONArray(new String[]{"Lorehold","Prismari","Quandrix","Silverquill","Witherbloom"}))
                    .put("pillars",new JSONArray(new String[]{"aulas e estudo mágico","provas e preparação","amizades, rivalidades e relacionamentos","clubes e atividades extracurriculares","empregos no campus","mistérios e perigos universitários","eventos sociais e competições","vida cotidiana entre grandes acontecimentos"}))
                    .put("academic_rules",new JSONObject().put("first_year","formação geral; visitar os cinco colégios não equivale a matrícula")
                            .put("college_choice","a escolha de colégio só é oferecida pelo app a partir do Ano 2")
                            .put("exam_flow","Studying → Testing").put("testing","o Mestre consulta evidência local antes de resolver mecânica")
                            .put("student_die","d4 compilado: máximo 1 por teste; cada dado usado volta após descanso longo; dados de prova expiram no fim do ano acadêmico").put("academic_runtime","StrixhavenAcademicRulesEngine pp.44-50"))
                    .put("movement_policy","mapa ilustrado explorável sem grid tático; o jogador pode tocar em qualquer ponto para explorar livremente ou usar POIs como atalhos; todo deslocamento avança o relógio da cena por estimativa do app, sem inventar teste ou custo mecânico de D&D")
                    .put("faerun_atlas_policy","preservado no save, desabilitado e não projetado enquanto Strixhaven estiver ativa");
        }catch(Exception ignored){AppLog.ignored("StrixhavenProfile","Falha tolerada ao montar descritor.",ignored);}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public String narrativeMapContext(){
        String place=currentPoiLabel().length()>0?currentMapLabel()+" / "+currentPoiLabel():currentMapLabel();
        return "PERFIL DE AVENTURA ATIVO: STRIXHAVEN. Local atual: "+place+". Ano "+year()+", semestre "+semester()+". "+
                (collegeLabel().length()>0?"Colégio escolhido: "+collegeLabel()+". ":"Nenhum colégio escolhido ainda. ")+
                "O Atlas de Faerûn continua preservado no save, mas está DESABILITADO neste perfil. Não use, cite ou inferira hexes, cidades, rotas, POIs, coordenadas ou nomes vindos do Atlas de Faerûn. A geografia válida é a universidade e seus mapas locais.";
    }

    public String masterInstruction(){
        return "CONTRATO NARRATIVO STRIXHAVEN — obrigatório enquanto adventure_profile=strixhaven_curriculum: continue usando as regras D&D selecionadas pelo host, mas narre a campanha como vida universitária mágica, não como fantasia genérica. "+
                "No Ano 1 o programa é geral: o jogador pode visitar Lorehold, Prismari, Quandrix, Silverquill e Witherbloom sem estar matriculado em nenhum deles. Só trate escolha de colégio como disponível a partir do Ano 2 e nunca confunda visita com matrícula. "+
                "Alterne organicamente aulas/estudo, provas e preparação, relacionamentos recorrentes, colegas e professores, empregos, extracurriculares, eventos sociais, competições, investigação e perigo. Preserve compromissos, horários e consequências entre sessões. As regras acadêmicas de provas, Dados do Estudante, tutoria, empregos, extracurriculares e Pontos de Relacionamento são compiladas localmente em StrixhavenAcademicRulesEngine e não devem ser reinventadas pelo narrador. "+
                "A posição contínua do meeple enviada no game_bus é autoridade espacial da cena; POI é opcional. Toques fora de marcadores representam exploração livre e NÃO autorizam inventar um nome oficial para o ponto. Um deslocamento de campus não exige teste por si só; qualquer risco, teste, custo ou regra adicional precisa vir do host/RAG. "+
                "Não transforme toda cena em combate, dungeon ou missão externa. Faerûn está temporariamente fora do contexto e só volta por comando explícito da interface.";
    }

    public JSONObject sanitizeNarratorBus(JSONObject bus){/*BR_JSON_WRAP*/try{
        JSONObject out;try{out=bus==null?new JSONObject():new JSONObject(bus.toString());}catch(Exception e){out=new JSONObject();}
        try{out.put("map",mapProjection());out.put("map_context",narrativeMapContext());out.put("scene",sceneProjection(out.optJSONObject("scene")));out.put("adventure_profile",descriptor());}catch(Exception ignored){AppLog.ignored("StrixhavenProfile","Falha tolerada ao sanitizar barramento.",ignored);}return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static String canonicalCollege(String id){if(id==null)return "";String c=id.trim().toLowerCase();if("lorehold".equals(c)||"prismari".equals(c)||"quandrix".equals(c)||"silverquill".equals(c)||"witherbloom".equals(c))return c;return "";}

    private static float defaultX(String mapId){if("university".equals(mapId))return .50f;return .50f;}
    private static float defaultY(String mapId){if("university".equals(mapId))return .60f;return .86f;}
    private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
}
