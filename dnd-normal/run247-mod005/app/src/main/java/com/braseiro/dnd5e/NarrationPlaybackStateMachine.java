package com.braseiro.dnd5e;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Máquina de estados explícita para o pipeline de reprodução de narração (Charon / Kokoro / Android).
 * Rastreia rigorosamente o ciclo de vida de cada bloco para evitar cortes prematuros,
 * trocas silenciosas de motor e repetição/salto de trechos durante fallback.
 */
public final class NarrationPlaybackStateMachine {

    public enum State {
        IDLE,
        PREPARING,
        SYNTHESIZING,
        CHUNK_READY,
        PLAYING,
        COMPLETED,
        FALLBACK_PENDING,
        FALLBACK_PLAYING,
        ERROR,
        CANCELLED
    }

    public enum VoiceSource {
        NONE("Nenhuma"),
        CHARON("Gemini Charon"),
        KOKORO("Braseiro Kokoro PT-BR"),
        ANDROID_TTS("Voz do Android");

        private final String label;
        VoiceSource(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final Object lock = new Object();

    private long sessionId;
    private long generation;
    private State state = State.IDLE;
    private VoiceSource activeVoiceSource = VoiceSource.NONE;
    private boolean manualCharonSelection = false;

    private List<String> chunks = Collections.emptyList();
    private int totalChunks = 0;

    private int requestedIndex = -1;
    private int synthesizedIndex = -1;
    private int startedPlaybackIndex = -1;
    private int completedPlaybackIndex = -1;
    private int lastSpokenCharPosition = 0;

    private String failureReason = "";
    private int httpErrorCode = 0;
    private boolean isQuotaError = false;

    public interface Listener {
        void onStateChanged(State oldState, State newState, NarrationPlaybackStateMachine machine);
        void onVoiceSourceChanged(VoiceSource oldSource, VoiceSource newSource);
        void onPlaybackProgress(int chunkIndex, int totalChunks, int spokenChars);
    }

    private Listener listener;

    public NarrationPlaybackStateMachine() {}

    public void setListener(Listener listener) {
        synchronized (lock) {
            this.listener = listener;
        }
    }

    public void startSession(long sessionId, long generation, List<String> textChunks, boolean manualCharon, VoiceSource initialSource) {
        State oldState;
        VoiceSource oldSource;
        synchronized (lock) {
            oldState = this.state;
            oldSource = this.activeVoiceSource;
            this.sessionId = sessionId;
            this.generation = generation;
            this.chunks = textChunks == null ? Collections.<String>emptyList() : new ArrayList<>(textChunks);
            this.totalChunks = this.chunks.size();
            this.manualCharonSelection = manualCharon;
            this.activeVoiceSource = initialSource == null ? VoiceSource.CHARON : initialSource;
            this.requestedIndex = -1;
            this.synthesizedIndex = -1;
            this.startedPlaybackIndex = -1;
            this.completedPlaybackIndex = -1;
            this.lastSpokenCharPosition = 0;
            this.failureReason = "";
            this.httpErrorCode = 0;
            this.isQuotaError = false;
            this.state = State.PREPARING;
        }
        notifyStateChange(oldState, State.PREPARING);
        if (oldSource != initialSource) {
            notifySourceChange(oldSource, initialSource);
        }
    }

    public void onSynthesisRequested(int index, long sessionGen) {
        State oldState;
        synchronized (lock) {
            if (sessionGen != this.generation || isTerminal()) return;
            this.requestedIndex = Math.max(this.requestedIndex, index);
            oldState = this.state;
            this.state = State.SYNTHESIZING;
        }
        notifyStateChange(oldState, State.SYNTHESIZING);
    }

    public void onChunkSynthesized(int index, long sessionGen) {
        State oldState;
        synchronized (lock) {
            if (sessionGen != this.generation || isTerminal()) return;
            this.synthesizedIndex = Math.max(this.synthesizedIndex, index);
            oldState = this.state;
            if (this.state != State.PLAYING && this.state != State.FALLBACK_PLAYING) {
                this.state = State.CHUNK_READY;
            }
        }
        notifyStateChange(oldState, this.state);
    }

    public void onChunkPlaybackStarted(int index, long sessionGen) {
        State oldState;
        int currentTotal;
        int currentSpoken;
        synchronized (lock) {
            if (sessionGen != this.generation || this.state == State.CANCELLED) return;
            this.startedPlaybackIndex = Math.max(this.startedPlaybackIndex, index);
            oldState = this.state;
            this.state = State.PLAYING;
            currentTotal = this.totalChunks;
            currentSpoken = calculateSpokenChars(this.completedPlaybackIndex);
        }
        notifyStateChange(oldState, State.PLAYING);
        notifyProgress(index, currentTotal, currentSpoken);
    }

    public void onChunkPlaybackCompleted(int index, long sessionGen) {
        State oldState;
        boolean allFinished = false;
        int currentTotal;
        int currentSpoken;
        synchronized (lock) {
            if (sessionGen != this.generation || this.state == State.CANCELLED) return;
            this.completedPlaybackIndex = Math.max(this.completedPlaybackIndex, index);
            this.lastSpokenCharPosition = calculateSpokenChars(this.completedPlaybackIndex);
            currentTotal = this.totalChunks;
            currentSpoken = this.lastSpokenCharPosition;
            if (this.completedPlaybackIndex >= this.totalChunks - 1) {
                oldState = this.state;
                this.state = State.COMPLETED;
                this.activeVoiceSource = VoiceSource.NONE;
                allFinished = true;
            } else {
                oldState = this.state;
            }
        }
        notifyProgress(index, currentTotal, currentSpoken);
        if (allFinished) {
            notifyStateChange(oldState, State.COMPLETED);
            notifySourceChange(VoiceSource.CHARON, VoiceSource.NONE);
        }
    }

    public void onSynthesisFailed(int failedIndex, long sessionGen, String reason, int httpCode, boolean quota) {
        State oldState;
        synchronized (lock) {
            if (sessionGen != this.generation || isTerminal()) return;
            this.failureReason = reason == null ? "Falha na síntese de áudio" : reason;
            this.httpErrorCode = httpCode;
            this.isQuotaError = quota;
            oldState = this.state;
            this.state = State.FALLBACK_PENDING;
        }
        notifyStateChange(oldState, State.FALLBACK_PENDING);
    }

    public void onFallbackStarted(VoiceSource fallbackSource, long sessionGen) {
        State oldState;
        VoiceSource oldSource;
        synchronized (lock) {
            if (sessionGen != this.generation || this.state == State.CANCELLED) return;
            oldState = this.state;
            oldSource = this.activeVoiceSource;
            this.activeVoiceSource = fallbackSource;
            this.state = State.FALLBACK_PLAYING;
        }
        notifyStateChange(oldState, State.FALLBACK_PLAYING);
        notifySourceChange(oldSource, fallbackSource);
    }

    public void onCancel() {
        State oldState;
        VoiceSource oldSource;
        synchronized (lock) {
            if (this.state == State.CANCELLED) return;
            oldState = this.state;
            oldSource = this.activeVoiceSource;
            this.state = State.CANCELLED;
            this.activeVoiceSource = VoiceSource.NONE;
        }
        notifyStateChange(oldState, State.CANCELLED);
        notifySourceChange(oldSource, VoiceSource.NONE);
    }

    public void onError(String error) {
        State oldState;
        VoiceSource oldSource;
        synchronized (lock) {
            oldState = this.state;
            oldSource = this.activeVoiceSource;
            this.failureReason = error;
            this.state = State.ERROR;
            this.activeVoiceSource = VoiceSource.NONE;
        }
        notifyStateChange(oldState, State.ERROR);
        notifySourceChange(oldSource, VoiceSource.NONE);
    }

    public int getFirstUnheardChunkIndex() {
        synchronized (lock) {
            // Se nenhum bloco terminou, o primeiro não ouvido é o 0.
            // Se o bloco K completou, o primeiro não ouvido é K + 1.
            return Math.max(0, this.completedPlaybackIndex + 1);
        }
    }

    public List<String> getUnheardChunks() {
        synchronized (lock) {
            int first = getFirstUnheardChunkIndex();
            if (first >= chunks.size()) return Collections.emptyList();
            return new ArrayList<>(chunks.subList(first, chunks.size()));
        }
    }

    public String getUnheardRemainingText() {
        List<String> un = getUnheardChunks();
        if (un.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String c : un) {
            String text = NpcVoiceDirector.text(c);
            if (text == null || text.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(text.trim());
        }
        return sb.toString();
    }

    public boolean isTerminal() {
        return state == State.COMPLETED || state == State.CANCELLED || state == State.ERROR;
    }

    public State getState() { synchronized (lock) { return state; } }
    public VoiceSource getActiveVoiceSource() { synchronized (lock) { return activeVoiceSource; } }
    public String getActiveVoiceLabel() { synchronized (lock) { return activeVoiceSource.getLabel(); } }
    public long getSessionId() { synchronized (lock) { return sessionId; } }
    public long getGeneration() { synchronized (lock) { return generation; } }
    public int getTotalChunks() { synchronized (lock) { return totalChunks; } }
    public int getStartedPlaybackIndex() { synchronized (lock) { return startedPlaybackIndex; } }
    public int getCompletedPlaybackIndex() { synchronized (lock) { return completedPlaybackIndex; } }
    public String getFailureReason() { synchronized (lock) { return failureReason; } }
    public boolean isManualCharonSelection() { synchronized (lock) { return manualCharonSelection; } }
    public boolean isQuotaError() { synchronized (lock) { return isQuotaError; } }

    private int calculateSpokenChars(int upToChunkIndex) {
        if (upToChunkIndex < 0 || chunks == null || chunks.isEmpty()) return 0;
        int count = 0;
        int limit = Math.min(upToChunkIndex + 1, chunks.size());
        for (int i = 0; i < limit; i++) {
            String t = NpcVoiceDirector.text(chunks.get(i));
            if (t != null) count += t.length();
        }
        return count;
    }

    private void notifyStateChange(State oldState, State newState) {
        if (oldState == newState) return;
        Listener l;
        synchronized (lock) { l = this.listener; }
        if (l != null) l.onStateChanged(oldState, newState, this);
    }

    private void notifySourceChange(VoiceSource oldSource, VoiceSource newSource) {
        if (oldSource == newSource) return;
        Listener l;
        synchronized (lock) { l = this.listener; }
        if (l != null) l.onVoiceSourceChanged(oldSource, newSource);
    }

    private void notifyProgress(int chunk, int total, int spokenChars) {
        Listener l;
        synchronized (lock) { l = this.listener; }
        if (l != null) l.onPlaybackProgress(chunk, total, spokenChars);
    }

    public JSONObject toDiagnosticJson() {
        JSONObject json = new JSONObject();
        try {
            synchronized (lock) {
                json.put("sessionId", sessionId);
                json.put("generation", generation);
                json.put("state", state.name());
                json.put("activeVoiceSource", activeVoiceSource.name());
                json.put("manualCharonSelection", manualCharonSelection);
                json.put("totalChunks", totalChunks);
                json.put("requestedIndex", requestedIndex);
                json.put("synthesizedIndex", synthesizedIndex);
                json.put("startedPlaybackIndex", startedPlaybackIndex);
                json.put("completedPlaybackIndex", completedPlaybackIndex);
                json.put("lastSpokenCharPosition", lastSpokenCharPosition);
                json.put("failureReason", failureReason);
                json.put("httpErrorCode", httpErrorCode);
                json.put("isQuotaError", isQuotaError);
            }
        } catch (Exception ignored) {}
        return json;
    }
}
