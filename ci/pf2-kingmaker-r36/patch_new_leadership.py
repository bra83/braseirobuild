"""Wire official New Leadership checks to the existing Kingdom activity, office editor and state."""
from pathlib import Path

def once(s,a,b):
    n=s.count(a)
    if n!=1: raise ValueError("NEW_LEADERSHIP_ANCHOR "+str(n)+" "+a[:130])
    return s.replace(a,b,1)

def repo(s):
    # Legacy assignment is founding-only. All later UI/API changes require a check.
    old="    public void assignLeader(String role,String kind,long entityId,String name,boolean invested,String notes){"
    replacement="""    public void assignLeader(String role,String kind,long entityId,String name,boolean invested,String notes){
        if(state().optInt("turn_number",0)>0){
            String k=key(role);JSONObject old=db.queryOne("SELECT * FROM kingmaker_leaders WHERE world_profile=? AND role_key=?",new String[]{profile(),k});
            if(!old.optString("leader_name").equals(clean(name))||!old.optString("entity_kind").equals(clean(kind))||old.optLong("entity_id")!=Math.max(0,entityId)||old.optInt("invested")!=(invested?1:0))
                throw new IllegalStateException("Depois da fundação, use a atividade Nova Liderança com teste de Reino.");
        }
        assignLeaderUnchecked(role,kind,entityId,name,invested,notes);
    }
    private void assignLeaderUnchecked(String role,String kind,long entityId,String name,boolean invested,String notes){"""
    s=once(s,old,replacement)
    a=s.index("    public JSONObject resolveNewLeadership(")
    b=s.index("    public JSONObject resolveProclamation(",a)
    middle="""    public JSONObject resolveNewLeadership(String role,String entityKind,long entityId,String name,boolean invested,String notes)throws Exception{
        if(state().optInt("turn_number",0)>0)throw new IllegalStateException("Nova Liderança exige d20 e perícia. Use resolveKingdomActivity.");
        assignLeader(role,entityKind,entityId,name,invested,notes);
        return db.queryOne("SELECT * FROM kingmaker_leaders WHERE world_profile=? AND role_key=?",new String[]{profile(),key(role)});
    }

    public JSONObject resolveNewLeadership(final String role,final String entityKind,final long entityId,final String name,final boolean invested,final int natural,final String skillKey,final String notes)throws Exception{
        requireEnabled();final String r=key(role),sk=key(skillKey);
        if(!rules.validLeadershipRole(role)||clean(name).isEmpty()||natural<1||natural>20)throw new IllegalArgumentException("Cargo, líder e d20 válidos são obrigatórios.");
        KingmakerLeadershipSemantics.skillCircumstance(r,sk);
        if(state().optInt("turn_number",0)==0)throw new IllegalStateException("Durante a fundação, nomeie líderes gratuitamente, sem teste.");
        if(activeTurn().length()==0)throw new IllegalStateException("Abra o turno do Reino para realizar Nova Liderança.");
        final JSONObject[] receipt={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject comp=db.queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND (name=? OR companion_key=?)",new String[]{profile(),clean(name),clean(name)});
            if(comp.length()>0&&!KingmakerCompanionSemantics.canLead(comp,state().optString("reputation_mode","fame")))
                throw new IllegalStateException("O companheiro ainda não aceita a nomeação.");
            boolean preferred=comp.length()>0&&KingmakerCompanionSemantics.automaticInvestment(comp,r);
            JSONObject check=recordCheck("new_leadership","Nova Liderança",role,sk,natural,0,"Kingmaker Adventure Path p.520",notes,false);
            String original=check.optString("degree"),degree=KingmakerLeadershipSemantics.improved(original,preferred);
            int currentTurn=state().optInt("turn_number",1);
            boolean jaethal="jaethal".equals(comp.optString("companion_key")),famous=!"infamy".equals(state().optString("reputation_mode","fame"));
            int die=jaethal&&famous?dice.nextInt(3)+1:0;
            JSONObject outcome=KingmakerLeadershipSemantics.appointment(degree,r,currentTurn,jaethal,famous,die);
            outcome.put("raw_degree",original).put("companion_degree_improved",preferred).put("check",check);
            if("CRITICAL_FAILURE".equals(degree))assignLeaderUnchecked(role,"",0,"",false,notes);
            else{
                for(JSONObject occupied:leaders()){
                    if(occupied.optInt("vacant")==1||r.equals(occupied.optString("role_key")))continue;
                    boolean matchingId=entityId>0&&entityId==occupied.optLong("entity_id")&&entityKind.equals(occupied.optString("entity_kind"));
                    boolean matchingName=entityId<=0&&clean(name).equalsIgnoreCase(occupied.optString("leader_name"));
                    if(matchingId||matchingName)assignLeaderUnchecked(occupied.optString("role_key"),"",0,"",false,"Mudança oficial de cargo");
                }
                assignLeaderUnchecked(role,entityKind,entityId,name,invested,notes);
            }
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
    # All current dispatch callers must supply the real d20/skill rather than falling back to a fictitious roll.
    a='return resolveNewLeadership(p.optString("role"), p.optString("entity_kind"), p.optLong("entity_id", 0), p.optString("name"), p.optBoolean("invested", false), notes);'
    b='if(!p.has("natural_roll"))throw new IllegalArgumentException("Informe o d20 real da atividade Nova Liderança.");return resolveNewLeadership(p.optString("role"), p.optString("entity_kind"), p.optLong("entity_id", 0), p.optString("name"), p.optBoolean("invested", false), nat, skill, notes);'
    s=once(s,a,b)
    s=once(s,'} else if ("proclamation".equals(k)) {','} else if ("new_leadership_approval".equals(k)) {\n            if(!p.has("natural_roll"))throw new IllegalArgumentException("Informe o d20 real da aceitação popular.");return resolveNewLeadershipApproval(p.optString("role"), skill, nat);\n        } else if ("proclamation".equals(k)) {')
    # Inject the real circumstance adjustment into the existing source-level kingdom skill resolver.
    a=s.index('    public JSONObject recordCheck(String activityKey,String title,String role,String skillKey,int natural,int dc,String sourceRef,String notes,boolean awardReputationOnCrit)')
    b=s.index('    public List<JSONObject> turnActivities(',a)
    part=s[a:b]
    needle='int modifier=rules.skillModifier('
    if part.count(needle)!=1:raise ValueError("Expected exactly one kingdom modifier")
    calc='''int newLeadershipShift="new_leadership".equals(activity)?KingmakerLeadershipSemantics.skillCircumstance(key(role),key(skillKey)):0;
        int incumbentShift=0;
        if(!"new_leadership".equals(activity)&&!"new_leadership_approval".equals(activity)){
            for(JSONObject office:leaders())if(office.optInt("vacant")!=1&&(role.equalsIgnoreCase(office.optString("role_name"))||key(role).equals(office.optString("role_key")))){
                JSONObject saved=db.queryOne("SELECT * FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),"leadership:"+office.optString("role_key")});
                incumbentShift=KingmakerLeadershipSemantics.leaderActivityModifier(jsonObject(saved.optString("effects_json","{}")),s.optInt("turn_number"));break;
            }
        }
        '''
    part=once(part,needle,calc+needle)
    # R36 base patched formula has already applied taxes / claim hex modifiers.
    import re
    pat=r'(int modifier=rules\.skillModifier\([^;]+)'
    matches=list(re.finditer(pat,part))
    if len(matches)!=1:raise ValueError("Kingdom modifier expression not unique")
    expression=matches[0].group(1)
    if 'vacancy-ruin+circumstanceBonus-claimPenalty' not in expression:raise ValueError("R36 modifier contract drift")
    part=once(part,expression,expression.replace('vacancy-ruin+circumstanceBonus-claimPenalty','vacancy-ruin+circumstanceBonus-claimPenalty+newLeadershipShift+incumbentShift'))
    s=s[:a]+part+s[b:]
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
