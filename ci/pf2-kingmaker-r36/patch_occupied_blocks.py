"""Use real built districts for housing and actual water-adjacent mills."""
def once(s,a,b):
 if s.count(a)!=1:raise ValueError('Occupancy anchor count='+str(s.count(a))+' '+a[:75])
 return s.replace(a,b,1)
def occupancy_repository(s):
 s=once(s,'KingmakerStructureSemantics.consumptionReduction(rows,jsonObject(town.optString("grid_json","{}")).optBoolean("water_mill"))','KingmakerStructureSemantics.consumptionReduction(rows,KingmakerStructureSemantics.hasWaterMill(town,rows))')
 s=once(s,'boolean waterMill=jsonObject(town.optString("grid_json","{}")).optBoolean("water_mill");','boolean waterMill=KingmakerStructureSemantics.hasWaterMill(town,rows);')
 # Keep declared block count only until the old save's placement is registered.
 s=once(s,'residential=Math.max(0,Math.min(occupied,residentialLots)),overcrowded=occupied>0&&residential<blocks?1:0;',
 'residential=Math.max(0,Math.min(occupied,residentialLots));int builtBlocks=KingmakerStructureSemantics.builtBlockCount(new JSONArray(structures(old.optString("settlement_key"))));int housingBlocks=builtBlocks<0?blocks:builtBlocks;int overcrowded=occupied>0&&residential<housingBlocks?1:0;')
 start=s.index('    private void refreshSettlementOccupancy(');end=s.index('    private int influencedFarmlandCount(',start);part=s[start:end]
 part=once(part,'v.put("overcrowded",occupied>0&&residential<blocks?1:0);','int builtBlocks=KingmakerStructureSemantics.builtBlockCount(new JSONArray(structures(settlementKey)));v.put("overcrowded",occupied>0&&residential<(builtBlocks<0?blocks:builtBlocks)?1:0);')
 return s[:start]+part+s[end:]
