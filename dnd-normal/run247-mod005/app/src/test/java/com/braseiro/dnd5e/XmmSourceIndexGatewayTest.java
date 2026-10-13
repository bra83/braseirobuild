package com.braseiro.dnd5e;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
public final class XmmSourceIndexGatewayTest {
 @Test public void indexed503SourceEntriesWithoutFalseAutomation()throws Exception {
   assertEquals(503,Xmm2025SourceIndex.size());
   assertEquals(120,Xmm2025SourceIndex.coverage().getInt("recharge_entries"));
   assertEquals(132,Xmm2025SourceIndex.coverage().getInt("spellcasting_statblocks"));
   assertFalse(Xmm2025SourceIndex.coverage().getBoolean("full_automation_certified"));
 }
 @Test public void habitatCreatureTypeAndCrFilters()throws Exception {
   assertTrue(Xmm2025SourceIndex.filter("habitat","mountain").getInt("count")>0);
   assertTrue(Xmm2025SourceIndex.filter("type","dragon").getInt("count")>0);
   assertTrue(Xmm2025SourceIndex.filter("cr","0").getInt("count")>0);
   assertFalse(Xmm2025SourceIndex.filter("invalid","monster").getBoolean("resolved"));
 }
 @Test public void gatewayExposesIndexedSourceWithoutHiddenMutation()throws Exception {
   OfficialBookRuntimeGateway gw=new OfficialBookRuntimeGateway(null);
   JSONObject q=gw.execute("xmm","source_reference",new JSONObject().put("name","Aboleth"));
   assertTrue(q.getBoolean("executed"));
   JSONObject f=gw.execute("xmm","filter",new JSONObject().put("dimension","type").put("value","undead"));
   assertTrue(f.getBoolean("executed"));
   assertTrue(f.getJSONObject("result").getInt("count")>0);
   assertFalse(f.getBoolean("auto_mutation_allowed"));
 }
}
