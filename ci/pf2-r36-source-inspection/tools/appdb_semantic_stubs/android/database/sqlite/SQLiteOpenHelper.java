package android.database.sqlite;
import android.content.Context;
public class SQLiteOpenHelper {
 public SQLiteOpenHelper(Context c,String n,Object f,int v){}
 public SQLiteDatabase getWritableDatabase(){return new SQLiteDatabase();} public SQLiteDatabase getReadableDatabase(){return new SQLiteDatabase();}
 public void setWriteAheadLoggingEnabled(boolean b){} public void onConfigure(SQLiteDatabase db){}
 public void close(){}
}
