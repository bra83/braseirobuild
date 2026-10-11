package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Regras e limites mecânicos locais. O Gemini nunca é a autoridade final. */
public final class MasterRulesArbiter {
    private final AppDb db;
    private final DndRulesEngine rules=new DndRulesEngine();
    private final MystaraRulesEngine mystaraRules=new MystaraRulesEngine();
    private final RuleResolutionVerifier resolutionVerifier;
    private final OfficialBookRuntimeGateway officialBookGateway;
    private JSONObject activeRuleRetrieval=new JSONObject();
    public MasterRulesArbiter(AppDb database){db=database;resolutionVerifier=new RuleResolutionVerifier(database);officialBookGateway=new OfficialBookRuntimeGateway(database);}
    public JSONObject executeOfficialBookRule(String bookId,String actionId,JSONObject input){return officialBookGateway.execute(bookId,actionId,input==null?new JSONObject():input);}

    /** Applies the typed sidekick level effect through its atomic Store contract. */
    public JSONObject commitCompiledSidekickLevel(CompiledSidekickLevelCommitContract.Input input){
        return CompiledSidekickLevelCommitContract.commit(new AppDbSidekickLevelStore(db),input);
    }

    public void setActiveRuleRetrieval(JSONObject retrieval){activeRuleRetrieval=retrieval==null?new JSONObject():retrieval;}

    public JSONObject rulesFrame(String action){
        JSONObject out=new JSONObject();try{
            String n=normalize(action);List<JSONObject> active=db.listActiveCharacters();JSONArray actors=new JSONArray();
            for(JSONObject h:active){
                String name=h.optString("name");
                if(n.contains(normalize(name))||active.size()==1){
                    JSONObject x=new JSONObject();
                    x.put("name",name).put("hp",h.optInt("hp")).put("max_hp",h.optInt("max_hp")).put("ac",h.optInt("ac"));
                    if(isMystara())appendMystaraActor(x,h);else{
                        int passive=10+rules.checkBonus(h,"Sabedoria","Percepção");JSONObject travel=db.getTravelRulesState();JSONObject paceEffects=travel.optJSONObject("active_pace_effects");if(paceEffects!=null)passive+=paceEffects.optInt("passive_perception_modifier",0);x.put("passive_perception",passive);
                        x.put("proficiency_bonus",rules.proficiencyBonus(h.optInt("level",1)));
                    }
                    actors.put(x);
                }
            }
            String activeProfile=isMystara()?MystaraRulesEngine.SYSTEM_LABEL:db.getCampaign().optString("rules_profile",DndRulesEngine.PROFILE_2014);
            out.put("rules_profile",activeProfile);
            if(!isMystara())out.put("mechanics_catalog",rules.mechanicsCatalog(activeProfile));
            out.put("travel_rules_state",db.getTravelRulesState());out.put("rule_retrieval_status",activeRuleRetrieval.optString("status","not_attached"));out.put("rule_evidence_count",activeRuleRetrieval.optInt("evidence_count",activeRuleRetrieval.optJSONArray("evidence")==null?0:activeRuleRetrieval.optJSONArray("evidence").length()));
            out.put("rules_boundary",new JSONObject().put("RAW","automático somente após gatilho confirmado").put("REGRA_OPCIONAL","exige habilitação explícita").put("DECISAO_DO_MESTRE","não aplicar silenciosamente").put("SIMULACAO_NARRATIVA","sem efeito mecânico"));
            out.put("actors",actors).put("scene_has_risk",hasRisk(n)).put("opposition_present",oppositionPresent(n));
            out.put("suggested_check",isMystara()?suggestedMystaraCheck(n,actors):suggested5eCheck(n));
            if(isMystara()){
                out.put("authority","O aplicativo resolve CA descendente, ataque por classe/nível, jogadas de proteção, testes iguais ou abaixo do atributo, habilidades de ladrão, surpresa, reação, moral e iniciativa 1d6. O Gemini apenas propõe quando existe incerteza real.");
                out.put("test_policy","Nunca use CD, bônus de proficiência, vantagem/desvantagem ou salvaguarda por atributo da 5e. Teste de habilidade/perícia geral: 1d20 igual ou abaixo do atributo. Habilidade de ladrão: d100 igual ou abaixo do percentual. Jogada de proteção: 1d20 igual ou acima do valor da ficha.");
            }else{
                out.put("authority","O aplicativo rola, calcula bônus, aplica PV, recursos, condições e combate. O Gemini apenas propõe quando existe incerteza real.");
                out.put("test_policy","Sem risco, oposição, custo ou incerteza relevante: não peça teste. Quando o ator não estiver claro, solicite seleção no aplicativo em vez de escolher um herói.");
            }
        }catch(Exception ignored){AppLog.ignored("MasterRulesArbiter","Falha tolerada; a execução continuou.",ignored);}return out;
    }

