package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;

/** Transparent module register: only ports actually executed by the universal engine. */
public final class UniversalGithubModules {
    private UniversalGithubModules() {}
    private static JSONObject entry(String id,String upstream,String feature,String status,String license) {
        try {
        return new JSONObject().put("id",id).put("upstream",upstream)
            .put("feature",feature).put("status",status).put("license",license);
        } catch (RuntimeException invalid) {
            throw invalid;
        } catch (Exception invalidJson) {
            throw new IllegalStateException("Falha ao estruturar catálogo de módulos",invalidJson);
        }
    }
    public static JSONArray catalog() {
        return new JSONArray()
            .put(entry("aethertable_roll_tag","IPedrax/AetherTable","Tag [[ROLL:1d20+N]] vira pedido seguro ao jogador","java_port_executed","MIT"))
            .put(entry("tome_entity","Caryyon/tome","Entidades, propriedades, recursos e revisão","java_port_executed","MIT"))
            .put(entry("loreweaver_outcome","1A7432/loreweaver","Resultado neutro sem confundir regras","java_port_executed","MIT"))
            .put(entry("year_zero_dice","Stefouch/foundry-year-zero-roller","Pool Year Zero e forçar rolagens","java_port_executed","MIT"))
            .put(entry("star_wars_dice","swrpg-online/dice","Faces narrativas SW e cancelamento","java_port_executed","MIT"));
    }
}
