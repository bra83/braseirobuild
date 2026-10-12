"""Immutable water borders, bridge/wall placement and island kingdom effects."""
def once(s,a,b):
 if s.count(a)!=1:raise ValueError('Border anchor count='+str(s.count(a))+' '+a[:75])
 return s.replace(a,b,1)
def border_repository(s):
 start=s.index('    public JSONObject resolveBuildStructure(');end=s.index('    public JSONObject resolveDemolish(',start);part=s[start:end]
 a='''        else if(def.optBoolean("requires_special_placement",false))'''
 part=once(part,a,'''        else if(KingmakerStructureSemantics.isBorderInfrastructure(chosenKey))
            KingmakerStructureSemantics.requireBorderInfrastructure(town,new JSONArray(structures(townKey)),chosenKey,buildOptions);
'''+a)
 part=once(part,'new String[]{"trade_focus","ruin","urban_grid"}','new String[]{"trade_focus","ruin","urban_grid","border"}')
 s=s[:start]+part+s[end:]
 s=once(s,'if(KingmakerStructureSemantics.isGridInfrastructure(k))effects.put("infrastructure_instances"','if(KingmakerStructureSemantics.isInfrastructure(k))effects.put("infrastructure_instances"')
 start=s.index('    private int officialStructureActivityBonus(');end=s.index('    public int officialCommodityCapacity(',start)
 part=once(s[start:end],'Math.max(0,town.optInt("influence"))','officialSettlementInfluence(town)');s=s[:start]+part+s[end:]
 s=once(s,'Math.max(0,settlement.optInt("influence"))','officialSettlementInfluence(settlement)')
 s=once(s,'if(!originalOptions.optString(field).equals(buildOptions.optString(field)))','if(originalOptions.has(field)&&!originalOptions.optString(field).equals(buildOptions.optString(field)))')
 s=once(s,'!KingmakerStructureSemantics.isGridInfrastructure(structure)','!KingmakerStructureSemantics.isInfrastructure(structure)')
 s=once(s,'if("repair_structure".equals(activity))civicItemBonus=Math.max(civicItemBonus,2);',
 '''if("repair_structure".equals(activity))civicItemBonus=Math.max(civicItemBonus,2);
        int islandTradePenalty="trade".equals(key(skillKey))?officialIslandTradePenalty():0;
        civicItemBonus-=islandTradePenalty;''')
 s=once(s,'.put("item",civicItemBonus)', '.put("item",civicItemBonus).put("island_trade_item_penalty",-islandTradePenalty)')
 s=once(s,'v.put("grid_json",jsonObject(gridJson).toString());','v.put("grid_json",KingmakerStructureSemantics.preserveWaterBorders(jsonObject(old.optString("grid_json","{}")),jsonObject(gridJson)).toString());')
 s=once(s,'v.put("influence",rules.settlementInfluence(settlementType));','v.put("influence",KingmakerStructureSemantics.isIsland(new JSONObject(old.toString()).put("developed_blocks",blocks),new JSONArray(structures(old.optString("settlement_key"))))?0:rules.settlementInfluence(settlementType));')
 helper='''    private int officialSettlementInfluence(JSONObject town){
        return KingmakerStructureSemantics.isIsland(town,new JSONArray(structures(town.optString("settlement_key"))))?0:Math.max(0,rules.settlementInfluence(town.optString("type")));
    }
    private int officialIslandTradePenalty(){int count=0;for(JSONObject town:settlements())if(KingmakerStructureSemantics.isIsland(town,new JSONArray(structures(town.optString("settlement_key")))))count++;return count;}
    public void configureSettlementWaterBorders(final String settlementKey,final int urbanGrid,final JSONArray waterBorders)throws Exception{
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject town=settlement(settlementKey);if(town.length()==0)throw new IllegalArgumentException("Assentamento inexistente.");
            JSONObject territory=hex(town.optString("hex_id"));JSONObject atlas=db.getMapHex(territory.optString("map_package_id"),town.optString("hex_id"));
            String terrain=(territory.optString("terrain")+" "+atlas.optString("terrain_primary")+" "+atlas.optString("terrain_secondary")).toLowerCase(java.util.Locale.ROOT);
            java.util.List<String> terrainWords=java.util.Arrays.asList(terrain.replace('â','a').split("[^a-z]+"));
            boolean hasWater=!java.util.Collections.disjoint(terrainWords,java.util.Arrays.asList("river","lake","swamp","rio","lago","pantano"))||jsonArray(atlas.optString("waterways_json","[]")).length()>0;
            JSONObject grid=KingmakerStructureSemantics.declareWaterBorders(town,urbanGrid,waterBorders,hasWater);
            ContentValues values=new ContentValues();values.put("grid_json",grid.toString());values.put("updated_at",System.currentTimeMillis());
            JSONObject adjusted=new JSONObject(town.toString()).put("grid_json",grid);
            values.put("influence",KingmakerStructureSemantics.isIsland(adjusted,new JSONArray(structures(settlementKey)))?0:rules.settlementInfluence(town.optString("type")));
            if(db.getWritableDatabase().update("kingmaker_settlements",values,"world_profile=? AND settlement_key=?",new String[]{profile(),settlementKey})!=1)throw new IllegalStateException("Bordas não persistidas.");
            audit("Bordas do assentamento registradas","Malha "+urbanGrid,town,settlement(settlementKey));
        }});
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
def border_ui(s):
 a='final EditText urbanGrid=labeledField(form,"Malha urbana nº (iluminação / ruas / esgoto)","1",false);'
 s=once(s,a,'final EditText urbanGrid=labeledField(form,"Malha urbana nº (infraestruturas)","1",false);')
 a='        urbanGrid.setInputType(InputType.TYPE_CLASS_NUMBER);'
 s=once(s,a,a+'''
        final String[] borderKeys={"north","east","south","west"};final Spinner infrastructureBorder=spinner(new String[]{"Norte","Leste","Sul","Oeste"},"Norte");form.addView(cardWithLabel("BORDA (PONTES / MURALHAS)",infrastructureBorder));''')
 s=once(s,'.put("urban_grid",num(urbanGrid,0))','.put("urban_grid",num(urbanGrid,0)).put("border",borderKeys[infrastructureBorder.getSelectedItemPosition()])')
 s=once(s,'Pontes e muralhas exigem posição de borda e permanecem bloqueadas neste formulário.','Pontes exigem uma borda de água previamente registrada; muralhas ocupam a borda indicada. Registre as bordas em EDITAR antes de instalar uma ponte.')
 s=once(s,'new String[]{"Editar assentamento","Definir foco comercial de estabelecimentos antigos"}','new String[]{"Editar assentamento","Definir foco comercial de estabelecimentos antigos","Registrar bordas de água"}')
 s=once(s,'else showSettlementTradeFocusDialog(settlement);','else if(which==1)showSettlementTradeFocusDialog(settlement);else showSettlementWaterBordersDialog(settlement);')
 helper='''    private void showSettlementWaterBordersDialog(final JSONObject settlement){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(12),0,dp(12),dp(16));form.addView(text("Declare as bordas escolhidas ao fundar o assentamento ou ao adicionar uma malha. A escolha fica salva e não pode ser alterada. Bordas de água exigem lago, rio ou pântano no hex.",13,TEXT,false));
        final EditText grid=labeledField(form,"Malha urbana nº","1",false);grid.setInputType(InputType.TYPE_CLASS_NUMBER);
        final String[] keys={"north","east","south","west"};final CheckBox[] water=new CheckBox[4];String[] names={"Norte","Leste","Sul","Oeste"};for(int i=0;i<4;i++){water[i]=check(names[i]+" — água",false);form.addView(water[i]);}
        new AlertDialog.Builder(this).setTitle("Bordas de "+settlement.optString("name")).setView(scrollableDialogView(form)).setNegativeButton("Cancelar",null).setPositiveButton("REGISTRAR",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{JSONArray borders=new JSONArray();for(int i=0;i<4;i++)if(water[i].isChecked())borders.put(keys[i]);kingmakerRepository.configureSettlementWaterBorders(settlement.optString("settlement_key"),num(grid,0),borders);showKingmaker();toast("Bordas registradas.");}catch(Exception error){toast(error.getMessage());}}}).show();
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
