package com.braseiro.dnd5e;

import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Autoridade única de uma sessão PLAY.
 * Todo callback assíncrono deve validar runId + sessionId antes de agir.
 */
public final class AudioSessionCoordinator {
    public static final class Session {
        public final long runId;
        public final String sessionId;
        public final String sceneId;
        public final long startedAt;
        Session(long run, String id, String scene, long at) {
            runId = run; sessionId = id; sceneId = scene; startedAt = at;
        }
    }

    private final AtomicLong counter = new AtomicLong(0L);
    private final AudioDiagnosticsRepository diagnostics;
    private volatile Session current;

    public AudioSessionCoordinator(AudioDiagnosticsRepository repository) {
        diagnostics = repository;
    }

    public synchronized Session begin(String sceneId) {/*BR_JSON_WRAP*/try{
        if (current != null) endLocked(current, "superseded");
        long run = counter.incrementAndGet();
        String id = diagnostics.newSessionId(sceneId);
        current = new Session(run, id, safe(sceneId), System.currentTimeMillis());
        JSONObject details = new JSONObject();
        try { details.put("runId", run).put("sceneId", safe(sceneId)); } catch(Exception ignored){AppLog.ignored("AudioSessionCoordinator","Falha tolerada; a execução continuou.",ignored);}
        diagnostics.event(id, "session-started", details);
        return current;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public synchronized void cancel(String reason) {
        Session old = current;
        counter.incrementAndGet();
        current = null;
        if (old != null) endLocked(old, safe(reason).length() == 0 ? "cancelled" : reason);
    }

    public synchronized void complete(Session session) {
        if (!isCurrent(session)) return;
        current = null;
        endLocked(session, "completed");
    }

    public boolean isCurrent(Session session) {
        Session now = current;
        return session != null && now != null && session.runId == now.runId && session.sessionId.equals(now.sessionId);
    }

    public long currentRunId() { Session value = current; return value == null ? -1L : value.runId; }
    public String currentSessionId() { Session value = current; return value == null ? "" : value.sessionId; }

    public void event(Session session, String type, JSONObject details) {
        if (!isCurrent(session)) return;
        diagnostics.event(session.sessionId, type, details);
    }

    public void event(Session session, String type, String key, Object value) {
        if (!isCurrent(session)) return;
        diagnostics.event(session.sessionId, type, key, value);
    }

    private void endLocked(Session session, String status) {/*BR_JSON_WRAP*/try{
        JSONObject details = new JSONObject();
        try {
            details.put("status", status);
            details.put("durationMs", Math.max(0L, System.currentTimeMillis() - session.startedAt));
        } catch(Exception ignored){AppLog.ignored("AudioSessionCoordinator","Falha tolerada; a execução continuou.",ignored);}
        diagnostics.event(session.sessionId, "session-ended", details);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static String safe(String value) { return value == null ? "" : value; }
}
