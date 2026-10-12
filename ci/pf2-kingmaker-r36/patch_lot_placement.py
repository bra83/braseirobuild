"""Record per-instance lots; enforce contiguity, water and block restrictions."""
def once(s,a,b):
 if s.count(a)!=1:raise ValueError('Lot anchor count='+str(s.count(a))+' '+a[:75])
 return s.replace(a,b,1)
def lot_repository(s):
 start=s.index('    public JSONObject resolveBuildStructure(');end=s.index('    public JSONObject resolveDemolish(',start);part=s[start:end]
 anchor='        final int lots=def.optInt("lots",1);'
 part=once(part,anchor,anchor+'''
        if(lots>0&&!KingmakerStructureSemantics.isInfrastructure(chosenKey)){
            JSONObject requested=buildOptions.optJSONObject("lot_placement");
            JSONObject previous=resume?jsonObject(pending.optString("effects_json","{}")).optJSONObject("build_options"):null;
            JSONObject saved=previous==null?null:previous.optJSONObject("lot_placement");
            if(saved!=null){
                if(requested!=null&&(requested.optInt("urban_grid")!=saved.optInt("urban_grid")||requested.optInt("block")!=saved.optInt("block")||(requested.optJSONArray("lots")!=null&&requested.optJSONArray("lots").length()>0&&!KingmakerStructureSemantics.sameLotPlacement(saved,requested))))throw new IllegalStateException("Retome a obra nos mesmos lotes reservados.");
                requested=saved;
            }
            buildOptions.put("lot_placement",KingmakerStructureSemantics.chooseLotPlacement(town,new JSONArray(structures(townKey)),chosenKey,lots,requested,resume?pendingKey:""));
        }''')
 s=s[:start]+part+s[end:]
 anchor='effects.put("official_build_effects",buildEffects);'
 s=once(s,anchor,anchor+'''
                if(def.optInt("lots",0)>0&&!KingmakerStructureSemantics.isInfrastructure(k)){
                    JSONObject position=buildOptions.optJSONObject("lot_placement");
                    if(position==null)position=KingmakerStructureSemantics.chooseLotPlacement(town,new JSONArray(structures(settlementKey)),k,def.optInt("lots"),null,"");
                    KingmakerStructureSemantics.requireLotPlacement(town,new JSONArray(structures(settlementKey)),k,def.optInt("lots"),position,"");
                    effects.put("lot_instances",KingmakerStructureSemantics.appendedLotPlacement(existing,position));
                }''')
 # Persist island influence as well as using it in check calculations.
 start=s.index('    private void refreshSettlementOccupancy(');end=s.index('    private int influencedFarmlandCount(',start)
 part=s[start:end]
 part=once(part,'v.put("updated_at",System.currentTimeMillis());','v.put("influence",officialSettlementInfluence(town));v.put("updated_at",System.currentTimeMillis());')
 s=s[:start]+part+s[end:]
 helper='''    public void declareExistingLotPlacement(final String settlementKey,final String structureKey,final int instance,final JSONObject position)throws Exception{
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject town=settlement(settlementKey);JSONObject row=db.queryOne("SELECT * FROM kingmaker_structures WHERE world_profile=? AND settlement_key=? AND structure_key=?",new String[]{profile(),settlementKey,structureKey});
            if(town.length()==0||row.length()==0||row.optInt("lots")<1||instance<1||instance>row.optInt("count",1))throw new IllegalArgumentException("Estrutura ou exemplar inválido.");
            JSONArray all=KingmakerStructureSemantics.lotPlacements(row);JSONObject old=all.optJSONObject(instance-1);
            if(old!=null&&old.has("lots")){if(KingmakerStructureSemantics.sameLotPlacement(old,position))return;throw new IllegalStateException("A posição registrada só muda após demolir e reconstruir.");}
            JSONArray known=new JSONArray();for(JSONObject item:structures(settlementKey)){
                if(item.optInt("lots",0)<1)continue;
                JSONArray placed=KingmakerStructureSemantics.lotPlacements(item),registered=new JSONArray();for(int n=0;n<placed.length();n++){JSONObject at=placed.optJSONObject(n);if(at!=null&&at.has("lots"))registered.put(at);}
                if(registered.length()>0){JSONObject copy=new JSONObject(item.toString()),effect=jsonObject(copy.optString("effects_json","{}"));effect.put("lot_instances",registered);copy.put("count",registered.length()).put("effects_json",effect);known.put(copy);}
            }
            String original=jsonObject(row.optString("effects_json","{}")).optString("original_structure_key",structureKey);
            JSONObject chosen=KingmakerStructureSemantics.chooseLotPlacement(town,known,original,row.optInt("lots"),position,"");
            JSONArray saved=new JSONArray();for(int n=0;n<row.optInt("count",1);n++)saved.put(n==instance-1?chosen:(all.optJSONObject(n)==null?new JSONObject():all.optJSONObject(n)));
            JSONObject effect=jsonObject(row.optString("effects_json","{}"));effect.put("lot_instances",saved);
            JSONObject options=effect.optJSONObject("build_options");if(options!=null&&row.optInt("count",1)==1){options.put("lot_placement",chosen);effect.put("build_options",options);}
            ContentValues v=new ContentValues();v.put("effects_json",effect.toString());v.put("updated_at",System.currentTimeMillis());
            if(db.getWritableDatabase().update("kingmaker_structures",v,"world_profile=? AND settlement_key=? AND structure_key=?",new String[]{profile(),settlementKey,structureKey})!=1)throw new IllegalStateException("Lotes não persistidos.");
            refreshSettlementOccupancy(settlementKey);
            audit("Lotes da estrutura registrados",structureKey+" #"+instance,row,new JSONObject().put("placement",chosen));
        }});
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
def lot_ui(s):
 anchor='        urbanGrid.setInputType(InputType.TYPE_CLASS_NUMBER);'
 s=once(s,anchor,anchor+'''
        final String[] civicBlocks={"1","2","3","4","5","6","7","8","9"};final Spinner civicBlock=spinner(civicBlocks,"1");form.addView(cardWithLabel("BLOCO (ESTRUTURAS COM LOTES)",civicBlock));
        final EditText civicLots=labeledField(form,"Lotes 1 a 4 (ex.: 1,2; vazio escolhe os livres no bloco)","",false);
        form.addView(text("Blocos: 1–3 ao norte, 4–6 ao centro, 7–9 ao sul. Em cada bloco, os lotes 1 e 2 ficam ao norte; 3 e 4 ao sul. Lotes diagonais não são contíguos.",12,MUTED,false));''')
 anchor='.put("border",borderKeys[infrastructureBorder.getSelectedItemPosition()])'
 s=once(s,anchor,anchor+'.put("lot_placement",new JSONObject().put("urban_grid",num(urbanGrid,0)).put("block",Integer.parseInt(String.valueOf(civicBlock.getSelectedItem()))).put("lots",parseDiceList(value(civicLots),4,4)))')
 s=once(s,'"Registrar bordas de água"}', '"Registrar bordas de água","Organizar lotes das estruturas existentes"}')
 s=once(s,'else showSettlementWaterBordersDialog(settlement);','else if(which==2)showSettlementWaterBordersDialog(settlement);else showExistingStructureLotsDialog(settlement);')
 anchor='for(JSONObject x:structures)message.append("\\n• ").append(x.optInt("count",1)).append("× ").append(x.optString("name")).append(" • ").append(x.optInt("lots",1)).append(" lote(s)");'
 replacement='''for(JSONObject x:structures){message.append("\\n• ").append(x.optInt("count",1)).append("× ").append(x.optString("name")).append(" • ").append(x.optInt("lots",1)).append(" lote(s)");try{JSONArray positions=KingmakerStructureSemantics.lotPlacements(x);for(int n=0;n<positions.length();n++){JSONObject at=positions.optJSONObject(n);if(at!=null&&at.has("lots"))message.append("\\n   #").append(n+1).append(" — malha ").append(at.optInt("urban_grid")).append(", bloco ").append(at.optInt("block")).append(", lotes ").append(at.optJSONArray("lots"));}}catch(Exception ignored){}}'''
 s=once(s,anchor,replacement)
 helper='''    private void showExistingStructureLotsDialog(final JSONObject settlement){
        final List<JSONObject> rows=new ArrayList<JSONObject>();final List<Integer> instances=new ArrayList<Integer>();final List<String> names=new ArrayList<String>();
        for(JSONObject row:kingmakerRepository.structures(settlement.optString("settlement_key"))){if(row.optInt("lots",0)<=0)continue;try{JSONArray placed=KingmakerStructureSemantics.lotPlacements(row);for(int n=0;n<row.optInt("count",1);n++)if(placed.optJSONObject(n)==null||!placed.optJSONObject(n).has("lots")){rows.add(row);instances.add(n+1);names.add(row.optString("name")+" #"+(n+1)+" — "+row.optInt("lots")+" lote(s)");}}catch(Exception error){toast(error.getMessage());return;}}
        if(rows.isEmpty()){toast("Todas as estruturas já têm posição registrada.");return;}
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(12),0,dp(12),dp(16));final Spinner building=spinner(names.toArray(new String[0]),names.get(0));form.addView(cardWithLabel("ESTRUTURA",building));final EditText grid=labeledField(form,"Malha urbana nº","1",false),block=labeledField(form,"Bloco 1 a 9","1",false),lots=labeledField(form,"Lotes contíguos 1 a 4 (vazio escolhe os livres)","",false);
        form.addView(text("Este registro não cobra RP nem altera as estruturas antigas. Ele salva sua posição pela primeira vez. Os lotes devem respeitar a água e as restrições do bloco; depois de registrados, não podem ser movidos gratuitamente.",12,MUTED,false));
        new AlertDialog.Builder(this).setTitle("Organizar lotes").setView(scrollableDialogView(form)).setNegativeButton("Cancelar",null).setPositiveButton("REGISTRAR",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{int selected=building.getSelectedItemPosition();JSONObject position=new JSONObject().put("urban_grid",num(grid,0)).put("block",num(block,0)).put("lots",parseDiceList(value(lots),4,4));kingmakerRepository.declareExistingLotPlacement(settlement.optString("settlement_key"),rows.get(selected).optString("structure_key"),instances.get(selected),position);showExistingStructureLotsDialog(settlement);}catch(Exception error){toast(error.getMessage());}}}).show();
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
