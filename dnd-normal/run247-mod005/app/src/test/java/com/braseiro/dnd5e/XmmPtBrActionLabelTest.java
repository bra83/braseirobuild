package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Source identifiers stay in English internally while action labels shown to players use PT-BR. */
public final class XmmPtBrActionLabelTest {
    @Test public void chargeAndManualRiderActionNamesAreShownInBrazilianPortuguese(){
        String[][] names={
            {"Claws","Garras"},{"Flame Whip","Chicote de Chamas"},
            {"Serpentine Gaze","Olhar Serpentino"},
            {"Beard","Barba"},{"Infernal Sting","Ferrão Infernal"},
            {"Dread Scythe","Foice do Pavor"},
            {"Morningstar","Maça-estrela"},{"Light Hammer","Martelo Leve"},
            {"Attach","Aderir"},{"Crush","Esmagar"},{"Chaos Blade","Lâmina do Caos"},
            {"Gore","Chifrada"},{"Sacred Weapon","Arma Sagrada"},{"Claw","Garra"},
            {"Swallow","Engolir"},{"Psychic Warp","Distorção Psíquica"},
            {"Brutal Gore","Chifrada Brutal"},{"Chaos Claw","Garra do Caos"},
            {"Chaos Staff","Cajado do Caos"},{"Ravage","Devastar"},{"Hooves","Cascos"},
            {"Faerie Dust","Pó Feérico"},{"Reel","Puxar"},{"Tentacle","Tentáculo"},
            {"Umbral Dagger","Adaga Umbral"},{"Ram","Investida"},
            {"Avalanche Slam","Golpe da Avalanche"},{"Talons","Garras"},
            {"Tusk (Boar or Hybrid Form Only)","Presa (Apenas Forma de Javali ou Híbrida)"}
        };
        for(String[] row:names)
            assertEquals(row[0],row[1],DndPtBrLocalizer.localizeText(row[0]));
    }

    @Test public void everyCompiledXmmActionLabelHasBrazilianPortugueseText(){
        JSONObject installed=Xmm2025CompiledBestiaryEngine.install();
        assertEquals(503,installed.optInt("statblocks_installed"));
        JSONArray ids=Xmm2025CompiledBestiaryEngine.catalog().optJSONArray("names");
        assertNotNull(ids);
        for(int i=0;i<ids.length();i++){
            String id=ids.optString(i);
            JSONObject block=CompiledStatblockEngine.get(id);
            for(String field:new String[]{"actions","special_actions"}){
                JSONArray actions=block.optJSONArray(field);
                if(actions==null)continue;
                for(int j=0;j<actions.length();j++){
                    JSONObject action=actions.optJSONObject(j);
                    if(action==null)continue;
                    String source=action.optString("name","").trim();
                    if(source.isEmpty())continue;
                    String localized=DndPtBrLocalizer.localizeText(source);
                    assertTrue(id+" / "+source+" is missing its PT-BR XMM label",
                        !source.equals(localized)||XmmPtBrActionLabelCatalog.all().containsKey(source));
                    if(!"Gythka".equals(source)&&!"Chatkcha".equals(source))
                        assertNotEquals(id+" / "+source,source,localized);
                }
            }
        }
    }
}