    private void appendMystaraActor(JSONObject x,JSONObject h)throws Exception{
        JSONObject sheet=parseObject(h.optString("sheet_json","{}"));
        String cls=sheet.optString("class_name",h.optString("class_name"));int level=sheet.optInt("level",h.optInt("level",1));
        x.put("system",MystaraRulesEngine.SYSTEM_ID).put("class_name",cls).put("level",level).put("ac_descending",h.optInt("ac",sheet.optInt("ac",9)));
        JSONObject mods=new JSONObject();mods.put("Força",mystaraRules.abilityModifier(sheet.optInt("str",h.optInt("str",10))));mods.put("Inteligência",mystaraRules.abilityModifier(sheet.optInt("int_score",h.optInt("int_score",10))));mods.put("Sabedoria",mystaraRules.abilityModifier(sheet.optInt("wis",h.optInt("wis",10))));mods.put("Destreza",mystaraRules.abilityModifier(sheet.optInt("dex",h.optInt("dex",10))));mods.put("Constituição",mystaraRules.abilityModifier(sheet.optInt("con",h.optInt("con",10))));mods.put("Carisma",mystaraRules.abilityModifier(sheet.optInt("cha",h.optInt("cha",10))));x.put("ability_modifiers",mods);
        x.put("saving_throws",mystaraRules.savesJson(cls,level));x.put("attack_target_ac0",mystaraRules.attackTargetAc0(cls,level,sheet.optInt("attack_rank",0)));
        x.put("general_skills",sheet.optJSONArray("skills_optional")!=null?sheet.optJSONArray("skills_optional"):new JSONArray());
        if("Ladrão".equalsIgnoreCase(cls))x.put("thief_skills",mystaraRules.thiefSkillsJson(level));
    }

