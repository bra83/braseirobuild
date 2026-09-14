package com.braseiro.starwarsedge;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class GeminiBridge {
    static final int PICK_SCENE_IMAGE_REQUEST = 4191;
    static final String SCENE_IMAGE_HOST = "braseiro.local";
    static final String SCENE_IMAGE_PATH = "/scene-image/current";

    private static final String DEFAULT_IMAGE_MODEL = "gemini-3.1-flash-image";
    private static final String DEFAULT_MASTER_MODEL = "gemini-3.8-flash";
    private static final String DEFAULT_TTS_MODEL = "gemini-3.1-flash-tts-preview";
    private static final String MODEL_PREFS = "braseiro.starwars.gemini.models.v1";
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/interactions";
    private static final String KEY_ALIAS = "braseiro.starwars.gemini.api.v1";
    private static final long MAX_IMAGE_BYTES = 12L * 1024L * 1024L;
    private static final long MAX_REMOTE_BYTES = 20L * 1024L * 1024L;

    private final Activity activity;
    private final WebView webView;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final File secretFile;
    private final File imageDir;
    private final File imageFile;
    private final File imageMetaFile;
    private final File ttsAudioFile;
    private final SharedPreferences modelPreferences;
    private volatile boolean generating = false;
    private volatile boolean masterGenerating = false;
    private volatile boolean ttsGenerating = false;
    private final Object ttsPlayerLock = new Object();
    private MediaPlayer ttsPlayer = null;
    private volatile String pendingImportSceneKey = null;
    private final Object sceneImageCommitLock = new Object();
    private final AtomicLong sceneImageGeneration = new AtomicLong();

    GeminiBridge(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
        this.secretFile = new File(activity.getNoBackupFilesDir(), "gemini-api-key.enc.json");
        this.imageDir = new File(activity.getFilesDir(), "scene-image");
        if (!imageDir.exists()) imageDir.mkdirs();
        this.imageFile = new File(imageDir, "current.image");
        this.imageMetaFile = new File(imageDir, "current.json");
        this.ttsAudioFile = new File(activity.getCacheDir(), "gemini-narration.wav");
        this.modelPreferences = activity.getSharedPreferences(MODEL_PREFS, Activity.MODE_PRIVATE);
    }

    Object javascriptInterface() {
        return new Interface();
    }

    void shutdown() {
        stopTtsPlayer();
        executor.shutdownNow();
    }

    boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PICK_SCENE_IMAGE_REQUEST) return false;
        String sceneKey = pendingImportSceneKey;
        pendingImportSceneKey = null;
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            notifyEvent("cancelado", new JSONObject());
            return true;
        }
        Uri uri = data.getData();
        long imageGeneration = nextSceneImageGeneration();
        executor.submit(() -> importSceneImage(uri, sceneKey, imageGeneration));
        return true;
    }

    WebResourceResponse openSceneImageResponse(Uri uri) {
        try {
            if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || !SCENE_IMAGE_HOST.equalsIgnoreCase(uri.getHost()) || !SCENE_IMAGE_PATH.equals(uri.getPath())) return null;
            synchronized (sceneImageCommitLock) {
                if (!imageFile.exists() || !imageMetaFile.exists()) return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", null, new ByteArrayInputStream(new byte[0]));
                JSONObject meta = readJson(imageMetaFile);
                String mime = meta == null ? "image/jpeg" : meta.optString("mimeType", "image/jpeg");
                return new WebResourceResponse(mime, null, new FileInputStream(imageFile));
            }
        } catch (Exception e) {
            return new WebResourceResponse("text/plain", "UTF-8", 500, "Error", null, new ByteArrayInputStream(new byte[0]));
        }
    }

    private final class Interface {
        @JavascriptInterface
        public String getStatus() {
            try { return statusJson().toString(); }
            catch (Exception e) { return "{\"configured\":false,\"model\":\"gemini-3.1-flash-image\",\"masterModel\":\"gemini-3.8-flash\",\"ttsModel\":\"gemini-3.1-flash-tts-preview\",\"generating\":false}"; }
        }

        @JavascriptInterface
        public String configureApiKey(String apiKey) {
            try {
                String clean = String.valueOf(apiKey == null ? "" : apiKey).trim();
                if (clean.length() < 20 || clean.length() > 256 || clean.matches(".*\\s+.*")) return fail("Informe uma chave Gemini válida, sem espaços.");
                encryptAndStore(clean);
                notifyEvent("configured", new JSONObject().put("configured", true));
                return new JSONObject().put("ok", true).put("configured", true).toString();
            } catch (Exception e) { return fail("Não foi possível proteger a chave Gemini neste dispositivo."); }
        }

        @JavascriptInterface
        public String configureModels(String masterModel, String imageModel, String ttsModel) {
            try {
                String master = normalizeModel(masterModel, DEFAULT_MASTER_MODEL);
                String image = normalizeModel(imageModel, DEFAULT_IMAGE_MODEL);
                String tts = normalizeModel(ttsModel, DEFAULT_TTS_MODEL);
                if (master == null || image == null || tts == null) return fail("Nome de modelo Gemini inválido.");
                modelPreferences.edit()
                        .putString("masterModel", master)
                        .putString("imageModel", image)
                        .putString("ttsModel", tts)
                        .apply();
                notifyEvent("models_configured", new JSONObject()
                        .put("masterModel", master).put("model", image).put("ttsModel", tts));
                return new JSONObject().put("ok", true).put("masterModel", master).put("model", image).put("ttsModel", tts).toString();
            } catch (Exception e) { return fail("Não foi possível salvar os modelos Gemini."); }
        }

        @JavascriptInterface
        public String speakTts(String rawText, String rawVoice) {
            String text = String.valueOf(rawText == null ? "" : rawText).trim();
            String voice = normalizeVoice(rawVoice);
            if (text.isEmpty()) return fail("Não há narração visível para ler.");
            if (text.length() > 12000) text = text.substring(0, 12000);
            if (voice == null) return fail("Voz Gemini inválida.");
            if (ttsGenerating) return fail("Já existe uma narração Gemini em processamento.");
            try {
                String apiKey = decryptStoredKey();
                if (apiKey == null || apiKey.isBlank()) return fail("Configure a chave Gemini antes de usar Gemini TTS.");
                ttsGenerating = true;
                stopTtsPlayer();
                notifyEvent("tts_generating", new JSONObject().put("voice", voice).put("ttsModel", currentTtsModel()));
                final String finalText = text;
                executor.submit(() -> generateTtsRemote(finalText, voice));
                return new JSONObject().put("ok", true).put("status", "started").put("ttsModel", currentTtsModel()).put("voice", voice).toString();
            } catch (Exception e) {
                ttsGenerating = false;
                return fail("Não foi possível iniciar Gemini TTS.");
            }
        }

        @JavascriptInterface
        public String stopTts() {
            ttsGenerating = false;
            stopTtsPlayer();
            return "{\"ok\":true}";
        }

        @JavascriptInterface
        public String clearApiKey() {
            try {
                if (secretFile.exists() && !secretFile.delete()) throw new IllegalStateException("Falha ao remover segredo local.");
                KeyStore store = KeyStore.getInstance("AndroidKeyStore");
                store.load(null);
                if (store.containsAlias(KEY_ALIAS)) store.deleteEntry(KEY_ALIAS);
                notifyEvent("cleared", new JSONObject().put("configured", false));
                return new JSONObject().put("ok", true).put("configured", false).toString();
            } catch (Exception e) { return fail("Não foi possível remover a chave Gemini."); }
        }

        @JavascriptInterface
        public String generateSceneImage(String prompt, String sceneKey) {
            String cleanPrompt = String.valueOf(prompt == null ? "" : prompt).trim();
            String cleanSceneKey = normalizeSceneKey(sceneKey);
            if (cleanPrompt.length() < 20 || cleanPrompt.length() > 5000) return fail("Prompt de cena inválido.");
            if (cleanSceneKey == null) return fail("Identificador da cena inválido.");
            if (generating) return fail("Já existe uma geração de imagem em andamento.");
            try {
                String apiKey = decryptStoredKey();
                if (apiKey == null || apiKey.isBlank()) return fail("Configure a chave Gemini antes de gerar a imagem.");
                generating = true;
                long imageGeneration = nextSceneImageGeneration();
                notifyEvent("generating", new JSONObject().put("sceneKey", cleanSceneKey));
                executor.submit(() -> generateRemote(cleanPrompt, cleanSceneKey, imageGeneration));
                return new JSONObject().put("ok", true).put("status", "started").put("sceneKey", cleanSceneKey).toString();
            } catch (Exception e) {
                generating = false;
                return fail("Não foi possível iniciar a geração de imagem.");
            }
        }

        @JavascriptInterface
        public String requestMasterProposal(String requestId, String requestJson) {
            String cleanId = normalizeMasterRequestId(requestId);
            String cleanRequest = String.valueOf(requestJson == null ? "" : requestJson).trim();
            if (cleanId == null) return fail("Identificador da solicitação do Mestre inválido.");
            if (cleanRequest.length() < 20 || cleanRequest.length() > 1_500_000) return fail("Contexto do Mestre inválido ou excessivo.");
            if (masterGenerating) return fail("O Mestre Gemini já está processando uma ação.");
            try {
                JSONObject request = new JSONObject(cleanRequest);
                if (!request.has("context") || !request.has("instruction")) return fail("Contexto estruturado do Motor Barbara incompleto.");
                String apiKey = decryptStoredKey();
                if (apiKey == null || apiKey.isBlank()) return fail("Configure a chave Gemini antes de usar o Mestre.");
                masterGenerating = true;
                executor.submit(() -> generateMasterRemote(cleanId, request));
                return new JSONObject().put("ok", true).put("status", "started").put("requestId", cleanId).put("model", currentMasterModel()).toString();
            } catch (Exception e) {
                masterGenerating = false;
                return fail("Não foi possível iniciar o Mestre Gemini.");
            }
        }

        @JavascriptInterface
        public String pickSceneImage(String sceneKey) {
            String cleanSceneKey = normalizeSceneKey(sceneKey);
            if (cleanSceneKey == null) return fail("Identificador da cena inválido.");
            try {
                pendingImportSceneKey = cleanSceneKey;
                activity.runOnUiThread(() -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("image/*");
                        activity.startActivityForResult(intent, PICK_SCENE_IMAGE_REQUEST);
                    } catch (Exception e) {
                        pendingImportSceneKey = null;
                        notifyEvent("image_error", jsonObject("message", "Não foi possível abrir o seletor de imagem."));
                    }
                });
                return new JSONObject().put("ok", true).put("status", "picker_opened").put("sceneKey", cleanSceneKey).toString();
            } catch (Exception e) {
                pendingImportSceneKey = null;
                return fail("Não foi possível abrir o seletor de imagem.");
            }
        }

        @JavascriptInterface
        public String clearSceneImage() {
            try {
                boolean had;
                synchronized (sceneImageCommitLock) {
                    nextSceneImageGeneration();
                    had = imageFile.exists() || imageMetaFile.exists();
                    File temp = new File(imageDir, "current.tmp");
                    if (imageFile.exists() && !imageFile.delete()) throw new IllegalStateException("Falha ao remover imagem.");
                    if (imageMetaFile.exists() && !imageMetaFile.delete()) throw new IllegalStateException("Falha ao remover metadados.");
                    if (temp.exists() && !temp.delete()) throw new IllegalStateException("Falha ao remover imagem temporária.");
                    notifyEvent("image_cleared", new JSONObject().put("cleared", true).put("hadImage", had));
                }
                return new JSONObject().put("ok", true).put("cleared", true).toString();
            } catch (Exception e) { return fail("Não foi possível remover a imagem de cena."); }
        }
    }

    private JSONObject statusJson() throws Exception {
        JSONObject out = new JSONObject()
                .put("configured", hasStoredKey())
                .put("model", currentImageModel())
                .put("masterModel", currentMasterModel())
                .put("ttsModel", currentTtsModel())
                .put("generating", generating)
                .put("masterGenerating", masterGenerating)
                .put("ttsGenerating", ttsGenerating);
        synchronized (sceneImageCommitLock) {
            JSONObject meta = readJson(imageMetaFile);
            if (imageFile.exists() && meta != null) {
                out.put("imageUrl", "https://" + SCENE_IMAGE_HOST + SCENE_IMAGE_PATH + "?v=" + imageFile.lastModified());
                out.put("imageSceneKey", meta.optString("sceneKey", null));
                out.put("imageMimeType", meta.optString("mimeType", null));
                out.put("imageUpdatedAt", meta.optString("updatedAt", null));
            }
        }
        return out;
    }

    private boolean hasStoredKey() {
        if (!secretFile.exists()) return false;
        try { return decryptStoredKey() != null; }
        catch (Exception e) { return false; }
    }

    private SecretKey getOrCreateSecretKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        KeyStore.Entry entry = store.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    private void encryptAndStore(String plain) throws Exception {
        SecretKey key = getOrCreateSecretKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        JSONObject payload = new JSONObject()
                .put("v", 1)
                .put("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .put("data", Base64.encodeToString(encrypted, Base64.NO_WRAP));
        writeBytes(secretFile, payload.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String decryptStoredKey() throws Exception {
        if (!secretFile.exists()) return null;
        JSONObject payload = new JSONObject(new String(readLimited(new FileInputStream(secretFile), 4096), StandardCharsets.UTF_8));
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        KeyStore.Entry entry = store.getEntry(KEY_ALIAS, null);
        if (!(entry instanceof KeyStore.SecretKeyEntry)) return null;
        SecretKey key = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        byte[] iv = Base64.decode(payload.getString("iv"), Base64.DEFAULT);
        byte[] encrypted = Base64.decode(payload.getString("data"), Base64.DEFAULT);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private void generateRemote(String prompt, String sceneKey, long imageGeneration) {
        try {
            String apiKey = decryptStoredKey();
            if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Chave Gemini indisponível.");
            JSONObject body = new JSONObject()
                    .put("model", currentImageModel())
                    .put("input", new JSONArray().put(new JSONObject().put("type", "text").put("text", prompt)))
                    .put("response_format", new JSONObject().put("type", "image").put("aspect_ratio", "16:9").put("image_size", "1K"));

            HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("x-goog-api-key", apiKey);
            byte[] request = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(request.length);
            try (java.io.OutputStream output = connection.getOutputStream()) { output.write(request); }

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            byte[] response = readLimited(stream, MAX_REMOTE_BYTES);
            connection.disconnect();
            if (status < 200 || status >= 300) throw new IllegalStateException(remoteError(response, status));

            JSONObject parsed = new JSONObject(new String(response, StandardCharsets.UTF_8));
            ImagePayload image = findImage(parsed);
            if (image == null) throw new IllegalStateException("A resposta do Gemini não continha uma imagem válida.");
            byte[] decoded = Base64.decode(image.data, Base64.DEFAULT);
            if (decoded.length < 32 || decoded.length > MAX_IMAGE_BYTES) throw new IllegalStateException("A imagem gerada excede o limite local ou está vazia.");
            String mime = detectImageMime(decoded);
            if (mime == null) throw new IllegalStateException("Formato de imagem retornado pelo Gemini não suportado.");
            if (!persistSceneImage(decoded, mime, sceneKey, "gemini", imageGeneration, "generated")) return;
            generating = false;
        } catch (Exception e) {
            generating = false;
            if (isCurrentSceneImageGeneration(imageGeneration)) {
                notifyEvent("erro", jsonObject("message", safeMessage(e)));
            }
        } finally {
            generating = false;
        }
    }

    private void generateMasterRemote(String requestId, JSONObject motorRequest) {
        try {
            String apiKey = decryptStoredKey();
            if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Chave Gemini indisponível.");

            JSONObject schema = masterProposalSchema();
            String systemInstruction =
                    "Você é o componente narrativo do Motor Barbara para Star Wars. " +
                    "Responda exclusivamente com uma proposta JSON compatível com o contrato recebido. " +
                    "Nunca aplique regras por conta própria, nunca invente IDs existentes, nunca revele fatos marcados como ocultos, " +
                    "e cite sourceRuleIds somente se esses IDs estiverem no contexto recuperado. " +
                    "A narração deve ser cinematográfica, concisa e em português do Brasil. " +
                    "As suggestedActions, quando úteis, são apenas opções narrativas; o jogador continua livre para escrever qualquer ação.";

            JSONObject body = new JSONObject()
                    .put("model", currentMasterModel())
                    .put("system_instruction", systemInstruction)
                    .put("input", motorRequest.toString())
                    .put("response_format", new JSONObject()
                            .put("type", "text")
                            .put("mime_type", "application/json")
                            .put("schema", schema))
                    .put("generation_config", new JSONObject()
                            .put("thinking_level", "medium"));

            HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("x-goog-api-key", apiKey);
            byte[] request = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(request.length);
            try (java.io.OutputStream output = connection.getOutputStream()) { output.write(request); }

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            byte[] response = readLimited(stream, 4L * 1024L * 1024L);
            connection.disconnect();
            if (status < 200 || status >= 300) throw new IllegalStateException(remoteError(response, status));

            JSONObject parsed = new JSONObject(new String(response, StandardCharsets.UTF_8));
            String outputText = extractInteractionText(parsed);
            if (outputText == null || outputText.isBlank()) throw new IllegalStateException("O Gemini não retornou uma proposta de Mestre.");
            JSONObject proposal = new JSONObject(outputText.trim());
            notifyMasterEvent(new JSONObject()
                    .put("requestId", requestId)
                    .put("ok", true)
                    .put("model", currentMasterModel())
                    .put("proposal", proposal));
        } catch (Exception e) {
            notifyMasterEvent(jsonObject(
                    "requestId", requestId,
                    "ok", false,
                    "message", safeMessage(e)));
        } finally {
            masterGenerating = false;
        }
    }

    private String currentImageModel() { return modelPreferences.getString("imageModel", DEFAULT_IMAGE_MODEL); }

    private String currentMasterModel() { return modelPreferences.getString("masterModel", DEFAULT_MASTER_MODEL); }

    private String currentTtsModel() { return modelPreferences.getString("ttsModel", DEFAULT_TTS_MODEL); }

    private String normalizeModel(String value, String fallback) {
        String clean = String.valueOf(value == null ? "" : value).trim();
        if (clean.isEmpty()) clean = fallback;
        return clean.matches("[A-Za-z0-9._:/-]{3,120}") ? clean : null;
    }

    private String normalizeVoice(String value) {
        String clean = String.valueOf(value == null ? "Kore" : value).trim();
        if (clean.isEmpty()) clean = "Kore";
        return clean.matches("[A-Za-z0-9_-]{2,64}") ? clean : null;
    }

    private void generateTtsRemote(String text, String voice) {
        try {
            String apiKey = decryptStoredKey();
            if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Chave Gemini indisponível.");
            JSONObject body = new JSONObject()
                    .put("model", currentTtsModel())
                    .put("input", new JSONArray().put(new JSONObject().put("type", "text").put("text", text)))
                    .put("response_format", new JSONObject().put("type", "audio"))
                    .put("generation_config", new JSONObject().put("speech_config",
                            new JSONArray().put(new JSONObject().put("voice", voice))));

            HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(90000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("x-goog-api-key", apiKey);
            byte[] request = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(request.length);
            try (java.io.OutputStream output = connection.getOutputStream()) { output.write(request); }
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            byte[] response = readLimited(stream, 24L * 1024L * 1024L);
            connection.disconnect();
            if (status < 200 || status >= 300) throw new IllegalStateException(remoteError(response, status));
            JSONObject parsed = new JSONObject(new String(response, StandardCharsets.UTF_8));
            AudioPayload audio = findAudio(parsed);
            if (audio == null) throw new IllegalStateException("A resposta Gemini TTS não continha áudio.");
            byte[] decoded = Base64.decode(audio.data, Base64.DEFAULT);
            if (decoded.length < 16 || decoded.length > 20L * 1024L * 1024L) throw new IllegalStateException("Áudio Gemini inválido ou excessivo.");
            byte[] playable = decoded;
            String mime = audio.mime == null ? "" : audio.mime.toLowerCase();
            if (!isWave(decoded) && (mime.contains("l16") || mime.contains("pcm") || mime.isBlank())) {
                playable = wrapPcm16MonoAsWav(decoded, extractSampleRate(mime));
            }
            writeBytes(ttsAudioFile, playable);
            playTtsFile();
            ttsGenerating = false;
            notifyEvent("tts_ready", new JSONObject().put("voice", voice).put("ttsModel", currentTtsModel()));
        } catch (Exception e) {
            ttsGenerating = false;
            notifyEvent("tts_error", jsonObject("message", safeMessage(e)));
        }
    }

    private AudioPayload findAudio(Object node) {
        if (node instanceof JSONObject object) {
            String type = object.optString("type", "");
            String mime = object.optString("mime_type", object.optString("mimeType", ""));
            String data = object.optString("data", "");
            if (!data.isBlank() && ("audio".equalsIgnoreCase(type) || mime.toLowerCase().startsWith("audio/"))) return new AudioPayload(data, mime);
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) { AudioPayload found = findAudio(object.opt(keys.next())); if (found != null) return found; }
        } else if (node instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) { AudioPayload found = findAudio(array.opt(i)); if (found != null) return found; }
        }
        return null;
    }

    private static final class AudioPayload {
        final String data; final String mime;
        AudioPayload(String data, String mime) { this.data = data; this.mime = mime; }
    }

    private boolean isWave(byte[] bytes) {
        return bytes.length >= 12 && bytes[0]=='R' && bytes[1]=='I' && bytes[2]=='F' && bytes[3]=='F' && bytes[8]=='W' && bytes[9]=='A' && bytes[10]=='V' && bytes[11]=='E';
    }

    private int extractSampleRate(String mime) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:rate|sr)=([0-9]{4,6})").matcher(mime);
            if (m.find()) return Math.max(8000, Math.min(48000, Integer.parseInt(m.group(1))));
        } catch (Exception ignored) { }
        return 24000;
    }

    private byte[] wrapPcm16MonoAsWav(byte[] pcm, int sampleRate) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(pcm.length + 44);
        int dataLen = pcm.length, riffLen = dataLen + 36, byteRate = sampleRate * 2;
        out.write(new byte[]{'R','I','F','F'}); writeLe32(out, riffLen); out.write(new byte[]{'W','A','V','E'});
        out.write(new byte[]{'f','m','t',' '}); writeLe32(out,16); writeLe16(out,1); writeLe16(out,1);
        writeLe32(out,sampleRate); writeLe32(out,byteRate); writeLe16(out,2); writeLe16(out,16);
        out.write(new byte[]{'d','a','t','a'}); writeLe32(out,dataLen); out.write(pcm);
        return out.toByteArray();
    }

    private void writeLe16(ByteArrayOutputStream out, int v) { out.write(v & 255); out.write((v >>> 8) & 255); }
    private void writeLe32(ByteArrayOutputStream out, int v) { out.write(v & 255); out.write((v >>> 8) & 255); out.write((v >>> 16) & 255); out.write((v >>> 24) & 255); }

    private void playTtsFile() throws Exception {
        synchronized (ttsPlayerLock) {
            stopTtsPlayerLocked();
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(ttsAudioFile.getAbsolutePath());
            player.setOnCompletionListener(mp -> { synchronized (ttsPlayerLock) { if (ttsPlayer == mp) { try { mp.release(); } catch (Exception ignored) {} ttsPlayer = null; } } });
            player.prepare();
            player.start();
            ttsPlayer = player;
        }
    }

    private void stopTtsPlayer() { synchronized (ttsPlayerLock) { stopTtsPlayerLocked(); } }
    private void stopTtsPlayerLocked() {
        if (ttsPlayer != null) { try { ttsPlayer.stop(); } catch (Exception ignored) {} try { ttsPlayer.release(); } catch (Exception ignored) {} ttsPlayer = null; }
    }

    private JSONObject masterProposalSchema() throws Exception {
        JSONObject string = new JSONObject().put("type", "string");
        JSONObject integer = new JSONObject().put("type", "integer");
        JSONObject suggestedCheck = new JSONObject().put("type", "object")
                .put("properties", new JSONObject()
                        .put("skillKey", string)
                        .put("difficulty", integer)
                        .put("challenge", integer)
                        .put("boost", integer)
                        .put("setback", integer)
                        .put("reason", string));
        JSONObject properties = new JSONObject()
                .put("narration", new JSONObject().put("type", "string").put("description", "Narração em português do Brasil, até 5000 caracteres; o Motor Barbara valida o limite localmente."))
                .put("sceneTitle", new JSONObject().put("type", "string").put("description", "Título curto da cena, até 120 caracteres; o Motor Barbara valida o limite localmente."))
                .put("sceneLocationId", new JSONObject().put("type", "string").put("description", "ID de local já existente no contexto, até 160 caracteres; o Motor Barbara valida o limite localmente."))
                .put("askPlayer", new JSONObject().put("type", "string").put("description", "Pergunta ao jogador, até 500 caracteres; o Motor Barbara valida o limite localmente."))
                .put("suggestedActions", new JSONObject().put("type", "array").put("maxItems", 6).put("items", new JSONObject().put("type", "string").put("description", "Opção narrativa, até 180 caracteres; o Motor Barbara valida o limite localmente.")))
                .put("suggestedCheck", suggestedCheck)
                .put("sourceRuleIds", new JSONObject().put("type", "array").put("items", string))
                .put("acquisitionRequest", new JSONObject().put("type", "object"))
                .put("acquisitionDecision", new JSONObject().put("type", "object"))
                .put("economyOperations", new JSONObject().put("type", "array").put("maxItems", 6).put("items", new JSONObject().put("type", "object")))
                .put("worldOperations", new JSONObject().put("type", "array").put("maxItems", 12).put("items", new JSONObject().put("type", "object")))
                .put("encounterDirective", new JSONObject().put("type", "object"));
        return new JSONObject()
                .put("type", "object")
                .put("properties", properties)
                .put("required", new JSONArray()
                        .put("narration")
                        .put("askPlayer")
                        .put("suggestedActions")
                        .put("sourceRuleIds"));
    }

    private String extractInteractionText(JSONObject root) {
        String direct = root.optString("output_text", null);
        if (direct != null && !direct.isBlank()) return direct;
        return findText(root);
    }

    private String findText(Object node) {
        if (node instanceof JSONObject object) {
            String type = object.optString("type", "");
            String text = object.optString("text", "");
            if (!text.isBlank() && (type.isBlank() || "text".equalsIgnoreCase(type))) return text;
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String found = findText(object.opt(key));
                if (found != null && !found.isBlank()) return found;
            }
        } else if (node instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) {
                String found = findText(array.opt(i));
                if (found != null && !found.isBlank()) return found;
            }
        }
        return null;
    }

    private void importSceneImage(Uri uri, String sceneKey, long imageGeneration) {
        try {
            if (sceneKey == null) throw new IllegalArgumentException("Cena de destino inválida.");
            String declaredMime = activity.getContentResolver().getType(uri);
            try (InputStream input = activity.getContentResolver().openInputStream(uri)) {
                if (input == null) throw new IllegalArgumentException("Não foi possível abrir a imagem selecionada.");
                byte[] bytes = readLimited(input, MAX_IMAGE_BYTES);
                if (bytes.length < 32) throw new IllegalArgumentException("Imagem vazia ou inválida.");
                String mime = detectImageMime(bytes);
                if (mime == null) throw new IllegalArgumentException("Use uma imagem PNG, JPEG ou WebP.");
                if (declaredMime != null && !declaredMime.startsWith("image/")) throw new IllegalArgumentException("O arquivo selecionado não é uma imagem.");
                persistSceneImage(bytes, mime, sceneKey, "import", imageGeneration, "imported");
            }
        } catch (Exception e) {
            if (isCurrentSceneImageGeneration(imageGeneration)) {
                notifyEvent("erro", jsonObject("message", safeMessage(e)));
            }
        }
    }

    private boolean persistSceneImage(byte[] bytes, String mime, String sceneKey, String source, long imageGeneration, String successEvent) throws Exception {
        synchronized (sceneImageCommitLock) {
            if (!isCurrentSceneImageGeneration(imageGeneration)) return false;
            if (!imageDir.exists() && !imageDir.mkdirs()) throw new IllegalStateException("Não foi possível criar armazenamento de imagem.");
            File temp = new File(imageDir, "current.tmp");
            writeBytes(temp, bytes);
            if (!isCurrentSceneImageGeneration(imageGeneration)) {
                if (temp.exists()) temp.delete();
                return false;
            }
            if (imageFile.exists() && !imageFile.delete()) throw new IllegalStateException("Não foi possível substituir a imagem anterior.");
            if (!temp.renameTo(imageFile)) throw new IllegalStateException("Não foi possível preservar a imagem de cena.");
            JSONObject meta = new JSONObject()
                    .put("sceneKey", sceneKey)
                    .put("mimeType", mime)
                    .put("source", source)
                    .put("updatedAt", Instant.now().toString());
            writeBytes(imageMetaFile, meta.toString().getBytes(StandardCharsets.UTF_8));
            notifyEvent(successEvent, new JSONObject().put("sceneKey", sceneKey).put("mimeType", mime));
            return true;
        }
    }

    private long nextSceneImageGeneration() {
        return sceneImageGeneration.incrementAndGet();
    }

    private boolean isCurrentSceneImageGeneration(long generation) {
        return sceneImageGeneration.get() == generation;
    }

    private ImagePayload findImage(Object node) {
        if (node instanceof JSONObject object) {
            String type = object.optString("type", "");
            String mime = object.optString("mime_type", object.optString("mimeType", ""));
            String data = object.optString("data", "");
            if (!data.isBlank() && ("image".equalsIgnoreCase(type) || mime.startsWith("image/"))) return new ImagePayload(data, mime);
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object child = object.opt(key);
                ImagePayload found = findImage(child);
                if (found != null) return found;
            }
        } else if (node instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) {
                ImagePayload found = findImage(array.opt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static final class ImagePayload {
        final String data;
        final String mime;
        ImagePayload(String data, String mime) { this.data = data; this.mime = mime; }
    }

    private String detectImageMime(byte[] bytes) {
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47) return "image/png";
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
        return null;
    }

    private String normalizeMasterRequestId(String requestId) {
        String clean = String.valueOf(requestId == null ? "" : requestId).trim();
        return clean.matches("master_[A-Za-z0-9_-]{6,80}") ? clean : null;
    }

    private String normalizeSceneKey(String sceneKey) {
        String clean = String.valueOf(sceneKey == null ? "" : sceneKey).trim();
        return clean.matches("scene_[0-9a-f]{8}") ? clean : null;
    }

    private String remoteError(byte[] response, int status) {
        try {
            JSONObject parsed = new JSONObject(new String(response, StandardCharsets.UTF_8));
            JSONObject error = parsed.optJSONObject("error");
            String message = error == null ? null : error.optString("message", null);
            if (message != null && !message.isBlank()) return "Gemini recusou a geração: " + truncate(message, 240);
        } catch (Exception ignored) {}
        return "Gemini recusou a geração (HTTP " + status + ").";
    }

    private String fail(String message) {
        try { return new JSONObject().put("ok", false).put("message", message).toString(); }
        catch (Exception ignored) { return "{\"ok\":false,\"message\":\"Falha no host Gemini.\"}"; }
    }


    private static JSONObject jsonObject(Object... keyValues) {
        JSONObject object = new JSONObject();
        if (keyValues == null) return object;
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            try { object.put(String.valueOf(keyValues[i]), keyValues[i + 1]); }
            catch (Exception ignored) {}
        }
        return object;
    }

    private String safeMessage(Exception error) {
        String message = error.getMessage();
        return truncate(message == null || message.isBlank() ? "Falha no host Gemini." : message, 260);
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private void notifyMasterEvent(JSONObject payload) {
        try {
            String raw = payload == null ? new JSONObject().put("ok", false).put("message", "Resposta vazia do Mestre.").toString() : payload.toString();
            String js = "window.braseiroMasterNativeEvent&&window.braseiroMasterNativeEvent(" + JSONObject.quote(raw) + ");";
            activity.runOnUiThread(() -> webView.evaluateJavascript(js, null));
        } catch (Exception ignored) {}
    }

    private void notifyEvent(String type, JSONObject payload) {
        try {
            String raw = new JSONObject().put("type", type).put("payload", payload == null ? new JSONObject() : payload).toString();
            String js = "window.braseiroGeminiNativeEvent&&window.braseiroGeminiNativeEvent(" + JSONObject.quote(raw) + ");";
            activity.runOnUiThread(() -> webView.evaluateJavascript(js, null));
        } catch (Exception ignored) {}
    }

    private JSONObject readJson(File file) {
        if (file == null || !file.exists()) return null;
        try { return new JSONObject(new String(readLimited(new FileInputStream(file), 64 * 1024), StandardCharsets.UTF_8)); }
        catch (Exception e) { return null; }
    }

    private byte[] readLimited(InputStream input, long maxBytes) throws Exception {
        if (input == null) return new byte[0];
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[64 * 1024];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) throw new IllegalArgumentException("Arquivo/resposta excede o limite local permitido.");
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private void writeBytes(File file, byte[] bytes) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IllegalStateException("Não foi possível criar armazenamento privado.");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
            output.getFD().sync();
        }
    }
}
