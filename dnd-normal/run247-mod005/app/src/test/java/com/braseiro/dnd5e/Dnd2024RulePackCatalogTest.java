package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class Dnd2024RulePackCatalogTest {
    private static final String[] BACKGROUNDS={"Acólito","Andarilho","Artesão","Artista",
        "Charlatão","Criminoso","Eremita","Escriba","Fazendeiro","Guarda","Guia",
        "Marinheiro","Mercador","Nobre","Sábio","Soldado"};
    private static final String[] CLASSES={"Bárbaro","Bardo","Bruxo","Clérigo","Druida","Feiticeiro",
        "Guerreiro","Ladino","Mago","Monge","Paladino","Patrulheiro"};
    private static final String[] SPECIES={"Aasimar","Anão","Draconato","Elfo","Gnomo","Golias",
        "Humano","Orc","Pequenino","Tiferino"};
    private static JSONObject pack()throws Exception{
        JSONArray names=new JSONArray(),classes=new JSONArray(),species=new JSONArray();
        for(String b:BACKGROUNDS)names.put(b);
        for(String c:CLASSES)classes.put(c);
        for(String sp:SPECIES)species.put(sp);
        return new JSONObject().put("schema",Dnd2024RulePackCatalog.SCHEMA)
            .put("scope","dnd2024").put("pack_id","phb2024-core")
            .put("revision",1).put("min_engine_code",202700032)
            .put("label","Pacote assinado v1")
            .put("catalog",new JSONObject().put("backgrounds",names).put("classes",classes).put("species",species));
    }
    private static void invalid(JSONObject pack){
        try{Dnd2024RulePackCatalog.parse(pack,202700032);fail("invalid content accepted");}
        catch(SecurityException expected){}catch(Exception e){throw new AssertionError(e);}
    }
    @Test public void validatesCompleteCoreCatalogAndDoesNotPruneOptions()throws Exception{
        Dnd2024RulePackCatalog.Snapshot x=Dnd2024RulePackCatalog.parse(pack(),202700032);
        assertEquals(1,x.revision);assertEquals(16,x.backgrounds.length);
        assertEquals(12,x.classes.length);assertEquals(10,x.species.length);
        Dnd2024RulePackCatalog.resetToBuiltin();
        Dnd2024RulePackCatalog.activate(x);
        assertEquals(16,Dnd2024RulePackCatalog.backgroundPool().length);
        assertEquals(12,Dnd2024RulePackCatalog.classPool().length);
        assertEquals(10,Dnd2024RulePackCatalog.speciesPool().length);
        assertEquals(1,Dnd2024RulePackCatalog.revision());
        Dnd2024RulePackCatalog.backgroundPool()[0]="destructive mutation";
        assertEquals("Acólito",Dnd2024RulePackCatalog.backgroundPool()[0]);
        Dnd2024RulePackCatalog.resetToBuiltin();
    }
    @Test public void rejectsRemovalUnknownDuplicatesAndWrongScope()throws Exception{
        JSONObject missing=pack();missing.getJSONObject("catalog").getJSONArray("backgrounds").remove(0);
        invalid(missing);
        JSONObject unknown=pack();unknown.getJSONObject("catalog").getJSONArray("backgrounds").put(0,"Antedecente imaginário");
        invalid(unknown);
        JSONObject duplicate=pack();duplicate.getJSONObject("catalog").getJSONArray("backgrounds").put(0,"Soldado");
        invalid(duplicate);
        JSONObject missingClass=pack();
        missingClass.getJSONObject("catalog").getJSONArray("classes").remove(0);
        invalid(missingClass);
        JSONObject wrongSpecies=pack();
        wrongSpecies.getJSONObject("catalog").getJSONArray("species").put(0,"Meio-Elfo");
        invalid(wrongSpecies);
        JSONObject wrong=pack().put("scope","mystara");invalid(wrong);
        invalid(pack().put("scope","dnd2014"));
        invalid(pack().put("scripts",new JSONArray()));
        invalid(pack().put("min_engine_code",202700033));
    }
    @Test public void rejectsUnsignedOrModifiedPayloads()throws Exception{
        KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keys=generator.generateKeyPair();
        byte[] original=pack().toString().getBytes(StandardCharsets.UTF_8);
        java.security.Signature signer=java.security.Signature.getInstance("SHA256withRSA");
        signer.initSign(keys.getPrivate());signer.update(original);
        byte[] signed=signer.sign();
        assertTrue(Dnd2024RulePackManager.verifyBytes(original,signed,keys.getPublic()));
        byte[] changed=original.clone();changed[20]^=1;
        assertFalse(Dnd2024RulePackManager.verifyBytes(changed,signed,keys.getPublic()));
        KeyPair other=generator.generateKeyPair();
        assertFalse(Dnd2024RulePackManager.verifyBytes(original,signed,other.getPublic()));
        assertFalse(Dnd2024RulePackManager.verifyBytes(original,new byte[0],keys.getPublic()));
    }
}
