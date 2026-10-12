package com.braseiro.pathfinder2e;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/**
 * Entrada descartável para imagens compartilhadas por Gemini e outros apps.
 *
 * Esta Activity nunca vira a tela principal nem cria um segundo documento.
 * Enquanto a permissão temporária ainda é válida, copia e valida a imagem fora
 * da thread principal; depois entrega apenas um token durável à tarefa original.
 */
public final class ShareReceiverActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean importRunning;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        importAndForward(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        importAndForward(intent);
    }

    private synchronized void importAndForward(Intent incoming) {
        if (importRunning) return;
        if (incoming == null || !Intent.ACTION_SEND.equals(incoming.getAction())) {
            finishReceiver();
            return;
        }
        String type = incoming.getType();
        if (type == null || !type.startsWith("image/")) {
            launchMain("", "O item compartilhado não é uma imagem compatível.");
            return;
        }
        final Uri source = sourceFrom(incoming);
        if (source == null) {
            launchMain("", "O aplicativo de origem não forneceu a imagem.");
            return;
        }
        importRunning = true;
        AppExecutors.io().execute(new Runnable() {
            @Override public void run() {
                String token = "";
                String error = "";
                try {
                    Uri durable = ImageStorageManager.importImage(
                            ShareReceiverActivity.this, source,
                            "scene_images", "shared_scene", 12);
                    token = PendingImageImportStore.enqueue(ShareReceiverActivity.this, durable);
                } catch (Exception failure) {
                    AppLog.e("ImageImport", "A imagem compartilhada foi rejeitada.", failure);
                    error = failure.getMessage() == null ? "A imagem recebida é inválida." : failure.getMessage();
                }
                final String readyToken = token;
                final String readyError = error;
                ui.post(new Runnable() {
                    @Override public void run() { launchMain(readyToken, readyError); }
                });
            }
        });
    }

    private Uri sourceFrom(Intent incoming) {
        Uri source = null;
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            source = incoming.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class);
        } else {
            source = (Uri) incoming.getParcelableExtra(Intent.EXTRA_STREAM);
        }
        if (source == null && incoming.getClipData() != null && incoming.getClipData().getItemCount() > 0) {
            source = incoming.getClipData().getItemAt(0).getUri();
        }
        return source;
    }

    private void launchMain(String token, String error) {
        try {
            Intent target = new Intent(this, MainActivity.class)
                    .setAction(PendingImageImportStore.ACTION_CONSUME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (token != null && token.length() > 0) {
                target.putExtra(PendingImageImportStore.EXTRA_TOKEN, token);
            }
            if (error != null && error.length() > 0) {
                target.putExtra(PendingImageImportStore.EXTRA_ERROR, error);
            }
            startActivity(target);
        } finally {
            finishReceiver();
        }
    }

    private void finishReceiver() {
        importRunning = false;
        finish();
    }
}
