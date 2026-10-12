package android.database;

public interface Cursor extends AutoCloseable {
    int FIELD_TYPE_NULL = 0;
    int FIELD_TYPE_INTEGER = 1;
    int FIELD_TYPE_FLOAT = 2;
    int FIELD_TYPE_STRING = 3;
    int FIELD_TYPE_BLOB = 4;

    boolean moveToFirst();
    boolean moveToNext();
    int getColumnIndex(String columnName);
    int getColumnIndexOrThrow(String columnName);
    String getString(int columnIndex);
    long getLong(int columnIndex);
    int getInt(int columnIndex);
    double getDouble(int columnIndex);
    float getFloat(int columnIndex);
    boolean isNull(int columnIndex);
    int getColumnCount();
    String getColumnName(int columnIndex);
    String[] getColumnNames();
    int getType(int columnIndex);
    void close();
}
