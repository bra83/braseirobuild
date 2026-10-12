"""Resolve demolition per lot; the first attempt disables the whole building."""
def once(s,a,b):
 if s.count(a)!=1:raise ValueError("Demolition anchor count="+str(s.count(a))+" "+a[:85])
 return s.replace(a,b,1)
def demolition_repository(s):
 start=s.index('    public JSONObject resolveDemolish(String settlementKey, String structureKey, String notes)')
 end=s.index('\n    /** Legacy entry point',start)
 new='''    public JSONObject resolveDemolish(String settlementKey,String structureKey,String notes)throws Exception{
        return resolveDemolish(settlementKey,structureKey,new java.util.Random().nextInt(20)+1,0,true,"lumber",new java.util.Random().nextInt(6)+1,notes);
    }
    public JSONObject resolveDemolish(final String settlementKey,final String structureKey,final int naturalRoll,final int chosenLot,final boolean clearEntire,final String commodity,final int recoveryD6,final String notes)throws Exception{
        requireEnabled();
        if(naturalRoll<1||naturalRoll>20)throw new IllegalArgumentException("Informe d20 natural entre 1 e 20.");
        final JSONObject town=settlement(settlementKey);
        final JSONObject before=db.queryOne("SELECT * FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key=?",new String[]{profile(),clean(settlementKey),key(structureKey)});
        if(town.length()==0||before.length()==0||before.optInt("count")<1||before.optInt("lots")<1)throw new IllegalArgumentException("Selecione uma estrutura ou escombros que ocupem lotes.");
        JSONObject turn=activeTurn();if(turn.length()==0||!"ACTIVITY".equals(turn.optString("phase")))throw new IllegalStateException("Demolir exige a fase de Atividades do Reino.");
        final JSONArray positions=KingmakerStructureSemantics.lotPlacements(before);
        if(positions.length()!=before.optInt("count"))throw new IllegalStateException("Registre os lotes existentes antes de demoli-los.");
        final JSONObject oldEffects=jsonObject(before.optString("effects_json","{}"));
        final boolean rubbleGroup=oldEffects.optBoolean("demolition_group");
        final JSONObject footprint=positions.optJSONObject(rubbleGroup?0:positions.length()-1);
        if(footprint==null||footprint.optJSONArray("lots")==null)throw new IllegalStateException("A posição física não está registrada.");
        final JSONArray physical=new JSONArray();
        if(rubbleGroup){for(int n=0;n<positions.length();n++){
            JSONObject p=positions.optJSONObject(n);
            if(p==null||p.optInt("urban_grid")!=footprint.optInt("urban_grid")||p.optInt("block")!=footprint.optInt("block")||p.optJSONArray("lots")==null||p.optJSONArray("lots").length()!=1)throw new IllegalStateException("Grupo de escombros inválido.");
            physical.put(p.optJSONArray("lots").opt(0));
        }}else{JSONArray lots=footprint.optJSONArray("lots");for(int n=0;n<lots.length();n++)physical.put(lots.opt(n));}
        final int selected=chosenLot>0?chosenLot:Integer.parseInt(physical.optString(0));
        // Validate the choice before logging a check or changing the settlement.
        KingmakerStructureSemantics.demolitionOutcome(physical,selected,"SUCCESS",clearEntire,commodity,recoveryD6);
        final JSONObject[] out={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject actual=db.queryOne("SELECT * FROM kingmaker_structures WHERE id=? AND world_profile=?",new String[]{String.valueOf(before.optLong("id")),profile()});
            if(actual.optInt("count")!=before.optInt("count")||!actual.optString("effects_json").equals(before.optString("effects_json")))throw new IllegalStateException("A estrutura mudou; atualize a seleção.");
            JSONObject check=recordCheckAtSettlement(settlementKey,"demolish","Demolir — "+before.optString("name"),"Liderança","engineering",naturalRoll,0,"Kingmaker pp.523–524",notes,false);
            JSONObject result=KingmakerStructureSemantics.demolitionOutcome(physical,selected,check.optString("degree"),clearEntire,commodity,recoveryD6);
            JSONArray remaining=result.optJSONArray("remaining_rubble_lots");
            if(!rubbleGroup&&before.optInt("count")>1){
                ContentValues changed=new ContentValues();changed.put("count",before.optInt("count")-1);changed.put("effects_json",KingmakerStructureSemantics.trimInstanceEffects(before,before.optInt("count")-1).toString());changed.put("updated_at",System.currentTimeMillis());
                db.getWritableDatabase().update("kingmaker_structures",changed,"id=? AND world_profile=?",new String[]{String.valueOf(before.optLong("id")),profile()});
            }else db.getWritableDatabase().delete("kingmaker_structures","id=? AND world_profile=?",new String[]{String.valueOf(before.optLong("id")),profile()});
            if(remaining.length()>0){
                JSONArray rubblePositions=new JSONArray();for(int n=0;n<remaining.length();n++)rubblePositions.put(new JSONObject().put("urban_grid",footprint.optInt("urban_grid")).put("block",footprint.optInt("block")).put("lots",new JSONArray().put(remaining.opt(n))));
                JSONObject effect=new JSONObject().put("demolition_group",true).put("construction_status","rubble").put("original_structure_key",oldEffects.optString("original_structure_key",structureKey)).put("lot_instances",rubblePositions).put("original_structure_disabled",true);
                ContentValues rubble=new ContentValues();long now=System.currentTimeMillis();
                String rubbleKey=rubbleGroup?structureKey:"rubble_demolition_"+before.optLong("id")+"_"+java.util.UUID.randomUUID().toString().replace("-","");
                rubble.put("world_profile",profile());rubble.put("settlement_key",settlementKey);rubble.put("structure_key",rubbleKey);rubble.put("name",rubbleGroup?before.optString("name"):"Escombros — "+before.optString("name"));rubble.put("count",remaining.length());rubble.put("lots",1);rubble.put("status","rubble");rubble.put("construction_rp",0);rubble.put("commodity_costs_json","{}");rubble.put("effects_json",effect.toString());rubble.put("source_ref","Kingmaker pp.523–524");rubble.put("notes",clean(notes));rubble.put("created_at",now);rubble.put("updated_at",now);
                long rubbleId=db.getWritableDatabase().insertOrThrow("kingmaker_structures",null,rubble);if(rubbleId<=0)throw new IllegalStateException("Os escombros não foram persistidos.");
                result.put("rubble_structure_key",rubbleKey);
            }
            refreshSettlementOccupancy(settlementKey);applyStructureSkillBonuses();
            JSONObject delta=new JSONObject();if(result.optInt("unrest")>0)delta.put("unrest",result.optInt("unrest"));
            if(result.optInt("recovered")>0){
                String kind=result.optString("commodity");int capacity=officialCommodityCapacity(kind),gain=Math.min(result.optInt("recovered"),Math.max(0,capacity-state().optInt(kind)));
                delta.put(kind,gain);result.put("commodities_stored",gain).put("commodities_over_capacity",result.optInt("recovered")-gain);
            }
            if(delta.length()>0)adjustState(delta,"Demolição por lote");
            result.put("check",check).put("outcome_pt","Lotes liberados: "+result.optJSONArray("cleared_lots")+". Escombros restantes: "+remaining+". A estrutura demolida não fornece efeitos.");
            audit("Demolição por lote",before.optString("name"),before,result);out[0]=result;
        }});return out[0];
    }
'''
 s=s[:start]+new+s[end:]
 old='return resolveDemolish(p.optString("settlement_key"), p.optString("structure_key"), notes);'
 new='return resolveDemolish(p.optString("settlement_key"),p.optString("structure_key"),nat,p.optInt("lot",0),p.optBoolean("clear_entire",true),p.optString("recovery_commodity","lumber"),p.optInt("recovery_d6",new java.util.Random().nextInt(6)+1),notes);'
 return once(s,old,new)
