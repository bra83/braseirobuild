package com.braseiro.dnd5e;

import android.content.ContentValues;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Motor para espalhar autonomamente NPCs ativos pelo mundo vivo (assentamentos,
 * tavernas, portos, torres peculiares, florestas e hexágonos) conforme sua ecologia e ofício.
 * Permite ao Mestre ativar, selecionar e remover NPCs do mundo vivo.
 */
public final class NpcWorldSpreaderEngine {
    private final AppDb db;

    public NpcWorldSpreaderEngine(AppDb database) {
        this.db = database;
    }

    private String profile() {
        return db.getActiveMapProfile();
    }

    /**
     * Alterna a participação de um NPC no mundo vivo (ativo/inativo).
     */
    public void setNpcActive(long npcId, boolean active) {
        db.setNpcActive(npcId, active);
        try {
            JSONObject npc = db.getNpcMemory(npcId);
            String name = npc.optString("name", "NPC");
            db.addAuditEvent("mundo_vivo",
                    active ? "NPC ativado no mundo vivo" : "NPC removido do mundo vivo",
                    name,
                    "active: " + (active ? 1 : 0),
                    npc.toString(),
                    true);
        } catch (Exception ignored) {}
    }

    /**
     * Remove permanentemente um NPC da base de memórias.
     */
    public void deleteNpc(long npcId) {
        try {
            JSONObject npc = db.getNpcMemory(npcId);
            String name = npc.optString("name", "NPC");
            db.deleteNpcMemory(npcId);
            db.addAuditEvent("mundo_vivo", "NPC excluído permanentemente", name, "", "", true);
        } catch (Exception ignored) {}
    }

    /**
     * Lista todos os NPCs com seu status no mundo vivo.
     */
    public JSONArray listWorldNpcs(int limit) {
        return db.listNpcMemories(limit);
    }

    /**
     * Distribui autonomamente todos os NPCs ativos pelo mundo vivo.
     */
    public JSONObject spreadActiveNpcs() {
        JSONObject report = new JSONObject();
        try {
            JSONArray npcs = db.listNpcMemories(5000);
            int activeCount = 0;
            int placedCount = 0;
            JSONArray placements = new JSONArray();

            List<LocationSlot> slots = buildAvailableSlots();

            for (int i = 0; i < npcs.length(); i++) {
                JSONObject npc = npcs.optJSONObject(i);
                if (npc == null || npc.optInt("active", 1) == 0) continue;
                activeCount++;

                LocationSlot target = matchSlot(npc, slots, i);
                long id = npc.optLong("id");
                String name = npc.optString("name");

                ContentValues v = new ContentValues();
                v.put("last_seen", target.name);
                v.put("hex_id", target.hexId);
                v.put("updated_at", System.currentTimeMillis());

                db.getWritableDatabase().update("npc_memory_profiles", v, "id=? AND world_profile=?",
                        new String[]{String.valueOf(id), profile()});

                placedCount++;
                placements.put(new JSONObject()
                        .put("npc_id", id)
                        .put("name", name)
                        .put("location", target.name)
                        .put("hex_id", target.hexId)
                        .put("environment", target.type));
            }

            report.put("success", true);
            report.put("active_npcs", activeCount);
            report.put("placed_npcs", placedCount);
            report.put("placements", placements);
            report.put("message", placedCount + " NPCs do mundo vivo foram distribuídos estrategicamente por assentamentos, portos, tavernas e masmorras.");

            db.addAuditEvent("mundo_vivo", "Distribuição autônoma de NPCs concluída",
                    "Total: " + placedCount, "", report.toString(), true);
        } catch (Exception e) {
            try {
                report.put("success", false);
                report.put("error", String.valueOf(e.getMessage()));
            } catch (Exception ignored) {}
        }
        return report;
    }

    private static final class LocationSlot {
        final String name;
        final String hexId;
        final String type; // port, tavern, tower, city, dungeon, wilderness

        LocationSlot(String name, String hexId, String type) {
            this.name = name;
            this.hexId = hexId;
            this.type = type;
        }
    }

