package com.braseiro.dnd5e;

import android.content.Context;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Fila serial para importações e indexações. Mantém checkpoints atômicos,
 * impede duplicação da mesma tarefa e conserva um histórico pequeno e útil.
 */
public final class ResilientTaskQueue {
    public interface Work { void run(Progress progress) throws Exception; }
    public interface Callback { void onSuccess(); void onError(String message); }
    public interface Progress { void update(int done, int total, String message); }

    private static final int MAX_HISTORY = 40;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final File stateFile;
    private final AppDb db;
    private final Set<String> inFlight = new HashSet<String>();
    private volatile boolean closed;

    public ResilientTaskQueue(Context context, AppDb db) {
        this.stateFile = new File(context.getApplicationContext().getFilesDir(), "durable_tasks.json");
        this.db = db;
        recoverInterruptedTasks();
    }

    public void submit(final String rawId, final String label, final Work work, final Callback callback) {
        final String id = rawId == null || rawId.trim().length() == 0 ? "task-" + System.currentTimeMillis() : rawId.trim();
        synchronized (inFlight) {
            if (closed) { safeError(callback,"A fila de tarefas já foi encerrada."); return; }
            if (!inFlight.add(id)) { safeError(callback,"Esta tarefa já está em execução ou aguardando: "+label); return; }
        }
        executor.execute(new Runnable() {
            public void run() {
                try {
                    checkpoint(id, label, "running", 0, 0, "Iniciando");
                    if (work == null) throw new Exception("Tarefa sem operação definida.");
                    work.run(new Progress() {
                        public void update(int done, int total, String message) {
                            checkpoint(id, label, "running", Math.max(0,done), Math.max(0,total), message);
                        }
                    });
                    checkpoint(id, label, "completed", 1, 1, "Concluído");
                    db.addAuditEvent("tarefa", label, "Concluída", "{}", "{}", false);
                    safeSuccess(callback);
                } catch (Exception e) {
                    String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                    checkpoint(id, label, "failed", 0, 0, message);
                    AppLog.e("ResilientTaskQueue",label+" falhou: "+message,e);
                    try { db.addAuditEvent("tarefa", label, "Falhou: " + message, "{}", "{}", false); }
                    catch (Exception auditError) { AppLog.w("ResilientTaskQueue","Não foi possível registrar a falha no histórico.",auditError); }
                    safeError(callback,message);
                } finally {
                    synchronized (inFlight) { inFlight.remove(id); }
                }
            }
        });
    }

    public JSONObject state() {/*BR_JSON_WRAP*/try{
        try {
            if (!stateFile.exists()) return emptyState();
            StringBuilder b = new StringBuilder(); String line;
            try (BufferedReader r = new BufferedReader(new FileReader(stateFile))) {
                while ((line = r.readLine()) != null) b.append(line);
            }
            JSONObject parsed = new JSONObject(b.toString());
            if (parsed.optJSONArray("tasks") == null) parsed.put("tasks",new JSONArray());
            return parsed;
        } catch (Exception e) {
            AppLog.w("ResilientTaskQueue","Checkpoint de tarefas corrompido; um estado vazio seguro será usado.",e);
            return emptyState();
        }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public synchronized void shutdown() {
        if (closed) return;
        closed = true;
        executor.shutdownNow();
        try { executor.awaitTermination(1200L, TimeUnit.MILLISECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private synchronized void recoverInterruptedTasks() {/*BR_JSON_WRAP*/try{
        try {
            JSONObject root = state();
            JSONArray tasks = root.optJSONArray("tasks");
            if (tasks == null) return;
            boolean changed = false;
            for (int i = 0; i < tasks.length(); i++) {
                JSONObject task = tasks.optJSONObject(i);
                if (task == null || !"running".equals(task.optString("status"))) continue;
                task.put("status", "interrupted");
                task.put("message", "Interrompida pelo fechamento do aplicativo; pode ser executada novamente.");
                task.put("updated_at", System.currentTimeMillis());
                changed = true;
            }
            if (!changed) return;
            atomicWrite(root);
            db.addAuditEvent("tarefa", "Tarefa interrompida detectada",
                    "O app preservou o checkpoint; a operação pode ser reiniciada.", "{}", root.toString(), false);
        } catch (Exception e) { AppLog.w("ResilientTaskQueue","Falha ao recuperar tarefas interrompidas.",e); }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private synchronized void checkpoint(String id, String label, String status, int done, int total, String message) {/*BR_JSON_WRAP*/try{
        try {
            JSONObject root = state();
            JSONArray tasks = root.optJSONArray("tasks");
            if (tasks == null) tasks = new JSONArray();
            JSONArray next = new JSONArray();
            for (int i = 0; i < tasks.length(); i++) {
                JSONObject old = tasks.optJSONObject(i);
                if (old != null && !id.equals(old.optString("id"))) next.put(old);
            }
            next.put(new JSONObject().put("id", id).put("label", label == null ? id : label).put("status", status)
                    .put("done", done).put("total", total).put("message", message == null ? "" : message)
                    .put("updated_at", System.currentTimeMillis()));
            root.put("tasks", trimHistory(next));
            atomicWrite(root);
        } catch (Exception e) { AppLog.w("ResilientTaskQueue","Não foi possível salvar o checkpoint de "+id+".",e); }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONArray trimHistory(JSONArray source) {/*BR_JSON_WRAP*/try{
        JSONArray active = new JSONArray(), recent = new JSONArray();
        for (int i = source.length() - 1; i >= 0; i--) {
            JSONObject task = source.optJSONObject(i); if (task == null) continue;
            String status = task.optString("status");
            if ("running".equals(status) || "failed".equals(status) || "interrupted".equals(status)) active.put(task);
            else if (recent.length() < MAX_HISTORY) recent.put(task);
        }
        JSONArray out = new JSONArray();
        for (int i = active.length() - 1; i >= 0; i--) out.put(active.opt(i));
        int remaining = Math.max(0,MAX_HISTORY-out.length());
        for (int i = recent.length() - 1; i >= 0 && remaining-- > 0; i--) out.put(recent.opt(i));
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private void atomicWrite(JSONObject root) throws Exception {
        File parent = stateFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new Exception("Pasta de tarefas indisponível.");
        AtomicFile atomic = new AtomicFile(stateFile);
        FileOutputStream out = null;
        try {
            out = atomic.startWrite();
            out.write(root.toString().getBytes("UTF-8"));
            out.flush();
            out.getFD().sync();
            atomic.finishWrite(out);
        } catch (Exception e) {
            if (out != null) atomic.failWrite(out);
            throw e;
        }
    }

    private static JSONObject emptyState(){/*BR_JSON_WRAP*/try{try{return new JSONObject().put("tasks",new JSONArray());}catch(Exception ignored){return new JSONObject();}}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static void safeSuccess(Callback callback){if(callback==null)return;try{callback.onSuccess();}catch(Exception e){AppLog.w("ResilientTaskQueue","Callback de sucesso falhou.",e);}}
    private static void safeError(Callback callback,String message){if(callback==null)return;try{callback.onError(message);}catch(Exception e){AppLog.w("ResilientTaskQueue","Callback de erro falhou.",e);}}
}
