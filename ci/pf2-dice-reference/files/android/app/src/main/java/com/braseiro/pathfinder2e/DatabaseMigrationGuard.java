package com.braseiro.pathfinder2e;
import android.content.Context;
import android.content.SharedPreferences;
import java.io.*;

/** Snapshot físico do banco PF2e antes da primeira abertura do schema 58 e rollback se o quick_check falhar. */
public final class DatabaseMigrationGuard {
    private static final String DB="braseiro_pathfinder2e.db",BACKUP="braseiro_pathfinder2e.pre_v58.db";
    private static final String PREF="db_migration_guard_pf2_v58",OK="v58_ok";
    private DatabaseMigrationGuard(){}
    public static void beforeOpen(Context c){
        try{
            SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);if(p.getBoolean(OK,false))return;
            File src=c.getDatabasePath(DB);if(!src.exists())return;
            File dst=new File(src.getParentFile(),BACKUP);copy(src,dst);
            File wal=new File(src.getPath()+"-wal");if(wal.exists())copy(wal,new File(dst.getPath()+"-wal"));
            File shm=new File(src.getPath()+"-shm");if(shm.exists())copy(shm,new File(dst.getPath()+"-shm"));
        }catch(Exception e){AppLog.w("DatabaseMigrationGuard","Não foi possível criar snapshot pré-schema 58 do Pathfinder.",e);}
    }
    public static void afterOpen(Context c,AppDb db)throws Exception{
        if(db.quickIntegrityCheck()){c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putBoolean(OK,true).apply();return;}
        db.close();File live=c.getDatabasePath(DB),bak=new File(live.getParentFile(),BACKUP);
        if(bak.exists()){
            deleteQuiet(live);deleteQuiet(new File(live.getPath()+"-wal"));deleteQuiet(new File(live.getPath()+"-shm"));copy(bak,live);
            File bw=new File(bak.getPath()+"-wal");if(bw.exists())copy(bw,new File(live.getPath()+"-wal"));
            File bs=new File(bak.getPath()+"-shm");if(bs.exists())copy(bs,new File(live.getPath()+"-shm"));
        }
        throw new IOException("A migração do banco Pathfinder para o schema 58 falhou no quick_check; o snapshot anterior foi restaurado.");
    }
    private static void copy(File a,File b)throws IOException{File parent=b.getParentFile();if(parent!=null)parent.mkdirs();try(InputStream in=new FileInputStream(a);OutputStream out=new FileOutputStream(b)){byte[] buf=new byte[1024*1024];int n;while((n=in.read(buf))>0)out.write(buf,0,n);out.flush();}}
    private static void deleteQuiet(File f){try{if(f.exists()&&!f.delete())AppLog.w("DatabaseMigrationGuard","Não foi possível remover artefato antigo: "+f.getName());}catch(Exception ignored){AppLog.ignored("DatabaseMigrationGuard","Falha tolerada ao limpar snapshot do Pathfinder.",ignored);}}
}