    public JSONObject validate(JSONObject envelope,String action){
        if(envelope==null)return new JSONObject();JSONArray warnings=new JSONArray();int verifiedMechanics=0,blockedMechanics=0;try{
            Map<String,JSONObject> heroes=heroesByName();
            JSONObject test=envelope.optJSONObject("pending_test");if(test!=null&&test.optBoolean("required",true)){
                String actor=test.optString("character","").trim();if(actor.length()>0&&!heroes.containsKey(normalize(actor))){test.remove("character");test.put("actor_selection_required",true);warnings.put("O teste citava um personagem inexistente; o aplicativo pedirá a seleção do ator.");}
                String n=normalize(action+" "+db.getSceneState().optString("danger")+" "+db.getSceneState().optString("threats_json"));if(!hasRisk(n)&&!oppositionPresent(n)){envelope.put("pending_test",JSONObject.NULL);warnings.put("Teste removido porque não havia risco, oposição, custo ou incerteza relevante no estado atual.");}
                else if(isMystara())validateMystaraTest(test,warnings);else{
                    int dc=Math.max(5,Math.min(30,test.optInt("dc",10)));test.put("dc",dc);test.put("mode",validMode(test.optString("mode","Normal")));String ability=validAbility(test.optString("ability","Sabedoria"));test.put("ability",ability);String skill=validSkill(test.optString("skill",""));if(skill.length()>0)test.put("skill",skill);else test.remove("skill");applyTravelCheckMode(test,skill,warnings);applyConditionTestMode(test,heroes,warnings);
                }
            }
            JSONObject verifiedTest=envelope.optJSONObject("pending_test");
            if(verifiedTest!=null&&verifiedTest.optBoolean("required",true)&&activeRuleRetrieval.optBoolean("required",false)){
                JSONArray retrievedEvidence=activeRuleRetrieval.optJSONArray("evidence");
                boolean sufficient=activeRuleRetrieval.optBoolean("sufficient_for_mechanics",retrievedEvidence!=null&&retrievedEvidence.length()>0);
                if(!sufficient){
                    envelope.put("pending_test",JSONObject.NULL);
                    warnings.put("O teste foi bloqueado porque a consulta obrigatória aos manuais não encontrou procedimento pertinente.");
                }else if(retrievedEvidence!=null&&retrievedEvidence.length()>0){
                    JSONObject source=resolutionVerifier.bestEvidenceForTest(verifiedTest,activeRuleRetrieval);
                    if(source==null){envelope.put("pending_test",JSONObject.NULL);warnings.put("O teste foi bloqueado porque nenhuma passagem recuperada corresponde ao tipo de teste proposto.");}
                    else {String procedureId=source.optString("_matched_procedure_id","");verifiedTest.put("procedure_id",procedureId);verifiedTest.put("rule_support",new JSONObject().put("rule_id",source.optString("rule_id")).put("procedure_id",procedureId).put("book",source.optString("book")).put("page",source.optInt("page")).put("excerpt",source.optString("excerpt")));}
                }
            }
            boolean unresolvedTest=envelope.optJSONObject("pending_test")!=null&&envelope.optJSONObject("pending_test").optBoolean("required",true);
            JSONArray updates=envelope.optJSONArray("state_updates"),clean=new JSONArray();if(updates!=null)for(int i=0;i<updates.length();i++){
                JSONObject u=updates.optJSONObject(i);if(u==null)continue;JSONObject hero=heroes.get(normalize(u.optString("character")));if(hero==null||u.optString("reason").trim().length()==0)continue;JSONObject safe=new JSONObject();safe.put("character",hero.optString("name"));safe.put("reason",trim(u.optString("reason"),260));
                boolean mechanical=hasMechanicalMutation(u);JSONObject proposedBasis=u.optJSONObject("mechanical_basis");
                boolean basisOk=!mechanical||validMechanicalBasis(proposedBasis,action,envelope,hero);RuleResolutionVerifier.Result manualResolution=null;
                if(mechanical&&basisOk&&manualOnlyBasis(proposedBasis)){
                    manualResolution=resolutionVerifier.verifyStateUpdate(proposedBasis,u,hero,activeRuleRetrieval);
                    basisOk=manualResolution.valid;
                    if(!basisOk)warnings.put("Alteração mecânica de "+hero.optString("name")+" bloqueada: "+manualResolution.reason);
                    else proposedBasis.put("calculation",manualResolution.normalizedCalculation).put("calculation_verified",true);
                }
                if(mechanical&&!basisOk){blockedMechanics++;if(manualResolution==null)warnings.put("Alteração mecânica de "+hero.optString("name")+" foi bloqueada por não apresentar base verificável.");}
                if(basisOk){if(mechanical)verifiedMechanics++;int max=Math.max(1,hero.optInt("max_hp",1));if(u.has("hp_set"))safe.put("hp_set",Math.max(0,Math.min(max,u.optInt("hp_set",hero.optInt("hp")))));else if(u.has("hp_delta"))safe.put("hp_delta",Math.max(-max,Math.min(max,u.optInt("hp_delta",0))));copyArray(u,safe,"add_items",20);copyArray(u,safe,"remove_items",20);copyArray(u,safe,"condition_add",12);copyArray(u,safe,"condition_remove",12);copyObject(u,safe,"resource_changes");if(mechanical){safe.put("mechanical_basis",sanitizeBasis(u.optJSONObject("mechanical_basis")));String resolutionKey=u.optString("resolution_key").trim();if(resolutionKey.length()>0)safe.put("resolution_key",trim(resolutionKey,180));}}
                if(u.optString("notes_append").trim().length()>0)safe.put("notes_append",trim(u.optString("notes_append"),500));if(safe.length()>2)clean.put(safe);
            }envelope.put("state_updates",clean);
            JSONObject rawCombat=envelope.optJSONObject("combat");boolean rawCombatMechanical=rawCombat!=null&&((rawCombat.optJSONArray("enemies")!=null&&rawCombat.optJSONArray("enemies").length()>0)||(rawCombat.optJSONArray("updates")!=null&&rawCombat.optJSONArray("updates").length()>0));
            validateCombat(envelope,action,warnings);JSONObject verifiedCombat=envelope.optJSONObject("combat");boolean combatAccepted=verifiedCombat!=null&&!"none".equalsIgnoreCase(verifiedCombat.optString("action","none"))&&verifiedCombat.optJSONObject("mechanical_basis")!=null;if(combatAccepted)verifiedMechanics++;else if(rawCombatMechanical)blockedMechanics++;
            if(unresolvedTest){
                envelope.put("state_updates",new JSONArray());envelope.put("combat",new JSONObject().put("action","none"));envelope.put("scene_state",new JSONObject());envelope.put("npc_updates",new JSONArray());envelope.put("npc_cognition_updates",new JSONArray());envelope.put("knowledge_updates",new JSONArray());envelope.put("quest_updates",new JSONArray());envelope.put("faction_updates",new JSONArray());envelope.put("clock_updates",new JSONArray());envelope.put("world_events",new JSONArray());envelope.put("event_resolutions",new JSONArray());envelope.put("pending_resolutions",new JSONArray());envelope.put("journal_updates",new JSONArray());envelope.put("map_updates",new JSONArray());envelope.put("tactical_updates",new JSONArray());envelope.put("_deferred_until_test",true);warnings.put("Mudanças persistentes foram adiadas até a resolução do teste pendente.");
            }
            envelope.put("_rules_report",new JSONObject().put("warnings",warnings).put("validated",true).put("valid",true).put("profile",isMystara()?MystaraRulesEngine.SYSTEM_ID:"dnd5e").put("mechanical_policy","aplicar somente se calculado/verificado localmente; citação isolada não autoriza valor numérico").put("verified_mechanics",verifiedMechanics).put("blocked_mechanics",blockedMechanics));
        }catch(Exception failure){
            AppLog.e("MasterRulesArbiter","Validação mecânica bloqueada.",failure);
            try{
                clearMechanicalAndPersistentChanges(envelope);
                envelope.put("_rules_report",new JSONObject().put("warnings",warnings).put("validated",false).put("valid",false).put("error",failure.getClass().getSimpleName()+": "+String.valueOf(failure.getMessage())).put("verified_mechanics",0).put("blocked_mechanics",1));
            }catch(Exception ignored){AppLog.ignored("MasterRulesArbiter","Falha ao fechar a validação mecânica.",ignored);}
        }return envelope;
    }


    /** Runtime entrypoint for compiled supplement mechanics. Fail-closed and never promotes RAG/index data. */
    public JSONObject executeCompiledSupplement(String ruleId, JSONObject inputs, JSONObject requestedMutation){
        JSONObject in=inputs==null?new JSONObject():inputs;
        if(requestedMutation==null||requestedMutation.length()==0)return SupplementRuntimeGateway.execute(ruleId,in);
        return SupplementRuntimeGateway.mutationEnvelope(ruleId,in,requestedMutation);
    }

