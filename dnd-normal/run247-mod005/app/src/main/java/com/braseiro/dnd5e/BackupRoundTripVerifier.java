package com.braseiro.dnd5e;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.security.MessageDigest;
import java.util.*;

/**
 * Verifica o round-trip de TODAS as seções portáteis, não apenas um catálogo fixo.
 * A comparação ignora IDs internos recalculados e timestamps de escrita, mas
 * preserva a ordem dos arrays embutidos nas fichas, turnos e procedimentos.
 * As linhas de tabelas no nível raiz são tratadas como multiconjuntos: a ordem
 * de retorno do SQLite não representa diferença mecânica.
 */
public final class BackupRoundTripVerifier {
    private BackupRoundTripVerifier() {}

    public static JSONObject verify(Context context, AppDb live) {
        JSONObject report = new JSONObject();
        String temp = "braseiro_roundtrip_" + java.util.UUID.randomUUID().toString() + ".db";
        AppDb sandbox = null;
        try {
            JSONObject before = live.exportAll();
            String world = live.getActiveMapProfile();
            sandbox = new AppDb(context, temp);
            sandbox.setActiveMapProfile(world);
            sandbox.getWritableDatabase();
            sandbox.importAll(new JSONObject(before.toString()));
            JSONObject after = sandbox.exportAll();
            report = compareSnapshots(before, after);
            report.put("backup_version", before.optInt("version"));
        } catch (Exception error) {
            try { report.put("ok", false).put("error", String.valueOf(error.getMessage())); }
            catch (Exception ignored) { AppLog.ignored("BackupRoundTripVerifier", "Erro de diagnóstico", ignored); }
        } finally {
            try { if (sandbox != null) sandbox.close(); }
            catch (Exception ignored) { AppLog.ignored("BackupRoundTripVerifier", "Erro ao fechar sandbox", ignored); }
            // Never remove a hard-coded database shared by other sessions.
            context.deleteDatabase(temp);
        }
        return report;
    }

    /**
     * Compares two portable snapshots using the exact contract used by the
     * Android SQLite round-trip. Package visibility lets the JVM contract test
     * exercise the comparator without pretending to execute Android SQLite.
     */
    static JSONObject compareSnapshots(JSONObject before, JSONObject after) throws Exception {
        JSONObject comparableAfter = new JSONObject(after.toString());
        JSONArray audit = comparableAfter.optJSONArray("auditLog");
        if (audit != null) {
            for (int i = 0; i < audit.length(); i++) {
                JSONObject event = audit.optJSONObject(i);
                if (isRestoreAuditEvent(event)) {
                    JSONArray retained = new JSONArray();
                    for (int j = 0; j < audit.length(); j++) if (j != i) retained.put(audit.opt(j));
                    comparableAfter.put("auditLog", retained);
                    break; // Import adds one documented audit event; all others remain compared.
                }
            }
        }
        JSONObject first = fingerprint(before), second = fingerprint(comparableAfter);
        boolean equal = first.optString("sha256").equals(second.optString("sha256"));
        // Root sections and per-table row counts must match independently
        // of the checksum; the checksum must never conceal missing data.
        boolean sameSections = first.optJSONArray("sections").toString()
            .equals(second.optJSONArray("sections").toString());
        boolean sameCounts = first.optJSONObject("counts").toString()
            .equals(second.optJSONObject("counts").toString());
        JSONArray differences = new JSONArray();
        TreeSet<String> comparedKeys = new TreeSet<String>();
        Iterator<String> a = before.keys(), b = comparableAfter.keys();
        while (a.hasNext()) comparedKeys.add(a.next());
        while (b.hasNext()) comparedKeys.add(b.next());
        for (String key : comparedKeys) if (!volatileRootKey(key)
                && !normalizedSection(before, key).equals(normalizedSection(comparableAfter, key)))
            differences.put(key);
        return new JSONObject().put("ok", equal && sameSections && sameCounts)
            .put("different_sections", differences).put("before", first).put("after", second);
    }

    private static boolean isRestoreAuditEvent(JSONObject event) {
        return event != null && "backup".equals(event.optString("category"))
            && "Restauração auditada".equals(event.optString("title"))
            && event.optString("summary").startsWith("Referências internas verificadas após importar backup v");
    }

