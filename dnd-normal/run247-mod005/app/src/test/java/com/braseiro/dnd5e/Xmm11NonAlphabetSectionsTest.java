package com.braseiro.dnd5e;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Eleven distinct source-bound 2025 Monster Manual editorial rule/index sections. */
public final class Xmm11NonAlphabetSectionsTest {
 private JSONObject sec(String chapter,String section) throws Exception {
    JSONObject result=Xmm2025BookIndex.section(chapter,section);
    assertTrue(chapter+":"+section,result.getBoolean("resolved"));
    assertFalse(result.getBoolean("auto_mutation_allowed"));
    assertFalse(result.getBoolean("auto_rolls"));
    return result;
 }
 @Test public void testXmm_sec_0_0()throws Exception {assertTrue(sec("ch_0","sec_0_0").getJSONArray("rules").length()>=4);}
 @Test public void testXmm_sec_0_1()throws Exception {assertTrue(sec("ch_0","sec_0_1").getJSONArray("fields").length()>8);}
 @Test public void testXmm_sec_0_2()throws Exception {assertTrue(sec("ch_0","sec_0_2").getJSONArray("fields").length()>=5);}
 @Test public void testXmm_sec_0_3()throws Exception {
    JSONArray parts=sec("ch_0","sec_0_3").getJSONArray("parts");
    // 2025 XMM pinned book JSON has 23 named subsections here, including
    // "Running a Monster" and "Limited Usage"; never truncate the last item.
    assertEquals("All 23 source subheadings of Parts of a Stat Block",23,parts.length());
    assertEquals("Size",parts.getString(0));
    assertEquals("Running a Monster",parts.getString(13));
    assertEquals("Limited Usage",parts.getString(22));
 }
 @Test public void testXmm_sec_2_0()throws Exception {
    JSONArray names=sec("ch_2","sec_2_0").getJSONArray("names");
    assertEquals(96,names.length());
    for(int i=0;i<names.length();i++)assertTrue(Xmm2025SourceIndex.get(names.getString(i)).length()>0);
 }
 @Test public void testXmm_sec_3_0()throws Exception {
    assertEquals(59,sec("ch_3","sec_3_0").getJSONArray("conversion_rows").length());
    assertEquals("Aarakocra Skirmisher",Xmm2025BookIndex.conversion("Aarakocra")
      .getJSONArray("equivalent2025").getString(0));
    assertFalse(Xmm2025BookIndex.conversion("unknown creature").getBoolean("resolved"));
 }
 @Test public void testXmm_sec_3_1()throws Exception {
    assertEquals(12,sec("ch_3","sec_3_1").getJSONObject("groups").length());
    assertTrue(Xmm2025BookIndex.indexedMonsters("habitat","Forest Monsters").getInt("count")>0);
 }
 @Test public void testXmm_sec_3_2()throws Exception {
    assertEquals(14,sec("ch_3","sec_3_2").getJSONObject("groups").length());
    assertTrue(Xmm2025BookIndex.indexedMonsters("type","Dragon").getInt("count")>0);
 }
 @Test public void testXmm_sec_3_3()throws Exception {
    assertEquals(12,sec("ch_3","sec_3_3").getJSONObject("groups").length());
    assertTrue(Xmm2025BookIndex.indexedMonsters("group","Angels").getInt("count")>0);
 }
 @Test public void testXmm_sec_3_4()throws Exception {
    assertEquals(30,sec("ch_3","sec_3_4").getJSONObject("groups").length());
    assertTrue(Xmm2025BookIndex.indexedMonsters("cr","CR 0").getInt("count")>0);
 }
 @Test public void testXmm_sec_4_0()throws Exception {
    assertTrue(sec("ch_4","sec_4_0").getBoolean("editorial_only"));
 }
 @Test public void androidGatewayExposesBookListsAndConversions()throws Exception {
    OfficialBookRuntimeGateway gw=new OfficialBookRuntimeGateway(null);
    JSONObject record=gw.execute("xmm","book_section",new JSONObject()
       .put("chapter_id","ch_3").put("section_id","sec_3_0"));
    assertTrue(record.getBoolean("executed"));
    JSONObject conversion=gw.execute("xmm","convert_2014",new JSONObject().put("name","Aarakocra"));
    assertTrue(conversion.getBoolean("executed"));
    assertFalse(conversion.getBoolean("auto_mutation_allowed"));
 }
}