    /** Readiness probe used by UI/Barbara before offering a mechanical action. */
    public JSONObject compiledSupplementReadiness(String ruleId, JSONObject inputs){
        return SupplementRuntimeGateway.readiness(ruleId,inputs==null?new JSONObject():inputs);
    }

    /** Unified readiness for source-bound subclass/spell/item/statblock mechanics. */
    public JSONObject compiledContentReadiness(String kind,String name){return CompiledContentRuntimeGateway.readiness(kind,name);}

    /** Returns only an authorized compiled mechanics record; actual state mutation remains under the normal arbiter transaction path. */
    public JSONObject executeCompiledContent(String kind,String name,JSONObject context){return CompiledContentRuntimeGateway.execute(kind,name,context);}

    private static boolean hasMechanicalMutation(JSONObject u){
        if(u==null)return false;if(u.has("hp_set")||u.has("hp_delta"))return true;
        for(String key:new String[]{"add_items","remove_items","condition_add","condition_remove"}){JSONArray a=u.optJSONArray(key);if(a!=null&&a.length()>0)return true;}
        JSONObject resources=u.optJSONObject("resource_changes");return resources!=null&&resources.length()>0;
    }
    private boolean validMechanicalBasis(JSONObject basis,String action,JSONObject envelope,JSONObject hero){
        if(basis==null)return false;String type=normalize(basis.optString("source_type")),evidence=basis.optString("evidence").trim();if(evidence.length()<4)return false;
        if(type.contains("compiled supplement")||type.contains("suplemento compilado"))return SupplementContentRegistry.verifyMechanicalBasis(basis);
        if(type.contains("manual")||type.contains("regra"))return verifyManualBasis(basis,envelope);
        if(type.contains("resolved test")||type.contains("teste resolvido")){String normalizedAction=normalize(action);return (normalizedAction.contains("mecanica local verificada")||normalizedAction.contains("resultado de teste obrigatorio"))&&verifyBoundLocalRoll(basis,hero);}
        if(type.contains("recent local roll")||type.contains("rolagem local"))return verifyBoundLocalRoll(basis,hero);
        if(type.contains("authorized activity")||type.contains("atividade autorizada"))return normalize(action).contains("atividade autorizada")&&normalize(evidence).contains("atividade")&&basis.optString("resolution_key","").trim().length()>0;
        if(type.contains("local engine")||type.contains("regra local")){JSONObject tick=envelope==null?null:envelope.optJSONObject("_local_tick");return tick!=null&&tick.optBoolean("committed",false);}
        return false;
    }

    private static boolean manualOnlyBasis(JSONObject basis){String type=normalize(basis==null?"":basis.optString("source_type"));return type.contains("manual")||type.contains("regra");}

    private static void clearMechanicalAndPersistentChanges(JSONObject envelope)throws Exception{
        for(String key:new String[]{"state_updates","npc_updates","npc_cognition_updates","knowledge_updates","quest_updates","faction_updates","guild_updates","clock_updates","world_events","event_resolutions","pending_resolutions","journal_updates","map_updates","tactical_updates"})envelope.put(key,new JSONArray());
        envelope.put("combat",new JSONObject().put("action","none"));
        envelope.put("scene_state",new JSONObject());
        envelope.put("_local_tick",new JSONObject());
        envelope.put("_deferred_until_test",true);
    }

    private boolean verifyManualBasis(JSONObject basis,JSONObject envelope){
        String book=basis.optString("book").trim(),excerpt=basis.optString("excerpt").trim();int page=basis.optInt("page",0);if(book.length()==0||page<=0||excerpt.length()<12)return false;
        String wanted=normalize(baseName(book));if(!db.verifyEnabledManualExcerpt(book,page,excerpt))return false;
        if(activeRuleRetrieval.optBoolean("required",false)&&!matchesRetrievedEvidence(book,page,excerpt))return false;
        JSONArray sources=envelope==null?null:envelope.optJSONArray("sources");if(sources==null||sources.length()==0)return false;
        for(int i=0;i<sources.length();i++){Object raw=sources.opt(i);String sourceBook="";int sourcePage=0;String sourceExcerpt="";
            if(raw instanceof JSONObject){JSONObject src=(JSONObject)raw;sourceBook=src.optString("book",src.optString("title",src.optString("source")));sourcePage=src.optInt("page",0);sourceExcerpt=src.optString("excerpt",src.optString("text"));}
            else sourceBook=String.valueOf(raw);
            if(normalize(sourceBook).contains(wanted)&&(sourcePage==0||sourcePage==page)&&(sourceExcerpt.length()==0||textOverlap(excerpt,sourceExcerpt)>=0.35d))return true;
        }
        return false;
    }

