"""Persist each trade focus and carry action context through synchronous resolution."""
def once(s,a,b):
 if s.count(a)!=1:raise ValueError('Focus anchor count='+str(s.count(a))+' '+a[:70])
 return s.replace(a,b,1)
def repository_focus(s):
 anchor='public final class KingmakerRepository {'
 s=once(s,anchor,anchor+'\n    private final ThreadLocal<JSONObject> structureActionContext=new ThreadLocal<JSONObject>();')
 signature='    public JSONObject resolveKingdomActivity(String activityKey, JSONObject params) throws Exception {'
 s=once(s,signature,signature+'''
        JSONObject previous=structureActionContext.get();
        try{structureActionContext.set(params==null?new JSONObject():new JSONObject(params.toString()));return resolveKingdomActivityWithContext(activityKey,params);}
        finally{if(previous==null)structureActionContext.remove();else structureActionContext.set(previous);}
    }
    private JSONObject resolveKingdomActivityWithContext(String activityKey,JSONObject params)throws Exception{''')
 s=once(s,'JSONObject p = params == null ? new JSONObject() : params;','JSONObject p = params == null ? new JSONObject() : new JSONObject(params.toString());')
 s=once(s,'int nat = p.optInt("natural_roll", 10);','int nat = p.has("natural_roll")?p.optInt("natural_roll",0):new java.util.Random().nextInt(20)+1;\n        p.put("natural_roll",nat);')
 s=once(s,'return resolveBuildStructure(p.optString("settlement_key"), p.optString("structure_key"), nat, skill, notes);',
 '''return resolveBuildStructure(p.optString("settlement_key"),p.optString("structure_key"),nat,skill,notes,p.optString("commodity_choice"),new JSONObject(p.toString()).put("ruin",p.optString("ruin",p.optString("ruin_choice"))));''')
 anchor='        int die1=options.optInt("die1",0),die2=options.optInt("die2",0);'
 s=once(s,anchor,anchor+'''
        boolean needsBuildDice=turnFirst&&("tavern_luxury".equals(k)||(("castle".equals(k)||"tavern_world_class".equals(k))&&count==0));
        if(needsBuildDice&&die1==0)die1=new java.util.Random().nextInt(4)+1;
        if(needsBuildDice&&"tavern_world_class".equals(k)&&die2==0)die2=new java.util.Random().nextInt(4)+1;''')
 anchor='int structureItem=officialStructureActivityBonus(settlementKey,activity,skillKey,structureContext);'
 s=once(s,anchor,'''JSONObject contextual=new JSONObject();JSONObject actionContext=structureActionContext.get();
        if(actionContext!=null){Iterator<String> contextKeys=actionContext.keys();while(contextKeys.hasNext()){String field=contextKeys.next();contextual.put(field,actionContext.opt(field));}}
        Iterator<String> explicitKeys=structureContext.keys();while(explicitKeys.hasNext()){String field=explicitKeys.next();contextual.put(field,structureContext.opt(field));}
        contextual.put("ability",rules.abilityForSkill(skillKey));
        String contextualSettlement=clean(settlementKey);if(contextualSettlement.length()==0)contextualSettlement=contextual.optString("settlement_key");
        int structureItem=officialStructureActivityBonus(contextualSettlement,activity,skillKey,contextual);''')
 # Expose context for a manually entered kingdom check as well as a named activity.
 anchor='    private JSONObject recordCheckAtSettlement(String settlementKey,String activityKey,String title,String role,String skillKey,int natural,int dc,String sourceRef,String notes,boolean awardReputationOnCrit,JSONObject structureContext)throws Exception{'
 wrapper='''    public JSONObject recordCheck(String activityKey,String title,String role,String skillKey,int natural,int dc,String sourceRef,String notes,boolean awardReputationOnCrit,JSONObject context)throws Exception{
        JSONObject safe=context==null?new JSONObject():new JSONObject(context.toString());
        return recordCheckAtSettlement(safe.optString("settlement_key"),activityKey,title,role,skillKey,natural,dc,sourceRef,notes,awardReputationOnCrit,safe);
    }
'''
 s=once(s,anchor,wrapper+anchor)
 # Focus must be selected before spending or rolling, even on a later resumed build.
 start=s.index('    public JSONObject resolveBuildStructure(');end=s.index('    public JSONObject resolveDemolish(',start)
 part=s[start:end];anchor='        KingmakerStructureSemantics.requireCapital(def,town,state().optString("capital_name"));'
 part=once(part,anchor,anchor+'''
        if(("guildhall".equals(chosenKey)||"trade_shop".equals(chosenKey))&&clean(buildOptions.optString("trade_focus")).length()==0)throw new IllegalArgumentException("Indique o foco comercial deste estabelecimento antes de construir.");''')
 s=s[:start]+part+s[end:]
 anchor='effects.put("official_build_effects",buildEffects);strV.put("effects_json", effects.toString());'
 replacement='''effects.put("official_build_effects",buildEffects);
                if("guildhall".equals(k)||"trade_shop".equals(k))effects.put("trade_focus_instances",KingmakerStructureSemantics.appendedGuildFocus(existing,buildOptions.optString("trade_focus")));
                strV.put("effects_json", effects.toString());'''
 s=once(s,anchor,replacement)
 helper='''    public void declareSettlementTradeFocus(final String settlementKey,final String structureKey,final int instance,final String focus)throws Exception{
        if(!"guildhall".equals(structureKey)&&!"trade_shop".equals(structureKey))throw new IllegalArgumentException("O estabelecimento não usa foco comercial.");
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject row=db.queryOne("SELECT * FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key=?",new String[]{profile(),settlementKey,structureKey});
            if(row.length()==0||!"built".equals(row.optString("status","built")))throw new IllegalStateException("Estabelecimento concluído inexistente.");
            JSONObject before=new JSONObject(row.toString()),effect=jsonObject(row.optString("effects_json","{}"));
            effect.put("trade_focus_instances",KingmakerStructureSemantics.declareGuildFocus(row,instance,focus));
            ContentValues v=new ContentValues();v.put("effects_json",effect.toString());v.put("updated_at",System.currentTimeMillis());
            if(db.getWritableDatabase().update("kingmaker_structures",v,"world_profile=? AND settlement_key=? AND structure_key=?",new String[]{profile(),settlementKey,structureKey})!=1)throw new IllegalStateException("Foco não persistido.");
            audit("Foco comercial definido",structureKey+" #"+instance+" • "+focus,before,new JSONObject().put("effects_json",effect));
        }});
    }
    public int pcStructureItemBonus(String settlementKey,String activity,String skill,String focus){
        if(settlement(settlementKey).length()==0)throw new IllegalArgumentException("Assentamento inexistente.");
        return KingmakerStructureSemantics.pcItemBonus(new JSONArray(structures(settlementKey)),activity,skill,focus);
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
def focus_ui(s):
 # Construction focus is plain-language trade information, not an implementation field.
 anchor='        final EditText roll=labeledField(form,"d20 de construção",String.valueOf(new java.util.Random().nextInt(20)+1),false);'
 s=once(s,anchor,'''        final EditText tradeFocus=labeledField(form,"Foco comercial (Salão de Guilda / Loja de Ofício)","",false);
