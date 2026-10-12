"""Bank/Mint effects reach actual checks and a persistent, atomic treasury ledger."""
def once(s,a,b):
 if s.count(a)!=1:raise ValueError('Treasury anchor count='+str(s.count(a))+' '+a[:70])
 return s.replace(a,b,1)
def repository_treasury(s):
 start=s.index('    public JSONObject resolveTapTreasury(');end=s.index('    public JSONObject resolveTradeCommodities(',start)
 s=s[:start]+'''    public JSONObject resolveTapTreasury(int ignoredRpAmount,String notes)throws Exception{
        throw new IllegalArgumentException("Recorrer ao Tesouro exige d20, local e escolha de Ruína; use os parâmetros oficiais da atividade.");
    }
    public JSONObject resolveTapTreasury(final JSONObject params)throws Exception{
        if(params.has("purpose")&&params.optString("purpose").length()>0&&!"personal".equals(params.optString("purpose")))throw new IllegalArgumentException("O financiamento de eventos precisa da consequência específica do evento; esta operação retira fundos para uso pessoal.");
        requireTreasuryPhase("COMMERCE");final String ruin=key(params.optString("ruin_choice"));
        if(!java.util.Arrays.asList("corruption","crime","decay","strife").contains(ruin))throw new IllegalArgumentException("Escolha a Ruína para uma possível falha crítica.");
        final int natural=params.optInt("natural_roll");if(natural<1||natural>20)throw new IllegalArgumentException("Informe o d20 natural.");
        final JSONObject[] result={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            requireTreasuryPhase("COMMERCE");JSONObject before=state(),ledger=treasuryLedger();String track="infamy".equals(before.optString("reputation_mode"))?"infamy":"fame";int priorReputation=before.optInt(track);
            JSONObject check=recordCheckAtSettlement(params.optString("settlement_key"),"tap_treasury","Recorrer ao Tesouro","Tesoureiro","statecraft",natural,0,"Kingmaker p.528",params.optString("notes"),false);
            JSONObject outcome=KingmakerStructureSemantics.tapTreasuryOutcome(before.optInt("level",1),check.optString("degree"),ledger.optInt("owed_gp")>0,ruin);
            finalizeTreasuryDegree(check,outcome.optString("degree"),track,priorReputation);
            int gold=outcome.optInt("gold_gained"),expires=before.optInt("turn_number")+1;
            if(gold>0){adjustCompanyGold(gold);ledger.put("owed_gp",ledger.optInt("owed_gp")+gold);}
            if(outcome.optInt("economy_circumstance_penalty")<0)ledger.put("economy_penalty_until_turn",Math.max(expires,ledger.optInt("economy_penalty_until_turn")));
            if(outcome.optInt("loyalty_circumstance_penalty")<0)ledger.put("loyalty_penalty_until_turn",Math.max(expires,ledger.optInt("loyalty_penalty_until_turn")));
            JSONObject changes=new JSONObject().put("unrest",outcome.optInt("unrest"));if(outcome.has(ruin))changes.put(ruin,outcome.optInt(ruin));adjustState(changes,"Recorrer ao Tesouro: consequência oficial");
            upsertFeature("treasury:ledger","treasury","Dívida e penalidades do Tesouro",before.optInt("level",1),ledger.toString(),"Kingmaker pp.528-529",params.optString("notes"));
            result[0]=outcome.put("check",check).put("ledger",ledger).put("outcome_pt",treasuryDegreeLabel(outcome.optString("degree"))+": "+gold+" PO retiradas; dívida total "+ledger.optInt("owed_gp")+" PO.");
            audit("Tesouro do Reino",result[0].optString("outcome_pt"),before,result[0]);
        }});return result[0];
    }

'''+s[end:]
 start=s.index('    public JSONObject resolveCapitalInvestment(');end=s.index('    public JSONObject resolveKingdomActivity(',start)
 s=s[:start]+'''    public JSONObject resolveCapitalInvestment(int goldAmount,String notes)throws Exception{
        throw new IllegalArgumentException("Investimento exige local com banco e escolhas de resultado; use os parâmetros oficiais da atividade.");
    }
    public JSONObject resolveCapitalInvestment(final JSONObject params)throws Exception{
        if(params.has("purpose")&&!java.util.Arrays.asList("","generate_rp","repay").contains(params.optString("purpose")))throw new IllegalArgumentException("Finalidade de investimento inválida.");
        requireTreasuryPhase("ACTIVITY");final int gold=params.optInt("gold_amount");
        if(gold<=0)throw new IllegalArgumentException("Informe fundos pessoais positivos.");
        final boolean repay="repay".equals(params.optString("purpose"));final String choice=params.optString("critical_failure_choice");
        if(!repay&&!"resource_die".equals(choice)&&!"none".equals(choice))throw new IllegalArgumentException("Escolha a consequência de uma possível falha crítica.");
        final int natural=params.optInt("natural_roll");if(!repay&&(natural<1||natural>20))throw new IllegalArgumentException("Informe o d20 natural.");
        final String townKey=params.optString("settlement_key");final String targetHex=treasuryLocationHex(params);
        if(!bankInfluencesHex(targetHex))throw new IllegalStateException("O investimento exige estar na influência de um assentamento com banco.");
        final JSONObject[] result={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            requireTreasuryPhase("ACTIVITY");if(!bankInfluencesHex(targetHex))throw new IllegalStateException("O banco não influencia mais o local.");
            JSONObject before=state(),ledger=treasuryLedger();int owed=ledger.optInt("owed_gp");
            if(repay&&(owed<=0||gold>owed))throw new IllegalArgumentException("A restituição precisa estar entre 1 e "+owed+" PO.");
            adjustCompanyGold(-gold);
            if(repay){
                ledger.put("owed_gp",owed-gold);upsertFeature("treasury:ledger","treasury","Dívida e penalidades do Tesouro",before.optInt("level",1),ledger.toString(),"Kingmaker pp.528-529",params.optString("notes"));
                JSONObject record=automaticTreasuryActivity("capital_investment","Restituir fundos ao Tesouro","NO_CHECK",params.optString("notes"));
                result[0]=new JSONObject().put("check",record).put("gold_spent",gold).put("owed_gp",owed-gold).put("rp_gained",0).put("outcome_pt","Restituídas "+gold+" PO. Dívida restante: "+(owed-gold)+" PO.");return;
            }
            int level=before.optInt("level",1),required=KingmakerStructureSemantics.investmentGp(level);
            JSONObject check=gold<required?automaticTreasuryActivity("capital_investment","Investimento insuficiente","CRITICAL_FAILURE",params.optString("notes")):
                recordCheckAtSettlement(townKey,"capital_investment","Investimento de Capital","Tesoureiro","trade",natural,0,"Kingmaker p.529",params.optString("notes"),false,new JSONObject().put("hex_id",targetHex));
            int count=KingmakerStructureSemantics.investmentDice(check.optString("degree"),gold,level,choice),die=rules.resourceDieSize(before.optInt("size",1));
            JSONArray rolls=params.optJSONArray("resource_rolls");if(rolls==null){rolls=new JSONArray();for(int i=0;i<count;i++)rolls.put(new java.util.Random().nextInt(die)+1);}
            int failure=params.optInt("failure_d4");if("FAILURE".equals(check.optString("degree"))&&failure==0)failure=new java.util.Random().nextInt(4)+1;
            JSONObject outcome=KingmakerStructureSemantics.investmentOutcome(level,gold,check.optString("degree"),die,rolls,failure,choice);
            int rp=outcome.optInt("rp_gained");
            if(params.optBoolean("next_turn")&&rp>0)upsertFeature("treasury:income:"+check.optLong("activity_id"),"treasury_income","Investimento para o próximo turno",level,new JSONObject().put("due_turn",before.optInt("turn_number")+1).put("rp",rp).toString(),"Kingmaker p.529",params.optString("notes"));
            else if(rp>0)adjustState(new JSONObject().put("resource_points",rp),"Investimento de Capital");
            if(outcome.optInt("crime")>0)adjustState(new JSONObject().put("crime",outcome.optInt("crime")),"Investimento desviado: Crime");
            result[0]=outcome.put("check",check).put("next_turn",params.optBoolean("next_turn")).put("outcome_pt",treasuryDegreeLabel(outcome.optString("degree"))+": "+gold+" PO investidas; "+rp+" RP "+(params.optBoolean("next_turn")?"para o próximo turno.":"para este turno."));
            audit("Investimento de Capital",result[0].optString("outcome_pt"),before,result[0]);
        }});return result[0];
    }

'''+s[end:]
 s=once(s,'return resolveTapTreasury(p.optInt("rp_amount", 2), notes);','return resolveTapTreasury(p);')
 s=once(s,'return resolveCapitalInvestment(p.optInt("gold_amount", 50), notes);','return resolveCapitalInvestment(p);')
 s=once(s,'int circumstanceBonus=Math.max(taxes,expansionExpert);','int circumstanceBonus=Math.max(taxes,expansionExpert);\n        int treasuryPenalty=treasuryCircumstancePenalty(ability,s.optInt("turn_number"));')
 s=once(s,'vacancy-ruin+circumstanceBonus-claimPenalty,s.optInt("unrest")','vacancy-ruin+circumstanceBonus-claimPenalty+treasuryPenalty,s.optInt("unrest")')
 s=once(s,'.put("claim_hex_stability_penalty",-claimPenalty)', '.put("claim_hex_stability_penalty",-claimPenalty).put("treasury_circumstance_penalty",treasuryPenalty)')
 start=s.index('    public JSONObject startTurn(JSONArray resourceRolls,int rulerVacancyUnrest)');end=s.index('\n    public ',start+20)
 part=once(s[start:end],'result[0]=activeTurn();','applyDeferredTreasuryIncome(number);result[0]=activeTurn();');s=s[:start]+part+s[end:]
 helper='''    private JSONObject strictTreasuryJson(String data){try{return new JSONObject(data);}catch(Exception error){throw new IllegalStateException("Dados do Tesouro inválidos; operação cancelada.",error);}}
    private JSONObject treasuryLedger(){JSONObject ledger=strictTreasuryJson(db.queryOne("SELECT * FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),"treasury:ledger"}).optString("effects_json","{}"));if(ledger.optInt("owed_gp")<0)throw new IllegalStateException("Dívida do Tesouro inválida.");return ledger;}
    private int treasuryCircumstancePenalty(String ability,int turn){if(!"economy".equals(ability)&&!"loyalty".equals(ability))return 0;int until=treasuryLedger().optInt(ability+"_penalty_until_turn");return until>0&&turn<=until?-1:0;}
    private void requireTreasuryPhase(String phase){requireEnabled();JSONObject turn=activeTurn();if(turn.length()==0||!"active".equals(turn.optString("status"))||!phase.equals(turn.optString("phase")))throw new IllegalStateException("Esta operação exige a fase de "+("COMMERCE".equals(phase)?"Comércio":"Atividades")+" de um turno ativo.");if(rules.isAnarchy(state().optInt("unrest"))||state().optInt("anarchy")==1)throw new IllegalStateException("Esta operação não é permitida em Anarquia.");}
    private String treasuryLocationHex(JSONObject params){String hex=clean(params.optString("hex_id"));if(hex.length()>0)return hex;JSONObject town=settlement(params.optString("settlement_key"));if(town.length()>0)return town.optString("hex_id");return db.getMapState().optString("current_hex_id");}
    private boolean bankInfluencesHex(String target){if(target.length()==0||hex(target).optInt("claimed")!=1)return false;for(JSONObject town:settlements())if(KingmakerStructureSemantics.count(new JSONArray(structures(town.optString("settlement_key"))),"bank")>0&&hexDistance(target,town.optString("hex_id"))<=town.optInt("influence"))return true;return false;}
    private void adjustCompanyGold(int delta)throws Exception{
        JSONObject party=db.getPartyState(),resources=strictTreasuryJson(party.optString("company_resources_json","{}"));double before=resources.optDouble("gold",0),after=before+delta;
        if(Double.isNaN(before)||Double.isInfinite(before)||before<0||after<0)throw new IllegalStateException("PO insuficientes ou saldo inválido nos recursos da companhia.");
        resources.put("gold",after);party.put("company_resources_json",resources.toString());db.savePartyStateChecked(party);
        double stored=strictTreasuryJson(db.getPartyState().optString("company_resources_json","{}")).optDouble("gold",-1);
        if(Math.abs(stored-after)>.0001)throw new IllegalStateException("Falha ao persistir os fundos da companhia.");
    }
    private static String treasuryDegreeLabel(String degree){return "CRITICAL_SUCCESS".equals(degree)?"Sucesso crítico":"SUCCESS".equals(degree)?"Sucesso":"FAILURE".equals(degree)?"Falha":"CRITICAL_FAILURE".equals(degree)?"Falha crítica":"Sem teste";}
    private JSONObject automaticTreasuryActivity(String activity,String title,String degree,String notes)throws Exception{
        JSONObject turn=activeTurn(),result=new JSONObject().put("degree",degree).put("degree_pt",treasuryDegreeLabel(degree)).put("automatic",true).put("natural_roll",0);
        ContentValues v=new ContentValues();v.put("world_profile",profile());v.put("turn_id",turn.optLong("id"));v.put("phase",turn.optString("phase"));v.put("activity_key",activity);v.put("title",title);v.put("actor_role","Tesoureiro");v.put("skill_key","trade");v.put("degree",degree);v.put("effects_json",result.toString());v.put("source_ref","Kingmaker p.529");v.put("notes",clean(notes));v.put("created_at",System.currentTimeMillis());
        long id=db.getWritableDatabase().insertOrThrow("kingmaker_turn_activities",null,v);return result.put("activity_id",id);
    }
    private void finalizeTreasuryDegree(JSONObject check,String degree,String track,int priorReputation)throws Exception{
        String original=check.optString("degree");check.put("original_degree",original).put("degree",degree).put("degree_pt",treasuryDegreeLabel(degree));
        if("CRITICAL_SUCCESS".equals(original)&&!original.equals(degree)){int awarded=state().optInt(track)-priorReputation;if(awarded>0)adjustState(new JSONObject().put(track,-awarded),"Reputação: resultado do Tesouro piorado pela dívida");}
        ContentValues v=new ContentValues();v.put("degree",degree);v.put("effects_json",check.toString());if(db.getWritableDatabase().update("kingmaker_turn_activities",v,"world_profile=? AND id=?",new String[]{profile(),String.valueOf(check.optLong("activity_id"))})!=1)throw new IllegalStateException("Falha ao registrar o grau final do Tesouro.");
    }
    private void applyDeferredTreasuryIncome(int number)throws Exception{
        int gained=0;for(JSONObject feature:features("treasury_income"))if("active".equals(feature.optString("status"))){JSONObject effect=jsonObject(feature.optString("effects_json","{}"));if(effect.optInt("due_turn")>0&&effect.optInt("due_turn")<=number){gained+=effect.optInt("rp");setFeatureStatus(feature.optString("feature_key"),"completed");}}
        if(gained>0){adjustState(new JSONObject().put("resource_points",gained),"Investimentos recebidos no novo turno");JSONObject turn=activeTurn(),resource=jsonObject(turn.optString("resource_roll_json","{}"));resource.put("deferred_capital_investment_rp",gained);ContentValues v=new ContentValues();v.put("resource_roll_json",resource.toString());db.getWritableDatabase().update("kingmaker_turns",v,"world_profile=? AND id=?",new String[]{profile(),String.valueOf(turn.optLong("id"))});}
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
def rules_treasury(s):
 s=once(s,'a.put("name","Investimento de Capital").put("category","commerce").put("phase","COMMERCE")','a.put("name","Investimento de Capital").put("category","leadership").put("phase","ACTIVITY")')
 s=once(s,'"Converte ouro dos aventureiros diretamente em RP para o Reino."','"Investe fundos pessoais na influência de um banco; um teste de Comércio determina RP em dados de recurso."')
 s=once(s,'requirements="Ouro suficiente; custo por RP depende do nível do Reino.";','requirements="Influência de assentamento com Banco; contribuição por nível conforme a tabela de tesouro. Restituição dispensa teste.";')
 return s
def treasury_ui(s):
 anchor='territory.addView(trade);Button atlas='
 s=once(s,anchor,'''territory.addView(trade);Button treasury=button("TESOURO E INVESTIMENTO DE CAPITAL",PANEL2);treasury.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showOfficialTreasuryDialog();}});territory.addView(treasury);Button atlas=''')
 helper='''    private void showOfficialTreasuryDialog(){
        final List<JSONObject> towns=kingmakerRepository.settlements();if(towns.isEmpty()){toast("Fundar um assentamento antes de usar o Tesouro.");return;}
        final String[] names=new String[towns.size()];for(int i=0;i<names.length;i++)names[i]=towns.get(i).optString("name");
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(12),0,dp(12),dp(16));
        final String[] operations={"Investir fundos pessoais","Restituir empréstimo do Tesouro","Recorrer ao Tesouro para uso pessoal"};final Spinner operation=spinner(operations,operations[0]),town=spinner(names,names[0]);form.addView(cardWithLabel("OPERAÇÃO",operation));form.addView(cardWithLabel("LOCAL DA OPERAÇÃO",town));
        int level=kingmakerRepository.state().optInt("level",1),required=KingmakerStructureSemantics.investmentGp(level);
        form.addView(text("Investimento exige influência de um banco. Contribuição por nível: "+required+" PO. Valor menor causa falha crítica automática; valor excedente é gasto. As PO são debitadas dos recursos da companhia. Restituir não exige teste.",13,TEXT,false));
        final EditText gold=labeledField(form,"PO a investir ou restituir",String.valueOf(required),false),natural=labeledField(form,"d20 natural",String.valueOf(new Random().nextInt(20)+1),false),notes=labeledField(form,"Notas","",true);
        final Spinner timing=spinner(new String[]{"Receber RP neste turno","Receber RP no próximo turno"},"Receber RP neste turno"),critical=spinner(new String[]{"Falha crítica: 0 RP e +1 Crime","Falha crítica: 1 dado de recurso em RP e em Crime"},"Falha crítica: 0 RP e +1 Crime");form.addView(cardWithLabel("DESTINO DO INVESTIMENTO",timing));form.addView(cardWithLabel("CONSEQUÊNCIA SE HOUVER FALHA CRÍTICA",critical));
        final String[] ruins={"corruption","crime","decay","strife"};final Spinner ruin=spinner(new String[]{"Corrupção","Crime","Decadência","Conflito"},"Crime");form.addView(cardWithLabel("RUÍNA EM FALHA CRÍTICA AO RECORRER AO TESOURO",ruin));
        new AlertDialog.Builder(this).setTitle("Tesouro do Reino").setView(scrollableDialogView(form)).setNegativeButton("Cancelar",null).setPositiveButton("RESOLVER",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{
            JSONObject params=new JSONObject().put("settlement_key",towns.get(town.getSelectedItemPosition()).optString("settlement_key")).put("gold_amount",num(gold,0)).put("natural_roll",num(natural,0)).put("notes",value(notes)).put("critical_failure_choice",critical.getSelectedItemPosition()==0?"none":"resource_die").put("next_turn",timing.getSelectedItemPosition()==1).put("ruin_choice",ruins[ruin.getSelectedItemPosition()]);
            if(operation.getSelectedItemPosition()==1)params.put("purpose","repay");
            JSONObject result=operation.getSelectedItemPosition()==2?kingmakerRepository.resolveTapTreasury(params):kingmakerRepository.resolveCapitalInvestment(params);
            new AlertDialog.Builder(MainActivity.this).setTitle("Tesouro resolvido").setMessage(result.optString("outcome_pt")).setPositiveButton("Fechar",null).show();showKingmaker();
        }catch(Exception error){toast(error.getMessage());}}}).show();
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
