"""Wire official New Leadership checks (pp. 514, 520, 537) and settlement/character hooks to the Kingdom repository, PF2e engine, and UI."""
from pathlib import Path

def once(s,a,b):
    n=s.count(a)
    if n!=1: raise ValueError("NEW_LEADERSHIP_ANCHOR "+str(n)+" "+a[:130])
    return s.replace(a,b,1)

def repo(s):
    # 1. Founding assignment is free; Upkeep Step 1 allows reselecting the 4 invested roles without a check when leader identity is unchanged.
    old="    public void assignLeader(String role,String kind,long entityId,String name,boolean invested,String notes){"
    replacement="""    public void assignLeader(String role,String kind,long entityId,String name,boolean invested,String notes){
        if(state().optInt("turn_number",0)>0){
            String k=key(role);JSONObject old=db.queryOne("SELECT * FROM kingmaker_leaders WHERE world_profile=? AND role_key=?",new String[]{profile(),k});
            boolean sameLeader=old.optString("leader_name").equals(clean(name))&&old.optString("entity_kind").equals(clean(kind))&&old.optLong("entity_id")==Math.max(0,entityId);
            boolean upkeepReinvest=sameLeader&&"UPKEEP".equals(activeTurn().optString("phase"));
            if(!upkeepReinvest&&(!sameLeader||old.optInt("invested")!=(invested?1:0)))
                throw new IllegalStateException("Depois da fundação, use a atividade Nova Liderança com teste de Reino.");
        }
        assignLeaderUnchecked(role,kind,entityId,name,invested,notes);
    }
    public JSONObject setRoleInvested(String role,boolean invested)throws Exception{
        requireEnabled();
        String k=key(role);
        JSONObject current=db.queryOne("SELECT * FROM kingmaker_leaders WHERE world_profile=? AND role_key=?",new String[]{profile(),k});
        if(current.length()==0||current.optInt("vacant",1)==1)throw new IllegalStateException("Não é possível investir em um cargo vago.");
        if(state().optInt("turn_number",0)>0&&!"UPKEEP".equals(activeTurn().optString("phase")))
            throw new IllegalStateException("A seleção dos quatro cargos investidos ocorre na fundação ou na Etapa 1 da Manutenção (Upkeep Step 1).");
        assignLeaderUnchecked(k,current.optString("entity_kind"),current.optLong("entity_id"),current.optString("leader_name"),invested,current.optString("notes"));
        return db.queryOne("SELECT * FROM kingmaker_leaders WHERE world_profile=? AND role_key=?",new String[]{profile(),k});
    }
    public JSONArray reselectInvestedRoles(final JSONArray investedRoleKeys)throws Exception{
        requireEnabled();
        if(state().optInt("turn_number",0)>0&&!"UPKEEP".equals(activeTurn().optString("phase")))
            throw new IllegalStateException("A seleção dos quatro cargos investidos ocorre na fundação ou na Etapa 1 da Manutenção (Upkeep Step 1).");
        final JSONArray[] out={new JSONArray()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            List<JSONObject> current=leaders();
            java.util.Set<String> auto=new java.util.LinkedHashSet<String>();
            for(JSONObject row:current)if(row.optInt("vacant",1)==0&&officialAutomaticInvestment(row))auto.add(row.optString("role_key"));
            java.util.Set<String> effective=KingmakerLeadershipSemantics.validateInvestedReselection(new JSONArray(current),investedRoleKeys,auto);
            long now=System.currentTimeMillis();
            for(JSONObject row:current){
                String rk=row.optString("role_key");
                int nextInv=row.optInt("vacant",1)==0&&effective.contains(rk)?1:0;
                ContentValues v=new ContentValues();v.put("invested",nextInv);v.put("updated_at",now);
                db.getWritableDatabase().update("kingmaker_leaders",v,"world_profile=? AND role_key=?",new String[]{profile(),rk});
            }
            out[0]=new JSONArray(leaders());
        }});
        return out[0];
    }
    private boolean isRoleIncumbentUnavailable(String roleKey){
        JSONObject row=db.queryOne("SELECT * FROM kingmaker_leaders WHERE world_profile=? AND role_key=?",new String[]{profile(),key(roleKey)});
        if(row.length()==0||row.optInt("vacant",1)==1)return true;
        String notes=row.optString("notes","").toLowerCase(Locale.ROOT);
        if(notes.contains("morto")||notes.contains("dead")||notes.contains("indispon")||notes.contains("unavailable"))return true;
        String kind=row.optString("entity_kind",""),leaderName=clean(row.optString("leader_name",""));
        long eid=row.optLong("entity_id",0);
        if("companion".equalsIgnoreCase(kind)||"npc".equalsIgnoreCase(kind)){
            JSONObject comp=db.queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND (name=? OR companion_key=? OR id=?)",new String[]{profile(),leaderName,leaderName,String.valueOf(eid)});
            if(comp.length()>0&&comp.optInt("available",1)!=1)return true;
        }
        if(eid>0&&("character".equalsIgnoreCase(kind)||"pc".equalsIgnoreCase(kind)||kind.isEmpty())){
            JSONObject ch=db.getCharacter(eid);
            if(ch.length()>0&&(ch.optInt("active",1)!=1||(ch.optInt("hp",1)<=0&&jsonObject(ch.optString("sheet_json","{}")).optBoolean("dead",false))))return true;
        }
        return false;
    }
    private void assignLeaderUnchecked(String role,String kind,long entityId,String name,boolean invested,String notes){"""
    s=once(s,old,replacement)
    # 2. Enforce strict single-role-per-leader across all 8 roles (both by entity_id and by leader_name) in assignLeaderUnchecked.
    dup_anchor='if(id>0){JSONObject duplicate=db.queryOne("SELECT * FROM kingmaker_leaders WHERE world_profile=? AND role_key!=? AND entity_kind=? AND entity_id=? AND vacant=0",new String[]{profile(),roleKey,entityKind,String.valueOf(id)});if(duplicate.length()>0)throw new IllegalStateException("A mesma pessoa só pode ocupar um cargo de liderança.");}'
    dup_replacement='if(id>0){JSONObject duplicate=db.queryOne("SELECT * FROM kingmaker_leaders WHERE world_profile=? AND role_key!=? AND entity_kind=? AND entity_id=? AND vacant=0",new String[]{profile(),roleKey,entityKind,String.valueOf(id)});if(duplicate.length()>0)throw new IllegalStateException("A mesma pessoa só pode ocupar um cargo de liderança.");}KingmakerLeadershipSemantics.validateSingleRolePerLeader(new JSONArray(leaders()),roleKey,entityKind,id,leader);'
    s=once(s,dup_anchor,dup_replacement)

    # 3. Replace resolveNewLeadership and resolveNewLeadershipApproval with full book-faithful implementation.
    a=s.index("    public JSONObject resolveNewLeadership(")
    b=s.index("    public JSONObject resolveProclamation(",a)
    middle="""    public JSONObject resolveNewLeadership(String role,String entityKind,long entityId,String name,boolean invested,String notes)throws Exception{
        if(state().optInt("turn_number",0)>0)throw new IllegalStateException("Nova Liderança exige d20 e perícia. Use resolveKingdomActivity.");
        assignLeader(role,entityKind,entityId,name,invested,notes);
        return db.queryOne("SELECT * FROM kingmaker_leaders WHERE world_profile=? AND role_key=?",new String[]{profile(),key(role)});
    }

    public JSONObject resolveNewLeadership(final String role,final String entityKind,final long entityId,final String name,final boolean invested,final int natural,final String skillKey,final String notes)throws Exception{
        return resolveNewLeadership(role,entityKind,entityId,name,invested,natural,skillKey,0,false,notes);
    }

    public JSONObject resolveNewLeadership(final String role,final String entityKind,final long entityId,final String name,final boolean invested,final int natural,final String skillKey,final int jaethalD3,final boolean emergency,final String notes)throws Exception{
        requireEnabled();final String r=key(role),sk=key(skillKey);
        if(!rules.validLeadershipRole(role)||clean(name).isEmpty()||natural<1||natural>20)throw new IllegalArgumentException("Cargo, líder e d20 válidos são obrigatórios.");
        KingmakerLeadershipSemantics.skillCircumstance(r,sk);
        if(state().optInt("turn_number",0)==0)throw new IllegalStateException("Durante a fundação, nomeie líderes gratuitamente, sem teste.");
        final boolean vacantNow=roleVacant(r),unavailableNow=isRoleIncumbentUnavailable(r);
        final JSONObject timing=KingmakerLeadershipSemantics.validateTiming(state().optInt("turn_number",0),activeTurn().optString("phase"),vacantNow,unavailableNow,emergency);
        if(activeTurn().length()==0&&!vacantNow&&!unavailableNow&&!emergency)throw new IllegalStateException("Abra o turno do Reino para realizar Nova Liderança.");
        final JSONObject[] receipt={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject comp=db.queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND (name=? OR companion_key=?)",new String[]{profile(),clean(name),clean(name)});
            if(comp.length()>0&&!KingmakerCompanionSemantics.canLead(comp,state().optString("reputation_mode","fame")))
                throw new IllegalStateException("O companheiro ainda não aceita a nomeação.");
            boolean preferred=comp.length()>0&&KingmakerCompanionSemantics.automaticInvestment(comp,r,twinsCurrentPersona());
            JSONObject check=recordCheck("new_leadership","Nova Liderança",role,sk,natural,0,"Kingmaker Adventure Path p.520",notes,false);
            String original=check.optString("degree"),degree=KingmakerLeadershipSemantics.improved(original,preferred);
            int currentTurn=state().optInt("turn_number",1);
            boolean jaethal="jaethal".equals(comp.optString("companion_key")),famous=!"infamy".equals(state().optString("reputation_mode","fame"));
            int die=jaethal&&famous?(jaethalD3>=1&&jaethalD3<=3?jaethalD3:(new java.security.SecureRandom().nextInt(3)+1)):0;
            JSONObject outcome=KingmakerLeadershipSemantics.appointment(degree,r,currentTurn,jaethal,famous,die);
            outcome.put("raw_degree",original).put("companion_degree_improved",preferred).put("timing_mode",timing.optString("mode")).put("check",check);
            String vacatedRole="";
            if("CRITICAL_FAILURE".equals(degree))assignLeaderUnchecked(role,"",0,"",false,notes);
            else{
                for(JSONObject occupied:leaders()){
                    if(occupied.optInt("vacant")==1||r.equals(occupied.optString("role_key")))continue;
                    boolean matchingId=entityId>0&&entityId==occupied.optLong("entity_id")&&entityKind.equals(occupied.optString("entity_kind"));
                    boolean matchingName=clean(name).equalsIgnoreCase(occupied.optString("leader_name"));
                    if(matchingId||matchingName){
                        vacatedRole=occupied.optString("role_key");
                        assignLeaderUnchecked(vacatedRole,"",0,"",false,"Mudança oficial de cargo");
                    }
                }
                boolean effectiveInvested=invested||(comp.length()>0&&preferred);
                assignLeaderUnchecked(role,entityKind,entityId,name,effectiveInvested,notes);
            }
            if(!vacatedRole.isEmpty())outcome.put("vacated_previous_role",vacatedRole);
            outcome.put("auto_invested_companion",comp.length()>0&&preferred&&!"CRITICAL_FAILURE".equals(degree));
            int unrest=outcome.optInt("new_leadership_unrest");
            if(unrest>0)adjustState(new JSONObject().put("unrest",unrest),"Nova Liderança: "+role);
            check.put("degree",degree).put("degree_before_companion",original).put("new_leadership_circumstance",KingmakerLeadershipSemantics.skillCircumstance(r,sk));
            upsertFeature("leadership:"+r,"leadership_assignment","Nova Liderança • "+role,Math.max(1,state().optInt("level",1)),outcome.toString(),"Kingmaker Adventure Path p.520; Companion Guide","Resultado da nomeação e aceitação popular");
            ContentValues log=new ContentValues();log.put("degree",degree);log.put("effects_json",check.toString());
            db.getWritableDatabase().update("kingmaker_turn_activities",log,"id=? AND world_profile=?",new String[]{String.valueOf(check.optLong("activity_id")),profile()});
            receipt[0]=outcome;
        }});return receipt[0];
    }

    public JSONObject resolveNewLeadershipApproval(final String role,final String skillKey,final int natural)throws Exception{
        requireEnabled();final String r=key(role),sk=key(skillKey);
        KingmakerLeadershipSemantics.skillCircumstance(r,sk);
        final JSONObject[] receipt={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject saved=db.queryOne("SELECT * FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),"leadership:"+r});
            JSONObject status=jsonObject(saved.optString("effects_json","{}")),turn=activeTurn();
            int number=state().optInt("turn_number",0);
            if(!status.optBoolean("approval_pending")||turn.length()==0||!"EVENT".equals(turn.optString("phase"))||number<=status.optInt("appointed_turn")||status.optInt("approval_checked_turn",-1)==number)
                throw new IllegalStateException("A aceitação só pode ser tentada uma vez, ao fim de um turno posterior à nomeação.");
            JSONObject check=recordCheck("new_leadership_approval","Aceitação da Liderança",role,sk,natural,0,"Kingmaker Adventure Path p.520", "",false);
            String degree=check.optString("degree");
            status.put("approval_checked_turn",number);
            if("SUCCESS".equals(degree)||"CRITICAL_SUCCESS".equals(degree))status.put("approval_pending",false).put("leader_activity_modifier",0);
            else if("CRITICAL_FAILURE".equals(degree)){
                status.put("approval_pending",false).put("vacant",true).put("leader_activity_modifier",0);
                assignLeaderUnchecked(role,"",0,"",false,"Rejeitado pela população");
                adjustState(new JSONObject().put("unrest",1),"Nova Liderança: aceitação rejeitada");
            }
            status.put("last_approval_degree",degree);
            upsertFeature("leadership:"+r,"leadership_assignment","Nova Liderança • "+role,Math.max(1,state().optInt("level",1)),status.toString(),"Kingmaker Adventure Path p.520","Aceitação popular");
            receipt[0]=new JSONObject().put("check",check).put("status",status);
        }});return receipt[0];
    }

"""
    s=s[:a]+middle+s[b:]
    # 4. Dispatch callers in resolveKingdomActivity
    a='return resolveNewLeadership(p.optString("role"), p.optString("entity_kind"), p.optLong("entity_id", 0), p.optString("name"), p.optBoolean("invested", false), notes);'
    b='if(!p.has("natural_roll"))throw new IllegalArgumentException("Informe o d20 real da atividade Nova Liderança.");if(p.has("jaethal_d3")||p.optBoolean("emergency",false))return resolveNewLeadership(p.optString("role"), p.optString("entity_kind"), p.optLong("entity_id", 0), p.optString("name"), p.optBoolean("invested", false), nat, skill, p.optInt("jaethal_d3", 0), p.optBoolean("emergency", false), notes);return resolveNewLeadership(p.optString("role"), p.optString("entity_kind"), p.optLong("entity_id", 0), p.optString("name"), p.optBoolean("invested", false), nat, skill, notes);'
    s=once(s,a,b)
    s=once(s,'} else if ("proclamation".equals(k)) {','} else if ("new_leadership_approval".equals(k)) {\n            if(!p.has("natural_roll"))throw new IllegalArgumentException("Informe o d20 real da aceitação popular.");return resolveNewLeadershipApproval(p.optString("role"), skill, nat);\n        } else if ("proclamation".equals(k)) {')

    # 5. Allow out-of-turn emergency New Leadership in recordCheckAtSettlement and inject circumstance shift.
    a=s.index('    public JSONObject recordCheck(String activityKey,String title,String role,String skillKey,int natural,int dc,String sourceRef,String notes,boolean awardReputationOnCrit)')
    b=s.index('    public List<JSONObject> turnActivities(',a)
    part=s[a:b]
    part=once(part,
        'requireEnabled();JSONObject turn=activeTurn();if(turn.length()==0)throw new IllegalStateException("Inicie um turno de Reino antes de registrar uma atividade.");',
        'requireEnabled();JSONObject turn=activeTurn();if(turn.length()==0&&!"new_leadership".equals(key(activityKey)))throw new IllegalStateException("Inicie um turno de Reino antes de registrar uma atividade.");if(turn.length()==0)turn=new JSONObject().put("id",0L).put("phase","OUT_OF_TURN").put("turn_number",state().optInt("turn_number",1));'
    )
    needle='int modifier=rules.skillModifier('
    if part.count(needle)!=1:raise ValueError("Expected exactly one kingdom modifier")
    calc='''int newLeadershipShift="new_leadership".equals(activity)?KingmakerLeadershipSemantics.skillCircumstance(key(role),key(skillKey)):0;
        int incumbentShift=0;
        if(!"new_leadership".equals(activity)&&!"new_leadership_approval".equals(activity)){
            for(JSONObject office:leaders())if(office.optInt("vacant")!=1&&(role.equalsIgnoreCase(office.optString("role_name"))||key(role).equals(office.optString("role_key")))){
                JSONObject saved=db.queryOne("SELECT * FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),"leadership:"+office.optString("role_key")});
                incumbentShift=KingmakerLeadershipSemantics.leaderActivityModifier(jsonObject(saved.optString("effects_json","{}")),s.optInt("turn_number"),activeTurn().optString("phase"));break;
            }
        }
        '''
    part=once(part,needle,calc+needle)
    import re
    pat=r'(int modifier=rules\.skillModifier\([^;]+)'
    matches=list(re.finditer(pat,part))
    if len(matches)!=1:raise ValueError("Kingdom modifier expression not unique")
    expression=matches[0].group(1)
    if 'vacancy-ruin+circumstanceBonus-claimPenalty' not in expression:raise ValueError("R36 modifier contract drift")
    part=once(part,expression,expression.replace('vacancy-ruin+circumstanceBonus-claimPenalty','vacancy-ruin+circumstanceBonus-claimPenalty+newLeadershipShift+incumbentShift'))
    s=s[:a]+part+s[b:]

    # 6. Wire Menagerie sapient creature Unrest at the start of a Kingdom turn (Kingmaker p. 548)
    turn_anchor='int nextUnrest=before.optInt("unrest")+vacancyUnrest+overcrowded;'
    turn_replacement='int menagerieUnrest=KingmakerStructureSemantics.menagerieSapientTurnUnrest(new JSONArray(structures("")));resource.put("menagerie_sapient_unrest",menagerieUnrest);turn.put("resource_roll_json",resource.toString());int nextUnrest=before.optInt("unrest")+vacancyUnrest+overcrowded+menagerieUnrest;'
    s=once(s,turn_anchor,turn_replacement)

    # 7. Add runtime methods for Paved Streets lot travel, Museum exhibit donation/removal, Menagerie creature display, and Character Saves/Skill Checks with Helix & Structure bonuses.
    pc_anchor="""    public int pcStructureItemBonus(String settlementKey,String activity,String skill,String focus){
        if(settlement(settlementKey).length()==0)throw new IllegalArgumentException("Assentamento inexistente.");
        return KingmakerStructureSemantics.pcItemBonus(new JSONArray(structures(settlementKey)),activity,skill,focus);
    }"""
    pc_addition=pc_anchor+"""
    public JSONObject moveInSettlementGrid(final String settlementKey,final int urbanGrid,final int adjacentLotSteps)throws Exception{
        requireEnabled();
        final JSONObject town=settlement(settlementKey);
        if(town.length()==0)throw new IllegalArgumentException("Assentamento inexistente.");
        final JSONObject[] out={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONArray rows=new JSONArray(structures(settlementKey));
            int minutes=KingmakerStructureSemantics.lotTravelMinutes(rows,urbanGrid,adjacentLotSteps);
            boolean paved=KingmakerStructureSemantics.hasPavedStreetsInGrid(rows,urbanGrid);
            JSONObject clock=db.getSceneState();
            double start=clock.optDouble("clock_hour",SceneTemporalResolver.hourForLabel(clock.optString("time_of_day","Manhã")));
            double total=start+(minutes/60.0d),exact=total%24.0d;
            clock.put("clock_hour",exact).put("day_number",Math.max(1,clock.optInt("day_number",1))+(int)Math.floor(total/24.0d)).put("time_of_day",SceneTemporalResolver.labelForHour(exact));
            db.saveSceneStateChecked(clock);
            out[0]=new JSONObject().put("settlement_key",clean(settlementKey)).put("urban_grid",urbanGrid).put("steps",adjacentLotSteps).put("paved_streets",paved).put("travel_minutes",minutes).put("clock_hour",exact).put("day_number",clock.optInt("day_number",1)).put("source_ref","Kingmaker p. 550");
            audit("Deslocamento urbano em assentamento",town.optString("name")+" • "+minutes+" min",new JSONObject(),out[0]);
        }});
        return out[0];
    }
    public JSONObject manageMuseumExhibit(final String settlementKey,final String action,final String itemKey,final String itemName,final int itemLevel,final boolean historicalOrRegionalValue,final String notes)throws Exception{
        requireEnabled();
        final JSONObject town=settlement(settlementKey);
        if(town.length()==0)throw new IllegalArgumentException("Assentamento inexistente.");
        final JSONObject[] out={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject museum=db.queryOne("SELECT * FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key='museum'",new String[]{profile(),clean(settlementKey)});
            if(museum.length()==0||!"built".equals(museum.optString("status","built"))||museum.optInt("count",0)<=0)
                throw new IllegalStateException("O assentamento não possui um Museu construído.");
            JSONObject effects=jsonObject(museum.optString("effects_json","{}"));
            JSONObject outcome=KingmakerStructureSemantics.museumExhibitOutcome(effects.optJSONArray("museum_exhibits"),action,itemKey,itemName,itemLevel,historicalOrRegionalValue);
            effects.put("museum_exhibits",outcome.optJSONArray("exhibits"));
            ContentValues v=new ContentValues();v.put("effects_json",effects.toString());v.put("updated_at",System.currentTimeMillis());
            db.getWritableDatabase().update("kingmaker_structures",v,"id=? AND world_profile=?",new String[]{String.valueOf(museum.optLong("id")),profile()});
            int unrestDelta=outcome.optInt("unrest_delta",0);
            if(unrestDelta!=0)adjustState(new JSONObject().put("unrest",unrestDelta),"Museu ("+action+"): "+clean(itemName));
            out[0]=outcome;
            audit("Acervo do Museu atualizado",clean(itemName)+" • "+action,museum,outcome);
        }});
        return out[0];
    }
    public JSONObject addCreatureToMenagerie(final String settlementKey,final String creatureKey,final String creatureName,final int creatureLevel,final int intelligenceMod,final String benefitChoice,final String ruinChoice,final String notes)throws Exception{
        requireEnabled();
        final JSONObject town=settlement(settlementKey);
        if(town.length()==0)throw new IllegalArgumentException("Assentamento inexistente.");
        final JSONObject[] out={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject menagerie=db.queryOne("SELECT * FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key='menagerie'",new String[]{profile(),clean(settlementKey)});
            if(menagerie.length()==0||!"built".equals(menagerie.optString("status","built"))||menagerie.optInt("count",0)<=0)
                throw new IllegalStateException("O assentamento não possui um Menagerie construído.");
            JSONObject s=state();
            JSONObject effects=jsonObject(menagerie.optString("effects_json","{}"));
            JSONObject outcome=KingmakerStructureSemantics.menagerieAdditionOutcome(effects.optJSONArray("menagerie_creatures"),s.optInt("level",1),creatureKey,creatureName,creatureLevel,intelligenceMod,benefitChoice,ruinChoice,s.optString("reputation_mode","fame"));
            effects.put("menagerie_creatures",outcome.optJSONArray("creatures"));
            ContentValues v=new ContentValues();v.put("effects_json",effects.toString());v.put("updated_at",System.currentTimeMillis());
            db.getWritableDatabase().update("kingmaker_structures",v,"id=? AND world_profile=?",new String[]{String.valueOf(menagerie.optLong("id")),profile()});
            JSONObject delta=new JSONObject();
            int cap=s.optInt("level",1)>=20?4:3;
            if(outcome.optInt("fame_delta")>0)delta.put("fame",Math.min(cap,s.optInt("fame")+outcome.optInt("fame_delta"))-s.optInt("fame"));
            if(outcome.optInt("infamy_delta")>0)delta.put("infamy",Math.min(cap,s.optInt("infamy")+outcome.optInt("infamy_delta"))-s.optInt("infamy"));
            if(outcome.optInt("ruin_delta")!=0&&!outcome.optString("ruin_key").isEmpty())delta.put(outcome.optString("ruin_key"),outcome.optInt("ruin_delta"));
            if(delta.length()>0)adjustState(delta,"Menagerie: "+clean(creatureName));
            out[0]=outcome;
            audit("Criatura adicionada ao Menagerie",clean(creatureName)+" (Nv "+creatureLevel+")",menagerie,outcome);
        }});
        return out[0];
    }
    public JSONObject resolveCharacterSavingThrow(long characterId,String save,int naturalRoll,int dc,String effectTrait,boolean inHouseAtEdgeOfTime)throws Exception{
        if(naturalRoll<1||naturalRoll>20)throw new IllegalArgumentException("d20 fora do intervalo 1–20.");
        JSONObject hero=db.getCharacter(characterId);
        if(hero.length()==0)throw new IllegalArgumentException("Personagem inexistente.");
        JSONObject copy=new JSONObject(hero.toString());
        JSONObject sheet=jsonObject(copy.optString("sheet_json","{}"));
        JSONObject helix=helixState();
        sheet.put("helix_ritual",helix).put("in_house_at_edge_of_time",inHouseAtEdgeOfTime).put("save_effect_trait",clean(effectTrait));
        copy.put("sheet_json",sheet.toString());
        Pathfinder2eRulesEngine core=new Pathfinder2eRulesEngine();
        int mod=core.savingThrowBonus(copy,save);
        int helixBonus=KingmakerCampaignGate.helixSaveStatusBonus(helix,characterId,inHouseAtEdgeOfTime,clean(effectTrait));
        return core.resolveCheck(naturalRoll,mod,dc).put("helix_status_bonus",helixBonus).put("in_house_at_edge_of_time",inHouseAtEdgeOfTime).put("effect_trait",clean(effectTrait));
    }
    public JSONObject resolveCharacterSkillCheck(long characterId,String settlementKey,String activity,String skill,String focus,int naturalRoll,int dc)throws Exception{
        if(naturalRoll<1||naturalRoll>20)throw new IllegalArgumentException("d20 fora do intervalo 1–20.");
        JSONObject hero=db.getCharacter(characterId);
        if(hero.length()==0)throw new IllegalArgumentException("Personagem inexistente.");
        JSONObject copy=new JSONObject(hero.toString());
        JSONObject sheet=jsonObject(copy.optString("sheet_json","{}"));
        int structureBonus=0;
        if(clean(settlementKey).length()>0){
            if(settlement(settlementKey).length()==0)throw new IllegalArgumentException("Assentamento inexistente.");
            JSONArray structs=new JSONArray(structures(settlementKey));
            sheet.put("settlement_structures",structs).put("settlement_activity",clean(activity)).put("settlement_focus",clean(focus));
            structureBonus=KingmakerStructureSemantics.pcItemBonus(structs,clean(activity),clean(skill).toLowerCase(Locale.ROOT),clean(focus));
        }
        copy.put("sheet_json",sheet.toString());
        Pathfinder2eRulesEngine core=new Pathfinder2eRulesEngine();
        int mod=core.checkBonus(copy,"",skill);
        return core.resolveCheck(naturalRoll,mod,dc).put("settlement_structure_item_bonus",structureBonus).put("settlement_key",clean(settlementKey)).put("activity",clean(activity));
    }"""
    s=once(s,pc_anchor,pc_addition)
    return s

