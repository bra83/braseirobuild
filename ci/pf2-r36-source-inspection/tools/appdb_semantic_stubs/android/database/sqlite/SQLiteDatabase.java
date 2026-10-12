package android.database.sqlite;
import android.content.ContentValues; import android.database.Cursor;
public class SQLiteDatabase {
 public static final int CONFLICT_IGNORE=4, CONFLICT_REPLACE=5, CONFLICT_ABORT=2;
 public void execSQL(String s){} public void execSQL(String s,Object[] a){}
 public Cursor rawQuery(String s,String[] a){return null;}
 public long insert(String t,String n,ContentValues v){return 0;} public long insertOrThrow(String t,String n,ContentValues v){return 0;}
 public long insertWithOnConflict(String t,String n,ContentValues v,int c){return 0;}
 public int update(String t,ContentValues v,String w,String[] a){return 0;} public int delete(String t,String w,String[] a){return 0;}
 public void beginTransaction(){} public void setTransactionSuccessful(){} public void endTransaction(){}
 public boolean inTransaction(){return false;} public int getVersion(){return 0;}
 public void setForeignKeyConstraintsEnabled(boolean b){}
}
