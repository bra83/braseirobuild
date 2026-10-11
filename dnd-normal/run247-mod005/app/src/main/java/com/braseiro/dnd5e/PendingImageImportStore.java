package com.braseiro.dnd5e;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Fila durável e de consumo único para imagens recebidas de outros apps.
 *
 * A URI temporária concedida pelo app de origem nunca é guardada aqui: somente
 * a cópia já validada no armazenamento privado do Braseiro entra na fila.
 */
public final class PendingImageImportStore {
    public static final String ACTION_CONSUME = "com.braseiro.dnd5e.action.CONSUME_SCENE_IMAGE";
    public static final String EXTRA_TOKEN = "pending_scene_image_token";
    public static final String EXTRA_ERROR = "pending_scene_image_error";
    private static final String PREFS = "pending_image_imports";
    private static final String KEY_QUEUE = "queue_v1";
    private static final int MAX_PENDING = 8;

    private PendingImageImportStore() {}

    public static synchronized String enqueue(Context context, Uri durableUri) throws Exception {/*BR_JSON_WRAP*/try{
        if (context == null || durableUri == null || !"file".equalsIgnoreCase(durableUri.getScheme())) {
            throw new Exception("A cópia durável da imagem não foi criada.");
        }
        JSONArray queue = read(context);
        JSONObject item = new JSONObject()
                .put("token", UUID.randomUUID().toString())
                .put("uri", durableUri.toString())
                .put("created_at", System.currentTimeMillis());
        JSONArray next = new JSONArray();
        next.put(item);
        for (int i = 0; i < queue.length() && next.length() < MAX_PENDING; i++) {
            JSONObject existing = queue.optJSONObject(i);
            if (existing != null && existing.optString("uri").length() > 0) next.put(existing);
        }
        preferences(context).edit().putString(KEY_QUEUE, next.toString()).commit();
        return item.optString("token");
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /** Retira exatamente um item. Itens ilegíveis são descartados durante a busca. */
    public static synchronized Uri claim(Context context, String requestedToken) {/*BR_JSON_WRAP*/try{
        JSONArray queue = read(context);
        JSONArray remaining = new JSONArray();
        Uri claimed = null;
        String token = requestedToken == null ? "" : requestedToken.trim();
        long cutoff = System.currentTimeMillis() - 3L * 24L * 60L * 60L * 1000L;
        for (int i = 0; i < queue.length(); i++) {
            JSONObject item = queue.optJSONObject(i);
            if (item == null || item.optLong("created_at", 0L) < cutoff) continue;
            Uri candidate = Uri.parse(item.optString("uri", ""));
            boolean readable = ImageStorageManager.isReadable(context, candidate);
            boolean selected = claimed == null && readable
                    && (token.length() == 0 || token.equals(item.optString("token", "")));
            if (selected) claimed = candidate;
            else if (readable) remaining.put(item);
        }
        preferences(context).edit().putString(KEY_QUEUE, remaining.toString()).commit();
        return claimed;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static JSONArray read(Context context) {
        try {
            return new JSONArray(preferences(context).getString(KEY_QUEUE, "[]"));
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