    private boolean matchesRetrievedEvidence(String book,int page,String excerpt){
        JSONArray evidence=activeRuleRetrieval.optJSONArray("evidence");if(evidence==null||evidence.length()==0)return false;String wanted=normalize(baseName(book));
        for(int i=0;i<evidence.length();i++){JSONObject source=evidence.optJSONObject(i);if(source==null)continue;String sourceBook=source.optString("book",source.optString("title"));int sourcePage=source.optInt("page",0);String sourceExcerpt=source.optString("excerpt");
            if((normalize(sourceBook).contains(wanted)||wanted.contains(normalize(sourceBook)))&&sourcePage==page&&textOverlap(excerpt,sourceExcerpt)>=0.45d)return true;
        }
        return false;
    }

    private boolean verifyBoundLocalRoll(JSONObject basis,JSONObject hero){
        JSONObject calculation=basis==null?null:basis.optJSONObject("calculation");if(calculation==null&&basis!=null)calculation=basis.optJSONObject("resolution");
        if(calculation==null||calculation.optLong("roll_id",0)<=0)return false;
        JSONObject roll=db.getDiceHistoryById(calculation.optLong("roll_id",0));
        return MechanicalRollBinding.verify(roll,calculation,hero,System.currentTimeMillis()).valid;
    }
    private static String baseName(String value){String s=value==null?"":value.replace('\\','/').trim();int slash=s.lastIndexOf('/');return slash>=0?s.substring(slash+1):s;}
    private static double textOverlap(String a,String b){java.util.HashSet<String> left=new java.util.HashSet<String>(),right=new java.util.HashSet<String>();for(String x:normalize(a).split("\\s+"))if(x.length()>3)left.add(x);for(String x:normalize(b).split("\\s+"))if(x.length()>3)right.add(x);if(left.isEmpty()||right.isEmpty())return 0d;int hit=0;for(String x:left)if(right.contains(x))hit++;return hit/(double)Math.min(left.size(),right.size());}

    private static JSONObject sanitizeBasis(JSONObject basis){JSONObject out=new JSONObject();if(basis==null)return out;try{out.put("source_type",trim(basis.optString("source_type"),60));out.put("evidence",trim(basis.optString("evidence"),300));if(basis.optString("rule_id").trim().length()>0)out.put("rule_id",trim(basis.optString("rule_id"),160));if(basis.optString("procedure_id").trim().length()>0)out.put("procedure_id",trim(basis.optString("procedure_id"),160));if(basis.optString("book").trim().length()>0)out.put("book",trim(basis.optString("book"),120));if(basis.optInt("page",0)>0)out.put("page",basis.optInt("page"));if(basis.optString("excerpt").trim().length()>0)out.put("excerpt",trim(basis.optString("excerpt"),800));if(basis.optString("resolution_key").trim().length()>0)out.put("resolution_key",trim(basis.optString("resolution_key"),180));JSONObject calculation=basis.optJSONObject("calculation");if(calculation==null)calculation=basis.optJSONObject("resolution");if(calculation!=null)out.put("calculation",new JSONObject(calculation.toString()));if(basis.optBoolean("calculation_verified",false))out.put("calculation_verified",true);}catch(Exception ignored){AppLog.ignored("MasterRulesArbiter","Falha ao limpar base mecânica.",ignored);}return out;}

    private void applyConditionTestMode(JSONObject test,Map<String,JSONObject> heroes,JSONArray warnings)throws Exception{String actor=test.optString("character","").trim();if(actor.length()==0)return;JSONObject hero=heroes.get(normalize(actor));if(hero==null)return;JSONObject combatant=db.findCombatantByName(hero.optString("name"));String conditions=combatant.length()>0?combatant.optString("conditions",""):parseObject(hero.optString("sheet_json","{}")).optString("conditions","");if(conditions.trim().isEmpty())return;String type=normalize(test.optString("type","ability")),ability=test.optString("ability",""),skill=test.optString("skill","");boolean adv="Vantagem".equals(test.optString("mode")),dis="Desvantagem".equals(test.optString("mode"));if(type.contains("save")||type.contains("salv")){JSONObject p=ConditionRulesEngine.savingThrowProfile(conditions,ability);if(p.optBoolean("automatic_failure",false)){test.put("automatic_failure",true).put("mode","Normal");warnings.put("A condição de "+hero.optString("name")+" impõe falha automática nesta salvaguarda.");return;}dis|=p.optBoolean("disadvantage",false);}else{JSONObject p=ConditionRulesEngine.abilityCheckProfile(conditions,ability,skill,false);if(p.optBoolean("automatic_failure",false)){test.put("automatic_failure",true).put("mode","Normal");warnings.put("A condição de "+hero.optString("name")+" impede este teste dependente de visão.");return;}dis|=p.optBoolean("disadvantage",false);}if(adv&&dis)test.put("mode","Normal");else if(dis)test.put("mode","Desvantagem");}

    private void applyTravelCheckMode(JSONObject test,String skill,JSONArray warnings)throws Exception{
        JSONObject state=db.getTravelRulesState(),effects=state.optJSONObject("active_pace_effects");if(effects==null)return;String n=normalize(skill);
        JSONArray disadvantage=effects.optJSONArray("disadvantage"),advantage=effects.optJSONArray("advantage");boolean dis=arrayContains(disadvantage,n),adv=arrayContains(advantage,n);
        JSONArray environmental=state.optJSONArray("active_environmental_effects");if(environmental!=null&&n.contains("percepcao"))for(int i=0;i<environmental.length();i++){JSONObject e=environmental.optJSONObject(i);if(e!=null&&normalize(e.optString("effect")).contains("precipitacao forte"))dis=true;}
        if(dis&&adv){test.put("mode","Normal");warnings.put("Vantagem e Desvantagem de viagem se cancelaram.");}else if(dis)test.put("mode","Desvantagem");else if(adv)test.put("mode","Vantagem");
    }

