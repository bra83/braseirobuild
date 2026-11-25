package br.com.tutorclinico;

import static org.junit.Assert.assertEquals;

import android.database.sqlite.SQLiteDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class RobolectricSmokeTest {
    @Test public void nativeSqliteAvailable() {
        SQLiteDatabase db = SQLiteDatabase.create(null);
        db.execSQL("CREATE TABLE t(a INTEGER)");
        db.execSQL("INSERT INTO t VALUES(7)");
        try (android.database.Cursor c = db.rawQuery("SELECT sqlite_version(), a FROM t", null)) {
            c.moveToFirst();
            System.out.println("SQLITE_VERSION=" + c.getString(0));
            assertEquals(7, c.getInt(1));
        }
    }
}
