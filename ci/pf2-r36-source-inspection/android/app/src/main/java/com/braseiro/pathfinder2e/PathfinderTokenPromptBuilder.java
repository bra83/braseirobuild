package com.braseiro.pathfinder2e;
import org.json.JSONObject;
/** Descrição visual persistente enviada ao Gemini; nunca é autoridade mecânica. */
public final class PathfinderTokenPromptBuilder {
 private PathfinderTokenPromptBuilder(){}
 public static String build(JSONObject c,String scene){JSONObject x=c==null?new JSONObject():c;StringBuilder p=new StringBuilder();p.append("TOKEN DE PERSONAGEM PATHFINDER 2E. Preserve exatamente a identidade visual já estabelecida entre cenas. Retrato/token circular, leitura clara em VTT, sem texto, números, ficha ou símbolos de regra. ");appendIdentity(p,x);add(p,"Cena atual",scene);p.append("Não mude ancestralidade, herança, idade aparente, rosto, pele, cabelo, olhos, cicatrizes, próteses, tatuagens ou equipamento icônico já estabelecido. A imagem é somente visual; a mecânica vem do motor PF2e.");return p.toString();}
 public static String describeForScene(JSONObject c){JSONObject x=c==null?new JSONObject():c;StringBuilder p=new StringBuilder();appendIdentity(p,x);return p.toString().trim();}
 private static void appendIdentity(StringBuilder p,JSONObject c){add(p,"Nome",c.optString("name"));add(p,"Ancestralidade",c.optString("ancestry",c.optString("species")));add(p,"Herança",c.optString("heritage"));add(p,"Classe",c.optString("class_name",c.optString("class")));add(p,"Aparência persistente",c.optString("visual_description",c.optString("appearance")));add(p,"Equipamento visível",c.optString("equipment",c.optString("inventory")));add(p,"Postura e expressão",c.optString("personality",c.optString("attitude")));add(p,"Marcas persistentes",c.optString("visual_identity",c.optString("scars")));}
 private static void add(StringBuilder b,String k,String v){if(v!=null&&!v.trim().isEmpty())b.append(k).append(": ").append(v.trim()).append(". ");}
}