    private List<LocationSlot> buildAvailableSlots() {
        List<LocationSlot> list = new ArrayList<LocationSlot>();
        try {
            // Tenta consultar assentamentos já registrados na campanha
            List<JSONObject> settlements = db.queryList("SELECT name, hex_id FROM campaign_settlements WHERE world_profile=? LIMIT 50",
                    new String[]{profile()});
            for (JSONObject s : settlements) {
                String name = s.optString("name", "");
                String hex = s.optString("hex_id", "H03-05");
                if (name.length() > 0) {
                    list.add(new LocationSlot(name, hex, "city"));
                    list.add(new LocationSlot("Taberna de " + name, hex, "tavern"));
                }
            }
        } catch (Exception ignored) {}

        // Slots canônicos garantidos dos suplementos
        list.add(new LocationSlot("Porto Livre de Tortuga", "H01-02", "port"));
        list.add(new LocationSlot("Cais dos Corsários (South Sea)", "H01-03", "port"));
        list.add(new LocationSlot("Taberna do Bode Embriagado", "H04-06", "tavern"));
        list.add(new LocationSlot("Bordel da Rosa Escarlate", "H04-06", "tavern"));
        list.add(new LocationSlot("Torre do Vidro Cantante", "H02-08", "tower"));
        list.add(new LocationSlot("Biblioteca Sussurrante", "H03-09", "tower"));
        list.add(new LocationSlot("Bosque dos Lamentos (Evernura)", "H05-11", "wilderness"));
        list.add(new LocationSlot("Vilarejo de Val-Frio (Evernura)", "H05-12", "wilderness"));
        list.add(new LocationSlot("Masmorras da Fortaleza Esquecida", "H06-04", "dungeon"));
        list.add(new LocationSlot("Mercado Central da Cidade Alta", "H04-05", "city"));
        list.add(new LocationSlot("Bairro dos Ferreiros e Armeiros", "H04-05", "city"));
        list.add(new LocationSlot("Quartel da Guarda Municipal", "H04-04", "city"));
        list.add(new LocationSlot("Vila Costeira dos Pescadores", "H02-02", "port"));

        return list;
    }

    private LocationSlot matchSlot(JSONObject npc, List<LocationSlot> slots, int seed) {
        String summary = (npc.optString("summary") + " " + npc.optString("visual_description")).toLowerCase(Locale.ROOT);
        String targetType = "city";

        if (summary.contains("marinheiro") || summary.contains("corsário") || summary.contains("corsario")
                || summary.contains("pescador") || summary.contains("navio") || summary.contains("barco") || summary.contains("mar")) {
            targetType = "port";
        } else if (summary.contains("mago") || summary.contains("alquimista") || summary.contains("erudito")
                || summary.contains("torre") || summary.contains("arcano") || summary.contains("pesquisa")) {
            targetType = "tower";
        } else if (summary.contains("estalajadeiro") || summary.contains("taverneiro") || summary.contains("cortesã")
                || summary.contains("cortesa") || summary.contains("bebida") || summary.contains("jogo") || summary.contains("ladino")) {
            targetType = "tavern";
        } else if (summary.contains("caçador") || summary.contains("cacador") || summary.contains("druida")
                || summary.contains("rastreador") || summary.contains("evernura") || summary.contains("floresta") || summary.contains("selva")) {
            targetType = "wilderness";
        } else if (summary.contains("torturador") || summary.contains("prisioneiro") || summary.contains("masmorra")
                || summary.contains("abissal") || summary.contains("cultista")) {
            targetType = "dungeon";
        }

        List<LocationSlot> matching = new ArrayList<LocationSlot>();
        for (LocationSlot s : slots) {
            if (s.type.equalsIgnoreCase(targetType)) {
                matching.add(s);
            }
        }

        if (matching.isEmpty()) {
            return slots.get(Math.abs(seed) % slots.size());
        }
        return matching.get(Math.abs(seed) % matching.size());
    }
}
