"""Expose paid, resumable in-place upgrades through the ordinary settlement UI."""
def once(s,a,b):
 if s.count(a)!=1:raise ValueError('Upgrade anchor count='+str(s.count(a))+' '+a[:80])
 return s.replace(a,b,1)
def upgrade_repository(s):
 anchor='        } else if ("build_structure".equals(k)) {'
 s=once(s,anchor,anchor+'\n            if(p.optString("upgrade_from").length()>0)return resolveStructureUpgrade(p.optString("settlement_key"),p.optString("upgrade_from"),p.optString("structure_key"),nat,skill,new JSONObject(p.toString()),notes);')
 helper='''
    public void declareHistoricalConstructionPayment(final String townKey,final String structureKey,final JSONObject payment,final String evidence)throws Exception{
        if(clean(evidence).length()<8)throw new IllegalArgumentException("Informe a fonte do pagamento histórico.");
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject row=db.queryOne("SELECT * FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key=?",new String[]{profile(),townKey,structureKey});
            if(row.length()==0||row.optInt("count")<1)throw new IllegalArgumentException("Estrutura inexistente.");
            JSONObject effects=jsonObject(row.optString("effects_json","{}"));JSONArray paid=effects.optJSONArray("construction_payments");
            if(paid==null){paid=new JSONArray();for(int n=0;n<row.optInt("count");n++)paid.put(new JSONObject().put("authority","unknown"));}
            if(paid.length()!=row.optInt("count"))throw new IllegalStateException("Pagamentos desalinhados com os exemplares.");
            int index=row.optInt("count")-1;if(!"unknown".equals(paid.optJSONObject(index).optString("authority")))throw new IllegalStateException("O pagamento deste exemplar já está comprovado.");
            JSONObject receipt=new JSONObject(payment.toString()).put("authority","declared_history").put("evidence",clean(evidence));
            KingmakerStructureSemantics.appendedConstructionPayment(new JSONObject(),receipt);
            JSONArray replacement=new JSONArray();for(int n=0;n<paid.length();n++)replacement.put(n==index?receipt:paid.opt(n));effects.put("construction_payments",replacement);
            ContentValues v=new ContentValues();v.put("effects_json",effects.toString());v.put("updated_at",System.currentTimeMillis());
            if(db.getWritableDatabase().update("kingmaker_structures",v,"id=? AND world_profile=?",new String[]{String.valueOf(row.optLong("id")),profile()})!=1)throw new IllegalStateException("Pagamento não persistido.");
            audit("Pagamento histórico da construção",structureKey,row,receipt);
        }});
    }
    public JSONObject resolveStructureUpgrade(final String townKey,final String from,final String target,final int natural,final String skillKey,final JSONObject options,final String notes)throws Exception{
        requireEnabled();if(natural<1||natural>20)throw new IllegalArgumentException("d20 deve ser de 1 a 20.");
        final JSONObject[] out={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject turn=activeTurn();if(turn.length()==0||!"ACTIVITY".equals(turn.optString("phase")))throw new IllegalStateException("Upgrade exige a fase de Atividades do Reino.");
            JSONObject town=settlement(townKey),old=db.queryOne("SELECT * FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key=?",new String[]{profile(),townKey,from});
            JSONObject def=rules.structure(target);if(town.length()==0||old.length()==0||def==null||!"built".equals(old.optString("status","built")))throw new IllegalStateException("Assentamento ou estrutura ativa inexistente.");
            if(!KingmakerStructureSemantics.canUpgrade(from,target))throw new IllegalArgumentException("Upgrade não previsto no manual.");
            KingmakerStructureSemantics.requireCapital(def,town,state().optString("capital_name"));
            if(state().optInt("level",1)<def.optInt("level",1)||settlementRank(town.optString("type"))<settlementRank(def.optString("min_settlement_type","village")))throw new IllegalStateException("Reino ou assentamento abaixo do nível mínimo do upgrade.");
            String expected=key(def.optString("construction_skill",def.optString("skill_key",""))),allowed=def.optString("construction_skills",""),skill=key(skillKey);
            JSONObject proficiency=skill(skill);if(proficiency.length()==0||(allowed.length()>0?!java.util.Arrays.asList(allowed.split(",")).contains(skill):expected.length()>0&&!expected.equals(skill)))throw new IllegalArgumentException("Escolha uma perícia de construção permitida.");
            java.util.List<String> ranks=java.util.Arrays.asList("untrained","trained","expert","master","legendary");
            int required=ranks.indexOf(key(def.optString("construction_rank","untrained"))),actual=ranks.indexOf(key(proficiency.optString("rank","untrained")));
            if(required<0||actual<required)throw new IllegalStateException("Proficiência insuficiente para o upgrade.");
            if(("guildhall".equals(target)||"trade_shop".equals(target))&&clean(options.optString("trade_focus")).length()==0)throw new IllegalArgumentException("Informe o foco comercial.");
            String featureKey="upgrade:"+townKey+":"+old.optLong("id"),siteKey="under_construction_upgrade_"+old.optLong("id");
            JSONObject saved=jsonObject(db.queryOne("SELECT * FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),featureKey}).optString("effects_json","{}"));
            if("failed".equals(saved.optString("status"))){
                long site=saved.optLong("failed_site_id");
                if(db.queryOne("SELECT id FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND (structure_key=? OR structure_key LIKE ?)",new String[]{profile(),townKey,siteKey,"rubble_demolition_"+site+"_%"}).length()>0)throw new IllegalStateException("Demola todos os escombros do upgrade antes de reiniciar.");
                if(saved.optInt("last_attempt_turn")==turn.optInt("turn_number"))throw new IllegalStateException("Reinicie o upgrade somente no próximo turno.");
            }
            boolean resume="unfinished".equals(saved.optString("status"));
            JSONObject chosen=new JSONObject(options.toString());
            if(resume){
                if(!target.equals(saved.optString("target"))||!old.optString("effects_json").equals(saved.optString("original_effects")))throw new IllegalStateException("Retome o mesmo upgrade; a estrutura original não pode mudar durante a obra.");
                if(saved.optInt("last_attempt_turn")==turn.optInt("turn_number"))throw new IllegalStateException("Retome somente no próximo turno do Reino.");
                chosen=new JSONObject(saved.optJSONObject("options").toString());
            }
            JSONObject differential=resume?saved.optJSONObject("difference"):KingmakerStructureSemantics.upgradeCost(old,old.optInt("count")-1,def);
            JSONObject previousPayment=jsonObject(old.optString("effects_json","{}")).optJSONArray("construction_payments").optJSONObject(old.optInt("count")-1);
            JSONArray filtered=KingmakerStructureSemantics.withoutLastInstance(new JSONArray(structures(townKey)),from,siteKey);
            JSONArray extra=new JSONArray();JSONObject position=null;
            if(KingmakerStructureSemantics.isBorderInfrastructure(target)){
                JSONArray infrastructure=jsonObject(old.optString("effects_json","{}")).optJSONArray("infrastructure_instances");
                JSONObject original=infrastructure==null?null:infrastructure.optJSONObject(old.optInt("count")-1);
                if(original==null)throw new IllegalStateException("Registre a borda da muralha original.");
                chosen.put("urban_grid",original.optInt("urban_grid")).put("border",original.optString("border"));
                KingmakerStructureSemantics.requireBorderInfrastructure(town,filtered,target,chosen);
            }else{
                JSONObject original=KingmakerStructureSemantics.lotPlacements(old).optJSONObject(old.optInt("count")-1);
                position=KingmakerStructureSemantics.upgradePlacement(town,filtered,target,def.optInt("lots"),original,chosen.optJSONObject("lot_placement"));
                chosen.put("lot_placement",position);extra=KingmakerStructureSemantics.additionalUpgradeLots(original,position);
            }
            JSONObject resources=state();JSONObject charge=new JSONObject();
            for(String f:new String[]{"resource_points","lumber","stone","ore","luxuries"}){
                int amount=Math.max(0,differential.optInt(f));if(!resume&&resources.optInt(f)<amount)throw new IllegalStateException("Recursos insuficientes para o upgrade: "+f);charge.put(f,-amount);
            }
            if(!resume){adjustState(charge,"Upgrade: diferença positiva paga antes do teste");}
            JSONObject check=recordCheckAtSettlement(townKey,"build_structure","Upgrade: "+old.optString("name")+" → "+def.optString("name"),"Vice-rei",skill,natural,officialConstructionDc(def),"Kingmaker p.543",notes,false);
            String degree=check.optString("degree");boolean critical="CRITICAL_SUCCESS".equals(degree),complete=critical||"SUCCESS".equals(degree);
            if(complete){
                if(old.optInt("count")>1){ContentValues v=new ContentValues();v.put("count",old.optInt("count")-1);v.put("effects_json",KingmakerStructureSemantics.trimInstanceEffects(old,old.optInt("count")-1).toString());v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_structures",v,"id=? AND world_profile=?",new String[]{String.valueOf(old.optLong("id")),profile()});}
                else db.getWritableDatabase().delete("kingmaker_structures","id=? AND world_profile=?",new String[]{String.valueOf(old.optLong("id")),profile()});
                db.getWritableDatabase().delete("kingmaker_structures","world_profile=? AND settlement_key=? AND structure_key=?",new String[]{profile(),townKey,siteKey});
                JSONObject refund=new JSONObject(),receipt=new JSONObject().put("authority","paid").put("upgraded_from",from).put("original_payment",new JSONObject(previousPayment.toString()));
                for(String f:new String[]{"resource_points","lumber","stone","ore","luxuries"}){
                    int delta=differential.optInt(f),crit=critical&&!"resource_points".equals(f)?Math.max(0,delta)/2:0;
                    refund.put(f,Math.max(0,-delta)+crit);receipt.put(f,previousPayment.optInt(f)+delta-crit);
                }
                chosen.put("construction_payment",receipt);
                JSONObject completed=buildStructure(townKey,target,notes,true,chosen);adjustState(refund,"Upgrade concluído: diferenças negativas e devolução crítica");
                out[0]=new JSONObject().put("status","completed").put("check",check).put("structure",completed).put("refund",refund);
            }else{
                boolean rubble="CRITICAL_FAILURE".equals(degree);
                if(extra.length()>0){
                    JSONObject reservation=new JSONObject().put("urban_grid",position.optInt("urban_grid")).put("block",position.optInt("block")).put("lots",extra);
                    JSONObject effects=new JSONObject().put("construction_status",rubble?"rubble":"unfinished").put("original_structure_key",target).put("lot_instances",new JSONArray().put(reservation));
                    ContentValues v=new ContentValues();long now=System.currentTimeMillis();v.put("world_profile",profile());v.put("settlement_key",townKey);v.put("structure_key",siteKey);v.put("name",(rubble?"Escombros do upgrade: ":"Reserva do upgrade: ")+def.optString("name"));v.put("count",1);v.put("lots",extra.length());v.put("status",rubble?"rubble":"unfinished");v.put("effects_json",effects.toString());v.put("created_at",now);v.put("updated_at",now);v.put("source_ref","Kingmaker p.543");db.getWritableDatabase().insertWithOnConflict("kingmaker_structures",null,v,SQLiteDatabase.CONFLICT_REPLACE);
                }
                out[0]=new JSONObject().put("failed_site_id",db.queryOne("SELECT id FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key=?",new String[]{profile(),townKey,siteKey}).optLong("id")).put("status",rubble?"failed":"unfinished").put("check",check).put("old_structure_still_active",true).put("reserved_lots",extra);
            }
            JSONObject progress=new JSONObject(out[0].toString()).put("target",target).put("difference",differential).put("options",chosen).put("original_effects",old.optString("effects_json")).put("last_attempt_turn",turn.optInt("turn_number"));
            upsertFeature(featureKey,"construction_upgrade","Upgrade: "+from+" → "+target,state().optInt("level",1),progress.toString(),"Kingmaker p.543",notes);
            refreshSettlementOccupancy(townKey);audit("Upgrade de estrutura",from+" → "+target,old,out[0]);
        }});return out[0];
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
def upgrade_ui(s):
 anchor='    private void showKingmakerStructureDialog(final JSONObject settlement){'
 s=once(s,anchor,'''    private void showKingmakerStructureDialog(final JSONObject settlement){
        new AlertDialog.Builder(this).setTitle("Construção em "+settlement.optString("name")).setItems(new String[]{"Construir nova estrutura","Melhorar estrutura existente"},new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){if(which==0)showKingmakerNewStructureDialog(settlement);else showKingmakerUpgradeSelection(settlement);}}).setNegativeButton("Voltar",null).show();
    }
    private void showKingmakerNewStructureDialog(final JSONObject settlement){''')
 helper='''
    private void showKingmakerUpgradeSelection(final JSONObject town){
        final List<JSONObject> rows=new ArrayList<JSONObject>();final List<String> labels=new ArrayList<String>();
        for(JSONObject row:kingmakerRepository.structures(town.optString("settlement_key")))if("built".equals(row.optString("status","built"))&&row.optInt("count")>0){rows.add(row);labels.add(row.optString("name")+" • exemplar #"+row.optInt("count"));}
        new AlertDialog.Builder(this).setTitle("Estrutura original").setItems(labels.toArray(new String[0]),new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int selected){showKingmakerUpgradeDialog(town,rows.get(selected));}}).setNegativeButton("Voltar",null).show();
    }
    private void showKingmakerUpgradeDialog(final JSONObject town,final JSONObject old){
        final List<String> targets=new ArrayList<String>(),labels=new ArrayList<String>();
        String[] all={"academy","military_academy","university","garrison","castle","palace","luxury_store","marketplace","magic_shop","occult_shop","theater","opera_house","guildhall","hospital","houses","mansion","orphanage","noble_villa","menagerie","temple","cathedral","tavern_popular","tavern_luxury","tavern_world_class","waterfront","wall_stone"};
        for(String target:all)if(KingmakerStructureSemantics.canUpgrade(old.optString("structure_key"),target)){targets.add(target);labels.add(kingmakerRules.structure(target).optString("name",target));}
        if(targets.isEmpty()){toast("Esta estrutura não tem upgrade no manual.");return;}
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(12),0,dp(12),dp(16));
        form.addView(text("O pagamento usa o custo efetivo do exemplar. A estrutura antiga funciona até a conclusão. O upgrade ocupa o mesmo bloco; uma falha pode ser retomada no próximo turno sem repagar.",13,TEXT,false));
        final Spinner targetChoice=spinner(labels.toArray(new String[0]),labels.get(0));form.addView(cardWithLabel("UPGRADE",targetChoice));
        final EditText lots=labeledField(form,"Lotes finais (vazio escolhe no mesmo bloco)","",false),focus=labeledField(form,"Foco comercial (Guildhall)","",false),roll=labeledField(form,"d20",String.valueOf(new Random().nextInt(20)+1),false),skill=labeledField(form,"Perícia (vazio usa a padrão da estrutura)","",false),notes=labeledField(form,"Notas","",true);roll.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this).setTitle("Melhorar "+old.optString("name")).setView(scrollableDialogView(form)).setNegativeButton("Cancelar",null).setNeutralButton("PAGAMENTO HISTÓRICO",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){showKingmakerHistoricalPayment(town,old);}}).setPositiveButton("RESOLVER UPGRADE",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{
            String target=targets.get(targetChoice.getSelectedItemPosition());JSONObject def=kingmakerRules.structure(target),options=new JSONObject().put("trade_focus",value(focus));
            if(value(lots).length()>0)options.put("lot_placement",new JSONObject().put("lots",parseDiceList(value(lots),4,4)));
            String chosen=value(skill);if(chosen.length()==0)chosen=def.optString("construction_skill",def.optString("skill_key","engineering"));
            JSONObject result=kingmakerRepository.resolveStructureUpgrade(town.optString("settlement_key"),old.optString("structure_key"),target,num(roll,0),chosen,options,value(notes));showKingmaker();toast("Upgrade: "+result.optString("status"));
        }catch(Exception e){toast("Upgrade: "+e.getMessage());}}}).show();
    }
    private void showKingmakerHistoricalPayment(final JSONObject town,final JSONObject old){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(12),0,dp(12),dp(16));form.addView(text("Informe valores realmente pagos após devoluções. Construção gratuita: todos zero. Este registro não altera os recursos atuais.",13,TEXT,false));
        final EditText rp=labeledField(form,"RP pagos","",false),wood=labeledField(form,"Madeira paga","",false),stone=labeledField(form,"Pedra paga","",false),ore=labeledField(form,"Minério pago","",false),lux=labeledField(form,"Luxos pagos","",false),evidence=labeledField(form,"Fonte / registro que comprova o pagamento","",true);
        new AlertDialog.Builder(this).setTitle("Pagamento do exemplar #"+old.optInt("count")).setView(scrollableDialogView(form)).setNegativeButton("Cancelar",null).setPositiveButton("REGISTRAR",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{
            for(EditText field:new EditText[]{rp,wood,stone,ore,lux})if(value(field).length()==0)throw new IllegalArgumentException("Preencha todos os custos, inclusive zeros.");
            kingmakerRepository.declareHistoricalConstructionPayment(town.optString("settlement_key"),old.optString("structure_key"),new JSONObject().put("resource_points",num(rp,-1)).put("lumber",num(wood,-1)).put("stone",num(stone,-1)).put("ore",num(ore,-1)).put("luxuries",num(lux,-1)),value(evidence));showKingmakerUpgradeDialog(town,old);
        }catch(Exception e){toast("Pagamento histórico: "+e.getMessage());}}}).show();
    }
'''
 # Existing app field name is checked by the Android compiler.
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