'''+anchor)
 anchor='new JSONObject().put("ruin",ruinKeys[ruinChoice.getSelectedItemPosition()]).put("die1",new Random().nextInt(4)+1).put("die2",new Random().nextInt(4)+1)'
 s=once(s,anchor,anchor+'.put("trade_focus",value(tradeFocus))')
 # General checks can declare which established trade applies and their settlement.
 start=s.index('    private void showKingdomCheckDialog()');end=s.index('\n    private ',start+20);part=s[start:end]
 anchor='natural.setInputType(InputType.TYPE_CLASS_NUMBER);'
 new='''final EditText relatedTrade=labeledField(form,"Foco comercial relacionado ao teste (opcional)","",false);final List<JSONObject> checkTowns=kingmakerRepository.settlements();final String[] locations=new String[checkTowns.size()+1];locations[0]="Capital / alcance do Reino";for(int i=0;i<checkTowns.size();i++)locations[i+1]=checkTowns.get(i).optString("name");final Spinner checkTown=spinner(locations,locations[0]);form.addView(cardWithLabel("LOCAL DO TESTE",checkTown));'''+anchor
 part=once(part,anchor,new)
 old='num(dc,0),value(source),value(notes));'
 new='''num(dc,0),value(source),value(notes),false,new JSONObject().put("trade_focus",value(relatedTrade)).put("settlement_key",checkTown.getSelectedItemPosition()==0?"":checkTowns.get(checkTown.getSelectedItemPosition()-1).optString("settlement_key")));'''
 part=once(part,old,new);s=s[:start]+part+s[end:]
 # Existing unregistered shops get a one-time declaration without rebuilding or charging.
 start=s.index('    private void showKingmakerSettlementDetails(');end=s.index('\n    private ',start+20);part=s[start:end]
 part=once(part,'showKingmakerSettlementEditDialog(settlement);','showSettlementEditOrTradeFocus(settlement);');s=s[:start]+part+s[end:]
 helper='''    private void showSettlementEditOrTradeFocus(final JSONObject settlement){
        new AlertDialog.Builder(this).setTitle(settlement.optString("name")).setItems(new String[]{"Editar assentamento","Definir foco comercial de estabelecimentos antigos"},new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){if(which==0)showKingmakerSettlementEditDialog(settlement);else showSettlementTradeFocusDialog(settlement);}}).setNegativeButton("Fechar",null).show();
    }
    private void showSettlementTradeFocusDialog(final JSONObject settlement){
        final List<String> kinds=new ArrayList<String>();final List<Integer> instances=new ArrayList<Integer>();final List<String> labels=new ArrayList<String>();
        for(JSONObject row:kingmakerRepository.structures(settlement.optString("settlement_key"))){String k=row.optString("structure_key");if(!"guildhall".equals(k)&&!"trade_shop".equals(k))continue;JSONObject effect=parseLooseJson(row.optString("effects_json","{}"));JSONArray focuses=effect.optJSONArray("trade_focus_instances");for(int i=0;i<row.optInt("count",1);i++)if(focuses==null||focuses.optString(i).length()==0){kinds.add(k);instances.add(i+1);labels.add(row.optString("name")+" #"+(i+1));}}
        if(labels.isEmpty()){toast("Todos os estabelecimentos existentes já têm foco definido.");return;}
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(12),0,dp(12),dp(16));final Spinner building=spinner(labels.toArray(new String[0]),labels.get(0));form.addView(cardWithLabel("ESTABELECIMENTO",building));final EditText trade=labeledField(form,"Foco comercial (ex.: padaria, ferraria)","",false);
        new AlertDialog.Builder(this).setTitle("Registrar foco comercial").setView(scrollableDialogView(form)).setNegativeButton("Cancelar",null).setPositiveButton("REGISTRAR",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{int selected=building.getSelectedItemPosition();kingmakerRepository.declareSettlementTradeFocus(settlement.optString("settlement_key"),kinds.get(selected),instances.get(selected),value(trade));toast("Foco registrado.");showKingmakerSettlementDetails(settlement);}catch(Exception error){toast(error.getMessage());}}}).show();
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
