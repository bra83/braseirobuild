package com.braseiro.pathfinder2e;

import android.content.Context;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * Camada de segurança da campanha.
 *
 * Os snapshots são escritos de forma atômica, possuem SHA-256, são validados
 * antes da restauração e nunca interpretam uma falha de leitura como campanha
 * vazia. Somente as cinco versões válidas mais recentes são mantidas.
 */
public final class CampaignSafetyManager {
    private static final int MAX_SNAPSHOTS = 5;
    private final Context context;
    private final AppDb db;
    private final File snapshotsDir;
    private final File diagnosticsFile;
    private String startupMessage = "Banco ainda não verificado.";

    public CampaignSafetyManager(Context context, AppDb db) {
        this.context = context.getApplicationContext();
        this.db = db;
        this.snapshotsDir = new File(this.context.getFilesDir(), "campaign_snapshots");
        this.diagnosticsFile = new File(this.context.getFilesDir(), "braseiro_diagnostics.log");
        if (!snapshotsDir.exists()) snapshotsDir.mkdirs();
    }

    public synchronized String startupCheck() {
        try {
            JSONObject health = db.healthCheck();
            if (!health.optBoolean("healthy")) {
                appendDiagnostic("Banco reprovado na inicialização: " + health);
                File latest = latestValidSnapshot();
                if (latest == null) {
                    startupMessage = "Banco com problema e nenhum snapshot válido disponível.";
                    return startupMessage;
                }
                restore(latest);
                JSONObject after = db.healthCheck();
                if (!after.optBoolean("healthy")) {
                    startupMessage = "A restauração foi tentada, mas o banco ainda precisa de diagnóstico.";
                    return startupMessage;
                }
                startupMessage = "Campanha recuperada automaticamente do último snapshot válido.";
                db.addAuditEvent("segurança", "Restauração automática", startupMessage, "{}", "{}", false);
                return startupMessage;
            }
            if (snapshotFiles().length == 0)
                startupMessage = "Banco íntegro • primeiro snapshot será criado em segundo plano.";
            else
                startupMessage = "Banco íntegro • snapshots automáticos ativos.";
        } catch (Exception e) {
            startupMessage = "A verificação de segurança falhou: " + safe(e);
            appendDiagnostic(startupMessage);
        }
        return startupMessage;
    }

    public String getStartupMessage() { return startupMessage; }

    /**
     * Cria o primeiro snapshot fora da inicialização da interface. O método é
     * idempotente: duas solicitações simultâneas nunca publicam dois snapshots.
     */
    public synchronized File ensureInitialSnapshot() throws Exception {
        if (snapshotFiles().length > 0) return null;
        return createSnapshot("Primeiro snapshot íntegro");
    }

    /** Contagem barata; não abre nem desserializa os snapshots. */
    public synchronized int snapshotFileCount() { return snapshotFiles().length; }

    public synchronized File beforeDestructiveChange(String reason) throws Exception {
        File snapshot = createSnapshot("Antes de: " + reason);
        db.addAuditEvent("segurança", "Snapshot preventivo", reason, "{}", "{}", false);
        return snapshot;
    }

