package com.braseiro.dnd5e;
import android.content.Context;
import android.content.SharedPreferences;
import java.io.*;

/** Snapshot físico antes da primeira abertura do schema 53 e rollback se o banco não passar no quick_check. */
public final class DatabaseMigrationGuard {
    private static final String DB="braseiro_dnd.db",BACKUP="braseiro_dnd.pre_v53.db";
    private DatabaseMigrationGuard(){}
    public static void beforeOpen(Context c){
        try{SharedPreferences p=c.getSharedPreferences("db_migration_guard_v5",Context.MODE_PRIVATE);if(p.getBoolean("v53_ok",false))return;File src=c.getDatabasePath(DB);if(!src.exists())return;File dst=new File(src.getParentFile(),BACKUP);copy(src,dst);File wal=new File(src.getPath()+"-wal");if(wal.exists())copy(wal,new File(dst.getPath()+"-wal"));File shm=new File(src.getPath()+"-shm");if(shm.exists())copy(shm,new File(dst.getPath()+"-shm"));}catch(Exception e){AppLog.w("DatabaseMigrationGuard","Não foi possível criar snapshot pré-5.0.",e);}
    }
    public static void afterOpen(Context c,AppDb db)throws Exception{
        SharedPreferences p=c.getSharedPreferences("db_migration_guard_v5",Context.MODE_PRIVATE);if(p.getBoolean("v53_ok",false))return;
        if(db.quickIntegrityCheck()){p.edit().putBoolean("v53_ok",true).apply();return;}
        db.close();File live=c.getDatabasePath(DB),bak=new File(live.getParentFile(),BACKUP);if(bak.exists()){deleteQuiet(live);deleteQuiet(new File(live.getPath()+"-wal"));deleteQuiet(new File(live.getPath()+"-shm"));copy(bak,live);File bw=new File(bak.getPath()+"-wal");if(bw.exists())copy(bw,new File(live.getPath()+"-wal"));File bs=new File(bak.getPath()+"-shm");if(bs.exists())copy(bs,new File(live.getPath()+"-shm"));}throw new IOException("A migração 5.0 falhou no quick_check; o banco anterior foi restaurado.");
    }
    private static void copy(File a,File b)throws IOException{b.getParentFile().mkdirs();try(InputStream in=new FileInputStream(a);OutputStream out=new FileOutputStream(b)){byte[] buf=new byte[1024*1024];int n;while((n=in.read(buf))>0)out.write(buf,0,n);out.flush();}}
    private static void deleteQuiet(File f){try{if(f.exists())f.delete();}catch(Exception ignored){AppLog.ignored("DatabaseMigrationGuard","Falha tolerada registrada na consolidação 5.0.",ignored);}}
}
