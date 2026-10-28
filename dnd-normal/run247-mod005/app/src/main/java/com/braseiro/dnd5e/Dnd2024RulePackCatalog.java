package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Fail-closed, data-only extension point for the PHB 2024 character generator.
 * Rule packs cannot inject Java, scripts, new mechanics, non-core editions or Mystara content.
 * Additional mechanics require a separately compiled and tested interpreter.
 */
public final class Dnd2024RulePackCatalog {
    public static final String SCHEMA = "braseiro.dnd2024.catalog.v1";
    private static final String[] CORE_BACKGROUNDS = {
        "Acólito","Andarilho","Artesão","Artista","Charlatão","Criminoso",
        "Eremita","Escriba","Fazendeiro","Guarda","Guia","Marinheiro",
        "Mercador","Nobre","Sábio","Soldado"
    };
    private static volatile Snapshot active = new Snapshot(0, CORE_BACKGROUNDS, "Catálogo interno 2024");

    private Dnd2024RulePackCatalog(){}

    static final class Snapshot {
        final int revision;
        final String[] backgrounds;
        final String label;
        Snapshot(int revision, String[] backgrounds, String label){
            this.revision=revision; this.backgrounds=backgrounds.clone(); this.label=label;
        }
    }
    static Snapshot parse(JSONObject pack, int engineCode) throws Exception {
        if(pack==null || !SCHEMA.equals(pack.optString("schema")))
            throw new SecurityException("Schema de regras 2024 incompatível");
        if(!"dnd2024".equals(pack.optString("scope")) ||
           !"phb2024-core".equals(pack.optString("pack_id")))
            throw new SecurityException("Pacote de outro sistema ou origem");
        for(java.util.Iterator<String> keys=pack.keys();keys.hasNext();){
            String k=keys.next();
            if(!Arrays.asList("schema","scope","pack_id","revision","min_engine_code","label","catalog").contains(k))
                throw new SecurityException("Campo externo não permitido: "+k);
        }
        int revision=pack.optInt("revision",0);
        int required=pack.optInt("min_engine_code",Integer.MAX_VALUE);
        if(revision<1 || required<1 || required>engineCode)
            throw new SecurityException("Versão de pacote/motor incompatível");
        JSONObject catalog=pack.optJSONObject("catalog");
        if(catalog==null||catalog.length()!=1 || !catalog.has("backgrounds"))
            throw new SecurityException("Catálogo de opções inválido");
        JSONArray names=catalog.optJSONArray("backgrounds");
        if(names==null||names.length()!=CORE_BACKGROUNDS.length)
            throw new SecurityException("Catálogo 2024 incompleto; nenhuma opção pode ser removida");
        Set<String> expected=new HashSet<>(Arrays.asList(CORE_BACKGROUNDS));
        String[] validated=new String[names.length()];
        for(int i=0;i<validated.length;i++){
            if(!(names.opt(i) instanceof String))
                throw new SecurityException("Opção sem identificador");
            String name=names.optString(i,"");
            if(!expected.remove(name))
                throw new SecurityException("Opção de antecedente desconhecida ou duplicada: "+name);
            validated[i]=name;
        }
        if(!expected.isEmpty())throw new SecurityException("Antecedentes ausentes");
        String label=pack.optString("label","").trim();
        if(label.length()>100)throw new SecurityException("Nome de pacote excessivo");
        return new Snapshot(revision,validated,label);
    }
    static synchronized void activate(Snapshot newPack){
        if(newPack==null)throw new IllegalArgumentException("Pacote nulo");
        if(newPack.revision<active.revision)throw new SecurityException("Downgrade de regras recusado");
        active=newPack;
    }
    static synchronized void resetToBuiltin(){active=new Snapshot(0,CORE_BACKGROUNDS,"Catálogo interno 2024");}
    public static int revision(){return active.revision;}
    public static String label(){return active.label;}
    public static String[] backgroundPool(){return active.backgrounds.clone();}
    public static boolean hasExternalPack(){return active.revision>0;}
}
