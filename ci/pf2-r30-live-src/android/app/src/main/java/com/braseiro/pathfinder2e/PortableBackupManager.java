package com.braseiro.pathfinder2e;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Backup automático opcional em uma pasta SAF escolhida pelo usuário. */
public final class PortableBackupManager {
    private static final String PREFIX = "Braseiro_campanha_";
    private static final String LEGACY_PREFIX = "Braseiro_Pathfinder2e_autobackup_";
    private final Context context;
    private final ContentResolver resolver;
    private final AppDb db;
    private final LibraryConnectionManager connections;

    public PortableBackupManager(Context context, AppDb db, LibraryConnectionManager connections) {
        this.context = context.getApplicationContext();
        this.resolver = this.context.getContentResolver();
        this.db = db;
        this.connections = connections;
    }

    public JSONObject createAutomaticBackup(int retention) throws Exception {
        JSONObject status = connections.status(LibraryConnectionManager.BACKUPS, true);
        if (!status.optBoolean("healthy", false)) throw new Exception("Pasta de backup indisponível: " + status.optString("detail"));
        Uri tree = Uri.parse(connections.storedUri(LibraryConnectionManager.BACKUPS));
        String treeId = DocumentsContract.getTreeDocumentId(tree);
        Uri root = DocumentsContract.buildDocumentUriUsingTree(tree, treeId);
        JSONObject backup = db.exportAll();
        BackupContractValidator.requireValid(backup);
        String world = backup.optString("worldProfile", "pf2");
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String name = PREFIX + world + "_" + stamp + ".json";
        Uri document = DocumentsContract.createDocument(resolver, root, "application/json", name);
        if (document == null) throw new Exception("O provedor não criou o arquivo de backup.");
        OutputStream out = resolver.openOutputStream(document, "w");
        if (out == null) throw new Exception("O provedor não abriu o arquivo de backup para gravação.");
        String serialized = backup.toString(2);
        byte[] bytes = serialized.getBytes("UTF-8");
        boolean complete = false;
        try {
            out.write(bytes); out.flush(); out.close(); out = null;
            InputStream in = resolver.openInputStream(document);
            if (in == null) throw new Exception("O provedor gravou o backup, mas não permitiu verificá-lo.");
            StringBuilder verify = new StringBuilder(); byte[] buffer = new byte[8192]; int read;
            try { while ((read = in.read(buffer)) >= 0) verify.append(new String(buffer, 0, read, "UTF-8")); }
            finally { in.close(); }
            JSONObject restored = new JSONObject(verify.toString());
            BackupContractValidator.requireValid(restored);
            if (!serialized.equals(verify.toString())) throw new Exception("O provedor alterou o conteúdo do backup durante a gravação.");
            if (!world.equals(restored.optString("worldProfile", world))) throw new Exception("O perfil do mundo mudou durante o backup.");
            complete = true;
        } finally {
            if (out != null) try { out.close(); } catch(Exception ignored){AppLog.ignored("PortableBackupManager","Falha tolerada; a execução continuou.",ignored);}
            if (!complete) try { DocumentsContract.deleteDocument(resolver, document); } catch(Exception ignored){AppLog.ignored("PortableBackupManager","Falha tolerada; a execução continuou.",ignored);}
        }
        int removed = prune(tree, Math.max(2, Math.min(20, retention)), world);
        return new JSONObject().put("name", name).put("uri", document.toString()).put("removed_old", removed).put("world", world).put("verified_bytes", bytes.length);
    }

    private int prune(Uri tree, int keep, String world) throws Exception {
        String treeId = DocumentsContract.getTreeDocumentId(tree);
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, treeId);
        String[] projection = {DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_LAST_MODIFIED};
        Cursor cursor = resolver.query(children, projection, null, null, null);
        if (cursor == null) return 0;
        ArrayList<Entry> backups = new ArrayList<Entry>();
        try {
            int idColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
            int nameColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            int modifiedColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED);
            while (cursor.moveToNext()) {
                String name = nameColumn >= 0 && !cursor.isNull(nameColumn) ? cursor.getString(nameColumn) : "";
                String currentPrefix=PREFIX+"pf2_";
                if (!name.startsWith(currentPrefix) || !name.toLowerCase(Locale.US).endsWith(".json")) continue;
                Entry entry = new Entry();
                entry.id = idColumn >= 0 ? cursor.getString(idColumn) : "";
                entry.modified = modifiedColumn >= 0 && !cursor.isNull(modifiedColumn) ? cursor.getLong(modifiedColumn) : 0L;
                backups.add(entry);
            }
        } finally { cursor.close(); }
        Collections.sort(backups, new Comparator<Entry>() { public int compare(Entry a, Entry b) { return Long.compare(b.modified, a.modified); } });
        int removed = 0;
        for (int i = keep; i < backups.size(); i++) {
            Entry entry = backups.get(i);
            if (entry.id == null || entry.id.length() == 0) continue;
            Uri document = DocumentsContract.buildDocumentUriUsingTree(tree, entry.id);
            try { if (DocumentsContract.deleteDocument(resolver, document)) removed++; }
            catch (Exception e) { AppLog.w("PortableBackup", "Não foi possível remover um backup antigo.", e); }
        }
        return removed;
    }

    private static final class Entry { String id; long modified; }
}