def pf2_core(s):
    old_check="""    public int checkBonus(JSONObject hero,String ability,String skill){
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
    }"""
    new_check="""    public int checkBonus(JSONObject hero,String ability,String skill){
        if(hero==null)return 0;
        JSONObject sheet=sheet(hero);
        String skillKey=skillKey(skill);
        int structureItem=0;
        JSONArray structures=sheet.optJSONArray("settlement_structures");
        if(structures!=null){
            structureItem=KingmakerStructureSemantics.pcItemBonus(structures,sheet.optString("settlement_activity",""),skillKey,sheet.optString("settlement_focus",""));
        }
        String explicit=skillKey.length()==0?"":skillKey+"_bonus";
        if(explicit.length()>0&&sheet.has(explicit)){
            int existingItem=Math.max(0,sheet.optInt(skillKey+"_item_bonus",0));
            return sheet.optInt(explicit,0)+Math.max(0,structureItem-existingItem);
        }
        if("perception".equals(skillKey)&&sheet.has("perception_bonus"))return sheet.optInt("perception_bonus",0);
        int level=Math.max(1,Math.min(20,sheet.optInt("level",hero.optInt("level",1))));
        String resolvedAbility=(ability==null||ability.trim().isEmpty())?abilityForSkill(skillKey):ability;
        int mod=attributeModifier(hero,resolvedAbility);
        String rank=rankFor(sheet,skillKey);
        int itemBonus=Math.max(Math.max(0,sheet.optInt(skillKey+"_item_bonus",0)),structureItem);
        return mod+proficiency(level,rank)+itemBonus;
    }"""
    s=once(s,old_check,new_check)
    old_save="""    public int savingThrowBonus(JSONObject hero,String save){
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
    }"""
    new_save="""    public int savingThrowBonus(JSONObject hero,String save){
        if(hero==null)return 0;
        JSONObject sheet=sheet(hero);
        String n=norm(save);
        String key,ability;
        if(n.contains("reflex")){key="reflex";ability="Destreza";}
        else if(n.contains("vont")||n.contains("will")){key="will";ability="Sabedoria";}
        else {key="fortitude";ability="Constituição";}
        int helixStatus=0;
        JSONObject helix=object(sheet.opt("helix_ritual"));
        if(helix!=null){
            long heroId=hero.optLong("id",sheet.optLong("id",0));
            helixStatus=KingmakerCampaignGate.helixSaveStatusBonus(helix,heroId,sheet.optBoolean("in_house_at_edge_of_time",false),sheet.optString("save_effect_trait",""));
        }
        int existingStatus=Math.max(0,sheet.optInt(key+"_status_bonus",0));
        int statusDelta=Math.max(0,helixStatus-existingStatus);
        if(sheet.has(key+"_bonus"))return sheet.optInt(key+"_bonus",0)+statusDelta;
        int level=Math.max(1,Math.min(20,sheet.optInt("level",hero.optInt("level",1))));
        String rank=sheet.optString(key+"_rank",sheet.optString(key+"_proficiency","untrained"));
        return attributeModifier(hero,ability)+proficiency(level,rank)+Math.max(existingStatus,helixStatus);
    }"""
    s=once(s,old_save,new_save)
    return s