def demolition_ui(s):
 s=once(s,'"Organizar lotes das estruturas existentes"}','"Organizar lotes das estruturas existentes","Demolir lote / limpar escombros"}')
 s=once(s,'else showExistingStructureLotsDialog(settlement);','else if(which==3)showExistingStructureLotsDialog(settlement);else showSettlementDemolitionDialog(settlement);')
 helper='''    private void showSettlementDemolitionDialog(final JSONObject town){
        final List<JSONObject> rows=new ArrayList<JSONObject>();final List<String> keys=new ArrayList<String>();
        for(JSONObject row:kingmakerRepository.structures(town.optString("settlement_key")))if(row.optInt("lots")>0&&row.optInt("count")>0){rows.add(row);keys.add(row.optString("name")+" • último exemplar #"+row.optInt("count"));}
        if(rows.isEmpty()){toast("Não há estruturas com lotes para demolir.");return;}
        new AlertDialog.Builder(this).setTitle("Demolir estrutura / escombros").setItems(keys.toArray(new String[0]),new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int selected){showSettlementDemolitionRollDialog(town,rows.get(selected));}}).setNegativeButton("Voltar",null).show();
    }
    private void showSettlementDemolitionRollDialog(final JSONObject town,final JSONObject row){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(12),0,dp(12),dp(16));
        form.addView(text("Engenharia contra a CD de Controle. Uma tentativa interrompe os efeitos de toda a estrutura. Sucesso libera um lote; somente sucesso crítico pode liberar todos de uma vez.",13,TEXT,false));
        final EditText lot=labeledField(form,"Lote físico 1–4 (0 escolhe o primeiro da estrutura)","0",false),natural=labeledField(form,"d20 natural",String.valueOf(new Random().nextInt(20)+1),false),die=labeledField(form,"1d6 de recuperação",String.valueOf(new Random().nextInt(6)+1),false),notes=labeledField(form,"Notas","",true);
        final CheckBox entire=check("No sucesso crítico: liberar toda a estrutura em vez de recuperar commodities",true);form.addView(entire);
        final String[] kinds={"lumber","stone","ore"};final String[] names={"Madeira","Pedra","Minério"};final Spinner commodity=spinner(names,"Madeira");form.addView(cardWithLabel("COMMODITY NA RECUPERAÇÃO",commodity));
        new AlertDialog.Builder(this).setTitle("Demolir — "+row.optString("name")).setView(scrollableDialogView(form)).setNegativeButton("Cancelar",null).setPositiveButton("RESOLVER",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{
            JSONObject result=kingmakerRepository.resolveDemolish(town.optString("settlement_key"),row.optString("structure_key"),num(natural,0),num(lot,0),entire.isChecked(),kinds[commodity.getSelectedItemPosition()],num(die,0),value(notes));
            showKingmaker();toast(result.optString("outcome_pt"));
        }catch(Exception error){toast(error.getMessage());}}}).show();
    }
'''
 i=s.rfind("\n}");return s[:i]+"\n"+helper+s[i:]