    private static JSONObject fingerprint(JSONObject root) throws Exception {
        if (root == null) throw new IllegalArgumentException("missing_portable_backup");
        ArrayList<String> keys = new ArrayList<String>();
        Iterator<String> iterator = root.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (!volatileRootKey(key)) keys.add(key);
        }
        Collections.sort(keys);
        JSONObject counts = new JSONObject();
        JSONArray sections = new JSONArray();
        StringBuilder material = new StringBuilder("braseiro-backup-roundtrip-v220|");
        for (String key : keys) {
            Object entry = root.opt(key);
            sections.put(key);
            String value;
            if (entry instanceof JSONArray && !"externalLibrariesExcluded".equals(key)) {
                JSONArray rows = (JSONArray) entry;
                counts.put(key, rows.length());
                ArrayList<String> normalizedRows = new ArrayList<String>();
                // Only these top-level arrays represent SQL rowsets whose
                // physical primary keys/timestamps are regenerated on import.
                // Nested objects and compiled registry records keep semantic IDs.
                for (int i = 0; i < rows.length(); i++) normalizedRows.add(stable(rows.opt(i), true));
                Collections.sort(normalizedRows);
                StringBuilder b = new StringBuilder("table:");
                for (String row : normalizedRows) appendLength(b, row);
                value = b.toString();
            } else {
                // The required object sections are single SQL rows (campaign,
                // story/combat/map/runtime state). Normalize their physical
                // columns at that row only; the compiled bundle is not a row.
                value = stable(entry, physicalObjectRoot(key));
            }
            appendLength(material, key);
            appendLength(material, value);
        }
        return new JSONObject().put("counts", counts).put("sections", sections)
            .put("sha256", sha(material.toString()));
    }

    private static String normalizedSection(JSONObject root, String key) throws Exception {
        if (root == null || !root.has(key) || volatileRootKey(key)) return "<absent>";
        Object entry = root.opt(key);
        if (entry instanceof JSONArray && !"externalLibrariesExcluded".equals(key)) {
            JSONArray rows = (JSONArray) entry;
            ArrayList<String> normalizedRows = new ArrayList<String>();
            for (int i = 0; i < rows.length(); i++) normalizedRows.add(stable(rows.opt(i), true));
            Collections.sort(normalizedRows);
            StringBuilder value = new StringBuilder("table:");
            for (String row : normalizedRows) appendLength(value, row);
            return value.toString();
        }
        return stable(entry, physicalObjectRoot(key));
    }

    private static void appendLength(StringBuilder into, String part) {
        into.append(part.length()).append(':').append(part);
    }

    private static boolean volatileRootKey(String key) {
        return "appVersion".equals(key) || "exportedAt".equals(key)
            || Backup21IntegrityEnvelope.KEY.equals(key);
    }

    private static boolean volatileField(String key) {
        // Only physical DB IDs and write timestamps are regenerated on import.
        // Fields such as expires_at, applied_at and action/turn order matter.
        return "id".equals(key) || "created_at".equals(key)
            || "updated_at".equals(key);
    }

    private static boolean physicalObjectRoot(String key) {
        for (String root : Backup21RequiredSections.OBJECTS)
            if (root.equals(key)) return true;
        return false;
    }

    private static String stable(Object value, boolean physicalSqlRow) throws Exception {
        if (value == null || value == JSONObject.NULL) return "null";
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            StringBuilder b = new StringBuilder("[");
            // An embedded ordered array may contain turn order, spell effects,
            // choices or route stops. Sorting it would mask actual corruption.
            for (int i = 0; i < array.length(); i++) appendLength(b, stable(array.opt(i), false));
            return b.append(']').toString();
        }
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            ArrayList<String> keys = new ArrayList<String>();
            Iterator<String> iterator = object.keys();
            while (iterator.hasNext()) keys.add(iterator.next());
            Collections.sort(keys);
            StringBuilder b = new StringBuilder("{");
            for (String key : keys) {
                if (physicalSqlRow && volatileField(key)) continue;
                appendLength(b, key);
                appendLength(b, stable(object.opt(key), false));
            }
            return b.append('}').toString();
        }
        if (value instanceof Number) {
            double d = ((Number)value).doubleValue();
            if (!Double.isFinite(d)) return "null";
            return new java.math.BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
        }
        return CanonicalJson.stringify(value);
    }

    private static String sha(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) hex.append(String.format(Locale.ROOT, "%02x", b));
        return hex.toString();
    }
}