    public synchronized File createSnapshot(String reason) throws Exception {
        JSONObject backup = db.exportAll();
        validateBackup(backup);
        String payload = backup.toString();
        JSONObject envelope = new JSONObject();
        envelope.put("format", "braseiro-safe-snapshot");
        envelope.put("format_version", 1);
        envelope.put("app_version", BuildConfig.VERSION_NAME);
        envelope.put("created_at", System.currentTimeMillis());
        envelope.put("reason", reason == null ? "Snapshot manual" : reason);
        envelope.put("payload_sha256", sha256(payload.getBytes("UTF-8")));
        // O texto exato também é guardado: assim o checksum não depende da
        // ordem em que JSONObject reexpõe as chaves em versões diferentes.
        envelope.put("payload", payload);
        envelope.put("backup", backup);
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date());
        String world = "pf2";
        File target = new File(snapshotsDir, "snapshot_" + world + "_" + stamp + ".json");
        atomicWrite(target, envelope.toString());
        readAndValidate(target);
        prune();
        return target;
    }

    public synchronized String restoreLatest() throws Exception {
        File latest = latestValidSnapshot();
        if (latest == null) throw new Exception("Nenhum snapshot válido foi encontrado.");
        restore(latest);
        db.addAuditEvent("segurança", "Snapshot restaurado", latest.getName(), "{}", "{}", false);
        return latest.getName();
    }

    public synchronized String restoreSnapshot(String fileName) throws Exception {
        String safeName = fileName == null ? "" : new File(fileName).getName();
        if (safeName.length() == 0 || !safeName.equals(fileName)) throw new Exception("Nome de snapshot inválido.");
        File target = new File(snapshotsDir, safeName);
        if (!target.isFile()) throw new Exception("O snapshot de reversão não está mais disponível.");
        restore(target);
        db.addAuditEvent("segurança", "Resolução do Mestre desfeita", safeName, "{}", "{}", false);
        return safeName;
    }

    private void restore(File file) throws Exception {
        JSONObject envelope = readAndValidate(file);
        JSONObject backup = envelope.getJSONObject("backup");
        validateBackup(backup);
        String backupWorld=backup.optString("worldProfile","pf2");if(!"pf2".equals(backupWorld)&&!"pathfinder_2e".equals(backupWorld))throw new Exception("O snapshot pertence a outro sistema e foi bloqueado.");
        db.importAll(backup);
        JSONObject health=db.healthCheck();
        if(!health.optBoolean("healthy",false))throw new Exception("O snapshot foi lido, mas a verificação do banco restaurado falhou: "+health.optString("error",health.optString("quick_check","estado desconhecido")));
    }

    public synchronized JSONArray listSnapshots() {
        JSONArray result = new JSONArray();
        File[] files = snapshotFiles();
        for (File file : files) {
            try {
                JSONObject env = readAndValidate(file);
                result.put(new JSONObject()
                        .put("name", file.getName())
                        .put("created_at", env.optLong("created_at", file.lastModified()))
                        .put("reason", env.optString("reason"))
                        .put("size_bytes", file.length())
                        .put("valid", true));
            } catch (Exception e) {
                try {
                    result.put(new JSONObject()
                            .put("name", file.getName())
                            .put("size_bytes", file.length())
                            .put("valid", false)
                            .put("error", safe(e)));
                } catch(Exception ignored){AppLog.ignored("CampaignSafetyManager","Falha tolerada; a execução continuou.",ignored);}
            }
        }
        return result;
    }

    public synchronized JSONObject diagnosticReport() {
        JSONObject out = new JSONObject();
        try {
            out.put("format", "braseiro-diagnostic");
            out.put("app_version", BuildConfig.VERSION_NAME);
            out.put("created_at", System.currentTimeMillis());
            out.put("database", db.healthCheck());
            out.put("snapshots", listSnapshots());
            out.put("audit_count", db.countAuditEvents());
            out.put("campaign", db.getCampaign());
            out.put("character_count", db.listCharacters().size());
            out.put("manual_count", db.countManuals());
            out.put("last_database_error", db.getLastDatabaseError());
            out.put("device", android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
            out.put("android_api", android.os.Build.VERSION.SDK_INT);
            out.put("runtime_log_bytes", AppLog.bytes());
            out.put("runtime_log_tail", AppLog.tail(24000));
        } catch (Exception e) {
            try { out.put("diagnostic_error", safe(e)); } catch(Exception ignored){AppLog.ignored("CampaignSafetyManager","Falha tolerada; a execução continuou.",ignored);}
        }
        return out;
    }

    public synchronized File writeDiagnosticReport() throws Exception {
        File dir = new File(context.getFilesDir(), "diagnostics");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "Braseiro_diagnostico_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".json");
        atomicWrite(file, diagnosticReport().toString(2));
        return file;
    }

    private JSONObject readAndValidate(File file) throws Exception {
        String raw = read(file);
        JSONObject env = new JSONObject(raw);
        if (!"braseiro-safe-snapshot".equals(env.optString("format")))
            throw new Exception("Formato de snapshot incompatível.");
        JSONObject backup = env.optJSONObject("backup");
        if (backup == null) throw new Exception("Snapshot sem conteúdo.");
        String payload = env.optString("payload", "");
        if (payload.length() == 0) payload = backup.toString();
        String actual = sha256(payload.getBytes("UTF-8"));
        if (!actual.equals(env.optString("payload_sha256")))
            throw new Exception("Checksum do snapshot não confere.");
        JSONObject parsed = new JSONObject(payload);
        validateBackup(parsed);
        env.put("backup", parsed);
        return env;
    }

    private void validateBackup(JSONObject backup) throws Exception {
        BackupContractValidator.requireValid(backup);
    }

    private File latestValidSnapshot() {
        for (File file : snapshotFiles()) {
            try { JSONObject envelope=readAndValidate(file);if(activeWorld().equals(envelope.getJSONObject("backup").optString("worldProfile",activeWorld())))return file; }
            catch (Exception e) { appendDiagnostic("Snapshot inválido " + file.getName() + ": " + safe(e)); }
        }
        return null;
    }

    /** Retorna somente snapshots Pathfinder 2e. */
    private File[] snapshotFiles() {
        File[] all=allSnapshotFiles();java.util.ArrayList<File> selected=new java.util.ArrayList<File>();
        for(File file:all){String name=file.getName();if(name.startsWith("snapshot_pf2_")){selected.add(file);continue;}try{JSONObject env=readAndValidate(file);String profile=env.getJSONObject("backup").optString("worldProfile","pf2");if("pf2".equals(profile)||"pathfinder_2e".equals(profile))selected.add(file);}catch(Exception e){appendDiagnostic("Snapshot incompatível "+name+": "+safe(e));}}
        return selected.toArray(new File[selected.size()]);
    }

    private File[] allSnapshotFiles() {
        File[] files = snapshotsDir.listFiles(new java.io.FileFilter() {
            public boolean accept(File file) {
                String name=file==null?"":file.getName();
                return file!=null&&file.isFile()&&name.startsWith("snapshot_")&&name.endsWith(".json");
            }
        });
        if (files == null) return new File[0];
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File a, File b) { return Long.compare(b.lastModified(), a.lastModified()); }
        });
        return files;
    }

    private String activeWorld(){return "pf2";}

    private void prune() {
        File[] files = snapshotFiles();
        int valid = 0;
        for (File file : files) {
            try {
                readAndValidate(file);
                valid++;
                if (valid > MAX_SNAPSHOTS && !file.delete()) appendDiagnostic("Não foi possível remover snapshot excedente: "+file.getName());
            } catch (Exception e) {
                File quarantine = new File(file.getParentFile(), file.getName() + ".corrompido");
                if(!file.renameTo(quarantine))appendDiagnostic("Não foi possível isolar snapshot corrompido: "+file.getName());
            }
        }
    }

    private void atomicWrite(File target, String text) throws Exception {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new Exception("Não foi possível criar a pasta de segurança.");
        AtomicFile atomic = new AtomicFile(target);
        FileOutputStream out = null;
        try {
            out = atomic.startWrite();
            out.write(text.getBytes("UTF-8"));
            out.flush();
            out.getFD().sync();
            atomic.finishWrite(out);
        } catch (Exception e) {
            if (out != null) atomic.failWrite(out);
            throw e;
        }
    }

    private void appendDiagnostic(String message) {
        AppLog.w("CampaignSafety", message);
        try {
            FileOutputStream out = new FileOutputStream(diagnosticsFile, true);
            out.write((new Date() + " • " + message + "\n").getBytes("UTF-8"));
            out.flush();
            out.close();
        } catch (Exception e) { AppLog.w("CampaignSafety", "Falha ao gravar o diagnóstico local.", e); }
    }

    private static String read(File file) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        StringBuilder b = new StringBuilder();
        char[] buffer = new char[8192];
        int n;
        while ((n = reader.read(buffer)) > 0) b.append(buffer, 0, n);
        reader.close();
        return b.toString();
    }

    private static String sha256(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder b = new StringBuilder();
        for (byte value : digest) b.append(String.format(Locale.US, "%02x", value & 0xff));
        return b.toString();
    }

    private static String safe(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
