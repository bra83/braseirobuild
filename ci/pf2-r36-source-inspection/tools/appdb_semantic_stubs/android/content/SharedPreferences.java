package android.content;
public interface SharedPreferences {
 int getInt(String k,int d); long getLong(String k,long d); boolean getBoolean(String k,boolean d); String getString(String k,String d); Editor edit();
 interface Editor { Editor putInt(String k,int v); Editor putLong(String k,long v); Editor putBoolean(String k,boolean v); Editor putString(String k,String v); Editor remove(String k); void apply(); boolean commit(); }
}