    private static boolean arrayContains(JSONArray a,String normalized){if(a==null||normalized.length()==0)return false;for(int i=0;i<a.length();i++)if(normalize(a.optString(i)).contains(normalized)||normalized.contains(normalize(a.optString(i))))return true;return false;}
    private void validateMystaraTest(JSONObject test,JSONArray warnings)throws Exception{
        String type=normalize(test.optString("type",""));
        if(type.length()==0){if(test.has("save_type"))type="save";else if(test.has("thief_skill"))type="thief skill";else type="ability";}
        if(type.contains("save")||type.contains("protec")){
            test.put("type","save");test.put("save_type",validMystaraSave(test.optString("save_type",MystaraRulesEngine.SAVE_NAMES[4])));
        }else if(type.contains("thief")||type.contains("ladrao")){
            test.put("type","thief_skill");test.put("thief_skill",validThiefSkill(test.optString("thief_skill","Mover-se em Silêncio")));
        }else{
            test.put("type","ability");test.put("ability",validAbility(test.optString("ability","Destreza")));test.put("modifier",Math.max(-5,Math.min(5,test.optInt("modifier",0))));
            String general=test.optString("general_skill",test.optString("skill",""));if(general.length()>0)test.put("general_skill",trim(general,80));
        }
        if(test.has("dc")||test.has("mode")){warnings.put("CD e vantagem/desvantagem foram removidas do teste de Mystara.");}
        test.remove("dc");test.remove("mode");test.remove("skill");
    }

    private void validateCombat(JSONObject envelope,String playerAction,JSONArray warnings)throws Exception{
        JSONObject combat=envelope.optJSONObject("combat");
        if(combat==null){envelope.put("combat",new JSONObject().put("action","none"));return;}
        String action=normalize(combat.optString("action","none"));
        if(!action.matches("none|start|update|end")){combat.put("action","none");warnings.put("Ação de combate desconhecida foi ignorada.");return;}
        combat.put("action",action);
        if("none".equals(action)||"end".equals(action)){combat.remove("enemies");combat.remove("updates");return;}
        JSONArray enemies=combat.optJSONArray("enemies"),updates=combat.optJSONArray("updates");
        boolean mechanical=(enemies!=null&&enemies.length()>0)||(updates!=null&&updates.length()>0);
        JSONObject basis=combat.optJSONObject("mechanical_basis");
        boolean enemyStats=enemies!=null&&enemies.length()>0;String basisType=normalize(basis==null?"":basis.optString("source_type"));
        boolean updatesExisting=updates!=null&&updates.length()>0;boolean manualBasis=basisType.contains("manual")||basisType.contains("regra");
        boolean basisValid=validMechanicalBasis(basis,playerAction,envelope,null)&&(!enemyStats||manualBasis);
        if(basisValid&&enemyStats&&manualBasis){
            RuleResolutionVerifier.Result verifiedEnemies=resolutionVerifier.verifyEnemyStatBlocks(basis,enemies,activeRuleRetrieval,isMystara());
            basisValid=verifiedEnemies.valid;
            if(!basisValid)warnings.put("Criação de inimigos bloqueada: "+verifiedEnemies.reason);
            else basis.put("calculation",verifiedEnemies.normalizedCalculation).put("calculation_verified",true);
        }
        if(basisValid&&updatesExisting&&manualBasis){
            for(int i=0;i<updates.length();i++){
                JSONObject proposed=updates.optJSONObject(i);if(proposed==null)continue;JSONObject existing=db.findCombatantByName(proposed.optString("name"));
                if(existing.length()==0){basisValid=false;break;}
                RuleResolutionVerifier.Result verified=resolutionVerifier.verifyStateUpdate(basis,proposed,existing,activeRuleRetrieval);
                if(!verified.valid){basisValid=false;warnings.put("Atualização de combate bloqueada: "+verified.reason);break;}
                basis.put("calculation",verified.normalizedCalculation).put("calculation_verified",true);
            }
        }
        if(mechanical&&!basisValid){
            combat.put("action","none");combat.put("enemies",new JSONArray());combat.put("updates",new JSONArray());
            warnings.put("A alteração de combate foi bloqueada por não apresentar base mecânica verificável.");return;
        }
        if(mechanical){combat.put("mechanical_basis",sanitizeBasis(basis));String resolutionKey=combat.optString("resolution_key").trim();if(resolutionKey.length()>0)combat.put("resolution_key",trim(resolutionKey,180));}
        if(enemies!=null){
            JSONArray clean=new JSONArray();
            for(int i=0;i<enemies.length()&&clean.length()<30;i++){
                JSONObject e=enemies.optJSONObject(i);if(e==null||e.optString("name").trim().length()==0)continue;
                JSONObject x=new JSONObject();x.put("name",trim(e.optString("name"),80));x.put("block_id",trim(e.optString("block_id"),180));x.put("count",Math.max(1,Math.min(20,e.optInt("count",1))));
                x.put("hp",Math.max(1,Math.min(999,e.optInt("hp",1))));int ac=e.optInt("ac",isMystara()?9:10);
                x.put("ac",isMystara()?Math.max(-10,Math.min(19,ac)):Math.max(1,Math.min(30,ac)));x.put("conditions",trim(e.optString("conditions"),200));
                if(isMystara()){x.put("initiative_bonus",0);x.put("morale",Math.max(2,Math.min(12,e.optInt("morale",7))));x.put("hit_dice",trim(e.optString("hit_dice"),30));x.put("attacks",trim(e.optString("attacks"),160));}
                else {x.put("initiative_bonus",Math.max(-10,Math.min(20,e.optInt("initiative_bonus",0))));x.put("attacks",trim(e.optString("attacks"),500));x.put("speed",trim(e.optString("speed"),120));x.put("resistances",trim(e.optString("resistances"),300));x.put("immunities",trim(e.optString("immunities"),300));x.put("vulnerabilities",trim(e.optString("vulnerabilities"),300));}
                clean.put(x);
            }
            combat.put("enemies",clean);
        }
        JSONArray safeUpdates=new JSONArray();
        if(updates!=null)for(int i=0;i<updates.length()&&safeUpdates.length()<60;i++){
            JSONObject u=updates.optJSONObject(i);if(u==null)continue;String name=trim(u.optString("name"),100);if(name.length()==0)continue;
            JSONObject existing=db.findCombatantByName(name);if(existing.length()==0){warnings.put("Atualização ignorada: combatente inexistente — "+name+".");continue;}
            JSONObject x=new JSONObject().put("name",existing.optString("name",name));int max=Math.max(1,existing.optInt("max_hp",existing.optInt("hp",1)));
            if(u.has("hp_set"))x.put("hp_set",Math.max(0,Math.min(max,u.optInt("hp_set",existing.optInt("hp")))));
            else if(u.has("hp_delta"))x.put("hp_delta",Math.max(-max,Math.min(max,u.optInt("hp_delta",0))));
            if(u.optString("conditions").trim().length()>0)x.put("conditions",trim(u.optString("conditions"),240));
            if(x.length()>1)safeUpdates.put(x);
        }
        combat.put("updates",safeUpdates);
        combat.put("reason",trim(combat.optString("reason"),300));
    }

