package com.braseiro.dnd5e;
import org.json.*;import org.junit.*;import static org.junit.Assert.*;

/** The original PR40 scope is exactly 70 source pointers, not 70 playable monsters. */
public class Xmm70MultiattackHostPlanTest {
    private static final String[] IDS={
        "Aarakocra Aeromancer",
        "Aberrant Cultist",
        "Aboleth",
        "Adult Black Dragon",
        "Adult Blue Dragon",
        "Adult Brass Dragon",
        "Adult Bronze Dragon",
        "Adult Copper Dragon",
        "Adult Gold Dragon",
        "Adult Green Dragon",
        "Adult Red Dragon",
        "Adult Silver Dragon",
        "Ancient Black Dragon",
        "Ancient Blue Dragon",
        "Ancient Brass Dragon",
        "Ancient Bronze Dragon",
        "Ancient Copper Dragon",
        "Ancient Gold Dragon",
        "Ancient Green Dragon",
        "Ancient Red Dragon",
        "Ancient Silver Dragon",
        "Arcanaloth",
        "Blob of Annihilation",
        "Bone Naga",
        "Bullywug Bog Sage",
        "Chimera",
        "Cloud Giant",
        "Doppelganger",
        "Dracolich",
        "Dryad",
        "Erinyes",
        "Fomorian",
        "Ghast Gravecaller",
        "Githyanki Knight",
        "Glabrezu",
        "Gladiator",
        "Gnoll Pack Lord",
        "Goblin Hexer",
        "Guardian Naga",
        "Gulthias Blight",
        "Horned Devil",
        "Hydra",
        "Ice Devil",
        "Invisible Stalker",
        "Jackalwere",
        "Kraken",
        "Lamia",
        "Lion",
        "Mummy Lord",
        "Oni",
        "Pirate",
        "Rakshasa",
        "Roper",
        "Salamander",
        "Shambling Mound",
        "Solar",
        "Sphinx of Secrets",
        "Sphinx of Valor",
        "Succubus",
        "Wight",
        "Yochlol",
        "Young Brass Dragon",
        "Young Bronze Dragon",
        "Young Copper Dragon",
        "Young Gold Dragon",
        "Young Silver Dragon",
        "Yuan-ti Abomination",
        "Yuan-ti Malison (Type 1)",
        "Yuan-ti Malison (Type 2)",
        "Yuan-ti Malison (Type 3)"
    };
    @Test public void originalScopeHasExplicitPerVariantAdmissionOrTypedBlock() throws Exception {
        assertEquals(70,IDS.length);
        for(String id:IDS){
            JSONObject plan=Xmm2025MultiattackPlanIndex.plan(id);assertTrue(id,plan.optBoolean("resolved"));
            JSONArray variants=plan.getJSONArray("v");
            if(variants.length()==0){assertEquals(id,"complex_multiattack_requires_manual_source_resolution",
                XmmMultiattackHostPlan.declaration(id,new JSONObject()).optString("statblock_multiattack_blocked"));continue;}
            for(int v=0;v<variants.length();v++){
                JSONArray groups=variants.getJSONArray(v),choices=new JSONArray();String firstUnsupported="";int slot=0;
                for(int g=0;g<groups.length();g++){
                    JSONObject group=groups.getJSONObject(g);
                    for(int n=0;n<group.getInt("c");n++){
                        if(firstUnsupported.isEmpty()&&!"attack".equals(group.getString("k")))
                            firstUnsupported="multiattack_component_executor_not_installed_"+group.getString("k")+"_"+slot;
                        choices.put(group.getJSONArray("o").getString(0));slot++;
                    }
                }
                JSONObject request=new JSONObject().put("variant_index",v).put("selected_actions",choices);
                if(plan.has("context")){JSONObject c=plan.getJSONObject("context");request.put(c.getString("field"),v==c.getInt("variant_index"));}
                JSONObject result=XmmMultiattackHostPlan.declaration(id,request);
                if(!firstUnsupported.isEmpty())assertEquals(id+" variant "+v,firstUnsupported,result.optString("statblock_multiattack_blocked"));
                else{
                    assertFalse(id+": "+result,result.has("statblock_multiattack_blocked"));
                    assertEquals(choices.toString(),result.getJSONArray("sequence").toString());
                    assertEquals(plan.getString("ptr"),result.getString("source_pointer"));
                    assertEquals(Xmm2025MultiattackPlanIndex.SOURCE_BLOB,result.getString("source_blob_sha1"));
                }
                choices.put(0,"Invented Attack");
                assertEquals(id,"attack_choice_not_allowed",XmmMultiattackHostPlan.declaration(id,request).optString("statblock_multiattack_blocked"));
            }
        }
    }
}
