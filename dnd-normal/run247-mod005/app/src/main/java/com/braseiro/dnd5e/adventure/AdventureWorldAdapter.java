package com.braseiro.dnd5e.adventure;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import com.braseiro.dnd5e.AppDb;
import com.braseiro.dnd5e.AppLog;
import com.braseiro.dnd5e.WorldStateEngine;
import org.json.JSONObject;

/**
 * Adaptador do Adventure Engine com o WorldStateEngine e LivingWorldEngine.
 * Aplica consequências no mundo (localização, relógios, narrativa de ambiente)
 * apenas após confirmação transacional da mecânica.
 */
public final class AdventureWorldAdapter {
    private final AppDb db;
    private final WorldStateEngine worldEngine;

    public AdventureWorldAdapter(AppDb database, WorldStateEngine world) {
        this.db = database;
        this.worldEngine = world;
    }

    /**
     * Aplica consequências no mundo quando um nó de aventura é ativado/transicionado.
     */
    public synchronized boolean applyNodeConsequences(AdventureNode targetNode, JSONObject resolution) {
        if (targetNode == null || db == null) return false;
        try {
            final String locationId = targetNode.getLocationId();
            if (locationId != null && locationId.length() > 0) {
                db.runInTransactionChecked(new AppDb.CheckedTransaction() {
                    @Override
                    public void run() throws Exception {
                        SQLiteDatabase sql = db.getWritableDatabase();
                        ContentValues cv = new ContentValues();
                        cv.put("location", locationId);
                        cv.put("updated_at", System.currentTimeMillis());
                        int changed = sql.update("scene_state_profiles", cv, "world_profile=?",
                                new String[]{db.getActiveMapProfile()});
                        if (changed != 1) throw new IllegalStateException(
                                "Local de aventura não persistido no perfil ativo.");
                    }
                });
            }
            if (worldEngine != null) {
                String nodeDesc = targetNode.getTitle() != null ? targetNode.getTitle() : targetNode.getId();
                JSONObject preview = worldEngine.preview("Avanco de aventura: " + nodeDesc);
                if (preview == null || preview.optString("error", "").length() > 0) {
                    AppLog.w("AdventureWorldAdapter", "Prévia de mundo indisponível na mudança de cena.", null);
                    return false;
                }
                if (!preview.optBoolean("committed", false) && !worldEngine.commit(preview)) {
                    AppLog.w("AdventureWorldAdapter", "O motor do mundo rejeitou o avanço do tempo.", null);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            AppLog.e("AdventureWorldAdapter", "Falha ao aplicar consequências no mundo para o nó: " + targetNode.getId(), e);
            return false;
        }
    }
}
