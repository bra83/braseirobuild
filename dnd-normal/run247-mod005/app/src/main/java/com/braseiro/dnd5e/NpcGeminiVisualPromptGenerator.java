package com.braseiro.dnd5e;

import android.content.Context;
import android.net.Uri;
import org.json.JSONObject;
import java.io.File;

/**
 * Gerador de prompts visuais e integração direta com o Gemini para criação de tokens e retratos
 * personalizados de NPCs baseados em suas características (raça, classe social, profissão, peculiaridades).
 */
public final class NpcGeminiVisualPromptGenerator {

    public interface PortraitCallback {
        void onSuccess(Uri storedTokenUri, String promptUsed);
        void onError(String error);
    }

    private NpcGeminiVisualPromptGenerator() {}

    /**
     * Constrói um prompt refinado para geração de retrato em estilo token de D&D 5e.
     */
    public static String buildPrompt(String name, String ancestry, String socialClass,
                                     String profession, String quirks, String additionalDetails) {
        StringBuilder sb = new StringBuilder();
        sb.append("A photorealistic cinematic live-action medieval fantasy character token portrait bust of a ");

        if (ancestry != null && !ancestry.trim().isEmpty()) {
            sb.append(ancestry.trim()).append(" ");
        } else {
            sb.append("human ");
        }

        if (profession != null && !profession.trim().isEmpty()) {
            sb.append(profession.trim()).append(", ");
        } else {
            sb.append("adventurer, ");
        }

        if (socialClass != null && !socialClass.trim().isEmpty()) {
            sb.append("social standing: ").append(socialClass.trim()).append(". ");
        }

        if (quirks != null && !quirks.trim().isEmpty()) {
            sb.append("Distinctive features: ").append(quirks.trim()).append(". ");
        }

        if (additionalDetails != null && !additionalDetails.trim().isEmpty()) {
            sb.append("Details: ").append(additionalDetails.trim()).append(". ");
        }

        sb.append("Centered circular-token-compatible composition, realistic skin and hair texture, practical medieval clothing, natural photographic depth of field, cinematic lighting, clean dark background. A real actor photographed on a medieval fantasy film set. No illustration, painting, cartoon, anime, digital art or stylized rendering.");
        return sb.toString();
    }

    /**
     * Dispara a geração de imagem pelo Gemini e armazena duravelmente o token no app.
     */
    public static void generateAndStoreToken(final Context context,
                                            final GeminiService gemini,
                                            final String apiKey,
                                            final String model,
                                            final String prompt,
                                            final long npcId,
                                            final PortraitCallback callback) {
        if (gemini == null || apiKey == null || apiKey.trim().isEmpty()) {
            if (callback != null) callback.onError("Chave de API do Gemini não configurada.");
            return;
        }

        gemini.generateImage(apiKey.trim(), model, prompt, new GeminiService.FileCallback() {
            @Override
            public void onSuccess(File file, String mime) {
                try {
                    Uri stored = ImageStorageManager.importImage(context, new java.io.FileInputStream(file), mime, "npc_avatars", "npc_gemini_" + npcId, Integer.MAX_VALUE);
                    if (!ImageStorageManager.isReadable(context, stored)) throw new java.io.IOException("Retrato armazenado não pôde ser lido.");
                    if (callback != null) {
                        callback.onSuccess(stored, prompt);
                    }
                } catch (Exception e) {
                    if (callback != null) callback.onError("Falha ao persistir imagem gerada: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }
}