    private JSONObject suggested5eCheck(String n){JSONObject x=new JSONObject();try{if(!hasRisk(n)&&!oppositionPresent(n))return x;if(contains(n,"furtiv","escond","silenc"))x.put("ability","Destreza").put("skill","Furtividade");else if(contains(n,"perce","observo","procuro","vigio"))x.put("ability","Sabedoria").put("skill","Percepção");else if(contains(n,"investig","examino","mecanismo"))x.put("ability","Inteligência").put("skill","Investigação");else if(contains(n,"convenc","persuad","negoci"))x.put("ability","Carisma").put("skill","Persuasão");else if(contains(n,"engan","minto","blef"))x.put("ability","Carisma").put("skill","Enganação");else if(contains(n,"intimid"))x.put("ability","Carisma").put("skill","Intimidação");else if(contains(n,"escal","forco","arrombo"))x.put("ability","Força").put("skill","Atletismo");else x.put("ability","Sabedoria");}catch(Exception ignored){AppLog.ignored("MasterRulesArbiter","Falha tolerada; a execução continuou.",ignored);}return x;}

    private JSONObject suggestedMystaraCheck(String n,JSONArray actors){JSONObject x=new JSONObject();try{
        if(!hasRisk(n)&&!oppositionPresent(n))return x;
        boolean thief=false;for(int i=0;i<actors.length();i++)if("Ladrão".equalsIgnoreCase(actors.optJSONObject(i).optString("class_name")))thief=true;
        if(contains(n,"fechadura","tranca","gazua")){if(thief)x.put("type","thief_skill").put("thief_skill","Abrir Fechaduras");else x.put("type","ability").put("ability","Destreza").put("general_skill","Trapaças");}
        else if(contains(n,"armadilha","mecanismo")){if(thief)x.put("type","thief_skill").put("thief_skill","Encontrar Armadilhas");else x.put("type","ability").put("ability","Inteligência").put("general_skill","Armadilha");}
        else if(contains(n,"furtiv","silenc")){if(thief)x.put("type","thief_skill").put("thief_skill","Mover-se em Silêncio");else x.put("type","ability").put("ability","Destreza").put("general_skill","Furtividade");}
        else if(contains(n,"escond","sombra")){if(thief)x.put("type","thief_skill").put("thief_skill","Esconder-se nas Sombras");else x.put("type","ability").put("ability","Destreza").put("general_skill","Furtividade");}
        else if(contains(n,"ouvir","escuto","ruido")){if(thief)x.put("type","thief_skill").put("thief_skill","Ouvir Ruídos");else x.put("type","ability").put("ability","Sabedoria").put("general_skill","Prontidão");}
        else if(contains(n,"perce","observo","vigio","perigo"))x.put("type","ability").put("ability","Sabedoria").put("general_skill","Senso de Perigo");
        else if(contains(n,"rastre","sobreviv","natureza"))x.put("type","ability").put("ability","Inteligência").put("general_skill",contains(n,"rastre")?"Rastreamento":"Sobrevivência");
        else if(contains(n,"convenc","persuad","negoci"))x.put("type","ability").put("ability","Carisma").put("general_skill","Persuasão");
        else if(contains(n,"engan","minto","blef","disfar"))x.put("type","ability").put("ability","Carisma").put("general_skill","Dissimulação");
        else if(contains(n,"intimid"))x.put("type","ability").put("ability","Força").put("general_skill","Intimidação");
        else if(contains(n,"escal")){if(thief)x.put("type","thief_skill").put("thief_skill","Escalar Paredes");else x.put("type","ability").put("ability","Destreza").put("general_skill","Montanhismo");}
        else if(contains(n,"forco","arrombo"))x.put("type","ability").put("ability","Força").put("general_skill","Força Bruta");
        else x.put("type","ability").put("ability","Sabedoria");
        x.put("mechanic","1d20 igual ou abaixo do atributo; habilidade de ladrão usa d100 igual ou abaixo do percentual");
    }catch(Exception ignored){AppLog.ignored("MasterRulesArbiter","Falha tolerada; a execução continuou.",ignored);}return x;}

