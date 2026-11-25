package br.com.tutorclinico;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34)
public class BackupCompatibilityTest {
 @Test public void testProtectedBackupAndLegacyObject() throws Exception {
  String json="{\"version\":1,\"cases\":[],\"appointments\":[]}";
  assertEquals(json,SecureStore.decryptBackup(json.getBytes(StandardCharsets.UTF_8),""));
  byte[] enc=SecureStore.encryptBackup(json,"senha-ação-123");
  assertEquals(json,SecureStore.decryptBackup(enc,"senha-ação-123"));
  try{SecureStore.decryptBackup(enc,"");fail("empty password");}catch(IllegalArgumentException e){assertTrue(e.getMessage().contains("senha"));}
  try{SecureStore.decryptBackup(enc,"senha-incorreta");fail("wrong password");}catch(SecurityException expected){}
 }
 @Test public void testFallbackExactlyMatchesStandard() throws Exception {
  String password="senha-ação-123";
  byte[] salt="abcdefghijklmnop".getBytes(StandardCharsets.UTF_8);
  PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,240000,256);
  byte[] standard=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
  assertArrayEquals(standard,SecureStore.pbkdf2Utf8(password,salt,240000));
 }
}
