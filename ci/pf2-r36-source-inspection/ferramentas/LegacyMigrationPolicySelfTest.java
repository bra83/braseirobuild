package com.braseiro.pathfinder2e;

import org.json.JSONObject;

public final class LegacyMigrationPolicySelfTest {
    private static int checks=0;
    public static void main(String[] args)throws Exception{
        JSONObject sheet=new JSONObject()
                .put("species","Humano")
                .put("class_name","Feiticeiro — Linhagem Dracônica")
                .put("background","Forasteiro")
                .put("concept","Golias • Feiticeiro — Linhagem golias • Forasteiro — uma promessa antiga.")
                .put("appearance","Golias, estatura muito alta, olhos atentos; veste equipamento coerente com a classe Feiticeiro — Linhagem humana e mantém a mesma aparência entre cenas.")
                .put("visual_description","Golias, estatura muito alta, olhos atentos; veste equipamento coerente com a classe Feiticeiro — Linhagem humana e mantém a mesma aparência entre cenas.")
                .put("history","Cresceu numa comunidade coerente com sua identidade golias e trabalhou como forasteiro antes de seguir o caminho de Feiticeiro — Linhagem humana. Um acontecimento mal resolvido levou essa pessoa a partir.")
                .put("features","Traços de espécie: Golias — visão e deslocamento.\nSubclasse de nível 1: Linhagem humana.");
        check(CharacterIdentityCoherence.repairSheet(sheet,"Humano","Feiticeiro — Linhagem Dracônica","Forasteiro","Golias"),"incoerência antiga detectada");
        check(sheet.optString("appearance").startsWith("Humano,"),"aparência usa a espécie da tabela");
        check(sheet.optString("appearance").contains("classe Feiticeiro — Linhagem Dracônica"),"aparência usa a classe da tabela");
        check(sheet.optString("concept").startsWith("Humano • Feiticeiro — Linhagem Dracônica"),"conceito não mistura espécie ou linhagem");
        check(sheet.optString("history").contains("identidade humana")&&!sheet.optString("history").contains("Linhagem humana"),"história mantém identidade e subclasse coerentes");
        check(sheet.optString("features").contains("Traços de espécie: Humano")&&sheet.optString("features").contains("Linhagem Dracônica"),"traços gerados foram reparados");

        JSONObject custom=new JSONObject().put("species","Humano").put("class_name","Guerreiro").put("history","Foi criado por uma família de golias e preserva essa amizade.");
        CharacterIdentityCoherence.repairSheet(custom,"Humano","Guerreiro","Soldado","Golias");
        check(custom.optString("history").contains("família de golias"),"texto autoral sem marcador gerado é preservado");

        String a=LegacyTransactionIdentity.key("dnd","Consequências da cena","cena","{\"narration\":\"A\"}",1000L);
        String b=LegacyTransactionIdentity.key("dnd","Consequências da cena","cena","{\"narration\":\"A\"}",1000L);
        String c=LegacyTransactionIdentity.key("dnd","Consequências da cena","cena","{\"narration\":\"B\"}",1001L);
        check(a.equals(b)&&a.startsWith("legacy-transaction:"),"effect_key legado é estável");
        check(!a.equals(c),"transações diferentes não compartilham recibo");
        JSONObject portable=new JSONObject().put("world_profile","dnd").put("title","Consequências da cena").put("summary","cena")
                .put("payload_json","{\"character_id\":1}").put("created_at",2000L);
        String beforeRemap=LegacyTransactionIdentity.keyForBackupRow(portable);
        portable.put("payload_json","{\"character_id\":987}");
        String afterRemap=LegacyTransactionIdentity.keyForBackupRow(portable);
        check(!beforeRemap.equals(afterRemap),"a identidade muda se for calculada indevidamente depois do remapeamento");
        JSONObject legacyEvent=new JSONObject().put("id",17).put("world_profile","dnd").put("title","A ponte cai")
                .put("description","O rio interrompe a estrada.").put("due_day",4).put("consequence_json","{\"map_updates\":[]}").put("created_at",3000L);
        String eventKey=LegacyWorldEventIdentity.keyForBackupRow(legacyEvent);
        check(eventKey.startsWith("legacy-event:")&&eventKey.equals(LegacyWorldEventIdentity.keyForBackupRow(legacyEvent)),"event_key legado é estável");
        check(LegacyWorldEventIdentity.transactionKey(eventKey).equals("world_event:"+eventKey),"evento e transação compartilham a mesma identidade");
        System.out.println("OK: "+checks+" verificações de migração real 4.4.2.");
    }
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
}