    private Map<String,JSONObject> heroesByName(){Map<String,JSONObject> out=new HashMap<String,JSONObject>();for(JSONObject h:db.listActiveCharacters())out.put(normalize(h.optString("name")),h);return out;}
    private boolean isMystara(){return "mystara".equals(db.getActiveMapProfile());}
    private static boolean hasRisk(String n){return contains(n,"perigo","armadilha","precipicio","veneno","ferido","morte","hostil","combate","ataque","embosc","fuga","desabar","tempo limitado","sob pressao")||contains(n,"tento","arris","forco","invado");}
    private static boolean oppositionPresent(String n){return contains(n,"inimigo","guarda","monstro","criatura","oponente","resiste","disputa","persegue","suspeita","hostil");}
    private static String validAbility(String raw){String n=normalize(raw);if(n.startsWith("for"))return "Força";if(n.startsWith("des"))return "Destreza";if(n.startsWith("con"))return "Constituição";if(n.startsWith("int"))return "Inteligência";if(n.startsWith("car"))return "Carisma";return "Sabedoria";}
    private static String validSkill(String raw){String n=normalize(raw);String[] skills={"Acrobacia","Arcanismo","Atletismo","Atuação","Enganação","Furtividade","História","Intimidação","Intuição","Investigação","Lidar com Animais","Medicina","Natureza","Percepção","Persuasão","Prestidigitação","Religião","Sobrevivência"};for(String s:skills)if(normalize(s).equals(n))return s;return "";}
    private static String validMode(String raw){String n=normalize(raw);if(n.contains("desvant"))return "Desvantagem";if(n.contains("vant"))return "Vantagem";return "Normal";}
    private static String validMystaraSave(String raw){String n=normalize(raw);if(n.contains("raio")||n.contains("veneno"))return MystaraRulesEngine.SAVE_NAMES[0];if(n.contains("varinha"))return MystaraRulesEngine.SAVE_NAMES[1];if(n.contains("paralis")||n.contains("petr"))return MystaraRulesEngine.SAVE_NAMES[2];if(n.contains("baforada")||n.contains("drag"))return MystaraRulesEngine.SAVE_NAMES[3];return MystaraRulesEngine.SAVE_NAMES[4];}
    private static String validThiefSkill(String raw){String n=normalize(raw);for(String s:MystaraRulesEngine.THIEF_SKILL_NAMES)if(normalize(s).equals(n)||normalize(s).contains(n)||n.contains(normalize(s)))return s;if(n.contains("furt")||n.contains("silenc"))return "Mover-se em Silêncio";if(n.contains("escond"))return "Esconder-se nas Sombras";if(n.contains("ouvir"))return "Ouvir Ruídos";if(n.contains("armad"))return "Encontrar Armadilhas";if(n.contains("escal"))return "Escalar Paredes";return "Abrir Fechaduras";}
    private static void copyArray(JSONObject from,JSONObject to,String key,int max)throws Exception{JSONArray a=from.optJSONArray(key);if(a==null)return;JSONArray c=new JSONArray();for(int i=0;i<a.length()&&i<max;i++)c.put(a.opt(i));to.put(key,c);}
    private static void copyObject(JSONObject from,JSONObject to,String key)throws Exception{JSONObject o=from.optJSONObject(key);if(o!=null)to.put(key,o);}
    private static JSONObject parseObject(String raw){try{return new JSONObject(raw==null?"{}":raw);}catch(Exception e){return new JSONObject();}}
    private static String trim(String v,int max){String s=v==null?"":v.trim();return s.length()<=max?s:s.substring(0,max)+"…";}
    private static boolean contains(String n,String... values){for(String v:values)if(n.contains(normalize(v)))return true;return false;}
    private static String normalize(String value){if(value==null)return "";String n=Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
}
