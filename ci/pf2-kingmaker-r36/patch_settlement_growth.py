"""Apply printed physical settlement growth in the construction transaction."""
def once(s,a,b):
 if s.count(a)!=1:raise ValueError('Growth anchor count='+str(s.count(a))+' '+a[:90])
 return s.replace(a,b,1)
def growth_repository(s):
 start=s.index('    public JSONObject resolveBuildStructure(');end=s.index('    public JSONObject resolveDemolish(',start);part=s[start:end]
 anchor='        final int lots=def.optInt("lots",1);'
 part=once(part,anchor,anchor+'\n        town.put("kingdom_level",s.optInt("level",1));')
 anchor='        int used=structureLotTotal(townKey)'
 part=once(part,anchor,'        JSONObject proposed=buildOptions.optJSONObject("lot_placement");\n        if(proposed!=null){JSONObject capacity=KingmakerStructureSemantics.constructionTown(town,new JSONArray(structures(townKey)),proposed);town.put("type",capacity.optString("type")).put("developed_blocks",capacity.optInt("developed_blocks"));}\n'+anchor)
 anchor='                JSONObject check=recordCheckAtSettlement(townKey,"build_structure"'
 part=once(part,anchor,'                if(buildOptions.optJSONObject("lot_placement")!=null)applyConstructionGrowth(townKey,buildOptions.optJSONObject("lot_placement"));\n'+anchor)
 s=s[:start]+part+s[end:]
 # Free structural gifts use the same growth predicates and transactional persistence.
 start=s.index('    private JSONObject buildStructure(final String settlementKey, final String structureKey, final String notes, final boolean prepaid, final JSONObject buildOptions)');end=s.index('    public JSONObject demolishStructure(',start);part=s[start:end]
 anchor='        int lotCost = def.optInt("lots", 1);'
 part=once(part,anchor,'''        town.put("kingdom_level",state().optInt("level",1));
        if(def.optInt("lots",0)>0&&!KingmakerStructureSemantics.isInfrastructure(structureKey)){
            JSONObject position=buildOptions.optJSONObject("lot_placement");
            if(position==null)position=KingmakerStructureSemantics.chooseLotPlacement(town,new JSONArray(structures(settlementKey)),structureKey,def.optInt("lots"),null,"");
            buildOptions.put("lot_placement",position);
            JSONObject capacity=KingmakerStructureSemantics.constructionTown(town,new JSONArray(structures(settlementKey)),position);
            town.put("type",capacity.optString("type")).put("developed_blocks",capacity.optInt("developed_blocks"));
        }
'''+anchor)
 anchor='                ContentValues sv = new ContentValues();'
 part=once(part,anchor,'                if(buildOptions.optJSONObject("lot_placement")!=null)applyConstructionGrowth(settlementKey,buildOptions.optJSONObject("lot_placement"));\n'+anchor)
 s=s[:start]+part+s[end:]
 anchor='String settlementType=key(type),oldType=key(old.optString("type"));'
 s=once(s,anchor,anchor+'if(!settlementType.equals(oldType))throw new IllegalStateException("O porte muda ao construir nos novos blocos, não pela edição administrativa.");')
 s=once(s,'JSONObject grid=KingmakerStructureSemantics.declareWaterBorders(town,urbanGrid,waterBorders,hasWater);','''JSONObject borderTown=new JSONObject(town.toString());
            if(urbanGrid>(Math.max(1,town.optInt("developed_blocks",1))-1)/9+1){
                borderTown.put("kingdom_level",state().optInt("level",1));JSONObject pendingGrid=jsonObject(town.optString("grid_json","{}"));JSONObject borders=pendingGrid.optJSONObject("water_borders");if(borders==null)borders=new JSONObject();if(!borders.has(String.valueOf(urbanGrid)))borders.put(String.valueOf(urbanGrid),waterBorders);pendingGrid.put("water_borders",borders);borderTown.put("grid_json",pendingGrid);
                borderTown=KingmakerStructureSemantics.constructionTown(borderTown,new JSONArray(structures(settlementKey)),new JSONObject().put("urban_grid",urbanGrid).put("block",1).put("lots",new JSONArray().put(1)));
            }
            JSONObject grid=KingmakerStructureSemantics.declareWaterBorders(borderTown,urbanGrid,waterBorders,hasWater);''')
 helper='''    private void applyConstructionGrowth(String settlementKey,JSONObject position)throws Exception{
        JSONObject before=settlement(settlementKey);before.put("kingdom_level",state().optInt("level",1));
        JSONObject after=KingmakerStructureSemantics.constructionTown(before,new JSONArray(structures(settlementKey)),position);
        if(before.optString("type").equals(after.optString("type"))&&before.optInt("developed_blocks")==after.optInt("developed_blocks"))return;
        String type=after.optString("type");ContentValues v=new ContentValues();
        v.put("type",type);v.put("developed_blocks",after.optInt("developed_blocks"));
        v.put("max_item_bonus",rules.settlementLimits(type).optInt("max_item_bonus",1));
        v.put("consumption","metropolis".equals(type)?6:"city".equals(type)?4:"town".equals(type)?2:1);
        v.put("updated_at",System.currentTimeMillis());
        if(db.getWritableDatabase().update("kingmaker_settlements",v,"world_profile=? AND settlement_key=?",new String[]{profile(),settlementKey})!=1)throw new IllegalStateException("A expansão não foi salva.");
        if(!before.optString("type").equals(type))grantSettlementMilestone(type);
        refreshSettlementOccupancy(settlementKey);audit("Expansão física do assentamento",type,before,after);
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
def growth_ui(s):
 return once(s,'Promoções são sequenciais e exigem capacidade preenchida: 4 lotes para vila; 4 blocos/8 lotes para cidade; 9 blocos/18 lotes para metrópole. Superlotação bloqueia expansão.','O porte cresce ao construir: vila cheia e Reino 3; dois lotes em cada um dos quatro blocos e Reino 9; dois lotes em cada bloco da malha e Reino 15. Superlotação bloqueia expansão. A edição não promove assentamentos.')
