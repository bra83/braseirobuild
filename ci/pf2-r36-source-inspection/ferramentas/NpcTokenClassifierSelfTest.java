package com.braseiro.pathfinder2e;
public final class NpcTokenClassifierSelfTest {
  private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
  public static void main(String[] args){
    org.json.JSONObject merchant=TokenClassifier.classify("humano_mercador_velho_barba_branca.png");
    check("Humano".equals(merchant.optString("species")),"humano não classificado");
    check("Mercador".equals(merchant.optString("role")),"mercador não classificado");
    org.json.JSONObject dwarf=TokenClassifier.classify("anao_ferreiro_barba_branca.png");
    check("Anão".equals(dwarf.optString("species")),"anão não classificado");
    check("Inventor".equals(dwarf.optString("role")),"ferreiro não classificado");
    org.json.JSONObject tiefling=TokenClassifier.classify("tiefling_bardo_chifres.png");
    check("Nephilim (visual legado)".equals(tiefling.optString("species")),"tiefling não classificado");
    check("Bardo".equals(tiefling.optString("role")),"bardo não classificado");
    System.out.println("OK: nomes reais da pasta de tokens classificados em português.");
  }
}