def ui(s):
    start=s.index("    private void showKingmakerLeaderEditDialog(")
    end=s.index("\n    private ",start+20)
    part=s[start:end]
    anchor='final EditText notes=labeledField(form,"Notas",role.optString("notes"),true);'
    add='''final String[] leadershipSkills={"Intriga","Política","Estadismo","Guerra"},leadershipKeys={"intrigue","politics","statecraft","warfare"};
    final Spinner skill=spinner(leadershipSkills,"Política");form.addView(cardWithLabel("PERÍCIA • NOVA LIDERANÇA",skill));
    final EditText natural=labeledField(form,"d20 real (1–20) • após a fundação","",false);natural.setInputType(InputType.TYPE_CLASS_NUMBER);
    form.addView(text("Na fundação, a nomeação é gratuita. Após o primeiro turno, o teste oficial é obrigatório; Jaethal pode causar Agitação.",12,MUTED,false));
    boolean awaitingApproval=false;for(JSONObject history:kingmakerRepository.features("leadership_assignment"))if(("leadership:"+role.optString("role_key")).equals(history.optString("feature_key"))){try{awaitingApproval=new JSONObject(history.optString("effects_json","{}")).optBoolean("approval_pending");}catch(Exception ignored){}}
    if(awaitingApproval){final Button approval=new Button(this);approval.setText("ACEITAÇÃO POPULAR • FASE EVENTOS");
        approval.setOnClickListener(new View.OnClickListener(){public void onClick(View v){try{
            JSONObject args=new JSONObject().put("role",role.optString("role_key")).put("skill_key",leadershipKeys[skill.getSelectedItemPosition()]).put("natural_roll",num(natural,0));
            JSONObject receipt=kingmakerRepository.resolveKingdomActivity("new_leadership_approval",args);
            toast("Aceitação: "+receipt.optJSONObject("check").optString("degree_pt"));showKingmaker();
        }catch(Exception ex){toast("Aceitação popular: "+ex.getMessage());}}});form.addView(approval);}'''
    part=once(part,anchor,anchor+add)
    original='kingmakerRepository.assignLeader(role.optString("role_key"),c.optString("kind"),c.optLong("id"),c.optString("name"),invested.isChecked(),value(notes));'
    replacement='''if(kingmakerRepository.state().optInt("turn_number",0)==0)kingmakerRepository.assignLeader(role.optString("role_key"),c.optString("kind"),c.optLong("id"),c.optString("name"),invested.isChecked(),value(notes));
    else {JSONObject args=new JSONObject().put("role",role.optString("role_key")).put("entity_kind",c.optString("kind")).put("entity_id",c.optLong("id")).put("name",c.optString("name")).put("invested",invested.isChecked()).put("natural_roll",num(natural,0)).put("skill_key",leadershipKeys[skill.getSelectedItemPosition()]).put("notes",value(notes));
          kingmakerRepository.resolveKingdomActivity("new_leadership",args);}'''
    if original in part:
        part=once(part,original,replacement)
    else:
        import re
        calls=[m for m in re.finditer(r'kingmakerRepository\.(?:assignLeader|resolveNewLeadership)\([^;]*?\);',part) if 'role.optString("role_key")' in m.group(0)]
        if len(calls)!=1:raise ValueError("NEW_LEADERSHIP_UI_CALL_DRIFT "+str(len(calls))+" "+part[-3000:])
        call=calls[0].group(0)
        part=once(part,call,replacement)
    return s[:start]+part+s[end:]
